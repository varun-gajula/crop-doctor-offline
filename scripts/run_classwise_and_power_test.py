import os
import sys
import subprocess
import time
import json
import re

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

def get_device_serial():
    if len(sys.argv) > 1 and not sys.argv[1].startswith("-"):
        return sys.argv[1]
    res = subprocess.run([ADB, "devices"], capture_output=True, text=True, encoding="utf-8")
    lines = [l.split()[0] for l in res.stdout.strip().splitlines()[1:] if "\tdevice" in l]
    return lines[0] if lines else "ZA222YGPNG"

DEVICE_SERIAL = get_device_serial()
OUT_PATH = os.path.join("benchmarks", "classwise_and_power_results.json")
os.makedirs("benchmarks", exist_ok=True)

def run_adb(args, timeout=30):
    cmd = [ADB, "-s", DEVICE_SERIAL] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

def get_battery_info():
    out = run_adb(["shell", "dumpsys", "battery"])
    info = {}
    for line in out.splitlines():
        line = line.strip()
        if ":" in line:
            parts = line.split(":", 1)
            k = parts[0].strip()
            v = parts[1].strip()
            info[k] = v
    return info

# Detect device info and app UID
manufacturer = run_adb(["shell", "getprop", "ro.product.manufacturer"])
model = run_adb(["shell", "getprop", "ro.product.model"])
device_display = f"{manufacturer} {model} ({DEVICE_SERIAL})".strip()

uid_out = run_adb(["shell", "pm", "list", "packages", "-U"])
app_uid = "10368"
for line in uid_out.splitlines():
    if "com.aiagricare" in line and "uid:" in line:
        m = re.search(r"uid:(\d+)", line)
        if m:
            app_uid = m.group(1)

print("=" * 80)
print("AI AGRI CARE (com.aiagricare) - Class-Wise Performance & Power Benchmark")
print(f"Target Device: {device_display}")
print(f"Application UID: {app_uid}")
print("Active Thresholds: conf=0.32, energy=-3.30, crop_mass=0.40, margin=0.15")
print("=" * 80)

# Step 0: Ensure app is installed and awake
run_adb(["shell", "input", "keyevent", "KEYCODE_WAKEUP"])
run_adb(["shell", "wm", "dismiss-keyguard"])
run_adb(["shell", "am", "force-stop", "com.aiagricare"])
time.sleep(1)
run_adb(["shell", "am", "start", "-n", "com.aiagricare/com.cropdoctor.MainActivity"])
time.sleep(3)

# Load class labels
with open("app/src/main/assets/labels.json", "r", encoding="utf-8") as f:
    labels = json.load(f)

base_data_dir = "C:/crop-data/crop_doctor_15class"

test_items = []
for cid in labels:
    folder = os.path.join(base_data_dir, cid)
    if os.path.exists(folder):
        files = [f for f in sorted(os.listdir(folder)) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]
        if files:
            # Pick 2 samples per class
            chosen = files[:2]
            for c_idx, fname in enumerate(chosen, 1):
                crop_part = cid.split("_")[0]
                is_crop = (cid != "other_not_a_crop")
                expected = cid if is_crop else "reject"
                test_items.append({
                    "class_id": cid,
                    "sample_num": c_idx,
                    "crop": crop_part if is_crop else "auto",
                    "expected": expected,
                    "file_path": os.path.join(folder, fname),
                    "filename": fname
                })

print(f"Total test suite: {len(test_items)} images across {len(labels)} classes (2 samples per class)")

# --- START POWER BENCHMARK SETUP ---
print("\n[POWER BENCHMARK] Initializing battery measurement...")
batt_start = get_battery_info()
v_start_mv = float(batt_start.get("voltage", 3920))
temp_start_c = float(batt_start.get("temperature", 330)) / 10.0
level_start = int(batt_start.get("level", 40))

# Query full learned battery capacity (or design capacity)
battery_capacity_mah = 5000.0
if "Full capacity" in batt_start:
    try:
        battery_capacity_mah = float(batt_start["Full capacity"]) / 1000.0
    except Exception:
        pass

