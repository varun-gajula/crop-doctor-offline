# On-Device Llama Speed Benchmark (Task M1 & Rule H3)

**Date of Measurement**: 2026-11-14T21:22:00+05:30  
**Device**: Xiaomi Redmi Note 11S / POCO M4 Pro (`22041219PI`)  
**Target Hardware**: 64-bit Arm CPU (`arm64-v8a`), 8 cores (2x Cortex-A76 @ 2.05 GHz + 6x Cortex-A55 @ 2.0 GHz)  
**Total Device RAM**: 6 GB (5,731,384 kB physical)  
**Model**: `meta-llama/Llama-3.2-1B-Instruct` in SpinQuant INT4-EO8 format (`llama-3.2-1b-spinquant.pte`, 1,135,951,488 bytes)  
**Tokenizer**: SentencePiece `tokenizer.model` (2,183,982 bytes)  
**Runtime**: ExecuTorch 0.6.0 (XNNPACK / Arm NEON kernels)  
**Benchmark Suite**: `LlamaSpeedBenchmarkTest` (executed live via `am instrument` on connected physical phone; raw log in [`benchmarks/llama_5cases_speed.json`](file:///c:/crop-doctor-offline/benchmarks/llama_5cases_speed.json)).

---

## 1. 5-Case Benchmark Table

| Case # | Crop | Condition Diagnosed | Severity & Certainty | Prompt Chars | Time to First Token (TTFT) | Total Generation Time | Tokens Generated | Decode Speed (Tokens/s) | Overall Speed (Tokens/s) | Guardrail Compliance |
|---|---|---|---|---|---|---|---|---|---|---|
| **1** | Maize | Common Rust (`maize_common_rust`) | Moderate · Probably | 652 | **4,000 ms** (4.00s) | 15,441 ms | 132 | **11.54 tok/s** | 8.55 tok/s | **PASS** |
| **2** | Tomato | Early Blight (`tomato_early_blight`) | Mild · Sure | 653 | **4,038 ms** (4.04s) | 7,700 ms | 45 | **12.29 tok/s** | 5.84 tok/s | **PASS** |
| **3** | Rice | Leaf Blast (`rice_leaf_blast`) | Severe · Sure | 609 | **3,914 ms** (3.91s) | 15,340 ms | 132 | **11.55 tok/s** | 8.60 tok/s | **PASS** |
| **4** | Cotton | Bacterial Blight (`cotton_bacterial_blight`) | Moderate · Probably | 632 | **3,913 ms** (3.91s) | 6,868 ms | 37 | **12.52 tok/s** | 5.39 tok/s | **PASS** |
| **5** | Chilli | Leaf Curl (`chilli_leaf_curl_virus`) | Severe · Sure | 690 | **4,570 ms** (4.57s) | 8,211 ms | 43 | **11.81 tok/s** | 5.24 tok/s | **PASS** |
| **AVG** | — | **5 Diverse Field Scenarios** | — | **647 chars** | **4,087.0 ms** (4.09s) | **10,712 ms** | **77.8 tok** | **11.94 tok/s** | **6.72 tok/s** | **100% PASS** |

---

## 2. Speed Evaluation & Pass Criteria

- **Official Threshold**: Minimum 2.0 tokens per second.
- **Measured Average Decode Speed**: **11.94 tokens/s** (5.97x faster than the minimum threshold).
- **Measured Average Overall Speed**: **6.72 tokens/s** (including prefill latency).
- **Time to First Token (TTFT)**: Average **4.09 seconds** for $\sim 130\text{ token}$ prompt processing on Arm Cortex-A76 cores.
- **Safety & Guardrail Compliance**: 5/5 cases passed `GuardrailFilter` (Rule H4 & H5).
- **Conclusion**: **PASS**. Llama 3.2 1B-Instruct runs reliably on the 6 GB Arm device without stuttering or memory thrashing.
