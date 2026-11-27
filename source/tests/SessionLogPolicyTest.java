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
        check(p.suppressed() == 3);
        p.reset();
        check(p.suppressed() == 0);
        check(p.accept(INFO, CORE, "connected", 1) != null);
        for (int i = 0; i < 300; i++) p.accept(INFO, CORE, "item " + i, 2);
        check(p.accept(INFO, CORE, "connected", 3) != null);
        System.out.println("SessionLogPolicyTest passed");
    }
}
