package ioniq6.cpclimate;
public final class SxmLiveMetadataTest {
    public static void main(String[] args){
        SxmLiveMetadata m=new SxmLiveMetadata(1,23,"Station"," Song ",null);
        if(!m.matches(1,23)||m.matches(1,24)||m.matches(2,23)||!m.title.equals("Song")||!m.artist.isEmpty())throw new AssertionError();
        SxmMetadataState state=new SxmMetadataState();
        if(!state.receive(m,100,1,23))throw new AssertionError("matching callback");
        state.channelChanged(1,23);
        if(state.current(1,23)!=m)throw new AssertionError("matching callback");
        state.channelChanged(1,24);
        state.channelChanged(1,23);
        if(state.current(1,23)!=null)throw new AssertionError("old track resurrected");
        SxmLiveMetadata next=new SxmLiveMetadata(1,24,"Other","New","");
        if(!state.receive(next,200,1,24))throw new AssertionError("matching new callback");
        if(state.receive(m,210,1,24))throw new AssertionError("wrong channel callback");
        if(state.receive(next,190,1,24))throw new AssertionError("out of order callback");
        if(state.receive(next,220,2,24))throw new AssertionError("wrong generation callback");
        state.channelChanged(1,24);
        if(state.current(1,24)!=next)throw new AssertionError("new channel");
        state.clear();
        if(state.current(1,24)!=null)throw new AssertionError("source lost");
        System.out.println("SxmLiveMetadataTest passed");
    }
}