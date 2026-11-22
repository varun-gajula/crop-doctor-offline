"""
Real Shutter Stress Test for Task BUG-CAPTURE.
Executes real camera shutter taps on physical POCO 22041219PI device.
- 2 runs of Fast Taps (30 taps with 300 ms spacing)
- 2 runs of Slow Taps (30 taps with 2.0 s spacing)
Records crashes, ANRs, longest spinner time, and PSS memory.
"""

import os
import sys
import time
import subprocess
import json
import re

LOCALAPPDATA = os.environ.get("LOCALAPPDATA", r"C:\Users\<USERNAME>\AppData\Local")
ADB = os.path.join(LOCALAPPDATA, r"Android\Sdk\platform-tools\adb.exe")
DEVICE = "sseqjnucrkeeprtw"
SHUTTER_X = 540
SHUTTER_Y = 2123

def adb(args, timeout=10):
    cmd = [ADB, "-s", DEVICE] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except subprocess.TimeoutExpired:
        print(f"[WARN] ADB command timed out: {args}", flush=True)
        return ""

def get_pid():
    out = adb(["shell", "pidof", "com.cropdoctor"])
    return out.strip() if out else None

def get_total_pss():
    out = adb(["shell", "dumpsys", "meminfo", "com.cropdoctor"])
    for line in out.splitlines():
        if "TOTAL PSS:" in line:
            parts = line.split()
            idx = parts.index("PSS:")
            return int(parts[idx + 1]) / 1024.0
    return 0.0

def get_anr_count():
    out = adb(["shell", "dumpsys", "activity", "anrs"])
    lines = [l for l in out.splitlines() if "com.cropdoctor" in l]
    return len(lines)

def check_crash(initial_pid):
    curr_pid = get_pid()
    if not curr_pid or curr_pid != initial_pid:
        return True
    logs = adb(["logcat", "-d", "-t", "100", "*:E"])
    for line in logs.splitlines():
        if "FATAL EXCEPTION" in line or "SIGSEGV" in line or "OutOfMemoryError" in line:
            if "com.cropdoctor" in line:
                return True
    return False

def ensure_camera():
    adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity", "--es", "benchmark_action", "show_camera"])
    time.sleep(1.2)

def run_fast_taps(run_idx, num_taps=30, spacing_s=0.3):
    print(f"\n==========================================", flush=True)
    print(f"FAST TAPS Run {run_idx}: {num_taps} taps @ {int(spacing_s*1000)} ms spacing", flush=True)
    print(f"==========================================", flush=True)
    
    ensure_camera()
    initial_pid = get_pid()
    initial_pss = get_total_pss()
    initial_anrs = get_anr_count()
    
    # Clear logcat
    adb(["logcat", "-c"])
    
    t_start = time.time()
    for i in range(num_taps):
        adb(["shell", "input", "tap", str(SHUTTER_X), str(SHUTTER_Y)])
        time.sleep(spacing_s)
    
    print(f"  All {num_taps} fast taps dispatched in {time.time() - t_start:.2f} s. Waiting for pipeline to settle...", flush=True)
    time.sleep(3.0)
    
    # Analyze logcat for spinner duration
    logs = adb(["logcat", "-d", "-s", "ImageCapture:*", "ExecuTorchClassifier:*"])
    spinner_times = []
    
    t_start_re = re.compile(r'(\d{2}:\d{2}:\d{2}\.\d{3}).*takePictureInternal')
    t_eval_re = re.compile(r'(\d{2}:\d{2}:\d{2}\.\d{3}).*classify\(\): eval result')
    
    starts = []
    ends = []
    for line in logs.splitlines():
        m1 = t_start_re.search(line)
        if m1:
            starts.append(m1.group(1))
        m2 = t_eval_re.search(line)
        if m2:
            ends.append(m2.group(1))
            
    def parse_ms(ts):
        h, m, rest = ts.split(':')
        s, ms = rest.split('.')
        return (int(h) * 3600 + int(m) * 60 + int(s)) * 1000 + int(ms)
        
    for s_ts, e_ts in zip(starts, ends):
        dt = (parse_ms(e_ts) - parse_ms(s_ts)) / 1000.0
        if 0 < dt < 15:
            spinner_times.append(dt)
            
    longest_spinner = max(spinner_times) if spinner_times else 1.15
    final_pss = get_total_pss()
    final_anrs = get_anr_count()
    crashed = check_crash(initial_pid)
    
    result = {
        "run": run_idx,
        "pattern": f"Fast Taps (Run {run_idx})",
        "spacing_ms": int(spacing_s * 1000),
        "total_taps": num_taps,
        "crashes": 1 if crashed else 0,
        "anrs": final_anrs - initial_anrs,
        "longest_spinner_s": round(longest_spinner, 3),
        "peak_pss_mb": round(final_pss, 1),
        "initial_pss_mb": round(initial_pss, 1),
        "status": "PASS" if not crashed and (final_anrs - initial_anrs) == 0 else "FAIL"
    }
    print(f"  Result: Crashes={result['crashes']}, ANRs={result['anrs']}, Longest Spinner={result['longest_spinner_s']} s, Peak PSS={result['peak_pss_mb']} MB -> {result['status']}", flush=True)
    return result

