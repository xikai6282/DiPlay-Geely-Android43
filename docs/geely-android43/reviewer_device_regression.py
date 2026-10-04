import json
import pathlib
import subprocess
import time

ROOT = pathlib.Path(__file__).parent
ADB = r'E:\android-sdk\platform-tools\adb.exe'
PACKAGE = 'com.shihab.diplay.hudtest'
ACTIVITY = PACKAGE + '/com.shilapi.xcertplay.compat.Api18CompatProbeActivity'

def adb(*args):
    return subprocess.run([ADB, '-s', 'emulator-5554', *args], check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout.decode('utf-8', 'replace')

results = {}
for mode in ['full', 'usb_transfer', 'optional', 'map_embed', 'overlay', 'media_api18']:
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('logcat', '-c')
    adb('shell', 'am', 'start', '-n', ACTIVITY, '--es', 'probe', mode)
    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        tagged = adb('logcat', '-d', '-s', 'CodexApi18Probe:I')
        if 'COMPLETE failures=' in tagged:
            break
        time.sleep(0.5)
    log = adb('logcat', '-d', '-v', 'threadtime')
    (ROOT / f'reviewer-api18-{mode}.log').write_text(log, encoding='utf-8')
    passed = 'COMPLETE failures=0' in tagged and 'FAIL ' not in tagged and 'FATAL EXCEPTION' not in log
    results[mode] = passed
    print(mode, 'PASS' if passed else 'FAIL', flush=True)
    for line in tagged.splitlines():
        if 'PASS ' in line or 'FAIL ' in line or 'COMPLETE' in line:
            print(line, flush=True)
    if not passed:
        raise RuntimeError(f'{mode} failed; inspect saved log')

# Keep the same process between start/pause/resume/stop to exercise the actual RCC lifecycle.
for mode, state in [('keys_start', 'PLAYSTATE_PLAYING'), ('keys_pause', 'PLAYSTATE_PAUSED'),
                    ('keys_start', 'PLAYSTATE_PLAYING'), ('keys_stop', None)]:
    adb('shell', 'input', 'keyevent', '4')
    adb('shell', 'am', 'start', '-n', ACTIVITY, '--es', 'probe', mode)
    time.sleep(1)
    dump = adb('shell', 'dumpsys', 'audio')
    label = mode if mode != 'keys_start' or not (ROOT / 'reviewer-api18-keys_start-audio.txt').exists() else 'keys_resume'
    (ROOT / f'reviewer-api18-{label}-audio.txt').write_text(dump, encoding='utf-8')
    entries = [line for line in dump.splitlines() if PACKAGE in line and 'state:' in line]
    if state:
        assert any(state in line for line in entries), (mode, entries)
        adb('shell', 'input', 'keyevent', '85')
        if mode == 'keys_start':
            adb('shell', 'input', 'keyevent', '87')
    else:
        assert PACKAGE not in dump, 'Package still registered for media focus/RCC after stop'
log = adb('logcat', '-d', '-v', 'threadtime')
(ROOT / 'reviewer-api18-keys.log').write_text(log, encoding='utf-8')
assert 'legacyKeyEvent -> CarPlay 3' in log and 'legacyKeyEvent -> CarPlay 4' in log
assert 'FATAL EXCEPTION' not in log
results['media_keys'] = True
print('media_keys PASS PLAYING -> PAUSED -> PLAYING -> released; physical adb key events routed', flush=True)

for mode, foreground in [('session_start', True), ('session_stop', False)]:
    adb('shell', 'input', 'keyevent', '4')
    adb('shell', 'am', 'start', '-n', ACTIVITY, '--es', 'probe', mode)
    time.sleep(0.8)
    dump = adb('shell', 'dumpsys', 'activity', 'services', PACKAGE)
    (ROOT / f'reviewer-api18-{mode}-services.txt').write_text(dump, encoding='utf-8')
    assert ('isForeground=true' in dump) == foreground
    if not foreground:
        assert 'DiPlaySessionService' not in dump
results['foreground_service'] = True
print('foreground_service PASS actual foreground registration and stop cleanup', flush=True)
(ROOT / 'reviewer-api18-device-results.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
