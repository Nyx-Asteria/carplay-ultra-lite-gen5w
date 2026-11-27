# Shared identity and Comfort integration

## Theme

The RC7 shared registry is published in zygoteInit and consumed by the
wireless provider in com.tcc.iap2server. DiPO is a separate process.
ThemeContribution is a consumer-side adapter for API v2; it resolves the
exported interface rather than duplicating its classes or native patches.
The theme tuple originates in SafariForest's modified E-GMP routing package.

The adapter is deliberately not invoked yet. Before wiring it in, establish
an authenticated, cross-process readiness signal (or a supported provider
mechanism) and coordinate cold start, crash guard, disable and re-pairing.
A successful registerTheme call does not report native configure success.
Do not enable the display theme merely because the registry accepted it.

## Comfort & Startup (optional)

Reference: https://github.com/lemoyneiv/hmgapi-comfort

Comfort owns configuration, startup automation, occupancy detection and
manual override behavior. CPUL must not run a second automation engine.
HMG-API RC8 client settings/saveSettings are caller-scoped. No direct
writes to another mod's settings file, spoofed caller identity, or copied
Comfort policy implementation.

Proposed upstream interface: versioned configuration read and atomic
threshold update with validation and a revision token (reject stale edits).
Return availability, climate/vent enabled state, active unit, and the
six current threshold values. Preserve unrelated settings. Comfort
performs normalization, persistence and engine reload itself.

Fields: heat_high_f, heat_med_f, heat_low_f, vent_low_f, vent_med_f,
vent_high_f; corresponding Celsius fields where supported by the installed
Comfort version. Validate the actual exported schema before using it.
Comfort defaults in Fahrenheit: 25/38/50 heating; 75/85/95 ventilation.
Do not display defaults as current settings if the service is unavailable.

CPUL's optional integration toggle defaults on when implemented.
Hide the CarPlay settings group if Comfort is absent or its API incompatible.
Use the supported CarPlay vehicle-settings characteristics for six visual
threshold controls; the exact layout and writable value types need validation.
Never expose rear seats or ambient lighting on this K5 profile.
No Comfort dependency is added to the manifest until an optional mechanism
is established; ordinary CPUL operation must not require Comfort.
