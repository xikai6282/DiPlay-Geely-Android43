# 上游 v0.2.10 → API18 源码与方法索引

基线提交：`3e43e25c55921bdf5149f5f92851acf202ed353a`。行号对应本仓库源码。

## automotive/build.gradle.kts

状态：modified


## automotive/src/main/AndroidManifest.xml

状态：modified


## automotive/src/main/java/com/shilapi/xcertplay/MainActivity.kt

状态：added

- L32: `class MainActivity : ComponentActivity() {`
- L36: `override fun onCreate(savedInstanceState: Bundle?) {`
- L71: `override fun onDestroy() {`
- L76: `private fun runSelfCheck(devicePath: String) {`
- L94: `private sealed class DiagnosticStatus {`
- L95: `data object Idle : DiagnosticStatus()`
- L96: `data object Running : DiagnosticStatus()`
- L97: `data class Result(val selfCheck: MfiSelfCheckResult) : DiagnosticStatus()`
- L98: `data class Failure(val message: String) : DiagnosticStatus()`
- L100: `fun message(): String = when (this) {`

## automotive/src/main/java/com/shilapi/xcertplay/shared/MyCarAppScreen.kt

状态：added

- L10: `class MyCarAppScreen(carContext: CarContext) : Screen(carContext) {`
- L11: `override fun onGetTemplate(): Template {`

## automotive/src/main/java/com/shilapi/xcertplay/shared/MyCarAppService.kt

状态：added

- L7: `class MyCarAppService : CarAppService() {`
- L8: `override fun createHostValidator(): HostValidator {`
- L12: `override fun onCreateSession(): Session {`

## automotive/src/main/java/com/shilapi/xcertplay/shared/MyCarAppSession.kt

状态：added

- L7: `class MyCarAppSession : Session() {`
- L8: `override fun onCreateScreen(intent: Intent): Screen {`

## automotive/src/main/java/com/shilapi/xcertplay/ui/theme/Color.kt

状态：added


## automotive/src/main/java/com/shilapi/xcertplay/ui/theme/Theme.kt

状态：added

- L26: `fun XcertplayTheme(`

## automotive/src/main/java/com/shilapi/xcertplay/ui/theme/Type.kt

状态：added


## common/build.gradle.kts

状态：modified


## common/src/main/AndroidManifest.xml

状态：modified


## common/src/main/java/com/shilapi/xcertplay/AirPlayPersistence.kt

状态：modified

- L22: `object AirPlayPersistence {`
- L91: `fun loadDisplayScaleTenths(context: Context): Int {`
- L98: `fun saveDisplayScaleTenths(context: Context, tenths: Int) {`
- L104: `fun loadHevcEnabled(context: Context): Boolean =`
- L108: `fun loadUiScalePercent(context: Context): Int = CarPlayUiScale.sanitize(`
- L113: `fun saveUiScalePercent(context: Context, percent: Int) {`
- L118: `fun saveHevcEnabled(context: Context, enabled: Boolean) {`
- L124: `fun loadHevcSoftwareDecoderEnabled(context: Context): Boolean =`
- L128: `fun saveHevcSoftwareDecoderEnabled(context: Context, enabled: Boolean) {`
- L134: `fun loadAdvancedAudioChannelMapping(context: Context): Boolean =`
- L138: `fun saveAdvancedAudioChannelMapping(context: Context, enabled: Boolean) {`
- L144: `fun loadNavigationStreamType(context: Context): Int =`
- L148: `fun saveNavigationStreamType(context: Context, streamType: Int) {`
- L154: `fun loadAudioFocusEnabled(context: Context): Boolean =`
- L158: `fun loadGeelyAudioRouting(context: Context): Boolean =`
- L161: `fun saveGeelyAudioRouting(context: Context, enabled: Boolean) {`
- L166: `fun loadGeelyNavigationAlert(context: Context): Boolean =`
- L169: `fun saveGeelyNavigationAlert(context: Context, enabled: Boolean) {`
- L174: `fun loadGeelyBluetoothDiagnosticsEnabled(context: Context): Boolean =`
- L178: `fun saveGeelyBluetoothDiagnosticsEnabled(context: Context, enabled: Boolean) {`
- L183: `fun saveAudioFocusEnabled(context: Context, enabled: Boolean) {`
- L189: `fun loadMediaAudioChannel(context: Context): Int =`
- L194: `fun saveMediaAudioChannel(context: Context, channel: Int) {`
- L200: `fun loadNavigationAudioChannel(context: Context): Int {`
- L207: `fun saveNavigationAudioChannel(context: Context, channel: Int) {`
- L213: `fun loadWirelessEnabled(context: Context): Boolean =`
- L217: `fun saveWirelessEnabled(context: Context, enabled: Boolean) {`
- L223: `fun loadMfiTarget(context: Context): MfiTarget {`
- L229: `fun saveMfiTarget(context: Context, target: MfiTarget) {`
- L235: `fun loadMfiI2cPath(context: Context): String =`
- L241: `fun saveMfiI2cPath(context: Context, path: String) {`
- L247: `fun loadRemoteMfiServer(context: Context): String =`
- L252: `fun saveRemoteMfiServer(context: Context, server: String) {`
- L258: `fun loadRemoteMfiToken(context: Context): String =`
- L263: `fun saveRemoteMfiToken(context: Context, token: String) {`
- L269: `fun loadWirelessHotspotMode(context: Context): WirelessHotspotMode {`
- L281: `fun saveWirelessHotspotMode(context: Context, mode: WirelessHotspotMode) {`
- L288: `fun loadManualHotspotSsid(context: Context): String =`
- L293: `fun saveManualHotspotSsid(context: Context, ssid: String) {`
- L299: `fun loadManualHotspotPassphrase(context: Context): String =`
- L304: `fun saveManualHotspotPassphrase(context: Context, passphrase: String) {`
- L310: `fun loadManualHotspotBand(context: Context): ManualHotspotBand {`
- L317: `fun saveManualHotspotBand(context: Context, band: ManualHotspotBand) {`
- L323: `fun loadManualHotspotChannel(context: Context): Int =`
- L328: `fun saveManualHotspotChannel(context: Context, channel: Int) {`
- L334: `fun loadManualHotspotSecurity(context: Context): ManualHotspotSecurity {`
- L345: `fun saveManualHotspotSecurity(context: Context, security: ManualHotspotSecurity) {`
- L351: `fun loadDebugLogsEnabled(context: Context): Boolean =`
- L355: `fun saveDebugLogsEnabled(context: Context, enabled: Boolean) {`
- L361: `fun loadAutoStartOnBoot(context: Context): Boolean =`
- L365: `fun saveAutoStartOnBoot(context: Context, enabled: Boolean) {`
- L371: `fun loadLocationReportingEnabled(context: Context): Boolean =`
- L375: `fun saveLocationReportingEnabled(context: Context, enabled: Boolean) {`
- L381: `fun loadManufacturer(context: Context): String =`
- L387: `fun saveManufacturer(context: Context, manufacturer: String) {`
- L393: `fun loadModel(context: Context): String =`
- L399: `fun saveModel(context: Context, model: String) {`
- L405: `fun loadOemLabel(context: Context): String =`
- L411: `fun saveOemLabel(context: Context, oemLabel: String) {`
- L417: `fun loadFps(context: Context): Int = AirPlayDisplaySettings.sanitizeFps(`
- L422: `fun loadMediaBufferMillis(context: Context): Int = com.shilapi.xcertplay.media.MediaAudioBuffer.sanitize(`
- L427: `fun saveMediaBufferMillis(context: Context, millis: Int) {`
- L432: `fun saveFps(context: Context, fps: Int) {`
- L438: `fun loadWidthPhysicalMm(context: Context): Int =`
- L446: `fun saveWidthPhysicalMm(context: Context, widthPhysicalMm: Int) {`
- L455: `fun loadPhysicalSizeBasis(context: Context): AirPlayPhysicalSizeBasis {`
- L462: `fun savePhysicalSizeBasis(context: Context, basis: AirPlayPhysicalSizeBasis) {`
- L468: `fun loadMaximumDetectedDisplay(context: Context): Pair<Int, Int> {`
- L474: `fun saveMaximumDetectedDisplay(`
- L485: `fun loadClusterMapEnabled(context: Context): Boolean =`
- L488: `fun saveClusterMapEnabled(context: Context, enabled: Boolean) {`
- L493: `fun loadCenterMapOverlay(context: Context): Boolean =`
- L497: `fun loadLauncherMapSharing(context: Context): Boolean =`
- L500: `fun saveLauncherMapSharing(context: Context, enabled: Boolean) {`
- L505: `internal fun observeLauncherMapSharing(context: Context, changed: (Boolean) -> Unit): () -> Unit {`
- L514: `fun saveCenterMapOverlay(context: Context, enabled: Boolean) {`
- L518: `fun loadClusterContent(context: Context): CarPlayClusterDisplay.Content =`
- L523: `fun saveClusterContent(context: Context, content: CarPlayClusterDisplay.Content) {`
- L527: `fun loadCenterMapFollowsDashboard(context: Context): Boolean =`
- L531: `fun saveCenterMapFollowsDashboard(context: Context, enabled: Boolean) {`
- L536: `fun loadClusterMapScalePercent(context: Context): Int = CarPlayClusterDisplay.STREAM_SCALE_PERCENT.let { default ->`
- L541: `fun saveClusterMapScalePercent(context: Context, percent: Int) {`
- L546: `fun loadClusterMarkerHorizontalStep(context: Context): Int =`
- L550: `fun saveClusterMarkerHorizontalStep(context: Context, step: Int) {`
- L555: `fun loadClusterMarkerVerticalStep(context: Context): Int =`
- L559: `fun saveClusterMarkerVerticalStep(context: Context, step: Int) {`
- L564: `fun loadRightHandDrive(context: Context): Boolean =`
- L568: `fun saveRightHandDrive(context: Context, rightHandDrive: Boolean) {`
- L574: `fun loadHideTopBar(context: Context): Boolean =`
- L578: `fun saveHideTopBar(context: Context, hide: Boolean) {`
- L584: `fun loadHideBottomBar(context: Context): Boolean =`
- L588: `fun saveHideBottomBar(context: Context, hide: Boolean) {`
- L594: `fun loadSafeAreaDrawOutside(context: Context): Boolean =`
- L598: `fun saveSafeAreaDrawOutside(context: Context, drawOutside: Boolean) {`
- L604: `fun loadSafeAreaRect(context: Context, widthPixels: Int, heightPixels: Int): SafeAreaRect? {`
- L612: `fun saveSafeAreaRect(`
- L630: `fun clearSafeAreaRect(`
- L644: `fun loadCustomAirPlayIconFile(context: Context): File? =`
- L647: `fun saveCustomAirPlayIcon(context: Context, encodedImage: ByteArray) {`
- L654: `fun clearCustomAirPlayIcon(context: Context) {`
- L658: `fun loadIdentity(context: Context): AirPlayIdentity {`
- L675: `fun loadPairings(context: Context, onSave: (String, ByteArray) -> Unit): PairingStore {`
- L684: `fun savePairing(context: Context, identifier: String, longTermPublicKey: ByteArray) {`
- L694: `fun loadLockdownRecord(context: Context): LockdownPairRecord? {`
- L722: `fun saveLockdownRecord(context: Context, record: LockdownPairRecord) {`
- L736: `fun clearLockdownRecord(context: Context) {`
- L750: `private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }`
- L752: `private fun String.decodeHex(): ByteArray {`
- L759: `private fun safeAreaKey(widthPixels: Int, heightPixels: Int): String =`

## common/src/main/java/com/shilapi/xcertplay/AndroidBluetoothFailureCopy.kt

状态：added

- L6: `internal object AndroidBluetoothFailureCopy {`
- L7: `fun forControllerMessage(context: Context, message: String): String? = when (message) {`

## common/src/main/java/com/shilapi/xcertplay/AudioChannelPreview.kt

状态：modified

- L22: `internal class AudioChannelPreview(private val context: Context? = null, private val onUnavailable: (Int) -> Unit) : Closeable {`
- L32: `fun play(channel: Int, navigation: Boolean, geelyFocusGain: Int? = null) {`
- L130: `override fun close() {`
- L138: `private fun tone(): ByteArray {`

## common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt

状态：modified

- L104: `class CarPlayHostActivity : ComponentActivity() {`
- L105: `private data class SettingsBaseline(`
- L117: `override fun attachBaseContext(newBase: Context) {`
- L122: `private fun createRuntimeConfig(): CarPlayRuntimeConfig = CarPlayRuntimeConfig(`
- L371: `override fun run() {`
- L383: `override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {`
- L404: `override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {`
- L409: `override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {`
- L422: `override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit`
- L425: `override fun onCreate(savedInstanceState: Bundle?) {`
- L454: `override fun handleOnBackPressed() {`
- L481: `private fun loadPersistedSettings() {`
- L527: `private fun requestStartupPrerequisites() {`
- L539: `private fun requestLocationPermission() {`
- L550: `private fun hasFineLocationPermission(): Boolean =`
- L554: `private fun requestVpnConsent() {`
- L565: `private fun requestWirelessPermissions() {`
- L579: `private fun hasRequiredWirelessPermissions(): Boolean =`
- L585: `private fun startForegroundServiceCompat(intent: Intent) {`
- L593: `private fun requiredWirelessPermissions(): List<String> = when {`
- L609: `override fun onNewIntent(intent: Intent) {`
- L628: `override fun onStart() {`
- L637: `override fun onResume() {`
- L666: `private fun syncTransport(): Boolean {`
- L706: `private fun reloadGeelyAudioSettingsIfChanged(): Boolean {`
- L715: `private fun reloadHotspotSettingsIfChanged(): Boolean {`
- L736: `private fun effectiveClusterTheme(): DiLink51ClusterLayout.Theme =`
- L740: `private fun onClusterActivityState(state: ClusterActivityState.Snapshot) {`
- L747: `private fun ensureClusterPresentation() {`
- L783: `private fun ensureDiLink51ClusterPresentation(theme: DiLink51ClusterLayout.Theme) {`
- L820: `private fun dismissClusterPresentation() {`
- L830: `private fun onClusterSurface(surface: Surface?) {`
- L842: `private fun clusterDisplayConfig(): AirPlayDisplayConfig? {`
- L867: `override fun dispatchKeyEvent(event: KeyEvent): Boolean {`
- L876: `override fun onWindowFocusChanged(hasFocus: Boolean) {`
- L884: `override fun onStop() {`
- L892: `private fun showCenterMap() {`
- L922: `private fun onHomeScreenVisible(visible: Boolean) {`
- L928: `private fun onCenterMapSurface(surface: Surface?) {`
- L934: `private fun updateClusterMapShown() {`
- L938: `override fun onConfigurationChanged(newConfig: Configuration) {`
- L950: `override fun onDestroy() {`
- L978: `private fun buildContentView(): View {`
- L1041: `private fun buildSettingsMenu(): View {`
- L1321: `override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {`
- L1328: `override fun onStartTrackingTouch(seekBar: SeekBar) = Unit`
- L1329: `override fun onStopTrackingTouch(seekBar: SeekBar) = Unit`
- L1666: `private fun persistMenuSettings() {`
- L1699: `private fun captureSettingsBaseline(): SettingsBaseline {`
- L1716: `private fun restoreSettingsBaseline() {`
- L1756: `private fun buildMfiTargetSection(): View {`
- L1880: `private fun updateMfiTargetFields() {`
- L1886: `private fun syncMfiSettingsControls() {`
- L1906: `private fun mfiTargetLabel(target: MfiTarget): String = when (target) {`
- L1913: `private fun buildIdentitySettingsSection(): View {`
- L1950: `private fun settingsCategoryHeader(title: String): TextView =`
- L1953: `private fun buildLocationReportingSection(): View =`
- L2000: `private fun onLocationReportingChanged(checked: Boolean) {`
- L2013: `private fun buildDebugLogsSection(): View =`
- L2024: `private fun buildStepSliderSection(`
- L2066: `override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {`
- L2072: `override fun onStartTrackingTouch(seekBar: SeekBar) = Unit`
- L2073: `override fun onStopTrackingTouch(seekBar: SeekBar) = Unit`
- L2087: `private fun buildAirPlayIconSection(): View {`
- L2174: `private fun buildDrivingSideSection(): View {`
- L2216: `private fun buildFullscreenSection(): View {`
- L2260: `private fun buildSafeAreaSection(): View {`
- L2326: `private fun buildSafeAreaEditor(): View {`
- L2384: `private fun settingsInputRow(`
- L2430: `private fun settingsSwitchRow(`
- L2456: `private fun afterTextChanged(onChanged: (String) -> Unit): TextWatcher =`
- L2458: `override fun beforeTextChanged(`
- L2465: `override fun onTextChanged(`
- L2472: `override fun afterTextChanged(text: Editable?) {`
- L2477: `private fun buildHotspotModeSection(): View {`
- L2649: `private fun updateManualHotspotFields() {`
- L2655: `private fun validateMfiSettings(): Boolean {`
- L2675: `private fun validateManualHotspotSettings(): Boolean {`
- L2700: `private fun hotspotModeLabel(mode: WirelessHotspotMode): String = when (mode) {`
- L2706: `private fun menuText(`
- L2719: `private fun updateHotspotStatus(status: CarPlayStatus) {`
- L2746: `private fun updateHotspotStatusBlock() {`
- L2816: `private fun updateResolutionMenu() {`
- L2882: `private data class CanvasSupport(val supported: Boolean, val reason: String, val details: String)`
- L2884: `private fun largerCanvasSupport(display: AirPlayDisplayConfig): CanvasSupport = try {`
- L2926: `private fun createAirPlayConfig(size: DisplaySize): AirPlayConfig {`
- L2997: `private fun loadAirPlayIcon(): AirPlayIcon {`
- L3011: `private fun decodeAirPlayIcon(encoded: ByteArray): AirPlayIcon? {`
- L3022: `private fun defaultAirPlayIconBytes(): ByteArray =`
- L3026: `private fun updateAirPlayIconPreview() {`
- L3042: `private fun currentActivitySize(): DisplaySize? {`
- L3050: `private fun resolvePhysicalSize(size: DisplaySize): AirPlayPhysicalSizeMm =`
- L3060: `private fun safeAreaSummary(): String {`
- L3073: `private fun updateSafeAreaSummary() {`
- L3077: `private fun openSafeAreaEditor() {`
- L3098: `private fun closeSafeAreaEditor() {`
- L3109: `private fun saveSafeAreaEditor() {`
- L3120: `private fun resetSafeAreaForCurrentSize() {`
- L3132: `private fun refreshDisplaySizeAfterLayout() {`
- L3139: `private fun normalizedManufacturer(): String =`
- L3142: `private fun normalizedModel(): String =`
- L3145: `private fun createMediaSink(`
- L3182: `private fun createMediaEngine(sink: AndroidMediaSink): CarPlayMediaEngine =`
- L3189: `private fun createSessionListener(controllerGeneration: Int): AirPlaySessionListener =`
- L3193: `override fun onSessionActive(session: AirPlaySession) {`
- L3207: `override fun onSessionEnded(session: AirPlaySession) {`
- L3221: `override fun onTransportError(message: String) {`
- L3233: `override fun onDebugLog(message: String) {`
- L3255: `private fun createStatusReporter(`
- L3274: `private fun adoptBackgroundSession(): Boolean {`
- L3324: `private fun startCarPlay(size: DisplaySize) {`
- L3418: `private fun refreshConfiguration(newConfig: Configuration = resources.configuration) {`
- L3429: `private fun syncAirPlayDarkMode() {`
- L3445: `private fun audioCaptureDirectory(): File? {`
- L3450: `private fun scheduleDisplaySize(width: Int, height: Int) {`
- L3463: `private fun applyDisplaySize(size: DisplaySize) {`
- L3496: `private fun displayRotation(): Int = videoView?.display?.rotation ?: windowManager.defaultDisplay.rotation`
- L3498: `private fun displayLayoutChanged(): Boolean {`
- L3505: `private fun contentRect(viewWidth: Int, viewHeight: Int): CarPlayVideoLayout {`
- L3510: `private fun updateVideoLayout(viewWidth: Int, viewHeight: Int) {`
- L3520: `private fun recordDetectedMaximum(size: DisplaySize) {`
- L3529: `private fun maybeStartCarPlay() {`
- L3553: `private fun reconnectAfterLoss(reason: String) {`
- L3584: `private fun restartCarPlay(reason: String) {`
- L3622: `private fun showDiPlayHome(page: String = "home") {`
- L3628: `private fun openSettingsMenu() = showDiPlayHome("settings")`
- L3630: `private fun saveSettingsAndReconnect() {`
- L3642: `private fun cancelSettingsEdits() {`
- L3648: `private fun finishSettingsMenu(prefix: String, restartForAudioChange: Boolean = false) {`
- L3672: `private fun exitApplication() {`
- L3679: `private fun shutdown(terminateProcess: Boolean, reason: String, completion: () -> Unit = {}) {`
- L3707: `private fun attachSurface(surface: Surface) {`
- L3716: `private fun onHostTouch(view: View, event: MotionEvent): Boolean {`
- L3792: `private fun pointerCentroid(event: MotionEvent, horizontal: Boolean): Float {`
- L3800: `private fun onScreenStreamStateChanged(generation: Int, type: Int, active: Boolean) {`
- L3824: `private fun setStatus(message: String) {`
- L3832: `private fun motionEventName(action: Int): String =`
- L3836: `private fun setConnectionStage(message: String) {`
- L3842: `private fun updateDebugOverlays() {`
- L3847: `private fun friendlyStage(message: String): String = when {`
- L3865: `private fun appendLog(message: String) {`
- L3870: `private fun appendFileLog(message: String) {`
- L3874: `private fun formattedLogLine(message: String, nowMillis: Long): String =`
- L3877: `private fun initializeSessionLog() {`
- L3890: `private fun refreshLogView(nowMillis: Long) {`
- L3906: `private fun scrollLogsToBottom() {`
- L3912: `private fun applyFullscreenMode() {`
- L3932: `private fun Switch.applyMenuSwitchTints() {`
- L3947: `private fun SeekBar.applyMenuSeekBarTints() {`
- L3954: `private fun View.tintBackgroundCompat(color: Int) {`
- L3960: `private fun RadioButton.tintRadioCompat() {`
- L3969: `private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()`
- L3971: `private fun CarPlayStatus.describe(): String = when (this) {`
- L4029: `private data class DisplaySize(val width: Int, val height: Int)`
- L4030: `private data class LogEntry(val timestampMillis: Long, val text: String)`
- L4031: `private data class HotspotStatus(`
- L4040: `internal data class CarPlaySessionDisplay(`
- L4052: `internal object CarPlayBackgroundSession {`
- L4057: `@Synchronized fun isOwner(candidate: Any): Boolean = owner === candidate`
- L4058: `@Synchronized fun hasSession(): Boolean = stopAction != null || stopping`
- L4061: `fun stop(completion: () -> Unit = {}) {`
- L4078: `data class Snapshot(`
- L4093: `fun snapshot(): Snapshot? {`
- L4101: `fun store(controller: CarPlayController, sink: AndroidMediaSink, width: Int, height: Int,`
- L4113: `fun clear(expected: CarPlayController? = null, keepOwner: Boolean = false) {`

