import subprocess
import time
import os
import re
import json
import sys

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

DEVICE_SERIAL = "sseqjnucrkeeprtw"
OUT_PATH = os.path.join("benchmarks", "phone_v15_stageA.json")
os.makedirs("benchmarks", exist_ok=True)

def run_adb(args, timeout=30):
    cmd = [ADB, "-s", DEVICE_SERIAL] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

print("=" * 70)
print("Crop Doctor - Task PHONE-V15: Stage A")
print("20 Immediate Double-Scans (40 scans) + 20 Spaced Scans (2s gap)")
print("Target Device: POCO M4 5G (22041219PI)")
print("=" * 70)

# Clear logcat buffer
run_adb(["logcat", "-G", "16M"])
run_adb(["logcat", "-c"])

img_path = "/data/data/com.cropdoctor/files/case5_tomato.jpg"
crop_hint = "tomato"

stage_data = {
    "stage": "A",
    "description": "20 cycles of immediate double scans (40 scans) + 20 spaced scans (2s gap)",
    "device": "POCO M4 5G (22041219PI)",
    "start_time": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "immediate_cycles": [],
    "spaced_scans": [],
    "total_scans_requested": 60,
    "stuck_count": 0,
    "crashes_anrs": 0,
    "all_durations_ms": [],
    "min_spinner_ms": None,
    "max_spinner_ms": None,
    "avg_spinner_ms": None,
    "elapsed_seconds": 0.0,
    "status": "IN_PROGRESS"
}

start_wall_time = time.time()

# -------------------------------------------------------------
# Part 1: 20 Immediate Double-Scans (40 scans)
# -------------------------------------------------------------
print("\n--- Part A1: 20 Immediate Double-Scans (0s gap) ---")
for cycle_idx in range(1, 21):
    cycle_start = time.time()
    
    # Run scan 1
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "false",
             "--es", "language", "en"])
             
    # Immediate second scan while first is in flight
    time.sleep(0.12)
    
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "false",
             "--es", "language", "en"])

    # Wait for the second scan to settle
    time.sleep(2.5)
    
    # Read logcat since last check
    logs = run_adb(["logcat", "-d"])
    run_adb(["logcat", "-c"])
    
    cycle_durations = []
    cycle_req_ids = []
    cycle_stuck = 0
    cycle_crashes = 0
    
    for line in logs.splitlines():
        if "CROPDOC_LLAMA" in line and "SCAN_END:" in line:
            m_dur = re.search(r"durationMs=(\d+)ms", line)
            m_req = re.search(r"requestId=(\d+)", line)
            if m_dur:
                cycle_durations.append(int(m_dur.group(1)))
            if m_req:
                cycle_req_ids.append(int(m_req.group(1)))
        if "WATCHDOG_8S_TRIGGERED" in line:
            cycle_stuck += 1
        if "FATAL EXCEPTION" in line or "ANR in com.cropdoctor" in line:
            cycle_crashes += 1
            
    # Check if scan took >= 8000ms
    for d in cycle_durations:
        if d >= 8000:
            cycle_stuck += 1

    cycle_info = {
        "cycle": cycle_idx,
        "type": "immediate_double",
        "scans_completed_in_cycle": len(cycle_durations),
        "request_ids": cycle_req_ids,
        "durations_ms": cycle_durations,
        "stuck": cycle_stuck > 0,
        "crashes": cycle_crashes > 0,
        "cycle_elapsed_s": round(time.time() - cycle_start, 2)
    }
    
    stage_data["immediate_cycles"].append(cycle_info)
    stage_data["all_durations_ms"].extend(cycle_durations)
    stage_data["stuck_count"] += cycle_stuck
    stage_data["crashes_anrs"] += cycle_crashes
    
    # Update running aggregates
    if stage_data["all_durations_ms"]:
        stage_data["min_spinner_ms"] = min(stage_data["all_durations_ms"])
        stage_data["max_spinner_ms"] = max(stage_data["all_durations_ms"])
        stage_data["avg_spinner_ms"] = round(sum(stage_data["all_durations_ms"]) / len(stage_data["all_durations_ms"]), 1)
        
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(stage_data, f, indent=2)
        
    print(f"Cycle {cycle_idx:02d}/20: Scans={len(cycle_durations)} Durations={cycle_durations}ms Stuck={cycle_stuck > 0}")

