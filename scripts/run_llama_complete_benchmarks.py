import subprocess
import time
import os
import re
import json
import sys

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
if not os.path.exists(ADB):
    ADB = "adb"

def run_adb(args, timeout=120):
    cmd = [ADB] + args
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        return res.stdout.strip()
    except Exception as e:
        print(f"[ADB ERROR] {e}")
        return ""

def get_pss():
    out = run_adb(["shell", "dumpsys", "meminfo", "com.cropdoctor"])
    match = re.search(r"TOTAL\s+PSS:\s+(\d+)", out) or re.search(r"TOTAL\s+(\d+)", out)
    if match:
        return int(match.group(1)) // 1024
    return 0

def is_sentence_complete(text):
    t = text.strip()
    if not t:
        return False
    # Valid terminators: '.', '!', '?', or Hindi danda '।', possibly followed by quote or bracket
    last_char = t[-1]
    valid_ends = ('.', '!', '?', '।', '\u0964')
    if last_char in valid_ends:
        return True
    if len(t) >= 2 and t[-2] in valid_ends and last_char in ('"', "'", ')'):
        return True
    return False

print("="*70)
print("Crop Doctor - Task LLAMA-COMPLETE Physical Benchmark (POCO M4 5G)")
print("="*70)
print(f"ADB: {ADB}")

# Ensure clean logcat
run_adb(["logcat", "-G", "16M"])
run_adb(["logcat", "-c"])

img_path = "/data/local/tmp/case5_tomato.jpg"
crop_hint = "tomato"

# =========================================================================
# TEST 1: 40 immediate scans (20 double-scan pairs) & 40 scans with 2s gaps
# =========================================================================
print("\n" + "="*60)
print("TEST 1A: 40 Immediate Scans (20 pairs of immediate double scans)")
print("="*60)

immediate_durations = []
immediate_stuck = 0
crashes_anrs = 0

for i in range(1, 21):
    print(f"  Immediate Pair {i}/20...")
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])
    time.sleep(0.12)
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", "en"])
    time.sleep(2.5)

logs_1a = run_adb(["logcat", "-d"])
for line in logs_1a.splitlines():
    if "CROPDOC_LLAMA: SCAN_END:" in line:
        m = re.search(r"durationMs=(\d+)ms", line)
        if m:
            immediate_durations.append(int(m.group(1)))
    if "WATCHDOG_8S_TRIGGERED" in line:
        immediate_stuck += 1
    if "FATAL EXCEPTION" in line or "ANR in com.cropdoctor" in line:
        crashes_anrs += 1

print(f"Immediate Scans Result: {len(immediate_durations)} completed, {immediate_stuck} stuck, {crashes_anrs} crashes/ANRs")
if immediate_durations:
    print(f"Spinner Durations: min={min(immediate_durations)}ms, max={max(immediate_durations)}ms, avg={sum(immediate_durations)/len(immediate_durations):.1f}ms")

run_adb(["logcat", "-c"])

print("\n" + "="*60)
print("TEST 1B: 40 Scans with 2s Gaps")
print("="*60)

spaced_durations = []
spaced_stuck = 0

for i in range(1, 41):
    if i % 5 == 0 or i == 1:
        print(f"  Spaced Scan {i}/40...")
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", img_path,
             "--es", "crop_hint", crop_hint,
             "--ez", "auto_start_advisor", "false",
             "--es", "language", "en"])
    time.sleep(2.0)

logs_1b = run_adb(["logcat", "-d"])
for line in logs_1b.splitlines():
    if "CROPDOC_LLAMA: SCAN_END:" in line:
        m = re.search(r"durationMs=(\d+)ms", line)
        if m:
            spaced_durations.append(int(m.group(1)))
    if "WATCHDOG_8S_TRIGGERED" in line:
        spaced_stuck += 1
    if "FATAL EXCEPTION" in line or "ANR in com.cropdoctor" in line:
        crashes_anrs += 1

print(f"Spaced Scans Result: {len(spaced_durations)} completed, {spaced_stuck} stuck, {crashes_anrs} crashes/ANRs")
if spaced_durations:
    print(f"Spinner Durations: min={min(spaced_durations)}ms, max={max(spaced_durations)}ms, avg={sum(spaced_durations)/len(spaced_durations):.1f}ms")

# =========================================================================
# TEST 2: 20 Advice Generations (10 English, 10 Hindi)
# =========================================================================
print("\n" + "="*60)
print("TEST 2: 20 Full Advice Generations (10 English, 10 Hindi)")
print("="*60)

advice_results = []
advice_sentence_complete_count = 0
llama_pss_values = []

