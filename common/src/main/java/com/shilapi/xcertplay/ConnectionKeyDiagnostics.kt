package com.shilapi.xcertplay

/** Retains exact diagnostic fields, never infers successful connection from text. */
internal class ConnectionKeyDiagnostics {
    private var request: String? = null
    private var observation: String? = null
    private var usb: String? = null
    fun update(line: String) {
        Regex("H52 ANW connect request result=(-?\\d+) index=(-?\\d+)").find(line)?.let {
            request = "ANW 请求：result=${it.groupValues[1]}，index=${it.groupValues[2]}"
            observation = null
        }
        Regex("H52 ANW connect observation index=(-?\\d+) result=(-?\\d+) state=(-?\\d+) addressPresent=(true|false) addressMatches=(true|false)").find(line)?.let {
            observation = "ANW 状态：index=${it.groupValues[1]}，result=${it.groupValues[2]}，state=${it.groupValues[3]}，目标匹配=${it.groupValues[5]}"
        }
        Regex("USBDEVFS_CLAIMINTERFACE iface=(\\d+) result=(-?\\d+) errno=(\\d+)").find(line)?.let {
            usb = "USB 接口 ${it.groupValues[1]}：result=${it.groupValues[2]}，errno=${it.groupValues[3]}"
        }
    }
    fun summary(): String = listOfNotNull(request, observation, usb).joinToString("\n")
}
