# H52 音频和蓝牙诊断候选独立验收（2026-10-04）

APK：DiPlay-0.2.10-Geely-Android43-H52-test.apk
SHA256：D841D6593D6006588006888FE56938D2323E7B9091428F6B33D53F6BAB1FAC05
大小：18,242,956 bytes。完整 standalone 包，包含原有认证私钥和证书。

主智能体安装固定 APK 后独立运行 Android 4.3.1/API18/x86 模拟器验收。
完整音视频、USB 分块回调、可选功能隔离、MapEmbed、Overlay、Media3隔离、H52焦点协调、H52蓝牙 Parcel 协议、媒体按键和前台服务回归全部通过。
实际 UI：设置页打开并滚动9次；蓝牙检测默认关闭、手动按钮随开关启用、开关重启持久化、缺失厂商服务显示未知、API18复制摘要可点击、关闭后重启仍关闭；均无 FATAL EXCEPTION。
H52音频能力保护：原生模拟器仅显示通用模式，不暴露不存在的厂商流。
独立汇总 shared/common 的 JUnit XML：566 tests，failures/errors/skipped均0。

静态检查 NewApi 三模块均0；整套 lint 未通过：shared12 errors/57 warnings，common87 errors/139 warnings，mobile只有20 warnings。包含非英语/简体中文资源缺翻译和现代Android权限等问题，不将其表述为全部旧问题。

验证边界：蓝牙是默认关闭的只读状态诊断，未实现OEM无线传输；模拟器Binder mock不证明车机服务可访问，更不证明iPhone无线握手。H52真实音频路由、CAN功放、电话/倒车优先级仍需实车验证。API18支持复制蓝牙摘要，完整诊断文件导出仍不支持。
实车为H52.10500；12000新增的扩展Socket/SPPConnectEx接口不可在10500上直接使用。
