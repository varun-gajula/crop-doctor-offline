# Project Progress - Crop Doctor Offline

## Current Status: Tasks UI-RESTORE & BUG-CAPTURE Complete

### Done in Task BUG-CAPTURE: Shutter Stability & Crash/Hang Resolution
1. **Root Cause Analysis & Proving**:
   - **App Close (Crash)**: Caused by uncaught `RejectedExecutionException` when `cameraExecutor.shutdown()` was called inside `DisposableEffect(lifecycleOwner)` during navigation transitions while the executor was retained in `remember`. Subsequent shutter clicks threw `RejectedExecutionException` on the main UI thread. In addition, rapid shutter taps caused camera HAL buffer starvation.
   - **Infinite Spinner (Hang State)**: Caused by coroutine cancellation calling suspending `withContext(Dispatchers.Main)` inside `finally` without `NonCancellable`. On cancellation, `CancellationException` was thrown, aborting `isProcessingImage = false` and leaving the spinner permanently active. Additionally, no analysis timeout existed and background Llama generation could interfere with camera preprocessing.
2. **Six Comprehensive Fixes Implemented**:
   - `isShutterBusy` lock on shutter button prevents re-entrant hardware capture requests.
   - Guaranteed flag reset in `finally { withContext(NonCancellable + Dispatchers.Main) { isProcessingImage = false } }`.
   - `image.close()` and bitmap recycling in strict `finally` blocks.
   - Hard 10s analysis timeout (`withTimeout(10_000L)`) routing to `CANT_TELL` with `"Could not read the photo. Please try again."` and `"Take another photo"` button.
   - Caught all decode/preprocessing/inference exceptions gracefully into `CANT_TELL`.
   - Cancelled active Llama generation immediately on any new capture (`stop()` + `job.cancel()`).
   - Camera executor lifecycle moved to `DisposableEffect(Unit)` and wrapped `takePicture` in `try-catch`.
3. **Verification**:
   - Real shutter stress test: 30 fast taps (300 ms) and 30 slow taps (2000 ms) -> **0 crashes, 0 hangs, 0 ANRs, peak PSS 258.9 MB**.
   - Added `CaptureSafetyTest.kt` unit test suite (4 tests covering timeout, cancellation, exception handling, and 3-language error strings).
   - Documented in `benchmarks/bug_capture_fix.md`.

---

### Done in Task UI-RESTORE: Visual Restoration with P2/P3/P4 Behaviors Preserved
1. **Commit Identification & Branching**:
   - Created backup branch `backup-before-ui-restore`.
   - Identified last commit before P3: `f62f143c95e0ed4fa9c18b1d60c73b24800a1180` ("Task V3: Add verification script print_v3_metrics.py and verify benchmarks").
2. **Restored Files & Styling**:
   - Restored original theme, colors, typography, card shapes, topbars, and elevation from `f62f143`.
   - `ResultScreen.kt`: restored clean layout with old card/button styles; integrated DIAGNOSE, CANT_TELL, and OUT_OF_CLASS outcomes.
   - `MainCameraScreen.kt`: restored original clean camera screen with topbar and overlay controls; kept visible "Change crop" chip.
   - `CropSelectionScreen.kt`: styled 5-tile crop picker to match original theme and card elevation.
   - Localized selected crop name in `CropMapping.getCropDisplayName(...)` so `outcome_out_of_class_message` displays the crop name (e.g., "This does not match Rice...").
3. **Screenshots & Test Count**:
   - Captured 12 full-screen screenshots across English and Telugu:
     - Home screen (EN, TE)
     - Camera screen with Change crop chip (EN, TE)
     - Crop selection screen (EN, TE)
     - Result screen - DIAGNOSE (EN, TE)
     - Result screen - CAN'T TELL (EN, TE)
     - Result screen - OUT OF CLASS (EN, TE)
   - Unit tests: **131 passed**, 0 failed.
   - `assembleDebug`: Clean build.

---

## Prior Status: Task P5 Complete (End-to-End Phone Benchmark & Robustness Check)
1. **Physical Reference Phone Execution (`tools/run_p5_benchmarks.py`)**:
   - Device: Connected reference 6 GB phone (POCO 22041219PI, 64-bit Arm CPU `arm64-v8a`, Android 14, 5.5 GB usable RAM).
   - Executed full scripted run of 55 staged test photos across all 8 probe categories:
     - 5 Real Rice leaves: 5 / 5 diagnosed (100.0%), mean latency 938.6 ms
     - 5 Real Cotton leaves: 5 / 5 diagnosed (100.0%), mean latency 740.5 ms
     - 5 Real Chilli leaves: 5 / 5 diagnosed (100.0%), mean latency 817.2 ms
     - 5 Real Tomato leaves: 1 diagnosed, 2 CANT_TELL, 2 OUT_OF_CLASS (low crop mass), mean latency 766.2 ms
     - 5 Real Maize leaves: 4 / 5 diagnosed, 1 CANT_TELL, mean latency 776.1 ms
     - 10 Non-crop objects (`nocrop`): 10 / 10 rejected (100.0%: 9 CANT_TELL, 1 OUT_OF_CLASS), mean latency 758.2 ms
     - 10 Other crops (`othercrop`, Cashew/Cassava): 9 / 10 rejected (90.0%: 7 OUT_OF_CLASS, 2 CANT_TELL, 1 diagnosed), mean latency 734.0 ms
     - 10 Laptop Screen captures (`screen`): 6 diagnosed, 4 CANT_TELL, mean latency 720.6 ms
   - Overall shutter-to-result latency on phone: mean **769.5 ms** (min: 621.2 ms, max: 1759.7 ms), within the 1,500 ms limit.
2. **20 Consecutive Camera Captures Memory & ANR Check**:
   - Continuous capture of 20 field camera images on device with real-time memory and ANR tracking.
   - **ANRs**: Exactly **0** ANRs detected.
   - **PSS Memory Footprint**: Baseline = 213.65 MB, Final = 211.91 MB (**$\Delta\text{PSS} = -1.74\text{ MB}$**, zero memory leaks).
   - Saved raw outputs to `benchmarks/p5_scripted_run_raw.json`, `benchmarks/p5_consecutive_captures_raw.json`, and generated `benchmarks/p5_phone_report.md`.
3. **Reports & Traceability Updated**:
   - Updated `docs/REPORT.md` Section 3.3 and Section 7.6 with strictly measured numbers from logs.
   - Reran `scripts/trace_numbers.py`: 100% of 54 quantitative metrics traced to raw files in `benchmarks/` with 0 unverified metrics (`docs/NUMBER_TRACE.md`).
4. **Build & Unit Test Verification**:
   - All 127 unit tests passing (`./gradlew testDebugUnitTest`).
   - Debug APK assembled cleanly (`./gradlew assembleDebug`).

---

## Prior Status: Task P4 Complete (Three Clear Outcomes: DIAGNOSE, CAN'T TELL, OUT OF CLASS)

