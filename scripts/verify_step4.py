import zipfile
import hashlib
import subprocess
import re
import os

ADB = r"C:\Users\<path>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
device = "sseqjnucrkeeprtw"

# 1. Check SHA-256 inside APK-15
apk15_path = r"app\build\outputs\apk\debug\app-debug.apk"
with zipfile.ZipFile(apk15_path, "r") as z:
    for n in z.namelist():
        if "vision.pte" in n:
            data = z.read(n)
            h = hashlib.sha256(data).hexdigest()
            print(f"APK-15 inside '{n}' SHA-256: {h}")

src_tr4 = r"C:\Users\<path>\Downloads\crop_doctor_final_15class_20ep (2)\vision.pte"
h_tr4 = hashlib.sha256(open(src_tr4, "rb").read()).hexdigest()
print(f"TR4 source file SHA-256:             {h_tr4}")
print(f"Match: {h == h_tr4}")

# 2. Install APK-15 over com.cropdoctor
print("\nInstalling APK-15 over com.cropdoctor...")
res = subprocess.run([ADB, "-s", device, "install", "-r", apk15_path], capture_output=True, text=True)
print(res.stdout.strip())

# 3. Install APK-20 over com.cropdoctor.old20
apk20_path = r"C:\crop-doctor-20class\app\build\outputs\apk\debug\app-debug.apk"
if os.path.exists(apk20_path):
    print("\nInstalling APK-20 over com.cropdoctor.old20...")
    res2 = subprocess.run([ADB, "-s", device, "install", "-r", apk20_path], capture_output=True, text=True)
    print(res2.stdout.strip())

# 4. Dump launcher UI
print("\nPressing Home and dumping launcher UI...")
subprocess.run([ADB, "-s", device, "shell", "input", "keyevent", "3"])
import time
time.sleep(2)
subprocess.run([ADB, "-s", device, "shell", "uiautomator", "dump", "/sdcard/launcher_dump.xml"])
subprocess.run([ADB, "-s", device, "pull", "/sdcard/launcher_dump.xml", "benchmarks/launcher_dump.xml"])

with open("benchmarks/launcher_dump.xml", "r", encoding="utf-8", errors="replace") as f:
    xml_content = f.read()

print("\n--- Launcher App Nodes Found ---")
matches = re.findall(r'text="([^"]*)"[^>]*package="[^"]*"', xml_content)
for text in sorted(set(matches)):
    if any(k in text.lower() for k in ["agri", "crop", "care", "old 20"]):
        print(f"  Found Launcher Item: '{text}'")
