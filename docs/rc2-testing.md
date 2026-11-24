# CarPlay Ultra-Lite K5 1.0.0-rc.2

Second experimental K5 testing release. RC1 testing found immediate crashes with parked video enabled; the restart-count guard also blocked repeated intentional test restarts. RC2 removes that guard entirely (including the old file read), retains exact firmware preflight and Park interlock, and defaults both AirPlay options off. Existing saved settings can override new defaults: explicitly switch both AirPlay options off before applying or restarting. The firmware collector now polls for USB continuously, logs to service stdout, and has a Dump firmware now toggle (OFF then ON, leave ON until the request is logged). Mounted USB fallback discovery is restored. Checker and Comfort are unchanged from RC1; keep installed copies rather than reinstalling them. Target: DL322 USA 260507, HMG-API >= 1.1.0-rc.8, existing working wireless projection with shared identity enabled. RC2 has not been validated on-car.

Download the complete testing bundle and extract it. Install individual signed mod ZIPs, not the outer bundle. Packages share an ephemeral test author key; future builds may require uninstall/reinstall. Comfort retains the original mod ID and app identity; back up settings before replacing the original, and restore original Comfort if needed. Do not run two Comfort engines.

1. Install the compatibility checker and firmware collector while parked. Insert USB; collect reports and inventory. Keep OEM binaries private.
2. Install compatible Comfort only if threshold editing is wanted.
3. Install CPUL; restart as required. Keep existing wireless projection configuration unchanged.
4. Test normal projection, reconnects, music/calls/navigation, climate, volume, Comfort, radio handoff and parked video. Video audio is experimental; do not rely on the Park interlock until verified.
5. If anything regresses, disable/uninstall CPUL and restart. Remove diagnostic mods after collection. Restore original Comfort as needed.

Direct radio tuning and custom K5 assets are deferred. The native radio shortcut is included. Test signing does not establish runtime compatibility.
