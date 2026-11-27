package ioniq6.cpclimate;

/** Pure radio mapping regressions; no OEM service or Android runtime required. */
public final class RadioPolicyTest {
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        check(K5Radio.currentRadioRequest(1,2,1,2), "current seek request");
        check(!K5Radio.currentRadioRequest(1,2,2,2), "new source supersedes seek");
        check(!K5Radio.currentRadioRequest(1,2,1,3), "new station supersedes seek");
        check(K5Radio.retainFmCache(15,0), "retain transient empty scan");
        check(!K5Radio.retainFmCache(0,0) && !K5Radio.retainFmCache(15,12), "accept new nonempty catalog");
        check(K5Radio.fmKhz(10030) == 100300, "Z100 frequency conversion");
        check(K5Radio.fmKhz(8750) == 87500, "lower tuner boundary");
        check(K5Radio.fmKhz(10800) == 108000, "upper tuner boundary");
        check(K5Radio.fmKhz(8749) == -1 && K5Radio.fmKhz(10801) == -1, "reject out of band");
        check(K5Radio.fmKhz(-1) == -1, "reject missing frequency");
        check(K5Radio.logoMatchesFrequency("0000102049_Z100_10030.logo", 100300), "matching station logo");
        check(!K5Radio.logoMatchesFrequency("0000102049_Z100_10030.logo", 100500), "reject wrong station logo");
        check(!K5Radio.logoMatchesFrequency(null, 100300), "reject missing filename");
        check(!K5Radio.logoMatchesFrequency("10030.logo", 100300), "reject unknown filename format");
        check(K5Radio.channelFlag(1, true) && K5Radio.channelFlag(0, false), "OEM numeric flags");
        check(K5Radio.channelFlag(Boolean.TRUE, true) && K5Radio.channelFlag(Boolean.FALSE, false), "boolean flags");
        check(!K5Radio.channelFlag(-1, false) && !K5Radio.channelFlag(2, true), "reject unknown flags");
        check(!K5Radio.channelFlag(null, false) && !K5Radio.channelFlag("1", true), "reject invalid flag types");
        check(!K5Radio.channelFlag(0.5, true), "reject fractional flags");
        check(!K5Radio.supportedArtwork(null) && !K5Radio.supportedArtwork(new byte[7]), "reject absent/truncated artwork");
        check(K5Radio.supportedArtwork(new byte[]{(byte)137,80,78,71,13,10,26,10}), "PNG signature");
        check(K5Radio.supportedArtwork(new byte[]{(byte)255,(byte)216,(byte)255,0,0,0,0,0}), "JPEG signature");
        check(!K5Radio.supportedArtwork(new byte[8]), "reject unknown image format");
        byte[] oversized = new byte[131073];
        oversized[0]=(byte)255; oversized[1]=(byte)216; oversized[2]=(byte)255;
        check(!K5Radio.supportedArtwork(oversized), "reject oversized artwork");
        check(!K5Radio.metadataNewEnough(100300,1,99,100300,1,100),"older callback rejected");
        check(K5Radio.metadataNewEnough(100300,1,100,100300,1,100),"same timestamp accepted");
        check(K5Radio.metadataNewEnough(100300,1,101,100300,1,100),"new callback accepted");
        check(K5Radio.metadataNewEnough(100500,1,99,100300,1,100),"new frequency independent");
        check(K5Radio.metadataNewEnough(100300,2,99,100300,1,100),"new session independent");
        java.util.Set<Long> scan=new java.util.TreeSet<Long>(java.util.Arrays.asList(99100L,100300L));
        java.util.Map<Long,String> observed=new java.util.TreeMap<Long,String>();
        observed.put(100300L,"Z100");observed.put(101100L,"Observed");
        java.util.Set<Long> retained=K5Radio.selectFmScan(scan,java.util.Collections.<Long>emptySet());
        check(retained.equals(scan),"empty OEM scan preserves actual OEM frequencies even with observed stations");
        java.util.List<Object> items=K5Radio.fmItemsFor(retained,observed);
        check(items.size()==3,"merge OEM and observed without duplicates");
        check("Z100".equals(((java.util.Map<?,?>)items.get(1)).get("mediaItemName")),"observed name on OEM station");
        java.util.Set<Long> replacement=K5Radio.selectFmScan(retained,java.util.Collections.singleton(102300L));
        check(replacement.size()==1&&replacement.contains(102300L),"nonempty OEM scan replaces prior scan");
        check(K5Radio.fmItemsFor(replacement,observed).size()==3,"observations remain separate after replacement");
        check(K5Radio.fmItemsFor(java.util.Collections.<Long>emptySet(),observed).size()==2,"observed-only fallback");
        java.util.Set<Integer> sxmKnown=java.util.Collections.singleton(10);
        java.util.Set<Integer> sxmExcluded=java.util.Collections.singleton(20);
        check(!K5Radio.sxmCatalogCandidate(20,sxmKnown,sxmExcluded),"full catalog cannot restore excluded channel");
        check(!K5Radio.sxmCatalogCandidate(10,sxmKnown,sxmExcluded),"duplicate channel omitted");
        check(!K5Radio.sxmCatalogCandidate(-1,sxmKnown,sxmExcluded),"invalid channel omitted");
        check(K5Radio.sxmCatalogCandidate(30,sxmKnown,sxmExcluded),"eligible full catalog candidate");
        check(K5Radio.retainSxmSnapshot(0,0,true),"transient empty catalog retained");
        check(!K5Radio.retainSxmSnapshot(0,3,true),"explicit all-excluded catalog replaces old catalog");
        check(!K5Radio.retainSxmSnapshot(1,0,true),"eligible catalog replaces old catalog");
        check(!K5Radio.retainSxmSnapshot(0,0,false),"no previous catalog to retain");
        System.out.println("Radio policy regressions passed");
    }
}
