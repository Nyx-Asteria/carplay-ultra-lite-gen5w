package ioniq6.cpclimate;
final class VolumePanelPolicy {
    static final long LEASE_MS = 2500L;
    static boolean fresh(long now, long issued) {
        return issued >= 0 && now >= issued && now - issued < LEASE_MS;
    }
    static boolean keyEventCurrent(long issued,long now,long sessionStart){
        return sessionStart>=0 && issued>=sessionStart && fresh(now,issued);
    }
    static boolean eligible(boolean enabled, boolean integration, boolean setting, long until, long now) {
        return enabled && integration && setting && now >= 0 && until > now && until - now <= LEASE_MS;
    }
    static boolean suppress(boolean enabled, boolean integration, boolean setting, long until, long now,
                            boolean connected, boolean foreground) {
        return eligible(enabled, integration, setting, until, now) && connected && foreground;
    }
}
