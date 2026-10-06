# H52 DiPlay repair status — 2026-10-06

This adapted DiPlay 0.2.10 tree studies GKUI compatibility for 2018–2020 Geely Borui, Binyue, Binrui, Jiaji and Xingyue models. This is the target adaptation scope, not a claim that every listed model is compatible. Vehicle verification currently covers one Geely Borui H52 running Android 4.3/API 18 only; other models and other hardware or firmware versions remain untested.

## Confirmed behavior

- **USB video:** On the connected iPhone only, the app uses root to read the active USB configuration and, when it identifies that iPhone, changes configuration 2 to configuration 6 through sysfs. This does not change the head unit's USB-host/ADB operating mode. The repair includes API 18 Lockdown TLS compatibility, asynchronous USB reads with disconnect detection, fullscreen presentation, UUID field byte-order handling, and H.264 hardware decoding. USB CarPlay video and reconnect were seen on the vehicle.
- **Wireless video:** The working route uses the built-in car hotspot. The app reads the actual `hostapd` channel through a bounded, read-only root query and includes the six-byte BSSID in the Wi-Fi configuration message. The user confirmed a successful session: phone TCP connected at 22:09:43 and the first frame arrived at 22:09:47. Earlier attempts with BSSID also waited without connecting, so this does not establish BSSID as the sole cause. The vehicle's 5 GHz hotspot was tested; other head units and 2.4 GHz were not.
- **Audio:** The user confirmed audible CarPlay music on stream 23. A navigation preview on stream 11 was audible, but wireless navigation's Opus stream could not create an Android decoder; its navigation statistics showed zero written frames. P13 stops advertising Opus on SDKs below 21 and retains PCM/AAC. P13 has not been installed or tested in a vehicle.
- **Picture configuration and decoder:** The saved display configuration was restored to 1920×720, 100% scale and 60 FPS; the vehicle configured H.264 at 1920×720 using `OMX.Freescale.std.video_decoder.avc.v3.hw-based`. The user still reports severe lag and poor touch response. Selecting a hardware decoder confirms the decoder path, not low-latency presentation or a smooth user experience.

## Candidate artifacts and boundaries

| Candidate | Change | SHA-256 | Vehicle status |
| --- | --- | --- | --- |
| P12-WifiBssid | Last deployed version; manual hotspot, actual-channel read and BSSID support | `27f1d4e907b5e0acff74653d830049bf21f956a8c4ae9986293a48ff1dbe4f81` | Wireless video confirmed by user; severe lag remains |
| P13-LegacyAudio | P12 plus SDK-gated Opus advertisement for legacy audio | `b5e61566b7dd7cedf6ea81bec808085c55227e448c70ca354a70daef555f6020` | Built and locally checked; not installed, no vehicle verification |
| P14-TouchNoDelay | P13 plus the event-socket TCP_NODELAY change from upstream PR 311 | `be63c739524e5006f8872271f7394cf14008dca7ff1e53c996bc4f0da15b7361` | Built and locally checked; not installed, no vehicle verification |

P13 and P14 source candidates correspond to locally repackaged test APKs. The public source tree does not include those APKs or the locally retained authentication payloads, so a build from this tree is not byte-for-byte the tested APK. The authentication payload retained in local test APKs is not published here.

P14 ports one specific compatible change from [upstream PR 311](https://github.com/shihabal3amri/DiPlay/pull/311); it is not the complete upstream 0.2.13 release. The upstream release targets API 28 and cannot be installed as-is on this API 18 unit.

## Open issues

- **Severe lag and touch response:** Still unresolved. The 44–48 counts in earlier logs are calls that submitted decoded output with `render=true`, not measured display FPS. `touch2frame` measures time until the next frame arrives; it does not measure physical touch-to-display latency. P14's TCP_NODELAY change is a hypothesis to test, not a demonstrated fix.
- **Surface lifecycle:** Surface release/recreation errors have occurred when leaving the projection, backgrounding the app or changing settings. Their relationship to lag during active foreground playback has not been established, and no lifecycle repair is confirmed.
- **Wireless navigation audio:** Opus decoder creation fails on the head unit and P13 has not had an on-vehicle test. Audible navigation from a preview does not verify the wireless Opus path.
- **OEM audio ownership:** `com.ecarx.multimedia` remains stopped following an earlier audio experiment. Persistent Bluetooth/CarPlay focus arbitration and restoration of the OEM app have not been completed. Stopping the OEM app is not a supported final fix.
- **Build and device validation:** The complete Gradle build was not run because the required toolchain was unavailable. P13/P14 validation consisted of local Java/D8 work, DEX assembly and readback, API 18 v1 signature checks, and ZIP alignment checks. Neither candidate received a vehicle or emulator test. An emulator does not establish behavior on this API 18 vehicle.

## Failed or abandoned route

The P11 framework Wi-Fi Direct route failed to start/attach on the head unit. A separate native command experiment did create a group owner and configure its interface/DHCP, but the integrated P11 candidate did not complete a CarPlay connection test; its integration also encountered command-completion problems. The user abandoned this route. It is not a confirmed working connection mode.

## Local authentication setup

The repository's standalone build expects locally supplied MFi identity inputs under `offline-mfi/identity.pk8` and `offline-mfi/certificate.p7b`; see `BUILD.md` and `mobile/build.gradle.kts` for the current build configuration. These files are not included. Provide only authorized local inputs outside version control. The standalone test APKs used locally may contain these authentication resources, which is why those APKs and raw pairing/evidence data are not part of this publication.

## Provenance and validation

This is an adapted source tree based on DiPlay 0.2.10 with H52/API 18 changes. Upstream licensing and notices are retained in `LICENSE`, `docs/licenses/`, `docs/THIRD_PARTY_NOTICES.md`, and the vendored component notices. P13/P14 APK hashes identify local candidates only; publishing this report does not publish or certify those binaries.
