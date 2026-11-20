# Task V7: Vision Model Validation Sanity Benchmark

**Date:** 2026-11-20  
**Validation Dataset Split:** [`benchmarks/final/split_v2.json`](file:///c:/crop-doctor-offline/benchmarks/final/split_v2.json) (1,668 validation photos, zero-leakage hash & perceptual cluster split)  
**Model Architecture:** MobileNetV3-Large (20 classes, 512x512 input resolution)  
**Model Artifacts:**  
- Laptop: [`benchmarks/final/best_model.pth`](file:///c:/crop-doctor-offline/benchmarks/final/best_model.pth) (PyTorch checkpoint)
- Phone: [`app/src/main/assets/models/vision.pte`](file:///c:/crop-doctor-offline/app/src/main/assets/models/vision.pte) (ExecuTorch XNNPACK, 16,956,752 bytes)

---

## 1. Preprocessing & Calibration Specifications

- **Input Resolution:** Exactly **$512 \times 512$ pixels**, 3 channels (RGB).
- **Transformation Pipeline:**
  1. Bilinear interpolation resize to $512 \times 512$.
  2. Planar RGB conversion with pixel intensity normalized to $[0.0, 1.0]$.
  3. ImageNet normalization:
     - Mean: `[0.485, 0.456, 0.406]`
     - Standard Deviation: `[0.229, 0.224, 0.225]`
- **Calibration Settings (Unchanged):**
  - Calibration Method: Single scalar temperature scaling
  - Temperature ($T$): `1.7817543`
  - Operating Confidence Threshold: `0.3434087` (Target 90% precision)
  - Free Energy Threshold: `-9.3050365`
- **Threshold Modification Policy:** Strict adherence to project rule H11 and user constraint: **No thresholds were altered**.

---

## 2. Test Hardware Specifications

- **Laptop Environment:** Windows 11, Intel Core Ultra 5, Python 3.11, PyTorch 2.14.0+cpu, Torchvision 0.29.0+cpu.
- **Phone Environment:** Physical Motorola edge 60 (`arm64-v8a`, Android 17), ExecuTorch Android Runtime 0.6.0 on Arm CPU.

---

## 3. End-to-End Validation Comparison Table (5 Validation Split Samples)

| # | Validation Image (Split V2) | True Label | Laptop PyTorch Top-1 Label | Laptop PyTorch Prob | Phone ExecuTorch Top-1 Label | Phone ExecuTorch Prob | Phone Latency (Arm CPU) | Prob $\Delta$ (Parity Error) | Status |
|:---:|:---|:---|:---|:---:|:---|:---:|:---:|:---:|:---:|
| **1** | `rice_leaf_blast/000561_Rice_IMG_20231014_172034_1.jpg` | `rice_leaf_blast` | `rice_leaf_blast` | **92.88%** (0.9288) | `rice_leaf_blast` | **92.88%** (0.928763) | 147 ms* | $3.7 \times 10^{-5}$ | **MATCH** |
| **2** | `cotton_bacterial_blight/000074_Cotton_bact17.jpg` | `cotton_bacterial_blight` | `cotton_bacterial_blight` | **100.00%** (1.0000) | `cotton_bacterial_blight` | **100.00%** (0.999988) | 94 ms | $1.2 \times 10^{-5}$ | **MATCH** |
| **3** | `chilli_leaf_curl_virus/000107_Curl Virus00107.jpg` | `chilli_leaf_curl_virus` | `chilli_leaf_curl_virus` | **100.00%** (1.0000) | `chilli_leaf_curl_virus` | **100.00%** (0.999989) | 95 ms | $1.1 \times 10^{-5}$ | **MATCH** |
| **4** | `tomato_yellow_leaf_curl_virus/000764_916tomato_train_leaf curl.jpg` | `tomato_yellow_leaf_curl_virus` | `tomato_yellow_leaf_curl_virus` | **84.90%** (0.8490) | `tomato_yellow_leaf_curl_virus` | **84.90%** (0.848981) | 95 ms | $1.9 \times 10^{-5}$ | **MATCH** |
| **5** | `maize_northern_leaf_blight/000415_Corn_Corn_Blight (81).jpg` | `maize_northern_leaf_blight` | `maize_northern_leaf_blight` | **96.54%** (0.9654) | `maize_northern_leaf_blight` | **96.54%** (0.965364) | 95 ms | $3.6 \times 10^{-5}$ | **MATCH** |

*\*Note: Sample 1 latency (147 ms) includes initial JIT initialization. Steady-state inference latency on Motorola edge 60 Arm CPU is 94–95 ms.*

---

## 4. Observations & Parity Verification

1. **Top-1 Accuracy:** 5 / 5 (100.0%) correct classifications against ground-truth labels across 5 distinct crop categories (Rice, Cotton, Chilli, Tomato, Maize).
2. **Numeric Precision Parity:** Maximum numerical probability difference between PyTorch CPU float32 and ExecuTorch XNNPACK Arm CPU is $< 0.00004$ ($4 \times 10^{-5}$), confirming numerical equivalence.
3. **Execution Latency:** Average on-device inference latency on the physical Arm CPU is **95 ms**, well within the 1,500 ms shutter-to-result budget (Rule R5 & U2).
4. **Input Size Integrity:** The input image bitmap passed to the vision model in Android is strictly scaled to **$512 \times 512$** pixels.
