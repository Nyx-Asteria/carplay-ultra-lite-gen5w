# Session logging contract (RC11)

CPUL uses a bounded asynchronous local spool and separate USB export worker. Protocol callbacks must never perform disk I/O. A completed report includes summary.txt, session.log, errors.log and optional category-specific diagnostics. USB completion requires checksum verification and a completion marker; interrupted copies stay eligible for retry.

Read summary.txt first: actual observed negotiation, listener readiness, seat dispatch and BCM results, last notification, warnings/errors, category event counts and drop counters. Absence of a milestone means it was not observed or retained; it is not proof of failure. Socket write success is not phone acknowledgement. Binder acceptance is not physical confirmation. Startup-only reports are not established sessions. Preserve these distinctions in WCP/STBT equivalents. Raw packet bodies, credentials, addresses and identifiers must not be exported; allowlist metadata before capture, then sanitize. Keep bounded retention and explicit generation isolation. Current CPUL limits: 20 reports, 64 MiB local spool, 8 MiB/report, 2048 queue slots, 256 pre-session events and 120 seconds pre-session retention. These are implementation limits, not vehicle-validated thresholds.

Known gaps: startup crashes before the first snapshot; native patch request versus actual activation; incomplete runtime radio diagnostics. Keep these explicit until verified.

Export SHA-256 uses the identical streaming implementation from WCP `159ef0c` and STBT `53c3e50` (SessionExporter.Sha256). No JCA provider is required. CPUL retains its existing directory manifest format. This is an integrity checksum, not authentication.
