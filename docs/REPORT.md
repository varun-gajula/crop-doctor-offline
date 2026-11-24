# Technical Report: Agri Care AI (Offline)
**Arm Bharat AI-SoC Challenge 2026–27**  
**Problem Statement PS1**: Offline Crop Disease/Pest Detection and Advisory for Farmers  
**Primary Benchmark Hardware**: 64-bit Arm CPU (`arm64-v8a`), 6 GB RAM reference Android phone (Xiaomi POCO M4 5G `22041219PI`, MediaTek Dimensity 700)  
**Secondary Test Hardware**: 64-bit Arm CPU (`arm64-v8a`), 12 GB RAM Android phone (Motorola Edge 60 `ZA222YGPNG`) — used for installation and functional checks only  
**Repository**: https://github.com/varun-gajula/crop-doctor-offline  
**Deliverable**: Technical Report  

---

## 1. Executive Summary

### Problem
Smallholder farmers in rural India face severe, rapid crop losses from foliar pathogens and pests. Existing diagnostic tools depend on cloud APIs that fail in disconnected fields lacking mobile signal, while commercial apps output raw Latin binomials without actionable, safe, local-language advisory.

### Approach
**Agri Care AI** is a fully autonomous on-device diagnostic and advisory system operating with **0 network permissions** (`android.permission.INTERNET: NOT FOUND`, `aapt2` verified). Running offline on a 64-bit Arm CPU (`arm64-v8a`) via a decoupled two-lane architecture:
- **Vision Lane**: MobileNetV3-Large (**16.15 MB**, 512x512 RGB) compiled for **ExecuTorch 0.6.0** (XNNPACK SIMD), classifying **15 categories** across 5 staple Indian crops (9 foliar diseases, 3 economic pests: Maize Fall Armyworm, Rice Yellow Stem Borer, Rice Hispa, healthy leaves, and non-crop guardrail `other_not_a_crop`).
- **Advisory Lane**: Meta's **Llama 3.2 1B-Instruct** 4-bit INT4 **SpinQuant** (**1.06 GiB**, 1,135,951,488 bytes) streaming conversational agronomic explanations on Arm CPU.
- **Deterministic Safety Core**: Rules engine and trilingual knowledge base in **Telugu (తెలుగు)**, **Hindi (हिन्दी)**, and **English** (576 SQLite FTS5 passages; ICAR, PJTSAU, ANGRAU). Verified guidance displays immediately upon shutter release; Llama streams underneath strictly as an explanatory aid. Llama never decides severity, urgency, or dosages; dosages require human-verified records (`dose_verified: true`); Telugu advice is 100% assembled from verified local knowledge base records.

### Five Key Results
1. **Diagnostic Accuracy**: **97.95% overall** (**98.18% macro**) on 15 classes (grouped validation); **66.1% recall (115 of 174 photos)** on noisy out-of-distribution PlantDoc field benchmark.
2. **Field Rejection**: Calibrated free energy ($-4.5866$) and crop-mass gates reject **96.7% (29 of 30)** non-crop photos and **72.9% (35 of 48)** off-target crops to `CAN'T TELL` / `OUT OF CLASS`.
3. **Edge Speed**: Vision load time is **116 ms**; double-scan average spinner latency is **238.9 ms** (min **196 ms**, max **398 ms**). Llama achieves **9,680.2 ms** TTFT (best: **8,152 ms**) and **7.27 tok/s** on Arm CPU after prompt trimming.
4. **6 GB RAM Budget**: Process peaks at **1,634 MB PSS** during active generation (**1,678 MB** during voice input) and **186.81 MB** for vision only (**147.68 MB** cold launch idle). 60s idle eviction reclaims **~1.45 GB** native RAM to a **243.79 MB** baseline.
5. **Safety Integrity**: Zero network permissions. Recommendations cite CIB&RC-registered actives with safety warnings; severe cases route to Kisan Call Centre (`1800-180-1551`).

### Hardware Used
- **POCO M4 5G (`22041219PI`, MediaTek Dimensity 700)**: 64-bit Arm CPU (`arm64-v8a`: 2x Cortex-A76 + 6x Cortex-A55), 6 GB RAM (**5,597 MB** available to OS), Android 14 (primary 6 GB benchmark device).
- **Motorola Edge 60 (`ZA222YGPNG`)**: 64-bit Arm CPU (`arm64-v8a`), **12 GB** RAM, Android 14 (secondary functional check device).

---

## 2. Problem and Requirements Traceability

### 2.1 The Smallholder Farming Challenge
Smallholder farmers in semi-arid and rural regions of Telangana, Andhra Pradesh, and broader India operate under extreme constraints. Foliar fungal pathogens (such as Rice Blast), bacterial blights, viral complexes, and invasive pests (such as Fall Armyworm) can defoliate crops within days. Existing commercial smartphone apps rely on cloud-hosted computer vision APIs that fail completely in remote fields lacking 4G/5G signal. Furthermore, commercial tools frequently output technical pathogen names without explaining practical, region-specific next steps or distinguishing between mild initial infections and catastrophic emergencies.

### 2.2 Traceability to Problem Statement PS1
Every requirement from the official Bharat AI-SoC Challenge Problem Statement PS1 is mapped to an implemented system feature and verified by benchmark evidence:

| Official Challenge Requirement | System Feature & Architectural Implementation | Implementation Status | Verification Evidence & Benchmark Log |
|---|---|:---:|---|
| **Offline Operation (No Internet)** | Manifest has zero network permissions. Entire diagnostic flow executes in airplane mode. | **PASS** | `benchmarks/permissions_check.txt` (`android.permission.INTERNET: NOT FOUND`), `benchmarks/airplane_mode_test.md` |
| **Crop Disease Identification** | 15-class MobileNetV3-Large classifier trained on 5 staple crops (Rice, Cotton, Chilli, Tomato, Maize). | **PASS** | `benchmarks/final/metrics.json` (97.95% grouped validation accuracy), `benchmarks/d6_class_audit.md` |
| **Pest and Disease Detection** | 3 committed economic insect pest classes: Maize Fall Armyworm, Rice Yellow Stem Borer, Rice Hispa. | **PASS** | `benchmarks/d5b_pest_audit.md`, `benchmarks/final/metrics.json` (100% recall on Stem Borer, 97.3% on FAW) |
| **Severity & Urgency Classification** | Dual-factor matrix: HSV necrotic lesion area (< 10%, 10% – 25%, > 25%) + farmer field-spread response (`Few`, `Some`, `Most`). | **PASS** | `app/src/test/java/com/cropdoctor/AgronomicRulesEngineTest.kt` (100% unit test pass across all combinations) |
| **Treatment & Practical Advisory** | 3-tiered guidance: Cultural/Prevention, Natural/Bio-pesticides, CIB&RC Chemical actives. Tiers ordered by urgency. | **PASS** | `app/src/main/assets/kb.json`, `docs/KB_REVIEW_CHECKLIST.md`, `benchmarks/u1_screenshots/` |
| **Region-Specific Recommendations** | Multi-region picker: Telangana (PJTSAU), Andhra Pradesh (ANGRAU), Other/National (ICAR). | **PASS** | `app/src/main/assets/kb.json`, `benchmarks/u1_screenshots/result_diagnose_te.png` |
| **Multilingual Vernacular Delivery** | Telugu, Hindi, English full UI and knowledge-base parity. Telugu is never generated by LLM. | **PASS** | `app/src/test/java/com/cropdoctor/LanguageParityTest.kt`, `benchmarks/a1_result_te.png`, `benchmarks/a1_result_hi.png` |
| **On-Device Advisory Runtime (ExecuTorch)** | Llama 3.2 1B-Instruct SpinQuant INT4 running on 64-bit Arm CPU via ExecuTorch XNNPACK. | **PASS** | `benchmarks/lat1_summary.md` (9,680.2 ms TTFT, 7.27 tok/s on Dimensity 700 CPU), `benchmarks/phone_v15_summary.md` |
| **Extension Worker Support & Referral** | Emergency referral card to Kisan Call Centre (`1800-180-1551`) and shareable case reports via Android share sheet. | **PASS** | `app/src/main/java/com/cropdoctor/ui/ShareReportHelper.kt`, `benchmarks/u1_screenshots/result_cant_tell_en.png` |
| **Offline Voice Interaction** | Vosk 0.3.47 offline Kaldi speech recognition running on 16 kHz mono audio on Arm CPU. | **PASS** | `benchmarks/integration_poco_results.json` (peak speech PSS 1,678 MB; decoupled lifecycle) |
| **Hardware Memory Envelope (6 GB RAM Budget)** | App peak memory strictly capped below 2.5 GB PSS with Llama loaded; below 500 MB without. | **PASS** | `benchmarks/integration_poco_results.json` (1,634 MB peak with Llama; 186.81 MB vision only) |