## common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt

状态：modified

- L26: `internal object CarPlayMediaKeys {`
- L41: `/** API 21+ implementation is isolated in ModernSession so this object loads on API 18. */`
- L60: `fun attach(context: Context, next: CarPlayController) {`
- L74: `fun detach(expected: CarPlayController?) {`
- L84: `fun onMediaAudioChanged(active: Boolean) {`
- L88: `private fun onIphonePlaying(expected: CarPlayController, playing: Boolean) {`
- L99: `private fun onNowPlayingChanged(expected: CarPlayController, update: CarPlayNowPlaying) {`
- L117: `private fun onArtworkChanged(expected: CarPlayController, id: Int, bytes: ByteArray) {`
- L123: `private fun onArtworkDecoded(expected: Any, id: Int, decoded: Bitmap?) {`
- L137: `private fun updateLocked(active: Boolean) {`
- L153: `private fun registerLegacyMediaButtons(context: Context) {`
- L156: `override fun onReceive(receiverContext: Context?, intent: Intent?) {`
- L190: `private fun unregisterLegacyMediaButtons() {`
- L206: `private fun regainFocusLocked() {`
- L225: `private fun publishPlaybackStateLocked() {`
- L241: `private fun releaseLocked() {`
- L259: `private fun send(index: Int, source: String) {`
- L268: `private fun decodeArtwork(bytes: ByteArray): Bitmap? {`

## common/src/main/java/com/shilapi/xcertplay/CarPlayVideo.kt

状态：modified

- L27: `internal object CarPlayVideo : CarPlayVideoListener {`
- L50: `// Avoid a direct Activity/Media3 type here: this state object is loaded by the API-18 host.`
- L53: `fun attach(context: Context, next: CarPlayController) {`
- L59: `override fun readParked(): Boolean? = appContext?.let(BydNavigationOutputs::parked)`
- L61: `override fun onVideoAllowedChanged(allowed: Boolean) {`
- L65: `override fun onVideoSessionEnded() {`
- L72: `override fun onVideoUiRequested() {`
- L76: `override fun onVideoMessage(streamId: Long, message: Map<String, Any?>) {`
- L85: `fun onMediaKey(index: Int): Boolean {`
- L96: `fun setPlaying(next: Boolean) {`
- L106: `fun onPlayerFailed(code: Int) {`
- L114: `fun onPlayerClosed(positionMillis: Int?) {`
- L119: `private fun handle(streamId: Long, message: Map<String, Any?>) {`
- L167: `class LoadedUrl(val status: Int?, val data: ByteArray?, val location: String?)`
- L177: `fun resolveOnIphone(url: String): LoadedUrl? {`
- L210: `private fun onUrlLoaded(message: Map<String, Any?>) {`
- L218: `private fun playerState(): VideoInCar.PlayerState? =`
- L221: `private fun reply(streamId: Long, message: Map<String, Any?>) {`
- L232: `private fun show() {`
- L249: `private fun closePlayer(reason: String) {`
- L255: `private fun stop() {`
- L264: `private class UrlAnswer {`
- L268: `fun complete(result: Map<*, *>) {`
- L273: `fun await(timeout: Long, unit: TimeUnit): Map<*, *>? =`

## common/src/main/java/com/shilapi/xcertplay/CarPlayVideoActivity.kt

状态：modified

- L53: `class CarPlayVideoActivity : Activity(), CarPlayVideoActivityBridge {`
- L70: `override fun run() {`
- L76: `override fun onCreate(savedInstanceState: Bundle?) {`
- L87: `override fun onPlaybackStateChanged(state: Int) {`
- L94: `override fun onTimelineChanged(timeline: Timeline, reason: Int) = logEncryption()`
- L96: `override fun onPlayerError(error: PlaybackException) {`
- L124: `private fun controlBar(): View {`
- L136: `fun round(icon: Int, label: String, onClick: () -> Unit) = ImageView(this).apply {`
- L155: `fun timeText() = TextView(this).apply {`
- L174: `override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {`
- L178: `override fun onStartTrackingTouch(bar: SeekBar) {`
- L183: `override fun onStopTrackingTouch(bar: SeekBar) {`
- L214: `private fun showControls() {`
- L224: `private fun updatePlayPause() {`
- L231: `private fun updateTime() {`
- L248: `private fun time(millis: Long): String {`
- L254: `override fun state(): VideoInCar.PlayerState {`
- L265: `override fun load() {`
- L280: `override fun applyRate() {`
- L286: `override fun skip(deltaMillis: Int) {`
- L292: `override fun applySeek() {`
- L298: `override fun requestFinish() = finish()`
- L301: `private fun logEncryption() {`
- L316: `private fun causes(error: Throwable): String = generateSequence(error) { it.cause }.take(4)`
- L319: `override fun onDestroy() {`
- L330: `private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()`

## common/src/main/java/com/shilapi/xcertplay/CarPlayVideoActivityBridge.kt

状态：added

- L5: `/** API-18-safe seam between the host state object and the API-23 Media3 Activity. */`
- L6: `internal interface CarPlayVideoActivityBridge {`
- L7: `fun load()`
- L8: `fun applyRate()`
- L9: `fun skip(deltaMillis: Int)`
- L10: `fun applySeek()`
- L11: `fun state(): VideoInCar.PlayerState`
- L12: `fun requestFinish()`

## common/src/main/java/com/shilapi/xcertplay/CenterMapOverlay.kt

状态：modified

- L31: `internal object CenterMapOverlay {`
- L46: `fun permitted(context: Context): Boolean =`
- L57: `fun scheduleShow() {`
- L63: `fun onDiPlayScreenShown() {`
- L74: `fun show(`
- L117: `override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {`
- L122: `override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) = Unit`
- L123: `override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit`
- L125: `override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {`
- L138: `// ViewOutlineProvider and clipToOutline are API 21. Keep the provider class out of`
- L165: `fun beginPinch(event: MotionEvent, liftedIndex: Int = -1) {`
- L255: `fun hide() {`
- L263: `fun diPlayInFront(): Boolean {`

## common/src/main/java/com/shilapi/xcertplay/ClusterMapPresentation.kt

状态：modified

- L37: `internal class ClusterMapPresentation(`
- L47: `override fun onCreate(savedInstanceState: Bundle?) {`
- L67: `fun transform() {`
- L75: `override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {`
- L80: `override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = transform()`
- L81: `override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit`
- L82: `override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {`
- L96: `override fun surfaceCreated(holder: SurfaceHolder) {`
- L101: `override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {`
- L105: `override fun surfaceDestroyed(holder: SurfaceHolder) {`
- L133: `fun setStreamActive(active: Boolean) {`
- L138: `fun setMapVisible(visible: Boolean) {`
- L149: `fun findDisplay(context: Context, theme: DiLink51ClusterLayout.Theme = DiLink51ClusterLayout.theme(context)): Display? {`
- L163: `fun describeDisplays(context: Context): String =`
- L167: `fun sizeOf(display: Display): Point = Point().also {`
- L175: `private class InstrumentContrastView(`
- L181: `override fun onDraw(canvas: Canvas) {`
- L185: `fun fade(x0: Float, y0: Float, x1: Float, y1: Float, start: Int, end: Int,`

## common/src/main/java/com/shilapi/xcertplay/DiLink51ClusterMonitor.kt

状态：modified

- L16: `* must be able to load this class in order to report "cluster map unavailable" rather than failing`
- L32: `* DiLink51ClusterMonitor would fail class verification there. Everything that touches them lives in`
- L37: `private class ModernUsage(context: Context) {`
- L47: `data class Event(`
- L56: `fun hasAccess(): Boolean = runCatching {`
- L71: `fun eventsSince(since: Long, now: Long): List<Event> = runCatching {`
- L107: `internal class DiLink51ClusterMonitor(context: Context, private val onState: (ClusterActivityState.Snapshot) -> Unit) {`
- L118: `private data class EventKey(val pkg: String?, val name: String?, val id: Int, val type: Int, val time: Long)`
- L120: `fun start() {`
- L125: `fun stop() {`
- L131: `private fun bootTime() = (System.currentTimeMillis() - SystemClock.elapsedRealtime()).coerceAtLeast(0)`
- L133: `private fun poll() {`
- L182: `fun hasAccess(context: Context): Boolean {`

## common/src/main/java/com/shilapi/xcertplay/DiPlayActivity.kt

状态：modified

- L55: `class DiPlayActivity : ComponentActivity() {`
- L80: `override fun run() { refreshStatus(); handler.postDelayed(this, 1000) }`
- L97: `override fun attachBaseContext(newBase: Context) {`
- L101: `override fun onCreate(savedInstanceState: Bundle?) {`
- L123: `override fun handleOnBackPressed() {`
- L130: `override fun onNewIntent(intent: Intent) {`
- L135: `override fun onSaveInstanceState(outState: Bundle) { outState.putString("page", page); outState.putBoolean("pending_car_hotspot", pendingCarHotspotSetup); super.onSaveInstanceState(outState) }`
- L136: `override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); render() }`
- L137: `private fun openOverlayPermission() {`
- L144: `override fun onStart() {`
- L149: `override fun onStop() {`
- L154: `override fun onResume() {`
- L171: `override fun onPause() { handler.removeCallbacks(tick); super.onPause() }`
- L173: `override fun onDestroy() {`
- L178: `private fun render() {`
- L202: `private fun home(content: LinearLayout) {`
- L252: `fun columns(first: View, second: View, stretchSecond: Boolean = false) = row().apply {`
- L274: `private fun settings(content: LinearLayout) {`
- L530: `private fun about(content: LinearLayout) {`
- L542: `private fun carHotspotOff(): Boolean =`
- L546: `private fun carHotspotOffDialog() {`
- L557: `private fun openCarWifiSettings() {`
- L571: `private fun openCarClientWifiSettings() {`
- L579: `private fun connectionSetup(content: LinearLayout) {`
- L600: `private fun wirelessLinkControls(parent: LinearLayout) {`
- L645: `private fun mediaChannelControl(parent: LinearLayout) {`
- L662: `private fun geelyAudioControl(parent: LinearLayout) {`
- L666: `fun summary(): String = getString(R.string.geely_audio_profile) + " · " + getString(`
- L709: `private fun navigationChannelControl(parent: LinearLayout) {`
- L729: `private fun showChannelDialog(title: String, current: Int, navigation: Boolean, onApply: (Int) -> Unit) {`
- L749: `private fun applyMediaChannel(value: Int, previous: Int, control: Button, summary: (Int) -> String) {`
- L756: `private fun applyNavigationChannel(value: Int, previous: Int, control: Button, summary: (Int) -> String) {`
- L763: `private fun channelLabel(value: Int): String = value.toString()`
- L765: `private fun storedSsid() = AirPlayPersistence.loadManualHotspotSsid(this)`
- L766: `private fun storedPassword() = AirPlayPersistence.loadManualHotspotPassphrase(this)`
- L767: `private fun hotspotError(ssid: String, password: String) =`
- L770: `private fun saveHotspotCredentials(ssid: String, password: String) {`
- L779: `private fun askHotspotCredentials(done: (String, String) -> Unit) {`
- L789: `fun hideKeyboard() {`
- L833: `private fun markerStepLabel(step: Int, negative: String, positive: String): String = when {`
- L839: `private fun showClusterAccessSetup() {`
- L881: `private fun checkAdbAccess(mayAsk: Boolean, reconnectWhenReady: Boolean = false) {`
- L900: `private fun adbStatusText(result: BydAdbAccess.Status?): String = when (result?.state) {`
- L919: `private fun hasPreciseLocation() =`
- L923: `private fun reconnectForLocation() {`
- L929: `private fun reconnectForClusterMap() {`
- L933: `private fun applyWirelessLink(mode: WirelessHotspotMode) {`
- L939: `private fun textInput(title: String, current: String, secret: Boolean, save: (String) -> Unit) {`
- L954: `private fun carPlaySizeControl(parent: LinearLayout) {`
- L965: `private fun connect(wireless: Boolean) {`
- L994: `private fun openProjection(wireless: Boolean = AirPlayPersistence.loadWirelessEnabled(this)) {`
- L1001: `private fun choosePhone() {`
- L1031: `private fun showBluetoothStackStatus(adapterPresent: Boolean, adapterEnabled: Boolean, manual: Boolean = false) {`
- L1145: `private fun showAndroidBluetoothUnavailable(adapterPresent: Boolean) {`
- L1159: `private fun cancelBluetoothStatusProbe() {`
- L1168: `private fun copyBluetoothDiagnosticSummary() {`
- L1182: `private fun wirelessHelp() {`
- L1191: `private fun handleWirelessRecovery() {`
- L1197: `private fun confirmWirelessReset() {`
- L1206: `private fun closeP2pChannel(channel: android.net.wifi.p2p.WifiP2pManager.Channel?) {`
- L1211: `private fun resetWirelessGroup() {`
- L1219: `override fun onSuccess() {`
- L1221: `fun waitUntilRemoved() {`
- L1234: `override fun onFailure(reason: Int) { closeP2pChannel(channel); toast(getString(R.string.could_not_reset_wi_fi_direct_close_the_other_projection_ap)) }`
- L1242: `private fun refreshStatus() {`
- L1259: `private fun reportFileName() = "DiPlay-${SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())}.txt"`
- L1261: `private fun chooseReportDestination() {`
- L1280: `private fun exportDiagnostics(uri: Uri? = null) {`
- L1355: `private fun permissionHelp(title: String, body: String) {`
- L1360: `private fun openSystem(intent: Intent) { runCatching { startActivity(intent) }.onFailure { toast(getString(R.string.open_this_setting_from_your_car_s_settings_app)) } }`
- L1361: `private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }`
- L1363: `private fun playTestTone(streamType: Int) {`
- L1400: `private fun ImageView.tintCompat(color: Int) {`
- L1404: `private fun Switch.tintButtonCompat(color: Int) {`
- L1408: `private fun paintChannel(index: Int, selected: Boolean) {`
- L1415: `private fun channelSelector(): ViewGroup {`
- L1455: `private fun version() = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0-beta.1"`
- L1456: `private fun languageSettings(content: LinearLayout) {`
- L1466: `private fun section(parent: LinearLayout, title: String, icon: Int? = null, build: (LinearLayout) -> Unit) {`
- L1478: `private fun toggle(parent: LinearLayout, title: String, description: String, value: Boolean, save: (Boolean) -> Unit) {`
- L1485: `private fun choice(parent: LinearLayout, title: String, options: List<String>, current: Int, reconnects: Boolean = true, save: (Int) -> Unit) {`
- L1505: `private fun card() = column().apply { background = rounded(SURFACE, BORDER); setPadding(dp(24), dp(24), dp(24), dp(24)) }`
- L1506: `private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(-1, -2) }`
- L1507: `private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(-1, -2) }`
- L1511: `private fun ripple(content: android.graphics.drawable.Drawable): android.graphics.drawable.Drawable =`
- L1517: `private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {`
- L1522: `private fun button(title: String, primary: Boolean, click: () -> Unit) = Button(this).apply {`
- L1530: `private fun rounded(color: Int, stroke: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(20).toFloat(); setStroke(dp(1), stroke) }`
- L1531: `private fun matchButton(top: Int = 0, height: Int = 68) = LinearLayout.LayoutParams(-1, dp(height)).apply { topMargin = dp(top) }`
- L1532: `private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }`
- L1533: `private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()`

## common/src/main/java/com/shilapi/xcertplay/DiPlayBluetooth.kt

状态：modified

- L8: `internal object DiPlayBluetooth {`
- L9: `fun localAddress(context: Context): String? {`

## common/src/main/java/com/shilapi/xcertplay/DiPlayBootstrap.kt

状态：modified

- L12: `internal object DiPlayBootstrap {`
- L15: `@Synchronized fun ensure(context: Context) {`
- L52: `fun deviceId(identity: AirPlayIdentity): String {`
- L59: `internal object DiPlayPreferences {`
- L60: `private fun prefs(context: Context) = context.getSharedPreferences("diplay", Context.MODE_PRIVATE)`
- L61: `fun phoneAddress(context: Context): String? = prefs(context).getString("phone_address", null)`
- L62: `fun phoneName(context: Context): String = prefs(context).getString("phone_name", null) ?: "Your iPhone"`
- L63: `fun savePhone(context: Context, address: String, name: String) {`
- L66: `fun autoConnect(context: Context) = prefs(context).getBoolean("auto_connect", false)`
- L67: `fun saveAutoConnect(context: Context, value: Boolean) {`

## common/src/main/java/com/shilapi/xcertplay/DiPlaySessionService.kt

状态：modified

- L18: `class DiPlaySessionService : Service() {`
- L19: `override fun onBind(intent: Intent?): IBinder? = null`
- L20: `override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {`
- L53: `override fun onTaskRemoved(rootIntent: Intent?) {`
- L60: `private fun notificationBuilder(manager: NotificationManager): Notification.Builder {`

## common/src/main/java/com/shilapi/xcertplay/GeelyBluetoothDiagnosticSnapshotStore.kt

状态：added

- L9: `internal object GeelyBluetoothDiagnosticSnapshotStore {`
- L12: `fun save(context: Context, androidAdapterState: String, snapshot: GeelyBluetoothSnapshot) {`
- L32: `fun report(context: Context, optInEnabled: Boolean): String {`

## common/src/main/java/com/shilapi/xcertplay/GeelyBluetoothDiagnosticsOptIn.kt

状态：added

- L6: `internal object GeelyBluetoothDiagnosticsOptIn {`
- L7: `fun enabled(context: Context): Boolean = AirPlayPersistence.loadGeelyBluetoothDiagnosticsEnabled(context)`

## common/src/main/java/com/shilapi/xcertplay/HomeScreenMonitor.kt

状态：modified

- L20: `internal class HomeScreenMonitor(context: Context, private val onChange: (Boolean) -> Unit) {`
- L34: `fun start() {`
- L52: `fun stop() {`
- L58: `private fun poll() {`
- L61: `// class so Android 4.3 can load it and simply never see a home-screen change.`
- L82: `private fun homeScreenObservable(): Boolean =`
- L92: `fun hasAccess(context: Context): Boolean = DiLink51ClusterMonitor.hasAccess(context)`
- L95: `fun defaultHome(context: Context): String? = runCatching {`
- L110: `private class ModernUsageEvents(appContext: Context) {`
- L112: `data class Resumed(val packageName: String?, val timeStamp: Long)`
- L114: `fun resumedSince(since: Long, now: Long): List<Resumed> = runCatching {`

## common/src/main/java/com/shilapi/xcertplay/HotspotUiLabels.kt

状态：added

- L7: `internal fun Context.hotspotBackendLabel(value: String): String = when (value) {`
- L14: `internal fun Context.hotspotBandLabel(value: String): String = when (value) {`

