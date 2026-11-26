package ioniq6.cpclimate;

/** Coalesce duplicate explicit selections during a bounded activation request. */
final class RadioSourceRequest {
    private long generation=-1,request=-1,deadline;
    private int target;
    synchronized void begin(long g,long r,int mode,long now){
        generation=g;request=r;target=mode;deadline=now+(mode==13?7000:5000);
    }
    synchronized boolean matches(long g,long r,int mode,long now){
        return generation==g&&request==r&&target==mode&&now<deadline;
    }
    synchronized void clear(){generation=-1;request=-1;deadline=0;}
    synchronized void clearIf(long g,long r){if(generation==g&&request==r)clear();}
}