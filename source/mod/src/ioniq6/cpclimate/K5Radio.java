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
    boolean enabled() { return mod.enabled() && mod.settingBoolean("k5_integration", true); }
    boolean available() { return launch!=null; }
    void describe(VehiclePage page) {
        page.sectionName("radio","Radio");
        page.tile("radio","radio");
        page.button("radio","radio.open","Open Kia radio","radio");
    }
    void pressed(String id) {
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
