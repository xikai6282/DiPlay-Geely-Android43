# BYD navigation displays

Phone navigation arrows, next-turn distance and street names can appear on supported BYD displays. Ordinary operation requires no ADB, root, laptop or helper process. The map app must provide structured navigation metadata; compatibility is not guaranteed for every map app or version.

## Validated windshield path

Live guidance and street names were physically confirmed in both DiAuto and DiPlay on DiLink5.1 / Android13, firmware `BYD-AUTO/IVI/IVI:13/TP1A.220624.014/eng.build20260722.221155:user/release-keys`. The standalone output is restricted to that firmware and the verified stock receiver version10601004/signing certificate. Other firmware is not implicitly enabled by this result. Existing cluster/SOME-IP outputs remain available on supported factory services; the DiPlay contributor independently reported DiLink5.0 cluster/HUD operation.

The app sends navigation-only broadcasts to the stock ClusterDebug receiver as its normal Android UID. Vendor output runs outside phone control callbacks. Street text uses the installed HAL's UTF-16LE chunk protocol, capped at48 UTF-16 units without splitting a surrogate pair. Output logs exclude street text.

Normal route end, disconnect, disabling navigation output and stale guidance trigger cleanup. Force-stop/process kill may leave the last instruction visible until the app opens again; a recovery journal handles that next launch. There is no guaranteed process-independent expiry. Run only one projection app at a time.

Enable BYD navigation in settings. In DiAuto it is opt-in under Navigation; in DiPlay it is enabled by default when available. Debug-only receivers/demos require Android's DUMP permission and are absent from release manifests. Development starter and vendor-access experiments are not part of the production navigation path.

## CarPlay map on the instrument cluster (experimental)

DiPlay can ask the iPhone for CarPlay's second, instrument-cluster screen and show it in the BYD cluster's map area. The iPhone renders this map itself; DiPlay decodes the stream onto the cluster projection display. No root or persistent helper is needed. The optional DiLink 5.1 automatic mode described below needs a one-time permission setup.

Validated on DiLink5.0 / Android12, firmware `BYD-AUTO/DiLink5.0/DiLink5.0:12/SKQ1.230128.001/eng.build.20251111.182747:user/release-keys`, with Apple Maps on iOS 27:

- Turn on "CarPlay map on dashboard" under BYD navigation. The switch appears only when a cluster projection display is visible to the app.
- In the cluster's own steering-wheel menu, choose "Full screen navi" or "Small screen navi". "Turn on by navi" shows arrows only, even during CarPlay navigation.
- The cluster's speed readout stays visible. "Small screen navi" crops the same picture on the cluster side; Android does not report that crop.
- The car marker is placed through CarPlay's safe area: the centre of the panel (x 35–64 %, y 16–75 %), measured with a calibration grid to be clear of BYD's own readouts in both Full and Small screen navi. "Car marker · horizontal" (Left 40 % … Right 40 %) and "Car marker · vertical" (Up 30 % … Down 30 %) move it from there in 10 % steps of the panel. Near a panel edge the safe area shrinks so the marker stays at its centre.
- "Dashboard shows" picks which of the cluster contents the iPhone offers in `altScreenURLs` DiPlay asks for: Map (`maps:/car/instrumentcluster/map`, default), Turn card (`…/instructioncard`) or Map with turn card (`maps:/car/instrumentcluster`). On the tested car the turn card fit the Small screen navi window well and streamed only 0–5 kbit/s against 0.3–4 Mbit/s for the map. The iPhone lays out the map, the car marker and the iOS glass turn card inside the same safe area, so the position settings below move whichever is shown; they are labelled "Car marker" or "Turn card" to match. The glass card cannot be moved on its own: it is painted into the dashboard video, not a separate Android view.
- "Dashboard map size" (or "Turn card size") sets the stream size, which the cluster scales up to the panel: Standard (100 %, sharpest), Larger (83 %, 1600x600, default) or Largest (67 %). Apple Maps ignores the reported physical size on the cluster, so resolution is the only way to change the map's scale.

How it works: the iPhone lists the cluster content it offers in its `/info` request (`altScreenURLs`). DiPlay declares a second display of the cluster's size with no input devices and `initialURL=maps:/car/instrumentcluster/map`; without an initial URL the iPhone streams only a black frame. BYD exposes the cluster projection area as public presentation displays owned by `com.byd.containerservice`. The stock map's display (`fission_bg_XDJAScreenProjection`) is hidden from third-party apps, but its `shared_…_0` sibling is composited on top of it, so DiPlay shows a `Presentation` there.

Limits: the cluster window belongs to the CarPlay screen, so it stops while that screen is closed and the session runs in the background. Other map apps and other firmware are untested.

