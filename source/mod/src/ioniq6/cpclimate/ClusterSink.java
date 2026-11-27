package ioniq6.cpclimate;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The CarPlay Ultra experiment's virtual cluster: the native SETUP handler sends the phone's
 * cluster screen stream (type 111) here, and this takes the video and throws it away, so iOS
 * sees a cluster display that works. Nothing is decrypted or shown; it logs how much came in.
 */
final class ClusterSink extends Thread {
    static final int PORT = 7132;
    /** a progress line every this many bytes */
    static final long REPORT_BYTES = 4L * 1024 * 1024;

    final VsChannel.Log out;
    final int port;
    final String what;

    ClusterSink(VsChannel.Log out) {
        this(out, PORT, "cluster");
    }

    /** a sink for another stream the phone opens (the videoPlayback experiment's data streams) */
    ClusterSink(VsChannel.Log out, int port, String what) {
        super("cp-climate-" + what);
        this.out = out;
        this.port = port;
        this.what = what;
        setDaemon(true);
    }

    private final java.util.concurrent.CountDownLatch bindFinished=new java.util.concurrent.CountDownLatch(1);
    private volatile boolean bound;
    private long nextConnection;

    boolean awaitReady(long timeoutMs) {
        try {
            return bindFinished.await(timeoutMs,java.util.concurrent.TimeUnit.MILLISECONDS) && bound;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void run() {
        long started = System.nanoTime();
        out.log("[listener] " + what + " bind requested port=" + port);
        try (ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(port));
            bound=true;
            bindFinished.countDown();
            out.log("[listener] " + what + " ready port=" + port + " elapsed_ms=" + (System.nanoTime()-started)/1000000L);
            while (true) {
                final Socket sock = s.accept();
                final long connection=++nextConnection;
                Thread worker=new Thread("cp-climate-"+what+"-conn-"+connection) {
                    @Override
                    public void run() {
                        drain(sock,connection);
                    }
                };
                worker.setDaemon(true);
                try {worker.start();}
                catch(RuntimeException e){VsChannel.close(sock);throw e;}
                catch(Error e){VsChannel.close(sock);throw e;}
            }
        } catch (Throwable t) {
            out.log("[listener] " + what + " listener stopped: " + t.getClass().getSimpleName());
        } finally {
            bound=false;
            bindFinished.countDown();
        }
    }

    private void drain(Socket sock,long connection) {
        long total = 0, next = REPORT_BYTES;
        long start = System.nanoTime();
        String label="[listener] "+what;
        String id=" connection="+connection;
        boolean eof=false;
        try {
            out.log(label+" stream opened"+id);
            InputStream in = sock.getInputStream();
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                if(total==0)out.log(label+" first bytes="+n+id);
                total += n;
                if (total >= next) {
                    out.log(what+" stream: "+total/1024+" KB in "+(System.nanoTime()-start)/1000000000L+" s"+id);
                    next += REPORT_BYTES;
                }
            }
            eof=n<0;
        } catch (Throwable t) {
            out.log(label+" stream failed: "+t.getClass().getSimpleName()+id);
        } finally {
            try {
                sock.close();
            } catch (Throwable ignored) {
            }
            out.log(label+" stream closed bytes="+total+" elapsed_ms="+Math.max(0,(System.nanoTime()-start)/1000000L)+id+" eof="+eof);
        }
    }
}
