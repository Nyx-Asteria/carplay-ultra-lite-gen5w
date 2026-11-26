# Phone-managed cluster layout experiment

Default off, restart required. Requires UI Synchronization and Ultra Sidebars. When enabled, uiSync does not issue the initial cluster REQUEST_LAYOUT, even if a preference was saved; the receiver still allows layout changes, acknowledges commands and gives focus. Preferences are retained for restoration when disabled. The virtual cluster, identity and native display advertisements are unchanged. Center wallpaper override is independent.

RC10: compare theme selector and negotiated panel traffic with only this toggle changed. Wired comparison is not a prerequisite: user reports wired currently requires disabling WCP and uninstalling Nemesis EWF, which also removes shared theme identity and changes the test configuration. Wireless is the primary validation target. This experiment does not establish that initial cluster layout selection causes the missing themes.
