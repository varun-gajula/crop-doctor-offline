# Technical Report: Crop Doctor Offline
**Arm Bharat AI-SoC Challenge 2026-27**  
**Problem Statement PS1**: Offline Crop Disease/Pest Detection and Advisory for Farmers  
**Target Hardware**: 64-bit Arm CPU (`arm64-v8a`), 6 GB RAM reference Android phone  
**Repository**: https://github.com/varun-gajula/crop-doctor-offline  
**Deliverable**: Technical Report (Section D4)  

---

## 1. Problem and Users

Smallholder farmers in rural India face recurring crop yield losses due to delayed or inaccurate diagnosis of foliar diseases and insect pests. Existing commercial solutions rely on remote cloud APIs that require active cellular data or high-speed Wi-Fi. In real farming environments across Telangana, Andhra Pradesh, and semi-arid regions of India, connectivity is either absent or highly intermittent. Furthermore, cloud-dependent apps introduce round-trip latency, ongoing API server costs, and privacy concerns regarding farm imagery.

The primary users of Crop Doctor are:
1. **Smallholder Farmers**: Cultivators managing small landholdings (typically 1–5 acres) of Rice, Cotton, Chilli, Tomato, or Maize, often with limited formal literacy and speaking regional languages (Telugu or Hindi). They require immediate, high-contrast visual diagnosis, spoken voice guidance, and non-technical agronomic advice within under 60 seconds.
2. **Village Agriculture Extension Workers & Krishi Sahayaks**: Field officers visiting remote village clusters who need rapid, reliable on-device diagnostic confirmation, standardized treatment guidelines, and shareable case reports that can be forwarded to agricultural university experts or the Kisan Call Centre without manual data re-entry.

---

## 2. Architecture

Crop Doctor is built around a decoupled two-lane edge architecture running 100% locally on the device's Arm CPU without any network access:

```
                          [ Leaf / Pest Photo ]
                                    │
                                    ▼
       ┌────────────────────────────────────────────────────────┐
       │             LANE 1: VISION INFERENCE (LOCAL)           │
       │  MobileNetV3-Large · ExecuTorch 0.6.0 (XNNPACK Arm64)  │
       │  Input: 320x320 RGB planar tensor                      │
       │  Output: Class ID (0-20), Softmax Confidence, Energy   │
       └────────────────────────────┬───────────────────────────┘
                                    │
                                    ▼
       ┌────────────────────────────────────────────────────────┐
       │         DETERMINISTIC AGRONOMIC RULES ENGINE           │
       │  - Severity: Leaf lesion area + Field spread question  │
       │  - Urgency: Early warning / Act this week / Act now   │
       │  - Certainty: Sure / Probably / Not sure               │
       └────────────────────────────┬───────────────────────────┘
                                    │
                                    ▼
       ┌────────────────────────────────────────────────────────┐
       │           LOCAL TRILINGUAL KNOWLEDGE BASE              │
       │  - Verified agronomic entries (PJTSAU, ANGRAU, ICAR)   │
       │  - Natural/traditional & CIB&RC-registered chemicals   │
       │  - English, Telugu (తెలుగు), Hindi (हिन्दी)            │
       └────────────────────────────┬───────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
       [ Result UI (< 1.5s) ]          ┌──────────────────────────┐
       - Diagnosis & Urgency Badge     │ LANE 2: ADVISORY REASON  │
       - Natural / Chemical Tiers      │ Llama 3.2 1B-Instruct    │
       - Emergency Referral Action     │ ExecuTorch 0.6.0 INT4    │
       - Offline Audio / Read-aloud    │ Streamed plain-language  │
                                       │ explanation (< 500 MB)   │
                                       └────────────┬─────────────┘
                                                    │
                                                    ▼
                                       ┌──────────────────────────┐
                                       │ IN-PROCESS SAFETY GUARD  │
                                       │ - Scrubs hallucinated    │
                                       │   dosages & products     │
                                       │ - Fallback: Offline guide│
                                       └──────────────────────────┘
```

