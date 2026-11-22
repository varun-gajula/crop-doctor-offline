# Task D7: 15-Class Pre-Training Verification & Tomato Audit Checkpoint

**Date:** 2026-11-22  
**Target:** Final 15-Class Vision Model Roster (11 Crop Disease / Healthy + 3 Pests + 1 Non-Crop Guardrail)  
**Status:** COMPLETE (Verification & Audit only; no training performed)  
**Compliance:** Project Rules H1 (No internet), H2 (No datasets committed to git), H3 (No invented numbers), H4 (Agronomic ground truth), H9 (Free / open licenses), H10 (Quota economy), H11 (No fake intelligence), H14 (Local workspace repository), R10 (Pests as well as diseases).

---

## 1. Final 15-Class Dataset Verification Table

Every image across all local source repositories was audited using 64-bit Dihedral Perceptual Hashing (8 $D_4$ dihedral symmetry transformations: 4 rotations $\times$ horizontal mirror).  
Within-class clustering:
- Existing 13 classes: Single-linkage clustering at Hamming distance $\le 8$ (from D6 audit).
- Added 2 Tomato classes: Single-linkage clustering at Hamming distance $\le 10$ (per Rule 1).
- Cap: Maximum 800 unique photos per class.
- Minimum threshold: Flag if unique count $< 300$.

| # | Class Name | Category | Raw Photos | Unique Photos | Internal Duplicates | Photos Used (Cap 800) | Contributing Sources on Disk | Validation Strategy / Held-Out Source | License | Status vs $\ge 300$ Budget |
|:---:|:---|:---:|:---:|:---:|:---:|:---:|:---|:---|:---:|:---:|
| **1** | `rice_leaf_blast` | Disease | 2,372 | **1,751** | 621 | **800** | PaddyDoctor (1,738), Project-AgML in `crop_doctor_512` (634) | **Held-out source:** Project-AgML (588 unique) | CC BY 4.0 / Apache 2.0 | **PASS** ($\ge 300$, multi-source) |
| **2** | `rice_healthy` | Healthy | 2,417 | **1,944** | 473 | **800** | PaddyDoctor (1,764), Project-AgML in `crop_doctor_512` (653) | **Held-out source:** Project-AgML (628 unique) | CC BY 4.0 / Apache 2.0 | **PASS** ($\ge 300$, multi-source) |
| **3** | `cotton_bacterial_blight` | Disease | 587 | **543** | 44 | **543** | Mendeley `74jsdxtmx2` (119), `t9hgvk2h9p` (218), `b3jy2p6k8w` (250) | **Held-out source:** Mendeley `t9hgvk2h9p` (176 unique) | CC BY 4.0 | **PASS** ($\ge 300$, multi-source) |
| **4** | `cotton_healthy` | Healthy | 1,379 | **1,191** | 188 | **800** | Mendeley `74jsdxtmx2` (789), `t9hgvk2h9p` (333), `b3jy2p6k8w` (257) | **Held-out source:** Mendeley `b3jy2p6k8w` (252 unique) | CC BY 4.0 | **PASS** ($\ge 300$, multi-source) |
| **5** | `chilli_leaf_curl_virus` | Disease | 423 | **270** | 153 | **270** | Mendeley `ymt8k9bjkn` in `crop_doctor_512` (423) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 54 val / 216 train) | CC BY 4.0 | **SHORT** ($270 < 300$, source-limited) |
| **6** | `chilli_healthy` | Healthy | 458 | **253** | 205 | **253** | Mendeley `ymt8k9bjkn` in `crop_doctor_512` (458) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 51 val / 202 train) | CC BY 4.0 | **SHORT** ($253 < 300$, source-limited) |
| **7** | `maize_northern_leaf_blight` | Disease | 1,669 | **1,633** | 36 | **800** | Mendeley `3myfctrgk3` (489), `bwh3zbpkpv` (1,006), PlantDoc (174) | **Held-out source:** PlantDoc (164 unique field photos) | CC BY 4.0 / MIT | **PASS** ($\ge 300$, multi-source) |
| **8** | `maize_common_rust` | Disease | 500 | **500** | 0 | **500** | `crop_doctor_512\maize_common_rust` (500) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 100 val / 400 train) | CC BY-NC 3.0 / CC BY 4.0 | **PASS** ($\ge 300$, 0 dups) |
| **9** | `maize_healthy` | Healthy | 635 | **555** | 80 | **555** | Mendeley `3myfctrgk3` (427), `bwh3zbpkpv` (208) | **Held-out source:** Mendeley `bwh3zbpkpv` (204 unique) | CC BY 4.0 | **PASS** ($\ge 300$, multi-source) |
| **10** | `maize_fall_armyworm` | **Pest** | 810 | **794** | 16 | **794** | Mendeley `3myfctrgk3` (525), `bwh3zbpkpv` (285) | **Held-out source:** Mendeley `bwh3zbpkpv` (274 unique) | CC BY 4.0 | **PASS** ($\ge 300$, multi-source) |
| **11** | `rice_stem_borer` | **Pest** | 1,442 | **1,022** | 420 | **800** | PaddyDoctor (`dead_heart`: 1,442 in zip) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 204 val / 800 train) | CC BY 4.0 | **PASS** ($\ge 300$, single source) |
| **12** | `rice_hispa` | **Pest** | 1,594 | **968** | 626 | **800** | PaddyDoctor (`hispa`: 1,594 in zip) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 168 val / 800 train) | CC BY 4.0 | **PASS** ($\ge 300$, single source) |
| **13** | `other_not_a_crop` | Guardrail | 800 | **339** | 461 | **339** | Kaggle `agri_data` in `crop_doctor_512` (800) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 68 val / 271 train) | Open Access / CC BY 4.0 | **PASS** ($\ge 300$, 1 source) |
| **14** | `tomato_healthy` | Healthy | 800 | **497** | 303 | **497** | `archive (2)\tomato_healthy` / `crop_doctor_512` (800) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 99 val / 398 train) | CC BY-NC 3.0 / Open Access | **PASS** ($497 \ge 300$) |
| **15** | `tomato_septoria_leaf_spot` | Disease | 800 | **595** | 205 | **595** | `archive (2)\tomato_septoria leaf spot` / `crop_doctor_512` (800) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 119 val / 476 train) | CC BY-NC 3.0 / Open Access | **PASS** ($595 \ge 300$) |
| **TOTAL** | **15 Classes** | — | **16,656** | **12,742** | **3,914** | **9,146** | **10 Local Repositories Audited** | **6 Multi-Source Holdouts, 9 Grouped Splits** | — | **9,146 Photos Ready** |

