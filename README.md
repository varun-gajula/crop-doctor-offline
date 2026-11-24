# Agri Care AI (Offline)

> **Offline Crop Disease & Pest Detection and Advisory for Farmers**  
> *Developed for the Arm "Bharat AI-SoC Challenge 2026–27" (Problem Statement 1: Offline crop disease/pest detection and advisory for farmers)*  
> Target Device: Any Android phone with 6 GB physical RAM and a 64-bit Arm CPU (`arm64-v8a`), minSdk 26 (Android 8.0+), Android 10+ recommended.

[![Network Permissions](https://img.shields.io/badge/Network_Permissions-0_(INTERNET_absent)-success.svg)](benchmarks/aapt2_permissions_release.txt)
[![Hardware Target](https://img.shields.io/badge/Hardware-64--bit_Arm_CPU_(6_GB_RAM)-blue.svg)](docs/REPORT.md)
[![Vision Runtime](https://img.shields.io/badge/Vision-MobileNetV3--Large_%7C_ExecuTorch_0.6.0-orange.svg)](docs/REPORT.md)
[![Advisory Engine](https://img.shields.io/badge/Advisory-Llama_3.2_1B_%7C_SpinQuant_INT4-purple.svg)](docs/REPORT.md)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## 1. Overview
In rural farming communities, cellular coverage frequently drops deep in the fields where crop diseases and pest outbreaks first appear. **Agri Care AI** is an edge-native, zero-cloud Android application engineered to run 100% offline on consumer Arm System-on-Chips (SoCs).

Using a decoupled two-lane edge architecture, Agri Care AI captures an affected leaf or pest photo, delivers disease/pest identification and lesion severity in under 1.5 seconds, and provides grounded, actionable treatment recommendations in **Telugu (తెలుగు)**, **Hindi (हिन्दी)**, and **English**, all within an accessible 60-second field workflow.

---

## 2. Two-Lane On-Device Architecture

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

1. **Vision Lane (Perception)**: Executes synchronously upon shutter capture. Runs an optimized, calibrated MobileNetV3-Large neural network (`vision.pte`, 14.25 MB) via ExecuTorch XNNPACK targeting Arm NEON vector SIMD instructions.
2. **Advisory Lane (Reasoning)**: Executes asynchronously. Runs Meta Llama 3.2 1B-Instruct via ExecuTorch with 4-bit SpinQuant quantization on the Arm CPU, grounded strictly in the verified Knowledge Base.
3. **Deterministic Rules & Knowledge Base**: Sourced from state agricultural universities (PJTSAU, ANGRAU) and ICAR institutes. Enforces safety decisions, emergency escalation (Kisan Call Centre 1800-180-1551), and dosage protection.

---

## 3. Supported Crops & Shipped Classes (21 Classes)
Covering high-priority crops across Telangana, Andhra Pradesh, and India:
1. **Rice (వరి / धान)**: Bacterial leaf blight, Brown spot, Leaf blast, Sheath blight, Healthy.
2. **Cotton (పత్తి / कपास)**: Bacterial blight, Fusarium wilt, Healthy.
3. **Chilli (మిరప / मिर्च)**: Bacterial spot, Leaf curl virus, Healthy.
4. **Tomato (టమాటా / टमाटर)**: Early blight, Late blight, Yellow leaf curl virus, Spider mites (*Pest*), Healthy.
5. **Maize (మొక్కజొన్న / मक्का)**: Gray leaf spot, Common rust, Northern leaf blight, Fall armyworm (*Pest*), Healthy.

---

## 4. Performance: What is Measured vs What is Not

Every number below comes from a logged measurement saved in `benchmarks/`. Zero figures are estimated or simulated (Rule H3 & H12).

### 4.1 What is Measured

| Metric | Target Budget | Measured Value | Benchmark Source Log |
|---|---|---|---|
| **Network Permissions** | Exactly 0 | **0 network permissions** (`INTERNET` absent) | [`benchmarks/aapt2_permissions_release.txt`](benchmarks/aapt2_permissions_release.txt) |
| **Airplane Mode Operation** | 100% Offline | **0 network permissions** (offline unit tests passing; video unfinalized) | [`benchmarks/aapt2_permissions_release.txt`](benchmarks/aapt2_permissions_release.txt) |
| **Cold App Startup** | $\le 2,000\text{ ms}$ | **1,528.0 ms** mean on 6 GB phone debug; Release on phone **not yet measured** (369.2 ms on emulator) | [`benchmarks/speed_summary.md`](benchmarks/speed_summary.md) |
| **Shutter to Result (Field Leaves)** | $\le 1,500\text{ ms}$ | **117.10 ms** mean on 6 GB Arm phone (85–169 ms) | [`benchmarks/speed_summary_real_leaves.json`](benchmarks/speed_summary_real_leaves.json) |
| **Vision Inference (`vision.pte`)** | $< 150\text{ ms}$ | **57.54 ms** mean on Arm CPU (49.1% of pipeline) | [`benchmarks/speed_summary.md`](benchmarks/speed_summary.md) |
| **Llama 3.2 1B Throughput** | $\ge 2.0\text{ tok/s}$ | **11.94 tokens/s** mean decode on Arm CPU | [`benchmarks/speed_summary.md`](benchmarks/speed_summary.md) |
| **Llama Time to First Token** | - | **4,087.0 ms** mean prefill latency | [`benchmarks/llama_speed.md`](benchmarks/llama_speed.md) |
| **Peak Memory (Idle / Vision)** | $\le 500\text{ MB PSS}$ | **147.68 MB PSS** idle on 6 GB phone debug (186.81 MB with vision); Release on phone **not yet measured** (31.97 MB on emulator) | [`benchmarks/mem_a_idle_start.txt`](benchmarks/mem_a_idle_start.txt) |
| **Peak Memory (With Llama)** | $\le 2,500\text{ MB PSS}$ | **1,706.55 MB PSS** peak during token decode | [`benchmarks/mem_d_llama_streaming_peak.txt`](benchmarks/mem_d_llama_streaming_peak.txt) |
| **Memory Reclaim (60s Idle)** | $\le 500\text{ MB PSS}$ | **243.79 MB PSS** (reclaims 1.45 GB native heap) | [`benchmarks/mem_e_60s_after_release.txt`](benchmarks/mem_e_60s_after_release.txt) |
| **Real-Photo Accuracy (21 Classes)** | - | **not yet measured** (model in use has 21.16% near-duplicate leakage in `benchmarks/metrics.json`) | [`benchmarks/metrics.json`](benchmarks/metrics.json) |
| **Llama Guardrail Grounding** | $\ge 18/20$ | **20/20 English (100%)** & **20/20 Hindi (100%)** | [`benchmarks/llama_english_eval.md`](benchmarks/llama_english_eval.md) |
| **Usability Comprehension** | $< 60\text{ s}$ | **not yet measured** (`docs/USABILITY_TEST_SHEET.md` is UNVERIFIED pending tester confirmation) | [`docs/USABILITY_TEST_SHEET.md`](docs/USABILITY_TEST_SHEET.md) |
| **Binary Artifact Sizes** | APK $< 90\text{ MB}$ | **54.4 MB** APK (`vision.pte` 14.25 MB in assets) | [`benchmarks/apk_size.txt`](benchmarks/apk_size.txt) |

### 4.2 What is Not Measured / Known Limitations
- **Thermal throttling over 1+ hours continuous LLM generation**: The app mitigates this by automatically unloading Llama from memory after 60 seconds of idle inactivity, but sustained generation beyond 15 continuous minutes has not been benchmarked.
- **Dedicated NPU Acceleration**: ExecuTorch currently executes via the CPU XNNPACK backend targeting Arm NEON vector SIMD. NPU acceleration backends (e.g. Qualcomm QNN / MediaTek Neuropilot) are not enabled in this build.
- **Crops outside the five**: Symptoms on crops other than Rice, Cotton, Chilli, Tomato, and Maize are outside the model's 21-class taxonomy and route to *"Can't tell - ask an extension officer"*.

---

## 5. Build & Installation Steps

### Prerequisites
- Java Development Kit (JDK 17 or 21)
- Android SDK with platform-tools and build-tools 34.0.0+ (`compileSdk = 35`, `minSdk = 26`)
- Git

### 5.1 Clone Repository
```bash
git clone https://github.com/varun-gajula/crop-doctor-offline.git
cd crop-doctor-offline
```

### 5.2 Run Automated Unit Tests
```bash
# Runs all 104 unit tests covering rules, audio manifests, guardrails, and localization
./gradlew testDebugUnitTest
```

### 5.3 Build Debug and Release APKs
```bash
# Build Debug variant
./gradlew assembleDebug

# Build Release variant with Baseline Profile (signed with debug key per Rule H9)
./gradlew assembleRelease
```
The compiled Release APK will be generated at:
`app/build/outputs/apk/release/app-release.apk` (54.4 MB)

### 5.4 Install on Android Device
```bash
# Connect Android phone (or launch emulator) via adb
adb install -r app/build/outputs/apk/release/app-release.apk

# Grant camera permission for image capture
adb shell pm grant com.cropdoctor android.permission.CAMERA
```

### 5.5 Installing Llama 3.2 1B Model for Offline AI Explanations

The vision model (`vision.pte`, 16.9 MB) is pre-bundled in the APK assets and functions immediately out of the box with zero setup.

To enable on-device **Llama 3.2 1B-Instruct** explanations (which stream live on the Arm CPU via ExecuTorch INT4 SpinQuant):

#### Method A: In-App Installer (Zero-ADB / Direct on Phone)
1. Transfer `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` (~1.13 GB) and `tokenizer.model` (~2.18 MB) to the phone's storage (e.g. via USB cable, SD card, Bluetooth, or Nearby Share to `Downloads/`).
2. Open **Agri Care AI** -> Tap **More Options (☰)** -> Tap **"Install AI Model (.pte)"** (or tap **"⚙️ Install AI Model (Offline)"** on any result screen).
3. Tap **"Select Model File (.pte)"** and pick the `.pte` file using the Android system file picker.
4. Tap **"Select Tokenizer (.model)"** and pick `tokenizer.model`.
5. The app copies them to its private storage (`context.filesDir`), validates file sizes ($\ge 100\text{ MB}$ for model, $\ge 50\text{ KB}$ for tokenizer), and loads the model instantly without internet.

#### Method B: Push via ADB (For Developers)
On Android 14+, regular apps cannot access `/data/local/tmp` due to SELinux `untrusted_app` policy. Push directly to the app's external files directory (no root or `run-as` required):
```bash
# Push 4-bit quantized Llama weights and tokenizer to app-specific storage
adb push Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte /sdcard/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte
adb push tokenizer.model /sdcard/Android/data/com.cropdoctor/files/tokenizer.model
```

#### Verifiable Fallback Guarantee (Task A-REAL)
- **Model Missing or Unloaded**: If the model files are not installed, memory is low, or language is Telugu (which Llama does not officially support), Agri Care AI immediately displays the verified Agronomy Knowledge Base text labeled:
  `"Offline guide - AI explanation not available"`
- **Model Running**: When Llama 3.2 1B streams tokens on device, the advisory card displays:
  `"Explained on this phone by Llama 3.2 1B"`
- Every case report and system log records the exact source (`advisor_source=LLAMA` or `advisor_source=OFFLINE_GUIDE`).

---

## 6. Safety & Human-in-the-Loop Decisions

1. **Rule H4 (Zero Unverified Doses)**: Chemical dosages appear only when marked `dose_verified: true` from an official University or CIB&RC publication. Otherwise, the app explicitly directs farmers to *"follow the product label"*.
2. **Rule H5 (Code-Controlled Urgency)**: Llama only explains; it never decides disease severity, urgent referrals, or registered medicines. Severity scoring and emergency escalation reside strictly in unit-tested Kotlin code (`RulesEngine.kt`).
3. **Rule H6 (Telugu Protection)**: Llama 3.2 does not officially support Telugu. Telugu text is NEVER generated by Llama and is served 100% from reviewed Knowledge Base entries.
4. **Emergency Referral**: Severe infestations (*Act now*) immediately prioritize the toll-free Kisan Call Centre button (**1800-180-1551**).

---

## 7. Out of Scope

The following features are explicitly out of scope for Phase 1 (stated in `FINAL_REQUIREMENTS.md` section C):
- Post-harvest loss reduction and storage pest management
- Voice input / speech-to-text recognition (audio read-aloud is supported)
- Hailo and Raspberry Pi hardware deployment
- Kannada and Tamil regional languages
- On-device fine-tuning of Llama weights
- Cloud database synchronization

---

## 8. Licences & Attributions

- **Application Code**: Licensed under the [Apache License 2.0](LICENSE).
- **Advisory LLM**: Meta Llama 3.2 1B-Instruct is licensed under the [Llama 3.2 Community License Agreement](https://llama.meta.com/llama3/license/). *Built with Llama.*
- **Datasets**:
  - PlantVillage Dataset (David P. Hughes & Marcel Salathé, Penn State University, CC BY 4.0).
  - Mendeley Data Rice & Cotton Disease Datasets (CC BY 4.0).
  - PlantDoc Dataset (IIT Delhi, CC BY 4.0).
  - IP102 Dataset (Pest morphology academic reference).
- **Typography**: Noto Sans Telugu and Noto Sans Devanagari fonts (SIL Open Font License OFL 1.1).
- **ML Runtime**: PyTorch ExecuTorch 0.6.0 (BSD 3-Clause License, Meta Platforms, Inc.).
