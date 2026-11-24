import os
import sys
import subprocess
import time
import json
import re

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

DEVICE_SERIAL = "sseqjnucrkeeprtw"
APK_PATH = r"app\build\outputs\apk\debug\app-debug.apk"
OUT_PATH = os.path.join("benchmarks", "phone_v15_part1.json")
os.makedirs("benchmarks", exist_ok=True)

def run_adb(args, timeout=60):
    cmd = [ADB, "-s", DEVICE_SERIAL] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

print("=" * 70)
print("Crop Doctor - Task PHONE-V15: Part 1 (10 Real Photos on POCO M4 5G)")
print("=" * 70)

# Step 1: Install APK
print("Installing debug APK to POCO M4 5G...")
install_out = run_adb(["install", "-r", APK_PATH], timeout=120)
print(f"Install Result: {install_out}")

# Step 2: Clear logcat and launch MainActivity to measure model load time
run_adb(["logcat", "-G", "16M"])
run_adb(["logcat", "-c"])
run_adb(["shell", "am", "force-stop", "com.cropdoctor"])
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity"])

print("Waiting for model initialization...")
model_load_time_ms = -1
poll_start = time.time()
while time.time() - poll_start < 20:
    time.sleep(0.5)
    logs = run_adb(["logcat", "-d", "-s", "ExecuTorchClassifier:V", "CropDoctorApp:V"])
    m = re.search(r"Successfully loaded ExecuTorch vision model in (\d+)ms", logs)
    if m:
        model_load_time_ms = int(m.group(1))
        print(f"Model Load Time: {model_load_time_ms} ms")
        break

photos = [
    {"crop": "rice", "file": r"C:\crop-data\realtest\gallery\gallery_01_rice_bacterial_leaf_blight_000001_Rice_20231006_164208.jpg", "label": "Rice Blight 1"},
    {"crop": "rice", "file": r"C:\crop-data\realtest\gallery\gallery_02_rice_leaf_blast_000001_Rice_aug_0_101.jpg", "label": "Rice Blast 2"},
    {"crop": "cotton", "file": r"C:\crop-data\realtest\gallery\gallery_05_cotton_bacterial_blight_000003_Cotton_bact0.jpg", "label": "Cotton Blight 1"},
    {"crop": "cotton", "file": r"C:\crop-data\realtest\gallery\gallery_06_cotton_fusarium_wilt_000003_Cotton_done1.jpg", "label": "Cotton Wilt 2"},
    {"crop": "chilli", "file": r"C:\crop-data\realtest\gallery\gallery_09_chilli_leaf_curl_virus_000005_Curl_Virus00005.jpg", "label": "Chilli Leaf Curl 1"},
    {"crop": "chilli", "file": r"C:\crop-data\realtest\gallery\gallery_11_chilli_healthy_000006_Healthy_Leaf00006.jpg", "label": "Chilli Healthy 2"},
    {"crop": "tomato", "file": r"C:\crop-data\realtest\gallery\gallery_13_tomato_yellow_leaf_curl_virus_000007_101tomato_train_leaf_curl.jpg", "label": "Tomato Leaf Curl 1"},
    {"crop": "tomato", "file": r"C:\crop-data\realtest\gallery\gallery_14_tomato_septoria_leaf_spot_000007_1050tomato_train_septoria_leaf_spot.jpg", "label": "Tomato Septoria 2"},
    {"crop": "maize", "file": r"C:\crop-data\realtest\gallery\gallery_17_maize_common_rust_000009_Corn_Corn_Common_Rust__1041_.jpg", "label": "Maize Rust 1"},
    {"crop": "maize", "file": r"C:\crop-data\realtest\gallery\gallery_19_maize_northern_leaf_blight_000010_Corn_Corn_Blight__1010_.jpg", "label": "Maize Blight 2"},
]

results = []

