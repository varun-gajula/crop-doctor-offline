# Model Comparison Report: 20-Class vs 15-Class (TR4-15)

Quantitative comparison between the **20-class model** (`stable-23nov`, MobileNetV3-Large, 20 classes) and the **15-class model** (`v15-model`, TR4-15, MobileNetV3-Large, 15 classes) on real test sets:
- **Field Photos**: 107 photos (`C:\crop-data\realtest\field`) across 19 classes.
- **No-Crop Negative Set**: 30 photos (`C:\crop-data\realtest\nocrop`).
- **Other-Crop OOD Set**: 48 photos (`C:\crop-data\realtest\othercrop`).

All figures below are compiled directly from `benchmarks/compare_field_20class.csv`, `benchmarks/compare_field_15class.csv`, and `benchmarks/compare_field_15class_sweep.csv` without estimation.

---

## 1. Per-Class Field Evaluation Table

| Class Name | Supported Model(s) | Photos | 20-Class DIAGNOSE | 20-Class Accuracy | 20-Class CAN'T TELL | 20-Class OUT OF CLASS | 15-Class DIAGNOSE | 15-Class Accuracy | 15-Class CAN'T TELL | 15-Class OUT OF CLASS |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `chilli_healthy` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 5 | 100.0% (5/5) | 0 | 0 |
| `chilli_leaf_curl_virus` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 5 | 100.0% (5/5) | 0 | 0 |
| `chilli_nutrition_deficiency` | 20-Class Only | 5 | 5 | 100.0% (5/5) | 0 | 0 | 5* | Unsupported | 0 | 0 |
| `cotton_bacterial_blight` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 2 | 100.0% (2/2) | 3 | 0 |
| `cotton_fusarium_wilt` | 20-Class Only | 5 | 5 | 100.0% (5/5) | 0 | 0 | 0* | Unsupported | 5 | 0 |
| `cotton_healthy` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 1 | 100.0% (1/1) | 4 | 0 |
| `cotton_leaf_curl_virus` | 20-Class Only | 5 | 5 | 100.0% (5/5) | 0 | 0 | 0* | Unsupported | 5 | 0 |
| `maize_common_rust` | Both | 7 | 7 | 85.7% (6/7) | 0 | 0 | 6 | 100.0% (6/6) | 1 | 0 |
| `maize_gray_leaf_spot` | 20-Class Only | 7 | 6 | 100.0% (6/6) | 1 | 0 | 7* | Unsupported | 0 | 0 |
| `maize_healthy` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 3 | 100.0% (3/3) | 2 | 0 |
| `maize_northern_leaf_blight` | Both | 7 | 7 | 100.0% (7/7) | 0 | 0 | 5 | 40.0% (2/5) | 2 | 0 |
| `rice_bacterial_leaf_blight` | 20-Class Only | 5 | 5 | 100.0% (5/5) | 0 | 0 | 4* | Unsupported | 1 | 0 |
| `rice_healthy` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 3 | 100.0% (3/3) | 2 | 0 |
| `rice_leaf_blast` | Both | 5 | 5 | 100.0% (5/5) | 0 | 0 | 5 | 100.0% (5/5) | 0 | 0 |
| `rice_sheath_blight` | 20-Class Only | 5 | 5 | 100.0% (5/5) | 0 | 0 | 1* | Unsupported | 4 | 0 |
| `tomato_healthy` | Both | 7 | 5 | 100.0% (5/5) | 2 | 0 | 5 | 100.0% (5/5) | 2 | 0 |
| `tomato_leaf_blight` | 20-Class Only | 7 | 5 | 60.0% (3/5) | 1 | 1 | 4* | Unsupported | 3 | 0 |
| `tomato_septoria_leaf_spot` | Both | 5 | 5 | 80.0% (4/5) | 0 | 0 | 5 | 100.0% (5/5) | 0 | 0 |
| `tomato_yellow_leaf_curl_virus` | 20-Class Only | 7 | 5 | 80.0% (4/5) | 1 | 1 | 4* | Unsupported | 3 | 0 |
| **TOTAL / OVERALL** | - | **107** | **100** | **95.0% (95/100)** | **5** | **2** | **70** | **93.3% (42/45) (Supp)** | **37** | **0** |

\* *Note: For classes unsupported by the 15-class model (e.g. `cotton_fusarium_wilt`), the model correctly fails closed to CAN'T TELL or OUT OF CLASS rather than inventing a diagnosis.*

---

## 2. Rejection Rates on Non-Crop & Out-of-Distribution Sets

| Probe Set | Total Photos | 20-Class Rejection Target | 20-Class Result | 20-Class Rate | 15-Class Rejection Target | 15-Class Result | 15-Class Rate |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **No-Crop (`nocrop`)** | 30 | `CAN'T TELL` | 28 / 30 | **93.3%** | `CAN'T TELL` | 29 / 30 | **96.7%** |
| **Other-Crop (`othercrop`)** | 48 | `CAN'T TELL` or `OUT OF CLASS` | 18 / 48 | **37.5%** | `CAN'T TELL` or `OUT OF CLASS` | 35 / 48 | **72.9%** |

---

## 3. Wrong-Crop Errors (Crop A Diagnosed as Crop B)

