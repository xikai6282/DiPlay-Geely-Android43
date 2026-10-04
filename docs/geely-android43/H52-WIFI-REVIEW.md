# H52 Wi‑Fi / SoftAP 状态与 DiPlay 连接路径审查

审查日期：2026-10-04
主固件：H52.10500（API 18，实际车辆版本）
对照固件：H52.12000
范围：固件静态反编译与现有 DiPlay 源码只读审查；未连接车机、未运行 ADB、未修改 App 或 APK。

## 结论

H52.10500 的证据不支持“Wi‑Fi 也像蓝牙一样存在 OEM 电源状态与 Android 标准状态分离”的判断。Android station/client Wi‑Fi 和车机 SoftAP 热点是同一 Android Wi‑Fi 服务提供的两种状态。吉利原厂 Settings/KC2 Wi‑Fi 页面分别读取 WifiManager.getWifiState() 与 getWifiApState()；ECarX Wi‑Fi 包也通过 Context.getSystemService("wifi") 取得相同 WifiManager。没有发现所审查的原厂路径改为独立 Wi‑Fi power Binder。

这意味着“普通 Wi‑Fi 显示开启，但 DiPlay 说车机热点未开启”本身不构成状态分裂证据：前者可能是 station/client 状态开启，后者要求 SoftAP AP 状态等于 13。H52 原厂 Settings 明确会在打开热点前关闭 station Wi‑Fi，进一步证明两种状态有独立语义且在此固件上可能互斥。

静态固件证据也不能证明实车此刻的 AP 状态、接口名或地址。如果车机原厂明确显示的是热点开启，而标准 getWifiApState() 不是 13，或 AP 接口不存在，才需要车机侧只读运行日志判断该车当前 system_server、状态机与设置页面是否出现实际不一致。

## H52.10500：标准 Wi‑Fi 服务与原厂调用链

### Android framework 与 Binder 服务

H52.10500 的 framework WifiManager 实现位于：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/wifi_review/framework/android/net/wifi/WifiManager.smali

- getWifiApConfiguration() :1480–1488 调用 IWifiManager.getWifiApConfiguration()。
- getWifiApState() :1547–1563 调用 IWifiManager.getWifiApEnabledState()。
- isWifiApEnabled() :1790–1806 只是判断 getWifiApState() 是否等于 13。
- setWifiApEnabled() :2113–2125 将请求委托给 IWifiManager；方法返回只代表 Binder 调用没有同步异常，不能当作热点已经就绪。
- AP 状态常量：DISABLING=10、DISABLED=11、ENABLING=12、ENABLED=13、FAILED=14；动作是 android.net.wifi.WIFI_AP_STATE_CHANGED_ACTION，额外字段名为 wifi_state（同文件 :85–101、:159–169）。

对应 Binder descriptor 为 android.net.wifi.IWifiManager，定义于：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/wifi_review/framework/android/net/wifi/IWifiManager$Stub.smali

- getWifiApEnabledState transaction 为 0x1e（字段 :61）。
- getWifiApConfiguration transaction 为 0x1f（字段 :59）。
- setWifiApEnabled transaction 为 0x1d（字段 :99）。
- setWifiApConfiguration transaction 为 0x20（字段 :97）。

只读 getter 的 Proxy 实现在 IWifiManager$Stub$Proxy.smali：getWifiApConfiguration() :1264 起，request 只写 interface token，transact 0x1f，reply 先 readException()、再读 Parcelable presence 标志和 WifiConfiguration；getWifiApEnabledState() :1353 起，request 同样只有 token，transact 0x1e，reply 读取 int。无须新增 OEM Binder 或 ANW SDK 才能查询这两个状态。

服务端是 system_server 的 WifiService：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/wifi_review/services/com/android/server/wifi/WifiService.smali

- enforceAccessPermission() :530–545 只检查 android.permission.ACCESS_WIFI_STATE。
- enforceChangePermission() :547–562 只检查 android.permission.CHANGE_WIFI_STATE。
- getWifiApConfiguration() :2407–2422 检查 ACCESS_WIFI_STATE 后调用 WifiStateMachine.syncGetWifiApConfiguration()。
- getWifiApEnabledState() :2424–2439 检查 ACCESS_WIFI_STATE 后调用 WifiStateMachine.syncGetWifiApState()。
- setWifiApConfiguration() :3089–3111 检查 CHANGE_WIFI_STATE 后调用状态机。
- setWifiApEnabled() :3113–3190 检查 CHANGE_WIFI_STATE 后向 WifiController 发送消息；在该方法中未看到签名权限、ADB 授权或额外 TETHER_PRIVILEGED 检查。这是异步控制请求，状态仍应通过 getter/broadcast 确认。

