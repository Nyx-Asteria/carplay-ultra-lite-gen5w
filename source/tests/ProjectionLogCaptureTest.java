package ioniq6.cpclimate;
public final class ProjectionLogCaptureTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        check("[projection] unsupported stream type=130".equals(ProjectionLogCapture.classify("Unsupported stream type: 130 token=PRIVATE")));
        check("[radio-sxm] forwarded decoded eligible catalog count=132".equals(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] forwarded decoded eligible catalog count=132")));
        check(ProjectionLogCapture.classify("I CPULCrossProcess: [radio-sxm] artwork unavailable: secret=abc")==null);
        check(ProjectionLogCapture.classify("SETUP token=private")==null);
        System.out.println("ProjectionLogCaptureTest passed");
    }
}
