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
OUT_PATH = os.path.join("benchmarks", "aiagricare_real_field_results.json")
os.makedirs("benchmarks", exist_ok=True)

def run_adb(args, timeout=40):
    cmd = [ADB, "-s", DEVICE_SERIAL] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

print("=" * 80)
print("AI AGRI CARE (com.aiagricare) - Real Field Image Evaluation")
print("Thresholds: confidence=0.32, energy=-3.30, crop_mass=0.40, margin=0.15")
print(f"Target Device: POCO M4 5G ({DEVICE_SERIAL})")
print("=" * 80)

# Real field photos dataset
photos = [
    # 1. RICE
    {"category": "field_crop", "crop": "rice", "expected": "rice_bacterial_leaf_blight",
     "file": r"C:\crop-data\realtest\gallery\gallery_01_rice_bacterial_leaf_blight_000001_Rice_20231006_164208.jpg", "label": "Rice Bacterial Leaf Blight"},
    {"category": "field_crop", "crop": "rice", "expected": "rice_leaf_blast",
     "file": r"C:\crop-data\realtest\gallery\gallery_02_rice_leaf_blast_000001_Rice_aug_0_101.jpg", "label": "Rice Leaf Blast"},
    {"category": "field_crop", "crop": "rice", "expected": "rice_healthy",
     "file": r"C:\crop-data\realtest\gallery\gallery_04_rice_healthy_000002_Rice_20231006_163740.jpg", "label": "Rice Healthy"},
    
    # 2. COTTON
    {"category": "field_crop", "crop": "cotton", "expected": "cotton_bacterial_blight",
     "file": r"C:\crop-data\realtest\gallery\gallery_05_cotton_bacterial_blight_000003_Cotton_bact0.jpg", "label": "Cotton Bacterial Blight"},
    {"category": "field_crop", "crop": "cotton", "expected": "cotton_fusarium_wilt",
     "file": r"C:\crop-data\realtest\gallery\gallery_06_cotton_fusarium_wilt_000003_Cotton_done1.jpg", "label": "Cotton Fusarium Wilt"},
    {"category": "field_crop", "crop": "cotton", "expected": "cotton_healthy",
     "file": r"C:\crop-data\realtest\gallery\gallery_08_cotton_healthy_000004_Cotton_h101.jpg", "label": "Cotton Healthy"},

    # 3. CHILLI
    {"category": "field_crop", "crop": "chilli", "expected": "chilli_leaf_curl_virus",
     "file": r"C:\crop-data\realtest\gallery\gallery_09_chilli_leaf_curl_virus_000005_Curl_Virus00005.jpg", "label": "Chilli Leaf Curl Virus"},
    {"category": "field_crop", "crop": "chilli", "expected": "chilli_healthy",
     "file": r"C:\crop-data\realtest\gallery\gallery_11_chilli_healthy_000006_Healthy_Leaf00006.jpg", "label": "Chilli Healthy"},

    # 4. TOMATO
    {"category": "field_crop", "crop": "tomato", "expected": "tomato_yellow_leaf_curl_virus",
     "file": r"C:\crop-data\realtest\gallery\gallery_13_tomato_yellow_leaf_curl_virus_000007_101tomato_train_leaf_curl.jpg", "label": "Tomato Yellow Leaf Curl"},
    {"category": "field_crop", "crop": "tomato", "expected": "tomato_septoria_leaf_spot",
     "file": r"C:\crop-data\realtest\gallery\gallery_14_tomato_septoria_leaf_spot_000007_1050tomato_train_septoria_leaf_spot.jpg", "label": "Tomato Septoria Leaf Spot"},
    {"category": "field_crop", "crop": "tomato", "expected": "tomato_healthy",
     "file": r"C:\crop-data\realtest\gallery\gallery_15_tomato_healthy_000008_1018tomato_train_healthy.jpg", "label": "Tomato Healthy"},

    # 5. MAIZE
    {"category": "field_crop", "crop": "maize", "expected": "maize_common_rust",
     "file": r"C:\crop-data\realtest\gallery\gallery_17_maize_common_rust_000009_Corn_Corn_Common_Rust__1041_.jpg", "label": "Maize Common Rust"},
    {"category": "field_crop", "crop": "maize", "expected": "maize_northern_leaf_blight",
     "file": r"C:\crop-data\realtest\gallery\gallery_19_maize_northern_leaf_blight_000010_Corn_Corn_Blight__1010_.jpg", "label": "Maize Northern Leaf Blight"},
    {"category": "field_crop", "crop": "maize", "expected": "maize_healthy",
     "file": r"C:\crop-data\realtest\gallery\gallery_20_maize_healthy_000010_Corn_Corn_Health__1018_.jpg", "label": "Maize Healthy"},

    # 6. OOD: OTHER CROP (Cashew) -> Expected: Rejection (CANT_TELL or OUT_OF_CLASS)
    {"category": "other_crop_ood", "crop": "cashew", "expected": "reject",
     "file": r"C:\crop-data\realtest\othercrop\other_cashew_anthracnose_00_anthracnose1000_.jpg", "label": "Other Crop: Cashew Anthracnose"},

    # 7. OOD: NO CROP (Object/Background) -> Expected: Rejection (CANT_TELL via Leaf Validator / Weeds)
    {"category": "no_crop_ood", "crop": "none", "expected": "reject",
     "file": r"C:\crop-data\realtest\nocrop\nocrop_object_00_IMG_20220824_221131.jpg", "label": "No Crop: Household Object"},
]

results = []

