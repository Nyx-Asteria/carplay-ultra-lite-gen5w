package ioniq6.cpclimate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * One vehicle state data stream (lane 0, or 1 for high priority): the port the native SETUP
 * handler gave the phone, the keys it saved, every message the phone sends logged and handed to
 * the VehicleState both lanes share (M3), and its answers sent back as async "cmnd" packages on
 * the lane they belong to (LIVI iap_tunnel.rs duplex). A "sync" package also gets an empty reply.
 *
 * Package: size (4, big-endian, header included), kind (4), 8 zero bytes, message type (4),
 * reply token (8), 4 zero bytes, then the body. Based on LIVI by Lasse Heitgres (https://github.com/f-io/LIVI).
 */
final class VsChannel extends Thread {
    static final int PORT = 7130;
    static final int HEADER = 32;
    static final int MAX_PACKAGE = 4 * 1024 * 1024;
    static final int SYNC = 0x73796e63, ASYNC = 0x6173796e, REPLY = 0x72706c79;
    static final int COMMAND = 0x636d6e64;
    static final long SESSION_PAIRING_MS = 5000;
    /** the real car (CarClimate), attached to each session's VehicleState; null in tests without one */
    static volatile VehicleState.Car car;
    static final int MAX_LOGGED = 200;
    static final int MAX_TEXT = 1500;

    /** where the log goes: CpClimate's ModContext on the car, stdout in tests */
    interface Log {
        void log(String m);
    }

    final int lane;
    final File dir;
    final Log out;
    final VehicleState[] state;
    final VsChannel[] lanes;
    private volatile Socket current;
    private Writer writer;
    private VehicleState writerOwner;
    volatile ServerSocket server;

    /** the open connection's sending half; one at a time, as the cipher counts its frames */
    static final class Writer {
        final OutputStream os;
        final ChaChaPoly cipher;
        final Socket sock;

        Writer(Socket sock, OutputStream os, ChaChaPoly cipher) {
            this.sock = sock;
            this.os = os;
            this.cipher = cipher;
        }

        synchronized void send(byte[] p) throws java.io.IOException {
            os.write(cipher.seal(p));
        }
    }

    /**
     * state[0] is the VehicleState both lanes share (a new one for each CarPlay session, set by
     * whoever starts the session); lanes[n] is the channel of lane n, for answers on the other lane.
     */
    VsChannel(int lane, File dir, Log out, VehicleState[] state, VsChannel[] lanes) {
        super("cp-climate-vs" + lane);
        this.lane = lane;
        this.dir = dir;
        this.out = out;
        this.state = state;
        this.lanes = lanes;
        setDaemon(true);
    }

    /** true while the phone is connected on this lane */
    synchronized boolean isOpen() {
        return writer != null;
    }

    /** sends one message to the phone on this lane; false if the lane has no connection */
    boolean send(byte[] message) {
        Writer w;
        synchronized (this) {
            w = writer;
        }
        if (w == null) return false;
        try {
            w.send(pack(ASYNC, COMMAND, 0, message));
            return true;
        } catch (Exception e) {
            out.log("VS" + lane + " send failed: " + e);
            close(w.sock);
            return false;
        }
    }

    /** answers go on their own lane, or on the other one if that is all that's open (as LIVI does) */
    void deliver(List<VehicleState.Out> answers) {
        for (VehicleState.Out a : answers) {
            VsChannel to = lanes[a.lane], other = lanes[1 - a.lane];
            int delivered=-1;
            if(to!=null&&to.send(a.message))delivered=a.lane;
            else if(other!=null&&other.send(a.message))delivered=1-a.lane;
            if(delivered<0)out.log("VS no stream left to answer on");
            if(a.seatDiagnostic!=null)out.log("[seat-feedback] notification"+a.seatDiagnostic+" requestedLane="+a.lane+" sentLane="+delivered+" socketWrite="+(delivered>=0));
        }
    }

    static boolean ownsConnection(VehicleState owner,VehicleState active,Socket socket,Socket currentSocket) {
        return owner!=null && owner==active && socket!=null && socket==currentSocket && !socket.isClosed();
    }

    /** Capture only writers belonging to the expected state. No socket I/O under the state lock. */
    synchronized Writer writerFor(VehicleState owner) {
        return owner != null && writerOwner == owner && writer != null && writer.sock == current ? writer : null;
    }

    static boolean sendCaptured(Writer w, byte[] message, Log out) {
        if (w == null) return false;
        try {
            w.send(pack(ASYNC, COMMAND, 0, message));
            return true;
        } catch (Exception e) {
            out.log("VS captured writer failed: " + e);
            close(w.sock);
            return false;
        }
    }

    /** Never fall back to a new session's writer when an old socket closes. */
    boolean deliverOwned(VehicleState owner, List<VehicleState.Out> answers) {
        Writer[] captured = new Writer[2];
        synchronized (state) {
            if (owner == null || state[0] != owner) return false;
            for (int i=0;i<2;i++) if (lanes[i]!=null) captured[i]=lanes[i].writerFor(owner);
        }
        if (captured[0]==null && captured[1]==null) return false;
        boolean complete=true;
        for (VehicleState.Out a : answers) {
            int delivered=-1;
            if (sendCaptured(captured[a.lane],a.message,out)) delivered=a.lane;
            else if (sendCaptured(captured[1-a.lane],a.message,out)) delivered=1-a.lane;
            if(delivered<0){
                complete=false;
                out.log("VS no session-owned stream left to answer on");
            }
            if(a.seatDiagnostic!=null)out.log("[seat-feedback] notification"+a.seatDiagnostic+" requestedLane="+a.lane+" sentLane="+delivered+" socketWrite="+(delivered>=0));
        }
        return complete;
    }

    /**
     * A session's two lanes connect within moments of each other, so a connection while the other
     * lane is closed, or has been open a while, starts a new session with a fresh VehicleState.
     */
    VehicleState startSession() {
        long now = System.currentTimeMillis();
        // one decision for both lanes, under the shared state's lock: the phone opens them within
        // milliseconds, so whichever comes second joins the session the first one started
        synchronized (state) {
            VehicleState current = state[0];
            if (current != null && now - current.startedAt < SESSION_PAIRING_MS) {
                out.log("VS" + lane + " joins the session's vehicle state");
                return current;
            }
            VehicleState vs = new VehicleState(out);
            vs.car = car;
            vs.startedAt = now;
            state[0] = vs;
            out.log("VS" + lane + " new session: fresh vehicle state");
            return vs;
        }
    }

    @Override
    public void run() {
        try {
            ServerSocket s = new ServerSocket();
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(PORT + lane));
            server = s;
            out.log("VS" + lane + " listening on port " + (PORT + lane));
            while (true) {
                final Socket sock = s.accept();
                Socket old = current;
                current = sock;
                if (old != null) close(old);
                new Thread("cp-climate-vs" + lane + "-conn") {
                    @Override
                    public void run() {
                        serve(sock);
                    }
                }.start();
            }
        } catch (Throwable t) {
            out.log("VS" + lane + " listener stopped: " + t);
        }
    }

    void serve(Socket sock) {
        String who = String.valueOf(sock.getRemoteSocketAddress());
        out.log("VS" + lane + " connection opened");
        int logged = 0;
        try {
            byte[] keys = readKeys();
            if (keys == null) {
                out.log("VS" + lane + " no keys from the SETUP: closed");
                return;
            }
            byte[] rd = new byte[32], wr = new byte[32];
            System.arraycopy(keys, 8, rd, 0, 32);
            System.arraycopy(keys, 40, wr, 0, 32);
            ChaChaPoly cipher = new ChaChaPoly(rd, wr);
            InputStream in = sock.getInputStream();
            OutputStream os = sock.getOutputStream();
            Writer w = new Writer(sock, os, cipher);
            final VehicleState owner;
            synchronized (state) {
                if (current != sock) return;
                owner = startSession();
                synchronized (this) {
                    if (current != sock) return;
                    writer = w;
                    writerOwner = owner;
                }
            }
            byte[] sealed = new byte[64 * 1024];
            int have = 0;
            ByteArrayOutputStream plain = new ByteArrayOutputStream();
            while (true) {
                if (have == sealed.length) sealed = java.util.Arrays.copyOf(sealed, sealed.length * 2);
                int n = in.read(sealed, have, sealed.length - have);
                if (n < 0) break;
                have += n;
                int used = cipher.open(sealed, have, plain);
                if (used < 0) {
                    out.log("VS" + lane + " a frame didn't authenticate: closed");
                    return;
                }
                System.arraycopy(sealed, used, sealed, 0, have - used);
                have -= used;
                byte[] p = plain.toByteArray();
                int at = 0;
                while (p.length - at >= HEADER) {
                    int size = be32(p, at);
                    if (size < HEADER || size > MAX_PACKAGE) {
                        out.log("VS" + lane + " implausible package size " + size + ": closed");
                        return;
                    }
                    if (p.length - at < size) break;
                    int kind = be32(p, at + 4), type = be32(p, at + 16);
                    long token = (long) be32(p, at + 20) << 32 | (be32(p, at + 24) & 0xffffffffL);
                    byte[] body = java.util.Arrays.copyOfRange(p, at + HEADER, at + size);
                    if (kind == SYNC) w.send(pack(REPLY, 0, token, new byte[0]));
                    logged++;
                    out.log("[vs-packet] " + SessionLogPolicy.packet("VS" + lane, kind, type, body.length));
                    // Never dispatch an old connection's request to a replacement state.
                    if (kind != REPLY) {
                        synchronized (state) {
                            if (!ownsConnection(owner,state[0],sock,current)) return;
                        }
                        deliverOwned(owner,owner.handle(lane,body));
                    }
                    at += size;
                }
                plain.reset();
                plain.write(p, at, p.length - at);
            }
            out.log("VS" + lane + " connection closed by the phone after " + logged + " message(s)");
        } catch (Throwable t) {
            out.log("VS" + lane + " connection ended: " + t);
        } finally {
            synchronized (this) {
                if (writer != null && writer.sock == sock) {
                    writer = null;
                    writerOwner = null;
                }
            }
            close(sock);
        }
    }

    byte[] readKeys() {
        File f = new File(dir, "cpc-vs-" + lane + ".key");
        byte[] b = new byte[72];
        try {
            InputStream in = new FileInputStream(f);
            try {
                int got = 0;
                while (got < b.length) {
                    int n = in.read(b, got, b.length - got);
                    if (n < 0) return null;
                    got += n;
                }
                return b;
            } finally {
                in.close();
            }
        } catch (Exception e) {
            return null;
        }
    }

    static byte[] pack(int kind, int type, long token, byte[] body) {
        byte[] p = new byte[HEADER + body.length];
        put32(p, 0, p.length);
        put32(p, 4, kind);
        put32(p, 16, type);
        put32(p, 20, (int) (token >>> 32));
        put32(p, 24, (int) token);
        System.arraycopy(body, 0, p, HEADER, body.length);
        return p;
    }

    static String text(byte[] body) {
        String s = body.length > 0 ? Opack.show(body) : "";
        if (s == null) s = "<" + body.length + " bytes, not OPACK>";
        return s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) + "..." : s;
    }

    static String tag(int v) {
        char[] c = { (char) (v >>> 24 & 0xff), (char) (v >>> 16 & 0xff), (char) (v >>> 8 & 0xff), (char) (v & 0xff) };
        for (char x : c) if (x < 0x20 || x > 0x7e) return Integer.toHexString(v);
        return new String(c);
    }

    static int be32(byte[] b, int at) {
        return (b[at] & 0xff) << 24 | (b[at + 1] & 0xff) << 16 | (b[at + 2] & 0xff) << 8 | (b[at + 3] & 0xff);
    }

    static void put32(byte[] b, int at, int v) {
        b[at] = (byte) (v >>> 24); b[at + 1] = (byte) (v >>> 16); b[at + 2] = (byte) (v >>> 8); b[at + 3] = (byte) v;
    }

    static void close(Socket s) {
        try {
            s.close();
        } catch (Exception e) {
            // already closed
        }
    }
}
