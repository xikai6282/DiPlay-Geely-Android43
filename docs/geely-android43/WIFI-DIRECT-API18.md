# Android 4.3 Wi-Fi Direct 适配（2026-10-05）

此更新基于 DiPlay v0.2.10 吉利 Android 4.3 分支，保留原项目 GPL-3.0 与来源说明。恢复第二种无线模式并接入旧版 Wi-Fi Direct 建组路径。它由车机创建网络供 iPhone 加入，不是连接 iPhone 个人热点。

## 代码路径

`common/.../DiPlayActivity.kt::wirelessLinkControls`：API18 起显示 MANUAL 与 WIFI_P2P。旧平台显示系统生成名称/密码、自动频段以及无线模块争用提示；选择 P2P 不会修改原厂 AP 配置。`AirPlayPersistence.loadWirelessHotspotMode` 保留 API18 的 WIFI_P2P 选择，默认安装仍为 MANUAL；旧 LOCAL_ONLY_HOTSPOT 配置仍迁移至 MANUAL。

`shared/.../CarPlayController.kt::startWirelessHotspot`：尊重所选模式，不再把旧平台 P2P 自动转换至 API26 的 LocalOnlyHotspotManager。WIFI_P2P 使用 WifiP2pGroupManager，MANUAL 使用现有读取原厂热点路径。20 秒启动上限保持不变。

`shared/.../network/WifiP2pGroupManager.kt::start`：API29+ 继续使用原有指定频率与回退计划。API18–28 使用 `createGroup(Channel, ActionListener)`，不创建 API29 的 WifiP2pConfig.Builder、不调用 requestP2pState、不请求固定频道。前提是客户端 Wi-Fi 已开启；不会自行关闭原厂热点或切换客户端 Wi-Fi。

成功回调仅说明创建请求完成，随后必须等待 requestGroupInfo 返回 owner=true、非空 networkName/passphrase/interface，并等待有效接口地址或 groupOwnerAddress。组名、密码必须来自当前组，不能使用原厂 AP 的密码代替 P2P 密码。API29 前不调用 getFrequency，返回 frequencyMHz=null、channel=0、bandLabel=Auto (firmware selected)。频道0沿用现有 iAP2 自动/未知频道表示，不能冒充已读取到的频道；iPhone 是否接受仍需要实车验证。

`checkPrerequisites`：API18–22 检查 CHANGE_WIFI_STATE，23–32 检查精确定位权限，33+ 检查附近设备权限。日志记录状态，不包含热点密码。缺少服务、Wi-Fi 关闭、BUSY、P2P_UNSUPPORTED、未完整返回组信息都作为失败处理。

`close/cleanupFailedStart/removeGroup`：只删除拥有精确当前组名且 isGroupOwner 的会话；启动前已有未知会话会拒绝接管。已有己方组仅按原有所有权记录判断后回收。旧系统可能保留 persistent profile；本实现移除活动组，不调用隐藏的删除持久配置接口。超时不自动发第二个 createGroup 请求，避免迟到回调重复建组。

`common/.../ConnectionRecoveryAdvice.kt`：明确 unsupported 时提示使用车机热点；BUSY 时提示结束已有会话并检查原厂热点与客户端 Wi-Fi 争用；未知错误不推断为手机密码问题。

## 平台证据与限制

Android 官方文档表明两参数 createGroup、getNetworkName、getPassphrase、getInterface 自 API14 起提供，getFrequency 从 API29 起提供：
- https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager
- https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pGroup

官方平台 API 存在不代表 H52 驱动一定支持建立 GO 网络。H52 固件证据与测试结果见后续段落。5 GHz 强制控制未实现；旧接口选择频段由固件决定。

## 使用

在连接设置选择“Wi-Fi 直连”，通过原厂设置开启客户端 Wi-Fi。若直连报告 BUSY，可通过原厂界面结束其他直连会话，或关闭原厂车机热点后重试。保留“尝试 H52 原厂蓝牙连接”开关，选择已配对 iPhone，保持手机蓝牙/Wi-Fi 开启并允许 CarPlay。连接页右侧日志显示系统建组、蓝牙与 iAP2 的各阶段；失败提示可引导切回“车机热点”。此模式无需手填 P2P SSID 和密码。

