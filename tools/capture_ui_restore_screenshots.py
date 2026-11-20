import subprocess
import time
import os
import shutil

adb = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"
device = "emulator-5554"
artifact_dir = r"C:\Users\ec026\.gemini\antigravity\brain\a51aa7bb-ac03-41b4-9356-fee5f244754c"
apk_path = r"app\build\outputs\apk\debug\app-debug.apk"

def run_adb(args):
    cmd = [adb, "-s", device] + args
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return res

os.makedirs("benchmarks", exist_ok=True)
os.makedirs(artifact_dir, exist_ok=True)

print("1. Installing debug APK on emulator...")
install_res = run_adb(["install", "-r", apk_path])
print("Install:", install_res.stdout.decode('utf-8', errors='ignore').strip())

print("2. Ensuring screen is unlocked...")
run_adb(["shell", "input", "keyevent", "KEYCODE_WAKEUP"])
run_adb(["shell", "wm", "dismiss-keyguard"])

print("3. Setting is_first_launch=false in shared_prefs...")
prefs_xml = '<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><boolean name="is_first_launch" value="false" /><string name="selected_language">en</string><string name="selected_region">Telangana</string></map>'
run_adb(["shell", "run-as", "com.cropdoctor", "mkdir", "-p", "/data/data/com.cropdoctor/shared_prefs"])
run_adb(["shell", f"run-as com.cropdoctor sh -c 'echo \"{prefs_xml}\" > /data/data/com.cropdoctor/shared_prefs/crop_doctor_prefs.xml'"])

# Start app once
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity"])
time.sleep(3.0)

# Scenarios to capture:
# (action, filename, language)
scenarios = [
    # English
    ("show_home", "ui_restore_home_en.png", "en"),
    ("show_camera", "ui_restore_camera_en.png", "en"),
    ("show_crop_selection_en", "ui_restore_crop_selection_en.png", "en"),
    ("show_outcome_diagnose", "ui_restore_result_diagnose_en.png", "en"),
    ("show_outcome_cant_tell", "ui_restore_result_cant_tell_en.png", "en"),
    ("show_outcome_out_of_class", "ui_restore_result_out_of_class_en.png", "en"),
    # Telugu
    ("show_home", "ui_restore_home_te.png", "te"),
    ("show_camera", "ui_restore_camera_te.png", "te"),
    ("show_crop_selection_te", "ui_restore_crop_selection_te.png", "te"),
    ("show_outcome_diagnose", "ui_restore_result_diagnose_te.png", "te"),
    ("show_outcome_cant_tell", "ui_restore_result_cant_tell_te.png", "te"),
    ("show_outcome_out_of_class", "ui_restore_result_out_of_class_te.png", "te"),
]

for action, fname, lang in scenarios:
    print(f"Triggering lang_{lang} then {action} -> {fname}...")
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", "--es", "benchmark_action", f"lang_{lang}"])
    time.sleep(1.0)
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", "--es", "benchmark_action", action])
    time.sleep(2.5)

    dev_path = f"/data/local/tmp/{fname}"
    run_adb(["shell", "screencap", "-p", dev_path])
    
    local_bm = os.path.join("benchmarks", fname)
    local_art = os.path.join(artifact_dir, fname)
    
    run_adb(["pull", dev_path, local_bm])
    if os.path.exists(local_bm):
        shutil.copy2(local_bm, local_art)
        size = os.path.getsize(local_bm)
        print(f"Captured: {fname} ({size} bytes)")
    else:
        print(f"Failed to capture: {fname}")

print("All UI-RESTORE screenshots successfully captured!")
