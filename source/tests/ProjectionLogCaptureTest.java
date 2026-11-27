package ioniq6.cpclimate;
public final class ProjectionLogCaptureTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check("[projection] unsupported stream type=130".equals(ProjectionLogCapture.classify("Unsupported stream type: 130 token=PRIVATE")));
        check("[radio-sxm] forwarded decoded eligible catalog count=132".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] forwarded decoded eligible catalog count=132")));
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
        System.out.println("ProjectionLogCaptureTest passed");
    }
}
