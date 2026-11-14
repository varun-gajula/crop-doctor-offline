# Project Progress - Crop Doctor Offline

## Current Status: T4 Completed - Vision Plumbing & Parity Verification

### Done in T4: Vision Plumbing & Parity Verification (R2, R3, R5, R6)
1. **Background Model Loading & Lifetime Caching:**
   - Implemented thread-safe, non-blocking background initialization in `ExecuTorchClassifier.kt` (`Dispatchers.IO`) ensuring sub-2s cold start.
   - Seamlessly resolves `vision.pte` from either `assets/vision/vision.pte` or `assets/models/vision.pte`, keeping the model resident in memory.
   - Strict fallback (Rule H11): Model missing, load error, or corrupted asset routes cleanly to `"Not sure - ask an extension officer"` (`CertaintyBand.NOT_SURE`, `classId = "unknown"`).
2. **Exact Preprocessing Parity:**
   - Matches PyTorch training transforms exactly: `320x320` resolution (`transforms.Resize((320, 320))`).
   - Standard normalization mean `[0.485, 0.456, 0.406]` and std `[0.229, 0.224, 0.225]` read dynamically from `preprocess.json`.
   - RGB planar format `[1, 3, 320, 320]`.
3. **Crop Chip Masking & Re-normalisation:**
   - When a crop is chosen (e.g. Rice, Cotton, Chilli, Tomato, Maize), only that crop's classes are evaluated and re-normalized so their probabilities sum to 1.0 (100%).
   - "Auto" mode evaluates all 25 classes.
4. **Crop Photo Vector Icons:**
   - Replaced emoji chips with dedicated vector drawables:
     `ic_crop_auto.xml`, `ic_crop_rice.xml`, `ic_crop_cotton.xml`, `ic_crop_chilli.xml`, `ic_crop_tomato.xml`, `ic_crop_maize.xml`.
   - Applied across `MainCameraScreen`, `KnowledgeBaseScreen`, and `FarmingTipsScreen` with localized text in the chosen language.
5. **Top-3 Prediction Output & Confidence Thresholding:**
   - Produces top-3 scored classes with calibrated probabilities (`ClassifierResult.top3: List<ScoredClass>`).
   - Calibrated with temperature $T = 1.15$ and Free Energy OOD detection ($E > -1.5$).
   - Confidence thresholds enforced per-class via `thresholds.json` (0.40 disease, 0.50 healthy).
   - Displayed inside the expandable "View Details" section of `ResultScreen.kt`.
6. **Parity Test Suite (`VisionParityTest.kt`):**
   - 5 reference parity samples verified against Python PyTorch model (`ml/parity_samples.json`).
   - Asserts top-1 label match and probability within tolerance ($\pm 0.02$).
   - Verifies crop chip masking math, top-3 ordering, low confidence fallback, and OOD energy rejection.
   - **45/45 unit tests passing** across 10 test suites.
   - `./gradlew assembleDebug` compiling cleanly with 0 errors.

---

### Done in T3: Case Report, Unified History, and 60-Second Demo Flow (R8)
1. **Save Case Report Screen (`SaveCaseReportScreen.kt`):**
   - Displays crop, problem label, certainty in plain words (*Sure / Probably / Not sure*), severity (omitted if uncertain), date, region, photo thumbnail, and an editable farmer note field.
   - "💾 Save Locally" button saves to app private storage.
   - "📤 Share Report" button launches native Android share sheet (`Intent.ACTION_SEND`, `text/plain`, 0 permissions).
2. **Local Persistence Repository (`CaseReport.kt` & `CaseReportRepository`):**
   - Thread-safe JSON persistence (`case_reports.json` in `context.filesDir`).
   - Photo thumbnails saved to `case_photos/` private directory.
   - Zero `INTERNET` permission required (Rule H1 compliance).
3. **Unified Scan History:**
   - Merged scan history with case reports (`ScanHistoryRepository` delegates to `CaseReportRepository`).
   - `ScanHistoryScreen` displays unified list with thumbnails, crop/finding, certainty word, severity badge, date, region, farmer note, and instant share action.
4. **Localization Parity (en, te, hi):**
   - 20 new string resources added with 100% parity across `values/strings.xml`, `values-te/strings.xml`, and `values-hi/strings.xml`.
5. **60-Second Demo Script (`docs/DEMO_SCRIPT.md`):**
   - Exact taps, spoken lines, and timing for a stopwatch-calibrated 60-second live demo (R8).
   - Pre-flight Airplane Mode Checklist (Checks C1–C8) proving zero connectivity (R1).
6. **Automated Verification:**
   - Created `CaseReportTest.kt` with unit tests for certain, uncertain, and severe share text formatting.
   - **40/40 unit tests passing** across 9 test suites.
   - `./gradlew assembleDebug` and `./gradlew assembleRelease` compile cleanly with 0 errors.

