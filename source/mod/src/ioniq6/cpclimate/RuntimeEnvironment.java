package ioniq6.cpclimate;

import android.content.Context;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** Read-only runtime evidence. A mount is not proof of the owning mod. */
final class RuntimeEnvironment {
    static final String[] TARGETS={
        "/system/framework/automotive-service.odex",
        "/system/app/RightScreenHome.odex",
        "/system/app/HKMC_Climate.odex",
        "/system/app/WelcomeGoodbye.apk"
    };
    static String mountState(BufferedReader reader) throws IOException {
        boolean[] mounted=new boolean[TARGETS.length];
        String line;
        while((line=reader.readLine())!=null){
            String[] fields=line.split(" ",7);
            if(fields.length<6)continue;
            for(int i=0;i<TARGETS.length;i++)if(TARGETS[i].equals(fields[4]))mounted[i]=true;
        }
        StringBuilder b=new StringBuilder();
        for(int i=0;i<TARGETS.length;i++){
            if(i>0)b.append(',');
            b.append(TARGETS[i].substring(TARGETS[i].lastIndexOf('/')+1)).append(':').append(mounted[i]?"mounted":"no_exact_mount");
        }
        return b.toString();
    }
    static String describe(String product,Context context){
        String mounts="unavailable";
        try(BufferedReader r=new BufferedReader(new FileReader("/proc/self/mountinfo"))){mounts=mountState(r);}
        catch(Exception ignored){}
        String ccnc="unknown";
        if(context!=null)try{
            context.getPackageManager().getPackageInfo("dev.ccnc.home",0);
            ccnc="installed";
        }catch(android.content.pm.PackageManager.NameNotFoundException e){ccnc="not_found";}
        catch(Exception ignored){}
        return "target_firmware=DL322_USA_260507; target_preflight=passed; product="+product+
            "; ccnc_home="+ccnc+"; exact_file_mounts="+mounts;
    }
}
