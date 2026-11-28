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
    /** Only fixed labels and tri-state booleans; never copy raw public-state values to USB. */
    static String companionState(Object enabled,Object bridge,Object gasoline,Object tbt,Object stbt,Object pps) {
        String w=flag(enabled), b=flag(bridge), g=flag(gasoline), t=flag(tbt), st=flag(stbt);
        String overlap="true".equals(w)&&"true".equals(t)&&"true".equals(st)?"configured_conflict":
                "false".equals(w)||"false".equals(t)||"false".equals(st)?"not_configured":"unknown";
        return "wcp_enabled="+w+"; wcp_bridge="+b+"; wcp_gasoline="+g+
                "; wcp_tbt="+t+"; stbt_enabled="+st+"; tbt_overlap="+overlap+
                "; pps_enabled="+flag(pps);
    }
    private static String flag(Object value) {
        return value instanceof Boolean?value.toString():"unknown";
    }
    private static Object value(org.json.JSONObject obj,String key) {
        return obj==null?null:obj.opt(key);
    }
    static String readCompanionState() {
        try(java.io.FileInputStream in=new java.io.FileInputStream("/data/hmgapi/public/state.json")){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1){
                if(out.size()+n>4*1024*1024)throw new IOException("state too large");
                out.write(buffer,0,n);
            }
            org.json.JSONObject mods=new org.json.JSONObject(out.toString("UTF-8")).getJSONObject("mods");
            org.json.JSONObject w=mods.optJSONObject("wireless-carplay-aa");
            org.json.JSONObject t=mods.optJSONObject("standalone-tbt");
            org.json.JSONObject p=mods.optJSONObject("phone-projection-scaling");
            org.json.JSONObject settings=w==null?null:w.optJSONObject("settings");
            return companionState(value(w,"enabled"),value(settings,"bridge"),value(settings,"k5_vehicle_identity"),
                    value(settings,"tbt"),value(t,"enabled"),value(p,"enabled"));
        }catch(Exception ignored){return companionState(null,null,null,null,null,null);}
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
