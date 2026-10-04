import subprocess, time, pathlib, xml.etree.ElementTree as ET, json, re, sys

ADB = sys.argv[1] if len(sys.argv) > 1 else r'E:\android-sdk\platform-tools\adb.exe'
OUT = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else pathlib.Path(__file__).parent / 'public-apk-ui'
OUT.mkdir(parents=True, exist_ok=True)
PACKAGE = 'com.shihab.diplay.hudtest'
HOME = PACKAGE + '/com.shilapi.xcertplay.DiPlayActivity'

def run(*args):
    value = subprocess.check_output([ADB, '-s', 'emulator-5554', *args]).decode('utf-8', 'replace')
    if 'Error:' in value or 'Exception' in value:
        raise RuntimeError(value)
    return value

def dump(name):
    run('shell', 'uiautomator', 'dump', '/data/local/tmp/public-ui.xml')
    value = run('shell', 'cat', '/data/local/tmp/public-ui.xml')
    (OUT / (name + '.xml')).write_text(value, encoding='utf-8')
    tree = ET.fromstring(value)
    assert any(n.attrib.get('package') == PACKAGE for n in tree.iter('node'))
    return tree

run('shell', 'am', 'force-stop', PACKAGE)
run('logcat', '-c')
run('shell', 'am', 'start', '-n', HOME)
time.sleep(2)
found = None
for step in range(5):
    tree = dump('home-' + str(step))
    found = next((n for n in tree.iter('node') if n.attrib.get('text') == '设置'), None)
    if found is not None:
        break
    run('shell', 'input', 'touchscreen', 'swipe', '20', '700', '20', '200', '600')
    time.sleep(1)
assert found is not None, 'Settings button not found'
b = [int(x) for x in re.findall(r'\d+', found.attrib['bounds'])]
run('shell', 'input', 'tap', str((b[0] + b[2]) // 2), str((b[1] + b[3]) // 2))
time.sleep(2)
activity = run('shell', 'dumpsys', 'activity', 'activities')
assert 'Settings' in activity, 'Settings activity not opened'
for step in range(9):
    dump('settings-' + str(step))
    run('shell', 'input', 'touchscreen', 'swipe', '20', '700', '20', '200', '600')
    time.sleep(.4)
log = run('logcat', '-d', '-v', 'threadtime')
(OUT / 'logcat.txt').write_text(log, encoding='utf-8')
assert 'FATAL EXCEPTION' not in log
result = {'install': True, 'launch': True, 'settingsOpened': True, 'settingsScrollSteps': 9, 'noFatalException': True}
(OUT / 'public-apk-ui-results.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
print('PUBLIC APK launch/settings/9 scrolls PASS', flush=True)
run('shell', 'am', 'force-stop', PACKAGE)
run('shell', 'am', 'start', '-n', HOME)
