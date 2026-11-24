# A/B/C Field Test: Agri Care AI (15-Class Opt-8) vs Opt-6 vs Old 20-Class

This document is for recording real-world field evaluations conducted side by side on connected physical devices:
- **Phone 1**: POCO M4 5G (`sseqjnucrkeeprtw`)
- **Phone 2**: Motorola Edge 60 (`ZA222YGPNG`)

---

## The Three Installed Apps

1. **APK-1 (15-Class, Opt-8 - Main)**:
   - **Package**: `com.cropdoctor`
   - **Launcher Label**: `Agri Care AI`
   - **Model**: TR4-15 MobileNetV3-Large (`15 classes`, `vision.pte` SHA-256: `6626b1...`)
   - **Thresholds (Opt-8)**: `conf=0.70`, `energy=-4.50`, `crop_mass=0.60`, `margin=0.20`
   - **Target Profile**: Highest field diagnostic yield (20/33 clean field photos, 85.0% accuracy, 83.3% nocrop rejection, 72.9% othercrop rejection).

2. **APK-2 (15-Class, Opt-6 - Balanced Guard)**:
   - **Package**: `com.cropdoctor.opt6`
   - **Launcher Label**: `Agri Care AI (Opt-6)`
   - **Model**: TR4-15 MobileNetV3-Large (`15 classes`, identical model weight file)
   - **Thresholds (Opt-6)**: `conf=0.65`, `energy=-4.75`, `crop_mass=0.55`, `margin=0.20`
   - **Target Profile**: Balanced OOD guard (77.1% othercrop rejection, 86.7% nocrop rejection, only 19.4% false diagnoses on off-target diseases).

3. **APK-3 (Old 20-Class - Baseline Test)**:
   - **Package**: `com.cropdoctor.old20`
   - **Launcher Label**: `Old 20-class (test)`
   - **Model**: 20-class MobileNetV3-Large (built from tag `stable-23nov`)
   - **Thresholds**: Baseline 20-class thresholds (`temperature=1.78`, `conf=0.34`, `energy=-8.0`).

---

## Instructions for Field Testing

1. All 3 apps are installed side-by-side on your phone launchers on both phones:
   - **`Agri Care AI`** (`com.cropdoctor`)
   - **`Agri Care AI (Opt-6)`** (`com.cropdoctor.opt6`)
   - **`Old 20-class (test)`** (`com.cropdoctor.old20`)
2. Turn **Airplane Mode ON ✈️**.
3. For each leaf:
   - Scan with **`Agri Care AI` (Opt-8)**: Record decision (`DIAGNOSE` / `CAN'T TELL` / `OUT OF CLASS`) and predicted top-1 class.
   - Scan with **`Agri Care AI (Opt-6)`**: Record decision and predicted top-1 class.
   - Scan with **`Old 20-class (test)`**: Record decision and predicted top-1 class.
   - Note which ones were correct based on your ground-truth observation.
4. **Note on Llama / Vosk**:
   - Llama and Vosk are **not required** for this vision test. All vision models (`vision.pte`) run 100% locally from bundled APK assets and display the vision classification (class, severity, certainty) immediately.

---

## Field Test Results Table

| Leaf # | True Crop | True Problem (My Entry) | Opt-8 Decision & Top-1 | Opt-8 Correct? | Opt-6 Decision & Top-1 | Opt-6 Correct? | Old 20 Decision & Top-1 | Old 20 Correct? | Field Condition / Note |
|:---:|:---|:---|:---|:---:|:---|:---:|:---|:---:|:---|
| 1 | | | | | | | | | |
| 2 | | | | | | | | | |
| 3 | | | | | | | | | |
| 4 | | | | | | | | | |
| 5 | | | | | | | | | |
| 6 | | | | | | | | | |
| 7 | | | | | | | | | |
| 8 | | | | | | | | | |
| 9 | | | | | | | | | |
| 10 | | | | | | | | | |
| 11 | | | | | | | | | |
| 12 | | | | | | | | | |
| 13 | | | | | | | | | |
| 14 | | | | | | | | | |
| 15 | | | | | | | | | |
| 16 | | | | | | | | | |
| 17 | | | | | | | | | |
| 18 | | | | | | | | | |
| 19 | | | | | | | | | |
| 20 | | | | | | | | | |
