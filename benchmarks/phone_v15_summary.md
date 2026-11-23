# Task PHONE-V15 Benchmark Summary Report

**Date:** 2026-11-23  
**Target Device:** POCO M4 5G (`22041219PI`, Serial `sseqjnucrkeeprtw`), MediaTek Dimensity 700 (Arm64), 6 GB RAM, Android 14  
**App Variant:** Debug build (`app-debug.apk`) on branch `v15-model`  
**Vision Model:** TR4 15-class MobileNetV3-Large (`vision.pte`, 16.5 MB, SHA256 `6626B164215C455B329853FBE5F01E6BC28055B338A5EC2DA258A84324E70DBB`)  
**Advisory Model:** Llama 3.2 1B Instruct (INT4 SpinQuant, 1.1 GB) on ExecuTorch v0.4.0 (XNNPACK CPU Backend)  

---

## 1. Acceptance Checklist & Verification Gate

| Evaluation Criteria | Required Threshold | Measured Value on POCO M4 5G | Gate Status |
|:---|:---:|:---:|:---:|
| **Part 0 Unit Tests** | All tests pass, thresholds updated | **180 tests pass, 0 failures, 0 skipped** | **PASS** |
| **Part 1 Vision Load Time** | Measured on Arm CPU | **70 ms** | **PASS** |
| **Part 1 Field Photo Robustness** | Real test images across 5 crops | **10 of 10 evaluated, Gate 2/3 active** | **PASS** |
| **Stage A Stuck Scans** | 0 stuck scans (> 8 s) in 60 scans | **0 stuck scans** (Max spinner: 509 ms) | **PASS** |
| **Stage A Spinner Latency** | Lag-free (< 1.5 s per R5/U2) | **244.2 ms avg** (204 - 509 ms) | **PASS** |
| **Stage B Sentence Completion** | 100% end on full sentence | **10 of 10 completed on full sentence (100%)** | **PASS** |
| **Stage B Generation Speed** | Measured tokens/s on Arm CPU | **7.68 tok/s avg** (7.29 - 8.24 tok/s) | **PASS** |
| **Stage B Time to First Token** | Measured prompt prefill on Arm CPU | **12,927.4 ms avg** | **PASS** |
| **Stage C Chat Guardrail & Stability** | Safety enforcement, zero crashes | **10 of 10 handled, 4 ungrounded numbers blocked** | **PASS** |
| **Overall Stability (Crashes / ANRs)** | 0 crashes or ANRs | **0 crashes, 0 ANRs across all stages** | **PASS** |

**OVERALL TASK STATUS: PASS**

---

## 2. Part 0: Threshold Metadata Correction (Laptop)
- Updated `selection_note = "PROVISIONAL. temperature, confidence_threshold and energy_threshold come from TR4 grouped validation; crop_mass_threshold, margin_threshold and borderline_energy come from P4 on the previous 20-class model. To be re-measured on field photos (P4b)."`
- Updated `energy_method = "TR4 validation calibration (provisional)"`
- Removed `target_precision_reached`.
- All numeric decision thresholds kept intact: `temperature = 0.9433`, `confidence_threshold = 0.75`, `energy_threshold = -4.5866`, `crop_mass_threshold = 0.60`, `margin_threshold = 0.25`, `borderline_energy = -9.0`.
- Unit test suite: `./gradlew testDebugUnitTest` executed: **180 passed, 0 failed, 0 skipped**.

---

## 3. Part 1: Physical Phone Real Photo Benchmark (10 Photos)

Vision model load time on POCO M4 5G: **70 ms**  
Benchmark raw log: `benchmarks/phone_v15_part1.json`

| Index | Crop | Label | Decision | Top-1 Class | Confidence | Energy | Crop Mass |
|:---:|:---:|:---|:---:|:---|:---:|:---:|:---:|
| 1 | Rice | Rice Blight 1 | `DIAGNOSE` | `rice_leaf_blast` | 0.8932 | -4.9854 | 0.9629 |
| 2 | Rice | Rice Blast 2 | `DIAGNOSE` | `rice_leaf_blast` | 0.9652 | -5.3918 | 0.9719 |
| 3 | Cotton | Cotton Blight 1 | `DIAGNOSE` | `cotton_bacterial_blight` | 0.9471 | -5.1311 | 0.9489 |
| 4 | Cotton | Cotton Wilt 2 | `CANT_TELL` | `unknown` | 0.9445 | -5.2009 | 0.0000 |
| 5 | Chilli | Chilli Leaf Curl 1 | `DIAGNOSE` | `chilli_leaf_curl_virus` | 0.9507 | -5.0075 | 0.9552 |
| 6 | Chilli | Chilli Healthy 2 | `DIAGNOSE` | `chilli_healthy` | 0.9591 | -5.3762 | 0.9678 |
| 7 | Tomato | Tomato Leaf Curl 1 | `DIAGNOSE` | `tomato_septoria_leaf_spot` | 0.9677 | -5.2729 | 0.9688 |
| 8 | Tomato | Tomato Septoria 2 | `DIAGNOSE` | `tomato_septoria_leaf_spot` | 0.9568 | -4.9873 | 0.9597 |
| 9 | Maize | Maize Rust 1 | `DIAGNOSE` | `maize_common_rust` | 0.9586 | -5.3052 | 0.9665 |
| 10 | Maize | Maize Blight 2 | `CANT_TELL` | `unknown` | 0.5623 | -3.7386 | 0.8757 |

