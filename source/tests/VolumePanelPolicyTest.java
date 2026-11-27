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
        check(CarAudio.observationFresh(1000,3499),"audio observation fresh");
        check(!CarAudio.observationFresh(1000,3500),"audio observation expired");
        check(!CarAudio.observationFresh(-1,1000),"audio observation absent");
        check(!CarAudio.observationFresh(1001,1000),"audio observation future");
        check(CarAudio.boseSampleCurrent(1,1,14,14,1000,1100),"Bose stable media sample");
        check(!CarAudio.boseSampleCurrent(1,2,14,14,1000,1100),"Bose session replaced");
        check(!CarAudio.boseSampleCurrent(1,1,14,38,1000,1100),"Bose source changed to FM");
        check(!CarAudio.boseSampleCurrent(1,1,38,41,1000,1100),"Bose source changed to SiriusXM");
        check(CarAudio.boseSampleCurrent(1,1,41,41,1000,11000),"Bose freshness boundary");
        check(!CarAudio.boseSampleCurrent(1,1,41,41,1000,11001),"Bose expired sample");
        check(!CarAudio.boseSampleCurrent(1,1,14,14,1100,1000),"Bose future timestamp");
        check(!CarAudio.boseSampleCurrent(1,1,14,14,-1,1000),"Bose missing timestamp");
        VsChannel.Log log=new VsChannel.Log(){public void log(String m){}};
        VehicleState original=new VehicleState(log), replacement=new VehicleState(log);
        check(CarAudio.commandOwned(original,original,original,1,1,1),"current audio owner");
        check(!CarAudio.commandOwned(original,replacement,replacement,1,1,1),"same-generation replacement");
        check(!CarAudio.commandOwned(original,original,replacement,1,1,1),"state replaced before observation");
        check(!CarAudio.commandOwned(original,original,original,1,2,2),"generation replaced");
        check(!CarAudio.commandOwned(original,null,original,1,1,1),"readiness revoked");
        System.out.println("VolumePanelPolicy: ALL OK");
    }
}
