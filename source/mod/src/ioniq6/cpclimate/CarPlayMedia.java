package ioniq6.cpclimate;

import java.lang.reflect.Method;

/**
 * Forward explicit media-button actions through DiPO's CarPlayService.
 * Connecting only attaches the receiver: no UI request, autoplay or source selection.
 */
final class CarPlayMedia implements VehicleState.Controls {
    static final long RELEASE_DELAY_MS = 100;

    interface Later {
        void post(Runnable action, long delayMs);
    }

    private final Later later;
    private final VsChannel.Log out;
    private Session current;
    private final hmgapi.hook.ModContext mod;

    private static final class Session {
        final Object service;
        final long generation;
        final Method onConsumer;
        final Class<?> consumer;

        Session(Object service,long generation) throws Exception {
            this.service = service;
            this.generation=generation;
            Method found = null;
            for (Method m : service.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (!"onConsumer".equals(m.getName()) || p.length != 2 || !p[0].isEnum() || p[1] != boolean.class) continue;
                if (found != null) throw new IllegalStateException("ambiguous onConsumer signature");
                found = m;
            }
            if (found == null) throw new NoSuchMethodException("onConsumer(enum, boolean)");
            onConsumer = found;
            consumer = found.getParameterTypes()[0];
        }

        Object button(String name) throws Exception {
            Object value = consumer.getField(name).get(null);
            if (!consumer.isInstance(value)) throw new IllegalStateException("invalid consumer " + name);
            return value;
        }
    }

    CarPlayMedia(Later later, VsChannel.Log out, hmgapi.hook.ModContext mod) {
        this.mod = mod;
        this.later = later;
        this.out = out;
    }

    synchronized void started(Object service) {
        long generation=CpClimate.sessionGeneration;
        if (current != null && current.service == service && current.generation==generation) return;
        current = null;
        final Session session;
        try {
            session = new Session(service,generation);
        } catch (Throwable t) {
            out.log("[media] controls unavailable: " + t);
            return;
        }
        current = session;
        out.log("[media] CarPlay connected; manual media controls attached");
    }

    synchronized void ended(Object service) {
        if (current == null || current.service != service) return;
        current = null;
        out.log("[media] CarPlay ended; manual media controls detached");
    }

    static boolean sessionCurrent(boolean sameSession,long ownerGeneration,long activeGeneration,boolean active){
        return sameSession && active && ownerGeneration==activeGeneration;
    }
    private synchronized boolean owns(Session session){
        return sessionCurrent(current==session,session.generation,CpClimate.sessionGeneration,CpClimate.sessionActive);
    }
    private void press(final Session session, final String name) {
        final Object button;
        synchronized (this) {
            if (!owns(session) || !mod.enabled() || !mod.settingBoolean("k5_integration", true) || !CpClimate.sessionActive) return;
        }
        try {
            button = session.button(name);
        } catch (Throwable t) {
            out.log("[media] " + name + " unavailable: " + t);
            return;
        }
        if(!owns(session))return;
        // Always pair a press with a release within the same connection, even when invocation reports an error.
        try {
            out.log("[media] " + name + " down: " + session.onConsumer.invoke(session.service, button, true));
        } catch (Throwable t) {
            out.log("[media] " + name + " down failed: " + t);
        } finally {
            later.post(new Runnable() {
                @Override public void run() {
                    synchronized (CarPlayMedia.this) {
                        if (!owns(session)) return; // never release into a new connection
                    }
                    try {
                        out.log("[media] " + name + " up: " + session.onConsumer.invoke(session.service, button, false));
                    } catch (Throwable t) {
                        out.log("[media] " + name + " up failed: " + t);
                    }
                }
            }, RELEASE_DELAY_MS);
        }
    }

    @Override public void pressed(String name, Object value) {
        if (!mod.enabled() || !mod.settingBoolean("k5_integration", true) || !CpClimate.sessionActive) return;
        final String button = "play".equals(name) ? "Play" : "pause".equals(name) ? "Pause"
                : "nextItem".equals(name) ? "ScanNextTrack" : "previousItem".equals(name) ? "ScanPreviousTrack" : null;
        if (button == null) return;
        final Session session;
        synchronized (this) {
            session = current;
        }
        if (session == null || !owns(session)) return;
        later.post(new Runnable() {
            @Override public void run() { press(session, button); }
        }, 0);
    }
}
