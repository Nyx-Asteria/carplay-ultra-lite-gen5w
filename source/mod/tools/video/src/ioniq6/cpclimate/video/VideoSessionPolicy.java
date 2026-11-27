package ioniq6.cpclimate.video;

/** Do not let a queued teardown of an older session close a newer authorized session.
 * Authorization loss always wins, even when generation information is missing. */
final class VideoSessionPolicy {
    static boolean ignoreStop(long requested,long current,long owner,boolean allowed) {
        return allowed && requested>=0 && current>requested && (owner<0 || owner==current);
    }
}