## common/src/main/java/com/shilapi/xcertplay/MapEmbedService.kt

状态：modified

- L36: `class MapEmbedService : Service() {`
- L44: `override fun onCreate() {`
- L54: `override fun onBind(intent: Intent): IBinder = messenger.binder`
- L56: `override fun onDestroy() {`
- L65: `private fun revokeSharing() {`
- L72: `private fun handle(message: Message) {`
- L87: `private fun safeSendingUid(message: Message): String = runCatching {`
- L91: `private fun attach(client: Messenger, caller: String, data: Bundle) {`
- L124: `private fun refuse(client: Messenger, caller: String, error: String) {`
- L129: `private fun send(client: Messenger, what: Int, data: Bundle) {`
- L137: `private fun releaseEmbed(embed: Embed?) {`
- L143: `private inner class Embed(`
- L163: `override fun onSurfaceTextureAvailable(texture: SurfaceTexture, w: Int, h: Int) {`
- L169: `override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, w: Int, h: Int) = cropToFill(this@apply, w, h)`
- L170: `override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit`
- L172: `override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {`
- L203: `fun resize(width: Int, height: Int) {`
- L207: `fun release() {`
- L215: `fun sharingDisabled() {`
- L251: `internal fun cropToFill(view: TextureView, width: Int, height: Int) {`

## common/src/main/java/com/shilapi/xcertplay/ModernSession.kt

状态：added

- L22: `internal class ModernSession(`
- L52: `fun setMetadata(info: CarPlayNowPlaying, artwork: Bitmap?) {`
- L56: `fun setPlaybackState(playing: Boolean, positionMillis: Long?, updatedAt: Long) {`
- L71: `fun regainFocus() {`
- L84: `fun close() {`
- L100: `internal fun androidMetadata(info: CarPlayNowPlaying, artwork: Bitmap?): MediaMetadata =`
- L123: `internal class CarPlayMediaCallback(private val send: (Int, String) -> Unit) : MediaSession.Callback() {`
- L124: `override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {`
- L134: `override fun onPlay() = send(CarPlayMediaButton.PLAY, "play")`
- L135: `override fun onPause() = send(CarPlayMediaButton.PAUSE, "pause")`
- L136: `override fun onSkipToNext() = send(CarPlayMediaButton.NEXT, "next")`
- L137: `override fun onSkipToPrevious() = send(CarPlayMediaButton.PREVIOUS, "previous")`

## common/src/main/java/com/shilapi/xcertplay/NavigationWidget.kt

状态：modified

- L24: `class NavigationWidget : AppWidgetProvider() {`
- L25: `override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {`
- L32: `internal object NavigationWidgetUpdater {`
- L53: `fun attach(appContext: Context) {`
- L60: `private fun schedule() {`
- L67: `fun views(context: Context, glance: CarPlayGlance.Snapshot): RemoteViews {`
- L111: `fun arrow(type: Int, drivingSide: Int): Int = when (type) {`
- L124: `private fun distance(context: Context, meters: Long): String = when {`
- L130: `private fun duration(context: Context, seconds: Long): String {`

## common/src/main/java/com/shilapi/xcertplay/RoundedClipCompat.kt

状态：added

- L11: `* this isolated class so Android 4.3 can load CenterMapOverlay and use its rectangular fallback.`
- L13: `internal object RoundedClipCompat {`
- L18: `fun apply(view: View, radius: Float) {`
- L20: `override fun getOutline(target: View, outline: Outline) {`

## common/src/main/res/values/bools.xml

状态：added


## common/src/main/res/values/strings.xml

状态：modified

- L279: `<string name="receiver_based_on_xcertplay_licensed_under_gpl_3_0_diplay">Receiver based on xcertplay, licensed under GPL-3.0. DiPlay’s interface follows DiAuto’s design, licensed under AGPL-3.0.\n\nIncludes AndroidX, Bouncy Castle, JmDNS and SLF4J. Source and license notices accompany this release.\n\nCarPlay and the CarPlay icon belong to Apple Inc. DiPlay is an independent project.</string>`

## common/src/main/res/values/themes.xml

状态：modified


## common/src/main/res/values-v21/themes.xml

状态：added


## common/src/main/res/values-v23/bools.xml

状态：added


## common/src/main/res/values-v23/themes.xml

状态：added


## common/src/main/res/values-zh-rCN/strings.xml

状态：modified


## common/src/test/java/com/shilapi/xcertplay/CarPlayMediaCallbackTest.kt

状态：modified

- L17: `class CarPlayMediaCallbackTest {`
- L22: `fun controllerPlayAndPauseAreExplicit() {`
- L35: `fun hardwarePlayAndPauseKeysToggle() {`
- L44: `fun aHeldKeySendsOnePress() {`
- L52: `fun nowPlayingFieldsBecomeAndroidMediaMetadata() {`
- L76: `private fun press(keyCode: Int, repeat: Int = 0) {`
- L82: `private fun button(event: KeyEvent) = Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(Intent.EXTRA_KEY_EVENT, event)`

## common/src/test/java/com/shilapi/xcertplay/GeelyBluetoothDiagnosticsOptInTest.kt

状态：added

- L16: `class GeelyBluetoothDiagnosticsOptInTest {`
- L19: `@Before fun clearPreferences() {`
- L24: `@Test fun vendorCallsAreGatedByPersistedOptInAndDefaultOff() {`
- L41: `@Test fun strictControllerAdapterFailuresHaveLocalizedTransportCopyOnly() {`

## gradle/libs.versions.toml

状态：modified


## mobile/build.gradle.kts

状态：modified


## mobile/src/debug/AndroidManifest.xml

状态：modified


## mobile/src/debug/assets/compat-probe/device-public-test.txt

状态：added


## mobile/src/debug/java/com/shilapi/xcertplay/compat/Api18CompatProbeActivity.kt

状态：added

- L47: `class Api18CompatProbeActivity : Activity(), SurfaceHolder.Callback {`
- L53: `override fun onReceive(context: Context?, intent: Intent?) {`
- L61: `override fun onCreate(savedInstanceState: Bundle?) {`
- L79: `override fun surfaceCreated(holder: SurfaceHolder) {`
- L83: `override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit`
- L84: `override fun surfaceDestroyed(holder: SurfaceHolder) = Unit`
- L86: `private fun runProbes(surface: Surface) {`
- L136: `fun finishCase(message: android.os.Message?) {`
- L152: `override fun onServiceConnected(name: android.content.ComponentName, binder: android.os.IBinder) {`
- L156: `override fun onServiceDisconnected(name: android.content.ComponentName) = Unit`
- L409: `private fun video(surface: Surface): String {`
- L446: `private fun audio(kind: AudioCodecKind): String {`
- L500: `private fun microphone(): String {`
- L516: `private fun beginVpn() {`
- L520: `override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {`
- L528: `private fun geelyFocus(): String {`
- L540: `fun count() = (listeners.get(coordinator) as Map<*, *>).size`
- L546: `fun keyedEntry(stream: Int): Map.Entry<*, *> =`
- L551: `fun callbackFor(entry: Map.Entry<*, *>): android.media.AudioManager.OnAudioFocusChangeListener =`
- L554: `fun volumeFor(stream: Int): Float {`
- L595: `private fun startVpn() { startService(Intent(this, Api18VpnProbeService::class.java)) }`
- L597: `private fun geelyAnwParcelMock(): String {`
- L603: `override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {`
- L631: `fun value(getter: String): Any? = result.javaClass.getMethod(getter).invoke(result)`
- L643: `private fun geelyEcarxParcelMock(): String {`
- L648: `override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {`
- L669: `private fun checkCase(name: String, action: () -> String) {`
- L677: `private fun line(message: String) {`
- L681: `private fun complete() { line("COMPLETE failures=${failures.get()}") }`
- L682: `override fun onDestroy() {`

## mobile/src/debug/java/com/shilapi/xcertplay/compat/Api18VpnProbeService.kt

状态：added

- L16: `class Api18VpnProbeService : VpnService() {`
- L17: `override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)`
- L18: `override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {`
- L23: `private fun runProbe() {`
- L85: `private fun echoRequest(): ByteArray {`

## mobile/src/debug/java/com/shilapi/xcertplay/compat/LegacyNetworkProbe.kt

状态：added

- L22: `internal object LegacyNetworkProbe {`
- L23: `fun bonjourDiscovery(context: Context): String {`
- L75: `fun bonjour(context: Context): String {`
- L79: `?: error("No emulator IPv4 interface for DNS-SD probe")`
- L119: `fun run(codecConfig: ByteArray): String {`
- L136: `override fun onConfig(codecData: ByteArray) {`
- L139: `override fun onClosed(cause: Throwable?) { if (cause != null) error.set(cause) }`

## mobile/src/debug/java/com/shilapi/xcertplay/compat/LegacyUsbProbe.kt

状态：added

- L11: `internal object LegacyUsbProbe {`
- L12: `fun run(): String {`
- L54: `private fun endpoint(address: Int, attributes: Int): UsbEndpoint =`
- L58: `private fun usbInterface(id: Int, cls: Int, sub: Int, proto: Int, endpoints: Array<Parcelable>): UsbInterface =`
- L62: `private fun device(interfaces: Array<Parcelable>): UsbDevice =`
- L67: `private fun iface(id: Int, alt: Int, endpoints: Int, cls: Int, sub: Int, proto: Int) =`
- L69: `private fun ep(address: Int, attributes: Int) = byteArrayOf(7, 5, address.toByte(), attributes.toByte(), 0, 2, 1)`
- L70: `private fun configuration(id: Int, interfaceCount: Int, body: ByteArray): ByteArray {`

## mobile/src/debug/java/com/shilapi/xcertplay/hud/StandaloneHudDemoActivity.kt

状态：modified

- L21: `class StandaloneHudDemoActivity : Activity() {`
- L30: `override fun run() {`
- L50: `override fun onCreate(state: Bundle?) {`
- L83: `private fun validateTarget() {`
- L103: `private fun startDemo() {`
- L124: `private fun transmit(packet: String) {`
- L129: `private fun clear(reason: String) {`
- L147: `override fun onStop() { clear("activity stopped"); BydNavigationOutputs.setDiagnosticHold(false); super.onStop() }`
- L148: `override fun onDestroy() { clear("activity destroyed"); super.onDestroy() }`

## mobile/src/main/AndroidManifest.xml

状态：modified


## settings.gradle.kts

状态：modified


## shared/build.gradle

状态：modified


## shared/src/main/AndroidManifest.xml

状态：modified


## shared/src/main/java/com/shilapi/xcertplay/adb/AdbKeys.kt

状态：modified

- L21: `object AdbKeys {`
- L35: `fun load(context: Context): KeyPair {`
- L53: `fun sign(token: ByteArray, key: PrivateKey): ByteArray =`
- L65: `fun publicKeyMessage(key: PublicKey): ByteArray {`
- L82: `private fun littleEndian(value: BigInteger, size: Int): ByteArray {`

## shared/src/main/java/com/shilapi/xcertplay/airplay/AirPlaySession.kt

状态：modified

- L19: `data class AirPlayDeviceInfo(`
- L27: `interface AirPlaySessionListener {`
- L28: `fun onSessionActive(session: AirPlaySession) {}`
- L29: `fun onSessionEnded(session: AirPlaySession) {}`
- L30: `fun onVideoFrameRendered(session: AirPlaySession) {}`
- L31: `fun onTransportError(message: String) {}`
- L32: `fun onDeviceInfo(session: AirPlaySession, info: AirPlayDeviceInfo) {}`
- L33: `fun onHostUiRequested(session: AirPlaySession) {}`
- L34: `fun onCommand(session: AirPlaySession, type: String, params: Map<String, Any?>) {}`
- L36: `fun onRemoteControlMessage(session: AirPlaySession, streamId: Long, message: Map<String, Any?>) {}`
- L38: `fun onVideoPlaybackUiRequested(session: AirPlaySession) {}`
- L39: `fun onDebugLog(message: String) {}`
- L43: `interface AirPlayMediaHandler {`
- L44: `fun onScreen(session: AirPlaySession, type: Int, stream: Map<String, Any?>): Int? = null`
- L45: `fun onAudio(session: AirPlaySession, type: Int, stream: Map<String, Any?>): Map<String, Any?>? = null`
- L46: `fun onDataStream(session: AirPlaySession, stream: Map<String, Any?>): Map<String, Any?>? = null`
- L47: `fun onFeedback(session: AirPlaySession): Map<String, Any?>? = null`
- L48: `fun onTeardown(session: AirPlaySession, type: Int) {}`
- L49: `fun onSessionClosed(session: AirPlaySession) {}`
- L50: `fun setIapTunnelHandler(handler: ((BlockingDuplexByteStream) -> Boolean)?) {}`
- L51: `fun onSetupResponseSent(session: AirPlaySession) {}`
- L60: `class AirPlaySession(`
- L106: `fun syncedNtp(): BigInteger = ntp.syncedNtp()`
- L108: `internal fun logDebug(message: String) = debugLog(message)`
- L110: `internal fun videoFrameRendered() {`
- L114: `internal fun logTrace(message: String) = trace(message)`
- L116: `fun start() {`
- L123: `override fun close() {`
- L140: `fun setClusterUiShown(shown: Boolean): Boolean {`
- L148: `fun sendCommand(command: Map<String, Any?>): Boolean = synchronized(eventWriteLock) {`
- L156: `fun sendRemoteControlMessage(streamId: Long, message: Map<String, Any?>): Boolean = synchronized(eventWriteLock) {`
- L167: `fun setVideoPlaybackAllowed(allowed: Boolean): Boolean = videoPlaybackEnabled && sendCommand(`
- L171: `private fun sendCommandLocked(command: Map<String, Any?>, extraHeaders: String = ""): Boolean {`
- L194: `fun sendTouch(contacts: List<AirPlayContact>): Boolean {`
- L215: `fun sendKnob(state: AirPlayKnobState, momentary: Boolean = true) {`
- L220: `fun sendKnobSelect(down: Boolean) =`
- L223: `fun sendMedia(index: Int) {`
- L228: `fun sendTelephony(index: Int) {`
- L233: `fun invokeSiri() {`
- L238: `fun sendIapMessage(data: ByteArray, timeoutMillis: Long = 0L): Boolean {`
- L273: `fun setNightMode(night: Boolean): Boolean = synchronized(eventWriteLock) {`
- L278: `private fun sendPendingNightModeLocked(): Boolean {`
- L287: `private fun sendHidReport(uid: Int, report: ByteArray): Boolean =`
- L296: `private fun runControl() {`
- L376: `private fun handle(request: RtspMessage.Request): RtspMessage.Response {`
- L439: `private fun notifySetupResponseSent() {`
- L447: `private fun debugLog(message: String, uiVisible: Boolean = true) {`
- L457: `private fun trace(message: String) {`
- L465: `private fun handleSetup(request: RtspMessage.Request): RtspMessage.Response {`
- L509: `private fun handleStreams(streams: List<*>): List<Any?> {`
- L554: `private fun handleCommand(request: RtspMessage.Request): RtspMessage.Response {`
- L579: `private fun handleTeardown(request: RtspMessage.Request): RtspMessage.Response {`
- L603: `private fun openTiming(peerPort: Int): Int {`
- L609: `private fun openKeepAlive(): Int {`
- L621: `private fun runKeepAlive(socket: DatagramSocket) {`
- L632: `private fun openEvent(): Int {`
- L639: `private fun teardown() {`
- L654: `private fun acceptEvent(server: ServerSocket) {`
- L692: `private fun runEventRead(socket: Socket) {`
- L742: `private fun spawnEvent(name: String, body: () -> Unit) {`
- L768: `internal fun setupEnabledFeatures(config: AirPlayConfig, proposed: List<*>?): List<String> {`
- L783: `internal fun teardownStreamTypes(body: Any?): List<Int>? {`
- L793: `internal fun safeClose(closeable: Any?) {`
- L806: `private fun ByteArray.toHex(): String =`
- L809: `private fun asMap(value: Any?): Map<String, Any?>? {`
- L816: `private fun string(value: Any?): String = value as? String ?: ""`
- L818: `private fun long(value: Any?): Long? = (value as? Number)?.toLong()`

## shared/src/main/java/com/shilapi/xcertplay/airplay/AudioStream.kt

状态：modified

- L12: `enum class AudioCodecKind { AAC_LC, OPUS, LPCM }`
- L14: `data class AudioFormat(`
- L29: `class AudioStream(`
- L34: `interface Listener {`
- L35: `fun onStarted(firstSample: Int) {}`
- L36: `fun onRtp(rtp: ByteArray, sample: Int) {}`
- L37: `fun onPacket(`
- L55: `fun listen(listener: Listener): Pair<Int, Int> {`
- L78: `override fun close() {`
- L86: `private fun runData(socket: DatagramSocket, listener: Listener) {`
- L174: `private fun runControl(socket: DatagramSocket) {`
- L185: `private fun bindAnyPort(): DatagramSocket {`
- L191: `private fun readU32Be(source: ByteArray, offset: Int): Int =`
- L197: `private fun ByteArray.toHexString(): String =`
- L214: `object AudioStreamCodec {`
- L215: `fun fromFormatBits(bits: Long, payloadType: Int, audioType: String = "media"): AudioFormat {`

## shared/src/main/java/com/shilapi/xcertplay/airplay/CarPlayMediaEngine.kt

状态：modified

- L14: `data class AudioStreamId(val type: Int, val audioType: String)`
- L17: `interface MediaSink {`
- L18: `fun onVideoCodec(type: Int, codec: VideoCodec) {}`
- L19: `fun onVideoConfig(type: Int, codecData: ByteArray) {}`
- L20: `fun onVideoFrame(type: Int, naluBytes: ByteArray) {}`
- L21: `fun setVideoRecoveryHandler(type: Int, handler: () -> Unit) {}`
- L22: `fun setVideoDiagnosticHandler(type: Int, handler: (String) -> Unit) {}`
- L23: `fun onScreenStreamActive(type: Int, active: Boolean) {}`
- L24: `fun onAudioStarted(id: AudioStreamId, format: AudioFormat, firstSample: Int) {}`
- L25: `fun onAudioRtp(id: AudioStreamId, format: AudioFormat, rtp: ByteArray, sample: Int) {}`
- L26: `fun onAudioStopped(id: AudioStreamId) {}`
- L27: `fun onMicrophoneStarted(id: AudioStreamId, config: MicrophoneConfig) {}`
- L28: `fun onMicrophoneStopped(id: AudioStreamId) {}`
- L29: `fun onIapMessage(bytes: ByteArray) {}`
- L37: `class CarPlayMediaEngine(`
- L42: `internal data class StreamKey(`
- L48: `private data class AudioMeta(`
- L57: `private data class PendingIapTunnel(`
- L71: `override fun setIapTunnelHandler(handler: ((BlockingDuplexByteStream) -> Boolean)?) {`
- L75: `override fun onScreen(session: AirPlaySession, type: Int, stream: Map<String, Any?>): Int? {`
- L98: `override fun onCodec(codec: VideoCodec) = sink.onVideoCodec(type, codec)`
- L99: `override fun onConfig(codecData: ByteArray) = sink.onVideoConfig(type, codecData)`
- L100: `override fun onFrame(naluBytes: ByteArray) = sink.onVideoFrame(type, naluBytes)`
- L101: `override fun onClosed(cause: Throwable?) {`
- L118: `override fun onAudio(session: AirPlaySession, type: Int, stream: Map<String, Any?>): Map<String, Any?>? {`
- L153: `override fun onStarted(firstSample: Int) {`
- L160: `override fun onRtp(rtp: ByteArray, sample: Int) =`
- L163: `override fun onPacket(`
- L183: `override fun onDataStream(session: AirPlaySession, stream: Map<String, Any?>): Map<String, Any?>? {`
- L222: `override fun onIap(bytes: ByteArray) = sink.onIapMessage(bytes)`
- L224: `override fun onClosed(cause: Throwable?) {`
- L251: `private fun videoDataStream(session: AirPlaySession, uuid: String, stream: Map<String, Any?>): Map<String, Any?>? {`
- L258: `fun key(label: String) = AirPlayCrypto.hkdfSha512(`
- L272: `override fun onSetupResponseSent(session: AirPlaySession) {`
- L287: `override fun onFeedback(session: AirPlaySession): Map<String, Any?>? {`
- L316: `override fun onTeardown(session: AirPlaySession, type: Int) {`
- L331: `override fun onSessionClosed(session: AirPlaySession) {`
- L345: `private fun replacePendingIapTunnel(session: AirPlaySession, next: PendingIapTunnel) {`
- L350: `private fun clearPendingIapTunnel(session: AirPlaySession? = null) {`
- L360: `private fun outputKey(session: AirPlaySession, stream: Map<String, Any?>): ByteArray? {`
- L364: `private fun microphoneConfig(`
- L404: `private fun dataStreamKey(`
- L419: `private fun isScreenStreamType(type: Int): Boolean =`
- L438: `internal fun unsignedPlistDecimal(value: Any?): String? = when (value) {`
- L447: `internal fun unsignedPlistInteger(value: Any?): Any = when (value) {`

