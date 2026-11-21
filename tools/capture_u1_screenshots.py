import os
import subprocess
import time
from PIL import Image
import numpy as np

ADB = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"
OUT_DIR = os.path.abspath("benchmarks/u1_screenshots")

os.makedirs(OUT_DIR, exist_ok=True)

def adb_cmd(args):
    cmd = [ADB] + args
    res = subprocess.run(cmd, capture_output=True, text=True)
    return res.stdout.strip()

def capture(name):
    path = os.path.join(OUT_DIR, f"{name}.png")
    remote_path = f"/sdcard/{name}.png"
    adb_cmd(["shell", "screencap", "-p", remote_path])
    adb_cmd(["pull", remote_path, path])
    adb_cmd(["shell", "rm", remote_path])
    
    # Verify image
    im = Image.open(path)
    arr = np.array(im)
    mean_val = arr.mean(axis=(0, 1))
    print(f"Captured: {name}.png ({os.path.getsize(path):,} bytes, mean={mean_val[:3].round(1)})")

def start_action(action):
    # Bring activity to foreground and pass benchmark_action
    adb_cmd([
        "shell", "am", "start", "-W",
        "-n", "com.cropdoctor/.MainActivity",
        "-e", "benchmark_action", action
    ])
    time.sleep(1.8)

def main():
    print("Preparing emulator environment...")
    adb_cmd(["shell", "input", "keyevent", "KEYCODE_WAKEUP"])
    adb_cmd(["shell", "wm", "dismiss-keyguard"])
    adb_cmd(["shell", "svc", "power", "stayon", "true"])
    adb_cmd(["shell", "settings", "put", "system", "screen_off_timeout", "2147483647"])
    adb_cmd(["shell", "settings", "put", "system", "screen_brightness", "255"])
    adb_cmd(["shell", "pm", "grant", "com.cropdoctor", "android.permission.CAMERA"])
    adb_cmd(["shell", "settings", "put", "system", "font_scale", "1.0"])
    time.sleep(1.0)

    screens = [
        # 1. Home Screen in 3 languages
        ("home_en", "show_home_en"),
        ("home_te", "show_home_te"),
        ("home_hi", "show_home_hi"),

        # 2. Crop Selection in 3 languages
        ("crop_selection_en", "show_crop_selection_en"),
        ("crop_selection_te", "show_crop_selection_te"),
        ("crop_selection_hi", "show_crop_selection_hi"),

        # 3. Camera Screen in 3 languages
        ("camera_en", "show_camera_en"),
        ("camera_te", "show_camera_te"),
        ("camera_hi", "show_camera_hi"),

        # 4. Result Outcomes in 3 languages
        ("result_diagnose_en", "show_outcome_diagnose_en"),
        ("result_diagnose_te", "show_outcome_diagnose_te"),
        ("result_diagnose_hi", "show_outcome_diagnose_hi"),

        ("result_cant_tell_en", "show_outcome_cant_tell_en"),
        ("result_cant_tell_te", "show_outcome_cant_tell_te"),
        ("result_cant_tell_hi", "show_outcome_cant_tell_hi"),

        ("result_out_of_class_en", "show_outcome_out_of_class_en"),
        ("result_out_of_class_te", "show_outcome_out_of_class_te"),
        ("result_out_of_class_hi", "show_outcome_out_of_class_hi"),
    ]

    for name, action in screens:
        print(f"--> Displaying {action}...")
        start_action(action)
        capture(name)

    # 5. 130% Font Scaling verification for Telugu and Hindi (Task U1 Item 5)
    print("\n--- Testing 130% Font Scaling ---")
    adb_cmd(["shell", "settings", "put", "system", "font_scale", "1.3"])
    time.sleep(1.5)

    font_tests = [
        ("home_te_font130", "show_home_te"),
        ("home_hi_font130", "show_home_hi"),
        ("result_diagnose_te_font130", "show_outcome_diagnose_te"),
        ("result_diagnose_hi_font130", "show_outcome_diagnose_hi"),
    ]

    for name, action in font_tests:
        print(f"--> Displaying {action} at 130% font scale...")
        start_action(action)
        capture(name)

    # Reset font scale back to 1.0
    adb_cmd(["shell", "settings", "put", "system", "font_scale", "1.0"])
    print("\nAll screenshots captured successfully in benchmarks/u1_screenshots/")

if __name__ == "__main__":
    main()
