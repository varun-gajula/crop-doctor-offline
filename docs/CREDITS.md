# Crop Doctor Offline - Visual Asset & Photo Credits

This document tracks all image assets bundled with Crop Doctor Offline, their origin, licensing, and attribution in compliance with project rules.

---

## 1. Crop Guide Photos (`app/src/main/assets/guide/`)

Per Task F1 rule 4:
- Only photos explicitly placed by the author/maintainer in `app/src/main/assets/guide/` are used.
- Web photos or unauthorized third-party images are strictly forbidden.
- When an entry does not have a photo in `app/src/main/assets/guide/<class_id>.jpg`, the UI gracefully renders a clean vector leaf icon (`Icons.Default.Spa`).

| Filename | Crop | Class ID | Source / Photographer | Licence | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| *(No user photos placed yet)* | — | — | — | — | Vector leaf icon displayed as default fallback |

---

## 2. Iconography & Vector Assets

All in-app vector icons are drawn directly from Android Open Source Project (AOSP) / Google Material Design Icons under Apache License 2.0:
- `Icons.Default.Spa` (Leaf icon)
- `Icons.Default.CameraAlt` (Camera action)
- `Icons.Default.Warning` (Alert / Danger)
- `Icons.Default.CheckCircle` (Verified / Natural)
- `Icons.Default.Phone` / `Icons.Default.Call` (Kisan Call Centre helpline)
- `Icons.Default.MenuBook` (Crop guide icon)
- `Icons.Default.History` (Saved reports icon)
- `Icons.Default.SupportAgent` (Extension officer icon)

---

## 3. Font Licenses

All bundled font families use the SIL Open Font License (OFL) 1.1:
- **Noto Sans Telugu** (Google / SIL Open Font License 1.1)
- **Noto Sans Devanagari** (Google / SIL Open Font License 1.1)
- **Roboto / Material Typography** (Apache License 2.0)

---

## 4. Candidate & Benchmarked Pest Datasets (Task D5b)

In compliance with **Rule H2 (No datasets committed to git repo)** and **Rule H9 (Free only / Open licenses)**, all training and evaluation datasets reside strictly on local disk under `C:\crop-data\` and external caches. None of these images are bundled in the repository or APK.

| Dataset Identifier | Crop | Pest(s) Covered | License | Source / Citation | Status / Disk Location |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **MLD (3myfctrgk3.1)** | Maize | Fall Armyworm (*Spodoptera frugiperda*) | CC BY 4.0 | Mendeley Data: [doi:10.17632/3myfctrgk3.1](https://doi.org/10.17632/3myfctrgk3.1) | Verified on disk (`C:\crop-data\candidates\raw\3myfctrgk3\`) |
| **CCMT (bwh3zbpkpv.1)** | Maize | Fall Armyworm (*S. frugiperda*), Grasshopper, Leaf Beetle | CC BY 4.0 | Mendeley Data: [doi:10.17632/bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1) | Verified on disk (`Downloads\Dataset for Crop Pest and Disease Detection\`) |
| **Paddy Doctor** | Rice | Yellow Stem Borer (*dead_heart*), Rice Hispa (*hispa*), Green Leafhopper (*tungro*) | CC BY 4.0 | IEEE DataPort: [doi:10.21227/zsc8-d217](https://doi.org/10.21227/zsc8-d217); Kaggle: [paddy-disease-classification](https://www.kaggle.com/competitions/paddy-disease-classification) | Verified on disk (`C:\crop-data\candidates\raw\paddy-disease-classification.zip`) |
| **Chili Krishna River Basin (ymt8k9bjkn.3)** | Chilli | Chilli Leaf Curl Complex (Thrips & Whitefly damage), Bacterial Spot | CC BY 4.0 | Mendeley Data: [doi:10.17632/ymt8k9bjkn.3](https://doi.org/10.17632/ymt8k9bjkn.3) | Verified on disk (`Downloads\Chili Leaf Disease Original Dataset\`) |
| **Paddy Field Pest Image Dataset (Islam et al.)** | Rice | BPH, Leaf Folder, Stem Borer, Green Leafhopper, Rice Bug | CC BY 4.0 | Mendeley Data: [doi:10.17632/y5hwbhrn5m.3](https://doi.org/10.17632/y5hwbhrn5m.3) | Candidate (Awaiting download) |
| **Makerere FAW** | Maize | Fall Armyworm (*Spodoptera frugiperda*) | CC BY 4.0 / Open Access | Zindi Challenge; Makerere AI Lab: [fall-armyworm](https://github.com/makerere-ai/fall-armyworm) | Candidate (Awaiting download) |
| **IP102 Benchmark** | Rice / Multi-crop | 14 Rice Pests, Corn Borers, Aphids, Mites | Academic / Non-commercial | CVPR 2019; [xpwu95/IP102](https://github.com/xpwu95/IP102) | Candidate (Awaiting download) |
| **Red Chili Pepper Pests** | Chilli | Aphids, Whiteflies, Thrips, Caterpillars (*Helicoverpa* / *Spodoptera*) | CC BY-SA 4.0 / Open Access | IJAIN 2023; Kaggle: [indraagustian/red-chili-pepper-pests-dataset](https://www.kaggle.com/datasets/indraagustian/red-chili-pepper-pests-dataset) | Candidate (Awaiting download) |

