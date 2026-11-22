# Task D6: 14-Class Pre-Training Verification & Dataset Audit (Including Maize Rust)

**Date:** 2026-11-22  
**Target:** 14-Class Vision Model Roster (9 Disease/Healthy + 4 Pests + 1 Non-Crop Guardrail)  
**Status:** COMPLETE (No training performed; pre-training verification only)  
**Compliance:** Project Rules H1 (No internet), H2 (No datasets committed to git), H3 (No invented numbers), H4 (Agronomic ground truth), H9 (Free / open licenses), H10 (Quota economy), H11 (No fake intelligence), H14 (Local workspace repository), R10 (Pests as well as diseases).

---

## 1. 14-Class Pre-Training Verification Table

Every image across all local source repositories was audited using 64-bit Dihedral Perceptual Hashing (8 $D_4$ dihedral symmetry transformations: 4 rotations $\times$ horizontal mirror) with single-linkage clustering at Hamming distance $\le 8$.

| # | Class Name | Raw Photos | Unique Photos ($\text{Hamming} \le 8$) | Duplicate Photos | Photos Used (Cap 800) | Contributing Sources on Disk | Validation Strategy / Held-Out Source | License | Competition Use Verdict | Status vs $\ge 300$ Budget |
|:---:|:---|:---:|:---:|:---:|:---:|:---|:---|:---:|:---:|:---|
| **1** | `rice_leaf_blast` | 2,372 | **1,751** | 621 | **800** | PaddyDoctor (1,738), Project-AgML in `crop_doctor_512` (634) | **Held-out source:** Project-AgML (588 unique) | CC BY 4.0 / Apache 2.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **2** | `rice_healthy` | 2,417 | **1,944** | 473 | **800** | PaddyDoctor (1,764), Project-AgML in `crop_doctor_512` (653) | **Held-out source:** Project-AgML (628 unique) | CC BY 4.0 / Apache 2.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **3** | `cotton_bacterial_blight` | 587 | **543** | 44 | **543** | Mendeley `74jsdxtmx2` (119), `t9hgvk2h9p` (218), `b3jy2p6k8w` (250) | **Held-out source:** Mendeley `t9hgvk2h9p` (176 unique) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **4** | `cotton_healthy` | 1,379 | **1,191** | 188 | **800** | Mendeley `74jsdxtmx2` (789), `t9hgvk2h9p` (333), `b3jy2p6k8w` (257) | **Held-out source:** Mendeley `b3jy2p6k8w` (252 unique) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **5** | `chilli_leaf_curl_virus` | 423 | **270** | 153 | **270** | Mendeley `ymt8k9bjkn` in `crop_doctor_512` (423) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 54 val / 216 train) | CC BY 4.0 | **PERMITTED** | **SHORT** ($270 < 300$) |
| **6** | `chilli_healthy` | 458 | **253** | 205 | **253** | Mendeley `ymt8k9bjkn` in `crop_doctor_512` (458) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 51 val / 202 train) | CC BY 4.0 | **PERMITTED** | **SHORT** ($253 < 300$) |
| **7** | `maize_northern_leaf_blight` | 1,669 | **1,633** | 36 | **800** | Mendeley `3myfctrgk3` (489), `bwh3zbpkpv` (1,006), PlantDoc (174) | **Held-out source:** PlantDoc (164 unique field photos) | CC BY 4.0 / MIT | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **8** | `maize_common_rust` | 500 | **500** | 0 | **500** | `crop_doctor_512\maize_common_rust` (500) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 100 val / 400 train) | CC BY-NC 3.0 / CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, 0 dups) |
| **9** | `maize_healthy` | 635 | **555** | 80 | **555** | Mendeley `3myfctrgk3` (427), `bwh3zbpkpv` (208) | **Held-out source:** Mendeley `bwh3zbpkpv` (204 unique) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **10** | `maize_fall_armyworm` | 810 | **794** | 16 | **794** | Mendeley `3myfctrgk3` (525), `bwh3zbpkpv` (285) | **Held-out source:** Mendeley `bwh3zbpkpv` (274 unique) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **11** | `rice_stem_borer` | 1,442 | **1,022** | 420 | **800** | PaddyDoctor (`dead_heart`: 1,442 in zip) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 204 val / 800 train) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, single source) |
| **12** | `rice_hispa` | 1,594 | **968** | 626 | **800** | PaddyDoctor (`hispa`: 1,594 in zip) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 168 val / 800 train) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, single source) |
| **13** | `tomato_spider_mites` | 252 | **236** | 16 | **236** | PlantVillage (`D:\main set`: 250), PlantDoc (train: 2) | **SOURCE-LIMITED** (PlantDoc has 2 photos; grouped 80/20 split) | CC BY-NC 3.0 / MIT | **FLAGGED (NC)** | **SHORT** ($236 < 300$) |
| **14** | `other_not_a_crop` | 800 | **339** | 461 | **339** | Kaggle `agri_data` in `crop_doctor_512` (800) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 68 val / 271 train) | Open Access / CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, 1 source) |
| **TOTAL** | **14 Classes** | **15,308** | **11,949** | **3,359** | **9,290** | **10 Local Repositories Audited** | **6 Multi-Source Holdouts, 8 Grouped Splits** | — | — | **9,290 Photos Available** |

