# Benchmark Verification Report: Task A-REAL (Real On-Device Llama Advisory)

**Date**: 2026-11-22  
**Target Device**: Xiaomi POCO M4 5G (`22041219PI`, Serial: `sseqjnucrkeeprtw`)  
**OS**: Android 14 (HyperOS, API 34), Linux kernel 5.10.198  
**CPU Architecture**: `arm64-v8a` (Arm Cortex cores, 64-bit)  
**Total System RAM**: 5,731,384 kB (~6 GB reference hardware)  
**App Build**: `com.cropdoctor` v1 (debug variant, commit with real verifiable Llama advisor & zero simulated delay)

---

## 1. Storage Directories & Android 14 SELinux Resolution

### 1.1 Root Cause & Access Permissions on Android 14
On Android 14+ (API 34/35), SELinux policy enforces strict sandboxing for untrusted application processes (`u:r:untrusted_app:s0`). 
- Standard untrusted application processes cannot traverse or read `/data/local/tmp` unless directory permissions are explicitly widened (standard production apps encounter `Permission denied` or `canRead() == false`).
- Earlier preliminary M1 benchmarks executed on `/data/local/tmp` only worked because manual adb developer commands had relaxed Unix permissions.
- In production, the app **must** read its weights either from internal private app storage (`context.filesDir`) or external app-specific storage (`context.getExternalFilesDir(null)`).

### 1.2 Storage Lookup Hierarchy in `ExecuTorchLlamaAdvisor.kt`
The advisor scans for model files in the following exact order:
1. **Primary Internal Storage (`context.filesDir`)**:
   - Model: `/data/user/0/com.cropdoctor/files/llama-3.2-1b-spinquant.pte` (1,135,951,488 bytes / 1083 MB)
   - Tokenizer: `/data/user/0/com.cropdoctor/files/tokenizer.model` (2,183,982 bytes / 2132 KB)
   - *Target directory for the in-app SAF file picker ("Install AI Model" screen).*
2. **External App-Specific Storage (`context.getExternalFilesDir(null)`)**:
   - Model: `/storage/emulated/0/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte`
   - Tokenizer: `/storage/emulated/0/Android/data/com.cropdoctor/files/tokenizer.model`
   - *Direct push destination accessible via standard USB debugging without root.*
3. **Developer Fallback (`/data/local/tmp`)**:
   - Only utilized if the file exists, `canRead()` returns true, and file size checks pass (> 50 MB for `.pte`, > 50 KB for tokenizer).

### 1.3 Exact `adb push` Sideload Commands
To install the model weights via USB without root:
```bash
adb shell "mkdir -p /storage/emulated/0/Android/data/com.cropdoctor/files"
adb push llama-3.2-1b-spinquant.pte /storage/emulated/0/Android/data/com.cropdoctor/files/
adb push tokenizer.model /storage/emulated/0/Android/data/com.cropdoctor/files/
```

---

## 2. Five On-Device Diagnosis Runs with Real Llama 3.2 1B

All 5 diagnoses were executed end-to-end on the physical POCO M4 5G hardware. Vision classification ran via `vision.pte` (MobileNetV3-Large on ExecuTorch XNNPACK), followed by Meta Llama 3.2 1B-Instruct 4-bit SpinQuant via ExecuTorch Arm64 CPU.

### 2.1 Measured Log Metrics Table (Hardware Runs on POCO M4 5G)

| Run # | Crop / Problem | Advisor Source | Time to First Token (TTFT) | Generated Tokens | Decode Speed (tok/s) | Total Logged tok/s | Peak RAM (PSS) | Decision Log Summary |
|---|---|---|---|---|---|---|---|---|
| **Run 1** | Rice Leaf Blast (`sample1_rice.jpg`) | `LLAMA` | 10,615 ms | 119 | 8.00 tok/s | 8.07 tok/s | 1,648 MB (1,688,126 kB) | `outcome=DIAGNOSE code=ACCEPTED` |
| **Run 2** | Maize Common Rust (`case2_maize.jpg`) | `LLAMA` | 10,696 ms | 119 | 7.63 tok/s | 7.69 tok/s | 1,606 MB (1,642,519 kB) | `outcome=DIAGNOSE code=ACCEPTED` |
| **Run 3** | Tomato Yellow Leaf Curl (`case5_tomato.jpg`) | `LLAMA` | 11,001 ms | 119 | 7.69 tok/s | 7.75 tok/s | 1,619 MB (1,658,465 kB) | `outcome=DIAGNOSE code=ACCEPTED` |
| **Run 4** | Rice Bacterial Leaf Blight (`gallery_01`) | `LLAMA` | 10,475 ms | 119 | 7.88 tok/s | 7.95 tok/s | 1,620 MB (1,659,325 kB) | `outcome=DIAGNOSE code=ACCEPTED` |
| **Run 5** | Maize Gray Leaf Spot (`gallery_18`) | `LLAMA` | 11,909 ms | 119 | 7.58 tok/s | 7.64 tok/s | 1,618 MB (1,655,987 kB) | `outcome=DIAGNOSE code=ACCEPTED` |
| **Mean** | **5 Real Field Scenarios** | **`LLAMA` (100%)** | **10,939.2 ms** | **119.0** | **7.76 tok/s** | **7.82 tok/s** | **1,622.2 MB** | **All 5 Runs Within RAM & Speed Budget** |

