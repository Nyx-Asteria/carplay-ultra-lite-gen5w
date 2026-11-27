package ioniq6.cpclimate;

/** Keep at most the latest callback; retire it on an observed incompatible channel transition. */
final class SxmMetadataState {
    private SxmLiveMetadata latest;
    private long lastReceived=-1;
    synchronized boolean receive(SxmLiveMetadata value,long received,long generation,int observedChannel){
        if(value==null || !value.matches(generation,observedChannel) || received<0 || received<lastReceived)return false;
        latest=value; lastReceived=received; return true;
    }
    synchronized void channelChanged(long generation,int channel){
        if(latest!=null&&!latest.matches(generation,channel))latest=null;
    }
    synchronized SxmLiveMetadata current(long generation,int channel){
        return latest!=null&&latest.matches(generation,channel)?latest:null;
    }
    synchronized void clear(){latest=null;lastReceived=-1;}
}