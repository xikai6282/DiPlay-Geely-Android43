# DiPlay 0.2.10 吉利 Android 4.3 适配测试版

基于 [shihabal3amri/DiPlay v0.2.10](https://github.com/shihabal3amri/DiPlay/tree/v0.2.10)，基线提交 `3e43e25c55921bdf5149f5f92851acf202ed353a`。感谢 DiPlay 原作者和贡献者，以及上游引用的 [xcertplay](https://github.com/shilapi/xcertplay) 和 [DiAuto](https://github.com/shihabal3amri/DiAuto)。保留原 GPL-3.0、DiAuto AGPL-3.0 及第三方许可声明；完整源码与文档随本次发布提供。

本次为**吉利 H41 类 Android 4.3（API18）车机兼容适配**，并非原作者官方版本或吉利/Apple 认证产品。

## 下载

- `DiPlay-0.2.10-Geely-Android43-car-test.apk`：完整车测包，保留内置实验性认证私钥与证书，无需另补认证文件即可尝试连接。安装到车机端。
- `DiPlay-0.2.10-Geely-Android43-source.zip`：对应完整 Android 应用源码树、原许可、适配技术文档、代码方法索引和测试记录；不含独立认证文件或 APK 签名私钥。
- `SHA256SUMS.txt`：下载文件校验值。

版本：`0.2.10-hud-test` / code 29；包名 `com.shihab.diplay.hudtest`；最低 Android 4.3/API18；含 armeabi-v7a、arm64-v8a、x86、x86_64 native 库。APK 是 debug 签名测试包。

## 适配内容

旧版 UI/权限/前台服务、MediaCodec 与 AudioTrack/AudioRecord、USB 描述符/分块短写、VPN、IPv6/mDNS、媒体按键、悬浮窗及中文文案。保留 0.2.10 传输和媒体行为；需要现代 API 的可选功能在 API18 上禁用或明确返回不支持。

## 验证与使用边界

545项 shared/common 单测通过，三个模块 NewApi lint 为0；shared/common仍有非NewApi lint问题，详见源码中的验收报告。完整APK已通过 Android 4.3.1 x86 模拟器音视频、监听、USB回调、VPN、媒体按键、服务和设置页面检查。

**吉利 ARM 实车、实际 USB/NCM、真实 iPhone 配对信任及无线 CarPlay 尚未验证。** 不承诺所有吉利车型或 iOS 版本可用。认证身份可从APK中提取；它是沿用上游实验配置的身份，不是为本适配新签发的 Apple MFi 身份，来源与分发说明见 [第三方声明](https://github.com/xikai6282/DiPlay-Geely-Android43/blob/main/docs/THIRD_PARTY_NOTICES.md)。内置认证材料不等于已经取得Apple认证或保证iPhone接受。

详见仓库 [README](https://github.com/xikai6282/DiPlay-Geely-Android43)、[代码级文档](https://github.com/xikai6282/DiPlay-Geely-Android43/blob/main/docs/geely-android43/TECHNICAL.md) 和 [验收报告](https://github.com/xikai6282/DiPlay-Geely-Android43/blob/main/docs/geely-android43/VALIDATION.md)。
