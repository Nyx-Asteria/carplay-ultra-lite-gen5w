package ioniq6.cpclimate;
import java.io.*;
public final class RuntimeEnvironmentTest {
    public static void main(String[] args)throws Exception{
        String sample="36 25 0:32 /source /system/framework/automotive-service.odex ro - ext4 /dev/block/test ro\n"
            +"37 25 0:32 /source /system/app/RightScreenHome.odex.extra ro - ext4 /dev/block/test ro\n"
            +"38 25 0:32 /source /system/app/HKMC_Climate.odex ro - ext4 /dev/block/test ro\n";
        String result=RuntimeEnvironment.mountState(new BufferedReader(new StringReader(sample)));
        if(!result.equals("automotive-service.odex:mounted,RightScreenHome.odex:no_exact_mount,HKMC_Climate.odex:mounted,WelcomeGoodbye.apk:no_exact_mount"))throw new AssertionError(result);
        System.out.println("RuntimeEnvironmentTest passed");
    }
}
