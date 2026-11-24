import subprocess
import time
import os
import re
import json
import sys

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

OUT_PATH = os.path.join("benchmarks", "lat1_after_stageB.json")
os.makedirs("benchmarks", exist_ok=True)

DEVICE_SERIAL = "sseqjnucrkeeprtw"

def run_adb(args, timeout=90):
    cmd = [ADB, "-s", DEVICE_SERIAL] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

print("=" * 70)
print("Crop Doctor - Task LAT-1: After Optimization (10 Advice Generations)")
print("5 English, 5 Hindi on POCO M4 5G (22041219PI)")
print("=" * 70)

test_runs = [
    {"run": 1, "lang": "en", "class_id": "tomato_septoria_leaf_spot", "crop": "tomato"},
    {"run": 2, "lang": "en", "class_id": "chilli_leaf_curl_virus", "crop": "chilli"},
    {"run": 3, "lang": "en", "class_id": "rice_leaf_blast", "crop": "rice"},
    {"run": 4, "lang": "en", "class_id": "cotton_bacterial_blight", "crop": "cotton"},
    {"run": 5, "lang": "en", "class_id": "maize_northern_leaf_blight", "crop": "maize"},
    {"run": 6, "lang": "hi", "class_id": "tomato_septoria_leaf_spot", "crop": "tomato"},
    {"run": 7, "lang": "hi", "class_id": "chilli_leaf_curl_virus", "crop": "chilli"},
    {"run": 8, "lang": "hi", "class_id": "rice_leaf_blast", "crop": "rice"},
    {"run": 9, "lang": "hi", "class_id": "cotton_bacterial_blight", "crop": "cotton"},
    {"run": 10, "lang": "hi", "class_id": "maize_northern_leaf_blight", "crop": "maize"},
]

stage_data = {
    "stage": "B_AFTER_LAT1",
    "description": "Task LAT-1: 10 advice generations with shortened prompt (5 English, 5 Hindi)",
    "device": "POCO M4 5G (22041219PI)",
    "start_time": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "runs": [],
    "total_runs": len(test_runs),
    "completed_full_sentence_count": 0,
    "crashes_anrs": 0,
    "all_ttft_ms": [],
    "all_tok_per_sec": [],
    "all_tokens": [],
    "all_duration_ms": [],
    "status": "IN_PROGRESS"
}

start_wall_time = time.time()

