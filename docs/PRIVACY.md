# Privacy and diagnostics

DiPlay's product flow uses local authentication and a direct USB/Wi-Fi connection to the iPhone. No account, remote authentication service or automatic diagnostic upload is used. The iPhone's CarPlay apps have their own internet and privacy behavior.

The head unit stores app preferences, paired-device selections, pairing data and bounded diagnostic logs in app storage. Authentication and pairing material are kept out of Android backup. Uninstalling removes app-private data; exported reports in Downloads remain until you delete them.

Diagnostic export is initiated by you. Reports include app/device versions, display settings and negotiation, connection transitions, Wi-Fi band/channel and state, and decoder recovery events. The exporter filters protocol payloads, credential-bearing lines and common identifiers. Redaction cannot promise to recognize every vendor-specific string: review reports before posting them publicly. A GitHub issue is public.

Microphone access supports Siri and calls. Bluetooth/Nearby devices and Wi-Fi/Location permissions support discovery and transport. The optional local VPN permission supports the USB link; it does not provide a remote internet VPN.

The verified DiLink 5.1 cluster profile optionally uses Android Usage Access to follow theme and mini-map-card visibility. This permission exposes app-activity history. DiPlay filters the results to four stock BYD cluster activities, processes them locally, and logs only inferred cluster theme/visibility changes. Unrelated activity events are not retained or uploaded. Automatic mode is opt-in; disabling it stops these queries. Usage Access can also be revoked as described in [BYD navigation](BYD_NAVIGATION.md#dilink-51-theme-profile).

The static website has no analytics script or account. GitHub Pages, GitHub and Telegram apply their own policies when you use those services.
