# CarPlay Ultra-Lite K5 — 1.0.0-rc.4

Experimental test-signed release for the 2022 Kia K5, DL322 USA 260507 (Gen5W_M). HMG-API 1.1.0-rc.8 or later required. Vehicle validation is pending.

## Changes
- Separate front seat heating and ventilation services, revised BCM feedback and existing OEM USM commands.
- Explicit steering-wheel heater on/off commands and BCM feedback; Comfort threshold integration retained.
- Consolidated CarPlay Ultra and Ultra themes/UI synchronization settings, default on; existing explicit saved choices retained.
- Metadata-derived theme layout/palette constraints, initial virtual Modular/Midnight selection and persisted choices; experimental center Wallpaper layout request. Native center wallpaper default is not yet resolved.
- Exact-firmware stock volume presentation hooks with fail-open readiness lease and delayed dismissal fallback. Volume suppression defaults on.
- Exact-firmware DiPO music OSD suppression, opt-in (default off). Both popup experiments use low-level SDK hooks outside normal HookPlan arbitration.
- Radio Vehicle app shortcut plus explicit two-minute read-only radio diagnostics. No direct tuning or automatic source changes.

## Installation
Use the signed CPUL package. Keep the known-working wireless setup if it already provides the necessary Ultra identity/session-start support. The existing optional Wireless 2.2.1-rc.1, Comfort, compatibility check and firmware collector packages remain in the bundle; their pinned sources are unchanged. Do not install competing identity patches. Test signatures change between builds; back up settings and uninstall conflicting test-signed versions if required. Preserve a rollback copy.

## Parked testing
1. Check wireless reconnect, normal CarPlay and native UI behavior.
2. Exercise front seat heating/ventilation and wheel heater; compare actual hardware levels and feedback. Check Comfort controls.
3. Check center theme selector and persistence across reconnects; report missing layouts, colors and wallpapers. No physical cluster changes intended.
4. Adjust volume and mute in CarPlay and in native UI; ensure stock popup suppression is confined to foreground CarPlay with valid replacement feedback. Toggle off if unexpected behavior occurs. Enable experimental media suppression separately and test track changes.
5. In Vehicle > Radio select Collect radio diagnostics (2 min), then open Kia radio. Tune known FM stations, switch AM, change presets and optionally tune an available SXM station; return to CarPlay and export logs including [radio-probe]. The collector records changed raw values and service availability; RDS and HD metadata and exact band callbacks remain incomplete.
6. Capture logs and report any boot guard, disconnect, unexpected audio behavior or control failure.

## Deferred
Native radio controls, center default wallpaper selection, further AirPlay playback fixes, standalone CarPlay scaling and wireless CP/AA debloating are not included. AirPlay remains experimental, off by default and Park-gated. The earlier decoder-query error was not visible after installing the separate Nemesis codec fix, but playback is still unverified. This release does not add a codec dependency or change parked-video behavior.

Build checks and signature verification must pass before publication. These checks do not establish vehicle functionality.
