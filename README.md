# AI Agri Care

> **100% Offline Edge-AI Crop Disease and Pest Advisory System for Android**

`[Android 8+ (minSdk 26)]` `[ExecuTorch MobileNetV3-Large]` `[Llama 3.2 1B Instruct]` `[100% Offline · Zero Cloud]` `[Apache-2.0]`

---

## Links

- **Demo Video:** [Watch Demo Video (<placeholder>)](https://github.com/varun-gajula/crop-doctor-offline)
- **Final Project Report:** docs/Final_Project_Report.docx *(submission document)*
- **Releases:** [Download Latest APK (<releases link>)](https://github.com/varun-gajula/crop-doctor-offline/releases)

---

## The Problem & The Solution

### The Problem (PS1)
Smallholder farmers in semi-arid and remote regions frequently lack reliable cellular connectivity when diagnosing crop diseases in the field. Cloud-dependent agricultural apps fail entirely under zero-connectivity conditions, while high latency, generic advice, and language barriers prevent timely intervention.

### What AI Agri Care Does
**AI Agri Care** is a standalone, completely offline Android application designed for edge execution. Without requiring internet access, data packets, or external server calls:
1. **Detects Crops Automatically:** Automatically identifies the crop type (Rice, Cotton, Chilli, Maize, Tomato) or accepts manual selection.
2. **Diagnoses Diseases & Pests:** Classifies 15 specific conditions (6 fungal/bacterial diseases, 3 destructive insect pests, 5 healthy crop states, and 1 non-crop rejection).
3. **Calculates Severity & Urgency:** Estimates visual leaf lesion coverage and urgency tier (*Early warning*, *Act this week*, *Act now*).
4. **Delivers Sourced Localized Advice:** Provides deterministic cultural, biological, and chemical recommendations in English, Telugu, and Hindi from 576 sourced passages (ICAR, PJTSAU, ANGRAU as listed in data/kb), pending expert review.
5. **Explains in Plain Language:** Optionally streams an on-device explanation via an embedded ExecuTorch Llama 3.2 1B Instruct LLM.

### 4-Step Field Diagnostic Flow
```
[ 1. Capture ] ──> [ 2. Vision Gate ] ──> [ 3. Advisory Match ] ──> [ 4. Action Plan ]
  Snap leaf          MobileNetV3           Deterministic KB           Treatment tiers,
  photo in field     auto-detect +         576 sourced passages       Llama explanation,
  (Airplane Mode)    calibrated gates      in En / Te / Hi            or Extension call
```

---

## Key Features

- **100% Offline by Design:** Operates under Airplane Mode with zero internet permissions in the Android manifest (Rule H1 verified via `aapt2`).
- **15 Supported Classes:** 6 high-impact crop diseases, 3 destructive pests, 5 healthy baselines, and 1 background non-crop class.
- **Auto Crop Detection:** Automatically classifies crop type from camera frames with optional manual override chips.
- **Severity & Urgency Scoring:** Automated lesion-to-leaf surface area ratio estimation and three-tier urgency classification (*Early warning / Act this week / Act now*).
- **Trilingual Localization:** Complete native UI, speech recognition, and localized advice in English, Telugu (తెలుగు), and Hindi (हिन्दी).
- **Sourced Regional Knowledge Base:** 576 sourced passages (ICAR, PJTSAU, ANGRAU as listed in data/kb), pending expert review.
- **On-Device Llama 3.2 Explanation:** Fully local ExecuTorch Llama 3.2 1B Instruct model synthesizes natural-language explanations without cloud calls.
- **Offline Voice & Read-Aloud:** Vosk offline speech recognition in English, Hindi and Telugu (Telugu recognition is limited). Read-aloud uses pre-rendered bundled Opus audio clips from app assets for reviewed guidance, falling back to Android system TextToSpeech (if installed on the phone) for dynamic text; Llama output is never routed to Telugu audio (Rule H6).
- **Offline Chat:** Interactive multi-turn chat answering farmer questions directly from local knowledge base guidance.
- **Case Report Export:** Instant generation and local sharing of case diagnostic summaries for extension officers.
- **One-Touch Extension Hotline:** Direct phone dialer integration to Kisan Call Centre (`1800-180-1551`) and regional university helplines.

---

## Architecture

AI Agri Care separates visual perception from safety-critical advisory recommendations through two decoupled pipelines joined by a deterministic local knowledge base:

```
                      ┌─────────────────────────────────────────┐
                      │            INPUT LEAF PHOTO             │
                      └────────────────────┬────────────────────┘
                                           │
                                           ▼
 ┌─────────────────────────────────────────────────────────────────────────────────────┐
 │ VISION LANE (ExecuTorch MobileNetV3-Large · 16.1 MB · FP32 Linear Classification Head)│
 ├─────────────────────────────────────────────────────────────────────────────────────┤
 │ 1. Preprocessing: 512x512 RGB with ImageNet mean [0.485, 0.456, 0.406] and std      │
 │    [0.229, 0.224, 0.225] (bilinear interpolation)                                   │
 │ 2. Backbone Feature Extraction: MobileNetV3-Large with linear head nn.Linear(960, 15)│
 │ 3. Calibrated Temperature Scaling: T = 0.9433                                       │
 │ 4. Multi-Gate Safety Filters (Pure Kotlin Code in VisionMath.kt):                   │
 │    • Crop Mass Gate: cropMass >= 0.40                                               │
 │    • Energy Gate: freeEnergy <= -3.30 (lower is in-distribution; > -3.30 rejects)   │
 │    • Confidence Gate: top1Prob >= 0.32                                              │
 │    • Margin Gate: margin >= 0.15                                                    │
 └──────────────────┬───────────────────────────────┬──────────────────────────────────┘
                    │ [PASS: Diagnosis]             │ [FAIL: Out-of-Distribution]
                    ▼                               ▼
 ┌──────────────────────────────────────┐  ┌───────────────────────────────────────────┐
 │ ADVISORY LANE (Deterministic KB)     │  │ SAFETY FALLBACK GATES                     │
 ├──────────────────────────────────────┤  ├───────────────────────────────────────────┤
 │ • 576 sourced passages (JSON)        │  │ • CAN'T TELL (Blurry / Poor lighting)     │
 │ • Cultural & biological management   │  │ • OUT OF CLASS (Unsupported leaf)         │
 │ • Chemical options; doses are shown  │  │ • Direct helpline referral (1800-180-1551)│
 │   only when verified, otherwise      │  └───────────────────────────────────────────┘
 │   follow product label               │
 └──────────────────┬───────────────────┘
                    │
                    ▼
 ┌─────────────────────────────────────────────────────────────────────────────────────┐
 │ OPTIONAL EXPLANATION (ExecuTorch Llama 3.2 1B Instruct · 4-bit Groupwise SpinQuant)  │
 ├─────────────────────────────────────────────────────────────────────────────────────┤
 │ • Context-grounded plain-language summary                                           │
 │ • Available RAM checked before loading Llama (requires >= 1,500 MB available RAM    │
 │   via MemoryBudgetDecider); model is released after 60 s idle                       │
 └─────────────────────────────────────────────────────────────────────────────────────┘
```

> **Safety Architecture Note:** Safety decisions are implemented strictly in plain Kotlin code (`VisionMath.kt` and `ExecuTorchClassifier.kt`). The LLM is never in the decision loop—it only explains pre-validated diagnoses and sourced KB remedies.

---

## Supported Classes (15 Classes)

| # | Class ID | English | Telugu (తెలుగు) | Hindi (हिन्दी) | Type |
|:---|:---|:---|:---|:---|:---|
| 1 | `rice_leaf_blast` | Rice Leaf Blast | వరి అగ్గి తెగులు | धान का झोंका रोग (ब्लास्ट) | Disease |
| 2 | `rice_healthy` | Healthy Rice | ఆరోగ్యకరమైన వరి | स्वस्थ धान | Healthy |
| 3 | `cotton_bacterial_blight` | Cotton Bacterial Blight | పత్తి బ్యాక్టీరియా ఆకుమచ్చ తెగులు | कपास का जीवाणु झुलसा | Disease |
| 4 | `cotton_healthy` | Healthy Cotton | ఆరోగ్యకరమైన పత్తి | स्वस्थ कपास | Healthy |
| 5 | `chilli_leaf_curl_virus` | Chilli Leaf Curl Virus | మిరప ఆకు ముడత తెగులు | मिर्च का पत्ती मरोड़ रोग | Disease |
| 6 | `chilli_healthy` | Healthy Chilli | ఆరోగ్యకరమైన మిరప | स्वस्थ मिर्च | Healthy |
| 7 | `maize_northern_leaf_blight` | Maize Northern Leaf Blight | మొక్కజొన్న ఆకు మాడు తెగులు | मक्का का उत्तरी पत्ती झुलसा | Disease |
| 8 | `maize_common_rust` | Maize Common Rust | మొక్కజొన్న తుప్పు తెగులు | मक्का का रतुआ रोग | Disease |
| 9 | `maize_healthy` | Healthy Maize | ఆరోగ్యకరమైన మొక్కజొన్న | स्वस्थ मक्का | Healthy |
| 10 | `maize_fall_armyworm` | Maize Fall Armyworm | మొక్కజొన్న కత్తెర పురుగు | मक्का का फॉल आर्मीवर्म | Pest |
| 11 | `rice_stem_borer` | Rice Yellow Stem Borer | వరి కాండం తొలుచు పురుగు | धान का पीला तना छेदक | Pest |
| 12 | `rice_hispa` | Rice Hispa | వరి హిస్పా (ముళ్ల పురుగు) | धान का हिस्पा कीट | Pest |
| 13 | `tomato_healthy` | Healthy Tomato | ఆరోగ్యకరమైన టమాటా | स्वस्थ टमाटर | Healthy |
| 14 | `tomato_septoria_leaf_spot` | Tomato Septoria Leaf Spot | టమాటా సెప్టోరియా ఆకుమచ్చ తెగులు | टमाटर का सेप्टोरिया धब्बा रोग | Disease |
| 15 | `other_not_a_crop` | Non-Crop / Weeds | పంట కానిది / ఇతర మొక్కలు | गैर-फसल / खरपतवार | Out of Class |

---

## Decision Thresholds

| Parameter | Calibrated Value | Meaning & Role |
| :--- | :--- | :--- |
| **Calibration Method** | `single_scalar_temperature_scaling` | Logit probability calibration |
| **Temperature (T)** | `0.9433` | Post-hoc temperature divisor |
| **Confidence Threshold** | `0.32` | Minimum calibrated softmax probability required to diagnose |
| **Energy Threshold** | `-3.30` | Free-energy cutoff: E(x) = -T * log(sum(exp(z_i / T))) <= -3.30 |
| **Crop Mass Threshold** | `0.40` | Minimum sum of leaf foreground probability |
| **Margin Threshold** | `0.15` | Minimum difference between top-1 and top-2 softmax scores |
| **Calibration Status** | `CALIBRATED_15CLASS_TR4_OPT8` | Verified across test suite |

*Thresholds were chosen on the same 30 photos; this is not an independent estimate.*

---

## Measured Performance & Hardware Benchmarks

All hardware metrics were measured on physical hardware: **POCO M4 5G (6 GB RAM, MediaTek Dimensity 700, Android 14)**.

| Metric | Measured Value | Benchmark Source File |
| :--- | :--- | :--- |
| **Grouped Test Accuracy** | **97.95%** | [benchmarks/final/metrics.json](benchmarks/final/metrics.json) |
| **Macro-Averaged Accuracy** | **98.18%** | [benchmarks/final/metrics.json](benchmarks/final/metrics.json) |
| **PlantDoc Stress Test Accuracy** | **66.1%** | [benchmarks/final/metrics.json](benchmarks/final/metrics.json) |
| **Real-Device Field Crop Detection** | **28/28 (100%)** | [benchmarks/aiagricare_real_field_results.json](benchmarks/aiagricare_real_field_results.json) |
| **Real-Device Field Top-1 Diagnosis** | **27/28 (96.4%)** | [benchmarks/aiagricare_real_field_results.json](benchmarks/aiagricare_real_field_results.json) |
| **Vision Inference Latency** | **~460 ms** (avg per scan) | [benchmarks/classwise_and_power_results.json](benchmarks/classwise_and_power_results.json) |
| **Energy Consumption** | **139.55 mJ** per scan | [benchmarks/classwise_and_power_results.json](benchmarks/classwise_and_power_results.json) |
| **Peak Memory Footprint (Llama Active)** | **1,634 MB** | [benchmarks/memory_summary.md](benchmarks/memory_summary.md) |
| **Llama 3.2 Time to First Token (TTFT)** | **9,680.2 ms** | [benchmarks/speed_summary.md](benchmarks/speed_summary.md) |
| **Internet Permissions** | **0** (`INTERNET` forbidden) | [benchmarks/aapt2_permissions.txt](benchmarks/aapt2_permissions.txt) |

*Tested on one phone (POCO M4 5G, 6 GB).*

---

## Screenshot Gallery

### Core Application Flow
| 1. Language Select | 2. Crop Select | 3. Camera Scan | 4. Top-1 Diagnosis |
| :---: | :---: | :---: | :---: |
| ![Language](docs/screenshots/flow_01_language_selection.png) | ![Crop](docs/screenshots/flow_02_crop_selection.png) | ![Camera](docs/screenshots/flow_03_camera_scan.png) | ![Diagnosis](docs/screenshots/flow_04_result_diagnosis.png) |

| 5. Certainty & Severity | 6. Treatment Tiers | 7. Llama Explanation | 8. Share Case Report |
| :---: | :---: | :---: | :---: |
| ![Certainty](docs/screenshots/flow_05_certainty_severity.png) | ![Treatments](docs/screenshots/flow_06_treatment_tiers.png) | ![Llama](docs/screenshots/flow_07_llama_explanation.png) | ![Share](docs/screenshots/flow_08_case_report_share.png) |

| 9. Extension Referral | 10. Can't Tell (Non-Crop) | 11. AI Model Manager | 12. Telugu Localized |
| :---: | :---: | :---: | :---: |
| ![Referral](docs/screenshots/flow_09_extension_referral.png) | ![Can't Tell](docs/screenshots/flow_10_cant_tell_non_crop.png) | ![Models](docs/screenshots/flow_11_install_ai_models.png) | ![Telugu](docs/screenshots/flow_12_result_telugu.png) |

| 13. Hindi Localized | 14. Airplane Mode (100% Offline) | 15. Offline Chat Q&A | 16. Voice-to-Text Input |
| :---: | :---: | :---: | :---: |
| ![Hindi](docs/screenshots/flow_13_result_hindi.png) | ![Offline](docs/screenshots/flow_14_airplane_mode_offline.png) | ![Chat](docs/screenshots/flow_15_chat_question_answer.png) | ![Voice](docs/screenshots/flow_16_voice_to_text.png) |

| 17. Read-Aloud |
| :---: |
| ![Read Aloud](docs/screenshots/flow_17_read_aloud.png) |

### Supported Class Diagnoses (Physical Device Captures)
| 01. Rice Leaf Blast | 02. Rice Healthy | 03. Cotton Bacterial Blight | 04. Cotton Healthy |
| :---: | :---: | :---: | :---: |
| ![Rice Blast](docs/screenshots/classes/01_rice_leaf_blast.png) | ![Rice Healthy](docs/screenshots/classes/02_rice_healthy.png) | ![Cotton Blight](docs/screenshots/classes/03_cotton_bacterial_blight.png) | ![Cotton Healthy](docs/screenshots/classes/04_cotton_healthy.png) |

| 05. Chilli Leaf Curl | 06. Chilli Healthy | 07. Maize Northern Blight | 08. Maize Common Rust |
| :---: | :---: | :---: | :---: |
| ![Chilli Curl](docs/screenshots/classes/05_chilli_leaf_curl_virus.png) | ![Chilli Healthy](docs/screenshots/classes/06_chilli_healthy.png) | ![Maize Blight](docs/screenshots/classes/07_maize_northern_leaf_blight.png) | ![Maize Rust](docs/screenshots/classes/08_maize_common_rust.png) |

| 09. Maize Healthy | 10. Maize Fall Armyworm | 11. Rice Stem Borer | 12. Rice Hispa |
| :---: | :---: | :---: | :---: |
| ![Maize Healthy](docs/screenshots/classes/09_maize_healthy.png) | ![Fall Armyworm](docs/screenshots/classes/10_maize_fall_armyworm.png) | ![Stem Borer](docs/screenshots/classes/11_rice_stem_borer.png) | ![Rice Hispa](docs/screenshots/classes/12_rice_hispa.png) |

| 13. Tomato Healthy | 14. Tomato Septoria Spot | 15. Other / Non-Crop |
| :---: | :---: | :---: |
| ![Tomato Healthy](docs/screenshots/classes/13_tomato_healthy.png) | ![Tomato Septoria](docs/screenshots/classes/14_tomato_septoria_leaf_spot.png) | ![Other Non Crop](docs/screenshots/classes/15_other_not_a_crop.png) |

### Rejection Gate Example
| CAN'T TELL (Blurry / Non-Leaf / Non-Crop) |
| :---: |
| ![Can't Tell](docs/screenshots/rejections/cant_tell_example.png) |

---

## Repository Structure

```
├── app/                  # Android application source code, UI layouts, and assets
├── ml/                   # Model definition (model.py), thresholds.json, and evaluation scripts
├── notebooks/            # Final Kaggle training notebook (Crop_Doctor_15Class_Kaggle_20ep.ipynb)
├── data/                 # 576-passage knowledge base (kb/), schema, and helpline listings
├── scripts/              # Leakage audits, field testing, and on-device latency benchmarks
├── scripts/setup/        # Automated dataset downloaders and model install utilities
├── benchmarks/           # Measured performance metrics, memory summaries, and verification logs
├── docs/                 # Documentation, number trace (NUMBER_TRACE.md), and model setup guides
├── docs/screenshots/     # Real-device application workflow and 15-class diagnostic screenshots
├── gradle/               # Gradle wrapper binaries and build tooling configuration
└── .github/              # Continuous integration workflow for testing and APK assembly
```

---

## Build and Run

### 1. Build Debug APK
Open the project in Android Studio or compile via command line:
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Run Unit Tests
Verify all 184 unit tests (100% passing):
```bash
./gradlew testDebugUnitTest
```
*(Verified: 184 tests passed, 0 failures, 0 skipped).*

### 3. Install Optional Large AI Models
The core vision classifier (`vision.pte`, 16.1 MB) is pre-bundled in `app/src/main/assets/models/`. To enable optional on-device Llama 3.2 1B natural language explanations and Vosk offline voice transcription, follow the push instructions in [docs/INSTALL_MODELS.md](docs/INSTALL_MODELS.md):
```bash
python scripts/setup/install_llama.py
python scripts/setup/download_vosk.py
```

### 4. Verify Zero Internet Permissions
Verify Rule H1 (no `android.permission.INTERNET` in manifest):
```bash
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk
```
Expected output contains only `CAMERA` and `RECORD_AUDIO`. Zero internet permissions.

---

## Reproduce Training

The vision model is trained using MobileNetV3-Large with linear classification head and temperature scaling:
1. **Download Datasets:** Run `python scripts/setup/download_datasets.py` to acquire raw public vision datasets (note: raw datasets are not stored in the repository due to size and license restrictions).
2. **Execute Training Notebook:** Open [notebooks/Crop_Doctor_15Class_Kaggle_20ep.ipynb](notebooks/Crop_Doctor_15Class_Kaggle_20ep.ipynb) on Kaggle with 2x T4 GPU. Preprocessing resizes images to 512x512 with ImageNet normalization.
3. **Export ExecuTorch Model:** The notebook executes 20 epochs with cosine annealing learning rate, runs grouped validation to prevent data leakage, calibrates temperature (T = 0.9433), and exports `vision.pte`.

---

## Known Limitations

- **Small Benchmark Sample:** The physical real-field validation was conducted on a curated test set of 28 high-confidence field photographs.
- **Single-Source Data Dependency:** Certain pest classes (e.g., Rice Hispa, Rice Stem Borer) rely on single public research repositories.
- **Heuristic Severity:** Lesion area percentage estimation is visual and heuristic; it has not undergone formal clinical trial evaluation by agronomists.
- **Pending Extension Review:** While all 576 knowledge base entries are extracted directly from published agricultural extension guides, formal peer review by university agronomists is ongoing.
- **Limited Offline Telugu Speech:** On-device Telugu speech recognition is limited in vocabulary; complex sentences utilize pre-rendered native audio clips or fallback text display.
- **Non-Commercial Academic Use:** Portions of the training data (e.g., PlantDoc) are licensed under CC BY-NC 4.0, restricting this model artifact to academic, educational, and non-commercial research use.
- **Single Test Hardware Platform:** All power and inference benchmarks were evaluated on a single physical smartphone model (POCO M4 5G, MediaTek Dimensity 700).

---

## Licences and Credits

- **Codebase:** Licensed under the [Apache License, Version 2.0](LICENSE).
- **Llama 3.2 1B Instruct:** Licensed under the [Llama 3.2 Community License](https://github.com/meta-llama/llama-models/blob/main/models/llama3_2/LICENSE). *Built with Llama.*
- **Vosk Speech Recognition:** Licensed under the [Apache License, Version 2.0](https://github.com/alphacep/vosk-api/blob/master/COPYING).
- **Noto Sans Fonts:** Licensed under the [SIL Open Font License, Version 1.1](https://scripts.sil.org/OFL).
- **Datasets:** PlantVillage, PlantDoc, Kaggle, AI Agri Care Field Dataset (complete attribution in [docs/CREDITS.md](docs/CREDITS.md)).

---

## Team & Mentors

- **Team Members:** `<team members>`
- **Project Mentor:** `<mentor>`
