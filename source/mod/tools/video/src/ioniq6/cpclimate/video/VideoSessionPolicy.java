package ioniq6.cpclimate.video;

/** Do not let a queued teardown of an older session close a newer authorized session.
 * Authorization loss always wins, even when generation information is missing. */
final class VideoSessionPolicy {
    static boolean videoWorker(String name) {
        return name != null && (name.startsWith("ExoPlayer") || name.equals("cp-climate-tls")
                || name.equals("cp-climate-proxy-tls") || name.startsWith("cp-climate-fetch-"));
    }
    static boolean ownsFailure(long failed, long current, long owner) {
        return failed >= 0 && failed == current && failed == owner;
    }
    static boolean ignoreStop(long requested,long current,long owner,boolean allowed) {
        return allowed && requested>=0 && current>requested && (owner<0 || owner==current);
    }
}
