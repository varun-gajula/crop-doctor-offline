# Task P4b: Decision Threshold Sweep & Options Table

**Date**: 2026-11-24  
**Model**: TR4 15-Class MobileNetV3-Large (`best_model.pth` / `vision.pte`)  
**Temperature**: 0.9433  

## 1. Probe Dataset Scope & Composition

- **Field Photos (`C:\crop-data\realtest\field`)**: 107 total photos.
  - **Near-Duplicates Excluded**: **30 photos** (perceptual hash Hamming distance $\le 8$ or exact SHA match against TR4 training data).
  - **Clean Photos Evaluated**: **77 photos**.
    - **Supported Classes (33 photos)**: `cotton_bacterial_blight` (5), `cotton_healthy` (5), `maize_common_rust` (3), `maize_healthy` (5), `maize_northern_leaf_blight` (7), `rice_healthy` (3), `rice_leaf_blast` (3), `tomato_healthy` (2). *(Note: `chilli_healthy`, `chilli_leaf_curl_virus`, and `tomato_septoria_leaf_spot` had all photos excluded as training near-duplicates)*.
    - **Unsupported Diseases on Supported Crops (44 photos)**: `cotton_fusarium_wilt` (5), `cotton_leaf_curl_virus` (5), `chilli_nutrition_deficiency` (5), `maize_gray_leaf_spot` (6), `rice_bacterial_leaf_blight` (5), `rice_sheath_blight` (5), `tomato_leaf_blight` (7), `tomato_yellow_leaf_curl_virus` (6).
- **`nocrop` (`C:\crop-data\realtest\nocrop`)**: 30 real phone photos of non-crop objects.
- **`othercrop` (`C:\crop-data\realtest\othercrop`)**: 48 photos of non-supported crop species (apple, grape, potato, sugarcane, etc.).
- **`unsupported_same_crop` (`C:\crop-data\realtest\unsupported`)**: 93 photos of diseases on supported crops outside the 15 classes.
- **`screen` (`C:\crop-data\realtest\screen`)**: 20 photos of crop leaves displayed on laptop/phone screens.
- **Preprocessing**: Viewfinder square crop applied to camera photos matching `ImagePreprocessor.kt` (`fraction=0.37`), bilinear resize to 512x512, ImageNet normalization.

---

## 2. Threshold Sweep Options Table

Targets specified by Requirement 4:
- **Target 1**: At least **90%** of supported photos not `OUT OF CLASS` (i.e. $\le 10\%$ `OUT OF CLASS`).
- **Target 2**: At least **80%** of `nocrop` `CAN'T TELL`.
- **Target 3**: At least **60%** of `othercrop` `OUT OF CLASS` or `CAN'T TELL`.
- **Target 4**: As few wrong `DIAGNOSE` decisions as possible.

| Option ID | Conf Th | Energy Th | Crop Mass Th | Margin Th | Supp DIAG (n/N, %) | Supp CAN'T TELL (n, %) | Supp OUT OF CLASS (n, %) | Supp DIAG Acc (n/N, %) | Wrong-Crop Errors | Nocrop CAN'T TELL (n, %) | Othercrop Rej (n, %) | Unsupp Wrong DIAG (n/93, %) | Targets Met? |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Opt-0** | 0.75 | -4.59 | 0.60 | 0.25 | 19/33 (57.6%) | 14 (42.4%) | 0 (0.0%) | 16/19 (84.2%) | 17 | 26/30 (86.7%) | 35/48 (72.9%) | 22/93 (23.7%) | **YES** |
| **Opt-1** | 0.45 | -6.50 | 0.45 | 0.15 | 1/33 (3.0%) | 32 (97.0%) | 0 (0.0%) | 1/1 (100.0%) | 0 | 30/30 (100.0%) | 48/48 (100.0%) | 0/93 (0.0%) | **YES** |
| **Opt-2** | 0.50 | -6.00 | 0.50 | 0.15 | 3/33 (9.1%) | 30 (90.9%) | 0 (0.0%) | 3/3 (100.0%) | 0 | 30/30 (100.0%) | 48/48 (100.0%) | 0/93 (0.0%) | **YES** |
| **Opt-3** | 0.55 | -5.50 | 0.50 | 0.20 | 3/33 (9.1%) | 30 (90.9%) | 0 (0.0%) | 3/3 (100.0%) | 1 | 29/30 (96.7%) | 48/48 (100.0%) | 2/93 (2.2%) | **YES** |
| **Opt-4** | 0.60 | -5.00 | 0.55 | 0.20 | 14/33 (42.4%) | 19 (57.6%) | 0 (0.0%) | 11/14 (78.6%) | 10 | 27/30 (90.0%) | 41/48 (85.4%) | 14/93 (15.1%) | **YES** |
| **Opt-5** | 0.65 | -5.00 | 0.55 | 0.20 | 14/33 (42.4%) | 19 (57.6%) | 0 (0.0%) | 11/14 (78.6%) | 10 | 27/30 (90.0%) | 41/48 (85.4%) | 14/93 (15.1%) | **YES** |
| **Opt-6** | 0.65 | -4.75 | 0.55 | 0.20 | 16/33 (48.5%) | 17 (51.5%) | 0 (0.0%) | 13/16 (81.2%) | 15 | 26/30 (86.7%) | 37/48 (77.1%) | 18/93 (19.4%) | **YES** |
| **Opt-7** | 0.70 | -4.70 | 0.60 | 0.20 | 17/33 (51.5%) | 16 (48.5%) | 0 (0.0%) | 14/17 (82.4%) | 16 | 26/30 (86.7%) | 36/48 (75.0%) | 21/93 (22.6%) | **YES** |
| **Opt-8** | 0.70 | -4.50 | 0.60 | 0.20 | 20/33 (60.6%) | 13 (39.4%) | 0 (0.0%) | 17/20 (85.0%) | 18 | 25/30 (83.3%) | 35/48 (72.9%) | 22/93 (23.7%) | **YES** |
| **Opt-9** | 0.65 | -4.59 | 0.65 | 0.25 | 20/33 (60.6%) | 13 (39.4%) | 0 (0.0%) | 17/20 (85.0%) | 17 | 26/30 (86.7%) | 35/48 (72.9%) | 22/93 (23.7%) | **YES** |
| **Opt-10** | 0.75 | -4.00 | 0.60 | 0.25 | 26/33 (78.8%) | 7 (21.2%) | 0 (0.0%) | 22/26 (84.6%) | 33 | 17/30 (56.7%) | 27/48 (56.2%) | 34/93 (36.6%) | NO |
| **Opt-11** | 0.80 | -3.50 | 0.70 | 0.30 | 25/33 (75.8%) | 6 (18.2%) | 2 (6.1%) | 21/25 (84.0%) | 38 | 10/30 (33.3%) | 26/48 (54.2%) | 38/93 (40.9%) | NO |

