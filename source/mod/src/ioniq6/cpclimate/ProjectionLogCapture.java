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
    ProjectionLogCapture(SessionLogging sink){super("cpul-projection-log");this.sink=sink;setDaemon(true);}
    static String classify(String body) {
        int marker=body.indexOf("CPULCrossProcess");
        if(marker>=0) {
            int colon=body.indexOf(':',marker);
            if(colon<0)return null;
            String event=body.substring(colon+1).trim();
            if(event.matches("\\[radio-sxm\\] forwarded decoded eligible catalog count=\\d{1,3}"))return event;
            if(event.equals("[radio-sxm] catalog bridge withheld: sender permission unavailable")||
               event.equals("[radio-sxm] decoded catalog observer installed"))return event;
            if(event.matches("\\[radio-sxm\\] (catalog bridge failed|artwork unavailable|catalog observer unavailable): [A-Za-z0-9_]{1,80}"))return event;
            return null;
        }
        Matcher m=STREAM.matcher(body);
        if(m.find())return "[projection] unsupported stream type="+m.group(1);
        if(body.contains("Unsupported stream type"))return "[projection] unsupported stream type=unknown";
        if(body.contains("Modes changed"))return "[projection] mode transition reported";
        if(body.contains("cp-climate:")) {
            m=NATIVE.matcher(body);if(m.find())return "[projection-packet] native stream type="+m.group(1);
            m=SCREEN.matcher(body);if(m.find())return "[projection-theme] screen SETUP type="+m.group(1);
            if(body.contains("Ultra display metadata supplemented"))return "[projection-theme] display metadata supplemented";
            if(body.contains("Ultra display registration"))return "[projection-theme] display registration reported";
            if(body.contains("type 130"))return "[projection-packet] type 130 reported";
            if(body.contains("SETUP reply has enabledFeatures"))return "[projection] feature negotiation reply";
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
