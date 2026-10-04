# 构建与复现

## 环境

本次本地构建使用 Windows、PowerShell 7、Android SDK（compileSdk 37）、Gradle Wrapper、Gradle daemon JVM 25（见 gradle/gradle-daemon-jvm.properties）。NDK 与插件具体版本以源码构建配置及 gradle/libs.versions.toml 为准；不要将 compileSdk 37 与最低运行版本 API 18 混淆。首次构建需要下载依赖。

```powershell
$env:ANDROID_HOME = 'E:\android-sdk' # 改成自己的 SDK 路径
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:HOST_OS = 'windows'
$env:PATH = 'C:\Program Files\Git\usr\bin;' + $env:PATH
Remove-Item Env:DIPLAY_AUTH_ASSETS_DIR -ErrorAction SilentlyContinue
.\gradlew.bat :mobile:assembleDebug --no-daemon --max-workers=1 '-Pkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.jvmargs=-Xmx768m -Dfile.encoding=UTF-8'
.\gradlew.bat :shared:testDebugUnitTest :common:testDebugUnitTest --no-daemon --max-workers=1 '-Pkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.jvmargs=-Xmx768m -Dfile.encoding=UTF-8'
.\gradlew.bat :shared:lintDebug :common:lintDebug :mobile:lintDebug --continue --no-daemon --max-workers=1 '-Pkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.jvmargs=-Xmx768m -Dfile.encoding=UTF-8'
```

APK 路径：mobile/build/outputs/apk/debug/mobile-debug.apk。公开测试包使用 debug 签名，不是正式签名发行版。签名及认证私钥不进仓库，签名配置环境变量见 mobile/build.gradle.kts。

## 完整车测构建

仅在已拥有合法可用的本地认证输入时，指定外部目录：

```powershell
$env:DIPLAY_AUTH_ASSETS_DIR = 'D:\your-local-auth-assets'
.\gradlew.bat :mobile:assembleStandaloneDebug --no-daemon --max-workers=1 '-Pkotlin.compiler.execution.strategy=in-process' '-Dorg.gradle.jvmargs=-Xmx768m -Dfile.encoding=UTF-8'
```

目录结构由 mobile/build.gradle.kts 明确要求为 offline-mfi/identity.pk8 与 offline-mfi/certificate.p7b。普通 assembleDebug 不包含这些输入；assembleStandaloneDebug 验证输入存在。此说明不提供认证材料，也不承诺 iPhone 接受任何身份。将认证输入打进 APK 后，它可从 APK 提取，不能再视为保密文件。

## 模拟器

创建 API 18、Android 4.3.1 x86 AVD 并使用 adb install -r 安装。调试版包含兼容性探针 Api18CompatProbeActivity；测试脚本 reviewer_device_regression.py 的 ADB 路径与设备序列号需按环境修改。完整 full 模式包含认证输入测试，所以公开无认证包不能拿该项失败当成 API 18 兼容失败；相关包的验证范围见 VALIDATION.md。

shared/common lint 存在既有非 NewApi 错误，完整 lint 返回非零是当前已知状态；不能把 NewApi=0 表述为全 lint 无错误。汽车现代模块不是本次 API18 mobile 交付验证范围。
