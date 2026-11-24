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
        sys.exit(1)
        
    print(f"Connected Device: {device}")
    run_adb(device, ["shell", "input", "keyevent", "224"])
    
    sample_classes = [
        ("tomato_septoria_leaf_spot", "tomato"),
        ("chilli_leaf_curl_virus", "chilli"),
        ("rice_leaf_blast", "rice"),
        ("cotton_bacterial_blight", "cotton"),
        ("maize_northern_leaf_blight", "maize")
    ]
    
    all_results = {
        "device": "POCO M4 5G (sseqjnucrkeeprtw)",
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "thread_benchmarks": {},
        "quality_150t_runs": []
    }
    
    # PART 1: Thread count benchmarks (2, 4, 6, 8 threads)
    for threads in [2, 4, 6, 8]:
        print(f"\n==========================================")
        print(f"Testing ExecuTorch Thread Count = {threads}")
        print(f"==========================================")
        all_results["thread_benchmarks"][threads] = []
        for run_idx, (cid, crop) in enumerate(sample_classes):
            print(f"Run {run_idx+1}/5: {cid} (threads={threads})...")
            run_adb(device, ["logcat", "-G", "16M"])
            run_adb(device, ["logcat", "-c"])
            
            run_adb(device, ["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
                             "--es", "benchmark_action", "test_advice",
                             "--es", "class_id", cid,
                             "--es", "language", "en",
                             "--ei", "threads", str(threads)])
            
            poll_start = time.time()
            ttft_ms = 0
            tok_per_sec = 0.0
            tokens = 0
            active_threads = threads
            prompt_toks = 0
            
            while time.time() - poll_start < 70:
                time.sleep(1.5)
                logs = run_adb(device, ["logcat", "-d"])
                match = re.search(r"ADVISOR_COMPLETE:.*?tokens=(\d+).*?ttft=(\d+)ms.*?tok_per_sec=([\d\.]+)", logs)
                if match:
                    tokens = int(match.group(1))
                    ttft_ms = int(match.group(2))
                    tok_per_sec = float(match.group(3))
                    
                    m_th = re.search(r"threads=(\d+)", logs)
                    if m_th:
                        active_threads = int(m_th.group(1))
                    m_pt = re.search(r"promptTokens=(\d+)", logs)
                    if m_pt:
                        prompt_toks = int(m_pt.group(1))
                    break
                    
            print(f"  -> TTFT: {ttft_ms} ms, Speed: {tok_per_sec} tok/s, Tokens: {tokens}, ActiveThreads: {active_threads}")
            all_results["thread_benchmarks"][threads].append({
                "class_id": cid,
                "crop": crop,
                "ttft_ms": ttft_ms,
                "tok_per_sec": tok_per_sec,
                "tokens": tokens,
                "active_threads": active_threads,
                "prompt_tokens": prompt_toks
            })
            
            # Save intermediate
            with open(OUT_PATH, "w") as f:
                json.dump(all_results, f, indent=2)

    # PART 2: 5 Sample cases in English & Hindi with the ~150-token prompt
    # Using best thread setting (or 4 threads as baseline)
    print("\n==========================================")
    print("Answer Quality & Latency: 5 English + 5 Hindi Cases")
    print("==========================================")
    for lang in ["en", "hi"]:
        for run_idx, (cid, crop) in enumerate(sample_classes):
            print(f"Quality Run ({lang.upper()}): {cid}...")
            run_adb(device, ["logcat", "-G", "16M"])
            run_adb(device, ["logcat", "-c"])
            
            run_adb(device, ["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
                             "--es", "benchmark_action", "test_advice",
                             "--es", "class_id", cid,
                             "--es", "language", lang,
                             "--ei", "threads", "4"])
            
            poll_start = time.time()
            ttft_ms = 0
            tok_per_sec = 0.0
            tokens = 0
            end_reason = "UNKNOWN"
            generated_text = ""
            prompt_toks = 0
            
            while time.time() - poll_start < 70:
                time.sleep(1.5)
                logs = run_adb(device, ["logcat", "-d"])
                match = re.search(r"ADVISOR_COMPLETE:.*?tokens=(\d+).*?ttft=(\d+)ms.*?tok_per_sec=([\d\.]+)", logs)
                if match:
                    tokens = int(match.group(1))
                    ttft_ms = int(match.group(2))
                    tok_per_sec = float(match.group(3))
                    
                    m_pt = re.search(r"promptTokens=(\d+)", logs)
                    if m_pt:
                        prompt_toks = int(m_pt.group(1))
                    m_er = re.search(r"endReason='(.*?)'", logs)
                    if m_er:
                        end_reason = m_er.group(1)
                    break
                    
            print(f"  -> [{lang.upper()}] TTFT: {ttft_ms} ms, Speed: {tok_per_sec} tok/s, End: {end_reason}")
            all_results["quality_150t_runs"].append({
                "class_id": cid,
                "crop": crop,
                "language": lang,
                "ttft_ms": ttft_ms,
                "tok_per_sec": tok_per_sec,
                "tokens": tokens,
                "end_reason": end_reason,
                "prompt_tokens": prompt_toks
            })
            
            with open(OUT_PATH, "w") as f:
                json.dump(all_results, f, indent=2)

    print(f"\nCompleted! All results saved to {OUT_PATH}")

if __name__ == "__main__":
    main()
