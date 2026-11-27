package ioniq6.cpclimate;

/** Passive, bounded observation of an explicit OEM source request. No mode writes. */
final class RadioActivationTrace {
    private long generation, request, started, acquiredAt;
    private int target, lastMode, lastStream;
    private boolean active, accepted, acquired, confirmed, sampled;
    synchronized String begin(long g,long r,int mode,long now) {
        generation=g;request=r;target=mode;started=now;active=true;
        accepted=acquired=confirmed=sampled=false;
        return "requested target="+target+" request="+r;
    }
    synchronized String result(long g,long r,boolean ok,long now) {
        if(!active||g!=generation||r!=request)return null;
        accepted=ok;
        if(!ok)active=false;
        return (ok?"accepted":"rejected")+" target="+target+" request="+r+" elapsed_ms="+(now-started);
    }
    synchronized String sample(long g,long r,int mode,int stream,long now) {
        if(!active)return null;
        if(g!=generation||r!=request){active=false;return "superseded target="+target+" request="+request;}
        // Missing binder samples are not evidence of a source transition.
        if(mode<0||stream<0){
            if(now-started>=8000){active=false;return "observation_incomplete target="+target+" request="+request;}
            return null;
        }
        boolean owns=target==2?mode==2&&stream==38:target==13&&mode==4&&stream==41;
        String event=null;
        if(owns&&!acquired){acquired=true;acquiredAt=now;event="acquired";}
        else if(!owns&&acquired){active=false;event="ownership_lost";}
        else if(owns&&!confirmed&&now-acquiredAt>=1000){confirmed=true;event="sustained";}
        else if(now-started>=8000){active=false;event=confirmed?"observation_complete":acquired?"acquired_at_deadline":accepted?"accepted_without_acquisition":"request_unconfirmed";}
        else if(!sampled||lastMode!=mode||lastStream!=stream)event="transition";
        sampled=true;lastMode=mode;lastStream=stream;
        return event==null?null:event+" target="+target+" request="+request+" audioMode="+mode+" stream="+stream+" elapsed_ms="+(now-started);
    }
}