---

## 3. System Design

### 3.1 Two-Lane Edge Architecture
Agri Care AI enforces a strict architectural separation between sensory perception (Lane 1: Vision) and linguistic reasoning (Lane 2: Advisory). Both lanes communicate solely through structured data contracts and the offline knowledge base:

```
                            [ Leaf or Pest Photo ]
                                      │
                                      ▼
        ┌─────────────────────────────────────────────────────────────┐
        │              LANE 1: ON-DEVICE VISION PIPELINE              │
        │  MobileNetV3-Large (512x512) · ExecuTorch 0.6.0 (XNNPACK)    │
        │  - Viewfinder Center Crop (fraction=0.37)                   │
        │  - Planar Normalization · Arm Neon SIMD Inference           │
        │  - Temperature Scaling Calibration (T = 0.9433)             │
        │  - Helmholtz Free Energy Calculation (E_thresh = -4.5866)   │
        └──────────────────────────────┬──────────────────────────────┘
                                       │ Raw Logits & Energy
                                       ▼
        ┌─────────────────────────────────────────────────────────────┐
        │             DETERMINISTIC AGRONOMIC RULES ENGINE            │
        │  - Decision Gate: DIAGNOSE / CAN'T TELL / OUT OF CLASS      │
        │  - Certainty: Sure / Probably / Not sure                    │
        │  - Lesion Area Fraction + Field Spread Question             │
        │  - Urgency Rating: Early warning / Act this week / Act now  │
        └──────────────────────────────┬──────────────────────────────┘
                                       │ Structured Finding
                                       ▼
        ┌─────────────────────────────────────────────────────────────┐
        │               OFFLINE LOCAL KNOWLEDGE BASE                  │
        │  - 576 Passages indexed in SQLite FTS5 with BM25            │
        │  - Sourced from ICAR, PJTSAU, ANGRAU (Pending Expert Review)│
        │  - Trilingual Structured Content (English, Telugu, Hindi)   │
        └──────────────────────────────┬──────────────────────────────┘
                                       │ Structured Result First (Diagnosis & Advice)
                       ┌───────────────┴───────────────┐
                       ▼                               ▼
        ┌──────────────────────────────┐ ┌─────────────────────────────┐
        │       RESULT SCREEN UI       │ │ LANE 2: ADVISORY REASONING  │
        │ - Diagnosis & Urgency Badge  │ │ Llama 3.2 1B SpinQuant INT4 │
        │ - Calibrated Certainty Rating│ │ ExecuTorch XNNPACK Arm CPU  │
        │ - Sourced Prevention Tier    │ │ (1.06 GiB / 1,135,951,488 B)│
        │ - Sourced Natural/Bio Tier   │ │ - MemoryBudgetDecider Guard │
        │ - CIB&RC Chemical & Safety   │ │ - Concurrency Mutex Lock    │
        │ - Extension Referral Action  │ │ - Grounded BM25 Passages    │
        └──────────────────────────────┘ │ - Active Regex Guardrail    │
                                         └──────────────┬──────────────┘
                                                        │ Streamed Tokens
                                                        ▼
                                         [ In-Process Guardrail Filter ]
                                         - Blocks ungrounded dosages
                                         - Fallback: "Offline guide"
```

#### End-to-End Component Functions and Data Movement Lifecycle
The complete runtime flow transitions through eight decoupled stages with strict data movement boundaries:
1. **Camera Frame Acquisition (`CameraX`)**: The live camera stream acquires high-resolution sensor frames. Prior to downsampling, a center crop of fraction $0.37$ is extracted to isolate the leaf area framed within the UI viewfinder, discarding irrelevant peripheral backgrounds.
2. **Tensor Transformation (`ImagePreprocessor.kt`)**: The center-cropped bitmap is resized via bilinear interpolation to $512 \times 512$ RGB. Pixel color values are converted to float32 planar memory layout $[1, 3, 512, 512]$ and normalized using standard ImageNet parameters ($\mu = [0.485, 0.456, 0.406], \sigma = [0.229, 0.224, 0.225]$).
3. **Vision Model Inference (`Classifier.kt`)**: The normalized planar tensor is dispatched to the ExecuTorch C++ runtime via JNI. MobileNetV3-Large forward pass executes across Arm Cortex-A76 performance cores utilizing XNNPACK vector SIMD instructions, producing a raw 15-element logit vector.
4. **Logit Calibration & Free Energy Calculation (`EnergyCalculator.kt`)**: Raw logits $z_i$ are scaled by temperature $T = 0.9433$. The Helmholtz free energy scalar $E(x) = -T \cdot \text{ln}(\sum \text{exp}(z_i / T))$ is computed via numerically stable LogSumExp. Calibrated class probabilities are derived via softmax over scaled logits.
5. **Deterministic Decision Gate Evaluation (`DecisionGate.kt`)**:
   - *Gate 1 (`LeafAreaValidator.kt`)*: Generates an HSV vegetative mask to verify that leaf matter exceeds the background threshold.
   - *Gate 2 (`EnergyCalculator.kt`)*: Rejects out-of-distribution inputs if free energy exceeds $-4.5866$ (routing to `CAN'T TELL`).
   - *Gate 3 (`DecisionGate.kt`)*: Enforces target crop probability mass ($\ge 0.60$), decision margin between top-1 and top-2 predictions ($\ge 0.25$), and top-1 confidence ($\ge 0.75$).
   - Returns a structured diagnostic outcome: `DIAGNOSE`, `OUT_OF_CLASS`, or `CANT_TELL`.
