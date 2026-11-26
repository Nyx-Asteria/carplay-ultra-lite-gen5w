# Theme eligibility checkpoint

The catalog diagnostic is a standalone CI Python tool, not a runtime asset installer. It retrieves metadata for the exact DBX asset/version and deliberately does not substitute another theme. The runtime Java consumer contributes the theme identity through WCP API v2; it does not download phone assets. Registry availability and registration are not proof of native activation.

CPUL previously evaluated provider availability only in zygoteInit. Recheck in each relevant consumer process, retaining exact stock firmware checks and settings gates. This avoids treating early provider absence as definitive. Process-local availability still cannot prove that the other process registered successfully; RC10 should correlate provider native result with both preflight logs, advertised display panels and actual phone theme selector. No asset download or new display advertisement added.
