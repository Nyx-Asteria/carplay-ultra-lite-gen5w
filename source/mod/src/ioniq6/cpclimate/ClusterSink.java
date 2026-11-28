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
                new Thread("cp-climate-cluster-conn") {
                    @Override
                    public void run() {
                        drain(sock);
                    }
                }.start();
            }
        } catch (Throwable t) {
            out.log("[listener] " + what + " listener stopped: " + t.getClass().getSimpleName());
        } finally {
            bound=false;
            bindFinished.countDown();
        }
    }

    void drain(Socket sock) {
        out.log("[listener] " + what + " stream opened");
        long total = 0, next = REPORT_BYTES;
        long start = System.currentTimeMillis();
        try {
            InputStream in = sock.getInputStream();
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                if(total==0)out.log("[listener] " + what + " first bytes=" + n);
                total += n;
                if (total >= next) {
                    out.log(what + " stream: " + total / 1024 + " KB in " + (System.currentTimeMillis() - start) / 1000 + " s");
                    next += REPORT_BYTES;
                }
            }
        } catch (Throwable t) {
            out.log("[listener] " + what + " stream failed: " + t.getClass().getSimpleName());
        } finally {
            try {
                sock.close();
            } catch (Throwable ignored) {
            }
            out.log("[listener] " + what + " stream closed bytes=" + total + " elapsed_ms=" + Math.max(0,System.currentTimeMillis()-start));
        }
    }
}
