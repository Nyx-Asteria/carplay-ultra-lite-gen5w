package ioniq6.cpclimate;
public final class RadioActivationTraceTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        RadioActivationTrace t=new RadioActivationTrace();
        check(t.begin(1,1,2,0).startsWith("requested"));
        check(t.result(1,1,true,10).startsWith("accepted"));
        check(t.sample(1,1,2,38,100).startsWith("acquired"));
        check(t.sample(1,1,2,38,200)==null);
        check(t.sample(1,1,0,14,400).startsWith("ownership_lost"));
        check(t.sample(1,1,2,38,500)==null);
        t.begin(1,2,13,1000);t.result(1,2,true,1010);
        check(t.sample(1,2,4,41,1100).startsWith("acquired"));
        check(t.sample(1,2,4,41,2200).startsWith("sustained"));
        check(t.sample(1,2,4,41,9000).startsWith("observation_complete"));
        t.begin(1,3,2,10000);t.result(1,3,true,10010);
        check(t.sample(1,3,0,14,18000).startsWith("accepted_without_acquisition"));
        t.begin(1,4,2,20000);
        check(t.sample(2,4,2,38,20010).startsWith("superseded"));
        t.begin(2,5,2,21000);
        check(t.result(2,5,false,21010).startsWith("rejected"));
        check(t.sample(2,5,2,38,22000)==null);
        System.out.println("RadioActivationTraceTest passed");
    }
}