import os
import sys
import time
import subprocess
import re
import json
from pathlib import Path

ADB = r"C:\Users\ec026\AppData\Local\Android\Sdk\platform-tools\adb.exe"

def get_device():
    res = subprocess.run([ADB, "devices"], stdout=subprocess.PIPE, text=True, encoding="utf-8", errors="replace")
    devices = []
    for line in res.stdout.splitlines()[1:]:
        line = line.strip()
        if line and "\tdevice" in line:
            devices.append(line.split("\t")[0])
    return devices[0] if devices else None

def run_adb(device, cmd):
    full_cmd = [ADB, "-s", device] + cmd
    res = subprocess.run(full_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8", errors="replace")
    return res.stdout.strip()

def clear_logcat(device):
    run_adb(device, ["logcat", "-c"])

def get_cropdoc_log(device):
    out = run_adb(device, ["logcat", "-d", "-s", "CROPDOC_DECISION"])
    lines = [l for l in out.splitlines() if "CROPDOC_DECISION:" in l]
    return lines[-1] if lines else None

def parse_cropdoc_line(line):
    # Example:
    # CROPDOC_DECISION: source=camera orig=3000x4000 exif=90 config=ARGB_8888 colorSpace=sRGB downscale=512x512 | gate1_leafValidator=PASS(frac=0.852) gate2_energy=OFF(energy=-5.1234 vs null) gate3_confidence=PASS(class=rice_leaf_blast, prob=0.9250 vs thresh=0.4018) gate4_weeds=PASS | decision=ACCEPTED code=ACCEPTED
    data = {"raw_log": line}
    
    m_src = re.search(r"source=(\w+)", line)
    data["source"] = m_src.group(1) if m_src else "unknown"
    
    m_orig = re.search(r"orig=(\d+x\d+)", line)
    data["orig_size"] = m_orig.group(1) if m_orig else "unknown"
    
    m_exif = re.search(r"exif=(\d+)", line)
    data["exif"] = int(m_exif.group(1)) if m_exif else 0
    
    m_cfg = re.search(r"config=(\w+)", line)
    data["config"] = m_cfg.group(1) if m_cfg else "unknown"
    
    m_cs = re.search(r"colorSpace=([^\s|]+)", line)
    data["color_space"] = m_cs.group(1) if m_cs else "unknown"
    
    m_g1 = re.search(r"gate1_leafValidator=([^(]+)\(frac=([0-9.]+)(?:,\s*reason=([^)]+))?\)", line)
    if m_g1:
        data["gate1_status"] = m_g1.group(1)
        data["gate1_frac"] = float(m_g1.group(2))
        data["gate1_reason"] = m_g1.group(3) or "OK"
    else:
        data["gate1_status"] = "UNKNOWN"
        data["gate1_frac"] = 0.0
        data["gate1_reason"] = ""
        
    m_g2 = re.search(r"gate2_energy=(\w+)\(energy=([-\d.]+)\s*vs\s*([^)]+)\)", line)
    if m_g2:
        data["gate2_status"] = m_g2.group(1)
        data["gate2_energy"] = float(m_g2.group(2))
        data["gate2_thresh"] = m_g2.group(3)
    else:
        data["gate2_status"] = "OFF"
        data["gate2_energy"] = 0.0
        data["gate2_thresh"] = "null"
        
    m_g3 = re.search(r"gate3_confidence=(\w+)\(class=([^,]+),\s*prob=([\d.]+)\s*vs\s*thresh=([\d.]+)\)", line)
    if m_g3:
        data["gate3_status"] = m_g3.group(1)
        data["gate3_class"] = m_g3.group(2)
        data["gate3_prob"] = float(m_g3.group(3))
        data["gate3_thresh"] = float(m_g3.group(4))
    else:
        data["gate3_status"] = "UNKNOWN"
        data["gate3_class"] = "none"
        data["gate3_prob"] = 0.0
        data["gate3_thresh"] = 0.4018
        
    m_g4 = re.search(r"gate4_weeds=([^\s|]+)", line)
    data["gate4_status"] = m_g4.group(1) if m_g4 else "UNKNOWN"
    
    m_dec = re.search(r"decision=(\w+)\s+code=(\w+)", line)
    if m_dec:
        data["decision"] = m_dec.group(1)
        data["reason_code"] = m_dec.group(2)
    else:
        data["decision"] = "UNKNOWN"
        data["reason_code"] = "UNKNOWN"
        
    return data

def run_set(device, set_name, remote_folder, image_source):
    print(f"\n==========================================")
    print(f"Running set: {set_name} (source={image_source})")
    print(f"==========================================")
    
    remote_base = f"/sdcard/Android/data/com.cropdoctor/files/realtest/{remote_folder}"
    files_out = run_adb(device, ["shell", f"ls '{remote_base}'"])
    remote_files = [f.strip() for f in files_out.splitlines() if f.strip().endswith(".jpg")]
    print(f"Found {len(remote_files)} files in {remote_base}/")
    
    results = []
    for idx, fname in enumerate(remote_files[:20]):
        remote_path = f"{remote_base}/{fname}"
        clear_logcat(device)
        
        # Launch diagnose_file intent with quoted path
        run_adb(device, [
            "shell", "am", "start",
            "-n", "com.cropdoctor/.MainActivity",
            "--es", "benchmark_action", "diagnose_file",
            "--es", "file_path", f"'{remote_path}'",
            "--es", "image_source", image_source,
            "--es", "crop_hint", "auto"
        ])
        
        # Poll for CROPDOC_DECISION log line
        log_line = None
        for _ in range(15):
            time.sleep(0.3)
            log_line = get_cropdoc_log(device)
            if log_line:
                break
                
        if log_line:
            parsed = parse_cropdoc_line(log_line)
            parsed["file_name"] = fname
            parsed["set_name"] = set_name
            results.append(parsed)
            print(f"[{idx+1:02d}/20] {fname}: {parsed['decision']} ({parsed['reason_code']}) - Prob: {parsed['gate3_prob']:.4f}, Energy: {parsed['gate2_energy']:.4f}")
        else:
            print(f"[{idx+1:02d}/20] {fname}: TIMEOUT / NO LOG")
            results.append({
                "file_name": fname,
                "set_name": set_name,
                "decision": "TIMEOUT",
                "reason_code": "TIMEOUT",
                "gate1_status": "TIMEOUT",
                "gate2_energy": 0.0,
                "gate3_prob": 0.0
            })
            
    return results

def main():
    device = get_device()
    if not device:
        print("ERROR: No connected Android device found via ADB!")
        sys.exit(1)
    print(f"Using connected device: {device}")
    
    all_results = {}
    
    # 1. Screen Photos (20 photos of laptop screen showing leaf images)
    all_results["screen"] = run_set(device, "Screen Photos (Laptop Screen)", "screen", "camera")
    
    # 2. Gallery Images (20 gallery photos)
    all_results["gallery"] = run_set(device, "Gallery Images", "gallery", "gallery")
    
    # 3. Camera Real Leaves (20 camera photos of real leaves)
    all_results["camera_real"] = run_set(device, "Camera Photos (Real Leaves)", "real_leaves", "camera")
    
    output_path = Path("benchmarks/p1_benchmark_raw.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(all_results, f, indent=2)
    print(f"\nAll benchmark results saved to {output_path}")

if __name__ == "__main__":
    main()