H52.10500 framework-res AndroidManifest.xml :211–213 声明 ACCESS_WIFI_STATE protectionLevel=normal，CHANGE_WIFI_STATE protectionLevel=dangerous。Android API 18 对 dangerous 权限采用安装时授权模型；DiPlay shared manifest :7–8 已声明这两个权限，mobile 模块 minSdk=18。现代 Android 的运行时权限规则或较新系统对热点管理的额外约束，不能直接套到 H52.10500 API 18 服务实现上。

这些结论证明本固件 service 代码允许持有 CHANGE_WIFI_STATE 的普通包请求 setter；它不表示 DiPlay 当前调用了该 setter。DiPlay 当前只通过 OEM Settings 页面让用户开热点，不能把“开启热点需要 ADB-only permission”这条源代码注释当作 H52.10500 API18 的实证结论。该注释在 DiPlayActivity.kt :563，与这台固件的服务端权限实现不符。

### 原厂界面分别读取 station 与 AP 状态

原生 Android Settings 的 WifiApEnabler 位于：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/wifi_review/settings/com/android/settings/wifi/WifiApEnabler.smali

- 构造器 :60–68 从 Context.getSystemService("wifi") 获取 WifiManager。
- :93–104 注册标准 WIFI_AP_STATE_CHANGED 与 TETHER_STATE_CHANGED 广播。
- setSoftapEnabled(Z) :562–615 先读 getWifiState()；准备打开热点且 station state 为 2 或 3 时，先执行 setWifiEnabled(false) (:582–607)，再调用 setWifiApEnabled(null, enable) (:611–615)。
- 热点 UI 的状态更新由标准 AP 状态广播驱动，不是另一个厂商 Wi‑Fi power service。

KC2 原厂设置页面也清楚地区分两项状态：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/wifi_review/kc2setting_wifi/com/neusoft/shockwave/setting/wifi/ConnectHomeFragment.smali

- station UI :482–505 读取 WifiManager.getWifiState()，仅 state 2/3 显示为开启。
- AP UI :547–568 读取 WifiManager.getWifiApState()，其中只把 13 视为 AP 开启。
- 页面刷新分别再次读取这两个 getter：station :840 起、AP :866 起。
- :1050–1067 同时监听标准 WIFI_STATE_CHANGED、WIFI_AP_STATE_CHANGED 等广播；同一监听代码里的厂商 .anw action 是蓝牙状态广播，Wi‑Fi 仍走 Android 标准 action。

因此用户需分清原厂 UI 上的“Wi‑Fi”station 开关与“热点/AP”开关。如果实车只看到 station Wi‑Fi 开启，而 DiPlay 手动热点模式提示 SoftAP 未开启，固件代码行为与该现象相符。

### ECarX 与 KC2 代码不是独立 Wi‑Fi 状态栈

10500 的 ECarX WifiApBean 位于：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/ecarx_adapter_full_smali/com/ecarx/xui/adaptapi/wifiap/specific/WifiApBean.smali

- 构造器 :75–111 通过 getSystemService("wifi") 获取 WifiManager，并调用 getWifiApConfiguration()。
- getWifiApClients() :208–225 调用 WifiManager 的 AP station-list 扩展接口。
- setMaxConnections() :331–368 判断标准 getWifiApState()==13，并通过 setWifiApEnabled()/setWifiApConfiguration() 控制。
- 自定义 com.neusoft.softap.sta_state_change 和 neu.sta.list 是 AP 客户端列表通知线索；它们不是独立 AP 电源状态 getter。

KC2 WifiBean 位于：

F:/CarPlay/_firmware_review/Geely-Bluetooth/H52-10500/wifi_review/kc2setting_wifi/com/neusoft/optimus/wheeljack/wificonnect/specific/WifiBean.smali

- initialize() :117–145 获取标准 WifiManager。
- 收到特定 EventBus 操作事件后，:204–237 读取 AP 状态，必要时 setWifiApEnabled(false) 再 setWifiEnabled(true)；反向事件路径 :239–248 可关闭 station Wi‑Fi。
- :27–68 发出的 0x77a 是应用内部总线操作码。它调度 OEM app 行为，但状态与控制最后仍落到标准 WifiManager。

