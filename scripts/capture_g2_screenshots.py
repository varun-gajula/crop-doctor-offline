import os
import subprocess
import time
from PIL import Image

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")

def adb(cmd):
    full_cmd = f'"{ADB}" {cmd}'
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def send_action(action):
    print(f"Sending action: {action}")
    adb(f"shell am start -n com.cropdoctor/.MainActivity --es benchmark_action {action}")
    time.sleep(1.5)

def scroll_to_top():
    adb("shell input swipe 540 500 540 2200 200")
    time.sleep(0.4)
    adb("shell input swipe 540 500 540 2200 200")
    time.sleep(0.4)

def scroll_to_regional_card():
    scroll_to_top()
    # Scroll slightly down so Regional Advisory card is centered in view
    adb("shell input swipe 540 1700 540 1000 300")
    time.sleep(0.8)

def capture(filename):
    os.makedirs("benchmarks", exist_ok=True)
    scroll_to_regional_card()
    out_path = os.path.join("benchmarks", filename)
    adb("shell screencap -p /sdcard/temp_screen.png")
    adb(f"pull /sdcard/temp_screen.png {out_path}")
    print(f"Saved: {out_path}")
    return out_path

def create_collage(images, output_path):
    cols = len(images)
    loaded = [Image.open(p) for p in images]
    w, h = loaded[0].size
    scale = 0.5
    thumb_w, thumb_h = int(w * scale), int(h * scale)
    
    collage = Image.new("RGB", (thumb_w * cols, thumb_h), (255, 255, 255))
    for idx, img in enumerate(loaded):
        thumb = img.resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        collage.paste(thumb, (idx * thumb_w, 0))
    
    collage.save(output_path, quality=90)
    print(f"Saved collage to: {output_path}")

def main():
    print("Capturing CHECKPOINT G2 Screenshots for 3 regions...")

    # 1. Telangana (English)
    send_action("region_telangana")
    send_action("lang_en")
    send_action("show_chilli")
    img_tg = capture("g2_region_telangana.png")

    # 2. Andhra Pradesh (English)
    send_action("region_andhra")
    send_action("show_chilli")
    img_ap = capture("g2_region_andhra.png")

    # 3. Other (English)
    send_action("region_other")
    send_action("show_chilli")
    img_other = capture("g2_region_other.png")

    # 4. Telugu versions
    send_action("region_telangana")
    send_action("lang_te")
    send_action("show_chilli")
    img_tg_te = capture("g2_region_telangana_te.png")

    send_action("region_andhra")
    send_action("lang_te")
    send_action("show_chilli")
    img_ap_te = capture("g2_region_andhra_te.png")

    send_action("region_other")
    send_action("lang_te")
    send_action("show_chilli")
    img_other_te = capture("g2_region_other_te.png")

    # 5. Capture Helpline Dialog in Andhra Pradesh (ANGRAU 1800-425-0430 + Kisan 1800-180-1551)
    send_action("lang_en")
    send_action("region_andhra")
    send_action("show_chilli")
    scroll_to_top()
    # Scroll down to Extension referral button
    adb("shell input swipe 540 1800 540 500 300")
    time.sleep(0.5)
    adb("shell input swipe 540 1800 540 800 300")
    time.sleep(0.5)
    # Tap the "Helplines / Refer to Extension Officer" button (approx x=540, y=1400)
    dump = adb("exec-out uiautomator dump /dev/tty")
    if "Helpline" in dump or "Extension" in dump or "Call" in dump:
        adb("shell input tap 540 1700")
        time.sleep(1.0)
        adb("shell screencap -p /sdcard/temp_screen.png")
        adb("pull /sdcard/temp_screen.png benchmarks/g2_helplines_andhra.png")
        print("Saved benchmarks/g2_helplines_andhra.png")
        adb("shell input keyevent 4")
        time.sleep(0.5)

    create_collage([img_tg, img_ap, img_other], "benchmarks/g2_regional_sources_collage.png")
    create_collage([img_tg_te, img_ap_te, img_other_te], "benchmarks/g2_regional_sources_telugu_collage.png")
    print("Done capturing CHECKPOINT G2 deliverables!")

if __name__ == "__main__":
    main()
