package ioniq6.cpclimate.check;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ioniq6.cpclimate.CarApi;

/** Read-only K5 target prerequisite checker; not an on-car compatibility certification. */
public final class Main {
    static final String VERSION = "0.1.0-alpha.1";
    static final String REFERENCE = "Kia K5 DL322 USA 260507";

    static final class Check {
        final String area, name, result, found, expected, meaning;

        Check(String area, String name, String result, String found, String expected, String meaning) {
            this.area = area;
            this.name = name;
            this.result = result;
            this.found = found;
            this.expected = expected;
            this.meaning = meaning;
        }
    }

    final List<Check> checks = new ArrayList<Check>();
    final Map<String, String> facts = new LinkedHashMap<String, String>();

    void add(String area, String name, String result, String found, String expected, String meaning) {
        checks.add(new Check(area, name, result, found == null ? "" : found, expected == null ? "" : expected, meaning == null ? "" : meaning));
    }

    String result(String area, String name) {
        for (Check c : checks) if (c.area.equals(area) && c.name.equals(name)) return c.result;
        return "MISSING";
    }

    boolean pass(String area, String name) {
        return "PASS".equals(result(area, name));
    }

    // ---- reading the unit ----

    static String prop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            return (String) sp.getMethod("get", String.class).invoke(null, key);
        } catch (Throwable t) {
            return "";
        }
    }

    static String md5(String path) {
        InputStream in = null;
        try {
            MessageDigest d = MessageDigest.getInstance("MD5");
            in = new FileInputStream(path);
            byte[] buf = new byte[65536];
            for (int n; (n = in.read(buf)) > 0; ) d.update(buf, 0, n);
            StringBuilder s = new StringBuilder();
            for (byte b : d.digest()) s.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            return s.toString();
        } catch (Throwable t) {
            return new File(path).exists() ? "unreadable" : "missing";
        } finally {
            try {
                if (in != null) in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    static Object service(String name) {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            return sm.getMethod("getService", String.class).invoke(null, name);
        } catch (Throwable t) {
            return null;
        }
    }

    /** a read-only binder call: interface token, int arguments, the reply read as "int" or "float"; null on failure */
    static String call(Object binder, String iface, int code, String reply, int... args) {
        if (binder == null || code < 0) return null;
        try {
            Class<?> parcel = Class.forName("android.os.Parcel");
            Method obtain = parcel.getMethod("obtain");
            Object data = obtain.invoke(null), rep = obtain.invoke(null);
            try {
                parcel.getMethod("writeInterfaceToken", String.class).invoke(data, iface);
                for (int a : args) parcel.getMethod("writeInt", int.class).invoke(data, a);
                Class<?> ib = Class.forName("android.os.IBinder");
                ib.getMethod("transact", int.class, parcel, parcel, int.class).invoke(binder, code, data, rep, 0);
                parcel.getMethod("readException").invoke(rep);
                Object v = parcel.getMethod("float".equals(reply) ? "readFloat" : "readInt").invoke(rep);
                return String.valueOf(v);
            } finally {
                parcel.getMethod("recycle").invoke(data);
                parcel.getMethod("recycle").invoke(rep);
            }
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            return "error: " + c.getClass().getSimpleName() + (c.getMessage() != null ? " " + c.getMessage() : "");
        }
    }

    static Integer constant(String cls, String field) {
        try {
            return Class.forName(cls).getField(field).getInt(null);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- the checks ----

    void system() {
        String product = prop("ro.build.product");
        facts.put("ro.build.product", product);
        facts.put("ro.board.platform", prop("ro.board.platform"));
        facts.put("ro.mobis.onebindir", prop("ro.mobis.onebindir"));
        facts.put("ro.build.version.release", prop("ro.build.version.release"));
        facts.put("uid", String.valueOf(android_uid()));
        String[] p = product.split("\\.");
        String model = p.length > 0 ? p[0] : "", region = p.length > 1 ? p[1] : "", fw = p.length > 0 ? p[p.length - 1] : "";
        boolean same = "DL322".equals(model) && "USA".equals(region) && "260507".equals(fw);
        facts.put("model", model);
        add("System", "Head unit model, region and firmware", same ? "PASS" : "FAIL", product, "DL322.USA.….260507",
                same ? "Target firmware; runtime functionality remains untested." : "Outside the K5 port's supported target.");
        String android = prop("ro.build.version.release");
        add("System", "Android", "4.4.2".equals(android) ? "PASS" : "WARN", android, "4.4.2", null);
        boolean hmgapi = new File("/system/bin/hmgapid").exists() || new File("/data/hmgapi").exists();
        add("System", "HMG-API installed", hmgapi ? "PASS" : "FAIL", hmgapi ? "yes" : "no", "yes",
                hmgapi ? null : "Both mods are HMG-API mods; install HMG-API (with this firmware's hook map) first.");
        File wireless = new File("/data/hmgapi/mods/wireless-carplay-aa");
        File mods = new File("/data/hmgapi/mods");
        String w = wireless.exists() ? "installed" : mods.canRead() ? "not installed" : "unknown (run as root to see)";
        add("System", "wireless-carplay-aa mod (wireless CarPlay)", wireless.exists() ? "PASS" : mods.canRead() ? "FAIL" : "INFO", w, "installed",
                "Retain the existing wireless installation.");
    }

    static String read(String path) {
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    static String jsonString(String json, String key) {
        if (json == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    static String exec(String... cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            InputStream in = p.getInputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
            p.waitFor();
            return b.toString("UTF-8").trim();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Inspect the installed provider without recommending upgrades or replacements. */
    void mods() {
        String base = "/data/hmgapi/mods/";
        String manifest = read(base + "wireless-carplay-aa/mod.json");
        String version = jsonString(manifest, "version");
        add("Mods", "Wireless provider", manifest == null ? "INFO" : "PASS",
                manifest == null ? "not verified" : "installed", "shared identification API", "Retain working wireless projection; API availability is checked at runtime.");
        String settings = android_uid() == 0 ? exec("hmgapid", "setting-get", "wireless-carplay-aa") : null;
        String compact = settings == null ? "" : settings.replaceAll("\\s+", "");
        boolean disabled = compact.contains("\"shared_identity\":false");
        boolean enabled = compact.contains("\"shared_identity\":true");
        add("Mods", "Shared identity setting", disabled ? "FAIL" : enabled ? "PASS" : "INFO",
                disabled ? "disabled" : enabled ? "enabled" : "not verified", "enabled",
                "Native activation and theme negotiation remain runtime checks.");
        File routing = new File(base + "egmp-carplay-ev-routing");
        add("Mods", "Legacy EV routing absent", routing.exists() ? "FAIL" : new File(base).canRead() ? "PASS" : "INFO",
                routing.exists() ? "installed" : "not detected", "not installed",
                "Do not run competing identity contributors.");
    }

    static int android_uid() {
        try {
            return (Integer) Class.forName("android.os.Process").getMethod("myUid").invoke(null);
        } catch (Throwable t) {
            return -1;
        }
    }

    void files() {
        file("CarPlay (DiPO)", "/system/lib/libCarPlayCore.so", "d3a47296437df56fb1ffc7ac3dfca4cb", "FAIL", "Stock native recipe target.");
        file("CarPlay (DiPO)", "/system/app/HKMC_DiPO.odex", "edc6f75f5ccf79b63dc85a0902b63d91", "FAIL", "Stock runtime guard target.");
        file("Shared identity", "/system/lib/libiap2session.so", "fc7cb675b46a44bd893a1edc054c5e71", "FAIL", "Wireless provider owns the native patch.");
        facts.put("md5 /system/framework/automotive.odex", md5("/system/framework/automotive.odex"));
    }

    void file(String area, String path, String want, String otherwise, String meaning) {
        String got = md5(path);
        facts.put("md5 " + path, got);
        add(area, path, want.equals(got) ? "PASS" : otherwise, got, want, meaning);
    }

    static final String[][] CALLS = {
            { CarApi.DATC, "registerDATCListener", "(Lcom/hkmc/datc/IDATCListener;Landroid/os/IBinder;)V", "1", "temperature callback (exact °C/°F)" },
            { CarApi.DATC, "getTemperatureUnit", "()I", "3", "the car's temperature unit" },
            { CarApi.DATC, "getDriverOnlyValue", "()I", "27", "Driver Only state" },
            { CarApi.DATC, "getVRClimateStatus", "(I)F", "29", "reading the climate (everything)" },
            { CarApi.DATC, "setVRClimateStatus", "(III)V", "28", "climate commands (fan, A/C, AUTO, temp, modes, defrost, recirculation, SYNC)" },
            { CarApi.DATC, "setFrontClimateControl", "(II)V", "41", "Driver Only and the passenger temperature" },
            { CarApi.USM, "getFrontSeatHeat_DrvState", "()I", "247", "driver seat heat state" },
            { CarApi.USM, "getFrontSeatHeat_AstState", "()I", "248", "passenger seat heat state" },
            { CarApi.USM, "setFrontSeatHeatDrvStep", "(I)V", "d5", "driver seat heat command" },
            { CarApi.USM, "setFrontSeatHeatAstStep", "(I)V", "d6", "passenger seat heat command" },
            { CarApi.VR, "set_RrDefogger_Cmd_AVN", "(I)V", "f", "rear defrost command" },
            { CarApi.BCM, "registerBCMListener", "(Lcom/hkmc/bcm/IBCMListener;Landroid/os/IBinder;)V", "1", "rear defrost state (listener)" },
    };

    void framework() {
        for (String[] c : CALLS) {
            int code = CarApi.code(c[0], c[1], c[2]);
            String iface = c[0].substring(c[0].lastIndexOf('.') + 1);
            String found = code < 0 ? "missing" : Integer.toHexString(code);
            add("Car framework", iface + "." + c[1] + c[2], code < 0 ? "FAIL" : "PASS", found, c[3],
                    code < 0 ? "Not on this firmware (or another signature): cp-climate leaves out " + c[4] + "."
                            : found.equals(c[3]) ? "Same number as the Ioniq 6." : "Another number than the Ioniq 6; cp-climate looks it up by name, so it uses the right one.");
        }
        String datc = CarApi.fingerprint(CarApi.DATC_LISTENER), bcm = CarApi.fingerprint(CarApi.BCM_LISTENER);
        add("Car framework", "IDATCListener fingerprint", CarApi.DATC_LISTENER_PRINT.equals(datc) ? "PASS" : "WARN", String.valueOf(datc), CarApi.DATC_LISTENER_PRINT,
                CarApi.DATC_LISTENER_PRINT.equals(datc) ? "cp-climate registers its temperature listener." : "Differs: cp-climate won't register the temperature listener (temperatures still come from the status flags, in whole degrees).");
        add("Car framework", "IBCMListener fingerprint", CarApi.BCM_LISTENER_PRINT.equals(bcm) ? "PASS" : "WARN", String.valueOf(bcm), CarApi.BCM_LISTENER_PRINT,
                CarApi.BCM_LISTENER_PRINT.equals(bcm) ? "cp-climate registers its body listener for the rear defroster's state." : "Differs: cp-climate won't register it (a listener answering a call wrongly can crash system_server). The rear window button still works, without showing the car's state.");
        // the climate flag numbers cp-climate sends and reads (DATCFlag for getVRClimateStatus/setVRClimateStatus, DATCControl for setFrontClimateControl)
        String[][] flags = {
                { "com.hkmc.datc.DATCFlag", "TEMP", "2" }, { "com.hkmc.datc.DATCFlag", "BLOWER_LEVEL", "5" },
                { "com.hkmc.datc.DATCFlag", "BLOWER_MODE", "6" }, { "com.hkmc.datc.DATCFlag", "AUTO", "8" },
                { "com.hkmc.datc.DATCFlag", "AC", "9" }, { "com.hkmc.datc.DATCFlag", "FRONT_DEFOG", "10" },
                { "com.hkmc.datc.DATCFlag", "INTAKE", "11" }, { "com.hkmc.datc.DATCFlag", "OP_STATE", "12" },
                { "com.hkmc.datc.DATCFlag", "SYNC", "22" }, { "com.hkmc.datc.DATCFlag", "TEMP_PS_C", "29" },
                { "com.hkmc.datc.DATCFlag$DATCControl", "DRIVER_ONLY", "24" }, { "com.hkmc.datc.DATCFlag$DATCControl", "TEMP_DR_C", "28" },
                { "com.hkmc.datc.DATCFlag$DATCControl", "TEMP_PS_C", "29" }, { "com.hkmc.datc.DATCFlag$DATCControl", "TEMP_PS_F", "31" },
        };
        StringBuilder bad = new StringBuilder();
        int same = 0;
        for (String[] f : flags) {
            Integer v = constant(f[0], f[1]);
            if (v != null && v == Integer.parseInt(f[2])) same++;
            else bad.append(bad.length() > 0 ? ", " : "").append(f[1]).append('=').append(v == null ? "missing" : String.valueOf(v));
        }
        add("Car framework", "Climate flag numbers (DATCFlag, DATCControl)", same == flags.length ? "PASS" : "FAIL",
                same + "/" + flags.length + " as the Ioniq 6" + (bad.length() > 0 ? "; differ: " + bad : ""), flags.length + "/" + flags.length,
                same == flags.length ? null : "cp-climate uses these numbers as on the Ioniq 6; different ones would read and set the wrong things. Needs a code change before installing.");
    }

    void live() {
        Object datc = service("com.hkmc.micom.datc"), usm = service("com.hkmc.micom.usm");
        facts.put("service com.hkmc.micom.datc", datc != null ? "present" : "missing");
        facts.put("service com.hkmc.micom.usm", usm != null ? "present" : "missing");
        facts.put("service com.hkmc.micom.bcm", service("com.hkmc.micom.bcm") != null ? "present" : "missing");
        facts.put("service com.hkmc.vr", service("com.hkmc.vr") != null ? "present" : "missing");
        int get = CarApi.code(CarApi.DATC, "getVRClimateStatus", "(I)F");
        int[][] flags = { { 12, 0 }, { 8, 0 }, { 9, 0 }, { 10, 0 }, { 11, 0 }, { 5, 0 }, { 6, 0 }, { 2, 0 }, { 29, 0 }, { 22, 0 } };
        String[] names = { "on (1 on, 2 off)", "AUTO", "A/C", "front defrost", "intake (1 recirc, 2 fresh)", "fan raw (0 off, 2-9)", "vent mode", "driver set temp", "passenger raw ((°C-14)*2)", "SYNC" };
        StringBuilder s = new StringBuilder();
        boolean ok = datc != null && get >= 0;
        for (int i = 0; i < flags.length; i++) {
            String v = call(datc, CarApi.DATC, get, "float", flags[i][0]);
            if (v == null || v.startsWith("error")) ok = false;
            s.append(names[i]).append(' ').append(v).append("; ");
        }
        String unit = call(datc, CarApi.DATC, CarApi.code(CarApi.DATC, "getTemperatureUnit", "()I"), "int");
        String only = call(datc, CarApi.DATC, CarApi.code(CarApi.DATC, "getDriverOnlyValue", "()I"), "int");
        String type = call(datc, CarApi.DATC, CarApi.code(CarApi.DATC, "getClimateType", "()I"), "int");
        s.append("unit ").append(unit).append(" (1 °C, 2 °F); driver only ").append(only).append("; climate type ").append(type);
        facts.put("climate readings", s.toString());
        add("Live climate", "Read-only climate readings", ok ? "PASS" : "WARN", s.toString(), "values (Ioniq 6 °F car: unit 2)",
                ok ? "The car answers cp-climate's reads. Check the values look like the car's panel." : "Some reads failed (as this user, or the service differs); see the values. Running the check as root rules out permissions.");
        if ("1".equals(unit)) add("Live climate", "Temperature unit °C", "WARN", "°C", "°F (tested)", "cp-climate has a °C path, but it was never tried on a car: temperatures need checking on the first drive.");
        String ds = call(usm, CarApi.USM, CarApi.code(CarApi.USM, "getFrontSeatHeat_DrvState", "()I"), "int");
        String ps = call(usm, CarApi.USM, CarApi.code(CarApi.USM, "getFrontSeatHeat_AstState", "()I"), "int");
        boolean seats = ds != null && ps != null && !ds.startsWith("error") && !ps.startsWith("error");
        add("Live climate", "Seat heat states", seats ? "PASS" : "WARN", "driver " + ds + ", passenger " + ps, "2 off, 6/7/8 low/mid/high",
                seats ? "If they show 2 with the seats off, the levels match the Ioniq 6's." : "Seat heat couldn't be read here.");
    }

    // ---- verdicts and the report ----

    List<String[]> verdicts() {
        List<String[]> v = new ArrayList<String[]>();
        boolean nativeOk = pass("System", "Head unit model, region and firmware") && pass("CarPlay (DiPO)", "/system/lib/libCarPlayCore.so") && pass("CarPlay (DiPO)", "/system/app/HKMC_DiPO.odex");
        boolean flags = pass("Car framework", "Climate flag numbers (DATCFlag, DATCControl)");
        boolean read = pass("Car framework", "IDATCService.getVRClimateStatus(I)F");
        boolean set = pass("Car framework", "IDATCService.setVRClimateStatus(III)V");
        boolean front = pass("Car framework", "IDATCService.setFrontClimateControl(II)V");
        v.add(verdict("CarPlay climate (dock temperatures, Climate app)", nativeOk && read && set && flags,
                !nativeOk ? "cp-climate's native patch doesn't fit this CarPlay library" : !flags ? "the climate flag numbers differ" : "climate calls missing"));
        v.add(verdict("Passenger temperature", nativeOk && front && flags, "needs setFrontClimateControl and matching flags"));
        boolean seat = pass("Car framework", "IUSMService.setFrontSeatHeatDrvStep(I)V") && pass("Car framework", "IUSMService.setFrontSeatHeatAstStep(I)V")
                && pass("Car framework", "IUSMService.getFrontSeatHeat_DrvState()I") && pass("Car framework", "IUSMService.getFrontSeatHeat_AstState()I");
        v.add(verdict("Front seat heat and ventilation method prerequisites", nativeOk && seat, "seat heat calls missing or CarPlay climate not possible"));
        v.add(verdict("Front defrost", nativeOk && set && flags, "needs setVRClimateStatus and the FRONT_DEFOG flag"));
        boolean rear = pass("Car framework", "IVRService.set_RrDefogger_Cmd_AVN(I)V");
        boolean rearState = rear && pass("Car framework", "IBCMListener fingerprint") && pass("Car framework", "IBCMService.registerBCMListener(Lcom/hkmc/bcm/IBCMListener;Landroid/os/IBinder;)V");
        v.add(new String[] { "Rear defrost (READY only)", !(nativeOk && rear) ? "NO" : rearState ? "YES" : "PARTLY",
                !(nativeOk && rear) ? "no rear defrost command on this firmware" : rearState ? "" : "works, but without showing the car's state (listener differs)" });
        boolean theme = pass("Shared identity", "/system/lib/libiap2session.so") && pass("Mods", "Wireless provider") && pass("Mods", "Shared identity setting") && pass("Mods", "Legacy EV routing absent");
        v.add(new String[] { "Ultra theme prerequisites", nativeOk && theme ? "PREREQUISITES MATCH" : "NOT VERIFIED", "Native activation and phone negotiation must be tested separately." });
        return v;
    }

    static String[] verdict(String feature, boolean yes, String why) {
        return new String[] { feature, yes ? "PREREQUISITES MATCH" : "NOT VERIFIED", yes ? "" : why };
    }

    String markdown() {
        StringBuilder m = new StringBuilder();
        m.append("# CarPlay Ultra-Lite compatibility report\n\n");
        m.append("Made ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())).append(" on ").append(facts.get("ro.build.product"))
                .append(" by cp-climate-check ").append(VERSION).append(". Reference car: ").append(REFERENCE)
                .append(". The check only reads; it changes nothing on the unit.\n\n");
        m.append("## Will it work on this car?\n\n| Feature | Works | Why not |\n|---|---|---|\n");
        for (String[] v : verdicts()) m.append("| ").append(v[0]).append(" | ").append(v[1]).append(" | ").append(v[2]).append(" |\n");
        int fails = 0, warns = 0;
        for (Check c : checks) {
            if ("FAIL".equals(c.result)) fails++;
            if ("WARN".equals(c.result)) warns++;
        }
        m.append("\n").append(fails == 0 && warns == 0 ? "Everything matches the reference car: the bundle should work as it is.\n"
                : fails + " FAIL, " + warns + " WARN. See what each means below. Don't install until the FAIL items are dealt with.\n");
        m.append("\n## Checks\n\n| Result | Area | Check | Found | Reference car | What it means |\n|---|---|---|---|---|---|\n");
        for (Check c : checks)
            m.append("| ").append(c.result).append(" | ").append(c.area).append(" | ").append(esc(c.name)).append(" | ").append(esc(c.found))
                    .append(" | ").append(esc(c.expected)).append(" | ").append(esc(c.meaning)).append(" |\n");
        m.append("\n## Facts\n\n");
        for (Map.Entry<String, String> e : facts.entrySet()) m.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        m.append("\n## What to send back, or give to your own Claude\n\n");
        m.append("Send this report (cp-climate-check.md and cp-climate-check.json). For a FAIL on a library, also send that file (`adb pull <path>`); ")
                .append("a FAIL on libCarPlayCore.so or libiap2session.so needs a new recipe built against your file. For a call or flag FAIL, also send ")
                .append("/system/framework/automotive.odex.\n\n");
        m.append("For Claude: cp-climate (GPL-3.0, source in the bundle's source/ folder) patches DiPO's libCarPlayCore.so with one HMG-API ")
                .append("recipe (tools/build.py builds it from the stock library and checks its placement with tests/RecipePlaceTest.java; the hook ")
                .append("sites are documented in native/hooks.c and HANDOFF.md), and its Java part looks the car's binder calls up by name ")
                .append("(CarApi.java). Listener fingerprints come from tools/listener_fingerprint.py on baksmali output of automotive.odex. The ")
                .append("theme identification is owned by the existing wireless provider. ")
                .append("Never register a listener whose fingerprint differs without checking every call that writes a reply.\n");
        return m.toString();
    }

    static String esc(String s) {
        return s.replace("|", "\\|").replace("\n", " ");
    }

    String json() {
        StringBuilder j = new StringBuilder("{\n  \"tool\": \"cp-climate-check ").append(VERSION).append("\",\n  \"reference\": \"").append(REFERENCE).append("\",\n  \"facts\": {");
        int i = 0;
        for (Map.Entry<String, String> e : facts.entrySet())
            j.append(i++ > 0 ? "," : "").append("\n    \"").append(q(e.getKey())).append("\": \"").append(q(e.getValue())).append('"');
        j.append("\n  },\n  \"verdicts\": [");
        i = 0;
        for (String[] v : verdicts())
            j.append(i++ > 0 ? "," : "").append("\n    {\"feature\": \"").append(q(v[0])).append("\", \"works\": \"").append(v[1]).append("\", \"why\": \"").append(q(v[2])).append("\"}");
        j.append("\n  ],\n  \"checks\": [");
        i = 0;
        for (Check c : checks)
            j.append(i++ > 0 ? "," : "").append("\n    {\"result\": \"").append(c.result).append("\", \"area\": \"").append(q(c.area)).append("\", \"check\": \"").append(q(c.name))
                    .append("\", \"found\": \"").append(q(c.found)).append("\", \"expected\": \"").append(q(c.expected)).append("\"}");
        return j.append("\n  ]\n}\n").toString();
    }

    static String q(String s) {
        StringBuilder o = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') o.append('\\').append(c);
            else if (c < 0x20) o.append(' ');
            else o.append(c);
        }
        return o.toString();
    }

    static void write(File f, String text) throws Exception {
        Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "/data/local/tmp/cp-climate-check");
        out.mkdirs();
        Main m = new Main();
        m.system();
        m.mods();
        m.files();
        m.framework();
        m.live();
        String md = m.markdown();
        write(new File(out, "cp-climate-check.md"), md);
        write(new File(out, "cp-climate-check.json"), m.json());
        for (String[] v : m.verdicts()) System.out.println(v[1] + "\t" + v[0] + (v[2].length() > 0 ? " (" + v[2] + ")" : ""));
        System.out.println("report: " + new File(out, "cp-climate-check.md"));
    }
}
