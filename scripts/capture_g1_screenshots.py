import os
import subprocess
import time
from PIL import Image

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
EMULATOR = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe")

def run_cmd(cmd):
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def adb(cmd):
    full_cmd = f'"{ADB}" {cmd}'
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def ensure_device_connected():
    devices_out = adb("devices")
    print("ADB Devices:\n", devices_out)
    lines = [line for line in devices_out.splitlines() if "\tdevice" in line]
    if lines:
        dev_id = lines[0].split("\t")[0]
        print(f"Using connected device/emulator: {dev_id}")
        return dev_id
    
    # Try starting emulator
    print("No device detected. Starting emulator Pixel_7a...")
    emu_cmd = f'"{EMULATOR}" -avd Pixel_7a -no-audio -no-boot-anim'
    subprocess.Popen(emu_cmd, shell=True)
    
    print("Waiting for emulator to boot...")
    for i in range(40):
        time.sleep(3)
        devs = adb("devices")
        if "device" in devs and "offline" not in devs:
            boot_complete = adb("shell getprop sys.boot_completed")
            if boot_complete == "1":
                print("Emulator booted successfully!")
                time.sleep(2)
                return "emulator-5554"
    raise RuntimeError("Failed to start or connect to an Android device/emulator.")

def send_benchmark_action(action):
    print(f"Sending action: {action}")
    adb(f"shell am start -n com.cropdoctor/.MainActivity --es benchmark_action {action}")
    time.sleep(2.0)

def scroll_to_top():
    adb("shell input swipe 540 600 540 1800 200")
    time.sleep(0.5)
    adb("shell input swipe 540 600 540 1800 200")
    time.sleep(0.5)

def capture_screen(filename):
    os.makedirs("benchmarks", exist_ok=True)
    scroll_to_top()
    out_path = os.path.join("benchmarks", filename)
    adb("shell screencap -p /sdcard/temp_screen.png")
    adb(f"pull /sdcard/temp_screen.png {out_path}")
    print(f"Saved screenshot: {out_path}")
    return out_path

def create_collage(images_dict, output_path):
    # Expecting 6 images: disease en/te/hi and pest en/te/hi
    cols = 3
    rows = 2
    loaded = [Image.open(p) for p in images_dict]
    w, h = loaded[0].size
    scale = 0.5
    thumb_w, thumb_h = int(w * scale), int(h * scale)
    
    collage = Image.new("RGB", (thumb_w * cols, thumb_h * rows), (255, 255, 255))
    for idx, img in enumerate(loaded):
        thumb = img.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (idx % cols) * thumb_w
        y = (idx // cols) * thumb_h
        collage.paste(thumb, (x, y))
    
    collage.save(output_path, quality=90)
    print(f"Collage saved: {output_path}")

def main():
    ensure_device_connected()
    
    # 1. Disease in English, Telugu, Hindi
    print("\n--- Capturing Disease Results (Tomato Early Blight) ---")
    send_benchmark_action("lang_en")
    send_benchmark_action("show_disease")
    time.sleep(1.0)
    img_d_en = capture_screen("g1_disease_en.png")
    
    send_benchmark_action("lang_te")
    send_benchmark_action("show_disease")
    time.sleep(1.0)
    img_d_te = capture_screen("g1_disease_te.png")
    
    send_benchmark_action("lang_hi")
    send_benchmark_action("show_disease")
    time.sleep(1.0)
    img_d_hi = capture_screen("g1_disease_hi.png")
    
    # 2. Pest in English, Telugu, Hindi
    print("\n--- Capturing Pest Results (Tomato Spider Mites) ---")
    send_benchmark_action("lang_en")
    send_benchmark_action("show_pest")
    time.sleep(1.0)
    img_p_en = capture_screen("g1_pest_en.png")
    
    send_benchmark_action("lang_te")
    send_benchmark_action("show_pest")
    time.sleep(1.0)
    img_p_te = capture_screen("g1_pest_te.png")
    
    send_benchmark_action("lang_hi")
    send_benchmark_action("show_pest")
    time.sleep(1.0)
    img_p_hi = capture_screen("g1_pest_hi.png")
    
    # Create 6-screen comparison collage
    screens = [img_d_en, img_d_te, img_d_hi, img_p_en, img_p_te, img_p_hi]
    create_collage(screens, "benchmarks/g1_pest_disease_collage.png")
    print("\nCHECKPOINT G1: Disease and Pest screenshots captured in all 3 languages successfully!")

if __name__ == "__main__":
    main()
