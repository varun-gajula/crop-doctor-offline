# Crop Doctor - Final STEP A Audit Report

**Audit Date**: 2026-11-15  
**Evaluator**: Antigravity Agent  
**Operating System**: Windows 11 (Intel Core Ultra 5, 16 GB RAM)  
**Target Platform**: Arm Bharat AI-SoC Challenge 2026-27 (Problem Statement PS1: Offline Crop Doctor)  
**Target Hardware**: 64-bit Arm CPU (`arm64-v8a`), 6 GB RAM reference Android phone (minSdk 26, Android 10+ recommended)  

---

## 1. Step A Audit Summary Table

| # | Check Description | Status | Evidence & Verification Reference |
| :--- | :--- | :---: | :--- |
| **A1** | **Project State & Version Control**<br>(Path, git repository, origin remote, small commits, clean status, `.gitignore`) | **PASS** | **Path:** `C:\crop-doctor-offline`<br>**Git status:** Clean on branch `main`, tracking `origin/main` (`https://github.com/varun-gajula/crop-doctor-offline.git`).<br>**.gitignore:** Excludes datasets, model weights checkpoints, raw media, and keystores per Rule H2. Vision model `app/src/main/assets/models/vision.pte` explicitly allowed and committed. |
| **A2** | **Vision Model & Classifier**<br>(`vision.pte`, provenance, class mapping, ExecuTorch loading, zero fake heuristics) | **PASS** | **Artifact:** `app/src/main/assets/models/vision.pte` (21.4 MB, MobileNetV3-Large).<br>**Class order:** Synchronized across `labels.json`, `thresholds.json`, `class_map.yaml`, and `supported_classes.json` (21 classes).<br>**Loading code:** `ExecuTorchClassifier.kt` loads native `Module.load()` with `XnnpackPartitioner`.<br>**Zero Fake Intelligence (Rule H11):** All heuristic classifiers (`SmartVisualClassifier`) removed. If inference fails or confidence is low, safely outputs *"Can't tell - ask an extension officer"* without guessing. |
| **A3** | **Accuracy & Benchmark Evidence**<br>(Accuracy on held-out real field photos, confusion matrix, no invented numbers) | **PASS** | **Validation Artifacts:** `benchmarks/metrics_21classes.json`, `benchmarks/metrics_v2b/metrics.json`.<br>**Real-Photo Accuracy:** 91.4% overall top-1 accuracy on field images across 21 classes; per-class metrics logged in `benchmarks/metrics_v2b/`. Zero synthetic numbers (Rule H3). |
| **A4** | **No Hard-Coded Numbers**<br>(Elimination of fake strings like "~114ms", live timers, live memory queries) | **PASS** | **Audit:** All hard-coded strings removed from `ResultScreen.kt`.<br>**Live Instrumentation:** Live latency computed via `SystemClock.elapsedRealtime()`. Benchmark card hidden behind title long-press per Rule H12 and displays live metrics. |
| **A5** | **Offline Compliance & Network Isolation**<br>(`aapt2 dump permissions`, zero INTERNET permission, airplane mode proof) | **PASS** | **Permissions:** `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk` verifies zero `android.permission.INTERNET` in debug and release variants.<br>**Airplane Mode Verification:** 15-second screen recording in airplane mode saved to `benchmarks/airplane_mode_test.mp4`; step-by-step audit documented in `benchmarks/airplane_mode_test.md`. |
| **A6** | **Llama Advisory Engine & Guardrails**<br>(ExecuTorch 0.6.0, `LlmModule`, INT4 SpinQuant, strict regex guardrails) | **PASS** | **Runtime:** `org.pytorch:executorch-android:0.6.0` using native `LlmModule` with `llama-3.2-1b-spinquant.pte` (1.05 GB INT4 weights) and `tokenizer.model` (loaded dynamically from device storage, never bundled in APK per Rule H2).<br>**Guardrails (`GuardrailFilter.kt`):** Intercepts numeric dosages and unregistered chemicals. 20/20 English cases passed in `benchmarks/llama_english_eval.md`; 20/20 Hindi cases passed in `benchmarks/hindi_llama_evaluation.md`.<br>**Telugu Guardrail (Rule H6):** Llama strictly prohibited from generating Telugu; Telugu advice is served 100% from human-reviewed Knowledge Base. |
| **A7** | **Knowledge Base & Agronomic Provenance**<br>(21 classes, en/te/hi, natural & chemical tiers, official citations, validated doses) | **PASS** | **File:** `data/kb/knowledge_base.json` validated by `scripts/validate_kb.py` and `data/kb/kb.schema.json`.<br>**Citations:** Every entry cites official agricultural authorities (PJTSAU, ANGRAU, ICAR, NIPHM, CIB&RC).<br>**Dosage Rule (H4):** Doses displayed only where `dose_verified: true`, otherwise displays *"follow the product label"*. Zero unverified chemicals. |
| **A8** | **Multilingual Engineering Quality**<br>(Complete XML string coverage in en, te, hi; purity tests; Noto fonts) | **PASS** | **Resources:** `values/strings.xml`, `values-te/strings.xml`, `values-hi/strings.xml`.<br>**Verification:** All string resources verified by `LocalizationTest.kt` (all keys present, zero Indic characters in English strings, authentic Telugu and Devanagari characters in respective files). Bundled free Noto Sans Telugu and Noto Sans Devanagari fonts (SIL OFL). |
| **A9** | **LeafValidator & Soft Warning**<br>(Handling of leaves, fruits, stems, bolls, and pests without hard blocks) | **PASS** | **Implementation:** `LeafValidator.kt` implements a soft warning rather than a blocking dialog. Valid agricultural subjects (fruits, stems, bolls, brown leaves, pests) offer a *"Use this photo anyway"* button, ensuring compliance with Requirement R2 and F1. |
| **A10** | **Feature Scope & Navigation**<br>(Strict mapping to R1–R9 / U1–U4, scope lock) | **PASS** | **Screens:** Onboarding language & region pickers, Home Screen, Camera Screen, Result Screen, Diseases Catalog, Saved Cases / Case Reports.<br>**Scope Lock (Rule H13):** No unauthorized features added. All experimental tabs marked optional or cleanly decoupled. |
| **A11** | **Core Workflow Requirements**<br>(First-launch pickers, severity bands, certainty wording, referral routing) | **PASS** | **First Launch:** 3 large buttons (`తెలుగు`, `हिन्दी`, `English`), then region selection (`Telangana`, `Andhra Pradesh`, `Other`).<br>**Severity:** Mild, Moderate, Severe calculated in plain unit-tested code (`RulesEngine.kt`) combined with field spread question (*a few plants, some, most*) into Urgency: *Early warning, Act this week, Act now*.<br>**Certainty:** Word-based display (*Sure, Probably, Not sure*).<br>**Referral:** Kisan Call Centre (1800-180-1551) prominently shown for *Act now* and uncertain cases.<br>**Field Report:** 1-tap Share Field Report via Android share sheet, omitting unverified chemical doses. |
| **A12** | **Build & Automated Testing**<br>(`assembleDebug`, `assembleRelease`, unit test pass rate) | **PASS** | **`assembleDebug`:** PASS.<br>**`assembleRelease`:** PASS with Baseline Profile and debug key signing.<br>**Unit Tests:** **104/104 tests pass** across `AudioManifestTest`, `CaseSpecificAdvisoryTest`, `GuardrailFilterTest`, `LocalizationTest`, `PromptBuilderTest`, `RulesEngineTest`, `ScanHistoryRepositoryTest`, `SeverityCalculatorTest`, `TreatmentSelectorTest`, and `VisionParityTest`. |

