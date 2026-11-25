# Benchmarks and Evaluation Results (benchmarks/)

This directory stores raw measurement logs, evaluation outputs, and hardware benchmark files supporting the final project report.

The most important files are `final/metrics.json` (vision model test metrics) and `aiagricare_real_field_results.json` (live on-device field test results).

### Benchmark Files and Supported Report Numbers:
- `final/metrics.json` (and `metrics.json`): Supports 97.95% grouped test accuracy, 98.18% macro accuracy, and 66.1% PlantDoc noisy stress accuracy.
- `aiagricare_real_field_results.json`: Supports real-device 28/28 crop detection yield and 27/28 top-1 field diagnosis accuracy.
- `classwise_and_power_results.json`: Supports ~460 ms per-scan vision latency and 139.55 mJ energy consumption per scan on POCO M4 5G.
- `memory_summary.md` (and `mem_d_llama_streaming_peak.txt`): Supports 1,634 MB peak RAM during active on-device Llama streaming.
- `speed_summary.md` (and `speed_summary_real_leaves.json`): Supports Llama 3.2 1B time to first token (9,680.2 ms).
- `aapt2_permissions.txt`: Confirms 0 INTERNET permissions (100% offline verification).
- `final/split_v2.json`: Documented grouped split counts preventing duplicate image leakage across train, val, and test.
- `final/confusion_matrix.png` & `final/confusion_matrix.csv`: Complete 15x15 class evaluation confusion matrix.
- `fallback_rate.md`: Documents 100% safe fallback to local knowledge base when RAM is constrained.
- `d5b_pest_audit.md`, `d6_class_audit.md`, `d7_checkpoint.md`: Data provenance and single-source dataset integrity audits.
