package ioniq6.cpclimate;

/** Channel-confirmed metadata with one short-lived candidate for a callback preceding tuner polling. */
final class SxmMetadataState {
    private static final long CANDIDATE_MS=2000;
    private SxmLiveMetadata latest, candidate;
    private long lastReceived=-1, candidateReceived=-1;
    synchronized boolean receive(SxmLiveMetadata value,long received,long generation,int observedChannel){
        if(value==null || value.generation!=generation || value.channel<0 || received<0 || received<lastReceived)return false;
        if(!value.matches(generation,observedChannel)){
            if(received>=candidateReceived){candidate=value;candidateReceived=received;}
            return false;
        }
        latest=value; lastReceived=received;
        if(candidate!=null && candidate.matches(generation,observedChannel)){candidate=null;candidateReceived=-1;}
        return true;
    }
    synchronized void channelChanged(long generation,int channel,long now){
        if(latest!=null&&!latest.matches(generation,channel))latest=null;
        if(candidate!=null && candidate.matches(generation,channel) && now>=candidateReceived
                && now-candidateReceived<=CANDIDATE_MS && candidateReceived>=lastReceived){
            latest=candidate;lastReceived=candidateReceived;
        }
        candidate=null;candidateReceived=-1;
    }
    synchronized SxmLiveMetadata current(long generation,int channel){
        return latest!=null&&latest.matches(generation,channel)?latest:null;
    }
    synchronized void clear(){latest=null;candidate=null;lastReceived=-1;candidateReceived=-1;}
}
