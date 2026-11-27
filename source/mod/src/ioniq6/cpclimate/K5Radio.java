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
    private boolean probing;
    /** Explicit, bounded diagnostic run. Poll only known read-only primitive getters.
     * No manager construction, listener registration, tuning or source changes. */
    private synchronized void startProbe() {
        if (probing || !enabled()) return;
        probing = true;
        final long generation = CpClimate.sessionGeneration;
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try { probeTuner(generation); }
                finally { synchronized (K5Radio.this) { probing = false; } }
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
            Object tuner = null, audio = null, satellite = null;
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
                String availability = "radio=" + (tuner != null) + " audio=" + (audio != null) + " sxm=" + (satellite != null);
                if (!availability.equals(previous.put("availability", availability))) log.log("[radio-probe] " + availability);
                collect("radio", tuner, radio, new String[]{"getLastFMFrequency", "getLastFM2Frequency",
                        "getLastAMFrequency", "getTuningValue", "getLastPresetNumber"}, previous);
                collect("audio", audio, CarAudio.IFACE, new String[]{"getAudioMode", "getCurStreamType"}, previous);
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