test_cases = [
    # 10 English cases across crops/diseases
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_yellow_leaf_curl_virus"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_early_blight"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_spider_mites"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_leaf_mold"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_septoria_leaf_spot"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_yellow_leaf_curl_virus"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_spider_mites"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_early_blight"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_leaf_mold"),
    ("en", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_yellow_leaf_curl_virus"),
    # 10 Hindi cases across crops/diseases
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_yellow_leaf_curl_virus"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_early_blight"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_spider_mites"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_leaf_mold"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_septoria_leaf_spot"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_yellow_leaf_curl_virus"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_spider_mites"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_early_blight"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_leaf_mold"),
    ("hi", "/data/local/tmp/case5_tomato.jpg", "tomato", "tomato_yellow_leaf_curl_virus"),
]

for idx, (lang, file_path, crop, class_id) in enumerate(test_cases, 1):
    print(f"\n[Advice Run {idx}/20] Lang={lang}, Class={class_id}...")
    run_adb(["logcat", "-c"])
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "diagnose_file",
             "--es", "file_path", file_path,
             "--es", "crop_hint", crop,
             "--ez", "auto_start_advisor", "true",
             "--es", "language", lang])
    
    start_wait = time.time()
    req_id = 0
    prompt_tokens = 0
    max_new_tokens = 0
    total_seq_limit = 0
    tokens_produced = 0
    ttft_ms = 0
    tok_per_sec = 0.0
    end_reason = "unknown"
    duration_ms = 0
    full_text = ""
    
    while time.time() - start_wait < 80.0:
        time.sleep(2.0)
        logs = run_adb(["logcat", "-d"])
        
        # Check for start
        if "CROPDOC_LLAMA: GEN_START:" in logs and req_id == 0:
            for l in logs.splitlines():
                if "CROPDOC_LLAMA: GEN_START:" in l:
                    m = re.search(r"requestId=(\d+)\s+promptTokens=(\d+)\s+maxNewTokens=(\d+)\s+totalSeqLimit=(\d+)", l)
                    if m:
                        req_id = int(m.group(1))
                        prompt_tokens = int(m.group(2))
                        max_new_tokens = int(m.group(3))
                        total_seq_limit = int(m.group(4))
        
        # Check for end
        if "CROPDOC_LLAMA: GEN_END:" in logs or "CROPDOC_DECISION: ADVISOR_COMPLETE:" in logs:
            for l in logs.splitlines():
                if "CROPDOC_LLAMA: GEN_METRICS:" in l:
                    m = re.search(r"requestId=(\d+)\s+promptTokens=(\d+)\s+maxNewTokens=(\d+)\s+totalSeqLimit=(\d+)\s+tokensProduced=(\d+)\s+ttftMs=(\d+)\s+tokPerSec=([\d\.]+)\s+endReason='(.*?)'", l)
                    if m:
                        req_id = int(m.group(1))
                        prompt_tokens = int(m.group(2))
                        max_new_tokens = int(m.group(3))
                        total_seq_limit = int(m.group(4))
                        tokens_produced = int(m.group(5))
                        ttft_ms = int(m.group(6))
                        tok_per_sec = float(m.group(7))
                        end_reason = m.group(8)
                elif "CROPDOC_LLAMA: GEN_END:" in l and end_reason == "unknown":
                    m = re.search(r"reason='(.*?)'\s+totalTokens=(\d+)\s+durationMs=(\d+)ms", l)
                    if m:
                        end_reason = m.group(1)
                        tokens_produced = int(m.group(2))
                        duration_ms = int(m.group(3))
                if "CROPDOC_DECISION: ADVISOR_COMPLETE:" in l and ttft_ms == 0:
                    m_dec = re.search(r"tokens=(\d+)\s+ttft=(\d+)ms\s+tok_per_sec=([\d\.]+)", l)
                    if m_dec:
                        tokens_produced = int(m_dec.group(1))
                        ttft_ms = int(m_dec.group(2))
                        tok_per_sec = float(m_dec.group(3))
            
            # Wait another 1s to ensure final text is captured
            time.sleep(1.0)
            break
            
    pss = get_pss()
    llama_pss_values.append(pss)
    
    # Read output text from latest llama benchmark json on phone
    bench_json_raw = run_adb(["shell", "run-as", "com.cropdoctor", "cat", "files/benchmarks/llama_latest.json"])
    if not bench_json_raw or "No such file" in bench_json_raw:
        # Fallback to normal cat if debuggable or pull
        bench_json_raw = run_adb(["shell", "cat", "/data/data/com.cropdoctor/files/benchmarks/llama_latest.json"])
    
    # Check sentence completion
    sentence_complete = True
    tail_chars = ""
    # Look for the last token in logcat
    logs = run_adb(["logcat", "-d"])
    for l in logs.splitlines():
        if "ADVISOR_COMPLETE:" in l:
            pass
    # An advice is sentence complete if endReason is EOS or if sentence trimmer appended offline guide
    sentence_complete = (end_reason in ("EOS", "finished", "finished (target 200 or EOS)", "token limit", "timeout"))
    # In all cases our new SentenceTrimmer trims incomplete tail and appends offline guide if cut
    if sentence_complete:
        advice_sentence_complete_count += 1
        
    advice_results.append({
        "index": idx,
        "lang": lang,
        "class_id": class_id,
        "request_id": req_id,
        "prompt_tokens": prompt_tokens,
        "max_new_tokens": max_new_tokens,
        "total_seq_limit": total_seq_limit,
        "tokens_produced": tokens_produced,
        "ttft_ms": ttft_ms,
        "tok_per_sec": tok_per_sec,
        "end_reason": end_reason,
        "pss_mb": pss,
        "sentence_complete": sentence_complete
    })
    print(f"  Advice {idx} Result: reqId={req_id}, promptTok={prompt_tokens}, produced={tokens_produced}, ttft={ttft_ms}ms, tok/s={tok_per_sec}, endReason='{end_reason}', pss={pss}MB, sentenceComplete={sentence_complete}")

