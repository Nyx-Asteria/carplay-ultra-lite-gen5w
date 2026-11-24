<div align="center">

# CarPlay Ultra-Lite K5

**Your Kia. Your CarPlay.**

A dedicated CarPlay Ultra-style experience for the Kia K5, powered by HMG-API.

[![Status](https://img.shields.io/badge/status-in_development-f59e0b?style=flat-square)](#development)
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
> This project is in early development. The features below are planned, not verified on-car. No installable K5 release is available yet.

## Features

- **Ultra interface** — themes, wallpapers and individual climate panels.
- **Integrated vehicle identification** — theme assets contributed through the existing wireless CarPlay shared identification API, without EV routing.
- **Native climate** — temperatures, fan, A/C, AUTO, SYNC, vents, recirculation and supported defrost controls.
- **Seat controls** — three-level heating and ventilation for the driver and front passenger.
- **Comfort integration** — optional visual editing of Comfort & Startup's three heating and three ventilation thresholds, using Comfort as the source of truth.
- **Parked video** — optional AirPlay video with strict Park and session-state safeguards.
- **K5 vehicle page** — vehicle information, native Kia shortcuts and additional supported controls.
- **HMG-API integration** — one signed K5 package, managed independently of wireless projection.

## Compatibility

| Vehicle | Software version | Verification |
| :--- | :--- | :--- |
| **2022 Kia K5 (US)** | `DL322.USA.0000.V015.012.260507` | Target vehicle; initial compatibility report collected, port not yet tested |

<details>
<summary><strong>Target platform</strong></summary>

| Property | Value |
| :--- | :--- |
| Head unit | Gen5W |
| Android | 4.4.2 / API 19 |
| Display | 1920 × 720 |
| Projection | Existing working HMG-API wireless CarPlay |

</details>

Other vehicles and firmware versions are outside the scope of this port. Matching native library fingerprints and framework methods will be required before hooks are applied.

## Installation

### Requirements

- The supported Kia K5 firmware.
- [HMG-API](https://github.com/Open-HMG/HMG-API) with working wireless CarPlay and hooks enabled.
- An iPhone running iOS 26 or later.
- A signed package from this fork.

> [!WARNING]
> There is no validated K5 package yet. Do not install the inherited Ioniq 6 or EV6 packages as a substitute.

The intended distribution is a single signed HMG-API USB package, published through [Releases](../../releases) once ready. Installation and rollback instructions will accompany the first prerelease.

The existing wireless-carplay-aa package and working Wi-Fi configuration remain separate and unchanged. The K5 port uses HMG-API and the existing wireless-carplay-aa installation. Shared CarPlay identity must be enabled. No wireless package version is pinned; shared API compatibility is checked at runtime. The modified E-GMP routing package is not intended to be installed.

## Development

### Stage 1 — Ultra interface and climate

- [x] Restrict the manifest to DL322 USA 260507 and add a stock firmware preflight.
- [ ] Validate hook maps, native recipes and runtime patch compatibility.
- [x] Replace the generic compatibility checker with K5-specific target and shared identity prerequisites (compile/runtime validation pending).
- [x] Obtain the original and modified routing sources and identify the theme contribution.
- [x] Confirm public wireless RC7 exposes independent theme registration and the target package includes shared identity support.
- [x] Add a theme-only API v2 consumer adapter.
- [ ] Wire the adapter to a verified cross-process readiness and recovery lifecycle.
- [ ] Remove EV routing, battery data and unnecessary multi-vehicle paths.
- [x] Assign a distinct package ID, development version and conflict with the original package.
- [ ] Audit remaining internal paths and package tooling.
- [ ] Validate climate methods, listener fingerprints, flags and command mappings.
- [ ] Enable supported climate and seat controls with physical-control feedback.
- [ ] Verify themes, wallpapers, per-seat panels and return to the Kia home screen.
- [ ] Build a signed prerelease with compatibility guards and reversible installation.
- [ ] Test wireless CarPlay audio, calls, navigation, reconnects and Android Auto regressions while parked.

The current development code deliberately withholds Ultra theme and AirPlay video advertising until their respective integration and safety checks are complete. This is not an installation-ready build.

### Stage 2 — AirPlay video

- [ ] Review upstream video and EV6 improvements.
- [ ] Require an active CarPlay session and verified fresh Park state; stop immediately on state loss, non-Park or errors.
- [ ] Keep video disabled by default and prevent automatic restart after returning to Park.
- [ ] Validate H.264 decoding, overlays, audio and return to normal CarPlay while parked.
- [ ] Retain TLS certificate validation and redact sensitive logs.

### Stage 3 — K5 experience


- [ ] Create a K5-specific vehicle page and Kia-inspired visual identity.
- [ ] Investigate fuel level, range, odometer and other read-only data.
- [ ] Add native Kia and installed-mod shortcuts.
- [ ] Explore volume, vehicle notifications, radio and sound settings.
- [ ] Evaluate GPU support for future 3D vehicle visuals.

## Diagnostics

HMG-API logs report individual firmware fingerprint results, climate startup and profile, native recipe requests, session generations and finalization, and theme API failures. Native recipe success is reported by HMG-API itself. Raw projection SETUP payloads are not copied into the mod log. The crash guard clears only after the same session remains active for the full health interval. The port does not intercept wireless iAP send traffic.

## Test settings

One boolean toggle per major feature, enabled by default once implemented. Climate integration enables both vehicle feedback and supported commands together. The K5 target exposes only front driver/passenger seats with three heating and three ventilation levels; no rear seat controls, rear climate zones or ambient lighting. Steering wheel heat and Driver Only are withheld pending equipment validation. Unsupported command groups are rejected before queueing and sending. Restart after changes. Theme and parked video switches will be added with their implementation.

## Technical details

The port is based on [feature/climate-and-video](https://github.com/SafariForest/carplay-ultra-lite-gen5w/tree/feature/climate-and-video) at `70e3ba2`. The [EV6 fork](https://github.com/lths/carplay-ultra-lite-ev6) serves as a reference for Kia-specific controls and parked-video safeguards.

| Component | Purpose |
| :--- | :--- |
| `libCarPlayCore.so` | Vehicle-state protocol and CarPlay interface integration |
| Wireless shared identification API | Theme contribution; wireless projection owns the iAP2 patch |
| Automotive framework | Vehicle-specific climate commands and feedback |
| HMG-API | Hook management, settings, packaging and recovery |

The original compatibility report rejected the combined theme, wallpaper and per-seat panel verdict. The upstream checker requires both an E-GMP model and a matching iAP2 library. The K5 is not an E-GMP vehicle, so support must be established through its actual firmware and capabilities rather than bypassing the verdict.

The original Ultra-Lite setup requires a modified EV-routing package to advertise the theme to the iPhone. This port will contribute theme identity through the existing wireless shared identification API rather than importing the older native identity patch. EV routing and fabricated EV data are not part of the design. Separate packages that patch the same locations must not coexist.

### Shared identity integration

Public [wireless RC7 API source](https://github.com/Open-HMG/hmgapi-wireless-cp-aa/blob/v2.2.0-rc.7/wireless-carplay-aa/src/hmgapi/mods/wireless/api/CarPlayIdentification.java) defines service `carplay-identification`, API version 2, `registerTheme`, `removeTheme` and a `ThemeIdentity(String, long, String)` value. The wireless provider must expose shared identity. The exact compiled API and runtime lifecycle still require validation. The intended theme tuple from SafariForest is `44C9FA31EC:985:19.0`. Register only a theme, never an EV vehicle contribution. If the service is missing or registration fails, leave Ultra advertising disabled and retain normal projection. Coordinate removal with disable/uninstall and session lifecycle. Do not install a second iAP2 recipe.

See [integration design](docs/integration.md) for the theme adapter and optional Comfort configuration contract. The adapter is not yet called by the runtime.

### Stage 4 — Optional Comfort integration

- [ ] Add optional [Comfort & Startup](https://github.com/lemoyneiv/hmgapi-comfort) threshold controls using an owner-authorized configuration bridge.
- [ ] If necessary, develop a separate Comfort fork with a supported configuration API.

Comfort integration must not block the core K5 port, video, vehicle information or shortcuts. No Comfort dependency is required for those features.

### Build from source

The K5 manifest uses package ID `carplay-ultra-lite-k5` and version `0.1.0-alpha.1`. The original `cp-climate` package conflicts with this port; do not install both.

The inherited build instructions are in [source/README.md](source/README.md). K5-specific build and packaging instructions will replace them as the implementation develops.

Use a consistent author key and never commit signing keys, proprietary firmware, device identifiers or credentials.

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
- **[Nyx-Asteria](https://github.com/Nyx-Asteria)** — K5 port, development and testing.

Released under **[GPL-3.0-or-later](LICENSE)**. Applicable upstream notices and corresponding source must be retained.

---

<div align="center">

<sub>Unofficial project. Not affiliated with Apple, Hyundai or Kia. Head-unit modifications are performed at your own risk.</sub>

</div>
