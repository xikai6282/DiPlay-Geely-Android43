# 吉利 Android 4.3 代码级适配说明

## 基线和边界

源码基于 DiPlay 0.2.10，而非 0.2.9。mobile/common/shared 最低 SDK 为 18，mobile 启用 legacy multidex；compileSdk 保留现代构建能力。新 API 的使用以 SDK 分支和隔离类控制，不能只下调 minSdk 或大面积 suppress NewApi。上游现代 automotive/Compose 分支仍保留，本文的运行验收针对 mobile。

完整逐文件方法和行号见 SOURCE-INDEX.md，精确增删见 UPSTREAM-TO-API18.patch。以下说明每个兼容边界的目的及运行行为；代码变动的对应关系同时保留在 MIGRATION-REVIEW.md。

## 连接流水线

HostActivity 读取连接模式和保存设置 → 启动 SessionService 和控制器 → 手动车机热点/能力允许的 P2P，或 USB 发现 → iAP/USBMUX/Lockdown 与认证 → 发布实际 AirPlay 监听端口 → 音视频解码、麦克风 RTP 和媒体状态。切换传输前先关闭当前控制器，持久化目标设置，再 recreate；避免 onResume 重复启动旧模式。

### Java 与框架兼容

- Base64Compat：替代 API26 java.util.Base64；保留基本编解码及 MIME 64列输出，旧系统不能在加载阶段引用现代实现。
- CharsetsCompat：替代 API19 StandardCharsets。时间使用 Calendar 等旧 API 替代 Instant；集合使用兼容 ConcurrentHashMap 和迭代/CAS 循环替代新方法。
- ContextCompat：旧系统权限检查走旧 API；服务启动延续旧已验证策略，API29+ 走 startForegroundService，旧系统 startService，再由服务调用旧版 startForeground。
- PendingIntentCompat、NetworkInterfaceCompat：隔离新 flags/接口 API，不改变上游权限或协议含义。

### USB 与 VPN

- LegacyUsbTransfer：API26以下使用有超时的同步 bulkTransfer；API28以下写入不超过16KB的分块。短写推进实际偏移，并共享 elapsedRealtime 截止时间，防止各块重新计时导致无限阻塞。IphoneUsbHost 和 NcmUsbBridge 同时接入。
- UsbDeviceLayout / ConfigurationDescriptorScanner / UsbCompat：从原始配置描述符识别配置和 NCM alt setting；通过 GET_CONFIGURATION 确认选中值，不默认 configuration=1。端点号用无符号值，拒绝无效配置。
- 上游 UsbMuxFrameBuffer 的长度检查、控制帧边界恢复和填充处理继续保留。模拟器中的32KB短写测试为回调引擎验证，不能替代真实USB硬件传输。
- VpnTunnelCompat：setBlocking 仅新版调用；旧 TUN 文件描述符读写对 EAGAIN 做退避，处理完整包边界和关闭，避免空闲时忙循环。

### 监听和发现

- WildcardBind：统一旧系统 IPv6 wildcard 绑定；Audio/Screen/NTP/iAP/麦克风/AirPlay 的生产监听路径共用兼容处理。
- Bonjour：API21以下使用接口级 mDNS；启动和工作线程必须采用同一有效分支。发现服务不能仅以 serviceName 去重，地址、端口、Bluetooth ID 和 TXT 更新必须触发有效更新。
- vendored JmDNS 3.6.3：socket 先 unbound，再 reuse，再 bind，以兼容系统 mdnsd；源码 Java API 做 API18 回退，保留原许可证。
- AirPlay 动态端口由实际监听结果生成并发布，不能退回固定端口掩盖冲突。H41 原厂服务是否占用端口或 USB 网卡须在实车检查。

### 音视频和媒体按键

- CodecCompat：使用旧静态枚举；API18 MediaCodec 通过 inputBuffers/outputBuffers 访问缓冲区，新版才用单缓冲区 API。Surface 重建时清理并重新附着。
- AudioTrack/AudioRecord：旧构造器及读写接口；ModernAudio 和 PortableAudio 的新类型在 SDK 分支后才加载。
- 旧 RCC 接收器处理 PLAY/PAUSE/NEXT，注册/播放/暂停/释放均可在 dumpsys audio 核实；模拟器按键成功不代表已发往真实 iPhone。
- Media3 需要 API23；API18 上播放器组件禁用，CarPlayVideo.show 拒绝请求，桥接对象不暴露 Media3 类型。上游 URL/redirect 校验保留。
- TLS 的 endpoint identification 参数只在支持的版本调用。模拟器证书与 ClientHello 验证不等于真实 iPhone 信任建立。

### UI、服务和可选功能

- 设置页 ImageView/Switch/SeekBar/Radio tint、showText、Ripple 与 theme 属性按实际 API 边界处理；API18 使用可用的 Holo 基础主题。
- RoundedClipCompat 隔离 API21 ViewOutlineProvider；API18 悬浮窗使用旧 window type，canDrawOverlays 只在API23+调用。
- MapEmbedService 在旧系统先返回 unsupported，不能读取较新 Message.sendingUid。旧 FrameLayout.LayoutParams 显式复制尺寸、gravity 和 margin。
- HomeScreenMonitor / DiLink51ClusterMonitor 将 UsageStats/AppOps 新类型隔离，API18返回不支持且不创建轮询线程。
- 服务的通知渠道、前台服务类型等只在对应 API 上使用。释放后核实 ServiceRecord 和媒体注册消失。

### 中文显示层

HotspotUiLabels 将内部 Wi-Fi P2P / LocalOnlyHotspot / Manual hotspot 标识映射到资源，内部传输标识和诊断保持原值。中文修正涵盖驾驶位位置、声道映射、使用情况访问权限、热点名称及状态等73项。WifiP2pGroupManager 调用 createGroup 并检查 isGroupOwner，因此该模式由车机建网，不能翻成苹果手机热点。实际无线支持仍需车机硬件测试。

## H41 原厂研究可借鉴的部分

静态固件呈现 API18 / ARMv7 / imx6、OEM Binder/JNI、系统 UID、USB NCM 网卡和 Audio HAL 路径。生命周期可借鉴 NONE → ATTACHED → CONNECTED → OPENED → AUTHED 的分阶段处理；旧 MediaCodec 缓冲区和 Surface 路径与本适配方向一致。

其系统服务、私有 net management、认证 IC 和 native 库依赖 OEM 权限/硬件，不能直接作为普通 APK 模块复制使用。系统属性中的品牌/型号也与用户称谓不完全一致，不能仅凭文件名确认所有吉利车型兼容。此次没有导入原厂私有实现。完整本地研究报告位于桌面交付 docs/H41-CarPlay-analysis.md，不作为公开源码分发。

## 已知限制

真实 iPhone 信任、无线握手、USB/NCM、ARMv7 native 装载、车机音频HAL与原厂服务冲突、重启自启等仍待实车验收。545项单测和模拟器验证支持兼容实现，不代表这些硬件链路已经通过。shared/common lint 仍有已记录的非NewApi问题，见 VALIDATION.md。
