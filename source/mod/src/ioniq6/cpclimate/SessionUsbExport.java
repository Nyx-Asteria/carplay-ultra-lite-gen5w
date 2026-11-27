package ioniq6.cpclimate;

import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;

/** Exports only finalized CPUL session files. Call on the spool owner thread, never on a protocol thread. */
final class SessionUsbExport {
    interface Destination { File find() throws Exception; }
    private final Destination destination;
    SessionUsbExport(Destination destination) { this.destination=destination; }

    static boolean sessionName(String name) {
        return name.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}_session-[0-9]+_[0-9a-f-]{36}");
    }
    int pending(File spool) throws Exception {
        File usb=destination.find();
        if(usb==null) return 0;
        File root=new File(usb,"CPUL-LOGS");
        SessionLogWriter.ensureDir(root);
        File[] files=spool.listFiles();
        if(files==null) return 0;
        Arrays.sort(files);
        int count=0;
        for(File f:files) {
            if(!f.isDirectory() || !sessionName(f.getName()) || new File(f,".exported").isFile()) continue;
            export(f,root);
            count++;
        }
        return count;
    }
    private static void export(File source,File root) throws Exception {
        File target=new File(root,source.getName());
        File partial=new File(root,source.getName()+".partial");
        String manifest=manifest(source);
        if(target.exists()) {
            if(!new File(target,"COMPLETE.txt").isFile() || !manifest.equals(readSmall(new File(target,"COMPLETE.txt"))) ||
                !manifest.equals(manifest(target))) throw new IOException("USB destination conflict");
        } else {
            if(partial.exists()) SessionLogWriter.delete(partial);
            SessionLogWriter.ensureDir(partial);
            if(root.getUsableSpace()<SessionLogWriter.size(source)+1048576L) throw new IOException("insufficient USB space");
            copyTree(source,partial);
            if(!manifest.equals(manifest(partial))) throw new IOException("USB verification failed");
            SessionLogWriter.writeText(new File(partial,"COMPLETE.txt"),manifest);
            if(!partial.renameTo(target)) throw new IOException("USB final rename failed");
        }
        // Local marker only after verified completion. If power is lost before this,
        // the existing destination is verified on retry rather than duplicated.
        SessionLogWriter.writeText(new File(source,".exported"),"verified\n");
    }
    private static boolean allowed(String name) {
        return name.equals("summary.txt") || name.equals("session.log") || name.equals("errors.log") ||
               name.matches("[a-z_]+\\.log");
    }
    private static void copyTree(File from,File to) throws IOException {
        File[] files=from.listFiles();
        if(files==null) throw new IOException("cannot list session");
        for(File f:files) {
            if(f.isDirectory() && f.getName().equals("diagnostics")) {
                File d=new File(to,"diagnostics"); SessionLogWriter.ensureDir(d); copyTree(f,d);
            } else if(f.isFile() && allowed(f.getName())) {
                InputStream in=new FileInputStream(f);
                try {
                    FileOutputStream out=new FileOutputStream(new File(to,f.getName()));
                    try { byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.getFD().sync(); }
                    finally {out.close();}
                } finally {in.close();}
            }
        }
    }
    private static String manifest(File dir) throws Exception {
        if(!new File(dir,"summary.txt").isFile()) throw new IOException("missing summary");
        StringBuilder s=new StringBuilder();
        manifest(dir,"",s);
        return s.toString();
    }
    private static void manifest(File dir,String prefix,StringBuilder s) throws Exception {
        File[] files=dir.listFiles();
        if(files==null) throw new IOException("cannot list manifest");
        Arrays.sort(files);
        for(File f:files) {
            if(f.isDirectory() && f.getName().equals("diagnostics")) manifest(f,prefix+"diagnostics/",s);
            else if(f.isFile() && allowed(f.getName())) {
                MessageDigest md=MessageDigest.getInstance("SHA-256");
                InputStream in=new FileInputStream(f);
                try{byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}finally{in.close();}
                s.append(prefix).append(f.getName()).append(' ').append(f.length()).append(' ');
                for(byte b:md.digest()) {int v=b&255;s.append(Character.forDigit(v>>>4,16)).append(Character.forDigit(v&15,16));}
                s.append('\n');
            }
        }
    }
    private static String readSmall(File f) throws IOException {
        if(f.length()>65536) throw new IOException("oversized completion marker");
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        InputStream in=new FileInputStream(f);
        try{byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>65536)throw new IOException("oversized marker");}}
        finally{in.close();}
        return out.toString("UTF-8");
    }
}
