# H52 热点工具与宽屏连接诊断

## 功能与证据边界

H52.105 原厂热点与客户端 WiFi 共用 WifiManager 服务，但 AP 开启状态和客户端 WiFi 开启状态不同。热点配置读取与热点启动必须独立于蓝牙接入开关。读取按钮把原厂 AP 配置填入现有设置，不修改原厂配置；启动按钮由用户确认后执行，仅复用原厂配置，不改名、密码或厂商附加字段。

5 GHz AP 暂未找到经过验证的控制接口：105 的 WifiConfiguration 没有 apBand/apChannel，标准 AP 配置到 netd 的链路没有频段参数。客户端的频段偏好不是 AP 频段控制。软件不能把自动频段或客户端 5 GHz 偏好宣称为已开启 5 GHz 热点。

## 真实连接阶段

shared/orchestration/CarPlayController.kt 的 CarPlayStatus 新增 BluetoothReady、WirelessIdentified、WirelessAuthenticated、WaitingForWifiJoin。BluetoothReady 只在蓝牙字节流成功返回后报告；它不代表 iAP2 握手成功。

shared/transport/Iap2WirelessControlClient.kt 的 run 新增可选 typed onStage 回调。Identification identify 返回后才报告 IDENTIFIED；MFI run 返回后才报告 AUTHENTICATED；0x5703 的 send 成功返回后才报告 WIFI_CONFIG_SENT 或 POST_TRANSPORT_WIFI_CONFIG_SENT。Controller 将这些已发生事件投递为 UI 阶段，避免从日志关键词猜测。

WaitingForWifiJoin 仅表示热点配置已通过 iAP2 发送，不能证明 iPhone 已入网或 AirPlay 已连通。超过等待时间的建议只能称原因尚未确认，不能把计时器当故障检测。

## 界面约束

宽屏左侧状态/建议/操作，右侧带时间的脱敏日志；按有效 dp 和宽高比选择布局，不硬编码物理像素。窄屏保持可用。视频开始后连接层隐藏、CarPlay 全屏。当前阶段与最近失败均保留，避免自动重试覆盖故障证据。日志有界、合并刷新，用户上翻暂停跟随，回最新恢复。

密码和认证材料不进入运行日志/导出诊断；源码不包含原始认证材料，完整测试 APK 保留已授权的本地认证材料。

## 验证

实现已完成，最终 APK 哈希及测试范围如下。模拟器可以验证 API18 调用失败处理、界面布局和状态呈现；不能验证 H52 AP 硬件或实际 iPhone 握手。

## 热点工具实现与边界

`shared/src/main/java/com/shilapi/xcertplay/network/CarHotspotTools.kt` 提供 `read/readState/start`，反射访问隐藏 WifiManager API。`configurationFrom` 保留 AP SSID/PSK原始字符（包括首尾引号），支持 NONE/WPA_PSK/WPA2_PSK，拒绝未验证的安全配置；AP band缺字段/未知值返回null；保留原始WifiConfiguration对象再传setter，避免损失maxNumSta等OEM字段。

`start` 已开启或启动中时不再发送setter；配置/状态不可读取时拒绝修改；只请求一次开启，accepted之后继续轮询，不把请求接受等同热点就绪。取消谓词在setter前检查，超时/离开界面后不会从迟到读取继续发起新开启请求；已经执行的远端setter无法撤回，界面提示结果需检查。UI通过单个全局worker/busy门串行，超时不释放busy直到worker真正退出，weak reference和generation过滤过期结果。

`common/.../DiPlayActivity.kt` 在车机热点模式提供读取/开启按钮，开启前显示WiFi互斥警告，读取成功通过 `AirPlayPersistence.saveManualHotspotProfile` 同一次prefs更新保存完整配置，清除pending setup并刷新界面。Android API18不展示DiPlay不能执行的P2P模式。密码默认遮罩，开启按钮保持当前车机配置，不修改频段。

`common/.../ConnectionRecoveryAdvice.kt` 对明确的已知错误提供固定处理建议，不回显SSID/PSK。未知socket超时保持原因尚未确认，不归因密码或蓝牙。Host配置构造的IllegalArgumentException在连接页显示失败和设置建议，不再让缺SSID直接崩溃，也不自动循环重复无效配置。

`mobile/src/debug/.../Api18CompatProbeActivity.kt` 的 `hotspot_read` 是只读运行时探针，不调用setter、不输出SSID/密码，分别记录配置可读与AP状态。模拟器的读取或开启结果不能替代H52实车验证。

## 原始参考

