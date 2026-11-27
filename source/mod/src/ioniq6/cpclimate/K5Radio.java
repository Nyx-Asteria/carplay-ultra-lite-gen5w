package ioniq6.cpclimate;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import hmgapi.hook.ModContext;

/** Stock North American radio handoff. Package confirmed in DL322 260507 report.
 * Never sends generic media keys to CarPlay or guesses OEM tuner transactions. */
final class K5Radio {
    static final String PACKAGE = "com.hkmc.av.app.radiona";
    final Context context;
    final ModContext mod;
    final VsChannel.Log log;
    final Handler main = new Handler(Looper.getMainLooper());
    final Intent launch;
    K5Radio(Context context, ModContext mod, VsChannel.Log log) {
        this.context=context; this.mod=mod; this.log=log;
        Intent found=null;
        if (enabled()) try {
            Intent candidate=context.getPackageManager().getLaunchIntentForPackage(PACKAGE);
            if (candidate!=null && candidate.getComponent()!=null
                    && PACKAGE.equals(candidate.getComponent().getPackageName())
                    && context.getPackageManager().resolveActivity(candidate,0)!=null) found=candidate;
        } catch (Exception e) { log.log("[radio] discovery failed: "+e.getClass().getSimpleName()); }
        launch=found;
        listenMetadata();
        listenSxmCatalog();
        log.log("[radio] stock radio launch available="+(launch!=null)+"; FM tuner subject to runtime service and firmware checks");
    }
    static final String METADATA_ACTION = "ioniq6.cpclimate.RADIO_METADATA";
    private volatile StationName stationName;
    private volatile StationArtwork stationArtwork;
    private final Object tagLock = new Object();
    private long tagFrequency = -1, tagGeneration = -1;
    private final String[] hdTags = {"", "", ""};
    private void clearTags() {
        synchronized (tagLock) {
            tagFrequency = -1;
            tagGeneration = -1;
            java.util.Arrays.fill(hdTags, "");
        }
    }
    private void receiveTag(long khz, int tag, String value) {
        if (tag < 0 || tag > 2 || value == null || value.length() > 256) return;
        synchronized (tagLock) {
            long generation = CpClimate.sessionGeneration;
            if (tagFrequency != khz || tagGeneration != generation) {
                java.util.Arrays.fill(hdTags, "");
                tagFrequency = khz;
                tagGeneration = generation;
            }
            hdTags[tag] = value.trim();
        }
    }
    private void publishTags(java.util.Map<Long,Object> values, long khz) {
        synchronized (tagLock) {
            if (tagFrequency != khz || tagGeneration != CpClimate.sessionGeneration) return;
            if (!hdTags[0].isEmpty()) values.put(4355L, hdTags[0]);
            values.put(4356L, hdTags[1]);
            values.put(4357L, hdTags[2]);
        }
    }
    private static final byte[] EMPTY_ARTWORK = new byte[0];
    private static final class StationArtwork {
        final long khz, generation;
        final byte[] bytes;
        StationArtwork(long khz, long generation, byte[] bytes) {
            this.khz = khz; this.generation = generation; this.bytes = bytes;
        }
    }
    // Observed OEM logo naming: 0000102049_Z100_10030.logo (100.3 MHz).
    // Withhold unknown names rather than assigning a delayed image to another station.
    static boolean logoMatchesFrequency(String file, long khz) {
        return file != null && file.toLowerCase(java.util.Locale.US).endsWith("_" + (khz / 10) + ".logo");
    }
    static boolean supportedArtwork(byte[] b) {
        if (b == null || b.length < 8 || b.length > 131072) return false;
        boolean png = (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71
                && b[4]==13 && b[5]==10 && b[6]==26 && b[7]==10;
        boolean jpeg = (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255;
        return png || jpeg;
    }
    private static final class StationName {
        final long khz, generation, received;
        final String name;
        StationName(long khz, long generation, long received, String name) {
            this.khz = khz; this.generation = generation; this.received = received; this.name = name;
        }
    }
    private void listenMetadata() {
        try {
            context.registerReceiver(new android.content.BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    if (!enabled() || !CpClimate.sessionActive || !fmActive) return;
                    long khz = i.getLongExtra("frequency", -1);
                    long received = i.getLongExtra("received", -1);
                    long now = android.os.SystemClock.elapsedRealtime();
                    String name = i.getStringExtra("name");
                    if (khz < 87500 || khz > 108000 || received < 0 || now < received
                            || now - received > 2000) return;
                    if (khz != lastFmFrequency) return;
                    if (name != null && name.length() <= 256) {
                        stationName = new StationName(khz, CpClimate.sessionGeneration, received, name);
                    }
                    receiveTag(khz, i.getIntExtra("tag", -1), i.getStringExtra("text"));
                    byte[] image = i.getByteArrayExtra("artwork");
                    if (image != null && logoMatchesFrequency(i.getStringExtra("file"), khz)) {
                        if (supportedArtwork(image)) {
                            StationArtwork previous=stationArtwork;
                            stationArtwork = new StationArtwork(khz, CpClimate.sessionGeneration, image);
                            if(previous==null || previous.khz!=khz || previous.generation!=CpClimate.sessionGeneration || !java.util.Arrays.equals(previous.bytes,image))
                                log.log("[radio-meta] station logo received " + khz + " kHz bytes=" + image.length);
                        } else log.log("[radio-meta] unsupported station logo encoding bytes=" + image.length);
                    }
                }
            }, new android.content.IntentFilter(METADATA_ACTION), "android.permission.DEVICE_POWER", main);
        } catch (Throwable t) { log.log("[radio-meta] receiver unavailable: " + t.getClass().getSimpleName()); }
    }
    private volatile long lastFmFrequency = -1;
    private volatile String probeStatus = "Ready";
    private volatile boolean fmActive, sxmActive, sxmAvailable;
    private Object sxmTuner;
    private static final String SXM_IFACE = "com.hkmc.media.sxm65h.ISXM65HService";
    private Object fmTuner, fmAudio;
    private String lastServiceHealth="";
    private void serviceHealth(String state){if(!state.equals(lastServiceHealth)){lastServiceHealth=state;log.log("[radio-service] "+state);}}
    private static final String RADIO_IFACE = "com.hkmc.media.IRadioService";

    /** North American OEM FM values are in 10 kHz units. Reject unknown/out-of-band readings. */
    static long fmKhz(int raw) {
        return raw >= 8750 && raw <= 10800 ? raw * 10L : -1L;
    }

    private int readInt(Object proxy, String iface, String method) throws Exception {
        if (proxy == null) return -1;
        java.lang.reflect.Method m = Class.forName(iface).getMethod(method);
        if (m.getReturnType() != int.class) return -1;
        return ((Integer)m.invoke(proxy)).intValue();
    }