# -------------------------------------------------------------
# Part 2: 20 Spaced Scans (2s gap)
# -------------------------------------------------------------
print("\n--- Part A2: 20 Spaced Scans (2s gap) ---")
for scan_idx in range(1, 21):
    scan_start = time.time()
    
    time.sleep(2.0) # 2.0 s gap
    
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "false",
             "--es", "language", "en"])
             
    time.sleep(2.0)
    
    logs = run_adb(["logcat", "-d"])
    run_adb(["logcat", "-c"])
    
    scan_durations = []
    scan_req_ids = []
    scan_stuck = 0
    scan_crashes = 0
    
    for line in logs.splitlines():
        if "CROPDOC_LLAMA" in line and "SCAN_END:" in line:
            m_dur = re.search(r"durationMs=(\d+)ms", line)
            m_req = re.search(r"requestId=(\d+)", line)
            if m_dur:
                scan_durations.append(int(m_dur.group(1)))
            if m_req:
                scan_req_ids.append(int(m_req.group(1)))
        if "WATCHDOG_8S_TRIGGERED" in line:
            scan_stuck += 1
        if "FATAL EXCEPTION" in line or "ANR in com.cropdoctor" in line:
            scan_crashes += 1
            
    for d in scan_durations:
        if d >= 8000:
            scan_stuck += 1
            
    scan_info = {
        "scan_index": scan_idx,
        "type": "spaced_2s",
        "scans_completed": len(scan_durations),
        "request_ids": scan_req_ids,
        "durations_ms": scan_durations,
        "stuck": scan_stuck > 0,
        "crashes": scan_crashes > 0,
        "elapsed_s": round(time.time() - scan_start, 2)
    }
    
    stage_data["spaced_scans"].append(scan_info)
    stage_data["all_durations_ms"].extend(scan_durations)
    stage_data["stuck_count"] += scan_stuck
    stage_data["crashes_anrs"] += scan_crashes
    
    if stage_data["all_durations_ms"]:
        stage_data["min_spinner_ms"] = min(stage_data["all_durations_ms"])
        stage_data["max_spinner_ms"] = max(stage_data["all_durations_ms"])
        stage_data["avg_spinner_ms"] = round(sum(stage_data["all_durations_ms"]) / len(stage_data["all_durations_ms"]), 1)

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(stage_data, f, indent=2)
        
    print(f"Spaced Scan {scan_idx:02d}/20: Durations={scan_durations}ms Stuck={scan_stuck > 0}")

total_elapsed = round(time.time() - start_wall_time, 2)
stage_data["elapsed_seconds"] = total_elapsed
stage_data["status"] = "PASS" if (stage_data["stuck_count"] == 0 and stage_data["crashes_anrs"] == 0) else "FAIL"

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(stage_data, f, indent=2)

print("\n" + "=" * 70)
print(f"STAGE A SUMMARY: Status={stage_data['status']}")
print(f"Total Cycles: {len(stage_data['immediate_cycles'])} immediate + {len(stage_data['spaced_scans'])} spaced")
print(f"Stuck Scans (>8s): {stage_data['stuck_count']}")
print(f"Crashes / ANRs: {stage_data['crashes_anrs']}")
print(f"Min Spinner: {stage_data['min_spinner_ms']} ms")
print(f"Max Spinner (Longest): {stage_data['max_spinner_ms']} ms")
print(f"Avg Spinner: {stage_data['avg_spinner_ms']} ms")
print(f"Total Elapsed Time: {total_elapsed} s")
print(f"Saved: {OUT_PATH}")
print("=" * 70)
