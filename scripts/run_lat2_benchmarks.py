import subprocess
import time
import os
import re
import json
import sys

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

OUT_PATH = os.path.join("benchmarks", "lat2_results.json")
os.makedirs("benchmarks", exist_ok=True)

def get_connected_device():
    res = subprocess.run([ADB, "devices"], capture_output=True, text=True, errors="replace")
    for line in res.stdout.strip().splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            return parts[0]
    return None

def run_adb(device, args, timeout=90):
    cmd = [ADB, "-s", device] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

def main():
    device = get_connected_device()
    if not device:
        print("ERROR: No connected Android device found via ADB.")
        print("Please connect the POCO phone via USB and enable USB debugging.")
        sys.exit(1)
        
    print(f"Connected Device: {device}")
    
    # Install updated APK
    apk_path = os.path.join("app", "build", "outputs", "apk", "debug", "app-debug.apk")
    print(f"Installing {apk_path}...")
    subprocess.run([ADB, "-s", device, "install", "-r", "-d", apk_path], check=True)
    
    sample_classes = [
        "tomato_septoria_leaf_spot",
        "chilli_leaf_curl_virus",
        "rice_leaf_blast",
        "cotton_bacterial_blight",
        "maize_northern_leaf_blight"
    ]
    
    # 1. Thread count experiment: 2, 4, 6, 8 threads across 5 runs
    thread_results = {}
    for threads in [2, 4, 6, 8]:
        print(f"\n==========================================")
        print(f"Testing ExecuTorch Thread Count = {threads}")
        print(f"==========================================")
        thread_results[threads] = []
        for run_idx, cid in enumerate(sample_classes):
            print(f"Run {run_idx+1}/5: {cid} (threads={threads})...")
            run_adb(device, ["logcat", "-G", "16M"])
            run_adb(device, ["logcat", "-c"])
            
            run_adb(device, ["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
                             "--es", "benchmark_action", "test_advice",
                             "--es", "class_id", cid,
                             "--es", "language", "en",
                             "--ei", "threads", str(threads)])
            
            # Poll for completion
            poll_start = time.time()
            ttft_ms = 0
            tok_per_sec = 0.0
            tokens = 0
            while time.time() - poll_start < 60:
                time.sleep(1.5)
                logs = run_adb(device, ["logcat", "-d"])
                match = re.search(r"ADVISOR_COMPLETE:.*?tokens=(\d+).*?ttft=(\d+)ms.*?tok_per_sec=([\d\.]+)", logs)
                if match:
                    tokens = int(match.group(1))
                    ttft_ms = int(match.group(2))
                    tok_per_sec = float(match.group(3))
                    break
                    
            print(f"  -> TTFT: {ttft_ms} ms, Speed: {tok_per_sec} tok/s, Tokens: {tokens}")
            thread_results[threads].append({
                "class_id": cid,
                "ttft_ms": ttft_ms,
                "tok_per_sec": tok_per_sec,
                "tokens": tokens
            })
            
    # Save results
    with open(OUT_PATH, "w") as f:
        json.dump(thread_results, f, indent=2)
    print(f"\nAll thread benchmarks saved to {OUT_PATH}")

if __name__ == "__main__":
    main()