## 验证

API28 Robolectric 测试覆盖旧重载调用、读取系统生成凭据、未知频率不伪造频道、确认 CarPlay 后不记忆虚假频率、关闭删除本次组、拒绝未知组、缺失密码时超时并清理、创建超时不重复请求。API18 模拟器检查实际 API 链接和界面/模式保存；模拟器无有效 Wi-Fi radio，不能验证真实建组或 iPhone 加入。具体结果与 APK SHA256 在交付清单中。

## H52.10500 原固件实证

原始路径 `E:/BaiduNetdiskDownload/kc_update_01.03.10500.H52.00030/system/framework`，build.prop 确认 `01.03.10500.H52.00030` / SDK18；与先前 H41 固件分开。Luna 从完整 odex 提取后核实：

- WifiP2pManager 的 public 两参数 createGroup 发 CREATE_GROUP (0x2200d)、arg1=-2；-2 是 PERSISTENT_NET_ID。服务会查找已有 persistent group，找不到则调用 WifiNative.p2pGroupAdd(true)，发送 `P2P_GROUP_ADD persistent`。因此本模式不能承诺每次生成新的密码，也不能承诺不留持久配置。
- WifiP2pGroup 的 getPassphrase/getInterface 为 public；requestGroupInfo 可返回组对象。Group 无 frequency 字段和 getter；即使解析到 supplicant freq 字段也不对应用暴露，本实现报告未知频道。
- WifiP2pService.getMessenger 检查 ACCESS_WIFI_STATE 和 CHANGE_WIFI_STATE，当前 APK 均有声明；不需要因此把普通安装包改成系统签名包。权限存在不代表无线硬件已成功执行建组。
- removeGroup 只拆活动会话；隐藏的 deletePersistentGroup 是独立操作。本实现不调用它，避免删掉车机其他应用共用的持久记录。

完整反汇编证据保留本地研究目录，不把原厂固件字节码作为 DiPlay 开源代码发布。

原固件静态证据详见 [H52-P2P-FIRMWARE-REVIEW.md](H52-P2P-FIRMWARE-REVIEW.md)，未包含原厂二进制。

## 源码定位

| 文件 | 入口行 | 用途 |
|---|---:|---|
| `common/src/main/java/com/shilapi/xcertplay/DiPlayActivity.kt` | 838 | 模式选项与旧系统帮助 |
| `common/src/main/java/com/shilapi/xcertplay/AirPlayPersistence.kt` | 281 | 保留已选择的直连模式 |
| `shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt` | 1806 | 按所选模式启动 |
| `shared/src/main/java/com/shilapi/xcertplay/network/WifiP2pGroupManager.kt` | 75 | 旧/新建组路径 |
| `shared/src/main/java/com/shilapi/xcertplay/network/WifiP2pGroupManager.kt` | 346 | 读取凭据和网络地址 |
| `shared/src/main/java/com/shilapi/xcertplay/network/WifiP2pGroupManager.kt` | 534 | 按平台检查权限 |
| `shared/src/main/java/com/shilapi/xcertplay/network/WifiP2pGroupManager.kt` | 663 | 精确组身份清理 |
| `common/src/main/java/com/shilapi/xcertplay/ConnectionRecoveryAdvice.kt` | 8 | 失败处理建议 |
| `mobile/src/debug/java/com/shilapi/xcertplay/compat/Api18CompatProbeActivity.kt` | 88 | API18 调试探针 |

最终 APK：`DiPlay-0.2.10-Geely-Android43-H52-WiFiDirect-test.apk`，SHA256 `d24f1e483a7ec813c94c5751be02bbf62f6160727b1d083a97c28f76b0f37713`。单元测试 shared=453、common=158，共611项，失败/错误/跳过均0。最终 APK API18 双模式/重启保存/接口链接/无崩溃检查通过；真实无线模块建组未测试。