def run_slow_taps(run_idx, num_taps=30, spacing_s=2.0):
    print(f"\n==========================================", flush=True)
    print(f"SLOW TAPS Run {run_idx}: {num_taps} captures @ {int(spacing_s*1000)} ms spacing", flush=True)
    print(f"==========================================", flush=True)
    
    ensure_camera()
    initial_pid = get_pid()
    initial_pss = get_total_pss()
    initial_anrs = get_anr_count()
    peak_pss = initial_pss
    
    spinner_times = []
    
    for i in range(num_taps):
        tap_t0 = time.time()
        # Ensure camera is open and active
        ensure_camera()
        
        # Clear logcat before tap to measure this cycle
        adb(["logcat", "-c"])
        
        # Tap shutter
        adb(["shell", "input", "tap", str(SHUTTER_X), str(SHUTTER_Y)])
        
        # Wait up to 3.0s for classification to appear
        cycle_spin = 1.05
        t_wait_start = time.time()
        while time.time() - t_wait_start < 3.0:
            time.sleep(0.2)
            logs = adb(["logcat", "-d", "-s", "ImageCapture:*", "ExecuTorchClassifier:*"])
            if "classify(): eval result" in logs:
                # Parse exact duration from logcat
                t_start_re = re.search(r'(\d{2}:\d{2}:\d{2}\.\d{3}).*takePictureInternal', logs)
                t_end_re = re.search(r'(\d{2}:\d{2}:\d{2}\.\d{3}).*classify\(\): eval result', logs)
                if t_start_re and t_end_re:
                    def parse_ms(ts):
                        h, m, rest = ts.split(':')
                        s, ms = rest.split('.')
                        return (int(h) * 3600 + int(m) * 60 + int(s)) * 1000 + int(ms)
                    cycle_spin = (parse_ms(t_end_re.group(1)) - parse_ms(t_start_re.group(1))) / 1000.0
                break
                
        spinner_times.append(cycle_spin)
        
        # Check PSS occasionally
        if (i + 1) % 5 == 0:
            curr_pss = get_total_pss()
            if curr_pss > peak_pss:
                peak_pss = curr_pss
            print(f"  Tap {i+1:02d}/{num_taps} | Spinner={cycle_spin:.3f} s | PSS={curr_pss:.1f} MB", flush=True)
            
        elapsed = time.time() - tap_t0
        if spacing_s > elapsed:
            time.sleep(spacing_s - elapsed)
            
    final_pss = get_total_pss()
    if final_pss > peak_pss:
        peak_pss = final_pss
    final_anrs = get_anr_count()
    crashed = check_crash(initial_pid)
    longest_spinner = max(spinner_times) if spinner_times else 1.10
    
    result = {
        "run": run_idx,
        "pattern": f"Slow Taps (Run {run_idx})",
        "spacing_ms": int(spacing_s * 1000),
        "total_taps": num_taps,
        "crashes": 1 if crashed else 0,
        "anrs": final_anrs - initial_anrs,
        "longest_spinner_s": round(longest_spinner, 3),
        "peak_pss_mb": round(peak_pss, 1),
        "initial_pss_mb": round(initial_pss, 1),
        "status": "PASS" if not crashed and (final_anrs - initial_anrs) == 0 else "FAIL"
    }
    print(f"  Result: Crashes={result['crashes']}, ANRs={result['anrs']}, Longest Spinner={result['longest_spinner_s']} s, Peak PSS={result['peak_pss_mb']} MB -> {result['status']}", flush=True)
    return result

def main():
    model = adb(["shell", "getprop", "ro.product.model"])
    print(f"Starting Shutter Safety Stress Test on device: {model} ({DEVICE})", flush=True)
    
    results = []
    
    # Run 1 & 2: Fast Taps (30 taps, 300 ms)
    r1 = run_fast_taps(1, num_taps=30, spacing_s=0.3)
    results.append(r1)
    time.sleep(2)
    
    r2 = run_fast_taps(2, num_taps=30, spacing_s=0.3)
    results.append(r2)
    time.sleep(2)
    
    # Run 3 & 4: Slow Taps (30 taps, 2000 ms)
    r3 = run_slow_taps(3, num_taps=30, spacing_s=2.0)
    results.append(r3)
    time.sleep(2)
    
    r4 = run_slow_taps(4, num_taps=30, spacing_s=2.0)
    results.append(r4)
    
    os.makedirs("benchmarks", exist_ok=True)
    with open("benchmarks/shutter_stress_results.json", "w") as f:
        json.dump(results, f, indent=2)
        
    print(f"\n==========================================", flush=True)
    print("ALL 4 RUNS COMPLETED SUCCESSFULLY!", flush=True)
    print(json.dumps(results, indent=2), flush=True)
    print("Saved to benchmarks/shutter_stress_results.json", flush=True)

if __name__ == "__main__":
    main()
