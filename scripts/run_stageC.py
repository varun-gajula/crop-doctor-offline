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
OUT_PATH = os.path.join("benchmarks", "llama_complete_stageC.json")
os.makedirs("benchmarks", exist_ok=True)

def run_adb(args, timeout=60):
    cmd = [ADB, "-s", DEVICE_SERIAL] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}", file=sys.stderr)
        return ""

print("=" * 70)
print("Crop Doctor - Task LLAMA-COMPLETE: Stage C (10 Chat Questions)")
print("5 English, 5 Hindi on POCO M4 5G (22041219PI)")
print("=" * 70)

chat_cases = [
    {"run": 1, "lang": "en", "query": "What is the natural remedy?", "class_id": "tomato_septoria_leaf_spot", "crop": "tomato"},
    {"run": 2, "lang": "en", "query": "How much fertilizer should I apply?", "class_id": "chilli_leaf_curl_virus", "crop": "chilli"},
    {"run": 3, "lang": "en", "query": "Is this disease contagious to other plants?", "class_id": "rice_leaf_blast", "crop": "rice"},
    {"run": 4, "lang": "en", "query": "What chemical spray is recommended?", "class_id": "cotton_bacterial_blight", "crop": "cotton"},
    {"run": 5, "lang": "en", "query": "How to prevent this next season?", "class_id": "maize_northern_leaf_blight", "crop": "maize"},
    {"run": 6, "lang": "hi", "query": "प्राकृतिक उपचार क्या है?", "class_id": "tomato_septoria_leaf_spot", "crop": "tomato"},
    {"run": 7, "lang": "hi", "query": "खाद कितनी मात्रा में डालें?", "class_id": "chilli_leaf_curl_virus", "crop": "chilli"},
    {"run": 8, "lang": "hi", "query": "क्या यह रोग फैलता है?", "class_id": "rice_leaf_blast", "crop": "rice"},
    {"run": 9, "lang": "hi", "query": "कौन सा कीटनाशक या फफूंदनाशक छिड़कें?", "class_id": "cotton_bacterial_blight", "crop": "cotton"},
    {"run": 10, "lang": "hi", "query": "इसकी रोकथाम कैसे करें?", "class_id": "maize_northern_leaf_blight", "crop": "maize"},
]

stage_data = {
    "stage": "C",
    "description": "10 chat questions (5 English, 5 Hindi)",
    "device": "POCO M4 5G (22041219PI)",
    "start_time": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "questions": [],
    "total_questions": len(chat_cases),
    "llama_answers_count": 0,
    "offline_guide_count": 0,
    "fallback_reasons": {},
    "crashes_anrs": 0,
    "all_durations_ms": [],
    "status": "IN_PROGRESS"
}

start_wall_time = time.time()

