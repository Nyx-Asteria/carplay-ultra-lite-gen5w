package ioniq6.cpclimate;

import hmgapi.hook.ModContext;
import java.io.File;
import java.util.EnumSet;
import java.util.Locale;
import ioniq6.cpclimate.SessionLogPolicy.Category;
import ioniq6.cpclimate.SessionLogPolicy.Level;

/** DiPO process logging adapter. No disk work on session or protocol hooks. */
final class SessionLogging {
    private final ModContext mod;
    private final SessionLogPolicy policy;
    private final SessionLogWriter writer;
    private final String metadata;
    private long generation=-1;
    private long endingAt=-1;
    private String endingReason;
    private final java.util.concurrent.ScheduledExecutorService timer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"cpul-log-teardown");t.setDaemon(true);return t;}});
    private final PreSessionEvents preSession=new PreSessionEvents();
    private long sessionStartedAt, previousEndedAt;

    SessionLogging(ModContext mod, File files, String androidBuild, String environment) {
        this.mod=mod;
        boolean detail=mod.settingBoolean("diagnostic_detail",true);
        boolean packets=mod.settingBoolean("diagnostic_packets",false);
        EnumSet<Category> categories=SessionLogPolicy.diagnosticCategories(detail,packets);
        StringBuilder settings=new StringBuilder("android_build=").append(androidBuild).append("; ").append(environment);
        settings.append("; diagnostic_detail=").append(detail).append("; diagnostic_packets=").append(packets);
        settings.append("; diagnostic_categories=").append(categories);
        boolean enabled=mod.settingBoolean("session_logging",true);
        boolean usb=mod.settingBoolean("session_usb_export",true);
        settings.append("; session_logging=").append(enabled).append("; session_usb_export=").append(usb);
        for(String key:SessionSettings.KEYS) {
            if ("diagnostic_detail".equals(key)||"diagnostic_packets".equals(key)||"session_logging".equals(key)||"session_usb_export".equals(key)) continue;
            settings.append("; ").append(key).append('=').append(SessionSettings.read(mod,key));
        }
        metadata=settings.toString();
        policy=new SessionLogPolicy(categories);
        writer=enabled?new SessionLogWriter(new File(files,"cpul-session-logs"),
            usb?new SessionUsbExport(new SessionUsbDestination()):null,categories):null;
        if(writer!=null)timer.scheduleWithFixedDelay(new Runnable(){public void run(){reportWriterHealth();}},5,5,java.util.concurrent.TimeUnit.SECONDS);
    }
    private String lastWriterFailure,lastExportFailure;
    private void reportWriterHealth(){
        try {
            String write=writer.failure(),export=writer.exportFailure();
            if(!java.util.Objects.equals(write,lastWriterFailure)){
                lastWriterFailure=write;
                mod.log(write==null?"[session-log] writer recovered":"[session-log] writer failed: "+SessionLogPolicy.sanitize(write));
            }
            if(!java.util.Objects.equals(export,lastExportFailure)){
                lastExportFailure=export;
                mod.log(export==null?"[session-log] USB export recovered":"[session-log] USB export failed: "+SessionLogPolicy.sanitize(export));
            }
        }catch(RuntimeException ignored){ /* health monitoring must not stop the teardown timer */ }
    }
    synchronized void start(long id) {
        if(generation==id)return;
        if(generation>=0)closeNow(endingReason==null?"superseded by new session":endingReason);
        policy.reset(); generation=id; sessionStartedAt=System.currentTimeMillis();
        if(writer!=null)writer.start(id,metadata);
        for(PreSessionEvents.Event e:preSession.drain(sessionStartedAt,previousEndedAt)) {
            Category category=category(e.message);
            Level level=level(e.message,category);
            String safe=policy.accept(level,category,e.message,System.nanoTime()/1000000L);
            if(writer!=null && safe!=null)writer.recordAt(generation,level,category,safe,e.at);
        }
    }
    synchronized void finish(String reason) {
        if(generation<0||endingAt>=0)return;
        final long id=generation;
        endingAt=System.currentTimeMillis();
        endingReason=reason;
        timer.schedule(new Runnable(){public void run(){
            synchronized(SessionLogging.this){
                if(generation==id&&endingAt>=0)closeNow(endingReason);
            }
        }},2000,java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    private void closeNow(String reason) {
        if(writer!=null)writer.finishAt(generation,reason,policy.suppressed(),endingAt<0?System.currentTimeMillis():endingAt);
        previousEndedAt=System.currentTimeMillis();
        generation=-1; endingAt=-1; endingReason=null;
    }
    synchronized void projection(String message) {
        projection(message,System.currentTimeMillis());
    }
    synchronized void projection(String message,long at) {
        long now=System.currentTimeMillis();
        if(generation<0) {
            if(!teardown(message))preSession.add(message,at,now,previousEndedAt);
        } else if(at>=sessionStartedAt-PreSessionEvents.MAX_AGE_MS && at>previousEndedAt
                && at<=now+1000 && (endingAt<0 || teardown(message))) logAt(message,at);
    }
    private static boolean teardown(String message) {
        String s=message.toLowerCase(Locale.US);
        return s.contains("closed")||s.contains("ended")||s.contains("failed")||s.contains("stopped")||s.contains("disconnect")||s.contains("finaliz");
    }
    synchronized void log(String message) {
        long now=System.currentTimeMillis();
        if(writer!=null && generation<0)preSession.add(message,now,now,previousEndedAt);
        logAt(message,now);
    }
    private void logAt(String message,long at) {
        Category category=category(message);
        Level level=level(message,category);
        String safe=policy.accept(level,category,message,System.nanoTime()/1000000L);
        if(safe==null)return;
        if(writer!=null&&generation>=0&&(endingAt<0||teardown(message)))writer.recordAt(generation,level,category,safe,at);
        // With persistent logging enabled, keep extra trace/debug traffic out of the shared HMG log.
        // Lifecycle events and warnings/errors remain visible; with logging disabled, preserve manual diagnostics.
        if(writer==null || (level!=Level.DEBUG && level!=Level.TRACE))mod.log(safe);
    }
    static Category category(String message) {
        String s=message.toLowerCase(Locale.US);
        if(s.startsWith("[vs-packet]")||s.startsWith("[vs-navigation]"))return Category.VS_RAW;
        if(s.startsWith("[projection-packet]"))return Category.PROJECTION_RAW;
        if(s.startsWith("[projection-theme]"))return Category.THEMES;
        if(s.startsWith("[projection-video]"))return Category.AIRPLAY;
        if(s.startsWith("[radio-sxm]"))return Category.SXM;
        if(s.startsWith("[radio"))return Category.RADIO;
        if(s.startsWith("[audio")||s.startsWith("[media"))return Category.AUDIO;
        if(s.startsWith("sync")||s.startsWith("[theme"))return Category.THEMES;
        if(s.startsWith("video")||s.startsWith("[video"))return Category.AIRPLAY;
        if(s.startsWith("car ")||s.startsWith("ambient ")||s.startsWith("[seat")||s.startsWith("[climate")||s.startsWith("[wheel")||s.startsWith("vs phone")||s.startsWith("vs car"))return Category.CLIMATE;
        return Category.CORE;
    }
    static Level level(String message,Category category) {
        String s=message.toLowerCase(Locale.US);
        if(s.contains("failed")||s.contains("didn't authenticate")||s.contains("implausible package"))return Level.ERROR;
        if(s.contains("unsupported stream")||s.contains("timed out")||s.contains("timeout")||s.contains("unavailable")||s.contains("refused")||s.contains("queue full")||s.contains("listener stopped"))return Level.WARN;
        if(category==Category.VS_RAW||category==Category.PROJECTION_RAW||s.startsWith("sync phone:")||s.startsWith("sync car:"))return Level.TRACE;
        if(s.startsWith("car reader and sender started")||s.startsWith("vs phone follows")||s.startsWith("vs phone reads ")||s.startsWith("[seat-fan] subscribed")||
           s.startsWith("[radio-api]")||s.startsWith("[radio-sxm] catalog accessor")||s.startsWith("[radio-probe]")||s.startsWith("[video] message #")||
           s.startsWith("[video] video: answer ")||s.startsWith("video: answer "))return Level.DEBUG;
        return Level.INFO;
    }
}
