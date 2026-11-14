# Real-Test Vision Model Evaluation Summary

- **Evaluated Images**: 5
- **Backend**: dummy
- **Energy Threshold**: -1.5
- **Default Confidence Threshold**: 0.4
- **Temperature (T)**: 1.15

## 1. Per-Crop Evaluation

| Crop | Total Photos | Accepted | Correct Crop Match | Crop Accuracy (%) |
| :--- | :--- | :--- | :--- | :--- |
| Chilli | 1 | 1 | 1 | 100.0% |
| Cotton | 1 | 1 | 1 | 100.0% |
| Rice | 1 | 1 | 1 | 100.0% |
| Tomato | 1 | 1 | 1 | 100.0% |

## 2. Predicted Class Distribution

| Predicted Class | Count |
| :--- | :--- |
| `rice_bacterial_leaf_blight` | 1 |
| `cotton_bacterial_blight` | 1 |
| `chilli_bacterial_spot` | 1 |
| `tomato_early_blight` | 1 |

## 3. Confusion Pairs (True Crop -> Predicted Class)

| True Crop | Predicted Class | Count |
| :--- | :--- | :--- |
| rice | `rice_bacterial_leaf_blight` | 1 |
| cotton | `cotton_bacterial_blight` | 1 |
| chilli | `chilli_bacterial_spot` | 1 |
| tomato | `tomato_early_blight` | 1 |

## 4. Non-Crop (OOD) Rejection Analysis

- **Total Non-Crop (nocrop) Photos**: 1
- **Correctly Rejected (True Negatives)**: 1 (100.0%)
- **Wrongly Accepted (False Positives)**: 0
- **Rejection Criterion (>= 8/10 or >= 80%)**: **PASS**
