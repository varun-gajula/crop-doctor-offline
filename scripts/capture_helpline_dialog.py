import os, subprocess, time, re

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")

def adb(cmd):
    return subprocess.run(f'"{ADB}" {cmd}', shell=True, capture_output=True, text=True).stdout.strip()

# 1. Test Andhra Pradesh Helplines
adb("shell am start -n com.cropdoctor/.MainActivity --es benchmark_action lang_en")
time.sleep(0.5)
adb("shell am start -n com.cropdoctor/.MainActivity --es benchmark_action region_andhra")
time.sleep(0.5)
adb("shell am start -n com.cropdoctor/.MainActivity --es benchmark_action show_severe_chilli")
time.sleep(1.5)

# Scroll to bottom to reveal red referral button
adb("shell input swipe 540 1800 540 400 300")
time.sleep(0.5)
adb("shell input swipe 540 1800 540 400 300")
time.sleep(0.5)

dump = adb("exec-out uiautomator dump /dev/tty")
# Find red call button: bounds [x1,y1][x2,y2]
m = re.search(r'text="📞 Call Kisan Call Centre"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', dump)
if m:
    x = (int(m.group(1)) + int(m.group(3))) // 2
    y = (int(m.group(2)) + int(m.group(4))) // 2
    print(f"Tapping referral button at ({x}, {y})")
    adb(f"shell input tap {x} {y}")
else:
    print("Could not find button by regex, tapping estimated coordinates (540, 1800)")
    adb("shell input tap 540 1800")

time.sleep(1.0)
raw = adb("exec-out uiautomator dump /dev/tty")
texts = re.findall(r'text="([^"]+)"', raw)
print("Helpline Dialog texts on screen:")
for t in texts:
    print(" -", t)

os.makedirs("benchmarks", exist_ok=True)
adb("shell screencap -p /sdcard/temp_screen.png")
adb("pull /sdcard/temp_screen.png benchmarks/g2_helplines_andhra.png")
print("Saved benchmarks/g2_helplines_andhra.png")

# Close dialog
adb("shell input keyevent 4")