    // Read the OEM cached scan results, never start a scan or retune.
    // DL322 automotive.odex: RadioAllBsmFreqInfo.mBsmFreq (int),
    // mIsBsmValidStaton (boolean). Keep the OEM spelling.
    private long lastListPoll;
    private String lastScanSummary = "";
    private java.util.List<Object> fmItems = java.util.Collections.emptyList();
    // Observed stations are a fallback, not a claimed complete RF scan.
    private final java.util.TreeMap<Long,String> observedFm = new java.util.TreeMap<Long,String>();
    private long observedGeneration = -1;
    private java.util.List<Object> readFmItems(Object tuner) throws Exception {
        Object raw = Class.forName(RADIO_IFACE).getMethod("getRadioAllBsmFreqList").invoke(tuner);
        if (!(raw instanceof java.util.List)) {
            String summary = "unexpected cache type=" + (raw == null ? "null" : raw.getClass().getName());
            if (!summary.equals(lastScanSummary)) { lastScanSummary = summary; log.log("[radio] FM scan cache " + summary); }
            throw new IllegalStateException("unexpected FM cache type");
        }
        Class<?> type = Class.forName("com.hkmc.media.RadioAllBsmFreqInfo");
        java.lang.reflect.Field freq = type.getDeclaredField("mBsmFreq");
        java.lang.reflect.Field valid = type.getDeclaredField("mIsBsmValidStaton");
        freq.setAccessible(true); valid.setAccessible(true);
        java.util.TreeSet<Long> frequencies = new java.util.TreeSet<Long>();
        java.util.List<?> entries = (java.util.List<?>) raw;
        int invalid = 0, unexpected = 0, offGrid = 0;
        for (Object entry : entries) {
            if (!type.isInstance(entry)) { unexpected++; continue; }
            if (!valid.getBoolean(entry)) { invalid++; continue; }
            long candidate = fmKhz(freq.getInt(entry));
            if (candidate < 87900 || candidate > 107900 || (candidate - 87900) % 200 != 0) offGrid++;
            else frequencies.add(candidate);
        }
        String scanSummary = "raw=" + entries.size() + " invalid=" + invalid
                + " unexpected=" + unexpected + " offGrid=" + offGrid + " unique=" + frequencies.size();
        if (!scanSummary.equals(lastScanSummary)) {
            lastScanSummary = scanSummary;
            log.log("[radio] FM scan cache " + scanSummary);
        }
        return fmItemsFor(frequencies);
    }

    static boolean retainFmCache(int previous,int next) { return previous>0 && next==0; }

    private java.util.List<Object> fmItemsFor(java.util.Set<Long> frequencies) {
        java.util.TreeSet<Long> all = new java.util.TreeSet<Long>(frequencies);
        all.addAll(observedFm.keySet());
        java.util.List<Object> items = new java.util.ArrayList<Object>();
        for (Long khz : all) {
            String label = String.format(java.util.Locale.US, "%.1f MHz", khz / 1000.0);
            java.util.Map<String,Object> item = new java.util.LinkedHashMap<String,Object>();
            item.put("identifier", Long.toString(khz));
            String name = observedFm.get(khz);
            if (name == null || name.isEmpty()) name = label;
            item.put("mediaItemName", name);
            item.put("mediaItemShortName", name);
            item.put("mediaItemType", "fm");
            item.put("mediaItemCategory", 0L);
            item.put("frequency", khz);
            items.add(item);
        }
        return items;
    }

    /** Read-only FM state; never change the OEM source merely because CarPlay connects. */
    java.util.Map<Long,Object> refreshFm() {
        java.util.Map<Long,Object> values = new java.util.LinkedHashMap<Long,Object>();
        values.put(4115L, true);
        values.put(4116L, "");
        values.put(4118L, java.util.Collections.emptyList());
        values.put(4354L, "");
        values.put(4355L, "");
        values.put(4356L, "");
        values.put(4357L, "");
        if (VehicleState.sxmSchemaTest) {
            values.put(4131L, true);
            values.put(4133L, "");
            values.put(4134L, java.util.Collections.emptyList());
        }
        values.put(4358L, EMPTY_ARTWORK);
        values.put(4359L, 0L);
        values.put(4117L, ClimateMap.BLANK);
        if (!enabled() || !CpClimate.sessionActive) {
            fmActive = false; sxmActive = false; sxmAvailable = false;
            stationName = null; stationArtwork = null; clearTags(); lastFmFrequency = -1;
            sxmArt = null; currentSxmChannel = -1; fullSxm = null; sxmLive.clear();
            sxmKnownGeneration = -1; sxmKnownIds = java.util.Collections.emptySet();
            lastListPoll = 0; fmItems = java.util.Collections.emptyList();
            fmTuner=null; fmAudio=null; sxmTuner=null; lastServiceHealth="";
            return values;
        }
        try {
            if (!"7c99638e39c369c45fab9c485b103cf0".equals(ModContext.md5("/system/framework/automotive.odex"))) { fmActive = false; sxmActive = false; sxmAvailable = false; return values; }
            if (fmAudio == null) fmAudio = service(CarAudio.SERVICE, CarAudio.IFACE);
            long khz=-1;
            try {
                if (fmTuner == null) fmTuner = service(serviceName("RADIO_SERVICE"), RADIO_IFACE);
                khz=fmKhz(readInt(fmTuner,RADIO_IFACE,"getLastFMFrequency"));
                if(khz<=0)fmTuner=null;
            }
            catch(Throwable t){fmTuner=null; lastListPoll=0; fmItems=java.util.Collections.emptyList();}
            int mode = readInt(fmAudio, CarAudio.IFACE, "getAudioMode");
            int stream = readInt(fmAudio, CarAudio.IFACE, "getCurStreamType");
            if(mode<0||stream<0)fmAudio=null;
            serviceHealth("FM="+(fmTuner!=null&&khz>0)+" audio="+(fmAudio!=null)+" mode="+mode+" stream="+stream);
            // RC4 observed FM mode 2 / stream 38, AM mode 1 / stream 39.
            activationLog(activationTrace.sample(CpClimate.sessionGeneration,tuneRequest.get(),mode,stream,android.os.SystemClock.elapsedRealtime()));
            fmActive = khz > 0 && mode == 2 && stream == 38;
            sxmActive = mode == 4 && stream == 41;
            refreshSxm(values);
            if (!fmActive) { stationName = null; stationArtwork = null; clearTags(); lastFmFrequency = -1; }
            if (observedGeneration != CpClimate.sessionGeneration) {
                observedFm.clear(); fmItems = java.util.Collections.emptyList(); lastListPoll = 0;
                observedGeneration = CpClimate.sessionGeneration;
            }
            if (fmActive) {
                if (lastFmFrequency != khz) {
                    StationName previous = stationName;
                    if (previous != null && previous.khz != khz) stationName = null;
                    StationArtwork art = stationArtwork;
                    if (art != null && art.khz != khz) stationArtwork = null;
                    synchronized (tagLock) { if (tagFrequency != khz) clearTags(); }
                    lastFmFrequency = khz;
                }
                values.put(4117L, khz);
                values.put(4354L, "FM");
                StationName current = stationName;
                String title = String.format(java.util.Locale.US, "%.1f MHz", khz / 1000.0);
                if (current != null && current.khz == khz && current.generation == CpClimate.sessionGeneration
                        && !current.name.isEmpty()) title = current.name;
                values.put(4355L, title);
                if (khz >= 87900 && khz <= 107900 && (khz - 87900) % 200 == 0) {
                    if (current != null && current.khz == khz && current.generation == CpClimate.sessionGeneration
                            && !current.name.isEmpty()) observedFm.put(khz, current.name);
                    else if (!observedFm.containsKey(khz)) observedFm.put(khz, "");
                }
                publishTags(values, khz);
                StationArtwork art = stationArtwork;
                if (art != null && art.khz == khz && art.generation == CpClimate.sessionGeneration)
                    values.put(4358L, art.bytes);
                values.put(4359L, 1L);
            }
            // Advertise an available FM source even before it is selected.
            // The command path still checks active source ownership before tuning.
            values.put(4115L, !(fmTuner != null && fmAudio != null && khz > 0));
            values.put(4116L, fmActive ? Long.toString(khz) : "");
            if (fmTuner != null && fmAudio != null && khz > 0) {
                long now = android.os.SystemClock.elapsedRealtime();
                if (lastListPoll == 0 || now - lastListPoll >= 10000) {
                    lastListPoll = now;
                    try {
                        java.util.List<Object> next = readFmItems(fmTuner);
                        if (!next.equals(fmItems)) log.log("[radio] cached OEM FM station list size=" + next.size());
                        if (retainFmCache(fmItems.size(),next.size())) {
                            log.log("[radio] empty FM cache; preserving previous stations count="+fmItems.size());
                        } else fmItems = next;
                    } catch (Throwable t) {
                        log.log("[radio] station list unavailable: " + t.getClass().getSimpleName()+"; retained="+fmItems.size());
                    }
                }
                java.util.Set<Long> cached = new java.util.TreeSet<Long>();
                for (Object item : fmItems) {
                    if (!(item instanceof java.util.Map)) continue;
                    Object frequency = ((java.util.Map<?,?>)item).get("frequency");
                    if (frequency instanceof Long) cached.add((Long)frequency);
                }
                values.put(4118L, fmItemsFor(cached));
            }
        } catch (Throwable t) {
            fmActive=false; sxmActive=false; sxmAvailable=false; fmAudio=null;
            stationName=null; stationArtwork=null; clearTags(); lastFmFrequency=-1;
            currentSxmChannel=-1; sxmArt=null; sxmLive.clear();
            activationLog(activationTrace.sample(CpClimate.sessionGeneration,tuneRequest.get(),-1,-1,android.os.SystemClock.elapsedRealtime()));
            serviceHealth("audio sample unavailable: "+t.getClass().getSimpleName());
        }
        return values;
    }

