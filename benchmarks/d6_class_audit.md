# Task D6: 13-Class Pre-Training Verification & Dataset Audit

**Date:** 2026-11-22  
**Target:** 13-Class Vision Model Roster (8 Disease/Healthy + 4 Pests + 1 Non-Crop Guardrail)  
**Status:** COMPLETE (No training performed; pre-training verification only)  
**Compliance:** Project Rules H1 (No internet), H2 (No datasets committed to git), H3 (No invented numbers — all figures from `benchmarks/d6_audit_data.json` and `benchmarks/d6_hashes.json`), H4 (Agronomic ground truth), H9 (Free / open licenses), H11 (No fake intelligence), H14 (Local workspace repository).

---

## 1. 13-Class Pre-Training Verification Table

Every image across all local source repositories was audited using 64-bit Dihedral Perceptual Hashing (8 $D_4$ dihedral symmetry transformations: 4 rotations $\times$ horizontal mirror) with single-linkage clustering at Hamming distance $\le 8$.

| # | Class Name | Raw Photos | Unique Photos ($\text{Hamming} \le 8$) | Duplicate Photos | Photos Used (Cap 800) | Contributing Sources on Disk | Validation Strategy / Held-Out Source | License | Competition Use Verdict | Status / Issues |
|:---:|:---|:---:|:---:|:---:|:---:|:---|:---|:---:|:---:|:---|
| **1** | `rice_leaf_blast` | 2,372 | **1,751** | 621 | **800** | PaddyDoctor (1,738), Project-AgML (634) | **Held-Out Source:** Project-AgML (588 unique photos) | CC BY 4.0 / Apache 2.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **2** | `rice_healthy` | 2,417 | **1,944** | 473 | **800** | PaddyDoctor (1,764), Project-AgML (653) | **Held-Out Source:** Project-AgML (628 unique photos) | CC BY 4.0 / Apache 2.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **3** | `cotton_bacterial_blight` | 587 | **543** | 44 | **543** | Mendeley 74jsdxtmx2 (119), t9hgvk2h9p (218), b3jy2p6k8w (250) | **Held-Out Source:** Mendeley t9hgvk2h9p (176 unique photos) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **4** | `cotton_healthy` | 1,379 | **1,191** | 188 | **800** | Mendeley 74jsdxtmx2 (789), t9hgvk2h9p (333), b3jy2p6k8w (257) | **Held-Out Source:** Mendeley b3jy2p6k8w (252 unique photos) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **5** | `chilli_leaf_curl_virus` | 423 | **270** | 153 | **270** | Mendeley ymt8k9bjkn (Krishna River Basin: 423) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 54 val / 216 train) | CC BY 4.0 | **PERMITTED** | **SHORT** ($270 < 300$, 1 source, complex label) |
| **6** | `chilli_healthy` | 458 | **253** | 205 | **253** | Mendeley ymt8k9bjkn (Krishna River Basin: 458) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 51 val / 202 train) | CC BY 4.0 | **PERMITTED** | **SHORT** ($253 < 300$, 1 source) |
| **7** | `maize_northern_leaf_blight` | 1,669 | **1,633** | 36 | **800** | Mendeley 3myfctrgk3 (489), bwh3zbpkpv (1,006), PlantDoc (174) | **Held-Out Source:** PlantDoc (164 unique field photos) | CC BY 4.0 / MIT | **PERMITTED** | **PASS** ($\ge 300$, multi-source; 16 corrupt CCMT files) |
| **8** | `maize_healthy` | 635 | **555** | 80 | **555** | Mendeley 3myfctrgk3 (427), bwh3zbpkpv (208) | **Held-Out Source:** Mendeley bwh3zbpkpv (204 unique photos) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source; 4 corrupt CCMT files) |
| **9** | `maize_fall_armyworm` | 810 | **794** | 16 | **794** | Mendeley 3myfctrgk3 (525), bwh3zbpkpv (285) | **Held-Out Source:** Mendeley bwh3zbpkpv (274 unique photos) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, multi-source) |
| **10** | `rice_stem_borer` | 1,442 | **1,022** | 420 | **800** | PaddyDoctor (`dead_heart`: 1,442) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 204 val / 800 train) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, single source) |
| **11** | `rice_hispa` | 1,594 | **968** | 626 | **800** | PaddyDoctor (`hispa`: 1,594) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 168 val / 800 train) | CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, single source) |
| **12** | `tomato_spider_mites` | 252 | **236** | 16 | **236** | PlantVillage (`D:\main set`: 250), PlantDoc (train: 2) | **SOURCE-LIMITED** (PlantDoc has only 2 photos; grouped 80/20 split) | CC BY-NC 3.0 / MIT | **FLAGGED (NC)** | **SHORT** ($236 < 300$, CC BY-NC 3.0, lab photos) |
| **13** | `other_not_a_crop` | 800 | **339** | 461 | **339** | Kaggle agri_data (`weeds_not_a_crop`: 800) | **SOURCE-LIMITED** (Grouped 80/20 cluster split: 68 val / 271 train) | Open Access / CC BY 4.0 | **PERMITTED** | **PASS** ($\ge 300$, 1 source; full archive has 2,100) |
| **TOTAL** | **13 Classes** | **14,808** | **11,449** | **3,359** | **8,790** | **10 Distinct Repositories Audited** | **6 Multi-Source Holdouts, 7 Grouped Splits** | — | — | **8,790 Photos for Training/Validation** |

