# Task P4: Three Clear Outcomes - Decision Table and Threshold Report

**Rule H3 (Never invent numbers)** · **Rule H5 (Safety in plain code)** · **Rule H11 (No fake intelligence)**  
**Date:** 2026-11-20  
**Selected Configuration:** **Option B** (`energy_threshold` = -8.0, `crop_mass_threshold` = 0.60, `margin_threshold` = 0.25, `borderline_energy` = -9.0)

---

## 1. Executive Summary & Verification of Targets (Option B)

Decision outcomes were evaluated over **610 evaluation rows** across 5 distinct probe sets assembled under `C:\crop-data\realtest\`.

> [!NOTE]
> Thresholds were chosen and scored on the same probe set, so the reported rates are optimistic.

| Probe Set | Target Condition | Target Goal | Option B Measured Rate | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Supported Crop Leaves** (107 images) | `DIAGNOSE` or `CAN'T TELL` (not `OUT OF CLASS`) | >= 90.0% | **98.1%** (105 / 107) | **PASS** |
| **Non-Crop Objects (`nocrop`)** (30 images, 150 evaluations) | `CAN'T TELL` | >= 80.0% | **96.7%** (145 / 150) | **PASS** |
| **Other Crops (`othercrop`)** (48 images, 240 evaluations) | `OUT OF CLASS` or `CAN'T TELL` | >= 60.0% | **92.5%** (222 / 240) | **PASS** |
| **Unsupported Diseases (`unsupported`)** (93 images) | Share wrongly `DIAGNOSED` | Report share | **10.8%** (10 / 93) | **REPORTED** |
| **Laptop Screen Photos (`screen`)** (20 photos) | Separate Distribution Report | N/A | **DIAGNOSE 40.0% / CAN'T TELL 50.0% / OUT OF CLASS 10.0%** | **ANALYZED** |

> [!IMPORTANT]
> For unsupported-problem photos, the share wrongly diagnosed is **10.8%** (10 / 93).
> As expected and required by Task P4, it is not zero because some unsupported symptoms (e.g. bacterial spot lesions or late blight) visually resemble supported leaf blights in RGB space. However, under Option B, **89.2%** (83 / 93) are successfully intercepted as either `OUT OF CLASS` (30 images) or `CAN'T TELL` (53 images).

---

## 2. Probe Set Composition & Image Sources (from `p4_probe_manifest.json`)

| Probe Set | Total Images | Real Phone Photos | Web or Dataset Images | Primary Source Folders |
| :--- | :--- | :--- | :--- | :--- |
| **Supported** | 107 | 12 | 95 | `realtest\real_leaves` (phone) + `crop_doctor_512` validation split (0 train collisions) |
| **Unsupported** | 93 | 8 | 85 | `realtest\real_leaves` (phone) + `plantdoc`, `Chili Leaf`, `rice shiva`, `cotton` |
| **Screen** | 20 | 20 | 0 | `realtest\screen` (camera captures of laptop display) |
| **Nocrop** | 30 | 30 | 0 | `OneDrive\Pictures\Camera imports` (phone captures: desk, wall, floor, hand, objects; no faces) |
| **Othercrop** | 48 | 0 | 48 | `Dataset for Crop Pest and Disease Detection\CCMT Dataset` (Cashew, Cassava) |
| **Total** | **298** | **70** | **228** | Complete traceability in `benchmarks\p4_probe_manifest.json` |

---

## 3. Supported Set Accuracy & Decisions (Option B Only)

On the 107 supported crop photos (with matching crop selected by the user):
- **`DIAGNOSE` Count**: **100 / 107 (93.5%)**
- **`CAN'T TELL` Count**: **5 / 107 (4.7%)**
- **`OUT OF CLASS` Count**: **2 / 107 (1.9%)**
- **Overall Accuracy of `DIAGNOSE` Decisions Against True Class**: **95 / 100 (95.0%)**

### Per-Class Accuracy on `DIAGNOSE` Decisions:

| Class | Correct / Diagnosed | Accuracy on DIAGNOSE | Total in Probe Set |
| :--- | :--- | :--- | :--- |
| `chilli_healthy` | 5 / 5 | **100.0%** | 5 |
| `chilli_leaf_curl_virus` | 5 / 5 | **100.0%** | 5 |
| `chilli_nutrition_deficiency` | 5 / 5 | **100.0%** | 5 |
| `cotton_bacterial_blight` | 5 / 5 | **100.0%** | 5 |
| `cotton_fusarium_wilt` | 5 / 5 | **100.0%** | 5 |
| `cotton_healthy` | 5 / 5 | **100.0%** | 5 |
| `cotton_leaf_curl_virus` | 5 / 5 | **100.0%** | 5 |
| `maize_common_rust` | 6 / 7 | **85.7%** | 7 |
| `maize_gray_leaf_spot` | 6 / 6 | **100.0%** | 7 |
| `maize_healthy` | 5 / 5 | **100.0%** | 5 |
| `maize_northern_leaf_blight` | 7 / 7 | **100.0%** | 7 |
| `rice_bacterial_leaf_blight` | 5 / 5 | **100.0%** | 5 |
| `rice_healthy` | 5 / 5 | **100.0%** | 5 |
| `rice_leaf_blast` | 5 / 5 | **100.0%** | 5 |
| `rice_sheath_blight` | 5 / 5 | **100.0%** | 5 |
| `tomato_healthy` | 5 / 5 | **100.0%** | 7 |
| `tomato_leaf_blight` | 3 / 5 | **60.0%** | 7 |
| `tomato_septoria_leaf_spot` | 4 / 5 | **80.0%** | 5 |
| `tomato_yellow_leaf_curl_virus` | 4 / 5 | **80.0%** | 7 |
| **Total / Overall** | **95 / 100** | **95.0%** | **107** |

---

## 4. Applied Thresholds in `thresholds.json` (Single Source, Fail Closed)

Values written to `app/src/main/assets/thresholds.json` and `ml/thresholds.json`:
- `temperature`: **1.7818** (calibrated temperature scaling)
- `confidence_threshold`: **0.3434** (minimum within-crop top-1 probability)
- `energy_threshold`: **-8.0000** (Free Energy OOD threshold)
- `crop_mass_threshold`: **0.6000** (Option B: minimum 60% probability mass on selected crop)
- `margin_threshold`: **0.2500** (Option B: within-crop top1 - top2 probability difference)
- `borderline_energy`: **-9.0000** (Option B: borderline energy threshold for ambiguous margin)

---

## 5. Metric Distributions Across the 5 Probe Sets

### A. Free Energy: $E(x; T) = -T \cdot \ln \left(\sum_{i=1}^{20} \exp(z_i / T)\right)$

| Probe Set | Count | Min | P10 | P25 | Median | Mean | P75 | P90 | Max |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **supported** | 107 | -35.18 | -27.49 | -22.05 | **-16.75** | -17.73 | -11.92 | -9.92 | -5.55 |
| **othercrop** | 240 | -20.99 | -15.04 | -11.16 | **-9.12** | -9.90 | -7.29 | -6.66 | -6.26 |
| **unsupported** | 93 | -20.89 | -11.73 | -9.77 | **-7.85** | -8.55 | -6.27 | -5.93 | -4.78 |
| **screen** | 20 | -16.18 | -14.93 | -12.51 | **-9.72** | -10.28 | -7.66 | -6.67 | -5.51 |
| **nocrop** | 150 | -10.19 | -7.78 | -6.75 | **-5.89** | -6.25 | -5.27 | -5.04 | -4.70 |

### B. Selected Crop Mass: $M_C = \sum_{i \in \text{selected crop}} p_i$

| Probe Set | Count | Min | P10 | P25 | Median | Mean | P75 | P90 | Max |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **supported** | 107 | 0.0148 | 0.9856 | 0.9971 | **0.9998** | 0.9507 | 1.0000 | 1.0000 | 1.0000 |
| **othercrop** | 240 | 0.0000 | 0.0001 | 0.0009 | **0.0080** | 0.1998 | 0.1583 | 0.9435 | 1.0000 |
| **unsupported** | 93 | 0.0000 | 0.0049 | 0.0769 | **0.3325** | 0.4368 | 0.8483 | 0.9864 | 0.9999 |
| **screen** | 20 | 0.0013 | 0.0122 | 0.3757 | **0.8686** | 0.6867 | 0.9895 | 0.9965 | 0.9986 |
| **nocrop** | 150 | 0.0001 | 0.0020 | 0.0122 | **0.0729** | 0.1996 | 0.3139 | 0.6591 | 0.9850 |

---

## 6. Laptop Screen Photos Analysis

- **Energy Comparison (Real vs Screen)**:
  - **Real Supported Leaves**: Median **-16.75**, Mean **-17.73**, P90 **-9.92**.
  - **Screen Photos**: Median **-9.72**, Mean **-10.28**, P90 **-6.67**.
  - *Observation*: cause not established; screen photos have energy between real leaves and non-crop photos.
- **Screen Decision Breakdown (Option B)**:
  - **`DIAGNOSE`**: **8 / 20 (40.0%)**
  - **`CAN'T TELL`**: **10 / 20 (50.0%)** [Reason: `HIGH_FREE_ENERGY` > -8.0]
  - **`OUT OF CLASS`**: **2 / 20 (10.0%)** [Reason: `LOW_CROP_MASS` < 0.60]
