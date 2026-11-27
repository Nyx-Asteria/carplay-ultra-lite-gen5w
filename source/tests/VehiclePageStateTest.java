package ioniq6.cpclimate;
import java.util.*;
public final class VehiclePageStateTest {
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args){
        VehiclePage p=new VehiclePage();
        p.line("Vehicle","range","Range","fuelpump");
        p.button("Vehicle","home","Home","house");
        p.toggle("Vehicle","test","Test","gear",false);
        Map<String,Object> fresh=new LinkedHashMap<String,Object>();
        fresh.put("range","300 mi");fresh.put("test",true);fresh.put("home",1L);
        long range=p.item("range").valueIid(),toggle=p.item("test").valueIid(),home=p.item("home").valueIid();
        Map<Long,Object> first=p.update(fresh,false);
        check(first.size()==2 && "300 mi".equals(first.get(range)) && Boolean.TRUE.equals(first.get(toggle)));
        check(p.update(fresh,false).isEmpty());
        Map<Long,Object> replay=p.update(fresh,true);
        check(replay.equals(first)&&!replay.containsKey(home));
        fresh.put("range",VehiclePage.NONE);
        check(VehiclePage.NONE.equals(p.update(fresh,true).get(range)));
        fresh.put("range",null);
        check(!p.update(fresh,true).containsKey(range));
        System.out.println("VehiclePageStateTest passed");
    }
}