---

## 2. Multi-Source Breakdown & Held-Out Validation Analysis

Per prompt requirement 2, every pest class and every rice and chilli class was analyzed for source coverage and validation hold-out feasibility:

### A. Pest Classes (4 Classes)
1. **`maize_fall_armyworm` (*Spodoptera frugiperda*)**:
   - **Sources on Disk:** 
     - Mendeley MLD `3myfctrgk3.1` (Maize Leaf Disease, Kenya/India): 525 raw photos $\rightarrow$ 520 unique clusters.
     - Mendeley CCMT `bwh3zbpkpv.1` (Crop Pest and Disease Detection, Ghana/India): 285 raw photos $\rightarrow$ 274 unique clusters.
   - **Cross-Source Overlap:** 0 duplicate images across MLD and CCMT (Hamming $\le 2$).
   - **Validation Strategy:** **True Cross-Dataset Hold-Out.** Hold out CCMT (274 unique photos) strictly for validation; train on MLD (520 unique photos). Guarantees zero geographic and camera sensor leakage.
2. **`rice_stem_borer` (*Scirpophaga incertulas* - Dead Heart)**:
   - **Sources on Disk:** Paddy Doctor Kaggle/IEEE DataPort `10.21227/zsc8-d217` (`train_images/dead_heart`): 1,442 raw photos $\rightarrow$ 1,022 unique clusters.
   - **Status:** **SOURCE-LIMITED** (only one source dataset exists on local disk).
   - **Validation Strategy:** Grouped cluster-isolated split (whole dihedral pHash clusters assigned to train or val). 80% train (800 capped), 20% validation (204 unique photos).
3. **`rice_hispa` (*Dicladispa armigera*)**:
   - **Sources on Disk:** Paddy Doctor (`train_images/hispa`): 1,594 raw photos $\rightarrow$ 968 unique clusters.
   - **Status:** **SOURCE-LIMITED** (only one source dataset exists on local disk).
   - **Validation Strategy:** Grouped cluster-isolated split. 80% train (800 capped), 20% validation (168 unique photos).
4. **`tomato_spider_mites` (*Tetranychus urticae*)**:
   - **Sources on Disk:**
     - PlantVillage (`D:\main set\Tomato_Spider_mites Two-spotted_spider_mite`): 250 raw photos $\rightarrow$ 234 unique clusters.
     - PlantDoc (`C:\crop-data\candidates\raw\plantdoc\train\Tomato two spotted spider mites leaf`): 2 raw photos $\rightarrow$ 2 unique clusters.
   - **Status:** **SOURCE-LIMITED IN PRACTICE.** PlantDoc contributes only 2 field photos, which is insufficient for an independent validation holdout.
   - **Validation Strategy:** PlantDoc's 2 photos held out as an external field probe; PlantVillage split 80/20 by cluster (47 val / 187 train). Total unique photos (236) is below the 300 threshold.

