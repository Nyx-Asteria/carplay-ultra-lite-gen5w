package ioniq6.cpclimate;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

/** Continuous allowlisted native events. Never persist arbitrary logcat lines or payloads. */
final class ProjectionLogCapture extends Thread {
    private final SessionLogging sink;
    private static final Pattern STREAM=Pattern.compile("(?i)unsupported stream type\\s*[:=]?\\s*(\\d{1,5})");
    private static final Pattern SETUP=Pattern.compile("(?i)stream type\\s*[:=]\\s*(\\d{1,5})");
    private static final Pattern NATIVE=Pattern.compile("(?i)theme stream audit: native path type (\\d{1,5})");
    private static final Pattern SCREEN=Pattern.compile("(?i)theme screen SETUP type (\\d{1,5})");
    private static final String[] FEATURES={"vehicleStateProtocol","altScreen","viewAreas","uiSync","iAPChannel","videoPlayback","hevc"};
    private static final Pattern OFFER=Pattern.compile("SETUP: the phone offers vehicleStateProtocol ([01]), altScreen ([01]), viewAreas ([01]), uiSync ([01]), iAPChannel ([01])(?:, videoPlayback ([01]), hevc ([01]))? \\(Ultra experiment (?:on|off), iAPChannel experiment (?:on|off), uiSync experiment (?:on|off)\\)");
    private static final Pattern ENABLED=Pattern.compile("SETUP reply has enabledFeatures \\[([^\\]\\r\\n]{1,160})\\]");
    private static final Pattern ACTUAL=Pattern.compile("SETUP actual enabledFeatures vehicleStateProtocol=([01]) altScreen=([01]) viewAreas=([01]) uiSync=([01]) iAPChannel=([01]) videoPlayback=([01]) hevc=([01]) other=(\\d{1,9})(?:\\s|$)");
    static String featureEvent(String body) {
        Matcher actual=ACTUAL.matcher(body);
        if(actual.find()){
            StringBuilder b=new StringBuilder("[projection-theme] enabled");
            for(int i=0;i<FEATURES.length;i++)b.append(' ').append(FEATURES[i]).append('=').append(actual.group(i+1));
            return b.append(" other=").append(actual.group(8)).toString();
        }
        Matcher offer=OFFER.matcher(body);
        if(offer.find()){
            StringBuilder b=new StringBuilder("[projection-theme] offered");
            for(int i=0;i<FEATURES.length;i++)b.append(' ').append(FEATURES[i]).append('=').append(offer.group(i+1)==null?"unknown":offer.group(i+1));
            return b.toString();
        }
        Matcher enabled=ENABLED.matcher(body);
        if(enabled.find()){
            java.util.Set<String> names=new java.util.HashSet<String>();
            for(String name:enabled.group(1).split(", *")) {
                if(!java.util.Arrays.asList(FEATURES).contains(name))return "[projection-theme] enabled features unrecognized";
                names.add(name);
            }
            StringBuilder b=new StringBuilder("[projection-theme] enabled");
            for(String name:FEATURES)b.append(' ').append(name).append('=').append(names.contains(name)?1:0);
            return b.toString();
        }
        if(body.contains("SETUP independent uiSync enabled; stock features preserved, VSP not offered"))
            return "[projection-theme] independent uiSync enabled; stock features preserved; VSP not offered";
        if(body.contains("SETUP without vehicleStateProtocol from the phone: reply unchanged"))
            return "[projection-theme] VSP not offered; reply unchanged";
        if(body.contains("SETUP reply left stock (test variant)"))
            return "[projection-theme] reply left stock";
        return null;
    }
    ProjectionLogCapture(SessionLogging sink){super("cpul-projection-log");this.sink=sink;setDaemon(true);}
    static String classify(String body) {
        int marker=body.indexOf("CPULCrossProcess");
        if(marker>=0) {
            int colon=body.indexOf(':',marker);
            if(colon<0)return null;
            String event=body.substring(colon+1).trim();
            if(event.matches("\\[theme\\] negotiation (?:snapshot )?(unavailable|generation_changed|readiness_changed|provider_changed|observation_failed)"))return event;
            if(event.matches("\\[theme\\] negotiation (?:snapshot )?generation=\\d{1,19} readiness=(UNAVAILABLE|REGISTERED|PREPARED|ARMED|ADVERTISED|FAILED) vehicleBuilt=(true|false) themeBuilt=(true|false) startRequested=(true|false) startArmed=(true|false) startSendCapability=(true|false) startReceiveCapability=(true|false)"))return event;
            if(event.matches("\\[radio-sxm\\] forwarded decoded eligible catalog count=\\d{1,3}(?: excluded=\\d{1,3})?"))return event;
            if(event.equals("[radio-sxm] catalog bridge withheld: sender permission unavailable")||
               event.equals("[radio-sxm] decoded catalog observer installed")||
               event.equals("[radio-sxm] current-channel observer installed"))return event;
            if(event.matches("\\[radio-sxm\\] (catalog bridge failed|artwork unavailable|catalog observer unavailable|live metadata bridge failed|current-channel observer unavailable): [A-Za-z0-9_]{1,80}"))return event;
            return null;
        }
        Matcher m=STREAM.matcher(body);
        if(m.find())return "[projection] unsupported stream type="+m.group(1);
        if(body.contains("Unsupported stream type"))return "[projection] unsupported stream type=unknown";
        if(body.contains("Modes changed"))return "[projection] mode transition reported";
        if(body.contains("cp-climate:")) {
            m=NATIVE.matcher(body);if(m.find())return "[projection-packet] native stream type="+m.group(1);
            m=SCREEN.matcher(body);if(m.find()){
                String result="[projection-theme] screen SETUP type="+m.group(1);
                Matcher ids=Pattern.compile("uuid=(Center_Display|Cluster_Display|absent|other) displayUUID=(Center_Display|Cluster_Display|absent|other)(?:$|\\s)").matcher(body);
                if(ids.find())result+=" uuid="+ids.group(1)+" displayUUID="+ids.group(2);
                return result;
            }
            if(body.contains("Ultra display metadata supplemented"))return "[projection-theme] display metadata supplemented";
            if(body.contains("Ultra display registration")){
                Matcher registration=Pattern.compile("main=(Center_Display|stock UUID), alternate=(Cluster_Display|virtual UUID), original count=(\\d{1,3})(?:$|\\s)").matcher(body);
                if(registration.find())return "[projection-theme] display registration main="+registration.group(1)+" alternate="+registration.group(2)+" originalCount="+registration.group(3);
                return "[projection-theme] display registration reported";
            }
            if(body.contains("type 130"))return "[projection-packet] type 130 reported";
            String features=featureEvent(body);if(features!=null)return features;
            if(body.contains("SETUP reply has enabledFeatures"))return "[projection-theme] enabled features unavailable";
            if(body.contains("videoPlayback")&&body.contains("stream set up"))return "[projection-video] playback stream setup reported";
            if(body.contains("video: answer not sent"))return "[projection-video] native answer send failed";
            if(body.contains("video: answers closed"))return "[projection-video] native answer channel closed";
            if(body.contains("### cp-climate:"))return "[projection] native hook failed";
        }
        m=SETUP.matcher(body);if(m.find())return "[projection-packet] stream type="+m.group(1);
        return null;
    }
    public void run(){
        while(!isInterrupted()){
            Process p=null;
            try{
                // API 19 logcat has no reliable --pid or -T option; reject stale backlog by timestamp.
                long started=System.currentTimeMillis();
                p=new ProcessBuilder("logcat","-v","threadtime","-s","TccCarPlay:*","CPULCrossProcess:I").redirectErrorStream(true).start();
                BufferedReader in=new BufferedReader(new InputStreamReader(p.getInputStream(),"UTF-8"));
                SimpleDateFormat date=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US);
                date.setLenient(false);
                try{
                    String line;
                    while((line=in.readLine())!=null){
                        if(line.length()<19)continue;
                        String event=classify(line);
                        if(event==null)continue;
                        try{
                            int year=Calendar.getInstance().get(Calendar.YEAR);
                            long at=date.parse(year+"-"+line.substring(0,18)).getTime();
                            if(at<started-1000||at>System.currentTimeMillis()+1000)continue;
                            sink.projection(event,at);
                        }catch(Exception ignored){continue;}
                    }
                }finally{in.close();}
                sink.projection("[projection] native log reader ended");
            }catch(Exception e){sink.projection("[projection] native log reader unavailable");}
            finally{if(p!=null)p.destroy();}
            try{Thread.sleep(5000);}catch(InterruptedException e){return;}
        }
    }
}