The system separates perceptual classification from conversational reasoning:
- **Lane 1 (Vision)** executes synchronously upon shutter release, delivering class identification, certainty band, lesion severity, and sourced treatment steps within 1.5 seconds.
- **Lane 2 (Advisory Reasoning)** loads conditionally on-demand to stream plain-word explanations. If available device memory is constrained or if the safety guardrail detects discrepancies, the advisory lane cleanly falls back to the deterministic Knowledge Base (*"Offline guide"*).

---

## 3. Vision Pipeline

### 3.1 Data and Provenance
The model is trained on a curated corpus of 21 agricultural classes spanning 5 core South Indian crops (Rice, Cotton, Chilli, Tomato, Maize). Training data incorporates open-access research datasets licensed under Creative Commons Attribution (CC BY 4.0):
- **PlantVillage Dataset** (David P. Hughes & Marcel Salathé, Penn State University, CC BY 4.0).
- **Mendeley Data Rice & Cotton Disease Repositories** (CC BY 4.0).
- **PlantDoc Real-World Field Benchmark** (IIT Delhi, CC BY 4.0).
- **IP102 Agricultural Pest Dataset** (cited as academic reference for pest morphology).

Data preparation utilized stratified train/validation/test splits (70/15/15) isolated at the parent folder and capture level to guarantee zero image leakage between training and validation sets.

### 3.2 Model Architecture & Training
- **Backbone**: MobileNetV3-Large with hard-swish activation, selected for optimal FLOPs-to-accuracy ratio on Arm Cortex-A vector pipelines.
- **Input Resolution**: $320 \times 320 \times 3$ RGB. Planar normalization using ImageNet mean `[0.485, 0.456, 0.406]` and standard deviation `[0.229, 0.224, 0.225]`.
- **Training Strategy**: Two-stage transfer learning using PyTorch with AdamW optimizer, cosine annealing learning rate schedule, and standard augmentations (random horizontal/vertical flips, affine rotation $\pm 15^\circ$, color jitter $\pm 0.1$).

### 3.3 Calibration & Out-of-Distribution Handling
To prevent overconfident predictions on unfamiliar leaves or non-crop images:
1. **Temperature Scaling ($T$)**: Post-hoc softmax calibration was fitted with $T = 1.7818$ on validation logits.
2. **Free Energy Out-of-Distribution (OOD) Scoring**: Computes Helmholtz free energy $E(x) = -T \cdot \log \sum_i e^{z_i / T}$ with calibrated energy threshold $E_{\text{thresh}} = -8.0000$ (`app/src/main/assets/thresholds.json`).
3. **Crop-Mass Filtering & Ambiguity Margin**: When a crop is selected by the farmer, within-crop softmax probability mass is required to meet $\ge 0.60$ (`crop_mass_threshold`), and top-1 vs top-2 margin must exceed $0.25$ unless free energy is strongly negative ($< -9.0000$).
4. **Three Clear Outcomes (Decided in Plain Code)**:
   - `DIAGNOSE`: In-crop disease or healthy class accepted above calibrated threshold.
   - `CANT_TELL`: High free energy ($E > -8.0$), low confidence, unvalidated leaf photo, weeds, or unverified disease classes. Rerouted to *"Not sure - ask an extension officer"*.
   - `OUT_OF_CLASS`: Low crop mass ($< 0.60$) or ambiguous borderline margin. Displays *"Out of class - plant does not match"*.

### 3.4 ExecuTorch Export & Optimization
The PyTorch model was exported to ExecuTorch 0.6.0 via:
1. `torch.export.export()` graph tracing.
2. Dialect transformation to Edge IR via `exir.to_edge()`.
3. Graph partitioning with `XnnpackPartitioner()`, binding linear and convolution kernels to Arm NEON vector SIMD instructions.
4. Compiled binary: `vision.pte` (**14.25 MB** in APK assets, verified in `benchmarks/metrics.json` and `benchmarks/vision_model_benchmarks.txt`).

### 3.5 Model Status and Validation Accuracy
The model in use is the original 25-output model (14.25 MB), mapped to 21 classes via `app/src/main/assets/model_class_map.json`. Its validation accuracy (98.8%) was measured on a random split with 21.16% near-duplicate leakage (recorded in `benchmarks/metrics.json`) and must not be read as real accuracy.

Real-world accuracy on held-out field photos across 21 classes is **not yet measured**.
- Overall Top-1 Accuracy: **not yet measured**
- Per-class accuracy across the 21 shipped disease, pest, and healthy classes: **not yet measured**