# Simulate unplugged battery mode for power calculation
run_adb(["shell", "dumpsys", "battery", "unplug"])
run_adb(["shell", "dumpsys", "batterystats", "--reset"])
time.sleep(1)

start_time = time.time()
print(f"  Starting Battery: {level_start}%, Voltage: {v_start_mv:.0f} mV, Temp: {temp_start_c:.1f} °C, Capacity: {battery_capacity_mah:.0f} mAh")
print("  Simulated On-Battery Mode: ACTIVE (dumpsys battery unplug)")
# ------------------------------------

results = []
scan_latencies = []

print("\n" + "-" * 80)
print(f"{'Idx':<4} {'Class ID':<28} {'Smpl':<5} {'Decision':<14} {'Top-1 Pred':<28} {'Conf':<7} {'Energy':<8} {'CropMass':<8} {'Lat(ms)':<7} {'Status'}")
print("-" * 115)

for idx, item in enumerate(test_items, 1):
    cid = item["class_id"]
    snum = item["sample_num"]
    local_path = item["file_path"]
    crop_hint = item["crop"]
    expected = item["expected"]
    
    # Push image to temp and copy via run-as
    run_adb(["push", local_path, "/data/local/tmp/test_photo.jpg"])
    run_adb(["shell", f"chmod 666 /data/local/tmp/test_photo.jpg && run-as com.aiagricare cp /data/local/tmp/test_photo.jpg /data/data/com.aiagricare/files/test_photo.jpg"])
    
    run_adb(["logcat", "-c"])
    
    t0 = time.time()
    remote_path = "/data/data/com.aiagricare/files/test_photo.jpg"
    run_adb(["shell", "am", "start", "-n", "com.aiagricare/com.cropdoctor.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", remote_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "false"])
             
    decision = "UNKNOWN"
    code = ""
    top1_class = "unknown"
    conf = 0.0
    energy = 0.0
    crop_mass = 0.0
    margin = 0.0
    
    while time.time() - t0 < 6:
        time.sleep(0.3)
        logs = run_adb(["logcat", "-d"])
        
        for line in logs.splitlines():
            if "CROPDOC_DECISION:" in line:
                m_o = re.search(r"outcome=([^\s]+)", line)
                m_code = re.search(r"code=([^\s]+)", line)
                m_cm = re.search(r"cropMass=([0-9.]+)", line)
                m_margin = re.search(r"margin=([0-9.]+)", line)
                if m_o: decision = m_o.group(1)
                if m_code: code = m_code.group(1)
                if m_cm: crop_mass = float(m_cm.group(1))
                if m_margin: margin = float(m_margin.group(1))
            
            if "PHOTO_RESULT:" in line:
                m_c = re.search(r"classId=([^\s]+)", line)
                m_cf = re.search(r"conf=([0-9.]+)", line)
                m_e = re.search(r"energy=([-\d.]+)", line)
                if m_c: top1_class = m_c.group(1)
                if m_cf: conf = float(m_cf.group(1))
                if m_e: energy = float(m_e.group(1))
                
        if decision in ["CANT_TELL", "OUT_OF_CLASS"]:
            break
        if decision != "UNKNOWN" and top1_class != "unknown":
            break
            
    latency_ms = int((time.time() - t0) * 1000)
    scan_latencies.append(latency_ms)
    
    if expected == "reject":
        is_pass = (decision in ["CANT_TELL", "OUT_OF_CLASS"])
    else:
        is_pass = (decision == "DIAGNOSE" and top1_class == expected)
        
    status = "PASS" if is_pass else ("REJECTED_OOD" if expected == "reject" else "FAIL")
    
    print(f"{idx:<4} {cid:<28} {snum:<5} {decision:<14} {top1_class:<28} {conf:<7.4f} {energy:<8.4f} {crop_mass:<8.4f} {latency_ms:<7} {status}")
    
    results.append({
        "index": idx,
        "class_id": cid,
        "sample_num": snum,
        "filename": item["filename"],
        "expected": expected,
        "decision": decision,
        "outcome_code": code,
        "top1_class": top1_class,
        "confidence": conf,
        "energy": energy,
        "crop_mass": crop_mass,
        "margin": margin,
        "latency_ms": latency_ms,
        "pass": is_pass
    })