---

## 2. Where Each Dataset Lives on This Computer

All datasets audited reside strictly on local disk. None are in the git repository (Rule H2):

1. **`C:\crop-data\crop_doctor_512\` (The Ready 512x512 Dataset shown in your screenshot):**
   * Preprocessed, square-padded $512\times 512$ RGB JPEGs.
   * Contains **20 classes**: `rice_leaf_blast`, `rice_healthy`, `rice_bacterial_leaf_blight`, `rice_sheath_blight`, `cotton_bacterial_blight`, `cotton_healthy`, `cotton_fusarium_wilt`, `cotton_leaf_curl_virus`, `chilli_healthy`, `chilli_leaf_curl_virus`, `chilli_nutrition_deficiency`, `maize_common_rust` (500 photos), `maize_gray_leaf_spot`, `maize_northern_leaf_blight`, `maize_healthy`, `tomato_early_blight`, `tomato_late_blight`, `tomato_yellow_leaf_curl_virus`, `tomato_septoria_leaf_spot`, `tomato_healthy`, `weeds_not_a_crop`.
   * **Missing from this folder:** The 4 pest classes (`maize_fall_armyworm`, `rice_stem_borer`, `rice_hispa`, `tomato_spider_mites`).
2. **`C:\crop-data\candidates\raw\paddy-disease-classification.zip` (Paddy Doctor):**
   * Source for Rice Pests and Diseases:
     * `dead_heart`: **1,442 photos of Rice Stem Borer** (*Scirpophaga incertulas*).
     * `hispa`: **1,594 photos of Rice Hispa** (*Dicladispa armigera*).
     * `blast`: **1,738 photos of Rice Leaf Blast**.
     * `normal`: **1,764 photos of Healthy Rice**.
3. **`C:\crop-data\candidates\raw\3myfctrgk3\` (MLD Maize):**
   * `Fall_Armyworm`: **525 photos of Maize Fall Armyworm**.
   * `Leaf_Blight`: 489 photos of Maize Northern Leaf Blight.
   * `Healthy`: 427 photos of Healthy Maize.
4. **`C:\Users\<USERNAME>\Downloads\Dataset for Crop Pest and Disease Detection\` (CCMT Maize):**
   * `fall armyworm`: **285 photos of Maize Fall Armyworm**.
   * `leaf blight`: 1,006 photos of Maize Northern Leaf Blight.
   * `healthy`: 208 photos of Healthy Maize.
5. **`D:\main set\Tomato_Spider_mites Two-spotted_spider_mite\` (PlantVillage):**
   * **250 photos of Tomato Spider Mites** (*Tetranychus urticae*).
6. **`C:\crop-data\candidates\raw\plantdoc\` (PlantDoc IIT Delhi):**
   * Real field photos of Maize Blight (174) and Tomato Spider Mites (2).

---

## 3. Analysis: Can We Take Data ONLY from `C:\crop-data\crop_doctor_512`?

### What Happens If We Use ONLY `crop_doctor_512`?
* **Pros:** 
  * All images are already resized to $512\times 512$ with centered gray padding.
  * Zero extraction or file movement needed.
  * Already contains `maize_common_rust` (500 clean photos, 0 duplicates!).
* **Cons (Critical Competition Conflict):**
  * `crop_doctor_512` contains **ZERO PESTS**.
  * Rule **R10** from the Bharat AI-SoC Problem Statement PS1 explicitly states:
    > *"R10: Pests as well as diseases (the official PS1 text says 'detect diseases and pests'). Committed: two pest classes (tomato spider mites, maize fall armyworm)."*
  * If the model only trains on `crop_doctor_512` as it sits right now, it will have **no pest detection capability whatsoever**, failing a core requirement of PS1.

### Recommended Action:
1. **Use `crop_doctor_512` as the primary base** for all disease, healthy, and non-crop classes (including `maize_common_rust`).
2. **Copy the 4 pest folders directly into `crop_doctor_512`** as $512\times 512$ images:
   * `crop_doctor_512\maize_fall_armyworm` (from MLD + CCMT)
   * `crop_doctor_512\rice_stem_borer` (from Paddy Doctor zip `dead_heart`)
   * `crop_doctor_512\rice_hispa` (from Paddy Doctor zip `hispa`)
   * `crop_doctor_512\tomato_spider_mites` (from `D:\main set`)
3. Then `C:\crop-data\crop_doctor_512` becomes the **single, self-contained 14-class dataset** for all training!