6. **Lesion Severity & Urgency Estimation (`LesionSeverityEstimator.kt`)**: Segments necrotic and chlorotic pixels in HSV space ($H \in [10^\circ, 45^\circ] \cup [0^\circ, 10^\circ]$) relative to total green foliage to compute lesion area fraction ($< 10\%$ Mild, $10\% - 25\%$ Moderate, $> 25\%$ Severe). Merges the visual metric with the farmer's one-tap field spread selection to generate a deterministic urgency rating: *Early warning*, *Act this week*, or *Act now*.
7. **Knowledge Base Retrieval (`AndroidSqlitePassageDatabase.kt`)**: The verified class key queries the local SQLite FTS5 database. 576 university-sourced passages (ICAR, PJTSAU, ANGRAU) are retrieved using BM25 full-text indexing for the user's selected language (Telugu, Hindi, English) and region (Telangana, Andhra Pradesh, Other).
8. **UI Presentation & Asynchronous Advisory (`ResultScreen.kt` & `Advisor.kt`)**:
   - The diagnosis badge, urgency rating, certainty level, and verified treatment tiers (Cultural, Bio, Chemical) are rendered on screen first.
   - In a background coroutine, `MemoryBudgetDecider.kt` verifies that available RAM is $\ge 1,500\text{ MB}$. If safe, `ExecuTorchLock.forwardMutex` is acquired, and ExecuTorch initializes `LlmModule` with 4-bit SpinQuant Llama 3.2 1B weights (`llama-3.2-1b-spinquant.pte`).
   - A concise 159-token context (system instructions + finding + top BM25 passages) is dispatched. Generated tokens stream into the UI asynchronously.
   - `GuardrailFilter.kt` monitors streamed text; any ungrounded dosage or unapproved chemical halts LLM generation and reverts to the verified knowledge-base guide.

### 3.2 Deterministic Decision Gates & Outcomes
To prevent "hallucinated diagnoses" on unfamiliar foliage or non-crop objects, raw model logits are processed through three sequential gates:
1. **Gate 1: Leaf Area Validation**: An HSV color mask validates that the central sensor crop contains sufficient vegetative plant matter. If non-plant background dominates, the app flags a soft warning.
2. **Gate 2: Helmholtz Free Energy OOD Scoring**: The model computes Helmholtz free energy $E(x) = -T \cdot \text{ln}(\sum \text{exp}(z_i / T))$. Here, temperature ($T = 0.9433$) and free energy threshold ($E_{\text{thresh}} = -4.5866$) were calibrated directly on the 15-class validation split. Unfamiliar leaves, off-target crop species, and non-crop textures produce abnormally high free energy ($E > -4.5866$; borderline energy cutoff $-9.0$, carried over from earlier 20-class calibration without 15-class recalibration) and are routed to `CAN'T TELL`.
3. **Gate 3: Crop-Mass & Ambiguity Margin Filter**: When the farmer selects a target crop (e.g., Rice), the sum of softmax probabilities across all classes belonging to that crop must meet $\ge 0.60$ (`crop_mass_threshold`), and the difference between top-1 and top-2 probabilities within that crop must exceed $0.25$ (`margin_threshold`). Both $0.60$ and $0.25$ were carried over from the earlier 20-class calibration without recalibration for the 15-class model. In addition, the top-1 calibrated softmax probability must meet or exceed $0.75$ (`confidence_threshold`), which was calibrated on the 15-class model, matching `thresholds.json`.

These gates yield three definitive, unit-tested outcomes:
- **`DIAGNOSE`**: The finding is accepted with high confidence and low energy. The UI displays the verified pathogen/pest name, certainty band (`Sure` / `Probably`), severity rating, and official treatments.
- **`OUT OF CLASS`**: The plant belongs to an unsupported species or the wrong crop was selected in picker mode. The UI clearly states: *"Out of class - plant does not match"*.
- **`CAN'T TELL`**: Energy is high, confidence is low, or ambiguous symptoms are detected. The UI displays *"Can't tell - ask an extension officer"* and provides a one-tap dialer to the Kisan Call Centre (`1800-180-1551`).

### 3.3 Deterministic Advisory Boundaries & Safety Rules
Llama 3.2 1B-Instruct serves strictly as a communication aid, never an authority. Its input context is populated exclusively with the structured finding and top-matching passages retrieved from the local SQLite FTS5 knowledge base. An in-process safety guardrail (`GuardrailFilter.kt`) scans Llama's streaming output: if Llama attempts to hallucinate numerical chemical dosages or unknown trade names, the stream is aborted and replaced with the static, verified text from the knowledge base (*"Offline guide"*). Telugu text is never generated by Llama; it is constructed deterministically from human-reviewed knowledge-base strings.

---

## 4. Methodology

### 4.1 Data Provenance and Dataset Audit
The vision training dataset was compiled from 10 distinct open-access repositories, subjected to rigorous provenance and licensing checks:

| Source Dataset | Primary Contributing Repositories | License | Commercial Compatibility | Audited Classes Fed |
|---|---|:---:|:---:|---|
| **PaddyDoctor** | Tamil Nadu smartphone field captures (IEEE DataPort) | CC BY 4.0 | Permitted | Rice blast, Rice healthy, Rice stem borer, Rice hispa |
| **Project-AgML** | Open-access rice disease benchmark | Apache 2.0 | Permitted | Rice blast, Rice healthy |
| **Mendeley Cotton** | Mendeley datasets `74jsdxtmx2`, `t9hgvk2h9p`, `b3jy2p6k8w` | CC BY 4.0 | Permitted | Cotton bacterial blight, Cotton healthy |
| **Krishna Chilli** | Mendeley dataset `ymt8k9bjkn` (Andhra Pradesh field captures) | CC BY 4.0 | Permitted | Chilli leaf curl virus, Chilli healthy |
| **MLD Maize** | Mendeley dataset `3myfctrgk3` | CC BY 4.0 | Permitted | Maize northern leaf blight, Maize healthy, Maize fall armyworm |
| **CCMT Maize** | Mendeley dataset `bwh3zbpkpv` | CC BY 4.0 | Permitted | Maize fall armyworm, Maize northern leaf blight, Maize healthy |
| **PlantVillage** | Penn State open repository | CC BY-NC 3.0 | Non-Commercial / Research | Maize common rust |
| **Archive2** | Kaggle open field tomato collection | CC BY-SA 4.0 | Permitted | Tomato septoria leaf spot, Tomato healthy |
| **DeepWeeds** | Nature Scientific Reports open agricultural weed benchmark | CC BY 4.0 | Permitted | Non-crop guardrail (`other_not_a_crop`) |
| **PlantDoc** | IIT Delhi real field benchmark | MIT | Permitted | Maize northern leaf blight (strictly isolated for noisy stress test) |

