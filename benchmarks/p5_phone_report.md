# Task P5: End-to-End Phone Benchmark & Robustness Check

**Date:** 2026-11-20 19:16:17  
**Device:** POCO 22041219PI (arm64-v8a)  
**OS Version:** Android 14  
**Total Physical RAM:** 5.5 GB (5,731,384 kB) — Reference 6 GB Phone (Rule H15)  

---

## 1. Executive Summary: Acceptance Gates

| Gate Criteria | Target Requirement | Measured Physical Phone Value | Status |
| :--- | :--- | :--- | :--- |
| **ANR Count** | Exactly 0 | **0** | **PASS** |
| **Memory Stability (20 Captures)** | Net PSS growth < 50 MB | **-1.74 MB** (213.65 -> 211.91 MB) | **PASS** |
| **Real Leaves Diagnosis Rate (25 photos)** | >= 80.0% diagnosed | **80.0%** (20/25) | **PASS** |
| **Non-Crop Rejection (10 photos)** | >= 80.0% CANT_TELL / OUT_OF_CLASS | **100.0%** (10/10) | **PASS** |
| **Other Crop Rejection (10 photos)** | >= 60.0% OUT_OF_CLASS / CANT_TELL | **90.0%** (9/10) | **PASS** |
| **Shutter-to-Result Latency** | <= 1,500 ms budget | **769.5 ms** (min: 621.2 ms, max: 1759.7 ms) | **PASS** |

---

## 2. Scripted Run Decision Table (55 Test Photos)

