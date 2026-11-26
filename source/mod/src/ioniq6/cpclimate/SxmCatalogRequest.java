package ioniq6.cpclimate;

/** Bounded retry of asynchronous, read-only OEM catalog requests. */
final class SxmCatalogRequest {
    private long generation=-1,last=-1;
    private int attempts;
    synchronized boolean begin(long g,long now,boolean hasCatalog) {
        if(generation!=g){generation=g;last=-1;attempts=0;}
        if(hasCatalog||attempts>=3||(last>=0&&now-last<30000))return false;
        last=now;attempts++;return true;
    }
}