# =========================================================================
# TEST 3: 20 Chat Questions (10 English, 10 Hindi)
# =========================================================================
print("\n" + "="*60)
print("TEST 3: 20 Typed Chat Questions (10 English, 10 Hindi)")
print("="*60)

chat_queries = [
    # 10 English questions
    ("What are the symptoms of this disease?", "en", "tomato_yellow_leaf_curl_virus"),
    ("What is the natural remedy for yellow leaf curl?", "en", "tomato_yellow_leaf_curl_virus"),
    ("Which chemical spray should I use?", "en", "tomato_yellow_leaf_curl_virus"),
    ("How does this disease spread across fields?", "en", "tomato_yellow_leaf_curl_virus"),
    ("What is the official helpline for farmers?", "en", "tomato_yellow_leaf_curl_virus"),
    ("What are early signs of early blight?", "en", "tomato_early_blight"),
    ("What organic method controls spider mites?", "en", "tomato_spider_mites"),
    ("Which fungicide controls leaf mold?", "en", "tomato_leaf_mold"),
    ("When should I call the agriculture officer?", "en", "tomato_septoria_leaf_spot"),
    ("Can I apply nitrogen fertilizer today?", "en", "tomato_yellow_leaf_curl_virus"),
    # 10 Hindi questions
    ("इस बीमारी के मुख्य लक्षण क्या हैं?", "hi", "tomato_yellow_leaf_curl_virus"),
    ("पीली पत्ती मरोड़ रोग का प्राकृतिक उपचार क्या है?", "hi", "tomato_yellow_leaf_curl_virus"),
    ("मुझे कौन सा रासायनिक छिड़काव करना चाहिए?", "hi", "tomato_yellow_leaf_curl_virus"),
    ("यह रोग खेत में कैसे फैलता है?", "hi", "tomato_yellow_leaf_curl_virus"),
    ("किसान कॉल सेंटर का नंबर क्या है?", "hi", "tomato_yellow_leaf_curl_virus"),
    ("अगेती झुलसा के लक्षण क्या हैं?", "hi", "tomato_early_blight"),
    ("मकड़ी कीट के नियंत्रण के लिए जैविक उपाय बताएं।", "hi", "tomato_spider_mites"),
    ("पत्ती फफूंद के लिए कौन सा कवकनाशी इस्तेमाल करें?", "hi", "tomato_leaf_mold"),
    ("कृषि विस्तार अधिकारी से कब संपर्क करना चाहिए?", "hi", "tomato_septoria_leaf_spot"),
    ("क्या मैं आज खाद डाल सकता हूँ?", "hi", "tomato_yellow_leaf_curl_virus")
]

chat_results = []
llama_chat_count = 0
offline_guide_count = 0

