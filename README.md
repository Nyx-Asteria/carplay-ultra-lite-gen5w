<div align="center">

# CarPlay Ultra-Lite K5

**Your Kia. Your CarPlay.**

An integrated CarPlay Ultra-style experience for the Kia K5 Gen5W_M, powered by HMG-API.

[![Development](https://img.shields.io/badge/development-RC7-f59e0b?style=flat-square)](#development)
[![HMG-API](https://img.shields.io/badge/HMG--API-1.1.0--rc.8%2B-2563eb?style=flat-square)](https://github.com/Open-HMG/HMG-API)
[![Platform](https://img.shields.io/badge/platform-K5_Gen5W__M-64748b?style=flat-square)](#compatibility)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-22c55e?style=flat-square)](LICENSE)

[Features](#features) · [Compatibility](#compatibility) · [Installation](#installation) · [Settings](#settings) · [Development](#development) · [Credits](#credits)

</div>

---

An independent K5 port of [SafariForest's CarPlay Ultra-Lite](https://github.com/SafariForest/carplay-ultra-lite-gen5w). Native vehicle controls, Ultra-style appearance and parked video are integrated into CarPlay without adding EV routing or changing the physical instrument cluster.

> **Development status:** RC7 is in preparation; RC6 is the previous testing release. RC7 changes are host-tested where covered, not yet validated on the vehicle. No RC7 package is published yet.

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

Heating and wheel heating were reported working in earlier testing. RC7 changes separate SeatFan feedback to publish BCM-confirmed level and power together; both front ventilation cycles are covered by host tests and require vehicle validation.

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

Manual FM tuning was verified on RC6. RC7 uses permission-checked OEM seek keys and cached FM station lists; those changes require vehicle testing. Bounded diagnostics collect source routing, HD/RDS metadata, SiriusXM favorites and Bose Centerpoint state. SiriusXM discovery remains disabled by default and does not enable tuning. Metadata, favorites and artwork publication remain incomplete.

### Parked AirPlay video

- CarPlay-native video using the iPhone proxy and ExoPlayer.
- Corrected redirect/range handling, playback intent retention and first-frame black shutter.
- Explicit HLS MIME hints, reduced startup buffering, preferred H.264 and 720p adaptive video with fallback.
- LIVI-compatible playback-status fields and scalar timing support alongside the existing response.
- Fresh Park feedback and active CarPlay session required; video is disabled by default. When enabled, audio is attempted with a picture-only fallback; an optional picture-only diagnostic disables audio rendering from startup and prefers a low video bitrate.

Playback remains experimental. Independent AirPlay mirroring and YouTube discovery are future work, not features of this build. See [standalone AirPlay integration](docs/standalone-airplay-integration.md).

## Compatibility

| Item | Target |
| --- | --- |
| Vehicle | 2022 Kia K5 US |
| Platform | Gen5W_M |
| Firmware | `DL322.USA.0000.V015.012.260507` |
| Android | 4.4.2 / API 19 |
| Display | 1920 × 720 |
| Framework | HMG-API 1.1.0-rc.8 or later |
| Phone | iOS 26 or later for Ultra features |

Exact firmware fingerprints gate activation. Other cars and firmware are unsupported.

## Installation

Download a published package from [Releases](https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/releases). Preserve a rollback copy of working wireless projection and back up settings first.

The planned development bundle includes:

| Package | Role |
| --- | --- |
| CPUL K5 | Vehicle integration and Ultra |
| [Wireless CP/AA fork](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa) | Wireless projection and Ultra identity/session-start |
| [Phone Projection Scaling](https://github.com/Nyx-Asteria/Phone-Projection-Scaling) | Stable 1.0.0 relative projection scaling |
| [Comfort fork](https://github.com/Nyx-Asteria/hmgapi-comfort) | Optional comfort thresholds |
| Compatibility check | Firmware and environment checks |
| Firmware collector | Allowlisted diagnostic export |

The next bundle is configured to use one permanent author identity. Older test-signed packages may need removal before installing packages signed with the permanent key. Do not install competing identity patches. Test wireless reconnect and rollback while parked.

## Settings

Restart after changing settings. Explicit saved selections are retained.

| Setting | Default |
| --- | --- |
| K5 Vehicle Integration | On |
| CarPlay Ultra | On |
| Enable Ultra Sidebars | On |
| UI Synchronization | Off |
| Force Theme Wallpaper (Experimental) | Off |
| SiriusXM Discovery | Off |
| Parked AirPlay Video & Audio | Off |
| AirPlay Picture Only (Diagnostic) | Off |

UI Synchronization requires Ultra Sidebars and CarPlay Ultra. The wallpaper experiment requires UI Synchronization and only requests a center layout if the phone negotiates that panel. Picture-only mode requires parked video. PPS owns scaling preferences; CPUL consumes them when active. Disable CPUL itself to use PPS's standalone CarPlay native patch.

## Development

| Milestone | Status |
| --- | --- |
| RC4–RC6 | Earlier vehicle testing releases |
| RC7 | Consolidated settings, seat feedback, radio diagnostics, theme negotiation, parked video diagnostics, stable PPS |
| Remaining vehicle validation | Seat cycling, radio controls and metadata, themes, standard CarPlay fallback, first video frame |
| Future | Complete SiriusXM controls, native metadata and artwork, standalone AirPlay and YouTube discovery |

[Development regression](.github/workflows/development-regression.yml) compiles the main module and video engine and runs climate, Park, volume policy and UI synchronization checks without publishing. [Release bundle](.github/workflows/release-bundle.yml) builds six packages and verifies author signatures before publishing a non-draft release marked Latest. The release is a testing build despite its publication status.

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

Released under **[GPL-3.0-or-later](LICENSE)**. Applicable upstream notices and corresponding source must be retained.

---

<div align="center">

<sub>Unofficial project. Not affiliated with Apple, Hyundai or Kia. Head-unit modifications are performed at your own risk.</sub>

</div>
