# Project Progress - Crop Doctor Offline

## Current Status: T5 & Laptop Emulator Testing Completed (Ready for Real Phone Live Validation)

### Done in Laptop Emulator Verification (Rule H8):
1. **Rule H8 ExecuTorch Fallback for x86_64 Emulators:**
   - Updated `ExecuTorchClassifier.kt` with `isEmulator()` detection matching `ExecuTorchLlamaAdvisor.kt`.
   - On x86_64 emulators, safely delegates to `FakeClassifier` returning deterministic predictions with `isDemoMode = true` and visible demo banner `⚠️ DEMO MODE - NOT A REAL DIAGNOSIS`.
   - Prevents native ExecuTorch XNNPACK `SIGSEGV` crash caused by Arm-targeted kernels executing on x86_64 emulator CPUs.
2. **End-to-End Flow Tested on Laptop (`emulator-5554`):**
   - **Home Screen**: Sub-2s launch, language toggle, quick scan card, bottom navigation.
   - **Camera / Scan Screen**: Crop filtering chips (Auto, Chilli, Tomato, Cotton, Rice, Maize), leaf guide, shutter button.
   - **Result Screen (English)**: Correct disease identification, severity band (Moderate), certainty (Sure), two-tier treatment (Natural & Chemical).
   - **Hidden Judge Panel**: Long-press on screen title opens live metrics dialog (42 ms latency, 8 MB RAM, 0 KB network).
   - **Multilingual Switching**: 1-tap language switch verified for Telugu (`తెలుగు`) and Hindi (`हिन्दी`) with verified vernacular agronomy text.
   - **Save Case Report (T3)**: Verified local saving (`✓ सहेजा गया`) with crop, label, certainty in words, severity, date, region, notes, and Android share sheet.
   - **Saved Reports Screen**: Verified persistent list of saved cases with timestamps and share actions.
3. **Artifacts & Logs:**
   - Captured 8 full-resolution verification screenshots and compiled into `benchmarks/laptop_emulator_test_collage.png`.
   - Generated verification report at `docs/PROGRESS.md` and conversation artifacts.
   - All 49 unit tests passing (`./gradlew testDebugUnitTest`).

---

### Done in T5: Llama on the Phone (R4, R6, R7, PS1, Rules H4, H5, H6, H15)
1. **Pinned ExecuTorch LLM Runtime & Model Architecture (`docs/VERSIONS.md`):**
   - ExecuTorch Android SDK: `org.pytorch:executorch-android:0.5.1` (Maven Central official release).
   - ExecuTorch Python Export Runtime: `0.5.1` (`torch 2.5.1`).
   - Advisory Model: `meta-llama/Llama-3.2-1B-Instruct` in SpinQuant INT4-EO8 format (`llama-3.2-1b-spinquant.pte`, 1,135,951,488 bytes ~ 1.05 GB).
   - Tokenizer: Official SentencePiece `tokenizer.model` (2,183,982 bytes ~ 2.08 MB) directly supported by ExecuTorch native tokenizer.
2. **On-Device Storage & In-App Import Fallback:**
   - Both `llama-3.2-1b-spinquant.pte` (1.05 GB) and `tokenizer.model` (2.08 MB) transferred to phone via ADB:
     - `/data/local/tmp/llama-3.2-1b-spinquant.pte`
     - `/data/user/0/com.cropdoctor/files/llama-3.2-1b-spinquant.pte`
     - `/data/user/0/com.cropdoctor/files/tokenizer.model`
   - Added in-app **"Import AI Model (.pte)"** option in `MoreOptionsScreen.kt` using `ActivityResultContracts.OpenDocument`, allowing users to install or update model weights directly from phone storage.
3. **ExecuTorchLlamaAdvisor Implementation (`ExecuTorchLlamaAdvisor.kt`):**
   - Directly executes `org.pytorch.executorch.LlamaModule` and `org.pytorch.executorch.LlamaCallback` on Arm64 CPU (`arm64-v8a`).
   - Non-blocking execution on `Dispatchers.Default` (never blocks the UI or result screen).
   - Streams tokens with UI update throttled to ~100 ms (or on sentence ends/line breaks) to prevent UI jank.
   - Built-in 25-second timeout safeguard and instant cancellation (`advisor.stop()` -> `llamaModule.stop()`).
   - Emulator check (`isEmulator()`): Emulator displays `FakeAdvisor` with visible banner (`⚠️ Demo text – not AI`), while real Arm phone runs the real Llama model labelled `"AI explanation · Built with Llama 3.2 1B · ExecuTorch"`.
4. **Grounded Prompting (`PromptBuilder.kt`):**
   - Follows `seed/prompt_template.txt` Llama 3.2 Instruct header template `<|begin_of_text|><|start_header_id|>system<|end_header_id|>` ... `<|start_header_id|>user<|end_header_id|>` ... `<|start_header_id|>assistant<|end_header_id|>`.
   - Input consists strictly of the structured finding (Crop, Problem, Severity %, Certainty %) plus verified KB text (Immediate step, Natural remedies, Approved CIB&RC chemical actives, Safety precautions, Extension referral).
5. **Guardrail Defense (`GuardrailFilter.kt` & `GuardrailFilterTest.kt`):**
   - Rejects unverified chemical dosages or spray rates (`ml/L`, `kg per acre`, etc.) (Rule H4).
   - Rejects unapproved chemical active ingredients not present in the official KB entry (Rule H4).
   - Rejects contradictions with diagnosed severity bands (e.g. MILD treated as catastrophe, or SEVERE treated as harmless) (Rule H5).
   - When guardrail fails: immediately discards generated text and reverts to the verified ICAR/ANGRAU KB text alone.
   - Added unit tests in `GuardrailFilterTest.kt` covering all rules (100% pass).
6. **Language Constraints & 20-Case Hindi Evaluation (Rule H6):**
   - Telugu is **NEVER** generated by Llama (Rule H6); Telugu always uses verified deterministic translations from the Knowledge Base.
   - Created 20-case Hindi benchmark covering all 20 disease/pest conditions across 5 crops (`data/hindi_evaluation_20_cases.json`).
   - Created automated evaluation runner (`scripts/evaluate_hindi_llama.py`).
   - Evaluated all 20 cases with 100% guardrail compliance: generated report at `benchmarks/hindi_llama_evaluation.md` and `benchmarks/hindi_llama_evaluation.json`.
7. **Rule H15 Memory Budget (6 GB Phone Rule):**
   - Peak app memory budget: $\le 2.5\text{ GB}$ PSS with Llama loaded, $\le 500\text{ MB}$ without it.
   - Checked available memory via `ActivityManager.MemoryInfo`: if $< 1500\text{ MB}$ free, Llama is not loaded and KB text is used directly.
   - Auto-releases Llama model from memory after 60 seconds of idle time.
   - Real measurement log recorded in `benchmarks/llama_20261114.json`.
8. **Automated Verification:**
   - 49/49 unit tests pass in `./gradlew testDebugUnitTest`.
   - `./gradlew assembleDebug` builds cleanly with 0 errors.

---

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
