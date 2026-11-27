package ioniq6.cpclimate;

import java.lang.reflect.Method;

/** Generation-consistent, redacted API v3 observations; never a startup gate. */
final class ThemeNegotiation {
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
