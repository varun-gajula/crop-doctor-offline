# Requirements Traceability Matrix - Crop Doctor

This document maps all challenge requirements (R1–R9 from the official briefing video), user-specified requirements (U1–U4), and engineering hard rules (H1–H10) to their corresponding features, implementation code, and verification proofs.

## 1. Challenge Requirements (Source of Truth: Briefing Video)

| Req ID | Requirement Statement | Feature / Component | Implementation File(s) | Verification Proof / Artifact |
| :--- | :--- | :--- | :--- | :--- |
| **R1** | Works with NO connectivity ("Farmers need answers before connectivity arrives"). | Complete offline execution; zero cloud calls; network-free manifest. | `app/src/main/AndroidManifest.xml` | `aapt2 dump permissions` logged in `benchmarks/permissions_check.txt`; airplane mode test log. |
| **R2** | Identify the threat: likely disease or pest from crop image. | Vision lane: MobileNetV3-Large image classifier on 25 crop classes. | `ml/model.py`, `app/.../vision/VisionClassifier.kt` | Confusion matrix and field test accuracy in `benchmarks/metrics.json`. |
| **R3** | Understand urgency: show severity; separate early warning from critical condition. | Severity calculation from diseased leaf area fraction; urgent visual banners. | `app/.../domain/SeverityCalculator.kt` | Unit tests in `SeverityCalculatorTest.kt`; calibrated on severity labels. |
| **R4** | Know the next step: practical, region-aware, multilingual steps (Telugu, Hindi, English mandatory). | Local agricultural knowledge base + localized step-by-step guidance cards. | `data/kb/knowledge_base.json`, `app/.../ui/AdvisoryCards.kt` | Schema validation `scripts/validate_kb.py`; UI language switch benchmarks. |
| **R5** | Flow: Capture -> Local Vision -> On-device Advisory -> Chosen Language. | Sequential unblocking flow: Shutter -> $<1.5$s Result -> Background Llama. | `app/.../ui/MainFlowViewModel.kt` | UI latency logs in `benchmarks/latency_log.md`. |
| **R6** | Two on-device pipelines: Vision lane + Advisory lane joined by local KB. | Dual pipeline decoupled architecture: Fast vision + Grounded Llama 3.2. | `app/.../vision/`, `app/.../advisor/`, `data/kb/` | Architecture diagram in `README.md`; pipeline timing benchmarks. |
| **R7** | Advisory model: Llama 3.2 1B Instruct via ExecuTorch on Arm CPU. | ExecuTorch runtime with XNNPACK Arm Neon acceleration; 4-bit SpinQuant. | `app/.../advisor/ExecuTorchLlamaAdvisor.kt` | `dumpsys meminfo` and token generation speed logged in `benchmarks/edge60_perf.md`. |
| **R8** | Demo proves value in under 1 minute: (1) photo, (2) label+severity, (3) switch language offline, (4) uncertainty/referral. | 4-step streamlined demo workflow under 60 seconds. | `docs/DEMO_SCRIPT.md` | Recorded unlisted YouTube demo video link; timestamps verified. |
| **R9** | Deliverables: GitHub source code, APK, demo video, technical report. | Public repository, signed debug APK, demo video, PDF technical report. | Entire repository, `docs/REPORT.md` | GitHub release `v0.1.0` link with APK SHA-256 and report PDF. |

---

## 2. User-Specified Requirements (U1–U4)

| Req ID | Requirement Statement | Feature / Component | Implementation File(s) | Verification Proof / Artifact |
| :--- | :--- | :--- | :--- | :--- |
| **U1** | Two-tier treatment: "Natural & traditional" and "Chemical (advanced)" with safety info; opens by severity. | Two-way treatment toggle; auto-selection based on mild/moderate/severe band. | `app/.../ui/TreatmentSection.kt`, `data/kb/knowledge_base.json` | Unit tests in `TreatmentTierTest.kt`. |
| **U2** | Clean, lag-free UI for farmers with limited reading ability (Grade-6, icons, colors, big touch targets >=56dp, result <=1.5s). | Material 3 high-contrast farmer-first UI, large typography, icon badges. | `app/.../ui/theme/`, `app/.../ui/ResultScreen.kt` | Usability test log with 3 non-engineers in `benchmarks/usability.md`. |
| **U3** | ZERO BUDGET: 100% free resources; no paid plans, trials, or credit cards. | Free tools only: Google Colab, Kaggle GPU, Hugging Face, Android Studio, GitHub. | `START_HERE_CHECKLIST.md` | Zero-cost dependency audit in `scripts/audit_requirements.md`. |
| **U4** | Telugu, Hindi, and English are all mandatory languages. | Bundled Noto Sans Telugu & Devanagari fonts; complete trilingual KB. | `app/src/main/assets/fonts/`, `data/kb/knowledge_base.json` | `scripts/validate_kb.py` passes all 25 classes with non-empty en, te, hi. |

---

## 3. Engineering Hard Rules (H1–H10)

| Rule ID | Rule Summary | Enforcing Mechanism | Verification Method |
| :--- | :--- | :--- | :--- |
| **H1** | No `INTERNET` permission in any build variant. | Stripped manifest; build check. | `aapt2 dump permissions <apk>` produces zero network permissions. |
| **H2** | Never commit datasets, Llama weights (~1.1 GB), keystores, or API keys. | Strict `.gitignore` from commit 1. | `git status`, repository size audit ($<50$ MB total). |
| **H3** | Never invent numbers (accuracy, latency, tokens/s, RAM). | Mandatory raw log files. | Every figure in `README.md` and `REPORT.md` links to a raw log in `benchmarks/`. |
| **H4** | Never invent agronomy. Every treatment cites an official source (ICAR/NIPHM/SAU/KVK/CIB&RC). Doses only with verified quote. | Strict JSON schema + automated validator script. | `python scripts/validate_kb.py` passes with zero errors. |
| **H5** | Safety & referral decisions live in pure unit-tested Kotlin code; Llama only explains. | Deterministic rules engine; guardrail interceptor. | Unit tests in `RulesEngineTest.kt` and `GuardrailTest.kt`. |
| **H6** | Telugu is NEVER generated by Llama; comes from human-verified KB. | Templated assembly for Telugu; Llama Telugu output blocked. | App code review and architectural isolation test. |
| **H7** | Stay inside project folder; no silent system installs. | Explicit instruction lists for user execution. | User logs and self-contained project structure. |
| **H8** | Emulator development unblocked without phone using Fakes. | `FakeClassifier` and `FakeAdvisor` mocks behind interfaces. | Instrumented emulator test pass without physical device. |
| **H9** | Zero budget: only free-tier tooling and open-source models. | Open-source stack (ExecuTorch, Llama Community License). | Financial audit: zero payments or card requirements. |
| **H10**| Quota economy on Antigravity free tier. | Batched operations, concise outputs, user-executed heavy tasks. | Phase-by-phase execution without exceeding weekly allowances. |

