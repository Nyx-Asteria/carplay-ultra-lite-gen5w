package ioniq6.cpclimate;
public final class ProjectionLogCaptureTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check("[projection] unsupported stream type=130".equals(ProjectionLogCapture.classify("Unsupported stream type: 130 token=PRIVATE")));
        check("[radio-sxm] forwarded decoded eligible catalog count=132".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] forwarded decoded eligible catalog count=132")));
        check("[radio-sxm] forwarded decoded eligible catalog count=132 excluded=7".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] forwarded decoded eligible catalog count=132 excluded=7")));
        check("[radio-sxm] current-channel observer installed".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] current-channel observer installed")));
        check("[radio-sxm] live metadata bridge failed: IllegalStateException".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] live metadata bridge failed: IllegalStateException")));
        check(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] live metadata bridge failed: private text")==null);
        check(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] artwork unavailable: secret=abc")==null);
        check(ProjectionLogCapture.classify("SETUP token=private")==null);
        String offered=ProjectionLogCapture.classify("cp-climate: SETUP: the phone offers vehicleStateProtocol 1, altScreen 1, viewAreas 0, uiSync 1, iAPChannel 0, videoPlayback 1, hevc 0 (Ultra experiment on, iAPChannel experiment off, uiSync experiment on)");
        check(offered.contains("videoPlayback=1")&&offered.contains("viewAreas=0"));
        String enabled=ProjectionLogCapture.classify("cp-climate: SETUP reply has enabledFeatures [vehicleStateProtocol, uiSync]");
        check(enabled.contains("uiSync=1")&&enabled.contains("altScreen=0"));
        check(ProjectionLogCapture.classify("cp-climate: SETUP reply has enabledFeatures [vehicleStateProtocol, secret=private]").equals("[projection-theme] enabled features unrecognized"));
        check(ProjectionLogCapture.classify("cp-climate: SETUP independent uiSync enabled; stock features preserved, VSP not offered").contains("independent uiSync enabled"));
        check(ProjectionLogCapture.classify("cp-climate: theme screen SETUP type 111 uuid=other displayUUID=Cluster_Display").contains("displayUUID=Cluster_Display"));
        check(ProjectionLogCapture.classify("cp-climate: theme screen SETUP type 110 uuid=secret displayUUID=private").equals("[projection-theme] screen SETUP type=110"));
        check(ProjectionLogCapture.classify("cp-climate: Ultra display registration: main=Center_Display, alternate=Cluster_Display, original count=1").contains("originalCount=1"));
        String identity="[theme] negotiation generation=7 readiness=ADVERTISED vehicleBuilt=true themeBuilt=true startRequested=true startArmed=true startSendCapability=true startReceiveCapability=false";
        check(identity.equals(ProjectionLogCapture.classify("I CPULCrossProcess: "+identity)));
        check(ProjectionLogCapture.classify("I CPULCrossProcess: "+identity+" password=private")==null);
        check(ProjectionLogCapture.classify("I CPULCrossProcess: "+identity.replace("ADVERTISED","private"))==null);
        check("[theme] negotiation generation_changed".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation generation_changed")));
        String snapshot=identity.replace("negotiation ","negotiation snapshot ");
        check(snapshot.equals(ProjectionLogCapture.classify("I CPULCrossProcess: "+snapshot)));
        check(ProjectionLogCapture.classify("I CPULCrossProcess: "+snapshot+" secret=private")==null);
        check("[theme] negotiation snapshot unavailable".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation snapshot unavailable")));
        String actual=ProjectionLogCapture.classify("cp-climate: SETUP actual enabledFeatures vehicleStateProtocol=1 altScreen=0 viewAreas=1 uiSync=1 iAPChannel=0 videoPlayback=0 hevc=1 other=2");
        check(actual.equals("[projection-theme] enabled vehicleStateProtocol=1 altScreen=0 viewAreas=1 uiSync=1 iAPChannel=0 videoPlayback=0 hevc=1 other=2"));
        check(ProjectionLogCapture.classify("cp-climate: SETUP actual enabledFeatures vehicleStateProtocol=1 altScreen=0 viewAreas=1 uiSync=1 iAPChannel=0 videoPlayback=0 hevc=1 other=private")==null);
        check("[theme] negotiation contribution_unconfirmed".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation contribution_unconfirmed")));
        check(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation contribution_unconfirmed secret=private")==null);
        check("[theme] negotiation contribution_withdrawn".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation contribution_withdrawn")));
        check("[theme] negotiation withdrawal_pending".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation withdrawal_pending")));
        check(ProjectionLogCapture.classify("I CPULCrossProcess: [theme] negotiation withdrawal_pending reason=private")==null);
        for(String phase:new String[]{"attempt","confirmed","unconfirmed","withheld"}){
            String event="[theme] initial contribution "+phase;
            check(event.equals(ProjectionLogCapture.classify("I CPULCrossProcess: "+event)));
            check(ProjectionLogCapture.classify("I CPULCrossProcess: "+event+" secret=private")==null);
        }
        String discovery="clientTypeUUID=00000000-0000-0000-0000-000000000000 clientTypeType=string clientUUID=string channelID=absent seed=number wantsDedicatedSocket=bool-true sendMessageAsIs=bool-false sendMessageWithoutReply=absent controlType=number-2 type=number streamID=absent dataPort=absent transportType=cf-123";
        check(("[projection-discovery] "+discovery).equals(ProjectionLogCapture.classify("cp-climate: RCS discovery "+discovery)));
        check(ProjectionLogCapture.classify("cp-climate: RCS discovery "+discovery+" clientUUID=PRIVATE")==null);
        check(ProjectionLogCapture.classify("cp-climate: RCS discovery "+discovery.replace("seed=number","seed=123456"))==null);
        check(ProjectionLogCapture.classify("cp-climate: RCS discovery "+discovery.replace("clientUUID=string","clientUUID=PRIVATE"))==null);
        System.out.println("ProjectionLogCaptureTest passed");
    }
}