---

## 4. Advisory Pipeline

### 4.1 Llama 3.2 1B-Instruct via ExecuTorch
The on-device advisory engine executes Meta Llama 3.2 1B-Instruct running locally via ExecuTorch 0.6.0:
- **Weights Format**: SpinQuant INT4 quantized weights (`llama-3.2-1b-spinquant.pte`, **1.05 GB**).
- **Tokenizer**: Official Llama 3.2 `tokenizer.model` (BPE vocabulary).
- **Execution Target**: Multi-threaded Arm CPU execution via XNNPACK backend.
- **Dynamic Deployment**: In accordance with Rule H2 and GitHub's 100 MB file limit, the 1.05 GB model file is loaded from phone external storage (via `adb push` or in-app download), keeping the APK lightweight at 54.4 MB.

### 4.2 Prompt Construction & Grounding
`PromptBuilder.kt` compiles a structured prompt strictly constrained by the vision findings and verified Knowledge Base entry:
```text
System: You are Crop Doctor, an offline farming advisory assistant for Indian farmers.
Explain the crop condition in simple words for a smallholder farmer.
Use ONLY the approved knowledge base information below.
Do NOT invent doses, concentrations, or chemical trade names.
Do NOT change the diagnosed severity or referral recommendation.

Crop: {crop}
Problem: {problem}
Type: {type}
Severity: {severity}
Certainty: {certainty}
Field Spread: {field_spread}
Region: {region}
Approved Advisory Text: {kb_entry_text}
```

### 4.3 In-Process Safety Guardrails (`GuardrailFilter.kt`)
To eliminate LLM hallucination risks:
1. **Zero-Dosage Enforcement**: Intercepts token streams in real-time. Any numeric dosage pattern (e.g. `X ml/L`, `Y kg/ha`) is scrubbed.
2. **Approved Product Whitelist**: Rejects any chemical active ingredient not explicitly listed in the grounded Knowledge Base for that specific crop and disease.
3. **Consistency Verification**: Checks that diagnosed crop and problem names match the model classification.
4. **Offline Guide Fallback**: If the guardrail rejects the generated text or if device RAM is low, the app presents the human-reviewed Knowledge Base text labelled *"Offline guide"*.

### 4.4 Grounding Benchmark Results
- **English Advisory Evaluation** (`benchmarks/llama_english_eval.md`): **20/20 cases passed (100%)** across 5 crops.
- **Hindi Advisory Evaluation** (`benchmarks/hindi_llama_evaluation.md`): **20/20 cases passed (100%)**, exceeding the 18/20 acceptance threshold.

---

## 5. Languages and Knowledge Base

### 5.1 Trilingual Architecture
Crop Doctor supports three mandatory languages:
- **English (en)**
- **Telugu (తెలుగు)** (pivotal language for Telangana and Andhra Pradesh)
- **Hindi (हिन्दी)**

All UI text, navigation, severity chips, and dialogs are 100% localized in `strings.xml`, `values-te/strings.xml`, and `values-hi/strings.xml`, validated by automated unit tests in `LocalizationTest.kt`. Bundled open-source fonts: Noto Sans Telugu and Noto Sans Devanagari (SIL Open Font License).

### 5.2 Rule H6: Telugu Safety Guardrail
Llama 3.2 does not officially support Telugu. Under Rule H6:
- **Telugu is NEVER generated by Llama.**
- Any attempt to pass Telugu text to Llama throws an `IllegalArgumentException` in unit tests (`PromptBuilderTest.kt`, `CaseSpecificAdvisoryTest.kt`).
- All Telugu advisory guidance is rendered directly from human-reviewed Knowledge Base strings.

### 5.3 Knowledge Base Provenance
`knowledge_base.json` covers all 21 shipped classes across two distinct treatment tiers:
- **Natural & Traditional Tier**: Cultural practices, bio-agents (*Trichoderma viride*, *Pseudomonas fluorescens*), neem formulations (Azadirachtin), sanitation, and field drainage.
- **Chemical (Advanced) Tier**: Only CIB&RC-registered active ingredients (e.g. Copper Oxychloride, Mancozeb, Hexaconazole, Chlorantraniliprole) citing official University and Government sources:
  - Professor Jayashankar Telangana State Agricultural University (PJTSAU)
  - Acharya N.G. Ranga Agricultural University (ANGRAU)
  - Indian Council of Agricultural Research (ICAR institutes: IIRR, CICR, IIHR, IIMR)
  - Central Insecticides Board & Registration Committee (CIB&RC)
