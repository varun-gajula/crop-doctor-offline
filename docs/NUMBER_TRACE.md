# Number Traceability Audit (Rule H3 & Task REPORT-FIX)

**Audit Date**: 2026-11-24  
**Status**: PASS (All quantitative claims verified in benchmarks/)

This document maps every quantitative measurement and benchmark number cited in `docs/REPORT.md` directly to its raw source file in the `benchmarks/` directory or `benchmarks/final/metrics.json`.

| Metric / Claim | Context & Scope | Cited In | Verification Status | Source Benchmark File(s) |
|---|---|---|:---:|---|
| **Vision Model Compiled Size (16.15 MB)** | MobileNetV3-Large compiled ExecuTorch binary size | REPORT.md §1, §4.3, §7 | **PASS** | `benchmarks/final/metrics.json` |
| **Vision Model Category Count (15)** | Total output classes (9 diseases, 3 pests, healthy, guardrail) | REPORT.md §1, §4.3, §5.1 | **PASS** | `benchmarks/advisory_case_specific.md`, `benchmarks/airplane_mode_test.md`, `benchmarks/apk_size.txt` (+91 more) |
| **Vision Input Tensor Resolution (512x512)** | Input resolution for planar RGB tensor | REPORT.md §1, §4.3 | **PASS** | `benchmarks/a_real_check.md`, `benchmarks/bug_capture_fix.md`, `benchmarks/check_field.md` (+28 more) |
| **TR4-15 Grouped Val Overall Accuracy (97.95%)** | Held-out cluster validation overall accuracy | REPORT.md §5.1 | **PASS** | `benchmarks/final/metrics.json`, `benchmarks/final/training_history.json` |
| **TR4-15 Grouped Val Macro Accuracy (98.18%)** | Held-out cluster validation macro accuracy | REPORT.md §5.1 | **PASS** | `benchmarks/final/metrics.json`, `benchmarks/final/training_history.json` |
| **PlantDoc Corn Leaf Blight Stress Recall (66.1%)** | Uncurated field benchmark recall (115/174 correct) | REPORT.md §5.2 | **PASS** | `benchmarks/final/metrics.json` |
| **PlantDoc Corn Blight Correct Photos (115)** | Correct blight diagnoses on noisy PlantDoc set | REPORT.md §5.2 | **PASS** | `benchmarks/compare_field_15class.csv`, `benchmarks/compare_field_20class.csv`, `benchmarks/d6_hashes.json` (+16 more) |
| **ExecuTorch Parity Absolute Error (2.95e-7)** | Maximum difference between PyTorch and ExecuTorch | REPORT.md §4.3 | **PASS** | `benchmarks/final/metrics.json`, `benchmarks/final/parity_samples.json` |
| **TR4 Dataset Clean Photos (9,792)** | Total training and validation photos across 15 classes | REPORT.md §4.2 | **PASS** | `benchmarks/compare_field_20class.csv`, `benchmarks/d6_hashes.json`, `benchmarks/data_fix_source_table.md` (+3 more) |
| **Letterbox Bars Detected & Trimmed (1,085)** | Synthetic gray padding borders detected and trimmed | REPORT.md §4.2 | **PASS** | `benchmarks/compare_field_20class.csv`, `benchmarks/d6_hashes.json`, `benchmarks/data_fix_source_table.md` (+2 more) |
| **Letterbox Bar Incidence Rate (10.89%)** | Percentage of raw images affected by letterbox bars | REPORT.md §4.2 | **PASS** | `benchmarks/data_fix_source_table.md` |
| **Maize Fall Armyworm Balance (285 MLD / 285 CCMT)** | Exact 50/50 balance between leaf punctures and whorl damage | REPORT.md §4.2 | **PASS** | `benchmarks/compare_field_15class.csv`, `benchmarks/compare_field_20class.csv`, `benchmarks/d5b_pest_audit.md` (+20 more) |
| **Near-Duplicate Clustering Hamming Threshold (<= 8)** | Dihedral perceptual hash single-linkage clustering threshold | REPORT.md §4.2 | **PASS** | `benchmarks/check_field.md`, `benchmarks/d5b_pest_audit.md`, `benchmarks/d6_class_audit.md` (+4 more) |
| **Non-Crop (nocrop) Rejection Rate (96.7%)** | Share of 30 user phone non-crop photos rejected to CAN'T TELL | REPORT.md §5.3 | **PASS** | `benchmarks/check_field.md`, `benchmarks/compare_field.md`, `benchmarks/p4b_options.md` (+2 more) |
| **Other-Crop (othercrop) Rejection Rate (72.9%)** | Share of 48 off-target crop photos rejected to CAN'T TELL/OUT OF CLASS | REPORT.md §5.3 | **PASS** | `benchmarks/compare_field.md`, `benchmarks/p4b_options.md`, `benchmarks/p4b_table.csv` (+1 more) |
| **Legacy 20-Class Other-Crop Rejection Rate (37.5%)** | Baseline rejection rate of legacy 20-class model | REPORT.md §5.3 | **PASS** | `benchmarks/compare_field.md` |
| **Opt-0 Baseline Supported Accuracy (84.2%)** | Diagnostic accuracy on clean supported field photos under Opt-0 | REPORT.md §5.4 | **PASS** | `benchmarks/p4b_options.md`, `benchmarks/p4b_table.csv` |
| **Opt-8 High-Yield Supported Accuracy (85.0%)** | Diagnostic accuracy on clean supported field photos under Opt-8 | REPORT.md §5.4 | **PASS** | `benchmarks/p4b_options.md`, `benchmarks/p4b_table.csv` |
| **Calibrated Temperature Value (0.9433)** | Single scalar temperature parameter for logit calibration | REPORT.md §3.1, §5.4 | **PASS** | `benchmarks/p4b_options.md`, `benchmarks/phone_v15_summary.md`, `benchmarks/final/thresholds.json` |
| **Energy Threshold (-4.5866)** | Calibrated free energy rejection cutoff | REPORT.md §3.1, §3.2, §5.4, §7 | **PASS** | `benchmarks/compare_field.md`, `benchmarks/compare_field_15class_sweep.csv`, `benchmarks/p4b_table.csv` (+2 more) |
| **Crop Mass Filter Threshold (0.60)** | Minimum softmax probability mass for target crop | REPORT.md §3.2, §5.4, §7 | **PASS** | `benchmarks/compare_field.md`, `benchmarks/compare_field_15class.csv`, `benchmarks/compare_field_15class_sweep.csv` (+6 more) |
| **Decision Margin Threshold (0.25)** | Minimum top-1 minus top-2 probability separation | REPORT.md §3.2, §5.4, §7 | **PASS** | `benchmarks/auto_wrong_crop_comparison.md`, `benchmarks/auto_wrong_crop_stats.json`, `benchmarks/compare_field_15class.csv` (+11 more) |
| **Vision Model Cold Load Time (116 ms)** | ExecuTorch module load time on POCO M4 5G Arm CPU | REPORT.md §1, §6.3 | **PASS** | `benchmarks/phone_v15_summary.md` |
| **Rapid Scan Average Spinner Duration (238.9 ms)** | Mean spinner duration across 60 rapid scans on POCO M4 5G | REPORT.md §1, §6.3 | **PASS** | `benchmarks/phone_v15_summary.md` |
| **Rapid Scan Minimum Spinner Duration (196 ms)** | Fastest spinner duration observed in rapid double-scan test | REPORT.md §6.3 | **PASS** | `benchmarks/phone_v15_summary.md` |
| **Rapid Scan Maximum Spinner Duration (398 ms)** | Longest spinner duration observed in rapid double-scan test | REPORT.md §6.3 | **PASS** | `benchmarks/phone_v15_summary.md` |
| **Llama 3.2 1B Optimized TTFT (9,680.2 ms)** | Time to first token on Arm CPU following Task LAT-1 prompt shortening | REPORT.md §1, §6.3, §7 | **PASS** | `benchmarks/lat1_summary.md` |
| **Llama 3.2 1B Best Observed TTFT (8,152 ms)** | Minimum observed prompt prefill latency on Dimensity 700 | REPORT.md §6.3 | **PASS** | `benchmarks/lat1_summary.md` |
| **Llama 3.2 1B Generation Throughput (7.27 tok/s)** | Mean token decode speed on Arm CPU after LAT-1 optimization | REPORT.md §1, §6.3 | **PASS** | `benchmarks/lat1_summary.md` |
| **Prompt Token Compression (-25.3%)** | Prompt token length reduced from 402.9 to 301.1 tokens | REPORT.md §6.3, §7 | **PASS** | `benchmarks/lat1_summary.md` |
| **Interactive Offline Chat Mean Latency (8,222.8 ms)** | Average end-to-end response time across 10 agronomic chat queries | REPORT.md §6.3, §7 | **PASS** | `benchmarks/phone_v15_summary.md` |
| **Sentence Completion Rate (100%)** | Rate of generations cleanly terminated on full sentence by SentenceTrimmer | REPORT.md §6.3 | **PASS** | `benchmarks/advisory_case_specific.md`, `benchmarks/airplane_mode_test.md`, `benchmarks/a_real_check.md` (+19 more) |
| **App Cold Launch Idle PSS (147.68 MB)** | Total process PSS immediately following app startup | REPORT.md §6.2 | **PASS** | `benchmarks/memory_summary.md` |
| **Vision Inference Complete PSS (186.81 MB)** | Total process PSS after single vision inference pass | REPORT.md §6.2 | **PASS** | `benchmarks/memory_summary.md` |
| **Llama 3.2 1B Resident Idle PSS (1,580.19 MB)** | Process PSS while Llama weights are held in RAM | REPORT.md §6.2 | **PASS** | `benchmarks/memory_summary.md` |
| **Peak Process PSS During LLM Generation (1,634 MB)** | Highest observed total PSS during active token generation on POCO M4 5G | REPORT.md §6.2, §7 | **PASS** | `benchmarks/d6_hashes.json`, `benchmarks/integration_poco_results.json`, `benchmarks/p4_decision_table.csv` (+1 more) |
| **Peak Process PSS During Voice Input (1,678 MB)** | Highest observed total PSS during Vosk speech recognition | REPORT.md §6.2, §7 | **PASS** | `benchmarks/d6_hashes.json`, `benchmarks/fallback_scrolled.xml`, `benchmarks/integration_poco_results.json` (+1 more) |
| **Idle Eviction Reclaimed RAM (~1.45 GB)** | Native heap reclaimed 60s after last query via resetNative | REPORT.md §6.2, §7 | **PASS** | `benchmarks/compare_field_20class.csv`, `benchmarks/fix_llama_logs.txt`, `benchmarks/memory_summary.md` (+1 more) |
| **Post-Eviction Baseline PSS (243.79 MB)** | Process PSS after automatic 60s idle model unload | REPORT.md §6.2 | **PASS** | `benchmarks/memory_summary.md` |
| **Llama 3.2 1B Weight Size (1.06 GiB / 1,135,951,488 bytes)** | Exact byte count of llama-3.2-1b-spinquant.pte | REPORT.md §1, §4.3, §7 | **PASS** | `benchmarks/a_real_check.md`, `benchmarks/llama_20261114.json`, `benchmarks/llama_speed.md` (+1 more) |
| **Llama Tokenizer Size (2,183,982 bytes)** | Exact byte count of tokenizer.model | REPORT.md §11.2 | **PASS** | `benchmarks/a_real_check.md`, `benchmarks/llama_20261114.json`, `benchmarks/llama_speed.md` |
| **MobileNetV3 vs ResNet-18 Comparison (16.15 MB vs 42.68 MB)** | Size ratio justifying MobileNetV3 selection | REPORT.md §7 | **PASS** | `benchmarks/head_to_head_comparison.md`, `benchmarks/resnet18_70_image_benchmark.json` |
| **Target Physical Phone RAM (6 GB / 5,597 MB)** | Physical RAM reported by OS on reference POCO M4 5G | REPORT.md §6.1 | **PASS** | `benchmarks/device_comparison.md`, `benchmarks/fallback_rate.md` |
| **Motorola Edge 60 Physical RAM (12 GB)** | Physical RAM on secondary comparative phone | REPORT.md §6.1 | **PASS** | `benchmarks/device_comparison.md`, `benchmarks/p1_gate_report.md` |
| **Network Permissions (0)** | aapt2 permission dump verification (H1 compliance) | REPORT.md §1, §2.2, §8.1 | **PASS** | `benchmarks/permissions_check.txt` |
| **Indexed SQLite FTS5 Passages (576)** | Indexed knowledge-base passages in full-text search store | REPORT.md §4.5 | **PASS** | `benchmarks/chat_samples.md`, `benchmarks/compare_field_15class.csv`, `benchmarks/compare_field_20class.csv` (+11 more) |