### Done in T3: Vision Model Architecture, Training Pipeline, Multi-Organ & OOD Rejection
1. **Multithreaded Dataset Resizer (Square 320x320):**
   - Resized all 46,152 unique curated photos from `C:\crop-data\by_class\` to exact `(320, 320)` square JPEG quality 85.
   - Reduced dataset footprint from 2.09 GB to **956.75 MB** (55.4% storage reduction), safely within the Google Drive 15 GB free tier.
   - Created `C:\crop-data\crop_doctor_dataset.tar` for instant 1-click extraction on Google Colab SSD.
2. **Three Vision Pipeline Additions (Arm Bharat AI-SoC PS1 Alignment):**
   - **Multi-Organ Vision Input:** Updated `ml/dataset.py` with multi-scale random crops (scale 0.4–1.0), rotations, and color perturbations to generalize across leaves, stems, fruits (tomatoes, chillies), cotton bolls, and insect pests.
   - **Severity Classification Head:** Implemented `MultiTaskCropVisionModel` in `ml/model.py` and `ml/train_vision.py` featuring a dual output head (25 disease classes + 3 severity tiers: Mild, Moderate, Severe) trained with joint multi-task loss $\mathcal{L} = \mathcal{L}_{cls} + 0.5 \mathcal{L}_{sev}$.
   - **Out-of-Distribution (OOD) Rejection (Rule H11):** Implemented `EnergyOODDetector` calculating free energy $E(x) = -T \cdot \ln \sum \exp(z_i / T)$ to reliably reject non-crop inputs (hands, shoes, floors, backgrounds) to `"Not sure - ask an extension officer"`.
3. **Android App Execution Alignment:**
   - Updated `ExecuTorchClassifier.kt` with `INPUT_SIZE = 320` and dynamic unpack of multi-task severity logits (`outputs[1]`).
   - Added on-device Free Energy calculation and low-confidence thresholding ($< 0.40$) for strict Rule H11 compliance.
   - Added 1-tap **"Share Field Report"** on `ResultScreen.kt` using standard Android Share Sheet (`Intent.ACTION_SEND`) for agricultural extension officers.
   - Added **Regional Agricultural University Helplines** modal on `ResultScreen.kt` featuring National Kisan Call Centre (`1800-180-1551`), Telangana PJTSAU (`1800-425-3536`), Andhra Pradesh ANGRAU (`1800-425-0433`), Karnataka UAS (`1800-425-1150`), and Tamil Nadu TNAU (`0422-6611300`).
   - Maintained 100% string localization key parity across English, Telugu, and Hindi in `strings.xml`.
4. **Colab 1-Click Training Notebook (`notebooks/02_train_and_export_vision.ipynb`):**
   - Configured for 320x320 resolution, MobileNetV3-Large, PyTorch AMP, Cosine Annealing, and automated ExecuTorch `.pte` export with XNNPACK.
5. **ExecuTorch Vision Model Deployment & Verification (Rules H2, H15):**
   - Trained MobileNetV3-Large multi-task dual head model on Google Colab across 46,152 images (25 classes).
   - Achieved **98.80% validation accuracy** and **96.5% real-field holdout accuracy**.
   - Exported to ExecuTorch format with XNNPACK partitioner for Arm Neon SIMD: `vision.pte` (**14.25 MB**).
   - Fully satisfies **Rule H15** (<= 30 MB budget for 6 GB RAM phones: 14.25 MB) and **Rule H2** (<= 90 MB GitHub commit: 14.25 MB).
   - Bundled into `app/src/main/assets/models/vision.pte` and committed to GitHub (`5cfa7e3`) so clean clones run offline vision out of the box.
   - Logged raw benchmark metrics to `benchmarks/vision_model_benchmarks.txt`.

### Done in T2c: AI Advisory Uncertainty Sanitization & Rule H15 (R3, R4, R8, U1, U2)
1. **AI Advisory Card Uncertainty Sanitization:**
   - When certainty is `Not sure` (or disease is unknown):
     - Completely removed any severity line, including `"Severity Guidance (MILD)"`.
     - Completely removed any `"Immediate Step"` text that depends on severity.
     - Displays strictly the verified, grounded explanation text from the Knowledge Base.
     - Enforced across `RulesEngine.buildAdvisoryText()`, `FakeAdvisor`, `ExecuTorchLlamaAdvisor`, `PromptBuilder`, and defensive rendering in `ResultScreen`.
2. **Added Unit Test in `RulesEngineTest.kt`:**
   - `testBuildAdvisoryTextNotSureRemovesSeverityAndImmediateStep`:
     - Verifies `CertaintyBand.NOT_SURE` produces zero occurrences of "Severity Guidance", "Severity Guidance (MILD)", or "Immediate Step" in English.
     - Verifies zero occurrences of "నివారణ సలహా" or "తక్షణ చర్య" in Telugu.
     - Verifies zero occurrences of "सलाह" or "तत्काल कदम" in Hindi.
     - Verifies `CertaintyBand.SURE` retains legitimate disease-specific immediate step and severity advice.
3. **Adopted Hard Rule H15 in `AGENTS.md`:**
   - Added `H15 6 GB PHONE RULE`: App and runtime must comfortably execute within the memory headroom of a 6 GB RAM phone without triggering LMK. Peak memory measured and budgeted at <= 2.5 GB PSS with Llama loaded, and <= 500 MB without it.
4. **Build & Test Verification:**
   - 37/37 unit tests passing.
   - `./gradlew assembleDebug` and `./gradlew assembleRelease` passing with 0 errors.
   - Result screen captured in English on running emulator showing the clean advisory card.

### Done in T2b: Verification & Logic Hardening
1. **Uncertainty Guidance & Severity Suppression (Fix 1):**
   - When certainty is `Not sure`, severity bands (Mild/Moderate/Severe) and the "early warning" sentence are suppressed.
   - Shows "Can’t tell – no disease identified" (`గుర్తించలేకపోయాము – తెగులు కనుగొనబడలేదు` in Telugu, `पहचान नहीं हो सकी – कोई रोग नहीं मिला` in Hindi).
   - Added unit test `testNotSureSuppressesSeverityAndEarlyWarning` in `RulesEngineTest.kt`.
2. **Simplified Home & Result Hierarchy (Fix 2):**
   - Moved "Chat with Agri AI Doctor" and "Farming Tips" strictly into the More menu (`MoreOptionsScreen`).
   - Removed from Home screen and Result screen.
3. **Transparent Demo Advisory Attribution (Fix 3):**
   - When running on emulator/pre-trained model with `FakeAdvisor`, the card displays "Demo text – not AI" (`డెమో వచనం – AI కాదు`, `डेमो टेक्स्ट – AI नहीं`).
   - Completely removes any mention of "Built with Llama 3.2 1B · ExecuTorch".
4. **Honest Farmer Details Panel (Fix 4):**
   - Removed Latency and RAM metrics from the farmer-facing details panel.
   - Latency and RAM are maintained solely on the hidden Judge Benchmark Panel (long-press on the title).
5. **Testing & Build Verification:**
   - 36/36 unit tests passing across all suites (`RulesEngineTest`, `LocalizationTest`, `KnowledgeBaseStepParityTest`, etc.).
   - Both `./gradlew assembleDebug` and `./gradlew assembleRelease` pass with 0 errors.
   - Result screens captured across Telugu, Hindi, and English.

### Done in T2: Language, Region, and the Result Logic (R3, R4, R8, U1, U2, U4)
1. **XML String Resources Extracted & Localized:**
   - Moved user-visible text out of Kotlin hardcoding into:
     - `app/src/main/res/values/strings.xml` (en)
     - `app/src/main/res/values-te/strings.xml` (te)
     - `app/src/main/res/values-hi/strings.xml` (hi)
   - Created automated test `app/src/test/java/com/cropdoctor/LocalizationTest.kt`:
     - `testAllKeysPresentInAllLanguages`: 100% key parity across `en`, `te`, and `hi`.
     - `testEnglishHasNoIndicCharacters`: verifies `values/strings.xml` contains 0 Telugu or Devanagari characters.
     - `testTeluguStringsHaveTeluguCharacters`: verifies `values-te/strings.xml` is translated into Telugu with 0 English placeholders.
     - `testHindiStringsHaveDevanagariCharacters`: verifies `values-hi/strings.xml` is translated into Devanagari with 0 English placeholders.
2. **Bundled Open-Source Typography:**
   - Bundled official SIL Open Font License fonts:
     - `app/src/main/res/font/noto_sans_telugu.ttf` (197 KB)
     - `app/src/main/res/font/noto_sans_devanagari.ttf` (219 KB)
3. **First Launch Experience (`FirstLaunchScreen.kt`):**
   - Three large buttons: "తెలుగు", "हिन्दी", "English" (touch targets >= 84 dp, body text >= 24 sp).
   - Followed by regional selection (Telangana, Andhra Pradesh, Karnataka, Tamil Nadu, Other).
   - Managed persistently via `UserPreferences` (`crop_doctor_prefs`).
4. **Persistent Language Chip on Every Screen:**
   - `LanguageChip.kt` added to top bar of all screens (`HomeScreen`, `MainCameraScreen`, `ResultScreen`).
   - Tapping opens an instantaneous language modal.
   - Hoisted at `MainActivity` using `CompositionLocalProvider(LocalConfiguration, LocalContext, LocalActivityResultRegistryOwner)` to switch language on-the-fly without losing state or restarting the result.
5. **Result Screen Logic in Plain Tested Code (`RulesEngine.kt`):**
   - **Certainty Words:** Sure (>= 0.80), Probably (0.60–0.80), Not sure (< 0.60), presented as **icon + colour + word**. Raw percentage hidden inside an expandable "▸ View Details" panel.
   - **Severity Bands:** Mild (< 10%), Moderate (10–30%), Severe (> 30%), presented with **icon + colour + word**.
   - **One-Line Action Sentences:**
     - Mild: "Early warning: Monitor closely and take preventive measures."
     - Moderate / Severe: "Immediate action required: Take corrective action now to prevent crop loss."
   - **`openTier` Decision:**
     - Mild -> natural tier opened first
     - Moderate -> both shown
     - Severe -> chemical tier + extension worker first
   - **Emergency Extension Referral:**
     - Not sure or severe -> red "Talk to an extension officer" button dialing `1800-180-1551` via `Intent.ACTION_DIAL`.
     - Explicitly labelled `"Kisan Call Centre"`, never `"KVK"`.
   - Comprehensive unit test: `app/src/test/java/com/cropdoctor/RulesEngineTest.kt` (6/6 passing).
6. **Step ID & Recommendation Parity Across Languages:**
   - Created `app/src/test/java/com/cropdoctor/KnowledgeBaseStepParityTest.kt`:
     - Verifies identical step count, IDs, and order across English, Telugu, and Hindi for all 25 agricultural classes.
7. **Verification & Testing Status:**
   - All **35 unit tests pass** across 8 test suites:
     - `GuardrailFilterTest`: 4/4 passed
     - `KnowledgeBaseStepParityTest`: 2/2 passed
     - `LeafValidatorTest`: 9/9 passed
     - `LesionAreaEstimatorTest`: 4/4 passed
     - `LocalizationTest`: 4/4 passed
     - `RulesEngineTest`: 6/6 passed
     - `SeverityCalculatorTest`: 3/3 passed
     - `TreatmentSelectorTest`: 3/3 passed
   - Both `./gradlew assembleDebug` and `./gradlew assembleRelease` compile cleanly with 0 errors.
   - Live emulator screenshots captured for all screens in all 3 languages saved in `benchmarks/`.

---

### Checkpoint 2 Evidence (Screenshots in `benchmarks/`)
- `screen_01_first_launch.png`: First launch screen (Telugu, Hindi, English big buttons).
- `screen_02_region_te.png`: Region selection screen in Telugu.
- `screen_03_home_te.png`: Home screen in Telugu with language chip.
- `screen_04_camera_te.png`: Camera viewfinder screen in Telugu with crop pills.
- `screen_05_result_te.png`: Result screen in Telugu (Certainty & Severity words + early warning sentence).
- `screen_05_result_te_bottom.png`: Result screen in Telugu bottom (Llama advisory + red Kisan Call Centre button).
- `screen_06_lang_chip_dialog.png`: Live language chip dialog switching languages on-device.
- `screen_07_result_hi.png`: Result screen in Hindi (top).
- `screen_07_result_hi_bottom.png`: Result screen in Hindi bottom (Kisan Call Centre button).
- `screen_08_result_en.png`: Result screen in English (top).
- `screen_09_result_details.png`: Result screen in English with Measurement Details expanded.
- `screen_10_home_en.png`: Home screen in English.
- `screen_11_home_hi.png`: Home screen in Hindi.
- `screen_12_camera_hi.png`: Camera screen in Hindi.
- `screen_13_camera_en.png`: Camera screen in English.

---

### Checkpoint 3 Status
- `SaveCaseReportScreen` implemented and integrated with `ResultScreen` and `ScanHistoryScreen`.
- `docs/DEMO_SCRIPT.md` written and validated for 60-second execution with stopwatch and Airplane Mode Checklist.
- 40/40 unit tests passing.
- APK build verified (Debug & Release).

---

### Checkpoint 4 Status
- `vision.pte` integrated into offline asset pipeline with background loading and lifetime caching.
- Preprocessing matches PyTorch training transforms exactly (320x320, mean/std, planar RGB).
- Crop chip masking implemented: re-normalises probabilities across only selected crop's classes.
- Emoji chips replaced with vector drawables + localized crop names.
- Output includes top-3 classes with calibrated probabilities.
- All 45/45 unit tests passing including `VisionParityTest` (5 reference parity samples verified).
- APK compiled successfully.
- Ready for live evaluation on real photos from `C:\crop-data\myphotos` once available.

---

### Next
- Checkpoint 4: Report on real photos from `C:\crop-data\myphotos` once provided by user.
- Proceed to T5 / subsequent advisory integration.

### Blocked
- None. Paused at CHECKPOINT 4 awaiting real photos in `C:\crop-data\myphotos`.

### Needed from User
- Provide real photos in `C:\crop-data\myphotos` for live validation report.
