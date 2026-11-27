package ioniq6.cpclimate;

/** Observational only. A matching BCM sample is evidence, never a retry or a phone acknowledgement. */
final class SeatCommandTrace {
    static final long TIMEOUT_MS = 3000;
    private final Pending[] pending = new Pending[2];
    private long next;
    static final class Pending {
        final long id, generation, started;
        final int side, requested, before;
        Pending(long id,long generation,long started,int side,int requested,int before) {
            this.id=id; this.generation=generation; this.started=started;
            this.side=side; this.requested=requested; this.before=before;
        }
        String prefix() { return "[seat-command] #"+id+" "+(side==0?"driver":"passenger"); }
    }
    synchronized String accepted(int side,int requested,int before,long generation,long now) {
        Pending old=pending[side], p=new Pending(++next,generation,now,side,requested,before);
        pending[side]=p;
        return (old!=null?old.prefix()+" superseded by #"+p.id+"; ":"")+p.prefix()
            +" binder=accepted requested="+requested+" before="+before+" generation="+generation;
    }
    synchronized String observe(int side,int actual,long generation,long now) {
        Pending p=pending[side];
        if(p==null)return null;
        String result;
        if(p.generation!=generation)result="session changed";
        else if(actual==p.requested)result=p.before==actual?"already at requested state":"BCM matched";
        else if(now-p.started>=TIMEOUT_MS)result=actual<2||actual>8?"BCM unavailable":"BCM unconfirmed";
        else return null;
        pending[side]=null;
        return p.prefix()+" "+result+" requested="+p.requested+" actual="+actual+" elapsed="+Math.max(0,now-p.started)+"ms";
    }
}
