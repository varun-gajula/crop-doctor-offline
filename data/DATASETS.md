# Dataset Audit, Provenance & Licensing - Crop Doctor

This specification documents the provenance, licensing, source URLs, access dates, class breakdown, and raw image counts for all datasets used in **Crop Doctor** (Task D1).

All datasets comply strictly with Project Rule **H9 (Free only / Open licenses)** and **H2 (No datasets committed to git repo)**. No scraping of web pages or search engine results was performed.

---

## 1. Verified Sources Overview

| Source Identifier | Crop(s) | Dataset Name | License | Source URL / DOI | Access Date | Raw Archive File | Status |
|---|---|---|---|---|---|---|---|
| `ymt8k9bjkn` | Chilli | Image Dataset on Chili Leaf Diseases in the Krishna River Basin | CC BY 4.0 | https://doi.org/10.17632/ymt8k9bjkn.3 | 2026-11-13 | `Chili Leaf Disease Original Dataset.zip` (501.47 MB) | Downloaded & Extracted |
| `74jsdxtmx2` | Cotton | Cotton Leaf Disease Dataset with Severity Levels | CC BY 4.0 | https://doi.org/10.17632/74jsdxtmx2.1 | 2026-11-13 | `Cotton-Original.zip` (106.50 MB) | Downloaded & Extracted |
| `t9hgvk2h9p` | Cotton | Cotton Leaf Image Dataset for Disease Classification | CC BY 4.0 | https://doi.org/10.17632/t9hgvk2h9p.1 | 2026-11-13 | `Cotton_Original_Dataset.zip` (58.09 MB) | Downloaded & Extracted |
| `3myfctrgk3` | Maize | MLD: Multi-Class Maize Leaf Disease Image Dataset | CC BY 4.0 | https://doi.org/10.17632/3myfctrgk3.1 | 2026-11-13 | `Maize dataset.zip` (26.07 MB) | Downloaded & Extracted |
| `Project-AgML/rice` | Rice | Rice Leaf Disease Classification | Apache 2.0 / Open Access | https://huggingface.co/datasets/Project-AgML/rice_leaf_disease_classification | 2026-11-13 | `rice/train-0000[0-2]-of-00003.parquet` (1,018.14 MB) | Downloaded (Parquet format) |
| `PlantVillage` | Tomato, Maize | PlantVillage Disease Classification Dataset | CC BY-NC 3.0 / Open Access | Hughes & Salathé (2015); https://github.com/spMohanty/PlantVillage-Dataset | 2026-11-13 | `plantvillage.zip` (2,087.96 MB) | Downloaded & Extracted |
| `PlantDoc` | Tomato, Maize | PlantDoc: A Dataset for Visual Plant Disease Detection | MIT License | https://github.com/pratikkayal/PlantDoc-Dataset | Proposed (Awaiting Approval) | None (Awaiting user approval before clone) | Pending Approval |

---

## 2. Detailed Dataset Breakdown