这段代码证明 OEM 软件有通过标准 WifiManager 处理 radio 模式的事件策略：一个分支会关闭 AP 并开启 station，另一分支会关闭 station。静态代码不能证明某个用户动作或实车当前时刻必定触发了哪一分支。对所检查的 ANW 蓝牙服务材料没有发现对应 Wi‑Fi/SoftAP Binder API；不需要以蓝牙栈的分离推断 Wi‑Fi 也分离。

## DiPlay 当前 Wi‑Fi/AP 流程

源码根目录：F:/CarPlay/DiPlay-0.2.10-android43-luna/DiPlay-0.2.10

### 手动热点模式状态门

CarHotspotStatus.kt :13–28 取得标准 WifiManager，通过反射调用 getWifiApState() 并严格比较 13；若失败则回退反射调用 isWifiApEnabled()；两者都不可用才返回 null。这里检测的是 car SoftAP，不是 isWifiEnabled()/station 状态。

DiPlayActivity.kt 的 carHotspotOff() :563–566 只有在 MANUAL 模式且检测明确为 false 时返回 true。它既影响首页提示，也用于 connect(wireless) :999 的连接前弹窗。connect 的凭据检查 :991–998 发生在前面。null 不拦截。

CarPlayController.kt startWirelessHotspot() :1787–1813 再执行一次同样的 MANUAL AP-off 检查（:1795–1799）；明确 false 会抛 IOException。其他模式不走此 AP state gate。因此两处 gate 都是 MANUAL-only、false-only；null 状态会继续进入后续步骤，随后由配置/接口/网络层决定是否能连接。

当前两个 API 都把 10/11/12/14 映射为 false；因此 disabled、transitioning、failed 在 UI 上被合并成“热点关闭”。如果 AP 正在启停，这可能产生暂时性拦截。更清楚的诊断应保留原始整数状态并分别呈现 disabled、starting/stopping、failed、unavailable，避免只暴露布尔值。

### DiPlay 的模式选项与原厂设置入口

DiPlayActivity.kt :623–660 在该设置页列出 MANUAL（built-in car hotspot）和 WIFI_P2P 两种选项；MANUAL 页面要求用户打开车机热点并保存 SSID/密码。:576–591 的 openCarWifiSettings() 先发起 com.android.settings.WIFI_TETHER_SETTINGS；未解析到页面或启动失败时回退到 Settings.ACTION_WIRELESS_SETTINGS。它只打开设置页面，不调用 AP setter。客户端 Wi‑Fi 设置按钮另走 ACTION_WIFI_SETTINGS（:593–599），再次区分 station 与热点入口。

API18 不能使用当前 DiPlay 的 P2P/LocalOnly 路径：WifiP2pGroupManager.start() 在 API29 以下直接拒绝（WifiP2pGroupManager.kt :75–78）；CarPlayController.kt :1787–1793 会把 API29 以下的 WIFI_P2P 转换为 LOCAL_ONLY_HOTSPOT，而 LocalOnlyHotspotManager.start() 在 API26 以下拒绝（LocalOnlyHotspotManager.kt :54–57）。正常 preference load 会把 API29 以下存储的 WIFI_P2P 和所有 LOCAL_ONLY_HOTSPOT 规范化为 MANUAL（AirPlayPersistence.kt :281–290）；不过设置 UI 仍列出 WIFI_P2P，容易让人误以为 H52.10500 可用。该车上应以 MANUAL 车机 AP 路径为可实施选项，不要将 Wi‑Fi Direct 开关状态混入 AP gate。
### MANUAL 模式的配置匹配与接口选择

ManualHotspotManager.kt：

- start() :74–109 读取 AP 配置；如果配置非 null 且系统 SSID 与用户录入的 expected SSID 不同，则在 :82–86 抛 IOException；配置 null 时继续采用用户录入值。之后从实机接口/扫描信息推断 channel、frequency 与安全类型。
- readLegacyApConfiguration() :337–363 反射调用 getWifiApConfiguration()，读取 SSID 和安全类型，并尝试反射读取 apChannel/apBand。H52.10500 的真实 WifiConfiguration 类没有这两个字段，反射失败分别回退到 channel=0/band=null；它当前也没有读取系统 preSharedKey，因此保存的热点密码不会自动带入。
- findLocalHotspotInterface() :204–233 枚举 NetworkInterface、筛选活动且非 loopback 接口，并选评分最高者。接口评分规则 :247–263 偏向 ap*/softap、p2p、wlan 前缀及私网 IPv4；没有 H52 专属固定接口名。
- API 18 没有后续版本 activeNetwork/getLinkProperties，:210–215 明确只在 API 23+排除 primary interface。故 API18依赖枚举与启发式评分；不能从固件中的 wlan0 客户端配置断言 AP 接口也叫 wlan0。
- H52.10500 /system/etc/dhcpcd/dhcpcd.conf 有 “interface wlan0” 配置条目；静态搜索没有找到 wifi.interface、wifi.tethering.interface 等 AP 接口配置。该条目本身不能确定 AP 接口名或地址，不能据此宣称 AP 地址为 192.168.43.1 或接口为 wlan0。
- AirPlayPersistence.kt :300–319 将 SSID 和 passphrase 保存进 App 私有 SharedPreferences；源代码路径没有加密层。

