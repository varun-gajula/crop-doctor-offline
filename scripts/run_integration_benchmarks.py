import subprocess
import time
import os
import re
import json

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

def run_adb(args):
    cmd = [ADB] + args
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    return res.stdout.strip()

def get_pss():
    out = run_adb(["shell", "dumpsys", "meminfo", "com.cropdoctor"])
    match = re.search(r"TOTAL\s+PSS:\s+(\d+)", out) or re.search(r"TOTAL\s+(\d+)", out)
    if match:
        return int(match.group(1)) // 1024
    return 0

print("Using ADB:", ADB)
run_adb(["logcat", "-G", "16M"])
run_adb(["logcat", "-c"])

# ==========================================
# PART 1: 20 cycles of: scan a DIAGNOSE, scan again immediately
# ==========================================
print("\n" + "="*60)
print("PART 1: 20 cycles of (scan a DIAGNOSE, scan again immediately)")
print("="*60)

double_scan_durations = []
double_scan_stuck = 0
crashes_anrs = 0
img_path = "/data/local/tmp/case5_tomato.jpg"
crop_hint = "tomato"

for i in range(1, 21):
    print(f"\n[Part 1] Double-scan Cycle {i}/20...")
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])
    time.sleep(0.15)
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])
    time.sleep(2.5)

logs_p1 = run_adb(["logcat", "-d"])
for line in logs_p1.splitlines():
    if "CROPDOC_LLAMA: SCAN_END:" in line:
        m = re.search(r"durationMs=(\d+)ms", line)
        if m:
            double_scan_durations.append(int(m.group(1)))
    if "WATCHDOG_8S_TRIGGERED" in line or "TimeoutCancellationException" in line:
        double_scan_stuck += 1
    if "FATAL EXCEPTION" in line or "ANR in com.cropdoctor" in line:
        crashes_anrs += 1

print(f"Part 1 Complete: {len(double_scan_durations)} scans logged, {double_scan_stuck} stuck scans, {crashes_anrs} crashes/ANRs.")
if double_scan_durations:
    print(f"Spinner: min={min(double_scan_durations)}ms, max={max(double_scan_durations)}ms, avg={sum(double_scan_durations)/len(double_scan_durations):.1f}ms")

run_adb(["logcat", "-c"])

# ==========================================
# PART 2: 10 full Llama advice generations to the end (5 English, 5 Hindi)
# ==========================================
print("\n" + "="*60)
print("PART 2: 10 full Llama advice generations (5 English, 5 Hindi)")
print("="*60)

en_generations = []
hi_generations = []
advice_completed_count = 0
llama_pss_values = []

# 5 English runs
for i in range(1, 6):
    print(f"\n[Part 2 - English] Generation {i}/5...")
    run_adb(["logcat", "-c"])
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])
    
    start_wait = time.time()
    tokens = 0
    duration_ms = 0
    reason = "unknown"
    ttft = 0
    tok_sec = 0.0
    
    while time.time() - start_wait < 65.0:
        time.sleep(2.0)
        logs = run_adb(["logcat", "-d"])
        if "CROPDOC_LLAMA: GEN_END:" in logs:
            for l in logs.splitlines():
                if "CROPDOC_LLAMA: GEN_END:" in l:
                    m = re.search(r"reason='(.*?)'\s+totalTokens=(\d+)\s+durationMs=(\d+)ms", l)
                    if m:
                        reason = m.group(1)
                        tokens = int(m.group(2))
                        duration_ms = int(m.group(3))
                if "CROPDOC_DECISION: ADVISOR_COMPLETE:" in l:
                    m_dec = re.search(r"ttft=(\d+)ms\s+tok_per_sec=([\d\.]+)", l)
                    if m_dec:
                        ttft = int(m_dec.group(1))
                        tok_sec = float(m_dec.group(2))
            break
            
    pss = get_pss()
    llama_pss_values.append(pss)
    completed = ("finished" in reason) and tokens >= 100
    if completed:
        advice_completed_count += 1
        
    en_generations.append({
        "gen": i, "lang": "en", "tokens": tokens, "duration_ms": duration_ms,
        "ttft_ms": ttft, "tokens_sec": tok_sec, "reason": reason, "pss_mb": pss, "completed": completed
    })
    print(f"  Result EN {i}: tokens={tokens}, duration={duration_ms}ms, ttft={ttft}ms, tok/s={tok_sec}, pss={pss}MB, reason='{reason}', completed={completed}")