---

## 3. Per-Class Diagnostic Accuracy on Clean Supported Field Photos

Breakdown across candidate options on the 8 supported classes present in the clean field set (33 photos total):

| Class Name | Total Clean | Opt-0 (Acc n/N) | Opt-4 (Acc n/N) | Opt-6 (Acc n/N) | Opt-7 (Acc n/N) | Opt-8 (Acc n/N) |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| `cotton_bacterial_blight` | 5 | 2/2 | 1/1 | 1/1 | 2/2 | 2/2 |
| `cotton_healthy` | 5 | 1/1 | 1/1 | 1/1 | 1/1 | 1/1 |
| `maize_common_rust` | 3 | 3/3 | 2/2 | 3/3 | 3/3 | 3/3 |
| `maize_healthy` | 5 | 3/3 | 1/1 | 2/2 | 2/2 | 3/3 |
| `maize_northern_leaf_blight` | 7 | 2/5 | 1/4 | 1/4 | 1/4 | 3/6 |
| `rice_healthy` | 3 | 2/2 | 2/2 | 2/2 | 2/2 | 2/2 |
| `rice_leaf_blast` | 3 | 3/3 | 3/3 | 3/3 | 3/3 | 3/3 |
| `tomato_healthy` | 2 | 0/0 | 0/0 | 0/0 | 0/0 | 0/0 |
| **TOTAL SUPPORTED** | 33 | **16/19 (84.2%)** | **11/14 (78.6%)** | **13/16 (81.2%)** | **14/17 (82.4%)** | **17/20 (85.0%)** |

---

## 4. Key Trade-off Analysis & Target Evaluation

1. **Nocrop Rejection (Target $\ge 80\%$)**:
   - Options with energy threshold $\le -4.50$ achieve **83.3% to 100.0%** `CAN'T TELL` on `nocrop` user phone objects.
   - All options pass Gate 1 (Leaf Validator fail-closed HSV check) on non-plant objects. Permissive energy thresholds (e.g. -4.00, -3.50) fail the target because some plant-like textures pass Gate 1 and then bypass the relaxed energy gate.
2. **Othercrop Rejection (Target $\ge 60\%$)**:
   - Current Baseline (Opt-0: `conf=0.75, energy=-4.59`): achieves **72.9% (35/48)** rejection.
   - Sweet-spot options (Opt-6, Opt-7, Opt-8): achieve **72.9% to 77.1%** rejection, safely beating the 60% target.
3. **Supported Photos Not OUT OF CLASS (Target $\ge 90\%$)**:
   - All balanced and guarded options (Opt-0 through Opt-8) achieve **100.0% not OUT OF CLASS** (0% OUT OF CLASS) on clean supported field photos when the farmer selects the correct crop.
4. **Unsupported Same-Crop False Diagnoses**:
   - On `unsupported_same_crop` (93 photos): stricter energy/confidence bounds limit false diagnoses to 15-24%, whereas permissive thresholds falsely diagnose over 36-40%.
   - **Opt-0 (Current Baseline)**, **Opt-6**, and **Opt-8** meet all 3 targets cleanly.

*(Note: `app/src/main/assets/thresholds.json` is UNCHANGED as required by Step 5)*.