The optional centre-screen map card mirrors that same dashboard stream. **Keep the home-screen map in sync with the dashboard** (on by default) shows the card. Turn it off to hide the home-screen card so only the instrument cluster shows the dashboard map. This does not change what CarPlay draws on the main screen.

### DiLink 5.1 theme profile

The exact Android 13 firmware `BYD-AUTO/IVI/IVI:13/TP1A.220624.014/eng.build20260722.221155:user/release-keys` has a separate profile for its measured 1920×720 cluster. Other firmware retains the original PR display selection, renderer and settings.

- Map uses shared display `_0`, with a 1920×480 viewport at y=144 and contrast bands behind the instrument readouts.
- Scenario and Simple use shared display `_1`, with a 600×720 side viewport at x=1320. Everything outside the side map is transparent. No white mini-map shading is added.
- The iPhone sends one continuous 1920×720 map. DiPlay crops it at native scale into each viewport, centered horizontally and aligned to the bottom to retain the vehicle marker. Theme changes do not restart CarPlay. The side crop shows less surrounding map area than a separately negotiated portrait stream.
- The two rendering surfaces remain alive when cards close. Window opacity controls visibility, and direct decoder surface handoffs preserve its video reference frames.

Enable **Follow instrument theme and map card** to follow the stock cluster activities. Android Usage Access is required because BYD's theme API is signature-protected. This firmware has no working Usage Access settings page, so the owner must approve one-time ADB setup. For the HUD Test package:

```sh
adb shell appops set com.shihab.diplay.hudtest GET_USAGE_STATS allow
```

Open **Settings → BYD navigation → Automatic map setup · ADB** for the guided setup, available even when automatic mode is off. It shows the command for the installed package, offers a copy button, explains multiple-device ADB selection, and displays the current permission status. After running the command on your computer, tap **Check and enable** to verify permission and enable both the cluster map and automatic following. If permission is still missing, the guide stays open with instructions; an active CarPlay session reconnects once after successful setup. The copy button copies to the car clipboard, so the command is also displayed for typing on your computer. To revoke it, substitute `default` for `allow`. Without access or a recognized active theme, automatic mode hides the overlay. Manual mode remains available but cannot follow card visibility. If no current theme can be inferred after startup, select a different theme once to produce a fresh event.

