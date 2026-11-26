package ioniq6.cpclimate;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Single writer; callers never perform disk IO. One instance owns one local spool. */
final class SessionLogWriter {
    static final int MAX_SESSIONS = 20;
    static final long MAX_TOTAL = 64L * 1024 * 1024;
    static final long MAX_SESSION = 8L * 1024 * 1024;
    static final int MAX_QUEUE = 2048;
    private static final long ERROR_RESERVE = 512 * 1024;
    private final File root;
    private final ArrayBlockingQueue<Event> queue = new ArrayBlockingQueue<Event>(MAX_QUEUE);
    private final AtomicLong lost = new AtomicLong();
    private final Thread worker;
    private volatile String failure;
    private volatile boolean closed;
    private File directory;
    private FileOutputStream normal, errors;
    private long start, written, errorBytes, suppressed, generation;
    private String currentId;

    private static final class Event {
        final int kind;
        final long time, generation;
        final String text;
        final boolean important;
        Event(int kind, long time, long generation, String text, boolean important) {
            this.kind=kind; this.time=time; this.generation=generation; this.text=text; this.important=important;
        }
    }
    SessionLogWriter(File root) {
        this.root=root;
        worker=new Thread(new Runnable(){ public void run(){ loop(); } }, "cpul-session-log");
        worker.setDaemon(true);
        worker.start();
    }
    String failure() { return failure; }
    void start(long generation, String metadata) {
        offer(new Event(1,System.currentTimeMillis(),generation,SessionLogPolicy.sanitize(metadata),true));
    }
    void record(long generation, SessionLogPolicy.Level level, SessionLogPolicy.Category category, String safe) {
        if (safe == null) return;
        offer(new Event(0,System.currentTimeMillis(),generation,level+" "+category+" "+SessionLogPolicy.sanitize(safe),
            level==SessionLogPolicy.Level.ERROR || level==SessionLogPolicy.Level.WARN));
    }
    void finish(long generation, String reason, long suppressed) {
        offer(new Event(2,System.currentTimeMillis(),generation,SessionLogPolicy.sanitize(reason)+"; policy_suppressed="+suppressed,true));
    }
    // Reserve capacity for lifecycle and error events. On overload evict ordinary messages first.
    private synchronized void offer(Event e) {
        if(closed) return;
        if(!e.important && queue.remainingCapacity() <= 128) { lost.incrementAndGet(); return; }
        if(queue.offer(e)) return;
        if(e.important) {
            for(Event old:queue) if(old.kind==0 && !old.important && queue.remove(old)) {
                lost.incrementAndGet();
                if(queue.offer(e)) return;
                break;
            }
            if(e.kind!=0) {
                for(Event old:queue) if(old.kind==0 && queue.remove(old)) {
                    lost.incrementAndGet();
                    if(queue.offer(e)) return;
                    break;
                }
            }
        }
        lost.incrementAndGet();
        if(e.kind!=0) failure="session lifecycle queue exhausted";
    }
    synchronized void shutdown() { closed=true; }
    boolean await(long millis) throws InterruptedException { worker.join(millis); return !worker.isAlive(); }
    private void loop() {
        long last=androidIndependentMonotonic();
        try {
            ensureDir(root);
            recover(root);
            prune();
            while(!closed || !queue.isEmpty()) {
                Event e=queue.poll(250,TimeUnit.MILLISECONDS);
                if(e!=null) {
                    try {
                        if(e.kind==1) {
                            if(directory!=null) complete(e.time,"superseded without finalization");
                            begin(e);
                        } else if(directory!=null && generation==e.generation) {
                            if(e.kind==2) complete(e.time,e.text);
                            else append(e);
                        }
                    } catch(IOException ex) {
                        failure=ex.toString(); abandon();
                    }
                }
                long now=androidIndependentMonotonic();
                if(now-last>=5000) {
                    try { sync(); } catch(IOException ex){ failure=ex.toString(); abandon(); }
                    last=now;
                }
            }
            if(directory!=null) complete(System.currentTimeMillis(),"logger shutdown; session end not observed");
        } catch(Exception e) { failure=e.toString(); }
        finally { abandon(); }
    }
    private static long androidIndependentMonotonic(){ return System.nanoTime()/1000000L; }
    private void begin(Event e) throws IOException {
        prune();
        start=e.time; generation=e.generation; written=0; errorBytes=0; suppressed=0;
        currentId=new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.US).format(new Date(start))
            +"_session-"+generation+"_"+UUID.randomUUID().toString();
        directory=new File(root,currentId+".open");
        ensureDir(directory);
        normal=new FileOutputStream(new File(directory,"session.log"));
        errors=new FileOutputStream(new File(directory,"errors.log"));
        writeText(new File(directory,"summary.txt"),"generation="+generation+"\nstart_ms="+start+"\n"+e.text+"\n");
        append(new Event(0,e.time,generation,"INFO CORE session started",false));
        sync();
    }
    private void append(Event e) throws IOException {
        byte[] b=(e.time+" "+e.text+"\n").getBytes("UTF-8");
        if(written+b.length <= MAX_SESSION-ERROR_RESERVE-8192) { normal.write(b); written+=b.length; }
        else suppressed++;
        if(e.important && errorBytes+b.length<=ERROR_RESERVE) { errors.write(b); errorBytes+=b.length; }
        else if(e.important) suppressed++;
    }
    private void sync() throws IOException {
        if(normal!=null) { normal.flush(); normal.getFD().sync(); }
        if(errors!=null) { errors.flush(); errors.getFD().sync(); }
    }
    private void complete(long end,String reason) throws IOException {
        File folder=directory;
        append(new Event(0,end,generation,"INFO CORE session finalized: "+reason,false));
        sync();
        normal.close(); normal=null; errors.close(); errors=null;
        FileOutputStream out=new FileOutputStream(new File(folder,"summary.txt"),true);
        try {
            out.write(("end_ms="+end+"\nduration_ms="+Math.max(0,end-start)+"\ntermination="+reason+
                "\nqueue_dropped="+lost.getAndSet(0)+"\nsize_dropped="+suppressed+"\n").getBytes("UTF-8"));
            out.getFD().sync();
        } finally { out.close(); }
        if(!folder.renameTo(new File(root,currentId))) throw new IOException("session finalization rename failed");
        directory=null;
        prune();
    }
    private void abandon() {
        try { if(normal!=null) normal.close(); } catch(IOException ignored){}
        try { if(errors!=null) errors.close(); } catch(IOException ignored){}
        normal=null; errors=null; directory=null;
    }
    static void ensureDir(File dir) throws IOException {
        if(!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create log directory");
    }
    static void writeText(File file,String text) throws IOException {
        FileOutputStream out=new FileOutputStream(file);
        try { out.write(text.getBytes("UTF-8")); out.getFD().sync(); } finally { out.close(); }
    }
    static void recover(File root) throws IOException {
        File[] files=root.listFiles();
        if(files==null) return;
        for(File f:files) if(f.isDirectory() && f.getName().endsWith(".open")) {
            FileOutputStream out=new FileOutputStream(new File(f,"summary.txt"),true);
            try { out.write("\ntermination=unknown; recovered unfinished session\n".getBytes("UTF-8")); out.getFD().sync(); }
            finally { out.close(); }
            File target=new File(root,f.getName().substring(0,f.getName().length()-5));
            if(target.exists() || !f.renameTo(target)) throw new IOException("recovery rename failed");
        }
    }
    private void prune() throws IOException {
        File[] files=root.listFiles();
        if(files==null) return;
        List<File> sessions=new ArrayList<File>();
        long size=0;
        for(File f:files) if(f.isDirectory() && !f.getName().endsWith(".open")) { sessions.add(f); size+=size(f); }
        Collections.sort(sessions,new Comparator<File>(){public int compare(File a,File b){return a.getName().compareTo(b.getName());}});
        // Reserve one full session while a writer may be active.
        while(sessions.size()>=MAX_SESSIONS || size>MAX_TOTAL-MAX_SESSION) {
            File f=sessions.remove(0); long n=size(f); delete(f); size-=n;
        }
    }
    static long size(File f) {
        if(!f.isDirectory()) return f.length();
        long n=0; File[] children=f.listFiles(); if(children!=null) for(File c:children)n+=size(c); return n;
    }
    static void delete(File f) throws IOException {
        if(f.isDirectory()) { File[] children=f.listFiles(); if(children!=null)for(File c:children)delete(c); }
        if(!f.delete() && f.exists()) throw new IOException("log retention delete failed");
    }
}
