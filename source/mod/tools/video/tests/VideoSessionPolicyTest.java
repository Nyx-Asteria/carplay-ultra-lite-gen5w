package ioniq6.cpclimate.video;
public final class VideoSessionPolicyTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(VideoSessionPolicy.ignoreStop(3,5,5,true));
        check(VideoSessionPolicy.ignoreStop(3,5,-1,true));
        check(!VideoSessionPolicy.ignoreStop(3,5,3,true));
        check(!VideoSessionPolicy.ignoreStop(3,5,5,false));
        check(!VideoSessionPolicy.ignoreStop(5,5,5,true));
        check(!VideoSessionPolicy.ignoreStop(-1,5,5,true));
        check(VideoSessionPolicy.videoWorker("ExoPlayer:Playback"));
        check(VideoSessionPolicy.videoWorker("cp-climate-tls"));
        check(VideoSessionPolicy.videoWorker("cp-climate-proxy-tls"));
        check(VideoSessionPolicy.videoWorker("cp-climate-fetch-1"));
        check(!VideoSessionPolicy.videoWorker("cp-climate-car-send"));
        check(!VideoSessionPolicy.videoWorker("cp-climate-video-return"));
        check(!VideoSessionPolicy.videoWorker(null));
        check(VideoSessionPolicy.ownsFailure(5,5,5));
        check(!VideoSessionPolicy.ownsFailure(3,5,5));
        check(!VideoSessionPolicy.ownsFailure(3,5,3));
        check(!VideoSessionPolicy.ownsFailure(-1,-1,-1));
        System.out.println("VideoSessionPolicyTest passed");
    }
}