# --- POWER BENCHMARK COLLECTION ---
end_time = time.time()
elapsed_sec = end_time - start_time
batt_end = get_battery_info()
v_end_mv = float(batt_end.get("voltage", v_start_mv))
temp_end_c = float(batt_end.get("temperature", 330)) / 10.0
level_end = int(batt_end.get("level", 40))

# Query batterystats for UID
bstats_raw = run_adb(["shell", "dumpsys", "batterystats"])

# Restore normal charging
run_adb(["shell", "dumpsys", "battery", "reset"])

# Parse UID power usage
uid_power_mah = 0.0
uid_cpu_mah = 0.0
uid_screen_mah = 0.0
uid_suffix = app_uid[-4:] if len(app_uid) >= 4 else app_uid

for line in bstats_raw.splitlines():
    line_s = line.strip()
    if f"UID u0a{uid_suffix}:" in line_s or f"UID {app_uid}:" in line_s:
        m_mah = re.search(r"UID [^:]+:\s*([0-9.]+)", line_s)
        if m_mah:
            uid_power_mah = float(m_mah.group(1))
    if f"screen=" in line_s and "cpu=" in line_s and (uid_cpu_mah == 0.0 or uid_screen_mah == 0.0):
        # Check if preceding line was our UID
        m_cpu = re.search(r"cpu=([0-9.]+)", line_s)
        m_scr = re.search(r"screen=([0-9.]+)", line_s)
        if m_cpu: uid_cpu_mah = float(m_cpu.group(1))
        if m_scr: uid_screen_mah = float(m_scr.group(1))

if uid_power_mah == 0.0 and uid_cpu_mah > 0:
    uid_power_mah = uid_cpu_mah + uid_screen_mah

v_avg_v = (v_start_mv + v_end_mv) / 2000.0  # mV to V

# Energy calculations
energy_mwh = uid_power_mah * v_avg_v  # mWh
power_mw = (energy_mwh / (elapsed_sec / 3600.0)) if elapsed_sec > 0 else 0.0
energy_per_scan_mj = (energy_mwh * 3600.0 / len(test_items)) if len(test_items) > 0 else 0.0

# Per-class summary aggregation
class_summary = {}
for cid in labels:
    items = [r for r in results if r["class_id"] == cid]
    correct = sum(1 for r in items if r["pass"])
    diagnosed = sum(1 for r in items if r["decision"] == "DIAGNOSE")
    avg_conf = sum(r["confidence"] for r in items) / len(items) if items else 0.0
    avg_lat = sum(r["latency_ms"] for r in items) / len(items) if items else 0.0
    class_summary[cid] = {
        "samples": len(items),
        "correct": correct,
        "diagnosed": diagnosed,
        "accuracy_pct": (correct / len(items) * 100.0) if items else 0.0,
        "avg_confidence": round(avg_conf, 4),
        "avg_latency_ms": round(avg_lat, 1)
    }

total_correct = sum(1 for r in results if r["pass"])
crop_classes = [c for c in labels if c != "other_not_a_crop"]
crop_results = [r for r in results if r["class_id"] in crop_classes]
crop_correct = sum(1 for r in crop_results if r["pass"])
crop_diagnosed = sum(1 for r in crop_results if r["decision"] == "DIAGNOSE")

