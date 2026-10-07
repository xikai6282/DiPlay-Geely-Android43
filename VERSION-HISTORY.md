# 吉利 H52 版本迭代与全部改动

## 从卡顿到流畅度优化，再到声音修复

P12 已打通博瑞 H52 的 USB/原车热点 CarPlay、1920×720 Freescale 硬解出画面，但存在卡顿、触控迟滞、无线导航无声。**P16 是流畅度优化的重要节点**：减少积压时的硬解销毁重建，调整视频线程调度与网络接收，继承触控 TCP_NODELAY，并接入吉利固件免 root 桥接。后续 P20/P21 完成无线音频协商与 API18 Opus 导航解码，实车确认音乐、导航、画面、触摸正常。P22 将这两组成果重新完整合并，修正 P21 打包遗漏。

## 版本关系

这不是 P12→P13→…→P22 每版都累加的单线升级：
- 性能主线：P12→P13→P14→最终 P16。
- 计时/检测支线：P12→P15→P17→P18→P19，不能默认已合进 P16。
- 音频线：P16→P20→P21；复核发现 P20/P21 重建 classes2 时使用过时树，漏掉部分 P16 更新。
- 当前整合：**最终 P16 APK重新提取DEX + P20/P21音频/HUD → P22**。

| 版本 | 修改和目的 | 验证与最终状态 |
| --- | --- | --- |
| P12 | 修复无线凭证BSSID；继承此前 API18、USB配置/NCM、宽屏Surface、原车蓝牙、音频流23/11、身份UUID等兼容修复 | H52 USB与热点无线出画面；卡顿/导航无声尚存 |
| P13 | API18将Opus能力清零，尝试改协商PCM导航 | 当时未实车验证；后续P16无线音频无法建立，此尝试在P20撤回 |
| P14 | AirPlay触控Event Socket开启TCP_NODELAY，移植上游PR311适用改动 | 继承进入P16/P22；不是完整升级所有上游0.2.13功能 |
| P15 | 输入缓冲等待、queue到output、release调用聚合计时，单独从P12分出 | 诊断候选；不是VPU纯耗时或屏幕FPS测量，不自动包含P13/P14 |
| P16 | 流畅度优化、免Root桥接、版本号与清单修正，详见下方 | 当时已实车硬解出画面；不能据单个零恢复计数窗口宣称所有卡顿根除 |
| P17–P19 | 会话结束保护、CPU/厂商蓝牙识别、忽略非激活或旧会话结束回调 | 独立检测/连接支线；不是最终P16/P22累计内容，E01整合仍需核对 |
| P20 | 恢复Opus声明，手机再次建立导航音频流 | 协商与接收恢复；API18 MediaCodec Opus解码失败，导航仍无声 |
| P21 | Concentus软件Opus导航、AAC媒体保留、BYD缺失服务门控 | 用户确认音乐/导航有声且画面触摸正常；打包误用了旧classes2，P16部分改动丢失 |
| P22 | 从最终P16重建，保留完整性能/USB桥接，合并P20/P21修复；界面与manifest版本P22/code31 | 签名、对齐、最终DEX与逐类差异检查通过；本包尚未再次实车/模拟器回归 |

## P16 已落地，P22 已逐项保留的修复

1. **积压恢复降低重建开销**：视频积压/队列溢出进入recover时，改由H52MediaCodecOptimizer调用MediaCodec.flush，继续原参考链重同步与关键帧请求，避免该路径每次销毁硬解。原错误异常及Surface重配置路径仍可重建。i.MX6上的flush触发后能否稳定恢复需专门回归；零recoveries窗口没有覆盖该路径。
2. **视频线程调度优化**：ScreenStream与VideoDecoder调用THREAD_PRIORITY_URGENT_DISPLAY（nice -8），减轻与原车后台竞争的影响。调用成功与实际收益需运行态观测。
3. **视频传输调优**：接收Socket开启TCP_NODELAY，申请512KB接收缓冲；实际生效缓冲大小由内核决定。
4. **触控链路调优**：继承P14 Event Socket TCP_NODELAY；触控协议、加密和排序保留。
5. **USB免root桥接**：配置切换EBUSY时，通过H52NoRootBridge调用固件usbprotect系统服务0x16，替代原su写sysfs回退。普通USB授权与既有配置检查保留；未root环境完整USB投屏尚未单独验收。
6. **热点信道免root读取**：H52WirelessDiagnostics调用桥接读取hostapd配置，使用观测值/配置值/36兜底；36是回退值，不能显示成已测得5GHz，不代表5GHz测试或永久改写已完成。
7. **版本显示和安装升级**：P16曾修正Activity版本显示、AXML versionName/code、Gradle配置；P22进一步统一0.2.13-H52-P22/code31，避免文件名与界面长期不一致。
8. **旧系统兼容保留**：API18/Dalvik、多DEX、Freescale硬解、原厂蓝牙、USB传输、已有认证载荷和音频流路由均从冻结P16继承。

