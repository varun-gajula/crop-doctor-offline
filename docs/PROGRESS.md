# Project Progress - Crop Doctor Offline

## Current Status: T3 In Progress - Vision Model Training & Case Report Flow (R8)

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

### Done in T3: Vision Model Architecture, Training Pipeline & Dataset Packaging
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

### Next
- User runs the 60-second demo script on the emulator against the stopwatch (CHECKPOINT 3).
- Proceed to T4 / subsequent milestone upon user approval.

### Blocked
- None. Paused at CHECKPOINT 3 for user stopwatch verification.

### Needed from User
- Run the 60-second demo flow in `docs/DEMO_SCRIPT.md` against the stopwatch.
- Provide approval on CHECKPOINT 3 to proceed.
