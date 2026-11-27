# RC10 CPUL audit checkpoint and candidate freeze

Reviewed candidate: 8514988d6270c9926642dc13cab68bcc26ef99a7
Validated runtime revision: 42594f784fe8afb7858606647e6310f3eb6e7f6d
Regression: https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/actions/runs/45931795153 (success).

The comparison against the validated runtime revision contains only CHANGELOG.md, docs/rc10-audit-disposition.md, docs/rc10-research-closure.md and docs/rc10-testing.md. The document containing this checkpoint adds no runtime changes.

The bounded RC10 CPUL development audit is closed: the identified corrections are regression-validated, subsystem dispositions and research limitations are recorded, and no further actionable CPUL-only change is identified in the reviewed scope. This is not exhaustive verification of every path or physical behavior.

Freeze runtime changes pending WCP cross-project review. The owner controls when WCP starts that review. Any subsequently identified runtime blocker requires a new commit, regression validation and updated candidate reference.

## Packaging-only work after WCP clearance

- Finalize WCP and STBT pins and their complete test commands.
- Reconcile CPUL manifest version (currently 1.0.0-rc.9), description and minimum HMG-API compatibility requirement (currently >=1.1.0-rc.8; target RC10). Confirm the requirement with final dependency review rather than implying earlier SDK compatibility was tested.
- Confirm seven-package build, permanent signing identity, signatures, checksums and final changelog.
- Do not trigger the release commit or publish without WCP clearance and owner authorization.

Vehicle testing is not an RC10 development gate. The documented runtime unknowns remain unverified.
