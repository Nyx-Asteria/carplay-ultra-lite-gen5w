package ioniq6.cpclimate;
public final class PassengerTempTraceTest {
    static void has(String s,String p){if(s==null||!s.contains(p))throw new AssertionError(s+" expected "+p);}
    public static void main(String[] args){
        PassengerTempTrace t=new PassengerTempTrace();
        has(t.accepted(true,22,1,100),"expected=25.0");
        if(t.observe(new PassengerTempTrace.Sample(25,77,65535,90),22,1,0,1,200)!=null)throw new AssertionError("stale callback");
        has(t.observe(new PassengerTempTrace.Sample(25.5f,78,65535,200),23,1,0,1,3100),"DATC mismatch");
        t.accepted(false,21,1,4000);
        has(t.observe(new PassengerTempTrace.Sample(25,77,65535,4100),22,2,0,1,4200),"DATC matched");
        t.accepted(true,22,1,5000);
        has(t.observe(null,22,2,0,1,5100),"unit changed");
        t.accepted(true,22,1,6000);
        has(t.observe(null,22,1,0,2,6100),"session changed");
        t.accepted(true,22,2,7000);
        has(t.observe(null,22,1,0,2,10000),"no fresh normal");
        System.out.println("PassengerTempTraceTest passed");
    }
}