# 5 Hindi runs
for i in range(1, 6):
    print(f"\n[Part 2 - Hindi] Generation {i}/5...")
    run_adb(["logcat", "-c"])
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "hi"])
    
    start_wait = time.time()
    tokens = 0
    duration_ms = 0
    reason = "unknown"
    ttft = 0
    tok_sec = 0.0
    
    while time.time() - start_wait < 65.0:
        time.sleep(2.0)
        logs = run_adb(["logcat", "-d"])
        if "CROPDOC_LLAMA: GEN_END:" in logs:
            for l in logs.splitlines():
                if "CROPDOC_LLAMA: GEN_END:" in l:
                    m = re.search(r"reason='(.*?)'\s+totalTokens=(\d+)\s+durationMs=(\d+)ms", l)
                    if m:
                        reason = m.group(1)
                        tokens = int(m.group(2))
                        duration_ms = int(m.group(3))
                if "CROPDOC_DECISION: ADVISOR_COMPLETE:" in l:
                    m_dec = re.search(r"ttft=(\d+)ms\s+tok_per_sec=([\d\.]+)", l)
                    if m_dec:
                        ttft = int(m_dec.group(1))
                        tok_sec = float(m_dec.group(2))
            break
            
    pss = get_pss()
    llama_pss_values.append(pss)
    completed = ("finished" in reason) and tokens >= 100
    if completed:
        advice_completed_count += 1
        
    hi_generations.append({
        "gen": i, "lang": "hi", "tokens": tokens, "duration_ms": duration_ms,
        "ttft_ms": ttft, "tokens_sec": tok_sec, "reason": reason, "pss_mb": pss, "completed": completed
    })
    print(f"  Result HI {i}: tokens={tokens}, duration={duration_ms}ms, ttft={ttft}ms, tok/s={tok_sec}, pss={pss}MB, reason='{reason}', completed={completed}")

# ==========================================
# PART 3: 5 typed chat questions
# ==========================================
print("\n" + "="*60)
print("PART 3: 5 typed chat questions")
print("="*60)

chat_questions = [
    ("What are the symptoms of this disease?", "en"),
    ("What is the natural remedy?", "en"),
    ("Which chemical spray should I use?", "en"),
    ("इस बीमारी के लक्षण क्या हैं?", "hi"),
    ("प्राकृतिक उपचार क्या है?", "hi")
]

chat_results = []
for i, (q, lang) in enumerate(chat_questions, 1):
    print(f"\n[Part 3] Chat Question {i}/5 ({lang}): '{q}'...")
    run_adb(["logcat", "-c"])
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "test_chat",
             "--es", "chat_query", q,
             "--es", "language", lang,
             "--es", "class_id", "tomato_yellow_leaf_curl_virus",
             "--es", "crop", "tomato"])
    
    start_wait = time.time()
    source = ""
    label = ""
    duration_ms = 0
    pss = 0
    response_text = ""
    
    while time.time() - start_wait < 45.0:
        time.sleep(1.5)
        logs = run_adb(["logcat", "-d"])
        if "CHAT_TEST_END:" in logs:
            for l in logs.splitlines():
                if "CHAT_TEST_END:" in l:
                    m = re.search(r"source='(.*?)'\s+label='(.*?)'\s+durationMs=(\d+)ms\s+pssMb=(\d+)MB.*?response='(.*?)'", l)
                    if m:
                        source = m.group(1)
                        label = m.group(2)
                        duration_ms = int(m.group(3))
                        pss = int(m.group(4))
                        response_text = m.group(5)
            break
            
    chat_results.append({
        "index": i, "query": q, "lang": lang, "source": source,
        "label": label, "duration_ms": duration_ms, "pss_mb": pss, "response": response_text
    })
    print(f"  Chat {i} Result: duration={duration_ms}ms, pss={pss}MB, source='{source}', label='{label}'")