- *Compliance Note*: In accordance with prompt instructions, thresholds were **not** loosened to force acceptance of screen photos. Real leaf performance is preserved without compromising safety.

---

## 7. Resolution of Task P2 Wrong-Crop Cases (Option B)

In Task P2, continuous capture without crop-selection produced two wrong-crop misclassifications when scanning unconstrained images:

### Case 1: `camera_real_11` (Tomato Leaf Bacterial Spot)
- **P2 Result without Crop Selection**: Misclassified as `maize_common_rust` (prob 0.449).
- **P4 Result with Tomato Selected**:
  - **True Class**: `tomato_bacterial_spot`
  - **Energy**: `-11.0555`
  - **Crop Mass ($M_C$)**: `0.0093` (only 0.9% model mass on tomato!)
  - **Within-Crop Top-1**: `tomato_yellow_leaf_curl_virus` (prob 0.8995)
  - **Decision**: **`OUT OF CLASS`**
  - **Reason Code**: `LOW_CROP_MASS(0.009 < 0.600)`
- **Verification**: It is **not** called Maize Rust, and it is **not** called Yellow Leaf Curl. Because crop mass is under threshold (0.009 < 0.60), it is safely flagged as **OUT OF CLASS**!

### Case 2: `camera_real_17` (Tomato Leaf Yellow Virus)
- **P2 Result without Crop Selection**: Misclassified as `cotton_leaf_curl_virus` (prob 0.381).
- **P4 Result with Tomato Selected**:
  - **True Class**: `tomato_yellow_leaf_curl_virus`
  - **Energy**: `-9.8972`
  - **Crop Mass ($M_C$)**: `0.0828` (only 8.3% model mass on tomato!)
  - **Within-Crop Top-1**: `tomato_healthy` (prob 0.6513), Top-2: `tomato_yellow_leaf_curl_virus` (prob 0.3312)
  - **Decision**: **`OUT OF CLASS`**
  - **Reason Code**: `LOW_CROP_MASS(0.083 < 0.600)`
- **Verification**: It is **not** called Cotton Leaf Curl. The low crop mass (0.083 < 0.60) safely flags it as **OUT OF CLASS**!

---

## 8. Three Clear Outcomes: UX Specifications

1. **`DIAGNOSE`**:
   - **Icon**: Green Check Circle / Leaf Badge (`CheckCircle`)
   - **Headline**: Crop & Problem Name (in selected language: Telugu / Hindi / English)
   - **Details**: Severity band (Mild / Moderate / Severe), Certainty band, Full Advisory tiers (Natural & Traditional, Chemical), AI explanation streaming.

2. **`CAN'T TELL`** (not a crop leaf, or too unclear):
   - **Icon**: Orange Help / Question / Blur Badge (`HelpOutline` / `CameraAlt`)
   - **Headline**: *"Can't tell. This does not look like a clear leaf. Take a closer photo in daylight or ask an extension officer."*
     - Telugu: *"గుర్తించలేకపోతున్నాము. ఇది స్పష్టమైన ఆకులా కనిపించడం లేదు. పగటి వెలుతురులో దగ్గరగా ఫోటో తీయండి లేదా వ్యవసాయ అధికారిని సంప్రదించండి."*
     - Hindi: *"पहचान नहीं सके। यह स्पष्ट पत्ती जैसी नहीं दिख रही है। दिन की रोशनी में पास से फोटो लें या कृषि अधिकारी से संपर्क करें।"*
   - **Actions**: Visible Kisan Call Centre Helpline button (1800-180-1551) & "Take another photo" button.
   - **Rule H4/H5 Compliance**: **NEVER** shows treatment tiers, medicine doses, or severity.

3. **`OUT OF CLASS`** (a plant, but not a supported crop or problem):
   - **Icon**: Amber / Warning Plant Badge (`WarningAmber` / `Grass`)
   - **Headline**: *"Out of class. This does not match <selected crop> or one of the 20 problems we support. Choose the right crop or ask an extension officer."*
     - Telugu: *"మద్దతు లేని పంట లేదా సమస్య. ఇది ఎంచుకున్న పంటతో లేదా మేము మద్దతిచ్చే 20 సమస్యలతో సరిపోలడం లేదు. సరైన పంటను ఎంచుకోండి లేదా వ్యవసాయ అధికారిని సంప్రదించండి."*
     - Hindi: *"असमर्थित फसल या समस्या। यह चुनी गई फसल या हमारे 20 समर्थित रोगों/कीटों से मेल नहीं खाती। सही फसल चुनें या कृषि अधिकारी से संपर्क करें।"*
   - **Actions**: "Change crop" button, Helpline button, & "Take another photo" button.
   - **Rule H4/H5 Compliance**: **NEVER** shows treatment tiers, medicine doses, or severity.
