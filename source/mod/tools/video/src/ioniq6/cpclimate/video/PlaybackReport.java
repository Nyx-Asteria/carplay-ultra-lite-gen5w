package ioniq6.cpclimate.video;

/** Shared factual playback snapshot for both existing wire envelopes. */
final class PlaybackReport {
    final boolean ready,keepUp,seekable;
    final long position,duration,buffered;
    final double rate;
    final String state;
    PlaybackReport(boolean valid,boolean prepared,boolean ended,boolean wanted,boolean playing,
                   boolean canSeek,long position,long duration,long buffered){
        ready=valid&&(prepared||ended);
        keepUp=valid&&prepared&&!ended;
        this.position=valid?Math.max(0,position):0;
        this.duration=valid?Math.max(0,duration):0;
        this.buffered=valid?Math.max(this.position,buffered):0;
        seekable=valid&&canSeek&&this.duration>0;
        // Keep intent through buffering; never report an ended/failed item as playing.
        rate=valid&&wanted&&!ended?1.0:0.0;
        state=!ready?"loading":playing&&!ended?"playing":"paused";
    }
}