package ioniq6.cpclimate;
import java.util.*;
public final class SeatSchemaTest {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        Map<Long,Object> values=new HashMap<Long,Object>();
        Map<Object,Object> service=VehicleState.seatFan(1041,"seat_front_left",values);
        check(Long.valueOf(0x22000002L).equals(service.get("type")));
        List<?> chars=(List<?>)service.get("characteristics");
        Map<?,?> level=(Map<?,?>)chars.get(0),on=(Map<?,?>)chars.get(1),key=(Map<?,?>)chars.get(2);
        check(Long.valueOf(0x31000012L).equals(level.get("type")));
        check(Long.valueOf(1).equals(level.get("format")));
        check(Long.valueOf(3).equals(level.get("maximumValue")));
        check(Long.valueOf(0).equals(on.get("format")));
        check("seat_front_left".equals(key.get("initialValue")));
        check(Long.valueOf(0).equals(values.get(1042L)));
        check(Boolean.FALSE.equals(values.get(1043L)));
        for(int i=0;i<=3;i++){
            check(Arrays.equals(OpackCodec.encode(Integer.valueOf(i)),OpackCodec.encode(Long.valueOf(i))));
            check(Long.valueOf(i).equals(OpackCodec.decode(OpackCodec.encode(Long.valueOf(i)))));
        }
        Map<Object,Object> update=new LinkedHashMap<Object,Object>();
        update.put(1042L,3L);update.put(1043L,true);
        String trace=VehicleState.seatNotificationDiagnostic(update);
        check(trace.contains("1042:Long=3")&&trace.contains("1043:Boolean=true"));
        System.out.println("SeatSchemaTest passed");
    }
}
