package ioniq6.cpclimate;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import hmgapi.hook.ModContext;

/** Stock North American radio handoff. Package confirmed in DL322 260507 report.
 * Never sends generic media keys to CarPlay or guesses OEM tuner transactions. */
final class K5Radio {
    static final String PACKAGE = "com.hkmc.av.app.radiona";
    final Context context;
    final ModContext mod;
    final VsChannel.Log log;
    final Handler main = new Handler(Looper.getMainLooper());
    final Intent launch;
    K5Radio(Context context, ModContext mod, VsChannel.Log log) {
        this.context=context; this.mod=mod; this.log=log;
        Intent found=null;
        if (enabled()) try {
            Intent candidate=context.getPackageManager().getLaunchIntentForPackage(PACKAGE);
            if (candidate!=null && candidate.getComponent()!=null
                    && PACKAGE.equals(candidate.getComponent().getPackageName())
                    && context.getPackageManager().resolveActivity(candidate,0)!=null) found=candidate;
        } catch (Exception e) { log.log("[radio] discovery failed: "+e.getClass().getSimpleName()); }
        launch=found;
        log.log("[radio] stock radio launch available="+(launch!=null)+"; direct tuner unavailable pending OEM interface evidence");
    }
    private volatile String probeStatus = "Ready";
    private volatile boolean fmActive;
    private Object fmTuner, fmAudio;
    private static final String RADIO_IFACE = "com.hkmc.media.IRadioService";

    /** North American OEM FM values are in 10 kHz units. Reject unknown/out-of-band readings. */
    static long fmKhz(int raw) {
        return raw >= 8750 && raw <= 10800 ? raw * 10L : -1L;
    }

    private int readInt(Object proxy, String iface, String method) throws Exception {
        if (proxy == null) return -1;
        java.lang.reflect.Method m = Class.forName(iface).getMethod(method);
        if (m.getReturnType() != int.class) return -1;
        return ((Integer)m.invoke(proxy)).intValue();
    }

    /** Read-only FM state; never change the OEM source merely because CarPlay connects. */
    java.util.Map<Long,Object> refreshFm() {
        java.util.Map<Long,Object> values = new java.util.LinkedHashMap<Long,Object>();
        values.put(4115L, true);
        values.put(4116L, "");
        values.put(4354L, "");
        values.put(4355L, "");
        values.put(4359L, 0L);
        values.put(4117L, ClimateMap.BLANK);
        if (!enabled() || !CpClimate.sessionActive) { fmActive = false; return values; }
        try {
            if (!"7c99638e39c369c45fab9c485b103cf0".equals(ModContext.md5("/system/framework/automotive.odex"))) { fmActive = false; return values; }
            if (fmTuner == null) fmTuner = service(serviceName("RADIO_SERVICE"), RADIO_IFACE);
            if (fmAudio == null) fmAudio = service(CarAudio.SERVICE, CarAudio.IFACE);
            long khz = fmKhz(readInt(fmTuner, RADIO_IFACE, "getLastFMFrequency"));
            int mode = readInt(fmAudio, CarAudio.IFACE, "getAudioMode");
            int stream = readInt(fmAudio, CarAudio.IFACE, "getCurStreamType");
            // RC4 observed FM mode 2 / stream 38, AM mode 1 / stream 39.
            fmActive = khz > 0 && mode == 2 && stream == 38;
            if (fmActive) {
                values.put(4117L, khz);
                values.put(4354L, "FM");
                values.put(4355L, String.format(java.util.Locale.US, "%.1f MHz", khz / 1000.0));
                values.put(4359L, 1L);
            }
            // Advertise an available FM source even before it is selected.
            // The command path still checks active source ownership before tuning.
            values.put(4115L, !(fmTuner != null && fmAudio != null && khz > 0));
            values.put(4116L, fmActive ? Long.toString(khz) : "");
        } catch (Throwable t) { fmActive = false; fmTuner = null; fmAudio = null; }
        return values;
    }

    /** Queue a seek only for the already-active FM tuner; never route a radio seek to the phone.
     * Recheck source ownership on the worker immediately before the OEM command. */
    private final java.util.concurrent.ExecutorService fmCommands =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    boolean seekFm(final boolean up) {
        if (!enabled() || !CpClimate.sessionActive || !fmActive) return false;
        final long generation = CpClimate.sessionGeneration;
        fmCommands.execute(new Runnable() {
            public void run() {
                if (!probeActive(generation)) return;
                try {
                    Object audio = service(CarAudio.SERVICE, CarAudio.IFACE);
                    if (readInt(audio, CarAudio.IFACE, "getAudioMode") != 2
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 38) return;
                    Object tuner = service(serviceName("RADIO_SERVICE"), RADIO_IFACE);
                    if (tuner == null || !probeActive(generation)) return;
                    java.lang.reflect.Method method = Class.forName(RADIO_IFACE).getMethod(up ? "seekUp" : "seekDown");
                    if (method.getReturnType() != void.class) return;
                    method.invoke(tuner);
                    log.log("[radio] FM seek " + (up ? "up" : "down"));
                } catch (Throwable t) { log.log("[radio] FM seek failed: " + t.getClass().getSimpleName()); }
            }
        });
        return true;
    }