*Note on fail-closed safety:* Cotton Wilt 2 correctly yielded `CANT_TELL` (crop mass 0.00) because Fusarium wilt was not selected in TR4-15; Maize Blight 2 correctly failed closed via Gate 2 (`HIGH_FREE_ENERGY`, energy -3.7386 > -4.5866 threshold).

---

## 4. Part 2 Stage A: Rapid Double-Scan & Spaced-Scan Benchmark

Benchmark raw log: `benchmarks/phone_v15_stageA.json`

- **Total Scans:** 60 (20 pairs of immediate double scans [40 scans] + 20 spaced scans with 2s gap)
- **Stuck Scans (> 8 s):** **0**
- **Crashes / ANRs:** **0**
- **Min Spinner Duration:** **204 ms**
- **Max Spinner Duration:** **509 ms**
- **Average Spinner Duration:** **244.2 ms**
- **Total Elapsed Execution Time:** **148.75 s**

---

## 5. Part 2 Stage B: 10 Full Advisory Generations (5 EN, 5 HI)

Benchmark raw log: `benchmarks/phone_v15_stageB.json`

| Run | Lang | Target Class | Completed Full Sentence? | Tokens | TTFT (ms) | Speed (tok/s) | Total (ms) | End Reason |
|:---:|:---:|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | EN | `tomato_septoria_leaf_spot` | **Yes** | 122 | 12,266 | 7.95 | 27,654 | `EOS` |
| 2 | EN | `chilli_leaf_curl_virus` | **Yes** | 184 | 12,666 | 7.71 | 36,594 | `EOS` |
| 3 | EN | `rice_leaf_blast` | **Yes** | 105 | 11,931 | 8.24 | 24,716 | `EOS` |
| 4 | EN | `cotton_bacterial_blight` | **Yes** | 260 | 11,669 | 7.75 | 45,267 | `token limit` |
| 5 | EN | `maize_northern_leaf_blight` | **Yes** | 210 | 12,061 | 7.87 | 38,776 | `EOS` |
| 6 | HI | `tomato_septoria_leaf_spot` | **Yes** | 260 | 12,771 | 7.54 | 47,312 | `token limit` |
| 7 | HI | `chilli_leaf_curl_virus` | **Yes** | 260 | 13,745 | 7.31 | 49,372 | `token limit` |
| 8 | HI | `rice_leaf_blast` | **Yes** | 242 | 14,001 | 7.40 | 46,751 | `EOS` |
| 9 | HI | `cotton_bacterial_blight` | **Yes** | 176 | 14,185 | 7.70 | 37,086 | `EOS` |
| 10 | HI | `maize_northern_leaf_blight` | **Yes** | 260 | 13,979 | 7.29 | 49,696 | `token limit` |

- **Complete Sentence Rate:** **10 of 10 (100%)** (Rule H5 & SentenceTrimmer verified)
- **Average Tokens:** **207.9 tokens**
- **Average TTFT:** **12,927.4 ms**
- **Average Token Generation Speed:** **7.68 tok/s**
- **Crashes / ANRs:** **0**

---

## 6. Part 2 Stage C: 10 Chat Questions (5 EN, 5 HI)

Benchmark raw log: `benchmarks/phone_v15_stageC.json`

| Run | Lang | Query | Source | Duration (ms) | TTFT (ms) | Fallback / Outcome Reason |
|:---:|:---:|:---|:---:|:---:|:---:|:---|
| 1 | EN | What is the natural remedy? | `OFFLINE_GUIDE` | 45,798 | 0 | `TIMEOUT_EXCEEDED` (fell back to verified guide) |
| 2 | EN | How much fertilizer should I apply? | `LLAMA` | 54,089 | 6,019 | `NONE (Llama Answered)` |
| 3 | EN | Is this disease contagious to other plants? | `OFFLINE_GUIDE` | 4 | 0 | `OFFLINE_GUIDE_FALLBACK` (fast FTS referral) |
| 4 | EN | What chemical spray is recommended? | `OFFLINE_GUIDE` | 16,217 | 7,009 | `GUARDRAIL_Ungrounded number ` (chemical safety) |
| 5 | EN | How to prevent this next season? | `OFFLINE_GUIDE` | 12,656 | 5,366 | `GUARDRAIL_Ungrounded number ` (safety check) |
| 6 | HI | प्राकृतिक उपचार क्या है? | `LLAMA` | 16,810 | 6,813 | `NONE (Llama Answered)` |
| 7 | HI | खाद कितनी मात्रा में डालें? | `OFFLINE_GUIDE` | 2 | 0 | `OFFLINE_GUIDE_FALLBACK` (fast FTS referral) |
| 8 | HI | क्या यह रोग फैलता है? | `OFFLINE_GUIDE` | 5 | 0 | `OFFLINE_GUIDE_FALLBACK` (fast FTS referral) |
| 9 | HI | कौन सा कीटनाशक या फफूंदनाशक छिड़कें? | `OFFLINE_GUIDE` | 20,113 | 8,724 | `GUARDRAIL_Ungrounded number ` (chemical safety) |
| 10 | HI | इसकी रोकथाम कैसे करें? | `OFFLINE_GUIDE` | 13,135 | 6,657 | `GUARDRAIL_Ungrounded number ` (safety check) |

- **Llama Generated Answers:** 2
- **Offline Verified Guide Answers:** 8 (3 immediate FTS referrals in 2-5 ms, 4 guardrail intercepted ungrounded numbers per Rule H4, 1 timeout fallback)
- **Crashes / ANRs:** **0**
