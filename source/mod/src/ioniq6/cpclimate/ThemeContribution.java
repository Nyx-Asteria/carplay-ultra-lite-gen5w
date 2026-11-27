package ioniq6.cpclimate;

import hmgapi.hook.ModContext;
import java.lang.reflect.Method;

/**
 * Theme-only consumer of Open-HMG wireless CarPlay's shared identity API v2.
 * No EV identity, native recipe, or copy of the provider API is shipped here.
 * Registry acceptance is not proof that native identification was armed.
 */
final class ThemeContribution {
    private static final String PROVIDER = "wireless-carplay-aa";
    private static final String API = "hmgapi.mods.wireless.api.CarPlayIdentification";
    private static final String SERVICE = "carplay-identification";
    private final ThemeOwnership ownership = new ThemeOwnership();
    private Method remove;

    boolean available(ModContext mod) {
        try {
            Object service = mod.use(PROVIDER, SERVICE);
            if (service == null) { mod.log("[theme] provider not published"); return false; }
            Class<?> api = Class.forName(API, false, service.getClass().getClassLoader());
            return api.isInstance(service) && api.getField("API_VERSION").getInt(null) == 2;
        } catch (Throwable t) { mod.error("[theme] capability probe failed", t); return false; }
    }

    synchronized boolean register(ModContext mod) {
        try {
            Object service = mod.use(PROVIDER, SERVICE);
            if (service == null) { mod.log("[theme] shared identity service unavailable"); return false; }
            if (ownership.confirmed(service)) return true;
            if (ownership.owner() != null && !unregister(mod)) return false;
            Class<?> api = Class.forName(API, false, service.getClass().getClassLoader());
            if (!api.isInstance(service) || api.getField("API_VERSION").getInt(null) != 2) { mod.log("[theme] incompatible shared identity API"); return false; }
            Class<?> theme = Class.forName(API + "$ThemeIdentity", false, api.getClassLoader());
            Object value = theme.getConstructor(String.class, long.class, String.class)
                    .newInstance("44C9FA31EC", 985L, "19.0");
            Method unregister = api.getMethod("removeTheme", String.class);
            boolean start = true; // contributed only when Ultra and Sidebars are enabled
            Method registration = null;
            try { registration = api.getMethod("registerTheme", String.class, theme, boolean.class); }
            catch (NoSuchMethodException stock) { /* original v2 provider */ }
            // The provider stores the contribution before invoking its change callback.
            // Retain withdrawal ownership even if that callback throws after insertion.
            ownership.begin(service);
            remove = unregister;
            try {
            if (registration != null) {
                registration.invoke(service, mod.id, value, start);
                mod.log("[theme] CarPlayStartSession requested=" + start + " (provider native result unverified)");
            } else {
                api.getMethod("registerTheme", String.class, theme).invoke(service, mod.id, value);
                if (start) mod.log("[theme] CarPlayStartSession unavailable: provider extension not installed; identification only");
            }
            } catch (Exception e) {
                unregister(mod);
                throw e;
            } catch (LinkageError e) {
                unregister(mod);
                throw e;
            }
            ownership.confirm();
            mod.log("Ultra theme contribution accepted by registry (native activation unverified)");
            return true;
        } catch (Exception e) {
            mod.error("Ultra theme contribution unavailable", e);
            return false;
        } catch (LinkageError e) {
            mod.error("Ultra theme API linkage unavailable", e);
            return false;
        }
    }

    synchronized boolean unregister(ModContext mod) {
        if (ownership.owner() == null) return true;
        ownership.uncertain();
        try {
            remove.invoke(ownership.owner(), mod.id);
            ownership.clear();
            remove = null;
            mod.log("[theme] contribution withdrawn");
            return true;
        } catch (Exception e) {
            mod.error("Ultra theme withdrawal failed", e);
            return false;
        }
    }
}