    // Experimental Apple source schema remains explicitly opt-in. Native
    // ownership is tracked even without it to prevent forwarding radio keys to the phone.
    private void refreshSxm(java.util.Map<Long,Object> values) {
        sxmAvailable = false;
        if (!VehicleState.sxmSchemaTest) return;
        try {
            if (sxmTuner == null) sxmTuner = service(serviceName("SXM65H_SERVICE"), SXM_IFACE);
            int channel = readInt(sxmTuner, SXM_IFACE, "getLastTunedChannelID");
            sxmAvailable = sxmTuner != null && channel >= 0;
            if(!sxmAvailable)sxmTuner=null;
            values.put(4131L, !sxmAvailable);
            if (sxmAvailable) {
                if (sxmActive) requestSxmCatalog();
                try { refreshSxmCatalog(values, channel); }
                catch (Throwable t) { log.log("[radio-sxm] catalog unavailable: " + t.getClass().getSimpleName()); }
            }
            if (!sxmAvailable || !sxmActive) { currentSxmChannel = -1; sxmArt = null; sxmLive.clear(); return; }
            if (currentSxmChannel != channel) {
                sxmArt = null;
                sxmLive.channelChanged(CpClimate.sessionGeneration,channel);
                currentSxmChannel = channel;
            }
            if (sxmArt != null && sxmArt.generation != CpClimate.sessionGeneration) sxmArt = null;
            SxmArt art = sxmArt;
            if (art != null && art.channel == channel && art.generation == CpClimate.sessionGeneration)
                values.put(4358L, art.bytes);
            values.put(4133L, Integer.toString(channel));
            values.put(4354L, "SXM");
            values.put(4355L, "SiriusXM");
            values.put(4359L, 1L);
            // A channel ID is not necessarily its displayed channel number.
            // Publish text only when the OEM channel record matches exactly.
            refreshSxmFavoriteMetadata(values, channel);
            SxmLiveMetadata live=sxmLive.current(CpClimate.sessionGeneration,channel);
            if(live!=null&&live.matches(CpClimate.sessionGeneration,channel)){
                values.put(4355L,live.title.isEmpty()?(live.name.isEmpty()?"SiriusXM":live.name):live.title);
                values.put(4356L,live.artist);
            }
        } catch (Throwable t) { sxmTuner=null; sxmAvailable=false; currentSxmChannel=-1; sxmArt=null; sxmLive.clear(); values.put(4131L,true); }
    }

    private final SxmCatalogRequest sxmCatalogRequest = new SxmCatalogRequest();
    // These exact void binder methods request asynchronous catalog updates.
    // Do not change category filters, tune, or request bulk artwork here.
    private void requestSxmCatalog() {
        long generation = CpClimate.sessionGeneration;
        long now = android.os.SystemClock.elapsedRealtime();
        SxmSnapshot snapshot=fullSxm;
        if (!sxmCatalogRequest.begin(generation,now,snapshot!=null&&snapshot.generation==generation&&snapshot.ids.length>0)) return;
        try {
            Class<?> iface = Class.forName(SXM_IFACE);
            java.lang.reflect.Method categories = iface.getMethod("requestAllAvailableCategories");
            java.lang.reflect.Method channels = iface.getMethod("requestAllAvailableChannels");
            if (categories.getReturnType() != void.class || channels.getReturnType() != void.class
                    || !probeActive(generation) || !sxmActive || !VehicleState.sxmSchemaTest) return;
            categories.invoke(sxmTuner);
            if (!probeActive(generation) || !sxmActive) return;
            channels.invoke(sxmTuner);
            log.log("[radio-sxm] requested OEM category and channel catalog; awaiting asynchronous updates");
        } catch (Throwable t) { log.log("[radio-sxm] catalog request unavailable: " + t.getClass().getSimpleName()); }
    }

