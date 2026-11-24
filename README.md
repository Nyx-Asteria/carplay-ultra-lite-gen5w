<div align="center">

# K5 CarPlay Ultra

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

**K5 CarPlay Ultra** is an independent port of [SafariForest's CarPlay Ultra-Lite](https://github.com/SafariForest/carplay-ultra-lite-gen5w), built specifically for the Kia K5.

The aim is to bring the Ultra-style interface, native climate controls and additional vehicle functionality into CarPlay without replacing the factory head unit or changing the existing wireless projection setup.

Unlike the original multi-vehicle project, this port targets one known vehicle and firmware configuration. Unnecessary compatibility paths and EV-specific functionality will be removed where appropriate.

> [!NOTE]
> This project is in early development. The features below are planned, not verified on-car. No installable K5 release is available yet.

## Features

- **Ultra interface** — themes, wallpapers and individual climate panels.
- **Integrated vehicle identification** — the theme-enabling portion of the original modified EV-routing package, incorporated into our own mod without EV routing.
- **Native climate** — temperatures, fan, A/C, AUTO, SYNC, vents, recirculation and supported defrost controls.
- **Seat controls** — heating, ventilation and steering wheel heat where equipped and verified.
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

The existing wireless-carplay-aa package and working Wi-Fi configuration remain separate and unchanged. The modified E-GMP routing package will not be a separate installation requirement once its theme-identification functionality has been integrated.

## Development

### Stage 1 — Ultra interface and climate

- [ ] Validate the captured K5 firmware, hook map, native recipes and runtime patch compatibility.
- [ ] Replace the generic compatibility checker with K5-specific checks.
- [ ] Obtain the exact theme-enabled routing source and integrate the required identity hooks into the K5 package.
- [ ] Remove EV routing, battery data and unnecessary multi-vehicle paths.
- [ ] Assign a distinct package ID and display name; update internal paths, settings, build tooling and conflict handling together.
- [ ] Validate climate methods, listener fingerprints, flags and command mappings.
- [ ] Enable supported climate and seat controls with physical-control feedback.
- [ ] Verify themes, wallpapers, per-seat panels and return to the Kia home screen.
- [ ] Build a signed prerelease with compatibility guards and reversible installation.
- [ ] Test wireless CarPlay audio, calls, navigation, reconnects and Android Auto regressions while parked.

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

## Technical details

The port is based on [feature/climate-and-video](https://github.com/SafariForest/carplay-ultra-lite-gen5w/tree/feature/climate-and-video) at `70e3ba2`. The [EV6 fork](https://github.com/lths/carplay-ultra-lite-ev6) serves as a reference for Kia-specific controls and parked-video safeguards.

| Component | Purpose |
| :--- | :--- |
| `libCarPlayCore.so` | Vehicle-state protocol and CarPlay interface integration |
| `libiap2session.so` | Theme identification integration, subject to recipe validation |
| Automotive framework | Vehicle-specific climate commands and feedback |
| HMG-API | Hook management, settings, packaging and recovery |

The original compatibility report rejected the combined theme, wallpaper and per-seat panel verdict. The upstream checker requires both an E-GMP model and a matching iAP2 library. The K5 is not an E-GMP vehicle, so support must be established through its actual firmware and capabilities rather than bypassing the verdict.

The original Ultra-Lite setup requires a modified EV-routing package to advertise the theme to the iPhone. This port will incorporate the required identification functionality into its own package, retaining the applicable source and attribution. EV routing and fabricated EV data are not part of the design. Separate packages that patch the same locations must not coexist.

### Build from source

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
