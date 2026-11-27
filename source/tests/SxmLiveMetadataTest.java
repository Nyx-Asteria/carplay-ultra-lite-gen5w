package ioniq6.cpclimate;
public final class SxmLiveMetadataTest {
    public static void main(String[] args){
        SxmLiveMetadata m=new SxmLiveMetadata(1,23,"Station"," Song ",null);
        if(!m.matches(1,23)||m.matches(1,24)||m.matches(2,23)||!m.title.equals("Song")||!m.artist.isEmpty())throw new AssertionError();
        SxmMetadataState state=new SxmMetadataState();
        if(!state.receive(m,100,1,23))throw new AssertionError("matching callback");
        state.channelChanged(1,23,220);
        if(state.current(1,23)!=m)throw new AssertionError("matching callback");
        state.channelChanged(1,24,220);
        state.channelChanged(1,23,220);
        if(state.current(1,23)!=null)throw new AssertionError("old track resurrected");
        SxmLiveMetadata next=new SxmLiveMetadata(1,24,"Other","New","");
        if(!state.receive(next,200,1,24))throw new AssertionError("matching new callback");
        if(state.receive(m,210,1,24))throw new AssertionError("wrong channel callback");
        if(state.receive(next,190,1,24))throw new AssertionError("out of order callback");
        if(state.receive(next,220,2,24))throw new AssertionError("wrong generation callback");
        state.channelChanged(1,24,220);
        if(state.current(1,24)!=next)throw new AssertionError("new channel");
        state.clear();
        if(state.current(1,24)!=null)throw new AssertionError("source lost");
        // A new-channel callback may beat the tuner poll, but cannot replace the old channel early.
        state.receive(m,300,1,23);
        if(state.receive(next,310,1,23))throw new AssertionError("unconfirmed candidate");
        if(state.current(1,23)!=m || state.current(1,24)!=null)throw new AssertionError("candidate leaked");
        state.channelChanged(1,24,320);
        if(state.current(1,24)!=next)throw new AssertionError("candidate not promoted");
        state.channelChanged(1,23,330);
        state.channelChanged(1,24,340);
        if(state.current(1,24)!=null)throw new AssertionError("candidate resurrected");
        state.clear();
        state.receive(next,400,1,23);
        state.channelChanged(1,24,2401);
        if(state.current(1,24)!=null)throw new AssertionError("expired candidate");
        state.receive(next,2500,1,23);
        state.channelChanged(2,24,2501);
        if(state.current(2,24)!=null)throw new AssertionError("cross-session candidate");
        state.receive(next,2600,1,23);
        state.clear();
        state.channelChanged(1,24,2601);
        if(state.current(1,24)!=null)throw new AssertionError("cleared candidate");
        System.out.println("SxmLiveMetadataTest passed");
    }
}