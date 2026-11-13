# Project Progress - Crop Doctor Offline

## Current Status: T2 & T2b Completed - Ready for T3

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

### Next
- Proceed to T3 (Vision model training/export or Advisory pipeline integration as requested).

### Blocked
- None. Paused at CHECKPOINT 2 for user review.

### Needed from User
- Review CHECKPOINT 2 results (screenshots, test suite pass count, and git status).
- Provide approval to proceed to the next milestone.
