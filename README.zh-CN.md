# DiPlay — 吉利 Android 4.3 适配

基于 [shihabal3amri/DiPlay](https://github.com/shihabal3amri/DiPlay) **v0.2.10** 的社区适配版本，目标为吉利 H41 类 Android 4.3（API 18）车机。原始 CarPlay 接收器、界面及协议实现由上游项目和其贡献者提供；本仓库主要完成旧 Android 系统兼容、中文文案修正、模拟器验收和适配文档。

**状态：实验性适配。Android 4.3.1 x86 模拟器验证通过；吉利 ARM 实车及真实 iPhone 连接尚未验证。** H41 固件只做静态研究，没有移植其私有系统服务、固件、认证材料或原厂 CarPlay 二进制。此项目与 Apple、吉利和原作者均无官方隶属或认证关系。

## 下载与安装

[本仓库 Releases](https://github.com/xikai6282/DiPlay-Geely-Android43/releases) 提供公开测试 APK 及对应源码。安装在车机端。测试包标识为 `com.shihab.diplay.hudtest`、`versionCode=29`、`versionName=0.2.10-hud-test`，最低 API 18。

Release 提供**内置实验性认证材料的完整车测 APK**，无需用户自行补充认证文件即可尝试连接。源码不包含独立认证密钥或 APK 签名密钥；本地重新构建完整包仍需外部认证输入，见构建文档。

这不是 Apple 认证产品。APK 中的认证身份可被提取；该实验身份并非为本项目新签发，来源说明沿用上游 [第三方声明](docs/THIRD_PARTY_NOTICES.md)。内置材料和模拟器签名测试均不能保证真实 iPhone 接受，未来 iOS 更新的兼容性也未验证。

## 本次适配

- API 18 启动、设置页、权限检查、前台服务、悬浮窗和旧版 UI 属性兼容。
- 旧版 MediaCodec 输入/输出缓冲区、AudioTrack/AudioRecord、RCC 媒体按键及音频焦点。
- 旧 USB 配置描述符扫描、同步短写/16 KB 分块、NCM 数据传输和 VPN 非阻塞回退。
- Java Base64、字符集、时间与集合 API 回退；IPv6 监听和接口级 Bonjour/mDNS。
- 保留 0.2.10 的 USBMUX framing、动态 AirPlay 端口、媒体元数据、封面队列及诊断边界。
- 中文术语与热点状态显示修正。Wi-Fi 直连模式由车机创建网络，iPhone 加入；它不代表车机连接苹果个人热点。
- 需要新版系统的 Media3 视频、使用情况监控和部分仪表盘功能，在 API 18 上明确禁用或返回不支持。无线功能还取决于实际车机固件能力，不能根据模拟器结果承诺可用。

## 文档

- [代码级适配技术说明](docs/geely-android43/TECHNICAL.md)
- [构建与复现](docs/geely-android43/BUILD.md)
- [验收结果与限制](docs/geely-android43/VALIDATION.md)
- [逐文件方法索引](docs/geely-android43/SOURCE-INDEX.md)
- [上游到适配版源码补丁](docs/geely-android43/UPSTREAM-TO-API18.patch)
- [详细迁移审查](MIGRATION-REVIEW.md)
- [上游与第三方来源](docs/geely-android43/ATTRIBUTION.md)
- [上游原始说明](docs/geely-android43/UPSTREAM-README.md)

## 开源来源与许可

基线：DiPlay v0.2.10，提交 `3e43e25c55921bdf5149f5f92851acf202ed353a`。本次工作从对应 0.2.10 源码归档开始，并非基于 0.2.9，也没有合并上游后续版本。

保留上游 [GPL-3.0 LICENSE](LICENSE)、版权声明及 [第三方许可文件](docs/licenses)。上游注明基础实现来自 xcertplay（GPL-3.0），界面及网站适配自 DiAuto（AGPL-3.0）；这些原始声明及适用许可继续保留，详见 [上游 Credits](docs/THIRD_PARTY_NOTICES.md)。本仓库的适配说明不会替代各组件原有许可。

## H52 新增测试版

新增原厂音频配置和默认关闭的厂商蓝牙只读检测按钮。**OEM 无线传输尚未实现**。API18 可复制蓝牙诊断摘要，完整文件导出仍不支持。

详见 [音频代码说明](docs/geely-android43/H52-AUDIO.md)、[蓝牙协议说明](docs/geely-android43/H52-BLUETOOTH.md)、[固定APK独立验收](docs/geely-android43/H52-VALIDATION.md)。