CarPlayController.kt :923–955 会在热点 manager 返回后检查可用 host address，并记录 backend、接口名、host address、band/channel 等诊断。:1023–1036 让 Bonjour/mDNS 使用同一 AP 接口地址。ManualHotspotManager 具体地址来自运行时 NetworkInterface，而非某个 H52 常量。调试日志中应避免加入密码或其它 Wi‑Fi secret。

### 配置读取、密码保护与 5 GHz AP 能力

普通应用在 H52.10500 可走标准 WifiManager.getWifiApConfiguration() 读 AP 配置：WifiService.getWifiApConfiguration() :2407–2422 只强制 ACCESS_WIFI_STATE（其 protectionLevel=normal，DiPlay 已声明）。方法虽是 API18 hidden API，运行时可用反射调用；Binder descriptor android.net.wifi.IWifiManager，transaction 0x1f（Stub :59；Proxy :1264 起）。

本固件返回的对象链没有清除密码字段：WifiStateMachine.syncGetWifiApConfiguration() :8933–8955 发送内部 CMD_REQUEST_AP_CONFIG=0x2001b；WifiApConfigStore$DefaultState.processMessage() :36–118 对该请求直接回复 CMD_RESPONSE_AP_CONFIG=0x2001c 并携带当前 mWifiApConfig；WifiApConfigStore.loadApConfiguration() :644–681 将保存的配置密码读入 WifiConfiguration.preSharedKey；该字段是 public（WifiConfiguration.smali :92），Parcel writer :1548–1588 会 writeString(preSharedKey)，Creator :43–104 再 readString 并写回 :103。因而 WPA 类热点有明文密码可供普通有 ACCESS_WIFI_STATE 权限的 App 读取，实际值是否可读仍以设备 getter 未抛错且配置非 null 为条件。配置存储路径为 /data/misc/wifi/softap.conf；服务端代码 :384–386 使用 writeUTF 写配置 PSK。本次没有读取该数据文件或任何车机真实凭据，只核对读写代码。

这支持实现“读取车机热点信息”按钮自动填充 SSID、WPA 安全类型及密码；安全类型可从 WifiConfiguration.allowedKeyManagement/getAuthType 判断。密码应默认保持遮罩、不得进入滚动日志/崩溃日志，也不要因读取按钮自动永久复制到 DiPlay 偏好；由用户明确保存手动热点配置后再沿用现有存储流程。

H52.10500 的 WifiConfiguration 实际字段表（ap_capability_classes/android/net/wifi/WifiConfiguration.smali :59–102）含 SSID、preSharedKey、maxNumSta 等，但没有 apBand 或 apChannel；完整反编译类中对这两个字段无定义。WifiApConfigStore 只保存 version、SSID、authType、preSharedKey、maxNumSta（:330–392），不保存热点频段或信道。因此当前 ManualHotspotManager 尝试反射 apBand/apChannel 会在此车失败并采用 band=null/channel=0；“自动读取频段”不能从这份 AP 配置获得。现有 frequencyFromScanResult() 只有扫描结果 SSID 和 BSSID 与所选本机 AP 接口 MAC 都匹配时才返回频率；API18 的 connectionInfo 路径明确回报自动/未知（:268–278），扫描途径不能视为可靠替代。

