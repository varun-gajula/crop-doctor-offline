# A/B Field Test: Agri Care AI (15-Class) vs Old 20-Class

This document is for recording real-world field evaluations conducted side by side on the physical POCO M4 5G between:
1. **APK-15 (`Agri Care AI`)**: Package `com.cropdoctor` (TR4-15 MobileNetV3-Large model, calibrated thresholds, Task LAT-1/LAT-2 prompt and runtime optimizations).
2. **APK-20 (`Old 20-class (test)`)**: Package `com.cropdoctor.old20` (Built from tag `stable-23nov`, 20-class MobileNetV3-Large model).

---

## Instructions for Field Testing

1. Both apps are installed side-by-side on your POCO phone launcher:
   - **`Agri Care AI`** (APK-15) -> 15-class TR4 model (`com.cropdoctor`).
   - **`Old 20-class (test)`** (APK-20) -> 20-class model (`com.cropdoctor.old20`).
2. Turn **Airplane Mode ON ✈️**.
3. For each leaf:
   - Scan the leaf using **`Agri Care AI` (APK-15)**: Record the decision (`DIAGNOSE` / `CAN'T TELL` / `OUT OF CLASS`) and the predicted top-1 class.
   - Scan the same leaf using **`Old 20-class (test)` (APK-20)**: Record the decision and the predicted top-1 class.
   - Fill in whether each app was correct based on your ground-truth observation.
4. **Note on Llama / Vosk**:
   - Llama and Vosk are **not required** for this vision test. Both vision models (`vision.pte`) run 100% locally from bundled APK assets and display the vision classification (class, severity, certainty) immediately.

---

## Field Test Results Table

| Leaf # | True Crop | True Problem (My Entry) | APK-15 Decision & Top-1 | APK-15 Correct? (Yes/No) | APK-20 Decision & Top-1 | APK-20 Correct? (Yes/No) | Note / Field Condition |
|:---:|:---|:---|:---|:---:|:---|:---:|:---|
| 1 | | | | | | | |
| 2 | | | | | | | |
| 3 | | | | | | | |
| 4 | | | | | | | |
| 5 | | | | | | | |
| 6 | | | | | | | |
| 7 | | | | | | | |
| 8 | | | | | | | |
| 9 | | | | | | | |
| 10 | | | | | | | |
| 11 | | | | | | | |
| 12 | | | | | | | |
| 13 | | | | | | | |
| 14 | | | | | | | |
| 15 | | | | | | | |
| 16 | | | | | | | |
| 17 | | | | | | | |
| 18 | | | | | | | |
| 19 | | | | | | | |
| 20 | | | | | | | |
