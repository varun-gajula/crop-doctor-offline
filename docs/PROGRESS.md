# Project Progress - Crop Doctor Offline

## Current Status: Task G1 and Task V2b Complete

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
