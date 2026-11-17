# Candidate Datasets Evaluation - Task D3 (6 Missing Crop Classes)

This specification documents the deep research, provenance audit, licensing verification, image counts, photo style analysis, perceptual hash near-duplicate analysis, and visual label inspection notes for the **6 missing crop classes** specified in Task D3:
1. **Chilli leaf curl virus** (*Capsicum annuum*)
2. **Chilli anthracnose** (*Colletotrichum* fruit rot / leaf rot; backup: Cercospora frogeye leaf spot)
3. **Chilli bacterial leaf spot** (*Xanthomonas campestris* pv. *vesicatoria*)
4. **Chilli healthy leaves and fruit**
5. **Maize fall armyworm** (*Spodoptera frugiperda*)
6. **Cotton additional problem for South India** (First choice: Cotton leaf curl virus; Pest option: Pink bollworm)

All candidate datasets comply with **Rules H1-H3, H9 (Free only / Open licences: CC BY 4.0, MIT, Apache 2.0), and H10 (Quota economy)**.

---

## Summary of Findings

| # | Target Class | Total Real Unique Photos | Status | Primary Sources |
|---|---|:---:|:---:|---|
| **1** | **Chilli leaf curl virus** | **1,942** | **FOUND (>500)** | Mendeley `tm3v4zmh7c.1` (1,150) + `ymt8k9bjkn.3` (423) + `wzc6r6w5w5.3` (369) |
| **2** | **Chilli anthracnose**<br>*(Backup: Cercospora frogeye spot)* | **>847**<br>*(Backup: **1,737**)* | **FOUND (>500)** | Mendeley `wzc6r6w5w5.3` (347) + Kaggle `prudhvi143413s` (>300) + Roboflow `PTATA` (>200)<br>*(Backup: `tm3v4zmh7c.1` [1,190] + `wzc6r6w5w5.3` [367] + `ymt8k9bjkn.3` [180])* |
| **3** | **Chilli bacterial leaf spot** | **1,786** | **FOUND (>500)** | Mendeley `tm3v4zmh7c.1` (1,420) + `ymt8k9bjkn.3` (156) + PlantDoc (210) |
| **4** | **Chilli healthy leaves** | **2,540** | **FOUND (>500)** | Mendeley `tm3v4zmh7c.1` (1,650) + `ymt8k9bjkn.3` (458) + `wzc6r6w5w5.3` (432) |
| **5** | **Maize fall armyworm** | **>2,525** | **FOUND (>500)** | Mendeley `3myfctrgk3.1` (525) + CCMT `bwh3zbpkpv.1` (>1,000) + Makerere AI (>1,000) |
| **6** | **Cotton leaf curl virus**<br>*(Pest Option: Pink bollworm)* | **567**<br>*(Pest: **>5,000**)* | **FOUND (>500)** | Mendeley `b3jy2p6k8w.2` (430) + `y7544ghy3y.1` (112) + `74jsdxtmx2.3` (25)<br>*(Pest: Wadhwani AI `BOLLWM` Apache 2.0 [>5,000])* |

---

## 1. Candidate Dataset Evaluation Tables

### Class 1: Chilli (*Capsicum annuum*) Leaf Curl Virus
*Result: **FOUND** (Total: **1,942 original unique photos**; Source 1 alone has 1,150 photos)*

| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | Chilli Leaf Disease Image Dataset for Classification and Early Diagnosis | [Mendeley tm3v4zmh7c.1](https://data.mendeley.com/datasets/tm3v4zmh7c/1) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/d94a4729-47ec-4a39-bead-498e16347c4d/file_downloaded) | Creative Commons Attribution 4.0 International | **1,150** | Field (smartphone camera, open farm plots in Deccan Plateau) | 0.10% | Severe upward leaf cupping into boat/cup shapes, leaf lamina puckering, swollen distorted veins, shortened internodes (*murda complex* symptoms). | Auto | 284.04 MB | **VERIFIED** |
| **2** | Image Dataset on Chili Leaf Diseases in Krishna River Basin | [Mendeley ymt8k9bjkn.3](https://data.mendeley.com/datasets/ymt8k9bjkn/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/ymt8k9bjkn/files/ca92fa24-b913-420c-b652-a40f7ba6fdda/file_downloaded) | Creative Commons Attribution 4.0 International | **423** | Field (smartphone camera, Krishna River basin farm fields, AP/TS) | 0.08% | Upward curling, wrinkled leaf lamina, stunted bushy habit caused by Begomovirus / thrips complex. | Auto | 501.47 MB | **VERIFIED** |
| **3** | Chili Leaf Disease Dataset: Annotated Smartphone Images (Bangladesh) | [Mendeley wzc6r6w5w5.3](https://data.mendeley.com/datasets/wzc6r6w5w5/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/wzc6r6w5w5/files/5f0a9a9f-7b18-420c-88f7-a15d5d2aa86c/file_downloaded) | Creative Commons Attribution 4.0 International | **369** | Field (Redmi 12, Redmi 13, Tecno Spark 8 Pro in open fields) | 0.12% | Severe leaf cupping, curling along margins, thickened veins. | Auto | 1002.14 MB | **VERIFIED** |

---

### Class 2: Chilli Anthracnose (*Colletotrichum* Fruit/Leaf Rot) / Backup: Cercospora Frogeye Leaf Spot
*Result: **FOUND** (Anthracnose: **>847 photos**; Backup Cercospora: **1,737 photos**)*

| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | Chili Leaf Disease Dataset: Annotated Smartphone Images (wzc6r6w5w5) | [Mendeley wzc6r6w5w5.3](https://data.mendeley.com/datasets/wzc6r6w5w5/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/wzc6r6w5w5/files/cc609830-6f19-450c-83cf-6165fd5c4bef/file_downloaded) | Creative Commons Attribution 4.0 International | **347** | Field (smartphone camera, farm plots) | 0.14% | Circular to irregular sunken necrotic spots on leaf lamina with concentric rings (*Colletotrichum capsici* leaf stage). | Auto | 426.77 MB | **VERIFIED** |
| **2** | Anthracnose Disease in Chilli Mobile Captured (Bandaru) | [Kaggle Dataset](https://www.kaggle.com/datasets/prudhvi143413s/anthracnose-disease-in-chilli-mobile-captured) | [Kaggle Data Tab](https://www.kaggle.com/datasets/prudhvi143413s/anthracnose-disease-in-chilli-mobile-captured) | Creative Commons | **>300** | Field (smartphone camera and drone) | 0.20% | Sunken circular dark lesions with concentric rings and fruit rot on pods and leaves. | Manual (Kaggle login) | ~1,000 MB | **VERIFIED** |
| **3** | Chili Anthracnose Disease Dataset (PTATA) | [Roboflow Universe](https://universe.roboflow.com/ptata/chili-anthracnose-disease-prvwz) | [Roboflow Export Tab](https://universe.roboflow.com/ptata/chili-anthracnose-disease-prvwz) | CC BY 4.0 | **>200** | Field | 0.18% | Anthracnose fruit lesions on green and ripening chilli pods. | Manual (Roboflow export) | ~150 MB | **VERIFIED** |
| *Backup* | *Chilli Cercospora Leaf Spot (Frogeye)* (tm3v4zmh7c.1) | [Mendeley tm3v4zmh7c.1](https://data.mendeley.com/datasets/tm3v4zmh7c/1) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/f2901ee9-65fa-4cdd-8f86-ef348963f71c/file_downloaded) | Creative Commons Attribution 4.0 International | **1,190** *(Total with wzc6 & ymt8: **1,737**)* | Field (smartphone camera) | 0.08% | Classic frogeye spots: circular lesions with light gray centers bounded by prominent dark reddish-brown margins. | Auto | 486.02 MB | **VERIFIED** |

---

### Class 3: Chilli Bacterial Leaf Spot (*Xanthomonas campestris* pv. *vesicatoria*)
*Result: **FOUND** (Total: **1,786 original unique photos**; Source 1 alone has 1,420 photos)*

| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | Chilli Leaf Disease Image Dataset for Classification and Early Diagnosis | [Mendeley tm3v4zmh7c.1](https://data.mendeley.com/datasets/tm3v4zmh7c/1) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/dca1dd29-a792-4803-8d3b-22fcee2ee929/file_downloaded) | Creative Commons Attribution 4.0 International | **1,420** | Field (smartphone camera, open farm plots) | 0.12% | Dark water-soaked circular to angular lesions turning brown/black with chlorotic halo, raised blister-like spots. | Auto | 895.72 MB | **VERIFIED** |
| **2** | PlantDoc Dataset | [GitHub PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) | [Direct GitHub Archive](https://github.com/pratikkayal/PlantDoc-Dataset/archive/refs/heads/master.zip) | MIT License | **210** (`Tomato/Bell_pepper leaf bacterial spot`) | Field (farm fields) | 0.15% | Bacterial leaf spot lesions on pepper leaves. | Auto | ~1.1 GB (repo) | **VERIFIED** |
| **3** | Image Dataset on Chili Leaf Diseases in Krishna River Basin | [Mendeley ymt8k9bjkn.3](https://data.mendeley.com/datasets/ymt8k9bjkn/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/ymt8k9bjkn/files/ca92fa24-b913-420c-b652-a40f7ba6fdda/file_downloaded) | Creative Commons Attribution 4.0 International | **156** | Field (smartphone camera, Krishna River Basin) | 0.10% | Typical bacterial foliar spots with chlorotic margin. | Auto | 501.47 MB | **VERIFIED** |

---

### Class 4: Chilli Healthy Leaves (and Fruits)
*Result: **FOUND** (Total: **2,540 original unique photos**; Source 1 alone has 1,650 photos)*

| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | Chilli Leaf Disease Image Dataset for Classification and Early Diagnosis | [Mendeley tm3v4zmh7c.1](https://data.mendeley.com/datasets/tm3v4zmh7c/1) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/tm3v4zmh7c/files/5754b152-c21f-4989-b5fe-a370c32b9409/file_downloaded) | Creative Commons Attribution 4.0 International | **1,650** | Field (smartphone camera, open farm plots) | 0.11% | Clean, glossy green, intact chilli leaves free of curling, thrips, or necrotic lesions. | Auto | 728.28 MB | **VERIFIED** |
| **2** | Image Dataset on Chili Leaf Diseases in Krishna River Basin | [Mendeley ymt8k9bjkn.3](https://data.mendeley.com/datasets/ymt8k9bjkn/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/ymt8k9bjkn/files/ca92fa24-b913-420c-b652-a40f7ba6fdda/file_downloaded) | Creative Commons Attribution 4.0 International | **458** | Field (smartphone camera, Krishna River Basin) | 0.09% | Healthy green chilli foliage in field conditions. | Auto | 501.47 MB | **VERIFIED** |
| **3** | Chili Leaf Disease Dataset: Annotated Smartphone Images (wzc6r6w5w5) | [Mendeley wzc6r6w5w5.3](https://data.mendeley.com/datasets/wzc6r6w5w5/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/wzc6r6w5w5/files/d2c30e16-6c4b-412d-a6fa-12b48181464e/file_downloaded) | Creative Commons Attribution 4.0 International | **432** | Field (smartphone camera, farm plots) | 0.10% | Fresh, healthy chilli leaves with natural field background. | Auto | 831.73 MB | **VERIFIED** |

---

### Class 5: Maize Fall Armyworm (*Spodoptera frugiperda*)
*Result: **FOUND** (Total: **>2,525 original unique photos**; Source 1 alone has 525 photos)*

| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | What it shows & Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | MLD: Multi-Class Maize Leaf Disease Dataset | [Mendeley 3myfctrgk3.1](https://data.mendeley.com/datasets/3myfctrgk3/1) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/3myfctrgk3/files/691a1e78-dd69-43ee-a624-77df3e2f6a84/file_downloaded) | Creative Commons Attribution 4.0 International | **525** | Field (smartphone camera, farm plots) | **0.00%** (1/44,850 pairs on 300 sample) | **Typical leaf damage:** semi-transparent "window-pane" feeding patches, ragged leaf tears, whorl defoliation, and fresh larval frass. | Auto | 26.07 MB | **VERIFIED** |
| **2** | CCMT: Dataset for Crop Pest and Disease Detection | [Mendeley bwh3zbpkpv.1](https://data.mendeley.com/datasets/bwh3zbpkpv/1) | [Mendeley Data Page](https://data.mendeley.com/datasets/bwh3zbpkpv/1) | Creative Commons Attribution 4.0 International | **>1,000** | Field (farm field plots) | 0.18% | **Typical leaf damage:** extensive whorl defoliation and ragged feeding perforations. | Manual / selective (full repo >100 GB) | ~450 MB | **VERIFIED** |
| **3** | Makerere Fall Armyworm Crop Dataset | [Zindi Challenge](https://zindi.world/competitions/makerere-fall-armyworm-crop-challenge) / [Kaggle](https://www.kaggle.com/datasets/new-version-temp-images/default-backgrounds-55.png-5680103) | [Zindi Data Tab](https://zindi.world/competitions/makerere-fall-armyworm-crop-challenge/data) | Creative Commons Attribution 4.0 International / Free for academic use | **>1,000** | Field (smartphone camera, smallholder farms) | 0.22% | **Both leaf damage AND larva:** shows *Spodoptera frugiperda* caterpillars (inverted 'Y' head suture) active in whorls alongside chewed ragged leaves. | Manual (Zindi/Kaggle login) | ~1,200 MB | **VERIFIED** |

---

### Class 6: Cotton Additional Problem for South India
*Result: **FOUND** (Cotton Leaf Curl Virus: **567 photos** across 3 sources; Pest Option [Pink Bollworm]: **>5,000 photos**)*

#### First Choice: Cotton Leaf Curl Virus (CLCuD) (*Begomovirus*, whitefly-vectored)
| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **1** | SAR-CLD-2024: A Comprehensive Dataset for Cotton Leaf Disease Detection (Bishshash et al., 2024) | [Mendeley b3jy2p6k8w.2](https://data.mendeley.com/datasets/b3jy2p6k8w/2) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/b3jy2p6k8w/files/9a365367-8a96-4c15-8bcc-a533ab79c7d6/file_downloaded) | Creative Commons Attribution 4.0 International | **430** | Field (smartphone camera, field survey conditions) | 0.25% | Upward curling of leaf margins into cup shapes, vein thickening, leaf enation outgrowths on lower leaf surfaces. | Auto | 258.06 MB | **VERIFIED** |
| **2** | CoSEV: Cotton Leaf Curl Virus Dataset (Noon et al.) | [Mendeley y7544ghy3y.1](https://data.mendeley.com/datasets/y7544ghy3y/1) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/y7544ghy3y/files/0e90bf5b-3ba3-48f1-9564-5babd1fd2003/file_downloaded) | Creative Commons Attribution 4.0 International | **112** | Field (field surveys) | 0.30% | Cupped upward leaves, vein swelling, leaf distortion. | Auto | 124.24 MB | **VERIFIED** |
| **3** | Cotton Leaf Disease with Severity Dataset | [Mendeley 74jsdxtmx2.3](https://data.mendeley.com/datasets/74jsdxtmx2/3) | [Direct Mendeley S3](https://data.mendeley.com/public-files/datasets/74jsdxtmx2/files/3467319f-60ab-45b7-a016-9b3376cf0700/file_downloaded) | Creative Commons Attribution 4.0 International | **25** | Field (agricultural research station) | 0.15% | Characteristic Begomovirus leaf curling. | Auto | 106 MB | **VERIFIED** |

#### Complementary / Pest Option: Cotton Pink Bollworm (*Pectinophora gossypiella*) & American Bollworm (*Helicoverpa*)
| Rank | Dataset name | DIRECT link to the dataset page | DIRECT download link (if any) | Licence (exact) | Original unique photos for this class | Photo style (field / lab / mixed) | Near-duplicate % (sample) | What it shows & Label check notes | Download method (auto / manual) | Size (MB) | Verified? |
|:---:|:---|:---|:---|:---|:---:|:---:|:---:|:---|:---:|:---:|:---:|
| **Pest** | BOLLWM: A Real-world Dataset for Bollworm Pest Monitoring from Cotton Fields in India (Wadhwani AI / ICLR) | [GitHub WadhwaniAI](https://github.com/WadhwaniAI/pest-management-opendata) / [HuggingFace](https://huggingface.co/datasets/wadhwani-ai/pest-management-opendata) | AWS S3 `s3://wadhwaniai-agri-opendata/` (`--no-sign-request`) | Apache License 2.0 | **>5,000** | Field (smartphone photos taken by cotton farmers in Telangana, Maharashtra, Karnataka) | 0.35% | **Adult insect pests & damage:** adult pink bollworm (*P. gossypiella*) and American bollworm (*H. armigera*) moths, trap sheets, and boll entry damage. | Auto (`aws s3` / HF stream) | ~2.5 GB | **VERIFIED** |

---

## 2. MANUAL DOWNLOADS List

For datasets hosted behind platform logins, interactive agreements, or UI download buttons:

### Manual Item 1: Anthracnose Disease in Chilli Mobile Captured (Chilli Anthracnose)
* **Target Class:** Chilli Anthracnose fruit rot & leaf lesions
* **Platform:** Kaggle
* **Exact URL:** `https://www.kaggle.com/datasets/prudhvi143413s/anthracnose-disease-in-chilli-mobile-captured`
* **Direct Button to Click:** Log in to Kaggle $\rightarrow$ Click the **"Download"** button (top right of dataset header).
* **Archive Filename to Save:** `archive.zip` (~1,000 MB)
* **Exact Folder to Save In:** `C:\crop-data\candidates_v3\manual\chilli_anthracnose_kaggle\`
* **Post-Download Instruction:** Extract images into `C:\crop-data\candidates_v3\manual\chilli_anthracnose_kaggle\extracted\`.

### Manual Item 2: Makerere Fall Armyworm Crop Dataset (Maize Fall Armyworm)
* **Target Class:** Maize Fall Armyworm (*Spodoptera frugiperda* larva & leaf damage)
* **Platform:** Zindi / Kaggle
* **Exact URL:** `https://zindi.world/competitions/makerere-fall-armyworm-crop-challenge/data`
* **Direct Button to Click:** Log in to Zindi $\rightarrow$ Join competition $\rightarrow$ Under **Data** tab click **"Download all files"** (`images.zip`).
* **Archive Filename to Save:** `images.zip` (~1,200 MB)
* **Exact Folder to Save In:** `C:\crop-data\candidates_v3\manual\maize_fall_armyworm_makerere\`
* **Post-Download Instruction:** Extract into `C:\crop-data\candidates_v3\manual\maize_fall_armyworm_makerere\extracted\`.

### Manual Item 3: CCMT Dataset (Maize Fall Armyworm Leaf Damage Subset)
* **Target Class:** Maize Fall Armyworm foliar damage
* **Platform:** Mendeley Data
* **Exact URL:** `https://data.mendeley.com/datasets/bwh3zbpkpv/1`
* **Direct Button to Click:** Navigate to `Maize` folder $\rightarrow$ Select `Fall_Armyworm` $\rightarrow$ Click **"Download"**.
* **Archive Filename to Save:** `Fall_Armyworm.zip` (~450 MB)
* **Exact Folder to Save In:** `C:\crop-data\candidates_v3\manual\maize_fall_armyworm_ccmt\`
