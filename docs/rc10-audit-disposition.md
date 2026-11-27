# RC10 CPUL development disposition

Runtime reference: 42594f784fe8afb7858606647e6310f3eb6e7f6d. Development regression 45931795153 succeeded. Through cc33a0e321db22051e783c40f0d51cb76a22a203 the only differences are CHANGELOG.md, docs/rc10-testing.md and docs/rc10-research-closure.md. This is a bounded disposition of reviewed development work, not certification of all source paths or vehicle behavior.

## Scope closure

| Area | Disposition |
| --- | --- |
| Seat heating and ventilation | Existing command arbitration, requested/actual separation, paired feedback and pre-subscription cache regression retained. Physical actuation and rendering remain unverified. |
| Passenger temperature | Retain existing verified transaction mapping, generation guards and DATC sample correlation. No newly established K5 setter. |
| FM and HD | Retain verified native methods and guarded source/tune requests, cached list and metadata ordering corrections. HD multicast encoding and physical activation/seek remain unverified. |
| SiriusXM | Preserve catalog exclusions, live metadata correlation and subscription/skip/lock checks. Display numbering, source activation and artwork remain unverified. |
| Audio/Bose | Retain read-only Bose sample policy, owner-bound volume/mute commands and OEM panel fallback. No new amplifier writes. |
| AirPlay | Retain playback generation, Park gating, screen ownership and teardown protections; first-frame disconnect causality remains unresolved. |
| Theme | Retain exact identity, provider ownership and rollback, independent display negotiation and experimental gates. Native activation, selector and wallpaper require runtime evidence. |
| Vehicle and native apps | Retain session-owned delivery, explicit HMG-API activity fallback, range/unit validity checks. Nemesis entry point and range argument semantics not established. |
| Firmware and IPSW research | Recorded evidence does not justify speculative method arguments or new wire fields. See rc9-seat-evidence.md, rc9-hd-radio-evidence.md, rc10-research-closure.md and rc10-testing.md. No additional extraction requested for this scope. |

## Release assurance

Reviewed development-regression.yml, release-bundle.yml and shared run-regressions.sh. Development and release use the same CPUL host suite. Release additionally builds companions, checks permanent signing UUID, verifies individual signatures, enforces seven packages and produces SHA256SUMS and the combined bundle. Current pinned WCP/STBT versions and companion-specific test commands are deliberately unchanged pending approval; update them against the finalized repositories before packaging.

The release workflow currently couples successful packaging to automatic publication on a matching Release commit. Do not trigger it prior to WCP approval and explicit publication authorization. Physical vehicle validation is excluded from development readiness.

## Remaining gates

- Confirm final comprehensive CPUL audit disposition and freeze exact revision.
- Only then authorize WCP cross-project audit.
- Update finalized companion pins and verify their complete test commands and package versions.
- Verify signed bundle before publication; no release authorization is conveyed by this document.

Historical rc10-roadmap.md contains dated implementation checkpoints and pending-CI annotations; this document records the current disposition rather than rewriting that history.
