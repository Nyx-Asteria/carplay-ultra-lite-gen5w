package ioniq6.cpclimate;

/** Channel-scoped snapshot from the OEM current-channel callback. */
final class SxmLiveMetadata {
    final long generation;
    final int channel;
    final String name,title,artist;
    SxmLiveMetadata(long generation,int channel,String name,String title,String artist){
        this.generation=generation;this.channel=channel;
        this.name=clean(name);this.title=clean(title);this.artist=clean(artist);
    }
    private static String clean(String s){if(s==null)return "";s=s.trim();return s.substring(0,Math.min(256,s.length()));}
    boolean matches(long g,int id){return channel>=0&&generation==g&&channel==id;}
}