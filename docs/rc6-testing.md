# CarPlay Ultra-Lite K5 1.0.0-rc.6 — diagnostic build

## Changes
- Restore independent Ultra toggles with clear descriptions. Default on: master, theme identity, CarPlayStartSession and K5 integration; all other experiments default off. Existing saved values may override defaults.
- Merge validated volume and media popup suppression into K5 integration; retain foreground and readiness checks.
- Merge parked video and audio; retain Park and session gates and picture-only fallback.
- Isolate receiver version and LIVI display extras. Move UI Sync last.
- Add RC4 center-layout request diagnostic and regression test; retain RC5 phone-managed default.
- Bundle WCP 2.2.1-rc.3 with legacy theme identity comparison (default off). No PPS changes.
- Preserve RC5 seat, FM and video implementation; no claim that ventilation, missing native apps or playback are fixed.

## Testing, while parked
1. Install matching signed CPUL and WCP packages over existing same-author versions; restart. Verify actual setting values.
2. Enable only K5 integration and the first three Ultra switches. Leave PPS unchanged. Verify connection, native apps, climate and volume.
3. Enable one Ultra experiment at a time, restart, record sidebars/apps and logs. Themed IDs require virtual cluster; original feature mask requires extended features.
4. Enable UI Sync last. If disconnect occurs, capture CPUL SYNC and native SETUP logs, then try RC4 center layout with UI Sync on.
5. Compare WCP legacy identity off/on with the same CPUL settings. Enable CarPlay's own troubleshooting log only when needed; it may contain phone identifying information.
6. Keep parked video disabled for theme tests; test separately with non-protected content while parked. Stop if session becomes unstable.

Known RC5 findings: passenger/driver heat and wheel heat work; driver ventilation stuck high/no feedback; native Vehicle and Radio apps absent with UI Sync off; video track discovery slow and phone closes session. These remain open.