# ==========================================
# PART 4: 5 voice questions (English and Hindi)
# ==========================================
print("\n" + "="*60)
print("PART 4: 5 voice questions (English and Hindi)")
print("="*60)

voice_questions = [
    ("en", "What are the symptoms?"),
    ("en", "What should I do now?"),
    ("en", "Which chemical spray to use?"),
    ("hi", "लक्षण क्या हैं?"),
    ("hi", "उपचार क्या है?")
]

voice_results = []
speech_pss_values = []

for i, (lang, desc) in enumerate(voice_questions, 1):
    print(f"\n[Part 4] Voice Test {i}/5 ({lang}): '{desc}'...")
    run_adb(["logcat", "-c"])
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "test_voice",
             "--es", "language", lang])
    
    start_wait = time.time()
    duration_ms = 0
    pss = 0
    recognized = ""
    
    while time.time() - start_wait < 25.0:
        time.sleep(1.0)
        logs = run_adb(["logcat", "-d"])
        if "VOICE_TEST_END:" in logs:
            for l in logs.splitlines():
                if "VOICE_TEST_END:" in l:
                    m = re.search(r"text='(.*?)'\s+durationMs=(\d+)ms\s+pssMb=(\d+)MB", l)
                    if m:
                        recognized = m.group(1)
                        duration_ms = int(m.group(2))
                        pss = int(m.group(3))
            break
            
    if pss > 0:
        speech_pss_values.append(pss)
    voice_results.append({
        "index": i, "lang": lang, "desc": desc, "duration_ms": duration_ms, "pss_mb": pss, "recognized": recognized
    })
    print(f"  Voice {i} Result: duration={duration_ms}ms, pss={pss}MB, recognized='{recognized}'")

# Check for overall crashes / ANRs across entire session
final_logs = run_adb(["logcat", "-d"])
for l in final_logs.splitlines():
    if "FATAL EXCEPTION" in l or "ANR in com.cropdoctor" in l:
        crashes_anrs += 1

# Summary
summary = {
    "crashes": crashes_anrs,
    "anrs": 0,
    "double_scans_total": 40,
    "double_scans_stuck": double_scan_stuck,
    "longest_spinner_ms": max(double_scan_durations) if double_scan_durations else 0,
    "min_spinner_ms": min(double_scan_durations) if double_scan_durations else 0,
    "avg_spinner_ms": sum(double_scan_durations)/len(double_scan_durations) if double_scan_durations else 0,
    "advice_completed_to_end": advice_completed_count,
    "total_advice_runs": 10,
    "en_generations": en_generations,
    "hi_generations": hi_generations,
    "chat_results": chat_results,
    "voice_results": voice_results,
    "pss_with_llama_mb": max(llama_pss_values) if llama_pss_values else 0,
    "pss_with_speech_mb": max(speech_pss_values) if speech_pss_values else 0
}

with open("benchmarks/integration_poco_results.json", "w", encoding="utf-8") as f:
    json.dump(summary, f, indent=2, ensure_ascii=False)

print("\n" + "="*60)
print("INTEGRATION BENCHMARKS COMPLETE")
print("="*60)
print(f"Crashes: {summary['crashes']}, ANRs: {summary['anrs']}")
print(f"Stuck scans: {summary['double_scans_stuck']} / 40")
print(f"Longest spinner: {summary['longest_spinner_ms']} ms")
print(f"Advice completed to end: {summary['advice_completed_to_end']} / 10")
print(f"Peak PSS with Llama: {summary['pss_with_llama_mb']} MB")
print(f"Peak PSS with Speech model: {summary['pss_with_speech_mb']} MB")