### 2.2 Representative Logcat Evidence (Run 1)
```text
11-22 18:23:21.716 20369 20397 I ExecuTorchLlamaAdvisor: LlamaModule.load() returned status 0 in 6183 ms
11-22 18:23:21.721 20369 20397 I ExecuTorch: RSS after loading model: 1956.507812 MiB (0 if unsupported)
11-22 18:23:32.334 20369 20397 I ExecuTorch: Prefill token result numel(): 128256
11-22 18:23:47.204 20369 20397 I ExecuTorch: 	Prompt Tokens: 324    Generated Tokens: 119
11-22 18:23:47.204 20369 20397 I ExecuTorch: 	Total inference time:		25.483000 (seconds)		 Rate: 	4.669780 (tokens/second)
11-22 18:23:47.204 20369 20397 I ExecuTorch: 		Prompt evaluation:	10.615000 (seconds)		 Rate: 	30.522845 (tokens/second)
11-22 18:23:47.204 20369 20397 I ExecuTorch: 		Generated 119 tokens:	14.868000 (seconds)		 Rate: 	8.003766 (tokens/second)
11-22 18:23:47.204 20369 20397 I ExecuTorch: 	Time to first generated token:	10.615000 (seconds)
11-22 18:23:47.205 20369 20397 I CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=LLAMA tokens=120 ttft=10615ms tok_per_sec=8.07
```

### 2.3 Memory Compliance with Rule H15 (6 GB Phone Budget)
- **Hard Rule H15 Limit**: Whole app, peak measured by the app/system, $\le 2.5\text{ GB}$ PSS (2,560 MB) while Llama is loaded; $\le 500\text{ MB}$ without it.
- **Measured Peak PSS**: Mean **1,622.2 MB** (1.58 GB). Max observed: **1,648 MB** (Run 1).
- **Compliance Margin**: 912 MB below the 2.5 GB threshold. Zero low-memory kills or GC thrashing observed.

---

## 3. Fallback Verification (Model Files Removed)

To verify deterministic fallback behavior when model files are absent:
1. Model files were temporarily disabled in all storage locations (`.pte.disabled`).
2. Diagnosis was executed on `sample1_rice.jpg`.

### 3.1 Logcat Verification
```text
11-22 18:31:14.619 29077 29100 I ExecuTorchLlamaAdvisor: findModelFiles() scanning for Llama 3.2 1B weights...
11-22 18:31:14.624 29077 29100 I ExecuTorchLlamaAdvisor: Llama 3.2 1B weights not found in storage. Active advisor mode: OFFLINE_GUIDE.
11-22 18:31:16.085 29077 29102 I CROPDOC_DECISION: CROPDOC_DECISION: source=gallery orig=512x512 exif=0 config=ARGB_8888 colorSpace=sRGB IEC61966-2.1 downscale=512x512 | gate1_leafValidator=PASS(frac=0.687) gate2_energy=PASS(energy=-10.0805 vs thresh=-8.0000) gate3_confidence=PASS(class=rice_leaf_blast, prob=0.9153 vs thresh=0.3434) gate4_weeds=PASS | cropMass=0.9833 margin=0.8603 | outcome=DIAGNOSE code=ACCEPTED
11-22 18:31:16.103 29077 29102 I ExecuTorchLlamaAdvisor: Llama weights not available (model=false) -> showing static offline guide
11-22 18:31:16.113 29077 29102 I CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=OFFLINE_GUIDE tokens=0 ttft=0ms tok_per_sec=0.00
```

### 3.2 Fallback UI & Timing Checks
- **Label Rendered**: `"Offline guide - AI explanation not available"`
- **Typing Delay**: **0 ms** (instant static rendering; zero simulated typing or coroutine delay).
- **Audit Tag**: `advisor_source=OFFLINE_GUIDE` recorded in both `CROPDOC_DECISION` log and exported `CaseReport` JSON.

---

## 4. UI Label Screenshots

1. **Real Llama 3.2 1B Advisory Badge**:
   - File: [`benchmarks/a_real_label_llama.png`](a_real_label_llama.png)
   - Badge Text: `"Explained on this phone by Llama 3.2 1B"`
   - Displays real token streaming and model attribution icon.

2. **Offline Guide Fallback Badge**:
   - File: [`benchmarks/a_real_label_offline_guide.png`](a_real_label_offline_guide.png)
   - Badge Text: `"Offline guide - AI explanation not available"`
   - Displays static university-verified agronomy recommendations.

---

## 5. Audit of Prior Advisory Measurements & Screenshots

In compliance with Task A-REAL Requirement 7, all historical benchmark files and screenshots generated prior to this verification were audited:

| Benchmark / Report File | Prior Status | Audited Status | Reason / Verification Evidence |
|---|---|---|---|
| `benchmarks/speed_summary.md` (Table 4.1 Llama section) | Listed 11.94 tok/s | **UNVERIFIED (Standalone CLI Benchmark)** | Measured with CLI harness without full UI pipeline integration; superseded by 7.82 tok/s end-to-end full app measurement. |
| `benchmarks/llama_speed.md` (Early 1B benchmarks) | Listed 4,087 ms TTFT | **UNVERIFIED (Warm Cache / Raw C++ harness)** | Raw ExecuTorch CLI measurement without app JVM/JNI binding overhead. Superseded by 10,939 ms full app cold TTFT. |
| `benchmarks/llama_real_poco_20261121_201606.json` | 7.59 tok/s | **VERIFIED** | Real ExecuTorch JNI call on POCO M4 5G hardware. |
| Any screenshot lacking `"Explained on this phone by Llama 3.2 1B"` badge | Preliminary demo | **UNVERIFIED (Simulated FakeAdvisor Streaming)** | Prior screenshots used `FakeAdvisor` simulated delay before Task A-REAL removed it from the app codebase. |
| `benchmarks/a_real_check.md` (This document) | - | **VERIFIED (Active Gate)** | Verified on POCO M4 5G (`sseqjnucrkeeprtw`) with 5 hardware runs, `CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=LLAMA`, and dumpsys PSS logging. |
