package ioniq6.cpclimate;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.view.KeyEvent;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.util.List;

/** Adapted from the EV6 VolumeKeys experiment. Observe stock key handling;
 * never replace or consume volume or mute events, or synthesize volume changes. */
final class VolumePanel {
    private static boolean installed;
    private static Handler worker;
    private static int hide = -1;
    private static long lastNotice;
    static void zygote(final ModContext mod) {
        mod.hook(null, "framework.hkmc_loader.automotive_service", new MethodHook() {
            @Override protected void afterHookedMethod(Param p) {
                if (installed || p.getResult() == null || p.args.length == 0 || !(p.args[0] instanceof Context)) return;
                installed = true;
                final Context context = (Context)p.args[0];
                final HandlerThread thread = new HandlerThread("k5-volume-observer");
                thread.start();
                final Handler handler = new Handler(thread.getLooper());
                mod.hook(null, "framework.window_manager_service.intercept_key", new MethodHook() {
                    @Override protected void afterHookedMethod(Param key) {
                        if (!mod.enabled() || !mod.settingBoolean("volume_panel", true) || !mod.settingBoolean("volume_link", true)
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
        if (mod == null || !mod.enabled() || !mod.settingBoolean("volume_panel", true) || !audio.panelReady()) return;
        if (worker == null) {
            hide = CarApi.code(CarAudio.IFACE, "hideVolumeDisplay", "(Z)V");
            if (hide < 0) { mod.log("[volume-panel] hide getter unavailable; stock panel retained"); return; }
            HandlerThread t = new HandlerThread("k5-volume-panel");
            t.start(); worker = new Handler(t.getLooper());
        }
        final long generation = CpClimate.sessionGeneration;
        for (long delay : new long[]{90,250}) worker.postDelayed(new Runnable() { public void run() {
            if (generation != CpClimate.sessionGeneration || !audio.panelReady() || !mod.settingBoolean("volume_panel", true) || !foreground(c)) return;
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
