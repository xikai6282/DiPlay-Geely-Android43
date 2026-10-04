# 吉利 H52 原厂音频通道适配

## 运行时能力和版本
固件10500/12000都定义 STREAM_CARPLAYAUDIO=23、STREAM_NAVI_TTS=11、STREAM_NAVI_ALERT=25、STREAM_MIC=14、MODE_CARPLAYAUDIO=14。mode14与stream14是不同类型，不能混用。仅在运行时检测到23/11/mode14正确时提供H52配置；原生Android和H41不启用。

`shared/.../media/GeelyAudioCapabilities.kt` 读取公开常量并检查精确值，提示音25单独作为可选能力。
`AudioChannelMapping.kt` 的 AudioStreamRouting 区分自动(null)与真实VOICE_CALL(0)；媒体用23、导航11；仅audioType=alert且用户启用时用25，不把全部导航送到25。
`AndroidMediaSink.kt` 构造实际AudioTrack时应用流号，创建被厂商拒绝时回退MUSIC(3)，焦点使用实际成功创建的流。
`AudioFocusCoordinator.kt` 按(channel,actualStream)独立保存lease/listener/refcount；只将返回1认为成功，失败静音；duck=0.2、loss=0、gain=1，旧listener回调不能改变替换后的lease。失去焦点后保留lease等待恢复，避免抢电话/倒车焦点；最后轨道释放前移除再abandon。

## 界面和生命周期
`CommonAirPlayPersistence.kt` 保存默认关闭的原厂音频偏好；`DiPlayActivity.kt` 在能力满足时显示H52选项及试听，通用模式不会被H52覆盖。`CarPlayHostActivity.kt` 比较偏好并在需要时重建会话，取消设置恢复原偏好。
`AudioChannelPreview.kt` 使用对应流申请并释放焦点，关闭对话框结束试听。

## 固件推导和实车边界
H52框架对流23选择CarPlay mode14依赖CAN功放状态，应用不强制setMode(14)。流11和25有不同策略；25部分XML优先级存在命名疑点，保持独立可选并需要试听。H41的Siri/电话映射不能直接套用。
模拟器验证了生产焦点协调代码在真实API18 AudioManager上的生命周期及stock能力拒绝，使用测试流3/4/5，未验证H52 HAL、真实扬声器、功放或倒车/电话优先级。实车测试应分别播放媒体、导航、提示音，检查音量、混音、电话与倒车打断后的恢复。
完整类路径、函数行号与实际变更见 SOURCE-INDEX.md 和 UPSTREAM-TO-API18.patch。
