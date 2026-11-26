package ioniq6.cpclimate;

import android.content.Context;
import android.content.Intent;
import hmgapi.hook.Hooks;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;

/** Passive observation of RadioNA's already decoded OEM catalog. Never owns its listener. */
final class SxmCatalogBridge {
    static final String ACTION = "ioniq6.cpclimate.SXM_CATALOG";
    static void install(final ModContext m, final Context context) {
        try {
            if (!"a909ed44ac10909a83af0a1109e12f92".equals(ModContext.md5("/system/app/HKMC_RadioNA.odex"))) return;
            if (context.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                m.log("[radio-sxm] catalog bridge withheld: sender permission unavailable");
                return;
            }
            ClassLoader cl = context.getClassLoader();
            Class<?> listener = Hooks.findClass("com.hkmc.av.app.radiona.SXMRadioAppService$SXM65HNaListener", cl);
            final Class<?> channel = Hooks.findClass("com.hkmc.media.sxm65h.Channel", cl);
            final Field id = field(channel, "mChannelID"), name = field(channel, "mChannelLongName");
            final Field sub = field(channel, "mIsSubscribed"), skip = field(channel, "mIsSkipped");
            Hooks.findAndHookMethod(listener, "onAllAvailableChannelsReceived", Array.newInstance(channel, 0).getClass(), int.class, new MethodHook() {
                @Override protected void afterHookedMethod(Param p) {
                    try {
                        if (!m.enabled() || !m.settingBoolean("k5_integration", true) || !m.settingBoolean("sxm_schema_test", false)) return;
                        Object records = p.args[0];
                        if (records == null || Array.getLength(records) > 512) return;
                        ArrayList<Integer> ids = new ArrayList<Integer>();
                        ArrayList<String> names = new ArrayList<String>();
                        for (int i = 0; i < Array.getLength(records); i++) {
                            Object row = Array.get(records, i);
                            if (!channel.isInstance(row) || !K5Radio.channelFlag(sub.get(row), true) || !K5Radio.channelFlag(skip.get(row), false)) continue;
                            Object raw = id.get(row);
                            if (!(raw instanceof Number) || ((Number)raw).intValue() < 0) continue;
                            ids.add(((Number)raw).intValue());
                            Object label = name.get(row);
                            String value = label instanceof String ? (String)label : "";
                            names.add(value.substring(0, Math.min(128, value.length())));
                        }
                        int[] keys = new int[ids.size()];
                        String[] empty = new String[ids.size()];
                        for (int i = 0; i < keys.length; i++) { keys[i] = ids.get(i); empty[i] = ""; }
                        context.sendBroadcast(new Intent(ACTION).setPackage(CpClimate.DIPO)
                                .putExtra("received", android.os.SystemClock.elapsedRealtime())
                                .putExtra("ids", keys).putExtra("names", names.toArray(new String[0]))
                                .putExtra("titles", empty).putExtra("artists", empty), "android.permission.DEVICE_POWER");
                        m.log("[radio-sxm] forwarded decoded eligible catalog count=" + keys.length);
                    } catch (Throwable t) { m.log("[radio-sxm] catalog bridge failed: " + t.getClass().getSimpleName()); }
                }
            });
            m.log("[radio-sxm] decoded catalog observer installed");
        } catch (Throwable t) { m.log("[radio-sxm] catalog observer unavailable: " + t.getClass().getSimpleName()); }
    }
    private static Field field(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name); f.setAccessible(true); return f;
    }
}
