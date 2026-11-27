package ioniq6.cpclimate;
public final class FmSeekTraceTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        FmSeekTrace t=new FmSeekTrace();
        check(t.begin(1,2,99100,100)==null);
        check(t.observe(1,2,2,38,99100,200)==null);
        check(t.observe(1,2,2,38,99300,300).startsWith("frequency_changed"));
        check(t.observe(1,2,2,38,99500,400)==null);
        t.begin(1,3,99300,1000);
        check(t.observe(1,3,2,38,99300,6000).startsWith("frequency_unchanged"));
        t.begin(1,4,99300,7000);
        check(t.observe(1,4,-1,-1,-1,12000).startsWith("observation_incomplete"));
        t.begin(1,5,99300,13000);
        check(t.observe(1,5,4,41,99300,13100).startsWith("ownership_lost"));
        t.begin(1,6,99300,14000);
        check(t.observe(2,6,2,38,99500,14100).startsWith("superseded"));
        t.begin(2,7,99500,15000);
        check(t.observe(2,8,2,38,99700,15100).startsWith("superseded"));
        t.begin(2,8,99500,16000);
        check(t.cancel().startsWith("session ended")&&t.cancel()==null);
        System.out.println("FmSeekTraceTest passed");
    }
}