## shared/src/main/java/com/shilapi/xcertplay/airplay/IapTunnel.kt

状态：modified

- L21: `class IapTunnel(`
- L25: `interface Listener {`
- L26: `fun onOpen(remoteAddress: String?) {}`
- L27: `fun onIap(bytes: ByteArray) {}`
- L28: `fun onDebug(message: String) {}`
- L29: `fun onClosed(cause: Throwable?) {}`
- L42: `fun listen(listener: Listener): Int {`
- L58: `private fun bindAny(): ServerSocket =`
- L61: `override fun close() {`
- L72: `fun awaitPeerConnection(timeoutMillis: Long): Boolean {`
- L82: `private fun accept(bound: ServerSocket) {`
- L106: `private fun run(sock: Socket) {`
- L139: `private fun decryptFrames(buffer: ByteArray): Pair<ByteArray, ByteArray> {`
- L158: `private fun parsePackages(buffer: ByteArray): ByteArray {`
- L176: `private fun readU16Le(source: ByteArray, offset: Int): Int =`
- L179: `private fun readU32Be(source: ByteArray, offset: Int): Int =`

## shared/src/main/java/com/shilapi/xcertplay/airplay/NtpClock.kt

状态：modified

- L19: `class NtpClock : Closeable {`
- L38: `fun listen(): Int {`
- L48: `fun start(peerAddress: InetAddress, port: Int) {`
- L55: `fun syncedNtp(): BigInteger = synchronized(clockLock) {`
- L59: `override fun close() {`
- L69: `private fun runSender() {`
- L86: `private fun sendRequest() {`
- L98: `private fun runReceiver() {`
- L116: `private fun handleMessage(message: ByteArray, source: InetSocketAddress) {`
- L123: `private fun respondToRequest(request: ByteArray, source: InetSocketAddress) {`
- L134: `private fun handleResponse(response: ByteArray) {`
- L169: `private fun applyOffset(offsetSec: Double) {`
- L181: `private fun currentSocket(): DatagramSocket? = synchronized(socketLock) { socket }`
- L204: `private fun wallClockNtpOffsetNs(monoNs: Long): Long {`
- L212: `private fun ntp64Now(): BigInteger {`
- L219: `private fun ntpFromNanos(ns: Long): BigInteger {`
- L232: `private fun ntpBytes(value: BigInteger): ByteArray {`
- L240: `private fun readNtp(value: ByteArray, offset: Int): BigInteger =`
- L243: `private fun writeU16Be(target: ByteArray, offset: Int, value: Int) {`

## shared/src/main/java/com/shilapi/xcertplay/airplay/ScreenStream.kt

状态：modified

- L13: `enum class VideoCodec { H264, H265 }`
- L22: `class ScreenStream(private val key: ByteArray, private val onDiagnostic: (String) -> Unit = {}) : Closeable {`
- L23: `interface Listener {`
- L24: `fun onCodec(codec: VideoCodec) {}`
- L25: `fun onConfig(codecData: ByteArray) {}`
- L26: `fun onFrame(naluBytes: ByteArray) {}`
- L27: `fun onClosed(cause: Throwable?) {}`
- L38: `fun listen(listener: Listener): Int {`
- L48: `override fun close() {`
- L55: `private fun accept(bound: ServerSocket) {`
- L65: `private fun run(sock: Socket) {`
- L90: `private fun onMessage(header: ByteArray, body: ByteArray) {`
- L117: `private fun readFully(input: InputStream, length: Int): ByteArray? {`
- L139: `private fun ByteArray.hexPrefix(length: Int): String =`
- L143: `object ScreenCodec {`
- L144: `fun decryptFrame(key: ByteArray, counter: Long, header: ByteArray, body: ByteArray): ByteArray =`
- L154: `fun lengthPrefixedToAnnexB(payload: ByteArray): ByteArray {`
- L178: `fun detectConfig(payload: ByteArray): Pair<VideoCodec, ByteArray> {`
- L189: `private fun looksLikeAvcC(payload: ByteArray): Boolean {`
- L197: `private fun readU16Be(source: ByteArray, offset: Int): Int =`
- L203: `private fun ByteArray.startsWithStartCode(): Boolean =`
- L210: `private fun readU32Be(source: ByteArray, offset: Int): Int =`
- L216: `private fun readU32Le(source: ByteArray, offset: Int): Int =`

## shared/src/main/java/com/shilapi/xcertplay/airplay/VideoInCar.kt

状态：modified

- L22: `object VideoInCar {`
- L49: `fun isPlayableUrl(url: String, iphoneLoadsAppSchemes: Boolean = false): Boolean {`
- L56: `fun featuresEx(legacyFeatures: Long): String {`
- L67: `fun info(legacyFeatures: Long, allowed: Boolean): Map<String, Any?> = linkedMapOf(`
- L82: `data class Item(val uuid: Any?, val url: String, val startMillis: Int)`
- L89: `fun parseItem(message: Map<String, Any?>, iphoneLoadsAppSchemes: Boolean = false): Item? {`
- L100: `fun seekMillis(message: Map<String, Any?>): Int? =`
- L104: `data class PlayerState(`
- L116: `fun playbackInfoResponse(messageId: Any?, itemUuid: Any?, state: PlayerState?): Map<String, Any?> {`
- L143: `fun seekResponse(messageId: Any?): Map<String, Any?> =`
- L150: `fun propertyResponse(messageId: Any?, key: Any?, state: PlayerState?): Map<String, Any?> {`
- L167: `fun errorNotification(itemUuid: Any?, code: Int): Map<String, Any?> =`
- L172: `fun playbackStateNotification(playing: Boolean, itemUuid: Any?): Map<String, Any?> =`
- L180: `fun cmTime(seconds: Double): Map<String, Any?> =`
- L183: `private fun range(start: Double, duration: Double) = linkedMapOf("start" to cmTime(start), "duration" to cmTime(duration))`
- L186: `private fun Map<String, Any?>.withoutNulls(): Map<String, Any?> {`
- L187: `fun strip(value: Any?): Any? = when (value) {`
- L196: `private fun seconds(time: Map<*, *>): Double? {`

## shared/src/main/java/com/shilapi/xcertplay/compat/Base64Compat.kt

状态：added

- L6: `object Base64Compat {`
- L7: `fun encodeToString(data: ByteArray): String = BcBase64.toBase64String(data)`
- L8: `fun decode(encoded: String): ByteArray = BcBase64.decode(encoded)`
- L9: `fun decodeMime(encoded: ByteArray): ByteArray = BcBase64.decode(String(encoded, CharsetsCompat.US_ASCII))`
- L12: `fun encodeMimeLines(data: ByteArray, lineSeparator: ByteArray): String =`

## shared/src/main/java/com/shilapi/xcertplay/compat/CharsetsCompat.kt

状态：added

- L6: `object CharsetsCompat {`

## shared/src/main/java/com/shilapi/xcertplay/compat/ConcurrentHashMapCompat.kt

状态：added


## shared/src/main/java/com/shilapi/xcertplay/compat/ContextCompat.kt

状态：added

- L12: `object ContextCompat {`
- L18: `fun checkSelfPermission(context: Context, permission: String): Int =`
- L31: `fun signatures(context: Context, packageName: String): Array<Signature> {`
- L57: `fun hasSigningInfo(info: PackageInfo): Boolean =`

## shared/src/main/java/com/shilapi/xcertplay/compat/GeelyBluetoothDiagnostics.kt

状态：added

- L20: `class GeelyBluetoothDiagnostics(context: Context) {`
- L41: `fun query(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS, callback: (GeelyBluetoothSnapshot) -> Unit): Request {`
- L81: `private fun serviceIntent() = Intent().setComponent(SERVICE_COMPONENT)`
- L83: `private fun startRead(pending: Pending, binder: IBinder) {`
- L92: `private fun startEcarxOnly(pending: Pending, bindingState: BindingState) {`
- L103: `private fun startReadTask(pending: Pending, read: () -> GeelyBluetoothSnapshot) {`
- L144: `private fun finish(pending: Pending, snapshot: GeelyBluetoothSnapshot, notify: Boolean = true) {`
- L162: `private fun release(pending: Pending) {`
- L166: `private inner class Pending(callback: (GeelyBluetoothSnapshot) -> Unit) {`
- L178: `override fun onServiceConnected(name: ComponentName?, service: IBinder?) {`
- L186: `override fun onServiceDisconnected(name: ComponentName?) {`
- L194: `fun deliver(snapshot: GeelyBluetoothSnapshot) {`
- L204: `class Request internal constructor(private val cancelRequest: () -> Unit) {`
- L205: `fun cancel() = cancelRequest()`
- L208: `internal interface ServiceBinding {`
- L209: `fun bind(context: Context, intent: Intent, connection: ServiceConnection): Boolean`
- L210: `fun unbind(context: Context, connection: ServiceConnection)`
- L213: `private object AndroidServiceBinding : ServiceBinding {`
- L214: `override fun bind(context: Context, intent: Intent, connection: ServiceConnection): Boolean =`
- L217: `override fun unbind(context: Context, connection: ServiceConnection) = context.unbindService(connection)`
- L236: `enum class BindingState {`
- L250: `enum class ReadState { OK, UNSUPPORTED, FAILED }`
- L252: `enum class ReadFailure { DESCRIPTOR_MISMATCH, INVALID_RESPONSE, REMOTE_FAILURE, SERVICE_UNAVAILABLE, PERMISSION_DENIED, TIMEOUT }`
- L254: `enum class AnwPowerState { OFF, ON, TURNING_ON, TURNING_OFF, UNKNOWN }`
- L256: `data class ReadValue<T>(val state: ReadState, val value: T? = null, val failure: ReadFailure? = null)`
- L258: `data class GeelyBluetoothSnapshot(`
- L266: `fun unavailable(state: BindingState) = GeelyBluetoothSnapshot(`
- L277: `internal object GeelyBluetoothReadOnlyProtocol {`
- L284: `fun read(binder: IBinder): GeelyBluetoothSnapshot {`
- L312: `private fun readPower(binder: IBinder): ReadValue<AnwPowerState> = transact(binder, TRANSACTION_POWER) { _, reply ->`
- L324: `private fun readPairedCount(binder: IBinder): ReadValue<Int> = transact(`
- L344: `private fun readSppInitialized(binder: IBinder): ReadValue<Boolean> = transact(binder, TRANSACTION_SPP_INITIALIZED) { _, reply ->`
- L352: `private fun readExactIntArray(parcel: Parcel, expectedLength: Int): IntArray {`
- L360: `private fun skipExactStringArray(parcel: Parcel, expectedLength: Int) {`
- L405: `internal object EcarxBluetoothReadOnlyProtocol {`
- L410: `fun read(): ReadValue<Boolean> {`
- L428: `fun readBinder(binder: IBinder): ReadValue<Boolean> {`

## shared/src/main/java/com/shilapi/xcertplay/compat/NetworkInterfaceCompat.kt

状态：added


## shared/src/main/java/com/shilapi/xcertplay/compat/PendingIntentCompat.kt

状态：added

- L7: `object PendingIntentCompat {`
- L8: `fun updateCurrentImmutableFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or`

## shared/src/main/java/com/shilapi/xcertplay/compat/SystemServices.kt

状态：added

- L16: `fun Context.appPrivateDir(): java.io.File =`

## shared/src/main/java/com/shilapi/xcertplay/compat/UsbCompat.kt

状态：added

- L15: `object UsbCompat {`
- L16: `fun configurationCount(device: UsbDevice): Int =`
- L20: `fun configuration(device: UsbDevice, index: Int): UsbConfiguration? =`
- L24: `fun configurationId(configuration: UsbConfiguration): Int =`
- L28: `fun interfaceCount(configuration: UsbConfiguration): Int =`
- L32: `fun usbInterface(configuration: UsbConfiguration, index: Int): UsbInterface? =`
- L36: `fun alternateSetting(usbInterface: UsbInterface): Int =`
- L46: `fun setConfiguration(`
- L60: `fun setConfigurationById(connection: UsbDeviceConnection, configurationId: Int): Boolean =`
- L64: `private fun setConfigurationLegacy(connection: UsbDeviceConnection, configurationId: Int): Boolean {`
- L76: `* legacy branch sends SET_INTERFACE (0x01 0x0B) with wIndex encoding interface and alternate.`
- L78: `fun setInterface(connection: UsbDeviceConnection, usbInterface: UsbInterface): Boolean =`
- L83: `* setInterface() does not exist and the platform object carries no alternate, so the value`
- L86: `fun setInterface(`
- L95: `// wIndex=interface id. Putting the alternate in wIndex instead selects alt 0.`
- L117: `private fun invokeInt(target: Any, name: String, fallback: Int): Int =`

## shared/src/main/java/com/shilapi/xcertplay/compat/WildcardBind.kt

状态：added

- L19: `object WildcardBind {`
- L22: `fun anyAddress(): InetAddress = InetAddress.getByAddress(ByteArray(4))`
- L25: `fun bind(socket: DatagramSocket, port: Int = 0, reuseAddress: Boolean = true): DatagramSocket {`
- L32: `fun bind(server: java.net.ServerSocket, port: Int = 0, backlog: Int = 50): java.net.ServerSocket {`
- L42: `fun listenAddress(): InetAddress = anyAddress()`

## shared/src/main/java/com/shilapi/xcertplay/hud/BydClusterSong.kt

状态：modified

- L13: `internal data class ClusterSong(val text: String, val playing: Boolean)`
- L20: `internal class ClusterSongState {`
- L27: `fun accept(frame: Iap2Frame): ClusterSong? {`
- L49: `fun current(): ClusterSong? = last`
- L52: `fun clear() {`
- L74: `fun text(title: String?, artist: String?): String? {`
- L93: `internal object BydClusterSong {`
- L108: `fun attach(appContext: Context) {`
- L113: `fun onFrame(frame: Iap2Frame) {`
- L126: `fun settingChanged(enabled: Boolean) {`
- L132: `fun end() {`
- L138: `private fun show(app: Context, song: ClusterSong) {`
- L143: `private fun stop(app: Context) {`
- L148: `private fun write(app: Context, song: ClusterSong) {`
- L162: `private fun clear(app: Context) {`
- L167: `private fun run(app: Context, args: String): Boolean {`
- L184: `object BydClusterSongTool {`
- L191: `fun main(args: Array<String>) {`
- L203: `private fun write(args: Array<String>) {`
- L226: `private fun describe(error: Throwable): String {`

## shared/src/main/java/com/shilapi/xcertplay/iap2/session/Iap2Session.kt

状态：modified

- L18: `* [Iap2CsmChannel] remains the lower-level frame transport. This class is the protocol-facing`
- L22: `class Iap2Session private constructor(`
- L29: `fun awaitReady(timeoutMillis: Long): Boolean {`
- L35: `fun send(frame: Iap2Frame, timeoutMillis: Long = DEFAULT_SEND_TIMEOUT_MILLIS) {`
- L52: `fun send(`
- L60: `fun sendRaw(`
- L68: `fun recv(timeoutMillis: Long): Iap2Frame? {`
- L84: `fun reader(frame: Iap2Frame): Iap2BodyReader = Iap2Messages.reader(frame)`
- L86: `override fun close() {`
- L94: `private fun emitFrameTrace(direction: Iap2TraceDirection, frame: Iap2Frame) {`
- L109: `private fun emitTrace(message: String) {`
- L120: `fun open(`
- L128: `fun openWireless(`
- L136: `fun openTunnel(`
- L144: `fun wrap(`

## shared/src/main/java/com/shilapi/xcertplay/location/AndroidCarPlayLocationProvider.kt

状态：modified

- L23: `class AndroidCarPlayLocationProvider(`
- L39: `override fun onLocationChanged(location: Location) {`
- L46: `override fun onProviderEnabled(provider: String) = Unit`
- L47: `override fun onProviderDisabled(provider: String) = Unit`
- L48: `override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) = Unit`
- L54: `override fun start(): Boolean = synchronized(stateLock) {`
- L83: `private fun seedLastKnownLocations() {`
- L99: `override fun stop() {`
- L113: `override fun latestNmea(): String? {`
- L133: `private fun latestFix(): Location? {`

## shared/src/main/java/com/shilapi/xcertplay/media/AndroidMediaSink.kt

状态：modified

- L34: `private fun inputBufferAt(codec: MediaCodec, index: Int): ByteBuffer? =`
- L38: `private fun outputBufferAt(codec: MediaCodec, index: Int): ByteBuffer? =`
- L43: `internal class AudioFocusCoordinator(`
- L50: `private data class Entry(val channel: AudioChannel, val attributes: Any)`
- L51: `private data class GeelyFocusKey(val channel: AudioChannel, val streamType: Int)`
- L52: `private class GeelyFocusLease(`
- L82: `fun acquire(track: AudioTrack, channel: AudioChannel, attributes: Any?, legacyStreamType: Int) {`
- L95: `fun acquireLegacy(track: AudioTrack, channel: AudioChannel, streamType: Int) {`
- L107: `fun release(track: AudioTrack) {`
- L123: `private fun acquireGeely(track: AudioTrack, channel: AudioChannel, streamType: Int) {`
- L174: `private fun handleGeelyFocusChange(`
- L199: `private fun setGeelyChannelVolume(key: GeelyFocusKey, volume: Float) {`
- L207: `private fun requestAudioFocusLegacy(streamType: Int): Int = manager`
- L211: `private fun refreshLegacyFocus() {`
- L223: `private fun abandonFocus() {`
- L247: `private fun refreshRequest() {`
- L289: `private fun setVolume(volume: Float) {`
- L293: `private fun AudioChannel.focusPriority(): Int = when (this) {`
- L315: `class AndroidMediaSink(`
- L368: `override fun setVideoRecoveryHandler(type: Int, handler: () -> Unit) {`
- L372: `override fun setVideoDiagnosticHandler(type: Int, handler: (String) -> Unit) {`
- L376: `private fun requestVideoRecovery(type: Int) {`
- L387: `fun setSurface(type: Int, surface: Surface) {`
- L392: `fun clearSurface(type: Int, surface: Surface) {`
- L400: `fun setMirrorSurface(type: Int, key: String, surface: Surface?) {`
- L413: `private fun mirrorDecoders(type: Int): List<VideoDecoder> = synchronized(mirrorLock) {`
- L420: `fun setScreenStreamActiveChangedListener(listener: ((Int, Boolean) -> Unit)?) {`
- L427: `override fun onVideoCodec(type: Int, codec: VideoCodec) {`
- L431: `override fun onVideoConfig(type: Int, codecData: ByteArray) {`
- L438: `override fun onVideoFrame(type: Int, naluBytes: ByteArray) {`
- L443: `override fun onScreenStreamActive(type: Int, active: Boolean) {`
- L460: `override fun onAudioStarted(id: AudioStreamId, format: AudioFormat, firstSample: Int) {`
- L465: `override fun onAudioRtp(id: AudioStreamId, format: AudioFormat, rtp: ByteArray, sample: Int) {`
- L469: `override fun onAudioStopped(id: AudioStreamId) {`
- L474: `private fun updateMediaAudio(id: AudioStreamId, active: Boolean) {`
- L483: `override fun onMicrophoneStarted(id: AudioStreamId, config: MicrophoneConfig) {`
- L499: `override fun onMicrophoneStopped(id: AudioStreamId) {`
- L507: `private fun enterCommunicationMode(id: AudioStreamId) {`
- L519: `private fun restoreAudioMode(id: AudioStreamId?) {`
- L534: `fun close() {`
- L562: `private fun videoDecoder(type: Int): VideoDecoder =`
- L565: `private fun newVideoDecoder(type: Int, surface: Surface?, statsLabel: String? = null) = VideoDecoder(`
- L577: `private fun audioRenderer(id: AudioStreamId, format: AudioFormat): AudioRenderer {`
- L598: `private class VideoDecoder(`
- L622: `fun configure(codec: VideoCodec, codecData: ByteArray) {`
- L626: `fun submit(nalus: ByteArray) {`
- L631: `fun setSurface(surface: Surface?) {`
- L635: `override fun close() {`
- L640: `private fun run() {`
- L675: `private fun configureDecoder(config: VideoJob.Config) {`
- L733: `private data class DecoderAttempt(val codecName: String?, val tuned: Boolean)`
- L735: `private fun buildFormat(mime: String, csd: List<ByteArray>, tuned: Boolean): MediaFormat =`
- L744: `private fun tryConfigure(`
- L774: `private fun softwareDecoderName(mime: String): String? {`
- L781: `private fun createDecoder(mime: String): MediaCodec {`
- L800: `private fun changeSurface(surface: Surface?) {`
- L822: `private fun feed(nalus: ByteArray) {`
- L859: `private fun recover(reason: String) {`
- L869: `private fun requestKeyFrameIfDue() {`
- L876: `private fun drainOutput(codec: MediaCodec) {`
- L900: `private fun logOutputFormat(format: MediaFormat) {`
- L921: `private fun releaseDecoder() {`
- L947: `private fun MediaFormat.intOrNull(key: String): Int? =`
- L959: `private class AudioRenderer(`
- L972: `private data class AudioPacket(val rtp: ByteArray, val sample: Int)`
- L1018: `fun start() {`
- L1024: `fun submit(rtp: ByteArray, sample: Int) {`
- L1048: `override fun close() {`
- L1053: `private fun run() {`
- L1083: `private fun configureCodec(mime: String) {`
- L1117: `private fun createTrack() {`
- L1143: `fun legacyTrack() = AudioTrack(legacyTrackStreamType, format.sampleRate, channelMask,`
- L1205: `private fun channelOverride(channel: AudioChannel, audioType: String): Int? = AudioStreamRouting.override(`
- L1215: `private fun audioAttributesFor(`
- L1220: `private fun mappedSelection(): AudioChannelSelection {`
- L1234: `private fun buildUsageTrack(`
- L1251: `private fun requestAudioFocus() {`
- L1266: `private fun abandonAudioFocus() {`
- L1270: `private fun streamType(): Int {`
- L1284: `private fun aacAudioSpecificConfig(): ByteArray {`
- L1292: `private fun usageFor(channel: AudioChannel): Int = when (channel) {`
- L1299: `private fun contentTypeFor(contentType: AudioContentType): Int = when (contentType) {`
- L1305: `private fun opusHead(): ByteArray {`
- L1319: `private fun opusCodecDelay(): ByteArray =`
- L1325: `private fun opusSeekPreRoll(): ByteArray =`
- L1331: `private fun handle(packet: AudioPacket) {`
- L1371: `private fun sampleTimestampUs(sample: Int): Long =`
- L1374: `private fun feedCodec(payload: ByteArray, presentationTimeUs: Long) {`
- L1409: `private fun drainCodec(codec: MediaCodec) {`
- L1448: `private fun writePcm(data: ByteArray, offset: Int = 0, length: Int = data.size) {`
- L1499: `private fun startPlayback(track: AudioTrack) {`
- L1505: `private fun maintainPlaybackBuffer() {`
- L1524: `private fun logStatsIfDue(force: Boolean = false) {`
- L1566: `private fun applyFadeIn(data: ByteArray, offset: Int, length: Int) {`
- L1578: `private fun byteSwapS16(source: ByteArray): ByteArray {`
- L1588: `private fun release() {`

