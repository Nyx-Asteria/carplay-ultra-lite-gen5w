package ioniq6.cpclimate;
public final class VolumePanelPolicyTest {
    private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
    public static void main(String[] args) {
        check(VolumePanelPolicy.fresh(1000,900),"fresh");
        check(!VolumePanelPolicy.fresh(1000,1001),"future");
        check(!VolumePanelPolicy.fresh(3500,1000),"expired");
        check(VolumePanelPolicy.suppress(true,true,true,3500,1000,true,true),"ready");
        check(!VolumePanelPolicy.suppress(false,true,true,3500,1000,true,true),"disabled");
        check(!VolumePanelPolicy.suppress(true,false,true,3500,1000,true,true),"integration off");
        check(!VolumePanelPolicy.suppress(true,true,false,3500,1000,true,true),"setting off");
        check(!VolumePanelPolicy.suppress(true,true,true,3500,1000,false,true),"disconnected");
        check(!VolumePanelPolicy.suppress(true,true,true,3500,1000,true,false),"native UI");
        check(!VolumePanelPolicy.suppress(true,true,true,3500,3500,true,true),"expired");
        check(!VolumePanelPolicy.suppress(true,true,true,9000,1000,true,true),"implausible");
        check(!VolumePanelPolicy.suppress(true,true,true,0,1000,true,true),"revoked");
        System.out.println("VolumePanelPolicy: ALL OK");
    }
}
