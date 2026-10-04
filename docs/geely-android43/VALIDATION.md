# 验收结果与边界（2026-10-04）

## 两个 APK 的区别

| 包 | 内容与验证 |
|---|---|
| Release 完整车测包 | 含外部指定认证输入，按发布者要求公开发布；SHA256 A0DBAA94D507B0AE57FF2FE668FE4A42B546F0E93735CD72E4F832DD08084E6B；最新版汉化完整模拟器回归通过 |
| 额外无认证 UI 测试包（不作为本次 Release 下载包） | 不含 offline-mfi 私钥/证书；SHA256 4DB19A206445B643C409F1AD27B8F81F8E7667E7879E277133894AC573F0909D；安装、启动及设置 UI 另行验证，不声称可直接完成 iPhone 认证 |

二者均为同一版适配代码，区别是构建时是否提供外部认证 assets。包名 com.shihab.diplay.hudtest，code 29，versionName 0.2.10-hud-test。签名为本地 debug 测试签名，不是生产签名。

## 自动验证

- shared 单测 405、common 单测 140，共545项，0失败/错误/跳过。
- 三模块 NewApi lint = 0。mobile lint 成功。
- shared lint仍有12 errors/56 warnings；common仍有39 errors/135 warnings，均非NewApi，包含权限、格式和其他语言缺少翻译等。不是全lint通过。

## Release完整车测包：Android 4.3.1 / API18 / x86 模拟器

完整回归返回全部 true：Base64/MIME、codec枚举、USB配置描述符、IPv6生产监听、Bonjour TXT、Lockdown证书/TLS ClientHello、MFI签名一致性、H264两次Surface重建40帧、PCM/AAC实际播放头推进、麦克风加密RTP、两项native库装载、VPN三次建立/空闲/关闭；此外USB32KB短写回调、旧使用情况功能禁用、MapEmbed unsupported实际回复、悬浮地图Surface创建关闭、Media3隔离、RCC播放暂停恢复释放、前台服务注册与清理均通过。

测试输出见 validation/。MFI签名测试不证明iPhone信任；USB短写为回调引擎，不是物理USB。主机架构x86，不能证明ARM实车的native装载。

## 设置页面 UI

完整 Release APK 以及额外无认证测试 APK 均已实际安装到 API18 模拟器，冷启动、打开设置页及9次滚动全部通过，未发现 FATAL EXCEPTION。完整 Release 包结果见 [full-apk-ui/public-apk-ui-results.json](full-apk-ui/public-apk-ui-results.json)，额外无认证包结果见 [public-apk-ui/public-apk-ui-results.json](public-apk-ui/public-apk-ui-results.json)，UI层级和日志见各自目录。UI检查不包含MFI认证测试；完整包另有上述完整回归。

## 待硬件验证

吉利H41实际固件权限、ARMv7 native装载、USB OTG/NCM、真实iPhone配对信任、无线CarPlay、原厂服务/端口冲突、AudioHAL、重启自启和断线重连。H41固件研究为静态研究，不是运行验收。