## shared/src/main/java/com/shilapi/xcertplay/media/AudioChannelMapping.kt

状态：modified

- L5: `internal enum class AudioChannelMappingMode {`
- L10: `internal enum class AudioChannel {`
- L17: `internal enum class AudioContentType {`
- L22: `internal data class AudioChannelSelection(`
- L36: `internal object AudioChannelMapper {`
- L40: `fun map(`
- L53: `fun usesNavigationStream(audioType: String, payloadType: Int, advanced: Boolean): Boolean =`
- L56: `private fun mapMobileCompatible(`
- L70: `private fun mapAutomotiveBus(`
- L84: `private fun mainHighAudioOrNavigation(payloadType: Int, navigationStreamType: Int): AudioChannelSelection =`
- L93: `internal object AudioStreamRouting {`
- L97: `fun override(`
- L111: `fun legacyStreamType(channel: AudioChannel, streamOverride: Int?): Int =`

## shared/src/main/java/com/shilapi/xcertplay/media/CodecCompat.kt

状态：added

- L12: `object CodecCompat {`
- L13: `fun decoders(): List<MediaCodecInfo> =`
- L22: `fun videoDecoderFor(mime: String): MediaCodecInfo? = decoders().firstOrNull { info ->`

## shared/src/main/java/com/shilapi/xcertplay/media/GeelyAudioCapabilities.kt

状态：added

- L6: `data class GeelyAudioCapabilities(val carPlay: Int, val navigationSpeech: Int, val navigationAlert: Int?) {`
- L8: `fun navigationStream(audioType: String, separateAlerts: Boolean): Int =`
- L16: `fun detect(): GeelyAudioCapabilities? = fromFields { name ->`
- L20: `internal fun fromFields(read: (String) -> Int?): GeelyAudioCapabilities? {`

## shared/src/main/java/com/shilapi/xcertplay/media/MicrophoneUplink.kt

状态：modified

- L27: `internal class MicrophoneUplink(`
- L42: `fun start(): Boolean {`
- L145: `private fun voiceEffects(sessionId: Int): List<AudioEffect> = listOfNotNull(`
- L155: `private fun enabledEffect(name: String, create: () -> AudioEffect?): AudioEffect? {`
- L176: `private fun releaseEffect(effect: AudioEffect) {`
- L184: `private fun capture(recorder: AudioRecord, socket: DatagramSocket) {`
- L231: `private fun sendFrame(socket: DatagramSocket, counters: MicrophoneCounters, frame: ByteArray) {`
- L248: `private fun sendPacket(`
- L270: `private fun routeType(recorder: AudioRecord): Int? = PortableAudio.routedDeviceType(recorder)`
- L272: `override fun close() {`
- L299: `private fun release() {`

## shared/src/main/java/com/shilapi/xcertplay/media/ModernAudio.kt

状态：added

- L19: `object ModernAudio {`
- L20: `fun attributes(usage: Int, contentType: Int, legacyStreamType: Int?): Any {`
- L32: `fun usageTrack(`
- L52: `fun microphoneRecorder(`
- L71: `fun legacyStreamType(attributes: Any): Int = runCatching {`

## shared/src/main/java/com/shilapi/xcertplay/media/OpusEncoder.kt

状态：modified

- L13: `internal class OpusEncoder(bitrate: Int) : Closeable {`
- L47: `fun encode(pcm: ByteArray): List<ByteArray> {`
- L76: `private fun drain(): List<ByteArray> {`
- L117: `override fun close() {`
- L145: `private fun inputBufferAt(codec: MediaCodec, index: Int) =`
- L149: `private fun outputBufferAt(codec: MediaCodec, index: Int) =`

## shared/src/main/java/com/shilapi/xcertplay/media/PortableAudio.kt

状态：added

- L8: `object PortableAudio {`
- L9: `fun writeBlocking(track: AudioTrack, data: ByteArray, offset: Int, length: Int): Int =`
- L17: `fun readBlocking(recorder: AudioRecord, data: ByteArray, offset: Int, length: Int): Int =`
- L25: `fun setVolume(track: AudioTrack, volume: Float) {`
- L34: `fun underrunCount(track: AudioTrack): Int =`
- L37: `fun bufferSizeInFrames(track: AudioTrack): Int =`
- L40: `fun routedDeviceType(track: AudioTrack): Int? =`
- L44: `fun bufferSizeBytes(track: AudioTrack): Int =`
- L47: `fun routedDeviceType(recorder: AudioRecord): Int? {`

## shared/src/main/java/com/shilapi/xcertplay/media/VideoDecodeQueue.kt

状态：modified

- L8: `internal sealed interface VideoJob {`
- L9: `data class Config(val codec: VideoCodec, val codecData: ByteArray) : VideoJob`
- L10: `data class Frame(val nalus: ByteArray, val receivedNs: Long = System.nanoTime()) : VideoJob`
- L11: `data class SurfaceChanged(val surface: Surface?) : VideoJob`
- L12: `data object Resync : VideoJob`
- L16: `internal class VideoReferenceChain {`
- L19: `fun reset() { needsKeyFrame = true }`
- L20: `fun accepts(bytes: ByteArray, codec: VideoCodec): Boolean =`
- L22: `fun onQueued() { needsKeyFrame = false }`
- L26: `internal class VideoDecodeQueue(`
- L33: `@Synchronized fun offer(job: VideoJob) {`
- L46: `@Synchronized fun discardFrames() {`
- L57: `fun poll(timeoutMillis: Long): VideoJob? = jobs.poll(timeoutMillis, TimeUnit.MILLISECONDS)`
- L61: `internal object VideoInputPump {`
- L62: `fun acquire(`

## shared/src/main/java/com/shilapi/xcertplay/mfi/RemoteMfiAuthenticationClient.kt

状态：modified

- L14: `class RemoteMfiAuthenticationClient(`
- L21: `private data class CertificateInfo(`
- L29: `private data class HttpResponse(`
- L50: `fun reset(): Unit = synchronized(operationLock) {`
- L61: `override fun protocolMajor(): Int = synchronized(operationLock) {`
- L70: `override fun readCertificate(maximumOutputLength: Int): ByteArray = synchronized(operationLock) {`
- L83: `override fun baaCertificates(): BaaCertificatePair = synchronized(operationLock) {`
- L88: `override fun signChallenge(challenge: ByteArray): ByteArray = synchronized(operationLock) {`
- L109: `private fun loadCertificateInfo(): CertificateInfo {`
- L164: `private fun parseBaaCertificatePackage(encoded: ByteArray): BaaCertificatePair {`
- L181: `private fun readU32(bytes: ByteArray, offset: Int): Int =`
- L187: `private fun request(`
- L215: `private fun execute(method: String, path: String, requestBody: String?): HttpResponse {`
- L241: `private fun readUtf8Limited(input: java.io.InputStream): String {`
- L257: `private fun decodeBase64(encoded: String, field: String): ByteArray = try {`
- L263: `private fun decodeSha256(hex: String): ByteArray {`
- L272: `private fun isRetryable(statusCode: Int): Boolean =`
- L295: `class RemoteMfiException(message: String, cause: Throwable? = null) : MfiException(message, cause)`
- L297: `class RemoteMfiHttpException(`
- L302: `/** Minimal flat-object JSON reader for the remote MFI wire format. */`
- L303: `internal object RemoteMfiJson {`
- L304: `fun string(json: String, name: String): String = optionalString(json, name)`
- L307: `fun optionalString(json: String, name: String): String? {`
- L312: `fun integer(json: String, name: String): Int {`
- L319: `private fun stringPattern(name: String): Regex =`
- L322: `private fun integerPattern(name: String): Regex =`
- L325: `private fun decodeJsonString(encoded: String): String {`

## shared/src/main/java/com/shilapi/xcertplay/network/CarHotspotStatus.kt

状态：modified

- L13: `object CarHotspotStatus {`
- L21: `fun isEnabled(context: Context): Boolean? {`

## shared/src/main/java/com/shilapi/xcertplay/network/CarPlayBonjour.kt

状态：modified

- L33: `data class CarPlayBonjourEndpoint(`
- L40: `sealed interface CarPlayBonjourEvent {`
- L41: `data class Discovery(val stage: Stage, val ipv4Count: Int = 0, val ipv6Count: Int = 0) : CarPlayBonjourEvent {`
- L42: `enum class Stage { ADDED, NO_MATCHING_ADDRESS, INVALID_PORT }`
- L44: `data class Resolved(val endpoint: CarPlayBonjourEndpoint) : CarPlayBonjourEvent`
- L46: `data class Probed(`
- L55: `fun CarPlayBonjourEvent.diagnosticSummary(): String = when (this) {`
- L66: `object CarPlayBonjourProtocol {`
- L67: `fun airPlayTxtRecords(`
- L81: `fun connectProbeRequest(`
- L122: `class CarPlayBonjour(`
- L161: `override fun serviceAdded(event: ServiceEvent) {`
- L168: `override fun serviceRemoved(event: ServiceEvent) {`
- L177: `override fun serviceResolved(event: ServiceEvent) {`
- L204: `override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit`
- L206: `override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {`
- L210: `override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit`
- L212: `override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {`
- L218: `override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {`
- L222: `override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {`
- L226: `override fun onDiscoveryStarted(serviceType: String) = Unit`
- L228: `override fun onDiscoveryStopped(serviceType: String) = Unit`
- L230: `override fun onServiceFound(serviceInfo: NsdServiceInfo) {`
- L239: `override fun onServiceLost(serviceInfo: NsdServiceInfo) {`
- L247: `fun start() {`
- L302: `override fun close() {`
- L336: `private fun registerAirPlay() {`
- L353: `private fun advertisedHostAddress(): InetAddress? {`
- L369: `private fun runWorker() {`
- L403: `private fun handleService(service: NsdServiceInfo) {`
- L425: `private fun resolveWithRetry(service: NsdServiceInfo): NsdServiceInfo? {`
- L432: `override fun onServiceResolved(serviceInfo: NsdServiceInfo) {`
- L437: `override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {`
- L478: `private fun preferredAddress(serviceInfo: NsdServiceInfo): InetAddress? {`
- L491: `private fun applyLocalScope(address: InetAddress): InetAddress {`
- L501: `private fun probe(`
- L534: `private fun probeOnce(address: InetAddress, port: Int): String {`
- L568: `private fun emit(event: CarPlayBonjourEvent) {`
- L577: `private fun decodeTxtValue(value: ByteArray): String =`
- L580: `private fun joinWorker(worker: Thread) {`

## shared/src/main/java/com/shilapi/xcertplay/network/CarPlayVpnService.kt

状态：modified

- L31: `class CarPlayVpnService : VpnService() {`
- L32: `inner class LocalBinder : Binder() {`
- L36: `sealed class AttachResult {`
- L37: `data object Started : AttachResult()`
- L38: `data object AlreadyStarted : AttachResult()`
- L39: `data class Failed(val message: String) : AttachResult()`
- L42: `private data class AirPlayAttachment(`
- L62: `override fun onBind(intent: Intent?): IBinder = binder`
- L65: `fun attach(`
- L122: `fun attachWireless(`
- L151: `fun detach() {`
- L155: `fun isAttached(): Boolean = active.get() && attachment != null`
- L158: `fun boundPort(): Int? = attachment?.config?.port`
- L160: `override fun onDestroy() {`
- L165: `private fun startAirPlayServer(`
- L183: `private fun acceptLoop(`
- L211: `override fun onRemoteControlMessage(`
- L219: `override fun onVideoPlaybackUiRequested(session: AirPlaySession) {`
- L223: `override fun onSessionEnded(session: AirPlaySession) {`
- L240: `private fun addSession(session: AirPlaySession) {`
- L244: `private fun removeSession(session: AirPlaySession?) {`
- L249: `private fun closeSessionsLocked() {`
- L262: `private fun onTransportError(`
- L286: `private fun releaseLocked() {`
- L307: `fun prepare(context: Context): Intent? = VpnService.prepare(context)`

## shared/src/main/java/com/shilapi/xcertplay/network/Ipv6NcmBridge.kt

状态：modified

- L21: `class Ipv6NcmBridge(`
- L42: `fun start() {`
- L54: `override fun close() {`
- L62: `private fun runNcmToTun() {`
- L92: `private fun runTunToNcm() {`
- L137: `private fun join(thread: Thread) {`
- L147: `private fun ByteArray.macString(): String =`
- L150: `private fun ByteArray.summary(offset: Int): String {`
- L165: `private fun ByteArray.u16(offset: Int): Int =`

## shared/src/main/java/com/shilapi/xcertplay/network/LocalOnlyHotspotManager.kt

状态：modified

- L31: `* the AP interface is usable. The reservation and multicast lock stay owned by this instance`
- L34: `class LocalOnlyHotspotManager(context: Context, private val onDiagnostic: (String) -> Unit = {}) : WirelessHotspotManager {`
- L54: `override fun start(timeoutMillis: Long): WirelessHotspotInfo {`
- L162: `private fun disconnectTwoPointFourStation() {`
- L187: `private fun requestHotspot(callback: WifiManager.LocalOnlyHotspotCallback): Int? {`
- L245: `private fun awaitRadioInfo(`
- L311: `override fun close() {`
- L334: `private fun createCallback(attempt: StartAttempt): WifiManager.LocalOnlyHotspotCallback =`
- L336: `override fun onStarted(`
- L356: `override fun onFailed(reason: Int) {`
- L367: `override fun onStopped() {`
- L383: `private fun awaitStart(`
- L407: `private fun acquireMulticastLock(attempt: StartAttempt): WifiManager.MulticastLock {`
- L424: `private fun readConfiguration(`
- L439: `private fun readSoftApConfiguration(configuration: SoftApConfiguration): HotspotConfiguration {`
- L462: `private fun readWifiConfiguration(configuration: WifiConfiguration): HotspotConfiguration {`
- L487: `private fun readConfiguredChannel(configuration: SoftApConfiguration): Pair<Int, String> {`
- L502: `private fun readLegacySoftApChannel(configuration: SoftApConfiguration): Pair<Int, String> {`
- L521: `private fun readWifiConfigurationChannel(configuration: WifiConfiguration): Int {`
- L533: `private fun readWifiConfigurationBandLabel(`
- L553: `private fun readLegacySoftApBandLabel(`
- L566: `private fun awaitApInterface(`
- L603: `private fun findInterface(`
- L618: `private fun activeInterfaces(): List<NetworkInterface> =`
- L628: `private fun upstreamInterfaceNames(): Set<String> {`
- L635: `private fun NetworkInterface.siteLocalIpv4Addresses(): Set<String> =`
- L639: `private fun NetworkInterface.interfaceBssid(): String? =`
- L645: `private fun NetworkInterface.hotspotAddress(): InetAddress? {`
- L663: `private fun ensureStartActive(attempt: StartAttempt) {`
- L669: `private fun ensureStartActiveLocked(attempt: StartAttempt) {`
- L675: `private fun cleanupFailedStart(`
- L690: `private fun waitNanos(nanos: Long) {`
- L701: `private fun deadlineAfter(timeoutMillis: Long): Long {`
- L707: `private fun validateSsid(value: String?): String {`
- L716: `private fun validatePassphrase(`
- L731: `private fun mapSoftApSecurity(securityType: Int): Iap2WirelessSecurity = when (securityType) {`
- L745: `private fun mapWifiConfigurationSecurity(`
- L767: `private fun requireChannel(channel: Int, source: String): Int {`
- L771: `private fun requireChannel(channel: Int, source: String, allowAuto: Boolean): Int {`
- L778: `private fun ByteArray.toMacAddressString(): String =`
- L781: `private fun Inet6Address.toEui64MacAddress(): String? {`
- L798: `private fun softApBandLabel(band: Int): String = when (band) {`
- L806: `private fun legacyBandLabel(channel: Int): String = when (channel) {`
- L812: `private fun unquote(value: String?): String? {`
- L822: `private fun failureReason(reason: Int): String = when (reason) {`
- L830: `private fun parseMacAddress(value: String): ByteArray {`
- L844: `private fun closeReservation(reservation: WifiManager.LocalOnlyHotspotReservation?) {`
- L848: `private fun releaseMulticastLock(lock: WifiManager.MulticastLock?) {`
- L857: `private class StartAttempt {`
- L863: `private class HotspotConfiguration(`
- L873: `private class ApInterface(`

## shared/src/main/java/com/shilapi/xcertplay/network/ManualHotspotManager.kt

状态：modified

- L29: `* The hotspot remains owned by the system. This manager only locates its interface and reads the`
- L34: `class ManualHotspotManager(`
- L74: `override fun start(timeoutMillis: Long): WirelessHotspotInfo {`
- L90: `var lastReason = "local hotspot interface was not found"`
- L157: `override fun close() {`
- L161: `private fun validateApConfiguration(configuration: ManualApConfiguration?) {`
- L204: `private fun findLocalHotspotInterface(): LocalHotspotInterface? {`
- L235: `private fun isUsableInterface(`
- L247: `private fun interfaceScore(name: String, address: InetAddress): Int {`
- L265: `private fun NetworkInterface.hotspotAddress(): InetAddress? =`
- L268: `private fun frequencyFromConnectionInfo(): Int? {`
- L280: `private fun frequencyFromScanResult(localInterface: LocalHotspotInterface): Int? {`
- L295: `private fun readApConfiguration(): ManualApConfiguration? =`
- L299: `private fun readSoftApConfiguration(): ManualApConfiguration? {`
- L338: `private fun readLegacyApConfiguration(): ManualApConfiguration? {`
- L366: `private fun mapSoftApSecurity(securityType: Int): Iap2WirelessSecurity = when (securityType) {`
- L375: `private fun mapWifiConfigurationSecurity(`
- L391: `private fun bandLabel(frequencyMHz: Int): String = when (frequencyMHz) {`
- L398: `private fun unquote(value: String?): String? {`
- L407: `private fun ByteArray.toMacAddressString(): String =`
- L410: `private fun sleep(nanos: Long) {`
- L419: `private fun deadlineAfter(timeoutMillis: Long): Long {`
- L425: `private fun remainingNanos(deadlineNanos: Long): Long =`
- L428: `private class ManualApConfiguration(`
- L436: `private class LocalHotspotInterface(`
- L461: `private fun ManualHotspotSecurity.toIap2Security(): Iap2WirelessSecurity = when (this) {`

## shared/src/main/java/com/shilapi/xcertplay/network/VpnTunnelCompat.kt

状态：added

