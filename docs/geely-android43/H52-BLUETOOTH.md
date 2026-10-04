# H52 Bluetooth status diagnostics

## Scope

This change adds an opt-in, read-only status report for Geely H52 Android 4.3 units. It does **not** implement OEM wireless CarPlay. The existing connection route still uses Android's standard Bluetooth/RFCOMM stack and remains strict when the standard adapter is missing or disabled. OEM pair records are never offered as connectable phones.

The setting is **off by default**. With it off, DiPlay does not bind the ANW service or query ECarX. Turning it on only reads status; it does not change the vehicle Bluetooth switch, start an OEM service, initialize/deinitialize SPP, connect, or write data. The setting and manual check are in Home → Settings → Diagnostics. The manual check is unavailable while the opt-in is off.

## Read-only interfaces

`shared/src/main/java/com/shilapi/xcertplay/compat/GeelyBluetoothDiagnostics.kt` binds explicitly to `com.anwsdk.service.AnwSdkService` with `bindService(..., flags = 0)`. This avoids auto-creating a dormant vendor service. It validates the ANW Binder descriptor `com.anwsdk.service.IAnwPhoneLink` and uses only these getters:

| Transaction | Request and reply | Interpretation |
| --- | --- | --- |
| `0x03` power | Interface token; reply integer `0/1/2/3` | Off / on / turning on / turning off. Other values or invalid replies are unknown. |
| `0x10` paired-list count | Four output capacities `1,16,16,16`; service return code must be `1`; validate the arrays and retain only the count | Names and addresses are discarded. The service's paired-list cache is not evidence that DiPlay can connect to those devices. |
| `0x43` SPP initialization | Interface token; reply integer must be `0` or `1` | Reports the service's internal SPP-initialized flag only; it is not an RFCOMM connection state. |

The ECarX query is separate: `ServiceManager.getService("ecarx_bluetooth_service")`, descriptor `ecarx.bluetooth.IBluetoothManager`, transaction `0x05`, token-only request and strict `0/1` reply. A false result means the manager reports “not enabled or not ready”; it does not prove that the whole vehicle radio is off. Both interfaces report permission errors, missing services, malformed replies, timeouts and unknown states separately from OFF/false.

Binder reads run away from the UI on one bounded worker. Bind/read deadlines, cancellation and disconnects are handled without allowing late callbacks to update a closed screen. A timed-out Binder call may continue until the remote call returns, so its worker slot remains occupied rather than spawning unbounded replacement threads.

## Firmware/version boundary

The reviewed H52-10500 and H52-12000 firmware images are Android 4.3/API 18. Inspection found the ANW `0x03`, `0x10` and `0x43` getter proxy wire formats identical between the two versions. The 12000 interface additionally exposes 14 socket/SPPConnectEx/SDP-related methods absent from 10500; this app deliberately does not use those version-specific methods. The ECarX transaction `0x05` implementation is verified from the 10500 manager interface. This is static protocol evidence, not proof that an ordinary app can access either Binder on a vehicle; SELinux/service runtime access still needs H52.10500 testing.

## UI, privacy and API 18

`common/src/main/java/com/shilapi/xcertplay/GeelyBluetoothDiagnosticsOptIn.kt` gates OEM queries, `GeelyBluetoothDiagnosticSnapshotStore.kt` caches coarse states/count/time only, and `AndroidBluetoothFailureCopy.kt` localizes the controller's existing strict Android transport failures. The Home dialog keeps Android adapter, ANW, and ECarX states separate. ANW ON with Android Bluetooth unavailable is explained as “vendor Bluetooth reports on; DiPlay's Android connection path is unavailable,” not as a working wireless connection. Unknown service state is never shown as off.

Device names and MAC addresses are not stored or included in the report. When diagnostics are disabled, a prior snapshot is marked historical. On Android 4.3 the H52 flow offers copying a sanitized Bluetooth summary to the clipboard. This is **not** full diagnostic file export; generic full-file export remains unavailable on API 18.

The API 18 debug probe's `geely_bluetooth` mode checks both protocol parsers using Android Binder/Parcel mocks and verifies that absent OEM services remain unknown. Mock success verifies the request/reply contract only; it does not establish vehicle Binder access, SPP connectivity or wireless CarPlay support. No firmware source, decompiled code, OEM binary, vehicle identity, private key, device name or MAC address is included in this document or implementation.

## Subsequent API18 export fix

The subsequent root-report build implements full diagnostic file export directly to the user-storage root without a file picker; see API18-REPORT-EXPORT.md. The earlier build limitation above remains historical.
