# Standalone AirPlay integration — design and validation boundary

## Scope
Keep CPUL CarPlay-native video (existing ExoPlayer, phone proxy and DiPO screen borrowing) independent of a future standalone AirPlay receiver. Standalone receiver is deferred from the next vehicle-test release. No listener, advertisement, new native binary or setting is enabled by this document.

## Reference implementations
- LIVI f-io/LIVI@f84638ed63739f8648cc6dded55338f36508ee64: native/crates/livi-cp/src/video_playback.rs and stack/video.rs implement CarPlay video; projection/airplay.rs in livi-core and livi-cp/src/airplay.rs implement a separate receiver. Its standalone handler does not demonstrate URL-video /play or /scrub support; its advertised capabilities are not proof of YouTube compatibility.
- CubbyNINJA/g5w-parkcast@3eec8d0703d8e12e8288f4e44bf45acf99e3da17: receiver/native/CMakeLists.txt, deps.lock, receiver/app/src/main/java/parkcast/receiver/NativeBridge.java, receiver/core/src/main/java/parkcast/core/{StartSequence,Advertiser,CodecDisplay}.java. AGPL-3.0; UxPlay GPL-3.0. Review combined-work licensing and provide corresponding source and notices before distributing derivative code. No code copied here.

## Proposed embedded receiver
1. Build and validate ParkCast's native UxPlay core independently with NDK r25c, armeabi-v7a, android-19, pinned dependencies and existing mdnsd support. Adapt host Perl path for Linux; no second mDNS daemon. Do not advertise unsupported capabilities or copy another receiver's identity.
2. Keep native library and receiver lifecycle separate from DiPO. Prefer an isolated companion process to avoid native crashes killing CarPlay. Use authenticated local IPC with narrow lifecycle commands. Persist independent pairing identity in private app storage; no logging of secrets.
3. Startup: verify the intended hotspot/interface, reserve non-conflicting sockets, register service with existing mdnsd, then accept. ParkCast used TCP/UDP 35000–35002 on its test unit; verify availability and actual interface on DL322 before binding. Never reserve DiPO TCP 7000 or its UDP ports; do not stop or reconfigure its network services. ParkCast's coexist probe is from another firmware, not proof of K5 compatibility.
4. Preserve generation-tagged native events (connection accepted, session started, codec config, access unit, session ended, advertisement lost). Drop stale events. Avoid large unbounded UI-thread queues.
5. Use a single video display owner at a time; refuse standalone playback during active CarPlay until coexistence has been demonstrated. Retain factory reverse-camera priority. Borrow no screen without a positive, fresh Park reading; black cover before decoder start, hide on first permitted fresh frame, cover and stop on Park loss, stale state, reverse, shutdown or ownership loss. Current CpClimate.videoAllowed() also requires sessionActive; standalone receiver needs its own explicitly authorized lifecycle while retaining ParkedVideo.allowed(), not a bypass of the existing guard. Verify monitor startup independent of CarPlay and fail closed.
6. First milestone: mirrored H.264 video from Photos or screen mirroring, no audio. Add audio only after safe ownership and teardown verified; verify actual YouTube picker separately, since screen mirroring does not imply URL playback support.

## Evidence and test gates
ParkCast's documented 2026-11-23 test (docs/evidence/S3_FIRST_RUN_FINDINGS.md) discovered and connected to receiver with CarPlay disconnected, but TCP 35000 never received a mirror data connection. No codec config reached Java. Do not claim working video. Investigate upstream SETUP and listener handoff using redacted diagnostics; avoid logging pairing secrets. 
- Host: native cross-build, dependency verification, JNI callback generation, port conflict rollback, shutdown, pairing persistence, fail-closed Park and reverse tests.
- Parked vehicle: standalone discovery, pairing, first H.264 frame, exit; confirm CarPlay reconnects afterward. Verify stale Park or reverse preempts video. No public release before these checks.
- Separate later milestone: active-CarPlay coexistence, audio routing and app-specific discovery. Test on actual DL322 firmware, not only the reference unit.
