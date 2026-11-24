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
    private Object registered;
    private Method remove;

    synchronized boolean register(ModContext mod) {
        if (registered != null) return true;
        try {
            Object service = mod.use(PROVIDER, SERVICE);
            if (service == null) { mod.log("[theme] shared identity service unavailable"); return false; }
            Class<?> api = Class.forName(API, false, service.getClass().getClassLoader());
            if (!api.isInstance(service) || api.getField("API_VERSION").getInt(null) != 2) { mod.log("[theme] incompatible shared identity API"); return false; }
            Class<?> theme = Class.forName(API + "$ThemeIdentity", false, api.getClassLoader());
            Object value = theme.getConstructor(String.class, long.class, String.class)
                    .newInstance("44C9FA31EC", 985L, "19.0");
            Method unregister = api.getMethod("removeTheme", String.class);
            api.getMethod("registerTheme", String.class, theme).invoke(service, mod.id, value);
            registered = service;
            remove = unregister;
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
        if (registered == null) return true;
        try {
            remove.invoke(registered, mod.id);
            registered = null;
            remove = null;
            mod.log("[theme] contribution withdrawn");
            return true;
        } catch (Exception e) {
            mod.error("Ultra theme withdrawal failed", e);
            return false;
        }
    }
}
