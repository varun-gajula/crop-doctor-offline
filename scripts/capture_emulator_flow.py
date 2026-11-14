import os
import subprocess
import time
from PIL import Image

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
DEVICE = "emulator-5554"

def adb(cmd):
    full = [ADB, "-s", DEVICE] + cmd
    res = subprocess.run(full, capture_output=True, text=True)
    return res.stdout.strip()

def tap(x, y):
    adb(["shell", "input", "tap", str(x), str(y)])
    time.sleep(1.0)

def long_press(x, y, ms=1500):
    adb(["shell", "input", "swipe", str(x), str(y), str(x), str(y), str(ms)])
    time.sleep(1.0)

def scroll_down():
    adb(["shell", "input", "swipe", "540", "1800", "540", "600", "300"])
    time.sleep(1.0)

def capture(out_path):
    adb(["shell", "screencap", "-p", "/data/local/tmp/s.png"])
    adb(["pull", "/data/local/tmp/s.png", out_path])
    time.sleep(0.5)

print("Starting clean emulator capture flow...")
# 1. Launch / Bring Home to front
adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity"])
time.sleep(1.5)

# Ensure on Home
capture("benchmarks/screen_1_home.png")
print("1. Home captured")

# 2. Tap Scan Leaf (540, 1000)
tap(540, 1000)
capture("benchmarks/screen_2_camera.png")
print("2. Camera captured")

# 3. Tap Capture Shutter (550, 2127)
tap(550, 2127)
time.sleep(1.5)
capture("benchmarks/screen_3_result_en.png")
print("3. Result EN captured")

# 4. Long press title for Judge Panel (258, 158)
long_press(258, 158, 1500)
capture("benchmarks/screen_4_judge_panel.png")
print("4. Judge Panel captured")

# Close judge panel (775, 1670)
tap(775, 1670)

# 5. Switch to Telugu: Tap Language Chip (961, 150), tap Telugu (280, 900)
tap(961, 150)
tap(280, 900)
time.sleep(1.0)
capture("benchmarks/screen_5_result_telugu.png")
print("5. Result Telugu captured")

# 6. Switch to Hindi: Tap Language Chip (950, 150), tap Hindi (280, 1110)
tap(950, 150)
tap(280, 1110)
time.sleep(1.0)
capture("benchmarks/screen_6_result_hindi.png")
print("6. Result Hindi captured")

# 7. Scroll down to Case Report button
scroll_down()
# Tap "📝 केस रिपोर्ट सहेजें" at (540, 2087)
tap(540, 2087)
capture("benchmarks/screen_7_save_report.png")
print("7. Save Case Report screen captured")

# Tap Back (105, 158) twice to return to Home
tap(105, 158)
tap(105, 158)

# 8. Open More -> Saved Case Reports
tap(907, 2210) # More bottom nav
tap(400, 400)  # Saved Case Reports
capture("benchmarks/screen_8_saved_cases.png")
print("8. Saved Cases list captured")

# Return to Home
tap(105, 158)
tap(160, 2210)

print("All screens captured cleanly!")