for item in test_runs:
    run_idx = item["run"]
    lang = item["lang"]
    class_id = item["class_id"]
    crop = item["crop"]
    
    print(f"\n--- Run {run_idx}/10: Lang={lang.upper()} Class={class_id} ---")
    run_adb(["logcat", "-G", "16M"])
    run_adb(["logcat", "-c"])
    
    # Launch test_advice
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "test_advice",
             "--es", "class_id", class_id,
             "--es", "language", lang])
             
    # Wait for generation to complete
    gen_finished = False
    tokens_produced = 0
    ttft_ms = 0
    tok_per_sec = 0.0
    duration_ms = 0
    end_reason = "UNKNOWN"
    prompt_tokens = 0
    max_tokens = 0
    seq_limit = 0
    advisor_source = "UNKNOWN"
    crashes = 0
    
    poll_start = time.time()
    while time.time() - poll_start < 70:
        time.sleep(1.5)
        logs = run_adb(["logcat", "-d"])
        
        if "FATAL EXCEPTION" in logs or "ANR in com.cropdoctor" in logs:
            crashes += 1
            end_reason = "CRASH"
            break
            
        if "CROPDOC_LLAMA: GEN_METRICS:" in logs:
            for line in logs.splitlines():
                if "CROPDOC_LLAMA: GEN_METRICS:" in line:
                    m_p = re.search(r"promptTokens=(\d+)", line)
                    m_m = re.search(r"maxNewTokens=(\d+)", line)
                    m_s = re.search(r"totalSeqLimit=(\d+)", line)
                    m_t = re.search(r"tokensProduced=(\d+)", line)
                    m_f = re.search(r"ttftMs=(\d+)", line)
                    m_r = re.search(r"tokPerSec=([\d\.]+)", line)
                    m_e = re.search(r"endReason='([^']+)'", line)
                    if m_p: prompt_tokens = int(m_p.group(1))
                    if m_m: max_tokens = int(m_m.group(1))
                    if m_s: seq_limit = int(m_s.group(1))
                    if m_t: tokens_produced = int(m_t.group(1))
                    if m_f: ttft_ms = int(m_f.group(1))
                    if m_r: tok_per_sec = float(m_r.group(1))
                    if m_e: end_reason = m_e.group(1)
                    
        if "CROPDOC_LLAMA: GEN_END:" in logs:
            for line in logs.splitlines():
                if "CROPDOC_LLAMA: GEN_END:" in line:
                    gen_finished = True
                    m_src = re.search(r"source=([A-Z_]+)", line)
                    m_dur = re.search(r"totalDurationMs=(\d+)", line)
                    if m_src: advisor_source = m_src.group(1)
                    if m_dur: duration_ms = int(m_dur.group(1))
            break
            
        if "CROPDOC_LLAMA: ADVISORY_COMPLETE" in logs or "CROPDOC_LLAMA: GENERATION_RESULT:" in logs:
            gen_finished = True
            break
            
    # Check text for sentence completion
    sentence_complete = True
    full_text = ""
    for line in logs.splitlines():
        if "CROPDOC_LLAMA: GENERATION_RESULT:" in line:
            full_text = line.split("CROPDOC_LLAMA: GENERATION_RESULT:", 1)[1].strip()
            
    if full_text:
        trimmed = full_text.rstrip()
        sentence_complete = trimmed.endswith((".", "!", "?", "।", "|"))
        
    if advisor_source == "UNKNOWN" and "OFFLINE_GUIDE" in logs:
        advisor_source = "OFFLINE_GUIDE"
        end_reason = "OFFLINE_GUIDE_FALLBACK"
    elif advisor_source == "UNKNOWN" and tokens_produced > 0:
        advisor_source = "LLAMA"
        
    print(f"Result Run {run_idx}: Source={advisor_source}, PromptTok={prompt_tokens}, Produced={tokens_produced}, TTFT={ttft_ms}ms, Speed={tok_per_sec} tok/s, EndReason='{end_reason}', SentenceComplete={sentence_complete}")
    
    run_entry = {
        "run": run_idx,
        "language": lang,
        "class_id": class_id,
        "crop": crop,
        "completed_on_full_sentence": sentence_complete,
        "prompt_tokens": prompt_tokens,
        "max_new_tokens": max_tokens,
        "total_seq_limit": seq_limit,
        "tokens_produced": tokens_produced,
        "ttft_ms": ttft_ms,
        "tokens_per_second": tok_per_sec,
        "total_duration_ms": duration_ms,
        "end_reason": end_reason,
        "advisor_source": advisor_source,
        "crashes": crashes > 0
    }
    stage_data["runs"].append(run_entry)
    
    if sentence_complete:
        stage_data["completed_full_sentence_count"] += 1
    if crashes > 0:
        stage_data["crashes_anrs"] += crashes
    if ttft_ms > 0:
        stage_data["all_ttft_ms"].append(ttft_ms)
    if tok_per_sec > 0:
        stage_data["all_tok_per_sec"].append(tok_per_sec)
    if tokens_produced > 0:
        stage_data["all_tokens"].append(tokens_produced)
    if duration_ms > 0:
        stage_data["all_duration_ms"].append(duration_ms)
        
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(stage_data, f, indent=2)
        
    time.sleep(2)

total_elapsed = round(time.time() - start_wall_time, 2)
stage_data["status"] = "PASS" if stage_data["crashes_anrs"] == 0 and stage_data["completed_full_sentence_count"] == 10 else "FAIL"
stage_data["total_elapsed_s"] = total_elapsed

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(stage_data, f, indent=2)

print("\n" + "=" * 70)
print(f"Task LAT-1 After Stage B Finished in {total_elapsed} s. Status: {stage_data['status']}")
print(f"Saved to {OUT_PATH}")
print("=" * 70)