- L19: `object VpnTunnelCompat {`
- L32: `fun configureBlocking(builder: VpnService.Builder): Boolean {`
- L43: `fun establish(builder: VpnService.Builder, linkLocal: String): ParcelFileDescriptor {`
- L59: `fun read(input: FileInputStream, buffer: ByteArray, running: () -> Boolean): Int {`
- L84: `fun readNonBlocking(input: FileInputStream, buffer: ByteArray, running: () -> Boolean): Int {`
- L101: `fun write(output: FileOutputStream, buffer: ByteArray, length: Int, running: () -> Boolean): Boolean =`
- L105: `fun write(`
- L125: `fun isWouldBlock(error: IOException): Boolean {`
- L131: `fun closeQuietly(descriptor: ParcelFileDescriptor?) {`

## shared/src/main/java/com/shilapi/xcertplay/network/WifiP2pGroupManager.kt

状态：modified

- L41: `class WifiP2pGroupManager(`
- L75: `override fun start(timeoutMillis: Long): WirelessHotspotInfo {`
- L220: `override fun onCarPlayConfirmed() = synchronized(stateLock) {`
- L229: `override fun close() {`
- L260: `private fun createChannelListener(`
- L263: `override fun onChannelDisconnected() {`
- L268: `private fun createActionListener(`
- L272: `override fun onSuccess() {`
- L289: `override fun onFailure(reason: Int) {`
- L301: `private fun failAttempt(attempt: StartAttempt, failure: IOException) {`
- L310: `private fun awaitGroupCreated(`
- L335: `private fun awaitUsableGroup(`
- L414: `private fun requestGroupInfo(`
- L440: `private fun requestConnectionAddress(`
- L458: `private fun await(latch: CountDownLatch, timeoutNanos: Long): Boolean = try {`
- L466: `private fun interfaceAddress(interfaceName: String): InetAddress? {`
- L485: `private fun awaitInterfaceAddress(`
- L501: `private data class Station(val state: SupplicantState?, val reportedFrequency: Int?) {`
- L509: `private fun readStation(): Station = runCatching {`
- L519: `private fun checkPrerequisites(station: Station) {`
- L551: `private fun logP2pState(attempt: StartAttempt, channel: WifiP2pManager.Channel) {`
- L560: `private fun interfaceHardwareAddress(interfaceName: String): String? =`
- L566: `private fun networkInterface(interfaceName: String): NetworkInterface? = try {`
- L572: `private fun groupSecurity(group: WifiP2pGroup): Iap2WirelessSecurity {`
- L583: `// bring-up that has already resolved its SSID, band, interface and host address.`
- L594: `private fun randomCredentials(): Credentials = Credentials(`
- L599: `private fun randomToken(length: Int): String =`
- L606: `private fun ensureStartActive(attempt: StartAttempt) {`
- L612: `private fun ensureStartActiveLocked(attempt: StartAttempt) {`
- L619: `private fun cleanupFailedStart(attempt: StartAttempt) {`
- L641: `private fun removeGroupBlocking(channel: WifiP2pManager.Channel, expectedName: String? = observedCreatedName ?: requestedName) {`
- L645: `private fun removeGroup(channel: WifiP2pManager.Channel, waitForCallback: Boolean,`
- L664: `override fun onSuccess() {`
- L668: `override fun onFailure(reason: Int) {`
- L686: `private fun waitNanos(nanos: Long) {`
- L697: `private fun deadlineAfter(timeoutMillis: Long): Long {`
- L703: `private fun remainingNanos(deadlineNanos: Long): Long =`
- L706: `private fun failureReason(reason: Int): String = when (reason) {`
- L714: `private fun is5Ghz(frequencyMHz: Int): Boolean = frequencyMHz in 5150..5895`
- L716: `private class StartAttempt {`
- L725: `private class CreateRequest {`
- L730: `private class Credentials(`

## shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt

状态：modified

- L94: `sealed class CarPlayStatus {`
- L95: `data object DiscoveringMfi : CarPlayStatus()`
- L96: `data object WaitingForMfi : CarPlayStatus()`
- L97: `data object RequestingMfiPermission : CarPlayStatus()`
- L98: `data object MfiReady : CarPlayStatus()`
- L99: `data object StartingHotspot : CarPlayStatus()`
- L100: `data class HotspotReady(`
- L108: `data object WaitingForPairedIphone : CarPlayStatus()`
- L109: `data object ConnectingBluetooth : CarPlayStatus()`
- L110: `data object RunningWireless : CarPlayStatus()`
- L111: `data object WirelessActive : CarPlayStatus()`
- L112: `data object DiscoveringIphone : CarPlayStatus()`
- L113: `data object WaitingForIphone : CarPlayStatus()`
- L114: `data object RequestingIphonePermission : CarPlayStatus()`
- L115: `data object WaitingForReenumeration : CarPlayStatus()`
- L116: `data object SelectingConfiguration : CarPlayStatus()`
- L117: `data object OpeningDataPaths : CarPlayStatus()`
- L118: `data object Pairing : CarPlayStatus()`
- L119: `data object ConnectingControl : CarPlayStatus()`
- L120: `data object AttachingNetwork : CarPlayStatus()`
- L121: `data object RunningControl : CarPlayStatus()`
- L122: `data object ControlEnded : CarPlayStatus()`
- L123: `data class Failed(val message: String, val wifiResetRequired: Boolean = false) : CarPlayStatus()`
- L126: `internal fun isWirelessHandoffInProgress(`
- L137: `* main thread. This class is the integration seam only and is not evidence of hardware operation.`
- L139: `class CarPlayController(`
- L162: `private enum class Phase { IDLE, MFI, WIRELESS, IPHONE, REENUMERATION, DATAPATHS, CONTROL }`
- L222: `fun sendVideoMessage(streamId: Long, message: Map<String, Any?>): Boolean =`
- L248: `override fun onServiceConnected(name: ComponentName, binder: IBinder) {`
- L253: `override fun onServiceDisconnected(name: ComponentName) {`
- L260: `override fun onSessionActive(session: AirPlaySession) {`
- L275: `override fun onSessionEnded(session: AirPlaySession) {`
- L293: `override fun onTransportError(message: String) {`
- L298: `override fun onDeviceInfo(session: AirPlaySession, info: AirPlayDeviceInfo) {`
- L308: `override fun onHostUiRequested(session: AirPlaySession) {`
- L319: `override fun onRemoteControlMessage(session: AirPlaySession, streamId: Long, message: Map<String, Any?>) {`
- L323: `override fun onVideoPlaybackUiRequested(session: AirPlaySession) {`
- L328: `override fun onCommand(session: AirPlaySession, type: String, params: Map<String, Any?>) {`
- L349: `override fun onDebugLog(message: String) {`
- L354: `fun attachUi(`
- L365: `fun isClosed(): Boolean = closed`
- L367: `fun hasActiveAirPlayAttachment(): Boolean = synchronized(lifecycleLock) {`
- L371: `fun start() {`
- L391: `fun reconnectMfi() = synchronized(lifecycleLock) {`
- L398: `fun reconnectIphone() = synchronized(lifecycleLock) {`
- L409: `fun sendTouch(contacts: List<AirPlayContact>): Boolean {`
- L422: `fun requestSiri(): Boolean {`
- L433: `fun sendMediaButton(index: Int): Boolean {`
- L444: `override fun close() {`
- L505: `private fun applyClusterUi(shown: Boolean) = synchronized(clusterUiLock) {`
- L520: `fun awaitClosed(timeoutMillis: Long): Boolean {`
- L531: `private fun onRouteFrame(frame: com.shilapi.xcertplay.iap2.wire.Iap2Frame) {`
- L543: `private fun onArtworkTransfer(transfer: com.shilapi.xcertplay.transport.Iap2ArtworkTransfer) {`
- L548: `private fun startMfi() {`
- L582: `private fun openLocalMfi(directory: java.io.File) {`
- L602: `private fun openRemoteMfi() {`
- L625: `private fun checkCh341Mfi(host: Ch341UsbHost) {`
- L636: `private fun openLinuxMfi() {`
- L658: `private fun requestCh341Permission(device: UsbDevice) {`
- L676: `private fun onCh341Permission(result: Ch341UsbHost.PermissionResult) {`
- L693: `private fun pollCh341Permission(device: UsbDevice) {`
- L697: `override fun run() {`
- L719: `private fun openCh341(device: UsbDevice) {`
- L773: `private data class MfiCandidateProbe(`
- L784: `fun describe(): String {`
- L793: `private fun hex(value: Int?): String =`
- L797: `private fun probeMfiCandidate(transport: I2cTransport, address7Bit: Int): MfiCandidateProbe = try {`
- L817: `private fun readMfiRegister(`
- L831: `private fun preferCertificateBearingAddress(`
- L847: `private fun waitForMfi() {`
- L859: `private fun startPhone() {`
- L880: `private fun startWireless() {`
- L894: `private fun restartWireless() {`
- L908: `private fun runWireless(generation: Int) {`
- L1027: `debugLog("wireless Bonjour services started mode=interface iface=${hotspotInfo.interfaceName ?: "unknown"}")`
- L1158: `private fun startWirelessTunnelControl(stream: BlockingDuplexByteStream): Boolean {`
- L1228: `private fun wirelessSessionListener(generation: Int): AirPlaySessionListener =`
- L1230: `override fun onSessionActive(session: AirPlaySession) {`
- L1236: `override fun onSessionEnded(session: AirPlaySession) {`
- L1242: `override fun onVideoFrameRendered(session: AirPlaySession) {`
- L1248: `override fun onRemoteControlMessage(session: AirPlaySession, streamId: Long, message: Map<String, Any?>) =`
- L1251: `override fun onVideoPlaybackUiRequested(session: AirPlaySession) =`
- L1255: `private fun onWirelessTunnelReady(generation: Int) {`
- L1272: `private fun maybeCompleteWirelessHandoff() {`
- L1296: `private fun armWirelessHandoffWatchdog(generation: Int) {`
- L1344: `private fun closeBluetoothBootstrapTransport() {`
- L1358: `private fun startIphone() {`
- L1367: `private fun checkIphoneAvailability() {`
- L1383: `private fun requestIphonePermission(device: UsbDevice) {`
- L1387: `private fun doRequestIphonePermission(device: UsbDevice) {`
- L1408: `private fun onIphonePermission(result: IphoneUsbHost.PermissionResult) {`
- L1463: `private fun pollIphonePermission(device: UsbDevice) {`
- L1467: `override fun run() {`
- L1489: `private fun beginReenumeration(device: UsbDevice) {`
- L1503: `private fun onIphoneAttached(device: UsbDevice) {`
- L1513: `private fun scheduleAvailabilityPoll(phase: Phase, check: () -> Unit) {`
- L1529: `private fun openDataPaths(device: UsbDevice) {`
- L1550: `private fun openNcm(device: UsbDevice): NcmUsbBridge {`
- L1588: `private fun runStack(usbSession: Iap2UsbSession, ncm: NcmUsbBridge) {`
- L1653: `fun wireSummary(bytes: ByteArray): String {`
- L1656: `fun value(index: Int) = bytes[index].toInt() and 0xff`
- L1662: `override fun send(data: ByteArray) {`
- L1677: `override fun recv(maxBytes: Int, timeoutMillis: Long): ByteArray? {`
- L1693: `override fun close() {`
- L1755: `private fun pairNewRecord(client: LockdownPairingClient): LockdownPairRecord =`
- L1764: `private fun rejectedPairRecordError(error: Throwable): String? {`
- L1775: `private fun isBluetoothHandoffCommand(type: String): Boolean =`
- L1779: `private fun startWirelessHotspot(generation: Int): WirelessHotspotInfo {`
- L1826: `private fun isStaleWirelessRun(generation: Int): Boolean =`
- L1829: `private fun selectWirelessBluetoothDevice(adapter: BluetoothAdapter): BluetoothDevice {`
- L1872: `private fun connectBluetoothSocket(socket: BluetoothSocket, address: String) {`
- L1915: `private fun logBluetoothConnectionSnapshot(device: BluetoothDevice, point: String) {`
- L1935: `private fun closeWirelessStack(service: CarPlayVpnService? = vpnService) {`
- L1960: `private fun isBluetoothDeviceConnected(device: BluetoothDevice): Boolean = try {`
- L1970: `private fun connectedBluetoothDevices(adapter: BluetoothAdapter): Set<BluetoothDevice> =`
- L1984: `override fun onServiceConnected(profileId: Int, proxy: BluetoothProfile) {`
- L1997: `override fun onServiceDisconnected(profileId: Int) {`
- L2009: `private fun accessoryBluetoothMac(adapter: BluetoothAdapter): String {`
- L2028: `private fun hostAddressText(address: InetAddress): String {`
- L2036: `private fun closeBestEffort(name: String, close: () -> Unit) {`
- L2049: `private fun controlLoopTimeoutMillis(): Long = when {`
- L2055: `private fun attachVpn(ncm: NcmUsbBridge, hostMac: ByteArray): Boolean {`
- L2099: `private fun ByteArray.macString(): String =`
- L2102: `private fun awaitVpnService(): CarPlayVpnService? {`
- L2113: `private fun bindVpn() {`
- L2128: `private fun unbindVpn() {`
- L2139: `private fun closeReceivers() {`
- L2152: `private fun closeMfiSession() {`
- L2166: `private fun fail(error: Throwable) {`
- L2172: `private fun debugLog(message: String) {`
- L2181: `private fun connectionDiagnostic(message: String) {`
- L2191: `private fun diagnosticFailureClass(error: Throwable): String =`
- L2194: `private fun elapsedMillis(startedNanos: Long): Long =`
- L2197: `private fun debugLog(message: String, error: Throwable) {`
- L2208: `private fun onStatus(status: CarPlayStatus) {`
- L2220: `private fun CarPlayStatus.debugLogMessage(): String = when (this) {`

## shared/src/main/java/com/shilapi/xcertplay/transport/Ch341UsbHost.kt

状态：modified

- L22: `* This class intentionally stops before the CH341 I2C stream protocol. [openAsync] executes the`
- L26: `class Ch341UsbHost(`
- L33: `sealed class PermissionRequest {`
- L34: `data class AlreadyGranted(val device: UsbDevice) : PermissionRequest()`
- L35: `data class Requested(val device: UsbDevice) : PermissionRequest()`
- L38: `sealed class PermissionResult {`
- L39: `data class Granted(val device: UsbDevice) : PermissionResult()`
- L40: `data class Denied(val device: UsbDevice) : PermissionResult()`
- L43: `sealed class OpenResult {`
- L44: `data class Connected(val session: Ch341UsbSession) : OpenResult()`
- L45: `data class Failed(val error: I2cTransportException) : OpenResult()`
- L48: `fun discover(): List<UsbDevice> =`
- L52: `fun requestPermission(device: UsbDevice): PermissionRequest {`
- L61: `fun parsePermissionResult(intent: Intent): PermissionResult? {`
- L73: `fun registerPermissionReceiver(onResult: (PermissionResult) -> Unit): Closeable {`
- L75: `override fun onReceive(context: Context, intent: Intent) {`
- L89: `fun openAsync(device: UsbDevice, executor: Executor, callback: (OpenResult) -> Unit) {`
- L99: `private fun open(device: UsbDevice): Ch341UsbSession {`
- L105: `?: throw I2cTransportException.DeviceUnavailable("No USB interface has both bulk IN and OUT endpoints")`
- L116: `private fun requireConfiguredDevice(device: UsbDevice) {`
- L122: `private fun permissionPendingIntent(): PendingIntent {`
- L132: `private fun Intent.usbDevice(): UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {`
- L139: `private fun findBulkEndpoints(device: UsbDevice): BulkEndpoints? {`
- L157: `private data class BulkEndpoints(`
- L165: `class Ch341UsbSession internal constructor(`
- L174: `internal fun bulkWrite(data: ByteArray, timeoutMillis: Int) {`
- L194: `internal fun bulkReadAtMost(`
- L251: `override fun close() {`
- L258: `private fun transfer(`
- L297: `private fun clearEndpointHalt(endpoint: UsbEndpoint, operation: String) {`
- L316: `private fun ByteArray.toHexPreview(): String = joinToString(" ") { "%02x".format(it) }`

## shared/src/main/java/com/shilapi/xcertplay/transport/ConfigurationDescriptorScanner.kt

状态：added

- L6: `* Configuration id and per-interface alternate settings are needed on Android 4.3, where the typed`
- L13: `* bNumInterfaces (offset 4) bounds how many interface descriptors belong to the configuration, so`
- L14: `* alternates are keyed by interface index rather than by descriptor order.`
- L16: `object ConfigurationDescriptorScanner {`
- L26: `* One interface descriptor inside the selected configuration: its number, every alternate`
- L31: `data class InterfaceRecord(`
- L41: `fun configurationId(descriptors: ByteArray): Int {`
- L47: `* Every interface descriptor belonging to the configuration selected by`
- L50: `fun interfaceRecords(descriptors: ByteArray, selectedConfigurationId: Int? = null): List<InterfaceRecord> {`
- L76: `// Endpoint descriptors belong to the interface descriptor that precedes them.`
- L98: `/** Alternate setting currently selected for each interface index, keyed 0-based. */`
- L99: `fun alternateSettingsByIndex(descriptors: ByteArray): Map<Int, Int> {`
- L106: `// bound the walk: interface 2 may contribute alt 0 and alt 1 entries. Walk every descriptor`
- L108: `// descriptors that sit between interface entries.`
- L117: `// ordered by interface number, so index and bInterfaceNumber coincide, but reading`
- L131: `fun findConfiguration(descriptors: ByteArray, selectedConfigurationId: Int? = null): Int? {`
- L150: `private fun findBestConfiguration(descriptors: ByteArray): Int? {`
- L170: `private fun findConfigurationById(descriptors: ByteArray, configurationId: Int): Int? {`
- L187: `private fun u8(bytes: ByteArray, offset: Int): Int =`
- L190: `private fun u16(bytes: ByteArray, offset: Int): Int {`

## shared/src/main/java/com/shilapi/xcertplay/transport/Iap2CsmChannel.kt

状态：modified

- L18: `class Iap2CsmChannel private constructor(`
- L37: `fun awaitReady(timeoutMillis: Long): Boolean {`
- L63: `fun send(frame: Iap2Frame, timeoutMillis: Long = DEFAULT_SEND_TIMEOUT_MILLIS) {`
- L94: `fun recv(timeoutMillis: Long): Iap2Frame? {`
- L126: `override fun close() {`
- L145: `private fun enqueue(frame: Iap2Frame) {`
- L154: `private fun takeReceived(): Iap2Frame? {`
- L160: `private fun checkOpen() {`
- L168: `private fun failClosed(failure: Throwable): Nothing {`
- L180: `private fun fatal(error: Error): Nothing {`
- L190: `private fun terminalize(failure: Throwable): Throwable {`
- L202: `private fun deadlineAfter(timeoutMillis: Long): Long {`
- L206: `private fun remainingMillis(deadlineNanos: Long): Long {`
- L212: `private fun requireTimeout(timeoutMillis: Long) {`
- L226: `fun open(`
- L233: `fun openWireless(`
- L240: `fun openTunnel(`

## shared/src/main/java/com/shilapi/xcertplay/transport/Iap2LinkChannel.kt

状态：modified

- L14: `* channel.  This class owns and closes [underlying].`
- L20: `class Iap2LinkChannel private constructor(`
- L26: `private data class Command(val sessionId: Int, val payload: ByteArray)`
- L47: `fun awaitReady(timeoutMillis: Long): Boolean {`
- L60: `fun peerMaxControlPayloadBytes(): Int? = synchronized(lock) { peerMaxControlPayloadBytes }`
- L67: `fun sendControl(bytes: ByteArray): Boolean {`
- L82: `internal fun sendControlAwaitCapacity(bytes: ByteArray, timeoutMillis: Long): Boolean {`
- L99: `fun recvControl(timeoutMillis: Long): ByteArray? {`
- L116: `override fun close() {`
- L146: `private fun runPump() {`
- L180: `private fun drainCommands(engine: Iap2LinkEngine) {`
- L197: `private fun flush(engine: Iap2LinkEngine) {`
- L203: `private fun drainEvents(engine: Iap2LinkEngine): Boolean {`
- L257: `private fun finish(failure: Throwable?) {`
- L269: `private fun finishLocked(failure: Throwable?) {`
- L276: `private fun terminalLocked(failure: Throwable?) {`
- L289: `private fun throwTerminalFailureLocked() {`
- L293: `private fun isClosing(): Boolean = synchronized(lock) { closing || terminated }`
- L295: `private fun closeUnderlyingOnce(): Throwable? {`
- L309: `private fun hasCommandCapacityLocked(bytes: Int): Boolean =`
- L313: `private fun enqueueCommandLocked(sessionId: Int, bytes: ByteArray): Boolean {`
- L322: `private fun waitFor(timeoutMillis: Long, readyPredicate: () -> Boolean): Boolean {`
- L344: `private fun combineFailures(first: Throwable?, second: Throwable?): Throwable? {`
- L373: `fun open(`
- L384: `fun openWireless(`
- L395: `fun openTunnel(`
- L402: `private fun nowMillis(): Long = System.nanoTime() / NANOS_PER_MILLISECOND`

## shared/src/main/java/com/shilapi/xcertplay/transport/Iap2LocationClient.kt

状态：modified