P16现场记录确认Freescale硬解、1920×720正常出画面；对话报告跟手改善及应用touch2frame约34–43ms、采样窗口recoveries=0。收流/解码提交计数不是物理屏幕FPS，不能将101ms接收处理统计直接认定为ChaCha或VPU单独耗时。没有严格的同场景前后对照百分比，因此不编造“流畅度提高多少%”。

## P20/P21 已落地，P22 已合并的修复

1. 恢复无线压缩音频Opus声明，撤回PCM-only尝试，解决手机仅保留iPhone音频输出、不创建CarPlay音频流的问题。
2. API21以下导航采用Concentus1.0.2纯Java软件Opus，输出PCM送原导航通道；现代系统仍用原MediaCodec。
3. 每个播放实例独立解码状态，复用short缓冲，释放时清理；坏包跳过并限制日志，支持有效短Opus包。
4. 保留AAC-LC音乐解码、媒体流23、导航流11及既有焦点逻辑。P21实车用户确认两者都从车机出声。
5. BYD HUD仅在指定SomeIp服务存在且启用时初始化；H52缺服务不启动300ms重试，P21现场计数为0。
6. 明确无线连接使用车机热点。本车观察到iPhone个人热点开启会阻止连接，关闭后恢复；不推广为所有车型/iOS的绝对规律。
7. 核对已装APK与交付hash，保留画面/触摸恢复的用户验证和Surface切换警告，避免把安装成功当功能通过。

## P22 新增的整合纠错

- 以最终P16 hash55fb297e…重新提取DEX，补回P21丢失的USB免root调用、视频socket调优、两处线程提权和flush恢复。
- P21源码与APK的USB路径不一致；P22修复并交付实际用于组装的smali树、classes4、构建脚本和源码快照。
- 修复继承源码中H52WirelessDiagnostics无参数调用与实际helper双参数签名不一致；API18 Java编译检查通过。
- 主DEX、classes3、全部native与其他原载荷字节保持P16；classes2实际仅音频声明、AudioRenderer、HUD和版本显示四个类有语义变化。

## 对话提出但尚未实施的优化

| 方案 | 当前状态 |
| --- | --- |
| ChaCha20-Poly1305 JNI/C/NEON加速、复用cipher | 尚未合入；不能宣称已从40–80ms降至1–2ms |
| 缓存MediaCodec inputBuffers | 尚未合入 |
| VideoDecodeQueue去filter/sum临时列表 | 尚未合入 |
| 视频ByteArray BufferPool | 尚未合入 |
| PR313 NSD/Bonjour容错移植 | 计划提及，P14→P16最终DEX没有新增对应改动，不计为已完成 |
| 30fps协商限制 | 设置建议，未在该轮强制修改；无充分证据把i.MX6此分辨率上限固定成30fps |
| 首次启动异步闪屏 | 尚未合入；需同时改Application/mainDEX启动依赖，不能只加Activity |
| Media3/BouncyCastle依赖剪枝 | 尚未合入；须保留实际使用与加密反射路径，16MB→5MB/60%提速尚未测量 |
| 安装脚本首次预热 | 建议未作为通用安装器交付；am start会启动Activity，不能称隐藏后台 |
| E01/6Q识别自动配置、自动读取原车热点、5GHz免root临时验证后持久化 | 属下一阶段双平台整合；失败不永久修改 |
| 方向盘按键 | 尚未接通，需采集真实车机事件来源 |
| Surface离开/返回、Siri、电话、长时和重启回归 | 未完整覆盖 |

## 免root与实车范围

这里的免root是应用不调用su、使用原厂固件服务；支持范围取决于该服务与固件权限。P16/P21测试车本身已有root，尚不能据此保证任意未root固件完整连接。P22没有运行5GHz测试、没有永久改写配置、没有覆盖安装车机。本仓库仍保留GPL/第三方来源与历史测试版本，旧报告按版本和日期理解。

