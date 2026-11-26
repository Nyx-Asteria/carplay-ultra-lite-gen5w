# RC9 Stage 1 — iOS 27 seat evidence

Source: supplied ios27-carplay-metadata archive, CarAccessoryFramework.refs.txt and symbols.txt (build 24A446). The declarations establish CAFSeatFan uses CAFUInt8Characteristic and CAFUInt8Range; CAFSeatHeatingCooling uses CAFInt32Characteristic and CAFInt32Range. Both expose Boolean on and autoMode and String vehicleLayoutKey. Both declare CAFPositionedRequired and characteristic update callbacks with fromGroupUpdate. These declarations do not establish numeric wire identifiers, required presence of autoMode, notification envelope, or exact grouping rules. Current K5 seatFan schema advertises format 1 and range 0..3, matching the unsigned type at the semantic level. Do not change service identifiers or add unsupported auto mode based solely on these declarations.

Baseline: preserve immediate heating feedback (default true), keep ventilation feedback experiment independent (default false). Collect characteristic value Java type, subscription and queued notification status for each seat write. BCM remains authoritative. No OEM dispatch changes in this commit.

## Notification and layout comparison

The supplied upstream 1.3.3 archive, `data/vsdump-src/vehicle_state/wire.rs`, constructs updateNotify with command and values on Lane::High and wraps the inner dictionary as pluginData. Our K5 implementation matches this envelope. CAFSeatFan and CAFSeatHeatingCooling both implement CAFPositionedRequired and declare indexBy and _characteristicDidUpdate:fromGroupUpdate:. The extracted headers do not expose their return values or method bodies. CAFSeatFan exposes fanLevelDisabled, fanLevelInvalid, fanLevelRestricted, hasOn, hasAutoMode and registration properties. These are investigative leads, not proof of a missing characteristic. Added delivery instrumentation distinguishes queued notifications from successful socket writes; socket write is not an Apple UI acknowledgement. No wire schema or command behavior changed.

## Exact iOS 27 constants to recover (24A446)

The actual symbol export locates eight-byte-spaced exported data symbols in __DATA_CONST,__const, but contains addresses rather than stored values. Do not confuse addresses with type IDs. Read eight bytes at each address from the extracted CarAccessoryFramework Mach-O (little endian), or use the matching dyld cache mapping.

| Symbol | VM address |
| --- | --- |
| CAFServiceTypeSeatFan | 0x26953a3f0 |
| CAFServiceTypeSeatHeatingCooling | 0x26953a3f8 |
| CAFCharacteristicTypeFanLevel | 0x26953aa08 |
| CAFCharacteristicTypeHeatingCoolingLevel | 0x26953aa18 |
| CAFCharacteristicTypeOn | 0x26953ab90 |
| CAFCharacteristicTypeAutoMode | 0x26953aac0 |
| CAFCharacteristicTypeVehicleLayoutKey | 0x26953acc0 |

Target method bodies: +[CAFSeatFan indexBy] 0x25731ff6c; -[CAFSeatFan _characteristicDidUpdate:fromGroupUpdate:] 0x25731ff7c; +[CAFSeatHeatingCooling indexBy] 0x2573210c4; -[CAFSeatHeatingCooling _characteristicDidUpdate:fromGroupUpdate:] 0x2573210d4. Also inspect SeatFan fanLevelCharacteristic at 0x25731f944 and heatingCoolingLevelCharacteristic at 0x257320a9c.

The supplied upstream 1.3.3 body.rs advertises only two heating services, type 0x22000001, level 0x31000021, power 0x30000002, layout 0x36000065, under accessory 1024. climate.rs uses 0x31000012 for cabin fan level. Format enum confirms Bool=0, Uint8=1, Int32=7, String=10. No upstream SeatFan service in that archive, so the K5 SeatFan service type 0x22000002 is not independently confirmed by that reference. Current schema is retained pending evidence. OPACK's compact integer representation is independent of Java Long vs Integer; both use the same encoding path.


## Incremental iOS 27.0.1 archive — verified constants and observer limitations

Source: master incremental archive, build 24A446, ARM64e; constants/seat-decoded-strings.txt and disassembly/seats/CAFSeatFan-characteristicDidUpdate.txt. Decoded constant objects are hexadecimal strings, not integer storage at the original symbol addresses. All six advertised K5 identifiers match: SeatFan 0x22000002, SeatHeatingCooling 0x22000001, FanLevel 0x31000012, HeatingCoolingLevel 0x31000021, On 0x30000002, VehicleLayoutKey 0x36000065. AutoMode 0x3000005F is also verified, but its required presence is not established. Both indexBy methods load VehicleLayoutKey. Retain existing identifiers and do not advertise an unsupported auto mode.

At 0x257320068 the fan-level branch tests a comparison against FanLevel, then calls 0x258026370 on the incoming characteristic and again on the characteristic retrieved via 0x2580ce9f0. At 0x2573200a4 it compares the results via 0x258001e40, branching at 0x2573200bc before the callback path. Similar checks occur for On, AutoMode and layout key. The exact property and callback selectors are unresolved: the reconstructed CAF Mach-O's __text spans 0x2572b0368 through 0x2573befa8 (exclusive), and __objc_stubs and __auth_stubs have zero size. The call targets above lie outside this executable section; symbol metadata does not label them. Do not claim that the property is an IID, UUID, availability flag or value. No new extraction requested at this time; continue with existing evidence and vehicle diagnostics.

Stage 1 baseline regression: 74fe862 passed development regression (run 45787840056). No release published.
