package ioniq6.cpclimate;

import hmgapi.hook.LoadedApp;
import hmgapi.hook.MethodHook;
import hmgapi.hook.ModContext;
import hmgapi.hook.ModEntry;

import java.io.File;
import java.io.FileWriter;

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
                && m.settingBoolean("ultra_theme", true) && m.settingBoolean("ultra_sidebars", true) && theme.available(m);
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
    private static volatile SessionLogging sessionLogging;
    static void runtimeLog(String msg) {
        SessionLogging logger=sessionLogging;
        if(logger!=null) logger.log(msg); else if(mod!=null) mod.log(msg);
    }
    static volatile long sessionGeneration;
    static volatile boolean sessionActive;
    static CarPlayMedia media;
    public static long videoSessionGeneration() { return sessionGeneration; }
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
            // Provider publication may occur after zygote preflight. Recheck in
            // the consumer process before registering; keep stock fingerprints.
            themePreflight=targetVerified && m.settingBoolean("ultra_theme",true)
                    && m.settingBoolean("ultra_sidebars",true) && theme.available(m);
            m.log("[theme] iAP2 process preflight="+themePreflight);
            if (themePreflight && m.enabled() && m.settingBoolean("ultra_theme", true) && m.settingBoolean("ultra_sidebars", true)) {
                theme.register(m);
            } else m.log("[theme] iAP2 contribution withheld by boot preflight/settings");
            return;
        }
        if (K5Radio.PACKAGE.equals(app.packageName)) {
            if (targetVerified && m.enabled() && m.settingBoolean("k5_integration", true)
                    && m.settingBoolean("sxm_schema_test", false) && app.context != null)
                SxmCatalogBridge.install(m, app.context);
            return;
        }
        if (!DIPO.equals(app.packageName)) return;
        m.log("[climate] startup target=" + targetVerified + " enabled=" + m.settingBoolean("k5_integration", true));
        if (!targetVerified) return;
        themePreflight=m.settingBoolean("ultra_theme",true)
                && m.settingBoolean("ultra_sidebars",true) && theme.available(m);
        m.log("[theme] DiPO process preflight="+themePreflight+" (registry availability only)");
        boolean climate = m.settingBoolean("k5_integration", true);
        boolean any = climate || m.settingBoolean("parked_video", false) || m.settingBoolean("ultra_theme", true);
        if (!any) { m.log("[runtime] all CPUL features off; disable CPUL itself to use standalone PPS CarPlay scaling"); return; }
        mod = m;
        File dir = app.context != null ? app.context.getFilesDir() : new File("/data/data/" + DIPO + "/files");
        if (sessionLogging == null) {
            sessionLogging = new SessionLogging(m, dir, property("ro.build.display.id"), RuntimeEnvironment.describe(property("ro.product.device"), app.context));
            new ProjectionLogCapture(sessionLogging).start();
        }
        ClimateMap.loadSetpoints(new File(dir, "cpc-setpoints"));
        {
            // RC6 verified baseline; themed identity and display IDs are paired.
            boolean ultra = m.enabled() && m.settingBoolean("ultra_theme", true);
            boolean cluster = ultra;
            boolean themedIds = cluster && themePreflight && m.settingBoolean("ultra_sidebars", true);
            boolean variant = ultra;
            boolean features = ultra;
            boolean enhanced = ultra;
            m.log("[theme] identity=" + themePreflight + " cluster=" + cluster + " themedIds=" + themedIds + " variant=" + variant + " features=" + features + " enhanced=" + enhanced);
            m.log("[theme] DiPO display profile enabled=" + ultra + " (shared provider native result not exposed)");
            writeDelegatedScale(dir, m); // Optional Phone Projection Scaling integration.
            NativeProfile.writeDock(m, dir, "1");                                  // the bottom dock with the climate bar
            NativeProfile.writeVersion(m, dir, ultra ? "950" : "off");                             // receiver version 950.7.1 in /info
            NativeProfile.writeFlag(m, dir, "cpc-extras", ultra, "LIVI display extras");
            NativeProfile.writeCluster(m, dir, "full");                            // the instrument panel with its plugin (climate overlay)
            NativeProfile.writeWide(m, dir, "0");
            NativeProfile.writeUltra(m, dir, themedIds ? 2 : cluster ? 1 : 0);                       // virtual cluster + the theme's display names
            boolean iapBridge = false, iap = false;               // iAP over CarPlay: an experiment, off
            NativeProfile.writeFlag(m, dir, "cpc-iap", false, "iAPChannel");
            // the theme's vehicle: the Aston Martin DBX unless the vehicle_variant test setting picks another
            // (the Climate app shows a placeholder instead of its car picture with DBX; which model it needs is a test)
            VehicleState.variant = variant ? "DBX" : "";
            m.log("vehicle variant " + (VehicleState.variant.isEmpty() ? "none" : VehicleState.variant));
            boolean uiSync = themedIds && m.settingBoolean("ultra_ui_sync", false);                               // the theme's layouts, palettes and wallpapers
            NativeProfile.writeFlag(m, dir, "cpc-uisync", uiSync, "uiSync (CarPlay theme layouts and palettes)");
            NativeProfile.writeFlag(m, dir,"cpc-sync-independent",uiSync && m.settingBoolean("ultra_independent_sync",false),"independent uiSync negotiation (experimental)");
            NativeProfile.writeFeatures(m, dir, features ? "livi_all" : "off");
            NativeProfile.writeFlag(m, dir, "cpc-xfeat", enhanced, "/info enhancedRequestCarUI");
            // bit 1 = videoPlayback (the phone's AirPlay video while parked), bit 2 = hevc; both only when the phone offers them
            // Video is opt-in and requires a compatible Park getter and initialized engine.
            int extraBits = m.settingBoolean("parked_video", false) && app.context != null ? 1 : 0;
            if (extraBits != 0) {
                ParkedVideo park = new ParkedVideo(m);
                if (!park.available()) { m.log("[video] Park getter missing; video withheld"); extraBits = 0; }
                else {
                    boolean sound = !m.settingBoolean("parked_video_picture_only", false); // opt-in isolation; default retains audio fallback
                    NativeProfile.writeNumber(m, dir, "cpc-video-nosound", sound ? 0 : 1, "video: picture only");
                    m.log("[video-audio] local player audio requested=" + sound + "; stock projection route untouched");
                    NativeProfile.writeNumber(m, dir, "cpc-video-nopicture", 0, "video: picture enabled");
                    new VideoMessages(new VsChannel.Log() { public void log(String s) { runtimeLog("[video] " + s); } });
                    if (!VideoMessages.startEngine(app.context)) {
                        m.log("[video] engine unavailable; feature withheld");
                        extraBits = 0;
                    } else { park.start(); m.log("[video] engine initialized; Park confirmation required"); }
                }
            }
            NativeProfile.writeNumber(m, dir, "cpc-xenable", extraBits, extraBits != 0 ? "SETUP extra features: videoPlayback" : "SETUP extra features off");
            NativeProfile.writeNumber(m, dir, "cpc-hid", 0, "LIVI input devices in /info");   // on, iOS drew a knob-focus box
            m.log("[native] requesting CarPlayCore recipe; application result is reported by HMG-API");

            // M2: the vehicle state data streams the native SETUP handler sends the phone to;
            // M3: answered from one VehicleState the two lanes share
            VsChannel.Log log = new VsChannel.Log() {
                @Override
                public void log(String msg) {
                    runtimeLog(msg);
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
            VehicleState.sxmSchemaTest = m.settingBoolean("k5_integration", true)
                    && m.settingBoolean("sxm_schema_test", false);
            // Front-seat and wheel controls follow the K5 integration setting.
            VehicleState.extras = ClimateMap.extras = climate;
            VehicleState.seatFanImmediateFeedback = climate && m.settingBoolean("seat_fan_immediate_feedback", false);
            VehicleState.seatHeatImmediateFeedback = m.settingBoolean("seat_heat_immediate_feedback", true);
            m.log("[seat-fan] immediate feedback experiment=" + VehicleState.seatFanImmediateFeedback + "; heating immediate=" + VehicleState.seatHeatImmediateFeedback);
            VehicleState.climateEnabled = climate;
            // Keep the front-seat layout and expose wheel heating only when OEM methods are available.
            VehicleState.wheelHeater = climate && CarClimate.wheelAvailable();
            m.log("[wheel] OEM methods available=" + VehicleState.wheelHeater);
            VehicleState.zonePower = false;
            VehicleState.rearZone = false;
            ClimateMap.blankWhenOff = true;
            VehicleState.ventCard = "driver";
            VehicleState.coolingMin = -3;
            // K5 uses separate front SeatFan services; no rear-seat services.
            VehicleState.coolingLayout = false;
            VehicleState.rowRear = false;
            VehicleState.twoRear = false;
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
                final K5Radio radio = vehicle == null ? null : vehicle.radio;
                VehicleState.mediaControls = new VehicleState.Controls() {
                    public void pressed(String name, Object value) {
                        // LIVI controlRequest parameters: frequency (integer kHz),
                        // identifier (decimal kHz string), optional sourceIdentifier.
                        if ("tuneToFrequency".equals(name) || "tuneToIdentifier".equals(name)) {
                            if (radio == null || !(value instanceof java.util.Map)) return;
                            java.util.Map<?,?> params = (java.util.Map<?,?>) value;
                            Object source = params.get("sourceIdentifier");
                            // Apple CAFMedia exposes multicastIndex, but the wire key and
                            // index base are not established. Observe explicit numeric fields
                            // only; retain the existing primary-frequency tuning behavior.
                            for (String key : new String[]{"multicastIndex","multicast"}) {
                                if (params.containsKey(key)) {
                                    Long index=OpackCodec.asLong(params.get(key));
                                    log.log("[radio] multicast parameter "+key+"="+(index==null?"non-integer":index)
                                            +"; native multicast mapping not enabled");
                                }
                            }
                            if ("SXM".equals(source)) {
                                if (!"tuneToIdentifier".equals(name) || !(params.get("identifier") instanceof String)
                                        || !radio.tuneSxm((String)params.get("identifier")))
                                    log.log("[radio-sxm] tune refused: unsupported or unknown channel ID");
                                return;
                            }
                            if (source != null && !"FM".equals(source)) return;
                            Long khz = null;
                            if ("tuneToFrequency".equals(name)) khz = OpackCodec.asLong(params.get("frequency"));
                            else if (params.get("identifier") instanceof String) {
                                try { khz = Long.valueOf((String)params.get("identifier")); }
                                catch (NumberFormatException ignored) {}
                            }
                            if (khz == null || !("FM".equals(source) ? radio.selectAndTuneFm(khz) : radio.tuneFm(khz)))
                                log.log("[radio] tune refused: invalid frequency or inactive FM");
                            return;
                        }
                        if ("changeMediaSource".equals(name)) {
                            if (radio == null || !(value instanceof java.util.Map)) return;
                            Object source = ((java.util.Map<?,?>) value).get("identifier");
                            if ("FM".equals(source)) {
                                if (!radio.selectFm()) log.log("[radio] FM selection refused");
                            } else if ("SXM".equals(source)) {
                                if (!radio.selectSxm()) log.log("[radio] SXM selection refused");
                            } else {
                                radio.cancelPendingTune();
                                log.log("[radio] unsupported source selection");
                            }
                            return;
                        }
                        if (radio != null && radio.fmSelected()) {
                            if ("nextItem".equals(name)) { radio.seekFm(true); return; }
                            if ("previousItem".equals(name)) { radio.seekFm(false); return; }
                            // FM playback controls must not accidentally control the phone.
                            return;
                        }
                        if (radio != null && radio.sxmSelected()) {
                            log.log("[radio] SXM playback control withheld until native command mapping is verified");
                            return;
                        }
                        media.pressed(name, value);
                    }
                };
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
            if (uiSync) {
                boolean forceWallpaper = m.settingBoolean("ultra_force_wallpaper", false);
                m.log("[theme] center Wallpaper layout request=" + forceWallpaper);
                boolean phoneManagedCluster=m.settingBoolean("ultra_phone_managed_cluster",false);
                m.log("[theme] cluster layout policy="+(phoneManagedCluster?"phone-managed":"legacy Modular/saved"));
                new UiSyncChannel(dir, log, phoneManagedCluster?null:UiSync.layout("Modular"), forceWallpaper).start();
            }
        }
        MethodHook copy = new MethodHook() {
            @Override
            protected void afterHookedMethod(Param p) {
                sessionActive = false;
                if (media != null) media.ended(p.thisObject);
                VideoMessages.stopEngine("session finalized");
                sessionGeneration++;
                runtimeLog("[session] finalized");
                if(sessionLogging!=null) sessionLogging.finish("DiPO finalization observed; cause unknown");
            }
        };
        m.hook(app.classLoader, "dipo.carplay.session_started", new MethodHook() {
            @Override
            protected void afterHookedMethod(Param p) {
                sessionGeneration++;
                sessionActive = true;
                if(sessionLogging!=null) sessionLogging.start(sessionGeneration);
                if (media != null) media.started(p.thisObject);
                runtimeLog("[session] started generation=" + sessionGeneration);
                IapBridge.carPlay = p.thisObject;
                VideoUi.install(p.thisObject, app.classLoader);
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
                                        || !sessionActive || !VolumePanel.foreground(mediaContext)) return;
                                p.setResult(null);
                            } catch (Throwable ignored) { /* Fail open. */ }
                        }
                    });
                    m.log("[media-osd] exact K5 presentation hook installed (K5 integration)");
                }
            } catch (Throwable t) {
                m.log("[media-osd] hook unavailable; OEM retained: " + t);
            }
        }

        m.log("M4 in " + app.processName + "; restart-count guard removed");
    }

    @Override public void onEnabled(ModContext m, boolean enabled) {
        if (!enabled) { theme.unregister(m); sessionActive = false; sessionGeneration++; VideoMessages.stopEngine("mod disabled"); if(sessionLogging!=null) sessionLogging.finish("mod disabled"); }
        m.log("[runtime] enabled=" + enabled + "; restart required to change advertised capabilities");
    }

    /** Only PPS owns scale settings; CPUL is the native execution host when both are on.
     * Read the public state at process startup, not a stale PPS private file. */
    static void writeDelegatedScale(File dir, ModContext m) {
        int percent = 100;
        try {
            java.io.FileInputStream in = new java.io.FileInputStream("/data/hmgapi/public/state.json");
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try {
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > 4 * 1024 * 1024) throw new java.io.IOException("state too large");
                    out.write(buffer, 0, n);
                }
            } finally { in.close(); }
            org.json.JSONObject mods = new org.json.JSONObject(out.toString("UTF-8")).getJSONObject("mods");
            org.json.JSONObject pps = mods.optJSONObject("phone-projection-scaling");
            if (pps != null && pps.optBoolean("enabled", false)) {
                int candidate = Integer.parseInt(pps.getJSONObject("settings").optString("carplay_scale", "100"));
                if (candidate >= 50 && candidate <= 100 && candidate % 5 == 0) percent = candidate;
            }
        } catch (Exception e) { m.log("[scaling] optional provider unavailable; using stock dimensions"); }
        File file = new File(dir, "cpc-size");
        File tmp = new File(dir, "cpc-size.tmp");
        try {
            // Remove the RC4 hardcoded value before writing the new startup snapshot.
            if (file.exists() && !file.delete()) throw new java.io.IOException("cannot reset old scale");
            FileWriter writer = new FileWriter(tmp);
            try { writer.write(percent + "\n"); } finally { writer.close(); }
            if (!tmp.renameTo(file)) throw new java.io.IOException("scale rename failed");
            m.log("[scaling] hosted scale " + percent + "% (PPS settings only)");
        } catch (Exception e) {
            tmp.delete();
            file.delete();
            // If deletion failed, truncate the old value to stock if possible.
            if (file.exists()) try {
                FileWriter reset = new FileWriter(file);
                try { reset.write("100\n"); } finally { reset.close(); }
            } catch (Exception ignored) {}
            m.error("[scaling] cannot reset/write hosted scale; verify stock dimensions", e);
        }
    }

}