H52.10500 framework 另有旧 WifiManager 设备频段偏好：WIFI_FREQUENCY_BAND_AUTO=0、5GHZ=1、2GHZ=2（WifiManager.smali :171–175），setFrequencyBand() :2057–2067；WifiStateMachine.setFrequencyBand() :8367–8397 写 Settings.Global["wifi_frequency_band"] 并送内部状态机消息，WifiNative.setBand() :2337–2365 下发 DRIVER SETBAND。这个单独的 radio/driver band 路径不是 WifiConfiguration 的 AP 字段。SoftAP 启动链由 WifiStateMachine$7 调用 INetworkManagementService.startAccessPoint(config, iface)（ap_capability_classes_extra/android/net/wifi/WifiStateMachine$7.smali :47–64）；NetworkManagementService.startAccessPoint() :6680–6778 交给 netd softap set 的配置参数只有接口、SSID、安全类型、密码、最大客户端数，未传 AP band/channel。不能把普通 Wi‑Fi 频段偏好、station 的 5 GHz 支持或资源文件里的 “5 GHz only” 选项当作车机 SoftAP 可选 5 GHz 的证明。

固件中存在 system/bin/hostapd、hostapd_cli 和 vendor/firmware/fw_bcmdhd_apsta.bin；hostapd 二进制字符串可见通用 hw_mode/country_code 配置关键字。这证明系统打包了 SoftAP/某种 Broadcom APSTA 软件路径，但没有证明该车型的驱动、固件、天线/RF设计或当前法规配置支持 5 GHz AP。NMS 传给 netd 的 SoftAP 命令不含频段或国家码。另有 WifiStateMachine 从 Settings.Global["wifi_country_code"] 读国家码后调用 WifiNative.setCountryCode()（WifiStateMachine.smali :6180–6214；WifiNative.smali :2490–2515 将其转成 DRIVER COUNTRY 命令）；build.prop 的 persist.sys.country=CN 是系统 locale 属性，ro.wifi.channels 为空，不等于运行时已验证的 Wi‑Fi regulatory domain。未检查设备运行时 Settings.Global 值、NVRAM 校准/法规表或实际驱动允许信道，故当前结论是“5 GHz AP 未证实”，不是断言硬件不支持。

当前固件默认设置还明确令 station Wi‑Fi 与 AP 互斥：原生 WifiApEnabler 在开 AP 前关 station。配置热更新也可能中断连接：WifiApConfigStore ActiveState 会 defer CMD_SET_AP_CONFIG 直到离开 active state（WifiApConfigStore$ActiveState.smali :124–136）；ECarX 的 setMaxConnections() :331–368 在 AP 已开启时直接先关再按更新后的 WifiConfiguration 重启 AP。不要把切换频段/修改安全参数视为无缝操作。ECarX WifiAp API 只暴露 max connections、客户端列表和回调（WifiAp.smali 方法表）；其实现仅改 WifiConfiguration.maxNumSta，没有 band/channel setter。


### iPhone 加网和 AirPlay 发现不是车机 iPhone hotspot 控制

DiPlay 的无线链路由车机 AP 承载。Iap2WirelessControlClient.kt :121–156 收到 iPhone 的 iAP2 0x5702 REQUEST_ACCESSORY_WIFI_CONFIGURATION 后发送 0x5703 配置；:173–203 在 0x4e0e transport notification 后也会重发配置。accessoryWiFiConfiguration() :242–249 从当前 WirelessHotspotInfo 生成响应。Iap2Endpoint.kt :365–382 对应这两类 endpoint。

接着 DiPlay 在车机所选 AP 地址上启动 AirPlay listener 与 Bonjour/mDNS：CarPlayController.kt :932–955 选 host，:1023–1036 让发现服务使用同一地址/接口；CarPlayBonjour.kt :592 的服务类型为 _airplay._tcp。流程要求 iPhone 获得的 SSID/passphrase 能加入车机 AP，并且 listener 和服务发现可在该 AP 路径上互通。该流程不是打开 iPhone 个人热点，也不是仅靠 station Wi‑Fi 开关便能满足。

MANUAL 模式的主要失配风险是用户输入的密码与车机 AP 密码不同、实际接口被启发式选错、AP 启用但 iPhone 未加入同一网段、或 mDNS/监听没有绑定到真实 AP 路径。状态 13 只证明 framework 报告 AP enabled，不证明 SSID 可加入、接口有地址、iAP2 配置成功或 AirPlay 服务可达。

## API 18 权限与 SDK 边界

