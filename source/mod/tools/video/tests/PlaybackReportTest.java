package ioniq6.cpclimate.video;
public final class PlaybackReportTest {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        PlaybackReport p=new PlaybackReport(true,false,false,true,false,false,10,-1,5);
        check(p.rate==1&&!p.ready&&!p.keepUp&&!p.seekable&&p.duration==0&&p.buffered==10);
        p=new PlaybackReport(true,true,false,true,true,true,100,1000,300);
        check(p.ready&&p.keepUp&&p.seekable&&p.rate==1&&p.state.equals("playing"));
        p=new PlaybackReport(true,false,true,true,false,true,1000,1000,1000);
        check(p.ready&&!p.keepUp&&p.rate==0&&p.state.equals("paused"));
        p=new PlaybackReport(false,true,false,true,true,true,100,1000,300);
        check(!p.ready&&!p.seekable&&p.rate==0&&p.position==0&&p.duration==0);
        System.out.println("PlaybackReportTest passed");
    }
}