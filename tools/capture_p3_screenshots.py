import subprocess
import time
import os

adb = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"
device = "emulator-5554"

def run_adb(args):
    cmd = [adb, "-s", device] + args
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return res

os.makedirs("benchmarks", exist_ok=True)

# Ensure screen is unlocked
run_adb(["shell", "input", "keyevent", "KEYCODE_WAKEUP"])
run_adb(["shell", "wm", "dismiss-keyguard"])

# Create shared_prefs directory and xml setting is_first_launch=false
prefs_xml = '<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><boolean name="is_first_launch" value="false" /><string name="selected_language">en</string><string name="selected_region">Telangana</string></map>'
run_adb(["shell", f"run-as com.cropdoctor mkdir -p /data/data/com.cropdoctor/shared_prefs"])
run_adb(["shell", f"run-as com.cropdoctor sh -c 'echo \"{prefs_xml}\" > /data/data/com.cropdoctor/shared_prefs/crop_doctor_prefs.xml'"])

# Start MainActivity to initialize
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity"])
time.sleep(3.0)

langs = [
    ("en", "show_crop_selection_en", "benchmarks/p3_crop_selection_en.png"),
    ("te", "show_crop_selection_te", "benchmarks/p3_crop_selection_te.png"),
    ("hi", "show_crop_selection_hi", "benchmarks/p3_crop_selection_hi.png")
]

for lang, action, out_path in langs:
    print(f"Triggering {action} for {lang}...")
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", "--es", "benchmark_action", action])
    time.sleep(2.5)
    
    dev_path = f"/data/local/tmp/{lang}_crop_sel.png"
    run_adb(["shell", "screencap", "-p", dev_path])
    run_adb(["pull", dev_path, out_path])
    if os.path.exists(out_path):
        size = os.path.getsize(out_path)
        print(f"Saved {out_path} ({size} bytes)")
    else:
        print(f"Error: {out_path} not found!")

print("All screenshots captured successfully.")
