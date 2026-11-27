package ioniq6.cpclimate;

import android.os.IBinder;
import android.os.Parcel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The car's volume and CarPlay's volume as one thing, both ways (setting volume_link).
 *
 * CarPlay's Volume page (the audio settings accessory in LIVI's description) has a level 0-100 for media,
 * navigation, phone and Siri, and a mute switch. The head unit keeps a level 0-45 per sound stream in
 * MicomAudioService (service com.hkmc.micom.audiosetup); its volume knob and the steering wheel buttons change
 * the stream that is playing (DiPOMUSIC, 14, while CarPlay plays music). Stock DiPO tells the phone none of it.
 *
 * - Car to phone: the four levels are read five times a second while CarPlay is connected and told to the phone.
 *   Read, not listened for: the service's listener has 79 callbacks answered inside system_server, and a
 *   listener answered wrongly has boot-looped this car before (see CarClimate.Body).
 * - Phone to car: a level the phone writes becomes setVolume(stream, level, 0) (tested from adb on 2026-11-10:
 *   stream 14, 8 to 9 and back; flag 0 shows no volume panel). Only after this session has shown the phone the
 *   car's own level: the description's first value is 50, which would be level 22.
 * - Mute: the phone's switch is muteAV()/unMuteAV() (the micom's AV mute). The car's own mute (getUserMuteState)
 *   shows as on too.
 * Every call is found by name and descriptor; a firmware without one leaves that part out.
 */
final class CarAudio extends Thread {
    static final String SERVICE = "com.hkmc.micom.audiosetup", IFACE = "com.hkmc.media.IMicomAudioService";
    static final long PERIOD_MS = 200;
    /** the head unit's volume steps (MicomAudioService logs "MaxVolume : 45") */
    static final int MAX_LEVEL = 45;
    /** CarPlay's volume value, then the car's stream: media DiPOMUSIC, navigation DiPOALT, phone DiPOCALL, Siri DiPOSIRI */
    static final long[][] LINKS = { { VehicleState.VOLUME, 14 }, { 2311, 13 }, { 2314, 11 }, { 2321, 12 } };

    static volatile CarAudio instance;
    private android.content.Context panelContext;
    /** sent to DiPO by VolumeKeys (system_server) right after a volume or mute key: read the levels now */
    static final String ACTION_CHANGED = "ioniq6.cpclimate.VOLUME_CHANGED";
    private final Object wake = new Object();
    private boolean woken;

    /** reads the car's levels now instead of at the next turn (the volume keys were used) */
    void readNow() {
        synchronized (wake) {
            woken = true;
            wake.notifyAll();
        }
    }

