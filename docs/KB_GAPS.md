# Knowledge Base Gaps & Exclusions Log (Rule H4 Compliance)

In strict accordance with **Rule H4** ("Never invent agronomy. Every knowledge-base entry cites at least one official source you actually read... A treatment appears only if an official source recommends it for that crop and problem; if you find none, leave it out and list it in `docs/KB_GAPS.md`"), this document records all agronomic items, practices, dosages, and treatments that were evaluated and intentionally excluded from `data/kb/knowledge_base.json` due to the lack of explicit, verified official university or ICAR recommendations.

---

## 1. Summary of Excluded Categories

| Category | Reason for Exclusion | Authoritative Policy / Source |
|---|---|---|
| **Fixed-Day Irrigation Calendars** | Fixed day counts (e.g. "water every 4 days") lead to severe root rot in heavy black soils or moisture stress in light sandy loams. ICAR/ANGRAU/PJTSAU require stage-based and moisture-based scheduling. | ICAR-IIRR, PJTSAU Vyavasaya Panchangam, ANGRAU Vyavasaya Darsini |
| **Blanket Micronutrient Recommendations** | Applying Zinc, Boron, or Iron without soil test results can induce severe phytotoxicity or micronutrient antagonism. | Soil Health Card Mission, ICAR-IISS Bhopal |
| **Unverified Chemical Dosages** | Dosages without human-verified CIB&RC label approvals are excluded to prevent chemical crop burns, environmental contamination, and farmer liability. | Rule H4, CIB&RC Major Uses of Pesticides |
| **Off-Label Chemical Tank Mixes** | Mixing fungicides with synthetic pyrethroids or organophosphates without label validation causes phytotoxicity and active ingredient degradation. | CIB&RC Regulations, NIPHM Hyderabad |
| **Commercial Biostimulants & Growth Tonics** | Proprietary humic acids, seaweed extracts, and enzyme tonics lack replicated State Agricultural University (SAU) multi-location trial validation. | ICAR-AICRP Guidelines |

---

## 2. Class-by-Class Omission and Gap Record

### Crop 1: Rice (*Oryza sativa*)

#### Class 0: `rice_bacterial_leaf_blight` (*Xanthomonas oryzae pv. oryzae*)
- **Excluded Items:**
  - *Fixed irrigation schedule*: Fixed-day irrigation was omitted; alternate wetting and drying (AWD) and immediate field drainage for 2–3 days were included based on ICAR-IIRR recommendations.
  - *Foliar chemical antibiotics (Streptomycin alone)*: Withdrawn as standalone spray per revised CIB&RC guidelines; included only as CIB&RC registered combination (Copper Oxychloride 50% WP + Streptocycline 90:10).
  - *Blanket Nitrogen application*: Excessive top-dressing omitted; nitrogen splitting into 3–4 doses with potash reinforcement retained (ICAR-IIRR).

#### Class 1: `rice_brown_spot` (*Bipolaris oryzae*)
- **Excluded Items:**
  - *Broad-spectrum systemic fungicide overdoses*: Omitted in early mild stages; nutritional correction (MOP potash, zinc sulphate) and certified seed treatments prioritized per ANGRAU guidance.
  - *Fixed water depth*: Deep standing water omitted; shallow 2–3 cm water layer specified to prevent soil drought.

#### Class 2: `rice_leaf_blast` (*Magnaporthe oryzae*)
- **Excluded Items:**
  - *Field drying during outbreak*: Strictly forbidden and omitted; drying predisposes rice to explosive blast outbreaks. Continuous 2–5 cm shallow water recommended per PJTSAU.
  - *Late nitrogen top-dressing*: Omitted per ICAR-IIRR; late urea accelerates spore germination.

#### Class 3: `rice_sheath_blight` (*Rhizoctonia solani*)
- **Excluded Items:**
  - *Deep continuous flooding*: Omitted; deep water accelerates sclerotial floatation and sheath infection up to the flag leaf. Intermittent drainage to 2 cm included.
  - *Dense planting recommendations*: High seedling density omitted; 20x15 cm spacing and skip alleys included per PJTSAU.

#### Class 4: `rice_healthy`
- **Excluded Items:**
  - *Prophylactic chemical fungicide sprays*: Strictly omitted per Good Agricultural Practices (GAP); chemical sprays on healthy crops create fungicide resistance.
  - *Fixed-dose fertilizer rates*: Blanket NPK rates omitted in favor of Soil Health Card (SHC) based Recommended Dose of Fertilizers (RDF).

---

### Crop 2: Cotton (*Gossypium hirsutum*)

#### Class 5: `cotton_bacterial_blight` (*Xanthomonas citri pv. malvacearum*)
- **Excluded Items:**
  - *Overhead sprinkler / rain-gun irrigation*: Strictly omitted; overhead water droplets splash bacterial inoculum across the canopy. Furrow drainage prioritized per ICAR-CICR.
  - *Non-delinted fuzzy seed planting*: Omitted; acid-delinting is mandatory for seed-borne blight eradication per PJTSAU.

#### Class 6: `cotton_fusarium_wilt` (*Fusarium oxysporum f. sp. vasinfectum*)
- **Excluded Items:**
  - *Foliar fungicide sprays*: Strictly omitted; vascular wilt resides inside the vascular xylem and root system and cannot be controlled by foliar sprays. Soil drenching and bioagent enrichment (Trichoderma + FYM) retained.
  - *Continuous monocropping*: Omitted; 3-year crop rotation with non-hosts (sorghum, maize, bajra) enforced per ICAR-CICR.

#### Class 7: `cotton_healthy`
- **Excluded Items:**
  - *Prophylactic chemical sprays for sucking pests*: Omitted; spraying before reaching Economic Threshold Levels (ETL) destroys natural predators (ladybird beetles, spiders). Weekly AESA scouting enforced.
  - *Flood irrigation in vertisols*: Omitted; excessive water stagnation causes root asphyxiation and square shedding.