- **Dosage Safety (Rule H4)**: Doses appear only where verified by a human against official label texts (`dose_verified: true`). Otherwise, the app explicitly states *"follow the product label"*.

### 5.4 Offline Audio Output Status
Offline audio guidance is implemented via `OfflineAudioAdvisor.kt` using bundled Opus clips in `app/src/main/assets/audio/`. The bundled clips are synthetic placeholders generated by a development script (`tools/make_audio_clips.py`) and Telugu/Hindi speech audio is not final. For dynamic explanations or when clips are missing, the app falls back to Android system TextToSpeech (displaying a setup guide if speech voices are not pre-installed).

---

## 6. Safety and Hazard List

| # | Identified Hazard | Potential Impact on Farmer | System Mitigation in Code | Code Location |
|---|---|---|---|---|
| **H-1** | **Misclassification of Disease** | Application of ineffective treatment; financial waste. | Calibrated confidence bands (*Sure / Probably / Not sure*). Low-confidence or out-of-distribution inputs route to *"Can't tell - ask an extension officer"*. | `ExecuTorchClassifier.kt`<br>`ThresholdManager.kt` |
| **H-2** | **Chemical Overdose Hallucination** | Crop scorching, toxic environmental runoff, farmer health risk. | Deterministic dosage gate: Llama prompt explicitly forbids doses; `GuardrailFilter` regex scrubs any numeric dosage pattern; KB shows *"follow the product label"* unless `dose_verified == true`. | `GuardrailFilter.kt`<br>`knowledge_base.json` |
| **H-3** | **Unregistered Chemical Spray** | Regulatory violation, crop contamination, loss of export market. | Strict whitelist: Llama is grounded only on CIB&RC-registered active ingredients present in the verified Knowledge Base entry. | `PromptBuilder.kt`<br>`GuardrailFilter.kt` |
| **H-4** | **Severe Epidemic Over-reliance on AI** | Delayed quarantine; total field devastation. | Severity logic in unit-tested code (`RulesEngine.kt`). When urgency is *Act now*, the Kisan Call Centre referral button (**1800-180-1551**) is prioritized at the top of the screen before any chemical details. | `RulesEngine.kt`<br>`ResultScreen.kt` |
| **H-5** | **Telugu Mistranslation / LLM Drift** | Farmer receives nonsensical or dangerous instructions. | Rule H6: Telugu advisory generation by Llama is strictly prohibited. Telugu text is served 100% from human-reviewed Knowledge Base entries. | `OfflineAudioAdvisor.kt`<br>`CaseSpecificAdvisoryTest.kt` |
| **H-6** | **Out-of-Memory (OOM) Crash on 6 GB Phone** | App crashes during field inspection; data loss. | Strict memory budget (Rule H15): Background model loading; pre-flight `ActivityManager` RAM check; auto-release of Llama native memory after 60 seconds of idle inactivity. | `ExecuTorchLlamaAdvisor.kt` |

---

## 7. Measurements on an Arm Phone

All numbers reported below are measured values recorded on device and preserved in `benchmarks/`. Zero estimated or simulated figures are included (Rule H3).

### 7.1 Hardware Test Environment
- **Device**: Xiaomi Redmi Note 11S / POCO M4 Pro (`22041219PI`)
- **CPU**: 64-bit Arm CPU (`arm64-v8a`), 8 physical cores (MediaTek Helio G96)
- **RAM**: 6 GB physical RAM
- **OS**: Android 14 (API 34)
- **Build Variant**: Release APK with Baseline Profile (`app-release.apk`)

### 7.2 Latency & Speed Benchmark Table

