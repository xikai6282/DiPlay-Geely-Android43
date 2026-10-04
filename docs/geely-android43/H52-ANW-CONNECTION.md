# H52.105 厂商蓝牙连接通路

## 开关和入口

设置项名称：**尝试 H52 原厂蓝牙连接**（资源 `geely_bt_connection_title`）。SharedPreferences 键为 `geely_h52_bluetooth_connection_enabled`，默认关闭；它与“厂商蓝牙状态检测”的只读开关相互独立。只读检测不会启用连接功能。

开启后，`DiPlayActivity.choosePhone()` 绕过 Android `BluetoothAdapter` 状态和 `BLUETOOTH_CONNECT` 权限检查，在后台调用 `AnwBluetoothBackend(applicationContext).pairedDevices()`。选择器只列出车机原厂蓝牙界面已有的配对记录；DiPlay 不会自动配对，也不会切换车机蓝牙电源。所选地址和“此手机来自 H52 配对列表”的来源标记保存在 `DiPlayPreferences`。切换 H52 / Android 通路后，旧选择不再匹配当前通路，下一次无线连接会要求重新选择。

`CarPlayHostActivity` 从独立偏好读取 `geelyBluetoothEnabled` 并传入 `CarPlayController`。Host 恢复前台时若发现无线 H52 开关变化，会先有序关闭现有 Controller，再重建会话。开关关闭时，Controller 继续走现有 Android Bluetooth/RFCOMM 路径。

相关代码：[`DiPlayActivity.kt`](../../common/src/main/java/com/shilapi/xcertplay/DiPlayActivity.kt)、[`DiPlayBootstrap.kt`](../../common/src/main/java/com/shilapi/xcertplay/DiPlayBootstrap.kt)、[`AirPlayPersistence.kt`](../../common/src/main/java/com/shilapi/xcertplay/AirPlayPersistence.kt)、[`CarPlayHostActivity.kt`](../../common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt)、[`CarPlayController.kt`](../../shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt)。

## Binder 和连接流程

[`AnwBluetoothBackend.kt`](../../shared/src/main/java/com/shilapi/xcertplay/transport/AnwBluetoothBackend.kt) 只绑定已运行的 `com.anwsdk.service/.AnwSdkService`，使用 flags `0`，不会用 `BIND_AUTO_CREATE` 启动服务。`Binding.await()` 最多等待 4 秒。[`AnwSppProtocol.kt`](../../shared/src/main/java/com/shilapi/xcertplay/transport/AnwSppProtocol.kt) 将同步 RPC 放进全局固定 2 线程、无排队容量的 worker 池；每次 Binder transact 最多等待 3 秒。超时任务会被取消；若底层 Binder 忽略中断，原 worker 仍占用槽位，不会不断创建替代线程，槽位耗尽时新 RPC 会快速失败。

实现的只读查询包括电源状态 `0x03`、本机地址 `0x07`、配对列表 `0x10`、SPP 初始化状态 `0x43`。配对请求按固件 Proxy 约定写四个容量值 `1,16,16,16`，并校验返回数量和地址格式。本机 MAC 必须是有效的六字节冒号分隔地址且不能全零，之后用于 CarPlay AirPlay 配置的 `btMac`。

若厂商 SPP 尚未初始化，连接开关授权 DiPlay 通过 `0x3e` 注册本机通用 SPP 服务 UUID `00001101-0000-1000-8000-00805f9b34fb`。这是明确的服务初始化副作用，但不会写原厂蓝牙电源，不调用 Denit，也不会修改已初始化的服务 UUID。初始化前先持久化 `init_uncertain`；只有确认返回值为 `1` 才清除。若超时或失败，标记保持锁定，避免重复执行状态不确定的 native 初始化；提示需要重启车机蓝牙服务后再试。SPP 初始化标志本身不代表已有连接。

接着 `0x40` 请求远端 iPhone 的 iAP2 UUID。UUID 当前编码为 16 字节 canonical big-endian；H52.105 native 端实际如何解释字节序仍需实车确认。返回 `result=1` 仅表示请求被接受，不代表链路建立。实现仅对 slot `0..3` 轮询 `0xd0` 并同时校验连接状态和远端地址；slot `4..9` 无法用当前 `0xd0` 输出布局确认，因此会释放已接受的 slot 并报失败。只有状态轮询确认后，`connect()` 才返回字节流；建立后每 3 秒复核 slot 状态及地址。

SPP 数据回调是全局 callback，descriptor 为 `com.anwsdk.service.IAnwSPPDataCallBack`，transaction `1` 携带 slot、字节数组和长度。`AnwSppDataCallback` 校验 slot/长度并只转交有效数据；`AnwBluetoothBackend` 按 slot 隔离回调。连接响应到达前的数据按 slot 缓存，每个可确认 slot 上限 64 KiB，只在该连接获得对应 slot 后送入；其他 slot 的数据不会串流。溢出、格式错误和断开都会让字节流失败并通知 owner 释放连接。

[`CallbackDuplexByteStream.kt`](../../shared/src/main/java/com/shilapi/xcertplay/transport/CallbackDuplexByteStream.kt) 用有界 FIFO 接收回调数据，单次 `send()` 的写块最多 8 KiB，并根据 positive partial-write 返回值推进 offset，直到整段写完；写事务要求结果为 `1` 且写入数大于零。关闭时先唤醒读者、拒绝迟到 callback，再由后台线程 unregister callback、按 ownership 清理 native slot 并 unbind。不会在 Binder callback 线程内同步调用远端清理。

## 验证范围和限制

`mobile/src/debug/.../Api18CompatProbeActivity.kt` 的 `geely_spp` 模式及 shared tests 使用本地 mock Binder 覆盖 Parcel 往返、SPP 回调、partial write 和 owned-slot cleanup。**这些 mock 只验证本地协议实现，不证明普通应用能在真实 H52 上完成无线连接。** 当前不能宣称 H52.105 真车无线 CarPlay 已打通；native UUID 字节序解释及车机实际 iAP2/SPP 互通仍待实车核验。

用户操作顺序：先在车机原厂蓝牙界面与 iPhone 配对；在 DiPlay“连接设置”开启 **尝试 H52 原厂蓝牙连接**；点“选择 iPhone”并从原厂配对列表选中设备；再发起无线连接。若失败，在“诊断”中保存报告（Android 4.3 保存到 `/sdcard`，界面会显示实际路径），并提供报告以区分服务绑定、初始化、slot 状态及连接阶段失败。报告和日志不能替代实车链路验证。

## 固定候选与独立测试

APK：DiPlay-0.2.10-Geely-Android43-H52-ANW-test.apk，18,274,529 bytes；SHA256：681485ca3ce2e5c4aec1857c9e1c064af9e173fa09b4e77598b64f0d0652c4dc。包含完整认证私钥和证书，源码不含原始凭据。shared443+common144=587项单测全部通过，0失败/错误/跳过。新增后端回归包括共享callback串流隔离、early数据、初始化失败flag锁定、accepted slot4..9释放、不同对端不误断、partial write和取消清理。API18实际Binder/Parcel mock探针与字节流探针通过；不代表厂商服务及iPhone握手已通过实车。
