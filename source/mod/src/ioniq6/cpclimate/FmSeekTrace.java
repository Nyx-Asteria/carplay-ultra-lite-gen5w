package ioniq6.cpclimate;

/** Passive correlation only: a changed frequency is not proof that seek caused it. */
final class FmSeekTrace {
    static final long TIMEOUT_MS=5000;
    private long generation,request,before,started;
    private boolean pending;
    synchronized String begin(long generation,long request,long before,long now){
        String old=pending?event("superseded",-1,now):null;
        this.generation=generation;this.request=request;this.before=before;started=now;pending=true;
        return old;
    }
    synchronized String cancel(){
        if(!pending)return null;
        pending=false;return "session ended request="+request;
    }
    synchronized String observe(long g,long r,int mode,int stream,long khz,long now){
        if(!pending)return null;
        String result;
        if(g!=generation||r!=request)result="superseded";
        else if(mode>=0&&stream>=0&&(mode!=2||stream!=38))result="ownership_lost";
        else if(mode==2&&stream==38&&before>0&&khz>0&&khz!=before)result="frequency_changed";
        else if(now-started>=TIMEOUT_MS)result=mode==2&&stream==38&&khz>0&&before>0?"frequency_unchanged":"observation_incomplete";
        else return null;
        pending=false;return event(result,khz,now);
    }
    private String event(String result,long khz,long now){
        return result+" request="+request+" beforeKhz="+before+" observedKhz="+khz+" elapsed_ms="+Math.max(0,now-started);
    }
}
