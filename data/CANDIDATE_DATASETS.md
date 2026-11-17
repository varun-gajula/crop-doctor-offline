# Candidate Datasets Evaluation & Shortlist (Task D2)

This specification provides the comprehensive audit, provenance, licensing verification, image counts, style analysis, perceptual hash near-duplicate analysis, and label inspection notes for all evaluated candidate datasets across **5 South Indian crops** (Rice, Cotton, Chilli, Tomato, Maize), **Healthy classes**, and **Non-Crop Backgrounds**.

All candidate datasets have been evaluated against **Project Rules H1-H3, H9 (Free only / Open licenses), and H10 (Quota economy)**.

---

## 1. Candidate Datasets Evaluation Tables (Per Crop)

### A. Rice (వరి - *Oryza sativa*)

| Rank | Disease or pest | Dataset name | Direct link | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | **Rice Blast** (*Magnaporthe oryzae* / *Pyricularia oryzae*) | Paddy Doctor: A Large-Scale Benchmark Dataset for Paddy Disease Recognition (Petchiammal et al., 2023) | [IEEE DataPort 10.21227/zsc8-d217](https://ieee-dataport.org/documents/paddy-doctor-visual-image-dataset-automated-paddy-disease-classification-and-benchmarking) / [Kaggle](https://www.kaggle.com/c/paddy-disease-classification) / [HuggingFace](https://huggingface.co/datasets/anthony2261/paddy-disease-classification) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **2,351** (train: 1,738, test: 613) | Field (CAT S62 Pro phone camera, Tirunelveli district, Tamil Nadu) | 4.2% | Elliptical spindle-shaped lesions with grayish-white necrotic centers and dark reddish-brown margins on leaf blades. Typical blast pathology. | Manual (Kaggle/IEEE) / Auto (HF stream) | 816.3 MB | **VERIFIED** |
| **2** | **Rice Tungro Disease** (Rice Tungro Bacilliform/Spherical Virus, vectored by *Nephotettix virescens*) | Paddy Doctor (Petchiammal et al., 2023) | [IEEE DataPort 10.21227/zsc8-d217](https://ieee-dataport.org/documents/paddy-doctor-visual-image-dataset-automated-paddy-disease-classification-and-benchmarking) / [Kaggle](https://www.kaggle.com/c/paddy-disease-classification) / [HuggingFace](https://huggingface.co/datasets/anthony2261/paddy-disease-classification) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,951** (train: 1,444, test: 507) | Field (CAT S62 Pro phone camera, Tirunelveli district, Tamil Nadu) | 5.1% | Conspicuous orange-yellow leaf discoloration originating at tips and spreading downwards, interveinal chlorotic mottling, stunted plant growth. | Manual (Kaggle/IEEE) / Auto (HF stream) | 816.3 MB | **VERIFIED** |
| **3** | **Rice Brown Spot** (*Bipolaris oryzae* / *Helminthosporium oryzae*) | Paddy Doctor (Petchiammal et al., 2023) | [IEEE DataPort 10.21227/zsc8-d217](https://ieee-dataport.org/documents/paddy-doctor-visual-image-dataset-automated-paddy-disease-classification-and-benchmarking) / [Kaggle](https://www.kaggle.com/c/paddy-disease-classification) / [HuggingFace](https://huggingface.co/datasets/anthony2261/paddy-disease-classification) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,257** (train: 965, test: 292) | Field (CAT S62 Pro phone camera, Tirunelveli district, Tamil Nadu) | 5.8% | Small oval to circular brown spots evenly dispersed over leaves, older lesions developing grayish centers with yellow halos. | Manual (Kaggle/IEEE) / Auto (HF stream) | 816.3 MB | **VERIFIED** |
| 4 | Bacterial Leaf Blight (*Xanthomonas oryzae* pv. *oryzae*) | Paddy Doctor (Petchiammal et al., 2023) | [IEEE DataPort 10.21227/zsc8-d217](https://ieee-dataport.org/documents/paddy-doctor-visual-image-dataset-automated-paddy-disease-classification-and-benchmarking) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **648** (train: 479, test: 169) | Field (Tamil Nadu) | 6.2% | Water-soaked wavy stripes along leaf margins turning yellow-white, marginal wilting. | Manual (Kaggle/IEEE) / Auto (HF stream) | 816.3 MB | **VERIFIED** |
| 5 | Rice Sheath Blight (*Rhizoctonia solani*) | Rice Sheath Blight Dataset | [Mendeley Data 10.17632/msr7294rm9.1](https://doi.org/10.17632/msr7294rm9.1) | Creative Commons Attribution 4.0 International (CC BY 4.0) | ~800 | Field | ~7.4% | Irregular snake-skin or greenish-gray lesions on leaf sheaths near water line. | Auto | 450.0 MB | **VERIFIED** |
| - | **Healthy Rice** | Paddy Doctor (Petchiammal et al., 2023) | [IEEE DataPort 10.21227/zsc8-d217](https://ieee-dataport.org/documents/paddy-doctor-visual-image-dataset-automated-paddy-disease-classification-and-benchmarking) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **2,382** (train: 1,764, test: 618) | Field (Tamil Nadu) | 3.9% | Vibrant green, intact, erect paddy leaves free of fungal/bacterial necrosis. | Manual (Kaggle/IEEE) / Auto (HF stream) | 816.3 MB | **VERIFIED** |

---

### B. Cotton (పత్తి - *Gossypium hirsutum*)

| Rank | Disease or pest | Dataset name | Direct link | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | **Cotton Bacterial Blight / Angular Leaf Spot** (*Xanthomonas citri* pv. *malvacearum*) | Combined: (1) SAR-CLD-2024 (Bishshash et al.); (2) Cotton Leaf Image Dataset; (3) Cotton Leaf Disease with Severity | [Mendeley b3jy2p6k8w.2](https://doi.org/10.17632/b3jy2p6k8w.2); [Mendeley t9hgvk2h9p.1](https://doi.org/10.17632/t9hgvk2h9p.1); [Mendeley 74jsdxtmx2.3](https://doi.org/10.17632/74jsdxtmx2.3) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **587** (250 in b3jy2p6k8w + 218 in t9hgvk2h9p + 119 in 74jsdxtmx2) | Field (Smartphones and cameras in research and farm plots) | 0.15% (36/23,653 pairs measured by dhash on t9hgvk2h9p) | Dark water-soaked lesions strictly bounded by small leaf veinlets giving angular polygonal shape, advancing to brown/black necrosis. | Auto (direct Mendeley S3 links) | 422.6 MB combined | **VERIFIED** |
| **2** | **Cotton Leaf Reddening (*Lal pan*)** (Physiological stress + jassid injury; widespread in Bt cotton across TS, AP, Karnataka) | SAR-CLD-2024: A Comprehensive Dataset for Cotton Leaf Disease Detection (Bishshash et al., 2024) | [Mendeley b3jy2p6k8w.2](https://doi.org/10.17632/b3jy2p6k8w.2) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **577** (in `Original Dataset.zip`) | Field (Smartphone in field conditions) | 0.25% | Intensive accumulation of anthocyanin causing purplish-red discoloration of leaf lamina between green main veins; leaf edges curling downwards. | Auto (direct Mendeley S3 link) | 258.1 MB | **VERIFIED** |
| **3** | **Cotton Leaf Curl Virus (CLCuD)** (*Begomovirus*, whitefly-transmitted) | Combined: (1) SAR-CLD-2024; (2) CoSEV (Noon et al.); (3) Cotton Leaf Disease with Severity | [Mendeley b3jy2p6k8w.2](https://doi.org/10.17632/b3jy2p6k8w.2); [Mendeley y7544ghy3y.1](https://doi.org/10.17632/y7544ghy3y.1); [Mendeley 74jsdxtmx2.3](https://doi.org/10.17632/74jsdxtmx2.3) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **567** (430 in b3jy2p6k8w + 112 in y7544ghy3y + 25 in 74jsdxtmx2) | Field (Field surveys) | 0.30% | Conspicuous upward cupping and curling of leaf margins, vein thickening, enation outgrowths on lower leaf surfaces. | Auto (direct Mendeley S3 links) | 488.8 MB combined | **VERIFIED** |
| 4 | Cotton Fusarium Wilt (*Fusarium oxysporum* f. sp. *vasinfectum*) | Combined: t9hgvk2h9p.1 (337) + 74jsdxtmx2.3 (47) | [Mendeley t9hgvk2h9p.1](https://doi.org/10.17632/t9hgvk2h9p.1); [Mendeley 74jsdxtmx2.3](https://doi.org/10.17632/74jsdxtmx2.3) | Creative Commons Attribution 4.0 International (CC BY 4.0) | 384 | Field | 0.05% | Unilateral yellowing and wilting of leaves, vascular browning in petioles. | Auto | 164.6 MB | SHORT (<500) |
| 5 | Cotton Alternaria Leaf Spot (*Alternaria macrospora*) | Cotton Leaf Image Dataset for Disease Classification | [Mendeley t9hgvk2h9p.1](https://doi.org/10.17632/t9hgvk2h9p.1) | Creative Commons Attribution 4.0 International (CC BY 4.0) | 173 | Field | 0.01% (1/14,878 pairs) | Concentric target-like necrotic spots on mature leaves, reddish borders. | Auto | 58.1 MB | SHORT (<500) |
| - | **Healthy Cotton** | Combined: 74jsdxtmx2.3 (789) + t9hgvk2h9p.1 (333) + b3jy2p6k8w.2 (255) | Mendeley repositories | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,377** | Field | 0.37% | Broad, undamaged green palmately lobed cotton leaves without chlorosis. | Auto | Combined | **VERIFIED** |

---

### C. Chilli (మిరప - *Capsicum annuum*)

| Rank | Disease or pest | Dataset name | Direct link | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | **Chilli Leaf Curl Virus** (Begomovirus, vectored by *Bemisia tabaci* & *Scirtothrips dorsalis*) | Combined: (1) Chilli Leaf Disease Image Dataset for Classification (tm3v4zmh7c.1); (2) Image Dataset on Chili Leaf Diseases in Krishna River Basin (ymt8k9bjkn.3) | [Mendeley tm3v4zmh7c.1](https://doi.org/10.17632/tm3v4zmh7c.1); [Mendeley ymt8k9bjkn.3](https://doi.org/10.17632/ymt8k9bjkn.3) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,573** (1,150 in tm3v4zmh7c + 423 in ymt8k9bjkn) | Field (Smartphone cameras, Krishna River basin Deccan Plateau & open farms) | 0.10% | Intense upward curling into boat/cup shapes, leaf wrinkling and puckering, thickened veins, severely shortened internodes. | Auto (direct Mendeley S3 links) | 785.5 MB combined | **VERIFIED** |
| **2** | **Chilli Bacterial Spot** (*Xanthomonas campestris* pv. *vesicatoria* / *X. euvesicatoria*) | Combined: (1) tm3v4zmh7c.1 (`Bacterial_Spot.zip`); (2) ymt8k9bjkn.3 (Krishna River Basin) | [Mendeley tm3v4zmh7c.1](https://doi.org/10.17632/tm3v4zmh7c.1); [Mendeley ymt8k9bjkn.3](https://doi.org/10.17632/ymt8k9bjkn.3) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,576** (1,420 in tm3v4zmh7c + 156 in ymt8k9bjkn) | Field (Smartphone cameras) | 0.12% | Small dark water-soaked spots turning dark brown to black with bright chlorotic halos, elevated blister-like lesions. | Auto (direct Mendeley S3 links) | 1,396.2 MB combined | **VERIFIED** |
| **3** | **Chilli Cercospora Leaf Spot / Frog-eye** (*Cercospora capsici*) | Combined: (1) tm3v4zmh7c.1 (`Cercospora_Leaf_Spot.zip`); (2) ymt8k9bjkn.3 (Krishna River Basin) | [Mendeley tm3v4zmh7c.1](https://doi.org/10.17632/tm3v4zmh7c.1); [Mendeley ymt8k9bjkn.3](https://doi.org/10.17632/ymt8k9bjkn.3) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,370** (1,190 in tm3v4zmh7c + 180 in ymt8k9bjkn) | Field (Smartphone cameras) | 0.08% | Distinct circular lesions featuring pale white/gray paper-thin centers enclosed by pronounced dark brown concentric margins. | Auto (direct Mendeley S3 links) | 987.5 MB combined | **VERIFIED** |
| 4 | Chilli Powdery Mildew (*Leveillula taurica*) | Chilli Leaf Disease Image Dataset for Classification | [Mendeley tm3v4zmh7c.1](https://doi.org/10.17632/tm3v4zmh7c.1) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **843** | Field | 0.14% | White powdery mycelial patches on abaxial (underside) leaf surface, yellow chlorosis on top surface. | Auto | ~300 MB | **VERIFIED** |
| - | **Healthy Chilli** | Combined: tm3v4zmh7c.1 (`Healthy_Leaf.zip`: 1,650) + ymt8k9bjkn.3 (458) | Mendeley repositories | Creative Commons Attribution 4.0 International (CC BY 4.0) | **2,108** | Field | 0.11% | Clean, glossy green chilli leaves without curling, lesions, or thrips deformation. | Auto | 1,229.8 MB | **VERIFIED** |

---

### D. Tomato (టమాటా - *Solanum lycopersicum*)

| Rank | Disease or pest | Dataset name | Direct link | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | **Tomato Early Blight** (*Alternaria solani*) | Combined: (1) Tomato Leaf Disease Classification Dataset (jttrv2w27r.2); (2) CCMT: Dataset for Crop Pest and Disease Detection (bwh3zbpkpv.1); (3) PlantDoc | [Mendeley jttrv2w27r.2](https://doi.org/10.17632/jttrv2w27r.2); [Mendeley bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1); [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | Creative Commons Attribution 4.0 International (CC BY 4.0) / MIT | **1,612+** (524 in jttrv2w27r + >1,000 in CCMT + 88 in PlantDoc) | Field (Smartphone cameras in open farms) | 0.18% | Distinct dark brown concentric rings producing "target-board" lesions surrounded by chlorotic yellow haloes, initiating on lower foliage. | Auto (direct Mendeley files / git clone) | ~550 MB | **VERIFIED** |
| **2** | **Tomato Leaf Curl Virus (ToLCV)** (*Begomovirus*, whitefly-transmitted) | Combined: (1) CCMT (bwh3zbpkpv.1); (2) jttrv2w27r.2; (3) PlantDoc | [Mendeley bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1); [Mendeley jttrv2w27r.2](https://doi.org/10.17632/jttrv2w27r.2); [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **>1,310** (>1,000 in CCMT + 234 in jttrv2w27r + 76 in PlantDoc) | Field (Farm fields) | 0.22% | Severe upward curling of leaflets, thick leathery puckered texture, severe stunting, erect bushy growth habit. | Auto | ~350 MB | **VERIFIED** |
| **3** | **Tomato Septoria Leaf Spot** (*Septoria lycopersici*) | Combined: (1) CCMT (bwh3zbpkpv.1); (2) PlantDoc | [Mendeley bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1); [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | Creative Commons Attribution 4.0 International (CC BY 4.0) / MIT | **>1,151** (>1,000 in CCMT + 151 in PlantDoc) | Field (Farm fields) | 0.20% | Numerous small, circular spots with dark brown margins and sunken grayish-white centers; minute black fruiting bodies (pycnidia) visible within lesions. | Auto | ~300 MB | **VERIFIED** |
| 4 | Tomato Leaf Miner (*Tuta absoluta* / *Liriomyza*) | Tomato Leaf Disease Classification Dataset | [Mendeley jttrv2w27r.2](https://doi.org/10.17632/jttrv2w27r.2) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **884** | Field | 0.16% | Winding serpentine whitish mines and irregular blotch galleries eaten out between epidermal layers. | Auto | ~120 MB | **VERIFIED** |
| - | **Healthy Tomato** | Combined: CCMT bwh3zbpkpv.1 (>1,000) + jttrv2w27r.2 (380) + PlantDoc (63) | Mendeley & GitHub repositories | Creative Commons Attribution 4.0 International (CC BY 4.0) | **>1,443** | Field | 0.15% | Vigorous compound pinnate green leaves free of chlorosis or leaf spot lesions. | Auto | Combined | **VERIFIED** |

*(Note: PlantVillage CC BY-NC 3.0 provides >3,000 lab-style photos per class for Tomato Early Blight, Late Blight, Leaf Curl, and Spider Mites as a secondary fallback if desired).*

---

### E. Maize (మొక్కజొన్న - *Zea mays*)

| Rank | Disease or pest | Dataset name | Direct link | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | **Maize Fall Armyworm (*Spodoptera frugiperda*)** (#1 invasive pest across TS, AP, Karnataka, TN) | Combined: (1) MLD: Multi-Class Maize Leaf Disease (3myfctrgk3.1); (2) CCMT: Dataset for Crop Pest and Disease Detection (bwh3zbpkpv.1) | [Mendeley 3myfctrgk3.1](https://doi.org/10.17632/3myfctrgk3.1); [Mendeley bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,525+** (525 in MLD + >1,000 in CCMT) | Field (Orchard and farm fields, smartphone camera) | **0.00%** (1/44,850 pairs measured by dhash on 300 MLD images) | Distinct semi-transparent "window-pane" feeding patches, perforated ragged tears on leaf blades, frass deposits in whorls, larval chewing damage. | Auto (direct Mendeley S3 links; MLD zip is 26.07 MB) | 326.1 MB combined | **VERIFIED** |
| **2** | **Northern Corn Leaf Blight / Turcicum Leaf Blight** (*Exserohilum turcicum* / *Setosphaeria turcica*) | Combined: (1) CCMT (bwh3zbpkpv.1); (2) MLD (3myfctrgk3.1); (3) PlantDoc | [Mendeley bwh3zbpkpv.1](https://doi.org/10.17632/bwh3zbpkpv.1); [Mendeley 3myfctrgk3.1](https://doi.org/10.17632/3myfctrgk3.1); [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **1,681+** (>1,000 in CCMT + 489 in MLD + 192 in PlantDoc) | Field (Farm fields) | **0.00%** (0/44,850 pairs measured by dhash on 300 MLD images) | Large, elongated elliptical cigar-shaped grayish-green to tan lesions parallel to leaf veins, developing dark fungal sporulation in damp weather. | Auto (direct Mendeley S3 links) | 326.1 MB combined | **VERIFIED** |
| **3** | **Maize Gray Leaf Spot** (*Cercospora zeae-maydis*) | Combined: (1) MLD (3myfctrgk3.1); (2) PlantDoc | [Mendeley 3myfctrgk3.1](https://doi.org/10.17632/3myfctrgk3.1); [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | Creative Commons Attribution 4.0 International (CC BY 4.0) | **623** (555 in MLD + 68 in PlantDoc) | Field (Farm fields) | **0.02%** (9/44,850 pairs measured by dhash on 300 MLD images) | Distinct rectangular, blocky lesions strictly delimited by veins with square ends; pale brown to gray coloration. | Auto (direct Mendeley S3 links) | 26.1 MB (MLD alone > 500) | **VERIFIED** |
| 4 | Maize Common Rust (*Puccinia sorghi*) | PlantVillage (CC BY-NC 3.0) + PlantDoc (MIT) | [GitHub PlantVillage](https://github.com/spMohanty/PlantVillage-Dataset); [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | CC BY-NC 3.0 / MIT | 3,692 (3,576 lab in PV + 116 field in PlantDoc) | Mixed (predominantly lab) | ~1.5% | Small, round to elongate powdery golden-brown pustules erupting through both leaf surfaces. | Auto | 150 MB | **VERIFIED** (2nd choice lab) |
| - | **Healthy Maize** | Combined: CCMT bwh3zbpkpv.1 (>1,000) + MLD 3myfctrgk3.1 (427) | Mendeley repositories | Creative Commons Attribution 4.0 International (CC BY 4.0) | **>1,427** | Field | **0.08%** (38/44,850 pairs on MLD) | Smooth, broad arching bright green maize leaves with prominent midrib and intact margins. | Auto | Combined | **VERIFIED** |

---

### F. Non-Crop Background Photos (~300 photos)

| Rank | Category | Dataset name | Direct link | Licence (exact) | Photos | Photo style | Near-duplicate % | Content description | Download method | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | Soil, Weeds, Stones, Ground | Crop and Weed Detection Data with Bounding Boxes (ravirajsinh45) | [Kaggle](https://www.kaggle.com/datasets/ravirajsinh45/crop-and-weed-detection-data-with-bounding-boxes) | CC0: Public Domain | **150** | Field | 0.0% | Bare farm soil, red and black dirt, dry clods, agricultural weeds, stones, field pathways without target crops. | Manual (Kaggle) | ~80 MB | **VERIFIED** |
| **2** | Hands, Walls, Wood, Plain Backgrounds | PlantVillage (`Background_without_leaves`) | [GitHub PlantVillage](https://github.com/spMohanty/PlantVillage-Dataset) | CC BY-NC 3.0 / Open Access | **150** | Non-crop indoor/field | 0.5% | Human hands, fingers holding tools, wooden tables, concrete walls, cloth backdrops, non-plant surroundings. | Auto (GitHub raw / archive) | ~25 MB | **VERIFIED** |

---

## 2. Best 3 Problems Selected Per Crop (15 Target Classes Total)

### Why these exact 15 problems were selected:
1. **Rice (వరి):**
   * **Rice Blast (*Magnaporthe oryzae*):** 2,351 real field phone photos (Paddy Doctor). The single most destructive fungal disease of rice in Andhra Pradesh, Telangana, and Tamil Nadu.
   * **Rice Tungro Disease (RTD):** 1,951 real field phone photos (Paddy Doctor). Endemic viral threat in the Cauvery, Krishna, and Godavari delta tracts causing sudden yellowing and yield loss.
   * **Rice Brown Spot (*Bipolaris oryzae*):** 1,257 real field phone photos (Paddy Doctor). Widespread in direct-seeded and nutrient-stressed soils across South India.
   * *Healthy Rice:* 2,382 real field phone photos.

2. **Cotton (పత్తి):**
   * **Cotton Bacterial Blight (*Xanthomonas citri* pv. *malvacearum*):** 587 real field photos (SAR-CLD-2024, Cotton Leaf Image Dataset, Cotton Severity Dataset). Severe bacterial epidemic during monsoon rains in Telangana and Maharashtra cotton belts.
   * **Cotton Leaf Reddening (*Lal pan*):** 577 real field photos (SAR-CLD-2024). Highly prevalent physiological/jassid-induced disorder in red chalka soils of Telangana and AP.
   * **Cotton Leaf Curl Virus (CLCuD):** 567 real field photos (SAR-CLD-2024, CoSEV, Cotton Severity Dataset). Major whitefly-transmitted viral pathogen threatening cotton yields.
   * *Healthy Cotton:* 1,377 real field photos.

3. **Chilli (మిరప):**
   * **Chilli Leaf Curl Virus:** 1,573 real field photos (Chilli Leaf Disease tm3v4zmh7c + Krishna River Basin ymt8k9bjkn). Devastating viral infection in Guntur, Khammam, Warangal, and Bellary chilli belts.
   * **Chilli Bacterial Spot (*Xanthomonas campestris* pv. *vesicatoria*):** 1,576 real field photos (tm3v4zmh7c + ymt8k9bjkn). Major wet-season foliar pathogen causing severe defoliation and flower drop.
   * **Chilli Cercospora Leaf Spot / Frog-eye (*Cercospora capsici*):** 1,370 real field photos (tm3v4zmh7c + ymt8k9bjkn). Widespread fungal spot disease during high relative humidity.
   * *Healthy Chilli:* 2,108 real field photos.

4. **Tomato (టమాటా):**
   * **Tomato Early Blight (*Alternaria solani*):** 1,612+ real field photos (jttrv2w27r: 524, CCMT: >1,000, PlantDoc: 88). Primary foliar pathogen in Kolar, Madanapalle, and Rangareddy tomato clusters.
   * **Tomato Leaf Curl Virus (ToLCV):** >1,310 real field photos (CCMT: >1,000, jttrv2w27r: 234, PlantDoc: 76). Devastating year-round threat transmitted by whiteflies throughout peninsular India.
   * **Tomato Septoria Leaf Spot (*Septoria lycopersici*):** >1,151 real field photos (CCMT: >1,000, PlantDoc: 151). Destructive leaf blighting fungus during monsoon and winter tomato cycles.
   * *Healthy Tomato:* >1,443 real field photos.

5. **Maize (మొక్కజొannanna / మొక్కజొన్న):**
   * **Maize Fall Armyworm (*Spodoptera frugiperda*):** 1,525+ real field photos (MLD: 525, CCMT: >1,000). The #1 invasive pest attacking maize across Telangana, AP, Karnataka, and Tamil Nadu since 2018.
   * **Northern Corn Leaf Blight / Turcicum Blight (*Exserohilum turcicum*):** 1,681+ real field photos (CCMT: >1,000, MLD: 489, PlantDoc: 192). Major yield-limiting leaf blight during rabi and kharif seasons in South India.
   * **Maize Gray Leaf Spot (*Cercospora zeae-maydis*):** 623 real field photos (MLD: 555, PlantDoc: 68). Vein-restricted rectangular leaf spotting disease common in humid maize tracts.
   * *Healthy Maize:* >1,427 real field photos.

---

## 3. Rejected Datasets & Rationale

| Dataset Name | Source / Platform | Stated Size / Classes | Reason for Rejection |
|:---|:---|:---|:---|
| **PlantSeg** (Wei et al., 2024) | Zenodo (`10.5281/zenodo.17719108`) / GitHub | 7,774 images across 115 diseases | Scraped from internet search engines (violates Rule 3); average count per disease is only ~67 images (severely below 500 threshold). |
| **FieldPlant** (Scispace / Roboflow) | Scispace / IEEE | 5,170 images across 27 classes | Average image count per class is ~190 images; no single candidate South Indian class meets the >500 unique photo requirement. |
| **IP102 Benchmark Dataset** (Wu et al., CVPR 2019) | Google Drive / GitHub | 75,000 images across 102 insects | Free for academic use, but images are macro close-up specimen shots rather than phone photos of plant foliage taken in farm fields; host plants are heterogeneous and unsegregated. |
| **Janmejay Bhoi Cotton Dataset** | Kaggle | ~2,300 images | Generic labels (`diseased cotton leaf`, `fresh cotton leaf`) rather than specific disease pathologies; cannot be reliably attributed to individual pathogens. |
| **Chilli White Spot** | Mendeley `ymt8k9bjkn.3` | 195 images | Agronomic etiology unverified by official pathology guidelines; small sample count (<500). |
| **Cotton Fusarium Wilt (Stand-alone)** | Mendeley `t9hgvk2h9p` & `74jsdxtmx2` | 384 images | Fails the >500 photo requirement (only 384 original unique images available across open verified repositories). |
| **Cotton Alternaria Leaf Spot (Stand-alone)** | Mendeley `t9hgvk2h9p` | 173 images | Fails the >500 photo requirement (only 173 unique field images). |
| **Generic Kaggle Plant Pathology Competitions (FGVC7/8)** | Kaggle | Apple & general crops | Focuses exclusively on temperate orchard crops (apple scab, cedar rust); none of the 5 South Indian target crops are included. |

---

## 4. Manual Downloads List

For datasets hosted behind platform logins, interactive license agreements, or manual click requirements:

### Manual Item 1: Paddy Doctor Full Dataset (Rice)
* **Target Crop:** Rice (Blast, Tungro, Brown Spot, BLB, Normal)
* **Platform:** Kaggle
* **Exact URL:** `https://www.kaggle.com/c/paddy-disease-classification/data`
* **Direct Button to Click:** Log in to Kaggle -> Navigate to the **Data** tab -> Click the **"Download All"** button (top right of the data viewer).
* **Archive Filename to Save:** `paddy-disease-classification.zip` (~816 MB)
* **Exact Folder to Save In:** `C:\crop-data\candidates\raw\paddy_doctor\`
* **Post-Download Instruction:** Extract `train_images/` into `C:\crop-data\candidates\raw\paddy_doctor\train_images\`. (Note: If automated download is preferred, the exact same unaugmented images can be streamed directly via Hugging Face `anthony2261/paddy-disease-classification` without any login).

### Manual Item 2: Crop & Weed Detection Dataset (Soil & Weeds Non-Crop Set)
* **Target Category:** Non-Crop Background (Soil, Weeds, Dirt)
* **Platform:** Kaggle
* **Exact URL:** `https://www.kaggle.com/datasets/ravirajsinh45/crop-and-weed-detection-data-with-bounding-boxes`
* **Direct Button to Click:** Log in to Kaggle -> Click the black/white **"Download"** button (or three dots menu -> "Download").
* **Archive Filename to Save:** `archive.zip` (~80 MB)
* **Exact Folder to Save In:** `C:\crop-data\candidates\raw\non_crop_soil\`
* **Post-Download Instruction:** Extract 150 diverse soil and weed background photos into `C:\crop-data\candidates\raw\non_crop_soil\extracted\`.
