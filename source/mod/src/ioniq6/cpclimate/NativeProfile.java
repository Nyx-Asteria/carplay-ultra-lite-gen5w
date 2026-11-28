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
    }

    /** the dock position setting, for the native /info hook (files/cpc-dock: 0 auto, 1 bottom, 2 side) */
    static void writeDock(ModContext mod, File dir, String dock) {
        File f = new File(dir, "cpc-dock");
        try {
            int edge = Integer.parseInt(dock.trim());
            if (edge < 0 || edge > 4) edge = 1;
            writeValue(f, edge + "\n");
            mod.log("CarPlay dock edge " + edge);
        } catch (Exception e) {
            f.delete();
            mod.log("CarPlay dock: bottom (" + e + ")");
        }
    }

    /** the receiver version experiment, for the native /info hook (files/cpc-version: 1 for 950.7.1) */
    static void writeVersion(ModContext mod, File dir, String version) {
        File f = new File(dir, "cpc-version");
        if (!"950".equals(version.trim())) {
            f.delete();
            return;
        }
        try {
            writeValue(f, "1\n");
            mod.log("CarPlay receiver version 950.7.1 in /info (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** an on/off experiment for the native /info hook: the file holds 1 while on */
    static void writeFlag(ModContext mod, File dir, String name, boolean on, String what) {
        writeNumber(mod, dir, name, on ? 1 : 0, what);
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
            f.delete();
            return;
        }
        try {
            writeValue(f, Long.toHexString(bits) + "\n");
            mod.log("/info feature bits 0x" + Long.toHexString(bits) + " (" + v + ") on (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** a small number for the native hooks; 0 deletes the file */
    static void writeNumber(ModContext mod, File dir, String name, int value, String what) {
        File f = new File(dir, name);
        if (value == 0) {
            f.delete();
            return;
        }
        try {
            writeValue(f, value + "\n");
            mod.log(what + " on (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** the Ultra experiment for the native hooks (files/cpc-ultra: 1 virtual cluster, 2 with the theme's display ids) */
    static void writeUltra(ModContext mod, File dir, int mode) {
        File f = new File(dir, "cpc-ultra");
        if (mode == 0) {
            f.delete();
            return;
        }
        try {
            writeValue(f, mode + "\n");
            mod.log("CarPlay Ultra virtual cluster on" + (mode == 2 ? ", with the theme's display ids" : "") + " (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

    /** the Apple widescreen view areas experiment (files/cpc-wide: 1 start whole, 2 start right) */
    static void writeWide(ModContext mod, File dir, String value) {
        File f = new File(dir, "cpc-wide");
        String v = value.trim();
        if (!"1".equals(v) && !"2".equals(v)) {
            f.delete();
            return;
        }
        try {
            writeValue(f, v + "\n");
            mod.log("Apple widescreen view areas, start " + ("2".equals(v) ? "right" : "whole") + " (experiment)");
        } catch (Exception e) {
            f.delete();
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
            f.delete();
            return;
        }
        try {
            writeValue(f, code + "\n");
            mod.log("instrument panel: " + v + " (experiment)");
        } catch (Exception e) {
            f.delete();
        }
    }

}