### 4.2 Near-Duplicate Clustering & Confound Resolution (Baseline vs. Clustered Dataset)
A critical scientific finding emerged during model iteration:
1. **The Early Unclustered Baseline**: In early training iterations without cluster deduplication, cross-source validation failed catastrophically. When individual repositories were held out, held-out recall dropped to between **2.5% and 41.8% across 5 classes** in baseline validation. The deep network had memorized camera sensor noise, uniform background soil, and synthetic letterbox padding rather than biological foliar pathology.
2. **The Clustered Dataset Rebuild**: To permanently resolve source confounding:
   - **Perceptual Hash Clustering**: All images were audited using 64-bit Dihedral Perceptual Hashing (D₄ group: 4 rotations × horizontal mirror) with single-linkage clustering at Hamming distance ≤ 8 (`benchmarks/d6_class_audit.md`). An 80/20 train/validation split was enforced strictly at the cluster level, guaranteeing zero near-duplicate leakage.
   - **Letterbox Border Trimming**: A variance-based scanning algorithm detected and trimmed synthetic gray border padding across **1,085 photos (10.89% of corpus)** before resizing (`benchmarks/data_fix_source_table.md`).
   - **Source-Balanced Sampling**: For multi-source classes, contributing sources were sampled in equal proportions (e.g., Maize Fall Armyworm was balanced at exactly 285 MLD photos [50.0%] and 285 CCMT photos [50.0%], balancing leaf blade puncture damage with whorl feeding damage).
   - **Domain-Shift Augmentation**: Heavy data augmentation was applied during training: `RandomResizedCrop(scale 0.5-1.0)`, `ColorJitter(0.2)`, `GaussianBlur`, `RandomDownscaleUpscale(p=0.3)`, and `RandomJPEGCompression(p=0.3)`.
   - **Class Cap**: All classes were capped at ≤ 800 photos, resulting in a clean, balanced corpus of **9,792 training/validation photos** (`benchmarks/data_fix_source_table.md`).

### 4.3 Model Architecture & Edge Compilation
- **Backbone**: MobileNetV3-Large with Hard-Swish non-linearities and Squeeze-and-Excitation attention modules in the final stages.
- **Input Dimensions**: 1 × 3 × 512 × 512 RGB planar tensor, normalized with ImageNet statistics (μ = [0.485, 0.456, 0.406], σ = [0.229, 0.224, 0.225]).
- **Compilation**: Lowered to PyTorch Edge IR and exported to ExecuTorch 0.6.0 with the XNNPACK CPU backend. The compiled binary size is **16.15 MB** (`benchmarks/final/metrics.json`), passing the 30 MB model size ceiling with 46% margin.
- **Numerical Parity**: Maximum absolute parity error between PyTorch eager execution and ExecuTorch runtime on Arm64 is **2.95 × 10⁻⁷**, confirming zero degradation from graph export (`benchmarks/final/metrics.json`).
- **Advisory Model Binary**: Llama 3.2 1B-Instruct quantized via SpinQuant INT4 is **1.06 GiB** (1,135,951,488 bytes) on disk (`docs/INSTALL_MODELS.md`, `benchmarks/llama_speed.md`).

### 4.4 Lesion Area Severity Estimation
Foliar severity is calculated dynamically using an HSV color segmentation algorithm:
$$\text{Lesion Fraction} = \frac{\text{Count}(\text{Necrotic / Chlorotic Pixels})}{\text{Count}(\text{Total Vegetative Pixels})}$$
The necrotic mask targets brown, black, yellow, and rust hues (H ∈ [10°, 45°] ∪ [0°, 10°], low V or high S). Damage is banded into:
- **Mild**: < 10% leaf damage
- **Moderate**: 10% – 25% leaf damage
- **Severe**: > 25% leaf damage

This visual metric is combined with the farmer's one-tap answer to *"How much of the field is affected?"* (`A few plants` / `Some areas` / `Most of field`) to determine the urgency rating:
- Mild damage + Few plants ⇒ **Early warning**
- Moderate damage OR Some areas ⇒ **Act this week**
- Severe damage OR Most of field ⇒ **Act now**

*Methodological Note: While biologically grounded, this image-processing heuristic has not been clinically validated against certified plant pathologists' Disease Severity Index (DSI) ratings.*

### 4.5 Trilingual Knowledge Base Architecture
The local knowledge base (`app/src/main/assets/kb.json`) contains structured entries across all 15 categories. Each entry is curated from official literature:
1. **PJTSAU** (*Vyavasaya Panchangam*): Telangana regional agro-climatic practices.
2. **ANGRAU** (*Vyavasaya Darsini*): Andhra Pradesh regional practices.
3. **ICAR Research Directorates**: National compendiums from ICAR-IIRR (Rice), ICAR-CICR (Cotton), ICAR-IIMR (Maize), and ICAR-IIHR (Horticulture).

Each problem provides three structured intervention tiers:
- **Tier 1: Cultural & Preventive**: Field sanitation, crop rotation, spacing, balanced nitrogen application.
- **Tier 2: Natural & Biological**: Neem oil (Azadirachtin 1500 ppm), *Trichoderma viride*, *Pseudomonas fluorescens*, pheromone traps, bird perches.
- **Tier 3: Registered Chemical Interventions**: CIB&RC-registered active ingredients (e.g., Mancozeb, Hexaconazole, Chlorantraniliprole, Emamectin Benzoate). Dosages are displayed only when backed by human-checked citations (`dose_verified: true`), otherwise defaulting to *"follow the product label"*.

All entries carry the formal status **"sourced, pending expert review"**.

---

## 5. Experimental Results

### 5.1 Grouped Cluster Validation Results (Final 15-Class Model)
The final 15-class MobileNetV3-Large model was evaluated on the held-out cluster validation split (**1,647 photos** across 15 classes). Clusters were isolated by perceptual hash Hamming distance ≤ 8, ensuring zero image overlap between train and validation:

| # | Class Name | Category | Support (N) | Correct (n) | Recall | Precision | F1-Score |
|:---:|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | `rice_leaf_blast` | Disease | 147 | 139 | 94.56% | 97.20% | 0.9586 |
| 2 | `rice_healthy` | Healthy | 151 | 149 | 98.68% | 96.13% | 0.9739 |
| 3 | `cotton_bacterial_blight` | Disease | 97 | 97 | 100.00% | 97.98% | 0.9898 |
| 4 | `cotton_healthy` | Healthy | 135 | 133 | 98.52% | 100.00% | 0.9925 |
| 5 | `chilli_leaf_curl_virus` | Disease | 54 | 53 | 98.15% | 100.00% | 0.9907 |
| 6 | `chilli_healthy` | Healthy | 51 | 51 | 100.00% | 98.08% | 0.9903 |
| 7 | `maize_northern_leaf_blight` | Disease | 104 | 100 | 96.15% | 95.24% | 0.9569 |
| 8 | `maize_common_rust` | Disease | 100 | 100 | 100.00% | 100.00% | 1.0000 |
| 9 | `maize_healthy` | Healthy | 80 | 79 | 98.75% | 100.00% | 0.9937 |
| 10 | `maize_fall_armyworm` | **Pest** | 112 | 109 | 97.32% | 96.46% | 0.9689 |
| 11 | `rice_stem_borer` | **Pest** | 125 | 125 | 100.00% | 100.00% | 1.0000 |
| 12 | `rice_hispa` | **Pest** | 119 | 111 | 93.28% | 94.07% | 0.9367 |
| 13 | `tomato_healthy` | Healthy | 99 | 98 | 98.99% | 98.00% | 0.9849 |
| 14 | `tomato_septoria_leaf_spot` | Disease | 119 | 117 | 98.32% | 99.15% | 0.9873 |
| 15 | `other_not_a_crop` | Guardrail | 68 | 68 | 100.00% | 100.00% | 1.0000 |
| **TOTAL** | **Macro / Overall** | — | **1,647** | **1,613** | **97.95%** | **98.18%** | **0.9818** |

