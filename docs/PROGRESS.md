# Project Progress - Crop Doctor Offline

## Current Status: T1 (Truth Cleanup) Completed - CHECKPOINT 1

### Done (T1: Truth Cleanup)
- **Git Ignore & Repository Hygiene:** Added `.gitignore` ignoring build artifacts, `*.pte` (except bundled vision weights if <=90MB), tokenizer files, keystores, `local.properties`, and datasets. Verified 0 large files tracked.
- **Strict Classifier Architecture:**
  - Defined clean `interface Classifier`.
  - Re-implemented `FakeClassifier` returning deterministic sample predictions tagged with `isDemoMode = true`.
  - `ResultScreen` displays prominent warning `"⚠️ DEMO MODE - NOT A REAL DIAGNOSIS"` in debug builds when fake classifier is used.
  - **Completely deleted `SmartVisualClassifier` from the codebase.**
  - `ExecuTorchClassifier` returns `"Not sure - ask an extension officer"` when `vision.pte` is missing or fails to load; never fabricates a diagnosis.
  - Extracted color analysis solely to `LesionAreaEstimator` for lesion area fraction estimation (Rule H11) with unit tests (`LesionAreaEstimatorTest`).
- **Eliminated Hard-Coded Numbers (Rule H12):**
  - Removed static `"~114ms / ~138MB / 0 KB Network"` benchmark HUD card from `ResultScreen`.
  - Replaced with live measurements (inference timer via `SystemClock.elapsedRealtime()`, memory via `Runtime.getRuntime()`).
  - Added hidden Judge Benchmark Panel dialog accessible via long-press on the result title.
  - Removed artificial confidence clamp `coerceIn(60, 99)`. Confidence now shows real un-clamped percentage with "Sure / Probably / Not sure" wording.
  - Removed all "100% accurate/verified" wording across app code.
- **Soft Warning for `LeafValidator` (Rule H11 & R2):**
  - `LeafValidator` changed to a SOFT warning: `"This may not be a crop photo"` with two buttons: `"Take again"` and `"Use this photo anyway"`.
  - Recognizes leaves, stems, fruits (tomatoes, chillies), cotton bolls, pests/insects, and necrotic brown leaves.
  - Added 9 unit tests in `LeafValidatorTest` verifying synthetic images for all agricultural subjects never get blocked, while human hands and desks trigger soft warning.
- **Fixed Failing Guardrail Test & Release Build:**
  - Updated `GuardrailFilter` regex to scrub `"of water"` dosage variations (`2.5 ml/L of water`). `GuardrailFilterTest` passes 100%.
  - Fixed invalid `companion object *;` in `proguard-rules.pro`.
  - Resolved Daemon JDK foojay toolchain issue by removing `gradle-daemon-jvm.properties` and using system JDK 17.
  - Both `./gradlew assembleDebug` and `./gradlew assembleRelease` pass with 0 errors.
  - All 23 unit tests pass across 5 test classes.
- **Rewrote `docs/REQUIREMENTS.md`:**
  - Completely purged references to non-existent files. Every row now strictly states either `proven by: <real file>` or `NOT YET PROVEN`.

### Next
- Proceed to T2 (or next phase as directed by user).

### Blocked
- None. Paused at CHECKPOINT 1.

### Needed from User
- Review CHECKPOINT 1 results (git log, unit test pass count, screenshots/evidence).
- Provide approval to proceed to next task.
