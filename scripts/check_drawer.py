import subprocess
import time
import re

ADB = r"C:\Users\<path>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
device = "sseqjnucrkeeprtw"

subprocess.run([ADB, "-s", device, "shell", "input", "keyevent", "3"])
time.sleep(1)
subprocess.run([ADB, "-s", device, "shell", "input", "swipe", "500", "1800", "500", "400", "300"])
time.sleep(1)

subprocess.run([ADB, "-s", device, "shell", "uiautomator", "dump", "/sdcard/drawer_dump.xml"])
subprocess.run([ADB, "-s", device, "pull", "/sdcard/drawer_dump.xml", "benchmarks/drawer_dump.xml"])

with open("benchmarks/drawer_dump.xml", "r", encoding="utf-8", errors="replace") as f:
    xml = f.read()

found = re.findall(r'text="([^"]*)"', xml) + re.findall(r'content-desc="([^"]*)"', xml)
matches = [t for t in set(found) if any(k in t.lower() for k in ["agri", "crop", "care", "20", "doctor"])]
print("App Drawer matches:", sorted(matches))