for item in chat_cases:
    run_idx = item["run"]
    lang = item["lang"]
    query = item["query"]
    class_id = item["class_id"]
    crop = item["crop"]
    
    print(f"\n--- Question {run_idx}/10: Lang={lang.upper()} Class={class_id} ---")
    print(f"    Query: '{query}'")
    
    run_adb(["logcat", "-G", "16M"])
    run_adb(["logcat", "-c"])
    
    # Launch test_chat with index and parameters
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "test_chat",
             "--ei", "chat_index", str(run_idx),
             "--es", "class_id", class_id,
             "--es", "crop", crop,
             "--es", "language", lang])
             
    chat_finished = False
    source = "UNKNOWN"
    label = "UNKNOWN"
    duration_ms = 0
    response_text = ""
    fallback_reason = "NONE"
    prompt_tokens = 0
    ttft_ms = 0
    crashes = 0
    pss_mb = 0
    
    poll_start = time.time()
    while time.time() - poll_start < 45:
        time.sleep(1.0)
        logs = run_adb(["logcat", "-d"])
        
        if "FATAL EXCEPTION" in logs or "ANR in com.cropdoctor" in logs:
            crashes += 1
            source = "CRASH"
            break
            
        if "CHAT_GEN_METRICS:" in logs:
            for line in logs.splitlines():
                if "CHAT_GEN_METRICS:" in line:
                    m_p = re.search(r"promptTokens=(\d+)", line)
                    m_f = re.search(r"ttftMs=(\d+)", line)
                    if m_p: prompt_tokens = int(m_p.group(1))
                    if m_f: ttft_ms = int(m_f.group(1))
                    
        if "CHAT_TEST_END:" in logs:
            for line in logs.splitlines():
                if "CHAT_TEST_END:" in line:
                    m_s = re.search(r"advisorSource='([^']+)'", line)
                    m_lbl = re.search(r"label='([^']+)'", line)
                    m_d = re.search(r"durationMs=(\d+)ms", line)
                    m_p = re.search(r"pssMb=(\d+)MB", line)
                    m_g = re.search(r"guardrailReason='([^']+)'", line)
                    m_r = re.search(r"response='(.*?)'", line)
                    if m_s: source = m_s.group(1)
                    if m_lbl: label = m_lbl.group(1)
                    if m_d: duration_ms = int(m_d.group(1))
                    if m_p: pss_mb = int(m_p.group(1))
                    if m_g and m_g.group(1) != "NONE": fallback_reason = f"GUARDRAIL_{m_g.group(1)}"
                    if m_r: response_text = m_r.group(1)
            chat_finished = True
            break
            
        if "CHAT_GUARDRAIL_REJECT" in logs:
            fallback_reason = "GUARDRAIL_UNGROUNDED"
        elif "Low device RAM" in logs or "Memory budget insufficient" in logs:
            fallback_reason = "RULE_H15_LOW_RAM"
        elif "NO_PASSAGE_FOUND" in logs:
            fallback_reason = "NO_FTS_PASSAGE"

    if not chat_finished:
        source = "TIMEOUT"
        fallback_reason = "TIMEOUT_EXCEEDED"
        duration_ms = int((time.time() - poll_start) * 1000)

    if source == "LLAMA":
        stage_data["llama_answers_count"] += 1
        fallback_reason = "NONE (Llama Answered)"
    else:
        stage_data["offline_guide_count"] += 1
        if fallback_reason == "NONE":
            fallback_reason = "OFFLINE_GUIDE_FALLBACK"
        stage_data["fallback_reasons"][fallback_reason] = stage_data["fallback_reasons"].get(fallback_reason, 0) + 1

    if duration_ms > 0:
        stage_data["all_durations_ms"].append(duration_ms)

    q_info = {
        "run": run_idx,
        "language": lang,
        "query": query,
        "class_id": class_id,
        "crop": crop,
        "source": source,
        "label": label,
        "duration_ms": duration_ms,
        "pss_mb": pss_mb,
        "prompt_tokens": prompt_tokens,
        "ttft_ms": ttft_ms,
        "fallback_reason": fallback_reason,
        "response_preview": response_text[:120] + "..." if len(response_text) > 120 else response_text,
        "crashes": crashes > 0
    }
    
    stage_data["questions"].append(q_info)
    stage_data["crashes_anrs"] += crashes
    
    # Save to benchmarks/llama_complete_stageC.json after EVERY question
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(stage_data, f, indent=2)

    print(f"  Q{run_idx} result: source={source}, duration={duration_ms}ms, ttft={ttft_ms}ms, prompt_tokens={prompt_tokens}, fallback='{fallback_reason}'")
    time.sleep(1.0)

total_elapsed = round(time.time() - start_wall_time, 2)
stage_data["total_elapsed_s"] = total_elapsed
stage_data["status"] = "PASS" if stage_data["crashes_anrs"] == 0 else "FAIL"

with open(OUT_PATH, "w", encoding="utf-8") as f:
    json.dump(stage_data, f, indent=2)

print("\n" + "=" * 70)
print(f"STAGE C SUMMARY: Status={stage_data['status']}")
print(f"Total Questions: {len(stage_data['questions'])}/10")
print(f"Llama Answers: {stage_data['llama_answers_count']}")
print(f"Offline Guide Answers: {stage_data['offline_guide_count']}")
print(f"Fallback Reasons: {stage_data['fallback_reasons']}")
print(f"Crashes / ANRs: {stage_data['crashes_anrs']}")
if stage_data["all_durations_ms"]:
    print(f"Avg Duration: {sum(stage_data['all_durations_ms'])/len(stage_data['all_durations_ms']):.1f}ms")
print(f"Total Time Taken: {total_elapsed}s")
print(f"Saved: {OUT_PATH}")
print("=" * 70)