| Measurement Parameter | Target Limit | Measured Value | Source Benchmark Log |
|---|---|---|---|
| **Cold App Startup (Phone Debug)** (`am start -W`) | $\le 2,000\text{ ms}$ | **1,528.0 ms** average (Runs: 2167, 2047, 1723, 865, 838 ms) | `benchmarks/speed_summary.md` |
| **Cold App Startup (Phone Release)** | $\le 2,000\text{ ms}$ | **not yet measured** (Emulator release: 369.2 ms) | `benchmarks/cold_start_release.txt` |
| **Shutter-to-Result (Real Field Leaves)** | $\le 1,500\text{ ms}$ | **117.10 ms** average (Min: 85.45 ms, Max: 169.52 ms) | `benchmarks/speed_summary_real_leaves.json` |
| - *Leaf Validation Stage* | - | 13.33 ms (11.4% of pipeline) | `benchmarks/speed_summary.md` |
| - *Preprocessing Stage* | - | 36.10 ms (30.8% of pipeline) | `benchmarks/speed_summary.md` |
| - *ExecuTorch Vision Inference* | - | 57.54 ms (49.1% of pipeline) | `benchmarks/speed_summary.md` |
| - *Calibration & Energy OOD* | - | 1.84 ms (1.6% of pipeline) | `benchmarks/speed_summary.md` |
| - *Lesion Area Estimation* | - | 8.18 ms (7.0% of pipeline) | `benchmarks/speed_summary.md` |
| - *Rules Engine & KB Lookup* | - | 0.10 ms (< 0.1% of pipeline) | `benchmarks/speed_summary.md` |
| **Llama 3.2 1B Time to First Token** | - | **4,087.0 ms** average TTFT (Runs: 4000, 4038, 3914, 3913, 4570 ms) | `benchmarks/llama_speed.md` |
| **Llama 3.2 1B Generation Throughput** | $\ge 2.0\text{ tok/s}$ | **11.94 tokens/s** average decode (Overall: 6.72 tokens/s) | `benchmarks/llama_speed.md` |
| **UI Frame Latency (50th Percentile)** | $\le 16.6\text{ ms}$ | **not yet measured** (Emulator release: 81ms total / 14ms GPU) | `benchmarks/jank_release.txt` |

### 7.3 Memory (PSS) Footprint Table (Rule H15)

| Operational Scenario | Target Limit | Measured Total PSS | Java Heap | Native Heap | Source Benchmark Log |
|---|---|---|---|---|---|
| **App Idle After Start (Phone Debug)** | $\le 500\text{ MB}$ | **147.68 MB** | 13.57 MB | 17.43 MB | `benchmarks/mem_a_idle_start.txt` |
| **App Idle After Start (Phone Release)** | $\le 500\text{ MB}$ | **not yet measured** (Emulator release: 31.97 MB) | 4.40 MB | 12.82 MB | `benchmarks/memory_release.txt` |
| **After One Vision Result (Phone Debug)** | $\le 500\text{ MB}$ | **186.81 MB** | 12.64 MB | 49.32 MB | `benchmarks/mem_b_after_vision.txt` |
| **Llama Loaded & Idle (Phone Debug)** | $\le 2,500\text{ MB}$ | **1,580.19 MB** | 12.57 MB | 1,445.10 MB | `benchmarks/mem_c_llama_loaded_idle.txt` |
| **Peak During Llama Decode (Phone Debug)** | $\le 2,500\text{ MB}$ | **1,706.55 MB** | 14.79 MB | 1,565.74 MB | `benchmarks/mem_d_llama_streaming_peak.txt` |
| **60s After Llama Release (Phone Debug)** | $\le 500\text{ MB}$ | **243.79 MB** | 12.96 MB | 107.29 MB | `benchmarks/mem_e_60s_after_release.txt` |

*Conclusion*: At peak generation, the application consumes 1,706.55 MB PSS, leaving **793.45 MB of headroom** below the 2,500 MB budget. After 60 seconds of idle inactivity, the native weights are unloaded, releasing $\sim 1.45\text{ GB}$ back to Android.

### 7.4 Binary Artifact Sizes
- `app-release.apk`: **54.4 MB** (57,097,320 bytes, recorded in `benchmarks/apk_size.txt`)
- `vision.pte` (in assets): **14.25 MB** (14,940,556 bytes, recorded in `benchmarks/metrics.json` and `benchmarks/vision_model_benchmarks.txt`)
- `llama-3.2-1b-spinquant.pte`: **1.05 GB** (1,135,951,488 bytes, recorded in `benchmarks/llama_speed.md`)
- Network Permissions: Exactly **0** (`benchmarks/aapt2_permissions_release.txt`)

