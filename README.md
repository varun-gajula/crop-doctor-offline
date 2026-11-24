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

> [!NOTE]
> **First-Time Model Setup**: The app works 100% offline out-of-the-box (vision, treatment advice, and case reports work with zero extra downloads). Installing the optional Llama 3.2 1B and Vosk speech models enables on-device AI explanations, interactive offline chat, and voice questions. See [Section 5.5](#55-first-time-setup-ai-model-files-optional) or the detailed [Setup Guide](docs/INSTALL_MODELS.md).

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

### 5.5 First-Time Setup: AI Model Files (Optional)

> **For complete details, links, and troubleshooting, read the full [`docs/INSTALL_MODELS.md`](docs/INSTALL_MODELS.md) guide.**

#### What is Needed & Why
- **Core App Works Offline Immediately**: The vision model (`vision.pte`, 14.25 MB) and the trilingual agronomy Knowledge Base (PJTSAU, ANGRAU, ICAR) are bundled directly inside the APK. Vision detection, urgency triage, treatment guidance, and extension worker case reports work **100% offline with zero extra setup or downloads**.
- **Why AI Files Are Sideloaded**: Large weights are required for on-device **Llama 3.2 1B-Instruct** generative reasoning, conversational offline chat, and **Vosk speech recognition**. Due to the [Meta Llama 3.2 Community License](https://llama.meta.com/) (which users accept directly) and GitHub file limits (> 100 MB), weights are not bundled in the APK.
- **Privacy & Security Guarantee**: The app contains **0 network permissions** (`INTERNET` is absent). No cellular data or Wi-Fi is ever needed, and no images, audio, or queries are ever uploaded.

#### Required Files, Sizes & Checksums
Users must accept the Llama 3.2 license before downloading:
1. **Llama 3.2 1B-Instruct (SpinQuant INT4)**: `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` (~1.13 GB / 1,135,951,488 bytes)  
   - SHA-256: `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b`  
   - Source: Accept license at [Meta Llama](https://llama.meta.com/) or [Hugging Face](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct), then download artifact from [ExecuTorch Community](https://huggingface.co/executorch-community/).
2. **Tokenizer**: `tokenizer.model` (~2.18 MB / 2,183,982 bytes)  
   - SHA-256: `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`  
   - Source: [Meta Llama Tokenizer](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct/resolve/main/original/tokenizer.model).
3. **Offline Voice Models (Optional, for Mic in Chat)**:
   - English: `vosk-model-small-en-in-0.4.zip` (35.8 MB, SHA-256: `20663dcac4d5cb783a579c54d98339344a688e4ec6e1b4a4b059fd1235454cc7`)
   - Hindi: `vosk-model-small-hi-0.22.zip` (42.4 MB, SHA-256: `7c50a10866889f0ac21d912c20537a055a597ed09fc1d3e5bcd798f9f0017e48`)
   - Telugu: `vosk-model-small-te-0.42.zip` (57.7 MB, SHA-256: `3e77b05becc7599a596ccebd8b22cb95eb4eca0cfca524827cdfbfd66af09497`)
   - Sources: [AlphaCephei Models](https://alphacephei.com/vosk/models/).

#### Method A: In-App File Picker (Zero-ADB / Direct on Phone)
1. Transfer the `.pte`, `tokenizer.model`, and optional `.zip` files to the phone's **`Download`** folder via USB file transfer or browser download.
2. In Agri Care AI, tap **More Options (☰)** -> **"Install AI Model (.pte)"** (or tap **"⚙️ Install AI Model (Offline)"** on any diagnosis result screen).
3. Tap **"Select Model File (.pte)"** and choose the `.pte` file using the system file picker.
4. Tap **"Select Tokenizer (.model)"** and choose `tokenizer.model`.
5. (Optional) Scroll to **"🎙️ Offline Speech Models (Vosk)"**, tap **"Install (.zip)"** next to your language, and pick the `.zip` file.
6. The app copies them into private storage, verifies sizes, and immediately activates offline AI mode.

#### Method B: Push via ADB (For Developers)
Push directly to the app's external files directory (no root required; accessible on Android 10+):
```bash
# Push model weights and tokenizer
adb push Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte /sdcard/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte
adb push tokenizer.model /sdcard/Android/data/com.cropdoctor/files/tokenizer.model

# Or use automated repository helper scripts:
python scripts/setup/install_llama.py --accept-license
python scripts/setup/download_vosk.py
```

#### How to Check It Worked
- **In Install Screen**: A green banner displays: `"All AI Models Installed & Ready — Llama 3.2 1B & Tokenizer are active"`.
- **On Result Screen**: Advisory card displays a green badge: **`Explained on this phone by Llama 3.2 1B`** (or in Hindi: **`इस फ़ोन पर Llama 3.2 1B द्वारा समझाया गया`**).
- **In Chat**: The mic 🎙️ button records speech offline and transcribes it into chat queries.

#### What You See If Files Are NOT Installed (Verifiable Fallback)
If model files are absent, the app displays the verified agronomic recommendations from the Knowledge Base with a blue badge:  
**`Offline guide - AI explanation not available`** (Hindi: **`ऑफ़लाइन गाइड - AI स्पष्टीकरण उपलब्ध नहीं है`**, Telugu: **`ఆఫ్‌లైన్ గైడ్ - AI వివరణ అందుబాటులో లేదు`**).

#### Troubleshooting
- **Insufficient Storage**: Ensure the phone has at least 3 GB of free storage before copying the ~1.13 GB model.
- **"Warning: Model file seems too small"**: File download was incomplete (< 100 MB). Check size and re-download.
- **Low Memory Fallback (Rule H15)**: If system available RAM drops below ~1.8 GB, Llama is bypassed to prevent out-of-memory crashes and the app gracefully serves the offline guide. Close heavy background apps to allow Llama to load.

---

## 6. Reproduce: Complete Offline Setup Without Large Files

In strict accordance with Project Rules **H2 (Zero datasets or weights committed)** and **H9 (Free-only / open licences)**, this repository does not bundle gigabytes of model binaries or raw photo datasets. A reviewer or evaluator can reproduce the entire offline environment using the verified utilities in `scripts/setup/`.

```
scripts/setup/
  ├── download_vosk.py         # Downloads, verifies SHA-256, extracts & pushes 3 Vosk speech models
  ├── install_llama.md         # Guide on license acceptance, official sources, and reference checksums
  ├── install_llama.py         # Verifies SHA-256 of supplied Llama weights & pushes via ADB
  ├── datasets_manifest.json   # Machine-readable provenance manifest of all 12 datasets
  └── download_datasets.py     # Automated Kaggle/Git ingestion, Mendeley guidance & clustering pipeline
```

### 6.1 Reproducing Speech Recognition (Vosk Offline Models)
Agri Care AI provides offline voice input for English, Hindi, and Telugu using open-source Vosk models.

```bash
# 1. Simulate workflow with dry-run (prints URLs, hashes, and target phone paths)
python scripts/setup/download_vosk.py --dry-run

# 2. Download, verify SHA-256, extract, and push to connected phone via ADB
python scripts/setup/download_vosk.py
```
- **Official Sources**: Downloads `vosk-model-small-en-in-0.4` (35.8 MB), `vosk-model-small-hi-0.22` (42.4 MB), and `vosk-model-small-te-0.42` (57.7 MB) from `https://alphacephei.com/vosk/models/`.
- **Integrity**: Compares SHA-256 against recorded checksums.
- **Target Path Read by App**: Installs into `/sdcard/Android/data/com.cropdoctor/files/vosk/<lang>/` (where `<lang>` is `en`, `hi`, `te`). No root permissions required.

### 6.2 Reproducing Advisory AI Engine (Llama 3.2 1B-Instruct)
To enable on-device generative reasoning grounded in the Knowledge Base:

```bash
# 1. Review license and source instructions
# Read scripts/setup/install_llama.md

# 2. Dry-run verification (computes SHA-256 of local files and checks target paths)
python scripts/setup/install_llama.py --model path/to/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte --tokenizer path/to/tokenizer.model --accept-license --dry-run

# 3. Active installation to connected phone
python scripts/setup/install_llama.py --model path/to/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte --tokenizer path/to/tokenizer.model --accept-license
```
- **Official Checksums**:
  - `llama-3.2-1b-spinquant.pte` (1.13 GB): `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b`
  - `tokenizer.model` (2.18 MB): `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`
- **Zero Credentials**: No API keys or passwords are used or stored.
- **Alternative In-App Installation**: Files can also be picked directly on the phone via **More Options (☰)** -> **"Install AI Model (.pte)"**.

### 6.3 Reproducing Dataset Ingestion & Preparation Pipeline
The vision backbone (`vision.pte`) is trained on a curated 15-class field dataset assembled from 12 open sources.

```bash
# 1. Audit manifest and check local availability of raw archives
python scripts/setup/download_datasets.py --dry-run

# 2. Automated ingestion for permissible sources (Kaggle and Git clones)
# Kaggle sources use the reviewer's own credentials from environment (KAGGLE_USERNAME/KEY)
python scripts/setup/download_datasets.py --download-all-available
```
- **Manifest**: `scripts/setup/datasets_manifest.json` tracks every dataset, DOI, licence (with explicit CC BY-NC 3.0 warnings), classes fed, and expected image counts.
- **Mendeley Sources**: Direct browser links and target folder instructions are printed for the 6 CC BY 4.0 Mendeley repositories protected by Cloudflare bot-checks.
- **Audited Preparation Pipeline**:
  Once raw files are in `C:\crop-data\candidates\raw\`, run:
  ```bash
  python scripts/prepare_dataset_v2.py
  ```
  This script enforces:
  1. **Provenance Source Tagging**: Every image is prefixed with `src-<Origin>_` (`src-PaddyDoctor_`, `src-AgML_`, `src-Mendeley74j_`, etc.) to prevent data leakage.
  2. **Letterbox Trimming & Resizing**: Removes synthetic border artifacts before lanczos resizing to uniform 512x512.
  3. **Near-Duplicate Single-Linkage Clustering**: Computes dihedral 64-bit dHash; clusters duplicate photos at Hamming $\le 8$ ($\le 10$ for tomato) to guarantee zero cluster memorization.
  4. **Cross-Class Collision Filtering**: Discards conflicting duplicates across different classes at Hamming $\le 2$.
  5. **Capping**: Enforces an 800-photo cap per source pool for balanced class distributions.
  6. **Packaging**: Generates `C:\crop-data\crop_doctor_15_v2.zip` for training.

### 6.4 Reproducing Application Build & Quality Gates
```bash
# Run full suite of automated unit tests (rules, localization, guardrails, audio)
./gradlew testDebugUnitTest

# Assemble production Release APK
./gradlew assembleRelease

# Verify zero network permissions in compiled binary (Rule H1)
aapt2 dump permissions app/build/outputs/apk/release/app-release.apk
```

---

## 7. Safety & Human-in-the-Loop Decisions

1. **Rule H4 (Zero Unverified Doses)**: Chemical dosages appear only when marked `dose_verified: true` from an official University or CIB&RC publication. Otherwise, the app explicitly directs farmers to *"follow the product label"*.
2. **Rule H5 (Code-Controlled Urgency)**: Llama only explains; it never decides disease severity, urgent referrals, or registered medicines. Severity scoring and emergency escalation reside strictly in unit-tested Kotlin code (`RulesEngine.kt`).
3. **Rule H6 (Telugu Protection)**: Llama 3.2 does not officially support Telugu. Telugu text is NEVER generated by Llama and is served 100% from reviewed Knowledge Base entries.
4. **Emergency Referral**: Severe infestations (*Act now*) immediately prioritize the toll-free Kisan Call Centre button (**1800-180-1551**).

---

## 8. Out of Scope

The following features are explicitly out of scope for Phase 1 (stated in `FINAL_REQUIREMENTS.md` section C):
- Post-harvest loss reduction and storage pest management
- Voice input / speech-to-text recognition (audio read-aloud is supported)
- Hailo and Raspberry Pi hardware deployment
- Kannada and Tamil regional languages
- On-device fine-tuning of Llama weights
- Cloud database synchronization

---

## 9. Licences & Attributions

For complete dataset citations, license texts, and provenance audits, see [`docs/CREDITS.md`](docs/CREDITS.md).

- **Application Code**: Licensed under the [Apache License 2.0](LICENSE).
- **Advisory LLM**: Meta Llama 3.2 1B-Instruct is licensed under the [Llama 3.2 Community License Agreement](https://llama.meta.com/llama3/license/). *Built with Llama.*
- **Offline Speech Recognition**: Vosk speech recognition engine and acoustic models licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) (Alpha Cephei Inc.).
- **Edge ML Runtime**: PyTorch ExecuTorch 0.6.0 ([BSD 3-Clause License](https://github.com/pytorch/executorch/blob/main/LICENSE), Meta Platforms, Inc.) and XNNPACK ([BSD 3-Clause License](https://github.com/google/XNNPACK/blob/master/LICENSE), Google LLC).
- **Datasets**: Audited under `scripts/setup/datasets_manifest.json` and `docs/CREDITS.md`:
  - Mendeley Data Rice, Cotton, Chilli, and Maize Datasets (CC BY 4.0).
  - Paddy Doctor Dataset (CC BY 4.0).
  - Project-AgML Rice Dataset (Apache 2.0 / Open Access).
  - PlantDoc Dataset (MIT License).
  - PlantVillage Dataset (CC BY-NC 3.0, *strictly non-commercial educational/research use*).
- **Typography & Iconography**: Noto Sans Telugu and Noto Sans Devanagari fonts (SIL Open Font License OFL 1.1); Material Design Icons (Apache 2.0).