---

### Crop 3: Chilli (*Capsicum annuum*)

#### Class 8: `chilli_bacterial_spot` (*Xanthomonas euvesicatoria*)
- **Excluded Items:**
  - *Sprinkler / mist irrigation*: Omitted; water splash transmits bacterial blisters rapidly across plants. Drip or ridge-and-furrow drainage enforced per ICAR-IIHR.
  - *Solanaceous crop rotations*: Rotating chilli after tomato, brinjal, or potato omitted due to common bacterial susceptibility.

#### Class 9: `chilli_leaf_curl_virus` (Begomovirus / Whitefly vector)
- **Excluded Items:**
  - *Curative chemical sprays*: Strictly omitted; viral diseases have no curative chemical. Vector suppression (yellow sticky traps, barrier crops) and rogueing enforced per ICAR-IIHR.
  - *High chemical nitrogen*: Omitted; succulent shoot flushes attract massive whitefly infestations.

#### Class 10: `chilli_healthy`
- **Excluded Items:**
  - *Chemical cocktail sprays*: Broad-spectrum insecticide cocktails omitted; IPM scouting with yellow and blue sticky traps retained per ANGRAU GAP.
  - *Over-irrigation during flowering*: Omitted; water excess triggers massive flower drop.

---

### Crop 4: Tomato (*Solanum lycopersicum*)

#### Class 11: `tomato_early_blight` (*Alternaria solani*)
- **Excluded Items:**
  - *Overhead sprinkler irrigation*: Omitted; wet leaves accelerate Alternaria spore germination. Drip irrigation and straw mulching to prevent soil splash retained per ICAR-IIHR.
  - *Nitrogen starvation*: Omitted; deficient plants senesce prematurely and suffer severe blight. Balanced NPK with potash specified.

#### Class 12: `tomato_late_blight` (*Phytophthora infestans*)
- **Excluded Items:**
  - *Evening and night irrigation*: Strictly omitted; overnight leaf wetness during cold, foggy weather triggers catastrophic sporulation. Morning drip irrigation enforced per ICAR-CPRI.
  - *Low-lying flat bed planting*: Omitted; raised bed or ridge planting required for rapid runoff.

#### Class 13: `tomato_yellow_leaf_curl_virus` (Begomovirus / Whitefly vector)
- **Excluded Items:**
  - *Direct curative chemicals*: Strictly omitted; viricides do not exist. Seedling protection under 40–50 mesh nets, border cropping with tall cereals, and sticky traps enforced per ICAR-IIHR.

#### Class 14: `tomato_spider_mites` (*Tetranychus urticae* - PEST)
- **Excluded Items:**
  - *Routine calendar-based synthetic pyrethroid sprays*: Strictly omitted; synthetic pyrethroids kill predatory phytoseiid mites and cause explosive spider mite resurgences.
  - *Water drought stress*: Hot, dry, dusty field conditions trigger rapid mite flare-ups; light irrigation / misting and biological control (NSKE, predatory mites) prioritized per ICAR-NBAIR.

#### Class 15: `tomato_healthy`
- **Excluded Items:**
  - *Prophylactic chemical drenches*: Omitted; trellising, sucker pruning, and balanced calcium nitrate applications retained per ICAR-IIHR GAP.

---

### Crop 5: Maize (*Zea mays*)

#### Class 16: `maize_gray_leaf_spot` (*Cercospora zeae-maydis*)
- **Excluded Items:**
  - *Late afternoon overhead sprinkler irrigation*: Omitted; prolonged canopy moisture facilitates Cercospora penetration through stomata.
  - *Zero tillage without residue management*: Surface debris harbors conidia; deep plowing and crop rotation with pulses enforced per ICAR-IIMR.

#### Class 17: `maize_common_rust` (*Puccinia sorghi*)
- **Excluded Items:**
  - *Nitrogen overdose without potassium*: Omitted; high nitrogen promotes soft leaf tissues susceptible to rust pustule eruptions.
  - *Late season planting*: Omitted; delayed planting exposes seedlings to peak airborne spore showers.

#### Class 18: `maize_northern_leaf_blight` (*Exserohilum turcicum*)
- **Excluded Items:**
  - *Monoculture rotation with sorghum or other grasses*: Omitted; Exserohilum survives on collateral graminaceous hosts. Rotation with non-grasses enforced per ICAR-IIMR.

#### Class 19: `maize_fall_armyworm` (*Spodoptera frugiperda* - PEST)
- **Excluded Items:**
  - *Indiscriminate blanket chemical sprays*: Strictly omitted; broadcasting broad-spectrum chemicals across whole plants wastes insecticide and fails to reach larvae feeding deep within the central whorl.
  - *Calendar-based chemical treatment without scouting*: Omitted; monitoring via pheromone traps (5/acre) and Economic Threshold Levels (5% at seedling, 10% at vegetative stage) enforced per ICAR-NBAIR and NIPHM. Cultural methods (sand+ash 9:1 in whorls, bird perches, handpicking) prioritized before chemical whorl applications.

#### Class 20: `maize_healthy`
- **Excluded Items:**
  - *Blanket chemical sprays*: Omitted per Good Agricultural Practices. Stage-specific irrigation (knee-high, tasseling, silking, grain filling) and split nitrogen applications enforced per ICAR-IIMR GAP.

---

## 3. Human Review Status
All 21 classes remain flagged with `needs_expert_review: true` in `data/kb/knowledge_base.json` and `app/src/main/assets/kb/knowledge_base.json` until validated by human domain experts via `docs/KB_REVIEW_CHECKLIST.md`.
