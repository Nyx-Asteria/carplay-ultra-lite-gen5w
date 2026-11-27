package ioniq6.cpclimate;
public final class ThemeOwnershipTest {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[] args){
        ThemeOwnership o=new ThemeOwnership(); Object first=new Object(), second=new Object();
        check(o.owner()==null && !o.confirmed(null));
        o.begin(first);check(o.owner()==first && !o.confirmed(first));
        o.confirm();check(o.confirmed(first) && !o.confirmed(second));
        o.uncertain();check(o.owner()==first && !o.confirmed(first));
        try{o.begin(second);throw new AssertionError("overwrote uncertain owner");}catch(IllegalStateException expected){}
        o.clear();check(o.owner()==null && !o.confirmed(first));
        o.begin(second);o.uncertain();check(o.owner()==second && !o.confirmed(second));
        o.clear();o.begin(second);o.confirm();check(o.confirmed(second));
        o.clear();check(!o.confirmed(second));
        System.out.println("ThemeOwnershipTest passed");
    }
}
