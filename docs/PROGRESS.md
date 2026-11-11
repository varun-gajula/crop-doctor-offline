# Project Progress - Crop Doctor Offline

## Phase Status Summary

- [x] **Phase 0: Setup & Architecture Baseline** (Checkpoint 0 passed, repo live)
- [x] **Phase 1: Dataset Verification & Preparation** (Checkpoint 1 reached)
- [ ] **Phase 2: Vision Model Training on Colab** (Next)
- [ ] **Phase 3: Vision Model Export to ExecuTorch (.pte)**
- [ ] **Phase 4: Grounded Knowledge Base (25 Classes, 3 Languages)**
- [ ] **Phase 5: Android App on Emulator (UI + Fakes + Pure Kotlin Rules)**
- [ ] **Phase 6: Real Vision Integration (ExecuTorch / XNNPACK)**
- [ ] **Phase 7: On-Device Llama 3.2 1B Integration (ExecuTorch Arm CPU)**
- [ ] **Phase 8: Offline Benchmarks & Usability Testing (Motorola Edge 60)**
- [ ] **Phase 9: Demonstration Video, Technical Report & README**
- [ ] **Phase 10: GitHub Release & Final Compliance Audit**

---

## Log

### 11 Nov 2026 - Phase 1 Completed
- Verified 25 target classes across 5 South Indian crops (Rice, Cotton, Chilli, Tomato, Maize).
- Authored `data/DATASETS.md` auditing CC BY 4.0 licenses, capture context, and citations.
- Built `data/class_map.yaml` defining full metadata, vernacular names (Telugu & Hindi), and knowledge-base mappings.
- Created `scripts/resize_images.py` to compress images down to $\le 320\text{ px}$ JPEG, protecting Google Drive's 15 GB quota.
- Created `scripts/download_data.py` with automated Git/PlantDoc ingestion and Mendeley browser download guides.
- Created `notebooks/01_data_check.ipynb` ready for execution on Google Colab.
- Paused at **CHECKPOINT 1** awaiting class list approval.

---

## Next Steps
- User runs `scripts/download_data.py` or downloads Mendeley datasets to `C:\crop-data\raw\`.
- User runs `scripts/resize_images.py` to produce resized images ($< 1\text{ GB}$).
- User uploads resized images to Google Drive (`MyDrive/crop-doctor/data_raw/`).
- Proceed to Phase 2 (Vision Model Training on Colab).

## Blockers / Risks
- Bangladesh domain shift in Maize MLD dataset: Mitigated by reserving a dedicated real-field test set photographed locally.
