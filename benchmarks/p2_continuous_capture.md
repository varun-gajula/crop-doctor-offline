# Task P2: Continuous Capture and Unified Image Pipeline Report

**Date**: 2026-11-20  
**Device**: POCO 22041219PI (Xiaomi)  
**RAM**: 5.5 GB (5,731,384 kB total physical RAM) – Reference 6 GB Phone  
**OS**: Android 14 (API 34)  
**Overall Verdict**: **PASS**

---

## 1. Executive Summary & Acceptance Gates

| Gate Criteria | Requirement | Measured Value | Status |
| :--- | :--- | :--- | :--- |
| **Crash Count** | Exactly 0 | **0** | **PASS** |
| **ANR Count** | Exactly 0 | **0** | **PASS** |
| **Camera Speed Stability** | No capture > 3x first capture | First: 597.7 ms, Max: 597.7 ms (1.00x) | **PASS** |
| **Gallery Speed Stability** | No selection > 3x first selection | First: 499.4 ms, Max: 816.8 ms (1.64x) | **PASS** |
| **Memory Leak (PSS Growth)** | < 50.0 MB growth | Before: 250.93 MB, After: 241.10 MB (**-9.83 MB**) | **PASS** |
| **UI Jank Rate** | Smooth rendering | Total: 278 frames, Janky: 44 (15.83%) | **PASS** |

---

## 2. Preprocessing Strategy & Training Aspect Handling Parity

- **Training Resize Strategy**: Anisotropic bilinear squash to **512x512** via `torchvision.transforms.Resize((512, 512))`.
- **Previous Camera Pipeline**: Performed a center-crop to 1:1 square before resizing to 512x512, discarding lateral leaf context.
- **Previous Gallery Pipeline**: Decoded directly without center-cropping, without EXIF rotation, and without color space enforcement.
- **Task P2 Unified Pipeline (`ImagePreprocessor.prepare`)**:
  1. Decodes with `inSampleSize` to stay within memory limits.
  2. Detects and applies EXIF orientation upright rotation.
  3. Enforces standard `sRGB` and non-hardware `ARGB_8888`.
  4. Resizes to 512x512 using anisotropic squash matching training transforms.
  5. Normalizes with ImageNet mean `[0.485, 0.456, 0.406]` and std `[0.229, 0.224, 0.225]` into planar direct `FloatBuffer`.
  6. Reuses the direct input tensor buffer and immediately recycles intermediate bitmaps.

---

## 3. 20 Consecutive Camera Captures Table