| # | Category | Selected Crop | Test Photo | Decision Outcome | Reason Code | Top-1 Class | Prob | Crop Mass | Time (ms) |
|:---:|:---|:---|:---|:---:|:---|:---|:---:|:---:|:---:|
| 1 | `real_rice` | `rice` | `real_rice_01.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_bacterial_leaf_blight` | 0.9006 | 0.9983 | 700.0 |
| 2 | `real_rice` | `rice` | `real_rice_02.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_bacterial_leaf_blight` | 0.8697 | 0.9989 | 787.8 |
| 3 | `real_rice` | `rice` | `real_rice_03.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_bacterial_leaf_blight` | 0.9743 | 0.9967 | 701.0 |
| 4 | `real_rice` | `rice` | `real_rice_04.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_bacterial_leaf_blight` | 0.8622 | 0.9961 | 1759.7 |
| 5 | `real_rice` | `rice` | `real_rice_05.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_bacterial_leaf_blight` | 0.5963 | 0.9779 | 744.6 |
| 6 | `real_cotton` | `cotton` | `real_cotton_01.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_bacterial_blight` | 1.0000 | 1.0000 | 789.2 |
| 7 | `real_cotton` | `cotton` | `real_cotton_02.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_bacterial_blight` | 0.9988 | 0.9996 | 886.1 |
| 8 | `real_cotton` | `cotton` | `real_cotton_03.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_bacterial_blight` | 1.0000 | 1.0000 | 678.6 |
| 9 | `real_cotton` | `cotton` | `real_cotton_04.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_bacterial_blight` | 0.9975 | 1.0000 | 685.1 |
| 10 | `real_cotton` | `cotton` | `real_cotton_05.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_bacterial_blight` | 0.9990 | 0.9988 | 663.4 |
| 11 | `real_chilli` | `chilli` | `real_chilli_01.jpg` | **DIAGNOSE** | `ACCEPTED` | `chilli_healthy` | 1.0000 | 1.0000 | 815.1 |
| 12 | `real_chilli` | `chilli` | `real_chilli_02.jpg` | **DIAGNOSE** | `ACCEPTED` | `chilli_healthy` | 1.0000 | 1.0000 | 738.7 |
| 13 | `real_chilli` | `chilli` | `real_chilli_03.jpg` | **DIAGNOSE** | `ACCEPTED` | `chilli_healthy` | 1.0000 | 1.0000 | 762.4 |
| 14 | `real_chilli` | `chilli` | `real_chilli_04.jpg` | **DIAGNOSE** | `ACCEPTED` | `chilli_healthy` | 1.0000 | 1.0000 | 841.0 |
| 15 | `real_chilli` | `chilli` | `real_chilli_05.jpg` | **DIAGNOSE** | `ACCEPTED` | `chilli_healthy` | 1.0000 | 1.0000 | 929.0 |
| 16 | `real_tomato` | `tomato` | `real_tomato_01.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.3649 | 0.6584 | 755.4 |
| 17 | `real_tomato` | `tomato` | `real_tomato_02.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `tomato_healthy` | 0.7518 | 0.0136 | 858.0 |
| 18 | `real_tomato` | `tomato` | `real_tomato_03.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `tomato_healthy` | 0.6547 | 0.0749 | 893.7 |
| 19 | `real_tomato` | `tomato` | `real_tomato_04.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_healthy` | 0.6246 | 0.2500 | 693.9 |
| 20 | `real_tomato` | `tomato` | `real_tomato_05.jpg` | **DIAGNOSE** | `ACCEPTED` | `tomato_healthy` | 0.9375 | 0.9936 | 630.1 |
| 21 | `real_maize` | `maize` | `real_maize_01.jpg` | **DIAGNOSE** | `ACCEPTED` | `maize_common_rust` | 0.9999 | 1.0000 | 873.2 |
| 22 | `real_maize` | `maize` | `real_maize_02.jpg` | **DIAGNOSE** | `ACCEPTED` | `maize_common_rust` | 0.9997 | 0.9999 | 735.3 |
| 23 | `real_maize` | `maize` | `real_maize_03.jpg` | **DIAGNOSE** | `ACCEPTED` | `maize_gray_leaf_spot` | 0.5139 | 0.9908 | 1028.6 |
| 24 | `real_maize` | `maize` | `real_maize_04.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `maize_gray_leaf_spot` | 0.8847 | 0.7996 | 621.2 |
| 25 | `real_maize` | `maize` | `real_maize_05.jpg` | **DIAGNOSE** | `ACCEPTED` | `maize_northern_leaf_blight` | 0.9176 | 0.9953 | 622.1 |
| 26 | `nocrop` | `auto` | `nocrop_01.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.3234 | 1.0000 | 942.6 |
| 27 | `nocrop` | `auto` | `nocrop_02.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.5769 | 1.0000 | 678.1 |
| 28 | `nocrop` | `auto` | `nocrop_03.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.7890 | 1.0000 | 680.7 |
| 29 | `nocrop` | `auto` | `nocrop_04.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_leaf_blight` | 0.2360 | 1.0000 | 664.0 |
| 30 | `nocrop` | `auto` | `nocrop_05.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `maize_northern_leaf_blight` | 0.4920 | 1.0000 | 769.0 |
| 31 | `nocrop` | `auto` | `nocrop_06.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `maize_gray_leaf_spot` | 0.3485 | 1.0000 | 807.8 |
| 32 | `nocrop` | `auto` | `nocrop_07.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_leaf_blight` | 0.7477 | 1.0000 | 670.8 |
| 33 | `nocrop` | `auto` | `nocrop_08.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `maize_gray_leaf_spot` | 0.4111 | 1.0000 | 844.2 |
| 34 | `nocrop` | `auto` | `nocrop_09.jpg` | **OUT_OF_CLASS** | `UNSUPPORTED_CLASS` | `tomato_leaf_blight` | 0.9764 | 1.0000 | 726.4 |
| 35 | `nocrop` | `auto` | `nocrop_10.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `maize_gray_leaf_spot` | 0.5457 | 1.0000 | 798.1 |
| 36 | `othercrop` | `rice` | `othercrop_01.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `rice_leaf_blast` | 0.4552 | 0.0012 | 816.7 |
| 37 | `othercrop` | `cotton` | `othercrop_02.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `cotton_bacterial_blight` | 0.9725 | 0.0278 | 711.0 |
| 38 | `othercrop` | `chilli` | `othercrop_03.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `chilli_leaf_curl_virus` | 0.8126 | 0.0003 | 770.6 |
| 39 | `othercrop` | `tomato` | `othercrop_04.jpg` | **DIAGNOSE** | `ACCEPTED` | `tomato_septoria_leaf_spot` | 0.8177 | 0.7271 | 676.1 |
| 40 | `othercrop` | `maize` | `othercrop_05.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `maize_common_rust` | 0.8988 | 0.0050 | 856.8 |
| 41 | `othercrop` | `rice` | `othercrop_06.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `rice_sheath_blight` | 0.5592 | 0.0054 | 764.1 |
| 42 | `othercrop` | `cotton` | `othercrop_07.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `cotton_leaf_curl_virus` | 0.8185 | 0.0288 | 682.3 |
| 43 | `othercrop` | `chilli` | `othercrop_08.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `chilli_nutrition_deficiency` | 0.6275 | 0.0050 | 771.8 |
| 44 | `othercrop` | `tomato` | `othercrop_09.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_healthy` | 0.5894 | 0.7142 | 637.2 |
| 45 | `othercrop` | `maize` | `othercrop_10.jpg` | **OUT_OF_CLASS** | `LOW_CROP_MASS` | `maize_northern_leaf_blight` | 0.3773 | 0.0001 | 653.6 |
| 46 | `screen` | `auto` | `screen_01.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_septoria_leaf_spot` | 0.6947 | 1.0000 | 773.4 |
| 47 | `screen` | `auto` | `screen_02.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_sheath_blight` | 0.9744 | 1.0000 | 671.8 |
| 48 | `screen` | `auto` | `screen_03.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_healthy` | 0.9970 | 1.0000 | 741.9 |
| 49 | `screen` | `auto` | `screen_04.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_bacterial_blight` | 0.8637 | 1.0000 | 665.9 |
| 50 | `screen` | `auto` | `screen_05.jpg` | **DIAGNOSE** | `ACCEPTED` | `cotton_fusarium_wilt` | 0.9710 | 1.0000 | 655.3 |
| 51 | `screen` | `auto` | `screen_06.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `tomato_yellow_leaf_curl_virus` | 0.5971 | 1.0000 | 675.7 |
| 52 | `screen` | `auto` | `screen_07.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `rice_healthy` | 0.5194 | 1.0000 | 633.6 |
| 53 | `screen` | `auto` | `screen_08.jpg` | **DIAGNOSE** | `ACCEPTED` | `chilli_leaf_curl_virus` | 0.9604 | 1.0000 | 789.1 |
| 54 | `screen` | `auto` | `screen_09.jpg` | **CANT_TELL** | `HIGH_FREE_ENERGY` | `rice_healthy` | 0.6750 | 1.0000 | 759.1 |
| 55 | `screen` | `auto` | `screen_10.jpg` | **DIAGNOSE** | `ACCEPTED` | `rice_healthy` | 0.9641 | 1.0000 | 840.1 |