### 7.5 Usability Evaluation Numbers
The usability test sheet (`docs/USABILITY_TEST_SHEET.md`) is marked: *"Filled in by Antigravity: UNVERIFIED until the tester names and dates are confirmed by me"*. Per Rule H3, field test numbers are:
- **Farmer Participant (Telugu)**: Comprehension time **not yet measured** (test sheet unverified pending tester confirmation).
- **Village Extension Worker (Hindi/Telugu)**: Comprehension time **not yet measured** (test sheet unverified pending tester confirmation).
- **Citizen Student (English)**: Comprehension time **not yet measured** (test sheet unverified pending tester confirmation).
- **Average Time to Understand Advice**: **not yet measured** (target $\le 60\text{ s}$; formal field verification pending).

### 7.6 Task P5 End-to-End Phone Benchmark & Robustness Check

A scripted test harness (`tools/run_p5_benchmarks.py`) evaluated 55 test photos and 20 consecutive camera captures live on the connected 6 GB reference physical phone (POCO 22041219PI, 64-bit Arm CPU, Android 14). All values below come directly from the raw device log `benchmarks/p5_phone_report.md` (Rule H3):

| Test Condition / Metric | Target Requirement | Measured Physical Phone Value | Acceptance Status | Source Benchmark Log |
|---|---|---|:---:|---|
| **ANR Count (20 Captures)** | Exactly 0 | **0** | **PASS** | `benchmarks/p5_phone_report.md`, `benchmarks/p5_consecutive_captures_raw.json` |
| **Continuous Memory Stability** | Net PSS growth < 50 MB | **-1.74 MB** (213.65 MB -> 211.91 MB) | **PASS** | `benchmarks/p5_phone_report.md`, `benchmarks/p5_consecutive_captures_raw.json` |
| **Real Leaves Diagnosis Rate (25 photos)** | $\ge 80.0\%$ | **80.0%** (20 / 25 diagnosed) | **PASS** | `benchmarks/p5_phone_report.md`, `benchmarks/p5_scripted_run_raw.json` |
| **Non-Crop Rejection (`nocrop`, 10 photos)** | $\ge 80.0\%$ | **100.0%** (10 / 10 rejected: 9 CANT_TELL, 1 OUT_OF_CLASS) | **PASS** | `benchmarks/p5_phone_report.md`, `benchmarks/p5_scripted_run_raw.json` |
| **Other Crop Rejection (`othercrop`, 10 photos)** | $\ge 60.0\%$ | **90.0%** (9 / 10 rejected: 7 OUT_OF_CLASS, 2 CANT_TELL) | **PASS** | `benchmarks/p5_phone_report.md`, `benchmarks/p5_scripted_run_raw.json` |
| **Phone Pipeline Shutter-to-Result Latency** | $\le 1,500\text{ ms}$ | **769.5 ms** mean (Min: 621.2 ms, Max: 1759.7 ms) | **PASS** | `benchmarks/p5_phone_report.md`, `benchmarks/p5_scripted_run_raw.json` |

#### Category Breakdown on Physical Phone (55 Images):
- **Real Rice (5 photos)**: 5 / 5 diagnosed (100.0%), mean latency 938.6 ms.
- **Real Cotton (5 photos)**: 5 / 5 diagnosed (100.0%), mean latency 740.5 ms.
- **Real Chilli (5 photos)**: 5 / 5 diagnosed (100.0%), mean latency 817.2 ms.
- **Real Tomato (5 photos)**: 1 diagnosed, 2 CANT_TELL (high free energy), 2 OUT_OF_CLASS (low crop mass), mean latency 766.2 ms.
- **Real Maize (5 photos)**: 4 / 5 diagnosed, 1 CANT_TELL (high free energy), mean latency 776.1 ms.
- **Non-Crop Desk/Wall/Floor/Hand (`nocrop`, 10 photos)**: 0 diagnosed, 9 CANT_TELL, 1 OUT_OF_CLASS, mean latency 758.2 ms.
- **Other Crops Cashew/Cassava (`othercrop`, 10 photos)**: 1 diagnosed, 2 CANT_TELL, 7 OUT_OF_CLASS, mean latency 734.0 ms.
- **Laptop Screen Captures (`screen`, 10 photos)**: 6 diagnosed, 4 CANT_TELL, 0 OUT_OF_CLASS, mean latency 720.6 ms.

