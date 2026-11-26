package ioniq6.cpclimate;

/** Keep at most the latest callback; retire it on an observed incompatible channel transition. */
final class SxmMetadataState {
    private SxmLiveMetadata latest;
    synchronized void receive(SxmLiveMetadata value){latest=value;}
    synchronized void channelChanged(long generation,int channel){
        if(latest!=null&&!latest.matches(generation,channel))latest=null;
    }
    synchronized SxmLiveMetadata current(long generation,int channel){
        return latest!=null&&latest.matches(generation,channel)?latest:null;
    }
    synchronized void clear(){latest=null;}
}