*(Source: `benchmarks/final/metrics.json`. These numbers reflect grouped validation on curated datasets where all contributing sources were present in training; they do not represent unconstrained in-field accuracy on uncontrolled mobile captures).*

### 5.2 Out-of-Distribution Noisy Stress Test (PlantDoc Benchmark)
To test robustness against unseen camera sensors and uncurated backgrounds, the model was evaluated on the **PlantDoc Corn leaf blight** benchmark (**174 photos** from IIT Delhi, completely excluded from training):
- **Correct Blight Diagnoses**: **115 of 174 photos (66.1% recall)** (`benchmarks/final/metrics.json`).
- **Prediction Distribution**: Maize Northern Leaf Blight (115), Maize Common Rust (40), Rice Leaf Blast (16), Rice Hispa (3).
- **Stress Test Evaluation**: On uncurated field imagery with complex backgrounds, the model achieved 66.1% (115/174) recall on this out-of-distribution benchmark. The remaining 59 photos were misclassified into other foliar pathology classes (40 common rust, 16 rice blast, 3 rice hispa).

### 5.3 Non-Crop & Out-of-Distribution Rejection Rates
Evaluated on strictly non-overlapping probe sets (`benchmarks/compare_field.md`, `benchmarks/check_field.md`):
- **`nocrop` Rejection Rate on Field Photography**: **96.7% (29 of 30 photos rejected to `CAN'T TELL`)**. All 30 photos are real phone captures taken by the team on Xiaomi `220233L2I` and `2201116SI` devices, with 0 perceptual hash overlap against training data (1 of 30 non-crop photos was falsely accepted).
- **`othercrop` Rejection Rate on Field Photography**: **72.9% (35 of 48 photos rejected to `CAN'T TELL` or `OUT OF CLASS`)**, testing off-target agricultural species (Apple, Grape, Potato, Sugarcane). This represents a marked improvement over the legacy 20-class model, which rejected only 37.5% (18/48), while 13 of 48 off-target photos falsely passed the gates.
- **Non-Crop Classification Performance (Held-Out Test Set vs. Field Photos)**: In grouped cluster validation on the 15-class held-out test set (`benchmarks/final/metrics.json`), the non-crop guardrail class (`other_not_a_crop`) achieved 100.0% precision and 100.0% recall (68/68 correct, with 0 non-crop errors on this held-out set). In contrast, on unconstrained field photography, rejection is not zero-error: 29 of 30 non-crop photos were rejected (1 false positive), and 35 of 48 off-target crop photos were rejected (13 false positives passing the gates).

### 5.4 Decision Threshold Sweep & Selection
A parametric sweep across confidence, free energy, crop mass, and margin thresholds was conducted on clean probe photography (`benchmarks/p4b_options.md`, `benchmarks/p4b_table.csv`):

| Threshold Profile | Conf Thresh | Energy Thresh | Crop Mass | Margin | Supported Diag Acc (n/N, %) | Nocrop Rejection (n/N, %) | Othercrop Rejection (n/N, %) | Unsupported Wrong Diag (n/93, %) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Baseline Thresholds (Selected)** | 0.75 | -4.5866 | 0.60 | 0.25 | **84.2% (16/19)** | **86.7% (26/30)** | **72.9% (35/48)** | 23.7% (22/93) |
| **Balanced Guard Thresholds** | 0.65 | -4.7500 | 0.55 | 0.20 | **81.2% (13/16)** | **86.7% (26/30)** | **77.1% (37/48)** | **19.4% (18/93)** |
| **High-Yield Diagnostic Thresholds** | 0.70 | -4.5000 | 0.60 | 0.20 | **85.0% (17/20)** | **83.3% (25/30)** | **72.9% (35/48)** | 23.7% (22/93) |

Evaluation on 33 clean field photos (`benchmarks/p4b_options.md`) confirmed that diagnostic performance differences across candidate profiles were within one photo; the baseline profile (`confidence_threshold: 0.75`, `energy_threshold: -4.5866`, `crop_mass_threshold: 0.60`, `margin_threshold: 0.25`) was retained.

**Provenance of Threshold Values**: Within the baseline profile, `confidence_threshold` ($0.75$), `energy_threshold` ($-4.5866$), and temperature scaling ($T = 0.9433$) were calibrated directly on the 15-class model grouped validation split. Conversely, `crop_mass_threshold` ($0.60$), `margin_threshold` ($0.25$), and `borderline_energy` ($-9.0$) were carried over from the earlier 20-class model calibration and were not independently recalibrated for the 15-class architecture.

### 5.5 Confusion Matrix Analysis
Analysis of off-diagonal errors in the 15 × 15 validation confusion matrix (`benchmarks/final/metrics.json`) reveals three primary agronomic confusions:
1. **Rice Leaf Blast vs. Rice Hispa (9 total confusions)**: 6 blast samples misclassified as hispa; 3 hispa samples misclassified as blast. Both manifest as narrow, elongated necrotic lesions on green rice leaf blades.
2. **Maize Northern Leaf Blight vs. Maize Fall Armyworm (7 total confusions)**: 4 blight samples misclassified as FAW; 3 FAW samples misclassified as blight. Severe FAW larval feeding strips leaves down to the midrib, visually mimicking elongated necrotic blight lesions.
3. **Tomato Healthy vs. Tomato Septoria Leaf Spot (3 total confusions)**: 1 healthy sample misclassified as septoria; 2 septoria samples misclassified as healthy. Confusions occur during earliest disease onset when minute (< 1 mm) water-soaked spots are difficult to distinguish from natural leaf margin blemishes.

---

## 6. Hardware Utilization and Optimization

### 6.1 Physical Test Device Specifications
All hardware benchmarks were recorded live on physical Android devices. Every device-dependent result is attributed to the exact hardware specifications from `benchmarks/device_comparison.md`:

- **Phone 1 (Primary Benchmark Device - 6 GB Budget Constraint)**:
  - **Commercial Name**: Xiaomi POCO M4 5G
  - **Hardware Model (`ro.product.model`)**: `22041219PI` (Serial: `sseqjnucrkeeprtw`)
  - **SoC (`ro.soc.model`)**: MediaTek Dimensity 700 (`MT6833_5G-C`)
  - **CPU Architecture**: 64-bit Arm CPU (`arm64-v8a`), 8 cores (2x Arm Cortex-A76 @ 2.2 GHz + 6x Arm Cortex-A55 @ 2.0 GHz)
  - **Physical RAM**: 6 GB LPDDR4X (5,597 MB total reported by OS)
  - **OS**: Android 14 (HyperOS, API 34), Linux kernel 5.10.198
  - **Role**: Primary physical benchmark device for all CPU latency, memory PSS, token throughput, and decision gate measurements.
  *(Audit Note: Early logs in `speed_summary.md` and `memory_summary.md` referenced model `22041219PI` under an alternative retail designation 'Redmi Note 11S / Helio G96'; hardware properties confirmed POCO M4 5G / Dimensity 700).*
