package ioniq6.cpclimate;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import hmgapi.hook.ModContext;
import java.util.Map;

/** Optional threshold-only client; Comfort owns all validation and persistence. */
final class ComfortThresholds {
    static final String AUTH = "dev.lemoyne.comfort.thresholds";
    static final Uri URI = Uri.parse("content://" + AUTH);
    static final String[] IDS = {"heat_high","heat_med","heat_low","vent_low","vent_med","vent_high"};
    static final String[] LABELS = {"High heating","Medium heating","Low heating","Low ventilation","Medium ventilation","High ventilation"};
    final Context context;
    final ModContext mod;
    final VsChannel.Log log;
    final boolean installed;
    private Bundle latest;
    private boolean failed;
    ComfortThresholds(Context context, ModContext mod, VsChannel.Log log) {
        this.context = context; this.mod = mod; this.log = log;
        android.content.pm.ProviderInfo p = context.getPackageManager().resolveContentProvider(AUTH, 0);
        installed = enabled() && p != null && "dev.lemoyne.comfort".equals(p.packageName);
        log.log("[comfort] compatible provider declared=" + installed);
    }
    boolean enabled() { return mod.enabled() && mod.settingBoolean("k5_integration", true); }
    boolean present() { return installed; }
    void describe(VehiclePage page) {
        page.sectionName("comfort_winter", "Comfort · Winter heating");
        page.tile("comfort_winter", "flame");
        page.sectionName("comfort_summer", "Comfort · Summer ventilation");
        page.tile("comfort_summer", "snowflake");
        for (int i=0;i<IDS.length;i++) {
            String id = "comfort." + IDS[i];
            String section = i < 3 ? "comfort_winter" : "comfort_summer";
            page.line(section, id, LABELS[i] + (i<3 ? " (≤)" : " (≥)"), i<3 ? "flame" : "snowflake");
            page.button(section, id+".minus", LABELS[i]+" −1°", "minus.circle");
            page.button(section, id+".plus", LABELS[i]+" +1°", "plus.circle");
        }
    }
    Bundle read(String operation, String id, Bundle args) {
        Bundle b = context.getContentResolver().call(URI, operation, id, args);
        if (b == null || b.getInt("version", -1) != 1 || !b.containsKey("metric")) throw new IllegalStateException("incompatible Comfort bridge");
        for (String key: IDS) if (!b.containsKey(key)) throw new IllegalStateException("missing threshold");
        return b;
    }
    void refresh(Map<String,Object> fresh) {
        try {
            latest = read("get", null, null);
            if (failed) log.log("[comfort] threshold bridge recovered");
            failed = false;
        } catch (Exception e) {
            latest = null;
            if (!failed) log.log("[comfort] threshold bridge unavailable: " + e.getClass().getSimpleName());
            failed = true;
        }
        for (String id: IDS) fresh.put("comfort."+id, latest == null ? VehiclePage.NONE : latest.getInt(id)+(latest.getBoolean("metric") ? " °C" : " °F"));
    }
    void adjust(String command) {
        if (!enabled() || !CpClimate.sessionActive || latest == null) return;
        for (String id: IDS) {
            int delta = command.equals("comfort."+id+".plus") ? 1 : command.equals("comfort."+id+".minus") ? -1 : 0;
            if (delta == 0) continue;
            try {
                Bundle args = new Bundle();
                args.putInt("delta", delta);
                args.putBoolean("metric", latest.getBoolean("metric"));
                latest = read("adjust", id, args);
                log.log("[comfort] confirmed " + id + "=" + latest.getInt(id));
            } catch (Exception e) { latest=null; log.log("[comfort] adjustment refused: "+e.getClass().getSimpleName()); }
            return;
        }
    }
}
