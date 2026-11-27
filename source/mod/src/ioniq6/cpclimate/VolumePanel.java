package ioniq6.cpclimate;

import android.app.ActivityManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.view.KeyEvent;
import hmgapi.hook.MethodHook;
import hmgapi.hook.Hooks;
import hmgapi.hook.Points;
import hmgapi.hook.ModContext;
import java.util.List;

/** Adapted from the EV6 VolumeKeys experiment. Observe stock key handling;
 * never replace or consume volume or mute events, or synthesize volume changes. */
final class VolumePanel {
    private static boolean installed;
    private static final String PANEL_MD5 = "04fd3c2e9bc7444c2ae812b57a83ac5d";
    private static final String READY = "ioniq6.cpclimate.VOLUME_PANEL_READY";
    private static final String PERMISSION = "android.permission.DEVICE_POWER";
    private static volatile long readyUntil;
    private static boolean readySent, verifiedPanel;
    private static long sentAt;
    private static Handler worker;
    private static int hide = -1;
    private static long lastNotice;
    static void zygote(final ModContext mod) {
        verifiedPanel = PANEL_MD5.equals(ModContext.md5("/system/framework/automotive-service.odex"));
        mod.hook(null, "framework.hkmc_loader.automotive_service", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                if (installed || p.getResult() == null || p.args.length == 0 || !(p.args[0] instanceof Context)) return;
                final Context context = (Context)p.args[0];
                if (android.os.Process.myUid() != 1000 || !"android".equals(context.getPackageName())) return;
                installed = true;
                final HandlerThread thread = new HandlerThread("k5-volume-observer");
                thread.start();
                final Handler handler = new Handler(thread.getLooper());
                installPresentationHooks(mod, context, p.getResult(), handler);
                mod.hook(null, "framework.window_manager_service.intercept_key", new MethodHook() {
                    @Override protected void afterHookedMethod(Param key) {
                        if (!mod.enabled() || !mod.settingBoolean("k5_integration", true)
                                || key.args.length == 0 || !(key.args[0] instanceof KeyEvent)) return;
                        KeyEvent e = (KeyEvent)key.args[0];
                        if (!isVolume(e.getKeyCode()) || e.getAction() != KeyEvent.ACTION_DOWN) return;
                        handler.post(new Runnable() { public void run() {
                            if (foreground(context)) context.sendBroadcast(new Intent(CarAudio.ACTION_CHANGED).setPackage("com.daudio.app.dipo"));
                        }});
                    }
                });
                mod.log("[volume-panel] passive stock-key observer installed");
            }
        });
    }
    // Exact private K5 test only. Low-level SDK hook does not participate in HookPlan;
    // migrate to declared replace points when HMG-API maps these OEM methods.
    private static void installPresentationHooks(final ModContext mod, final Context context,
                                                 Object loader, Handler handler) {
        if (!verifiedPanel) { mod.log("[volume-panel] unknown firmware; presentation hooks withheld"); return; }
        try {
            if (context.getPackageManager().getApplicationInfo("com.daudio.app.dipo", 0).uid != 1000
                    || context.checkCallingOrSelfPermission(PERMISSION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
            ClassLoader cl = (ClassLoader) Points.get(null, "framework.hkmc_loader.class_loader", loader);
            if (cl == null || !Points.has("framework.dipo_manager.current_stat")) return;
            Class<?> panel = Hooks.findClass("com.hkmc.micom.MicomVolumePanel", cl);
            java.lang.reflect.Method legacy = Hooks.findMethodExact(panel, "onShowVolumeChanged", int.class, int.class);
            java.lang.reflect.Method modern = Hooks.findMethodExact(panel, "onShowVolumeChangedNew",
                    int.class, int.class, int.class, int.class, int.class, int.class, int.class);
            if (legacy.getReturnType() != void.class || modern.getReturnType() != void.class) return;
            context.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    long now = android.os.SystemClock.elapsedRealtime();
                    long issued = i.getLongExtra("issued", -1);
                    if (!VolumePanelPolicy.fresh(now, issued)) { readyUntil = 0; return; }
                    readyUntil = i.getBooleanExtra("ready", false) ? issued + VolumePanelPolicy.LEASE_MS : 0;
                }
            }, new IntentFilter(READY), PERMISSION, handler);
            MethodHook presentation = new MethodHook() {
                @Override protected void beforeHookedMethod(Param p) {
                    try {
                        long now = android.os.SystemClock.elapsedRealtime();
                        if (!VolumePanelPolicy.eligible(mod.enabled(), mod.settingBoolean("k5_integration", true),
                                true, readyUntil, now)) return;
                        Object dipo = context.getSystemService("dipo");
                        if (dipo == null) return;
                        Object stat = Points.call(null, "framework.dipo_manager.current_stat", dipo);
                        boolean connected = stat instanceof Number && ((Number)stat).intValue() == 1;
                        if (!VolumePanelPolicy.suppress(mod.enabled(), mod.settingBoolean("k5_integration", true),
                                true, readyUntil,
                                android.os.SystemClock.elapsedRealtime(), connected, foreground(context))) return;
                        p.setResult(null);
                        if (now - lastNotice > 2000) {
                            mod.log("[volume-panel] suppressed " + p.method.getName() + " before presentation");
                            lastNotice = now;
                        }
                    } catch (Throwable ignored) { /* Fail open. */ }
                }
            };
            Hooks.findAndHookMethod(panel, legacy.getName(), int.class, int.class, presentation);
            Hooks.findAndHookMethod(panel, modern.getName(), int.class, int.class, int.class, int.class,
                    int.class, int.class, int.class, presentation);
            mod.log("[volume-panel] exact K5 legacy/new presentation hooks installed");
        } catch (Throwable t) {
            readyUntil = 0;
            mod.log("[volume-panel] presentation setup failed: " + t);
        }
    }

    static synchronized void publishReady(Context c, boolean ready) {
        if (c == null || android.os.Process.myUid() != 1000) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (ready == readySent && (!ready || now - sentAt < 1000)) return;
        try {
            c.sendBroadcast(new Intent(READY).setPackage("android").putExtra("ready", ready)
                    .putExtra("issued", now), PERMISSION);
            readySent = ready; sentAt = now;
        } catch (Throwable ignored) {}
    }
    static boolean isVolume(int key) {
        if (key == KeyEvent.KEYCODE_VOLUME_UP || key == KeyEvent.KEYCODE_VOLUME_DOWN || key == KeyEvent.KEYCODE_VOLUME_MUTE || key == KeyEvent.KEYCODE_MUTE) return true;
        for (String name : new String[]{"KEYCODE_VOLUME_UP_DUMMY","KEYCODE_VOLUME_DOWN_DUMMY"}) {
            try { if (KeyEvent.class.getField(name).getInt(null) == key) return true; }
            catch (Throwable ignored) {}
        }
        return false;
    }
    static boolean foreground(Context c) {
        try {
            ActivityManager am = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> tasks = am == null ? null : am.getRunningTasks(1);
            return tasks != null && !tasks.isEmpty() && tasks.get(0).topActivity != null
                    && "com.daudio.app.dipo".equals(tasks.get(0).topActivity.getPackageName());
        } catch (Throwable t) { return false; }
    }
    static synchronized void onStockKey(final Context c, final CarAudio audio) {
        final ModContext mod = CpClimate.mod;
        if (mod == null || !mod.enabled() || !mod.settingBoolean("k5_integration", true) || !audio.panelReady()) return;
        if (worker == null) {
            hide = CarApi.code(CarAudio.IFACE, "hideVolumeDisplay", "(Z)V");
            if (hide < 0) { mod.log("[volume-panel] hide getter unavailable; stock panel retained"); return; }
            HandlerThread t = new HandlerThread("k5-volume-panel");
            t.start(); worker = new Handler(t.getLooper());
        }
        final long generation = CpClimate.sessionGeneration;
        for (long delay : new long[]{90,250}) worker.postDelayed(new Runnable() { public void run() {
            if (generation != CpClimate.sessionGeneration || !audio.panelReady() || !mod.settingBoolean("k5_integration", true) || !foreground(c)) return;
            Parcel data = Parcel.obtain(), reply = Parcel.obtain();
            try {
                IBinder b = CarClimate.service(CarAudio.SERVICE);
                if (b == null) return;
                data.writeInterfaceToken(CarAudio.IFACE);
                data.writeInt(0);
                if (!b.transact(hide,data,reply,0)) return;
                reply.readException();
                long now = android.os.SystemClock.elapsedRealtime();
                if (now-lastNotice > 2000) { mod.log("[volume-panel] dismissed stock popup after key"); lastNotice=now; }
            } catch (Throwable t) { mod.log("[volume-panel] stock popup retained: "+t.getClass().getSimpleName()); }
            finally { data.recycle(); reply.recycle(); }
        }},delay);
    }
}
