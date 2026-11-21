# Task F1: Growth Stage Verification & Missing Stage Audit

**Date:** 2026-11-21  
**Project:** Crop Doctor Offline (Bharat AI-SoC Challenge 2026-27, PS1)  
**Rule H4 Compliance:** Never invent agronomy. Every knowledge-base entry cites at least one official source (ICAR, NIPHM, PJTSAU, ANGRAU, or State Agricultural Universities). A growth stage is assigned ONLY where an official source in the knowledge base explicitly states it.

---

## 1. Summary of Growth Stage Distribution

- **Total Crops Supported:** 5 (`rice`, `cotton`, `chilli`, `tomato`, `maize`)
- **Total Problem Classes in Scope:** 16 (14 diseases + 2 pests; healthy crop baselines excluded from problem guide)
- **Entries with Official Sourced Stage:** 7 entries (43.8%)
- **Entries Without Stage ("Any stage"):** 9 entries (56.2%)

---

## 2. Entries with Sourced Growth Stages (7 Classes)

The following entries explicitly specify a growth stage in their official package of practice (POP) or threshold advisory:

| Crop | Class ID | Growth Stage | Official Source in Knowledge Base | Exact Sourced Text / Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **Rice** | `rice_bacterial_leaf_blight` | **Seedling** | ICAR-IIRR Hyderabad & PJTSAU POP | Referral quote: *"kresek seedling wilting appears, or lesions reach the flag leaf"* |
| **Rice** | `rice_brown_spot` | **Fruiting or Grain filling** | ICAR-IIRR Hyderabad & ANGRAU | Treatment quote: *"Foliar application at boot leaf stage"* / Referral: *"before grain filling or grain discoloration"* |
| **Rice** | `rice_sheath_blight` | **Fruiting or Grain filling** | ICAR-IIRR Hyderabad & ANGRAU | Referral quote: *"reach the penultimate or flag leaf sheath on >20% of tillers before grain filling"* |
| **Cotton** | `cotton_bacterial_blight` | **Vegetative** | ICAR-CICR Nagpur & PJTSAU POP | Treatment quote: *"Foliar spray with Pseudomonas fluorescens (10 g/L) at vegetative stage"* |
| **Chilli** | `chilli_leaf_curl_virus` | **Flowering** | PJTSAU & Dr. YSRHU Horticulture Guide | Referral quote: *"as soon as 5-10% of plants show leaf curling before flowering"* |
| **Tomato** | `tomato_yellow_leaf_curl_virus` | **Seedling** | ICAR-IIHR Tomato IPM & TNAU POP | Sourced quote: *"Nylon net nurseries completely exclude whitefly vectors during critical seedling stage."* |
| **Maize** | `maize_fall_armyworm` | **Vegetative** | ICAR-NBAIR Bengaluru, ICAR-IIMR & NIPHM | Sourced quote: *"ETL (5% at seedling stage, 10% at vegetative whorl stage, 20% at tasseling/flowering stage)"* |

---

## 3. Entries Left Without Stage ("Any stage") (9 Classes)

In strict adherence to Rule H4 ("Never invent agronomy") and Task F1 specifications ("If an entry has no stage, show it under 'Any stage'. Do NOT invent stages or add entries."), the following 9 problems do not carry an exclusive growth stage and are categorized under **"Any stage"**:

| # | Crop | Class ID | Problem Name (EN / TE / HI) | Agronomic Rationale (Why no restricted stage) | Official Reference |
| :-: | :--- | :--- | :--- | :--- | :--- |
| 1 | **Rice** | `rice_leaf_blast` | Rice Leaf Blast / వరి అగ్గి తెగులు / धान का झुलसा रोग | *Pyricularia oryzae* occurs across seedling nursery, vegetative tillering (leaf blast), and panicle emergence (neck blast). No single stage restricts its occurrence. | ICAR-IIRR & PJTSAU Rice Package |
| 2 | **Cotton** | `cotton_fusarium_wilt` | Cotton Fusarium Wilt / పత్తి ఫ్యుసేరియం ఎండు తెగులు / कपास का फ्यूजेरियम उकठा | Soil-borne vascular pathogen (*Fusarium oxysporum f. sp. vasinfectum*) infects roots and causes wilting from early seedling emergence through peak boll development. | ICAR-CICR Nagpur |
| 3 | **Chilli** | `chilli_bacterial_spot` | Chilli Bacterial Spot / మిరప బాక్టీరియల్ మచ్చ తెగులు / मिर्च का जीवाणु धब्बा रोग | *Xanthomonas vesicatoria* affects leaves, stems, and fruits during warm, rainy periods at any vegetative or reproductive growth stage. | ICAR-IIHR Bengaluru & Dr. YSRHU |
| 4 | **Tomato** | `tomato_early_blight` | Tomato Early Blight / టమాటా ముందస్తు ఆకు ఎండు తెగులు / टमाटर का अगेती झुलसा | *Alternaria solani* is a polycyclic foliar disease attacking foliage and stems continuously after transplanting whenever humidity is high. | ICAR-IIHR Bengaluru & PJTSAU |
| 5 | **Tomato** | `tomato_late_blight` | Tomato Late Blight / టమాటా ఆలస్యపు ఆకు ఎండు తెగులు / टमाटर का पछेती झुलसा | *Phytophthora infestans* attacks any green canopy tissue whenever cool, moist overcast weather conditions occur, regardless of plant age. | ICAR-IIHR Bengaluru & TNAU |
| 6 | **Tomato** | `tomato_spider_mites` | Tomato Two-Spotted Spider Mites / టమాటా ఎర్ర నల్లి పురుగు / टमाटर का लाल मकड़ी कीट | *Tetranychus urticae* colonizes foliage during dry, warm spells at any post-seedling vegetative, flowering, or fruiting phase. | ICAR-IIHR & NIPHM Pest Advisory |
| 7 | **Maize** | `maize_gray_leaf_spot` | Maize Gray Leaf Spot / మొక్కజొన్న బూడిద రంగు ఆకు మచ్చ తెగులు / मक्का का धूसर पत्ती धब्बा | *Cercospora zeae-maydis* develops on lower leaves and progresses upward across both vegetative and grain filling stages when relative humidity exceeds 90%. | ICAR-IIMR Ludhiana |
| 8 | **Maize** | `maize_common_rust` | Maize Common Rust / మొక్కజొన్న తుప్పు తెగులు / मक्का का सामान्य रतुआ | *Puccinia sorghi* airborne urediniospores infect foliage whenever cool temperatures (16–25°C) and high humidity prevail, across all field stages. | ICAR-IIMR Ludhiana & PJTSAU |
| 9 | **Maize** | `maize_northern_leaf_blight` | Maize Northern Leaf Blight / మొక్కజొన్న టర్సికం ఆకు ఎండు తెగులు / मक्का का उत्तरी पत्ती झुलसा | *Exserohilum turcicum* produces large elliptical cigar-shaped lesions on leaves from early whorl to harvest maturity. | ICAR-IIMR Ludhiana & UAS Dharwad |

---

## 4. UI Display & Test Verification

1. In the "Crop guide" screen, each crop's problem list groups items by their respective growth stage headers:
   - **Seedling**
   - **Vegetative**
   - **Flowering**
   - **Fruiting or Grain filling**
   - **Any stage** (contains the 9 unconstrained classes above)
2. Every item displays a **Disease** or **Pest** chip and its localized name.
3. Unit tests in `CropGuideTest.kt` programmatically assert that:
   - Exactly these 7 classes have non-null stage and group into the expected growth stage buckets.
   - Exactly these 9 classes have null stage and group into "Any stage".
   - Healthy classes are excluded from the problem list.
