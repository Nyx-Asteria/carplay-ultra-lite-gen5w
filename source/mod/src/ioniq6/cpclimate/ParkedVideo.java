package ioniq6.cpclimate;

import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import hmgapi.hook.ModContext;

/** Adapted from lths/carplay-ultra-lite-ev6 ParkedVideo; read-only, fail closed. */
final class ParkedVideo extends Thread {
    static final String SERVICE = "com.hkmc.micom.system", IFACE = "com.hkmc.micom.ISystemService";
    static volatile ParkedVideo instance;
    final ParkedState state = new ParkedState();
    final int code = CarApi.code(IFACE, "getGearPStatus", "()I");
    final ModContext mod;
    ParkedVideo(ModContext mod) { super("k5-video-park"); this.mod = mod; setDaemon(true); instance = this; }
    boolean available() { return code >= 0; }
    static boolean allowed() {
        ParkedVideo p = instance;
        return p != null && p.mod.enabled() && p.mod.settingBoolean("parked_video", true)
                && p.state.allowed(SystemClock.elapsedRealtime());
    }
    @Override public void run() {
        int previous = Integer.MIN_VALUE;
        boolean wasAllowed = false;
        mod.log("[video] Park monitor started; getter transaction=" + code);
        while (!isInterrupted()) {
            int raw = -1;
            long at = SystemClock.elapsedRealtime();
            try {
                IBinder service = CarClimate.service(SERVICE);
                if (code >= 0 && service != null) {
                    Parcel data = Parcel.obtain(), reply = Parcel.obtain();
                    try {
                        data.writeInterfaceToken(IFACE);
                        if (service.transact(code, data, reply, 0)) { reply.readException(); raw = reply.readInt(); }
                    } finally { data.recycle(); reply.recycle(); }
                }
            } catch (Throwable unavailable) { raw = -1; }
            state.sample(raw, at);
            boolean now = CpClimate.videoAllowed();
            if (raw != previous || now != wasAllowed) mod.log("[video] Park raw=" + raw + " authorized=" + now);
            if (wasAllowed && !now) VideoMessages.stopEngine("Park or session authorization lost");
            previous = raw; wasAllowed = now;
            try { Thread.sleep(500); } catch (InterruptedException done) { return; }
        }
    }
}
