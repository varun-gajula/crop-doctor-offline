import subprocess
import time
import re

ADB = r"C:\Users\<path>\AppData\Local\Android\Sdk\platform-tools\adb.exe"
device = "sseqjnucrkeeprtw"

subprocess.run([ADB, "-s", device, "shell", "input", "keyevent", "3"])
time.sleep(1)

for page in range(5):
    subprocess.run([ADB, "-s", device, "shell", "uiautomator", "dump", "/sdcard/launcher_dump.xml"], capture_output=True)
    subprocess.run([ADB, "-s", device, "pull", "/sdcard/launcher_dump.xml", "benchmarks/launcher_dump.xml"], capture_output=True)
    with open("benchmarks/launcher_dump.xml", "r", encoding="utf-8", errors="replace") as f:
        xml = f.read()
    found = re.findall(r'text="([^"]*)"', xml) + re.findall(r'content-desc="([^"]*)"', xml)
    relevant = [t for t in set(found) if any(k in t.lower() for k in ["agri", "crop", "care", "old 20", "20-class", "doctor"])]
    print(f"Page {page}: {relevant}")
    subprocess.run([ADB, "-s", device, "shell", "input", "swipe", "950", "1200", "100", "1200", "300"], capture_output=True)
    time.sleep(1)
