package ioniq6.cpclimate;

import hmgapi.hook.ModContext;
import java.io.File;
import java.io.FileWriter;

/** Startup configuration consumed by the firmware-specific native CarPlay hooks. */
final class NativeProfile {
    private NativeProfile() {}

    // Keep the existing on-disk format and caller-specific failure handling.
    private static void writeValue(File file, String value) throws java.io.IOException {
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(value);
        } finally {
            writer.close();
        }
        CpClimate.runtimeLog("[native-profile] " + file.getName() + " written value=" + value.trim());
    }

    private static boolean clearValue(File file) {
        boolean removed = !file.exists() || file.delete();
        CpClimate.runtimeLog("[native-profile] " + file.getName() + " disabled; clear=" + (removed ? "ok" : "failed"));
        return removed;
    }

    private static void writeFailed(File file, Exception error) {
        CpClimate.runtimeLog("[native-profile] " + file.getName() + " write failed: " + error.getClass().getSimpleName());
        clearValue(file);
    }

    /** the dock position setting, for the native /info hook (files/cpc-dock: 0 auto, 1 bottom, 2 side) */
    static void writeDock(ModContext mod, File dir, String dock) {
        File f = new File(dir, "cpc-dock");
        try {
            int edge = Integer.parseInt(dock.trim());
            if (edge < 0 || edge > 4) edge = 1;
            writeValue(f, edge + "\n");
            CpClimate.runtimeLog("CarPlay dock edge " + edge);
        } catch (Exception e) {
            writeFailed(f,e);
            CpClimate.runtimeLog("CarPlay dock: bottom (" + e + ")");
        }
    }

    /** the receiver version experiment, for the native /info hook (files/cpc-version: 1 for 950.7.1) */
    static void writeVersion(ModContext mod, File dir, String version) {
        File f = new File(dir, "cpc-version");
        if (!"950".equals(version.trim())) {
            clearValue(f);
            return;
        }
        try {
            writeValue(f, "1\n");
            CpClimate.runtimeLog("CarPlay receiver version 950.7.1 in /info (experiment)");
        } catch (Exception e) {
            writeFailed(f,e);
        }
    }

    /** an on/off experiment for the native /info hook: the file holds 1 while on */
    static boolean writeFlag(ModContext mod, File dir, String name, boolean on, String what) {
        return writeNumber(mod, dir, name, on ? 1 : 0, what);
    }

    /** LIVI's /info feature bits that DiPO's lacks, without its audio ones (CARPLAY_AUDIO_FEATURES: bits 9, 11, 18, 20, 22, 26, 40) */
    static final long LIVI_NON_AUDIO = 0x205203a462L;
    /** all of LIVI's bits that DiPO's lacks, audio ones too */
    static final long LIVI_ALL = 0x615653aee2L & ~0x4104040280L;

    /** the /info feature bits experiment (files/cpc-feat, hex): livi_nonaudio, livi_all or bit_N; off deletes it */
    static void writeFeatures(ModContext mod, File dir, String value) {
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
            clearValue(f);
            return;
        }
        try {
            writeValue(f, Long.toHexString(bits) + "\n");
            CpClimate.runtimeLog("/info feature bits 0x" + Long.toHexString(bits) + " (" + v + ") on (experiment)");
        } catch (Exception e) {
            writeFailed(f,e);
        }
    }

    /** a small number for the native hooks; 0 deletes the file */
    static boolean writeNumber(ModContext mod, File dir, String name, int value, String what) {
        File f = new File(dir, name);
        if (value == 0) {
            return clearValue(f);
        }
        try {
            writeValue(f, value + "\n");
            CpClimate.runtimeLog(what + " on (experiment)");
            return true;
        } catch (Exception e) {
            writeFailed(f,e);
            return false;
        }
    }

    /** the Ultra experiment for the native hooks (files/cpc-ultra: 1 virtual cluster, 2 with the theme's display ids) */
    static boolean writeUltra(ModContext mod, File dir, int mode) {
        File f = new File(dir, "cpc-ultra");
        if (mode == 0) {
            return clearValue(f);
        }
        try {
            writeValue(f, mode + "\n");
            CpClimate.runtimeLog("CarPlay Ultra virtual cluster on" + (mode == 2 ? ", with the theme's display ids" : "") + " (experiment)");
            return true;
        } catch (Exception e) {
            writeFailed(f,e);
            return false;
        }
    }

    /** the Apple widescreen view areas experiment (files/cpc-wide: 1 start whole, 2 start right) */
    static String wideMode(String value) {
        return "1".equals(value) || "2".equals(value) ? value : "0";
    }
    static boolean writeWide(ModContext mod, File dir, String value) {
        File f = new File(dir, "cpc-wide");
        String v = wideMode(value);
        if (!"1".equals(v) && !"2".equals(v)) {
            return clearValue(f);
        }
        try {
            writeValue(f, v + "\n");
            CpClimate.runtimeLog("Apple widescreen view areas, start " + ("2".equals(v) ? "right" : "whole") + " (experiment)");
            return true;
        } catch (Exception e) {
            writeFailed(f,e);
            return false;
        }
    }

    /** the instrument panel experiment (files/cpc-nocluster: 1 off, 2 no showsInstruments, 3 no plugin) */
    static void writeCluster(ModContext mod, File dir, Object value) {
        File f = new File(dir, "cpc-nocluster");
        String v = value == null ? "full" : String.valueOf(value);
        // an older version saved this setting as a toggle: false meant off
        int code = "off".equals(v) || "false".equals(v) ? 1 : "no_instruments".equals(v) ? 2 : "no_plugin".equals(v) ? 3
                : "plugin_main".equals(v) ? 4 : "plugin_stream".equals(v) ? 5 : 0;
        if (code == 0) {
            clearValue(f);
            return;
        }
        try {
            writeValue(f, code + "\n");
            CpClimate.runtimeLog("instrument panel: " + v + " (experiment)");
        } catch (Exception e) {
            writeFailed(f,e);
        }
    }

}
