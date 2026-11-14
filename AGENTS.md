# Crop Doctor - project rules (always on)

## Mission
Build **Crop Doctor**, a fully OFFLINE Android app for the Arm "Bharat AI-SoC Challenge 2026-27", Problem Statement **PS1: Offline crop disease/pest detection and advisory for farmers**.
Target device: ANY Android phone with 6 GB RAM and a 64-bit Arm CPU (arm64-v8a), Android 10 or newer recommended (minSdk 26 = Android 8). The Motorola Edge 60 (12 GB) is no longer the reference device; the 6 GB phone is. Phase-1 submission deadline: **23 Nov 2026**.

## Requirements - source of truth (from the organisers' briefing video; never drop or weaken)
- R1 Works with NO connectivity ("Farmers need answers before connectivity arrives").
- R2 Identify the threat: likely disease or pest from a crop image (leaf, stem, fruit or pest).
- R3 Understand urgency: show severity; separate an early warning from a condition needing immediate action.
- R4 Know the next step: practical, region-aware, multilingual steps for the farmer or an extension worker. Telugu, Hindi and English are mandatory.
- R5 Flow: capture image on the Arm device -> local vision inference (label + severity) -> on-device advisory assistant (treatment/intervention guidance) -> steps shown in the chosen language.
- R6 Two on-device pipelines: Vision lane (image -> disease/pest model -> class + severity) and Advisory lane (structured finding -> Llama -> recommendation), joined by a local knowledge base (crop, region, treatment, language).
- R7 Advisory model: Llama 3.2 1B (or 3B) Instruct running with **ExecuTorch on the Arm CPU** (ExecuTorch is the reference runtime). The vision model is a separate, documented pipeline.
- R8 The demo proves value in under one minute: (1) photograph an affected crop/pest, (2) show local detection, disease/pest label and severity, (3) switch the advisory language and show the same recommendation offline, (4) explain uncertainty and route severe cases to an extension worker.
- R9 Deliverables: source code on GitHub, the application (APK), a demo video, a technical report.
- R10 Pests as well as diseases (the official PS1 text says "detect diseases and pests"). Committed: two pest classes (tomato spider mites, maize fall armyworm). The result shows the type (Disease / Pest / Healthy) and pest-management steps. Stretch: rice pests from IP102 (academic-use licence).
- R11 Region-specific advice. Regions: Telangana, Andhra Pradesh, Other. Each knowledge-base entry names its regional source (PJTSAU, ANGRAU or ICAR) and states a regional difference only when an official source says so, otherwise "same for all regions". Never invent one. Helpline numbers only after verification against official pages.
- R12 Urgency in code (H5): severity from leaf damage plus the farmer's one-tap "how much of the field is affected" answer gives Early warning / Act this week / Act now. Includes an early-warning line and sourced fertiliser and irrigation practices in the prevention tier.
- R13 Extension-worker tools: referral button (Kisan Call Centre 1800-180-1551) and a "Save case report" that can be shared by the Android share sheet. A shared report never contains unverified chemicals or doses.
- R14 The technical report documents the vision pipeline and the Llama/ExecuTorch advisory pipeline separately, with measured Arm CPU, RAM, latency and tokens/s numbers, licences and "Built with Llama". Out of scope (say so in the report): post-harvest loss, voice input, Hailo/Pi, Kannada/Tamil, Llama fine-tuning, cloud sync.
- FINAL_REQUIREMENTS.md holds the line-by-line traceability to the official text, acceptance tests and the release gate.

## Added by the user (not from the video)
- U1 For every problem the app also shows HOW TO TREAT it in two tiers: "Natural and traditional" (field practices, bio-agents, neem-based and other traditional methods that appear in an official source) and "Chemical (advanced)" (registered fungicides, insecticides, miticides), with safety information, and the code decides which tier is opened first according to severity.
- U2 The UI is clean, easy to understand for a farmer with limited reading ability, and lag-free (see the UX and performance budget below).

