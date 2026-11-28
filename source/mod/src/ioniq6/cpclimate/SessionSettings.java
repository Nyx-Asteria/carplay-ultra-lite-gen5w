package ioniq6.cpclimate;

import hmgapi.hook.ModContext;

/** Restart-scoped settings recorded in each session header. Keep defaults aligned with settings.json. */
final class SessionSettings {
    private SessionSettings() {}
    static final String[] KEYS = {
        "k5_integration",
        "seat_heat_immediate_feedback",
        "legacy_seat_heat_arbitration",
        "seat_fan_immediate_feedback",
        "ultra_theme",
        "ultra_sidebars",
        "ultra_ui_sync",
        "ultra_independent_sync",
        "sxm_schema_test",
        "parked_video",
        "parked_video_picture_only",
        "session_logging",
        "session_usb_export",
        "diagnostic_detail",
        "diagnostic_packets"
    };
    static final boolean[] DEFAULTS = {
        true,
        true,
        false,
        false,
        true,
        true,
        false,
        false,
        false,
        false,
        false,
        true,
        true,
        true,
        false
    };
    static final String WIDE_KEY="ultra_widescreen_areas";
    static final String WIDE_DEFAULT="0";
    static String widescreen(ModContext mod) {
        return NativeProfile.wideMode(mod.settingString(WIDE_KEY,WIDE_DEFAULT));
    }
    static boolean read(ModContext mod, String key) {
        for (int i=0; i<KEYS.length; i++) if (KEYS[i].equals(key)) return mod.settingBoolean(key, DEFAULTS[i]);
        throw new IllegalArgumentException("Unknown session setting: " + key);
    }
}
