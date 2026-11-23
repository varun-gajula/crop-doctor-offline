import subprocess
import time
import os
import re
import json
import sys

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

OUT_PATH = os.path.join("benchmarks", "llama_complete_stageA.json")
os.makedirs("benchmarks", exist_ok=True)

def run_adb(args, timeout=30):
    cmd = [ADB] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

print("=" * 70)
print("Crop Doctor - Task LLAMA-COMPLETE: Stage A (20 Double Scans / 40 Scans)")
print("Target Device: POCO M4 5G (22041219PI)")
print("=" * 70)

# Clear logcat buffer
run_adb(["logcat", "-G", "16M"])
run_adb(["logcat", "-c"])

img_path = "/data/local/tmp/case5_tomato.jpg"
crop_hint = "tomato"

stage_data = {
    "stage": "A",
    "description": "20 cycles of immediate double scans (40 scans total)",
    "device": "POCO M4 5G (22041219PI)",
    "start_time": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "cycles": [],
    "total_cycles": 20,
    "total_scans_requested": 40,
    "completed_scans": 0,
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
stuck_detected = False

for cycle_idx in range(1, 21):
    cycle_start = time.time()
    print(f"\n--- Cycle {cycle_idx}/20: Firing double scan (2 scans immediately) ---")
    
    # Run scan 1
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])
             
    # Immediate second scan while first is in flight
    time.sleep(0.12)
    
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])

    # Wait for the second scan to settle
    time.sleep(2.5)
    
    # Read logcat since last check
    logs = run_adb(["logcat", "-d"])
    
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
            
    # Check if scan took > 8000ms
    for d in cycle_durations:
        if d >= 8000:
            cycle_stuck += 1

    cycle_info = {
        "cycle": cycle_idx,
        "scans_completed_in_cycle": len(cycle_durations),
        "request_ids": cycle_req_ids,
        "durations_ms": cycle_durations,
        "stuck": cycle_stuck > 0,
        "crashes": cycle_crashes > 0,
        "cycle_elapsed_s": round(time.time() - cycle_start, 2)
    }
    
    stage_data["cycles"].append(cycle_info)
    stage_data["all_durations_ms"].extend(cycle_durations)
    stage_data["completed_scans"] += len(cycle_durations)
    stage_data["stuck_count"] += cycle_stuck
    stage_data["crashes_anrs"] += cycle_crashes
    stage_data["elapsed_seconds"] = round(time.time() - start_wall_time, 2)
    
    if stage_data["all_durations_ms"]:
        durs = stage_data["all_durations_ms"]
        stage_data["min_spinner_ms"] = min(durs)
        stage_data["max_spinner_ms"] = max(durs)
        stage_data["avg_spinner_ms"] = round(sum(durs) / len(durs), 1)

    # Save results to benchmarks/llama_complete_stageA.json after every cycle
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(stage_data, f, indent=2)

    print(f"  Cycle {cycle_idx} result: durations={cycle_durations}ms, stuck={cycle_stuck}, crashes={cycle_crashes}")
    
    # Clear logcat for clean cycle-by-cycle parsing
    run_adb(["logcat", "-c"])

    if cycle_stuck > 0:
        print(f"[FATAL] Scan stuck in cycle {cycle_idx}! Stopping as required.")
        stuck_detected = True
        stage_data["status"] = "STUCK_FAILURE"
        with open(OUT_PATH, "w", encoding="utf-8") as f:
            json.dump(stage_data, f, indent=2)
        break

total_time = round(time.time() - start_wall_time, 2)
stage_data["elapsed_seconds"] = total_time
if not stuck_detected and stage_data["crashes_anrs"] == 0:
    stage_data["status"] = "PASS"
else:
    stage_data["status"] = "FAIL"

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(stage_data, f, indent=2)

print("\n" + "=" * 70)
print(f"STAGE A SUMMARY: Status={stage_data['status']}")
print(f"Total Cycles: {len(stage_data['cycles'])}/20")
print(f"Completed Scans: {stage_data['completed_scans']}")
print(f"Stuck Count: {stage_data['stuck_count']}")
print(f"Crashes / ANRs: {stage_data['crashes_anrs']}")
if stage_data["all_durations_ms"]:
    print(f"Spinner Durations: min={stage_data['min_spinner_ms']}ms, max={stage_data['max_spinner_ms']}ms (longest), avg={stage_data['avg_spinner_ms']}ms")
print(f"Time Taken: {total_time}s")
print(f"Saved: {OUT_PATH}")
print("=" * 70)
