package ioniq6.cpclimate;
import java.io.*;
import java.net.Socket;
import java.util.*;
public final class VehicleDeliveryOwnershipTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        VsChannel.Log log=new VsChannel.Log(){public void log(String m){}};
        VehicleState[] state={new VehicleState(log)};
        VsChannel[] lanes=new VsChannel[2];
        lanes[1]=new VsChannel(1,new File("."),log,state,lanes);
        VehicleState owner=state[0];
        check(!lanes[1].deliverOwned(owner,Collections.<VehicleState.Out>emptyList()),"no writer");
        state[0]=new VehicleState(log);
        check(!lanes[1].deliverOwned(owner,Collections.<VehicleState.Out>emptyList()),"stale owner");
        byte[] key=new byte[32];
        ByteArrayOutputStream original=new ByteArrayOutputStream();
        VsChannel.Writer captured=new VsChannel.Writer(new Socket(),original,new ChaChaPoly(key,key));
        check(VsChannel.sendCaptured(captured,new byte[]{1,2,3},log)&&original.size()>0,"captured writer receives payload");
        check(!VsChannel.sendCaptured(null,new byte[]{1},log),"absent fallback rejected");
        captured.sock.close();
        System.out.println("VehicleDeliveryOwnershipTest passed");
    }
}
