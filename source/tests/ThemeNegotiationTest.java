package ioniq6.cpclimate;
public final class ThemeNegotiationTest {
    public enum Ready { ARMED, ADVERTISED, FAILED }
    public static final class Identity {
        public final long generation;
        public final Ready readiness;
        Identity(long g, Ready r){generation=g;readiness=r;}
    }
    public static final class Evidence {
        public long generation=7;
        public boolean vehicleBuilt=true,themeBuilt=true,startRequested=true,startArmed=true,startSendCapability=true,startReceiveCapability=false;
    }
    public static final class Theme {
        public Theme(String id,long version,String protocol){}
    }
    public static final class IncompleteProvider {
        public void registerTheme(String owner,Theme theme,boolean start){}
        public void removeTheme(String owner){}
        public Identity identityStatus(){return null;}
        public Identity negotiationStatus(){return null;}
    }
    public static final class Provider {
        public void registerTheme(String owner,Theme theme,boolean start){}
        public void removeTheme(String owner){}
        int calls;
        long second=7;
        Ready state=Ready.ADVERTISED;
        Evidence evidence=new Evidence();
        public Identity identityStatus(){return new Identity(++calls%2==0?second:7,state);}
        public Evidence negotiationStatus(){return evidence;}
    }
    static void check(boolean v){if(!v)throw new AssertionError();}
    public static void main(String[] args)throws Exception{
        ThemeNegotiation.verifyContract(Provider.class,Theme.class);
        try {
            ThemeNegotiation.verifyContract(IncompleteProvider.class,Theme.class);
            throw new AssertionError("incomplete negotiation contract accepted");
        } catch(NoSuchFieldException expected){}
        Provider p=new Provider();
        String s=ThemeNegotiation.sample(Provider.class,p);
        check(s.contains("themeBuilt=true") && s.contains("startReceiveCapability=false"));
        p.second=8;check("generation_changed".equals(ThemeNegotiation.sample(Provider.class,p)));
        p.second=7;p.evidence.generation=6;check("generation_changed".equals(ThemeNegotiation.sample(Provider.class,p)));
        p.evidence.generation=7;p.state=Ready.FAILED;
        check(ThemeNegotiation.sample(Provider.class,p).contains("themeBuilt=false"));
        System.out.println("ThemeNegotiationTest passed");
    }
}