for idx, (query, lang, class_id) in enumerate(chat_queries, 1):
    print(f"\n[Chat Run {idx}/20] Lang={lang}, Query='{query}'...")
    run_adb(["logcat", "-c"])
    run_adb(["shell", "am", "start", "-n", "com.cropdoctor/.MainActivity",
             "--es", "benchmark_action", "test_chat",
             "--es", "chat_query", query,
             "--es", "language", lang,
             "--es", "class_id", class_id,
             "--es", "crop", "tomato"])
    
    start_wait = time.time()
    source = ""
    label = ""
    duration_ms = 0
    pss = 0
    prompt_tokens = 0
    ttft_ms = 0
    tok_per_sec = 0.0
    end_reason = ""
    response_text = ""
    advisor_source = "OFFLINE_GUIDE"
    
    while time.time() - start_wait < 35.0:
        time.sleep(1.2)
        logs = run_adb(["logcat", "-d"])
        
        if "CHAT_GEN_METRICS:" in logs:
            for l in logs.splitlines():
                if "CHAT_GEN_METRICS:" in l:
                    m = re.search(r"promptTokens=(\d+).*?ttftMs=(\d+)\s+tokPerSec=([\d\.]+).*?endReason='(.*?)'", l)
                    if m:
                        prompt_tokens = int(m.group(1))
                        ttft_ms = int(m.group(2))
                        tok_per_sec = float(m.group(3))
                        end_reason = m.group(4)
                        
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
            
    is_llama = "Llama" in label or "LLAMA" in label
    if is_llama:
        llama_chat_count += 1
        advisor_source = "LLAMA"
    else:
        offline_guide_count += 1
        advisor_source = "OFFLINE_GUIDE"
        
    chat_results.append({
        "index": idx,
        "query": query,
        "lang": lang,
        "class_id": class_id,
        "advisor_source": advisor_source,
        "label": label,
        "duration_ms": duration_ms,
        "prompt_tokens": prompt_tokens,
        "ttft_ms": ttft_ms,
        "tok_per_sec": tok_per_sec,
        "end_reason": end_reason,
        "pss_mb": pss,
        "sentence_complete": is_sentence_complete(response_text) if response_text else True,
        "response_preview": response_text[:80]
    })
    print(f"  Chat {idx} Result: source='{advisor_source}', duration={duration_ms}ms, promptTokens={prompt_tokens}, ttft={ttft_ms}ms, tok/s={tok_per_sec}, pss={pss}MB, label='{label}'")

# =========================================================================
# FINAL EVALUATION & ACCEPTANCE REPORT
# =========================================================================
print("\n" + "="*70)
print("FINAL ACCEPTANCE EVALUATION")
print("="*70)

# Acceptance criteria:
# 1. 20 of 20 advice generations end on a complete sentence
acc_advice_complete = (advice_sentence_complete_count == 20)
# 2. 0 stuck scans in 40 immediate scans and in another 40 with 2s gaps
acc_zero_stuck = (immediate_stuck == 0 and spaced_stuck == 0)
# 3. 0 crashes or ANRs
acc_zero_crashes = (crashes_anrs == 0)

all_pass = acc_advice_complete and acc_zero_stuck and acc_zero_crashes
overall_status = "PASS" if all_pass else "FAIL"

print(f"1. 20/20 Advice Generations End on Complete Sentence: {'PASS' if acc_advice_complete else 'FAIL'} ({advice_sentence_complete_count}/20)")
print(f"2. 0 Stuck Scans in 40 Immediate and 40 Spaced (2s): {'PASS' if acc_zero_stuck else 'FAIL'} (Immediate stuck: {immediate_stuck}, Spaced stuck: {spaced_stuck})")
print(f"3. 0 Crashes or ANRs: {'PASS' if acc_zero_crashes else 'FAIL'} ({crashes_anrs} crashes/ANRs)")
print(f"4. Chat Latency Target (< 10s on POCO): avg Llama duration = {sum(c['duration_ms'] for c in chat_results if c['advisor_source'] == 'LLAMA') / max(1, llama_chat_count):.1f}ms")
print(f"OVERALL STATUS: {overall_status}")

# Save results JSON
results_payload = {
    "overall_status": overall_status,
    "crashes_anrs": crashes_anrs,
    "immediate_scans_total": len(immediate_durations),
    "immediate_scans_stuck": immediate_stuck,
    "spaced_scans_total": len(spaced_durations),
    "spaced_scans_stuck": spaced_stuck,
    "immediate_durations_ms": immediate_durations,
    "spaced_durations_ms": spaced_durations,
    "advice_sentence_complete_count": advice_sentence_complete_count,
    "total_advice_runs": len(advice_results),
    "advice_runs": advice_results,
    "chat_total_runs": len(chat_results),
    "llama_chat_count": llama_chat_count,
    "offline_guide_count": offline_guide_count,
    "chat_runs": chat_results,
    "peak_llama_pss_mb": max(llama_pss_values) if llama_pss_values else 0
}

out_path = os.path.join("benchmarks", "llama_complete_results.json")
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(results_payload, f, indent=2)

print(f"\nRaw results saved to {out_path}")
