import json,re,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
ADB=r"E:\android-sdk\platform-tools\adb.exe"
PKG="com.shihab.diplay.hudtest"
OUT=Path(__file__).parent/"p2p-legacy-ui";OUT.mkdir(exist_ok=True)
def adb(*a):return subprocess.check_output([ADB,"-s","emulator-5554",*a]).decode("utf-8","replace")
def dump(name):
 adb("shell","uiautomator","dump","/data/local/tmp/p2p-ui.xml")
 s=adb("shell","cat","/data/local/tmp/p2p-ui.xml")
 (OUT/(name+".xml")).write_text(s,encoding="utf-8")
 return ET.fromstring(s)
def find(t,text):return next((n for n in t.iter("node") if text in n.get("text","")),None)
def open_settings():
 adb("shell","am","force-stop",PKG)
 adb("shell","am","start","-n",PKG+"/com.shilapi.xcertplay.DiPlayActivity","--es","page","connection")
 time.sleep(2);adb("shell","input","keyevent","82")
def reveal(text):
 for i in range(8):
  t=dump("scroll-"+str(i));n=find(t,text)
  if n is not None:return t,n
  adb("shell","input","swipe","1700","570","1700","240","350");time.sleep(.4)
 raise AssertionError("Option not found: "+text)
adb("logcat","-c")
open_settings()
t,n=reveal("Wi-Fi 直连")
assert find(t,"车机热点") is not None
assert find(t,"Android 4.3 Wi-Fi") is not None
b=list(map(int,re.findall(r"\d+",n.get("bounds"))))
adb("shell","input","tap",str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(1)
remote="/data/data/"+PKG+"/shared_prefs/xcertplay_airplay.xml"
prefs=ET.fromstring(adb("shell","cat",remote))
assert next(x.text for x in prefs if x.get("name")=="wireless_hotspot_mode")=="WIFI_P2P"
open_settings();t,n=reveal("Wi-Fi 直连")
assert n.get("text","").startswith("✓"),"Legacy mode was reset after Activity recreation"
adb("shell","screencap","-p","/data/local/tmp/p2p-ui.png")
adb("pull","/data/local/tmp/p2p-ui.png",str(OUT/"modes.png"))
adb("shell","am","force-stop",PKG)
adb("shell","svc","wifi","enable");time.sleep(2)
adb("shell","am","start","-n",PKG+"/com.shilapi.xcertplay.compat.Api18CompatProbeActivity","--es","probe","wifi_p2p")
time.sleep(16)
log=adb("logcat","-d","-s","CodexApi18Probe:I","*:S")
(OUT/"api18-p2p-probe.log").write_text(log,encoding="utf-8")
assert "COMPLETE failures=0" in log,log
alllog=adb("logcat","-d","-v","threadtime")
(OUT/"logcat.txt").write_text(alllog,encoding="utf-8")
assert "FATAL EXCEPTION" not in alllog
(OUT/"result.json").write_text(json.dumps({"twoModesVisible":True,"legacyLimitationsVisible":True,"modeSelectionPersistsAfterRestart":True,"api18ProbeNoFailures":True,"noFatalException":True,"realH52OrIPhoneTested":False},indent=2),encoding="utf-8")
print("API18 Wi-Fi Direct UI and compatibility probe PASS")