    /** US FM: odd 200 kHz channels, OEM frequency in 10 kHz units.
     * RadioNA WrapperUtil identifies the boolean as isFMStep50KHz; the
     * two-argument MicomRadioManager overload supplies PI/PTy zero. */
    boolean tuneFm(final long khz) {
        if (khz < 87900 || khz > 107900 || (khz - 87900) % 200 != 0
                || !enabled() || !CpClimate.sessionActive || !fmActive) return false;
        final long generation = CpClimate.sessionGeneration;
        fmCommands.execute(new Runnable() {
            public void run() {
                if (!probeActive(generation)) return;
                try {
                    Object audio = service(CarAudio.SERVICE, CarAudio.IFACE);
                    if (readInt(audio, CarAudio.IFACE, "getAudioMode") != 2
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 38) return;
                    Object tuner = service(serviceName("RADIO_SERVICE"), RADIO_IFACE);
                    if (tuner == null || !probeActive(generation)) return;
                    java.lang.reflect.Method method = Class.forName(RADIO_IFACE).getMethod(
                            "directTune", int.class, boolean.class, int.class, int.class);
                    if (method.getReturnType() != boolean.class) return;
                    Object result = method.invoke(tuner, (int)(khz / 10), false, 0, 0);
                    log.log("[radio] FM tune " + khz + " kHz accepted=" + result);
                } catch (Throwable t) { log.log("[radio] FM tune failed: " + t.getClass().getSimpleName()); }
            }
        });
        return true;
    }

