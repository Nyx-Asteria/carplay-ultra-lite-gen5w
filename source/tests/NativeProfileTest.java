package ioniq6.cpclimate;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public final class NativeProfileTest {
    static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args)throws Exception{
        File dir=Files.createTempDirectory("cpul-native-profile").toFile();
        File ultra=new File(dir,"cpc-ultra"),sync=new File(dir,"cpc-uisync"),child=new File(sync,"occupied"),wide=new File(dir,"cpc-wide");
        try {
            check(NativeProfile.writeUltra(null,dir,2),"write themed mode");
            check("2\n".equals(new String(Files.readAllBytes(ultra.toPath()),StandardCharsets.UTF_8)),"exact mode bytes");
            check(NativeProfile.writeUltra(null,dir,1),"replace with generic mode");
            check("1\n".equals(new String(Files.readAllBytes(ultra.toPath()),StandardCharsets.UTF_8)),"exact replacement bytes");
            check(NativeProfile.writeUltra(null,dir,0)&&!ultra.exists(),"clear mode");
            check(NativeProfile.writeUltra(null,dir,0),"already absent");
            check(NativeProfile.writeFlag(null,dir,"cpc-uisync",true,"test"),"enable flag");
            check("1\n".equals(new String(Files.readAllBytes(sync.toPath()),StandardCharsets.UTF_8)),"exact flag bytes");
            check(NativeProfile.writeFlag(null,dir,"cpc-uisync",false,"test")&&!sync.exists(),"disable flag");
            check(NativeProfile.writeWide(null,dir,"1"),"whole area");
            check("1\n".equals(new String(Files.readAllBytes(wide.toPath()),StandardCharsets.UTF_8)),"whole area bytes");
            check(NativeProfile.writeWide(null,dir,"2"),"right area");
            check("2\n".equals(new String(Files.readAllBytes(wide.toPath()),StandardCharsets.UTF_8)),"right area bytes");
            check(NativeProfile.writeWide(null,dir,"invalid")&&!wide.exists(),"invalid mode clears");
            check("0".equals(NativeProfile.wideMode(null)),"null mode disabled");
            File independent=new File(dir,"cpc-sync-independent");
            check(NativeProfile.writeDisplayProfile(null,dir,"2",2,true,true),"themed profile");
            check(ultra.exists()&&wide.exists()&&sync.exists()&&independent.exists(),"themed profile all requested flags");
            check(NativeProfile.writeDisplayProfile(null,dir,"1",1,true,true),"generic cluster profile");
            check(ultra.exists()&&wide.exists()&&!sync.exists()&&!independent.exists(),"generic cluster cannot retain themed sync flags");
            check(NativeProfile.writeDisplayProfile(null,dir,"0",0,true,true),"standard profile");
            check(!ultra.exists()&&!wide.exists()&&!sync.exists()&&!independent.exists(),"standard profile clears all display flags");
            check(sync.mkdir()&&child.createNewFile(),"create deterministic obstruction");
            check(!NativeProfile.writeFlag(null,dir,"cpc-uisync",true,"test"),"failed write");
            check(!NativeProfile.writeFlag(null,dir,"cpc-uisync",false,"test"),"failed removal");
            check(child.exists(),"do not recursively remove unrelated contents");
            check(!NativeProfile.writeDisplayProfile(null,dir,"2",2,true,true),"blocked group publication fails");
            check(!ultra.exists()&&!wide.exists()&&!independent.exists()&&child.exists(),"rollback clears writable flags and preserves obstruction");
        } finally {child.delete();sync.delete();ultra.delete();wide.delete();new File(dir,"cpc-sync-independent").delete();dir.delete();}
        System.out.println("NativeProfileTest passed");
    }
}