---

## 3. 20 Consecutive Camera Captures Table (Memory & ANR Stability)

| Run # | Camera File | Time (ms) | PSS Before (MB) | PSS After (MB) | Delta PSS (MB) | Outcome | Decision Code | ANR Count |
|:---:|:---|:---:|:---:|:---:|:---:|:---:|:---|:---:|
| 1 | `camera_real_01_Corn_Gray_leaf_spot_IMG_42231.jpg` | 1010.0 | 215.66 | 216.78 | +1.12 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 2 | `camera_real_02_Corn_Gray_leaf_spot_rsz0803Figure6.jpg` | 648.6 | 212.06 | 215.21 | +3.15 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 3 | `camera_real_03_Corn_leaf_blight_02c.jpg` | 894.2 | 212.10 | 215.00 | +2.90 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 4 | `camera_real_04_Corn_leaf_blight_0796.20graylssymt.jpg` | 608.1 | 214.36 | 216.87 | +2.50 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 5 | `camera_real_05_Corn_rust_leaf_0796.39maizerust.jpg` | 616.2 | 212.58 | 215.13 | +2.56 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 6 | `camera_real_06_Corn_rust_leaf_0796.40comrust.jpg` | 850.3 | 213.00 | 215.68 | +2.68 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 7 | `camera_real_07_Tomato_Early_blight_leaf_039b47d574bc4bb8a14259a1cd96a741.jpg` | 644.1 | 212.44 | 215.53 | +3.09 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 8 | `camera_real_08_Tomato_Early_blight_leaf_100983448.jpg` | 710.2 | 212.67 | 215.68 | +3.01 | **OUT_OF_CLASS** | `BORDERLINE_AMBIGUOUS` | 0 |
| 9 | `camera_real_09_Tomato_leaf_1684.jpg` | 684.1 | 211.75 | 214.96 | +3.21 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 10 | `camera_real_10_Tomato_leaf_2013-08-20-06.jpg` | 651.2 | 211.79 | 215.08 | +3.29 | **CANT_TELL** | `GLOBAL_TOP1_WEEDS` | 0 |
| 11 | `camera_real_11_Tomato_leaf_bacterial_spot__2320_Bacterial_Spot_and_Speck.jpg` | 728.9 | 211.72 | 214.91 | +3.19 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 12 | `camera_real_12_Tomato_leaf_bacterial_spot_07.17.18-Common_Tomato_Diseases_Canker-258x300.jpg` | 633.3 | 212.98 | 217.21 | +4.23 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 13 | `camera_real_13_Tomato_leaf_late_blight_110822-206-Tomato-blight.jpg` | 600.4 | 214.48 | 215.77 | +1.29 | **OUT_OF_CLASS** | `UNSUPPORTED_CLASS` | 0 |
| 14 | `camera_real_14_Tomato_leaf_late_blight_18c.jpg` | 630.4 | 212.89 | 215.09 | +2.20 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 15 | `camera_real_15_Tomato_leaf_mosaic_virus_9511.img.jpg` | 594.8 | 211.93 | 214.86 | +2.93 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 16 | `camera_real_16_Tomato_leaf_mosaic_virus_D_29AlfMov.jpg` | 618.9 | 214.28 | 217.28 | +3.00 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 17 | `camera_real_17_Tomato_leaf_yellow_virus_11-40580_5.jpg` | 612.4 | 214.26 | 215.90 | +1.64 | **DIAGNOSE** | `ACCEPTED` | 0 |
| 18 | `camera_real_18_Tomato_leaf_yellow_virus_17fc47.jpg` | 768.4 | 214.44 | 217.48 | +3.04 | **CANT_TELL** | `GLOBAL_TOP1_WEEDS` | 0 |
| 19 | `camera_real_19_Tomato_mold_leaf_49_20Leafmold_20Bottom.jpg` | 622.2 | 212.91 | 215.90 | +2.99 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |
| 20 | `camera_real_20_Tomato_mold_leaf_50_20Leafmold_20Top.jpg` | 752.5 | 212.00 | 214.61 | +2.61 | **CANT_TELL** | `HIGH_FREE_ENERGY` | 0 |

---

## 4. Latency & Robustness Distributions by Category

| Category | Count | Diagnosed | Can't Tell | Out of Class | Min Time (ms) | Mean Time (ms) | Max Time (ms) |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **real_rice** | 5 | 5 | 0 | 0 | 700.0 | 938.6 | 1759.7 |
| **real_cotton** | 5 | 5 | 0 | 0 | 663.4 | 740.5 | 886.1 |
| **real_chilli** | 5 | 5 | 0 | 0 | 738.7 | 817.2 | 929.0 |
| **real_tomato** | 5 | 1 | 2 | 2 | 630.1 | 766.2 | 893.7 |
| **real_maize** | 5 | 4 | 1 | 0 | 621.2 | 776.1 | 1028.6 |
| **nocrop** | 10 | 0 | 9 | 1 | 664.0 | 758.2 | 942.6 |
| **othercrop** | 10 | 1 | 2 | 7 | 637.2 | 734.0 | 856.8 |
| **screen** | 10 | 6 | 4 | 0 | 633.6 | 720.6 | 840.1 |