- L12: `data class CarPlayLocationFix(`
- L32: `interface Iap2LocationProvider : Closeable {`
- L34: `fun onRequested(components: Set<Int>) = Unit`
- L37: `fun start(): Boolean`
- L39: `fun stop()`
- L41: `fun latestNmea(): String?`
- L43: `override fun close() {`
- L49: `object NmeaLocationEncoder {`
- L50: `fun encode(fix: CarPlayLocationFix): String {`
- L92: `private fun degreesToNmea(value: Double, latitude: Boolean): NmeaCoordinate {`
- L109: `private fun checksum(body: String): String {`
- L115: `private fun ofEpochMilli(millis: Long): Timestamp = Timestamp(millis)`
- L117: `private fun format(format: String, vararg arguments: Any): String =`
- L120: `private data class NmeaCoordinate(val value: String, val hemisphere: String)`
- L122: `private class Timestamp(millis: Long) {`
- L145: `class Iap2LocationRequest {`
- L155: `class Iap2LocationReporter(`
- L168: `fun handle(frame: Iap2Frame, send: (Iap2Frame) -> Unit): Boolean = when (frame.messageId) {`
- L193: `fun tick(send: (Iap2Frame) -> Unit) {`
- L215: `fun pollTimeout(remainingMillis: Long): Long = when {`
- L221: `private fun sinceAttemptMillis() = (nanoTime() - lastAttemptNanos) / 1_000_000`
- L223: `private fun start(send: (Iap2Frame) -> Unit) {`
- L229: `private fun startProvider(): Boolean {`
- L239: `private fun sendLatest(send: (Iap2Frame) -> Unit) {`
- L254: `object Iap2LocationMessages {`
- L263: `fun requestedComponents(frame: Iap2Frame): Set<Int> =`
- L266: `fun locationInformation(nmeaSentence: String): Iap2Frame {`

## shared/src/main/java/com/shilapi/xcertplay/transport/IphoneCarPlayConfiguration.kt

状态：modified

- L19: `object IphoneCarPlayConfiguration {`
- L33: `fun find(device: UsbDevice): UsbConfiguration? {`
- L47: `fun describe(configuration: UsbConfiguration): String =`
- L57: `fun usbMuxInterface(configuration: UsbConfiguration): UsbInterface? =`
- L64: `fun usbMuxInterface(layout: UsbDeviceLayout): UsbInterfaceView? =`
- L71: `fun hasUsbMux(configuration: UsbConfiguration): Boolean = usbMuxInterface(configuration) != null`
- L73: `fun usbMuxEndpoints(usbInterface: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {`
- L94: `fun hasCdcNcm(configuration: UsbConfiguration): Boolean =`
- L99: `private fun hasAppleEthernet(configuration: UsbConfiguration): Boolean =`
- L107: `fun readLayout(device: UsbDevice, connection: UsbDeviceConnection): UsbDeviceLayout =`
- L110: `fun hasCarPlayFunction(layout: UsbDeviceLayout): Boolean =`

## shared/src/main/java/com/shilapi/xcertplay/transport/IphoneUsbHost.kt

状态：modified

- L28: `class IphoneUsbMatcher private constructor(`
- L46: `fun matches(vendorId: Int, productId: Int): Boolean =`
- L55: `fun appleVendor(): IphoneUsbMatcher = IphoneUsbMatcher(null, allowAnyAppleProduct = true)`
- L68: `class IphoneUsbHost(`
- L76: `sealed class PermissionRequest {`
- L77: `data class AlreadyGranted(val device: UsbDevice) : PermissionRequest()`
- L78: `data class Requested(val device: UsbDevice) : PermissionRequest()`
- L81: `sealed class PermissionResult {`
- L82: `data class Granted(val device: UsbDevice) : PermissionResult()`
- L83: `data class Denied(val device: UsbDevice) : PermissionResult()`
- L86: `sealed class TransitionResult {`
- L88: `data object ReenumerationRequested : TransitionResult()`
- L90: `data class Failed(val error: IphoneUsbException) : TransitionResult()`
- L93: `sealed class Iap2SessionResult {`
- L94: `data class Connected(val session: Iap2UsbSession) : Iap2SessionResult()`
- L95: `data class Failed(val error: IphoneUsbException) : Iap2SessionResult()`
- L98: `fun discover(): List<UsbDevice> =`
- L105: `fun hasCarPlayConfiguration(device: UsbDevice): Boolean {`
- L130: `fun requestPermission(device: UsbDevice): PermissionRequest {`
- L139: `fun parsePermissionResult(intent: Intent): PermissionResult? {`
- L151: `fun parseAttachedDevice(intent: Intent): UsbDevice? {`
- L158: `fun registerPermissionReceiver(onResult: (PermissionResult) -> Unit): Closeable =`
- L162: `fun registerAttachReceiver(onAttached: (UsbDevice) -> Unit): Closeable =`
- L171: `fun requestCarPlayReenumerationAsync(`
- L206: `fun openIap2UsbSessionAsync(`
- L240: `private fun runTransition(`
- L263: `private fun openIap2UsbSession(device: UsbDevice): Iap2UsbSession {`
- L295: `?: throw IphoneUsbException.Protocol("USBMUX interface exposes no bulk endpoint pair")`
- L305: `throw IphoneUsbException.DeviceUnavailable("Android could not claim USBMUX interface 1")`
- L316: `private fun requireConfiguredDevice(device: UsbDevice) {`
- L322: `private fun permissionPendingIntent(): PendingIntent {`
- L332: `private fun registerReceiver(filter: IntentFilter, onReceive: (Intent) -> Unit): Closeable {`
- L334: `override fun onReceive(context: Context, intent: Intent) = onReceive(intent)`
- L347: `private fun Intent.usbDevice(): UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {`
- L369: `class Iap2UsbSession internal constructor(`
- L381: `fun write(data: ByteArray, timeoutMillis: Int) = synchronized(writeLock) {`
- L403: `fun read(timeoutMillis: Long): ByteArray? = synchronized(readLock) {`
- L457: `override fun close() {`
- L467: `private fun checkOpen() {`
- L471: `private fun checkOpenLocked() {`
- L476: `private fun readSynchronously(timeoutMillis: Long): ByteArray? {`
- L492: `private fun drainCancelledRead(request: UsbRequest) {`
- L506: `private fun failSession(message: String, cause: Throwable? = null): IphoneUsbException.DeviceUnavailable {`
- L514: `private fun requestDiagnostics(timeoutMillis: Long, bufferBytes: Int? = null): String = buildString {`
- L527: `private fun describeUsbEndpoint(endpoint: UsbEndpoint): String =`
- L532: `sealed class IphoneUsbException(message: String, cause: Throwable? = null) : IOException(message, cause) {`
- L533: `class PermissionDenied(message: String, cause: Throwable? = null) : IphoneUsbException(message, cause)`
- L534: `class DeviceUnavailable(message: String, cause: Throwable? = null) : IphoneUsbException(message, cause)`
- L535: `class TimedOut(message: String, cause: Throwable? = null) : IphoneUsbException(message, cause)`
- L536: `class Protocol(message: String) : IphoneUsbException(message)`

## shared/src/main/java/com/shilapi/xcertplay/transport/LegacyUsbTransfer.kt

状态：added

- L24: `object LegacyUsbTransfer {`
- L47: `fun interface ChunkTransfer {`
- L52: `fun transfer(offset: Int, length: Int, timeoutMillis: Int): Int`
- L62: `fun endpointReader(`
- L71: `fun endpointWriter(`
- L84: `fun readOnce(`
- L106: `fun writeAll(`
- L129: `fun remainingMillis(deadlineNanos: Long): Int {`

## shared/src/main/java/com/shilapi/xcertplay/transport/LockdownPairRecord.kt

状态：modified

- L26: `class LockdownPairRecord private constructor(`
- L52: `fun toPairRequestDictionary(): LockdownPlistValue.Dictionary = LockdownPlistValue.Dictionary(`
- L62: `override fun toString(): String = "LockdownPairRecord(redacted)"`
- L66: `fun restore(`
- L101: `internal fun create(`
- L122: `object LockdownPairRecordGenerator {`
- L124: `fun generate(`
- L147: `internal class CertificateMaterial(`
- L168: `private object CertificateMaterialGenerator {`
- L176: `fun generate(devicePublicKeyPem: ByteArray): CertificateMaterial {`
- L222: `private fun certificate(`
- L258: `private fun verifyGeneratedCertificate(tbs: ByteArray, signature: ByteArray, signingPublicKey: PublicKey) {`
- L266: `private fun rootExtensions(): ByteArray = sequence(`
- L270: `private fun leafExtensions(`
- L292: `private fun extension(oid: String, critical: Boolean, value: ByteArray): ByteArray =`
- L296: `private fun parsePkcs1RsaPublicKey(pem: ByteArray): RSAPublicKey {`
- L311: `private fun pkcs1PublicKey(publicKey: RSAPublicKey): ByteArray = sequence(`
- L316: `private fun distinguishedName(commonName: String?): ByteArray =`
- L319: `private fun pem(label: String, der: ByteArray): ByteArray {`
- L324: `private fun algorithmIdentifier(oid: String): ByteArray = sequence(objectIdentifier(oid), der(0x05, ByteArray(0)))`
- L325: `private fun sequence(vararg values: ByteArray): ByteArray = der(0x30, concatenate(*values))`
- L326: `private fun set(vararg values: ByteArray): ByteArray = der(0x31, concatenate(*values))`
- L327: `private fun explicit(number: Int, value: ByteArray): ByteArray = der(0xa0 + number, value)`
- L328: `private fun utf8String(value: String): ByteArray = der(0x0c, value.toByteArray(CharsetsCompat.UTF_8))`
- L330: `private fun certificateTime(valueMillis: Long): ByteArray {`
- L342: `private fun integer(value: BigInteger): ByteArray {`
- L347: `private fun boolean(value: Boolean): ByteArray = der(0x01, byteArrayOf(if (value) 0xff.toByte() else 0))`
- L348: `private fun octetString(value: ByteArray): ByteArray = der(0x04, value)`
- L349: `private fun bitString(value: ByteArray, unusedBits: Int = 0): ByteArray {`
- L354: `private fun objectIdentifier(value: String): ByteArray {`
- L357: `"Invalid object identifier"`
- L361: `arcs.drop(2).forEach { arc -> require(arc >= 0) { "Invalid object identifier" }; writeBase128(output, arc) }`
- L365: `private fun writeBase128(output: ByteArrayOutputStream, value: Long) {`
- L378: `private fun der(tag: Int, content: ByteArray): ByteArray {`
- L393: `private fun concatenate(vararg values: ByteArray): ByteArray {`
- L399: `private class DerReader(private val bytes: ByteArray) {`
- L402: `fun readConstructed(expectedTag: Int): DerReader = DerReader(readValue(expectedTag))`
- L404: `fun readPositiveInteger(): BigInteger {`
- L413: `fun exhausted(): Boolean = offset == bytes.size`
- L415: `private fun readValue(expectedTag: Int): ByteArray {`

## shared/src/main/java/com/shilapi/xcertplay/transport/LockdownPlistChannel.kt

状态：modified

- L11: `sealed class LockdownPlistValue {`
- L12: `data class Dictionary(val entries: Map<String, LockdownPlistValue>) : LockdownPlistValue()`
- L13: `data class Text(val value: String) : LockdownPlistValue()`
- L14: `data class Integer(val value: Long) : LockdownPlistValue()`
- L15: `data class Boolean(val value: kotlin.Boolean) : LockdownPlistValue()`
- L17: `class Data(bytes: ByteArray) : LockdownPlistValue() {`
- L28: `* A frame is a four-byte big-endian XML plist byte length followed by UTF-8 XML. This class does`
- L32: `class LockdownPlistChannel(`
- L52: `fun send(message: LockdownPlistValue.Dictionary) = synchronized(ioLock) {`
- L58: `fun receive(timeoutMillis: Long = defaultTimeoutMillis): LockdownPlistValue.Dictionary = synchronized(ioLock) {`
- L64: `fun request(`
- L80: `fun detach(): BlockingDuplexByteStream = synchronized(ioLock) {`
- L92: `override fun close() {`
- L102: `private fun sendLocked(message: LockdownPlistValue.Dictionary) {`
- L113: `private fun receiveLocked(timeoutMillis: Long): LockdownPlistValue.Dictionary {`
- L125: `private fun readFully(byteCount: Int, deadlineNanos: Long): ByteArray {`
- L145: `private fun parse(xml: ByteArray): LockdownPlistValue.Dictionary {`
- L182: `private fun parseValue(parser: XmlPullParser, depth: Int): LockdownPlistValue {`
- L226: `private fun encode(message: LockdownPlistValue.Dictionary): String = buildString {`
- L234: `private fun StringBuilder.appendValue(value: LockdownPlistValue, depth: Int) {`
- L257: `private fun StringBuilder.appendEscaped(value: String): StringBuilder = apply {`
- L272: `private fun simpleText(parser: XmlPullParser, name: String): String {`
- L290: `private fun requireEmpty(parser: XmlPullParser, name: String) {`
- L296: `private fun checkOpen() {`
- L302: `private fun validateTimeout(timeoutMillis: Long) {`
- L318: `fun putU32(target: ByteArray, offset: Int, value: Long) {`
- L325: `fun readU32(source: ByteArray, offset: Int): Long =`

## shared/src/main/java/com/shilapi/xcertplay/transport/LockdownTlsEngineFactory.kt

状态：modified

- L25: `object LockdownTlsEngineFactory {`
- L27: `fun create(pairRecord: LockdownPairRecord): SSLEngine {`
- L67: `private fun decodePkcs8Pem(pem: ByteArray): ByteArray {`
- L81: `private fun ByteArray.indexOf(needle: ByteArray, startIndex: Int = 0): Int {`
- L97: `private object UsbLockdownTrustManager : X509TrustManager {`
- L98: `override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit`
- L100: `override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit`
- L102: `override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()`

## shared/src/main/java/com/shilapi/xcertplay/transport/NcmFunctionDiscovery.kt

状态：modified

- L11: `* Finds the NCM control/data interface pair inside an active iPhone configuration.`
- L14: `* setting 1, the setting that carries the bulk endpoints. This class only reads descriptors.`
- L16: `object NcmFunctionDiscovery {`
- L25: `data class NcmFunction(`
- L32: `* Alternate setting the data interface must be switched to before bulk transfers. On API`
- L41: `fun find(configuration: UsbConfiguration): NcmFunction? = findCdcNcm(configuration, null)`
- L44: `fun find(configuration: UsbConfiguration, layout: UsbDeviceLayout?): NcmFunction? =`
- L52: `fun find(layout: UsbDeviceLayout): NcmFunction? {`
- L77: `private fun findCdcNcm(configuration: UsbConfiguration, layout: UsbDeviceLayout?): NcmFunction? {`
- L99: `private fun interfaces(configuration: UsbConfiguration): List<UsbInterface> =`
- L103: `private fun bulkEndpoints(usbInterface: UsbInterface): Pair<UsbEndpoint, UsbEndpoint>? {`

## shared/src/main/java/com/shilapi/xcertplay/transport/NcmUsbBridge.kt

状态：modified

- L25: `class NcmUsbBridge internal constructor(`
- L63: `fun send(frame: ByteArray, timeoutMillis: Int) = synchronized(writeLock) {`
- L106: `fun recv(timeoutMillis: Long): ByteArray? {`
- L128: `override fun close() {`
- L156: `private fun drainStatus(endpoint: UsbEndpoint) {`
- L185: `private fun ByteArray.hex(limit: Int): String =`
- L188: `private fun drainFrames() {`
- L209: `private fun appendBuffered(source: ByteArray, length: Int) {`
- L221: `private fun enqueueFrame(frame: ByteArray) {`
- L229: `private fun pollFrame(): ByteArray {`
- L236: `private fun readChunk(timeoutMillis: Long): Int? {`
- L282: `private fun readChunkSynchronously(timeoutMillis: Long): Int? {`
- L296: `private fun failSession(message: String, cause: Throwable? = null): IphoneUsbException.DeviceUnavailable {`
- L304: `private fun checkOpen() {`
- L308: `private fun checkOpenLocked() {`
- L313: `private fun readU16(source: ByteArray, offset: Int): Int =`
- L316: `private fun readU32(source: ByteArray, offset: Int): Int =`
- L332: `fun open(connection: UsbDeviceConnection, function: NcmFunctionDiscovery.NcmFunction): NcmUsbBridge {`
- L341: `// same interface id, so it must be claimed once and switched with setInterface.`
- L406: `private fun readNcmHostMac(connection: UsbDeviceConnection, controlInterfaceId: Int): ByteArray? {`
- L427: `private fun ethernetMacStringIndex(raw: ByteArray, controlInterfaceId: Int): Int? {`
- L449: `private fun ByteArray.macString(): String =`

## shared/src/main/java/com/shilapi/xcertplay/transport/UsbDeviceLayout.kt

状态：added

- L13: `* configuration id and per-interface alternate setting are recovered from rawDescriptors, which the`
- L17: `data class UsbDeviceLayout(`
- L21: `fun firstInterfaceMatching(predicate: (UsbInterfaceView) -> Boolean): UsbInterfaceView? =`
- L24: `fun describe(): String =`
- L29: `/** One interface plus the alternate setting currently selected for it. */`
- L30: `data class UsbInterfaceView(`
- L39: `fun endpoint(index: Int) = platform.getEndpoint(index)`
- L42: `object UsbDeviceLayoutReader {`
- L48: `fun rawDescriptors(connection: UsbDeviceConnection): ByteArray = runCatching {`
- L59: `fun read(device: UsbDevice, connection: UsbDeviceConnection): UsbDeviceLayout {`
- L64: `// USB Multiplex and the CDC-NCM control interface rather than blindly taking index 0.`
- L86: `fun fromDescriptors(device: UsbDevice, connection: UsbDeviceConnection): UsbDeviceLayout {`
- L96: `fun fromDescriptors(`
- L109: `// Match on endpoint addresses too: two alternates of one interface differ only there,`
- L110: `// so the endpoint set identifies which alternate the platform object represents.`
- L131: `fun activeConfigurationId(connection: UsbDeviceConnection): Int? {`

## shared/src/test/java/com/shilapi/xcertplay/compat/GeelyBluetoothDiagnosticsTest.kt

状态：added

- L26: `class GeelyBluetoothDiagnosticsTest {`
- L27: `@Test fun anwGettersUse10500DescriptorCodesAndExactOutputCapacities() {`
- L40: `@Test fun unsupportedTransactionIsNotReportedAsOffOrFalse() {`
- L49: `@Test fun unknownPowerCodeAndTruncatedRepliesRemainUnknown() {`
- L63: `@Test fun malformedPairedArrayCapacitiesAndReturnCodeDoNotBecomeEmptyList() {`
- L77: `@Test fun wrongAnwDescriptorStopsBeforeAnyTransaction() {`
- L85: `@Test fun ecarxGetterUsesItsOwnDescriptorAndTransactionAndRejectsNonBooleanValues() {`
- L102: `@Test fun bindTimeoutUnbindsAndLateServiceCallbackCannotStartBinderReads() {`
- L119: `@Test fun serviceDisconnectProducesUnknownAnwStateAndIndependentEcarxRead() {`
- L135: `@Test fun disconnectDuringBinderReadMarksAnwUnknownButKeepsEcarxResultSeparate() {`
- L159: `@Test fun busyCallbackCanBeCancelledBeforeMainQueueDelivery() {`
- L173: `private fun client(`
- L185: `private fun mainLooper() = shadowOf(Looper.getMainLooper())`
- L187: `private class ManualExecutor : Executor {`
- L189: `override fun execute(command: Runnable) { task = command }`
- L190: `fun runPending() { task?.run(); task = null }`
- L193: `private class FakeBinding : GeelyBluetoothDiagnostics.ServiceBinding {`
- L198: `override fun bind(context: Context, intent: Intent, connection: ServiceConnection): Boolean {`
- L205: `override fun unbind(context: Context, connection: ServiceConnection) {`
- L210: `private class FakeAnwBinder(`
- L226: `override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {`
- L259: `private class FakeEcarxBinder(`
- L269: `override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {`

## shared/src/test/java/com/shilapi/xcertplay/media/GeelyAudioCapabilitiesTest.kt

状态：added

- L6: `class GeelyAudioCapabilitiesTest {`
- L10: `@Test fun h52ProvidesSeparateMediaSpeechAndAlertRoutes() {`
- L19: `@Test fun stockAndroidDoesNotEnableVendorRouting() {`
- L23: `@Test fun h41NavigationConstantAloneIsInsufficient() {`
- L27: `@Test fun differingVendorMediaNumberIsRejected() {`
- L31: `@Test fun missingOrWrongModeDoesNotEnableProfile() {`
- L36: `@Test fun missingAlertExtensionFallsBackToSpeech() {`
- L42: `@Test fun unrelatedAlertNumberIsNotExposed() {`
- L46: `@Test fun alertStreamIsUsedOnlyForAlertAudioType() {`
- L54: `@Test fun automaticRouteAndRealVoiceCallStreamZeroAreDistinct() {`

