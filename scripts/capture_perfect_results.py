import subprocess
import time
import os

ADB = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"

def run_adb(cmd):
    full_cmd = f'"{ADB}" {cmd}'
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def tap(x, y):
    run_adb(f"shell input tap {x} {y}")
    time.sleep(1.5)

def swipe_up():
    run_adb("shell input swipe 540 600 540 1800")
    time.sleep(1)

def swipe_down():
    run_adb("shell input swipe 540 1800 540 600")
    time.sleep(1)

def capture(filename):
    out_path = os.path.join("benchmarks", filename)
    run_adb(f"shell screencap -p /sdcard/{filename}")
    run_adb(f"pull /sdcard/{filename} {out_path}")
    print(f"Captured: {out_path}")
    return out_path

def main():
    print("Capturing verified T2b Result screens in all 3 languages...")
    
    # 1. Scroll up to top of Telugu Result screen
    swipe_up()
    swipe_up()
    capture("result_screen_te.png")
    
    # Also capture bottom of Telugu Result screen
    swipe_down()
    capture("result_screen_te_bottom.png")
    swipe_up()

    # 2. Switch to Hindi via Language Chip
    print("Opening language picker from Telugu...")
    tap(920, 160)
    time.sleep(1)
    # Tap Hindi option in dialog
    tap(540, 1200)
    time.sleep(2)
    swipe_up()
    capture("result_screen_hi.png")
    swipe_down()
    capture("result_screen_hi_bottom.png")
    swipe_up()

    # 3. Switch to English via Language Chip
    print("Opening language picker from Hindi...")
    tap(920, 160)
    time.sleep(1)
    # Tap English option in dialog
    tap(540, 1320)
    time.sleep(2)
    swipe_up()
    capture("result_screen_en.png")
    
    # Tap View Details on English screen
    tap(180, 1140)
    time.sleep(1)
    capture("result_screen_en_details.png")

    # Scroll down to capture bottom of English Result screen (Demo text - not AI + Kisan Call Centre)
    swipe_down()
    capture("result_screen_en_bottom.png")

    print("All Result screens successfully captured!")

if __name__ == "__main__":
    main()

