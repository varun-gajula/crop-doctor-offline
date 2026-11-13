import subprocess
import time
import os
import xml.etree.ElementTree as ET

ADB = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"

def run_adb(cmd):
    full_cmd = f'"{ADB}" {cmd}'
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def tap(x, y):
    run_adb(f"shell input tap {x} {y}")
    time.sleep(1.5)

def capture(filename):
    out_path = os.path.join("benchmarks", filename)
    run_adb(f"shell screencap -p /sdcard/{filename}")
    run_adb(f"pull /sdcard/{filename} {out_path}")
    print(f"Captured: {out_path}")
    return out_path

def get_ui_dump():
    run_adb("shell uiautomator dump /data/local/tmp/dump.xml")
    xml_str = run_adb("shell cat /data/local/tmp/dump.xml")
    try:
        return ET.fromstring(xml_str)
    except Exception as e:
        print("XML parse error:", e)
        return None

def find_node(root, text_contains):
    if root is None:
        return None
    for elem in root.iter('node'):
        txt = elem.attrib.get('text', '')
        if text_contains.lower() in txt.lower():
            return elem
    return None

def get_center(bounds_str):
    # bounds="[53,1153][1027,1374]"
    clean = bounds_str.replace("][", ",").replace("[", "").replace("]", "")
    parts = [int(p) for p in clean.split(",")]
    return ((parts[0] + parts[2]) // 2, (parts[1] + parts[3]) // 2)

def main():
    print("Starting T2b screenshot capture workflow...")
    
    # 1. Inspect First Launch Screen
    root = get_ui_dump()
    te_node = find_node(root, "తెలుగు")
    if te_node is not None:
        cx, cy = get_center(te_node.attrib['bounds'])
        print(f"Tapping Telugu button at {cx}, {cy}")
        tap(cx, cy)
    else:
        print("Telugu button not found on first screen, checking current state...")

    # 2. Region selection screen
    time.sleep(1)
    root = get_ui_dump()
    tg_node = find_node(root, "తెలంగాణ") or find_node(root, "Telangana")
    if tg_node is not None:
        cx, cy = get_center(tg_node.attrib['bounds'])
        print(f"Tapping Telangana button at {cx}, {cy}")
        tap(cx, cy)

    time.sleep(2)
    # 3. Verify Home Screen (Check: No Chatbot button and No Farming Tips tile)
    root = get_ui_dump()
    chat_node = find_node(root, "Chat with Agri AI Doctor") or find_node(root, "అగ్రి AI డాక్టర్")
    tips_node = find_node(root, "వ్యవసాయ చిట్కాలు") or find_node(root, "Farming Tips")
    print(f"Home verification -> Chatbot button present: {chat_node is not None} (MUST BE FALSE)")
    print(f"Home verification -> Farming tips present: {tips_node is not None} (MUST BE FALSE)")
    capture("t2b_screen_home_te.png")

    # Tap Scan Leaf
    scan_node = find_node(root, "ఆకును స్కాన్ చేయండి") or find_node(root, "స్కాన్") or find_node(root, "Scan")
    if scan_node is not None:
        cx, cy = get_center(scan_node.attrib['bounds'])
        print(f"Tapping Scan Leaf at {cx}, {cy}")
        tap(cx, cy)
    else:
        print("Tapping center of scan card (540, 950)")
        tap(540, 950)

    time.sleep(2)
    # 4. Camera Screen: Tap Capture button
    capture("t2b_screen_camera_te.png")
    print("Tapping camera shutter button (540, 2150)...")
    tap(540, 2150)
    time.sleep(2)

    # Check for soft warning dialog if present
    root = get_ui_dump()
    use_anyway = find_node(root, "ఫోటోను ఉపయోగించండి") or find_node(root, "Use this photo anyway") or find_node(root, "ఉపయోగించండి")
    if use_anyway is not None:
        cx, cy = get_center(use_anyway.attrib['bounds'])
        print(f"Soft warning appeared -> tapping Use anyway at {cx}, {cy}")
        tap(cx, cy)
        time.sleep(3)

    time.sleep(2)
    # 5. Arrived at Result Screen in Telugu!
    print("Arrived at Result Screen!")
    capture("t2b_result_te.png")

    # Expand Details panel
    root = get_ui_dump()
    details_node = find_node(root, "వివరాలు చూడండి") or find_node(root, "View Details") or find_node(root, "వివరాలు")
    if details_node is not None:
        cx, cy = get_center(details_node.attrib['bounds'])
        print(f"Tapping View Details at {cx}, {cy}")
        tap(cx, cy)
        time.sleep(1)
        capture("t2b_result_details_te.png")

    # Switch to Hindi via Language Chip
    print("Tapping Language Chip at top right (980, 140)...")
    tap(980, 140)
    time.sleep(1)
    root = get_ui_dump()
    hi_picker = find_node(root, "हिन्दी")
    if hi_picker is not None:
        cx, cy = get_center(hi_picker.attrib['bounds'])
        print(f"Selecting Hindi in picker at {cx}, {cy}")
        tap(cx, cy)
    else:
        print("Selecting Hindi at default dialog coordinate (540, 1200)")
        tap(540, 1200)
    time.sleep(2)
    capture("t2b_result_hi.png")

    # Switch to English via Language Chip
    print("Tapping Language Chip at top right (980, 140)...")
    tap(980, 140)
    time.sleep(1)
    root = get_ui_dump()
    en_picker = find_node(root, "English")
    if en_picker is not None:
        cx, cy = get_center(en_picker.attrib['bounds'])
        print(f"Selecting English in picker at {cx}, {cy}")
        tap(cx, cy)
    else:
        print("Selecting English at default dialog coordinate (540, 1320)")
        tap(540, 1320)
    time.sleep(2)
    capture("t2b_result_en.png")

    # Expand English Details to show Confidence only (no Latency, no RAM)
    root = get_ui_dump()
    en_details = find_node(root, "View Details")
    if en_details is not None:
        cx, cy = get_center(en_details.attrib['bounds'])
        print(f"Tapping View Details in English at {cx}, {cy}")
        tap(cx, cy)
        time.sleep(1)
        capture("t2b_result_en_details.png")

    print("All T2b verification screens captured successfully!")

if __name__ == "__main__":
    main()

