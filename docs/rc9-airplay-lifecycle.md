# RC9 AirPlay lifecycle checkpoint

Wireless transport review is owned by the independent WCP project. A wired comparison is optional, not a release gate; do not modify the user's working wireless setup solely for this test.

RC8 rendered an initial H.264 frame before projection teardown. This does not establish that decoding, the HTTP proxy, or sender status caused the disconnect.

RC9 uses shared playback status reporting across both existing envelopes. Player teardown now removes the shared analytics listener before releasing the old player; the old item identity remains until its player is detached. Initial picture logging includes the existing reported rate, readiness and position. No invented first-frame acknowledgment, extra sender messages, codec changes or Park bypass.

RC10: correlate first picture, playback responses, sender stop, native finalization, WCP transport events and proxy failures using session logs. Compare wired only if readily available. A queued callback already executing before listener removal is not ruled out by static review.
