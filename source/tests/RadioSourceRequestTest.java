package ioniq6.cpclimate;
public final class RadioSourceRequestTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        RadioSourceRequest r=new RadioSourceRequest();
        r.begin(1,2,2,100);
        check(r.matches(1,2,2,200));
        check(!r.matches(1,2,13,200));
        check(!r.matches(1,3,2,200));
        check(!r.matches(2,2,2,200));
        check(!r.matches(1,2,2,5100));
        r.begin(1,3,13,6000);
        r.clearIf(1,2);
        check(r.matches(1,3,13,12000));
        r.clearIf(1,3);
        check(!r.matches(1,3,13,12000));
        System.out.println("RadioSourceRequestTest passed");
    }
}