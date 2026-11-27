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
            if (!api.isInstance(service) || api.getField("API_VERSION").getInt(null) != 3) return false;
            Class<?> theme = Class.forName(API + "$ThemeIdentity", false, api.getClassLoader());
            ThemeNegotiation.verifyContract(api, theme);
            return true;
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
            ThemeNegotiation.verifyContract(api, theme);
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
                long lastSnapshot = android.os.SystemClock.elapsedRealtime();
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        if (!mod.enabled() || !mod.settingBoolean("ultra_theme", true)
                                || !mod.settingBoolean("ultra_sidebars", true)) {
                            synchronized (ThemeContribution.this) {
                                if (observer != Thread.currentThread()) return;
                                boolean withdrawn = unregister(mod);
                                String status = withdrawn ? "contribution_withdrawn" : "withdrawal_pending";
                                if (!status.equals(last)) {
                                    mod.log("[theme] negotiation " + status);
                                    android.util.Log.i("CPULCrossProcess", "[theme] negotiation " + status);
                                    last = status;
                                }
                                previous = null;
                            }
                            Thread.sleep(2000);
                            continue;
                        }
                        Object service;
                        try {
                            service = mod.use(PROVIDER, SERVICE);
                        } catch (RuntimeException e) {
                            reportLookupFailure(mod);
                            Thread.sleep(2000);
                            continue;
                        } catch (LinkageError e) {
                            reportLookupFailure(mod);
                            Thread.sleep(2000);
                            continue;
                        }
                        synchronized (ThemeContribution.this) {
                            if (observer != Thread.currentThread()) return;
                            if (service == null) unregister(mod);
                            else if (!ownership.confirmed(service)) register(mod);
                        }
                        boolean owned;
                        synchronized (ThemeContribution.this) {
                            if (observer != Thread.currentThread()) return;
                            owned = service != null && ownership.confirmed(service);
                        }
                        String next = service == null ? "unavailable" : "contribution_unconfirmed";
                        if (owned) {
                            try {
                                Class<?> api = Class.forName(API, false, service.getClass().getClassLoader());
                                if (api.isInstance(service) && api.getField("API_VERSION").getInt(null) == 3)
                                    next = ThemeNegotiation.sample(api, service);
                            } catch (Exception e) { next = "observation_failed"; }
                            catch (LinkageError e) { next = "observation_failed"; }
                        }
                        try {
                            if (service != mod.use(PROVIDER, SERVICE)) { service = null; next = "provider_changed"; }
                        } catch (RuntimeException e) { service = null; next = "observation_failed"; }
                        catch (LinkageError e) { service = null; next = "observation_failed"; }
                        // A late-starting DiPO reader may miss the initial transition.
                        // Periodically emit a fresh observation, never replay old evidence.
                        synchronized (ThemeContribution.this) {
                            if (observer != Thread.currentThread()) return;
                            // Do not publish a sample after eligibility or ownership was lost
                            // during the reflective calls. Never treat builder evidence as acceptance.
                            if (!mod.enabled() || !mod.settingBoolean("ultra_theme", true)
                                    || !mod.settingBoolean("ultra_sidebars", true)) {
                                next = unregister(mod) ? "contribution_withdrawn" : "withdrawal_pending";
                                service = null;
                            } else if (owned && service != null && !ownership.confirmed(service)) {
                                next = "contribution_unconfirmed";
                            }
                            long now = android.os.SystemClock.elapsedRealtime();
                            if (service != previous || !next.equals(last)) {
                                mod.log("[theme] negotiation " + next);
                                android.util.Log.i("CPULCrossProcess", "[theme] negotiation " + next);
                                previous = service;
                                last = next;
                                lastSnapshot = now;
                            } else if (now - lastSnapshot >= 10000) {
                                android.util.Log.i("CPULCrossProcess", "[theme] negotiation snapshot " + next);
                                lastSnapshot = now;
                            }
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

    private long lastLookupFailure = -10000;
    private synchronized void reportLookupFailure(ModContext mod) {
        if (observer != Thread.currentThread()) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastLookupFailure < 10000) return;
        lastLookupFailure = now;
        mod.log("[theme] negotiation observation_failed");
        android.util.Log.i("CPULCrossProcess", "[theme] negotiation observation_failed");
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
        } catch (LinkageError e) {
            mod.error("Ultra theme withdrawal linkage unavailable", e);
            return false;
        }
    }
}
