# Release review — 27 September 2026

# 0.2.0 — BYD navigation and connection improvements

- Standalone windshield HUD arrows, distance and street names on the verified DiLink5.1 firmware; no ADB, root or computer helper.
- Retain contributor cluster/SOME-IP navigation, route parsing, BYD CarPlay icon and display-size presets.
- Fix Car hotspot startup by using scoped IPv6 when available and binding discovery/probing to the AP interface. Physically confirmed on the development car.
- Drain asynchronously decoded audio during packet gaps and rebuild the music buffer after starvation. Wi-Fi Direct is much better in the user retest; occasional audio cutouts remain for a later version.
- Preserve bounded music-buffer choices, USB read improvements and decoder recovery; fix USB request/close races and keep vendor output outside phone callbacks.
- Save audio/video/receive timing and discovery diagnostics without road names or protocol payloads.
- HUD cleanup on normal end/disconnect/off/stale input; interrupted sessions recover on the next app launch. Force-stop may leave guidance visible until reopening.
- Thanks to @romanchukg-cloud and @georgiyrr for PR #3 and vehicle testing.

## Validation scope

The user physically confirmed live standalone HUD guidance and street names in both apps. The latest DiPlay test also confirmed Car hotspot startup and substantially improved Wi-Fi Direct performance. Occasional audio cutouts remain; the user explicitly deferred them to another version and authorized pushing, merging and releasing these changes. The release packages now permit the verified standalone backend while retaining the exact firmware/stock-receiver guard. See [BYD navigation](BYD_NAVIGATION.md).

Earlier local review blockers for the standalone HUD path are resolved by physical tests. No claim is made that every vehicle, map app, force-stop sequence or USB failure mode was physically tested. Release checks include unit tests, build/lint, package/signature inspection and public-source credential checks. Android signing secrets and accessory private assets remain outside the repository/source archives.

Review corrections retain codec release/reconfigure during video recovery, synchronize USB request publication with close, interpret vendor status codes correctly, and separate vendor work from phone control callbacks. Runtime authentication assets follow the established explicit local release packaging.