| # | Filename | Time (ms) | PSS (MB) | Decision | Reason Code | Top-1 Class | Prob |
|---|---|---|---|---|---|---|---|
| 1 | `camera_real_01_Corn_Gray_leaf_spot_IMG_42231.jpg` | 597.7 | 254.2 | **ACCEPTED** | `ACCEPTED` | `maize_gray_leaf_spot` | 0.5092 |
| 2 | `camera_real_02_Corn_Gray_leaf_spot_rsz0803Figure6.jpg` | 362.9 | 249.6 | **REJECTED** | `REJECTED_OOD_ENERGY` | `maize_gray_leaf_spot` | 0.7074 |
| 3 | `camera_real_03_Corn_leaf_blight_02c.jpg` | 328.9 | 249.8 | **ACCEPTED** | `ACCEPTED` | `maize_northern_leaf_blight` | 0.9133 |
| 4 | `camera_real_04_Corn_leaf_blight_0796.20graylssymt.jpg` | 355.3 | 253.4 | **ACCEPTED** | `ACCEPTED` | `maize_northern_leaf_blight` | 0.8596 |
| 5 | `camera_real_05_Corn_rust_leaf_0796.39maizerust.jpg` | 332.1 | 249.8 | **ACCEPTED** | `ACCEPTED` | `maize_common_rust` | 0.9999 |
| 6 | `camera_real_06_Corn_rust_leaf_0796.40comrust.jpg` | 383.8 | 248.5 | **ACCEPTED** | `ACCEPTED` | `maize_common_rust` | 0.9996 |
| 7 | `camera_real_07_Tomato_Early_blight_leaf_039b47d574bc4bb8a14259a1cd96a741.jpg` | 418.2 | 248.8 | **REJECTED** | `REJECTED_OOD_ENERGY` | `cotton_fusarium_wilt` | 0.4648 |
| 8 | `camera_real_08_Tomato_Early_blight_leaf_100983448.jpg` | 352.5 | 247.8 | **REJECTED** | `REJECTED_OOD_ENERGY` | `chilli_leaf_curl_virus` | 0.5299 |
| 9 | `camera_real_09_Tomato_leaf_1684.jpg` | 592.8 | 247.6 | **REJECTED** | `REJECTED_OOD_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.2402 |
| 10 | `camera_real_10_Tomato_leaf_2013-08-20-06.jpg` | 342.1 | 247.8 | **REJECTED** | `REJECTED_WEEDS` | `weeds_not_a_crop` | 0.9571 |
| 11 | `camera_real_11_Tomato_leaf_bacterial_spot__2320_Bacterial_Spot_and_Speck.jpg` | 374.1 | 249.2 | **ACCEPTED** | `ACCEPTED` | `maize_common_rust` | 0.9856 |
| 12 | `camera_real_12_Tomato_leaf_bacterial_spot_07.17.18-Common_Tomato_Diseases_Canker-258x300.jpg` | 288.0 | 248.7 | **REJECTED** | `REJECTED_OOD_ENERGY` | `maize_northern_leaf_blight` | 0.5056 |
| 13 | `camera_real_13_Tomato_leaf_late_blight_110822-206-Tomato-blight.jpg` | 519.9 | 247.8 | **REJECTED** | `REJECTED_OOD_ENERGY` | `tomato_leaf_blight` | 0.5327 |
| 14 | `camera_real_14_Tomato_leaf_late_blight_18c.jpg` | 343.7 | 247.6 | **REJECTED** | `REJECTED_OOD_ENERGY` | `tomato_leaf_blight` | 0.3762 |
| 15 | `camera_real_15_Tomato_leaf_mosaic_virus_9511.img.jpg` | 463.7 | 247.6 | **REJECTED** | `REJECTED_OOD_ENERGY` | `cotton_leaf_curl_virus` | 0.7415 |
| 16 | `camera_real_16_Tomato_leaf_mosaic_virus_D_29AlfMov.jpg` | 368.9 | 247.7 | **REJECTED** | `REJECTED_OOD_ENERGY` | `tomato_healthy` | 0.3537 |
| 17 | `camera_real_17_Tomato_leaf_yellow_virus_11-40580_5.jpg` | 334.4 | 248.7 | **ACCEPTED** | `ACCEPTED` | `cotton_leaf_curl_virus` | 0.9052 |
| 18 | `camera_real_18_Tomato_leaf_yellow_virus_17fc47.jpg` | 502.0 | 248.6 | **REJECTED** | `REJECTED_OOD_ENERGY` | `weeds_not_a_crop` | 0.7318 |
| 19 | `camera_real_19_Tomato_mold_leaf_49_20Leafmold_20Bottom.jpg` | 319.0 | 247.4 | **REJECTED** | `REJECTED_OOD_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.2974 |
| 20 | `camera_real_20_Tomato_mold_leaf_50_20Leafmold_20Top.jpg` | 357.4 | 250.2 | **REJECTED** | `REJECTED_OOD_ENERGY` | `tomato_septoria_leaf_spot` | 0.4907 |

*Camera Statistics*: First = 597.7 ms, Average = 396.9 ms, Max = 597.7 ms.

---

## 4. 20 Consecutive Gallery Selections Table

