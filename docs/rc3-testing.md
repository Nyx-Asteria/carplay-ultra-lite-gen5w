# K5 CPUL RC3 testing

- 80% scale; consolidated K5 integration on by default.
- Publish dynamic accessory schema before native recipe; explicit signed seat range and cooling on-state fix.
- Comfort winter and summer tiles; native Apple rows.
- Ultra master and all experimental components default off. Start with identity alone; restart and re-pair online. Add cluster, themed IDs, DBX, UI sync, extended features and enhanced UI one at a time. Full feature mask requires extended features; themed IDs require cluster.
- Both AirPlay settings remain separate and off by default; test parked.
- Collector RC3 removes digest dependency and verifies file sizes. Hash on host.
- Native identity equivalence and seat feedback unverified; radio tuner and fuel range unresolved.
- Test signing key changes between runs; uninstall old test packages if necessary.

- Expanded collector includes stock SystemUI, Mobis_SystemPopup, HKMC_MediaGate, HKMC_NaviPopupService, RightScreenHome, Launcher_WP3, BTAudio, framework2, media_cmd and UI resource packages verified in the K5 firmware inventory. These are diagnostic candidates, not confirmed popup owners. Allow extra USB space.
- Steering wheel heater is binary and conditional on OEM method availability; hardware feedback and toggle behavior need validation.
