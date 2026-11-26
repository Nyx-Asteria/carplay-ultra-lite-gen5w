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