    /** Explicit FM selection via the stock framework wrapper. Never invoked on connect.
     * DL322 260507 automotive.odex: ModeManager(Context), changeMode(Integer):boolean,
     * ModeConstants.ModeType FM1=2. Foreground behavior requires parked validation. */
    // A tune with explicit FM source may activate FM first. Only the latest
    // pending request is retained; it expires and cannot cross sessions.
    private volatile long tuneRequest;
    boolean selectAndTuneFm(final long khz) {
        if (khz < 87900 || khz > 107900 || (khz - 87900) % 200 != 0
                || !enabled() || !CpClimate.sessionActive) return false;
        final long generation = CpClimate.sessionGeneration;
        final long request = ++tuneRequest;
        if (fmActive) return tuneFm(khz);
        if (!selectFm()) return false;
        final long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
        main.postDelayed(new Runnable() {
            public void run() {
                if (!probeActive(generation) || request != tuneRequest) return;
                if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    log.log("[radio] FM activation timed out; pending tune discarded");
                    return;
                }
                if (fmActive) { tuneFm(khz); return; }
                main.postDelayed(this, 250);
            }
        }, 250);
        return true;
    }

    boolean selectFm() {
        if (!enabled() || !CpClimate.sessionActive) return false;
        final long generation = CpClimate.sessionGeneration;
        fmCommands.execute(new Runnable() {
            public void run() {
                if (!probeActive(generation)) return;
                try {
                    if (!"7c99638e39c369c45fab9c485b103cf0".equals(
                            ModContext.md5("/system/framework/automotive.odex"))) return;
                    // Construct the framework wrapper on a Looper thread, as its
                    // implementation can create Handler-backed listener transports.
                    main.post(new Runnable() {
                        public void run() {
                            if (!probeActive(generation)) return;
                            try {
                                Class<?> type = Class.forName("com.hkmc.mode.ModeManager");
                                java.lang.reflect.Method change = type.getMethod("changeMode", Integer.class);
                                if (change.getReturnType() != boolean.class) return;
                                Object manager = type.getConstructor(Context.class).newInstance(context);
                                if (!probeActive(generation)) return;
                                Object accepted = change.invoke(manager, Integer.valueOf(2));
                                log.log("[radio] OEM FM1 mode request accepted=" + accepted);
                            } catch (Throwable t) {
                                log.log("[radio] FM selection failed: " + t.getClass().getSimpleName());
                            }
                        }
                    });
                } catch (Throwable t) {
                    log.log("[radio] FM selection unavailable: " + t.getClass().getSimpleName());
                }
            }
        });
        return true;
    }

    boolean fmSelected() { return fmActive; }

    String diagnosticStatus() { return probeStatus; }
    private boolean probing;
    /** Explicit, bounded diagnostic run. Poll only known read-only primitive getters.
     * No manager construction, listener registration, tuning or source changes. */
    private synchronized void startProbe() {
        if (probing || !enabled()) return;
        probing = true;
        probeStatus = "Collecting (2 min)";
        final long generation = CpClimate.sessionGeneration;
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try { probeTuner(generation); }
                finally { synchronized (K5Radio.this) { probing = false; probeStatus = "Finished — see log"; } }
            }
        }, "k5-radio-probe");
        worker.setDaemon(true);
        worker.start();
    }
    private boolean probeActive(long generation) {
        return enabled() && CpClimate.sessionActive && generation == CpClimate.sessionGeneration;
    }
    private Object service(String name, String ifaceName) throws Exception {
        android.os.IBinder binder = CarClimate.service(name);
        if (binder == null || !ifaceName.equals(binder.getInterfaceDescriptor())) return null;
        Class<?> iface = Class.forName(ifaceName);
        Object proxy = Class.forName(ifaceName + "$Stub")
                .getMethod("asInterface", android.os.IBinder.class).invoke(null, binder);
        return iface.isInstance(proxy) ? proxy : null;
    }
    private String serviceName(String fieldName) throws Exception {
        java.lang.reflect.Field field = Class.forName("com.hkmc.Service").getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(null);
        if (!(value instanceof String) || ((String)value).length() == 0)
            throw new IllegalStateException("invalid service name");
        return (String)value;
    }
    private void collect(String label, Object proxy, String ifaceName, String[] getters,
                         java.util.Map<String,String> previous) {
        if (proxy == null) return;
        for (String getter : getters) {
            String key = label + "." + getter;
            String value;
            try {
                java.lang.reflect.Method method = Class.forName(ifaceName).getMethod(getter);
                Class<?> type = method.getReturnType();
                if (type != int.class && type != boolean.class) continue;
                value = String.valueOf(method.invoke(proxy));
            } catch (Throwable t) { value = "unavailable:" + t.getClass().getSimpleName(); }
            if (!value.equals(previous.put(key, value))) log.log("[radio-probe] " + key + " raw=" + value);
        }
    }
    private void probeTuner(long generation) {
        if (!probeActive(generation)) return;
        try {
            if (!"7c99638e39c369c45fab9c485b103cf0".equals(ModContext.md5("/system/framework/automotive.odex"))) {
                log.log("[radio-probe] unknown framework; withheld");
                return;
            }
            String radio = "com.hkmc.media.IRadioService";
            String sxm = "com.hkmc.media.sxm65h.ISXM65HService";
            String modeIface = "com.hkmc.mode.IModeManager";
            Object tuner = null, audio = null, satellite = null, mode = null;
            java.util.Map<String,String> previous = new java.util.HashMap<String,String>();
            long deadline = android.os.SystemClock.elapsedRealtime() + 120000L;
            log.log("[radio-probe] start: 120 seconds, change stock bands/stations manually");
            while (probeActive(generation) && android.os.SystemClock.elapsedRealtime() < deadline) {
                try { if (tuner == null) tuner = service(serviceName("RADIO_SERVICE"), radio); }
                catch (Throwable ignored) {}
                try { if (audio == null) audio = service(CarAudio.SERVICE, CarAudio.IFACE); }
                catch (Throwable ignored) {}
                try { if (satellite == null) satellite = service(serviceName("SXM65H_SERVICE"), sxm); }
                catch (Throwable ignored) {}
                try { if (mode == null) mode = service("com.hkmc.micom.mode", modeIface); }
                catch (Throwable ignored) {}
                String availability = "radio=" + (tuner != null) + " audio=" + (audio != null) + " sxm=" + (satellite != null) + " mode=" + (mode != null);
                if (!availability.equals(previous.put("availability", availability))) log.log("[radio-probe] " + availability);
                collect("radio", tuner, radio, new String[]{"getLastFMFrequency", "getLastFM2Frequency",
                        "getLastAMFrequency", "getTuningValue", "getLastPresetNumber"}, previous);
                collect("audio", audio, CarAudio.IFACE, new String[]{"getAudioMode", "getCurStreamType"}, previous);
                collect("mode", mode, modeIface, new String[]{"getCurrentMode", "getCurrentReadyMode",
                        "getLastRadioMode", "isAvailableCurrentMode", "isChangeModeScheduled"}, previous);
                collect("sxm", satellite, sxm, new String[]{"getLastTunedChannelID"}, previous);
                Thread.sleep(1500);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (Throwable t) { log.log("[radio-probe] discovery unavailable: " + t.getClass().getSimpleName()); }
        finally { log.log("[radio-probe] finished"); }
    }
    boolean enabled() { return mod.enabled() && mod.settingBoolean("k5_integration", true); }
    boolean available() { return launch!=null; }
    void describe(VehiclePage page) {
        page.sectionName("radio","Radio");
        page.tile("radio","radio");
        page.button("radio","radio.open","Open Kia radio","radio");
        page.line("radio","radio.status","Diagnostics status","radio");
        page.button("radio","radio.diagnostics","Collect radio diagnostics (2 min)","radio");
    }
    void pressed(String id) {
        if ("radio.diagnostics".equals(id)) { if (CpClimate.sessionActive) startProbe(); return; }
        if (!"radio.open".equals(id) || !available() || !enabled() || !CpClimate.sessionActive) return;
        final long generation=CpClimate.sessionGeneration;
        main.post(new Runnable() { public void run() {
            if (!enabled() || !CpClimate.sessionActive || generation!=CpClimate.sessionGeneration) return;
            try {
                context.startActivity(new Intent(launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                log.log("[radio] stock radio opened by explicit request");
            } catch (Exception e) { log.log("[radio] launch failed: "+e.getClass().getSimpleName()); }
        }});
    }
}
