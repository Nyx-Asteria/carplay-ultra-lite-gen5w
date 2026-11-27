package ioniq6.cpclimate;
import java.io.*;
public final class SessionUsbExportTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static void hashRegression() throws Exception {
        java.util.Random rng=new java.util.Random(12345);
        for(int len:new int[]{0,1,55,56,57,63,64,65,119,120,127,128,129,32767,32768,32769,4194304}) {
            byte[] data=new byte[len]; rng.nextBytes(data);
            byte[] expected=java.security.MessageDigest.getInstance("SHA-256").digest(data);
            SessionUsbExport.Sha256 stream=new SessionUsbExport.Sha256();
            for(int off=0;off<len;) {int count=Math.min(37,len-off);stream.update(data,off,count);off+=count;}
            check(java.util.Arrays.equals(expected,stream.digest()));
        }
    }
    public static void main(String[] args)throws Exception{
        hashRegression();
        File root=File.createTempFile("cpul-export","");check(root.delete());check(root.mkdir());
        java.security.Provider[] providers=java.security.Security.getProviders();
        for(java.security.Provider provider:providers)java.security.Security.removeProvider(provider.getName());
        try{
            try {java.security.MessageDigest.getInstance("SHA-256");throw new AssertionError("provider unexpectedly available");}
            catch(java.security.NoSuchAlgorithmException expected){}
            File spool=new File(root,"spool"),usb=new File(root,"usb");
            check(spool.mkdir());check(usb.mkdir());
            String id="2026-11-26_01-00-00_session-7_12345678-1234-1234-1234-123456789abc";
            File s=new File(spool,id);check(s.mkdir());
            SessionLogWriter.writeText(new File(s,"summary.txt"),"end");
            SessionLogWriter.writeText(new File(s,"session.log"),"data");
            SessionLogWriter.writeText(new File(s,"secret.bin"),"not exported");
            check(new File(spool,id+".open").mkdir());
            final File[] drive={null};
            SessionUsbExport x=new SessionUsbExport(new SessionUsbExport.Destination(){public File find(){return drive[0];}});
            check(x.pending(spool)==0);
            drive[0]=usb;
            File partial=new File(usb,"CPUL-LOGS/"+id+".partial");check(partial.mkdirs());
            SessionLogWriter.writeText(new File(partial,"session.log"),"interrupted");
            check(x.pending(spool)==1);
            File dest=new File(usb,"CPUL-LOGS/"+id);
            check(new File(dest,"COMPLETE.txt").isFile());check(!partial.exists());
            check(!new File(dest,"secret.bin").exists());
            check(x.pending(spool)==0);
            check(new File(s,".exported").delete());
            check(x.pending(spool)==1);
            check(new File(s,".exported").delete());
            SessionLogWriter.writeText(new File(dest,"session.log"),"corrupt");
            boolean failed=false;try{x.pending(spool);}catch(IOException e){failed=true;}
            check(failed&&!new File(s,".exported").exists());
            // A destination disappearing during an export must not mark the source complete.
            SessionLogWriter.writeText(new File(dest,"session.log"),"data");
            final int[] probes={0};
            SessionUsbExport unstable=new SessionUsbExport(new SessionUsbExport.Destination(){public File find(){return ++probes[0]<=2?usb:null;}});
            failed=false;try{unstable.pending(spool);}catch(IOException e){failed=true;}
            check(failed&&!new File(s,".exported").exists());
            check(x.pending(spool)==1);
            check(new File(s,".exported").delete());
            final int[] identities={0};
            SessionUsbExport swapped=new SessionUsbExport(new SessionUsbExport.Destination(){
                public File find(){return usb;}
                public String identity(File folder){return ++identities[0]<=3?"mount-31":"mount-32";}
            });
            failed=false;try{swapped.pending(spool);}catch(IOException e){failed=true;}
            check(failed&&!new File(s,".exported").exists());
            check(x.pending(spool)==1);
            System.out.println("SessionUsbExportTest passed");
        }finally{for(java.security.Provider provider:providers)java.security.Security.addProvider(provider);SessionLogWriter.delete(root);}
    }
}