## Hard rules
- H1 The app manifest has NO `INTERNET` permission in any build variant. Verify with `aapt2 dump permissions` and keep the output in `benchmarks/`.
- H2 Never commit datasets, the Llama model files (about 1.1 GB), keystores, tokens or API keys. `.gitignore` exists from the first commit. Our own vision model IS committed so that a clean clone can build the APK, but each git file must stay under 90 MB (GitHub blocks 100 MB): use fp16 or a smaller backbone, or Git LFS.
- H3 Never invent numbers (accuracy, latency, tokens/s, RAM, sizes). Every figure in README/report comes from a logged measurement saved in `benchmarks/`. If it was not measured, write "not yet measured".
- H4 Never invent agronomy. Every knowledge-base entry cites at least one official source you actually read (ICAR, NIPHM, a state agricultural university such as PJTSAU, ANGRAU, UAS, TNAU, KAU, a Krishi Vigyan Kendra, or a CIB&RC-registered label). A treatment appears only if an official source recommends it for that crop and problem; if you find none, leave it out. Chemical products: name only active ingredients registered by CIB&RC for that crop (check the CIB&RC "Major Uses of Pesticides" list). Doses and spray rates may appear ONLY in structured `dose` fields copied from an official source, each with `source_url`, `source_quote` and `dose_verified`; the app shows a dose only when `dose_verified` is true (a human has checked it), otherwise it says "follow the product label". Never compute, guess or paraphrase a dose, and never let Llama write one. Always include the safety line (protective clothing, label, waiting days before harvest). Every entry carries `needs_expert_review: true` until a human signs it off.
- H5 Safety decisions (severity bands, certainty level, referral to an extension worker) live in plain, unit-tested app code. Llama only EXPLAINS; it never decides severity, referral or medicines. Its input is only the structured finding plus approved knowledge-base text, and its output is shown labelled "AI explanation".
- H6 Telugu is NEVER generated by Llama (Llama 3.2 does not officially support Telugu). Telugu text comes from the knowledge base (reviewable by a human).
- H7 Stay inside the project folder. No `sudo`, no `rm -rf` outside build directories, no silent installs of system software: tell the user what is missing and the command to install it.
- H8 The user does not always have the phone. Mark phone-only steps "REQUIRES PHONE" and build everything else against the emulator using interfaces `Classifier` and `Advisor`, each with a Fake implementation.
- H9 ZERO BUDGET. Everything must be free: no paid APIs, cloud credits, subscriptions, store fees, paid fonts or assets, and nothing that asks for a credit card. Use only open-source or free-tier tools; if something would cost money, stop and tell the user. Never suggest upgrading a plan.
- H11 NO FAKE INTELLIGENCE. A disease or pest label comes ONLY from the trained model (`vision.pte`). No hand-written colour, shape or aspect-ratio rule may choose a label. Plain heuristics are allowed only for (a) a SOFT "is this a crop photo?" warning with a "use this photo anyway" button, and (b) the lesion-fraction severity estimate. If the model is missing, fails to load, or is not sure, show "Not sure - ask an extension officer"; never fall back to a guess.
- H12 NO HARD-CODED MEASUREMENTS. Any speed, RAM, accuracy or offline figure shown in the app, README or report is computed at run time or copied from a log in `benchmarks/`. A literal like "~114 ms" in the UI is forbidden. Any benchmark panel is hidden from farmers (long-press on the title) and shows live values.
- H13 SCOPE LOCK. Build only what R1-R9 and U1-U4 ask for. Any extra screen or feature (chatbot, tips, history, branding extras) needs the user's written approval first; until then list it as "optional" in `docs/PROGRESS.md`. Required items must be finished and proven before any optional one.
- H14 ONE FOLDER. Work only in the workspace folder that holds this AGENTS.md (never in Antigravity's scratch area). It must be a git repo with a GitHub remote; commit after every small step, run `./gradlew assembleDebug` after every change that touches Kotlin, and push at every CHECKPOINT.
- H15 6 GB PHONE RULE. Judges evaluate RAM use, and the app must run on any phone with 6 GB RAM, the reference is a 6 GB phone. Budget: whole app, peak measured by the app itself, at most 2.5 GB PSS while the Llama model is loaded; at most 500 MB without it. Load the Llama model only when an advisory is requested, check ActivityManager memory info first (if low memory, show the knowledge-base text only and say so), and release the model after 60 s idle. Keep the vision model under 30 MB and load it once. Never keep bitmaps at full camera size: downscale before inference. Measure peak memory on the phone and on an emulator with 6 GB RAM, and save both to benchmarks/. A number that was not measured is "not yet measured" (H3).
- H10 QUOTA ECONOMY. The user's Antigravity plan is the free tier, with a quota that refreshes weekly. Be economical: do not re-read large files without need, do not print huge outputs, batch related edits into one step, prefer a script over repeated manual agent steps, and ask before any large rewrite. Tell the user when a phase is expensive so they can plan.

## Working agreement
The user is a B.Tech ECE student, new to Android and ML, on a Windows 11 laptop (Intel Core Ultra 5, 16 GB RAM, no NVIDIA GPU). GPU training and ExecuTorch export run on Google Colab, executed by the user. Explain each step in plain English (what and why, 2-3 sentences) and give exact copy-paste commands for anything the user must run. At every CHECKPOINT stop and wait. Keep `docs/PROGRESS.md` (done / next / blocked / needed from user). Make small commits with clear messages. Each reply ends with: what is done, what is next, what you need from the user.

## UX and performance budget (U2)
- First launch: three big buttons "తెలుగు", "हिन्दी" and "English", then region. One main action per screen. A one-tap language chip is on every screen.
- Touch targets at least 56 dp; body text at least 18 sp (Telugu at least 20 sp); every message has an icon plus short words (Grade-6 level, no jargon). Severity is shown by colour AND icon AND word. Certainty is shown in words (Sure / Probably / Not sure), the percentage only in details. High contrast that stays readable in sunlight (Material 3).
- The result (vision + knowledge base) appears within 1.5 s of the shutter; Llama text streams in afterwards and never blocks the screen.
- Cold start at most 2 s on the 6 GB reference phone; smooth camera preview; under 2% janky frames. No inference, file or JSON work on the main thread; load models once in the background and keep them; throttle streaming text updates to about every 100 ms. Judge speed on a RELEASE build with a Baseline Profile (debug Compose builds are much slower).
- Usability test with 3 non-engineers (ideally one farmer): from opening the app to explaining the advice in under 60 seconds, without help.

## Tech decisions (change only with the user's approval)
- Android: Kotlin, Jetpack Compose, CameraX, minSdk 26, Gradle Kotlin DSL, one app module with clean packages.
- Vision: RAM is a real limit (rule H15: the app must run on a 6 GB phone). Choose the backbone by accuracy on REAL field photos, subject to: result within 1.5 s of the shutter on the 6 GB phone, file under 30 MB (H15) and under 90 MB (GitHub). Compare MobileNetV3-Large, EfficientNet-B0 and ConvNeXt-Tiny (fp16 if over 90 MB), trained in PyTorch on Colab; deployed with ExecuTorch (XNNPACK); int8 is optional, fp32/fp16 is fine. Fallback runtime for vision only: LiteRT/TFLite. Speed, memory (H15) and data quality are the real limits.
- Advisory size: start with Llama-3.2-1B. Try 3B only after 1B works end to end on the phone, and only if a ready ExecuTorch `.pte` exists and tokens/s and quality are measured and better; otherwise stay on 1B.
- Advisory: Llama-3.2-1B-Instruct, pre-exported 4-bit SpinQuant `.pte` from the `executorch-community` Hugging Face organisation (verify the exact repo and file names). Copied to the phone separately (adb push or in-app import); never bundled in the APK or git.
- Pin the ExecuTorch Python package and the Android library to compatible versions and write the versions down; a mismatch is the most common failure.
- Knowledge base: one JSON file in APK assets, validated by a JSON schema and a script.
- Languages: Telugu (te), Hindi (hi) and English (en) are all mandatory. Bundle the free Noto Sans Telugu and Noto Sans Devanagari fonts (SIL Open Font License). Llama 3.2 officially supports Hindi, so Llama may generate the Hindi explanation, but test its quality on 20 cases; if it is poor or fails the guardrail, treat Hindi like Telugu (fixed templates + knowledge-base text).
- Telugu glossary for consistent wording: crop = పంట, disease = తెగులు, pest/insect = పురుగు, leaf = ఆకు, spray = పిచికారీ, fungicide = శిలీంధ్రనాశిని, insecticide = పురుగుమందు, farmer = రైతు, agriculture extension officer = వ్యవసాయ విస్తరణ అధికారి, severity = తీవ్రత; rice = వరి, cotton = పత్తి, chilli = మిరప, tomato = టమాటా, maize = మొక్కజొన్న.
- Hindi glossary: crop = फसल, disease = रोग, pest/insect = कीट, leaf = पत्ती, spray = छिड़काव, fungicide = फफूंदनाशक, insecticide = कीटनाशक, farmer = किसान, agriculture extension officer = कृषि विस्तार अधिकारी, severity = गंभीरता; rice = धान, cotton = कपास, chilli = मिर्च, tomato = टमाटर, maize = मक्का.

- More Telugu terms: whitefly = తెల్ల దోమ, mite = నల్లి, thrips = తామర పురుగు, fall armyworm = కత్తెర పురుగు. Class names (English, Telugu, Hindi, pathogen, data source) come from `seed/class_names_checked.tsv`: use them as written, never rename a class silently, and ask the user about every row marked UNVERIFIED. All Telugu and Hindi text stays `needs_review` until a native speaker or agriculture officer signs it off.

- Free-only stack: Antigravity (free tier), Android Studio and emulator, Google Colab or Kaggle Notebooks (free GPU), Google Drive (15 GB free, shared with Gmail - keep data small), GitHub (free public repo and Releases), Hugging Face (free), ExecuTorch and Llama 3.2 (free to use under the Llama 3.2 Community License), CC BY datasets (credit them). Sign the APK with the debug key; no Play Store.

## Repo layout
```
crop-doctor-offline/
  AGENTS.md  README.md  LICENSE  .gitignore
  app/                     Android app (Kotlin/Compose)
  ml/                      training + export code (importable by notebooks)
  notebooks/               01_data_check, 02_train_vision, 03_export_vision (Colab)
  data/DATASETS.md  data/class_map.yaml  data/kb/knowledge_base.json  data/kb/kb.schema.json
  scripts/                 download_data.py, validate_kb.py, audit_requirements.md
  docs/                    REQUIREMENTS.md, PROGRESS.md, REPORT.md, KB_REVIEW_CHECKLIST.md, DEMO_SCRIPT.md
  benchmarks/              raw measurement logs (phone + Colab)
  seed/                    earlier drafts from the user (read-only inspiration)
```

## Definition of done
Every requirement R1-R9 has a row in `docs/REQUIREMENTS.md` pointing to the code, the test or the measurement that proves it; the APK runs with airplane mode on; a clean clone of the GitHub repo builds the APK by following the README.