for idx, p in enumerate(photos, 1):
    local_path = p["file"]
    crop = p["crop"]
    label = p["label"]
    filename = os.path.basename(local_path)
    remote_path = f"/sdcard/Download/test_photo_{idx}.jpg"
    
    print(f"\n[{idx}/10] Testing {crop.upper()} ({label}): {filename}")
    # Push image to /data/local/tmp and copy via run-as to app internal storage
    run_adb(["push", local_path, "/data/local/tmp/test_photo.jpg"])
    run_adb(["shell", "chmod 666 /data/local/tmp/test_photo.jpg && run-as com.cropdoctor cp /data/local/tmp/test_photo.jpg /data/data/com.cropdoctor/files/test_photo.jpg"])
    
    run_adb(["logcat", "-c"])
    
    # Launch diagnose_file
    remote_path = "/data/data/com.cropdoctor/files/test_photo.jpg"
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", remote_path,
             "--ez", "auto_start_advisor", "false"])
             
    # Wait for result
    decision = "UNKNOWN"
    top1_class = "unknown"
    confidence = 0.0
    energy = 0.0
    crop_mass = 0.0
    
    p_start = time.time()
    while time.time() - p_start < 15:
        time.sleep(0.5)
        logs = run_adb(["logcat", "-d"])
        
        # Check PHOTO_RESULT or CROPDOC_DECISION
        if "PHOTO_RESULT:" in logs:
            for line in logs.splitlines():
                if "PHOTO_RESULT:" in line:
                    m_c = re.search(r"classId=([^\s]+)", line)
                    m_o = re.search(r"outcome=([^\s]+)", line)
                    m_cf = re.search(r"conf=([0-9.]+)", line)
                    m_e = re.search(r"energy=([-\d.]+)", line)
                    m_cm = re.search(r"cropMass=([0-9.]+)", line)
                    if m_c: top1_class = m_c.group(1)
                    if m_o: decision = m_o.group(1)
                    if m_cf: confidence = float(m_cf.group(1))
                    if m_e: energy = float(m_e.group(1))
                    if m_cm: crop_mass = float(m_cm.group(1))
            break
            
        if "CROPDOC_DECISION:" in logs:
            for line in logs.splitlines():
                if "CROPDOC_DECISION:" in line:
                    m_o = re.search(r"outcome=([^\s]+)", line)
                    m_cm = re.search(r"cropMass=([0-9.]+)", line)
                    m_e = re.search(r"energy=([-\d.]+)", line)
                    if m_o: decision = m_o.group(1)
                    if m_cm: crop_mass = float(m_cm.group(1))
                    if m_e: energy = float(m_e.group(1))
            for line in logs.splitlines():
                if "classify(): eval result" in line:
                    m_c = re.search(r"classId=([^\s,]+)", line)
                    m_cf = re.search(r"conf=([0-9.]+)", line)
                    if m_c: top1_class = m_c.group(1)
                    if m_cf: confidence = float(m_cf.group(1))
            if decision != "UNKNOWN" and top1_class != "unknown":
                break

    item_res = {
        "index": idx,
        "crop": crop,
        "label": label,
        "filename": filename,
        "decision": decision,
        "top1_class": top1_class,
        "confidence": confidence,
        "energy": energy,
        "crop_mass": crop_mass
    }
    results.append(item_res)
    print(f"    -> Decision: {decision}, Top-1: {top1_class}, Conf: {confidence:.4f}, Energy: {energy:.4f}, CropMass: {crop_mass:.4f}")

part1_data = {
    "device": "POCO M4 5G (22041219PI)",
    "model_load_time_ms": model_load_time_ms,
    "total_photos": len(results),
    "photos": results,
    "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S")
}

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(part1_data, f, indent=2)

print("\n" + "=" * 70)
print(f"PART 1 COMPLETE! Model Load Time: {model_load_time_ms} ms")
print(f"Saved: {OUT_PATH}")
print("=" * 70)