- **Phone 2 (Secondary Device - Functional Check)**:
  - **Commercial Name**: Motorola Edge 60
  - **Hardware Model (`ro.product.model`)**: `ZA222YGPNG`
  - **CPU Architecture**: 64-bit Arm CPU (`arm64-v8a`)
  - **Physical RAM**: 12 GB RAM
  - **OS**: Android 14 (Display P3 wide-gamut screen)
  - **Role**: Used for installation and functional checks only.

### 6.2 Process Memory Footprint (PSS) on 6 GB Phone
Memory utilization was logged via `dumpsys meminfo com.cropdoctor` on the physical 6 GB POCO M4 5G (`22041219PI`) across all operational lifecycle states (`benchmarks/memory_summary.md`, `benchmarks/integration_poco_results.json`):

| Lifecycle Operational State | Total Process PSS | Java Heap PSS | Native Heap PSS | 6 GB RAM Budget Ceiling | Headroom Below Ceiling | Verification Source Log |
|---|:---:|:---:|:---:|:---:|:---:|---|
| **App Cold Launch (Idle)** | 147.68 MB | 13.57 MB | 17.43 MB | ≤ 500.0 MB | **+352.32 MB** | `benchmarks/memory_summary.md` |
| **Vision Inference Complete** | 186.81 MB | 12.64 MB | 49.32 MB | ≤ 500.0 MB | **+313.19 MB** | `benchmarks/memory_summary.md` |
| **Llama 3.2 1B Resident in RAM** | 1,580.19 MB | 13.82 MB | 1,445.10 MB | ≤ 2,500.0 MB | **+919.81 MB** | `benchmarks/memory_summary.md` |
| **Peak During Active LLM Token Decode** | **1,634.00 MB** | 14.10 MB | 1,565.74 MB | ≤ 2,500.0 MB | **+866.00 MB** | `benchmarks/integration_poco_results.json` |
| **Peak During Voice Input (Vosk Audio)** | **1,678.00 MB** | 14.50 MB | 1,582.00 MB | ≤ 2,500.0 MB | **+822.00 MB** | `benchmarks/integration_poco_results.json` |
| **60s After Idle Eviction (`resetNative`)** | **243.79 MB** | 13.91 MB | 107.29 MB | ≤ 500.0 MB | **+256.21 MB** | `benchmarks/memory_summary.md` |
| **Memory Reclaimed After Release** | **1.45 GB** | — | — | — | — | `benchmarks/memory_summary.md` |

Every stage safely complies with the target 6 GB phone memory ceiling (≤ 2.5 GB peak PSS with Llama loaded, ≤ 500 MB for vision only).

### 6.3 On-Device Latency & Execution Throughput
Measured live on the connected POCO M4 5G (`22041219PI`, MediaTek Dimensity 700 Arm CPU):

- **Vision Model Cold Load Time**: **116 ms** (`benchmarks/phone_v15_summary.md`).
- **Rapid Double-Scan Spinner Latency**: **238.9 ms average** (min 196 ms, max 398 ms across 60 executions on POCO M4 5G in `benchmarks/phone_v15_summary.md`).
- **Sourced Advice Card Presentation**: The vision result (diagnosis badge, certainty indicator, and verified treatment steps) is rendered on screen first upon vision inference completion, followed by Llama's streaming explanation; reading the recommended agronomic practices does not wait for LLM token generation.
- **Llama 3.2 1B-Instruct Generation Metrics (Prompt Trimming Optimization)**:
  - **Average Input Context**: **301.1 tokens** (shortened by -25.3% from 402.9 tokens in `benchmarks/lat1_summary.md`).
  - **Time to First Token (TTFT)**: **9,680.2 ms (~9.7 s)** on Arm CPU (accelerated by -25.5% from 12,986.4 ms; best observed TTFT: **8,152 ms** in `benchmarks/lat1_after_stageB.json`).
  - **Generation Speed**: **7.27 tok/s** on Arm CPU (improved from 6.90 tok/s).
  - **Sentence Completion Rate**: **100% (10 of 10 runs)** terminated cleanly on a grammatically complete sentence verified by `SentenceTrimmer`.
- **Interactive Offline Chat Latency**: **8,222.8 ms average** across 10 agronomic questions (`benchmarks/phone_v15_summary.md`).

### 6.4 Edge Optimization Techniques

To achieve real-time vision inference and responsive local LLM execution within a 6 GB memory envelope on an Arm CPU, eleven concrete optimizations were implemented and measured:

1. **Backbone Model Selection**: MobileNetV3-Large (16.15 MB binary size) was selected over ResNet-18 (42.68 MB). MobileNetV3 is **2.6x smaller** in storage, consumes < 200 MB runtime RAM (versus > 400 MB for ResNet), and remains well within the 6 GB RAM budget while achieving 97.95% accuracy (`benchmarks/head_to_head_comparison.md`).
2. **4-Bit SpinQuant Quantization**: Quantizing Llama 3.2 1B weights from FP16 (> 2.5 GB) to 4-bit INT4-EO8 (**1.06 GiB** / 1,135,951,488 bytes) enabled the model to reside in memory with a peak PSS of only **1,634 MB**, preventing out-of-memory kernel termination on 6 GB devices (`benchmarks/integration_poco_results.json`).
3. **Dynamic On-Demand LLM Eviction**: Llama weights and ExecuTorch native contexts load into RAM only when the user requests an explanation, and are automatically unloaded after **60 seconds of inactivity**. This returns **~1.45 GB of native RAM** to the operating system (`benchmarks/memory_summary.md`).
4. **MemoryBudgetDecider Guard**: Before initializing Llama, `MemoryBudgetDecider.kt` queries the Android `ActivityManager`. If system available RAM is < 1,500 MB, LLM loading is bypassed in **0 ms**, gracefully presenting the static knowledge-base text (*"Offline guide"*) and protecting system stability (`benchmarks/fallback_rate.md`).
5. **Process-Wide Concurrency Mutex (`ExecuTorchLock`)**: A thread-safe mutex (`forwardMutex`) coordinates vision and LLM inference. Vision classification and Llama generation are mutually exclusive, eliminating CPU core contention and thermal throttling on the Arm Cortex-A76 cores.
6. **Active Work Cancellation**: If the user taps the camera shutter while Llama is actively streaming tokens, ongoing LLM generation is aborted via an atomic cancellation flag, freeing CPU cores for the incoming vision pass.
7. **Prompt Trimming Optimization**: System instructions and finding headers were compressed from 402.9 to 301.1 tokens (-25.3%). On the Arm CPU, prefill time dropped from 12,986.4 ms to **9,680.2 ms (-25.5% faster)**, saving over 3.3 seconds per query (`benchmarks/lat1_summary.md`).
8. **Ultra-Short Chat Context Architecture**: Interactive chat queries utilize a streamlined 159-token input context (82-token system instructions + 19-token finding + 44-token top BM25 passage), delivering interactive responses in an average of **8.2 s** on CPU (`benchmarks/lat1_summary.md`).
9. **Viewfinder Preprocessing Crop**: CameraX preview applies a center crop (`fraction=0.37`) matching the model's receptive field prior to bilinear downsampling, eliminating irrelevant field margins.
10. **Decoupled Vosk Speech Lifecycle**: The Vosk Kaldi speech model is instantiated only while the microphone button is pressed and released before Llama initializes. This decoupled lifecycle restricts peak speech PSS to **1,678 MB** without memory overlap (`benchmarks/integration_poco_results.json`).
11. **Fail-Closed Threshold Boundaries**: Combining 15-class calibrated thresholds (free energy -4.5866, confidence 0.75) with carried-over heuristic thresholds from the earlier 20-class calibration (crop mass 0.60, margin 0.25) rejects 29/30 (96.7%) non-crop objects and 35/48 (72.9%) off-target crop leaves on field test photography (`benchmarks/compare_field.md`).

