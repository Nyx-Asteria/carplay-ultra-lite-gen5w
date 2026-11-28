# Retired development infrastructure

The RC10 one-shot publication workflow and the historical WCP session-start overlay are retained in the original k5-compatibility history and the rebased RC10 baseline `fbeb2beac5fc32249595dbdbd2a306b8d39fb18d`. They are not active CPUL build inputs. The signed release workflow checks out the pinned WCP repository and builds its native identity directly. Do not apply the old overlay to a newer provider.

## Historical overlay provenance

# Wireless provider-side CarPlayStartSession source overlay

Public base: Open-HMG/hmgapi-wireless-cp-aa @ 23c564fb09d9951d2c81e1fe599bf1cd058918fb (2.2.0-rc.9 source).
Adapted from the SafariForest CarPlay Ultra-Lite 1.0.0 distribution's
egmp-carplay-ev-routing-v0.4.3-ioniq6.15 source archive, specifically
StartSession.java, identity.c and StartSessionTest.java, based on LIVI by Lasse Heitgres.
Original authors and GPL-3.0-or-later notices retained; Wireless base is AGPL-3.0.
This is a source overlay for a separately reviewed/built Wireless provider,
not a second iAP2 patch, not a replacement binary, and not an upstream commit.
Apply with `sh apply.sh /path/to/clean/pinned/wireless-checkout`.
Run `sh test.sh` for host codec and ownership fixtures.
Rebuild the provider native serializer with its existing native/identity/build.sh
and compile its Java hooks before signing/packaging a test provider.
Do not mix with the stock provider or a second identity mod.
The extension is requested only by a theme owner opting in through the
three-argument registration overload, and additionally gated to DL322 USA
with the reviewed iAP2 binary. Missing receiver metadata or hotspot address
keeps stock startup. Pair normally once to populate cpc-airplay, then restart
and re-pair for Ultra tests. No hotspot credentials, receiver identifiers,
or message contents are logged. Native dispatch ABI and runtime behavior
remain hardware-unverified. Existing two-argument API consumers are unaffected.

