package ioniq6.cpclimate;
public final class SxmCatalogRequestTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        SxmCatalogRequest r=new SxmCatalogRequest();
        check(r.begin(1,0,false));
        check(!r.begin(1,29999,false));
        check(r.begin(1,30000,false));
        check(!r.begin(1,60000,true));
        check(r.begin(1,60000,false));
        check(!r.begin(1,90000,false));
        check(r.begin(2,90001,false));
        check(!r.begin(3,90002,true));
        check(r.begin(3,90003,false));
        System.out.println("SxmCatalogRequestTest passed");
    }
}