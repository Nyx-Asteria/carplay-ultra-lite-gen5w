# K5 radio integration

DL322 USA 260507 owner report identifies the installed radio package as `com.hkmc.av.app.radiona`. The firmware inventory lists `/system/app/HKMC_RadioNA.apk` and `.odex` but does not include their contents. Direct AM/FM/SXM tuning, presets, seek and station metadata are not implemented. Obtain the radio APK/ODEX and automotive framework interfaces before selecting a tuner protocol. The current Vehicle page opens the stock radio only on explicit request; it never selects a source on connection or sends phone media keys as radio commands.
