package ioniq6.cpclimate;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Handler;
import android.os.Looper;
import hmgapi.hook.ModContext;
import java.util.LinkedHashMap;
import java.util.Map;

/** K5-only consumer of the EV6 fork's VehiclePage framework. Read-only vehicle calls. */
final class K5Vehicle extends Thread implements VehiclePage.Listener {
    private static final String HSCAN = "com.hkmc.telematics.hscan.tm_interface.ITM_HSCANService";
    private static final String SERVICE = "tm_hscan_service";
    final VehiclePage page = new VehiclePage();
    final Context context;
    final ModContext mod;
    final VehicleState[] state;
    final VsChannel[] lanes;
    final VsChannel.Log log;
    final Map<String, Intent> shortcuts = new LinkedHashMap<String, Intent>();
    final Handler main = new Handler(Looper.getMainLooper());
    int odometer = -1, range = -1, unit = -1;
    String last = "";
    final ComfortThresholds comfort;
    final K5Radio radio;
    final java.util.concurrent.ArrayBlockingQueue<String> adjustments = new java.util.concurrent.ArrayBlockingQueue<String>(16);

    K5Vehicle(Context context, ModContext mod, VehicleState[] state, VsChannel[] lanes, VsChannel.Log log) {
        super("k5-vehicle");
        setDaemon(true);
        this.context = context; this.mod = mod; this.state = state; this.lanes = lanes; this.log = log;
        VehiclePage.tiles = "dict";
        if (mod.settingBoolean("vehicle_page", true)) {
        page.sectionName("Vehicle", "Kia K5");
        page.line("Vehicle", "range", "Fuel range", "fuelpump");
        page.line("Vehicle", "odometer", "Odometer", "road.lanes");
        add("home", "Car home screen", "house", new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        addPackage("hmgapi", "HMG-API", "puzzlepiece.extension", "hmgapi.app");
        addPackage("nemesis", "NEMESIS Manager", "shippingbox", "com.nemesis.manager");
        addPackage("settings", "Vehicle settings", "gearshape", "com.android.vehiclesettings");
        }
        radio = new K5Radio(context, mod, log);
        if (radio.available()) radio.describe(page);
        comfort = new ComfortThresholds(context, mod, log);
        if (comfort.present()) comfort.describe(page);
        page.listener = this;
        log.log("[vehicle] K5 page initialized; shortcuts=" + shortcuts.keySet());
    }
    private void addPackage(String id, String label, String symbol, String pkg) {
        try { add(id, label, symbol, context.getPackageManager().getLaunchIntentForPackage(pkg)); }
        catch (Exception e) { log.log("[vehicle] shortcut discovery failed " + id + ": " + e.getClass().getSimpleName()); }
    }
    private void add(String id, String label, String symbol, Intent i) {
        if (i != null && context.getPackageManager().resolveActivity(i, 0) != null) {
            shortcuts.put(id, i); page.button("Head unit", id, label, symbol);
        } else log.log("[vehicle] shortcut unavailable " + id);
    }
    public void pressed(final String id) {
        if (id.startsWith("radio.")) { radio.pressed(id); return; }
        if (id.startsWith("comfort.")) {
            if (comfort.enabled() && CpClimate.sessionActive && !adjustments.offer(id + ":" + CpClimate.sessionGeneration))
                log.log("[comfort] adjustment queue full");
            return;
        }
        final Intent i = shortcuts.get(id);
        if (i == null) return;
        main.post(new Runnable() { public void run() {
            if (!enabled() || !CpClimate.sessionActive) { log.log("[vehicle] shortcut rejected: inactive session"); return; }
            try { context.startActivity(new Intent(i).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); log.log("[vehicle] opened " + id); }
            catch (Exception e) { log.log("[vehicle] launch failed " + id + ": " + e.getClass().getSimpleName()); }
        }});
    }
    public void switched(String id, boolean value) { }
    boolean enabled() { return mod.enabled() && mod.settingBoolean("vehicle_page", true); }
    void lookup() {
        try {
            ClassLoader cl = new dalvik.system.PathClassLoader("/system/framework/com.hkmc.ext.hcan.jar", context.getClassLoader());
            odometer = CarApi.code(cl, HSCAN, "getOdometer", "(B)F");
            range = CarApi.code(cl, HSCAN, "ableDriveDistance", "(B)I");
            unit = CarApi.code(CarApi.USM, "getDistanceSpeedUnit", "()I");
        } catch (Throwable t) { log.log("[vehicle] getter lookup failed: " + t.getClass().getSimpleName()); }
        log.log("[vehicle] getter availability odometer=" + (odometer >= 0) + " range=" + (range >= 0) + " unit=" + (unit >= 0));
    }
    double read(String service, String iface, int code, boolean arg, boolean floating) {
        if (code < 0) return Double.NaN;
        Parcel data = Parcel.obtain(), reply = Parcel.obtain();
        try {
            IBinder b = CarClimate.service(service);
            if (b == null) return Double.NaN;
            data.writeInterfaceToken(iface);
            if (arg) data.writeByte((byte)0);
            if (!b.transact(code, data, reply, 0)) return Double.NaN;
            reply.readException();
            return floating ? reply.readFloat() : reply.readInt();
        } catch (Throwable t) { return Double.NaN; }
        finally { data.recycle(); reply.recycle(); }
    }
    @Override public void run() {
        lookup();
        while (!isInterrupted()) {
            try {
                if ((enabled() || comfort.enabled()) && CpClimate.sessionActive) {
                    Map<String,Object> fresh = new LinkedHashMap<String,Object>();
                    if (enabled()) {
                    double u = read("com.hkmc.micom.usm", CarApi.USM, unit, false, false);
                    double o = read(SERVICE, HSCAN, odometer, true, true);
                    double r = read(SERVICE, HSCAN, range, true, false);
                    // EV6 semantics are candidate mappings only; unknown unit is never guessed.
                    boolean knownUnit = u == 0 || u == 1;
                    fresh.put("odometer", knownUnit && o > 0 ? VehiclePage.distance(o, u == 1) : VehiclePage.NONE);
                    fresh.put("range", knownUnit && r >= 0 && r <= 2000 ? VehiclePage.distance(r, u == 1) : VehiclePage.NONE);
                    String status = "odometer=" + !VehiclePage.NONE.equals(fresh.get("odometer")) + " range=" + !VehiclePage.NONE.equals(fresh.get("range")) + " unit=" + knownUnit;
                    if (!status.equals(last)) { log.log("[vehicle] readings " + status); last = status; }
                    }
                    if (comfort.present() && comfort.enabled()) {
                        String command = adjustments.poll();
                        if (command != null) {
                            int split = command.lastIndexOf(':');
                            if (Long.parseLong(command.substring(split + 1)) == CpClimate.sessionGeneration)
                                comfort.adjust(command.substring(0, split));
                        }
                        comfort.refresh(fresh);
                    }
                    Map<Long,Object> changed = page.update(fresh);
                    VehicleState current;
                    synchronized (state) { current = state[0]; }
                    if (current != null && !changed.isEmpty()) lanes[1].deliver(current.fromCar(changed));
                }
                if (!CpClimate.sessionActive) adjustments.clear();
                Thread.sleep(comfort.present() ? 1000 : 5000);
            } catch (InterruptedException done) { return; }
            catch (Throwable t) { log.log("[vehicle] refresh failed: " + t.getClass().getSimpleName()); try { Thread.sleep(5000); } catch (InterruptedException done) { return; } }
        }
    }
}
