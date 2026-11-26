# RC9 Bose Centerpoint diagnostic

Keep OEM setting 48 read-only. Raw availability and setting values are not confirmed enumerations and are not translated into an on/off control. Publish each pair as one immutable sample with generation, timestamp and source; invalidate on disconnect/read failure and hide if the current media source differs. The next car test should compare raw samples with the OEM Centerpoint UI off/on for CarPlay, HD FM and SiriusXM where available. No setter or guessed binder transaction introduced. This is diagnostic hygiene, not a verified Centerpoint control.
