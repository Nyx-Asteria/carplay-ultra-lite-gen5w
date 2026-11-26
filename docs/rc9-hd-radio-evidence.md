# RC9 HD Radio evidence and RC10 backlog

iOS 24A446 master archive, headers/System_Library_PrivateFrameworks_CarAccessoryFramework.framework_CarAccessoryFramework.objc.txt:
- CAFMedia tuneToFrequency:inSourceWithIdentifier:multicastIndex:completion:
- CAFMedia tuneToMediaItem:inSource:multicastIndex:completion:
- CAFMedia tuneToMediaItemIdentifier:inSourceWithIdentifier:multicastIndex:completion:
- CAFMediaItem multicast is declared unsigned char; CAFNowPlayingSnapshot multicast is unsigned 64-bit. These declarations do not establish wire keys, numbering or absence/default semantics.

K5FirmwareDump(5).zip HKMC_RadioNA.odex strings include spsChannelDirect, spsChannelTuneDirect, onHDRadioSPSStatusChanged, onHDRadioID3TagChanged, onHDRadioID3TagsCleared, onHDRadioID3AlbumArtReceived, mSPSChannelNumber. Strings alone do not establish callback argument ordering or method semantics.

Current CPUL K5Radio receives RADIO_METADATA broadcasts keyed by frequency, not program. Current branch tree contains no identifiable Java producer for this action; investigate the installed package and other hook sources before changing its contract. Preserve HD1 metadata, do not invent program IDs or enable speculative native multicast writes. Observe numeric multicast fields on explicit tune requests without changing tuning behavior; candidate field names are not verified wire keys.

RC10 parked validation: HD1/HD2/HD3/HD1 at same frequency, compare OEM program and CarPlay metadata; record incoming multicast control parameters and OEM callbacks. Verify source activation and seat UI using RC9 logs. No fresh firmware or IPSW extraction is a prerequisite for RC9.
