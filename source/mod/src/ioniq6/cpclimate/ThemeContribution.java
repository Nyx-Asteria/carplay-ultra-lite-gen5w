package ioniq6.cpclimate;

import hmgapi.hook.ModContext;
import java.lang.reflect.Method;

/**
 * Theme-only consumer of Open-HMG wireless CarPlay's shared identity API v3.
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
            return api.isInstance(service) && api.getField("API_VERSION").getInt(null) == 3;
        } catch (Throwable t) { mod.error("[theme] capability probe failed", t); return false; }
    }

    synchronized boolean register(ModContext mod) {
        try {
            Object service = mod.use(PROVIDER, SERVICE);
            if (service == null) { mod.log("[theme] shared identity service unavailable"); return false; }
            if (ownership.confirmed(service)) return true;
            if (ownership.owner() != null && !unregister(mod)) return false;
            Class<?> api = Class.forName(API, false, service.getClass().getClassLoader());
            if (!api.isInstance(service) || api.getField("API_VERSION").getInt(null) != 3) { mod.log("[theme] incompatible shared identity API"); return false; }
            Class<?> theme = Class.forName(API + "$ThemeIdentity", false, api.getClassLoader());
            Object value = theme.getConstructor(String.class, long.class, String.class)
                    .newInstance("44C9FA31EC", 985L, "19.0");
            Method unregister = api.getMethod("removeTheme", String.class);
            boolean start = true; // contributed only when Ultra and Sidebars are enabled
            Method registration = api.getMethod("registerTheme", String.class, theme, boolean.class);
            // The provider stores the contribution before invoking its change callback.
            // Retain withdrawal ownership even if that callback throws after insertion.
            ownership.begin(service);
            remove = unregister;
            try {
                registration.invoke(service, mod.id, value, start);
                mod.log("[theme] CarPlayStartSession requested=" + start + " (provider native result unverified)");
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

    private Thread observer;
    synchronized void observe(final ModContext mod) {
        if (observer != null) return;
        observer = new Thread(new Runnable() {
            public void run() {
                String last = null;
                Object previous = null;
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (!mod.enabled() || !mod.settingBoolean("ultra_theme", true)
                                || !mod.settingBoolean("ultra_sidebars", true)) {
                            synchronized (ThemeContribution.this) {
                                if (observer != Thread.currentThread()) return;
                                unregister(mod);
                            }
                            Thread.sleep(2000);
                            continue;
                        }
                        Object service = mod.use(PROVIDER, SERVICE);
                        synchronized (ThemeContribution.this) {
                            if (observer != Thread.currentThread()) return;
                            if (service == null) unregister(mod);
                            else if (!ownership.confirmed(service)) register(mod);
                        }
                        String next = "unavailable";
                        if (service != null) {
                            try {
                                Class<?> api = Class.forName(API, false, service.getClass().getClassLoader());
                                if (api.isInstance(service) && api.getField("API_VERSION").getInt(null) == 3)
                                    next = ThemeNegotiation.sample(api, service);
                            } catch (Exception e) { next = "observation_failed"; }
                            catch (LinkageError e) { next = "observation_failed"; }
                        }
                        if (service != mod.use(PROVIDER, SERVICE)) { service = null; next = "provider_changed"; }
                        if (service != previous || !next.equals(last)) {
                            mod.log("[theme] negotiation " + next);
                            previous = service;
                            last = next;
                        }
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally {
                    synchronized (ThemeContribution.this) {
                        if (observer == Thread.currentThread()) observer = null;
                    }
                }
            }
        }, "cpul-theme-evidence");
        observer.setDaemon(true);
        observer.start();
    }

    synchronized void stopObservation() {
        if (observer != null) { observer.interrupt(); observer = null; }
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
