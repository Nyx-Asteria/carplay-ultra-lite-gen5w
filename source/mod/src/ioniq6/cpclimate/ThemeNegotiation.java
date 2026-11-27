package ioniq6.cpclimate;

import java.lang.reflect.Method;

/** Generation-consistent, redacted API v3 observations; never a startup gate. */
final class ThemeNegotiation {
    /** Check API shape without invoking provider code or gating on native readiness. */
    static void verifyContract(Class<?> api, Class<?> theme) throws Exception {
        theme.getConstructor(String.class, long.class, String.class);
        api.getMethod("registerTheme", String.class, theme, boolean.class);
        api.getMethod("removeTheme", String.class);
        Class<?> identity = api.getMethod("identityStatus").getReturnType();
        Class<?> evidence = api.getMethod("negotiationStatus").getReturnType();
        requireField(identity, "generation", long.class);
        if (!identity.getField("readiness").getType().isEnum())
            throw new NoSuchFieldException("readiness enum");
        requireField(evidence, "generation", long.class);
        for (String field : new String[]{"vehicleBuilt","themeBuilt","startRequested","startArmed","startSendCapability","startReceiveCapability"})
            requireField(evidence, field, boolean.class);
    }

    private static void requireField(Class<?> type, String name, Class<?> expected) throws Exception {
        if (type.getField(name).getType() != expected) throw new NoSuchFieldException(name);
    }

    static String sample(Class<?> api, Object service) throws Exception {
        Method identity = api.getMethod("identityStatus");
        Object before = identity.invoke(service);
        Object evidence = api.getMethod("negotiationStatus").invoke(service);
        Object after = identity.invoke(service);
        if (before == null || evidence == null || after == null) return "unavailable";
        long generation = generation(before);
        if (generation != generation(evidence) || generation != generation(after)) return "generation_changed";
        String state = ((Enum<?>)after.getClass().getField("readiness").get(after)).name();
        String prior = ((Enum<?>)before.getClass().getField("readiness").get(before)).name();
        if (!state.equals(prior)) return "readiness_changed";
        StringBuilder b = new StringBuilder("generation=").append(generation).append(" readiness=").append(state);
        boolean valid = "ARMED".equals(state) || "ADVERTISED".equals(state);
        for (String field : new String[]{"vehicleBuilt","themeBuilt","startRequested","startArmed","startSendCapability","startReceiveCapability"}) {
            boolean value = evidence.getClass().getField(field).getBoolean(evidence);
            b.append(' ').append(field).append('=').append(("startRequested".equals(field) || valid) && value);
        }
        return b.toString();
    }
    private static long generation(Object value) throws Exception {
        return value.getClass().getField("generation").getLong(value);
    }
}
