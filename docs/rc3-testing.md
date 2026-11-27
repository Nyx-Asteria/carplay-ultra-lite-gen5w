# K5 CPUL RC3 testing

- 80% scale; consolidated K5 integration on by default.
- Publish dynamic accessory schema before native recipe; explicit signed seat range and cooling on-state fix.
- Comfort winter and summer tiles; native Apple rows.
- Ultra master and individual experiments default off except the conditional session-start request. Start with identity alone; restart and re-pair online. Add cluster, themed IDs, DBX, UI sync, extended features and enhanced UI one at a time. Full feature mask requires extended features; themed IDs require cluster.
- Both AirPlay settings remain separate and off by default; test parked.
- Collector RC3 removes digest dependency and verifies file sizes. Hash on host.
- Native identity equivalence and seat feedback unverified; radio tuner and fuel range unresolved.
- Test signing key changes between runs; uninstall old test packages if necessary.

- Overlay diagnostic collector includes SystemUI, Mobis_SystemPopup, HKMC_MediaGate, framework2 and media_cmd, plus the existing firmware collection. These are candidates, not confirmed popup owners.
- Steering wheel heater is binary and conditional on OEM method availability; hardware feedback and toggle behavior need validation.

- Wireless 2.2.1-rc.1 is included as a separate test-signed package. Restores direct OEM hotspot startup, retains removal of projection arbitration and post-RC9 navigation fixes. Session-start is prepared before Bluetooth iAP2 component 4 connects; requires theme identity, verified firmware and receiver metadata from an ordinary CarPlay session. Test Wireless alone before enabling CPUL Ultra. Preserve a rollback copy of working RC7 log 2; different signatures may require uninstalling before installing.
- RC3 is the current test bundle. Release listing is normal (not marked prerelease); the software remains experimental.
