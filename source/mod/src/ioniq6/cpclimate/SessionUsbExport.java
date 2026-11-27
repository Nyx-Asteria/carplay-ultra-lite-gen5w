package ioniq6.cpclimate;

import java.io.*;
import java.util.Arrays;

/** Exports only finalized CPUL session files. Call on the spool owner thread, never on a protocol thread. */
final class SessionUsbExport {
    interface Destination { File find() throws Exception; }
    private final Destination destination;
    SessionUsbExport(Destination destination) { this.destination=destination; }

    static boolean sessionName(String name) {
        return name.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}-[0-9]{2}-[0-9]{2}_session-[0-9]+_[0-9a-f-]{36}");
    }
    File destinationRoot() throws Exception {
        File usb=destination.find();
        if(usb==null)return null;
        File root=new File(usb,"CPUL-LOGS");
        SessionLogWriter.ensureDir(root);
        return root;
    }
    void exportOne(File source,File root)throws Exception { export(source,root); }
    int pending(File spool) throws Exception {
        File root=destinationRoot();
        if(root==null)return 0;
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
    private void requireDestination(File root) throws Exception {
        File usb=destination.find();
        if(usb==null || !new File(usb,"CPUL-LOGS").getCanonicalFile().equals(root.getCanonicalFile()) || !root.isDirectory())
            throw new IOException("USB destination changed or unavailable");
    }
    private void export(File source,File root) throws Exception {
        requireDestination(root);
        File target=new File(root,source.getName());
        File partial=new File(root,source.getName()+".partial");
        String manifest=manifest(source);
        requireDestination(root);
        if(target.exists()) {
            if(!new File(target,"COMPLETE.txt").isFile() || !manifest.equals(readSmall(new File(target,"COMPLETE.txt"))) ||
                !manifest.equals(manifest(target))) throw new IOException("USB destination conflict");
        } else {
            if(partial.exists()) SessionLogWriter.delete(partial);
            SessionLogWriter.ensureDir(partial);
            if(root.getUsableSpace()<SessionLogWriter.size(source)+1048576L) throw new IOException("insufficient USB space");
            copyTree(source,partial);
            requireDestination(root);
            if(!manifest.equals(manifest(partial))) throw new IOException("USB verification failed");
            SessionLogWriter.writeText(new File(partial,"COMPLETE.txt"),manifest);
            requireDestination(root);
            if(!partial.renameTo(target)) throw new IOException("USB final rename failed");
        }
        // Local marker only after verified completion. If power is lost before this,
        // the existing destination is verified on retry rather than duplicated.
        requireDestination(root);
        if(!manifest.equals(manifest(target)) || !manifest.equals(readSmall(new File(target,"COMPLETE.txt"))))
            throw new IOException("USB final verification failed");
        requireDestination(root);
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
                Sha256 md=new Sha256();
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

    /** Provider-independent export checksum; identical to WCP/STBT development implementation. */
    static final class Sha256 {
        private static final int[] K = {
            0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
            0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
            0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
            0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
            0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
            0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
            0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
            0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2
        };
        private final int[] h={0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
        private final int[] w=new int[64];
        private final byte[] block=new byte[64];
        private long length;
        private int used;
        private boolean finished;
        void update(byte[] bytes,int off,int n) {
            if(finished) throw new IllegalStateException("digest finished");
            if(off<0 || n<0 || off>bytes.length-n) throw new IndexOutOfBoundsException();
            length+=n;
            while(n>0) {
                int take=Math.min(n,64-used);
                System.arraycopy(bytes,off,block,used,take);
                used+=take;off+=take;n-=take;
                if(used==64){compress();used=0;}
            }
        }
        private void compress() {
            for(int i=0;i<16;i++){int j=i*4;w[i]=((block[j]&255)<<24)|((block[j+1]&255)<<16)|((block[j+2]&255)<<8)|(block[j+3]&255);}
            for(int i=16;i<64;i++){
                int x=w[i-15],y=w[i-2];
                int s0=Integer.rotateRight(x,7)^Integer.rotateRight(x,18)^(x>>>3);
                int s1=Integer.rotateRight(y,17)^Integer.rotateRight(y,19)^(y>>>10);
                w[i]=w[i-16]+s0+w[i-7]+s1;
            }
            int a=h[0],b=h[1],c=h[2],d=h[3],e=h[4],f=h[5],g=h[6],z=h[7];
            for(int i=0;i<64;i++){
                int s1=Integer.rotateRight(e,6)^Integer.rotateRight(e,11)^Integer.rotateRight(e,25);
                int ch=(e&f)^(~e&g);
                int t1=z+s1+ch+K[i]+w[i];
                int s0=Integer.rotateRight(a,2)^Integer.rotateRight(a,13)^Integer.rotateRight(a,22);
                int maj=(a&b)^(a&c)^(b&c);
                int t2=s0+maj;
                z=g;g=f;f=e;e=d+t1;d=c;c=b;b=a;a=t1+t2;
            }
            h[0]+=a;h[1]+=b;h[2]+=c;h[3]+=d;h[4]+=e;h[5]+=f;h[6]+=g;h[7]+=z;
        }
        byte[] digest(){
            if(finished) throw new IllegalStateException("digest finished");
            finished=true;
            long bits=length*8L;
            block[used++]=(byte)0x80;
            if(used>56){Arrays.fill(block,used,64,(byte)0);compress();used=0;}
            Arrays.fill(block,used,56,(byte)0);
            for(int i=0;i<8;i++) block[63-i]=(byte)(bits>>>(i*8));
            compress();
            byte[] result=new byte[32];
            for(int i=0;i<8;i++)for(int j=0;j<4;j++)result[i*4+j]=(byte)(h[i]>>>(24-j*8));
            return result;
        }
    }
}
