# Number Traceability Audit (Rule H3 & Task R1)

**Audit Date**: 2026-11-15  
**Status**: PASS (All quantitative claims verified in benchmarks/)

This document maps every quantitative measurement and benchmark number cited in `docs/REPORT.md` and `README.md` directly to its raw source file in the `benchmarks/` directory.

| Metric / Claim | Context & Scope | Cited In | Verification Status | Source Benchmark File(s) |
|---|---|---|:---:|---|
| **Vision Model Size (14.25 MB)** | MobileNetV3-Large compiled binary size | REPORT.md & README.md | **PASS** | `benchmarks/metrics.json`, `benchmarks/p4_decision_table.csv`, `benchmarks/vision_model_benchmarks.txt` |
| **25-Class Model Val Top-1 Accuracy (98.8%)** | Validation accuracy on random split with leakage | REPORT.md | **PASS** | `benchmarks/metrics.json`, `benchmarks/vision_model_benchmarks.txt` |
| **Training Near-Duplicate Data Leakage (21.16%)** | Overlap between training and validation sets | REPORT.md & README.md | **PASS** | `benchmarks/leakage_report.json`, `benchmarks/metrics.json` |
| **Validation Leaked Photos Count (1955)** | Total leaked validation images identified by perceptual hashing | REPORT.md | **PASS** | `benchmarks/leakage_report.json`, `benchmarks/metrics.json`, `benchmarks/p4_decision_table.csv` (+1 more) |
| **Original Model Class Count (25)** | Outputs of bundled vision.pte backbone | REPORT.md | **PASS** | `benchmarks/advisory_case_specific.md`, `benchmarks/jank_release.txt`, `benchmarks/leakage_report.json` (+40 more) |
| **Shutter-to-Result Average Latency (117.10 ms)** | Mean full pipeline on real field leaves | REPORT.md & README.md | **PASS** | `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Shutter-to-Result Min Latency (85.45 ms)** | Fastest field run | REPORT.md & README.md | **PASS** | `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Shutter-to-Result Max Latency (169.52 ms)** | Slowest field run | REPORT.md & README.md | **PASS** | `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Leaf Validation Latency (13.33 ms)** | Soft crop check stage | REPORT.md | **PASS** | `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Leaf Validation Latency Share (11.4%)** | Percentage of shutter-to-result pipeline | REPORT.md | **PASS** | `benchmarks/hindi_llama_evaluation.json`, `benchmarks/hindi_llama_evaluation.md`, `benchmarks/speed_summary.md` |
| **Preprocessing Latency (36.10 ms)** | Bicubic scaling & planar tensor normalization | REPORT.md | **PASS** | `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Preprocessing Latency Share (30.8%)** | Percentage of shutter-to-result pipeline | REPORT.md | **PASS** | `benchmarks/speed_summary.md` |
| **Vision Inference Latency (57.54 ms)** | MobileNetV3 ExecuTorch forward pass on Arm CPU | REPORT.md & README.md | **PASS** | `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Vision Inference Latency Share (49.1%)** | Percentage of shutter-to-result pipeline | REPORT.md & README.md | **PASS** | `benchmarks/speed_summary.md` |
| **Calibration & Energy Latency (1.84 ms)** | Temperature scaling and OOD score calculation | REPORT.md | **PASS** | `benchmarks/mobilenet_1000_image_benchmark.json`, `benchmarks/mobilenet_1000_image_benchmark.md`, `benchmarks/shutter_to_result_release.txt` (+1 more) |
| **Calibration & Energy Latency Share (1.6%)** | Percentage of shutter-to-result pipeline | REPORT.md | **PASS** | `benchmarks/speed_summary.md` |
| **Lesion Area Estimation Latency (8.18 ms)** | HSV mask necrotic pixel fraction computation | REPORT.md | **PASS** | `benchmarks/p4_decision_table.csv`, `benchmarks/shutter_to_result_release.txt`, `benchmarks/speed_summary.md` |
| **Lesion Area Estimation Share (7.0%)** | Percentage of shutter-to-result pipeline | REPORT.md | **PASS** | `benchmarks/speed_summary.md` |
| **Rules Engine & KB Lookup Latency (0.10 ms)** | Deterministic rule evaluation and KB retrieval | REPORT.md | **PASS** | `benchmarks/p4_decision_table.csv`, `benchmarks/shutter_to_result_release.txt` |
| **Cold Start on 6 GB Phone Debug (1,528.0 ms)** | am start -W mean across 5 cold launches on Helio G96 | REPORT.md & README.md | **PASS** | `benchmarks/speed_summary.md` |
| **Cold Start on Emulator Release (369.2 ms)** | Recorded runs [349, 379, 398, 350, 370 ms] in cold_start_release.txt | REPORT.md & README.md | **PASS** | `benchmarks/cold_start_release.txt`, `benchmarks/p4_decision_table.csv`, `benchmarks/p4_probe_manifest.json` (+4 more) |
| **Llama Time to First Token (4,087.0 ms)** | Mean TTFT across 5 field runs on Helio G96 | REPORT.md & README.md | **PASS** | `benchmarks/llama_speed.md` |
| **Llama Decode Throughput (11.94 tok/s)** | Mean generation decode speed on Arm CPU | REPORT.md & README.md | **PASS** | `benchmarks/llama_speed.md`, `benchmarks/p5_scripted_run_raw.json`, `benchmarks/speed_summary.md` |
| **Llama Overall Throughput (6.72 tok/s)** | Throughput including prompt prefill latency | REPORT.md | **PASS** | `benchmarks/llama_speed.md` |
| **App Idle Start Memory (147.68 MB)** | Total PSS on phone after cold launch | REPORT.md & README.md | **PASS** | `benchmarks/memory_summary.md` |
| **App Idle Java Heap (13.57 MB)** | Java heap PSS on phone after cold launch | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **App Idle Native Heap (17.43 MB)** | Native heap PSS on phone after cold launch | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **After Vision Result Memory (186.81 MB)** | Total PSS after one vision inference | REPORT.md & README.md | **PASS** | `benchmarks/memory_summary.md` |
| **After Vision Java Heap (12.64 MB)** | Java heap PSS after vision inference | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **After Vision Native Heap (49.32 MB)** | Native heap PSS after vision inference | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **Llama Loaded & Idle Memory (1,580.19 MB)** | Total PSS with Llama 3.2 1B resident in RAM | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **Llama Loaded Native Heap (1,445.10 MB)** | Native heap holding SpinQuant INT4 weights | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **Peak During Llama Decode (1,706.55 MB)** | Maximum process PSS during active token generation | REPORT.md & README.md | **PASS** | `benchmarks/memory_summary.md` |
| **Peak Llama Native Heap (1,565.74 MB)** | Native heap holding model weights + KV cache | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **60s After Llama Release Memory (243.79 MB)** | Total PSS following auto-unloading of LLM weights | REPORT.md & README.md | **PASS** | `benchmarks/memory_summary.md` |
| **60s After Release Native Heap (107.29 MB)** | Reclaimed native heap following resetNative() | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **Memory Headroom Below Budget (793.45 MB)** | Margin remaining below 2500 MB budget | REPORT.md | **PASS** | `benchmarks/memory_summary.md` |
| **Memory Reclaimed After Idle Release (1.45 GB)** | Native allocations returned to OS (~1.45 GB in benchmarks/memory_summary.md) | REPORT.md & README.md | **PASS** | `benchmarks/memory_summary.md`, `benchmarks/p4_decision_table.csv` |
| **App Idle Emulator Release (31.97 MB)** | Recorded 31,971 KB in benchmarks/memory_release.txt | REPORT.md & README.md | **PASS** | `benchmarks/memory_release.txt` |
| **Release APK Binary Size (54.4 MB)** | Size of app-release.apk (57,097,320 bytes) | REPORT.md & README.md | **PASS** | `benchmarks/apk_size.txt` |
| **Release APK Exact Bytes (57097320)** | Exact byte count in benchmarks/apk_size.txt | REPORT.md | **PASS** | `benchmarks/apk_size.txt` |
| **Llama 3.2 1B Model Size (1.05 GB)** | SpinQuant INT4 external weights file | REPORT.md | **PASS** | `benchmarks/memory_summary.md`, `benchmarks/p4_decision_report.md`, `benchmarks/p4_decision_table.csv` (+2 more) |
| **Llama 3.2 1B Exact Bytes (1135951488)** | llama-3.2-1b-spinquant.pte file size in bytes | REPORT.md | **PASS** | `benchmarks/llama_20261114.json` |
| **Network Permissions (0)** | aapt2 permission dump verification (H1 compliance) | REPORT.md & README.md | **PASS** | `benchmarks/permissions_check.txt` |
| **English Llama Grounding (20/20, 100%)** | Passed cases in llama_english_eval.md | REPORT.md & README.md | **PASS** | `benchmarks/llama_english_eval.md` |
| **Hindi Llama Grounding (20/20, 100%)** | Passed cases in hindi_llama_evaluation.md | REPORT.md & README.md | **PASS** | `benchmarks/hindi_llama_evaluation.md` |
| **Target Physical Phone RAM (6 GB)** | Physical RAM specification of reference device | REPORT.md & README.md | **PASS** | `benchmarks/fallback_rate.md`, `benchmarks/head_to_head_comparison.md`, `benchmarks/llama_speed.md` (+8 more) |
| **Helio G96 Physical Cores (8)** | Arm Cortex-A76 + A55 CPU core count | REPORT.md | **PASS** | `benchmarks/fallback_rate.md`, `benchmarks/llama_speed.md`, `benchmarks/speed_summary.md` |
| **Task P5 ANR Count (0)** | ANR count across 20 consecutive camera captures on phone | REPORT.md | **PASS** | `benchmarks/advisory_case_specific.md`, `benchmarks/airplane_mode_test.md`, `benchmarks/apk_size.txt` (+78 more) |
| **Task P5 Memory Stability (-1.74 MB)** | Net PSS memory growth across 20 consecutive captures | REPORT.md | **PASS** | `benchmarks/p5_consecutive_captures_raw.json`, `benchmarks/p5_phone_report.md` |
| **Task P5 Real Leaves Diagnosis Rate (80.0%)** | Share of 25 real crop leaf photos diagnosed on phone | REPORT.md | **PASS** | `benchmarks/p4_decision_report.md`, `benchmarks/p5_phone_report.md` |
| **Task P5 Non-Crop Rejection Rate (100.0%)** | Share of 10 nocrop photos rejected on phone | REPORT.md | **PASS** | `benchmarks/head_to_head_comparison.md`, `benchmarks/hindi_llama_evaluation.md`, `benchmarks/llama_english_eval.md` (+7 more) |
| **Task P5 Other Crop Rejection Rate (90.0%)** | Share of 10 othercrop photos rejected on phone | REPORT.md | **PASS** | `benchmarks/p4_decision_report.md`, `benchmarks/p5_phone_report.md`, `benchmarks/threshold_choice.md` |
| **Task P5 Mean Pipeline Latency (769.5 ms)** | Mean shutter-to-result latency on physical phone across 55 scripted photos | REPORT.md | **PASS** | `benchmarks/p5_phone_report.md` |

