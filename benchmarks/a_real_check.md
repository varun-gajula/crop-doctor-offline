# Benchmark Verification Report: Task A-REAL (Real On-Device Llama Advisory)

**Date**: 2026-11-21  
**Target Device**: Xiaomi POCO M4 Pro 5G (`22041219PI`, Serial: `sseqjnucrkeeprtw`)  
**OS**: Android 14 (HyperOS, API 34), Linux kernel 5.10.198  
**CPU Architecture**: `arm64-v8a` (Arm Cortex cores)  
**Total RAM**: 5,731,384 kB (~6 GB reference hardware)  
**App Build**: `com.cropdoctor` v1 (debug variant, commit updated with prompt KV cache fix)

---

## 1. Storage Paths & Model File Resolution

On real Android hardware (Android 14+):
- **Primary Supported Storage**: The app's own external private folder (`context.getExternalFilesDir(null)`):
  `/sdcard/Android/data/com.cropdoctor/files/`
  - Model file: `llama-3.2-1b-spinquant.pte` (1,135,951,488 bytes / 1083 MB)
  - Tokenizer: `tokenizer.model` (2,183,982 bytes / 2132 KB)
  *Advantages*: Readable by app without special storage permissions, accessible via `adb push` over USB, and compatible with the in-app offline file picker.
- **Secondary Fallback**: Internal private app storage (`context.filesDir`):
  `/data/user/0/com.cropdoctor/files/`
- **Development Fallback**: `/data/local/tmp/`
  - On this POCO Android 14 device, `/data/local/tmp/` was verified readable by `run-as com.cropdoctor` because permissions were set to `-rw-rw-rw-`.
  - Resolution order in `ExecuTorchLlamaAdvisor.kt` scans internal `filesDir`, external `getExternalFilesDir`, and then `/data/local/tmp` independently, validating that files exist, are readable, and exceed size thresholds (> 50 MB for model, > 50 KB for tokenizer).

---

## 2. On-Device Real Llama Execution Measurement

A real diagnosis was executed using `sample1_rice.jpg` (Rice Leaf Blast) with on-device vision inference and on-device Llama 3.2 1B advisory:

### Logcat Execution Log:
```text
11-21 20:15:31.909 I ExecuTorchLlamaAdvisor: Located Llama 3.2 1B: model=/data/local/tmp/llama-3.2-1b-spinquant.pte (1083 MB), tok=/data/local/tmp/tokenizer.model
11-21 20:15:33.922 I CROPDOC_DECISION: CROPDOC_DECISION: source=gallery orig=512x512 exif=0 config=ARGB_8888 colorSpace=sRGB IEC61966-2.1 downscale=512x512 | gate1_leafValidator=PASS(frac=0.687) gate2_energy=PASS(energy=-10.0805 vs thresh=-8.0000) gate3_confidence=PASS(class=rice_leaf_blast, prob=0.9153 vs thresh=0.3434) gate4_weeds=PASS | cropMass=0.9833 margin=0.8603 | outcome=DIAGNOSE code=ACCEPTED
11-21 20:15:33.945 I ExecuTorchLlamaAdvisor: Loading Llama 3.2 1B ExecuTorch module (arm64-v8a): model=/data/local/tmp/llama-3.2-1b-spinquant.pte, tok=/data/local/tmp/tokenizer.model...
11-21 20:15:40.209 I ExecuTorchLlamaAdvisor: LlamaModule.load() returned status 0 in 6264 ms
11-21 20:16:06.784 I ExecuTorchLlamaAdvisor: LlamaModule.stop() requested
11-21 20:16:06.785 I CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=LLAMA tokens=120 ttft=10762ms tok_per_sec=7.59
11-21 20:16:06.834 I ExecuTorchLlamaAdvisor: Benchmark logged to /data/user/0/com.cropdoctor/files/benchmarks/llama_20261121_201606.json (Tokens/s: 7.59, RAM: 1654MB)
```

### Measured Metrics Summary:
| Metric | Value | Budget / Requirement | Status |
|---|---|---|---|
| **Advisor Source** | `LLAMA` | Real on-device model | PASS |
| **Model Load Latency** | 6,264 ms | Background lazy load | PASS |
| **Time to First Token (TTFT)** | 10,762 ms (inc. load) | Streaming non-blocking UI | PASS |
| **Generation Speed** | **7.59 tokens/sec** | Real Arm64 CPU inference | PASS |
| **Tokens Generated** | 120 tokens | ~100-150 token budget | PASS |
| **Peak RAM (PSS)** | **1,654 MB** | **$\le 2,500$ MB limit (Rule H15)** | PASS |
| **Guardrail Validation** | Passed (`guardrail_passed=true`) | Rule H4 & H5 verified | PASS |
| **UI Badge** | `🤖 Explained on this phone by Llama 3.2 1B` | Task A-REAL Requirement 4 | PASS |

---

## 3. Fallback to Offline Agronomy Guide

When the model file is not present or when language is Telugu (Rule H6):
- `advisor_source`: `OFFLINE_GUIDE`
- UI Badge: `📖 Offline agronomy guide - AI explanation not available`
- Generation: Static verified KB text rendered immediately in one shot (zero simulated typing delay).
- Log Line: `CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=OFFLINE_GUIDE tokens=0 ttft=0ms tok_per_sec=0.00`

---

## 4. Audit of Prior Advisory Measurements

In accordance with Task A-REAL Requirement 7:
- Any benchmark logs or reports created prior to commit `8dded07` that recorded advisory generation without the `advisor_source=LLAMA` verification tag are formally audited as **UNVERIFIED (Simulated FakeAdvisor Streaming)**.
- The only verified on-device Llama 3.2 1B benchmark logs are:
  1. `benchmarks/llama_speed_poco_5cases.json` (instrumentation benchmark: 12.47 tok/s)
  2. `benchmarks/llama_real_poco_20261121_201606.json` (end-to-end full app diagnosis: 7.59 tok/s, 1654 MB PSS)
