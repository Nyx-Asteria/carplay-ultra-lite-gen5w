package ioniq6.cpclimate;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public final class NativeProfileTest {
    static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args)throws Exception{
        File dir=Files.createTempDirectory("cpul-native-profile").toFile();
        File ultra=new File(dir,"cpc-ultra"),sync=new File(dir,"cpc-uisync"),child=new File(sync,"occupied");
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
            check(sync.mkdir()&&child.createNewFile(),"create deterministic obstruction");
            check(!NativeProfile.writeFlag(null,dir,"cpc-uisync",true,"test"),"failed write");
            check(!NativeProfile.writeFlag(null,dir,"cpc-uisync",false,"test"),"failed removal");
            check(child.exists(),"do not recursively remove unrelated contents");
        } finally {child.delete();sync.delete();ultra.delete();dir.delete();}
        System.out.println("NativeProfileTest passed");
    }
}