Only the four stock full-map, mini-map, Scenario and Simple activity events are processed locally. Closing the mini-map card hides the side overlay; Map theme selects the full layer. Merely moving focus to a head-unit app does not hide a still-visible cluster activity. See [Android's UsageStatsManager documentation](https://developer.android.com/reference/android/app/usage/UsageStatsManager) for the permission model.

### Dashboard map only in Small and Full navi (optional, needs ADB)

The iPhone draws and streams the cluster map for the whole session, even while the cluster shows no projection: in Off and "Turn on by navi" the cluster draws arrows only (in "Turn on by navi" the stock map even removes its own cluster window). With "Dashboard map only in Small and Full navi · needs ADB" turned on, DiPlay reads the mode the driver picked on the wheel every second and, while it is Off or "Turn on by navi", sends `stopUI` for the alt screen (`{"type": "stopUI", "params": {"uuid": <alt screen UUID>}}`). When the driver picks Small or Full screen navi it sends `showUI` with the map URL (`{"uuid", "url": "maps:/car/instrumentcluster/map"}`) and `forceKeyFrame` for the same UUID. CarKit handles both as car-initiated commands (`_handleStopUIWithParameters:` / `_handleShowUIWithParameters:`); the stream stays up, so nothing reconnects.

Measured on the car: after `stopUI` the cluster stream carried no frames at all while the main screen went on as usual; after a switch on the wheel `showUI` went out about 0.6 s later and the map was back within a second. If the mode cannot be read (no ADB access), DiPlay keeps the map streaming as without the setting.

Ordinary apps cannot read the mode: BYD's `INSTRUMENT_NAVI_TYPE` needs a BYD signature. The adb shell reads it through the `autoservice` binder (instrument device 1007, feature `0x40C03032`): `service call autoservice 5 i32 1007 i32 1086337074` → `Parcel(00000000 0000000N)`, N = 1 Off, 2 Turn on by navi, 3 Small screen navi, 4 Full screen navi. (The shell can also set it through `INSTRUMENT_NAVI_TYPE_SET`, `0x4C10A018`, with `service call autoservice 6 …`; DiPlay does not change the mode.)

DiPlay runs the read through the head unit's own adbd on `127.0.0.1:5555` ("ADB over network" in developer options) with its own RSA key. The car asks once to allow that key; DiPlay offers the key only from the settings button "Check ADB access", never in the background, so the dialog cannot appear while driving. The TLS pairing flavour of wireless debugging is not supported.

## Car battery for the iPhone (optional, needs ADB)

CarPlay's vehicle status lets the car tell the iPhone its charge and range; Apple Maps then warns about a low charge and offers chargers on the way. With "Car battery for the iPhone · needs ADB" turned on, DiPlay declares an electric vehicle in its iAP2 identification (VehicleInformation with engine type electric and the chosen charging connectors, VehicleStatus with range, range warning, charge and maximum range) and answers the iPhone's StartVehicleStatusUpdates (`0xA100`) with VehicleStatusUpdate (`0xA101`) every 30 s.

DiPlay declares the electric vehicle only when it already has a battery reading as the iPhone identifies the accessory. With ADB off or not approved, or on a car without these properties, the identification stays as without the switch, and the log says `iap2 no battery reading: not declaring an electric vehicle`. The battery is read when DiPlay opens and when CarPlay starts, without blocking the settings page or the iAP2 loop; "Check ADB access" shows whether the battery can be read.

"Charging connectors" picks what the iPhone is told the car can plug into: CCS2 and Type 2 (Europe, the default), GB/T DC and AC (China), or CCS1 and J1772 (North America). Pick the one that matches the car's charging inlet.

The values come from the adb shell (apps need a BYD signature for them), read every 30 s while the iPhone asks. Every sample first runs `getprop ro.car.protocol`, through the same authorized ADB connection. Both the settings check and the background reader select addresses from this property; there is no manual protocol setting or Android-version heuristic. Empty, unreadable or unsupported protocol values produce no battery reading, without falling back to CANFD.

For `CANFD`, the existing addresses are:

- charge: statistic device 1014, `0x4A505038` (`STATISTIC_ELEC_PERCENTAGE`), float percent — `service call autoservice 7 i32 1014 i32 1246777400`;
- range: 1014, `0x4A50203E` (`STATISTIC_ELEC_DRIVING_RANGE` on this platform), km — `service call autoservice 5 i32 1014 i32 1246765118`;
- energy left: power device 1005, `882901008` (`POWER_BATTERY_REMAIN_ELECTRICITY`), float kWh;
- charging: charging device 1009, `876609560` (BMS state, 1 = charging).

For `CAN`, the verified SDK addresses are:

- charge: float percent — `service call autoservice 7 i32 1014 i32 1033543720`;
- range: integer km — `service call autoservice 5 i32 1014 i32 1033203771`;
- charging: BMS state, 1 = charging — `service call autoservice 5 i32 1009 i32 876611608`.

An October 2, 2026 ADB capture from a head unit reporting `CAN` returned 51 %, 36 km and charging state 1. The separate integer SOC interface agreed at 51 %. Remaining energy is not verified on this protocol: the CANFD energy command returned zero. DiPlay therefore does not run it for CAN, keeps energy and capacity unknown, and omits the Wh parameters from vehicle updates while retaining SOC, range and charging state. The contributor has also confirmed testing on their vehicle.

Full charge is estimated only from known energy; full range is scaled up from the current range and SOC. A protocol change discards the previous capacity estimate, and a failed sample clears the cached reading and capacity. Settings and background samples are serialized so an older read cannot overwrite a newer protocol's data. At or below "Low charge warning" (20 % by default) DiPlay sets the range warning. On the CANFD car above DiPlay read 25 %, 150 km and 25.1 kWh, and with the warning threshold at 30 % Apple Maps offered to find a charging station. The suggestion comes from Apple Maps and iOS; Google Maps did not react in testing.

If CarPlay connected before the first battery reading was available, battery reporting stays off for that connection. In BYD navigation settings, **Check ADB access** now caches the reading it displays. **Apply and reconnect** checks and caches a valid reading before reconnecting; if ADB or the battery is unavailable, it keeps the current connection and shows the failure instead. Enabling battery reporting during an active session uses the same check-before-reconnect path. Returning after idle also requests an immediate background refresh rather than waiting for the next 30-second poll. The UI and iAP2 loop never wait for ADB.

## Wheel speed for tunnels (optional, needs ADB)

"Report location to iPhone" (Settings → Location) sends the head unit's position as `$GPGGA` + `$GPRMC` in iAP2 LocationInformation (`0xFFFB`). In a tunnel or car park there is no fix, and the iPhone has only its own motion sensors. "Wheel speed for tunnels · needs ADB" adds the car's speed and gear so the iPhone can keep the position moving:

- DiPlay also sets VehicleSpeedData (id 20) in the LocationInformation identification component. It sends `$PASCD` only if the iPhone selects it (id 4) in StartLocationInformation (`0xFFFA`); the log shows the ids the iPhone asked for (`components=[…]`).
- Every LocationInformation (about once a second) carries the samples since the previous one, even without a GPS fix: `$PASCD,<first sample, s since boot>,C,<P/R/N/D>,0,<n>,<offset s>,<speed m/s>,…*CS`. The layout copies a production head unit's log; what `C` and `0` stand for is not public.
- Speed: device 1013, `-1807745016`, float km/h (BYD SDK speed), read four times a second over adb — `service call autoservice 7 i32 1013 i32 -1807745016`. Gear: device 1011, `555745336`, 1 P, 2 R, 3 N, 4 D, read once a second — `service call autoservice 5 i32 1011 i32 555745336`.

Checked in the car at walking speed: the gear followed D, R and P (4, 2, 1), and the speed arrives in whole km/h. With the setting on, the iPhone asked for vehicle speed (id 4) and DiPlay sent `$PASCD` over both the Bluetooth and the Wi-Fi link. Whether the iPhone uses the speed in a tunnel is still to be tested. Gyro and accelerometer (`$PAGCD`, `$PAACD`) are not sent because their layout is not public.

## Video while parked (optional, needs ADB)

iOS 27 can play video on the CarPlay screen while the car is parked ("video in car"): the iPhone hands the head unit a media URL and drives playback, and the head unit plays it in its own player. With "Video while parked · needs ADB" turned on, DiPlay offers this and plays the video full screen over CarPlay. Video starts on the car screen as soon as it is sent to CarPlay on the iPhone, with no further step in Now Playing. A tap shows **Back to CarPlay**, play/pause, 10 s back and forward and a time bar that can be dragged to seek. The switch is off by default and reconnects CarPlay.

Video is allowed only while the gear reads P. DiPlay reads the gearbox once a second through the adb shell (gearbox device 1011, `service call autoservice 5 i32 1011 i32 555745336` → 1 P, 2 R, 3 N, 4 D) and tells the iPhone with `setVideoPlaybackAllowed`. Leaving P closes the player and the iPhone goes on with audio only; so does a gear that cannot be read (no ADB access). The steering-wheel keys drive the car's player while it is open: play/pause toggles it and next/previous skip 10 s. They do not go to the iPhone, which ends the video session on a CarPlay play/pause.

What the iPhone expects, as observed with iOS 27 and checked against Apple's CarPlay Simulator (Additional Tools for Xcode 27) and its AirPlay web app:

- `/info` carries `videoPlaybackInfo`: `videoPlaybackAllowed`, `featuresEx` (the legacy feature bits plus bits 0 and 64, base64 of the little-endian bit set) and `playbackCapabilities`. SETUP enables `videoPlayback` when the iPhone proposes it; without `videoPlaybackInfo` the iPhone tears the session down.
- The iPhone opens a "CarPlayVideo Settings App" data stream (`BB493F61-…`), encrypted like the iAP tunnel; each `sync` package gets an empty `rply`. Playback runs over remote control sessions without a socket (`A6B27562-…` video setup, `E3DC3EA6-…` overlay UI, `controlType` 1), answered with a stream ID from 3 up. Tearing down one of them must not close the iAP tunnel (stream ID 1).
- Playback messages arrive as `POST /command` with `X-Apple-StreamID` and `{params: {data: bplist}}`: `insertPlayQueueItem`, `setRate`, `seek`, `playbackInfo`, `property`, `setProperty`, `stop`. DiPlay answers `playbackInfo`, `seek` and `property` the way Apple's web app does and sends `playbackState` when the car pauses or resumes. The iPhone sends `insertPlayQueueItem` and `setRate` 1 as soon as the video goes to CarPlay and then asks for `playbackInfo` every second, so DiPlay opens its player on that `setRate`; `requestUI` with `videoplayback:` (Now Playing's video button) opens it too.
- URLs with an app's own scheme are loaded through the iPhone, as Apple's receiver does: an `unhandledURL` request (`FCUP_Response_URL`, `FCUP_Response_IsContentKeyRequest`, `FCUP_Response_RequestID`) is answered with `FCUP_Response_Data`. A resource loader's answer has status 0 rather than an HTTP status. FairPlay keys (`skd`) are not requested.

The player is Media3 ExoPlayer, which parses media in the app. The head unit's own MP4 parser (`libmmparser_lite.so` in `media.extractor`) aborted on progressive Safari video on a DiLink 5.0 Tang.

What plays: video from Safari and from video player apps works in the car, including pause, seeking and the wheel keys. Apple TV sends HLS encrypted with `cbcs` (SAMPLE-AES) and keys for FairPlay, Widevine and PlayReady only; over AirPlay the iPhone brokers just the FairPlay key (`unhandledURL`, `streamingKey`), which needs a licensed FairPlay receiver, so Apple TV+ does not play here. When the car's player cannot play an item, DiPlay tells the iPhone as Apple's receiver does (`{type: error, error: {domain, code}, uuid}`), shows a short note and returns to CarPlay. Netflix does not support AirPlay. In testing YouTube played audio only.