for idx, p in enumerate(photos, 1):
    local_path = p["file"]
    crop = p["crop"]
    label = p["label"]
    expected = p["expected"]
    category = p["category"]
    filename = os.path.basename(local_path)
    
    print(f"\n[{idx}/{len(photos)}] Testing {label} ({filename})...")
    
    if not os.path.exists(local_path):
        print(f"    [SKIP] Local file not found: {local_path}")
        continue

    # Push to temp and copy into app files directory
    run_adb(["push", local_path, "/data/local/tmp/test_photo.jpg"])
    run_adb(["shell", "chmod 666 /data/local/tmp/test_photo.jpg && run-as com.aiagricare cp /data/local/tmp/test_photo.jpg /data/data/com.aiagricare/files/test_photo.jpg"])
    
    # Clear logcat before running
    run_adb(["logcat", "-c"])
    
    # Launch diagnose_file on com.aiagricare
    remote_path = "/data/data/com.aiagricare/files/test_photo.jpg"
    crop_hint = crop if category == "field_crop" else "auto"
    run_adb(["shell", "am", "start", "-n", "com.aiagricare/com.cropdoctor.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", remote_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "false"])
             
    # Poll logcat for result
    decision = "UNKNOWN"
    code = ""
    top1_class = "unknown"
    confidence = 0.0
    energy = 0.0
    crop_mass = 0.0
    margin = 0.0
    gate1_str = ""
    gate2_str = ""
    gate3_str = ""
    
    p_start = time.time()
    while time.time() - p_start < 8:
        time.sleep(0.4)
        logs = run_adb(["logcat", "-d"])
        
        for line in logs.splitlines():
            if "CROPDOC_DECISION:" in line:
                m_o = re.search(r"outcome=([^\s]+)", line)
                m_code = re.search(r"code=([^\s]+)", line)
                m_cm = re.search(r"cropMass=([0-9.]+)", line)
                m_margin = re.search(r"margin=([0-9.]+)", line)
                m_g1 = re.search(r"gate1_leafValidator=([^\s]+)", line)
                m_g2 = re.search(r"gate2_energy=([^\s]+)", line)
                m_g3 = re.search(r"gate3_confidence=([^\s]+)", line)
                if m_o: decision = m_o.group(1)
                if m_code: code = m_code.group(1)
                if m_cm: crop_mass = float(m_cm.group(1))
                if m_margin: margin = float(m_margin.group(1))
                if m_g1: gate1_str = m_g1.group(1)
                if m_g2: gate2_str = m_g2.group(1)
                if m_g3: gate3_str = m_g3.group(1)
            
            if "PHOTO_RESULT:" in line:
                m_c = re.search(r"classId=([^\s]+)", line)
                m_cf = re.search(r"conf=([0-9.]+)", line)
                m_e = re.search(r"energy=([-\d.]+)", line)
                if m_c: top1_class = m_c.group(1)
                if m_cf: confidence = float(m_cf.group(1))
                if m_e: energy = float(m_e.group(1))
        
        if decision != "UNKNOWN" and top1_class != "unknown":
            break

    is_correct = False
    if expected == "reject":
        is_correct = (decision in ["CANT_TELL", "OUT_OF_CLASS"])
    else:
        is_correct = (decision == "DIAGNOSE" and top1_class == expected)

    status_symbol = "PASS" if is_correct else ("REJECTED_OOD" if decision in ["CANT_TELL", "OUT_OF_CLASS"] and expected == "reject" else "FAIL")
    print(f"    [{status_symbol}] Decision: {decision} ({code}), Top-1: {top1_class}, Conf: {confidence:.4f}, Energy: {energy:.4f}, CropMass: {crop_mass:.4f}, Margin: {margin:.4f}")

    results.append({
        "index": idx,
        "label": label,
        "category": category,
        "filename": filename,
        "expected": expected,
        "top1_class": top1_class,
        "decision": decision,
        "outcome_code": code,
        "confidence": confidence,
        "energy": energy,
        "crop_mass": crop_mass,
        "margin": margin,
        "gate1": gate1_str,
        "gate2": gate2_str,
        "gate3": gate3_str,
        "correct": is_correct
    })

summary = {
    "app": "AI agri care",
    "application_id": "com.aiagricare",
    "device": f"POCO M4 5G ({DEVICE_SERIAL})",
    "thresholds": {
        "confidence": 0.32,
        "energy": -3.30,
        "crop_mass": 0.40,
        "margin": 0.15
    },
    "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "total_images": len(results),
    "field_photos_count": len([r for r in results if r["category"] == "field_crop"]),
    "field_photos_diagnosed": len([r for r in results if r["category"] == "field_crop" and r["decision"] == "DIAGNOSE"]),
    "field_photos_correct": len([r for r in results if r["category"] == "field_crop" and r["correct"]]),
    "ood_photos_rejected": len([r for r in results if r["category"] != "field_crop" and r["correct"]]),
    "results": results
}

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(summary, f, indent=2)

print("\n" + "=" * 80)
print(f"Summary for AI agri care ({summary['application_id']}):")
print(f"  Field Photos Tested: {summary['field_photos_count']}")
print(f"  Field Photos Diagnosed: {summary['field_photos_diagnosed']}/{summary['field_photos_count']}")
print(f"  Field Photos Correct: {summary['field_photos_correct']}/{summary['field_photos_count']}")
print(f"  OOD / Non-crop Rejected: {summary['ood_photos_rejected']}/{len(results) - summary['field_photos_count']}")
print(f"Results saved to: {OUT_PATH}")
print("=" * 80)
