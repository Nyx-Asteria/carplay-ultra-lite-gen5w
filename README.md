<div align="center">

# CarPlay Ultra K5

**Your Kia. Your CarPlay.**

An integrated CarPlay Ultra-style experience for the Kia K5 Gen5W_M, powered by HMG-API.

[![Development](https://img.shields.io/badge/development-3.0.0_development-f59e0b?style=flat-square)](#development)
[![HMG-API](https://img.shields.io/badge/HMG--API-1.1.0--rc.10%2B-2563eb?style=flat-square)](https://github.com/Open-HMG/HMG-API)
[![Platform](https://img.shields.io/badge/platform-K5_Gen5W__M-64748b?style=flat-square)](#compatibility)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-22c55e?style=flat-square)](LICENSE)

[Features](#features) · [Compatibility](#compatibility) · [Installation](#installation) · [Settings](#settings) · [Development](#development) · [Credits](#credits)

</div>

---

An independent K5 port of [SafariForest's CarPlay Ultra-Lite](https://github.com/SafariForest/carplay-ultra-lite-gen5w). Native vehicle controls, Ultra-style appearance and parked video are integrated into CarPlay without adding EV routing or changing the physical instrument cluster.

> **3.0.0 development is underway. RC11 has a reported wireless App Transport/projection regression; retain the known-working RC10 rollback bundle.** See the [RC11 development plan](docs/rc11-plan.md), [RC11 testing guide](docs/rc11-testing.md), [RC10 testing guide](docs/rc10-testing.md), [changelog](CHANGELOG.md) and [Releases](https://github.com/Nyx-Asteria/carplay-ultra-k5/releases). Retain a known-working rollback package.

## Features

### Climate & comfort

| Feature | Implementation |
| --- | --- |
| Front climate | Temperature, airflow, fan and defrost integration |
| Heated seats | Independent driver/passenger off, low, medium and high |
| Ventilated seats | Independent driver/passenger off, low, medium and high; explicit SeatFan schema |
| Seat command arbitration | Coalesces level and power writes into one OEM command per physical seat |
| Feedback | BCM state mapping and raw diagnostics; OEM USM commands |
| Heated steering wheel | Explicit on/off commands with BCM feedback |
| Comfort thresholds | Optional separate Comfort package |
| Vehicle information | Odometer and vehicle shortcuts; fuel range remains unresolved |

Heating and wheel heating were reported working in earlier testing. RC7 changes separate SeatFan feedback to publish BCM-confirmed level and power together; both front ventilation cycles are covered by host tests and require vehicle validation. RC8 adds authoritative state validation and an opt-in immediate feedback comparison; driver heating reversion and ventilation visual feedback remain unresolved.

### Ultra appearance & projection

- Consolidated CarPlay Ultra and theme/UI synchronization options.
- Theme identity and session-start contribution through the companion wireless provider.
- RC11 metadata-derived layout and palette validation, connection-local UI Sync handshakes, and phone-managed center and virtual cluster layouts. No forced wallpaper or production layout persistence. Black wallpaper and the blank theme selector remain unresolved.
- Optional passive RemoteControl discovery for unclaimed type-130 requests. Diagnostic only; no FileTransfer capabilities or unknown stream acceptance.
- Optional [Phone Projection Scaling](https://github.com/Nyx-Asteria/Phone-Projection-Scaling) integration: CPUK5 hosts CarPlay scaling, PPS independently handles Android Auto scaling. No competing CarPlay native patches.
- Firmware-gated stock volume popup suppression and optional stock media popup suppression.

### Native FM radio

- Native CarPlay FM source selection, frequency tuning, seeking and frequency feedback.
- North American 200 kHz tuning grid with source ownership checks.
- Bounded asynchronous source activation and cancellation.
- Unsupported European DAB service omitted; stock radio shortcut and read-only diagnostic collector retained.

FM tuning and physical seek were verified in earlier vehicle tests. RC8 implements native OEM seek, cached/observed station lists, HD primary-program metadata and artwork, and opt-in SiriusXM source/catalog/direct tuning with OEM eligibility checks. HD multicast is diagnostic-only; Bose Centerpoint is read-only. New radio functions require vehicle validation.

### Parked AirPlay video

- CarPlay-native video using the iPhone proxy and ExoPlayer.
- Corrected redirect/range handling, playback intent retention and first-frame black shutter.
- Explicit HLS MIME hints, reduced startup buffering, preferred H.264 and 720p adaptive video with fallback.
- LIVI-compatible playback-status fields and scalar timing support alongside the existing response.
- Fresh Park feedback and active CarPlay session required; video is disabled by default. When enabled, audio is attempted with a picture-only fallback; an optional picture-only diagnostic disables audio rendering from startup and prefers a low video bitrate.

RC7 observed hardware decoder initialization and a DRM unsupported error on protected content. RC8 reportedly rendered a first hardware-decoded frame, but the session disconnected shortly afterward; sustained playback remains unverified. Playback remains experimental. Independent AirPlay mirroring and YouTube discovery are future work, not features of this build. See [standalone AirPlay integration](docs/standalone-airplay-integration.md).

## Compatibility

| Item | Target |
| --- | --- |
| Vehicle | 2022 Kia K5 US |
| Platform | Gen5W_M |
| Firmware | `DL322.USA.0000.V015.012.260507` |
| Android | 4.4.2 / API 19 |
| Display | 1920 × 720 |
| Framework | HMG-API 1.1.0-rc.10 (pinned build) |
| Phone | iOS 26 or later for Ultra features |

Exact firmware fingerprints gate activation. Other cars and firmware are unsupported.

## Installation

Download a published package from [Releases](https://github.com/Nyx-Asteria/carplay-ultra-k5/releases). Preserve a rollback copy of working wireless projection and back up settings first.

The RC10 bundle includes:

| Package | Role |
| --- | --- |
| CPUK5 | Vehicle integration and Ultra |
| [Wireless CP/AA fork](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa) | 2.2.1-rc.6 wireless projection and Ultra identity/session-start |
| [Phone Projection Scaling](https://github.com/Nyx-Asteria/Phone-Projection-Scaling) | Stable 1.0.0 relative projection scaling |
| [Comfort fork](https://github.com/Nyx-Asteria/hmgapi-comfort) | Optional comfort thresholds |
| [Standalone TBT](https://github.com/Nyx-Asteria/Standalone_TBT) | 1.0.0-rc.3; experimental Waze recovery, single navigation publisher |
| Compatibility check | Firmware and environment checks |
| Firmware collector | Incremental navigation/iAP2 binaries and opt-in CPUK5 runtime logs |

The RC10 bundle is configured to use one permanent author identity. Older test-signed packages may need removal before installing packages signed with the permanent key. Do not install competing identity patches. Explicitly disable WCP Integrated turn-by-turn, apply and restart before using Standalone TBT. Keep Enable wireless CarPlay on; audio and Now Playing follow that master switch, and shared identity is independent. Remove the original TBT fork first. Test wireless reconnect and rollback while parked.

## Settings

Restart after changing settings. Explicit saved selections are retained.

| Setting | Default |
| --- | --- |
| K5 Vehicle Integration | On |
| Seat Heating Immediate Feedback | On |
| Legacy Heated Seat Compatibility (Experimental) | Off |
| Seat Ventilation Immediate Feedback (Experimental) | Off |
| CarPlay Ultra | On |
| Enable Ultra Sidebars | On |
| UI Synchronization | Off |
| Independent UI Sync Negotiation (Experimental) | Off |
| Widescreen View Areas (Experimental) | Disabled |
| SiriusXM Discovery | Off |
| Parked AirPlay Video & Audio | Off |
| AirPlay Picture Only (Diagnostic) | Off |
| Automatic Session Logging | On |
| Automatic USB Session Export | On |
| Detailed Session Diagnostics | On |
| Protocol Packet Diagnostics | Off |
| RemoteControl SETUP Discovery (Diagnostic) | Off |

UI Synchronization requires Ultra Sidebars and CarPlay Ultra. Both layouts and wallpapers are phone-managed in RC11; the retired forced-layout options are ignored. Passive RemoteControl discovery is independent of UI Sync and packet diagnostics; enable only for targeted capture, then restart and export logs. Picture-only mode requires parked video. PPS owns scaling preferences; CPUK5 consumes them when active. Disable CPUK5 itself to use PPS's standalone CarPlay native patch.

## Development

3.0.0 development begins from the RC11 release commit. Read the [living handoff](handoff.md) for current priorities. RC11 began with a content-identical rebase of RC10. See the [RC11 plan](docs/rc11-plan.md), [RC10 test plan](docs/rc10-testing.md), and [changelog](CHANGELOG.md). RC11 was the previous coordinated release candidate. No 3.0.0 publication is authorized by the branding-only change.

| Milestone | Status |
| --- | --- |
| RC4–RC6 | Earlier vehicle testing releases |
| RC7 | Consolidated settings, seat feedback, radio diagnostics, theme negotiation, parked video diagnostics, stable PPS |
| Remaining vehicle validation | Seat cycling, radio controls and metadata, themes, standard CarPlay fallback, first video frame |
| RC8 | Testing: radio, climate feedback, theme diagnostics, companion TBT and incremental collector |
| Future | HD multicast control, confirmed SXM/Bose support, theme selector, standalone AirPlay and YouTube discovery |

[Development regression](.github/workflows/development-regression.yml) compiles the main module and video engine and runs climate, Park, volume policy and UI synchronization checks without publishing. [Release bundle](.github/workflows/release-bundle.yml) builds seven packages and verifies author signatures. An exact release commit on the final RC branch signs and publishes only after all bundle verification succeeds; ordinary pushes do not publish. See the [release consolidation and synchronized version policy](docs/release-consolidation.md). Release validation status is documented explicitly; a clean release name does not imply vehicle validation.

Read [development notes](docs/next-development.md), [radio diagnostics](docs/k5-radio.md) and [integration details](docs/integration.md) for technical background. Host tests do not establish vehicle functionality.

## Technical details

CPUK5 owns the CarPlayCore vehicle-state integration; Wireless owns iAP2 identification. Theme tuple: `44C9FA31EC:985:19.0`. Package ID: `carplay-ultra-lite-k5` (legacy identifier intentionally retained for in-place upgrades, along with the permanent author signing identity).

The firmware collector exports allowlisted OEM binaries for interface analysis. Keep proprietary firmware private. No physical cluster customization or custom K5 model is shipped.

## Credits

- **[SafariForest](https://github.com/SafariForest/carplay-ultra-lite-gen5w)** — original CarPlay Ultra-Lite implementation.
- **[lths](https://github.com/lths/carplay-ultra-lite-ev6)** — EV6 port and Kia-specific reference.
- **[LIVI / Lasse Heitgres](https://github.com/f-io/LIVI)** — vehicle-state protocol foundations.
- **[isaacwisdom](https://github.com/isaacwisdom/egmp-carplay-ev-routing)** — original E-GMP routing project underlying theme identification.
- **[Open-HMG](https://github.com/Open-HMG/HMG-API)** — head-unit mod framework and tooling.
- **[lemoyneiv](https://github.com/lemoyneiv/hmgapi-comfort)** — Comfort & Startup and threshold validation.
- **[CubbyNINJA / ParkCast](https://github.com/CubbyNINJA/g5w-parkcast)** — standalone AirPlay research reference; no receiver code incorporated yet.
- **[Nyx-Asteria](https://github.com/Nyx-Asteria)** — K5 port, development and testing.

Based on LIVI by Lasse Heitgres (https://github.com/f-io/LIVI).

Released under **[GPL-3.0-or-later](LICENSE)**. Applicable upstream notices and corresponding source must be retained.

---

<div align="center">

<sub>Unofficial project. Not affiliated with Apple, Hyundai or Kia. Head-unit modifications are performed at your own risk.</sub>

</div>
