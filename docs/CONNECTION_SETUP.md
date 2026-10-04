# Built-in car hotspot setup

DiAuto & DiPlay — Built-in car hotspot test builds
28 September 2026

DiAuto-connection-setup-test.apk: Android phone / Android Auto
DiPlay-connection-setup-test.apk: iPhone / Apple CarPlay
Install the APK on the car, not on your phone. Update the matching existing
test app without uninstalling to preserve settings.

These builds remove the Local hotspot option. Built-in car hotspot is the
default; Wi-Fi Direct and USB remain available. Previous Local hotspot
selections switch to built-in hotspot. Check and save the car's real hotspot
details before connecting.

BUILT-IN HOTSPOT SETUP — BOTH APPS
1. In the car's settings, turn on its built-in Wi-Fi hotspot. Select 5 GHz
   if available. Note the hotspot name and password exactly.
2. Open DiAuto or DiPlay on the car. Go to Settings → Connection setup
   (tap Open connection setup if shown).
3. Select Built-in car hotspot. Tap Save hotspot details and use this mode
   (or Edit saved hotspot), enter the car's hotspot name and password, and
   save. Use Hide keyboard if needed. Leave the car hotspot on.
4. Turn on Bluetooth and Wi-Fi on your phone. Pair it with the car's
   Bluetooth. Allow the app permissions requested on the car.
5. Return to the app and tap Connect phone. Select your phone when asked.
   In DiPlay, use Choose iPhone if you need to select a different phone.
6. Accept the Android Auto or CarPlay prompts on your phone.

You do not need to join the hotspot manually on your phone before tapping
Connect phone. The app sends its details over Bluetooth so the phone can
join automatically. Use the car's hotspot, not your phone's Personal Hotspot.
ADB is not required for this connection setup. A car internet plan is not
required; phone internet availability depends on its network settings.
If you change the car hotspot name or password, update it in the app too.
Test one projection app at a time.

OPTIONAL: DIPLAY AUTOMATIC INSTRUMENT MAP
On the supported DiLink 5.1 firmware, open Settings → BYD navigation →
Automatic map setup · ADB. Follow the displayed one-time computer setup,
then tap Check and enable. Open the cluster's map card or select Map theme.
This permission is for automatic cluster theme/card detection, not hotspot
connection. The guide explains the exact command for the installed app.

WHAT TO TEST / REPORT
Check first connection, reconnect after restarting the app/car, maps,
music/audio, and any instrument-map features supported by your car.
If something fails, note the time and steps, car model, DiLink/Android
version, phone model/OS, and which app you used. Export a diagnostic report
from Settings → Diagnostics → Save diagnostic report. Reports are saved
under Downloads/DiAuto or Downloads/DiPlay. Share the report with your test
feedback; do not include your hotspot password.