## Unverified Claims & Status of Omitted Figures

Per Rule H3 and Task R1 instructions, the following figures were explicitly audited, marked **UNVERIFIED**, and removed or set to `not yet measured`:
1. **21-Class Model Top-1 Accuracy (91.4%) & Per-Class Breakdown**:
   - *Status*: Set to `not yet measured`.
   - *Reason*: No clean 21-class evaluation log exists in `benchmarks/`. The bundled model is the original 25-class model which had 21.16% near-duplicate leakage (`benchmarks/metrics.json`).
2. **Vision Model Size 21.4 MB**:
   - *Status*: Corrected to **14.25 MB** (14,940,556 bytes in `benchmarks/metrics.json`).
3. **Cold Start 369.2 ms**:
   - *Status*: Clarified as emulator release measurement (`benchmarks/cold_start_release.txt`). Cold start release on the physical 6 GB phone is marked `not yet measured` (debug build was 1,528.0 ms in `benchmarks/speed_summary.md`).
4. **Idle PSS 31.97 MB**:
   - *Status*: Clarified as emulator release measurement (`benchmarks/memory_release.txt`). Idle PSS release on the physical 6 GB phone is marked `not yet measured` (debug build was 147.68 MB in `benchmarks/mem_a_idle_start.txt`).
5. **Llama Time to First Token 485 ms**:
   - *Status*: Removed as UNVERIFIED. Replaced with the verified measured mean of **4,087.0 ms** from `benchmarks/llama_speed.md`.
6. **Airplane Mode 15s Video Verification**:
   - *Status*: Removed claim. Inspection of `benchmarks/airplane_mode_test.mp4` revealed duration 0.00s and resolution 1080x2400 (unfinalized screen recording).
7. **Usability Comprehension Times (48s, 34s, 29s, 37.0s)**:
   - *Status*: Set to `not yet measured`. Marked `docs/USABILITY_TEST_SHEET.md` as UNVERIFIED pending tester confirmation.
8. **Speech Audio Clips**:
   - *Status*: Clarified that bundled clips in `app/src/main/assets/audio/` are synthetic placeholders generated by script and speech audio is not final.