- DiPlay shared manifest :3–14 声明 CHANGE_WIFI_STATE、ACCESS_WIFI_STATE、ACCESS_NETWORK_STATE、INTERNET、CHANGE_WIFI_MULTICAST_STATE 和定位权限。
- getWifiApState()、isWifiApEnabled()、getWifiApConfiguration()、setWifiApEnabled() 在 API18 framework 中存在，但属于非公开 SDK 能力；当前状态/配置 getter 用反射访问。不能只因 framework 中方法是 public 就认定它在 Android SDK API surface 可直接编译调用。
- 本 H52 固件 system_server 对只读 AP state/config 实际检查 ACCESS_WIFI_STATE；对写 AP state/config 检查 CHANGE_WIFI_STATE。H52 framework-res manifest 对应 protectionLevel 分别为 normal 与 dangerous。API18 上这两个 manifest 权限可在安装阶段授予；现代 Android 的 runtime permission 模型不应反推此设备。
- 在 H52.10500 上从反射调用隐藏 setWifiApEnabled() 的静态权限路径看，不存在“必须 ADB 才能请求”的 service-side 条件。不过该 setter 会异步改变整车 Wi‑Fi radio mode；DiPlay 现有 MANUAL 流程不调用它，而是将用户带到原厂热点设置页。状态识别与网络可通信验证仍须分开。
- 只读 AP 状态采用标准 WifiManager/API18 Hidden API 的反射方式即可；无需复刻 ECarX、打包厂商 jar、访问 ANW 蓝牙 service，也不应拿 station isWifiEnabled() 代替 AP 状态。

## 对照 H52.12000

以 10500 为主结论；12000 仅作方法级对照。两版反编译源码中以下文件 SHA-256 相同：

| 文件 | SHA‑256 |
|---|---|
| framework WifiManager.smali | EF8B061CF9CFA08C455AD23E8187C07E95AEC695889713CB81968CB2EE29F193 |
| IWifiManager$Stub.smali | 394021DC22CBA6C41A4FCFFE64A498D130A05DB9CF31C81DBFF3FFBC9FB5D931 |
| system_server WifiService.smali | 64D94C5C9F6317090F0127EF3F1CFA49AD85E5C7602B09619937596433D50872 |
| Settings WifiApEnabler.smali | 3590E55062314BF88053775C4503E3AED0AD0A2D6D4037EB7C2C980FE905532D |
| KC2 WifiBean.smali | 340B9C0D7B12A67C426CC30FBCCD81866D1106AF146B6074D0DF167A1BD211FF |
| ECarX WifiApBean.smali | 9128BCC0604DF20BC7F60CEDB9E506DDF0B34A31B9F69CB907E12BFDA79FBA21 |

KC2 ConnectHomeFragment.smali 整文件 hash 不同；不过 12000 对应调用点仍分别以 getWifiState() 和 getWifiApState() 更新 station/AP UI（约 :484/:549、:840/:866），并监听标准 WIFI_STATE_CHANGED 与 WIFI_AP_STATE_CHANGED。故差异不能被简化成“所有 APK 完全相同”，但本报告的核心 Wi‑Fi 服务、权限、Settings 开热点流程及 ECarX/KC2控制路径有同内容源码支撑。两套原始固件与 APK/ODEX 的整体字节当然不是相同版本。

## 可实施的只读诊断建议

1. 在用户显式进入诊断时同时显示 station state（WifiManager.getWifiState()/isWifiEnabled）与 AP raw state（反射 getWifiApState，保留 10–14 原始值）；不可用单独显示 Unknown，不映射成 Off。
2. 同次读取 AP SSID、安全类型及可用时的 preSharedKey；密码只填入遮罩输入框，默认不显示且绝不写入日志。将服务错误、AP 未启用、transition、配置不可读区分开。
3. 枚举活动非 loopback NetworkInterface 的名称、IPv4/IPv6 地址和 interface index；不要在报告里预置 wlan0 或 192.168.43.1。已有 CarPlayController 连接诊断会记录所选 interface/host，可用于与车机侧状态对照。
4. 先用只读 getter 和标准广播观察用户在原厂设置里分别切换 station 与热点时的变化；广播只作变化提示，状态以再次读取 Binder getter 为准。不要为诊断调用 setWifiEnabled()/setWifiApEnabled()。
5. 在用户确认 AP enabled 后再验证 iPhone 加入、iAP2 0x5702/0x5703 配置交换、AP 接口上的 AirPlay listener/mDNS。AP 状态=13 不是无线连接成功证明。

当前静态证据能回答的是“Wi‑Fi API/原厂设置路径是否显示为分离栈”：未发现分离，且有统一 WifiManager 的直接正证据。要判断用户车辆某一瞬间真实运行态，还需在 H52.10500 上采集上述只读 getter/接口信息；本轮没有操作车机，也没有据此推定热点当前状态。
