# H52.10500 Android 4.3 Wi‑Fi Direct 固件核查

核查对象：`<local-H52.10500-firmware>\system\framework`。`system/build.prop` 标识为 `01.03.10500.H52.00030`、Android 4.3、SDK 18、incremental `2018-07-03`。本报告只根据该固件的 framework/services ODEX 反汇编，不根据新版本 Android API 推断。

输入 ODEX：

- `framework.odex` SHA-256：`A2FB4B0B3B123F1CF50BB3D3496913CB83E93A8D8202FFAE9C1D90270A323560`
- `services.odex` SHA-256：`C02E7D697C4DE42DA3B18B4A390BAC6276BDC1288EA60C5DE6A45CCCADB0298C`

使用本机 `<local-tools>/baksmali-2.5.2.jar` 及 `smali-lib` 依赖，以 API 18 模式从 H52.10500 的 `framework.odex` 选择性 deodex。类保存在本报告旁边的 `<local-api18-extract>\framework\android\net\wifi\p2p` 和 `<local-group-list-extract>\framework\android\net\wifi\p2p`。未修改项目源码或固件输入。

## 旧 createGroup 的持久化行为

H52 `WifiP2pManager.smali`：

- `createGroup(Channel, ActionListener)` 声明在第 505 行。方法第 519 行向服务发送 `0x2200d` (`CREATE_GROUP`)，第 521 行将 `arg1` 设为 `-2`；`WifiP2pGroup.smali` 第 21 行定义 `PERSISTENT_NET_ID = -2`。
- `WifiP2pService$P2pStateMachine$InactiveState.smali` 第 53–67 行处理消息；第 543–573 行在 `arg1 == -2` 时读取该设备既有 group network ID。若找到了既有项，第 587 行按 network ID 再建组；若没有，第 615–627 行落到 `WifiNative.p2pGroupAdd(true)`。
- H52 `WifiNative.smali` 第 1271–1297 行显示 `p2pGroupAdd(true)` 下发 `P2P_GROUP_ADD persistent`。因此此旧重载不是临时组请求：它会复用既有 persistent group，或请求创建 persistent group。

## Getter 与频率

H52 `WifiP2pGroup.smali` 中，`getInterface()` 是 public（第 665–673 行），`getPassphrase()` 是 public（第 705–713 行）。`WifiP2pManager.requestGroupInfo(Channel, GroupInfoListener)` 是固件类中的 public 方法（`WifiP2pManager.smali` 第 818 行）；它经 manager channel 向服务请求当前组信息。`WifiP2pService$P2pStateMachine$DefaultState.smali` 第 508–535 行回送当前 `WifiP2pGroup` 的拷贝。因此本机 API18 可在取得当前组对象后读取接口名和组口令；这是对固件运行时字节码的结论，不代表 Android SDK stub 一定允许直接编译调用隐藏 API。

频率不可通过 H52 的 `WifiP2pGroup` 读取：该类字段只有 clients、interface、owner、network ID、network name、passphrase、group-owner 标志，没有 frequency 字段，也没有频率 getter。supplicant group-start 正则在第 59 行包含 `freq=(\d+)`，但构造器（第 273–347 行）读取匹配组时仅保存 group name、passphrase、owner 及 persistent/temporary network ID，没有读取该频率捕获组。`requestGroupInfo()` 返回的 `WifiP2pGroup` 因此不提供 channel/frequency。应用报告 channel/frequency 时应保留为未知，不能将 0 解释成已测量的信道。

## 权限与安装时授权

`WifiP2pService.smali` 第 960–981 行的 `getMessenger()` 在发放 service Messenger 前调用两项检查：`enforceAccessPermission()`（第 673–688 行）检查 `android.permission.ACCESS_WIFI_STATE`；`enforceChangePermission()`（第 690–705 行）检查 `android.permission.CHANGE_WIFI_STATE`。这条 Binder/Messenger 入口承载 `initialize()` 后的 P2P API 操作。

H52 `framework_res/AndroidManifest.xml` 第 212–213 行将 `ACCESS_WIFI_STATE` 声明为 `normal`，`CHANGE_WIFI_STATE` 声明为 `dangerous`。在目标 Android 4.3/API18 上属于安装时权限模型；对该固件而言，取得这两个 manifest 权限即可通过这里的 service 检查，无须把 Android 6+ 运行时权限规则套用到目标设备。此处没有发现 `ACCESS_FINE_LOCATION` 检查。

## 结束会话与删除持久记录是两回事

`WifiP2pManager.removeGroup(Channel, ActionListener)` 第 698–724 行发送独立的 `REMOVE_GROUP` 消息（`0x22010`）。`WifiP2pService$P2pStateMachine$GroupCreatedState.smali` 第 612–652 行处理该消息：以当前组 `getInterface()` 调用 `WifiNative.p2pGroupRemove(interface)`，成功后进入 `OngoingGroupRemovalState`。此分支没有从 persistent group list 删除 network ID。

持久记录另由 `deletePersistentGroup(Channel, netId, ActionListener)`（Manager 第 533 行）触发 `DELETE_PERSISTENT_GROUP`。`WifiP2pService$P2pStateMachine$P2pEnabledState.smali` 第 922–940 行从 `mGroups` 移除 network ID 并报告成功；`WifiP2pGroupList$1.smali` 第 47–83 行的 LRU 删除回调会通知 group delete listener；`WifiP2pService$P2pStateMachine$1.smali` 第 39–60 行由该 listener 调 `WifiNative.removeNetwork(netId)` 并 `saveConfig()`。`WifiNative.smali` 第 2208–2236 行显示 `removeNetwork(id)` 下发 `REMOVE_NETWORK id`。

因此，`removeGroup()` 结束当前 P2P 会话，但不会按这条代码路径清除 persistent credentials；要删除持久记录需另外调用 `deletePersistentGroup()`。管理清理逻辑应保留二者区别，并在创建前后使用 `requestPersistentGroupInfo()` 枚举持久组；不要仅以 `removeGroup()` 成功推断持久组已删除。

## 结论

H52.10500/API18 的旧两参数 createGroup 重载会创建或复用 persistent group；`WifiP2pGroup` 的 `getPassphrase()` 和 `getInterface()` 都存在且为 public；频率不会通过此 group API 暴露；service Messenger 入口检查 `ACCESS_WIFI_STATE` 与 `CHANGE_WIFI_STATE`，在 API18 上分别是 normal 与 dangerous 安装时权限；结束会话与删除 persistent group 是不同操作。

本次只做了固件静态反汇编。没有在 H52 实车创建/清理 P2P group，没有测量无线频率，也没有验证实车 Wi‑Fi Direct 射频可用性。