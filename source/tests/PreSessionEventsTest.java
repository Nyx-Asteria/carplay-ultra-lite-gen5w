package ioniq6.cpclimate;
public final class PreSessionEventsTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        PreSessionEvents q=new PreSessionEvents();
        q.add("old",1000,2000,0);
        q.add("previous",10000,12000,0);
        q.add("recent",12500,12500,0);
        q.add("future",20000,12500,0);
        java.util.List<PreSessionEvents.Event> r=q.drain(13000,11000);
        check(r.size()==1 && r.get(0).at==12500 && r.get(0).message.equals("recent"));
        check(q.drain(13000,0).isEmpty());
        q.add("expires",15000,15000,0);
        check(q.drain(25001,0).isEmpty());
        for(int i=0;i<40;i++)q.add("event"+i,30000+i,30100,0);
        r=q.drain(30100,0);
        check(r.size()==32 && r.get(0).message.equals("event8"));
        System.out.println("PreSessionEventsTest passed");
    }
}
