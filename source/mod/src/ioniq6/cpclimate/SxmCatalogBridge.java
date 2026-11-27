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
    private static void diagnostic(ModContext m,String event) {
        m.log(event);
        android.util.Log.i("CPULCrossProcess",event);
    }
    static final String ACTION = "ioniq6.cpclimate.SXM_CATALOG";
    static void install(final ModContext m, final Context context) {
        try {
            if (!"a909ed44ac10909a83af0a1109e12f92".equals(ModContext.md5("/system/app/HKMC_RadioNA.odex"))) return;
            if (context.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                diagnostic(m,"[radio-sxm] catalog bridge withheld: sender permission unavailable");
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
                        ArrayList<Integer> excluded = new ArrayList<Integer>();
                        ArrayList<String> names = new ArrayList<String>();
                        for (int i = 0; i < Array.getLength(records); i++) {
                            Object row = Array.get(records, i);
                            if (!channel.isInstance(row)) continue;
                            Object raw = id.get(row);
                            if (!(raw instanceof Number) || ((Number)raw).intValue() < 0) continue;
                            int key=((Number)raw).intValue();
                            if (!K5Radio.channelFlag(sub.get(row), true) || !K5Radio.channelFlag(skip.get(row), false)) {
                                excluded.add(key);
                                continue;
                            }
                            ids.add(key);
                            Object label = name.get(row);
                            String value = label instanceof String ? (String)label : "";
                            names.add(value.substring(0, Math.min(128, value.length())));
                        }
                        int[] keys = new int[ids.size()];
                        int[] denied = new int[excluded.size()];
                        for(int i=0;i<denied.length;i++) denied[i]=excluded.get(i);
                        String[] empty = new String[ids.size()];
                        for (int i = 0; i < keys.length; i++) { keys[i] = ids.get(i); empty[i] = ""; }
                        context.sendBroadcast(new Intent(ACTION).setPackage(CpClimate.DIPO)
                                .putExtra("received", android.os.SystemClock.elapsedRealtime())
                                .putExtra("ids", keys).putExtra("excluded", denied).putExtra("names", names.toArray(new String[0]))
                                .putExtra("titles", empty).putExtra("artists", empty), "android.permission.DEVICE_POWER");
                        diagnostic(m,"[radio-sxm] forwarded decoded eligible catalog count=" + keys.length + " excluded=" + denied.length);
                    } catch (Throwable t) { diagnostic(m,"[radio-sxm] catalog bridge failed: " + t.getClass().getSimpleName()); }
                }
            });
            diagnostic(m,"[radio-sxm] decoded catalog observer installed");
            // DL322 RadioNA DEX declares this exact callback with a Channel object.
            // Observe the decoded current-channel record, not catalog track snapshots.
            try {
                final Field title=field(channel,"mTitle"), artist=field(channel,"mArtist");
                Hooks.findAndHookMethod(listener,"onCurrentChannelUpdated",channel,new MethodHook(){
                    @Override protected void afterHookedMethod(Param p){
                        try {
                            if(!m.enabled()||!m.settingBoolean("k5_integration",true)||!m.settingBoolean("sxm_schema_test",false))return;
                            Object row=p.args[0];
                            if(!channel.isInstance(row))return;
                            Object raw=id.get(row);
                            if(!(raw instanceof Number)||((Number)raw).intValue()<0)return;
                            context.sendBroadcast(new Intent(ACTION).setPackage(CpClimate.DIPO)
                                .putExtra("received",android.os.SystemClock.elapsedRealtime())
                                .putExtra("liveChannel",((Number)raw).intValue())
                                .putExtra("liveName",bounded(name.get(row)))
                                .putExtra("liveTitle",bounded(title.get(row)))
                                .putExtra("liveArtist",bounded(artist.get(row))),"android.permission.DEVICE_POWER");
                        }catch(Throwable t){diagnostic(m,"[radio-sxm] live metadata bridge failed: "+t.getClass().getSimpleName());}
                    }
                });
                diagnostic(m,"[radio-sxm] current-channel observer installed");
            }catch(Throwable t){diagnostic(m,"[radio-sxm] current-channel observer unavailable: "+t.getClass().getSimpleName());}

            final android.os.HandlerThread worker = new android.os.HandlerThread("cpul-sxm-art");
            worker.start();
            final android.os.Handler handler = new android.os.Handler(worker.getLooper());
            Hooks.findAndHookMethod(listener, "onChannelArtReceived", int.class, int.class, int.class, String.class, new MethodHook() {
                @Override protected void afterHookedMethod(Param p) {
                    if (!m.enabled() || !m.settingBoolean("k5_integration", true) || !m.settingBoolean("sxm_schema_test", false)) return;
                    final int channelId = ((Number)p.args[0]).intValue();
                    final String filename = p.args[3] instanceof String ? (String)p.args[3] : "";
                    final long received = android.os.SystemClock.elapsedRealtime();
                    if (channelId < 0 || filename.isEmpty() || filename.length() > 512) return;
                    handler.post(new Runnable() { public void run() {
                        try {
                            java.io.File root = new java.io.File("/data/data/com.hkmc.media.sxm65h/sms_hb/channelart").getCanonicalFile();
                            java.io.File file = new java.io.File(filename);
                            if (!file.isAbsolute()) file = new java.io.File(root, filename);
                            file = file.getCanonicalFile();
                            if (!root.equals(file.getParentFile()) || !file.isFile() || file.length() > 131072 || file.length() < 8) return;
                            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                            java.io.InputStream in = new java.io.FileInputStream(file);
                            try {
                                byte[] buffer = new byte[4096]; int n;
                                while ((n = in.read(buffer)) != -1) {
                                    if (bytes.size() + n > 131072) return;
                                    bytes.write(buffer, 0, n);
                                }
                            } finally { in.close(); }
                            byte[] image = bytes.toByteArray();
                            if (!K5Radio.supportedArtwork(image) || android.os.SystemClock.elapsedRealtime() - received > 2000) return;
                            context.sendBroadcast(new Intent(ACTION).setPackage(CpClimate.DIPO)
                                    .putExtra("received", received).putExtra("artChannel", channelId)
                                    .putExtra("artwork", image), "android.permission.DEVICE_POWER");
                        } catch (Throwable t) { diagnostic(m,"[radio-sxm] artwork unavailable: " + t.getClass().getSimpleName()); }
                    }});
                }
            });
        } catch (Throwable t) { diagnostic(m,"[radio-sxm] catalog observer unavailable: " + t.getClass().getSimpleName()); }
    }
    private static String bounded(Object value){
        if(!(value instanceof String))return "";
        String s=((String)value).trim();
        return s.substring(0,Math.min(256,s.length()));
    }
    private static Field field(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name); f.setAccessible(true); return f;
    }
}