---

## 7. Safety, Honesty, and Guardrails

### 7.1 What Agri Care AI Will NOT Do
To protect rural farmers from agronomic harm and maintain scientific honesty:
- **No Unverified Chemical Dosages**: The application never computes, estimates, or extrapolates spray dosages. Dosages appear only when copied verbatim from published university compendiums with `dose_verified: true`. If unverified, the app displays *"follow the product label"*.
- **No LLM-Generated Prescriptions**: Llama 3.2 is constrained to explaining the biology and cultural context of the diagnosis. An active regex filter (`GuardrailFilter.kt`) intercepts any generated numbers resembling application rates, chemical concentrations, or unapproved trade names.
- **No LLM-Generated Telugu**: Llama 3.2 does not natively support Telugu. Attempting generative Telugu yields hallucinatory phonetics. All Telugu advice is assembled deterministically from reviewed knowledge-base strings.
- **No Simulated AI Code in Production**: All fallback and mock components were permanently excised from release builds. If model weights are missing or unreadable, the app cleanly falls back to the deterministic offline guide.
- **Zero Internet Permissions**: The app cannot transmit telemetry, farmer location, or camera images off the phone. Verified via `aapt2 dump permissions` (`benchmarks/permissions_check.txt`).

### 7.2 Extension Worker Referral
When visual confidence is borderline, when free energy indicates an unfamiliar pathogen, or when necrotic severity is classified as Severe, the application surfaces an emergency extension referral card. The farmer can tap to dial the national **Kisan Call Centre (1800-180-1551)** or export a standardized case report via the Android share sheet for village Krishi Vigyan Kendra (KVK) officers.

---

## 8. Limitations

In accordance with scientific integrity, the limitations of Agri Care AI are documented once in neutral wording:
1. **Field Performance Disparity on Northern Leaf Blight**: Maize Northern Leaf Blight exhibited the weakest diagnostic recall on uncurated field photography (66.1% recall on the PlantDoc stress benchmark, 115/174), frequently confused with common rust (40 instances) or rice blast (16 instances) due to similar elongated necrotic lesion morphology under complex outdoor lighting.
2. **Unvalidated Severity Heuristic**: The HSV necrotic lesion fraction calculation (< 10%, 10% – 25%, > 25%) is an unvalidated engineering heuristic. It has not been clinically correlated against certified plant pathologists' Disease Severity Index (DSI) ratings.
3. **Single-Source Class Representations**: Five of the 15 classes (`chilli_leaf_curl_virus`, `chilli_healthy`, `maize_common_rust`, `tomato_spider_mites`, `other_not_a_crop`) originate from single repository sources without independent cross-source evaluation datasets, presenting residual risk of background memorization across diverse agro-climatic zones.
4. **Vernacular Translation & Agronomic Review**: All Telugu and Hindi knowledge-base entries and vernacular UI strings require formal review and sign-off by native speakers and certified agricultural extension officers to ensure regional dialect accuracy and cultural clarity.
5. **CC BY-NC Dataset Usage**: The vision training corpus incorporates PlantVillage imagery distributed under Creative Commons Attribution-NonCommercial (CC BY-NC 3.0), which restricts commercial deployment and limits the model's current usage to academic, non-commercial, and competition research benchmarking.
6. **Threshold Calibration Scope**: While temperature scaling ($T = 0.9433$), confidence threshold ($0.75$), and free energy threshold ($-4.5866$) were calibrated directly on the 15-class model validation set, the crop mass threshold ($0.60$), decision margin ($0.25$), and borderline energy cutoff ($-9.0$) were carried over from the earlier 20-class calibration and were not independently recalibrated for the 15-class architecture.
7. **Small Real-Photo Field Sample**: Unconstrained in-field evaluation on farmers' phones remains limited in sample size (33 clean field photos, 30 non-crop, 48 other-crop). Broader multi-district agronomic field validation is ongoing.
8. **Acoustic Speech Recognition in Telugu**: While English and Hindi Vosk models achieve high accuracy, the open-source Telugu Kaldi acoustic model exhibits higher word error rates due to limited regional dialect training data.
9. **Pest Severity Constraints**: Lesion area segmentation is optimized for foliar necrosis and is poorly suited for mobile insect counts or boring damage inside maize stalks.

---

## 9. Roadmap

Future development is organized into concrete, staged engineering milestones:
1. **Live Field Diagnostic Accuracy & Extended Multi-District Evaluation**: Systematic validation on uncurated, standing crops in real farming conditions across diverse light, camera angles, and leaf backgrounds to validate in-field generalization beyond laboratory probe sets.
2. **3-Farmer Usability & Field Comprehension Trials**: Conducting structured usability trials with rural smallholders to measure end-to-end task completion times (opening app to understanding advisory under 60 seconds), icon clarity, and audio-visual comprehension across regional dialects.
3. **On-Device Object Detection & Pest Counting**: Evaluating lightweight detection backbones (e.g., MobileNet-SSD or Nano architectures) running on Arm CPU via ExecuTorch to localize foliar lesions and count physical insect larvae.
4. **Crop Portfolio Expansion**: Extending the diagnostic roster beyond the initial 15 categories to cover additional regional pulses (e.g., Red Gram / Pigeonpea) and oilseeds (Groundnut) through curated field photography campaigns.
5. **Native Indic Language Generative Advisory**: Testing emerging quantized Indic small language models on ExecuTorch Arm CPU for direct vernacular generation in Telugu and other regional languages while maintaining deterministic safety guardrails.

---

## 10. Reproducibility, Repository & Licenses

### 10.1 Repository Structure
```
crop-doctor-offline/
├── app/                     # Android Jetpack Compose application (Kotlin)
│   ├── src/main/java/       # Architecture: classifier, advisor, rules engine, UI
│   └── src/main/assets/     # Bundled assets: vision.pte, kb.json, thresholds.json
├── benchmarks/              # Measured benchmark logs, hardware dumps, and JSON runs
├── docs/                    # Technical documentation: REPORT.md, REQUIREMENTS.md, NUMBER_TRACE.md
├── ml/                      # PyTorch training, evaluation, calibration, and export scripts
├── notebooks/               # Production training notebooks (15-class model)
├── scripts/                 # Benchmark automation, hardware runners, traceability tools
├── LICENSE                  # Apache 2.0 open-source code license
└── README.md                # Project documentation and build instructions
```

