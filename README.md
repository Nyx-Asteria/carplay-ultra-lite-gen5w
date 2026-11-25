<div align="center">

# CarPlay Ultra-Lite K5

**Your Kia. Your CarPlay.**

A dedicated CarPlay Ultra-style experience for the Kia K5, powered by HMG-API.

[![Status](https://img.shields.io/badge/status-RC5_testing-f59e0b?style=flat-square)](#development)
[![HMG-API](https://img.shields.io/badge/HMG--API-required-2563eb?style=flat-square)](https://github.com/Open-HMG/HMG-API)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-22c55e?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-K5_Gen5W-64748b?style=flat-square)](#compatibility)

[Features](#features) · [Compatibility](#compatibility) · [Installation](#installation) · [Development](#development) · [Technical details](#technical-details)

</div>

---

## Overview

CarPlay Ultra-Lite K5 is an independent port of [SafariForest's CarPlay Ultra-Lite](https://github.com/SafariForest/carplay-ultra-lite-gen5w) for the Kia K5 Gen5W_M. It retains the existing wireless projection installation and does not add EV routing.

## Features

| Feature | Status |
| --- | --- |
| Front climate and media | Basic temperature and media controls observed working in RC2; further feedback testing required |
| Front seats | Signed three-stage heating and ventilation range; individual levels need validation |
| Heated steering wheel | Binary control, conditional on OEM method availability; untested |
| Vehicle | Odometer and app shortcuts; fuel range unresolved |
| Comfort | Optional winter heating and summer ventilation threshold sections, using separate increase/decrease controls |
| Volume and media | Synchronization and passive stock volume dismissal; duplicate OEM overlays remain unresolved |
| Radio | Native radio shortcut; direct tuner integration deferred |
| CarPlay scaling | Fixed 80% experiment; visual result unverified |
| Ultra | Independent experimental controls; previous combined theme caused session failure |
| AirPlay video/audio | Experimental, disabled by default; prior video initialization failed. Video requires fresh Park feedback and active session. |

## Compatibility

Target: 2022 Kia K5 US, `DL322.USA.0000.V015.012.260507`, Gen5W_M, Android 4.4.2/API 19, 1920 × 720. Other vehicles and firmware are unsupported. Exact library fingerprints gate activation.

## Installation

- [HMG-API](https://github.com/Open-HMG/HMG-API) v1.1.0-rc.8 or later and an existing working Wireless CarPlay installation.
- iOS 26 or later for Ultra-style features.
- Install the test-signed CPUL package from [Releases](https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/releases). Optional Comfort and diagnostic packages are separate.
- Preserve a rollback copy of the working Wireless package and Wi-Fi setup. The included fork is optional for basic CPUL and required for the session-start extension. Do not install a competing EV Routing identity patch.
- Test signing keys change between builds; uninstall previous test packages if signatures conflict. Back up settings first.

## Settings

All settings require restart. K5 integration defaults on. Ultra master, individual Ultra experiments and both parked-video options default off, except the session-start request which defaults on but requires theme identity and a compatible provider.

| Setting | Default |
| --- | --- |
| K5 vehicle integration | On |
| Ultra master | Off |
| Theme identity | Off |
| CarPlayStartSession | On (conditional; provider extension required) |
| Virtual cluster / themed display IDs | Off |
| DBX vehicle variant | Off |
| UI synchronization | Off |
| Extended features / original full feature mask | Off |
| Enhanced RequestCarUI | Off |
| Parked video / video audio | Off |

Themed display IDs require virtual cluster; full mask requires extended features. No custom K5 model is shipped.

## Development

RC5 includes the accumulated RC3 follow-up changes: binary wheel control, targeted overlay collection, and session-start contribution support. See [testing notes](docs/rc5-testing.md).

The bundle includes [Wireless 2.2.1-rc.1](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa), a separately test-signed fork retaining upstream navigation changes while restoring direct OEM hotspot startup. Its experimental session-start extension prepares the response before Bluetooth iAP2 component 4 connects. Requires theme identity and receiver metadata; an ordinary session may be needed first. Stock Wireless remains identification-only. Test the Wireless fork separately with Ultra off before enabling the extension; retain a rollback copy of the working package.

## Technical details

CPUL owns the CarPlayCore vehicle-state integration; Wireless owns iAP2 identification. The theme tuple is `44C9FA31EC:985:19.0`. No fabricated EV data or competing iAP2 patch is installed. The optional [Comfort fork](https://github.com/Nyx-Asteria/hmgapi-comfort) owns threshold validation and persistence.

The diagnostic collector exports allowlisted firmware, including SystemUI, Mobis_SystemPopup, HKMC_MediaGate, framework2 and media_cmd, for the unresolved popup hooks. Keep proprietary firmware private. The collector verifies sizes and defers hashing to the host.

Package ID: `carplay-ultra-lite-k5`; version: `1.0.0-rc.5`. The [RC5 workflow](.github/workflows/rc5-bundle.yml) builds five test-signed packages and publishes only after build checks and signature verification. Hardware validation remains separate. Test parked, including normal wireless reconnects and rollback.

## Credits

- **[SafariForest](https://github.com/SafariForest/carplay-ultra-lite-gen5w)** — original CarPlay Ultra-Lite implementation.
- **[lths](https://github.com/lths/carplay-ultra-lite-ev6)** — EV6 port and Kia-specific reference.
- **[LIVI / Lasse Heitgres](https://github.com/f-io/LIVI)** — vehicle-state protocol foundations.
- **[isaacwisdom](https://github.com/isaacwisdom/egmp-carplay-ev-routing)** — original E-GMP routing project underlying theme identification.
- **[Open-HMG](https://github.com/Open-HMG/HMG-API)** — head-unit mod framework and tooling.
- **[lemoyneiv](https://github.com/lemoyneiv/hmgapi-comfort)** — Comfort & Startup and threshold validation.
- **[Nyx-Asteria](https://github.com/Nyx-Asteria)** — K5 port, development and testing.

Released under **[GPL-3.0-or-later](LICENSE)**. Applicable upstream notices and corresponding source must be retained.

---

<div align="center">

<sub>Unofficial project. Not affiliated with Apple, Hyundai or Kia. Head-unit modifications are performed at your own risk.</sub>

</div>