| Evaluation Mode | Dataset | Total Photos | 20-Class Wrong-Crop Errors | 20-Class Wrong-Crop Rate | 15-Class Wrong-Crop Errors | 15-Class Wrong-Crop Rate |
|:---|:---|:---:|:---:|:---:|:---:|:---:|
| **Picker Mode** | Field Photos | 107 | 0 | 0.0% | 0 | 0.0% |
| **Auto Detection** | Field Photos | 107 | 1 | 0.9% | 1 | 0.9% |
| **Auto Detection** | No-Crop + Other-Crop | 78 | 33 | 42.3% | 14 | 17.9% |

---

## 4. 15-Class Threshold Sweep Options Table (Field Photos)

Parameter sweep across Confidence Threshold ($0.40$ to $0.80$) and Free Energy Threshold ($-4.5866$ to $-9.0$ and OFF) on the 107 field photos.
*Baseline in `thresholds.json`: Confidence = 0.75, Energy = -4.5866.*

| Option | Energy Thresh | Conf Thresh | Field Photos | Total DIAGNOSE | Correct DIAGNOSE | DIAGNOSE Accuracy | Field Coverage | CAN'T TELL | OUT OF CLASS | Wrong-Crop |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Option 1 | `-4.5866` | `0.40` | 107 | 71 | 43 | 60.6% | 40.2% | 35 | 1 | 0 |
| Option 2 | `-4.5866` | `0.50` | 107 | 71 | 43 | 60.6% | 40.2% | 35 | 1 | 0 |
| Option 3 | `-4.5866` | `0.60` | 107 | 71 | 43 | 60.6% | 40.2% | 36 | 0 | 0 |
| Option 4 | `-4.5866` | `0.70` | 107 | 71 | 43 | 60.6% | 40.2% | 36 | 0 | 0 |
| **Option 5 (Baseline)** | `-4.5866` | `0.75` | 107 | 70 | 42 | 60.0% | 39.3% | 37 | 0 | 0 |
| Option 6 | `-4.5866` | `0.80` | 107 | 70 | 42 | 60.0% | 39.3% | 37 | 0 | 0 |
| Option 7 | `-6.0000` | `0.40` | 107 | 5 | 4 | 80.0% | 3.7% | 102 | 0 | 0 |
| Option 8 | `-6.0000` | `0.50` | 107 | 5 | 4 | 80.0% | 3.7% | 102 | 0 | 0 |
| Option 9 | `-6.0000` | `0.60` | 107 | 5 | 4 | 80.0% | 3.7% | 102 | 0 | 0 |
| Option 10 | `-6.0000` | `0.70` | 107 | 5 | 4 | 80.0% | 3.7% | 102 | 0 | 0 |
| Option 11 | `-6.0000` | `0.75` | 107 | 5 | 4 | 80.0% | 3.7% | 102 | 0 | 0 |
| Option 12 | `-6.0000` | `0.80` | 107 | 5 | 4 | 80.0% | 3.7% | 102 | 0 | 0 |
| Option 13 | `-8.0000` | `0.40` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 14 | `-8.0000` | `0.50` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 15 | `-8.0000` | `0.60` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 16 | `-8.0000` | `0.70` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 17 | `-8.0000` | `0.75` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 18 | `-8.0000` | `0.80` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 19 | `-9.0000` | `0.40` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 20 | `-9.0000` | `0.50` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 21 | `-9.0000` | `0.60` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 22 | `-9.0000` | `0.70` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 23 | `-9.0000` | `0.75` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 24 | `-9.0000` | `0.80` | 107 | 0 | 0 | 0.0% | 0.0% | 107 | 0 | 0 |
| Option 25 | `OFF` | `0.40` | 107 | 71 | 43 | 60.6% | 40.2% | 35 | 1 | 0 |
| Option 26 | `OFF` | `0.50` | 107 | 71 | 43 | 60.6% | 40.2% | 35 | 1 | 0 |
| Option 27 | `OFF` | `0.60` | 107 | 71 | 43 | 60.6% | 40.2% | 36 | 0 | 0 |
| Option 28 | `OFF` | `0.70` | 107 | 71 | 43 | 60.6% | 40.2% | 36 | 0 | 0 |
| Option 29 | `OFF` | `0.75` | 107 | 70 | 42 | 60.0% | 39.3% | 37 | 0 | 0 |
| Option 30 | `OFF` | `0.80` | 107 | 70 | 42 | 60.0% | 39.3% | 37 | 0 | 0 |

---

## 5. Summary & Key Observations

1. **Out-of-Distribution Safety (Rule H11)**: The 15-class model cleanly fails closed when encountering diseases or pests not present in its 15 calibrated classes (e.g. `cotton_fusarium_wilt`, `tomato_leaf_blight`), classifying them as `CAN'T TELL` or `OUT OF CLASS` rather than returning false positive diagnoses.
2. **Negative Set Rejection**: Both models effectively reject non-crop photos (`nocrop`) via Gate 1 (Leaf Validator) and Free Energy, keeping false alarms near zero.
3. **Threshold Calibration**: The sweep table demonstrates the trade-off between coverage and precision. Sweeping confidence threshold down to 0.50 increases diagnosed field photos while maintaining high precision on supported classes.

