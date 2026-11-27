package ioniq6.cpclimate;

import java.util.EnumSet;
import static ioniq6.cpclimate.SessionLogPolicy.Level.*;
import static ioniq6.cpclimate.SessionLogPolicy.Category.*;

public final class SessionLogPolicyTest {
    private static void check(boolean b) { if (!b) throw new AssertionError(); }
    public static void main(String[] args) {
        SessionLogPolicy p = new SessionLogPolicy(EnumSet.of(CLIMATE, VS_RAW));
        check(p.accept(INFO, CORE, "connected", 0) != null);
        check(p.accept(INFO, CORE, "connected", 1) == null);
        check(p.accept(INFO, CORE, "connected", 10000) != null);
        check(p.accept(DEBUG, CLIMATE, "feedback", 0) != null);
        check(p.accept(DEBUG, RADIO, "getter", 0) == null);
        check(p.accept(TRACE, PROJECTION_RAW, "stream metadata", 0) == null);
        check(p.accept(TRACE, VS_RAW, SessionLogPolicy.packet("VS0", 1, 2, 100), 0) != null);
        check(p.accept(ERROR, RADIO, "failed", 0) != null);
        check(p.accept(ERROR, RADIO, "failed", 1) != null);
        check(p.accept(WARN, RADIO, "timeout", 0) != null);
        check(p.accept(WARN, RADIO, "timeout", 1) != null);
        check(SessionLogPolicy.sanitize("session key: ABC").equals("[sensitive message withheld]"));
        check(SessionLogPolicy.sanitize("https://example.com/private").equals("[sensitive message withheld]"));
        check(SessionLogPolicy.sanitize("one\ntwo").equals("one two"));
        check(SessionLogPolicy.sanitize("peer aa:bb:cc:dd:ee:ff").equals("peer [address withheld]"));
        check(SessionLogPolicy.sanitize("peer 192.168.1.10:7000").equals("peer [address withheld]:7000"));
        check(SessionLogPolicy.sanitize("peer [fe80::1234%wlan0]:7000").equals("peer [[address withheld]]:7000"));
        check(SessionLogPolicy.sanitize("peer 2001:db8:0:1:2:3:4:5").equals("peer [address withheld]"));
        check(SessionLogPolicy.sanitize("at 12:34:56").equals("at 12:34:56"));
        check(p.suppressed() == 3);
        p.reset();
        check(p.suppressed() == 0);
        check(p.accept(INFO, CORE, "connected", 1) != null);
        for (int i = 0; i < 300; i++) p.accept(INFO, CORE, "item " + i, 2);
        check(p.accept(INFO, CORE, "connected", 3) != null);
        check(SessionLogPolicy.diagnosticCategories(false,false).isEmpty());
        check(SessionLogPolicy.diagnosticCategories(true,false).equals(EnumSet.of(CLIMATE,RADIO,SXM,AUDIO,THEMES,AIRPLAY)));
        check(SessionLogPolicy.diagnosticCategories(false,true).equals(EnumSet.of(VS_RAW,PROJECTION_RAW)));
        check(SessionLogPolicy.diagnosticCategories(true,true).size()==8);
        SessionLogPolicy detail=new SessionLogPolicy(SessionLogPolicy.diagnosticCategories(true,false));
        check(detail.enabled(DEBUG,THEMES) && detail.enabled(DEBUG,AIRPLAY));
        check(!detail.enabled(TRACE,VS_RAW) && !detail.enabled(TRACE,PROJECTION_RAW));
        check(detail.enabled(WARN,PROJECTION_RAW));
        System.out.println("SessionLogPolicyTest passed");
    }
}
