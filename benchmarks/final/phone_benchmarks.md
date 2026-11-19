# Final Phone Benchmarks (Task V7)

**Device:** CPH2753 (OPPO/Realme)  
**SoC / Architecture:** arm64-v8a  
**OS:** Android 16 (ColorOS)  
**Total RAM:** 5,757,440 kB (~6 GB reference device, Rule H15)  
**Build Variant:** Debug (Release Baseline Profile improves cold start further)  

---

## 1. Model Load Latencies

| Component | Model File | Size | Load Mode | Load Time (Arm CPU) | Status |
|:---|:---|:---:|:---:|:---:|:---:|
| **Vision Model** | `vision.pte` | 16,927,056 B (16.1 MB) | ExecuTorch C++ runtime (`Module.load`) | **307 ms** | PASS |
| **Advisory Model** | `llama-3.2-1b-spinquant.pte` + `tokenizer.model` | 1,135,951,488 B (1.05 GB) + 2.18 MB | ExecuTorch LLM C++ extension (`LlmModule.load`) | **6,641 ms** (6.64 s) | PASS |

---

## 2. Shutter-to-Result Latency (10 Diagnosed Real Field Photo Runs)

- **Target Budget:** $\le 1,500\text{ ms}$ (1.5 s)
- **Average Shutter-to-Result:** **144.70 ms** (10.3x faster than budget)
- **Min:** 133.68 ms
- **Max:** 204.95 ms
- **Confidence Threshold:** 0.4018166 (Temperature: 1.3302)
- **Status:** **PASS**

### Per-Stage Breakdown (Averages across 10 diagnosed runs)

| Pipeline Stage | Average Latency | Description |
|:---|:---:|:---|
| 1. Leaf Validation | 7.49 ms | Green pixel fraction & blur check (`LeafValidator`) |
| 2. Preprocessing | 62.86 ms | Bilinear resize 512x512 RGB + ImageNet mean/std normalization |
| 3. Vision Inference | 68.07 ms | ExecuTorch forward pass on Arm CPU (20-class output) |
| 4. Calibration & Post-proc | 1.07 ms | Temperature scaling ($T=1.3302$), softmax, confidence check |
| 5. Lesion Area Estimation | 5.09 ms | Color segmentation in HSV space for damage quantification |
| 6. Rules Engine | 0.12 ms | Severity band calculation & Tier routing (Natural vs Chemical) |
| 7. Grounded KB Retrieval | 0.01 ms | O(1) indexed lookup from verified agronomy database |
| **Total Full Pipeline** | **144.70 ms** | **End-to-end shutter to actionable advice screen** |

*Note on Rejections:* In accordance with Rule H11 and requirements, candidate images with confidence $< 0.4018$ were rejected as "Can't tell" (routed to extension officer) with zero hallucination.

---

## 3. Peak Memory (PSS) Compliance (Rule H15: 6 GB Phone Rule)

| Operational State | Measured Total PSS | Target Budget | Native Heap PSS | Java Heap PSS | Status |
|:---|:---:|:---:|:---:|:---:|:---:|
| **App + Vision Model (Without Llama)** | **135.5 MB** (135,552 KB) | $\le 500\text{ MB}$ | 17.1 MB | 19.3 MB | **PASS** (73% headroom) |
| **App + Vision + Llama 1B Generating** | **1.52 GB** (1,593,611 KB) | $\le 2.5\text{ GB}$ | 801.0 MB | 2.1 MB | **PASS** (39% headroom) |

---

## 4. Cold Start Latency (5 Consecutive Cold Starts)

Measured via `adb shell am start -W -n com.cropdoctor/.MainActivity`:

| Run | Launch State | TotalTime | WaitTime | Status |
|:---:|:---:|:---:|:---:|:---:|
| Run 1 | COLD | 1,921 ms | 1,954 ms | PASS |
| Run 2 | COLD | 1,739 ms | 1,750 ms | PASS |
| Run 3 | COLD | 1,742 ms | 1,762 ms | PASS |
| Run 4 | COLD | 1,772 ms | 1,783 ms | PASS |
| Run 5 | COLD | 1,729 ms | 1,740 ms | PASS |
| **Average** | **COLD** | **1,780.6 ms** (~1.78 s) | **1,797.8 ms** | **PASS ($\le 2.0\text{ s}$ UX budget)** |

---

## 5. On-Device Llama Advisory Performance (5 Grounded Cases)

- **Model:** `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte`
- **Average Decode Speed:** **10.53 tokens/second** on Arm CPU
- **Average Time to First Token (TTFT):** **1,995 ms** (~2.0 s)
- **Guardrail Pass Rate:** **100% (5/5)** (No unapproved chemicals or doses)
- **Sample Decode Speeds:**
  - Case 1 (Maize Rust): 9.34 tok/s (132 tokens)
  - Case 2 (Tomato TYLCV): 10.43 tok/s (35 tokens)
  - Case 3 (Rice Leaf Blast): 11.08 tok/s (30 tokens)
  - Case 4 (Cotton Bacterial Blight): 11.32 tok/s (39 tokens)
  - Case 5 (Chilli Leaf Curl): 10.45 tok/s (114 tokens)
