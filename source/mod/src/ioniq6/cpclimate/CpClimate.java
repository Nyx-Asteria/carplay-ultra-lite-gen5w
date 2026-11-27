package ioniq6.cpclimate;

import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import hmgapi.hook.ModEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CarPlay climate, milestone 1: DiPO tells the iPhone it speaks the vehicle state protocol
 * (the recipe adds vehicleStateProtocolInfo and displayPanels to /info and echoes the
 * feature in the SETUP reply) and logs the phone's SETUP requests, to see whether it then
 * asks for the vehicle state data streams. M2 accepts those streams (VsChannel); M3 answers
 * them as LIVI does (VehicleState). M4 puts the car's own climate on CarPlay both ways
 * (CarClimate): the car's state mirrored to the phone, the phone's changes sent to the car.
 *
 * Based on LIVI by Lasse Heitgres (https://github.com/f-io/LIVI).
 */
public final class CpClimate extends ModEntry {
    static final String DIPO = "com.daudio.app.dipo";
    /** the session's stream SETUPs come in after it has started */
    static final long COPY_DELAY_MS = 5000;
    static final int MAX_LINES = 600;
    /** log entries copied into HMG-API's saved log: the patched SETUP log, ours, DiPO's stream refusals */
    static final String[] MARKS = { "Setting up session", "cp-climate", "Unsupported stream type",
            "Command '", "Request accessory UI", "ChangeModeBorrow", "ChangeModeUnborrow", "Modes changed" };

    private boolean targetVerified;
    private boolean themePreflight;
    private final ThemeContribution theme = new ThemeContribution();

    @Override
    public void zygoteInit(ModContext m) {
        String product = property("ro.product.device");
        boolean model = "wp_daudioplus_dl322_us".equals(product);
        // Exact firmware is enforced by the manifest and stock binary fingerprints below.
        boolean core = fingerprint(m, "CarPlayCore", "/system/lib/libCarPlayCore.so", "d3a47296437df56fb1ffc7ac3dfca4cb");
        boolean identity = fingerprint(m, "iAP2", "/system/lib/libiap2session.so", "fc7cb675b46a44bd893a1edc054c5e71");
        boolean dipo = fingerprint(m, "DiPO", "/system/app/HKMC_DiPO.odex", "edc6f75f5ccf79b63dc85a0902b63d91");
        targetVerified = model && core && identity && dipo;
        m.log("[preflight] target=" + model + " core=" + core + " identity=" + identity + " dipo=" + dipo + " ready=" + targetVerified);
        themePreflight = targetVerified
                && m.settingBoolean("ultra_theme", true) && theme.available(m);
        m.log("[theme] boot preflight=" + themePreflight);
        if (targetVerified && m.settingBoolean("k5_integration", true)) VolumePanel.zygote(m);
    }

    private static String property(String key) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) { return ""; }
    }

    private static boolean fingerprint(ModContext m, String name, String path, String expected) {
        String actual = ModContext.md5(path);
        boolean match = expected.equals(actual);
        m.log("[preflight] " + name + " md5=" + actual + " match=" + match);
        return match;
    }

    static ModContext mod;
    static final Set<String> copied = new HashSet<String>();
    static boolean busy;
    static volatile long sessionGeneration;
    static volatile boolean sessionActive;
    static CarPlayMedia media;
    public static boolean videoAllowed() { return VideoUi.enabled && sessionActive && ParkedVideo.allowed(); }

    /** files/cpc-vspi.bplist: VehicleState.vspi for the native /info hook (removed if it can't be built) */
    static boolean writeVspi(File dir, VsChannel.Log log) {
        File to = new File(dir, "cpc-vspi.bplist"), tmp = new File(dir, "cpc-vspi.bplist.tmp");
        try {
            byte[] b = new VehicleState(new VsChannel.Log() {
                @Override
                public void log(String m) {
                }
            }).vspi();
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try {
                out.write(b);
            } finally {
                out.close();
            }
            if (!tmp.renameTo(to)) throw new java.io.IOException("rename failed");
            log.log("vehicleStateProtocolInfo for /info written (" + b.length + " bytes, accessory versions " + VehicleState.changedVersions + ")");
            return true;
        } catch (Throwable t) {
            to.delete();
            tmp.delete();
            log.log("vehicleStateProtocolInfo for /info not written: " + t);
            return false;
        }
    }

    @Override
    public void appLoaded(ModContext m, LoadedApp app) {
        if ("com.tcc.iap2server".equals(app.packageName)) {
            if (themePreflight && m.enabled() && m.settingBoolean("ultra_theme", true)) {
                theme.register(m);
            } else m.log("[theme] iAP2 contribution withheld by boot preflight/settings");
            return;
        }
        if (!DIPO.equals(app.packageName)) return;
        m.log("[climate] startup target=" + targetVerified + " enabled=" + m.settingBoolean("k5_integration", true));
        if (!targetVerified) return;
        boolean climate = m.settingBoolean("k5_integration", true);
        boolean any = climate || m.settingBoolean("k5_integration", true) || m.settingBoolean("parked_video", false) || m.settingBoolean("ultra_theme", true);
        if (!any) { m.log("[runtime] all features off; native patch withheld"); return; }
        mod = m;
        File dir = app.context != null ? app.context.getFilesDir() : new File("/data/data/" + DIPO + "/files");
        ClimateMap.loadSetpoints(new File(dir, "cpc-setpoints"));
        {
            // CarPlay Ultra-Lite 1.0: two settings. Everything the development builds made switchable is fixed
            // at what was tested on the Ioniq 6 (CEEV USA 260507, 2026-11-08); the code for the other values stays.
            // Theme registration is not integrated yet. Never advertise unsupported Ultra mode.
            boolean ultra = m.enabled() && m.settingBoolean("ultra_theme", true);
            boolean cluster = ultra;
            boolean themedIds = cluster;
            boolean variant = ultra;
            boolean features = ultra;
            boolean enhanced = ultra;
            m.log("[theme] identity=" + themePreflight + " cluster=" + cluster + " themedIds=" + themedIds + " variant=" + variant + " features=" + features + " enhanced=" + enhanced);
            m.log("[theme] DiPO display profile enabled=" + ultra + " (shared provider native result not exposed)");
            writeSize(dir, "80");
            writeDock(dir, "1");                                  // the bottom dock with the climate bar
            writeVersion(dir, "950");                             // receiver version 950.7.1 in /info
            writeFlag(dir, "cpc-extras", climate, "LIVI display extras");
            writeCluster(dir, "full");                            // the instrument panel with its plugin (climate overlay)
            writeWide(dir, "0");
            writeUltra(dir, themedIds ? 2 : cluster ? 1 : 0);                       // virtual cluster + the theme's display names
            boolean iapBridge = false, iap = false;               // iAP over CarPlay: an experiment, off
            writeFlag(dir, "cpc-iap", false, "iAPChannel");
            // the theme's vehicle: the Aston Martin DBX unless the vehicle_variant test setting picks another
            // (the Climate app shows a placeholder instead of its car picture with DBX; which model it needs is a test)
            String model = "DBX";
            VehicleState.variant = variant && !"none".equals(model) ? model : "";
            m.log("vehicle variant " + (VehicleState.variant.isEmpty() ? "none" : VehicleState.variant));
            boolean uiSync = ultra && m.settingBoolean("ultra_ui_sync", true);                               // the theme's layouts, palettes and wallpapers
            writeFlag(dir, "cpc-uisync", uiSync, "uiSync (CarPlay theme layouts and palettes)");
            writeFeatures(dir, features ? "livi_all" : "off");
            writeFlag(dir, "cpc-xfeat", enhanced, "/info enhancedRequestCarUI");
            // bit 1 = videoPlayback (the phone's AirPlay video while parked), bit 2 = hevc; both only when the phone offers them
            // Stage 2 is not validated: do not offer video even if an old setting is saved.
            int extraBits = m.settingBoolean("parked_video", false) && app.context != null ? 1 : 0;
            if (extraBits != 0) {
                ParkedVideo park = new ParkedVideo(m);
                if (!park.available()) { m.log("[video] Park getter missing; video withheld"); extraBits = 0; }
                else {
                    boolean sound = m.settingBoolean("video_audio", false);
                    writeNumber(dir, "cpc-video-nosound", sound ? 0 : 1, "video: picture only");
                    m.log("[video-audio] local player audio requested=" + sound + "; stock projection route untouched");
                    writeNumber(dir, "cpc-video-nopicture", 0, "video: picture enabled");
                    new VideoMessages(new VsChannel.Log() { public void log(String s) { m.log("[video] " + s); } });
                    if (!VideoMessages.startEngine(app.context)) {
                        m.log("[video] engine unavailable; feature withheld");
                        extraBits = 0;
                    } else { park.start(); m.log("[video] engine initialized; Park confirmation required"); }
                }
            }
            writeNumber(dir, "cpc-xenable", extraBits, extraBits != 0 ? "SETUP extra features: videoPlayback" : "SETUP extra features off");
            writeNumber(dir, "cpc-hid", 0, "LIVI input devices in /info");   // on, iOS drew a knob-focus box
            m.log("[native] requesting CarPlayCore recipe; application result is reported by HMG-API");

            // M2: the vehicle state data streams the native SETUP handler sends the phone to;
            // M3: answered from one VehicleState the two lanes share
            VsChannel.Log log = new VsChannel.Log() {
                @Override
                public void log(String msg) {
                    mod.log(msg);
                }
            };
            VehicleState[] state = new VehicleState[1];
            VsChannel[] lanes = new VsChannel[2];
            for (int lane = 0; lane < 2; lane++) lanes[lane] = new VsChannel(lane, dir, log, state, lanes);

            // M4: the car's climate on CarPlay both ways (CarClimate reads it and sends the phone's changes)
            CarClimate.configureTestSwitches(m);
            CarClimate.rearDefrost = true;
            // the ambient light test (off): one shared, writable setting in CarPlay's car settings whose value is a #RRGGBB
            // that is sent to the car's mood light; see VehicleState.withAmbientTest and CarClimate.writeAmbient
            VehicleState.ambientTest = CarClimate.ambientTest = false;
            // the extra seat and wheel controls (off): rear seat heat, front seat ventilation, steering wheel heater
            VehicleState.extras = ClimateMap.extras = climate;
            VehicleState.climateEnabled = climate;
            // A: ventilation as separate SeatFan services; B: as the seats' own cooling levels, plus a rear zone
            // the driver card has room for three buttons: AUTO, vent position, seat (2026-11-10: more were drawn on top
            // of each other). The wheel heater and the per-seat power are off unless turned on.
            VehicleState.wheelHeater = climate && CarClimate.wheelAvailable();
            m.log("[wheel] OEM methods available=" + VehicleState.wheelHeater);
            VehicleState.zonePower = false;
            VehicleState.rearZone = false;
            ClimateMap.blankWhenOff = true;
            VehicleState.ventCard = "driver";
            VehicleState.coolingMin = -3;
            String layout = "a"; // upstream separate SeatFan services for front ventilation
            VehicleState.coolingLayout = "b".equals(layout) || "c".equals(layout) || "d".equals(layout);
            VehicleState.rowRear = "c".equals(layout);
            VehicleState.twoRear = "d".equals(layout);
            // /info's copy of the car (the phone builds its car from it): cp-climate's description, not LIVI's
            K5Vehicle vehicle = null;
            if (app.context != null && (m.settingBoolean("k5_integration", true))) {
                vehicle = new K5Vehicle(app.context, m, state, lanes, log);
                VehicleState.vehiclePage = vehicle.page;
            } else { VehicleState.vehiclePage = null; m.log("[vehicle] page disabled or context unavailable"); }
            if (!writeVspi(dir, log)) { m.log("[native] dynamic schema unavailable; patch withheld"); return; }
            m.nativePatch(app, "libCarPlayCore.so");
            m.log("[climate] profile=K5 front heat/vent -3..3, rear seats=false, ambient=false");
            CarClimate car = climate ? new CarClimate(state, lanes, log) : null;
            VsChannel.car = car;
            for (VsChannel ch : lanes) ch.start();
            if (car != null) car.start();
            if (vehicle != null) vehicle.start();
            if (m.settingBoolean("k5_integration", true)) {
                CarAudio audio = new CarAudio(state, lanes, log, m);
                CarAudio.instance = audio;
                audio.listen(app.context);
                audio.start();
            } else m.log("[audio] volume link disabled");
            if (m.settingBoolean("k5_integration", true)) {
                final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                media = new CarPlayMedia(new CarPlayMedia.Later() {
                    public void post(Runnable action, long delayMs) { handler.postDelayed(action, delayMs); }
                }, log, m);
                VehicleState.mediaControls = media;
                m.log("[media] manual controls ready; no autoplay");
            }
            m.log("[climate] reader and both vehicle-state lanes started");
            // the Ultra experiment: the virtual cluster's video goes here and is thrown away
            if (cluster) new ClusterSink(log).start();
            if ((extraBits & 1) != 0) new ClusterSink(log, 7134, "videoPlayback").start();
            if ((extraBits & 1) != 0) VideoMessages.instance.start();
            // video stage 1: requestUI("videoplayback:") kept from DiPO (no jump to the launcher), screen borrowed as LIVI does
            VideoUi.out = log;
            VideoUi.enabled = (extraBits & 1) != 0;
            // the iAPChannel experiment, stage A: the phone's iAP stream is decrypted and logged only
            if (iap) {
                IapBridge.out = log;
                IapBridge.loader = app.classLoader;
                new IapChannel(dir, log, iapBridge).start();
            }
            // the uiSync experiment: the theme's layout/palette channel, answered by UiSync
            // Bootstrap the known Modular layout; palette selection is still subject to asset verification.
            if (uiSync) new UiSyncChannel(dir, log, UiSync.layout("Modular")).start();
        }
        MethodHook copy = new MethodHook() {
            @Override
            protected void afterHookedMethod(Param p) {
                sessionActive = false;
                if (media != null) media.ended(p.thisObject);
                VideoMessages.stopEngine("session finalized");
                sessionGeneration++;
                m.log("[session] finalized");
                copyLater(COPY_DELAY_MS);
            }
        };
        m.hook(app.classLoader, "dipo.carplay.session_started", new MethodHook() {
            @Override
            protected void afterHookedMethod(Param p) {
                sessionGeneration++;
                sessionActive = true;
                if (media != null) media.started(p.thisObject);
                m.log("[session] started generation=" + sessionGeneration);
                IapBridge.carPlay = p.thisObject;
                VideoUi.install(p.thisObject, app.classLoader);
                copyLater(COPY_DELAY_MS);
            }
        });
        m.hook(app.classLoader, "dipo.carplay.session_finalized", copy);
        // Private K5 experiment: exact DiPO fingerprint and presentation-only method.
        // Low-level hook is outside HookPlan; retain OEM behavior on any uncertainty.
        if (app.context != null && "edc6f75f5ccf79b63dc85a0902b63d91".equals(ModContext.md5("/system/app/HKMC_DiPO.odex"))) {
            try {
                final android.content.Context mediaContext = app.context;
                Class<?> service = hmgapi.hook.Hooks.findClass("com.daudio.app.dipo.service.DipoService", app.classLoader);
                java.lang.reflect.Method osd = hmgapi.hook.Hooks.findMethodExact(service, "b", int.class, boolean.class);
                if (osd.getReturnType() == void.class && java.lang.reflect.Modifier.isPrivate(osd.getModifiers())) {
                    hmgapi.hook.Hooks.findAndHookMethod(service, "b", int.class, boolean.class, new MethodHook() {
                        @Override protected void beforeHookedMethod(Param p) {
                            try {
                                if (!m.enabled() || !m.settingBoolean("k5_integration", true)
                                        || !m.settingBoolean("suppress_stock_media", false)
                                        || !sessionActive || !VolumePanel.foreground(mediaContext)) return;
                                p.setResult(null);
                            } catch (Throwable ignored) { /* Fail open. */ }
                        }
                    });
                    m.log("[media-osd] exact K5 presentation hook installed (opt-in)");
                }
            } catch (Throwable t) {
                m.log("[media-osd] hook unavailable; OEM retained: " + t);
            }
        }

        m.log("M4 in " + app.processName + "; restart-count guard removed");
    }

    @Override public void onEnabled(ModContext m, boolean enabled) {
        if (!enabled) { theme.unregister(m); sessionActive = false; sessionGeneration++; VideoMessages.stopEngine("mod disabled"); }
        m.log("[runtime] enabled=" + enabled + "; restart required to change advertised capabilities");
    }

    /** the CarPlay size setting, for the native /info hook (files/cpc-size, 50-100 %) */
    static void writeSize(File dir, String size) {
        File f = new File(dir, "cpc-size");
        try {
            int pct = Integer.parseInt(size.trim());
            if (pct < 50 || pct > 100) pct = 100;
            FileWriter w = new FileWriter(f);
            try {
                w.write(pct + "\n");
            } finally {
                w.close();
            }
            mod.log("CarPlay size " + pct + " %");
        } catch (Exception e) {
            f.delete();
            mod.log("CarPlay size: real size (" + e + ")");
        }
    }

    /** the dock position setting, for the native /info hook (files/cpc-dock: 0 auto, 1 bottom, 2 side) */
    static void writeDock(File dir, String dock) {
        File f = new File(dir, "cpc-dock");
        try {
            int edge = Integer.parseInt(dock.trim());
            if (edge < 0 || edge > 4) edge = 1;
            FileWriter w = new FileWriter(f);
            try {
                w.write(edge + "\n");
            } finally {
                w.close();
            }
            mod.log("CarPlay dock edge " + edge);
        } catch (Exception e) {
            f.delete();
            mod.log("CarPlay dock: bottom (" + e + ")");
        }
    }

    /** the receiver version experiment, for the native /info hook (files/cpc-version: 1 for 950.7.1) */
    static void writeVersion(File dir, String version) {
        File f = new File(dir, "cpc-version");
        if (!"950".equals(version.trim())) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write("1\n");
            } finally {
                w.close();
            }
            mod.log("CarPlay receiver version 950.7.1 in /info (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** an on/off experiment for the native /info hook: the file holds 1 while on */
    static void writeFlag(File dir, String name, boolean on, String what) {
        File f = new File(dir, name);
        if (!on) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write("1\n");
            } finally {
                w.close();
            }
            mod.log(what + " on (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** LIVI's /info feature bits that DiPO's lacks, without its audio ones (CARPLAY_AUDIO_FEATURES: bits 9, 11, 18, 20, 22, 26, 40) */
    static final long LIVI_NON_AUDIO = 0x205203a462L;
    /** all of LIVI's bits that DiPO's lacks, audio ones too */
    static final long LIVI_ALL = 0x615653aee2L & ~0x4104040280L;

    /** the /info feature bits experiment (files/cpc-feat, hex): livi_nonaudio, livi_all or bit_N; off deletes it */
    static void writeFeatures(File dir, String value) {
        File f = new File(dir, "cpc-feat");
        String v = value.trim();
        long bits = 0;
        if ("livi_nonaudio".equals(v)) bits = LIVI_NON_AUDIO;
        else if ("livi_all".equals(v)) bits = LIVI_ALL;
        else if (v.startsWith("bit_")) {
            try {
                int bit = Integer.parseInt(v.substring(4));
                if (bit >= 0 && bit < 63) bits = 1L << bit;
            } catch (NumberFormatException e) {
                bits = 0;
            }
        }
        if (bits == 0) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write(Long.toHexString(bits) + "\n");
            } finally {
                w.close();
            }
            mod.log("/info feature bits 0x" + Long.toHexString(bits) + " (" + v + ") on (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** a small number for the native hooks; 0 deletes the file */
    static void writeNumber(File dir, String name, int value, String what) {
        File f = new File(dir, name);
        if (value == 0) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write(value + "\n");
            } finally {
                w.close();
            }
            mod.log(what + " on (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** the Ultra experiment for the native hooks (files/cpc-ultra: 1 virtual cluster, 2 with the theme's display ids) */
    static void writeUltra(File dir, int mode) {
        File f = new File(dir, "cpc-ultra");
        if (mode == 0) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write(mode + "\n");
            } finally {
                w.close();
            }
            mod.log("CarPlay Ultra virtual cluster on" + (mode == 2 ? ", with the theme's display ids" : "") + " (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** the Apple widescreen view areas experiment (files/cpc-wide: 1 start whole, 2 start right) */
    static void writeWide(File dir, String value) {
        File f = new File(dir, "cpc-wide");
        String v = value.trim();
        if (!"1".equals(v) && !"2".equals(v)) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write(v + "\n");
            } finally {
                w.close();
            }
            mod.log("Apple widescreen view areas, start " + ("2".equals(v) ? "right" : "whole") + " (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** the instrument panel experiment (files/cpc-nocluster: 1 off, 2 no showsInstruments, 3 no plugin) */
    static void writeCluster(File dir, Object value) {
        File f = new File(dir, "cpc-nocluster");
        String v = value == null ? "full" : String.valueOf(value);
        // an older version saved this setting as a toggle: false meant off
        int code = "off".equals(v) || "false".equals(v) ? 1 : "no_instruments".equals(v) ? 2 : "no_plugin".equals(v) ? 3
                : "plugin_main".equals(v) ? 4 : "plugin_stream".equals(v) ? 5 : 0;
        if (code == 0) {
            f.delete();
            return;
        }
        try {
            FileWriter w = new FileWriter(f);
            try {
                w.write(code + "\n");
            } finally {
                w.close();
            }
            mod.log("instrument panel: " + v + " (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    static synchronized void copyLater(final long delay) {
        if (busy) return;
        busy = true;
        new Thread("cp-climate") {
            @Override
            public void run() {
                try {
                    Thread.sleep(delay);
                    copy();
                } catch (Throwable t) {
                    mod.error("copying the CarPlay lines", t);
                } finally {
                    synchronized (CpClimate.class) { busy = false; }
                }
            }
        }.start();
    }

    /** DiPO's own TccCarPlay lines: an entry spans several lines that share one header */
    static synchronized void copy() throws Exception {
        Process p = Runtime.getRuntime().exec(new String[] { "logcat", "-d", "-v", "threadtime", "-s", "TccCarPlay:*" });
        List<String> lines = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String l;
            while ((l = in.readLine()) != null) lines.add(l);
        } finally {
            in.close();
            p.destroy();
        }
        int sent = 0, entries = 0;
        boolean streams130 = false;
        for (int i = 0; i < lines.size() && sent < MAX_LINES; i++) {
            String l = lines.get(i);
            if (!marked(l)) continue;
            String head = header(l);
            if (head == null || !copied.add(head)) continue;
            entries++;
            for (int j = i; j < lines.size() && sent < MAX_LINES && head.equals(header(lines.get(j))); j++) {
                String body = lines.get(j).substring(head.length());
                if (body.contains("\"type\" : 130") || body.contains("stream type: 130")) streams130 = true;
                // Do not copy arbitrary SETUP payloads: they can contain device identifiers or session keys.
                if (body.contains("Unsupported stream type") || body.contains("Modes changed"))
                    mod.log("[projection] " + (body.contains("Unsupported stream type") ? "unsupported stream reported" : "mode transition reported"));
                sent++;
            }
        }
        if (entries > 0) {
            mod.log("CP copied " + entries + " entr" + (entries == 1 ? "y" : "ies") + ", " + sent + " line(s); type 130 streams "
                    + (streams130 ? "ASKED FOR" : "not seen"));
        }
    }

    static boolean marked(String l) {
        for (String m : MARKS) if (l.contains(m)) return true;
        return false;
    }

    /** "11-07 12:44:01.930  1363  1363 " from a threadtime line, or null */
    static String header(String l) {
        if (l.length() < 31 || l.charAt(2) != '-' || l.charAt(5) != ' ') return null;
        int k = 18;
        for (int f = 0; f < 2; f++) {
            while (k < l.length() && l.charAt(k) == ' ') k++;
            while (k < l.length() && Character.isDigit(l.charAt(k))) k++;
        }
        return k < l.length() ? l.substring(0, k + 1) : null;
    }
}
