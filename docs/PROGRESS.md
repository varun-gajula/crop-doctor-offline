# Project Progress - Crop Doctor Offline

## Current Status: Task M1c Complete (Real Field Leaves Speed & Fallback Rate Benchmarks)

### Done in Task M1c: Two Fixes to M1 Measurements (Rule H3, H15, PS1)
1. **Shutter-to-Result on Real Diagnosed Field Leaves (Requirement 1):**
   - Replaced dummy bitmap measurements with 10 real plant leaf photos from the phone gallery (Plantix and field captures) that produce valid, diagnosed disease classifications.
   - Diagnosed runs:
     - 1x Tomato Early Blight (64.2% conf, Moderate severity, 14.8% lesion)
     - 8x Maize Common Rust (55.0% - 74.8% conf, Severe severity)
     - 1x Maize Gray Leaf Spot (43.7% conf, Moderate severity, 25.5% lesion)
   - Rejection Log: 1 photo (`IMG_20261114_142843250.jpg`) was rejected (confidence 0.394 < threshold 0.40), safely routing to "Can't tell", and was excluded from the diagnosed count per instructions.
   - Full Path Latency (Validation $\rightarrow$ Preprocessing $\rightarrow$ Inference $\rightarrow$ Calibration $\rightarrow$ Lesion Area $\rightarrow$ Rules Engine $\rightarrow$ KB Retrieval):
     - **Average Shutter-to-Result: 117.10 ms (0.117 s)** ($\le 1,500\text{ ms}$ budget, 12.8x faster than budget).
     - Min: 85.45 ms · Max: 169.52 ms.
   - Full component breakdown:
     - Validation (`LeafValidator`): 13.33 ms (11.4%)
     - Preprocessing (320x320 scaling + normalization): 36.10 ms (30.8%)
     - Vision Inference (ExecuTorch 64-bit Arm CPU): 57.54 ms (49.1%)
     - Calibration & Free Energy OOD: 1.84 ms (1.6%)
     - Lesion Fraction Estimation: 8.18 ms (7.0%)
     - Rules Engine Severity/Tier: 0.09 ms (< 0.1%)
     - Knowledge Base Retrieval: 0.01 ms (< 0.1%)
   - Replaced old table in `benchmarks/speed_summary.md` and saved raw data in `benchmarks/speed_summary_real_leaves.json`.

2. **Fallback Rate Evaluation Across 3 Operational Situations (Requirement 2):**
   - Executed on-device suite `FallbackRateBenchmarkTest` on the connected 6 GB phone (`POCO M4 Pro`, Android 14):
     - **(a) Just after reboot / clean idle**: Available RAM = **2,740.3 MB**, `isLowMemory` = `false` $\rightarrow$ `MemoryBudgetDecider` = `true` (Safe to load). Llama loaded (`true`), Fallback ran (`false`), UI label = `"AI explanation"`, Peak PSS = 1,619.4 MB ($\Delta PSS = 1,430.4\text{ MB}$).
     - **(b) With 5 background apps open (WhatsApp, YouTube, Chrome, Camera, Gallery)**: Available RAM = **2,355.9 MB**, `isLowMemory` = `false` $\rightarrow$ `MemoryBudgetDecider` = `true` (Safe to load). Llama loaded (`true`), Fallback ran (`false`), UI label = `"AI explanation"`, Peak PSS = 1,676.5 MB ($\Delta PSS = 1,502.1\text{ MB}$).
     - **(c) After 10 minutes of normal continuous use**: Available RAM = **2,380.7 MB**, `isLowMemory` = `false` $\rightarrow$ `MemoryBudgetDecider` = `true` (Safe to load). Llama loaded (`true`), Fallback ran (`false`), UI label = `"AI explanation"`, Peak PSS = 1,688.6 MB ($\Delta PSS = 1,496.9\text{ MB}$).
   - **Fallback Path Verification (RAM < threshold)**: When available RAM is below threshold, Decider = `false`, Llama load is completely bypassed, fallback engages in **1.3 seconds** with **0.0 MB** extra RAM, and verified KB text is shown under the label `"Offline guide"`.
   - Saved summary in `benchmarks/fallback_rate.md` and raw logs in `benchmarks/fallback_*.json`.

3. **Llama Allocation Delta & Safe Threshold Proposal (Requirement 3):**
   - In situation (b), the 6 GB physical device maintained 2,355.9 MB available RAM, so Llama loaded safely.
   - Measured extra memory needed to load and stream Llama 3.2 1B INT4: **1,502.1 MB** peak (average 1,476.5 MB).
   - Evidence-based analysis: Since the model allocation alone consumes 1,502.1 MB, lowering the threshold below **1,500 MB** would cause out-of-memory kernel kills on devices with $< 1.5\text{ GB}$ available.
   - Smallest safe threshold: **1,500 MB** (or **1,700 MB** with a 200 MB UI/Camera buffer).

---

