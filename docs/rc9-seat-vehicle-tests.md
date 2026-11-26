# RC9 vehicle validation — Stage 1

Automated baseline: 74fe862, development regression 45787840056 passed. Apple identifiers and positioning confirmed in incremental iOS archive; visual feedback remains unverified. No additional vehicle dump needed for RC9.

Test parked, with seat heating immediate feedback ON and ventilation immediate feedback OFF first. For driver and passenger independently: off → heat levels 1/2/3 → off; off → vent levels 1/2/3 → off; heat → vent; vent → heat; physical switches at each level; reconnect with an active seat. Record physical state and displayed level/power separately. Repeat ventilation with immediate feedback ON, without changing heating. Export CPUL session logs after each setting configuration. Check [seat-feedback] writes, BCM paired feedback, requested/sent lane and socketWrite. Successful socket write does not establish UI acceptance. If heating regresses, stop and restore baseline. No unsupported AutoMode or identifier changes.
