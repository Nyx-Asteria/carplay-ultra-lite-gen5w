<div align="center">

# CarPlay Ultra-Lite K5

**Your Kia. Your CarPlay.**

A dedicated CarPlay Ultra-style experience for the Kia K5, powered by HMG-API.

[![Status](https://img.shields.io/badge/status-RC1_development-f59e0b?style=flat-square)](#development)
[![HMG-API](https://img.shields.io/badge/HMG--API-required-2563eb?style=flat-square)](https://github.com/Open-HMG/HMG-API)
[![License](https://img.shields.io/badge/license-GPL--3.0--or--later-22c55e?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-K5_Gen5W-64748b?style=flat-square)](#compatibility)

[Features](#features) · [Compatibility](#compatibility) · [Installation](#installation) · [Development](#development) · [Technical details](#technical-details)

</div>

---

## Overview

**CarPlay Ultra-Lite K5** is an independent port of [SafariForest's CarPlay Ultra-Lite](https://github.com/SafariForest/carplay-ultra-lite-gen5w), built specifically for the Kia K5.

The aim is to bring the Ultra-style interface, native climate controls and additional vehicle functionality into CarPlay without replacing the factory head unit or changing the existing wireless projection setup.

Unlike the original multi-vehicle project, this port targets one known vehicle and firmware configuration. Unnecessary compatibility paths and EV-specific functionality will be removed where appropriate.

> [!NOTE]
> The RC1 source implements the features below. Build verification is not on-car validation. No signed RC1 package has been published yet.

## Features

| Feature | RC1 implementation |
| :--- | :--- |
| Ultra interface | Shared theme identity, layouts, wallpapers and virtual cluster; inherited DBX vehicle variant, not a custom K5 model |
| Climate | Front temperature, HVAC, A/C, AUTO, SYNC, fan, vents, recirculation and rear defrost integration |
| Seats | Three-stage driver and front-passenger heating and ventilation |
| Vehicle page | Fuel range, odometer and installed head-unit app shortcuts |
| Comfort | Optional six live heating/ventilation thresholds with one-degree increase/decrease buttons and persistent settings through the [compatible Comfort fork](https://github.com/Nyx-Asteria/hmgapi-comfort) |
| Volume | Media, navigation, call and Siri volume/mute synchronization; passive dismissal of the stock volume popup |
| Media | Explicit phone play, pause and track controls |
| AirPlay video | Park-only playback with active-session and fresh Park feedback gates |
| AirPlay audio | Experimental local player audio with diagnostics and picture-only recovery; audible output not established |
| Radio | Explicit shortcut to the installed Kia radio; direct tuning, station metadata and presets deferred |

All features above have source implementations; none is claimed verified on the K5. Unsupported hardware controls are withheld. The virtual cluster does not replace the factory instrument cluster.

## Compatibility

| Vehicle | Software version | Verification |
| :--- | :--- | :--- |
| **2022 Kia K5 (US)** | `DL322.USA.0000.V015.012.260507` | Target vehicle; initial compatibility report collected, port not yet tested |

<details>
<summary><strong>Target platform</strong></summary>

| Property | Value |
| :--- | :--- |
| Head unit | Gen5W_M |
| Android | 4.4.2 / API 19 |
| Display | 1920 × 720 |
| Projection | Existing working HMG-API wireless CarPlay |

</details>

Other vehicles and firmware versions are outside the scope of this port. Matching native library fingerprints and framework methods will be required before hooks are applied.

## Installation

### Requirements

- The supported Kia K5 firmware.
- [HMG-API](https://github.com/Open-HMG/HMG-API) v1.1.0-rc.8 or later, with working wireless CarPlay and hooks enabled.
- An iPhone running iOS 26 or later.
- A signed package from this fork.

> [!WARNING]
> There is no validated K5 package yet. Do not install the inherited Ioniq 6 or EV6 packages as a substitute.

The intended distribution is a single signed HMG-API USB package, published through [Releases](https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/releases) once ready. Installation and rollback instructions will accompany the first prerelease.

The existing wireless-carplay-aa package and working Wi-Fi configuration remain separate and unchanged. The K5 port uses HMG-API and the existing wireless-carplay-aa installation. Shared CarPlay identity must be enabled. No wireless package version is pinned; shared API compatibility is checked at runtime. The modified E-GMP routing package is not intended to be installed.

## Development

### RC1 implementation

- [x] K5-specific package, manifest and firmware preflight.
- [x] Theme-only shared identity API adapter and runtime integration.
- [x] Front climate and seat integration.
- [x] Vehicle information, installed-app shortcuts and native radio handoff.
- [x] Optional Comfort threshold bridge and CarPlay controls.
- [x] Volume/media synchronization and passive stock volume panel dismissal.
- [x] Parked AirPlay video and experimental local audio recovery.
- [x] Read-only checker and radio/framework file collection.
- [ ] Complete source review and verify latest build.
- [ ] Produce and verify test-key-signed RC1 packages.
- [ ] Validate all features and rollback on the target vehicle.

### Deferred enhancements

Direct AM/FM/SiriusXM tuning, station metadata and presets require OEM tuner interface evidence. Custom K5 assets require investigation of Apple's theme asset delivery and signing. Neither blocks RC1.

### On-car validation

Test while safely parked: installation and rollback, normal wireless projection and reconnects, calls/navigation/music, Android Auto regression, climate and physical control feedback, theme and vehicle-page rendering, Comfort threshold round trips, physical volume controls, radio handoff, and video stop on loss of Park or valid session state. Do not rely on the video interlock until verified.

## Diagnostics

HMG-API logs report individual firmware fingerprint results, climate startup and profile, native recipe requests, session generations and finalization, and theme API failures. Native recipe success is reported by HMG-API itself. Raw projection SETUP payloads are not copied into the mod log. The crash guard clears only after the same session remains active for the full health interval. The port does not intercept wireless iAP send traffic.

## Test settings

Feature toggles are enabled by default and require a restart.

| Setting | Purpose |
| :--- | :--- |
| `climate_test` | Climate and supported front seat controls |
| `vehicle_page` | Vehicle information and installed-app shortcuts |
| `ultra_theme` | Shared Ultra theme and virtual display |
| `parked_video` | Park-restricted AirPlay video |
| `video_audio` | Experimental local video audio with picture-only fallback |
| `volume_link` | Audio stream synchronization |
| `volume_panel` | Dismiss stock popup when CarPlay is foreground and volume synchronization is ready |
| `media_controls` | Explicit phone media commands |
| `comfort_thresholds` | Optional Comfort threshold editing |
| `native_radio` | Stock radio launch shortcut |

Rear seat controls, rear zones, ambient lighting, steering wheel heating and Driver Only are outside the current K5 profile. Feature separation is implemented but still needs runtime verification.

## Technical details

The port is based on [feature/climate-and-video](https://github.com/SafariForest/carplay-ultra-lite-gen5w/tree/feature/climate-and-video) at `70e3ba2`. The [EV6 fork](https://github.com/lths/carplay-ultra-lite-ev6) serves as a reference for Kia-specific controls and parked-video safeguards.

| Component | Purpose |
| :--- | :--- |
| `libCarPlayCore.so` | Vehicle-state protocol and CarPlay interface integration |
| Wireless shared identification API | Theme contribution; wireless projection owns the iAP2 patch |
| Automotive framework | Vehicle-specific climate commands and feedback |
| HMG-API | Hook management, settings, packaging and recovery |

The original compatibility report rejected the combined theme, wallpaper and per-seat panel verdict. The upstream checker requires both an E-GMP model and a matching iAP2 library. The K5 is not an E-GMP vehicle, so support must be established through its actual firmware and capabilities rather than bypassing the verdict.

The original Ultra-Lite setup requires a modified EV-routing package to advertise the theme to the iPhone. This port contributes theme identity through the existing wireless shared identification API rather than importing the older native identity patch. EV routing and fabricated EV data are not part of the design. Separate packages that patch the same locations must not coexist.

### Shared identity integration

Public [wireless RC7 API source](https://github.com/Open-HMG/hmgapi-wireless-cp-aa/blob/v2.2.0-rc.7/wireless-carplay-aa/src/hmgapi/mods/wireless/api/CarPlayIdentification.java) defines service `carplay-identification`, API version 2, `registerTheme`, `removeTheme` and a `ThemeIdentity(String, long, String)` value. The wireless provider must expose shared identity. The exact compiled API and runtime lifecycle still require validation. The intended theme tuple from SafariForest is `44C9FA31EC:985:19.0`. Register only a theme, never an EV vehicle contribution. If the service is missing, the preflight withholds Ultra advertising. Cross-process registration and native activation require further verification. Coordinate removal with disable/uninstall and session lifecycle. Do not install a second iAP2 recipe.

See [integration design](docs/integration.md) for the theme adapter and optional Comfort configuration contract. The runtime calls the adapter; provider acceptance and native activation still require on-car verification.

### Optional Comfort integration

Use the [compatible Comfort fork](https://github.com/Nyx-Asteria/hmgapi-comfort). The bridge exposes only six seat thresholds, validates requests inside Comfort and persists through its own settings API. It does not duplicate the automation engine. The section is omitted when the provider is not installed at initialization; compatibility and successful setting changes are checked at runtime. Core projection has no Comfort dependency.

### Build from source

Package ID: `carplay-ultra-lite-k5` · version: `1.0.0-rc.1`.

The [K5 verification workflow](.github/workflows/k5-verify.yml) builds the pinned HMG-API SDK, runs host checks and merges the video engine. Compiled hooks are not signed installable packages. See [source/README.md](source/README.md) for inherited build details; its original Ioniq 6 feature and settings descriptions are historical.

The first prerelease is intended to be signed with a dedicated test key. Do not commit private keys or OEM binaries. Packages signed with different keys may require uninstalling the previous package before installation.

The standalone checker collects reports and fixed allowlisted radio/framework binaries into `CarPlayUltraLite-Check/radio/` on USB. Keep collected proprietary binaries private; do not commit them.

### Development standards

- Keep commits meaningful, independently coherent and tested where possible.
- Amend mistakes into their original commit; use fixup/autosquash for earlier commits rather than retaining standalone repair commits.
- Keep distinct enhancements in separate commits.
- Keep code, tests, manifests, settings and documentation consistent.
- Preserve upstream history and attribution. Use expected-head checks when rewriting our development branch; coordinate shared history rewrites.
- Host tests do not establish on-car compatibility. Fail closed on unknown firmware, unavailable capabilities or invalid safety state.

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