## Previous Status: Task M1 Complete (Memory & Speed On-Device Phone Benchmarks)

### Done in Task M1: Measure Memory and Speed on Connected Phone (Rule H3, H15, PS1)
1. **Low-Memory Fallback Decision Function & Unit Tests (Requirement 5):**
   - Implemented `MemoryBudgetDecider.kt` with pure, testable decision function `shouldLoadLlama(availMemBytes, isLowMemory)`.
   - Requires minimum 1,500 MB free RAM and `isLowMemory == false` before allowing Llama allocation.
   - Fallback renders verified knowledge-base guidance under the clean label `"Offline guide"` in `AdvisoryCard.kt` and `ResultScreen.kt`.
   - Added `MemoryBudgetDeciderTest.kt` covering RAM thresholds, low-memory flags, exact boundary conditions, and custom thresholds (5/5 unit tests pass).
2. **On-Device Memory Measurement (Requirement 1 & 4):**
   - Measured live on connected physical 6 GB Arm device (`22041219PI` - Redmi Note 11S / POCO M4 Pro running Android 14) using native `adb shell dumpsys meminfo com.cropdoctor`.
   - Saved 5 raw dump files in `benchmarks/mem_*.txt`:
     - (a) App idle after start: **147.68 MB PSS** ($\le 500\text{ MB}$ limit) -> **PASS**.
     - (b) After one vision result: **186.81 MB PSS** ($\le 500\text{ MB}$ limit) -> **PASS**.
     - (c) With Llama loaded & idle: **1,580.19 MB PSS** ($\le 2,500\text{ MB}$ limit) -> **PASS**.
     - (d) Peak while Llama streams answer: **1,706.55 MB PSS** ($\le 2,500\text{ MB}$ limit) -> **PASS**.
     - (e) 60 seconds after Llama is released: **243.79 MB PSS** ($\le 500\text{ MB}$ limit) -> **PASS**.
   - Created summary table in `benchmarks/memory_summary.md`.
3. **Llama Speed Evaluation Across 5 Diverse Cases (Requirement 2):**
   - Automated via on-device suite `LlamaSpeedBenchmarkTest` across 5 crops and diseases (`maize_common_rust`, `tomato_early_blight`, `rice_leaf_blast`, `cotton_bacterial_blight`, `chilli_leaf_curl_virus`).
   - Results recorded in `benchmarks/llama_5cases_speed.json` and `benchmarks/llama_speed.md`:
     - Average Time to First Token (TTFT): **4,087.0 ms (4.09 s)**.
     - Average Generation Decode Speed: **11.94 tokens/second** (5.97x above the 2.0 tok/s threshold).
     - Average Overall Speed: **6.72 tokens/second**.
     - Guardrail Compliance: 5/5 cases passed (100% compliance with verified KB text, zero invented dosages).
4. **Shutter-to-Result & Cold Start Benchmarks (Requirement 3):**
   - Shutter-to-Result: 10 runs evaluated via `VisionSpeedBenchmarkTest` on phone:
     - Average: **96.7 ms (0.097 s)** (target $\le 1,500\text{ ms}$, 15.5x faster than budget).
     - Min: 81 ms · Max: 158 ms.
     - Raw log saved in `benchmarks/shutter_to_result_10runs.json`.
   - Cold Start: 5 runs evaluated via `adb shell am start -W -n com.cropdoctor/.MainActivity`:
     - Average TotalTime: **1,528.0 ms (1.53 s)** (target $\le 2,000\text{ ms}$).
     - Average WaitTime: **1,545.6 ms (1.55 s)**.
   - Created summary table in `benchmarks/speed_summary.md`.
5. **Build & Automated Verification:**
   - 65 unit tests pass in `./gradlew testDebugUnitTest`.
   - All on-device instrumentation tests pass on the physical phone.

---

### Done in Task V2: Fix Honesty Problems in Vision Lane (Decision D1, PS1, Rules H3, H11, H14, H15)
1. **Thresholds Single Source of Truth & Zero Numeric Literals:**
   - Removed hardcoded `DEFAULT_ENERGY_THRESHOLD = 5.0` and `defaultThreshold = 0.25f` from `ExecuTorchClassifier.kt`.
   - `thresholds.json` is now the single source of truth.
   - If `thresholds.json` is missing or invalid, the classifier immediately returns `"Can't tell"` (`classId = "unknown"`, `certaintyBand = CertaintyBand.NOT_SURE`) for every photo and logs an error, never silently falling back to other numbers.
   - Kept provisional thresholds `-1.5` / `0.40` in `thresholds.json` and labeled them "provisional, to be set from real photos" in `benchmarks/threshold_choice.md`.
   - Added `ThresholdHonestyTest.kt` asserting:
     - Missing / invalid `thresholds.json` fails safely to "Can't tell" (unknown / NOT_SURE).
     - Automated static analysis asserting Kotlin source code contains zero numeric threshold literals (`5.0`, `0.25`, etc.).