### 10.2 Build & Execution Instructions
1. **Clone the Public Repository**:
   ```bash
   git clone https://github.com/varun-gajula/crop-doctor-offline.git
   cd crop-doctor-offline
   ```
2. **Build the Production APK**:
   ```bash
   ./gradlew assembleRelease
   ```
3. **Install Advisory AI Models (USB / ADB)**:
   ```bash
   adb push llama-3.2-1b-spinquant.pte /storage/emulated/0/Android/data/com.cropdoctor/files/
   adb push tokenizer.model /storage/emulated/0/Android/data/com.cropdoctor/files/
   ```
   *(Alternatively, use the in-app SAF file picker on the "Install AI Model" screen).*
4. **Execute Verification Benchmarks**:
   ```bash
   python scripts/trace_numbers.py
   python scripts/run_integration_benchmarks.py
   ```

### 10.3 Third-Party Licenses & Attributions
- **Application Code**: Apache License 2.0 (© 2026 Agri Care AI Contributors).
- **Meta Llama 3.2 1B-Instruct**: [Llama 3.2 Community License Agreement](https://llama.meta.com/llama3/license/). *Built with Llama.* Weights are not redistributed in the repository.
- **ExecuTorch & XNNPACK**: BSD 3-Clause License (© 2023–2024 Meta Platforms, Inc. & PyTorch Contributors; © Google LLC).
- **Vosk Offline Speech Recognition**: Apache License 2.0 (© Alpha Cephei Inc.).
- **Typography**: Noto Sans Telugu and Noto Sans Devanagari (SIL Open Font License OFL 1.1).
- **Datasets**: Creative Commons Attribution 4.0 (CC BY 4.0: PaddyDoctor, Mendeley data, Project-AgML, DeepWeeds), MIT License (PlantDoc), and CC BY-NC 3.0 (PlantVillage, restricted to educational/competition benchmarking).

---

## 11. Appendix

### 11.1 Detailed Class-Level Validation Performance
From the final 15-class model evaluation log (`benchmarks/final/metrics.json`):

| Class Identifier | Class Name | Support (N) | Correct (n) | Recall | Precision | F1-Score |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `class_0` | `rice_leaf_blast` | 147 | 139 | 94.56% | 97.20% | 0.9586 |
| `class_1` | `rice_healthy` | 151 | 149 | 98.68% | 96.13% | 0.9739 |
| `class_2` | `cotton_bacterial_blight` | 97 | 97 | 100.00% | 97.98% | 0.9898 |
| `class_3` | `cotton_healthy` | 135 | 133 | 98.52% | 100.00% | 0.9925 |
| `class_4` | `chilli_leaf_curl_virus` | 54 | 53 | 98.15% | 100.00% | 0.9907 |
| `class_5` | `chilli_healthy` | 51 | 51 | 100.00% | 98.08% | 0.9903 |
| `class_6` | `maize_northern_leaf_blight` | 104 | 100 | 96.15% | 95.24% | 0.9569 |
| `class_7` | `maize_common_rust` | 100 | 100 | 100.00% | 100.00% | 1.0000 |
| `class_8` | `maize_healthy` | 80 | 79 | 98.75% | 100.00% | 0.9937 |
| `class_9` | `maize_fall_armyworm` | 112 | 109 | 97.32% | 96.46% | 0.9689 |
| `class_10` | `rice_stem_borer` | 125 | 125 | 100.00% | 100.00% | 1.0000 |
| `class_11` | `rice_hispa` | 119 | 111 | 93.28% | 94.07% | 0.9367 |
| `class_12` | `tomato_healthy` | 99 | 98 | 98.99% | 98.00% | 0.9849 |
| `class_13` | `tomato_septoria_leaf_spot` | 119 | 117 | 98.32% | 99.15% | 0.9873 |
| `class_14` | `other_not_a_crop` | 68 | 68 | 100.00% | 100.00% | 1.0000 |

### 11.2 Final 15-Class Confusion Matrix (15 × 15)
Rows represent true ground-truth classes; columns represent predicted classes (`benchmarks/final/metrics.json`):

```
       [ 0] [ 1] [ 2] [ 3] [ 4] [ 5] [ 6] [ 7] [ 8] [ 9] [10] [11] [12] [13] [14]
 [ 0]   139    1    0    0    0    0    1    0    0    0    0    6    0    0    0
 [ 1]     1  149    0    0    0    0    0    0    0    0    0    1    0    0    0
 [ 2]     0    0   97    0    0    0    0    0    0    0    0    0    0    0    0
 [ 3]     0    0    2  133    0    0    0    0    0    0    0    0    0    0    0
 [ 4]     0    0    0    0   53    1    0    0    0    0    0    0    0    0    0
 [ 5]     0    0    0    0    0   51    0    0    0    0    0    0    0    0    0
 [ 6]     0    0    0    0    0    0  100    0    0    4    0    0    0    0    0
 [ 7]     0    0    0    0    0    0    0  100    0    0    0    0    0    0    0
 [ 8]     0    0    0    0    0    0    1    0   79    0    0    0    0    0    0
 [ 9]     0    0    0    0    0    0    3    0    0  109    0    0    0    0    0
 [10]     0    0    0    0    0    0    0    0    0    0  125    0    0    0    0
 [11]     3    5    0    0    0    0    0    0    0    0    0  111    0    0    0
 [12]     0    0    0    0    0    0    0    0    0    0    0    0   98    1    0
 [13]     0    0    0    0    0    0    0    0    0    0    0    0    2  117    0
 [14]     0    0    0    0    0    0    0    0    0    0    0    0    0    0   68
```

### 11.3 Benchmark Log File References
Every quantitative claim in this report is traceable to raw benchmark logs in the repository:
- `benchmarks/final/metrics.json`: 15-class model validation metrics, stress test results, confusion matrix, and model size.
- `benchmarks/device_comparison.md`: Hardware specifications for POCO M4 5G and Motorola Edge 60.
- `benchmarks/lat1_summary.md` & `benchmarks/lat1_after_stageB.json`: Llama TTFT, token generation throughput, and context reduction data.
- `benchmarks/phone_v15_summary.md`: Vision model load time, rapid scan spinner latency, and chat latency on POCO M4 5G.
- `benchmarks/integration_poco_results.json`: Peak process memory (PSS) with Llama (1,634 MB) and with speech (1,678 MB).
- `benchmarks/memory_summary.md`: Detailed PSS breakdown across app launch, vision inference, Llama residency, and idle eviction.
- `benchmarks/compare_field.md`: Rejection rates on non-crop (96.7%) and other-crop (72.9%) sets.
- `benchmarks/check_field.md`: Probe dataset origin audit and training set overlap analysis.
- `benchmarks/p4b_options.md` & `benchmarks/p4b_table.csv`: Decision threshold sweep profiles and trade-off table.
- `benchmarks/data_fix_source_table.md` & `benchmarks/d6_class_audit.md`: Dataset source distribution, letterbox border trimming audit, and perceptual hash clustering.
- `benchmarks/permissions_check.txt`: Manifest permission verification confirming 0 network permissions.