    /** listens for VolumeKeys' word that a key changed the volume, so CarPlay's slider moves at once */
    void listen(android.content.Context context) {
        panelContext = context;
        if (context == null) return;
        try {
            context.registerReceiver(new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context c, android.content.Intent i) {
                    readNow();
                    VolumePanel.onStockKey(c, CarAudio.this);
                }
            }, new android.content.IntentFilter(ACTION_CHANGED));
        } catch (Throwable t) {
            out.log("[audio] no word from the volume keys (the levels are still read " + (1000 / PERIOD_MS) + " times a second): " + t);
        }
    }

    final int txGet = CarApi.code(IFACE, "getVolume", "(II)I"), txSet = CarApi.code(IFACE, "setVolume", "(III)V");
    final int txMute = CarApi.code(IFACE, "muteAV", "()V"), txUnmute = CarApi.code(IFACE, "unMuteAV", "()V");
    final int txUserMute = CarApi.code(IFACE, "getUserMuteState", "()Z");
    final int txCurrent = CarApi.code(IFACE, "getCurStreamType", "()I");
    final int txMode = CarApi.code(IFACE, "getAudioMode", "()I");
    /**
     * The stream CarPlay's first (media) level stands for: the one the car's knob moves now, if it has a level
     * (a call, Siri, the radio), else CarPlay's music. On the car, 2026-11-10: with CarPlay silent the current
     * stream was 33, which has no level (getVolume -1), and the knob's level is not DiPOMUSIC's.
     */
    private volatile int mainStream = 14;

    // Only route to native sources verified on DL322; never follow transient
    // navigation, call, or alert streams for the main media slider.
    private int mediaStream() throws Exception {
        if (txCurrent < 0 || txMode < 0) return 14;
        int mode = call(txMode), stream = call(txCurrent);
        if (mode == 2 && stream == 38) return 38;
        if (mode == 4 && stream == 41) return 41;
        return 14;
    }

    private final VehicleState[] state;
    private final VsChannel[] lanes;
    private final VsChannel.Log out;
    /** the session that has been shown the car's levels; the phone's writes count only for it */
    private volatile VehicleState shown;
    private volatile boolean muted;
    private final LinkedBlockingQueue<long[]> sends = new LinkedBlockingQueue<long[]>(64);
    private final hmgapi.hook.ModContext mod;
    private final java.util.Set<Long> observed = new java.util.HashSet<Long>();
    private volatile long observedGeneration = -1;
    private final int[] wanted = new int[LINKS.length];
    private int failures;

    CarAudio(VehicleState[] state, VsChannel[] lanes, VsChannel.Log out, hmgapi.hook.ModContext mod) {
        super("cp-climate-audio");
        this.mod = mod;
        this.state = state;
        this.lanes = lanes;
        this.out = out;
        setDaemon(true);
    }

    /** a level 0-45 as CarPlay's 0-100 */
    static long percent(int level) {
        return Math.round(Math.max(0, Math.min(MAX_LEVEL, level)) * 100.0 / MAX_LEVEL);
    }

    /** CarPlay's 0-100 as a level 0-45 */
    static int level(long percent) {
        return (int) Math.round(Math.max(0, Math.min(100, percent)) * MAX_LEVEL / 100.0);
    }

    /**
     * What the phone wrote, from VehicleState's thread: volume levels and mute are queued for the car. True if
     * any of the values was one of this class's (CarClimate then doesn't report it as unknown).
     */
    boolean active() { return mod.enabled() && mod.settingBoolean("k5_integration", true) && CpClimate.sessionActive; }
    boolean phoneWrote(Map<Long, Object> written) {
        boolean mine = false;
        for (int i = 0; i < LINKS.length; i++) {
            Object v = written.get(LINKS[i][0]);
            if (v == null) continue;
            mine = true;
            Long pct = OpackCodec.asLong(v);
            if (pct == null || txSet < 0) continue;
            if (!active() || shown == null || shown != state[0] || observedGeneration != CpClimate.sessionGeneration || !hasObserved(LINKS[i][0])) {
                out.log("[audio] volume from the phone ignored: the car's level hasn't been shown to it yet");
                continue;
            }
            int level = level(pct);
            synchronized (wanted) {
                wanted[i] = level;
            }
            if (!sends.offer(new long[] { i, level, CpClimate.sessionGeneration, i == 0 ? mainStream : LINKS[i][1] })) out.log("[audio] queue full; level dropped");
        }
        Object m = written.get(VehicleState.MUTE);
        if (m instanceof Boolean) {
            mine = true;
            if (active() && shown != null && shown == state[0] && observedGeneration == CpClimate.sessionGeneration && hasObserved(VehicleState.MUTE)) {
                if (!sends.offer(new long[] { -1, (Boolean) m ? 1 : 0, CpClimate.sessionGeneration, -1 })) out.log("[audio] queue full; mute dropped");
            }
        }
        return mine;
    }

    boolean panelReady() { return active() && shown != null && shown == state[0] && observedGeneration == CpClimate.sessionGeneration && hasObserved(VehicleState.VOLUME); }

    private synchronized boolean hasObserved(long iid) { return observed.contains(iid); }

    private void startSender() {
        Thread t = new Thread("cp-climate-audio-send") {
            @Override
            public void run() {
                while (true) {
                    long[] s;
                    try {
                        s = sends.take();
                    } catch (InterruptedException e) {
                        return;
                    }
                    try {
                        if (!active() || s[2] != CpClimate.sessionGeneration || shown == null || shown != state[0]) continue;
                        if (s[0] < 0) {
                            call(s[1] == 1 ? txMute : txUnmute);
                            muted = s[1] == 1;
                            out.log("[audio] " + (muted ? "muted" : "un-muted") + " for the phone");
                            continue;
                        }
                        // a slider being dragged writes many levels: only the newest goes to the car
                        synchronized (wanted) {
                            if (wanted[(int) s[0]] != s[1]) continue;
                        }
                        int stream = (int) s[3];
                        if (s[0] == 0 && stream != mediaStream()) {
                            out.log("[audio] media source changed; stale volume write discarded");
                            continue;
                        }
                        call(txSet, stream, (int) s[1], 0);
                        out.log("[audio] stream " + stream + " set to " + s[1] + " for the phone");
                    } catch (Throwable t) {
                        out.log("[audio] not sent: " + t);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        if (txGet < 0) {
            out.log("[audio] volume link left out: this firmware has no getVolume(int, int)");
            return;
        }
        out.log("[audio] volume link: calls " + txGet + "," + txSet + "," + txMute + "," + txUnmute + "," + txUserMute
                + (txGet == 8 && txSet == 7 && txMute == 106 && txUnmute == 107 && txUserMute == 73 ? " (as the Ioniq 6's)" : " (not the Ioniq 6's)"));
        startSender();
        String last = null;
        while (true) {
            try {
                synchronized (wake) {
                    if (!woken) wake.wait(PERIOD_MS);
                    woken = false;
                }
                VehicleState vs = state[0];
                if (!active() || vs == null || !(lanes[0].isOpen() || lanes[1].isOpen())) { shown = null; observedGeneration = -1; synchronized (this) { observed.clear(); } sends.clear(); VolumePanel.publishReady(panelContext, false); continue; }
                long generation = CpClimate.sessionGeneration;
                Map<Long, Object> fresh = new LinkedHashMap<Long, Object>();
                StringBuilder seen = new StringBuilder();
                int main = mediaStream();
                seen.append("main=").append(main).append(' ');
                for (long[] link : LINKS) {
                    int level = call(txGet, link[0] == VehicleState.VOLUME ? main : (int) link[1], 0);
                    if (level < 0 || level > MAX_LEVEL) continue;   // a stream this car doesn't have, or another scale
                    fresh.put(link[0], percent(level));
                    seen.append(link[1]).append('=').append(level).append(' ');
                }
                if (txUserMute >= 0) { boolean mute = call(txUserMute) != 0; fresh.put(VehicleState.MUTE, mute); seen.append("mute=").append(mute); }
                if (!seen.toString().equals(last)) {
                    last = seen.toString();
                    out.log("[audio] " + last);
                }
                if (!active() || generation != CpClimate.sessionGeneration || vs != state[0]) continue;
                lanes[1].deliver(vs.fromCar(fresh));
                synchronized (this) { observed.clear(); observed.addAll(fresh.keySet()); }
                mainStream = main;
                observedGeneration = generation;
                shown = vs;
                VolumePanel.publishReady(panelContext, panelReady() && mod.settingBoolean("suppress_stock_volume", true));
                failures = 0;
            } catch (InterruptedException e) {
                return;
            } catch (Throwable t) {
                if (failures++ < 5) out.log("[audio] read failed: " + t);
            }
        }
    }

    /** a call taking ints and answering an int, a boolean or nothing (0) */
    private int call(int code, int... args) throws Exception {
        if (code < 0) throw new IllegalStateException("this firmware has no such call");
        IBinder b = CarClimate.service(SERVICE);
        if (b == null) throw new IllegalStateException("no " + SERVICE);
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(IFACE);
            for (int a : args) data.writeInt(a);
            if (!b.transact(code, data, reply, 0)) throw new IllegalStateException("audio transaction rejected");
            reply.readException();
            return reply.dataAvail() >= 4 ? reply.readInt() : 0;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