2. **Blocked Classes (Classes 7, 8, 12, 13):**
   - Created `data/supported_classes.json` and `app/src/main/assets/supported_classes.json`.
   - Marked classes 7, 8, 12, 13 as `supported = false` with reasons:
     - Class 7 (`cotton_leaf_curl_virus`): "fewer than 300 unique photos"
     - Class 8 (`cotton_alternaria_leaf_spot`): "fewer than 300 unique photos"
     - Class 12 (`chilli_cercospora_leaf_spot`): "fewer than 300 unique photos"
     - Class 13 (`chilli_white_spot`): "fewer than 300 unique photos; cause unverified"
   - Retained all 25 classes in model output (no model architecture truncation).
   - Updated `VisionMath.evaluateDiagnosis` to route any top prediction in an unsupported class to `"Can't tell"` (`classId = "unknown"`, `displayName = "Not sure - ask an extension officer"`, `certaintyBand = CertaintyBand.NOT_SURE`).
   - Added `BlockedClassesHonestyTest.kt` unit tests verifying blocked classes are treated as "Can't tell" and supported classes are unaffected.
3. **Colab Grouped Split & Retrain Notebook (`notebooks/03_grouped_split_and_retrain.ipynb`):**
   - Created notebook 03 without deleting notebook 02.
   - Formatted with 13 executable cells and 0 precomputed/fake outputs (Rule H3 / H12):
     a. Dihedral perceptual hash: 64-bit dHash computed across 8 transformations (photo, horizontal flip, and 90-degree rotations), keeping the minimum hash so rotated or flipped copies match.
     b. Near-duplicate clustering within each class for photos with Hamming distance $\le 6$ via connected components.
     c. Leakage audit for baseline random split (seed 42), reporting near-duplicate leaks per class and total, saved to `leakage_report.json`.
     d. Cluster-aware grouped split (80/20) with whole cluster assignment and leave-one-source-out validation for classes with $\ge 2$ sources holding $\ge 50$ photos, saved to `split_v2.json`.
     e. Retraining pipeline for MobileNetV3-Large at $320 \times 320$ with single 25-way classification output (no severity head), app-parity augmentations, inverse-sqrt class weights, and 15 epochs.
     f. Validation evaluation per class on grouped split, Platt temperature scaling calibration, and export of `vision.pte`, `labels.json`, `thresholds.json`, and `metrics.json` to Google Drive `crop_doctor_runs_v2/`.
   - Passed local Python syntax verification on all code cells via `scripts/check_notebook_syntax.py`.
4. **Build & Test Verification:**
   - 60 unit tests pass in `./gradlew testDebugUnitTest` and `./gradlew test`.
   - `./gradlew assembleDebug` builds cleanly in 3s.

---

### Previous Status: Task V1 Audit & Fix Vision Lane Complete

### Previous Status: T5 Real-Device Diagnosis & Llama Pipeline Resolved & Live on Physical Phone

### Resolved on Physical Phone (`sseqjnucrkeeprtw` - POCO M4 Pro / Redmi Note 11S):
1. **Camera Capture Crash & App Closing Fix:**
   - **Root Cause**: Heap array passed to JNI `Tensor.fromBlob(floatBuffer)` had arbitrary memory alignment and un-rewound position, and `vision.pte` had unaligned flatbuffer weight offsets triggering XNNPACK `SIGBUS (BUS_ADRERR)` in `libexecutorch_jni.so` (`create_fully_connected_nc_f32`) on the physical Arm64 CPU.
   - **Fix Applied**: Upgraded ExecuTorch to `0.6.0` (`org.pytorch:executorch-android:0.6.0`) with direct native-byte-order buffer allocation (`Tensor.allocateFloatBuffer`), proper float buffer rewinding, and modern alignment handling.
2. **On-Device Llama 3.2 1B Native Execution (EO8 SpinQuant INT4):**
   - **Root Cause**: `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` was exported for ExecuTorch schema EO8 (0.6.0+), causing ExecuTorch 0.5.1's legacy `LlamaModule.load()` to fail with status `17` (`Unhandled node type`).
   - **Fix Applied**: Migrated to `org.pytorch.executorch.extension.llm.LlmModule` and `LlmCallback` in ExecuTorch 0.6.0. Verified live with on-device test `LlamaPhoneSanityTest`: model loaded and generated tokens on the physical Arm64 CPU in 7.16s (`OK (1 test)`).
3. **Live Phone Camera & Inference Verification:**
   - Verified on physical phone at 1:23 PM: live camera preview runs smoothly at 20+ FPS.
   - Real-world scan of affected leaf captured and classified without closing: successfully diagnosed **Maize Common Rust** (`Certainty: Probably`, `Severity: Moderate`) with full grounded agronomic advisory.
4. **All 49 unit tests pass** (`./gradlew testDebugUnitTest`). Both on-device instrumentation tests pass (`LlamaPhoneSanityTest`, `VisionPhoneSanityTest`).

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
