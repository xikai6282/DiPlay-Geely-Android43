# DiPlay · 吉利 Android 4.3 适配

面向博瑞 H52 / i.MX6 / API18 的 CarPlay 适配。**视频流畅度与触控优化、原厂免 root 桥接，以及音乐和导航声音修复，是这一轮迭代的主要成果。** 其他车型仍需独立验证。

## 下载与测试状态

| 版本 | 用途 | 状态 |
| --- | --- | --- |
| **[P21 实车测试包](https://github.com/xikai6282/DiPlay-Geely-Android43/releases/tag/h52-p21-20261008)** | 音乐、导航声音修复及无效 HUD 重试停止 | **用户实车确认两者有声，画面和触摸正常** |
| [P22 整合候选](https://github.com/xikai6282/DiPlay-Geely-Android43/releases/tag/h52-p22-20261008) | 最终 P16 的流畅度/免 root 改动 + P21 音频/HUD 修复 | **未再次实车测试，标记 Pre-release** |

P21 已上传并保留，P22 不替代 P21 的实车验证状态。下载页同时提供 APK、源码归档、校验值和说明。包名为 `com.shihab.diplay.repair1`；P21 清单仍显示 P16/code30，P22 已统一显示 P22/code31，请用文件名与 SHA256 区分。

## 流畅度、触控和免 root 修复

**P16 是从“已出画面但卡顿”推进到流畅度优化的关键版本。** 最终 P16 已加入以下改动，P22 已逐项核对并保留：

- 视频积压恢复由每次销毁硬解改为 flush 与关键帧重同步，减少该路径反复重建开销。
- 视频接收与解码线程提升至 URGENT_DISPLAY；视频 Socket 开启 TCP_NODELAY，申请 512KB 接收缓冲。
- 继承 P14 的触控 Event Socket TCP_NODELAY，改善交互发送延迟。
- USB 配置切换与热点信道读取接入吉利固件 usbprotect 桥接，替代对应 su 路径，保留我们自己的免 root 实现。
- 修正界面、清单与源码版本号，继续保留 API18/Dalvik、多 DEX、Freescale 硬解和原厂蓝牙兼容。

P16 阶段现场已确认 Freescale 硬解与 1920×720 正常画面；对话记录报告跟手改善。应用提交统计不等于物理屏幕 FPS；零恢复计数窗口未覆盖 flush 触发后的恢复，仍需专门回归。

**打包纠错：** P20/P21 曾沿用过时的 classes2 树，导致 USB 免 root 调用、视频线程/Socket 和恢复改动遗漏。P21 的音频实车结果仍有效；P22 已从最终 P16 APK 重新提取 DEX 后合并修复。P21 源码 ZIP 与其运行 APK 的 USB 路径不完全一致，归档供历史审查；P22 提供实际组装 smali 树和重打包脚本。

## 音乐、导航和后台修复

- 撤回 API18 清零 Opus 的协商尝试，恢复手机建立无线音频流。
- API21 以下以 Concentus 软件解码导航 Opus，保留 AAC-LC 音乐及既有媒体/导航音频通道。
- 每个解码实例独立状态、复用采样缓冲，结束时清理；坏包限量记录。
- BYD SomeIp 服务存在且启用才启动 HUD；H52 不再每 300ms 反复尝试绑定不存在的服务。
- P21 实车用户确认：音乐、导航都从车机出声，画面和触摸正常。切换页面仍观察过 Surface 释放警告，不能据此宣称所有生命周期问题已修复。

## 版本迭代与全部改动

| 阶段 | 内容 |
| --- | --- |
| P12 | USB / 原车热点无线连接、宽屏与硬解基线，仍有卡顿和导航无声 |
| P13 / P14 | 音频 PCM 协商尝试 / 触控 TCP_NODELAY；PCM-only 尝试后续撤回 |
| P15 | 从 P12 单独分出的硬解调用计时诊断包 |
| **P16** | **流畅度、触控、USB/Wi-Fi 免 root 桥接、版本号修正** |
| P17–P19 | 会话保护与 CPU/蓝牙检测支线，不默认累计进 P16 |
| P20 / **P21** | 无线音频协商恢复 / **导航 Opus 解码、音乐有声、HUD 门控实车通过** |
| P22 | 重新合并完整 P16 与 P21；本地检查通过，等待实车复测 |

完整逐项说明、版本分支关系、打包遗漏与未实施方案集中在 **[VERSION-HISTORY.md](VERSION-HISTORY.md)**。后续更新统一写在该文件，不在首页末尾重复追加。

## 使用与后续工作

1. 在原车蓝牙中配对 iPhone，打开 DiPlay 的 H52 原厂蓝牙连接通路。
2. 无线连接使用车机热点，iPhone 保持 Wi-Fi/蓝牙开启。本车观察到手机个人热点开启会阻止连接，关闭后恢复。
3. 免 root 桥接取决于对应固件服务；测试车本身已有 root，尚未完成无 root 环境全部 USB 场景验证。fallback36 是回退信道值，不代表已测得 5GHz。
4. E01/6Q 自动识别与配置、自动读取原车热点、方向盘按键、首次启动优化仍在规划。5GHz 必须免 root 临时测试成功后才允许持久修改，失败不永久写入。

当前默认分支应用源码保留历史 P12，最新源码与实际组装文件在各下载包中；P22 全 Gradle 构建、模拟器及实车复测未运行。历史技术说明见 [H52 修复状态](docs/geely-android43/H52-REPAIR-STATUS-2026-10-06.md)、[构建说明](docs/geely-android43/BUILD.md)、[API18 迁移审查](MIGRATION-REVIEW.md)。

## 来源与许可

基于 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay) v0.2.10 的 API18 适配，选择性移植上游 0.2.13 的适用修复，不是直接运行官方现代系统 APK。保留 [GPL-3.0 LICENSE](LICENSE) 和 [第三方来源及许可](docs/THIRD_PARTY_NOTICES.md)。实验性认证资产沿用现有声明，APK 不是 Apple 认证产品；源码归档不提供独立认证或签名私钥。

