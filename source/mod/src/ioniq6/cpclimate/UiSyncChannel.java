package ioniq6.cpclimate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * The uiSync experiment: the native SETUP handler sends the phone's uiSync data stream (type 130,
 * clientTypeUUID 07D9F906-…) here, with its keys in files/cpc-vs-3.key. Decrypted like the iAP
 * stream (a duplex tunnel, LIVI iap_tunnel.rs): every package body that isn't a reply is a uiSync
 * message for UiSync, whose answers go back as async 'cmnd' packages.
 */
final class UiSyncChannel extends Thread {
    static final int PORT = 7135;
    static final String KEYS = "cpc-vs-3.key";
    static final int MAX_LOGGED = 300;

    final File dir;
    final VsChannel.Log out;
    final UiSync sync;

    UiSyncChannel(File dir, VsChannel.Log out, UiSync.Layout layout) {
        this(dir, out, layout, false);
    }

    UiSyncChannel(File dir, VsChannel.Log out, UiSync.Layout layout, boolean legacyCenterLayout) {
        super("cp-climate-uisync");
        this.dir = dir;
        this.out = out;
        sync = new UiSync(new File(dir, "cpc-theme-preferences.properties"), true);
        sync.wanted = layout;
        sync.legacyCenterLayout = legacyCenterLayout;
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
        out.log("[listener] uiSync bind requested port=" + PORT);
        try (ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(PORT));
            bound=true;
            bindFinished.countDown();
            out.log("[listener] uiSync ready elapsed_ms=" + (System.nanoTime()-started)/1000000L);
            out.log("SYNC listening on port " + PORT + " (uiSync experiment, cluster layout "
                    + (sync.wanted == null ? "not requested" : sync.wanted.name + " " + sync.wanted.palette) + ", force center Wallpaper=" + sync.legacyCenterLayout + ")");
            while (true) {
                final Socket sock = s.accept();
                // Capture keys at admission, not when the worker eventually runs.
                // A subsequent SETUP must not replace this socket's cipher.
                final ChaChaPoly cipher = snapshotCipher(dir);
                if (cipher == null) {
                    out.log("SYNC no complete keys at socket admission: closed");
                    VsChannel.close(sock);
                    continue;
                }
                Thread worker = new Thread("cp-climate-uisync-conn") {
                    @Override
                    public void run() {
                        serve(sock, cipher);
                    }
                };
                worker.setDaemon(true);
                worker.start();
            }
        } catch (Throwable t) {
            out.log("[listener] uiSync listener stopped: " + t.getClass().getSimpleName());
        } finally {
            bound=false;
            bindFinished.countDown();
        }
    }

    static String panelId(byte[] body){
        try{
            Object decoded=OpackCodec.decode(body);
            if(!(decoded instanceof java.util.Map))return null;
            Object display=((java.util.Map<?,?>)decoded).get("displayUUID");
            return "Center_Display".equals(display)||"Cluster_Display".equals(display)?(String)display:null;
        }catch(RuntimeException ignored){return null;}
    }

    /** Shared by the transport and host tests; one router per authenticated connection. */
    static final class PanelRouter {
        private final java.util.Map<String,UiSync> panels=new java.util.HashMap<String,UiSync>();
        private final File preferences;
        private final UiSync configuration;
        PanelRouter(File preferences,UiSync configuration) {
            this.preferences=preferences;this.configuration=configuration;
        }
        UiSync select(byte[] body) {
            // Missing displayUUID is the historical legacy lane; an explicit
            // unknown or malformed display must not share its handshake state.
            String id;
            try {
                Object decoded=OpackCodec.decode(body);
                if (!(decoded instanceof java.util.Map)) return null;
                java.util.Map<?,?> frame=(java.util.Map<?,?>)decoded;
                if (!frame.containsKey("displayUUID")) id="legacy";
                else {
                    Object display=frame.get("displayUUID");
                    if (!"Center_Display".equals(display) && !"Cluster_Display".equals(display)) return null;
                    id=(String)display;
                }
            } catch (RuntimeException e) { return null; }
            UiSync state=panels.get(id);
            if(state==null){
                state=new UiSync(preferences,true);
                state.wanted=configuration.wanted;
                state.legacyCenterLayout=configuration.legacyCenterLayout;
                state.reopen();
                panels.put(id,state);
            }
            return state;
        }
    }

    static ChaChaPoly snapshotCipher(File dir) {
        byte[] keys = IapChannel.readKeys(dir, KEYS);
        if (keys == null) return null;
        byte[] rd = new byte[32], wr = new byte[32];
        try {
            System.arraycopy(keys, 8, rd, 0, 32);
            System.arraycopy(keys, 40, wr, 0, 32);
            return new ChaChaPoly(rd, wr);
        } finally {
            java.util.Arrays.fill(keys, (byte)0);
            java.util.Arrays.fill(rd, (byte)0);
            java.util.Arrays.fill(wr, (byte)0);
        }
    }

    void serve(Socket sock, ChaChaPoly cipher) {
        // Sequence numbers and reset state belong to this authenticated connection.
        // A reconnect must not reset an older connection's handshake.
        final PanelRouter router=new PanelRouter(new File(dir,"cpc-theme-preferences.properties"),sync);
        out.log("SYNC connection opened");
        int logged = 0, packages = 0;
        java.util.Set<String> seenPanels = new java.util.HashSet<String>();
        try {
            InputStream in = sock.getInputStream();
            VsChannel.Writer w = new VsChannel.Writer(sock, sock.getOutputStream(), cipher);
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
                    out.log("SYNC a frame didn't authenticate: closed");
                    return;
                }
                System.arraycopy(sealed, used, sealed, 0, have - used);
                have -= used;
                byte[] p = plain.toByteArray();
                int at = 0;
                while (p.length - at >= VsChannel.HEADER) {
                    int size = VsChannel.be32(p, at);
                    if (size < VsChannel.HEADER || size > VsChannel.MAX_PACKAGE) {
                        out.log("SYNC implausible package size " + size + ": closed");
                        return;
                    }
                    if (p.length - at < size) break;
                    int kind = VsChannel.be32(p, at + 4);
                    long token = (long) VsChannel.be32(p, at + 20) << 32 | (VsChannel.be32(p, at + 24) & 0xffffffffL);
                    int len = size - VsChannel.HEADER;
                    packages++;
                    if (kind == VsChannel.SYNC) w.send(VsChannel.pack(VsChannel.REPLY, 0, token, new byte[0]));
                    if (kind != VsChannel.REPLY && len > 0) {
                        byte[] body = java.util.Arrays.copyOfRange(p, at + VsChannel.HEADER, at + size);
                        List<byte[]> answers;
                        List<String> picked;
                        UiSync sync=router.select(body);
                        if (sync == null) {
                            if (logged++ < MAX_LOGGED) out.log("[theme-sync] unsupported or malformed panel frame ignored");
                            at += size;
                            continue;
                        }
                        boolean becameReady;
                        boolean resetStarted;
                        synchronized (sync) {
                            boolean wasReady = sync.ready;
                            long previousSession = sync.session;
                            try {
                                answers = sync.handle(body);
                            } catch (IllegalArgumentException e) {
                                answers = java.util.Collections.emptyList();
                                if (logged++ < MAX_LOGGED) out.log("SYNC unreadable message bytes=" + body.length);
                            }
                            becameReady = !wasReady && sync.ready;
                            resetStarted = previousSession != sync.session && sync.awaitingResetComplete;
                            picked = new java.util.ArrayList<String>(sync.choices);
                            sync.choices.clear();
                        }
                        // Report which panels actually negotiate, including their first
                        // message even if the general packet log budget has been reached.
                        try {
                            Object decoded = OpackCodec.decode(body);
                            if (decoded instanceof java.util.Map) {
                                Object display = ((java.util.Map<?,?>)decoded).get("displayUUID");
                                if (("Center_Display".equals(display) || "Cluster_Display".equals(display))
                                        && seenPanels.add(String.valueOf(display)))
                                    out.log("[theme-sync] first message for " + display);
                            }
                        } catch (RuntimeException ignored) {}
                        if (logged++ < MAX_LOGGED) out.log("SYNC phone: bytes=" + body.length);
                        for (byte[] a : answers) {
                            w.send(VsChannel.pack(VsChannel.ASYNC, VsChannel.COMMAND, 0, a));
                            if (logged++ < MAX_LOGGED) out.log("SYNC car: bytes=" + a.length);
                        }
                        String display = panelId(body);
                        String lane = display == null ? "legacy" : display;
                        if (resetStarted) out.log("[theme-sync] panel=" + lane + " reset session=" + sync.session);
                        if (becameReady) out.log("[theme-sync] panel=" + lane + " initialization replies written session="
                                + sync.session + " version=" + sync.version + " replies=" + answers.size());
                        for (String c : picked) out.log("SYNC the phone picked " + c);
                    }
                    at += size;
                }
                plain.reset();
                plain.write(p, at, p.length - at);
            }
            out.log("SYNC connection closed by the phone after " + packages + " package(s)");
        } catch (Throwable t) {
            out.log("SYNC connection ended after " + packages + " package(s): " + t);
        } finally {
            out.log("[theme-sync] negotiated traffic panels=" + seenPanels);
            VsChannel.close(sock);
        }
    }
}
