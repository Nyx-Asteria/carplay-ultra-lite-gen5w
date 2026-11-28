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
        check(q.drain(15000+PreSessionEvents.MAX_AGE_MS+1,0).isEmpty());
        for(int i=0;i<PreSessionEvents.LIMIT+8;i++)q.add("event"+i,30000+i,31000,0);
        r=q.drain(31000,0);
        check(r.size()==PreSessionEvents.LIMIT && r.get(0).message.equals("event8"));
        q.add("password=example",40000,40000,0);
        check(q.drain(40000,0).get(0).message.equals("[sensitive message withheld]"));
        q.add("early negotiation",50000,50000,0);
        check(q.drain(110000,0).size()==1);
        System.out.println("PreSessionEventsTest passed");
    }
}
