package ioniq6.cpclimate;
public final class SeatCommandTraceTest {
    static void has(String s,String part){if(s==null||!s.contains(part))throw new AssertionError(s+" missing "+part);}
    public static void main(String[] args){
        if (!SeatCommandTrace.sessionMatches(1,1,true)
                || SeatCommandTrace.sessionMatches(1,2,true)
                || SeatCommandTrace.sessionMatches(1,1,false)) throw new AssertionError("session guard");
        SeatCommandTrace t=new SeatCommandTrace();
        has(t.accepted(0,7,8,1,100),"#1 driver");
        if(t.observe(0,8,1,200)!=null)throw new AssertionError("premature result");
        has(t.accepted(1,4,2,1,200),"#2 passenger");
        has(t.observe(0,7,1,400),"BCM matched");
        has(t.observe(1,2,1,3300),"unconfirmed");
        has(t.accepted(0,5,5,1,4000),"#3");
        has(t.observe(0,5,1,4100),"already at requested state");
        t.accepted(0,6,5,1,5000);
        has(t.accepted(0,8,5,1,5100),"superseded");
        has(t.observe(0,8,2,5200),"session changed");
        t.accepted(1,7,-1,2,6000);
        has(t.observe(1,-1,2,9000),"unavailable");
        System.out.println("SeatCommandTraceTest passed");
    }
}
