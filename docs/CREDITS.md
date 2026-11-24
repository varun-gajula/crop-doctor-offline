# Crop Doctor Offline - Attributions, Data Provenance & Licences

This document provides a comprehensive audit of all external datasets, machine learning models, edge runtimes, software libraries, typography, and agronomic knowledge sources utilized in **Crop Doctor Offline** (**Agri Care AI**).

In strict compliance with Project Rules **H2 (No datasets or weights committed to git)**, **H9 (Zero budget / open licences)**, and **FINAL_REQUIREMENTS.md**, all third-party components have been verified for licence compatibility and ethical data provenance.

---

## 1. Machine Learning Models & Runtimes

### 1.1 Meta Llama 3.2 1B-Instruct (Offline Advisory Engine)
- **Licence:** [Llama 3.2 Community License Agreement](https://llama.meta.com/llama3/license/)  
- **Copyright:** © Meta Platforms, Inc.  
- **Attribution:** *Built with Llama.*  
- **Model Details:** 4-bit SpinQuant INT4 quantized weights exported for ExecuTorch CPU XNNPACK Arm64 runtime (`llama-3.2-1b-spinquant.pte`, 1,135,951,488 bytes; SHA-256: `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b`).  
- **Tokenizer:** `tokenizer.model` (2,183,982 bytes; SHA-256: `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55`).  
- **Terms:** Permitted for commercial and non-commercial use with monthly active users under 700 million. Weights are never shipped inside the APK or Git repository; reviewers and users obtain them directly after accepting the community licence.

### 1.2 Vosk Offline Speech Recognition (Voice Input Lane)
- **Licence:** [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)  
- **Copyright:** © 2019–2024 Alpha Cephei Inc.  
- **Models Utilized:**
  1. **Indian English (`vosk-model-small-en-in-0.4`):** Apache 2.0. Checksum SHA-256: `20663dcac4d5cb783a579c54d98339344a688e4ec6e1b4a4b059fd1235454cc7`.
  2. **Hindi (`vosk-model-small-hi-0.22`):** Apache 2.0. Checksum SHA-256: `7c50a10866889f0ac21d912c20537a055a597ed09fc1d3e5bcd798f9f0017e48`.
  3. **Telugu (`vosk-model-small-te-0.42`):** Apache 2.0. Checksum SHA-256: `3e77b05becc7599a596ccebd8b22cb95eb4eca0cfca524827cdfbfd66af09497`.
- **Integration:** Automated download and checksum verification via `scripts/setup/download_vosk.py`. Released from RAM immediately after speech inference to maintain the strict 2.5 GB memory budget.

### 1.3 ExecuTorch & XNNPACK (Edge Inference Runtime)
- **ExecuTorch 0.6.0:** [BSD 3-Clause License](https://github.com/pytorch/executorch/blob/main/LICENSE), © 2023–2024 Meta Platforms, Inc. and PyTorch contributors.
- **XNNPACK:** [BSD 3-Clause License](https://github.com/google/XNNPACK/blob/master/LICENSE), © Google LLC. Targeting Arm NEON vector SIMD on 64-bit Arm CPUs (`arm64-v8a`).

---

## 2. Training & Evaluation Datasets (Full Audit)

The vision classifier (`vision.pte`) is trained on real agricultural field photography across 15 production classes and rejection categories. The table below details every dataset utilized:

| # | Dataset Name | Provider / Source | Licence | Commercial Use? | Classes Fed | Expected Photo Count |
|---|---|---|---|---|---|---|
| 1 | **Paddy Doctor** | IEEE DataPort ([doi:10.21227/zsc8-d217](https://doi.org/10.21227/zsc8-d217)) / Kaggle | **CC BY 4.0** | Allowed | Rice leaf blast, Rice healthy, Rice yellow stem borer, Rice hispa | 10,407 raw (~6,538 utilized) |
| 2 | **Project-AgML Rice** | Hugging Face ([AgML](https://huggingface.co/datasets/Project-AgML/rice_leaf_disease_classification)) | **Apache 2.0** / Open Access | Allowed | Rice leaf blast, Rice healthy | 3,829 images |
| 3 | **Chilli Krishna Basin** | Mendeley Data ([doi:10.17632/ymt8k9bjkn.3](https://doi.org/10.17632/ymt8k9bjkn.3)) | **CC BY 4.0** | Allowed | Chilli leaf curl virus, Chilli healthy, Chilli bacterial spot, Chilli cercospora | 1,570 photos |
| 4 | **Cotton Severity Levels** | Mendeley Data ([doi:10.17632/74jsdxtmx2.1](https://doi.org/10.17632/74jsdxtmx2.1)) | **CC BY 4.0** | Allowed | Cotton bacterial blight, Cotton healthy, Cotton fusarium wilt | 980 photos |
| 5 | **Cotton Original Dataset** | Mendeley Data ([doi:10.17632/t9hgvk2h9p.1](https://doi.org/10.17632/t9hgvk2h9p.1)) | **CC BY 4.0** | Allowed | Cotton bacterial blight, Cotton healthy, Cotton alternaria | 1,060 photos |
| 6 | **Cotton Disease Dataset** | Mendeley Data ([doi:10.17632/b3jy2p6k8w.1](https://doi.org/10.17632/b3jy2p6k8w.1)) | **CC BY 4.0** | Allowed | Cotton bacterial blight, Cotton healthy | 600 photos |
| 7 | **MLD Maize Leaf Disease** | Mendeley Data ([doi:10.17632/3myfctrgk3.1](https://doi.org/10.17632/3myfctrgk3.1)) | **CC BY 4.0** | Allowed | Maize northern leaf blight, Maize healthy, Maize fall armyworm | 2,096 photos |
| 8 | **CCMT Crop Pest & Disease** | Mendeley Data ([doi:10.17632/bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1)) | **CC BY 4.0** | Allowed | Maize fall armyworm, Maize northern leaf blight, Maize healthy | 1,200 photos |
| 9 | **PlantDoc Dataset** | IIT Delhi ([GitHub](https://github.com/pratikkayal/PlantDoc-Dataset)) | **MIT License** | Allowed | Maize northern leaf blight (held-out validation) | 2,598 raw images |
| 10 | **PlantVillage Dataset** | Hughes & Salathé, Penn State ([GitHub](https://github.com/spMohanty/PlantVillage-Dataset)) | **CC BY-NC 3.0** *(Marked)* | **RESTRICTED (Non-Commercial Only)** | Maize common rust, Tomato early/late blight, Tomato yellow leaf curl, Tomato spider mites, Tomato healthy | 54,306 images (~18,000 utilized) |
| 11 | **Tomato Leaf Field (Archive2)** | Kaggle ([kaustubhb999/tomatoleaf](https://www.kaggle.com/datasets/kaustubhb999/tomatoleaf)) | **CC BY-SA 4.0** | Allowed | Tomato septoria leaf spot, Tomato healthy | 10,000 photos |
| 12 | **DeepWeeds / Agricultural Weeds** | Nature Scientific Reports ([doi:10.1038/s41598-018-38343-3](https://doi.org/10.1038/s41598-018-38343-3)) | **CC BY 4.0** | Allowed | Other non-crop & weed rejection (`other_not_a_crop`) | 1,000 photos |

> [!IMPORTANT]
> **Non-Commercial Attribution Notice (PlantVillage):**  
> The PlantVillage dataset is licensed under **Creative Commons Attribution-NonCommercial 3.0 Unported (CC BY-NC 3.0)**.  
> Citation: Hughes, D. P., & Salathé, M. (2015). *"An open access repository of images on plant health to enable the development of mobile disease diagnostics."* arXiv:1511.08060.  
> PlantVillage imagery is used exclusively for educational research, model benchmarking, and competition demonstration in the Arm Bharat AI-SoC Challenge 2026–27. Any commercial deployment of models trained with PlantVillage must replace these weights with open CC BY or proprietary field photography.

---

## 3. Candidate & Extended Pest Reference Datasets

For pest morphology evaluation and extension research:

| Dataset Identifier | Crop | Pest(s) Covered | Licence | Source / Citation |
|---|---|---|---|---|
| **IP102 Benchmark** | Rice / Multi-crop | 14 Rice Pests, Corn Borers, Aphids, Mites | Academic / Non-commercial | Wu et al., CVPR 2019; [xpwu95/IP102](https://github.com/xpwu95/IP102) |
| **Makerere FAW** | Maize | Fall Armyworm (*Spodoptera frugiperda*) | CC BY 4.0 / Open Access | Makerere AI Lab / Zindi Challenge |
| **Red Chili Pepper Pests** | Chilli | Aphids, Whiteflies, Thrips, Caterpillars | CC BY-SA 4.0 / Open Access | Agustian et al., IJAIN 2023 |
| **Paddy Field Pest Image Dataset** | Rice | BPH, Leaf Folder, Stem Borer, Green Leafhopper | CC BY 4.0 | Islam et al., Mendeley Data ([doi:10.17632/y5hwbhrn5m.3](https://doi.org/10.17632/y5hwbhrn5m.3)) |

---

## 4. Typography, Vector Icons & Application Code

1. **Application Code:**  
   - Licensed under the **Apache License 2.0** ([LICENSE](../LICENSE)).
2. **Material Design Icons:**  
   - All in-app iconography (`Spa`, `CameraAlt`, `Warning`, `CheckCircle`, `Phone`, `MenuBook`, `History`, `SupportAgent`, etc.) are from the Android Open Source Project (AOSP) under Apache License 2.0.
3. **Typography & Font Assets:**  
   - **Noto Sans Telugu** (Google / SIL Open Font License OFL 1.1).
   - **Noto Sans Devanagari** (Google / SIL Open Font License OFL 1.1).
   - **Roboto Typography** (Apache License 2.0).

---

## 5. Agronomic Knowledge Base Citations & Official Sources

Every entry in the trilingual Knowledge Base (`app/src/main/assets/kb.json`) cites verified agricultural research literature:

1. **Professor Jayashankar Telangana State Agricultural University (PJTSAU):**  
   - *Vyavasaya Panchangam (వ్యవసాయ పంచాంగం)* annual package of practices for Rice, Cotton, Chilli, and Maize in Telangana.
   - PJTSAU Pest & Disease Advisory Bulletin.
2. **Acharya N.G. Ranga Agricultural University (ANGRAU):**  
   - *Vyavasaya Darsini (వ్యవసాయ దర్శిని)* crop protection compendium for Andhra Pradesh agro-climatic zones.
3. **Indian Council of Agricultural Research (ICAR):**  
   - ICAR-IIRR (Indian Institute of Rice Research, Hyderabad) — Rice Blast, Stem Borer, Hispa management.
   - ICAR-CICR (Central Institute for Cotton Research, Nagpur) — Cotton Bacterial Blight & Wilt management.
   - ICAR-IIMR (Indian Institute of Maize Research, Ludhiana) — Fall Armyworm and Northern Leaf Blight guidelines.
   - ICAR-IIHR (Indian Institute of Horticultural Research, Bengaluru) — Chilli & Tomato disease protocols.
4. **Central Insecticides Board & Registration Committee (CIB&RC):**  
   - Directorate of Plant Protection, Quarantine & Storage, Ministry of Agriculture & Farmers Welfare, Government of India.
   - Verification of active ingredients and registered crop indications from the official *"Major Uses of Pesticides"* registry.
5. **National Institute of Plant Health Management (NIPHM):**  
   - Biological control protocols (Neem seed kernel extract / NSKE 5%, *Trichoderma harzianum*, *Pseudomonas fluorescens*).
