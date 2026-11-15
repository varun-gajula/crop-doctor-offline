# Project Progress - Crop Doctor Offline

## Current Status: Tasks G1, G2, G3, G4, V2b, V3, V4, and A2 Complete

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
