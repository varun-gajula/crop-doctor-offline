# Dataset Audit & Attribution Specification - Crop Doctor

This document records the provenance, licensing, image counts, capture context, and class mappings for all training and evaluation datasets used in **Crop Doctor** for the 25 target classes across 5 crops.

---

## 1. Primary Datasets by Crop

### A. Rice (వరి)
* **Dataset Name:** Rice Leaf Disease Classification (Project-AgML) & Mendeley Rice Disease Datasets
* **Sources & Identifiers:**
  * Hugging Face: `Project-AgML/rice_leaf_disease_classification` (3,829 images)
  * Mendeley Data: `10.17632/fw8khx7p2d.1` (Rice Leaf Diseases Dataset) & `10.17632/bwh7zbpzk7.1`
* **License:** CC BY 4.0 (Mendeley) / Open Access Educational
* **Capture Type:** Mixed (Field and laboratory images)
* **Target Classes for Crop Doctor:**
  1. `rice_bacterial_leaf_blight` (*Xanthomonas oryzae pv. oryzae*)
  2. `rice_brown_spot` (*Bipolaris oryzae*)
  3. `rice_leaf_blast` (*Magnaporthe oryzae*)
  4. `rice_sheath_blight` (*Rhizoctonia solani*)
  5. `rice_healthy`
* **Excluded Classes:** `leaf_scald` (omitted to keep strictly 4 major problems + healthy per scope decision).

---

### B. Cotton (పత్తి)
* **Dataset Name:** Cotton Leaf Disease Dataset with Severity Levels & Cotton Leaf Image Dataset
* **Sources & Identifiers:**
  * Mendeley Data `10.17632/74jsdxtmx2.3` (Cotton Leaf Disease Dataset with Severity Levels)
  * Mendeley Data `10.17632/t9hgvk2h9p.1` (Cotton Leaf Image Dataset for Disease Classification)
  * Backup pest set: Mendeley `10.17632/b3jy2p6k8w.1` (SAR-CLD-2024: Cotton leaf hopper jassids)
* **License:** Creative Commons Attribution 4.0 International (CC BY 4.0)
* **Capture Type:** Field photos with severity grading (Mild, Moderate, Severe, Critical)
* **Target Classes for Crop Doctor:**
  1. `cotton_bacterial_blight` (*Xanthomonas citri pv. malvacearum*)
  2. `cotton_fusarium_wilt` (*Fusarium oxysporum f. sp. vasinfectum*)
  3. `cotton_leaf_curl_virus` (Cotton Leaf Curl Geminivirus - CLCuV, transmitted by whitefly)
  4. `cotton_alternaria_leaf_spot` (*Alternaria macrospora*)
  5. `cotton_healthy`
* **Key Calibration Value:** `74jsdxtmx2` contains explicit severity labels which we use to calibrate our transparent leaf area lesion segmentation formula.

---

### C. Chilli (మిరప)
* **Dataset Name:** Image Dataset on Chili Leaf Diseases in the Krishna River Basin of the Deccan Plateau, India
* **Source & Identifier:** Mendeley Data `10.17632/ymt8k9bjkn.3`
* **Contributors:** Rashmi Laxmikant Malghan, Lingaraj K., Karthik Rao M.C., Lalit Garg
* **License:** CC BY 4.0
* **Capture Type:** 1,856 authentic field photos collected across farmer holdings in Karnataka and Andhra Pradesh (Deccan Plateau / Krishna River Basin). Highly representative of South Indian agro-climatic conditions.
* **Target Classes for Crop Doctor:**
  1. `chilli_bacterial_spot` (*Xanthomonas campestris pv. vesicatoria*)
  2. `chilli_leaf_curl_virus` (Chilli leaf curl virus - ChiLCV, whitefly vector)
  3. `chilli_cercospora_leaf_spot` (*Cercospora capsici* - Frog-eye spot)
  4. `chilli_white_spot` (Ashy/white chlorotic lesion stage of Cercospora / Choanephora)
  5. `chilli_healthy`
* **Excluded Classes:** `nutrition_deficiency` (omitted from pathology scope).

---

### D. Tomato (టమాటా)
* **Dataset Name:** PlantVillage & PlantDoc
* **Sources & Identifiers:**
  * PlantVillage (Hughes et al., 2015; CC BY-NC 3.0 / open research)
  * PlantDoc (Singh et al., IIT Delhi, 2019; 2,598 real in-situ field photos; MIT License)
* **Capture Type:** Multi-source hybrid. PlantVillage provides canonical lab lesion geometry, while PlantDoc provides unconstrained in-field backgrounds, illumination changes, and occlusion.
* **Target Classes for Crop Doctor:**
  1. `tomato_early_blight` (*Alternaria solani*)
  2. `tomato_late_blight` (*Phytophthora infestans*)
  3. `tomato_yellow_leaf_curl_virus` (Tomato yellow leaf curl virus - TYLCV)
  4. `tomato_spider_mites` (*Tetranychus urticae* - Two-spotted spider mite; pest)
  5. `tomato_healthy`

---

### E. Maize (మొక్కజొన్న)
* **Dataset Name:** MLD: Multi-Class Maize Leaf Disease Image Dataset & PlantDoc Maize
* **Sources & Identifiers:**
  * Mendeley Data `10.17632/3myfctrgk3.1` (MLD from Bangladesh, CC BY 4.0; Blight, Fall Armyworm, Gray Spot, Healthy)
  * PlantVillage / PlantDoc (Common rust, Northern corn leaf blight)
* **License:** CC BY 4.0 / MIT
* **Capture Type:** Field photos including invasive pest damage
* **Target Classes for Crop Doctor:**
  1. `maize_gray_leaf_spot` (*Cercospora zeae-maydis*)
  2. `maize_common_rust` (*Puccinia sorghi*)
  3. `maize_northern_leaf_blight` (*Exserohilum turcicum*)
  4. `maize_fall_armyworm` (*Spodoptera frugiperda* - Leaf defoliation pest)
  5. `maize_healthy`

---

## 2. Mitigation of Shortcut Learning & Domain Shift

1. **Source Disentanglement:**
   * Lab-only datasets (like PlantVillage) risk teaching the neural network background shortcuts (e.g. gray laboratory paper or wooden table textures).
   * **Mitigation:** Every crop blends at least two distinct capture sources (e.g., PlantDoc field photos combined with PlantVillage; Krishna Basin field photos for Chilli).
2. **Data Augmentation:**
   * Random affine rotations ($-30^\circ$ to $+30^\circ$), ColorJitter (brightness, contrast, saturation variations representing variable field sunlight), RandomResizedCrop ($0.7$ to $1.0$), and slight Gaussian blur to simulate camera autofocus variations on mobile phones.
3. **Dedicated Real-Field Holdout Test Set:**
   * A held-out evaluation set of 20 real smartphone field photos per crop (~100 total) captured in local field conditions will be evaluated separately to report genuine field accuracy.

---

## 3. Storage & Free-Tier Google Drive Budget

* **Raw Total:** ~18,000 raw high-resolution images (~9.5 GB).
* **Drive Quota:** 15 GB shared with Gmail.
* **Downscaling Policy:** Every image is preprocessed using `scripts/resize_images.py`:
  * Max dimension (width or height): $\le 320\text{ px}$.
  * Encoding: JPEG at quality $85\%$.
  * Mean file size after resize: $\approx 25\text{ KB} - 35\text{ KB}$.
  * Total resized dataset footprint: **$\approx 650\text{ MB} - 950\text{ MB}$ (Well under the 3 GB ceiling)**.