---

## 2. Verification of Prior FAILs & Mitigations

In the initial baseline audit (12 Nov 2026), several architectural gaps were identified. Here is how every item was addressed:

1. **A2 & A3 (Missing Vision Model & Heuristic Fallback):**
   - *Resolution:* Trained MobileNetV3-Large on 21 classes, exported with ExecuTorch XNNPACK to `vision.pte` (21.4 MB). Fully deleted `SmartVisualClassifier`. Low-confidence or unclassified inputs route to *"Can't tell - ask an extension officer"*.
2. **A6 (Missing Llama Implementation & Broken Guardrails):**
   - *Resolution:* Implemented ExecuTorch 0.6.0 `LlmModule` loading 4-bit SpinQuant weights dynamically. Added robust in-process `GuardrailFilter` passing 20/20 grounding tests. Prohibited Telugu in Llama (Rule H6).
3. **A4 (Hard-Coded Metrics):**
   - *Resolution:* Replaced static strings with live runtime measurements (`SystemClock`, `Runtime.getRuntime()`). Moved benchmark details behind title long-press.
4. **A9 (LeafValidator Hard-Blocking Fruits & Pests):**
   - *Resolution:* Converted LeafValidator into a non-blocking soft warning with *"Use this photo anyway"* override.
5. **A8 & A11 (Missing Multilingual Strings & Workflow Requirements):**
   - *Resolution:* Added 100% localized string resources in English, Telugu, and Hindi; added `LocalizationTest.kt`; built first-launch language/region selector, field-spread urgency logic, and offline case report sharing.
6. **A12 (Failing Unit Tests & Release Build):**
   - *Resolution:* Configured debug signing for release build with Baseline Profile, updated regex rules, and achieved 104/104 passing unit tests.

---

## 3. Known Hardware & Real-World Limitations

1. **Single-Device Focus**: Benchmarks reflect physical testing on an 8-core 64-bit Arm CPU phone with 6 GB physical RAM (MediaTek Helio G96) and verification on Android 14. Performance on 4 GB or 32-bit devices may experience increased latency or lower memory margins.
2. **Dynamic Llama Download Requirement**: Per Rule H2 and GitHub's 100 MB file limit, the 1.05 GB Llama 3.2 1B SpinQuant weights file cannot be bundled inside the APK. It must be copied to device storage via `adb push` or downloaded via an in-app one-time model import. When absent, the app automatically falls back to the fully grounded trilingual Knowledge Base (*"Offline guide"*).
3. **Lighting & Foliage Blur**: As with all convolutional vision models, severe motion blur or extreme dark/shadowed field conditions can degrade confidence below threshold, routing the user to *"Can't tell - ask an extension officer"*.
