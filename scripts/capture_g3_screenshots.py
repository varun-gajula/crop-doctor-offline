import os
import subprocess
import time
import re
from PIL import Image

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")

def adb(cmd):
    full_cmd = f'"{ADB}" {cmd}'
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def send_action(action):
    print(f"Sending action: {action}")
    adb(f"shell am start -n com.cropdoctor/.MainActivity --es benchmark_action {action}")
    time.sleep(1.2)

def scroll_to_top():
    adb("shell input swipe 540 500 540 2200 200")
    time.sleep(0.3)
    adb("shell input swipe 540 500 540 2200 200")
    time.sleep(0.3)

def scroll_to_field_question():
    scroll_to_top()
    adb("shell input swipe 540 1800 540 1200 300")
    time.sleep(0.8)

def tap_spread_option(option_name):
    for attempt in range(3):
        dump = adb("exec-out uiautomator dump /dev/tty")
        m = re.search(rf'text="([^"]*{re.escape(option_name)}[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', dump)
        if m:
            x = (int(m.group(2)) + int(m.group(4))) // 2
            y = (int(m.group(3)) + int(m.group(5))) // 2
            print(f"Tapping '{option_name}' at ({x}, {y})")
            adb(f"shell input tap {x} {y}")
            time.sleep(1.0)
            return True
        else:
            print(f"Attempt {attempt+1}: '{option_name}' not in view, scrolling slightly...")
            adb("shell input swipe 540 1400 540 1100 200")
            time.sleep(0.6)
    print(f"Could not find button for '{option_name}'")
    return False

def capture(filename):
    os.makedirs("benchmarks", exist_ok=True)
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
    print("Capturing CHECKPOINT G3 Screenshots for Urgency from Field Spread...")

    # 1. English - Early Warning (Mild chilli + tap 'A few plants')
    send_action("lang_en")
    send_action("show_mild_chilli")
    scroll_to_field_question()
    tap_spread_option("A few plants")
    img_ew_en = capture("g3_urgency_early_warning_en.png")

    # 2. English - Act this week (Moderate chilli + tap 'A few plants')
    send_action("show_chilli")
    scroll_to_field_question()
    tap_spread_option("A few plants")
    img_tw_en = capture("g3_urgency_act_this_week_en.png")

    # 3. English - Act now (Moderate chilli + tap 'Some' or 'Most')
    scroll_to_field_question()
    tap_spread_option("Most")
    img_an_en = capture("g3_urgency_act_now_en.png")

    # 4. Telugu - Early Warning (Mild chilli + tap 'కొన్ని మొక్కలు')
    send_action("lang_te")
    send_action("show_mild_chilli")
    scroll_to_field_question()
    tap_spread_option("కొన్ని మొక్కలు")
    img_ew_te = capture("g3_urgency_early_warning_te.png")

    # 5. Telugu - Act now (tap 'ఎక్కువ భాగం')
    scroll_to_field_question()
    tap_spread_option("ఎక్కువ భాగం")
    img_an_te = capture("g3_urgency_act_now_te.png")

    # 6. Hindi - Early Warning (Mild chilli + tap 'कुछ पौधे')
    send_action("lang_hi")
    send_action("show_mild_chilli")
    scroll_to_field_question()
    tap_spread_option("कुछ पौधे")
    img_ew_hi = capture("g3_urgency_early_warning_hi.png")

    # 7. Hindi - Act now (tap 'अधिकांश खेत')
    scroll_to_field_question()
    tap_spread_option("अधिकांश खेत")
    img_an_hi = capture("g3_urgency_act_now_hi.png")

    # Revert to English
    send_action("lang_en")

    # Collages
    create_collage([img_ew_en, img_tw_en, img_an_en], "benchmarks/g3_urgency_levels_collage.png")
    create_collage([img_ew_en, img_ew_te, img_ew_hi], "benchmarks/g3_urgency_languages_collage.png")
    print("Done capturing CHECKPOINT G3 deliverables!")

if __name__ == "__main__":
    main()