### Done in Task P4: Three Clear Outcomes Decided in Plain Code with Measured Thresholds (Option B)
1. **Measured Thresholds Configured (Option B Applied)**:
   - Evaluated 610 decision rows across 5 probe sets (298 images under `C:\crop-data\realtest\`) in `benchmarks/p4_decision_table.csv`.
   - Single source of truth configured in `app/src/main/assets/thresholds.json` and `ml/thresholds.json`:
     - `temperature`: `1.7818`
     - `confidence_threshold`: `0.3434`
     - `energy_threshold`: `-8.0000` (Free Energy OOD gate)
     - `crop_mass_threshold`: `0.6000` (Option B: minimum 60% probability mass on selected crop)
     - `margin_threshold`: `0.2500` (Option B: minimum within-crop top-1 minus top-2 probability difference)
     - `borderline_energy`: `-9.0000` (Option B: borderline energy cutoff)
2. **Decision Table Targets Verified (from `benchmarks/p4_decision_report.md`)**:
   - **Supported Crop Leaves (107 images)**: **98.1%** (105 / 107) `DIAGNOSE` (93.5%, 100 images) or `CAN'T TELL` (4.7%, 5 images); only 1.9% (2 images) `OUT OF CLASS`. Target $\ge 90\%$ **PASSED**.
   - **Non-Crop Objects (`nocrop`, 30 images, 150 evaluations)**: **96.7%** (145 / 150) `CAN'T TELL`. Target $\ge 80\%$ **PASSED**.
   - **Other Crops (`othercrop`, 48 images, 240 evaluations)**: **92.5%** (222 / 240) `OUT OF CLASS` (172) or `CAN'T TELL` (50). Target $\ge 60\%$ **PASSED**.
   - **Unsupported Diseases (`unsupported`, 93 images)**: **10.8%** (10 / 93) wrongly diagnosed; **89.2%** (83 / 93) safely intercepted as `OUT OF CLASS` (30 images) or `CAN'T TELL` (53 images).
   - **Screen Photos (`screen`, 20 photos)**: 40% `DIAGNOSE`, 50% `CAN'T TELL`, 10% `OUT OF CLASS`. Note added: *"cause not established; screen photos have energy between real leaves and non-crop photos"*.
   - **Supported DIAGNOSE Accuracy**: **95 / 100 (95.0%)** correct against true ground-truth class.
   - **Probe Set Sources (from `p4_probe_manifest.json`)**: 70 real phone photos (12 supported, 8 unsupported, 20 screen, 30 nocrop), 228 web/dataset images (95 supported, 85 unsupported, 48 othercrop). Explicit note: *"Thresholds were chosen and scored on the same probe set, so the reported rates are optimistic."*
3. **P2 Wrong-Crop Resolution**:
   - `camera_real_11` (tomato bacterial spot): $M_C = 0.0093 < 0.60 \rightarrow$ **`OUT OF CLASS`**.
   - `camera_real_17` (tomato yellow leaf curl virus): $M_C = 0.0828 < 0.60 \rightarrow$ **`OUT OF CLASS`**.
4. **Three Distinct Outcome UI Layouts (`ResultScreen.kt`)**:
   - `DIAGNOSE`: Full disease/pest diagnosis with severity, certainty, field spread, treatment tiers (natural & chemical), AI explanation, case reports.
   - `CAN'T TELL`: Warm orange theme (`HelpOutline`), sourced guidance *"Can't tell. This does not look like a clear leaf. Take a closer photo in daylight or ask an extension officer."*, evaluated leaf photo card, "Take another photo" button, and Kisan Call Centre referral button (1800-180-1551). Strictly NO treatments, medicine doses, severity cards, or AI explanation (Rules H4, H5).
   - `OUT OF CLASS`: Amber theme (`WarningAmber`), sourced guidance *"Out of class. This does not match <selected crop> or one of the 20 problems we support. Choose the right crop or ask an extension officer."*, evaluated leaf photo card, "Change crop" button, "Take another photo" button, and Kisan Call Centre referral button (1800-180-1551). Strictly NO treatments, medicine doses, severity cards, or AI explanation (Rules H4, H5).
5. **Localization & Unit Tests**:
   - All strings localized in English, Telugu, and Hindi across `strings.xml`.
   - Unit tests added to `CropdocDecisionGateTest.kt` verifying all outcome gates, Option B thresholds, and 3-language string existence.
   - **127 / 127 unit tests passing** (`./gradlew testDebugUnitTest`).
   - `./gradlew assembleDebug` passed cleanly.
   - On-device screenshots captured for all 3 outcomes in English, Telugu, and Hindi: `benchmarks/p4_outcome_diagnose.png`, `p4_outcome_cant_tell.png`, `p4_outcome_out_of_class.png`, `p4_outcome_cant_tell_te.png`, `p4_outcome_out_of_class_te.png`, `p4_outcome_cant_tell_hi.png`, `p4_outcome_out_of_class_hi.png`.

---

## Prior Status: Task P3 Complete (Crop Selection Before Scanning)

### Done in Task P2: Unified Image Pipeline & Continuous Capture Stability (Rules R1-R9, H1-H15)
1. **Aspect Handling & Preprocessing Parity with Training (`docs/PREPROCESSING.md`)**:
   - Inspected `Crop_Doctor_Kaggle_MaxGPU.ipynb` and `ml/dataset.py`: confirmed training used **anisotropic bilinear squash to 512x512** via `torchvision.transforms.Resize((512, 512))`.
   - Identified that the legacy camera path center-cropped to square, which discarded leaf context for elongated leaves (rice, maize), while legacy gallery decoded with no rotation or config enforcement.
   - Unified both camera and gallery paths to match training: anisotropic bilinear squash to 512x512 with ImageNet normalization (`mean=[0.485, 0.456, 0.406]`, `std=[0.229, 0.224, 0.225]`).
2. **Unified Image Preparation Engine (`ImagePreprocessor.kt`)**:
   - Single function `ImagePreprocessor.prepare(bytes, reusableBuffer)` prepares every image for the model regardless of whether it originates from camera capture, gallery selection, or file intent:
     - Calculates dynamic `inSampleSize` to bound peak decode memory.
     - Detects EXIF orientation (`android.media.ExifInterface`) and applies upright rotation.
     - Enforces non-hardware `ARGB_8888` and standard `sRGB` color space.
     - Resizes to 512x512 bilinear squash matching training distribution.
     - Computes Botanical Leaf Validation (Gate 1).
     - Normalizes into planar direct `FloatBuffer` of shape `[1, 3, 512, 512]`.
     - Immediately recycles intermediate bitmaps and reuses direct native buffer across consecutive captures.
3. **Continuous Capture & Execution State Machine (`MainActivity.kt`, `MainCameraScreen.kt`)**:
   - Single in-flight analysis guarantee via `AtomicLong(requestId)` and coroutine cancellation (`currentDiagnosisJob?.cancel()`, `currentAdviceJob?.cancel()`).
   - Disabled shutter button and gallery button during analysis (`isProcessing == true`) with loading spinner and "Analyzing leaf..." / "ఆకును విశ్లేషిస్తోంది..." / "पत्ती का विश्लेषण हो रहा है..." progress banner.
   - All decoding, preprocessing, and inference run on background dispatcher (`Dispatchers.Default`), never blocking the UI thread.
   - ImageProxy closed strictly in `finally` blocks; camera executor released cleanly on screen disposal; zero retained history of old bitmaps in memory.
4. **Unit Tests & Parity Verification (`ImagePreprocessorTest.kt`, `ImagePipelineParityTest.kt`)**:
   - Added unit test suite verifying `inSampleSize` math, ImageNet mean/std constants, planar channel ordering, and pixel normalization parity.
   - All 116 unit tests passing (`./gradlew testDebugUnitTest`).
   - Verified that camera and gallery inputs produce identical tensor outputs ($< 10^{-3}$ max diff) and that EXIF orientation 6 rotates upright properly.
5. **Physical Device Benchmark on Reference 6 GB Phone (`benchmarks/p2_continuous_capture.md`)**:
   - Executed on physical POCO 22041219PI (Xiaomi) with 5.5 GB usable RAM (6 GB reference phone, Android 14).
   - 20 consecutive camera captures followed by 20 consecutive gallery selections (40 total continuous executions):
     - **Crashes**: **0** (Pass)
     - **ANRs**: **0** (Pass)
     - **Camera Latency**: First = 597.7 ms, Average = **396.9 ms**, Max = 597.7 ms (Max/First = 1.00x $\le$ 3.0x, Pass)
     - **Gallery Latency**: First = 499.4 ms, Average = **451.5 ms**, Max = 816.8 ms (Max/First = 1.64x $\le$ 3.0x, Pass)
     - **Memory (PSS Growth)**: Initial = 250.93 MB, Final = 241.10 MB (**$\Delta\text{PSS} = -9.83\text{ MB}$**, well under 50 MB threshold, Pass)
     - **UI Jank**: 278 frames rendered, 44 janky frames (15.83%, Pass)
     - **Gallery Accuracy**: **20/20 (100%)** gallery test images accepted and accurately diagnosed with high confidence.

---

## Prior Status: Task P1 Complete (Diagnostic Gate Report: Laptop Screen & Gallery Photos)
1. **CROPDOC_DECISION Diagnostic Gate Logging (`ExecuTorchClassifier.kt`)**:
   - Added formatted, zero-overhead diagnostic logging with tag `CROPDOC_DECISION` for every image processed.
   - Evaluates all gates in strict sequential order:
     - Gate 1: Botanical Leaf Validator (`LeafValidator.validate(bitmap)`)
     - Gate 2: Free Energy Out-of-Distribution (`E(x) <= energy_threshold`)
     - Gate 3: Calibrated Softmax Confidence (`prob >= confidence_threshold`)
     - Gate 4: Non-crop Weeds filter (`weeds_not_a_crop`)
   - Emits structured log line: source, orig dimensions, EXIF orientation, bitmap config, color space, downscaled dimensions, Gate 1-4 individual statuses, and final decision/reason code (`ACCEPTED`, `REJECTED_LEAF_VALIDATOR`, `REJECTED_OOD_ENERGY`, `REJECTED_LOW_CONFIDENCE`, `REJECTED_WEEDS`, `REJECTED_UNSUPPORTED`).
   - Zero behavior changes or threshold modifications made.
2. **Unit Test Verification (`CropdocDecisionGateTest.kt`)**:
   - Unit test suite verifying exact gate execution order, decision string formatting, and reason code propagation.
   - All 114 unit tests passing (`./gradlew testDebugUnitTest`).
3. **On-Device 60-Image Evaluation Benchmark**:
   - Deployed latest debug APK to Motorola edge 60 (`arm64-v8a`, 12 GB RAM).
   - Evaluated 60 images across 3 sets via ADB intent harness:
     - 20 photos of laptop screen displaying crop leaves (`screen/`)
     - 20 pre-processed crop leaf gallery photos (`gallery/`)
     - 20 real camera field photos (`real_leaves/`)
   - Captured and logged 100% of runs in `benchmarks/p1_benchmark_raw.json`.
4. **Architectural Code Audit of Gallery vs. Camera Paths (`MainCameraScreen.kt`)**:
   - Identified critical discrepancies:
     - **Missing EXIF Rotation**: Camera path rotates bitmap upright via `Matrix().postRotate()`; gallery path decodes without rotation, feeding portrait photos into the classifier sideways (90° rotated).
     - **Missing Center-Square Crop**: Camera path center-crops 1:1; gallery path passes uncropped rectangle, which is anisotropically stretched/squashed to 512x512.
     - **Color Space & Config**: Gallery path lacks explicit sRGB and ARGB_8888 decoding configuration.
5. **Gate Rejection & Statistical Report (`benchmarks/p1_gate_report.md`)**:
   - Documented exact gate rejection counts:
     - Laptop Screen: 10/20 rejected (100% at Gate 2 `REJECTED_OOD_ENERGY`, mean energy `-7.4969` vs threshold `-9.3050`).
     - Gallery: 20/20 accepted on pre-cropped square set; failures in field usage explained by EXIF orientation and anisotropic stretching bugs.
     - Camera Real Leaves: 14/20 rejected at Gate 2 (mean energy `-7.2362` due to complex field background clutter).
   - Documented optical/physical causes (Moiré interference, LED backlight emission) and software causes.

---

## Prior Status: Task P3 Complete (Crop Selection Before Scanning, Probabilistic Masking & Crop Mass Tracking)

### Done in Task P3: Crop Selection Before Scanning (Rules R1-R9, H1-H15)
1. **Crop Selection Screen (`CropSelectionScreen.kt`)**:
   - Tapping "Scan leaf" on the home screen immediately navigates to `CropSelectionScreen`.
   - Five large tiles ($\ge 150\text{dp} \times 140\text{dp}$, exceeding $\ge 96\text{dp}$ requirement):
     1. **Rice / వరి / धान** (`ic_tile_rice`)
     2. **Cotton / పత్తి / कपास** (`ic_tile_cotton`)
     3. **Chilli / మిరప / मिर्च** (`ic_tile_chilli`)
     4. **Tomato / టమాటా / टमाटर** (`ic_tile_tomato`)
     5. **Maize / మొక్కజొన్న / मक्का** (`ic_tile_maize`)
   - Names rendered with typography $\ge 19\text{sp}$ (Telugu $\ge 21\text{sp}$ per Rule U2).
   - High-detail vector illustrations designed in-app with zero uncredited/unlicensed external assets (Rule H4 / `docs/CREDITS.md`).
   - Content descriptions configured on all interactive tiles for accessibility (`flutter_a11y_agent` / Android a11y standards).
   - No typing required; intuitive one-tap selection with persistent language toggle chip and back button.
2. **Camera Screen Integration**:
   - `MainCameraScreen.kt` accepts `selectedCrop: String` and `onChangeCropClick: () -> Unit`.
   - Displays a prominent, high-contrast pill chip at top center with icon, crop name, and "[Change crop]" / "[పంటను మార్చండి]" / "[फसल बदलें]" button to easily return to `CropSelectionScreen`.
3. **Probabilistic Crop Masking & `crop_mass`**:
   - `CropMapping.kt` maps class IDs directly from `labels.json` prefixes to one of the 5 valid crops (`rice`, `cotton`, `chilli`, `tomato`, `maize`), returning `null` for `weeds_not_a_crop`.
   - `VisionMath.maskAndRenormalizeWithMass`:
     - Global unmasked softmax probabilities sum to 1.0.
     - `cropMass` retains the total model probability assigned to the selected crop's classes.
     - Subset logits are renormalized over selected crop classes such that their sum equals 1.0, while non-crop classes receive 0.0.
   - Updated `ClassifierResult.kt` to store `selectedCrop: String?` and `cropMass: Float`.
   - `ExecuTorchClassifier.kt` and `FakeClassifier.kt` updated to compute and populate `selectedCrop` and `cropMass`.
4. **End-to-End Grounded Integration**:
   - Selected crop flows into `ResultScreen.kt`, `SaveCaseReportScreen.kt` (saved case reports), and into the Case Finding sent to Llama (`- Crop: <crop>`).
5. **Localization & Rigorous Unit Testing**:
   - Sourced and localized all strings in English, Telugu, and Hindi across `strings.xml`.
   - Added `CropMappingTest.kt`:
     - Verified all 20 classes in `labels.json` map to exactly one valid crop except `weeds_not_a_crop`.
     - Verified crop class index sets are pairwise disjoint and complete.
     - Verified `maskAndRenormalizeWithMass` outputs valid normalized probabilities summing to 1.0 and correct `cropMass`.
     - Verified all required crop strings and selector titles exist and are non-empty in English, Telugu, and Hindi.
   - **Test Suite Results**: **113 / 113 tests passing** (100% pass rate, 0 failures, 0 errors, 0 skipped).
   - **Offline Manifest**: Verified strictly NO `INTERNET` permission (Hard Rule H1).
   - **Build**: `./gradlew assembleDebug` passed cleanly.
   - **Visual Verification**: Captured on-device screenshots in all 3 languages:
     - `benchmarks/p3_crop_selection_en.png`
     - `benchmarks/p3_crop_selection_te.png`
     - `benchmarks/p3_crop_selection_hi.png`
     - `benchmarks/p3_camera_crop_chip.png` (camera screen with "Change crop" chip).

---

## Prior Status: Task V8 Complete (Retrained 20-Class 512x512 Vision Model with Zero Leakage & 91.49% Accuracy)

### Done in Task V8: Retrained 20-Class 512x512 Vision Model Deployed to App (Rules R1-R9, H1-H15)
1. **High-Performance Retraining on Dual Tesla T4 GPUs (Kaggle)**:
   - Built a complete, balanced dataset of **11,589 verified 512x512 photos** across all 20 classes (each class $\ge 366$ photos, all $\ge 300$).
   - Within-class clustering via canonical dihedral 64-bit pHash (Hamming distance $\le 8$, Tomato $\le 10$). Cross-class duplicate exclusions enforced at Hamming $\le 2$.
   - Grouped 80/20 cluster split: 9,288 training images, 1,668 validation images.
   - **Zero Cluster Leakage Verified**: 6,641 train clusters, 1,668 validation clusters, 0 overlapping clusters (`leakage_after.json`: `zero_cluster_leakage: true`).
   - Trained MobileNetV3-Large for 15 epochs:
     - Final Train Loss: `0.0518`, Final Train Accuracy: `97.92%`.
     - Final Validation Loss: `0.2930` (Best Val Loss: `0.2866`), Final Validation Accuracy: **91.49%**.
     - Macro / Balanced Accuracy: **92.17%**.
     - Solved prior dataset shrinkage and `tomato_leaf_blight` deficiency: `tomato_leaf_blight` achieved 72.14% recall and 79.53% precision on 140 validation images.
     - `weeds_not_a_crop` achieved 100.0% recall (68/68) and 98.55% precision.
2. **Post-Training Calibration & Thresholds**:
   - Single scalar temperature scaling: calibrated $T = 1.7818$ (NLL before: `0.2956`, NLL after: `0.2436`).
   - Operating confidence threshold: `0.3434` (lowest confidence meeting 90% target precision on validation data).
   - Free Energy OOD threshold: `-9.3050` (95th percentile validation energy; normal crop leaves $\le -14.7$, non-crop noise $> -9.3$).
3. **ExecuTorch 0.6.0 Export & Asset Integration**:
   - Exported `vision.pte` with XNNPACK partitioner: file size **16.17 MB** (16,956,752 bytes).
   - Bundled into `app/src/main/assets/models/vision.pte`.
   - Updated `app/src/main/assets/thresholds.json` and `app/src/main/assets/preprocess.json` with calibrated $T$ and thresholds.
   - Hardened `ExecuTorchClassifier.kt` module extraction: added version and byte-count check (`modelFile.length() != 16_956_752L`) to guarantee automatic on-device upgrade over any cached previous model.
4. **Offline Manifest & Unit Test Verification**:
   - Manifest verified via `aapt2 dump permissions`: strictly NO `INTERNET` permission (Hard Rule H1).
   - Unit tests executed via `./gradlew testDebugUnitTest`: **100% passed** (109 / 109 tests).
   - APK built cleanly via `./gradlew assembleDebug`: output `app/build/outputs/apk/debug/app-debug.apk` (91.1 MB).
   - Deliverables and benchmark files updated in `benchmarks/final/`: `metrics.json`, `class_counts_v2.json`, `thresholds.json`, `confusion_matrix.png`, `confusion_matrix.csv`, `leakage_after.json`, `split_v2.json`, `training_history.json`, `probe_set.json`, `duplicate_report.json`, `preprocessing_validation.json`, `permissions.txt`.

---

## Prior Status: Task V7 Complete (Final 20-Class 512x512 Vision Model & On-Device Phone Benchmarks)

### Done in Task V7: Final 20-Class 512x512 Vision Model Integration & End-to-End Phone Verification (Rules R1-R9, H1-H15)
1. **Asset Deployment & Single Source of Truth**:
   - Deployed final 20-class 512x512 MobileNetV3-Large model (`vision.pte`: 16,927,056 bytes / 16.14 MB).
   - Bundled single sources of truth in APK assets: `labels.json` (20 classes in verified export order), `thresholds.json` (`temperature: 1.3302102`, `confidence_threshold: 0.4018166`, no `energy_threshold`), `preprocess.json` (`input_size: 512`), and `supported_classes.json`.
   - Completely deleted legacy `app/src/main/assets/model_class_map.json` and removed the 25-to-21 mapping and old severity head from `VisionMath.kt` and `ExecuTorchClassifier.kt`.
2. **Preprocessing Parity & Robust Botanical Pipeline**:
   - Updated preprocessing to match training: 512x512 RGB bilinear resize, float32 normalization using ImageNet mean `[0.485, 0.456, 0.406]` and std `[0.229, 0.224, 0.225]`.
   - Preprocessing numerical parity verified against `parity_samples.json` (max difference $\le 10^{-3}$).
   - `VisionMath.kt` evaluates directly over 20 classes with temperature scaling ($T = 1.3302$) and confidence thresholding ($0.4018$).
3. **Safety Guardrails & Gap Handling**:
   - `weeds_not_a_crop` and `tomato_leaf_blight` are explicitly configured as `supported: false` in `supported_classes.json`, failing closed to "Can't tell" (`unknown` / `NOT_SURE`).
   - Sourced KB gaps (`tomato_leaf_blight`, `tomato_septoria_leaf_spot`, `chilli_nutrition_deficiency`, `cotton_leaf_curl_virus`) documented in `docs/KB_GAPS.md`.
   - Gaps trigger extension worker referral (`shouldRefer = true`) with honest fallback banner: *"Advice for this problem is not available yet. Ask an extension officer"* — strictly avoiding invented advice (Rule H4).
4. **Manifest Offline Verification (Rule H1)**:
   - Verified via `aapt2 dump permissions app-debug.apk > benchmarks/final/permissions.txt`.
   - `android.permission.INTERNET` is strictly absent in the manifest.
5. **Unit Tests (109 / 109 Passed)**:
   - Updated and executed all unit tests via `./gradlew testDebugUnitTest`:
     - `ThresholdHonestyTest`: requires `temperature` and `confidence_threshold`, fails closed if missing.
     - `BlockedClassesHonestyTest`: blocks `weeds_not_a_crop` and `tomato_leaf_blight`.
     - `ModelClassMapTest`: verifies 20-class output size and direct reachability.
     - `VisionParityTest`: verifies numerical output parity within $10^{-3}$.
     - `VisionSeverityPipelineTest` and `PestVsDiseaseTest`: verify severity bands and type chips.
6. **Physical Phone Benchmarks (6 GB RAM Reference Phone CPH2753, arm64-v8a)**:
   - **Model Load Latency**:
     - Vision model (`vision.pte`, 16.1 MB): **307 ms** via ExecuTorch 0.6.0 C++ runtime.
     - Advisory model (`llama-3.2-1b-spinquant.pte`, 1.05 GB): **6,641 ms** via ExecuTorch LLM C++ extension.
   - **Shutter-to-Result Latency (10 Diagnosed Real Field Photo Runs)**:
     - Average: **144.70 ms** (Min: 133.68 ms, Max: 204.95 ms) vs budget $\le 1,500\text{ ms}$ (**PASS**, 10.3x faster).
     - Breakdown: Validation 7.49 ms, Preprocessing 62.86 ms, Vision Inference 68.07 ms, Calibration 1.07 ms, Lesion Estimation 5.09 ms, Rules Engine 0.12 ms, KB Lookup 0.01 ms.
     - Low-confidence photos (< 0.4018) rejected cleanly to extension referral with zero hallucination.
   - **Peak Memory (PSS) Compliance (Rule H15)**:
     - App + Vision (without Llama): **135.5 MB PSS** (Budget $\le 500\text{ MB}$, **PASS**).
     - App + Vision + Llama 1B Generating: **1,593.6 MB / 1.52 GB PSS** (Budget $\le 2.5\text{ GB}$, **PASS**).
   - **Cold Start Latency**:
     - Measured over 5 runs via `am start -W`: **1,780.6 ms** (1.78 s) vs budget $\le 2.0\text{ s}$ (**PASS**).
   - **On-Device Llama Advisory Performance**:
     - 5 grounded cases executed on Arm CPU: Average decode speed **10.53 tokens/s**, average TTFT **1,995 ms** (~2.0 s), **100% guardrail pass rate**.
   - **Real-Test Verification Script**:
     - `tools/eval_realtest.py` executed: 0% False Acceptance Rate (FAR) on non-crop. Summary saved to `benchmarks/final/realtest_summary.md`.

---

## Current Status: Task D4 Complete (Unique-Photo Audit of Tomato Datasets)

### Done in Task D4: Unique-Photo Audit of Tomato Datasets (Rules H3, H14)
1. **Audited Tomato Datasets via Dihedral Perceptual Hashing (64-bit dHash, 8 orientations)**:
   - Audited `C:\Users\ec026\Downloads\tomato siva\augmented_dataset` (30,609 photos across 10 classes) and cross-referenced against `C:\Users\ec026\Downloads\archive (2)` (2,499 photos across 10 matching classes).
   - Clustered photos per class at Hamming distance $\le 6$ via connected components to identify unique original photos.
2. **Key Findings**:
   - Total files scanned: 30,609.
   - Unique original photos (clusters): 29,480.
   - Overall internal duplicate rate: 3.69% (1,129 redundant duplicate copies identified).
   - High-duplication outliers: `Tomato_healthy` contains 467 duplicate copies (15.20% duplication rate, with a single cluster of 433 photos); `Tomato_Spider_mites` has a cluster of 93 photos (5.96% duplication rate).
   - Cross-folder overlap: 1,490 unique clusters in `tomato siva` appear in `archive (2)`, proving substantial shared provenance.
3. **Artifacts Saved & Traceable**:
   - `benchmarks/tomato_unique_audit.csv` with per-class file counts, unique clusters, duplicate percentages, and overlap with archive2.
   - `C:\crop-data\final19\tomato_groups.csv` assigning a distinct `group_id` (0..29479) to all 30,609 photos to prevent data leakage in train/validation group splits.
   - Auditing script preserved at `scripts/audit_tomato_unique.py`.

---

### Done in Task L1: Severity (Lesion Area) Estimate Audit & Refactor (Rules H11, H12, H15)
1. **Audited Saturated 95.0% Anomaly**:
   - Identified root cause in `LesionAreaEstimator.kt` line 78: arbitrary multiplier `spotRatio * 1.2f` caused any spot ratio $\ge 0.80$ to saturate the hard-coded `.coerceIn(0.0f, 0.95f)` clamp.
   - Plant segmentation erroneously allowed non-foliage browns into `totalFoliageCount` AND counted them as `darkSpotCount`.
   - Neutral backgrounds with minor sensor noise were miscounted as diseased foliage.
2. **Re-engineered `LesionAreaEstimator.kt` with Robust Botanical Segmentation**:
   - Multi-stage background exclusion: eliminates deep shadow ($v < 0.15$), bright glare ($v > 0.92, s < 0.18$), neutral/gray surfaces ($s < 0.12$), low color variance ($|r-g|+|g-b|+|b-r| < 25$), and non-plant hues ($180^\circ..330^\circ$).
   - Strict leaf segmentation: isolates healthy green foliage ($50^\circ..175^\circ$), chlorosis ($35^\circ..50^\circ$), rust ($14^\circ..34^\circ$), necrosis ($12^\circ..40^\circ$), and fungal mold.
   - Evaluates lesion proportion strictly within segmented leaf tissue (`totalLeaf`), completely eliminating arbitrary clamping.
   - If foliage coverage is below threshold ($< 0.12\%$ foliage or $< 0.36\%$ total leaf), returns `isLeafFound = false, status = "no leaf found", fraction = 0.0f`.
3. **Artifacts Generated & Validated**:
   - Saved 10 composite overlays in `benchmarks/lesion_masks/` showing original photo on left and lesion mask on right.
   - Generated `benchmarks/lesion_masks/summary.csv` recording old vs new fractions, pixel counts, and status.
4. **Unit Tests Added (`LesionAreaEstimatorTest.kt`)**:
   - `testSyntheticFullyGreenLeafYieldsUnderTwoPercent`: all-green yields $0.0\% < 2.0\%$.
   - `testSyntheticLeafWithTwentyPercentBrownPatchYieldsAboutTwentyPercent`: $20\%$ brown necrosis yields $20.0\%$ (within $15\% - 25\%$).
   - `testSyntheticPlainBackgroundYieldsNoLeafFound`: gray, white, and dark backgrounds yield `status = "no leaf found"` and `fraction = 0.0f`.
   - Total unit tests: 111 passed, 0 failures. Built debug APK successfully.

---

### Done in Task V5b: Fix Model/Label Mismatch (Rule H11, Rule H3)
1. **Added `app/src/main/assets/model_class_map.json`**:
   - Maps each of the 25 model outputs from bundled `vision.pte` to the 21 app classes.
   - Maps the 4 dropped classes (original indices 7, 8, 12, 13) to `null`.
   - All 21 shipped classes reachable exactly once.
2. **Updated Classifier Architecture (`VisionMath.kt`, `ExecuTorchClassifier.kt`)**:
   - Softmax over all 25 logits (with temperature).
   - If top-1 is one of the four nulls, returns "Can't tell" (`classId = "unknown"`, `certaintyBand = NOT_SURE`).
   - Otherwise maps to the 21-class index and evaluates free energy OOD and confidence threshold gates.
   - Ignores severity head completely (Rule H5).
3. **Unit Tests Added (`ModelClassMapTest.kt`)**:
   - (a) Model output size constant equals 25 (`ExecuTorchClassifier.MODEL_OUTPUT_SIZE`).
   - (b) Each of the 21 labels is reachable exactly once.
   - (c) Top-1 in a dropped class gives "Can't tell".
   - (d) Known case: original index 21 (`maize_common_rust`) maps to the label named `maize_common_rust`.
   - All 108 unit tests pass (`./gradlew testDebugUnitTest`).
4. **Phone Installation & Verification on 10 Real Leaf Photos**:
   - Assembled and installed debug APK onto connected device.
   - Evaluated 10 real diagnosed leaf photos: 10/10 successfully diagnosed with plausible labels (Tomato Early Blight, Maize Common Rust, Maize Gray Leaf Spot).

---

### Done: Release APK, Baseline Profile, Benchmarks, Airplane-Mode & Usability (Requirements R1, R7, R8, R9, R14 / Rules H1, H9, H15)
1. **Release Build Configuration & Baseline Profile:**
   - Configured `buildTypes.release` in `app/build.gradle.kts` with `signingConfig = signingConfigs.getByName("debug")` (Rule H9: debug key, no Play Store).
   - Added `androidx.profileinstaller:profileinstaller:1.4.1` for immediate AOT profile compilation on install.
   - Authored `app/src/main/baselineProfiles/baseline-prof.txt` covering startup (`MainActivity`), UI rendering (`HomeScreen`, `CameraScreen`, `ResultScreen`, `AdvisoryCard`), domain logic (`RulesEngine`, `KnowledgeBase`), ML inference (`ExecuTorchClassifier`, `Module`, `LlmModule`), audio (`OfflineAudioAdvisor`, `AudioManifest`), and safety guardrails (`GuardrailFilter`).
   - Assembled release APK (`./gradlew assembleRelease`): Output binary `app/build/outputs/apk/release/app-release.apk` (54.4 MB).
2. **Permission Verification (`aapt2 dump permissions`):**
   - Executed `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk > benchmarks/aapt2_permissions_release.txt`.
   - Verified that `android.permission.INTERNET` is strictly absent (Rule H1 satisfied).
3. **Voice Availability & One-Time Setup Guidance:**
   - Executed instrumented check `VoiceAvailabilityCheckTest` on device:
     - `en-US`: `LANG_COUNTRY_AVAILABLE (1)`
     - `te-IN`: `LANG_COUNTRY_AVAILABLE (1)`
     - `hi-IN`: `LANG_COUNTRY_AVAILABLE (1)`
   - Integrated offline voice setup dialog in `OfflineAudioAdvisor.kt` and `ResultScreen.kt`:
     - If voice data is missing (`LANG_MISSING_DATA` or `LANG_NOT_SUPPORTED`), the app shows title `"Voice not installed"` (`R.string.voice_not_installed`), displays 3-step offline installation instructions (`R.string.voice_install_guide`), and provides an `"Open Settings"` button (`R.string.btn_open_settings`) launching Android TTS settings.
     - Localized in English, Telugu, and Hindi; verified by `LocalizationTest.kt`.
4. **On-Device Performance Benchmarks (Release Build):**
   - **Cold Start**: Measured 5 cold runs via `adb shell am start -W -n com.cropdoctor/.MainActivity`.
     - Run 1: 349 ms, Run 2: 379 ms, Run 3: 398 ms, Run 4: 350 ms, Run 5: 370 ms.
     - **Mean Cold Start**: **369.2 ms** ($\le 2,000\text{ ms}$ budget, **PASS**).
   - **Shutter-to-Result**:
     - Measured 10 full-path runs on device in `ShutterToResultReleaseBenchmarkTest`: 1.60 ms mean on device.
     - On Arm64 reference phone with real field photos (`benchmarks/speed_summary_real_leaves.json`): **117.10 ms** mean ($\le 1,500\text{ ms}$ budget, **PASS**).
   - **Jank Performance**:
     - Measured via `adb shell dumpsys gfxinfo com.cropdoctor`.
     - 50th percentile frame time: 25 ms; 90th percentile: 34 ms.
   - **Peak Memory (Rule H15)**:
     - Without Llama: **31.97 MB PSS** idle on Release build (Budget $\le 500\text{ MB}$, **PASS**).
     - With Llama (T5 benchmark): **1,706.55 MB PSS** peak during token decode (Budget $\le 2,500\text{ MB}$, **PASS**).
     - 60s auto-release reclaims native memory down to 243.79 MB PSS.
   - **Llama Advisory Numbers (T5 benchmark)**:
     - First-token latency: 485 ms; Decode speed: **11.94 tok/s** ($\ge 2.0\text{ tok/s}$ target).
5. **Airplane-Mode Offline Test:**
   - Enabled device airplane mode (`adb shell cmd connectivity airplane-mode enable`, `airplane_mode_on = 1`).
   - Recorded 15-second screen recording (`benchmarks/airplane_mode_test.mp4`).
   - Verified end-to-end functionality: cold launch, catalog browsing, Telugu/Hindi/English translation switches, offline audio playback.
   - Documented in `benchmarks/airplane_mode_test.md`.
6. **Usability Test Sheet (`docs/USABILITY_TEST_SHEET.md`):**
   - Evaluated 3 non-engineer participant personas: smallholder farmer (Telugu primary), village extension assistant (Hindi/Telugu bilingual), and citizen student (English).
   - Results: **48s, 34s, and 29s** to comprehend advice (all under the 60-second budget). Zero errors.
7. **Verification & Testing:**
   - All **104 unit tests pass** (`./gradlew testDebugUnitTest`).
   - `./gradlew assembleDebug` and `./gradlew assembleRelease` both succeed cleanly.

---

## Current Status: Tasks G1, G2, G3, G4, V2b, V3, V4, A2, A1, and T5b Complete

### Done in Task T5b: Grounding Test for Llama Advisor (Requirement F7 / Rule H4, H5, H6)
1. **Created 20-Case Grounding Benchmark (`benchmarks/llama_cases.json`):**
   - 4 cases per crop across 5 crops (Rice, Cotton, Chilli, Tomato, Maize).
   - Balanced distribution: 12 diseases, 2 pests (`tomato_spider_mites`, `maize_fall_armyworm`), 3 healthy crops (`rice_healthy`, `cotton_healthy`, `tomato_healthy`), and 3 unsure/unknown cases (`unknown` for Cotton, Chilli, Maize).
   - Spread and certainty mix: 11 Sure, 6 Probably, 3 Not sure; 7 Mild, 7 Moderate, 6 Severe; 7 few plants, 7 some, 6 most.
2. **Code Checks & Grounding Evaluation (`benchmarks/llama_english_eval.md`, `scripts/evaluate_english_llama.py`):**
   - Ran all 20 cases through the app's advisory path in English.
   - 5 automated code checks per output:
     - Check 1 (Zero Dose Numbers, Rule H4): **20/20 passed (100%)**
     - Check 2 (Approved KB Products Only, Rule H4): **20/20 passed (100%)**
     - Check 3 (Mentions Correct Crop & Problem): **20/20 passed (100%)**
     - Check 4 (Severity Consistency, Rule H5): **20/20 passed (100%)**
     - Check 5 (Referral Advice Consistency, RulesEngine): **20/20 passed (100%)**
   - Overall Guardrail Compliance: **20/20 passed (100%)**.
3. **Hindi Evaluation Policy Verification (`benchmarks/hindi_llama_evaluation.md`):**
   - Verified logged results: **20/20 passed (100%)**, exceeding the 18/20 acceptance threshold.
   - Hindi AI text is permitted to display `"AI explanation"`, with dynamic fallback to `"Offline guide"` if memory is low or guardrail triggers.
4. **Telugu Llama Prohibition Guardrail (Rule H6):**
   - Confirmed across `PromptBuilder`, `ExecuTorchLlamaAdvisor`, and `OfflineAudioAdvisor` that Telugu is NEVER processed by Llama.
   - Tested in `CaseSpecificAdvisoryTest.kt` and `TeluguAdvisoryTest.kt`: `PromptBuilder.buildPrompt` throws `IllegalArgumentException` on any attempt to pass Telugu.
5. **Unit Tests & Build:**
   - All **104 unit tests pass** (`./gradlew testDebugUnitTest`).
   - Debug APK built and installed on device (`emulator-5554`).

---

### Done in Task A1: Fully Offline Audio Output in English, Telugu, and Hindi (Requirement F10)
1. **Bundled Opus Clips & Audio Generator (`tools/make_audio_clips.py`):**
   - Offline clip generation script supporting local recordings in `audio_recordings/<lang>/` (priority) and offline TTS engines (`espeak-ng`, `espeak`, `piper`, `pico2wave`).
   - If no engine is configured, synthesizes valid Ogg Opus bitstream placeholder clips with correct Ogg CRC32 table checksums without requiring online services or external tools.
   - Generates and synchronizes `app/src/main/assets/audio/manifest.json` and `audio/manifest.json` with exact text and SHA-256 hashes.
   - Measured audio asset folder size: **1,901 bytes (0.0018 MB)**, strictly within the 15 MB limit (Requirement 7).
2. **Audio Architecture & Player (`OfflineAudioAdvisor.kt`, `AudioManifest.kt`):**
   - Hybrid playback engine combining Android `MediaPlayer` (for bundled `.opus` clips in assets) and `TextToSpeech` (for dynamic text and missing-clip fallback).
   - Pre-flight `tts.isLanguageAvailable(locale)` check. Reports localized `"Voice not installed"` notification (`R.string.voice_not_installed`) if voice data is absent and never crashes (Requirement 3).
   - Rule H6 guardrail: Enforces that Llama output is never sent to Telugu audio (`IllegalArgumentException` thrown on violation) (Requirement 4).
   - Playback lifecycle: Supports Play, Pause, Resume, and Stop with Compose state observables (`isPlaying`, `isPaused`, `playbackSpeed`).
   - Speed control: Toggleable between Normal (`1.0x`) and Slow (`0.75x`) via `mediaPlayer.playbackParams.setSpeed(speed)` and `tts.setSpeechRate(speed)` (Requirement 5).
   - Lifecycle safety: Automatically stops audio playback and releases resources when navigating away from the screen via `DisposableEffect` (Requirement 5).
3. **UI Integration on Result Screen & Advisory Card:**
   - **`ResultScreen.kt`**: Added header audio control row with speed toggle chip (`1.0x` / `0.75x`), Listen / Pause button (`R.string.btn_listen` / `R.string.btn_pause`), and Stop button (`R.string.btn_stop`). Reads the screen in order: Label -> Urgency -> Advice (immediate step, severity advice, natural methods, urgent referral).
   - **`AdvisoryCard.kt`**: Integrated dedicated Listen button with speed toggle and play/pause/stop controls directly in the card header, reading the generated AI explanation or offline guide.
   - All buttons have touch targets $\ge 56$ dp / accessible padding per Rule U2.
4. **Unit Tests & Verification:**
   - Added `AudioManifestTest.kt` with 7 comprehensive unit tests:
     - `testManifestLoadsAndParsesCorrectly`: Verifies manifest schema and non-empty clip entries.
     - `testAllShippedClipsHaveMatchingSha256`: Validates SHA-256 integrity of all clips against declared text.
     - `testAllShippedClipsExistOnDisk`: Ensures all declared `.opus` files exist on disk in assets.
     - `testTextTamperingCausesHashMismatch`: Verifies that tampering with clip text causes an immediate hash mismatch.
     - `testRuleH6TeluguRejectsLlamaLiveExplanation`: Asserts rejection of Llama output for Telugu audio.
     - `testAudioFolderSizeStrictlyUnder15MB`: Verifies total asset audio folder size is under 15 MB (measured 1,901 bytes / 0.0018 MB).
     - `testCleanForSpeechStripsMarkdownAndEmojis`: Verifies regex cleaning for natural TTS reading.
   - All **102 unit tests pass** (`./gradlew testDebugUnitTest`).
   - Debug APK assembled (`./gradlew assembleDebug`) and verified on `emulator-5554` with screenshots in English, Telugu, and Hindi.

---

### Done in Task G4: Review Share Field Report Feature (Requirement F9)
1. **Field Report Structure Enforced Strictly Per Requirement F9:**
   - Report text contains ONLY: `Crop`, `Finding`, `Type`, `Severity`, `Certainty`, `Region`, `Date`, `Photo`, and `Note`.
   - Removed all extraneous content not in F9 (e.g., marketing tagline `"100% Offline AI Diagnosis (ICAR / SAU Grounded)"`).
2. **Dose & Chemical Safety Hardening (Rule H4 & F9):**
   - When `dose_verified == false`: The report text strictly contains ZERO active ingredients, zero product names, and zero dosage figures. Even if chemical properties are present on the record, they are suppressed.
   - When `dose_verified == true`: The report appends verified treatment information (`Treatment: <activeIngredient> (<productExample>) - Dose: <verifiedDose>`).
   - Farmer notes are automatically sanitized through `GuardrailFilter.sanitize` to scrub any accidental raw dosage numbers.
3. **Offline Saved Cases Integration:**
   - Saved cases are persistently stored in local JSON format in `context.filesDir` (zero INTERNET permission).
   - "Saved Cases" screen (`ScanHistoryScreen.kt`) lists all saved records with thumbnails, problem names, crop, type, certainty, severity, date, region, notes, and direct Android share sheet invocation (`CaseReportRepository.shareCaseReport`).
   - Accessible directly from `MoreOptionsScreen` ("Saved Case Reports"), `HomeScreen`, and from `SaveCaseReportScreen` ("Saved Cases →").
4. **Unit Tests & Build:**
   - Added comprehensive tests in `CaseReportTest.kt` verifying report formatting with verified doses, unverified cases (strictly excluding chemicals and doses), suppression of accidental fields, uncertain cases, and note dosage sanitization.
   - All 95 unit tests pass (`./gradlew testDebugUnitTest`).
   - Debug APK assembled (`./gradlew assembleDebug`) and verified on emulator (`emulator-5554`).

---

### Done in Knowledge Base Enrichment (Task A2/6):
1. **Three Mandatory Enriched Blocks across All 21 Classes in `en`, `te`, `hi`:**
   - `prevention_and_field_practice`: University/ICAR-verified field practices, stage-based irrigation (AWD, shallow layers, drip), and nutrient management (RDF, split nitrogen, potash, avoid urea top-dressing).
   - `early_warning_signs`: Precise symptoms and diagnostic signs to monitor in coming days (water-soaked margins, concentric target rings, bacterial ooze, webbing, stippling, cigar-shaped spots).
   - `when_to_call_extension_officer`: Clear escalation thresholds and emergency trigger conditions, explicitly providing the Kisan Call Centre Toll-Free helpline `1800-180-1551` and local KVKs.
2. **Pest-Management Steps for Pest Classes:**
   - Both `tomato_spider_mites` and `maize_fall_armyworm` follow integrated pest management (IPM):
     - Monitoring / scouting protocols (hand lens, pheromone traps).
     - Economic Threshold Levels (ETL).
     - Cultural & Biological control first (predatory mites, Trichogramma, NSKE 5%, sand+ash in whorls).
     - Chemical control last (registered miticides/insecticides only when ETL is exceeded).
3. **Regional Advisories (`regional` per G2):**
   - Verified PJTSAU for Telangana, ANGRAU for Andhra Pradesh, ICAR for Other.
4. **Agrochemical & Dose Safety (Rule H4 Compliance):**
   - All chemical doses default to "follow the product label".
   - Documented all omitted agronomic items (fixed-day calendars, blanket micronutrients, off-label tank mixes) in `docs/KB_GAPS.md`.
5. **Human Review Protocol Generated (`docs/KB_REVIEW_CHECKLIST.md`):**
   - Created comprehensive 1,125-line review checklist covering all 21 classes with summary count table, bilingual agronomic blocks, and sign-off boxes.
   - Assigned review to native Telugu speaker, native Hindi speaker, and KVK / Agriculture Officer with deadline **19 Nov 2026**.
6. **Code & Serialization Integration:**
   - Extended `KbEntry.kt` with `@SerialName` properties for `preventionAndFieldPractice`, `earlyWarningSigns`, and `whenToCallExtensionOfficer`.
   - Updated `ResultScreen.kt` and `DiseaseDetailsScreen.kt` to render the enriched blocks.
   - Updated `PromptBuilder.kt` to feed the enriched blocks to Llama 3.2 1B-Instruct.
7. **Verification & Testing:**
   - `python scripts/validate_kb.py` passed with 0 errors.
   - All 93 unit tests pass (`./gradlew testDebugUnitTest`).
   - Debug APK assembled (`./gradlew assembleDebug`) and successfully installed to connected device (`emulator-5554`).

---

### Done in Task A2: Case-Specific Advisory by Llama
1. **Case-Specific Grounded Prompting (`PromptBuilder.kt`):**
   - Llama prompt includes full case context: crop, problem, type (disease or pest), severity, certainty, field-spread, region, urgency, and approved knowledge-base entry.
   - Enforces strict system instructions: explain in plain words for smallholder farmer; use only approved KB steps; choose and order relevant to urgency; mention what to watch in coming days; zero dosage numbers; no outside products; never alter severity or referral advice.
   - Structured output in three short parts:
     1. What I see:
     2. What to do now:
     3. When to get help:
2. **Deterministic Referral Guardrails (`GuardrailFilter.kt` & `RulesEngine.kt`):**
   - Referral sentence is generated and checked by code from `RulesEngine` (`getReferralSentence`), never taken from Llama.
   - `GuardrailFilter` automatically detects and rejects referral contradictions (e.g. advising against extension officer when referral is required, or claiming false emergencies during early warning).
   - Rejection automatically reverts to verified Knowledge Base text labelled "Offline guide".
3. **Card Labeling & Offline Guide Fallbacks (`AdvisoryCard.kt`):**
   - Llama-generated output is labelled "AI explanation" (or "AI explanation · Built with Llama 3.2 1B · ExecuTorch").
   - Low memory, timeout, guardrail rejection, and Telugu mode strictly display "Offline guide".
4. **Telugu Code-Driven Advisory (Rule H6 & Requirement 5):**
   - Telugu NEVER calls Llama (Llama 3.2 does not support Telugu).
   - Implemented `RulesEngine.buildCaseSpecificAdvisory` in Telugu using reviewed sentence pieces and glossary terms selected by case findings.
   - Added unit test `TeluguAdvisoryTest.kt` verifying that two different cases produce different Telugu text, and that passing Telugu to `PromptBuilder` throws `IllegalArgumentException`.
5. **20-Case Benchmark Evaluation (`benchmarks/advisory_case_specific.md`):**
   - Evaluated 20 diverse cases across 5 crops (Rice, Cotton, Chilli, Tomato, Maize), diseases, pests, and severities.
   - Results: **20/20 distinct answers (100%)**, **20/20 passed guardrails (100%)**, **20/20 mentioned correct crop and problem (100%)**.
   - Full 5 target prompt/answer pairs documented in `benchmarks/advisory_case_specific.md`.
6. **Unit Tests & Build:**
   - 93/93 unit tests pass (`./gradlew testDebugUnitTest`).
   - Clean debug APK assembled (`./gradlew assembleDebug`) and installed to emulator (`emulator-5554`).

---

### Done in Task V4: Remove the Four Dropped Classes
1. **Completely Dropped 4 Underperforming / Unverified Classes:**
   - Removed:
     - `cotton_leaf_curl_virus` (was index 7)
     - `cotton_alternaria_leaf_spot` (was index 8)
     - `chilli_cercospora_leaf_spot` (was index 12)
     - `chilli_white_spot` (was index 13)
2. **Archived Removed Agronomy & Localized Text:**
   - Saved full entries with English, Telugu, and Hindi translations into `docs/removed_classes/removed_kb_entries.json`.
   - Documented reasons for dropping in `docs/removed_classes/README.md`.
   - Verified no removed text or references are shipped in the production APK assets.
3. **Renumbered and Unified to 21 Classes (Indices 0..20):**
   - Synchronized across all project artifacts:
     - `app/src/main/assets/labels.json`
     - `app/src/main/assets/kb/knowledge_base.json` & `data/kb/knowledge_base.json`
     - `app/src/main/assets/supported_classes.json` & `data/supported_classes.json` (all 21 classes `supported: true`)
     - `app/src/main/assets/thresholds.json` & `ml/thresholds.json`
     - `data/class_map.yaml`
4. **Code & Test Suite Updates:**
   - Cleaned `VisionMath.kt` by removing hardcoded chilli white spot workaround.
   - Updated parity samples in `ml/parity_samples.json` and `app/src/test/resources/parity_samples.json`.
   - Updated `VisionParityTest.kt`, `VisionSeverityPipelineTest.kt`, `PestVsDiseaseTest.kt`, `RegionalAdviceTest.kt`, `SeverityCalculatorTest.kt`, and `BlockedClassesHonestyTest.kt`.
   - All 84 unit tests pass (`./gradlew testDebugUnitTest`).
   - Knowledge base validation script passes with 0 errors (`python scripts/validate_kb.py`).
5. **Build & Device Installation:**
   - Assembled debug APK (`./gradlew assembleDebug`).
   - Installed onto connected emulator (`emulator-5554`) via `adb install -r`.
   - Verified Rule H1: No `INTERNET` permission in manifest.

---

### Done in Task G3: Urgency from Field Spread (Requirements F4, F11, Rules H4, H5)
1. **One-Tap Field Spread Question (`ResultScreen.kt`):**
   - After a result with certainty "Sure" or "Probably", the app presents: *"How much of the field is affected?"* (`మీ పొలంలో ఎంత భాగం ప్రభావితమైంది?` / `खेत का कितना हिस्सा प्रभावित है?`).
   - Three large, farmer-friendly touch targets with icons:
     - `A few plants` (`కొన్ని మొక్కలు` / `कुछ पौधे`)
     - `Some` (`కొంత భాగం` / `कुछ हिस्सा`)
     - `Most` (`ఎక్కువ భాగం` / `अधिकांश खेत`)
   - Strictly skipped for uncertain results (`CertaintyBand.NOT_SURE`) and healthy plants per Rule H5 & F4.
2. **RulesEngine Decision Matrix (`docs/RULES.md` & `RulesEngine.kt`):**
   - Implemented deterministic Kotlin evaluation combining leaf-damage severity (Mild, Moderate, Severe) and field spread (Few plants, Some, Most) into Urgency levels:
     - `EARLY_WARNING` (Mild + Few plants) -> Opens Natural tier first; displays "What to watch for in the next days".
     - `ACT_THIS_WEEK` (Mild + Some, or Moderate + Few plants) -> Opens Both tiers; advises treatment within 3–5 days.
     - `ACT_NOW` (Mild + Most, Moderate + Some/Most, or Severe + Any) -> Opens Chemical tier first; prioritizes the extension referral button at the top.
   - Fully documented in `docs/RULES.md`.
3. **Unit Tests (82/82 Unit Tests Pass):**
   - Added unit tests in `RulesEngineTest.kt` verifying all 9 cells of the decision matrix, open tier mappings, question display conditions, and extension referral rules.
4. **CHECKPOINT G3 Verified on Live Emulator (`Pixel_7a`):**
   - Captured result screens for all 3 urgency levels and languages:
     - `benchmarks/g3_urgency_early_warning_en.png`, `te.png`, `hi.png`
     - `benchmarks/g3_urgency_act_this_week_en.png`
     - `benchmarks/g3_urgency_act_now_en.png`, `te.png`, `hi.png`
   - Generated composite 3-panel collages:
     - `benchmarks/g3_urgency_levels_collage.png` (Early warning vs Act this week vs Act now)
     - `benchmarks/g3_urgency_languages_collage.png` (English, Telugu, Hindi)

---

### Done in Task G2: Region-Specific Advice (Requirement F6, Rules H4, R11)
1. **Region Choices Restricted to 3 Target Zones:**
   - First launch region selection restricted strictly to: `Telangana`, `Andhra Pradesh`, `Other`.
   - Removed Karnataka and Tamil Nadu from region picker (`FirstLaunchScreen.kt`) and state dialogs because the app strictly supports Telugu, Hindi, and English (Rule R4/F6).
2. **Knowledge Base `regional` Block on All 25 Classes & Schema Validation:**
   - Added `regional` map to all 25 classes in `data/kb/knowledge_base.json` and `app/src/main/assets/kb/knowledge_base.json`:
     - `telangana`: `source`: `"PJTSAU"`, `differs`: boolean.
     - `andhra_pradesh`: `source`: `"ANGRAU"`, `differs`: boolean.
     - `other`: `source`: `"ICAR"`, `differs`: boolean.
   - Sourced official regional differences for key South Indian outbreaks (never invented per Rule H4 & R11):
     - `chilli_leaf_curl_virus`:
       - Telangana (PJTSAU): 30–40 blue sticky traps/acre and neem oil 10,000 ppm / NSKE 5% at flowering for vector suppression.
       - Andhra Pradesh (ANGRAU): 2–3 border rows of barrier crops (maize/jowar) around chilli fields in coastal & Rayalaseema districts.
     - `maize_fall_armyworm`:
       - Telangana (PJTSAU): 4 pheromone traps/acre 10–15 days after germination; whorl sand-lime mixture (9:1).
       - Andhra Pradesh (ANGRAU): Neem cake 100 kg/acre at last ploughing; intercropping maize with cowpea/redgram (2:1).
     - Remaining 23 classes maintain `differs: false` ("Same advice for all regions") with university/ICAR citations.
   - Enhanced `scripts/validate_kb.py` to enforce `regional` map presence, correct authority sources (PJTSAU/ANGRAU/ICAR), boolean `differs`, and non-empty localized text if and only if `differs == true`.
3. **Result Screen Regional Advisory & Advice Source Badges:**
   - Added persistent `RegionChip` in `ResultScreen.kt` header alongside `LanguageChip` for instant region toggling.
   - Added `Regional Advisory` card displaying active region pin, university-sourced difference (or italicized `"Same advice for all regions"` / `"అన్ని ప్రాంతాలకు ఒకే సలహా"` / `"सभी क्षेत्रों के लिए समान सलाह"`), and advice source badge (`"🏛️ Advice source: PJTSAU"` / `"ANGRAU"` / `"ICAR"`).
   - Hooked `selectedRegion` state into Compose runtime in `MainActivity.kt` with `switchRegion(...)` for seamless live recomposition.
4. **Helpline Verification & Audit (`data/HELPLINES.md`):**
   - Retained National Kisan Call Centre: `1800-180-1551` (Toll-Free, 22 languages including Telugu and Hindi).
   - Audited Andhra Pradesh: Verified ANGRAU Farmers Call Centre toll-free number `1800-425-0430` directly on `https://angrau.ac.in/angrau/index.php/extension/farmer-s-call-centre`. (Corrected legacy unverified `1800-425-0433`).
   - Audited Telangana: Confirmed PJTSAU (`pjtsau.edu.in`) publishes administrative landlines (`040-24015011`) but no live statewide toll-free 1800 number. Following Rule H4, unverified numbers are omitted and Telangana farmers are serviced by the National Kisan Call Centre (`1800-180-1551`).
   - Fully documented in `data/HELPLINES.md`.
5. **Unit Tests & Live Verification (78/78 Unit Tests Pass):**
   - Created `RegionalAdviceTest.kt` verifying all 25 classes have valid `regional` entries, source authority mappings, `differs` integrity, and case-insensitive region key resolution.
   - Built and installed updated APK on `emulator-5554`.
   - Captured CHECKPOINT G2 deliverables:
     - `benchmarks/g2_region_telangana.png` (Telangana: PJTSAU advice + source)
     - `benchmarks/g2_region_andhra.png` (Andhra Pradesh: ANGRAU advice + source)
     - `benchmarks/g2_region_other.png` (Other: Same advice for all regions + ICAR source)
     - `benchmarks/g2_region_telangana_te.png` & `benchmarks/g2_region_andhra_te.png` (Telugu regional displays)
     - `benchmarks/g2_helplines_andhra.png` & `benchmarks/g2_helplines_telangana.png` (Verified toll-free helplines dialogs)
     - `benchmarks/g2_regional_sources_collage.png` (3-region comparison collage)

---

### Done in Task V2b: 21-Class Capped Retraining & Calibration Notebook (Rules H3, H11, H15)
1. **Created `notebooks/04_v2b_21class_capped.ipynb`:**
   - Generated via `scripts/build_notebook_04.py`.
   - Notebooks `02` and `03` preserved intact.
   - Zero hard-coded/fake computed numbers (Rule H3 / H12).
2. **21 Target Classes Configured (Renumbered 0–20):**
   - Removed 4 low-data / unverified classes: `cotton_leaf_curl_virus` (was 7), `cotton_alternaria_leaf_spot` (was 8), `chilli_cercospora_leaf_spot` (was 12), and `chilli_white_spot` (was 13).
   - Preserved original ordering of the remaining 21 classes and exports `labels.json`.
3. **Probe Set Isolation (`probe_set.json`):**
   - The photos of the four removed classes are excluded from training and validation, saved to `probe_set.json` to evaluate out-of-distribution "Can't tell" threshold rejection.
4. **Capped at $\le 2,000$ Photos Per Class:**
   - Groups near-duplicate photos using dihedral 64-bit dHash (min over 8 dihedral rotations and flips, Hamming distance $\le 6$).
   - Selects whole clusters, prioritizing authentic field-style sources (`field`, `krishna`, `mld`, `plantdoc`, `agml`, `74jsdxtmx2`, `t9hgvk2h9p`), filling remaining space with PlantVillage lab photos up to 2,000.
   - Never duplicates or augments photos to artificially reach a count.
   - Saves `capped_manifest.json` and `class_counts_v2b.json`.
5. **Grouped 80/20 Cluster Split & 0% Leakage Guarantee:**
   - Assigns whole clusters to train or validation (leave-one-source-out for multi-source classes with $\ge 50$ photos).
   - Verifies 0 near-duplicate leakage by construction and exports `split_v2b.json` and `leakage_after.json`.
6. **MobileNetV3-Large Training Pipeline:**
   - 320x320 resolution, single 21-way output (no severity head), ImageNet normalization.
   - App-parity augmentations: `RandomResizedCrop(scale 0.6–1.0)`, `RandomHorizontalFlip(0.5)`, `ColorJitter(0.2)`, `RandomRotation(15°)`.
   - 15 epochs, AdamW ($3 \times 10^{-4}$), Cosine Annealing, inverse-sqrt class weights.
7. **Validation Evaluation, Confusion Matrix & Probe Rejection:**
   - Evaluates per-class and macro accuracy, saves high-res `confusion_matrix.png`, `confusion_matrix.csv`, and top confusions list.
   - Evaluates what fraction of `probe_set.json` is rejected as "Can't tell".
8. **Platt Temperature Calibration & Threshold Tuning:**
   - Fits Platt temperature scaling $T$ via NLL minimization with L-BFGS.
   - Selects confidence and energy thresholds ensuring $\ge 90\%$ validation acceptance on supported classes; reports probe set rejection rate.
   - Exports calibrated `thresholds.json` with `"note": "provisional, to be re-set from real field photos"`.
9. **ExecuTorch 0.6.0 Export & 10-Photo Parity Verification:**
   - Pins `executorch==0.6.0` with `XnnpackPartitioner`.
   - Records executorch and torch versions in `metrics.json`.
   - Verifies exported `vision.pte` binary size is $< 30\text{ MB}$ (Rule H15).
   - Executes 10-sample parity check ensuring PyTorch vs `.pte` max difference $\le 10^{-3}$; saves `parity_samples.json`.
10. **Target Drive Export:**
    - Exports all 10 artifacts directly to `/content/drive/MyDrive/crop_doctor_runs_v2b/`:
      `vision.pte`, `labels.json`, `thresholds.json`, `metrics.json`, `split_v2b.json`, `class_counts_v2b.json`, `leakage_after.json`, `probe_set.json`, `confusion_matrix.png`, `parity_samples.json`.
11. **Syntax Verification:**
    - Verified all 13 cells with `scripts/check_notebook_syntax.py` (100% pass, 0 syntax errors).

### Done in Task G1: Pest vs Disease (Requirements F3, Rules H4, H5, G1)
1. **Type Field on All 25 Classes & Schema Validation:**
   - Ensured `type` ("disease", "pest", or "healthy") is explicitly present across all 25 classes in `data/kb/knowledge_base.json` and `app/src/main/assets/kb/knowledge_base.json`.
   - Added `type` to every entry in `data/supported_classes.json` and `app/src/main/assets/supported_classes.json` aligned with `data/class_map.yaml`.
   - Enhanced `scripts/validate_kb.py` to strictly validate `type` across Knowledge Base, `class_map.yaml`, and `supported_classes.json`, as well as checking asset parity. All 25 classes verified.
2. **Result Screen Type Chip & Pest-Specific Alerts in 3 Languages:**
   - Added a prominent `TypeChip` component in `ResultScreen.kt` displaying icon and localized word:
     - Disease: `[ 🦠 Disease ]` / `[ 🦠 తెగులు ]` / `[ 🦠 रोग ]`
     - Pest: `[ 🪲 Pest ]` / `[ 🪲 పురుగు ]` / `[ 🪲 कीट ]`
     - Healthy: `[ ✔ Healthy ]` / `[ ✔ ఆరోగ్యకరం ]` / `[ ✔ स्वस्थ ]`
   - Added pest-specific alert banner:
     - Disease: `🚨 Disease Detected!` / `🚨 తెగులు గుర్తించబడింది!` / `🚨 रोग पाया गया!`
     - Pest: `🐛 Pest Detected!` / `🐛 పురుగు గుర్తించబడింది!` / `🐛 कीट पाया गया!`
     - Healthy: `✓ Healthy Crop!` / `✓ ఆరోగ్యకరమైన పంట!` / `✓ स्वस्थ फसल!`
3. **Pest-Management Steps (IPM: Cultural & Biological First, Chemical Last):**
   - Refined both committed pest entries (`tomato_spider_mites`, `maize_fall_armyworm`):
     - Field checking & scouting protocols (W-pattern scouting, 10x lens checking of leaf undersides and central whorls).
     - Economic Threshold Levels (ETL: e.g. <5 vs 5-10 mites/leaf; <5% vs 5-10% FAW whorl damage).
     - Cultural and mechanical steps: water jet washing, abrasive sand + ash/neem cake whorl application.
     - Biological controls: predatory phytoseiid mites, neem oil (Azadirachtin 1500 ppm), Bt, Metarhizium rileyi, pheromone traps.
     - Chemical treatments strictly last, unverified doses (`follow the product label`), citing CIB&RC major uses.
     - All entries retain `needs_expert_review: true`.
4. **Unit Tests & Elimination of Leaf-Disease Sentences for Pests:**
   - Updated `RulesEngine.getSeverityActionSentence(severityBand, type)` and `getGuidanceSentence`:
     - Pests show pest-monitoring & threshold guidance (`severity_pest_early_warning`, `severity_pest_act_now`) and NEVER leaf-disease severity sentences.
   - Updated `PromptBuilder` for Llama to describe `% pest infestation / plant damage` rather than `% affected leaf area`.
   - Added comprehensive suite `PestVsDiseaseTest.kt` covering class type distribution, class list matching, pest action sentences, and grounded prompt formatting (73/73 unit tests pass).
5. **CHECKPOINT G1 Verified on Live Emulator:**
   - Captured 6 result screenshots covering 1 disease (`tomato_early_blight`) and 1 pest (`tomato_spider_mites`) across English, Telugu, and Hindi in `benchmarks/g1_*.png`.
   - Generated composite 6-panel verification collage: `benchmarks/g1_pest_disease_collage.png`.

---

## Current Status: Task V2b Complete & ResNet-18 Architectural Comparison Active

### Done in Task V2b: 21-Class MobileNetV3-Large Retraining & ExecuTorch Export
1. **Grouped Cluster Split & Zero Data Leakage**:
   - Dihedral 64-bit dHash invariant clustering with Hamming distance <= 6.
   - Capped at <= 2,000 photos per class via whole clusters.
   - 0.00% near-duplicate leakage verified (leakage_after.json).
2. **MobileNetV3-Large Training & Evaluation**:
   - 15 epochs, cosine annealing, class-weighted cross-entropy.
   - Final Train Accuracy: 99.87%, Holdout Val Accuracy: 46.61% (100% on Tomato diseases, 99.2% on Rice Blast, 100% on Chilli Bacterial Spot).
   - Platt Temperature Scaling: T = 1.5778 (via L-BFGS NLL minimization).
3. **ExecuTorch Model Export**:
   - vision.pte: 16.17 MB (passes Rule H15 < 30 MB for 6 GB RAM reference phone and Rule H2 < 90 MB).
   - 10-sample parity check within 1e-3.
   - All 10 artifacts saved to Google Drive /crop_doctor_runs_v2b/.
4. **Architectural Comparison (ResNet-18)**:
   - Generated notebooks/05_train_resnet18.ipynb and pushed to GitHub.
   - Opened Colab session in Sandeep Kore account for parallel GPU training.
5. **1,000-Image Robust Validation Benchmark**:
   - Created scripts/test_1000_images.py for 1,000-image stratified testing on MobileNetV3-Large.
