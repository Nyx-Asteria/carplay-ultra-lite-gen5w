# RC8 climate development checkpoint

Target: DL322 USA 260507, Gen5W_M, API 19. Development branch only; no release implied.

## RC7 vehicle evidence (November 25)

- Passenger heat: 3, 2, 1, off commanded and BCM confirmed (raw 8, 7, 6, 2).
- Driver heat: phone level requests produce the expected raw 7, 6 and 2 transactions, but BCM stays at 8 in the recorded failure sequence. A successful binder return is not physical acceptance. A separate transition from raw 5 to 8 was seen; whether a physical button caused it is unknown.
- Passenger ventilation: phone repeatedly sends level 3, raw 5. Tester confirms physical activation but the CarPlay button stays inactive. Lower levels and mode switching remain unverified. Driver ventilation remains unresolved.
- Other climate functions previously reported working: temperatures, sync, fan, airflow, auto, AC, recirculation, defrost, HVAC power, steering wheel heater.
- Do not infer actual phone notification delivery from a BCM confirmation log.

## Development changes

- Preserve prior seat state and retain an independent actual BCM snapshot for arbitration.
- Explicit inactive mode power takes precedence over its retained nonzero level; genuinely ambiguous activation remains rejected.
- Pair subscribed ventilation level/power notifications for both phone and physical changes. Deduplicate unchanged physical readings. Read/register responses use actual BCM ventilation state when known.
- Trace ventilation subscription IDs, paired feedback eligibility, resolved OEM transaction and pre-dispatch BCM/USM readings.
- Guard disconnected seat dispatch. No change to known OEM raw values or passenger setter.
- Regression tests cover physical and requested feedback, duplicate suppression, pending-write read/register responses and expanded schema IID uniqueness. Host tests cannot verify Apple UI behavior.

## Next parked vehicle test

Keep the known working Ultra/Sidebars profile. Disable Comfort automatic climate temporarily for the isolation run; restore its original setting afterwards. Do not change multiple schema settings at once.

1. Passenger heat control: from off, request high, medium, low, off; record physical indicators and BCM states.
2. Driver heat: repeat the same steps, waiting at least three seconds per command. Do not touch physical buttons during this sequence. Then repeat with physical buttons alone. Record pre-dispatch USM and BCM readings and subsequent BCM changes.
3. Passenger ventilation: use physical buttons to select high, medium, low and off. Record CarPlay icon and level for each step. Then request ventilation via CarPlay and record whether it activates or cycles. Repeat for driver.
4. Switch heat to ventilation and back, separately per seat. Do not issue simultaneous opposite-mode requests.
5. Reconnect while passenger ventilation is active; record initial icon, registration and read responses. Compare with a fresh connection when off.
6. If needed repeat driver heating with Comfort restored, keeping all other settings identical.

Capture [seat], [seat-dispatch], [seat-fan], VS phone follows, VS phone reads, and CAR sent lines with timestamps. Record which controls were physically pressed. Logs showing subscriptions false indicate an eligibility issue; true only establishes eligibility, not phone acceptance. If BCM changes without visual feedback, inspect outgoing notification and schema; if BCM remains unchanged after a successful transaction, investigate OEM execution context. Do not send unsolicited characteristic updates or invent Apple type IDs without evidence.

## Deferred

Separate SeatFan schema is experimental; LIVI provides cabin FanLevel and seat heating, not a verified ventilated-seat service. No protocol identifier changes without independent evidence. Driver setter matches HMG-API base hook map; runtime K5 acceptance remains unresolved. No Android Auto work. No version bump.