### A. Rice (వరి)
* **Primary Source:** Hugging Face `Project-AgML/rice_leaf_disease_classification`
* **License:** Apache 2.0 (AgML framework) / Open Access Educational
* **URL:** `https://huggingface.co/datasets/Project-AgML/rice_leaf_disease_classification`
* **Raw Image Counts:**
  * Total images in dataset: 3,829 across 6 classes
  * Class 0 - `Bacterial_Leaf_Blight`: 636 total (89 original field, 547 augmented) -> Maps to **Index 00**
  * Class 1 - `Brown_Spot`: 646 total (150 original field, 496 augmented) -> Maps to **Index 01**
  * Class 2 - `Healthy_Rice_Leaf`: 653 total (187 original field, 466 augmented) -> Maps to **Index 04**
  * Class 3 - `Leaf_Blast`: 634 total (108 original field, 526 augmented) -> Maps to **Index 02**
  * Class 5 - `Sheath_Blight`: 632 total (288 original field, 344 augmented) -> Maps to **Index 03**
  * *Unmapped / Excluded:* Class 4 - `Leaf_Scald`: 628 images (Not in Crop Doctor's 25 classes; excluded)
* **Audit Note on Rule H11 / Synthetic Data:** The AgML dataset authors expanded ~100-300 field images per class with rotational/spatial augmentations. All duplicates/near-duplicates within each class are filtered during ingest.

---

### B. Cotton (పత్తి)
* **Sources:**
  1. Mendeley Data `10.17632/74jsdxtmx2.1` ("Cotton Leaf Disease Dataset with Severity Levels", CC BY 4.0)
  2. Mendeley Data `10.17632/t9hgvk2h9p.1` ("Cotton Leaf Image Dataset for Disease Classification", CC BY 4.0)
* **Raw Image Counts:**
  * **Index 05 (Cotton Bacterial Blight):**
    * `74jsdxtmx2/bacterial blight`: 119
    * `t9hgvk2h9p/Bacterial Blight`: 218
    * *Total:* 337 (SHORT)
  * **Index 06 (Cotton Fusarium Wilt):**
    * `74jsdxtmx2/fussarium wilt`: 47
    * `t9hgvk2h9p/Fusarium Wilt`: 337
    * *Total:* 384 (SHORT)
  * **Index 07 (Cotton Leaf Curl Virus):**
    * `74jsdxtmx2/curl virus`: 25
    * *Total:* 25 (BLOCKED - critically short)
  * **Index 08 (Cotton Alternaria Leaf Spot):**
    * `t9hgvk2h9p/Alternaria Leaf Spot`: 173
    * *Total:* 173 (BLOCKED)
  * **Index 09 (Cotton Healthy Cotton):**
    * `74jsdxtmx2/healthy`: 789
    * `t9hgvk2h9p/Healthy Leaf`: 333
    * *Total:* 1,122 (OK)
  * *Unmapped / Excluded:*
    * `t9hgvk2h9p/Verticillium Wilt`: 312 images (Excluded; Verticillium wilt is distinct from Fusarium wilt and not in the 25 target classes).

---

### C. Chilli (మిరప)
* **Primary Source:** Mendeley Data `10.17632/ymt8k9bjkn.3` ("Image Dataset on Chili Leaf Diseases in the Krishna River Basin of the Deccan Plateau, India")
* **License:** CC BY 4.0
* **URL:** `https://data.mendeley.com/datasets/ymt8k9bjkn/3`
* **Raw Image Counts:**
  * **Index 10 (Chilli Bacterial Spot):** 156 images (BLOCKED)
  * **Index 11 (Chilli Leaf Curl):** 423 images (SHORT)
  * **Index 12 (Chilli Cercospora Leaf Spot):** 180 images (BLOCKED)
  * **Index 13 (Chilli White Spot):** 195 images (**UNVERIFIED - 0 mapped per prompt requirement**)
  * **Index 14 (Chilli Healthy Chilli):** 458 images (SHORT)
  * *Unmapped / Excluded:*
    * `Nutrition Deficiency`: 444 images (Abiotic nutritional stress; outside pathology scope).
    * `White spot`: 195 images (Kept unmapped pending visual inspection / agronomic cause verification).

---

### D. Tomato (టమాటా)
* **Primary Source:** PlantVillage (`plantvillage.zip`)
* **License:** CC BY-NC 3.0 / Open Access
* **Secondary Candidate (Pending Approval):** PlantDoc (`https://github.com/pratikkayal/PlantDoc-Dataset`, MIT License)
* **Raw Image Counts (PlantVillage):**
  * **Index 15 (Tomato Early Blight):** 3,000 images (OK)
  * **Index 16 (Tomato Late Blight):** 5,727 images (OK)
  * **Index 17 (Tomato Yellow Leaf Curl Virus):** 16,071 images (OK)
  * **Index 18 (Tomato Spider Mites):** 5,028 images (OK)
  * **Index 19 (Tomato Healthy Tomato):** 4,773 images (OK)
  * *Unmapped / Excluded PlantVillage Classes:*
    * `Tomato___Bacterial_spot`: 6,381
    * `Tomato___Leaf_Mold`: 2,856
    * `Tomato___Septoria_leaf_spot`: 5,313
    * `Tomato___Target_Spot`: 4,212
    * `Tomato___Tomato_mosaic_virus`: 1,119

---

### E. Maize (మొక్కజొన్న)
* **Primary Sources:**
  1. Mendeley Data `10.17632/3myfctrgk3.1` (MLD: Multi-Class Maize Leaf Disease, CC BY 4.0)
  2. PlantVillage (`plantvillage.zip`, CC BY-NC 3.0)
* **Secondary Candidate (Pending Approval):** PlantDoc (`https://github.com/pratikkayal/PlantDoc-Dataset`, MIT License)
* **Raw Image Counts:**
  * **Index 20 (Maize Gray Leaf Spot):**
    * PlantVillage (`Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot`): 1,539
    * Mendeley 3myfctrgk3 (`Gray_Spot`): 555
    * *Total:* 2,094 (OK)
  * **Index 21 (Maize Common Rust):**
    * PlantVillage (`Corn_(maize)___Common_rust_`): 3,576
    * *Total:* 3,576 (OK)
  * **Index 22 (Maize Northern Leaf Blight):**
    * PlantVillage (`Corn_(maize)___Northern_Leaf_Blight`): 2,955
    * Mendeley 3myfctrgk3 (`Leaf_Blight`): 489
    * *Total:* 3,444 (OK)
  * **Index 23 (Maize Fall Armyworm):**
    * Mendeley 3myfctrgk3 (`Fall_Armyworm`): 525
    * *Total:* 525 (SHORT)
  * **Index 24 (Maize Healthy Maize):**
    * PlantVillage (`Corn_(maize)___healthy`): 3,486
    * Mendeley 3myfctrgk3 (`Healthy`): 427
    * *Total:* 3,913 (OK)

---

## 3. Potential Open Extra Sources for SHORT / BLOCKED Classes

For all classes below 1,000 photos, the following openly licensed repositories are identified for user review:

1. **Cotton Leaf Curl Virus (Index 07 - currently 25 photos, BLOCKED):**
   * *Mendeley Data:* `10.17632/45hdnnr4h5.1` ("Cotton Leaf Curl Disease (CLCuD) Dataset", CC BY 4.0) - ~450 field photos.
   * *Mendeley Data:* `10.17632/c92cvfct3d.1` ("Cotton Leaf Disease Dataset", CC BY 4.0) - ~300 photos.
2. **Cotton Alternaria Leaf Spot (Index 08 - currently 173 photos, BLOCKED):**
   * *Mendeley Data:* `10.17632/c92cvfct3d.1` (CC BY 4.0) - ~250 additional photos.
3. **Chilli Bacterial Spot (Index 10 - currently 156 photos, BLOCKED):**
   * *PlantVillage:* `Pepper__bell___Bacterial_spot` (CC BY-NC 3.0) - 997 photos (*Capsicum annuum*, same host species and same pathogen *Xanthomonas euvesicatoria*).
   * *Mendeley Data:* `10.17632/v6234vgw43.1` (CC BY 4.0) - ~350 field photos.
4. **Chilli Cercospora Leaf Spot (Index 12 - currently 180 photos, BLOCKED):**
   * *Mendeley Data:* `10.17632/49h82sv9xk.1` ("Chilli Disease Dataset", CC BY 4.0) - ~400 field photos.
5. **Chilli Leaf Curl & Healthy (Indices 11 & 14 - currently 423 & 458 photos, SHORT):**
   * *Mendeley Data:* `10.17632/49h82sv9xk.1` (CC BY 4.0) - ~500 field photos.
   * *PlantVillage:* `Pepper__bell___healthy` - 1,478 photos.
6. **Maize Fall Armyworm (Index 23 - currently 525 photos, SHORT):**
   * *Mendeley Data:* `10.17632/bwh3zbpkpv.1` ("CCMT: Dataset for Crop Pest and Disease Detection", CC BY 4.0) - ~1,200 authentic farm photos of *Spodoptera frugiperda* damage on maize.
7. **Rice Classes (Indices 00, 01, 02, 03, 04 - currently ~630-650 photos each, SHORT):**
   * *Mendeley Data:* `10.17632/fw8khx7p2d.1` ("Rice Leaf Diseases Dataset", CC BY 4.0) - ~1,200 photos covering blast, brown spot, and bacterial blight.
   * *Mendeley Data:* `10.17632/msr7294rm9.1` ("Rice Sheath Blight Dataset", CC BY 4.0) - ~800 field photos.