    private volatile SxmSnapshot fullSxm;
    private final SxmMetadataState sxmLive=new SxmMetadataState();
    private volatile SxmArt sxmArt;
    private volatile int currentSxmChannel = -1;
    private static final class SxmArt {
        final int channel;
        final long generation;
        final byte[] bytes;
        SxmArt(int channel, long generation, byte[] bytes) {
            this.channel = channel; this.generation = generation; this.bytes = bytes;
        }
    }
    private static final class SxmSnapshot {
        final long generation;
        final int[] ids;
        final String[] names, titles, artists;
        SxmSnapshot(long generation, int[] ids, String[] names, String[] titles, String[] artists) {
            this.generation = generation; this.ids = ids; this.names = names; this.titles = titles; this.artists = artists;
        }
    }
    private void listenSxmCatalog() {
        try {
            context.registerReceiver(new android.content.BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent i) {
                    if (!enabled() || !CpClimate.sessionActive || !VehicleState.sxmSchemaTest) return;
                    long received = i.getLongExtra("received", -1);
                    long age = android.os.SystemClock.elapsedRealtime() - received;
                    if (age < 0 || age > 2000) return;
                    if(i.hasExtra("liveChannel")){
                        int id=i.getIntExtra("liveChannel",-1);
                        long generation=CpClimate.sessionGeneration;
                        if(sxmActive&&id>=0){
                            boolean accepted=sxmLive.receive(new SxmLiveMetadata(generation,id,i.getStringExtra("liveName"),i.getStringExtra("liveTitle"),i.getStringExtra("liveArtist")),received,generation,currentSxmChannel);
                            if(!accepted)log.log("[radio-sxm] live callback withheld ID="+id+" observed="+currentSxmChannel);
                            else log.log("[radio-sxm] live callback accepted ID="+id);
                        }
                        return;
                    }
                    if (i.hasExtra("artChannel")) {
                        int id = i.getIntExtra("artChannel", -1);
                        byte[] bytes = i.getByteArrayExtra("artwork");
                        if (sxmActive && id >= 0 && id == currentSxmChannel && supportedArtwork(bytes))
                            sxmArt = new SxmArt(id, CpClimate.sessionGeneration, bytes);
                        return;
                    }
                    int[] ids = i.getIntArrayExtra("ids");
                    String[] names = i.getStringArrayExtra("names"), titles = i.getStringArrayExtra("titles"), artists = i.getStringArrayExtra("artists");
                    if (ids == null || ids.length > 512 || names == null || titles == null || artists == null
                            || names.length != ids.length || titles.length != ids.length || artists.length != ids.length) return;
                    // A transient empty response must not erase a previously usable catalog.
                    // Live lock/subscription/skip checks still govern every tuning command.
                    SxmSnapshot previous=fullSxm;
                    if(ids.length==0 && previous!=null && previous.generation==CpClimate.sessionGeneration && previous.ids.length>0){
                        log.log("[radio-sxm] empty catalog response; withholding replacement");
                        return;
                    }
                    fullSxm = new SxmSnapshot(CpClimate.sessionGeneration, ids, names, titles, artists);
                    log.log("[radio-sxm] full OEM catalog received count=" + ids.length);
                }
            }, new android.content.IntentFilter(SxmCatalogBridge.ACTION), "android.permission.DEVICE_POWER", main);
        } catch (Throwable t) { log.log("[radio-sxm] catalog receiver unavailable: " + t.getClass().getSimpleName()); }
    }
    private SxmSnapshot appliedSxm;
    private long sxmDiscoveryGeneration = -1;
    private int sxmCatalogChannel = -1;
    private final java.util.Map<Integer,String[]> sxmMetadata = new java.util.HashMap<Integer,String[]>();
    private volatile java.util.Set<Integer> sxmKnownIds = java.util.Collections.emptySet();
    private volatile long sxmKnownGeneration = -1;
    private long sxmCatalogPoll;
    private java.util.List<Object> sxmItems = java.util.Collections.emptyList();

    // The verified Smart Favorites API supplies a bounded initial catalog.
    // IDs are opaque OEM identifiers, never synthesized from channel numbers.
    private long loggedSxmGeneration=-1;
    private java.util.Set<Integer> loggedSxmIds=java.util.Collections.emptySet();
    private void refreshSxmCatalog(java.util.Map<Long,Object> values, int channelId) throws Exception {
        long now = android.os.SystemClock.elapsedRealtime();
        long generation = CpClimate.sessionGeneration;
        SxmSnapshot snapshot = fullSxm;
        if (snapshot != null && snapshot.generation != generation) snapshot = null;
        if (generation != sxmKnownGeneration || snapshot != appliedSxm || channelId != sxmCatalogChannel || now - sxmCatalogPoll >= 10000) {
            appliedSxm = snapshot;
            sxmCatalogChannel = channelId;
            sxmMetadata.clear();
            sxmCatalogPoll = now;
            sxmKnownGeneration = -1;
            sxmKnownIds = java.util.Collections.emptySet();
            sxmItems = java.util.Collections.emptyList();
            Object records = null;
            try {
                int category = readInt(sxmTuner, SXM_IFACE, "getSmartFavoritesCategoryID");
                if (category >= 0) records = Class.forName(SXM_IFACE).getMethod("getCategoryChannels", int.class)
                        .invoke(sxmTuner, category);
            } catch (Exception e) {
                log.log("[radio-sxm] Smart Favorites unavailable: " + e.getClass().getSimpleName());
            }
            // Discover read-only catalog accessors without invoking unknown methods.
            if (sxmDiscoveryGeneration != generation) {
                sxmDiscoveryGeneration = generation;
                for (java.lang.reflect.Method method : Class.forName(SXM_IFACE).getMethods()) {
                    String name = method.getName();
                    if (!name.startsWith("get") || !(name.contains("Channel") || name.contains("Categor"))) continue;
                    StringBuilder signature = new StringBuilder(name).append('(');
                    for (Class<?> parameter : method.getParameterTypes()) signature.append(parameter.getName()).append(',');
                    signature.append(") -> ").append(method.getReturnType().getName());
                    log.log("[radio-sxm] catalog accessor " + signature);
                }
            }
            Class<?> type = Class.forName("com.hkmc.media.sxm65h.Channel");
            if (records == null || !records.getClass().isArray()
                    || records.getClass().getComponentType() != type)
                records = java.lang.reflect.Array.newInstance(type, 0);
            java.lang.reflect.Field id = type.getDeclaredField("mChannelID");
            id.setAccessible(true);
            java.lang.reflect.Field subscribed = type.getDeclaredField("mIsSubscribed");
            java.lang.reflect.Field skipped = type.getDeclaredField("mIsSkipped");
            subscribed.setAccessible(true); skipped.setAccessible(true);
            java.util.Set<Integer> known = new java.util.HashSet<Integer>();
            java.util.List<Object> items = new java.util.ArrayList<Object>();
            for (int i = 0; i < Math.min(100, java.lang.reflect.Array.getLength(records)); i++) {
                Object entry = java.lang.reflect.Array.get(records, i);
                if (entry == null) continue;
                Object raw = id.get(entry);
                if (!(raw instanceof Number)) continue;
                int channel = ((Number)raw).intValue();
                if (channel >= 0) sxmMetadata.put(channel, new String[] {channelText(type, entry, "mChannelLongName"), "", ""});
                if (channel < 0 || !channelFlag(subscribed.get(entry), true)
                        || !channelFlag(skipped.get(entry), false) || known.contains(channel)) continue;
                // The DL322 binder exposes isChannelLocked(int): int.
                // Only an explicit zero permits direct tuning; do not bypass parental locks.
                if (!sxmUnlocked(sxmTuner, channel)) continue;
                known.add(channel);
                String name = channelText(type, entry, "mChannelLongName");
                if (name.isEmpty()) name = "SiriusXM";
                java.util.Map<String,Object> item = new java.util.LinkedHashMap<String,Object>();
                item.put("identifier", Integer.toString(channel));
                item.put("mediaItemName", name);
                item.put("mediaItemShortName", name);
                item.put("mediaItemCategory", 0L);
                items.add(item);
            }
            if (snapshot != null) {
                // Favor fresh per-channel favorites metadata over the older full catalog.
                for (int i = 0; i < snapshot.ids.length; i++) {
                    int channel = snapshot.ids[i];
                    if (channel < 0 || known.contains(channel)) continue;
                    if (!sxmUnlocked(sxmTuner, channel)) continue;
                    known.add(channel);
                    String name = snapshot.names[i];
                    if (name == null || name.isEmpty()) name = "SiriusXM";
                    java.util.Map<String,Object> item = new java.util.LinkedHashMap<String,Object>();
                    item.put("identifier", Integer.toString(channel));
                    item.put("mediaItemName", name);
                    item.put("mediaItemShortName", name);
                    item.put("mediaItemCategory", 0L);
                    items.add(item);
                    // Full catalog is a name snapshot, not authoritative live track metadata.
                    if (!sxmMetadata.containsKey(channel)) sxmMetadata.put(channel, new String[]{name, "", ""});
                }
            }
            sxmItems = items;
            sxmKnownIds = java.util.Collections.unmodifiableSet(known);
            sxmKnownGeneration = generation;
            if(loggedSxmGeneration!=generation || !loggedSxmIds.equals(known)) {
                loggedSxmGeneration=generation;
                loggedSxmIds=new java.util.HashSet<Integer>(known);
                log.log("[radio-sxm] eligible catalog size=" + items.size()+" observedID="+channelId+" currentIDListed="+known.contains(channelId));
            }
        }
        values.put(4134L, sxmItems);
    }

    private void refreshSxmFavoriteMetadata(java.util.Map<Long,Object> values, int channel) {
        if (sxmKnownGeneration != CpClimate.sessionGeneration) return;
        String[] metadata = sxmMetadata.get(channel);
        if (metadata == null) return;
        // Smart Favorites is a catalog, not proof of the currently playing track.
        if (!metadata[0].isEmpty()) values.put(4355L, metadata[0]);
    }
    // OEM channel records have been observed with numeric 0/1 flags.
    // Accept boolean representations too; unknown values are not authorization.
    static boolean channelFlag(Object value, boolean expected) {
        if (value instanceof Boolean) return ((Boolean)value).booleanValue() == expected;
        if (value instanceof Number) {
            double n = ((Number)value).doubleValue();
            return n == (expected ? 1 : 0);
        }
        return false;
    }

    private int readIntWithChannel(Object tuner, String name, int channel) throws Exception {
        java.lang.reflect.Method m = Class.forName(SXM_IFACE).getMethod(name, int.class);
        return tuner != null && m.getReturnType() == int.class ? ((Integer)m.invoke(tuner, channel)).intValue() : -1;
    }

    private boolean sxmUnlocked(Object tuner, int channel) {
        try {
            if (tuner == null) return false;
            java.lang.reflect.Method method = Class.forName(SXM_IFACE).getMethod("isChannelLocked", int.class);
            return method.getReturnType() == int.class
                    && Integer.valueOf(0).equals(method.invoke(tuner, channel));
        } catch (Throwable t) { return false; }
    }

    private static String channelText(Class<?> type, Object entry, String name) throws Exception {
        java.lang.reflect.Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(entry);
        if (!(value instanceof String)) return "";
        String text = ((String)value).trim();
        return text.substring(0, Math.min(256, text.length()));
    }

    // DL322 ODEX debug parameter names: tuneDirect(int channelID, boolean override).
    // Never request override; accept only IDs returned by the OEM catalog.
    synchronized boolean tuneSxm(String identifier) {
        final int channel;
        try { channel = Integer.parseInt(identifier); }
        catch (NumberFormatException e) { return false; }
        final long generation = CpClimate.sessionGeneration;
        if (!VehicleState.sxmSchemaTest || !sxmAvailable || !probeActive(generation)
                || sxmKnownGeneration != generation || !sxmKnownIds.contains(channel)) return false;
        final long station=stationRequest.incrementAndGet();
        long pending=pendingSource(13,generation);
        final long request=pending>=0?pending:tuneRequest.incrementAndGet();
        if (sxmActive) { queueSxmTune(channel, generation, request, station); return true; }
        if (pending<0&&!selectRadioMode(request, 13)) return false;
        final long deadline = android.os.SystemClock.elapsedRealtime() + 7000;
        main.postDelayed(new Runnable() {
            public void run() {
                if (!probeActive(generation) || request != tuneRequest.get() || station!=stationRequest.get()) return;
                if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    log.log("[radio-sxm] activation timed out; tune discarded"); return;
                }
                if (sxmActive) { queueSxmTune(channel, generation, request, station); return; }
                main.postDelayed(this, 250);
            }
        }, 250);
        return true;
    }
    private void queueSxmTune(final int channel, final long generation, final long request, final long station) {
        fmCommands.execute(new Runnable() {
            public void run() {
                try {
                    if (!probeActive(generation) || request != tuneRequest.get() || station!=stationRequest.get()
                            || !VehicleState.sxmSchemaTest || sxmKnownGeneration != generation
                            || !sxmKnownIds.contains(channel)) return;
                    Object audio = service(CarAudio.SERVICE, CarAudio.IFACE);
                    Object tuner = service(serviceName("SXM65H_SERVICE"), SXM_IFACE);
                    if (tuner == null || readInt(audio, CarAudio.IFACE, "getAudioMode") != 4
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 41) return;
                    java.lang.reflect.Method tune = Class.forName(SXM_IFACE).getMethod("tuneDirect", int.class, boolean.class);
                    int subscribed = readIntWithChannel(tuner, "isChannelSubscribed", channel);
                    int skipped = readIntWithChannel(tuner, "isChannelSkipped", channel);
                    boolean unlocked = sxmUnlocked(tuner, channel);
                    if (subscribed != 1 || skipped != 0 || !unlocked) {
                        log.log("[radio-sxm] tune refused ID=" + channel + " subscribed=" + subscribed
                                + " skipped=" + skipped + " unlocked=" + unlocked);
                        return;
                    }
                    if (tune.getReturnType() != int.class || !probeActive(generation)
                            || request != tuneRequest.get() || station!=stationRequest.get()) return;
                    Object result = tune.invoke(tuner, channel, false);
                    log.log("[radio-sxm] native tune ID=" + channel + " result=" + result + "; awaiting channel feedback");
                } catch (Throwable t) { log.log("[radio-sxm] tune failed: " + t.getClass().getSimpleName()); }
            }
        });
    }

    boolean sxmSelected() { return sxmActive; }
    synchronized boolean selectSxm() {
        if (!VehicleState.sxmSchemaTest || !sxmAvailable || !enabled() || !CpClimate.sessionActive) return false;
        return selectExplicitSource(13,sxmActive);
    }

    /** Queue a seek only for the already-active FM tuner; never route a radio seek to the phone.
     * Recheck source ownership on the worker immediately before the OEM command. */
    private final java.util.concurrent.ExecutorService fmCommands =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    synchronized boolean seekFm(final boolean up) {
        if (!enabled() || !CpClimate.sessionActive || !fmActive) {
            log.log("[radio] seek refused: FM not active"); return false;
        }
        final long generation = CpClimate.sessionGeneration;
        final long request = tuneRequest.incrementAndGet();
        final long station = stationRequest.incrementAndGet();
        sourceRequest.clear();
        log.log("[radio] FM seek queued direction="+(up?"up":"down")+" request="+request);
        fmCommands.execute(new Runnable() {
            public void run() {
                if (!probeActive(generation) || !currentRadioRequest(request,station,tuneRequest.get(),stationRequest.get())) {
                    log.log("[radio] stale FM seek discarded request="+request); return;
                }
                try {
                    Object audio = service(CarAudio.SERVICE, CarAudio.IFACE);
                    if (readInt(audio, CarAudio.IFACE, "getAudioMode") != 2
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 38) return;
                    if (!probeActive(generation) || !currentRadioRequest(request,station,tuneRequest.get(),stationRequest.get())) return;
                    Object tuner = service(serviceName("RADIO_SERVICE"), RADIO_IFACE);
                    if (tuner == null || !probeActive(generation) || !currentRadioRequest(request,station,tuneRequest.get(),stationRequest.get())) return;
                    java.lang.reflect.Method seek = Class.forName(RADIO_IFACE).getMethod(up ? "seekUp" : "seekDown");
                    if (seek.getReturnType() != void.class) {
                        log.log("[radio] FM seek withheld: unexpected return type="+seek.getReturnType().getName());
                        return;
                    }
                    // Recheck ownership after resolving the tuner, just as for direct tuning.
                    if (readInt(audio, CarAudio.IFACE, "getAudioMode") != 2
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 38
                            || !probeActive(generation) || !currentRadioRequest(request,station,tuneRequest.get(),stationRequest.get())) return;
                    seek.invoke(tuner);
                    log.log("[radio] native FM " + (up ? "seekUp" : "seekDown") + " dispatched; awaiting frequency feedback");
                } catch (Throwable t) { log.log("[radio] FM seek failed: " + t.getClass().getSimpleName()); }
            }
        });
        return true;
    }

    static boolean currentRadioRequest(long request,long station,long activeRequest,long activeStation) {
        return request==activeRequest && station==activeStation;
    }

    /** US FM: odd 200 kHz channels, OEM frequency in 10 kHz units.
     * RadioNA WrapperUtil identifies the boolean as isFMStep50KHz; the
     * two-argument MicomRadioManager overload supplies PI/PTy zero. */
    boolean tuneFm(final long khz) {
        return tuneFm(khz,tuneRequest.incrementAndGet(),stationRequest.incrementAndGet());
    }
    private boolean tuneFm(final long khz, final long request, final long station) {
        if (khz < 87900 || khz > 107900 || (khz - 87900) % 200 != 0
                || !enabled() || !CpClimate.sessionActive || !fmActive) return false;
        final long generation = CpClimate.sessionGeneration;
        fmCommands.execute(new Runnable() {
            public void run() {
                if (!probeActive(generation) || request != tuneRequest.get() || station!=stationRequest.get()) return;
                try {
                    Object audio = service(CarAudio.SERVICE, CarAudio.IFACE);
                    if (readInt(audio, CarAudio.IFACE, "getAudioMode") != 2
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 38) return;
                    Object tuner = service(serviceName("RADIO_SERVICE"), RADIO_IFACE);
                    if (tuner == null || !probeActive(generation) || request != tuneRequest.get() || station!=stationRequest.get()) return;
                    java.lang.reflect.Method method = Class.forName(RADIO_IFACE).getMethod(
                            "directTune", int.class, boolean.class, int.class, int.class);
                    if (method.getReturnType() != boolean.class || !probeActive(generation) || request != tuneRequest.get() || station!=stationRequest.get()
                            || readInt(audio, CarAudio.IFACE, "getAudioMode") != 2
                            || readInt(audio, CarAudio.IFACE, "getCurStreamType") != 38
                            || request != tuneRequest.get() || station!=stationRequest.get()) return;
                    Object result = method.invoke(tuner, (int)(khz / 10), false, 0, 0);
                    log.log("[radio] FM tune " + khz + " kHz accepted=" + result);
                } catch (Throwable t) { log.log("[radio] FM tune failed: " + t.getClass().getSimpleName()); }
            }
        });
        return true;
    }

    /** Explicit FM selection via the stock framework wrapper. Never invoked on connect.
     * DL322 260507 automotive.odex: ModeManager(Context), changeMode(Integer):boolean,
     * ModeConstants.ModeType FM1=2. Foreground behavior requires parked validation. */
    // A tune with explicit FM source may activate FM first. Only the latest
    // pending request is retained; it expires and cannot cross sessions.
    private final RadioActivationTrace activationTrace=new RadioActivationTrace();
    private void activationLog(String event){if(event!=null)log.log("[radio-source] "+event);}
    private final java.util.concurrent.atomic.AtomicLong tuneRequest = new java.util.concurrent.atomic.AtomicLong();
    private final RadioSourceRequest sourceRequest=new RadioSourceRequest();
    private final java.util.concurrent.atomic.AtomicLong stationRequest=new java.util.concurrent.atomic.AtomicLong();
    // Reuse a pending mode request, but let a newer station selection supersede its tune.
    private long pendingSource(int target,long generation) {
        long request=tuneRequest.get();
        return sourceRequest.matches(generation,request,target,android.os.SystemClock.elapsedRealtime())?request:-1;
    }
    synchronized void cancelPendingTune() { tuneRequest.incrementAndGet(); sourceRequest.clear(); }
    private boolean selectExplicitSource(int target,boolean owns) {
        long generation=CpClimate.sessionGeneration, now=android.os.SystemClock.elapsedRealtime();
        if(sourceRequest.matches(generation,tuneRequest.get(),target,now)) {
            log.log("[radio-source] duplicate selection coalesced target="+target);
            return true;
        }
        long request=tuneRequest.incrementAndGet();
        sourceRequest.clear();
        if(owns)return true;
        return selectRadioMode(request,target);
    }
    synchronized boolean selectAndTuneFm(final long khz) {
        if (khz < 87900 || khz > 107900 || (khz - 87900) % 200 != 0
                || !enabled() || !CpClimate.sessionActive) return false;
        final long generation = CpClimate.sessionGeneration;
        final long station=stationRequest.incrementAndGet();
        long pending=pendingSource(2,generation);
        final long request=pending>=0?pending:tuneRequest.incrementAndGet();
        if (fmActive) return tuneFm(khz, request, station);
        if (pending<0&&!selectFm(request)) return false;
        final long deadline = android.os.SystemClock.elapsedRealtime() + 5000;
        main.postDelayed(new Runnable() {
            public void run() {
                if (!probeActive(generation) || request != tuneRequest.get() || station!=stationRequest.get()) return;
                if (android.os.SystemClock.elapsedRealtime() >= deadline) {
                    log.log("[radio] FM activation timed out; pending tune discarded");
                    return;
                }
                if (fmActive) { tuneFm(khz, request, station); return; }
                main.postDelayed(this, 250);
            }
        }, 250);
        return true;
    }

    synchronized boolean selectFm() {
        if(!enabled()||!CpClimate.sessionActive)return false;
        return selectExplicitSource(2,fmActive);
    }

    private boolean selectFm(final long request) { return selectRadioMode(request, 2); }

    private boolean selectRadioMode(final long request, final int targetMode) {
        if (!enabled() || !CpClimate.sessionActive) return false;
        final long generation = CpClimate.sessionGeneration;
        sourceRequest.begin(generation,request,targetMode,android.os.SystemClock.elapsedRealtime());
        fmCommands.execute(new Runnable() {
            public void run() {
                if (!probeActive(generation) || request != tuneRequest.get()) return;
                try {
                    if (!"7c99638e39c369c45fab9c485b103cf0".equals(
                            ModContext.md5("/system/framework/automotive.odex"))) return;
                    // Construct the framework wrapper on a Looper thread, as its
                    // implementation can create Handler-backed listener transports.
                    main.post(new Runnable() {
                        public void run() {
                            if (!probeActive(generation) || request != tuneRequest.get()) return;
                            try {
                                Class<?> type = Class.forName("com.hkmc.mode.ModeManager");
                                java.lang.reflect.Method change = type.getMethod("changeMode", Integer.class);
                                if (change.getReturnType() != boolean.class) return;
                                Object manager = type.getConstructor(Context.class).newInstance(context);
                                if (!probeActive(generation) || request != tuneRequest.get()) return;
                                activationLog(activationTrace.begin(generation,request,targetMode,android.os.SystemClock.elapsedRealtime()));
                                Object accepted = change.invoke(manager, Integer.valueOf(targetMode));
                                activationLog(activationTrace.result(generation,request,Boolean.TRUE.equals(accepted),android.os.SystemClock.elapsedRealtime()));
                                if(!Boolean.TRUE.equals(accepted))sourceRequest.clearIf(generation,request);
                                log.log("[radio] OEM mode " + targetMode + " request accepted=" + accepted);
                            } catch (Throwable t) {
                                sourceRequest.clearIf(generation,request);
                                log.log("[radio] mode selection failed: " + t.getClass().getSimpleName());
                            }
                        }
                    });
                } catch (Throwable t) {
                    log.log("[radio] mode selection unavailable: " + t.getClass().getSimpleName());
                }
            }
        });
        return true;
    }

    boolean fmSelected() { return fmActive; }

    String diagnosticStatus() { return probeStatus; }
    private boolean probing;
    /** Explicit, bounded diagnostic run. Poll only known read-only primitive getters.
     * No manager construction, listener registration, tuning or source changes. */
    private synchronized void startProbe() {
        if (probing || !enabled()) return;
        probing = true;
        probeStatus = "Collecting (2 min)";
        // Match the system-side permission-gated, two-minute observation window.
        try {
            if (android.os.Process.myUid() == 1000 &&
                    context.checkCallingOrSelfPermission("android.permission.DEVICE_POWER")
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                context.sendBroadcast(new Intent("ioniq6.cpclimate.RADIO_TRACE")
                        .setPackage("android")
                        .putExtra("issued", android.os.SystemClock.elapsedRealtime()),
                        "android.permission.DEVICE_POWER");
                log.log("[radio-probe] requested system mode/key trace");
            } else log.log("[radio-probe] system trace permission unavailable; polling only");
        } catch (Throwable t) {
            log.log("[radio-probe] system trace request failed: " + t.getClass().getSimpleName());
        }
        final long generation = CpClimate.sessionGeneration;
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try { probeTuner(generation); }
                finally { synchronized (K5Radio.this) { probing = false; probeStatus = "Finished — see log"; } }
            }
        }, "k5-radio-probe");
        worker.setDaemon(true);
        worker.start();
    }
    private boolean probeActive(long generation) {
        return enabled() && CpClimate.sessionActive && generation == CpClimate.sessionGeneration;
    }
    private Object service(String name, String ifaceName) throws Exception {
        android.os.IBinder binder = CarClimate.service(name);
        if (binder == null || !ifaceName.equals(binder.getInterfaceDescriptor())) return null;
        Class<?> iface = Class.forName(ifaceName);
        Object proxy = Class.forName(ifaceName + "$Stub")
                .getMethod("asInterface", android.os.IBinder.class).invoke(null, binder);
        return iface.isInstance(proxy) ? proxy : null;
    }
    private String serviceName(String fieldName) throws Exception {
        java.lang.reflect.Field field = Class.forName("com.hkmc.Service").getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(null);
        if (!(value instanceof String) || ((String)value).length() == 0)
            throw new IllegalStateException("invalid service name");
        return (String)value;
    }
    private void collect(String label, Object proxy, String ifaceName, String[] getters,
                         java.util.Map<String,String> previous) {
        if (proxy == null) return;
        for (String getter : getters) {
            String key = label + "." + getter;
            String value;
            try {
                java.lang.reflect.Method method = Class.forName(ifaceName).getMethod(getter);
                Class<?> type = method.getReturnType();
                if (type != int.class && type != boolean.class) continue;
                value = String.valueOf(method.invoke(proxy));
            } catch (Throwable t) { value = "unavailable:" + t.getClass().getSimpleName(); }
            if (!value.equals(previous.put(key, value))) log.log("[radio-probe] " + key + " raw=" + value);
        }
    }
    // Inventory exact runtime signatures without invoking unknown getters or registering
    // unverified listeners. This is only run from the explicit radio diagnostic.
    private void inventory(String name) {
        try {
            Class<?> type = Class.forName(name);
            java.util.TreeSet<String> signatures = new java.util.TreeSet<String>();
            for (java.lang.reflect.Method method : type.getMethods()) {
                String n = method.getName().toLowerCase(java.util.Locale.US);
                if (n.matches(".*(radio|station|preset|channel|listener|artwork|image|text|title|artist|sis|id3|seek|tune|mode|status|frequency|program).*")) {
                    signatures.add(method.toGenericString());
                }
            }
            for (String signature : signatures) log.log("[radio-api] " + signature);
        } catch (Throwable t) { log.log("[radio-api] " + name + ": " + t.getClass().getSimpleName()); }
    }

    /** Read-only OEM smart favorites discovery. Channel IDs are not assumed
     * to be display channel numbers; no subscription or tuning changes. */
    private void probeSxmFavorites(Object satellite, String iface, java.util.Map<String,String> previous) {
        if (satellite == null) return;
        try {
            int category = readInt(satellite, iface, "getSmartFavoritesCategoryID");
            if (category < 0) return;
            Object channels = Class.forName(iface).getMethod("getCategoryChannels", int.class)
                    .invoke(satellite, category);
            if (channels == null || !channels.getClass().isArray()) return;
            Class<?> channel = Class.forName("com.hkmc.media.sxm65h.Channel");
            if (channels.getClass().getComponentType() != channel) return;
            String[] names = {"mChannelID", "mChannelLongName", "mCategoryLongName",
                    "mTitle", "mArtist", "mImageName", "mIsSubscribed", "mIsSkipped"};
            java.lang.reflect.Field[] fields = new java.lang.reflect.Field[names.length];
            for (int i=0; i<names.length; i++) {
                fields[i] = channel.getDeclaredField(names[i]);
                fields[i].setAccessible(true);
            }
            int count = java.lang.reflect.Array.getLength(channels);
            String summary = "category=" + category + " count=" + count;
            if (!summary.equals(previous.put("sxm.favorites", summary)))
                log.log("[radio-sxm] smart favorites " + summary);
            for (int i=0; i<Math.min(count, 100); i++) {
                Object entry = java.lang.reflect.Array.get(channels, i);
                if (entry == null) continue;
                StringBuilder line = new StringBuilder();
                for (java.lang.reflect.Field field : fields) {
                    String value = String.valueOf(field.get(entry)).replace('\n', ' ').replace('\r', ' ');
                    if (value.length() > 128) value = value.substring(0,128);
                    line.append(field.getName()).append('=').append(value).append(' ');
                }
                String value = line.toString();
                if (!value.equals(previous.put("sxm.favorite." + i, value)))
                    log.log("[radio-sxm] " + value);
            }
        } catch (Throwable t) {
            String error = t.getClass().getSimpleName();
            if (!error.equals(previous.put("sxm.favorites.error", error)))
                log.log("[radio-sxm] smart favorites unavailable: " + error);
        }
    }

    private void probeTuner(long generation) {
        if (!probeActive(generation)) return;
        try {
            if (!"7c99638e39c369c45fab9c485b103cf0".equals(ModContext.md5("/system/framework/automotive.odex"))) {
                log.log("[radio-probe] unknown framework; withheld");
                return;
            }
            String radio = "com.hkmc.media.IRadioService";
            String sxm = "com.hkmc.media.sxm65h.ISXM65HService";
            String modeIface = "com.hkmc.mode.IModeManager";
            Object tuner = null, audio = null, satellite = null, mode = null;
            java.util.Map<String,String> previous = new java.util.HashMap<String,String>();
            long deadline = android.os.SystemClock.elapsedRealtime() + 120000L;
            long nextSxmProbe = 0;
            log.log("[radio-probe] start: 120 seconds, change stock bands/stations manually");
            for (String type : new String[]{radio, sxm, modeIface,
                    "com.hkmc.media.MicomRadioManager", "com.hkmc.media.MicomRadioListener",
                    "com.hkmc.media.MicomRadioStatus", "com.hkmc.media.MicomRadioPresetData",
                    "com.hkmc.media.RdsStationInfo", "com.hkmc.media.HDRadioManager",
                    "com.hkmc.media.HDRadioListener", "com.hkmc.media.sxm65h.Manager"}) {
                if (!probeActive(generation)) return;
                inventory(type);
            }
            while (probeActive(generation) && android.os.SystemClock.elapsedRealtime() < deadline) {
                try { if (tuner == null) tuner = service(serviceName("RADIO_SERVICE"), radio); }
                catch (Throwable ignored) {}
                try { if (audio == null) audio = service(CarAudio.SERVICE, CarAudio.IFACE); }
                catch (Throwable ignored) {}
                try { if (satellite == null) satellite = service(serviceName("SXM65H_SERVICE"), sxm); }
                catch (Throwable ignored) {}
                try { if (mode == null) mode = service("com.hkmc.micom.mode", modeIface); }
                catch (Throwable ignored) {}
                String availability = "radio=" + (tuner != null) + " audio=" + (audio != null) + " sxm=" + (satellite != null) + " mode=" + (mode != null);
                if (!availability.equals(previous.put("availability", availability))) log.log("[radio-probe] " + availability);
                collect("radio", tuner, radio, new String[]{"getLastFMFrequency", "getLastFM2Frequency",
                        "getLastAMFrequency", "getTuningValue", "getLastPresetNumber"}, previous);
                collect("audio", audio, CarAudio.IFACE, new String[]{"getAudioMode", "getCurStreamType"}, previous);
                collect("mode", mode, modeIface, new String[]{"getCurrentMode", "getCurrentReadyMode",
                        "getLastRadioMode", "isAvailableCurrentMode", "isChangeModeScheduled"}, previous);
                collect("sxm", satellite, sxm, new String[]{"getLastTunedChannelID"}, previous);
                // Correlate tuner ownership with the channel ID in one sample.
                // Last tuned channel alone is not evidence that SXM owns audio.
                if (audio != null && satellite != null) try {
                    int audioMode = readInt(audio, CarAudio.IFACE, "getAudioMode");
                    int stream = readInt(audio, CarAudio.IFACE, "getCurStreamType");
                    int channel = readInt(satellite, sxm, "getLastTunedChannelID");
                    int current = -1, ready = -1;
                    if (mode != null) {
                        try { current = readInt(mode, modeIface, "getCurrentMode"); } catch (Throwable ignored) {}
                        try { ready = readInt(mode, modeIface, "getCurrentReadyMode"); } catch (Throwable ignored) {}
                    }
                    String tuple = "audioMode=" + audioMode + " stream=" + stream
                            + " currentMode=" + current + " readyMode=" + ready + " sxmChannelID=" + channel;
                    if (!tuple.equals(previous.put("sourceTuple", tuple)))
                        log.log("[radio-source] " + tuple);
                } catch (Throwable t) {
                    String error = t.getClass().getSimpleName();
                    if (!error.equals(previous.put("sourceTuple.error", error)))
                        log.log("[radio-source] sample unavailable: " + error);
                }
                long now = android.os.SystemClock.elapsedRealtime();
                if (now >= nextSxmProbe) {
                    nextSxmProbe = now + 10000L;
                    probeSxmFavorites(satellite, sxm, previous);
                }
                Thread.sleep(1500);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        catch (Throwable t) { log.log("[radio-probe] discovery unavailable: " + t.getClass().getSimpleName()); }
        finally { log.log("[radio-probe] finished"); }
    }
    boolean enabled() { return mod.enabled() && mod.settingBoolean("k5_integration", true); }
    boolean available() { return launch!=null; }
    void describe(VehiclePage page) {
        page.sectionName("radio","Radio");
        page.tile("radio","radio");
        page.button("radio","radio.open","Open Kia radio","radio");
        page.line("radio","radio.status","Diagnostics status","radio");
        page.button("radio","radio.diagnostics","Collect radio diagnostics (2 min)","radio");
    }
    void pressed(String id) {
        if ("radio.diagnostics".equals(id)) { if (CpClimate.sessionActive) startProbe(); return; }
        if (!"radio.open".equals(id) || !available() || !enabled() || !CpClimate.sessionActive) return;
        final long generation=CpClimate.sessionGeneration;
        main.post(new Runnable() { public void run() {
            if (!enabled() || !CpClimate.sessionActive || generation!=CpClimate.sessionGeneration) return;
            try {
                context.startActivity(new Intent(launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                log.log("[radio] stock radio opened by explicit request");
            } catch (Exception e) { log.log("[radio] launch failed: "+e.getClass().getSimpleName()); }
        }});
    }
}