final_report = {
    "app": "AI agri care",
    "application_id": "com.aiagricare",
    "device": device_display,
    "app_uid": app_uid,
    "thresholds": {
        "confidence": 0.32,
        "energy": -3.30,
        "crop_mass": 0.40,
        "margin": 0.15
    },
    "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "performance": {
        "total_images": len(results),
        "total_classes": len(labels),
        "overall_accuracy_pct": round(total_correct / len(results) * 100.0, 1),
        "crop_classes_accuracy_pct": round(crop_correct / len(crop_results) * 100.0, 1),
        "crop_classes_diagnostic_yield_pct": round(crop_diagnosed / len(crop_results) * 100.0, 1),
        "avg_inference_latency_ms": round(sum(scan_latencies) / len(scan_latencies), 1),
        "min_latency_ms": min(scan_latencies),
        "max_latency_ms": max(scan_latencies),
        "class_summary": class_summary
    },
    "power_consumption": {
        "duration_seconds": round(elapsed_sec, 2),
        "battery_capacity_mah": battery_capacity_mah,
        "battery_level_start_pct": level_start,
        "battery_level_end_pct": level_end,
        "voltage_start_mv": v_start_mv,
        "voltage_end_mv": v_end_mv,
        "voltage_avg_v": round(v_avg_v, 3),
        "temperature_start_c": temp_start_c,
        "temperature_end_c": temp_end_c,
        "temperature_delta_c": round(temp_end_c - temp_start_c, 1),
        "uid_power_drain_mah": round(uid_power_mah, 4),
        "uid_energy_consumed_mwh": round(energy_mwh, 4),
        "uid_average_power_mw": round(power_mw, 2),
        "energy_per_scan_mj": round(energy_per_scan_mj, 2),
        "estimated_scans_per_1pct_battery": round((battery_capacity_mah * 0.01) / (uid_power_mah / len(test_items))) if uid_power_mah > 0 else "N/A",
        "estimated_total_scans_full_charge": round(battery_capacity_mah / (uid_power_mah / len(test_items))) if uid_power_mah > 0 else "N/A"
    },
    "individual_results": results
}

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(final_report, f, indent=2)

# Also write device-specific file
dev_clean = re.sub(r"[^a-zA-Z0-9_]+", "_", f"{manufacturer}_{model}".lower().strip("_"))
dev_out = os.path.join("benchmarks", f"{dev_clean}_classwise_power_results.json")
with open(dev_out, "w", encoding="utf-8") as f:
    json.dump(final_report, f, indent=2)
if "motorola" in dev_clean:
    edge_out = os.path.join("benchmarks", "motorola_edge60_classwise_power_results.json")
    with open(edge_out, "w", encoding="utf-8") as f:
        json.dump(final_report, f, indent=2)

print("\n" + "=" * 80)
print(f"BENCHMARK SUMMARY FOR {device_display}")
print("=" * 80)
print(f"Total Images: {len(results)} across {len(labels)} classes")
print(f"Crop-Class Accuracy: {crop_correct}/{len(crop_results)} ({final_report['performance']['crop_classes_accuracy_pct']}%)")
print(f"Crop-Class Diagnostic Yield: {crop_diagnosed}/{len(crop_results)} ({final_report['performance']['crop_classes_diagnostic_yield_pct']}%)")
print(f"Avg Latency: {final_report['performance']['avg_inference_latency_ms']} ms (Min: {final_report['performance']['min_latency_ms']} ms, Max: {final_report['performance']['max_latency_ms']} ms)")
print("\nPOWER & ENERGY:")
print(f"  Test Duration: {elapsed_sec:.1f} s")
print(f"  Battery Voltage: {v_avg_v:.3f} V | Temp: {temp_start_c:.1f}°C -> {temp_end_c:.1f}°C (Delta: +{temp_end_c - temp_start_c:.1f}°C)")
print(f"  App Power Drain: {uid_power_mah:.4f} mAh ({energy_mwh:.4f} mWh)")
print(f"  Average Power Draw: {power_mw:.2f} mW")
print(f"  Energy Per Scan: {energy_per_scan_mj:.2f} mJ/scan")
if uid_power_mah > 0:
    print(f"  Endurance: ~{final_report['power_consumption']['estimated_scans_per_1pct_battery']} scans per 1% battery (~{final_report['power_consumption']['estimated_total_scans_full_charge']} scans on a full {battery_capacity_mah:.0f} mAh charge)")
print(f"\nFull benchmark report saved to: {OUT_PATH} and {dev_out}")
print("=" * 80)
