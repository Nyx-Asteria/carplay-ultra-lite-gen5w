package ioniq6.cpclimate;

import java.io.*;

/** The dumper's known K5 mount candidates, verified against the live mount table.
 * The DiPO process must have write permission; absence is not a fatal error. */
final class SessionUsbDestination implements SessionUsbExport.Destination {
    private static final String[] ROOTS={"/storage/usb0","/storage/usb1","/storage/usb2",
        "/mnt/media_rw/usb0","/mnt/media_rw/usb1","/mnt/media_rw/usb2"};
    public File find() {
        // Use registered HMG-API directories when the client is visible in this process.
        try {
            Class<?> api=Class.forName("hmgapi.client.HmgApi");
            Object client=api.getMethod("connect").invoke(null);
            Object folders=client.getClass().getMethod("usbFolders").invoke(client);
            if(folders instanceof Iterable) for(Object entry:(Iterable<?>)folders) {
                if(entry instanceof String) {
                    File f=new File((String)entry);
                    if(onKnownMount(f)&&writable(f))return f;
                }
            }
        } catch(Exception ignored) {} catch(LinkageError ignored) {}
        for(String path:ROOTS) {
            File root=new File(path);
            if(mounted(root)&&writable(root))return root;
        }
        return null;
    }
    private static boolean onKnownMount(File folder) throws IOException {
        String path=folder.getCanonicalPath();
        for(String p:ROOTS) {
            File root=new File(p);
            String base=root.getCanonicalPath();
            if((path.equals(base)||path.startsWith(base+File.separator))&&mounted(root))return true;
        }
        return false;
    }
    private static boolean mounted(File root) {
        try {
            String canonical=root.getCanonicalPath();
            BufferedReader in=new BufferedReader(new FileReader("/proc/mounts"));
            try {
                String line;
                while((line=in.readLine())!=null) {
                    String[] fields=line.split(" ");
                    if(fields.length>2&&(fields[1].equals(root.getPath())||fields[1].equals(canonical)))return true;
                }
            } finally {in.close();}
        } catch(IOException ignored) {}
        return false;
    }
    private static boolean writable(File folder) {
        if(!folder.isDirectory()||!folder.canWrite())return false;
        File probe=null;
        try {probe=File.createTempFile(".cpul-write-",".tmp",folder);return true;}
        catch(IOException ignored){return false;}
        finally {if(probe!=null)probe.delete();}
    }
}
