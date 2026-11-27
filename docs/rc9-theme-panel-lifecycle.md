# RC9 uiSync panel lifecycle

The existing connection maintains a single UiSync handshake even if two display IDs are interleaved. RC9 separates recognized Center_Display and Cluster_Display handshake state, while preserving a bounded legacy state for messages without a recognized panel ID. No change to display identities, theme catalog, requested cluster layout or default-off forced center layout. Preference writes merge the existing properties under a shared lock to prevent one panel overwriting another's selections.

This is a defensive compatibility correction, not proof that missing center negotiation or empty theme selector is fixed. RC10 must verify whether frames for both displays share a connection and whether any frames omit displayUUID. Unknown display identities are not advertised. Existing DBX theme identity remains unchanged.

WCP reported native AirPlay patches and watchdog investigation; transport changes are not justified yet. Wired comparison remains optional. CPUL retains playback and uiSync ownership.
