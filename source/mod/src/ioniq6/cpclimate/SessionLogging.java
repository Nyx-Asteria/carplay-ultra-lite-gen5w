package ioniq6.cpclimate;

import hmgapi.hook.ModContext;
import java.io.File;
import java.util.EnumSet;
import java.util.Locale;
import ioniq6.cpclimate.SessionLogPolicy.Category;
import ioniq6.cpclimate.SessionLogPolicy.Level;

/** DiPO process logging adapter. No disk work on session or protocol hooks. */
final class SessionLogging {
    private final ModContext mod;
    private final SessionLogPolicy policy;
    private final SessionLogWriter writer;
    private final String metadata;
    private long generation=-1;
    private long endingAt=-1;
    private String endingReason;
    private final java.util.concurrent.ScheduledExecutorService timer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"cpul-log-teardown");t.setDaemon(true);return t;}});
    private final java.util.ArrayDeque<String> preSession=new java.util.ArrayDeque<String>();

    SessionLogging(ModContext mod, File files, String firmware) {
        this.mod=mod;
        EnumSet<Category> categories=EnumSet.noneOf(Category.class);
        StringBuilder settings=new StringBuilder("firmware=").append(firmware);
        for(Category c:Category.values()) {
            if(c==Category.CORE)continue;
            String key="diagnostic_"+c.name().toLowerCase(Locale.US);
            boolean on=mod.settingBoolean(key,false);
            if(on)categories.add(c);
            settings.append("; ").append(key).append('=').append(on);
        }
        boolean enabled=mod.settingBoolean("session_logging",true);
        boolean usb=mod.settingBoolean("session_usb_export",true);
        settings.append("; session_logging=").append(enabled).append("; session_usb_export=").append(usb);
        for(String key:new String[]{"k5_integration","seat_fan_immediate_feedback","seat_heat_immediate_feedback","ultra_theme","ultra_sidebars",
            "ultra_ui_sync","ultra_force_wallpaper","sxm_schema_test","parked_video","parked_video_picture_only"})
            settings.append("; ").append(key).append('=').append(mod.settingBoolean(key,
                key.equals("seat_heat_immediate_feedback")||key.equals("k5_integration")||key.equals("ultra_theme")||key.equals("ultra_sidebars")));
        metadata=settings.toString();
        policy=new SessionLogPolicy(categories);
        writer=enabled?new SessionLogWriter(new File(files,"cpul-session-logs"),
            usb?new SessionUsbExport(new SessionUsbDestination()):null,categories):null;
    }
    synchronized void start(long id) {
        if(generation==id)return;
        if(generation>=0)closeNow(endingReason==null?"superseded by new session":endingReason);
        policy.reset(); generation=id;
        if(writer!=null)writer.start(id,metadata);
        while(!preSession.isEmpty())log(preSession.removeFirst());
    }
    synchronized void finish(String reason) {
        if(generation<0||endingAt>=0)return;
        final long id=generation;
        endingAt=System.currentTimeMillis();
        endingReason=reason;
        timer.schedule(new Runnable(){public void run(){
            synchronized(SessionLogging.this){
                if(generation==id&&endingAt>=0)closeNow(endingReason);
            }
        }},2000,java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    private void closeNow(String reason) {
        if(writer!=null)writer.finishAt(generation,reason,policy.suppressed(),endingAt<0?System.currentTimeMillis():endingAt);
        generation=-1; endingAt=-1; endingReason=null;
    }
    synchronized void projection(String message) {
        if(generation<0) {
            if(preSession.size()>=32)preSession.removeFirst();
            preSession.addLast(message);
        } else if(endingAt<0 || teardown(message)) log(message);
    }
    private static boolean teardown(String message) {
        String s=message.toLowerCase(Locale.US);
        return s.contains("closed")||s.contains("ended")||s.contains("failed")||s.contains("stopped")||s.contains("disconnect")||s.contains("finaliz");
    }
    synchronized void log(String message) {
        Category category=category(message);
        Level level=level(message,category);
        String safe=policy.accept(level,category,message,System.nanoTime()/1000000L);
        if(safe==null)return;
        if(writer!=null&&generation>=0&&(endingAt<0||teardown(message)))writer.record(generation,level,category,safe);
        mod.log(safe);
    }
    static Category category(String message) {
        String s=message.toLowerCase(Locale.US);
        if(s.startsWith("[vs-packet]"))return Category.VS_RAW;
        if(s.startsWith("[projection-packet]"))return Category.PROJECTION_RAW;
        if(s.startsWith("[projection-theme]"))return Category.THEMES;
        if(s.startsWith("[projection-video]"))return Category.AIRPLAY;
        if(s.startsWith("[radio-sxm]"))return Category.SXM;
        if(s.startsWith("[radio"))return Category.RADIO;
        if(s.startsWith("[audio")||s.startsWith("[media"))return Category.AUDIO;
        if(s.startsWith("sync")||s.startsWith("[theme"))return Category.THEMES;
        if(s.startsWith("video")||s.startsWith("[video"))return Category.AIRPLAY;
        if(s.startsWith("car ")||s.startsWith("ambient ")||s.startsWith("[seat")||s.startsWith("[climate")||s.startsWith("[wheel")||s.startsWith("vs phone")||s.startsWith("vs car"))return Category.CLIMATE;
        return Category.CORE;
    }
    static Level level(String message,Category category) {
        String s=message.toLowerCase(Locale.US);
        if(s.contains("failed")||s.contains("didn't authenticate")||s.contains("implausible package"))return Level.ERROR;
        if(s.contains("unsupported stream")||s.contains("timed out")||s.contains("timeout")||s.contains("unavailable")||s.contains("refused")||s.contains("queue full")||s.contains("listener stopped"))return Level.WARN;
        if(category==Category.VS_RAW||category==Category.PROJECTION_RAW||s.startsWith("sync phone:")||s.startsWith("sync car:"))return Level.TRACE;
        if(s.startsWith("car reader and sender started")||s.startsWith("vs phone follows")||s.startsWith("vs phone reads ")||s.startsWith("[seat-fan] subscribed")||
           s.startsWith("[radio-api]")||s.startsWith("[radio-sxm] catalog accessor")||s.startsWith("[radio-probe]")||s.startsWith("[video] message #")||
           s.startsWith("[video] video: answer ")||s.startsWith("video: answer "))return Level.DEBUG;
        return Level.INFO;
    }
}
