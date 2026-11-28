package ioniq6.cpclimate;
import java.io.*;
public final class RuntimeEnvironmentTest {
    public static void main(String[] args)throws Exception{
        String sample="36 25 0:32 /source /system/framework/automotive-service.odex ro - ext4 /dev/block/test ro\n"
            +"37 25 0:32 /source /system/app/RightScreenHome.odex.extra ro - ext4 /dev/block/test ro\n"
            +"38 25 0:32 /source /system/app/HKMC_Climate.odex ro - ext4 /dev/block/test ro\n";
        String result=RuntimeEnvironment.mountState(new BufferedReader(new StringReader(sample)));
        if(!result.equals("automotive-service.odex:mounted,RightScreenHome.odex:no_exact_mount,HKMC_Climate.odex:mounted,WelcomeGoodbye.apk:no_exact_mount"))throw new AssertionError(result);
        String conflict=RuntimeEnvironment.companionState(true,true,true,true,true,false);
        if(!conflict.equals("wcp_enabled=true; wcp_bridge=true; wcp_gasoline=true; wcp_tbt=true; stbt_enabled=true; tbt_overlap=configured_conflict; pps_enabled=false"))throw new AssertionError(conflict);
        if(!RuntimeEnvironment.companionState(false,true,true,true,true,true).contains("tbt_overlap=not_configured"))throw new AssertionError("disabled WCP");
        if(!RuntimeEnvironment.companionState(true,true,true,false,true,true).contains("tbt_overlap=not_configured"))throw new AssertionError("TBT off");
        String unknown=RuntimeEnvironment.companionState(true,"secret",null,"true",true,null);
        if(unknown.contains("secret")||!unknown.contains("wcp_bridge=unknown")||!unknown.contains("tbt_overlap=unknown"))throw new AssertionError(unknown);
        System.out.println("RuntimeEnvironmentTest passed");
    }
}