## shared/src/test/java/com/shilapi/xcertplay/transport/ConfigurationDescriptorScannerTest.kt

状态：added

- L9: `* a device descriptor, more than one configuration, interface 0 with an endpoint, interface 2 with`
- L12: `class ConfigurationDescriptorScannerTest {`
- L14: `private fun deviceDescriptor(length: Int = 18) = ByteArray(length).also {`
- L19: `private fun configDescriptor(configurationId: Int, totalLength: Int, interfaceCount: Int) =`
- L26: `private fun interfaceDescriptor(number: Int, alternate: Int, ifaceClass: Int, subclass: Int = 0, protocol: Int = 0) =`
- L32: `/** Endpoint descriptor: 7 bytes, class 0xff/0xfe/0x03/0x01/0x01 and 8 endpoints. */`
- L33: `private fun endpointDescriptor(address: Int, attributes: Int) =`
- L40: `private fun classSpecificDescriptor(subtype: Int, length: Int = 5) =`
- L43: `private fun concat(vararg parts: ByteArray): ByteArray =`
- L46: `@Test fun readsConfigurationIdAndAlternateSettingsKeyedByInterfaceNumber() {`
- L55: `// Keyed by bInterfaceNumber, not by descriptor ordinal: the first interface here is 0 and`
- L56: `// the second is 2, because interface 1 does not exist on this configuration.`
- L63: `@Test fun scanContinuesPastShortEndpointAndClassSpecificDescriptors() {`
- L73: `// never reach interface 2.`
- L80: `@Test fun keepsTheHighestAlternateSettingForAnInterface() {`
- L90: `// alt 1 is the selected alternate and must win over alt 0 for interface 2.`
- L97: `@Test fun selectsTheConfigurationWithTheMostInterfaces() {`
- L115: `@Test fun emptyAndTruncatedDescriptorsDoNotThrow() {`
- L130: `@Test fun interfaceRecordsKeepEveryAlternateWithItsEndpoints() {`
- L151: `@Test fun interfaceRecordsAreScopedToTheSelectedConfiguration() {`
- L170: `@Test fun configurationZeroIsRejectedRatherThanSilentlyReselected() {`
- L192: `@Test fun anAbsentSelectedConfigurationIsRejected() {`
- L206: `@Test fun endpointDescriptorsDoNotLeakAcrossInterfaces() {`

## shared/src/test/java/com/shilapi/xcertplay/transport/LegacyUsbTransferTest.kt

状态：added

- L13: `class LegacyUsbTransferTest {`
- L17: `private fun withClock(body: () -> Unit) {`
- L27: `private fun deadline(millis: Long) = now + millis * 1_000_000L`
- L30: `private fun pattern(size: Int, seed: Int = 1) = ByteArray(size) { ((it + seed) and 0xFF).toByte() }`
- L32: `@Test fun readRequestsOneChunkAndNeverExceedsCapacity() {`
- L47: `@Test fun readHonoursANonZeroTargetOffset() {`
- L56: `@Test fun readReportsZeroAndNegativeWithoutInventingData() {`
- L66: `@Test fun readRejectsNonPositiveTimeoutAndOutOfRangeBuffers() {`
- L86: `@Test fun writeSplitsALongFrameAndWalksEveryByteInOrder() {`
- L110: `@Test fun shortWriteAdvancesSoTheWholeFrameIsSent() {`
- L133: `@Test fun writeReportsWhatWasTransferredWhenTheDeviceStops() {`
- L144: `@Test fun writeStopsWhenTheDeadlineHasPassed() {`
- L153: `@Test fun theTotalDeadlineIsSharedAndShrinksAcrossChunks() {`
- L170: `@Test fun writeStopsOnceTheSharedDeadlineIsExhausted() {`

## vendor/jmdns/build.gradle

状态：added


## vendor/jmdns/README.md

状态：added

- L16: `| impl/JmDNSImpl.openMulticastSocket | MulticastSocket is created unbound, setReuseAddress(true) is applied, then it is bound | new MulticastSocket(SocketAddress) binds *before* address reuse can be enabled, so the bind fails with EADDRINUSE whenever Android's own mdnsd already holds UDP 5353. Group selection, interface binding, joinGroup, TTL, closeMulticastSocket and error recovery are unchanged. |`
- L32: `ConcurrentMap.putIfAbsent / replace are *not* touched: they are original interface methods`

## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSConstants.java

状态：added

- L21: `public final class DNSConstants {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSLabel.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSOperationCode.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSOptionCode.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSRecordClass.java

状态：added

- L98: `* Checks if the class is unique`
- L101: `* @return <code>true</code> is the class is unique, <code>false</code> otherwise.`
- L109: `* @return class for name`
- L120: `logger.warn("Could not find record class for name: {}", name);`
- L127: `* @return class for name`
- L136: `logger.debug("Could not find record class for index: {}", index);`

## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSRecordType.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSResultCode.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/DNSState.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/constants/package-info.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSCache.java

状态：added

- L58: `public class DNSCache extends ConcurrentHashMap<String, List<DNSEntry>> {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSEntry.java

状态：added

- L27: `* DNS entry with a name, type, and class. This is the base class for questions and records.`
- L31: `public abstract class DNSEntry {`
- L126: `* Check if the requested record class match the current record class`
- L257: `* @return <code>true</code> is the two class are the same, <code>false</code> otherwise.`
- L304: `* @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSIncoming.java

状态：added

- L31: `public final class DNSIncoming extends DNSMessage {`
- L38: `public static class MessageInputStream extends ByteArrayInputStream {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSListener.java

状态：added

- L25: `interface DNSListener {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSMessage.java

状态：added

- L29: `public abstract class DNSMessage {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSOutgoing.java

状态：added

- L30: `public final class DNSOutgoing extends DNSMessage {`
- L32: `public static class MessageOutputStream extends ByteArrayOutputStream {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSQuestion.java

状态：added

- L33: `public class DNSQuestion extends DNSEntry {`
- L61: `private static class DNS4Address extends DNSQuestion {`
- L86: `private static class DNS6Address extends DNSQuestion {`
- L111: `private static class HostInformation extends DNSQuestion {`
- L120: `private static class Pointer extends DNSQuestion {`
- L160: `private static class Service extends DNSQuestion {`
- L190: `private static class Text extends DNSQuestion {`
- L211: `private static class AllRecords extends DNSQuestion {`
- L258: `*            Record class to resolve`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSRecord.java

状态：added

- L46: `public abstract class DNSRecord extends DNSEntry {`
- L219: `public static class IPv4Address extends Address {`
- L260: `public static class IPv6Address extends Address {`
- L313: `public static abstract class Address extends DNSRecord {`
- L452: `// info.setAddress(_addr); This is done in the subclass so we don't have to test for class type`
- L484: `public static class Pointer extends DNSRecord {`
- L594: `public static class Text extends DNSRecord {`
- L713: `public static class Service extends DNSRecord {`
- L913: `public static class HostInformation extends DNSRecord {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSStatefulObject.java

状态：added

- L32: `* <b>Implementation note:</b> This interface is accessed from multiple threads. The implementation must be thread safe.`
- L36: `public interface DNSStatefulObject {`
- L39: `* This class define a semaphore. On this multiple threads can wait the arrival of one event. Thread wait for a maximum defined by the timeout.`
- L41: `* Implementation note: this class is based on {@link java.util.concurrent.Semaphore} so that they can be released by the timeout timer.`
- L46: `final class DNSStatefulObjectSemaphore {`
- L118: `class DefaultImplementation extends ReentrantLock implements DNSStatefulObject {`
- L469: `* Checks if this object is associated with the task and in the same state.`
- L568: `* Waits for the object to be announced.`
- L572: `* @return <code>true</code> if the object is announced, <code>false</code> otherwise`
- L577: `* Waits for the object to be canceled.`
- L581: `* @return <code>true</code> if the object is canceled, <code>false</code> otherwise`

## vendor/jmdns/src/main/java/javax/jmdns/impl/DNSTaskStarter.java

状态：added

- L35: `* This class is used by JmDNS to start the various task required to run the DNS discovery. This interface is only there in order to support MANET modifications.`
- L42: `public interface DNSTaskStarter {`
- L47: `final class Factory {`
- L53: `* This interface defines a delegate to the DNSTaskStarter class to enable subclassing.`
- L55: `public interface ClassDelegate {`
- L77: `* Assigns <code>delegate</code> as DNSTaskStarter's class delegate. The class delegate is optional.`
- L80: `*            The object to set as DNSTaskStarter's class delegate.`
- L89: `* Returns DNSTaskStarter's class delegate.`
- L91: `* @return DNSTaskStarter's class delegate.`
- L100: `* Returns a new instance of DNSTaskStarter using the class delegate if it exists.`
- L159: `final class DNSTaskStarterImpl implements DNSTaskStarter {`
- L173: `public static class StarterTimer extends Timer {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/HostInfo.java

状态：added

- L41: `public class HostInfo implements DNSStatefulObject {`
- L52: `private final static class HostInfoState extends DNSStatefulObject.DefaultImplementation {`
- L107: `logger.warn("Could not initialize the host network interface on {}because of an error: {}", addr, e.getMessage(), e);`
- L210: `// Ignore loopback packets on a regular interface unless this is also a loopback interface.`

## vendor/jmdns/src/main/java/javax/jmdns/impl/JmDNSImpl.java

状态：added

- L70: `public class JmDNSImpl extends JmDNS implements DNSStatefulObject, DNSTaskStarter {`
- L131: `public static class ServiceTypeEntry extends AbstractMap<String, String>implements Cloneable {`
- L137: `private static class SubTypeEntry implements Entry<String, String>, java.io.Serializable, Cloneable {`
- L167: `* Replaces the value corresponding to this entry with the specified value (optional operation). This implementation simply throws <tt>UnsupportedOperationException</tt>, as this class implements an <i>immutable</i> map entry.`
- L272: `* Returns an iterator over the elements in this set. The elements are returned in no particular order (unless this set is an instance of some class that provides a guarantee).`
- L400: `* Create an instance of JmDNS and bind it to a specific network interface given its IP-address.`
- L412: `* Create an instance of JmDNS and bind it to a specific network interface given its IP-address.`
- L494: `// selection, interface binding, joinGroup, TTL and closeMulticastSocket — is unchanged.`
- L1557: `* _ibisip_http._tcp.local: type PTR, class IN, DeviceManagementService._ibisip_http._tcp.local`
- L1558: `* _ibisip_http._tcp.local: type PTR, class IN, PassengerCountingService._ibisip_http._tcp.local`
- L1560: `* DeviceManagementService._ibisip_http._tcp.local: type TXT, class IN, cache flush`
- L1561: `* PassengerCountingService._ibisip_http._tcp.local: type TXT, class IN, cache flush`
- L1562: `* example.local: type A, class IN, cache flush, addr 192.0.2.1`
- L1563: `* DeviceManagementService._ibisip_http._tcp.local: type SRV, class IN, cache flush, priority 0, weight 0, port 5000, target example.local`
- L1564: `* PassengerCountingService._ibisip_http._tcp.local: type SRV, class IN, cache flush, priority 0, weight 0, port 5001, target example.local`
- L1565: `* DeviceManagementService._ibisip_http._tcp.local: type NSEC, class IN, cache flush, next domain name DeviceManagementService._ibisip_http._tcp.local`
- L1566: `* PassengerCountingService._ibisip_http._tcp.local: type NSEC, class IN, cache flush, next domain name PassengerCountingService._ibisip_http._tcp.local`
- L1567: `* example.local: type NSEC, class IN, cache flush, next domain name example.local`
- L2159: `private static class ServiceCollector implements ServiceListener {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/JmmDNSImpl.java

状态：added

- L56: `* This class enable multihoming mDNS. It will open a mDNS per IP address of the machine.`
- L60: `public class JmmDNSImpl implements JmmDNS, NetworkTopologyListener, ServiceInfoImpl.Delegate {`
- L718: `* If the network change, this class will reconfigure the list of DNS do adapt to the new configuration.`
- L720: `static class NetworkChecker extends TimerTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/ListenerStatus.java

状态：added

- L30: `* This class track the status of listener.<br/>`
- L31: `* The main purpose of this class is to collapse consecutive events so that we can guarantee the correct call back sequence.`
- L37: `public class ListenerStatus<T extends EventListener> {`
- L39: `public static class ServiceListenerStatus extends ListenerStatus<ServiceListener> {`
- L167: `public static class ServiceTypeListenerStatus extends ListenerStatus<ServiceTypeListener> {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/NameRegister.java

状态：added

- L21: `public interface NameRegister {`
- L37: `abstract class BaseRegister implements NameRegister {`
- L89: `class UniqueNamePerInterface extends BaseRegister {`
- L153: `class UniqueNameAcrossInterface extends BaseRegister {`
- L206: `class Factory {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/NetworkTopologyDiscoveryImpl.java

状态：added

- L29: `* This class implements NetworkTopologyDiscovery.`
- L33: `public class NetworkTopologyDiscoveryImpl implements NetworkTopologyDiscovery {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/NetworkTopologyEventImpl.java

状态：added

- L27: `public class NetworkTopologyEventImpl extends NetworkTopologyEvent implements Cloneable {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/package-info.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/ServiceEventImpl.java

状态：added

- L25: `public class ServiceEventImpl extends ServiceEvent {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/ServiceInfoImpl.java

状态：added

- L54: `public class ServiceInfoImpl extends ServiceInfo implements DNSListener, DNSStatefulObject {`
- L80: `public interface Delegate {`
- L86: `private final static class ServiceInfoState extends DNSStatefulObject.DefaultImplementation {`
- L791: `// There is a timing/ concurrency issue here.  The ServiceInfo object is subject to concurrent change.`
- L1200: `*            record class of the query`
- L1216: `*            record class of the query`
- L1225: `// [PJYF Dec 6 2011] This is bad hack as I don't know what the spec should really mean in this case. i.e. what is the class of our registered services.`

## vendor/jmdns/src/main/java/javax/jmdns/impl/ServiceTypeDecoder.java

状态：added

- L22: `class ServiceTypeDecoder {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/SocketListener.java

状态：added

- L27: `class SocketListener extends Thread {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/DNSTask.java

状态：added

- L28: `* This is the root class for all task scheduled by the timer in JmDNS.`
- L32: `public abstract class DNSTask extends TimerTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/package-info.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/RecordReaper.java

状态：added

- L26: `public class RecordReaper extends DNSTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/resolver/DNSResolverTask.java

状态：added

- L28: `* This is the root class for all resolver tasks.`
- L32: `public abstract class DNSResolverTask extends DNSTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/resolver/package-info.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/resolver/ServiceInfoResolver.java

状态：added

- L33: `public class ServiceInfoResolver extends DNSResolverTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/resolver/ServiceResolver.java

状态：added

- L33: `public class ServiceResolver extends DNSResolverTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/resolver/TypeResolver.java

状态：added

- L28: `* Helper class to resolve service types.`
- L36: `public class TypeResolver extends DNSResolverTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/Responder.java

状态：added

- L35: `public class Responder extends DNSTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/state/Announcer.java

状态：added

- L33: `public class Announcer extends DNSStateTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/state/Canceler.java

状态：added

- L30: `public class Canceler extends DNSStateTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/state/DNSStateTask.java

状态：added

- L33: `* This is the root class for all state tasks. These tasks work with objects that implements the`
- L34: `* {@link javax.jmdns.impl.DNSStatefulObject} interface and therefore participate in the state machine.`
- L38: `public abstract class DNSStateTask extends DNSTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/state/package-info.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/state/Prober.java

状态：added

- L36: `public class Prober extends DNSStateTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/tasks/state/Renewer.java

状态：added

- L30: `public class Renewer extends DNSStateTask {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/util/ByteWrangler.java

状态：added

- L26: `* This class contains all the byte shifting`
- L31: `public class ByteWrangler {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/util/NamedThreadFactory.java

状态：added

- L24: `public class NamedThreadFactory implements ThreadFactory {`

## vendor/jmdns/src/main/java/javax/jmdns/impl/util/SimpleLockManager.java

状态：added

- L24: `public class SimpleLockManager {`
- L29: `* Acquires a {@link Locked} object for a resource with a given key.`
- L50: `* Attempts to acquire a {@link Locked} object for a resource with a given key within specified time.`
- L76: `private class LockedImpl extends Locked {`
- L97: `public abstract static class Locked implements Closeable {`
- L105: `public static class LockFailedException extends Exception {`

## vendor/jmdns/src/main/java/javax/jmdns/JmDNS.java

状态：added

- L31: `public abstract class JmDNS implements Closeable {`
- L36: `public interface Delegate {`
- L89: `* Create an instance of JmDNS and bind it to a specific network interface given its IP-address.`
- L131: `* Create an instance of JmDNS and bind it to a specific network interface given its IP-address.`
- L137: `* <li>Use the {@link NetworkTopologyDiscovery} to find a valid network interface and IP.</li>`
- L188: `* Return the address of the interface to which this instance of JmDNS is bound.`
- L197: `* Return the address of the interface to which this instance of JmDNS is bound.`
- L210: `* Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.`
- L223: `* Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.`
- L238: `* Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.`
- L253: `* Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.`
- L270: `* Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.`
- L282: `* Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.`

## vendor/jmdns/src/main/java/javax/jmdns/JmmDNS.java

状态：added

- L29: `* This class will monitor network topology changes, and will create or destroy JmDNS instances as required. It is your responsibility to maintain services registration (hint: use a {@link NetworkTopologyListener}).<br/>`
- L30: `* Most of this class methods have no notion of transaction: if an Exception is raised in the middle of execution, you may be in an incoherent state.`
- L37: `public interface JmmDNS extends Closeable {`
- L42: `final class Factory {`
- L46: `* This interface defines a delegate to the EOClassDescriptionRegister class to enable subclassing.`
- L48: `public interface ClassDelegate {`
- L68: `* Assigns <code>delegate</code> as JmmDNS's class delegate. The class delegate is optional.`
- L71: `*            The object to set as JmmDNS's class delegate.`
- L80: `* Returns JmmDNS's class delegate.`
- L82: `* @return JmmDNS's class delegate.`
- L91: `* Returns a new instance of JmmDNS using the class delegate if it exists.`
- L151: `* Return the list of addresses of the interface to which this instance of JmmDNS is bound.`
- L160: `* Return the list of addresses of the interface to which this instance of JmmDNS is bound.`
- L180: `* Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.`
- L194: `* Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.`
- L210: `* Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.`
- L226: `* Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.`

## vendor/jmdns/src/main/java/javax/jmdns/NetworkTopologyDiscovery.java

状态：added

- L23: `* This class is used to resolve the list of Internet address to use when attaching JmDNS to the network.`
- L25: `* To create you own filtering class for Internet Addresses you will need to implement the class and the factory delegate. These must be called before any other call to JmDNS.`
- L28: `* public static class MyNetworkTopologyDiscovery implements NetworkTopologyDiscovery {`
- L44: `* public static class MyClass implements NetworkTopologyDiscovery.Factory.ClassDelegate {`
- L62: `public interface NetworkTopologyDiscovery {`
- L67: `final class Factory {`
- L71: `* This interface defines a delegate to the NetworkTopologyDiscovery.Factory class to enable subclassing.`
- L73: `public interface ClassDelegate {`
- L92: `* Assigns <code>delegate</code> as NetworkTopologyDiscovery's class delegate. The class delegate is optional.`
- L95: `*            The object to set as NetworkTopologyDiscovery's class delegate.`
- L104: `* Returns NetworkTopologyDiscovery's class delegate.`
- L106: `* @return NetworkTopologyDiscovery's class delegate.`
- L115: `* Returns a new instance of NetworkTopologyDiscovery using the class delegate if it exists.`
- L156: `* @param interfaceAddress the interface IP address`
- L164: `* @param interfaceAddress the interface IP address`
- L171: `* @param interfaceAddress the interface IP address`

## vendor/jmdns/src/main/java/javax/jmdns/NetworkTopologyEvent.java

状态：added

- L24: `public abstract class NetworkTopologyEvent extends EventObject {`

## vendor/jmdns/src/main/java/javax/jmdns/NetworkTopologyListener.java

状态：added

- L23: `public interface NetworkTopologyListener extends EventListener {`

## vendor/jmdns/src/main/java/javax/jmdns/package-info.java

状态：added


## vendor/jmdns/src/main/java/javax/jmdns/ServiceEvent.java

状态：added

- L18: `public abstract class ServiceEvent extends EventObject implements Cloneable {`
- L29: `*            The object on which the Event initially occurred.`

## vendor/jmdns/src/main/java/javax/jmdns/ServiceInfo.java

状态：added

- L42: `public abstract class ServiceInfo implements Cloneable {`

## vendor/jmdns/src/main/java/javax/jmdns/ServiceListener.java

状态：added

- L23: `public interface ServiceListener extends EventListener {`

## vendor/jmdns/src/main/java/javax/jmdns/ServiceTypeListener.java

状态：added

- L23: `public interface ServiceTypeListener extends EventListener {`
