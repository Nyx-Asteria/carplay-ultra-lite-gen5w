# EV6 fork adaptation

Imported unchanged from https://github.com/lths/carplay-ultra-lite-ev6/tree/1edc0d0b7aa3cf34f501e90fea24a48d7d98f99d (GPL-3.0-or-later; see upstream history for individual contributors):

- VehiclePage.java: CarAccessoryFramework settings schema and state machinery.
- ParkedState.java and ParkedStateTest.java: freshness-checked Park predicate and host regression tests.

These classes are not yet connected to the K5 runtime. Park status must be obtained from a signature-checked read-only getter and enforced throughout video playback, including independent stale-sample watchdog, session end and engine shutdown. Vehicle page content and commands must be K5-specific. Do not import EV data or control paths.
