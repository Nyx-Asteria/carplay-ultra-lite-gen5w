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
        System.out.println("VideoSessionPolicyTest passed");
    }
}