### B. Rice Classes (4 Classes in 13-Class Roster)
1. **`rice_leaf_blast` (*Magnaporthe oryzae*)**:
   - **Sources on Disk:** Paddy Doctor (1,738 raw $\rightarrow$ 1,163 unique) and Project-AgML / RiceAUG (634 raw $\rightarrow$ 588 unique).
   - **Validation Strategy:** **True Cross-Dataset Hold-Out.** Hold out Project-AgML (588 unique photos) for validation; train on Paddy Doctor (800 capped).
2. **`rice_healthy`**:
   - **Sources on Disk:** Paddy Doctor (1,764 raw $\rightarrow$ 1,316 unique) and Project-AgML / RiceAUG (653 raw $\rightarrow$ 628 unique).
   - **Validation Strategy:** **True Cross-Dataset Hold-Out.** Hold out Project-AgML (628 unique photos) for validation; train on Paddy Doctor (800 capped).
3. **`rice_stem_borer`**: **SOURCE-LIMITED** (Paddy Doctor).
4. **`rice_hispa`**: **SOURCE-LIMITED** (Paddy Doctor).

### C. Chilli Classes (2 Classes in 13-Class Roster)
1. **`chilli_leaf_curl_virus`**:
   - **Sources on Disk:** Mendeley `ymt8k9bjkn.3` (Krishna River Basin, Deccan Plateau): 423 raw photos $\rightarrow$ 270 unique clusters.
   - **Status:** **SOURCE-LIMITED** on disk (candidate `tm3v4zmh7c.1` was not downloaded).
   - **Validation Strategy:** Grouped cluster-isolated 80/20 split (54 val / 216 train).
2. **`chilli_healthy`**:
   - **Sources on Disk:** Mendeley `ymt8k9bjkn.3` (Krishna River Basin): 458 raw photos $\rightarrow$ 253 unique clusters.
   - **Status:** **SOURCE-LIMITED** on disk.
   - **Validation Strategy:** Grouped cluster-isolated 80/20 split (51 val / 202 train).

---

## 3. Analysis of `chilli_leaf_curl_virus`: Thrips vs Virus Presentation

Per prompt requirement 3:
1. **Visual & Statistical Foliar Symptom Distribution:**
   - In Mendeley dataset `ymt8k9bjkn.3` ("Image Dataset on Chili Leaf Diseases in the Krishna River Basin of the Deccan Plateau, India"), all 423 images are stored under a single directory: `Curl Virus`.
   - **Color & Chlorosis Analysis:**
     - High-chlorosis / severe vein-clearing images (Yellow Index $> 5.0$, e.g., `Curl Virus00341.JPG`, `Curl Virus00046.JPG`): Leaves exhibit distinct chlorotic mottling, intense vein clearing, and thickened main veins characteristic of **Chilli Leaf Curl Begomovirus (ChiLCV)** transmitted by whitefly (*Bemisia tabaci*).
     - Low-chlorosis / dark-green upward-cupping images (Yellow Index $< 1.3$, e.g., `Curl Virus00238.JPG`, `Curl Virus00198.JPG`): Leaves exhibit deep-green upward boat-shaped margin curling with longitudinal wrinkling and petiole elongation, characteristic of **Chilli Thrips (*Scirtothrips dorsalis*)** feeding rasp injury.
2. **Can Thrips and Virus Photos Be Listed / Separated?**
   - Heuristically, foliar symptom clustering separates the 423 photos into ~230 thrips-dominant upward-cupping photos and ~193 virus-dominant chlorotic/vein-clearing photos.
   - However, **the source dataset provides no laboratory molecular diagnosis (PCR / ELISA)** or microscopic vector counts.
