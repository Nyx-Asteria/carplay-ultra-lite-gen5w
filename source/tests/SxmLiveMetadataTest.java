package ioniq6.cpclimate;
public final class SxmLiveMetadataTest {
    public static void main(String[] args){
        SxmLiveMetadata m=new SxmLiveMetadata(1,23,"Station"," Song ",null);
        if(!m.matches(1,23)||m.matches(1,24)||m.matches(2,23)||!m.title.equals("Song")||!m.artist.isEmpty())throw new AssertionError();
        System.out.println("SxmLiveMetadataTest passed");
    }
}