AOSP WifiConfiguration 的旧热点自动频段值是AP_BAND_ANY=-1，详见 [AOSP源文件](https://android.googlesource.com/platform/frameworks/base/+/dcbed5f/wifi/java/android/net/wifi/WifiConfiguration.java)。H52.105本身没有此AP频段字段，不能把这一常量当作H52可控制5 GHz的证据。

## 方法级源码索引

### common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt

| 方法 | 行号 |
|---|---:|
| `buildWideConnectionPanel` | 1105 |
| `createStatusReporter` | 3540 |
| `startCarPlay` | 3617 |
| `renderConnectionStatus` | 4154 |
| `failureText` | 4200 |
| `guidanceFor` | 4258 |
| `updateDebugOverlays` | 4368 |
| `isWideConnectionSurface` | 4378 |
| `appendLog` | 4386 |
| `enqueueUiLogLine` | 4408 |
| `refreshLogView` | 4438 |
| `scrollLogsToBottom` | 4455 |

### common/src/main/java/com/shilapi/xcertplay/DiPlayActivity.kt

| 方法 | 行号 |
|---|---:|
| `confirmEnableCarHotspot` | 615 |
| `submitCarHotspotAction` | 626 |
| `completeCarHotspotAction` | 700 |
| `persistCarHotspotConfiguration` | 738 |
| `exportDiagnostics` | 1687 |

### common/src/main/java/com/shilapi/xcertplay/AirPlayPersistence.kt

| 方法 | 行号 |
|---|---:|
| `saveManualHotspotProfile` | 363 |

### common/src/main/java/com/shilapi/xcertplay/ConnectionRecoveryAdvice.kt

| 方法 | 行号 |
|---|---:|
| `forFailure` | 8 |

### shared/src/main/java/com/shilapi/xcertplay/network/CarHotspotTools.kt

| 方法 | 行号 |
|---|---:|
| `read` | 30 |
| `readState` | 36 |
| `start` | 40 |
| `read` | 50 |
| `readState` | 60 |
| `configurationFrom` | 62 |
| `start` | 111 |
| `awaitEnabled` | 174 |
| `readState` | 288 |
| `requestEnable` | 289 |
| `readState` | 302 |
| `requestEnable` | 312 |

### shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt

| 方法 | 行号 |
|---|---:|
| `startWireless` | 885 |
| `startWirelessHotspot` | 1806 |
| `debugLogMessage` | 2247 |

### shared/src/main/java/com/shilapi/xcertplay/transport/Iap2WirelessControlClient.kt

| 方法 | 行号 |
|---|---:|
| `run` | 23 |

### mobile/src/debug/java/com/shilapi/xcertplay/compat/Api18CompatProbeActivity.kt

| 方法 | 行号 |
|---|---:|
| `runProbes` | 86 |


## 最终验收与交付

shared453 + common152 = 605 项单元测试通过，0失败/错误/跳过；最后仅调整常见错误标题的中文显示，随后重新编译最终APK并完成API18界面回归。实际安卓4.3模拟器使用1920×720/density160，另测试480×800窄屏回退。

最终APK回归覆盖：宽屏分栏/时间戳日志/阶段建议、热点名称缺失不崩溃且提示读取配置、热点配置实际读取成功且未知频段/信道不伪报、开启确认与取消、开启失败处理、日志上翻暂停/新日志不强制滚动/回最新恢复、手动重试和自动重试期间保留失败建议。

实际API18只读探针读取配置成功（WPA2），band/channel不可获得；热点开启在模拟器上报告失败，不是H52硬件开启验证。ANW Binder/Parcel/回调字节流mock探针通过，不能证明真实H52/iPhone无线握手。主视频到达后全屏的分支已代码审查，本轮没有真实CarPlay视频会话验证。

Windows内存不足曾导致测试JVM失败；关闭测试模拟器并以单worker、Gradle512MB/测试256MB SerialGC重新执行后通过。构建弃用提示仍存在，未宣称全量lint通过。

APK：`DiPlay-0.2.10-Geely-Android43-H52-WiFiLog-test.apk`，18,307,246 bytes。SHA256：`5073e0b906b8811b1ae5bf8f364fb5cea29148f726cce558a866abdaa1540c26`。认证私钥/证书已确认随APK保留；源码不含原始凭据。

实车使用：在原厂界面配对iPhone；DiPlay连接设置开启H52原厂蓝牙尝试开关并选原厂配对手机；点击读取车机热点信息；热点未开启时点击开启车机热点并阅读确认提示（失败则用原厂界面开启）；启动无线连接。连接页左侧按实际阶段和建议操作，右侧可上翻查看日志，回到最新恢复跟随。返回DiPlay的设置可保存完整报告到用户内存根目录 `/sdcard/DiPlay-诊断报告-日期时间.txt`。