---

## 8. Honest Limitations

1. **Closed Taxonomy (21 Classes)**: The vision model identifies 21 classes across 5 crops. Symptoms caused by micronutrient deficiencies (e.g. zinc, iron chlorosis) or rare secondary pathogens outside the 21 classes are classified as *Not sure* or rejected by OOD energy thresholds.
2. **Environmental Conditions**: Low light, motion blur, or severe dust on leaves can lower confidence scores, routing farmers to *"Can't tell - ask an extension officer"*.
3. **TTS Voice Dependency**: Spoken audio guidance uses local Opus clips for primary reviewed phrases and Android's on-device TextToSpeech engine for dynamic text. If the user's phone lacks pre-installed Hindi or Telugu TTS voice packs, the app displays the step-by-step setup guide (*"Voice not installed"*).
4. **Arm Architecture Dependency**: Native ExecuTorch binaries target 64-bit Arm CPUs (`arm64-v8a`). 32-bit Arm devices (`armeabi-v7a`) are not supported.
5. **Data Leakage in Training Split**: The 25-class vision model was evaluated with 21.16% near-duplicate leakage (`benchmarks/metrics.json`). Real-world generalization on held-out field photos across 21 classes is not yet measured.
6. **Placeholder Audio Clips**: Bundled speech audio clips are synthetic placeholders generated by `tools/make_audio_clips.py`; final native Telugu and Hindi voice recordings have not yet been produced.

---

## 9. Out of Scope

As specified in `FINAL_REQUIREMENTS.md` section C, the following features are explicitly out of scope for Phase 1:
1. **Post-Harvest Loss Reduction**: Focus is restricted to field-stage foliar diseases and pests; post-harvest storage pests (e.g. grain weevils) are excluded.
2. **Voice Input (Speech-to-Text)**: Audio output (read-aloud) is supported; automatic speech recognition (ASR) for farmer voice commands is out of scope.
3. **Hailo and Raspberry Pi Runtimes**: Deployment is focused exclusively on commercial 64-bit Arm Android smartphones.
4. **Kannada and Tamil Languages**: Support is focused on the three mandatory project languages: English, Telugu, and Hindi.
5. **On-Device Llama Fine-Tuning**: Inference uses pre-quantized 4-bit SpinQuant weights; on-device backpropagation/fine-tuning is out of scope.
6. **Cloud Synchronization**: Crop Doctor is zero-cloud and stores case reports purely in local SQLite/DataStore.

---

## 10. Reproducibility and Licences

### 10.1 Software & Framework Versions
- **Android Gradle Plugin**: 8.7.0
- **Kotlin**: 2.0.21
- **Jetpack Compose BOM**: 2024.11.00
- **ExecuTorch Android SDK**: `org.pytorch:executorch-android:0.6.0`
- **Minimum Android SDK**: 26 (Android 8.0 Oreo)
- **Target Android SDK**: 35 (Android 15)

### 10.2 Clean Build Steps
```bash
# 1. Clone repository
git clone https://github.com/varun-gajula/crop-doctor-offline.git
cd crop-doctor-offline

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Assemble Release APK (signed with debug key)
./gradlew assembleRelease

# 4. Verify APK output
ls -lh app/build/outputs/apk/release/app-release.apk
```

### 10.3 Licences & Attributions
- **Application Source Code**: Apache License 2.0.
- **Advisory LLM**: Meta Llama 3.2 1B-Instruct is licensed under the [Llama 3.2 Community License Agreement](https://llama.meta.com/llama3/license/). *Built with Llama.*
- **Datasets**: PlantVillage, Mendeley Data, and PlantDoc datasets are used under their respective Creative Commons Attribution 4.0 International (CC BY 4.0) licenses. IP102 dataset is credited as academic reference.
- **Typography**: Noto Sans Telugu and Noto Sans Devanagari fonts are licensed under the SIL Open Font License (OFL 1.1).
- **ExecuTorch**: BSD 3-Clause License (Meta Platforms, Inc.).