3. **Should the Label Be Split or Renamed?**
   - **Verdict on SPLIT:** **DO NOT SPLIT.**
     - *Agronomic Reason:* In South Indian field conditions (Andhra Pradesh Guntur/Prakasam/Kurnool and Telangana Khammam/Warangal), thrips feeding damage and ChiLCV infection frequently occur simultaneously on the same plant ("Chilli Murda Complex").
     - *Machine Learning / Rule H11 Reason:* Artificially bifurcating 423 photos without biological ground truth violates **Rule H11 (No Fake Intelligence)**. Furthermore, splitting 270 unique clusters would produce two sub-classes of only ~135 photos each, far below the statistical viability threshold ($\ge 300$).
   - **Verdict on RENAME:** **RENAME.**
     - In alignment with ICAR-IIHR and ANGRAU pest management guides, rename the class description to:
       **"Chilli Leaf Curl Complex (Thrips & Virus)"** (`chilli_leaf_curl_complex`).
     - Display name in Telugu: **మిరప బొబ్బర / ఆకుముడుత (తామర పురుగులు & వైరస్)**.
     - Display name in Hindi: **मिर्च का लीफ कर्ल कॉम्प्लेक्स (थ्रिप्स व वायरस)**.
     - This honestly captures both the insect vector/injury (*Scirtothrips dorsalis*) and the viral pathogen (*Begomovirus*), perfectly satisfying Problem Statement PS1.

---

## 4. Source Dataset Licensing & Competition Legality Audit

Per prompt requirement 4, all 10 source repositories were audited for license permissions and competition compliance:

| Repository / Source | License | Academic / Competition Allowed? | Commercial Use Allowed? | Can Images Go to Git Repo? | Rule H9 / H2 Compliance Verdict |
|:---|:---|:---:|:---:|:---:|:---|
| **Paddy Doctor** (`paddy-disease-classification.zip`) | CC BY 4.0 | **YES** | **YES** | **NO (Rule H2)** | Fully compliant; competition use explicitly permitted. |
| **Project-AgML Rice** (`Project-AgML/rice`) | Apache 2.0 / Open Access | **YES** | **YES** | **NO (Rule H2)** | Fully compliant. |
| **Mendeley MLD** (`3myfctrgk3.1` - Maize) | CC BY 4.0 | **YES** | **YES** | **NO (Rule H2)** | Fully compliant. |
| **Mendeley CCMT** (`bwh3zbpkpv.1` - Maize) | CC BY 4.0 | **YES** | **YES** | **NO (Rule H2)** | Fully compliant. |
| **Mendeley Krishna Basin** (`ymt8k9bjkn.3` - Chilli) | CC BY 4.0 | **YES** | **YES** | **NO (Rule H2)** | Fully compliant. |
| **Mendeley Cotton Repositories** (`74jsdxtmx2`, `t9hgvk2h9p`, `b3jy2p6k8w`) | CC BY 4.0 | **YES** | **YES** | **NO (Rule H2)** | Fully compliant. |
| **PlantDoc** (IIT Delhi, ACM CoDS-COMAD 2020) | MIT License | **YES** | **YES** | **NO (Rule H2)** | Fully compliant; open MIT license permits all competition uses. |
| **PlantVillage** (Hughes & Salathé, 2015) | CC BY-NC 3.0 | **YES (Academic)** | **NO (Non-Commercial)** | **NO (Rule H2)** | **FLAGGED FOR COMMERCIAL USE.** Permitted for academic student competition (no product sales), but commercial redistribution is restricted. |
| **Kaggle `agri_data`** (Agricultural Weeds / Non-crop) | CC BY 4.0 / Open Access | **YES** | **YES** | **NO (Rule H2)** | Fully compliant. |

**Summary:** **Zero sources forbid competition use.** All sources permit evaluation and modeling in the Arm Bharat AI-SoC Challenge. PlantVillage carries a Non-Commercial (NC) restriction, which is safe for this non-profit student submission (Rule H9) but noted for commercial transparency.

---

## 5. Cross-Class and Cross-Source Duplicate Audit Findings

