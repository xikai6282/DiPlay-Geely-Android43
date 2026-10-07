# P21 · H52 音乐与导航声音修复（2026-10-08）

**补充复核：P21 的音频、画面和触摸已实车通过，但重建 classes2 时误用过时树，遗漏部分最终 P16 的流畅度与 USB 免 root 调用。源码 ZIP 与实际 APK 的 USB 路径不一致。P22 本地候选已重新整合，尚未测试，暂不发布。**

[正式 P21 Release（含 APK、源码 ZIP、校验文件）](https://github.com/xikai6282/DiPlay-Geely-Android43/releases/tag/h52-p21-20261008) · [完整逐项版本记录](../../VERSION-HISTORY.md)


本目录保存实际安装并验证的 P21。只在一台博瑞 H52 / i.MX6 / Android 4.3 API18 实车测试，E01尚未验证。

## 下载

- [P21 测试 APK](DiPlay-H52-P21-OpusSoftwareHudGate-test.apk)
- [P21 完整源码快照](DiPlay-H52-P21-source.zip)

APK SHA-256：`d15a8655b0225579e018d578619fbff23d2a4d5da0de6b2e971b768fec66437f`
源码 ZIP SHA-256：`31f2a61ed7498417135d79f16e76d8c6ddec2826530d90b8177b001ca2d4d6e1`

包名 `com.shihab.diplay.repair1`，最低API18。Manifest仍显示`0.2.13-H52-P16` / versionCode30；本轮为保持其他APK内容不变未改版本字段，请以P21文件名和SHA256区分。可覆盖同签名repair1测试包；旧hudtest是独立包，应关闭其他投影应用避免竞争。

## 修复与实车结果

1. 恢复无线音频Opus能力声明，修复P16在API18清零Opus后手机不建立音频流的问题。
2. API21以下导航Opus使用Concentus1.0.2纯Java解码，PCM送入原有导航播放通道；现代系统保留原MediaCodec路径。每个播放实例独立解码状态，结束时清理，坏包有限日志并跳过。
3. 音乐继续使用AAC-LC和原有车机媒体通道；实车用户确认音乐和导航均从车机出声。
4. BYD HUD仅在指定服务存在且启用时初始化；H52不启动缺失服务的300ms重试，实车新进程bindService=false计数为0。
5. 用户确认画面和触摸正常；安装后拉回APK的SHA256与交付包一致。

## 连接与限制

无线使用车机热点，iPhone关闭个人热点并保持Wi-Fi/蓝牙开启。本车现场发现手机个人热点开启会阻止进入无线CarPlay；没有把它推广为所有iOS/车机的通用结论。

classes3保留P16热点桥接，但USB配置路径在classes2重建时回退为su，不能宣称完整保留免Root。当前测试车机本身已有Root，未完成未Root环境完整验证，不能据此保证所有固件免Root。5GHz免Root临时测试/永久修改、E01整合、自动热点配置、方向盘按键和启动瘦身尚未合入P21。

切换页面时仍出现Surface已释放/解码器重建警告，随后用户确认画面与触摸恢复；不宣称生命周期问题彻底修复。未完成长时间运行、重启、Siri/电话、所有USB场景及其他车型回归。

## 构建和来源

P21相对P20修改AudioRenderer与BydHudBridge，并新增软件解码器DEX和许可文件；相对最终P16还存在前述误回退，原先只对P20做比对不充分。API18 v1/v2签名、zipalign、最终DEX回读、其他payload保持检查通过。软件Opus50帧本地编码/解码样本通过；全Gradle构建未运行，源码快照不是经过全工程重新编译验收的发布版。默认分支原应用源码仍为历史P12；P21源码见本目录ZIP。

Concentus依赖：`io.github.jaredmdobson:concentus:1.0.2`，[Maven Central](https://central.sonatype.com/artifact/io.github.jaredmdobson/concentus/1.0.2)。许可保留于APK及源码包。无线压缩音频要求参见[Apple WWDC2016](https://developer.apple.com/videos/play/wwdc2016/722/)。

源码包不含独立认证密钥及签名密钥；APK保留现有实验性认证资产及原项目声明，不是Apple认证产品。

