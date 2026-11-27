package ioniq6.cpclimate;
/** Diagnostic correlation only: does not change setters or published temperature. */
final class PassengerTempTrace {
    static final long TIMEOUT_MS=3000;
    static final class Sample {
        final float c,f; final int state; final long at;
        Sample(float c,float f,int state,long at){this.c=c;this.f=f;this.state=state;this.at=at;}
    }
    private long next,id,generation,started;
    private boolean celsius,pending;
    private int raw;
    synchronized String accepted(boolean celsius,int raw,long generation,long at){
        String previous=pending?"#"+id+" superseded; ":"";
        this.celsius=celsius;this.raw=raw;this.generation=generation;started=at;id=++next;pending=true;
        return "[climate-passenger] "+previous+"#"+id+" binder=accepted raw="+raw+" expected="+expected()+" unit="+(celsius?"C":"F");
    }
    private double expected(){return celsius?raw/2.0+14:raw+56;}
    synchronized String observe(Sample sample,float statusRaw,int unit,float sync,long generation,long now){
        if(!pending)return null;
        boolean fresh=sample!=null&&sample.at>=started&&sample.at<=now;
        float value=fresh?(celsius?sample.c:sample.f):Float.NaN;
        boolean normal=fresh&&sample.state==0xffff&&!Float.isNaN(value);
        boolean sameUnit=unit==(celsius?1:2);
        String result;
        if(this.generation!=generation)result="session changed";
        else if(!sameUnit && (unit==1||unit==2))result="unit changed";
        else if(normal&&sameUnit&&Math.abs(value-expected())<0.01)result="DATC matched";
        else if(now-started>=TIMEOUT_MS)result=normal?"DATC mismatch":"no fresh normal DATC feedback";
        else return null;
        pending=false;
        return "[climate-passenger] #"+id+" "+result+" expected="+expected()+" actual="+value
            +" callbackFresh="+fresh+" callbackState="+(sample==null?-1:sample.state)+" statusRaw="+statusRaw+" sync="+sync+" unit="+unit+" elapsed="+Math.max(0,now-started)+"ms";
    }
}
