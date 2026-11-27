<div align="center">

# CarPlay Ultra-Lite K5

**Your Kia. Your CarPlay.**

An integrated CarPlay Ultra-style experience for the Kia K5 Gen5W_M, powered by HMG-API.

[![Development](https://img.shields.io/badge/development-RC11_development-f59e0b?style=flat-square)](#development)
[![HMG-API](https://img.shields.io/badge/HMG--API-1.1.0--rc.10%2B-2563eb?style=flat-square)](https://github.com/Open-HMG/HMG-API)
[![Platform](https://img.shields.io/badge/platform-K5_Gen5W__M-64748b?style=flat-square)](#compatibility)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-22c55e?style=flat-square)](LICENSE)

[Features](#features) · [Compatibility](#compatibility) · [Installation](#installation) · [Settings](#settings) · [Development](#development) · [Credits](#credits)

</div>

---

An independent K5 port of [SafariForest's CarPlay Ultra-Lite](https://github.com/SafariForest/carplay-ultra-lite-gen5w). Native vehicle controls, Ultra-style appearance and parked video are integrated into CarPlay without adding EV routing or changing the physical instrument cluster.

> **RC11 is in development; RC10 is the current vehicle-testing baseline.** See the [RC11 development plan](docs/rc11-plan.md), [RC10 testing guide](docs/rc10-testing.md), [changelog](CHANGELOG.md) and [Releases](https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/releases). Retain a known-working rollback package.

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
- Metadata-derived layout and palette validation, persisted selection and phone-managed center layout.
- Midnight virtual-cluster default, connection-local UI Sync handshakes and an opt-in center Wallpaper/blue layout request. Black wallpaper and the blank theme selector remain unresolved.
- Optional [Phone Projection Scaling](https://github.com/Nyx-Asteria/Phone-Projection-Scaling) integration: CPUL hosts CarPlay scaling, PPS independently handles Android Auto scaling. No competing CarPlay native patches.
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

Download a published package from [Releases](https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/releases). Preserve a rollback copy of working wireless projection and back up settings first.

The RC10 bundle includes:

| Package | Role |
| --- | --- |
| CPUL K5 | Vehicle integration and Ultra |
| [Wireless CP/AA fork](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa) | 2.2.1-rc.6 wireless projection and Ultra identity/session-start |
| [Phone Projection Scaling](https://github.com/Nyx-Asteria/Phone-Projection-Scaling) | Stable 1.0.0 relative projection scaling |
| [Comfort fork](https://github.com/Nyx-Asteria/hmgapi-comfort) | Optional comfort thresholds |
| [Standalone TBT](https://github.com/Nyx-Asteria/Standalone_TBT) | 1.0.0-rc.3; experimental Waze recovery, single navigation publisher |
| Compatibility check | Firmware and environment checks |
| Firmware collector | Incremental navigation/iAP2 binaries and opt-in CPUL runtime logs |

The RC10 bundle is configured to use one permanent author identity. Older test-signed packages may need removal before installing packages signed with the permanent key. Do not install competing identity patches. Explicitly disable WCP Integrated turn-by-turn, apply and restart before using Standalone TBT. Keep Enable wireless CarPlay on; audio and Now Playing follow that master switch, and shared identity is independent. Remove the original TBT fork first. Test wireless reconnect and rollback while parked.

## Settings

Restart after changing settings. Explicit saved selections are retained.

| Setting | Default |
| --- | --- |
| K5 Vehicle Integration | On |
| Seat Heating Immediate Feedback | On |
| Seat Ventilation Immediate Feedback (Experimental) | Off |
| CarPlay Ultra | On |
| Enable Ultra Sidebars | On |
| UI Synchronization | Off |
| Independent UI Sync Negotiation (Experimental) | Off |
| Phone-Managed Cluster Layout (Experimental) | Off |
| Force Theme Wallpaper (Experimental) | Off |
| SiriusXM Discovery | Off |
| Parked AirPlay Video & Audio | Off |
| AirPlay Picture Only (Diagnostic) | Off |

UI Synchronization requires Ultra Sidebars and CarPlay Ultra. The wallpaper experiment requires UI Synchronization and only requests a center layout if the phone negotiates that panel. Picture-only mode requires parked video. PPS owns scaling preferences; CPUL consumes them when active. Disable CPUL itself to use PPS's standalone CarPlay native patch.

## Development

RC11 development begins with a content-identical rebase of RC10. See the [RC11 plan](docs/rc11-plan.md), [RC10 test plan](docs/rc10-testing.md), and [changelog](CHANGELOG.md). No RC11 release has been approved.

| Milestone | Status |
| --- | --- |
| RC4–RC6 | Earlier vehicle testing releases |
| RC7 | Consolidated settings, seat feedback, radio diagnostics, theme negotiation, parked video diagnostics, stable PPS |
| Remaining vehicle validation | Seat cycling, radio controls and metadata, themes, standard CarPlay fallback, first video frame |
| RC8 | Testing: radio, climate feedback, theme diagnostics, companion TBT and incremental collector |
| Future | HD multicast control, confirmed SXM/Bose support, theme selector, standalone AirPlay and YouTube discovery |

[Development regression](.github/workflows/development-regression.yml) compiles the main module and video engine and runs climate, Park, volume policy and UI synchronization checks without publishing. [Release bundle](.github/workflows/release-bundle.yml) builds seven packages and verifies author signatures. Publication requires separate explicit approval. The release is a testing build despite its publication status.

Read [development notes](docs/next-development.md), [radio diagnostics](docs/k5-radio.md) and [integration details](docs/integration.md) for technical background. Host tests do not establish vehicle functionality.

## Technical details

CPUL owns the CarPlayCore vehicle-state integration; Wireless owns iAP2 identification. Theme tuple: `44C9FA31EC:985:19.0`. Package ID: `carplay-ultra-lite-k5`.

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
