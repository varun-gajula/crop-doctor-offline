import os
import subprocess
import time
from PIL import Image
import numpy as np

ADB = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"
OUT_DIR = os.path.abspath("benchmarks/f1_screenshots")

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
    adb_cmd([
        "shell", "am", "start", "-W",
        "-n", "com.cropdoctor/.MainActivity",
        "-e", "benchmark_action", action
    ])
    time.sleep(2.0)

def main():
    print("Preparing emulator environment for Task F1 screenshots...")
    adb_cmd(["shell", "input", "keyevent", "KEYCODE_WAKEUP"])
    adb_cmd(["shell", "wm", "dismiss-keyguard"])
    adb_cmd(["shell", "svc", "power", "stayon", "true"])
    adb_cmd(["shell", "settings", "put", "system", "screen_off_timeout", "2147483647"])
    adb_cmd(["shell", "settings", "put", "system", "screen_brightness", "255"])
    adb_cmd(["shell", "settings", "put", "system", "font_scale", "1.0"])
    time.sleep(1.0)

    screens = [
        # 1. Crop Guide - Crop Selection (same 5 tiles) in 3 languages
        ("crop_guide_select_en", "show_crop_guide_select_en"),
        ("crop_guide_select_te", "show_crop_guide_select_te"),
        ("crop_guide_select_hi", "show_crop_guide_select_hi"),

        # 2. Crop Guide - Problems of <crop> grouped by growth stage in 3 languages
        ("crop_guide_problems_en", "show_crop_guide_problems_en"),
        ("crop_guide_problems_te", "show_crop_guide_problems_te"),
        ("crop_guide_problems_hi", "show_crop_guide_problems_hi"),

        # 3. Crop Guide - Problem Details with advice tiers (prevention, natural, chemical) in 3 languages
        ("crop_guide_details_en", "show_crop_guide_details_en"),
        ("crop_guide_details_te", "show_crop_guide_details_te"),
        ("crop_guide_details_hi", "show_crop_guide_details_hi"),
    ]

    for name, action in screens:
        print(f"--> Capturing {action}...")
        start_action(action)
        capture(name)

    print("\nAll Task F1 screenshots captured successfully in benchmarks/f1_screenshots/")

if __name__ == "__main__":
    main()