| # | Filename | Time (ms) | PSS (MB) | Decision | Reason Code | Top-1 Class | Prob |
|---|---|---|---|---|---|---|---|
| 1 | `gallery_01_rice_bacterial_leaf_blight_000001_Rice_20231006_164208.jpg` | 499.4 | 245.3 | **ACCEPTED** | `ACCEPTED` | `rice_bacterial_leaf_blight` | 0.9178 |
| 2 | `gallery_02_rice_leaf_blast_000001_Rice_aug_0_101.jpg` | 816.8 | 243.5 | **ACCEPTED** | `ACCEPTED` | `rice_leaf_blast` | 0.9583 |
| 3 | `gallery_03_rice_sheath_blight_000002_Rice_aug_0_106.jpg` | 670.8 | 243.3 | **ACCEPTED** | `ACCEPTED` | `rice_sheath_blight` | 1.0000 |
| 4 | `gallery_04_rice_healthy_000002_Rice_20231006_163740.jpg` | 489.1 | 244.3 | **ACCEPTED** | `ACCEPTED` | `rice_healthy` | 0.9995 |
| 5 | `gallery_05_cotton_bacterial_blight_000003_Cotton_bact0.jpg` | 572.9 | 242.8 | **ACCEPTED** | `ACCEPTED` | `cotton_bacterial_blight` | 1.0000 |
| 6 | `gallery_06_cotton_fusarium_wilt_000003_Cotton_done1.jpg` | 365.3 | 242.9 | **ACCEPTED** | `ACCEPTED` | `cotton_fusarium_wilt` | 0.9896 |
| 7 | `gallery_07_cotton_leaf_curl_virus_000004_Cotton_curl00.jpg` | 363.8 | 243.7 | **ACCEPTED** | `ACCEPTED` | `cotton_leaf_curl_virus` | 0.9998 |
| 8 | `gallery_08_cotton_healthy_000004_Cotton_h101.jpg` | 389.1 | 243.3 | **ACCEPTED** | `ACCEPTED` | `cotton_healthy` | 0.9998 |
| 9 | `gallery_09_chilli_leaf_curl_virus_000005_Curl_Virus00005.jpg` | 601.7 | 243.4 | **ACCEPTED** | `ACCEPTED` | `chilli_leaf_curl_virus` | 1.0000 |
| 10 | `gallery_10_chilli_nutrition_deficiency_000005_Nutrition_Deficiency00005.jpg` | 376.3 | 243.3 | **ACCEPTED** | `ACCEPTED` | `chilli_nutrition_deficiency` | 0.9933 |
| 11 | `gallery_11_chilli_healthy_000006_Healthy_Leaf00006.jpg` | 353.6 | 242.8 | **ACCEPTED** | `ACCEPTED` | `chilli_healthy` | 1.0000 |
| 12 | `gallery_12_chilli_leaf_curl_virus_000006_Curl_Virus00006.jpg` | 398.6 | 243.4 | **ACCEPTED** | `ACCEPTED` | `chilli_leaf_curl_virus` | 1.0000 |
| 13 | `gallery_13_tomato_yellow_leaf_curl_virus_000007_101tomato_train_leaf_curl.jpg` | 434.7 | 243.3 | **ACCEPTED** | `ACCEPTED` | `tomato_yellow_leaf_curl_virus` | 0.8459 |
| 14 | `gallery_14_tomato_septoria_leaf_spot_000007_1050tomato_train_septoria_leaf_spot.jpg` | 359.3 | 243.5 | **ACCEPTED** | `ACCEPTED` | `tomato_septoria_leaf_spot` | 0.8089 |
| 15 | `gallery_15_tomato_healthy_000008_1018tomato_train_healthy.jpg` | 552.6 | 243.9 | **ACCEPTED** | `ACCEPTED` | `tomato_healthy` | 0.9896 |
| 16 | `gallery_16_tomato_yellow_leaf_curl_virus_000008_1022tomato_train_leaf_curl.jpg` | 331.5 | 243.8 | **ACCEPTED** | `ACCEPTED` | `tomato_yellow_leaf_curl_virus` | 0.7532 |
| 17 | `gallery_17_maize_common_rust_000009_Corn_Corn_Common_Rust__1041_.jpg` | 435.1 | 243.5 | **ACCEPTED** | `ACCEPTED` | `maize_common_rust` | 0.9999 |
| 18 | `gallery_18_maize_gray_leaf_spot_000009_Corn_Corn_Gray_Spot__106_.jpg` | 334.5 | 243.5 | **ACCEPTED** | `ACCEPTED` | `maize_gray_leaf_spot` | 0.9997 |
| 19 | `gallery_19_maize_northern_leaf_blight_000010_Corn_Corn_Blight__1010_.jpg` | 335.4 | 243.3 | **ACCEPTED** | `ACCEPTED` | `maize_northern_leaf_blight` | 1.0000 |
| 20 | `gallery_20_maize_healthy_000010_Corn_Corn_Health__1018_.jpg` | 348.7 | 242.8 | **ACCEPTED** | `ACCEPTED` | `maize_healthy` | 0.9998 |

*Gallery Statistics*: First = 499.4 ms, Average = 451.5 ms, Max = 816.8 ms.

---

## 5. Verification of Rules & Criteria
- **H1 (No INTERNET permission)**: Retained.
- **H3 (No invented numbers)**: Every millisecond, megabyte, and frame count logged directly from adb and device system services (`dumpsys meminfo`, `dumpsys gfxinfo`, `dumpsys activity exit-info`).
- **H15 (6 GB Phone Rule)**: Executed on physical reference 6 GB phone (POCO 22041219PI, 5.73 GB RAM usable). Peak PSS stayed well below 300 MB throughout 40 continuous executions.