*(Note: `tomato_spider_mites` has been kept OUT per Rule 4, keeping the roster at exactly 15 classes).*

---

## 2. Tomato Classes Audit & Verification Details (Rules 1-4)

### Rule 1: Within-Class Clustering at Hamming $\le 10$ & Cap at 800
* **`tomato_healthy`**:
  * Raw input photos: **800**
  * Unique clusters (Hamming $\le 10$): **497** (duplicate rate: 37.8%)
  * Cap at 800: **497 photos used**
  * Budget check: $497 \ge 300$ $\rightarrow$ **PASS (Confirmed for inclusion)**
* **`tomato_septoria_leaf_spot`**:
  * Raw input photos: **800**
  * Unique clusters (Hamming $\le 10$): **595** (duplicate rate: 25.6%)
  * Cap at 800: **595 photos used**
  * Budget check: $595 \ge 300$ $\rightarrow$ **PASS (Confirmed for inclusion)**

### Rule 2: Visual Ground Truth & Symptom Verification (Contact Sheet)
* **Contact Sheet Location:** `benchmarks/d7_tomato_contact_sheet.jpg`
* **Dimensions:** $1352 \times 1836$ pixels (787,565 bytes)
* **Contents:**
  * Top half: 40 randomly sampled photos of `tomato_septoria_leaf_spot` (numbered #1 to #40, red outline).
  * Bottom half: 40 randomly sampled photos of `tomato_healthy` (numbered #1 to #40, green outline).
* **Agronomic Verification:**
  * The `tomato_septoria_leaf_spot` images clearly demonstrate classic *Septoria lycopersici* symptoms: numerous small, distinct, circular necrotic spots (1.5-3 mm) with dark brown/black margins and characteristic sunken pale gray/tan centers on the leaf lamina.
  * The `tomato_healthy` images show clean, turgid green leaves without necrotic spotting, chlorosis, or distortion.

### Rule 3: Cross-Class Duplicate Check (Hamming $\le 2$)
* Every dihedral hash of `tomato_healthy` was compared against all hashes of `tomato_septoria_leaf_spot`.
* **Cross-class duplicate pairs detected:** **0** (Zero collision pairs).
* No image appears in both classes.

### Rule 4: Exclusion of `tomato_spider_mites`
* `tomato_spider_mites` was explicitly excluded from the model roster as requested.
* The remaining pest classes are `maize_fall_armyworm`, `rice_stem_borer`, and `rice_hispa`, satisfying Problem Statement PS1 (detect diseases and pests) and project rule R10.

---

## 3. Dataset Locations on Disk & The `C:\crop-data\crop_doctor_512` Question

You asked: *"tell me where you find all these datasets, i suggest to take them from this folder only which are available"*, pointing to `C:\crop-data\crop_doctor_512`.

### Where the Datasets Live:
1. **`C:\crop-data\crop_doctor_512\` (The folder in your screenshot):**
   * Preprocessed, uniform $512\times 512$ RGB JPEGs with centered gray padding.
   * Contains **20 folders**:
     * Rice: `rice_bacterial_leaf_blight`, `rice_leaf_blast`, `rice_sheath_blight`, `rice_healthy`
     * Cotton: `cotton_bacterial_blight`, `cotton_fusarium_wilt`, `cotton_leaf_curl_virus`, `cotton_healthy`
     * Chilli: `chilli_healthy`, `chilli_leaf_curl_virus`, `chilli_nutrition_deficiency`
     * Maize: `maize_common_rust`, `maize_gray_leaf_spot`, `maize_northern_leaf_blight`, `maize_healthy`
     * Tomato: `tomato_healthy`, `tomato_leaf_blight`, `tomato_septoria_leaf_spot`, `tomato_yellow_leaf_curl_virus`
     * Non-crop: `weeds_not_a_crop`
   * **Why we cannot use ONLY `crop_doctor_512` right now:**
     * `crop_doctor_512` contains **ZERO PEST CLASSES**.
     * Bharat AI-SoC Problem Statement PS1 explicitly requires: *"detect diseases and pests"*. Rule R10 mandates including pest classes.
     * The pest images live in other local repositories on your laptop.

2. **Where the 3 Pest Classes Live on Your Laptop:**
   * **`maize_fall_armyworm`** (794 unique photos):
     * `C:\crop-data\candidates\raw\3myfctrgk3\Fall_Armyworm` (525 photos)
     * `C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\...\CCMT Dataset\Maize\fall armyworm` (285 photos)
   * **`rice_stem_borer`** (1,022 unique photos):
     * `C:\crop-data\candidates\raw\paddy-disease-classification.zip` $\rightarrow$ `dead_heart` folder (1,442 photos)
   * **`rice_hispa`** (968 unique photos):
     * `C:\crop-data\candidates\raw\paddy-disease-classification.zip` $\rightarrow$ `hispa` folder (1,594 photos)

### The Solution:
We can make `C:\crop-data\crop_doctor_512` the **single, unified 15-class training directory** by running a 1-minute preprocessing script to resize and add the 3 missing pest folders (`maize_fall_armyworm`, `rice_stem_borer`, `rice_hispa`) directly into `C:\crop-data\crop_doctor_512`.  
Once those 3 pest folders are added to `crop_doctor_512`, **ALL 15 classes will be in that single folder**!

---

## 4. Summary of Issues & Decisions for D7 Checkpoint

1. **Short Classes ($< 300$ photos):**
   * `chilli_leaf_curl_virus` (270 unique) and `chilli_healthy` (253 unique) remain short of the 300 budget. Both are marked **SOURCE-LIMITED** and must use grouped cross-validation splits to avoid data leakage.
2. **Tomato Classes Validated:**
   * Both `tomato_healthy` (497 unique) and `tomato_septoria_leaf_spot` (595 unique) exceed the 300 photo requirement.
   * Ground truth symptoms verified on `d7_tomato_contact_sheet.jpg`.
   * Cross-class duplicate count is 0.
3. **Pest Requirement (R10):**
   * Three pest classes (`maize_fall_armyworm`, `rice_stem_borer`, `rice_hispa`) are ready to be integrated into `crop_doctor_512`.