Per prompt requirement 5, an all-pairs collision matrix was evaluated across all 14,808 photos:
1. **Cross-Class Duplicate Conflicts:**
   - **Raw Hash Matches ($\text{Hamming} \le 2$):** 64 collision pairs initially flagged.
   - **Root-Cause Analysis:** All 64 collision pairs were generated by **16 corrupted/unreadable JPEG files** in `CCMT Dataset\Maize\leaf blight` and **4 corrupted files** in `CCMT Dataset\Maize\healthy` that failed image decoding and returned hash `0` ($0 \oplus 0 = 0$).
   - **Valid Image Cross-Class Duplicates:** **EXACTLY 0.** Zero valid images in the dataset appear under two different class names.
2. **Cross-Source Duplicates (Within Same Class):**
   - Evaluated across all multi-source classes (`rice_leaf_blast`, `rice_healthy`, `cotton_bacterial_blight`, `cotton_healthy`, `maize_northern_leaf_blight`, `maize_healthy`, `maize_fall_armyworm`, `tomato_spider_mites`).
   - Matches at $\text{Hamming} \le 2$: **EXACTLY 0.** No source contains re-encoded or copied photos from another source.

---

## 6. List of Issues to Fix Before Training

The following concrete issues were identified during this audit and must be addressed prior to model training:

1. **Scrub 20 Corrupted JPEG Files in CCMT Maize Dataset:**
   - The following 16 files in `CCMT Dataset\Maize\leaf blight` fail PIL decoding:
     `leaf blight37_.jpg`, `leaf blight58_.jpg`, `leaf blight82_.jpg`, `leaf blight98_.jpg`, `leaf blight379_.jpg`, `leaf blight452_.jpg`, `leaf blight453_.jpg`, `leaf blight522_.jpg`, `leaf blight590_.jpg`, `leaf blight666_.jpg`, `leaf blight667_.jpg`, `leaf blight745_.jpg`, `leaf blight746_.jpg`, `leaf blight830_.jpg`, `leaf blight912_.jpg`, `leaf blight913_.jpg`.
   - The following 4 files in `CCMT Dataset\Maize\healthy` fail PIL decoding:
     `healthy18_.jpg`, `healthy87_.jpg`, `healthy88_.jpg`, `healthy189_.jpg`.
   - *Fix:* Ensure the data loader explicitly verifies images with `PIL.Image.verify()` and ignores corrupted streams during manifest generation.
2. **Address 3 Under-Sampled Classes ($< 300$ Unique Photos):**
   - `tomato_spider_mites`: 236 unique photos (needs 64 more photos to meet the 300 threshold).
   - `chilli_healthy`: 253 unique photos (needs 47 more photos).
   - `chilli_leaf_curl_virus`: 270 unique photos (needs 30 more photos).
   - *Fix Options:*
     - (a) Accept these 3 classes with weighted loss / focal loss given their critical agronomic value in South India.
     - (b) For chilli, download candidate `tm3v4zmh7c.1` (adds 1,150 raw curl and 1,650 raw healthy photos) to easily exceed 1,000 photos per class.
3. **Formalize Grouped Cluster Splits for 5 "Source-Limited" Classes:**
   - `rice_stem_borer`, `rice_hispa`, `chilli_leaf_curl_virus`, `chilli_healthy`, and `other_not_a_crop` have only one local source repository.
   - *Fix:* Cross-dataset validation is impossible for these 5 classes. A strict cluster-isolated grouped split ($\text{Hamming} \le 8$) must be enforced so that no near-duplicate image from the same cluster leaks from training into validation.
4. **Rename `chilli_leaf_curl_virus` to `chilli_leaf_curl_complex`:**
   - Update `class_map.yaml`, `supported_classes.json`, and UI string resources so that the advisory explicitly informs farmers that curling involves both thrips (*Scirtothrips dorsalis*) and Begomovirus.
5. **Expand `other_not_a_crop` from 800-Photo Sample to Full Archive:**
   - The 800-photo sample from `agri_data` yields 339 unique clusters (461 internal duplicates due to sequential video frames).
   - *Fix:* Ingest the full 2,100 photos in `C:\Users\<USERNAME>\Downloads\archive\agri_data\data` to expand unique background clusters from 339 to $>800$, providing a more diverse non-crop rejection head.
