package com.shilapi.xcertplay

import java.util.Locale

/** Suggestions depend on explicit failures, never elapsed time or arbitrary keyword guesses. */
internal object ConnectionRecoveryAdvice {
    data class Advice(val chinese: String, val english: String)
    fun forFailure(message: String, factoryBluetooth: Boolean = false): Advice {
        val error = message.lowercase(Locale.US)
        return when {
            error.contains("h52 anw previous initialization") || error.contains("h52 anw spp initialization failed") -> Advice(
                "原厂 SPP 初始化失败或状态未确认。请通过原厂界面重启蓝牙；若仍不恢复，重启车机后再试。保存报告可进一步核对初始化结果。",
                "Factory SPP initialization failed or is unconfirmed. Restart Bluetooth through the factory UI; if necessary restart the head unit, then retry. Save the report for analysis.")
            error.contains("h52 anw service") || error.contains("h52 anw binder unavailable") -> Advice(
                "请检查吉利原厂蓝牙界面是否正常开启，再重新检测并选择已配对的 iPhone。服务仍不可用时请保存报告。",
                "Check that the Geely factory Bluetooth UI is on, then refresh and select the paired iPhone. Save a report if the service remains unavailable.")
            error.contains("h52 factory bluetooth is not on") -> Advice(
                "请在吉利原厂蓝牙界面开启蓝牙，再回到 DiPlay 重试。",
                "Turn Bluetooth on in the Geely factory UI, then retry in DiPlay.")
            error.contains("bluetooth is not enabled") || error.contains("bluetooth adapter is unavailable") -> Advice(
                if (factoryBluetooth) "当前原厂连接模式未获得可用通路。请检查原厂蓝牙并重新选择 iPhone，保存报告核对实际连接模式。" else "原生 Android 蓝牙通路不可用。H52 车机可在设置开启“尝试 H52 原厂蓝牙连接”，再从原厂配对列表重新选择 iPhone。其他设备请检查系统蓝牙。",
                if (factoryBluetooth) "The selected factory route is unavailable. Check factory Bluetooth, select the iPhone again, and save a report to verify the active route." else "The Android Bluetooth route is unavailable. On H52, enable the experimental factory Bluetooth route and select the iPhone again. On other devices check system Bluetooth.")
            error.contains("h52 anw connect request rejected") || error.contains("h52 anw connection not confirmed") || error.contains("h52 anw spp link disconnected") -> Advice(
                "请确认 iPhone 在附近、蓝牙已开启且原厂配对有效，然后重试。原厂配对成功不代表 SPP 已建立；若仍失败，保存报告，暂不能确认是手机还是厂商通路问题。",
                "Check that the iPhone is nearby, Bluetooth is on and factory pairing is valid, then retry. Pairing does not prove SPP is ready; save a report if it still fails. The cause is not yet confirmed.")
            error.contains("wi-fi p2p is unsupported") || error.contains("wi-fi p2p service unavailable") -> Advice(
                "当前固件未提供可用的 Wi-Fi 直连服务。请切回“车机热点”模式，读取原厂热点信息后连接。",
                "The firmware does not provide usable Wi-Fi Direct. Switch to the built-in car hotspot and read its configuration.")
            error.contains("wi-fi p2p is busy") -> Advice(
                "Wi-Fi 直连正在忙。请结束系统或其他应用的直连会话；车机热点与直连可能争用无线模块，可在原厂设置关闭热点、开启 Wi-Fi 后重试，或使用车机热点模式。",
                "Wi-Fi Direct is busy. End other direct sessions. The car hotspot may share the radio; disable it and enable client Wi-Fi in factory settings, or use car-hotspot mode.")
            error.contains("existing wi-fi direct connection needs a reset") -> Advice(
                "检测到已有 Wi-Fi 直连会话，为避免断开其他应用，本次未接管。请在原厂设置结束该会话，或切回车机热点模式。",
                "An existing Wi-Fi Direct session was not taken over. End it in system settings, or use car-hotspot mode.")
            error.contains("wi-fi p2p creategroup failed") || error.contains("waiting for a usable wi-fi p2p group") || error.contains("wi-fi direct did not respond") -> Advice(
                "系统未确认可用的 Wi-Fi 直连网络。请检查车机 Wi-Fi 已开启；仍失败时切回车机热点并保存日志。建组失败不能认定为 iPhone 密码错误。",
                "The system did not confirm a usable Wi-Fi Direct group. Enable Wi-Fi; if it still fails, use the built-in hotspot and save logs. This does not prove an iPhone password error.")
            error.contains("could not claim the ncm") -> Advice(
                "已识别 iPhone，但 Android 无法占用 USB 网络接口。请退出原厂投屏或其他占用 USB 的应用，重新插拔数据线后重试；供电正常不代表接口可用。保存报告可查看具体内核错误码。",
                "The iPhone was found, but Android could not claim its USB network interface. Exit other USB/projection apps and reconnect the cable. Power alone does not prove interface availability; save the report for the kernel error.")
            error.contains("usb configuration changed or could not be confirmed") -> Advice(
                "USBMUX 打开后未确认相同的 USB 配置，本次没有再次切换配置。请重新插拔 iPhone 并重试，保存报告检查是否发生重枚举或其他应用占用。",
                "The USB configuration was not confirmed after USBMUX opened. Reconnect the iPhone and save the report; configuration was not reset underneath the session.")
            error.contains("the car hotspot is off") -> Advice(
                "请在 DiPlay 热点设置或车机原厂界面开启车机热点，再点击“读取车机热点信息”并重试。WiFi 客户端开启不等于热点开启。",
                "Enable the car hotspot in DiPlay or the factory settings, read its configuration, and retry. Client Wi-Fi being on does not mean the hotspot is on.")
            error.contains("manualhotspotssid is required") || error.contains("manualhotspotpassphrase") -> Advice(
                "车机热点信息尚未完整配置。请在设置点击“读取车机热点信息”，或填写原厂热点名称、密码和安全类型，再重试。",
                "Car hotspot information is incomplete. Read the factory hotspot settings, or enter its SSID, password and security type, then retry.")
            error.contains("manual hotspot ssid does not match") || error.contains("manual hotspot security") || error.contains("manual hotspot is not running on") || error.contains("manual hotspot channel") || error.contains("manual hotspot band") -> Advice(
                "保存的热点信息与车机配置不一致。请点击“读取车机热点信息”重新填入；无法读取时，核对原厂热点名称、密码和安全类型。",
                "The saved hotspot information differs from the car configuration. Read the car hotspot information again, or check the factory SSID, password and security settings manually.")
            error.contains("waiting for the manual hotspot") -> Advice(
                "没有确认可用的热点网络接口。请检查原厂热点实际开启，并让 iPhone 加入该热点；仍失败时保存报告，不能据此认定密码错误。",
                "No usable hotspot network interface was confirmed. Check that the factory hotspot is on and join it from the iPhone. Save a report if it persists; this does not prove the password is wrong.")
            error.contains("securityexception") || error.contains("permission denied") || error.contains("permission denial") -> Advice(
                "接口明确拒绝了权限。请检查应用权限并保存报告；若固件限制该接口，请使用原厂设置完成对应操作。",
                "The interface explicitly denied permission. Check app permissions and save a report; use the factory settings if the firmware restricts the operation.")
            error.contains("wireless carplay control channel closed before tunnel iap2 ready") -> Advice(
                "蓝牙启动阶段结束前，无线控制通道尚未就绪。请检查 iPhone 的 CarPlay 允许提示及是否加入车机热点，再重试并保存报告。",
                "The wireless control tunnel was not ready before Bluetooth bootstrap ended. Check the iPhone CarPlay prompts and its car-hotspot connection, then retry and save a report.")
            else -> Advice(
                "原因尚未确认。请保留最近失败信息，检查最后确认阶段，必要时重试；可返回 DiPlay 保存诊断报告进一步分析。",
                "The cause is not confirmed. Keep the latest failure and last confirmed stage, retry if appropriate, and return to DiPlay to save a diagnostic report.")
        }
    }
}
