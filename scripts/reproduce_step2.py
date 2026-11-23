import subprocess
import time
import os
import sys

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

def run_adb(args):
    cmd = [ADB] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.stdout.strip()

print("Using ADB:", ADB)
print("Device state:", run_adb(["get-state"]))

# Ensure benchmark directory exists
os.makedirs("benchmarks", exist_ok=True)
log_file_path = os.path.join("benchmarks", "fix_llama_logs.txt")

with open(log_file_path, "w", encoding="utf-8") as out_f:
    out_f.write("=== STEP 2 REPRODUCTION LOGS ON POCO M4 5G (22041219PI) ===\n\n")

# Clear logcat first
run_adb(["logcat", "-c"])

def log_and_record(section_title, duration_sec=5):
    print(f"\n--- {section_title} ---")
    with open(log_file_path, "a", encoding="utf-8") as out_f:
        out_f.write(f"\n========================================\n{section_title}\n========================================\n")
    time.sleep(duration_sec)
    # Dump logcat
    logs = run_adb(["logcat", "-d", "-s", "CROPDOC_LLAMA", "CROPDOC_DECISION", "MainActivity", "MainActivityBenchmark"])
    with open(log_file_path, "a", encoding="utf-8") as out_f:
        out_f.write(logs + "\n")
    run_adb(["logcat", "-c"])

# SCENARIO 1: Scan a leaf that gives DIAGNOSE, then scan again right away; repeat 10 times.
print("\nStarting Scenario 1: 10 cycles of (scan DIAGNOSE -> scan again right away)")
for cycle in range(1, 11):
    print(f"Cycle {cycle}/10: scanning DIAGNOSE then immediate second scan...")
    # First scan
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", 
             "--es", "benchmark_action", "diagnose_file", 
             "--es", "file_path", "/data/local/tmp/case5_tomato.jpg", 
             "--es", "crop_hint", "tomato", 
             "--ez", "auto_start_advisor", "true"])
    # Wait a very brief moment (200ms) then scan again right away
    time.sleep(0.3)
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", 
             "--es", "benchmark_action", "diagnose_file", 
             "--es", "file_path", "/data/local/tmp/case5_tomato.jpg", 
             "--es", "crop_hint", "tomato", 
             "--ez", "auto_start_advisor", "true"])
    
    # Wait 9 seconds so watchdog (8s) triggers if buffering!
    time.sleep(9.0)
    log_and_record(f"SCENARIO 1 - CYCLE {cycle}/10 (scan immediately after scan)", duration_sec=1)

# SCENARIO 2: Scan a DIAGNOSE, wait until the advice finishes or stalls, then scan again.
print("\nStarting Scenario 2: scan DIAGNOSE, wait for advice finish/stall, then scan again...")
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", 
         "--es", "benchmark_action", "diagnose_file", 
         "--es", "file_path", "/data/local/tmp/case5_tomato.jpg", 
         "--es", "crop_hint", "tomato", 
         "--ez", "auto_start_advisor", "true"])

# Wait for advice to finish or stall (~28s)
print("Waiting 28s for Llama generation to complete/stall...")
time.sleep(28.0)
log_and_record("SCENARIO 2 - PART A (scan DIAGNOSE waited 28s for finish)", duration_sec=1)

# Now scan again
print("Now scanning again after finish/stall...")
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", 
         "--es", "benchmark_action", "diagnose_file", 
         "--es", "file_path", "/data/local/tmp/case5_tomato.jpg", 
         "--es", "crop_hint", "tomato", 
         "--ez", "auto_start_advisor", "true"])
time.sleep(9.0)
log_and_record("SCENARIO 2 - PART B (second scan after finish/stall)", duration_sec=1)

# SCENARIO 3: Scan a DIAGNOSE, press Back, scan again.
print("\nStarting Scenario 3: scan DIAGNOSE, press Back during generation, then scan again...")
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", 
         "--es", "benchmark_action", "diagnose_file", 
         "--es", "file_path", "/data/local/tmp/case5_tomato.jpg", 
         "--es", "crop_hint", "tomato", 
         "--ez", "auto_start_advisor", "true"])
print("Waiting 3s for generation to start...")
time.sleep(3.0)
print("Pressing Back keyevent...")
run_adb(["shell", "input", "keyevent", "4"])
time.sleep(1.0)
print("Scanning again after Back...")
run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", 
         "--es", "benchmark_action", "diagnose_file", 
         "--es", "file_path", "/data/local/tmp/case5_tomato.jpg", 
         "--es", "crop_hint", "tomato", 
         "--ez", "auto_start_advisor", "true"])
time.sleep(9.0)
log_and_record("SCENARIO 3 (scan DIAGNOSE, press Back, scan again)", duration_sec=1)

print("\nReproduction complete! Logs saved to", log_file_path)
