# Knowledge Base Expert Review Checklist (21 Shipped Classes)

> [!IMPORTANT]
> **Review Instructions for Domain Experts:**
> 1. **Target Reviewers:** (a) Native Telugu speaker with farming vocabulary, (b) Native Hindi speaker with farming vocabulary, (c) Agriculture Officer / Krishi Vigyan Kendra (KVK) Agronomist.
> 2. **Review Deadline:** **19 November 2026**.
> 3. **Verification Scope:** Verify language naturalness, farmer comprehension (Grade-6 reading level), agronomic accuracy of irrigation/nutrients, IPM steps for pests, and safety thresholds.
> 4. **Review Policy (Rule H4):** Any class not signed off by 19 Nov 2026 continues to display `needs_expert_review: true` in the application.

---

## 1. Summary Count Table of Shipped Classes

| Index | Class ID | English Name | Telugu Name | Hindi Name | Type | Pathogen / Cause | Official Sources Cited |
|:---:|:---|:---|:---|:---|:---:|:---|:---|
| 0 | `rice_bacterial_leaf_blight` | **Rice Bacterial Leaf Blight** | వరి బ్యాక్టీరియల్ ఆకు ఎండు తెగులు | धान का जीवाणु पत्ती झुलसा | Disease | *Xanthomonas oryzae pv. oryzae* | ANGRAU, ANGRAU Crop Production Guide, CIB&RC Major Uses, ICAR, ICAR-IIRR Hyderabad, PJTSAU |
| 1 | `rice_brown_spot` | **Rice Brown Spot** | వరి గోధుమ రంగు మచ్చ తెగులు | धान का भूरा धब्बा रोग | Disease | *Bipolaris oryzae* | ANGRAU, CIB&RC Major Uses, ICAR, PJTSAU, TNAU Agritech Portal |
| 2 | `rice_leaf_blast` | **Rice Leaf Blast** | వరి అగ్గితెగులు | धान का झुलसा रोग (ब्लास्ट) | Disease | *Magnaporthe oryzae* | ANGRAU, CIB&RC Major Uses, ICAR, NIPHM Hyderabad, PJTSAU |
| 3 | `rice_sheath_blight` | **Rice Sheath Blight** | వరి పొడ తెగులు | धान का शीथ ब्लाइट | Disease | *Rhizoctonia solani* | ANGRAU, ANGRAU Guidelines, CIB&RC Major Uses, ICAR, PJTSAU |
| 4 | `rice_healthy` | **Healthy Rice** | ఆరోగ్యకరమైన వరి | स्वस्थ धान | Healthy | *None* | ANGRAU, ICAR, ICAR-IIRR Package of Practices, PJTSAU |
| 5 | `cotton_bacterial_blight` | **Cotton Bacterial Blight** | పత్తి బాక్టీరియల్ ఆకుమచ్చ తెగులు | कपास का जीवाणु झुलसा | Disease | *Xanthomonas citri pv. malvacearum* | ANGRAU, CIB&RC Major Uses, ICAR, ICAR-CICR Nagpur, PJTSAU |
| 6 | `cotton_fusarium_wilt` | **Cotton Fusarium Wilt** | పత్తి ఎండు తెగులు | कपास का उकठा (विल्ट) रोग | Disease | *Fusarium oxysporum f. sp. vasinfectum* | ANGRAU, CIB&RC Major Uses, ICAR, ICAR-CICR Cotton Advisory, PJTSAU |
| 7 | `cotton_healthy` | **Healthy Cotton** | ఆరోగ్యకరమైన పత్తి | स्वस्थ कपास | Healthy | *None* | ANGRAU, ICAR, NIPHM Cotton IPM, PJTSAU |
| 8 | `chilli_bacterial_spot` | **Chilli Bacterial Spot** | మిరప బాక్టీరియా మచ్చ తెగులు | मिर्च का जीवाणु धब्बा रोग | Disease | *Xanthomonas campestris pv. vesicatoria* | ANGRAU, CIB&RC Major Uses, ICAR, IIHR Bengaluru, PJTSAU |
| 9 | `chilli_leaf_curl_virus` | **Chilli Leaf Curl Virus** | మిరప బొబ్బర / ఆకుముడుత తెగులు | मिर्च का लीफ कर्ल वायरस (मुरड़िया) | Disease | *Chilli leaf curl virus (ChiLCV)* | ANGRAU, CIB&RC Major Uses, ICAR, IIHR IPM for Chilli, NIPHM Advisory, PJTSAU |
| 10 | `chilli_healthy` | **Healthy Chilli** | ఆరోగ్యకరమైన మిరప | स्वस्थ मिर्च | Healthy | *None* | ANGRAU, ICAR, IIHR Chilli IPM, PJTSAU |
| 11 | `tomato_early_blight` | **Tomato Early Blight** | టమాటా అగేతి ఆకుమచ్చ తెగులు | टमाटर का अगेती झुलसा | Disease | *Alternaria solani* | ANGRAU, CIB&RC Major Uses, ICAR, IIHR Package for Solanaceous Crops, NIPHM Advisory, PJTSAU |
| 12 | `tomato_late_blight` | **Tomato Late Blight** | టమాటా పచేతి ఆకుమచ్చ తెగులు | टमाटर का पछेती झुलसा | Disease | *Phytophthora infestans* | ANGRAU, CIB&RC Major Uses, ICAR, IIHR Bengaluru, PJTSAU |
| 13 | `tomato_yellow_leaf_curl_virus` | **Tomato Yellow Leaf Curl Virus** | టమాటా ఆకుముడుత వైరస్ తెగులు | टमाटर का पीला पत्ती मरोड़ विषाणु | Disease | *Tomato yellow leaf curl virus (TYLCV)* | ANGRAU, CIB&RC Major Uses, ICAR, IIHR Tomato IPM, NIPHM Advisory, PJTSAU |
| 14 | `tomato_spider_mites` | **Tomato Spider Mites** | టమాటా ఎర్ర నల్లి | टमाटर का मकड़ी कीट (माइट्स) | Pest | *Tetranychus urticae (Two-Spotted Spider Mite)* | ANGRAU, ANGRAU Crop Production Guide, CIB&RC Major Uses, ICAR, ICAR-IIHR Tomato IPM, NBAIR Bengaluru, PJTSAU |
| 15 | `tomato_healthy` | **Healthy Tomato** | ఆరోగ్యకరమైన టమాటా | स्वस्थ टमाटर | Healthy | *None* | ANGRAU, ICAR, IIHR Tomato Guide, PJTSAU |
| 16 | `maize_gray_leaf_spot` | **Maize Gray Leaf Spot** | మొక్కజొన్న బూడిద రంగు ఆకుమచ్చ తెగులు | मक्का का ग्रे लीफ स्पॉट | Disease | *Cercospora zeae-maydis* | ANGRAU, CIB&RC Major Uses, ICAR, ICAR-IIMR Ludhiana, PJTSAU |
| 17 | `maize_common_rust` | **Maize Common Rust** | మొక్కజొన్న కుంకుమ తెగులు | मक्का का रतुआ रोग (रस्ट) | Disease | *Puccinia sorghi* | ANGRAU, CIB&RC Major Uses, ICAR, ICAR-IIMR Hybrid Directory, PJTSAU |
| 18 | `maize_northern_leaf_blight` | **Maize Northern Leaf Blight** | మొక్కజొన్న తుప్పు తెగులు / ఆకు ఎండడం | मक्का का उत्तरी पत्ती झुलसा | Disease | *Exserohilum turcicum* | ANGRAU, ANGRAU Maize Package, CIB&RC Major Uses, ICAR, PJTSAU |
| 19 | `maize_fall_armyworm` | **Maize Fall Armyworm** | మొక్కజొన్న కత్తెర పురుగు | मक्का का फाल आर्मीवर्म कीट | Pest | *Spodoptera frugiperda* | ANGRAU, CIB&RC Major Uses, ICAR, ICAR-NBAIR FAW Advisory, NIPHM FAW Management Protocol, PJTSAU, PJTSAU Maize Pest Advisory |
| 20 | `maize_healthy` | **Healthy Maize** | ఆరోగ్యకరమైన మొక్కజొన్న | स्वस्थ मक्का | Healthy | *None* | ANGRAU, ICAR, ICAR-IIMR Surveillance Guide, PJTSAU |

---

## 2. Detailed Class-by-Class Review Sheets

### Class 0: `rice_bacterial_leaf_blight`
- **Crop:** Rice | **Type:** Disease | **Scientific Cause:** *Xanthomonas oryzae pv. oryzae* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Rice Bacterial Leaf Blight
  - **Telugu:** వరి బ్యాక్టీరియల్ ఆకు ఎండు తెగులు
  - **Hindi:** धान का जीवाणु पत्ती झुलसा

#### Immediate Cultural Step
- **English:** Stop top-dressing with nitrogenous fertilizers immediately and drain excess standing field water.
- **Telugu:** నత్రజని ఎరువుల వాడకాన్ని వెంటనే ఆపండి మరియు పొలంలో నిలిచిన అదనపు నీటిని బయటకు పంపండి.
- **Hindi:** खेत में यूरिया डालना तुरंत बंद करें और खेत का अतिरिक्त पानी बाहर निकालें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIRR & PJTSAU. Practice alternate wetting and drying (AWD) rather than continuous deep flooding; drain standing water for 2-3 days during disease outbreak to aerate root zone. Strictly avoid excess urea/nitrogen top-dressing; apply recommended nitrogen in 3-4 splits. Apply balanced potassium (potash 40-50 kg/ha) at basal and panicle initiation to thicken cell walls against bacteria. Keep bunds free of weed hosts (Leersia, Cyperus).
- **Telugu:** మూలం: ICAR-IIRR & PJTSAU. నిరంతరం లోతుగా నీరు నిల్వ ఉంచకుండా ఆరుతడి (తడి-పొడి) పద్ధతిని పాటించండి; తెగులు ఉధృతి ఉన్నప్పుడు 2-3 రోజుల పాటు పొలంలో నీటిని తీసివేసి నేలకు గాలి తగలనివ్వండి. అధిక నత్రజని (యూరియా) వేయకండి; సిఫార్సు చేసిన నత్రజనిని 3-4 దఫాలుగా వేయండి. బాక్టీరియా నిరోధకత పెంచడానికి ఎకరాకు సిఫార్సు చేసిన పొటాష్ ఎరువును ఆఖరి దుక్కిలో మరియు చిరుపొట్ట దశలో వేయండి. గట్లపై కలుపు మొక్కలను తొలగించండి.
- **Hindi:** स्रोत: ICAR-IIRR और PJTSAU. लगातार खेत में पानी भरकर रखने के बजाय बारी-बारी से गीला और सूखा (AWD) रखें; रोग दिखने पर 2-3 दिन तक खेत से पानी निकालकर जड़ों को हवा लगने दें। अतिरिक्त यूरिया/नाइट्रोजन डालने से बचें; नाइट्रोजन को 3-4 भागों में बांटकर दें। जीवाणु प्रतिरोधक क्षमता बढ़ाने हेतु अनुशंसित पोटाश आखिरी जुताई और बालियां निकलने की अवस्था में अवश्य दें। मेड़ों को खरपतवार मुक्त रखें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch leaf tips and upper margins for tiny dull-green water-soaked stripes. Look for translucent wavy yellow-white stripes drying downward, and inspect for milky bacterial ooze beads on young leaves in early morning dew.
- **Telugu:** ఆకుల కొనలు మరియు అంచుల వెంట నీటితో తడిసిన చిన్న ఆకుపచ్చని చారలను గమనించండి. పసుపు-తెలుపు అలల తరంగాల వంటి చారలు క్రిందికి విస్తరిస్తున్నాయో లేదో మరియు తెల్లవారుజామున మంచు సమయంలో ఆకులపై ముత్యాల వంటి బాక్టీరియా జిగురు బిందువులు కనిపిస్తున్నాయో లేదో చూడండి.
- **Hindi:** पत्तियों के किनारों और सिरों पर हल्के हरे जल-युक्त धब्बों पर नजर रखें। देखें कि क्या पीली-सफेद लहरदार धारियां नीचे की ओर बढ़ रही हैं और सुबह की ओस में पत्तियों पर दूधिया जीवाणु स्राव की बूंदें दिख रही हैं या नहीं।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if water-soaked wavy lesions affect >20% of tillers, kresek seedling wilting appears, or lesions reach the flag leaf. Contact local KVK or Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** 20% కంటే ఎక్కువ పిలకలకు అలల తరంగాల మచ్చలు సోకినప్పుడు, నారు లేదా పిలక దశలో క్రెసెక్ (మొక్కలు ఎండిపోవడం) కనిపించినప్పుడు లేదా మచ్చలు జెండా ఆకుకు చేరినప్పుడు వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా స్థానిక KVK కి కాల్ చేయండి.
- **Hindi:** यदि 20% से अधिक कल्ले रोगग्रस्त हों, पत्तियों पर लहरदार धारियां ध्वज पत्ती (फ्लैग लीफ) तक पहुंच जाएं या क्रेसेक (पौधों का सूखना) दिखे, तो तुरंत कृषि विस्तार अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) या स्थानीय KVK पर कॉल करें।

#### Natural & Cultural Treatments
- **Step 1:** Drain standing field water and allow soil to aerate for 2-3 days.
  - *Telugu:* పొలంలో నిలిచిన నీటిని తీసివేసి 2-3 రోజుల పాటు నేలను ఆరనివ్వండి.
  - *Hindi:* खेत से पानी निकालकर 2-3 दिन तक धूप लगने दें।
  - *Citation:* ICAR-IIRR Hyderabad ([Source Link](https://icar-iirr.org)) — *"Intermittent drying of field suppresses bacterial multiplication."*
- **Step 2:** Apply fresh cow-dung filtrate (20 g/L) or neem seed kernel extract (NSKE 5%).
  - *Telugu:* తాజా ఆవు పేడ నీరు (20 గ్రా/లీటరు) లేదా వేప గింజల కషాయం (5%) పిచికారీ చేయండి.
  - *Hindi:* ताजा गाय के गोबर का छाना हुआ घोल (20 ग्राम/लीटर) या 5% नीम गिरी का काढ़ा छिड़कें।
  - *Citation:* ANGRAU Crop Production Guide ([Source Link](https://angrau.ac.in)) — *"Cow dung slurry spray acts as an effective traditional bio-bactericide."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Copper Oxychloride 50% WP + Streptocycline (90:10)` (Example: `Blitox 50 WP + Streptocycline`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear mask and gloves during spray. Keep cattle away from sprayed area.
  - *Safety Line (TE):* పిచికారీ సమయంలో మాస్క్, గ్లౌజులు ధరించండి. పశువులను మందు చల్లిన పొలానికి దూరంగా ఉంచండి.
  - *Safety Line (HI):* छिड़काव करते समय मास्क और दस्ताने पहनें। पशुओं को दूर रखें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for bacterial leaf blight in rice."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 1: `rice_brown_spot`
- **Crop:** Rice | **Type:** Disease | **Scientific Cause:** *Bipolaris oryzae* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Rice Brown Spot
  - **Telugu:** వరి గోధుమ రంగు మచ్చ తెగులు
  - **Hindi:** धान का भूरा धब्बा रोग

#### Immediate Cultural Step
- **English:** Apply potash and micronutrients (Zinc sulphate) as brown spot is strongly linked to soil nutrient deficiency.
- **Telugu:** పొటాష్ మరియు జింక్ సల్ఫేట్ వంటి సూక్ష్మపోషకాలను అందించండి, ఎందుకంటే పోషక లోపం వల్ల ఈ తెగులు వేగంగా వ్యాపిస్తుంది.
- **Hindi:** पोटाश और जिंक सल्फेट खाद दें, क्योंकि यह बीमारी मिट्टी में पोषक तत्वों की कमी से तेजी से फैलती है।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIRR & ANGRAU. Avoid water stress and dry soil conditions in rainfed plots; maintain 2-3 cm shallow water level throughout vegetative phase. Correct nutritional starvation in poor/sandy soils: apply balanced NPK, apply full recommended potash, and apply zinc sulphate (25 kg/ha) at transplanting. Treat seeds with Trichoderma viride (10 g/kg) or hot water before sowing.
- **Telugu:** మూలం: ICAR-IIRR & ANGRAU. పొలాన్ని ఎండనివ్వకుండా తేమ ఒత్తిడి లేకుండా చూడండి; పైరు ఎదుగుదల దశలో 2-3 సెం.మీ మేర పల్చటి నీటి నిల్వను నిర్వహించండి. పోషకాహార లోపం ఉన్న చౌడు/ఇసుక నేలల్లో సమతుల్య NPK ఎరువులతో పాటు సిఫార్సు చేసిన పొటాష్ మరియు ఆఖరి దుక్కిలో ఎకరాకు 10-12 కిలోల జింక్ సల్ఫేట్ వేయండి. విత్తన శుద్ధి కోసం కిలో విత్తనానికి 10 గ్రాముల ట్రైకోడెర్మా విరిడె వాడండి.
- **Hindi:** स्रोत: ICAR-IIRR और ANGRAU. खेत में नमी की कमी न होने दें; वानस्पतिक वृद्धि अवस्था में 2-3 सेमी उथला पानी बनाए रखें। कमजोर या रेतीली मिट्टी में पोषक तत्वों की कमी दूर करें: संतुलित NPK के साथ अनुशंसित पोटाश और रोपाई के समय जिंक सल्फेट (25 किग्रा/हेक्टेयर) डालें। बुवाई से पूर्व ट्राइकोडर्मा विरिडी (10 ग्राम/किग्रा) से बीज उपचार करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for small, circular to oval sesame-seed-sized dark brown spots with gray-white centers and yellow halos on lower leaves. Inspect for spread to upper leaves and glumes.
- **Telugu:** క్రింది ఆకులపై నువ్వుల గింజ ఆకారంలో, మధ్యలో బూడిద-తెలుపు రంగు మరియు చుట్టూ పసుపు రంగు వలయం కలిగిన చిన్న గోధుమ రంగు మచ్చలను గమనించండి. మచ్చలు పై ఆకులకు మరియు గింజలపైకి విస్తరిస్తున్నాయో లేదో పరిశీలించండి.
- **Hindi:** निचली पत्तियों पर तिल के आकार के छोटे, गोल से अंडाकार गहरे भूरे धब्बे देखें जिनका केंद्र भूरा-सफेद और किनारा पीला होता है। देखें कि धब्बे ऊपरी पत्तियों और बालियों के दानों पर तो नहीं फैल रहे हैं।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if spots cover >15% of flag leaf area before grain filling or grain discoloration spreads across panicles despite fertilizer application. Contact Kisan Call Centre (Toll-Free 1800-180-1551) or local KVK.
- **Telugu:** గింజ పాలుపోసుకునే దశకు ముందే జెండా ఆకులో 15% కంటే ఎక్కువ భాగం మచ్చలు కప్పివేసినా లేదా ఎరువులు వేసినా గింజలపై మచ్చలు వ్యాపిస్తున్నట్లయితే వ్యవసాయ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా KVK ని సంప్రదించండి.
- **Hindi:** यदि दाना भरने से पहले ध्वज पत्ती का 15% से अधिक हिस्सा धब्बों से ढक जाए या दानों पर भूरापन फैलने लगे, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) या स्थानीय KVK पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Foliar application of Pseudomonas fluorescens (5-10 g/L) at boot leaf stage.
  - *Telugu:* సూడోమోనాస్ ఫ్లోరోసెన్స్ (5-10 గ్రా/లీ) చిరుపొట్ట దశలో ఆకులపై పిచికారీ చేయండి.
  - *Hindi:* स्यूडोमोनास फ्लोरोसेंस (5-10 ग्राम/लीटर) का घोल पत्तियों पर छिड़कें।
  - *Citation:* TNAU Agritech Portal ([Source Link](https://agritech.tnau.ac.in)) — *"Bio-priming and foliar spray of Pseudomonas suppresses Bipolaris."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Mancozeb 75% WP or Propiconazole 25% EC` (Example: `Dithane M-45 / Tilt`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Avoid inhaling spray mist. Keep protective visor on during spraying.
  - *Safety Line (TE):* మందు పొగ లేదా తుంపర పీల్చవద్దు. ముఖానికి కవచం ధరించండి.
  - *Safety Line (HI):* दवा का धुआं या फुहार सांस में न जाने दें। मुंह पर मास्क रखें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved for brown spot in paddy."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 2: `rice_leaf_blast`
- **Crop:** Rice | **Type:** Disease | **Scientific Cause:** *Magnaporthe oryzae* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Rice Leaf Blast
  - **Telugu:** వరి అగ్గితెగులు
  - **Hindi:** धान का झुलसा रोग (ब्लास्ट)

#### Immediate Cultural Step
- **English:** Do not apply urea when blast spots are visible; spray protective fungicide in early morning or late afternoon.
- **Telugu:** అగ్గితెగులు మచ్చలు కనిపించినప్పుడు యూరియా వేయవద్దు; ఉదయం లేదా సాయంత్రం వేళల్లో మందు పిచికారీ చేయండి.
- **Hindi:** ब्लास्ट के धब्बे दिखने पर यूरिया बिल्कुल न डालें; सुबह या शाम के समय अनुशंसित दवा छिड़कें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIRR & PJTSAU. Maintain a continuous 2-5 cm shallow water layer; never let the field dry or crack, as drought stress greatly increases blast susceptibility. Strictly avoid heavy or late top-dressing of nitrogen/urea; apply nitrogen in 3 equal splits and balance with recommended potash to strengthen leaf epidermis. Remove grassy weed hosts (Echinochloa) from field bunds.
- **Telugu:** మూలం: ICAR-IIRR & PJTSAU. పొలంలో నిరంతరం 2-5 సెం.మీ మేర పలుచని నీటి పొరను నిర్వహించండి; పొలాన్ని ఎండనివ్వడం లేదా నెర్రలు రానివ్వడం చేయవద్దు, తేమ లేకపోతే అగ్గి తెగులు తీవ్రంగా పెరుగుతుంది. అధిక లేదా ఆలస్యంగా యూరియా వేయడం వెంటనే ఆపండి; నత్రజనిని 3 సమ భాగాలుగా వేయండి మరియు ఆకులను దృఢపరచడానికి సిఫార్సు చేసిన పొటాష్ ఎరువును తప్పనిసరిగా వాడండి. గట్లపై ఉన్న తుంగ, గడ్డి కలుపును తొలగించండి.
- **Hindi:** स्रोत: ICAR-IIRR और PJTSAU. खेत में 2-5 सेमी पानी बनाए रखें; खेत को सूखने या दरारें न पड़ने दें, क्योंकि सूखे से झोंका (ब्लास्ट) रोग तेजी से फैलता है। यूरिया/नाइट्रोजन का अधिक या देर से छिड़काव बिल्कुल न करें; नाइट्रोजन को 3 बराबर किस्तों में दें और पत्तियों की मजबूती हेतु पोटाश का संतुलित प्रयोग करें। मेड़ों से घास वाले खरपतवार हटाएं।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for small spindle-shaped (diamond-shaped or eye-shaped) spots with pointed ends, gray/ash center, and dark brown or reddish borders on leaf blades during humid, overcast weather with heavy morning dew.
- **Telugu:** మబ్బులతో కూడిన వాతావరణం మరియు ఉదయం మంచు కురిసే రోజుల్లో ఆకులపై రెండు చివర్లు మొనదేలిన కంటి ఆకారపు (కండె ఆకారపు) చిన్న మచ్చలు, మధ్యలో బూడిద రంగు మరియు అంచుల్లో ముదురు గోధుమ రంగు అంచులు కనిపిస్తున్నాయో లేదో గమనించండి.
- **Hindi:** अधिक नमी और बादल छाए रहने पर पत्तियों पर आंख या तकुए (स्पिंडल) के आकार के छोटे धब्बे देखें, जिनका केंद्र राख जैसा भूरा और किनारे गहरे लाल-भूरे होते हैं।

#### (c) When to Call Extension Officer
- **English:** Call extension officer IMMEDIATELY if spindle lesions cover >5% leaf area during tillering or if blackish lesions appear at neck nodes of panicles (neck blast), as blast can destroy fields within days. Call Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** పిలకల దశలో కంటి ఆకారపు మచ్చలు 5% కంటే ఎక్కువ ఆకు విస్తీర్ణాన్ని కప్పివేసినా లేదా వెన్ను మెడ వద్ద నల్లని మచ్చలు (మెడ విరుపు తెగులు) కనిపించినా తక్షణమే వ్యవసాయ అధికారిని సంప్రదించండి, ఎందుకంటే అగ్గి తెగులు కొద్ది రోజుల్లోనే పంటను పూర్తిగా నాశనం చేస్తుంది. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** यदि कल्ले फूटते समय स्पिंडल धब्बे 5% से अधिक पत्ती क्षेत्र पर फैल जाएं या बालियों की गर्दन पर काले धब्बे (गर्दन तोड़) दिखें, तो तुरंत कृषि अधिकारी को बुलाएं। यह रोग कुछ ही दिनों में पूरी फसल नष्ट कर सकता है। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Spray neem oil (3-5 ml/L) or NSKE 5% at first appearance of spots.
  - *Telugu:* మచ్చలు మొదలైన వెంటనే వేప నూనె (3-5 మి.లీ/లీ) లేదా వేప కషాయం పిచికారీ చేయండి.
  - *Hindi:* शुरुआती लक्षण दिखते ही नीम का तेल (3-5 मिली/लीटर) छिड़कें।
  - *Citation:* NIPHM Hyderabad ([Source Link](https://niphm.gov.in)) — *"Botanicals and Trichoderma suppress early sporulation of Magnaporthe."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Tricyclazole 75% WP` (Example: `Beam 75 WP / Sivic`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear protective gear. Observe pre-harvest interval strictly.
  - *Safety Line (TE):* రక్షణ దుస్తులు ధరించండి. కోతకు ముందు నిర్దేశించిన రోజుల వ్యవధి పాటించండి.
  - *Safety Line (HI):* सुरक्षात्मक पोशाक पहनें। कटाई से पहले प्रतीक्षा समय का पालन करें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Tricyclazole registered specifically for paddy blast control in India."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 3: `rice_sheath_blight`
- **Crop:** Rice | **Type:** Disease | **Scientific Cause:** *Rhizoctonia solani* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Rice Sheath Blight
  - **Telugu:** వరి పొడ తెగులు
  - **Hindi:** धान का शीथ ब्लाइट

#### Immediate Cultural Step
- **English:** Form alleys (aeration pathways) every 2 meters in the crop and direct the spray nozzle toward the lower plant sheaths.
- **Telugu:** పైరులో ప్రతి 2 మీటర్లకు గాలి బాటలు తీయండి మరియు పిచికారీ నాజిల్ మొక్కల అడుగు భాగానికి తగిలేలా మందు చల్లండి.
- **Hindi:** घने खेत में हर 2 मीटर पर हवा के लिए जगह बनाएं और स्प्रे का रुख पौधों के निचले हिस्से की तरफ रखें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIRR & PJTSAU. Drain standing water to 2 cm and aerate the field periodically; avoid stagnant deep flooding. Avoid close planting (maintain 20x15 cm spacing or provide alleyways every 2 meters for aeration). Avoid excess nitrogen application; split nitrogen into 3-4 doses and apply full recommended potash at basal and panicle initiation. Skim floating sclerotia during puddling.
- **Telugu:** మూలం: ICAR-IIRR & PJTSAU. నీటి మట్టాన్ని 2 సెం.మీ తగ్గించి పొలానికి గాలి ఆడేలా చేయండి; లోతుగా నిలిచిన నీటిని తీసివేయండి. దట్టంగా నాటకండి (20x15 సెం.మీ దూరం పాటించండి లేదా ప్రతి 2 మీటర్లకు దారులు వదలండి). అధిక నత్రజని వేయకండి; నత్రజనిని 3-4 సార్లు వేయండి మరియు సిఫార్సు చేసిన పొటాష్‌ను ఆఖరి దుక్కిలో మరియు చిరుపొట్ట దశలో వేయండి. దమ్ము చేసేటప్పుడు నీటిపై తేలే శిలీంధ్ర గుళికలను జల్లెడతో తొలగించి నాశనం చేయండి.
- **Hindi:** स्रोत: ICAR-IIRR और PJTSAU. खेत में पानी का स्तर 2 सेमी तक कम करें और बीच-बीच में हवा लगने दें; गहरे रुके हुए पानी से बचें। अत्यधिक घनी रोपाई न करें (20x15 सेमी दूरी रखें या हवा के लिए हर 2 मीटर पर गलियां छोड़ें)। अधिक यूरिया न डालें; नाइट्रोजन 3-4 भागों में दें और पोटाश की पूरी खुराक दें। कद्दू (लेव) करते समय पानी पर तैरने वाले फफूंद अवशेषों को छानकर नष्ट करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for oval, water-soaked, greenish-gray snake-skin-like banded lesions on leaf sheaths just above the water level. Inspect whether lesions are advancing upward toward the flag leaf.
- **Telugu:** నీటి మట్టానికి కాస్త పైన ఉన్న ఆకు తొడుగులపై పాము చర్మం వంటి పసుపు-బూడిద రంగు మచ్చలు మరియు అలల తరంగాల వంటి పట్టీలను గమనించండి. ఈ మచ్చలు కాండం మీదుగా పైకి జెండా ఆకు వైపు పాకుతున్నాయో లేదో పరిశీలించండి.
- **Hindi:** पानी की सतह के ठीक ऊपर तने/पत्ती के आवरण पर सांप की केंचुली जैसे अंडाकार, हरे-भूरे लहरदार धब्बे देखें। जांचें कि क्या ये धब्बे ऊपर की ओर फ्लैग लीफ की तरफ बढ़ रहे हैं।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if banded sheath lesions reach the penultimate or flag leaf sheath on >20% of hills, threatening grain filling. Contact Kisan Call Centre (Toll-Free 1800-180-1551) or local KVK.
- **Telugu:** 20% కంటే ఎక్కువ కుదుళ్ళలో పాముపొడ మచ్చలు రెండవ పై ఆకు తొడుగుకు లేదా జెండా ఆకుకు చేరినప్పుడు వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా KVK ని సంప్రదించండి.
- **Hindi:** यदि 20% से अधिक पौधों में आवरण झुलसा के धब्बे ऊपर की दूसरी पत्ती या फ्लैग लीफ तक पहुंच जाएं, तो तुरंत कृषि विस्तार अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) या स्थानीय KVK पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Apply Pseudomonas fluorescens or Trichoderma harzianum enriched farmyard manure to soil.
  - *Telugu:* సూడోమోనాస్ లేదా ట్రైకోడెర్మా కలిపిన పశువుల ఎరువును నేలలో వేయండి.
  - *Hindi:* ट्राइकोडर्मा या स्यूडोमोनास से उपचारित गोबर की खाद खेत में डालें।
  - *Citation:* ANGRAU Guidelines ([Source Link](https://angrau.ac.in)) — *"Soil enrichment with bio-antagonists suppresses sclerotial germination."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Hexaconazole 5% SC or Validamycin 3% L` (Example: `Contaf 5 SC / Sheathmar`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Direct spray to lower stem sheaths. Wear boots and protective eye-wear.
  - *Safety Line (TE):* మొక్కల మొదళ్ల వద్దకు మందు చేరేలా చల్లండి. రబ్బరు బూట్లు, కళ్ళజోడు ధరించండి.
  - *Safety Line (HI):* पौधों की जड़ों और तनों के निचले हिस्से पर छिड़कें। गमबूट और चश्मा पहनें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for sheath blight in rice crop."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 4: `rice_healthy`
- **Crop:** Rice | **Type:** Healthy | **Scientific Cause:** *None* | **Referral Threshold:** `NONE`
- **Names:**
  - **English:** Healthy Rice
  - **Telugu:** ఆరోగ్యకరమైన వరి
  - **Hindi:** स्वस्थ धान

#### Immediate Cultural Step
- **English:** No chemical treatment needed. Maintain optimal water depth (2-5 cm) and balanced NPK fertilization.
- **Telugu:** ఎటువంటి రసాయన మందులు అవసరం లేదు. పొలంలో 2-5 సెం.మీ నీరు ఉంచి తగిన మోతాదులో ఎరువులు వేయండి.
- **Hindi:** किसी रासायनिक दवा की जरूरत नहीं है। उचित जल स्तर (2-5 सेमी) और संतुलित खाद बनाए रखें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIRR & PJTSAU GAP. Practice alternate wetting and drying (AWD) after tillering to conserve water and strengthen roots. Apply fertilizers strictly based on Soil Health Card; split nitrogen (50% basal, 25% active tillering, 25% panicle initiation). Incorporate green manure (Daincha/Sunnhemp) before puddling and maintain clean, weed-free bunds.
- **Telugu:** మూలం: ICAR-IIRR & PJTSAU GAP. నీటిని ఆదా చేయడానికి మరియు వేర్లు దృఢంగా ఉండటానికి పిలకల దశ తర్వాత ఆరుతడి (తడి-పొడి) పద్ధతిని పాటించండి. సాయిల్ హెల్త్ కార్డు ఆధారంగా మాత్రమే ఎరువులు వేయండి; నత్రజనిని సమపాళ్లలో విభజించండి (50% ఆఖరి దుక్కిలో, 25% పిలకల దశలో, 25% చిరుపొట్ట దశలో). దమ్ముకు ముందు జనుము లేదా జీలుగా వంటి పచ్చిరొట్ట ఎరువులను కలియదున్నండి మరియు గట్లను కలుపు లేకుండా ఉంచండి.
- **Hindi:** स्रोत: ICAR-IIRR और PJTSAU GAP. पानी की बचत और जड़ों के विकास हेतु कल्ले फूटने के बाद बारी-बारी से गीला-सूखा (AWD) रखें। मृदा स्वास्थ्य कार्ड के अनुसार ही संतुलित उर्वरक दें; नाइट्रोजन को 3 भागों में दें (50% बेसल, 25% कल्ले फूटते समय, 25% बाली बनते समय)। जुताई से पूर्व ढैंचा या सनई जैसी हरी खाद मिट्टी में मिलाएं और मेड़ों को साफ रखें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Canopy is uniform green and healthy. Regularly scout for leaf tip browning, yellow stripes, spindle spots, or sheath patches after heavy rains or overcast humid spells.
- **Telugu:** పైరు సమానమైన ఆకుపచ్చ రంగుతో ఆరోగ్యంగా ఉంది. భారీ వర్షాలు లేదా మేఘావృతమైన తేమతో కూడిన వాతావరణం తర్వాత ఆకుల కొనలు ఎండటం, పసుపు చారలు, కంటి ఆకారపు మచ్చలు లేదా తొడుగు మచ్చలు ఏమైనా కనిపిస్తున్నాయేమో క్రమం తప్పకుండా గమనించండి.
- **Hindi:** फसल एकसमान हरी और स्वस्थ है। भारी बारिश या नमी वाले बादलों के बाद पत्तियों के सिरे सूखने, पीली धारियों, स्पिंडल धब्बों या तने पर चकत्तों की नियमित निगरानी करते रहें।

#### (c) When to Call Extension Officer
- **English:** Routine guidance only. Contact local KVK or Kisan Call Centre (Toll-Free 1800-180-1551) for seasonal recommendations on panicle stage nutrient management and grain filling water schedules.
- **Telugu:** సాధారణ సలహాల కొరకు మాత్రమే. చిరుపొట్ట దశలో ఎరువుల యాజమాన్యం మరియు గింజ పాలుపోసుకునే దశలో నీటి నిర్వహణపై కాలానుగుణ సిఫార్సుల కోసం స్థానిక KVK లేదా కిసాన్ కాల్ సెంటర్ (1800-180-1551) ను సంప్రదించండి.
- **Hindi:** केवल सामान्य मार्गदर्शन हेतु। बालियां निकलते समय पोषण प्रबंधन और दाना भरते समय सिंचाई कार्यक्रम की मौसमी सलाह के लिए स्थानीय KVK या किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Perform regular field scouting twice a week to detect any early pest or disease symptoms.
  - *Telugu:* ఏవైనా తెగుళ్ళు ఆశిస్తున్నాయేమో చూడటానికి వారానికి రెండుసార్లు పొలాన్ని పరిశీలించండి.
  - *Hindi:* सप्ताह में दो बार खेत का निरीक्षण करें ताकि किसी बीमारी की शुरुआत पहचानी जा सके।
  - *Citation:* ICAR-IIRR Package of Practices ([Source Link](https://icar-iirr.org)) — *"Regular scouting prevents disease outbreaks."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 5: `cotton_bacterial_blight`
- **Crop:** Cotton | **Type:** Disease | **Scientific Cause:** *Xanthomonas citri pv. malvacearum* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Cotton Bacterial Blight
  - **Telugu:** పత్తి బాక్టీరియల్ ఆకుమచ్చ తెగులు
  - **Hindi:** कपास का जीवाणु झुलसा

#### Immediate Cultural Step
- **English:** Avoid overhead sprinkler irrigation which spreads bacteria, and collect fallen infected leaves.
- **Telugu:** బాక్టీరియా వ్యాప్తి చెందకుండా తుంపర సేద్యం ఆపండి, తెగులు సోకి రాలిన ఆకులను ఏరి కాల్చండి.
- **Hindi:** ऊपर से पानी का छिड़काव न करें जिससे जीवाणु फैलें, और गिरी हुई संक्रमित पत्तियों को नष्ट करें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-CICR Nagpur & PJTSAU. Avoid overhead or sprinkler irrigation; provide proper furrow drainage to prevent waterlogging in black soils. Avoid excessive nitrogen fertilizer; maintain balanced potassium nutrition. Use acid-delinted certified seed treated with carboxin/thiram or hot water. Burn or bury infected cotton stalks after harvest and do not enter field when plants are wet with morning dew.
- **Telugu:** మూలం: ICAR-CICR & PJTSAU. స్ప్రింక్లర్ లేదా పైనుంచి నీరు చిలకరించే పద్ధతులను నివారించండి; నల్లరేగడి నేలల్లో నీరు నిల్వ ఉండకుండా సాళ్ల మధ్య సరైన డ్రైనేజీ కాలువలు తీయండి. అధిక నత్రజని ఎరువులు వేయవద్దు; సమతుల్య పొటాష్‌ను అందించండి. యాసిడ్ డీలింటింగ్ చేసి విత్తన శుద్ధి చేసిన ధృవీకృత విత్తనాలను మాత్రమే వాడండి. పంట కోత తర్వాత తెగులు సోకిన మోడులను కాల్చివేయండి లేదా లోతుగా పాతిపెట్టండి; ఉదయం మంచు ఉన్నప్పుడు పొలంలో పనులు చేయవద్దు.
- **Hindi:** स्रोत: ICAR-CICR और PJTSAU. फव्वारा सिंचाई से बचें; काली मिट्टी में जलभराव रोकने के लिए नालियों द्वारा उचित जल निकासी रखें। अधिक नाइट्रोजन न डालें; संतुलित पोटाश दें। एसिड-डिलिंटेड और उपचारित प्रमाणित बीजों का ही उपयोग करें। कटाई के बाद रोगग्रस्त डंठलों को नष्ट करें या गहरा दबाएं; सुबह की ओस में फसल के बीच काम करने से बचें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch lower leaf undersides for tiny, dark green water-soaked angular spots delimited by veinlets. Watch for dark purplish-black cankers on petioles and stems (black arm phase) and water-soaked oily spots on developing green bolls.
- **Telugu:** ఆకుల అడుగు భాగంలో ఈనెల మధ్య ఏర్పడే చిన్న, నీటితో తడిసిన కోణీయ (యాంగ్యులర్) నూనె మచ్చలను గమనించండి. కొమ్మలు మరియు కాండంపై నల్లని చారల పుండ్లు (బ్లాక్ ఆర్మ్ దశ) మరియు కాయలపై నూనె వంటి మచ్చలు కనిపిస్తున్నాయేమో పరిశీలించండి.
- **Hindi:** पत्तियों की निचली सतह पर नसों के बीच बने छोटे, कोणीय (angular) तैलीय जल-युक्त धब्बों पर ध्यान दें। शाखाओं और तनों पर काले घाव (ब्लैक आर्म अवस्था) और गूलरों पर तैलीय धब्बे देखें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if black-arm cankers girdle >10% of main vegetative branches or water-soaked oily spots appear on developing bolls. Contact Kisan Call Centre (Toll-Free 1800-180-1551) or local KVK.
- **Telugu:** 10% కంటే ఎక్కువ ప్రధాన కొమ్మలపై నల్లటి పుండ్లు ఏర్పడి కొమ్మలు విరిగిపోతున్నా లేదా పసి కాయలపై నూనె మచ్చలు విస్తరిస్తున్నా వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా KVK ని సంప్రదించండి.
- **Hindi:** यदि मुख्य शाखाओं के 10% से अधिक हिस्से पर काले घाव (ब्लैक आर्म) फैल जाएं या बनते हुए गूलरों पर तैलीय धब्बे दिखाई दें, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Foliar spray with Pseudomonas fluorescens (10 g/L) at vegetative stage.
  - *Telugu:* పైరు శాఖీయ దశలో సూడోమోనాస్ ఫ్లోరోసెన్స్ (10 గ్రా/లీ) పిచికారీ చేయండి.
  - *Hindi:* स्यूडोमोनास फ्लोरोसेंस (10 ग्राम/लीटर) का पत्तियों पर छिड़काव करें।
  - *Citation:* ICAR-CICR Nagpur ([Source Link](https://cicr.icar.gov.in)) — *"Bio-formulation reduces leaf spot severity in cotton."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Copper Oxychloride 50% WP + Streptocycline` (Example: `Blitox + Streptocycline`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear protective gear. Keep livestock away from sprayed fields.
  - *Safety Line (TE):* రక్షణ తొడుగులు ధరించండి. పిచికారీ చేసిన పొలంలో పశువులను మేపవద్దు.
  - *Safety Line (HI):* सुरक्षात्मक किट पहनें। पशुओं को छिड़के गए खेत से दूर रखें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved combination for bacterial blight in cotton."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 6: `cotton_fusarium_wilt`
- **Crop:** Cotton | **Type:** Disease | **Scientific Cause:** *Fusarium oxysporum f. sp. vasinfectum* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Cotton Fusarium Wilt
  - **Telugu:** పత్తి ఎండు తెగులు
  - **Hindi:** कपास का उकठा (विल्ट) रोग

#### Immediate Cultural Step
- **English:** Uproot and burn wilted plants; drench the root zone of neighbouring healthy plants to prevent soil spread.
- **Telugu:** ఎండిపోయిన మొక్కలను వేర్లతో సహా పీకి తగులబెట్టండి; పక్క మొక్కల వేర్ల వద్ద మందు ద్రావణాన్ని తడపండి.
- **Hindi:** मुरझाए पौधों को उखाड़कर नष्ट करें और आसपास के पौधों की जड़ों में अनुशंसित दवा का घोल डालें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-CICR Nagpur & PJTSAU. Ensure good soil drainage; avoid excess furrow irrigation in heavy black clay soils; never allow runoff water from wilted patches to drain into healthy areas. Apply well-rotted farmyard manure (FYM) enriched with Trichoderma viride (2 kg in 50 kg FYM per acre). Practice 3-year crop rotation with non-host crops (sorghum, maize, or bajra); deep summer plowing to expose chlamydospores.
- **Telugu:** మూలం: ICAR-CICR & PJTSAU. సరైన నేల డ్రైనేజీని నిర్వహించండి; నల్లరేగడి నేలల్లో మితిమీరిన నీటి తడులు ఇవ్వవద్దు; తెగులు సోకిన ప్రదేశం నుండి పారే నీరు ఆరోగ్యకరమైన పొలానికి వెళ్లకుండా కట్టలు వేయండి. ఎకరాకు 2 కిలోల ట్రైకోడెర్మా విరిడె కలిపిన చివికిన పశువుల ఎరువును వేయండి. జొన్న, మొక్కజొన్న లేదా సజ్జ వంటి పైర్లతో 3 సంవత్సరాల పంట మార్పిడి చేయండి; వేసవిలో లోతు దుక్కులు చేయండి.
- **Hindi:** स्रोत: ICAR-CICR और PJTSAU. उचित जल निकासी रखें; भारी काली मिट्टी में अत्यधिक सिंचाई न करें; विल्ट प्रभावित क्षेत्र का अपवाह पानी स्वस्थ पौधों की तरफ न जाने दें। प्रति एकड़ 2 किग्रा ट्राइकोडर्मा विरिडी से समृद्ध सड़ी गोबर की खाद डालें। ज्वार, मक्का या बाजरा के साथ 3 साल का फसल चक्र अपनाएं; गर्मियों में गहरी जुताई करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for loss of leaf turgor and midday drooping of foliage. Look for yellowing and browning starting from lower leaf margins progressing inward, and inspect split lower stems for a dark brown discolored vascular ring.
- **Telugu:** మధ్యాహ్న ఎండ వేళల్లో ఆకులు వాలిపోవడం మరియు వాడిపోవడాన్ని గమనించండి. క్రింది ఆకుల అంచుల నుండి పసుపు మరియు గోధుమ రంగులోకి మారి ఎండిపోవడం ప్రారంభమౌతుందో చూడండి. కాండం క్రింది భాగాన్ని నిలువుగా చీల్చి చూస్తే లోపలి నాళాల భాగం ముదురు గోధుమ లేదా నల్లగా మారడాన్ని పరిశీలించండి.
- **Hindi:** दोपहर की धूप में पत्तियों का मुरझाना और लटकना देखें। निचली पत्तियों के किनारों से शुरू होकर पीलापन और भूरापन अंदर की ओर बढ़ते देखें, तथा तने को चीरकर देखने पर अंदर की संवहनी नलियों में भूरा या काला घेरा जांचें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer IMMEDIATELY upon noticing wilting patches in the field; soil-borne vascular wilt cannot be cured by foliar sprays and requires soil quarantine and drenching guidance. Call Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** పొలంలో మొక్కలు వడలిపోయి ఎండిపోతున్న పాచెస్‌ను గమనించిన వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి; ఇది నేల ద్వారా వ్యాపించే తెగులు కాబట్టి ఆకులపై మందులు పిచికారీ చేయడం వల్ల ప్రయోజనం ఉండదు, నేల శుద్ధి మరియు నివారణ మార్గదర్శకత్వం అవసరం. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** खेत में पौधों के मुरझाने के पैच दिखते ही तुरंत कृषि अधिकारी से संपर्क करें; मिट्टी जनित होने के कारण पत्तियों पर छिड़काव से यह ठीक नहीं होता और मिट्टी के उपचार की सलाह जरूरी है। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Apply Trichoderma viride or T. harzianum (2.5 kg mixed with 250 kg well-decomposed FYM per acre).
  - *Telugu:* ఎకరాకు 2.5 కిలోల ట్రైకోడెర్మాను 250 కిలోల పశువుల ఎరువుతో కలిపి నేలలో వేయండి.
  - *Hindi:* प्रति एकड़ 2.5 किग्रा ट्राइकोडर्मा को 250 किग्रा सड़ी गोबर खाद में मिलाकर डालें।
  - *Citation:* ICAR-CICR Cotton Advisory ([Source Link](https://cicr.icar.gov.in)) — *"Trichoderma bio-agent antagonizes Fusarium wilt pathogen in soil."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Carbendazim 50% WP (Root drenching)` (Example: `Bavistin 50 WP`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Drench at the base of affected plants. Do not contaminate drinking water bodies.
  - *Safety Line (TE):* మొక్కల మొదళ్ల వద్ద మాత్రమే తడపండి. తాగునీటి వనరులలో మందు కలవకుండా జాగ్రత్తపడండి.
  - *Safety Line (HI):* पौधों की जड़ों के पास ही घोल डालें। पानी के स्रोतों को दूषित न करें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for soil drenching against Fusarium in cotton."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 7: `cotton_healthy`
- **Crop:** Cotton | **Type:** Healthy | **Scientific Cause:** *None* | **Referral Threshold:** `NONE`
- **Names:**
  - **English:** Healthy Cotton
  - **Telugu:** ఆరోగ్యకరమైన పత్తి
  - **Hindi:** स्वस्थ कपास

#### Immediate Cultural Step
- **English:** No chemical treatment needed. Maintain optimal soil moisture and scout for early sucking pest attack.
- **Telugu:** రసాయనాలు అవసరం లేదు. నేలలో తగినంత తేమ ఉంచి రసం పీల్చే పురుగుల రాకను గమనించండి.
- **Hindi:** किसी दवा की जरूरत नहीं है। नमी बनाए रखें और कीटों की निगरानी करते रहें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-CICR & PJTSAU GAP. Irrigate at critical physiological stages (square formation, peak flowering, and boll development); avoid prolonged water stagnation. Apply balanced RDF; split nitrogen doses; spray 1% potassium nitrate (13:0:45) or 1% MgSO4 at flowering to prevent leaf reddening. Maintain clean field borders and conduct weekly agro-ecosystem analysis (AESA).
- **Telugu:** మూలం: ICAR-CICR & PJTSAU GAP. కీలక దశల్లో (పూత పిందె, పూత దశ, మరియు కాయ ఎదుగుదల) తడులు ఇవ్వండి; ఎక్కువ రోజులు నీరు నిల్వ ఉండకుండా చూడండి. సమతుల్య NPK ఎరువులను సిఫార్సు ప్రకారం వేయండి; నత్రజనిని దఫాలుగా వేయండి; ఆకు ఎరుపు తెగులు రాకుండా పూత దశలో 1% పొటాషియం నైట్రేట్ (13-0-45) లేదా మెగ్నీషియం సల్ఫేట్ పిచికారీ చేయండి. గట్లపై కలుపు లేకుండా ఉంచి ప్రతి వారం చీడపీడల గమనింపు చేయండి.
- **Hindi:** स्रोत: ICAR-CICR और PJTSAU GAP. महत्वपूर्ण अवस्थाओं (कलियां बनते समय, फूल आने पर और गूलर विकास) पर सिंचाई करें; लंबे समय तक जलभराव न होने दें। संतुलित खाद दें; नाइट्रोजन को किस्तों में डालें; पत्तियों के लाल पड़ने से रोकने हेतु फूल आने पर 1% पोटेशियम नाइट्रेट या 1% मैग्नीशियम सल्फेट का छिड़काव करें। मेड़ों को साफ रखें और साप्ताहिक निरीक्षण करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Crop is healthy with vigorous square and boll formation. Regularly inspect leaf undersides for sucking pests (leafhoppers, whiteflies, thrips) and examine squares for bollworm entry holes or premature flaring.
- **Telugu:** పంట ఏపుగా పెరిగి ఆరోగ్యకరమైన పూత మరియు కాయలతో ఉంది. ఆకుల అడుగున రసం పీల్చే పురుగులు (పచ్చ దోమ, తెల్ల దోమ, తామర పురుగులు) ఏమైనా ఉన్నాయేమో మరియు మొగ్గలపై రంధ్రాలు లేదా మొగ్గలు విచ్చుకుని రాలిపోతున్నాయేమో క్రమం తప్పకుండా గమనించండి.
- **Hindi:** फसल मजबूत कलियों और गूलरों के साथ स्वस्थ है। पत्तियों की निचली सतह पर रस चूसक कीटों (माहू, सफेद मक्खी, थ्रिप्स) और कलियों पर सुंडी के छेद या समय पूर्व गिरने की नियमित जांच करते रहें।

#### (c) When to Call Extension Officer
- **English:** Routine guidance only. Contact local KVK or Kisan Call Centre (Toll-Free 1800-180-1551) for advice on sucking pest ETL thresholds, square retention sprays, or harvest defoliation.
- **Telugu:** సాధారణ సలహాల కొరకు మాత్రమే. రసం పీల్చే పురుగుల ఆర్థిక నష్ట పరిమితి (ETL), పూత రాలకుండా నివారించే పిచికారీలు లేదా పత్తి తీత యాజమాన్యం కొరకు స్థానిక KVK లేదా కిసాన్ కాల్ సెంటర్ (1800-180-1551) ను సంప్రదించండి.
- **Hindi:** केवल सामान्य मार्गदर्शन हेतु। रस चूसक कीटों की ईटीएल सीमा, फूल-कलियां झड़ने से रोकने और चुगाई प्रबंधन की सलाह के लिए स्थानीय KVK या किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Place bird perches (10-15 per acre) to invite insectivorous birds into the field.
  - *Telugu:* ఎకరాకు 10-15 పక్షి స్థావరాలు (టి-ఆకారపు కర్రలు) ఏర్పాటు చేయండి.
  - *Hindi:* खेत में प्रति एकड़ 10-15 टी-आकार की पक्षी बैठकें लगाएं।
  - *Citation:* NIPHM Cotton IPM ([Source Link](https://niphm.gov.in)) — *"Bird perches enhance natural predation of cotton pests."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 8: `chilli_bacterial_spot`
- **Crop:** Chilli | **Type:** Disease | **Scientific Cause:** *Xanthomonas campestris pv. vesicatoria* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Chilli Bacterial Spot
  - **Telugu:** మిరప బాక్టీరియా మచ్చ తెగులు
  - **Hindi:** मिर्च का जीवाणु धब्बा रोग

#### Immediate Cultural Step
- **English:** Do not work in the chilli field while plants are wet with morning dew or rain to prevent spreading bacteria.
- **Telugu:** ఉదయం మంచు లేదా వర్షపు నీటితో మొక్కలు తడిగా ఉన్నప్పుడు పొలంలో పనులు చేయవద్దు.
- **Hindi:** जब पौधे सुबह की ओस से भीगे हों, तब खेत में कोई काम न करें ताकि बीमारी न फैले।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIHR Bengaluru & PJTSAU. Strictly avoid overhead sprinkler irrigation that splashes bacterial cells between plants; irrigate via drip or ridge-and-furrow system with rapid drainage. Avoid excess nitrogen fertilizer; ensure adequate calcium and potassium for leaf and fruit cell wall strength. Treat seed with hot water (50°C for 25 min) or streptocycline; rotate with non-solanaceous crops for 2 years.
- **Telugu:** మూలం: ICAR-IIHR & PJTSAU. బాక్టీరియా ఒక మొక్క నుండి మరొక మొక్కకు చిందకుండా ఉండటానికి స్ప్రింక్లర్ లేదా పైనుంచి నీరు చిలకరించే పద్ధతులను ఖచ్చితంగా నివారించండి; డ్రిప్ లేదా బోదెల ద్వారా నీరు అందించి నీరు నిల్వకుండా చూడండి. అధిక నత్రజని వేయకండి; కణాల దృఢత్వానికి సమతుల్య పొటాష్ మరియు కాల్షియం అందించండి. విత్తనాలను వేడి నీటితో (50°C వద్ద 25 నిమిషాలు) లేదా స్ట్రెప్టోసైక్లిన్‌తో శుద్ధి చేయండి; టమాటా, వంగ కాకుండా ఇతర పైర్లతో 2 ఏళ్ల పంట మార్పిడి చేయండి.
- **Hindi:** स्रोत: ICAR-IIHR और PJTSAU. फव्वारा सिंचाई से पूरी तरह बचें क्योंकि इससे जीवाणु छिटककर फैलते हैं; ड्रिप या मेड़-नाली विधि से सिंचाई करें और जल निकासी सुगम रखें। अतिरिक्त नाइट्रोजन न डालें; कोशिका भित्ति की मजबूती हेतु कैल्शियम और पोटाश संतुलित दें। बीज को गर्म पानी (50°C पर 25 मिनट) या स्ट्रेप्टोसाइक्लिन से उपचारित करें; 2 साल तक सोलेनेसी कुल (टमाटर, बैंगन) की फसलें न लगाएं।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch lower leaf undersides for small, dark circular water-soaked blister-like spots. Watch for dark brown spots with chlorotic yellow halos and rough, raised, brown scab-like spots on green chilli fruits.
- **Telugu:** ఆకుల అడుగు భాగంలో చిన్న, నీటితో తడిసిన బొబ్బల వంటి గుండ్రటి మచ్చలను గమనించండి. మచ్చలు ముదురు గోధుమ రంగులోకి మారి చుట్టూ పసుపు రంగు వలయం ఏర్పడటం మరియు పచ్చి మిరపకాయలపై గరుకైన గోధుమ రంగు పొక్కుల వంటి మచ్చలు వస్తున్నాయేమో పరిశీలించండి.
- **Hindi:** पत्तियों की निचली सतह पर छोटे, जल-युक्त फफोले जैसे गोल धब्बे देखें। जांचें कि धब्बे गहरे भूरे होकर पीले घेरे में बदल रहे हैं या नहीं और हरी मिर्च के फलों पर खुरदुरे उभरे हुए भूरे चकत्ते तो नहीं बन रहे।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if raised bacterial scab lesions appear on developing green chilli fruits or premature leaf drop exceeds 25% of canopy. Contact Kisan Call Centre (Toll-Free 1800-180-1551) or local KVK.
- **Telugu:** ఎదుగుతున్న పచ్చి మిరపకాయలపై గరుకైన బాక్టీరియా పొక్కులు కనిపించినా లేదా 25% కంటే ఎక్కువ ఆకులు రాలిపోతున్నా వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా KVK ని సంప్రదించండి.
- **Hindi:** यदि बढ़ती हुई हरी मिर्च पर उभरे हुए खुरदुरे चकत्ते दिखने लगें या 25% से अधिक पत्तियां झड़ने लगें, तो तुरंत कृषि विस्तार अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Spray Pseudomonas fluorescens (10 g/L) at nursery and transplanting stage.
  - *Telugu:* నారుమడి మరియు నాటిన దశలో సూడోమోనాస్ ఫ్లోరోసెన్స్ (10 గ్రా/లీ) పిచికారీ చేయండి.
  - *Hindi:* नर्सरी और रोपाई के समय स्यूडोमोनास (10 ग्राम/लीटर) का छिड़काव करें।
  - *Citation:* IIHR Bengaluru ([Source Link](https://iihr.res.in)) — *"Pseudomonas induces systemic resistance against Xanthomonas in chilli."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Copper Oxychloride 50% WP + Streptocycline` (Example: `Blitox 50 WP + Streptocycline`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear mask and goggles. Wash hands thoroughly with soap after spray.
  - *Safety Line (TE):* మాస్క్, కళ్ళద్దాలు ధరించండి. పిచికారీ తర్వాత చేతులను సబ్బుతో శుభ్రంగా కడుక్కోండి.
  - *Safety Line (HI):* मास्क और चश्मा पहनें। दवा के बाद साबुन से अच्छी तरह हाथ धोएं।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered bactericide combination for chilli bacterial spot."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 9: `chilli_leaf_curl_virus`
- **Crop:** Chilli | **Type:** Disease | **Scientific Cause:** *Chilli leaf curl virus (ChiLCV)* | **Referral Threshold:** `MODERATE`
- **Names:**
  - **English:** Chilli Leaf Curl Virus
  - **Telugu:** మిరప బొబ్బర / ఆకుముడుత తెగులు
  - **Hindi:** मिर्च का लीफ कर्ल वायरस (मुरड़िया)

#### Immediate Cultural Step
- **English:** There is NO chemical cure for the virus. Rogue out infected plants and spray to manage vector thrips and whiteflies.
- **Telugu:** ఈ వైరస్ కు ఎటువంటి మందు లేదు. తెగులు సోకిన మొక్కలను పీకివేయండి మరియు రసం పీల్చే పురుగులను అరికట్టండి.
- **Hindi:** वायरस का कोई सीधा इलाज नहीं है। ग्रसित पौधों को हटाएं और थ्रिप्स/सफेद मक्खी को नियंत्रित करें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIHR Bengaluru & PJTSAU. Provide regular light irrigations; avoid severe moisture stress which promotes whitefly vector infestation. Avoid excessive nitrogen fertilizer that causes tender succulent shoot growth favored by whiteflies; apply zinc and boron micronutrients. Plant 2-3 barrier border rows of maize or sorghum; install yellow sticky traps (15-20 per acre) to trap whiteflies; use silver reflective mulch.
- **Telugu:** మూలం: ICAR-IIHR & PJTSAU. క్రమం తప్పకుండా తేలికపాటి తడులు ఇవ్వండి; బెట్ట ఒత్తిడికి గురికాకుండా చూడండి, ఎందుకంటే బెట్ట పరిస్థితుల్లో తెల్ల దోమల ఉధృతి పెరుగుతుంది. తెల్ల దోమలను ఆకర్షించే అధిక నత్రజని వాడకాన్ని తగ్గించండి; జింక్ మరియు బోరాన్ వంటి సూక్ష్మ పోషకాలను అందించండి. పొలం చుట్టూ రక్షణ పంటగా 2-3 వరుసల మొక్కజొన్న లేదా జొన్న వేయండి; తెల్ల దోమల నివారణకు ఎకరాకు 15-20 పసుపు రంగు జిగురు అట్టలను అమర్చండి; వెండి రంగు ప్లాస్టిక్ మల్చింగ్ వాడండి.
- **Hindi:** स्रोत: ICAR-IIHR और PJTSAU. नियमित हल्की सिंचाई करें; सूखे का तनाव न आने दें क्योंकि इससे सफेद मक्खी का प्रकोप बढ़ता है। अधिक नाइट्रोजन न दें जिससे कोमल शाखाएं न बढ़ें जो मक्खियों को आकर्षित करती हैं; जिंक और बोरॉन सूक्ष्म पोषक तत्व दें। खेत के चारों ओर मक्का या ज्वार की 2-3 कतारों की बाड़ लगाएं; सफेद मक्खी की रोकथाम हेतु प्रति एकड़ 15-20 पीले चिपचिपे ट्रैप लगाएं; चमकीली मल्चिंग का प्रयोग करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for upward curling and puckering of young apical leaf margins, vein thickening, shortening of petioles, and stunted bushy growth of terminal shoots.
- **Telugu:** పై లేత ఆకుల అంచులు పైకి దోనెలా ముడుచుకుపోవడం, ఆకుల ఈనెలు లావుగా మారడం, ఆకుల తొడిమలు చిన్నవిగా మారడం మరియు మొక్క పైభాగాలు గిడసబారి గుబురుగా మారడాన్ని గమనించండి.
- **Hindi:** ऊपरी नई पत्तियों के किनारों का ऊपर की ओर मुड़ना (नाव के आकार में), नसों का मोटा होना, डंठल का छोटा होना और पौधों की ऊपरी शाखाओं का बौना व झाड़ीनुमा होना देखें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer as soon as 5-10% of plants show leaf curling before flowering; viral diseases have no chemical cure and vector management and rogueing must be coordinated under officer guidance. Call Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** పూత దశకు ముందే 5-10% మొక్కలలో ఆకు ముడత లక్షణాలు కనిపించిన వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి; వైరస్ తెగుళ్లకు రసాయన మందుల ద్వారా చికిత్స సాధ్యం కాదు, తెగులు సోకిన మొక్కలను పీకివేయడం మరియు తెల్లదోమ నివారణ చర్యలను అధికారి మార్గదర్శకత్వంలో చేపట్టాలి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** फूल आने से पहले यदि 5-10% पौधों में पत्ती मरोड़ दिखे तो तुरंत कृषि विस्तार अधिकारी से संपर्क करें; वायरल रोगों का कोई रासायनिक इलाज नहीं है, इसलिए रोगी पौधों को उखाड़ना और सफेद मक्खी नियंत्रण अधिकारी की सलाह से ही करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Erect barrier crops of maize or jowar around the chilli plot (4 rows) to block incoming whiteflies.
  - *Telugu:* మిరప తోట చుట్టూ 4 వరుసల్లో మొక్కజొన్న లేదా జొన్న పంటను రక్షణ కవచంగా వేయండి.
  - *Hindi:* खेत के चारों ओर 4 कतारों में मक्का या ज्वार लगाएं ताकि उड़ने वाले कीट रुक सकें।
  - *Citation:* IIHR IPM for Chilli ([Source Link](https://iihr.res.in)) — *"Border crops significantly reduce viruliferous vector ingress."*
- **Step 2:** Spray Neem oil 10,000 ppm (1 ml/L) or pongamia oil (5 ml/L).
  - *Telugu:* వేప నూనె 10,000 పి.పి.ఎం (1 మి.లీ/లీ) లేదా కానుగ నూనె పిచికారీ చేయండి.
  - *Hindi:* नीम का तेल (1 मिली/लीटर) या करंज का तेल (5 मिली/लीटर) छिड़कें।
  - *Citation:* NIPHM Advisory ([Source Link](https://niphm.gov.in)) — *"Neem and pongamia oils suppress sucking pest populations."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Diafenthiuron 50% WP or Fipronil 5% SC (VECTOR CONTROL ONLY)` (Example: `Pegasus / Regent`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Controls vectors (thrips/whiteflies) only; cannot cure viral damage. Wear protective gear.
  - *Safety Line (TE):* ఇది పురుగులను మాత్రమే అరికడుతుంది, వైరస్ ను నయం చేయదు. రక్షణ తొడుగులు ధరించండి.
  - *Safety Line (HI):* यह केवल कीटों को मारता है, वायरस को ठीक नहीं कर सकता। सुरक्षात्मक किट पहनें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for sucking pest/vector control in chilli."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 10: `chilli_healthy`
- **Crop:** Chilli | **Type:** Healthy | **Scientific Cause:** *None* | **Referral Threshold:** `NONE`
- **Names:**
  - **English:** Healthy Chilli
  - **Telugu:** ఆరోగ్యకరమైన మిరప
  - **Hindi:** स्वस्थ मिर्च

#### Immediate Cultural Step
- **English:** No chemical application required. Maintain balanced irrigation and weed-free bunds.
- **Telugu:** ఎటువంటి రసాయనాలు అవసరం లేదు. సక్రమంగా నీటి తడులు ఇచ్చి గట్లపై కలుపు లేకుండా చూసుకోండి.
- **Hindi:** किसी दवा की जरूरत नहीं। उचित सिंचाई करें और मेड़ों पर खरपतवार न उगने दें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIHR & ANGRAU GAP. Maintain uniform soil moisture through drip or furrow irrigation; avoid water stress during flowering and fruit setting. Apply recommended NPK with secondary nutrients (Ca, Mg, S); apply neem cake in soil at transplanting; spray balanced micronutrient mixture at 45 and 60 days. Scout weekly for thrips and mites using yellow and blue sticky traps.
- **Telugu:** మూలం: ICAR-IIHR & ANGRAU GAP. డ్రిప్ లేదా సాళ్ల ద్వారా సమానమైన తేమను నిర్వహించండి; పూత మరియు కాయ దశలలో నీటి ఎద్దడి లేకుండా చూడండి. సిఫార్సు చేసిన NPK తో పాటు కాల్షియం, మెగ్నీషియం, గంధకం అందించండి; నాటే సమయంలో వేపపిండిని నేలలో కలపండి; నాటిన 45 మరియు 60 రోజులకు సూక్ష్మపోషకాల మిశ్రమాన్ని పిచికారీ చేయండి. తామర పురుగులు మరియు నల్లుల కోసం పసుపు, నీలి రంగు జిగురు అట్టలను అమర్చి ప్రతి వారం గమనించండి.
- **Hindi:** स्रोत: ICAR-IIHR और ANGRAU GAP. ड्रिप या नाली द्वारा एकसमान नमी बनाए रखें; फूल और फल बनते समय पानी का तनाव न होने दें। अनुशंसित NPK के साथ कैल्शियम, मैग्नीशियम और सल्फर दें; रोपाई के समय नीम की खली डालें; 45 व 60 दिन पर सूक्ष्म पोषक तत्वों का छिड़काव करें। थ्रिप्स और माइट्स की निगरानी हेतु पीले व नीले चिपचिपे ट्रैप लगाएं।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Foliage is vibrant green and flat. Regularly check for downward leaf curling (mites), upward leaf cupping (thrips/whitefly), or collar damp-off during prolonged wet soil spells.
- **Telugu:** పైరు ముదురు ఆకుపచ్చగా ఆరోగ్యకరమైన ఆకులతో ఉంది. ఆకులు క్రిందికి ముడుచుకోవడం (నల్లి పురుగులు), ఆకులు పైకి ముడుచుకోవడం (తామర పురుగులు/తెల్ల దోమ) లేదా నేల ఎక్కువ రోజులు తడిగా ఉన్నప్పుడు మొదలు కుళ్ళు లక్షణాలు ఏమైనా కనిపిస్తున్నాయేమో క్రమం తప్పకుండా పరిశీలించండి.
- **Hindi:** फसल गहरी हरी और स्वस्थ पत्तियों वाली है। पत्तियों का नीचे मुड़ना (माइट्स), ऊपर मुड़ना (थ्रिप्स/सफेद मक्खी) या अधिक गीली मिट्टी में तने के पास गलन की नियमित जांच करते रहें।

#### (c) When to Call Extension Officer
- **English:** Routine guidance only. Contact local KVK or Kisan Call Centre (Toll-Free 1800-180-1551) for advice on integrated sucking pest management schedules, fruit drop prevention, and harvest grading.
- **Telugu:** సాధారణ సలహాల కొరకు మాత్రమే. రసం పీల్చే పురుగుల సమగ్ర నివారణ, పూత మరియు పిందె రాలకుండా నివారించే చిట్కాలు మరియు నాణ్యమైన కాయల కోత కొరకు స్థానిక KVK లేదా కిసాన్ కాల్ సెంటర్ (1800-180-1551) ను సంప్రదించండి.
- **Hindi:** केवल सामान्य मार्गदर्शन हेतु। रस चूसक कीटों के एकीकृत प्रबंधन, फल-फूल झड़ने से रोकने और मिर्च तुड़ाई के तरीकों के लिए स्थानीय KVK या किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Install blue and yellow sticky traps (10 each per acre) for early pest detection.
  - *Telugu:* ముందస్తు పురుగుల గుర్తింపు కోసం ఎకరాకు 10 నీలం, 10 పసుపు జిగురు అట్టలను అమర్చండి.
  - *Hindi:* कीटों की निगरानी के लिए प्रति एकड़ 10 नीले और 10 पीले चिपचिपे कार्ड लगाएं।
  - *Citation:* IIHR Chilli IPM ([Source Link](https://iihr.res.in)) — *"Sticky traps provide early warning of thrips and whiteflies."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 11: `tomato_early_blight`
- **Crop:** Tomato | **Type:** Disease | **Scientific Cause:** *Alternaria solani* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Tomato Early Blight
  - **Telugu:** టమాటా అగేతి ఆకుమచ్చ తెగులు
  - **Hindi:** टमाटर का अगेती झुलसा

#### Immediate Cultural Step
- **English:** Prune and dispose of lower infected leaves touching the soil and avoid overhead sprinkler watering.
- **Telugu:** నేలకు తగిలే కింద ఆకులను తెంపి నాశనం చేయండి మరియు పైనుండి నీరు చిలకరించవద్దు.
- **Hindi:** जमीन से छू रही निचली ग्रसित पत्तियों को काटकर नष्ट करें और ऊपर से पानी न डालें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIHR Bengaluru & TNAU. Use drip irrigation; strictly avoid overhead sprinkler watering that wets leaves; irrigate during morning hours. Apply balanced NPK fertilizer; avoid nitrogen deficiency which triggers early plant senescence and blight vulnerability; apply potash. Mulch beds with paddy straw to prevent rain-splash from soil; stake plants to keep foliage off ground; prune and destroy lower infected leaves.
- **Telugu:** మూలం: ICAR-IIHR & TNAU. డ్రిప్ పద్ధతి ద్వారా నీరు అందించండి; ఆకులు తడిసేలా పైనుంచి స్ప్రింక్లర్ల ద్వారా నీరు పెట్టవద్దు; ఉదయం వేళల్లో మాత్రమే నీరు పెట్టండి. సమతుల్య NPK ఎరువులను వాడండి; నత్రజని లోపం రాకుండా చూడండి, ఎందుకంటే పోషక లోపం వల్ల ఆకులు త్వరగా పండి తెగులు తీవ్రమవుతుంది; తగినంత పొటాష్ వేయండి. నేల నుండి ఆకులపైకి మట్టి చిందకుండా వరి గడ్డితో మల్చింగ్ చేయండి; కర్రలతో మొక్కలకు ఆసరా కల్పించండి; క్రింది భాగాన తెగులు సోకిన ఆకులను కత్తిరించి నాశనం చేయండి.
- **Hindi:** स्रोत: ICAR-IIHR और TNAU. ड्रिप सिंचाई अपनाएं; पत्तियों को गीला करने वाली फव्वारा सिंचाई से बचें; सुबह के समय सिंचाई करें। संतुलित NPK उर्वरक दें; नाइट्रोजन की कमी न होने दें क्योंकि कमजोर पौधों में यह रोग तेजी से फैलता है; पोटाश अवश्य दें। मिट्टी के छींटे पत्तियों पर पड़ने से रोकने हेतु धान के पुआल से मल्चिंग करें; पौधों को डंडों से सहारा दें; नीचे की रोगग्रस्त पत्तियों को काटकर नष्ट करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch lowest older leaves for small brown-black spots rapidly developing distinct concentric target-board rings surrounded by a chlorotic yellow zone; watch for lower leaves yellowing and drying up.
- **Telugu:** మొక్క క్రింది భాగంలోని పాత ఆకులపై చిన్న గోధుమ-నలుపు రంగు మచ్చలు ఏర్పడి, వాటి చుట్టూ పసుపు వలయంతో కూడిన లక్ష్య బోర్డు (కాన్సెంట్రిక్ రింగులు) వంటి వలయాలు కనిపిస్తున్నాయేమో మరియు క్రింది ఆకులు పసుపు రంగులోకి మారి ఎండిపోతున్నాయేమో గమనించండి.
- **Hindi:** निचली पुरानी पत्तियों पर छोटे भूरे-काले धब्बे देखें जो तेजी से संकेंद्रित छल्लों (टारगेट-बोर्ड जैसे गोल छल्ले) में बदलते हैं और उनके चारों ओर पीलापन होता है; निचली पत्तियों का पीला पड़कर सूखना देखें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if target-board spots spread upward past the mid-canopy into upper leaves or dark sunken concentric rot lesions appear on tomato fruit calyx/stem ends. Contact Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** వలయాల మచ్చలు మొక్క మధ్య భాగం దాటి పై ఆకులకు విస్తరిస్తున్నా లేదా కాయల తొడిమల వద్ద నల్లటి సొట్టబడిన వలయాల కుళ్ళు మచ్చలు కనిపించినా వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** यदि संकेंद्रित छल्ले वाले धब्बे पौधे के बीच से ऊपर की पत्तियों तक फैलने लगें या फलों के डंठल के पास धंसे हुए काले सड़े धब्बे दिखें, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Mulch soil around plants with straw to prevent rain splashes carrying spores from soil to foliage.
  - *Telugu:* వర్షపు చినుకుల ద్వారా నేలలోని శిలీంధ్రం ఆకులకు చేరకుండా మొక్కల మొదళ్ల వద్ద గడ్డితో మల్చింగ్ చేయండి.
  - *Hindi:* पौधों के चारों ओर पुआल की मल्चिंग करें ताकि मिट्टी के फफूंद छींटों से पत्तियों तक न पहुंचें।
  - *Citation:* IIHR Package for Solanaceous Crops ([Source Link](https://iihr.res.in)) — *"Mulching creates a physical barrier against soil-splashed Alternaria conidia."*
- **Step 2:** Foliar spray with Trichoderma viride (10 g/L) or NSKE 5%.
  - *Telugu:* ట్రైకోడెర్మా విరిడి (10 గ్రా/లీ) లేదా 5% వేప గింజల కషాయం పిచికారీ చేయండి.
  - *Hindi:* ट्राइकोडर्मा विरिडी (10 ग्राम/लीटर) या 5% नीम काढ़ा पत्तियों पर छिड़कें।
  - *Citation:* NIPHM Advisory ([Source Link](https://niphm.gov.in)) — *"Bio-fungicides suppress foliar blights in vegetables."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Mancozeb 75% WP or Chlorothalonil 75% WP` (Example: `Dithane M-45 / Kavach`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear gloves and face mask. Wash hands thoroughly before eating.
  - *Safety Line (TE):* గ్లౌజులు, మాస్క్ ధరించండి. భోజనానికి ముందు చేతులను శుభ్రంగా కడుక్కోండి.
  - *Safety Line (HI):* दस्ताने और मास्क पहनें। भोजन से पहले हाथ साबुन से अच्छी तरह धोएं।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for tomato early blight control."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 12: `tomato_late_blight`
- **Crop:** Tomato | **Type:** Disease | **Scientific Cause:** *Phytophthora infestans* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Tomato Late Blight
  - **Telugu:** టమాటా పచేతి ఆకుమచ్చ తెగులు
  - **Hindi:** टमाटर का पछेती झुलसा

#### Immediate Cultural Step
- **English:** This is an emergency disease that can wipe out a crop in 72 hours under cool wet weather. Spray immediately.
- **Telugu:** చల్లని, తేమ వాతావరణంలో ఈ తెగులు 72 గంటల్లో పంటను నాశనం చేస్తుంది. తక్షణమే మందు పిచికారీ చేయండి.
- **Hindi:** यह बीमारी ठंडे और नम मौसम में 3 दिन में पूरी फसल बर्बाद कर सकती है। तुरंत छिड़काव करें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-CPRI & ICAR-IIHR. Strictly avoid evening or overhead irrigation; irrigate through drip or furrow in morning hours; withhold irrigation during cloudy, foggy, or high-humidity cold spells. Avoid excessive nitrogen fertilizer that creates dense succulent foliage; maintain adequate potassium. Plant on raised ridges with wide row spacing for air circulation; destroy volunteer potato/tomato plants and nightshade weeds.
- **Telugu:** మూలం: ICAR-CPRI & ICAR-IIHR. సాయంత్రం వేళల్లో లేదా పైనుంచి స్ప్రింక్లర్లతో నీరు పెట్టవద్దు; ఉదయం వేళల్లో డ్రిప్ లేదా బోదెల ద్వారా మాత్రమే నీరు అందించండి; చలికాలంలో మబ్బులు, పొగమంచు లేదా అధిక తేమ ఉన్నప్పుడు నీటి తడులను నిలిపివేయండి. దట్టమైన ఆకులు పెరిగేలా అధిక నత్రజని వేయకండి; పొటాష్‌ను సమతుల్యంగా అందించండి. గాలి బాగా ఆడేలా ఎత్తైన బోదెలపై ఎక్కువ ఎడం ఉండేలా నాటండి; పొలంలో మొలిచే గచ్చకాయ, ఇతర సోలనేసి జాతి కలుపు మొక్కలను నాశనం చేయండి.
- **Hindi:** स्रोत: ICAR-CPRI और ICAR-IIHR. शाम को या ऊपर से फव्वारा सिंचाई बिल्कुल न करें; सुबह के समय ड्रिप या नाली से सिंचाई करें; बादल, कोहरा या अधिक नमी वाले ठंडे मौसम में सिंचाई रोक दें। अधिक नाइट्रोजन न दें जिससे घनी पत्तियां न बनें; पोटाश संतुलित रखें। हवा के आवागमन हेतु ऊंची मेड़ों पर उचित दूरी पर रोपाई करें; खेत के आसपास सोलेनेसी खरपतवार और पुराने पौधों को नष्ट करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for rapidly enlarging irregular water-soaked pale-green to dark brown lesions on leaves and petioles during cool (15-20°C) damp weather; inspect leaf undersides for fine white cottony fungal mold growth in early foggy mornings.
- **Telugu:** చల్లని (15-20°C), తేమతో కూడిన వాతావరణంలో ఆకులు మరియు కాండంపై వేగంగా విస్తరించే నీటితో తడిసిన లేత ఆకుపచ్చ నుండి ముదురు గోధుమ రంగు మచ్చలను గమనించండి; తెల్లవారుజామున పొగమంచు వేళల్లో ఆకుల అడుగున తెల్లటి దూది వంటి బూజు పెరుగుదలను మరియు కాయలపై ముదురు గోధుమ రంగు నూనె వంటి మచ్చలను పరిశీలించండి.
- **Hindi:** ठंडे (15-20°C) और नम मौसम में पत्तियों और डंठलों पर तेजी से फैलते हुए जल-युक्त हल्के हरे से गहरे भूरे धब्बे देखें; सुबह के कोहरे में पत्तियों के नीचे सफेद रुई जैसी फफूंद और फलों पर भूरे तैलीय धब्बे जांचें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer IMMEDIATELY upon the very first symptom of water-soaked lesions with white downy growth; late blight spreads exponentially and can destroy entire fields within 3-5 days. Contact Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** తెల్లటి బూజుతో కూడిన నీటి మచ్చలు కనిపించిన వెంటనే అత్యవసరంగా వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి; లేట్ బ్లైట్ తెగులు అతి వేగంగా వ్యాపించి కేవలం 3-5 రోజుల్లోనే మొత్తం పంటను సర్వనాశనం చేస్తుంది. వెంటనే కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** पत्तियों पर सफेद फफूंद वाले जल-युक्त धब्बे दिखते ही बिना किसी देरी के तुरंत कृषि अधिकारी से संपर्क करें; पछेती झुलसा अत्यंत तेजी से फैलता है और 3-5 दिनों में पूरा खेत नष्ट कर सकता है। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Spray copper-based bio-mix or Bordeaux mixture 1% immediately upon disease forecast.
  - *Telugu:* తెగులు సూచనలు కనిపించిన వెంటనే 1% బోర్డో మిశ్రమాన్ని పిచికారీ చేయండి.
  - *Hindi:* बीमारी की शुरुआत में 1% बोर्डो मिश्रण का तुरंत छिड़काव करें।
  - *Citation:* IIHR Bengaluru ([Source Link](https://iihr.res.in)) — *"Bordeaux mixture provides robust preventative surface protection."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Metalaxyl 8% + Mancozeb 64% WP or Cymoxanil 8% + Mancozeb 64% WP` (Example: `Ridomil Gold / Curzate`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear respirator mask and goggles. Highly critical to spray before disease engulfs field.
  - *Safety Line (TE):* శ్వాసక్రియ మాస్క్, కళ్ళద్దాలు ధరించండి. తెగులు ముదరక ముందే పిచికారీ పూర్తి చేయండి.
  - *Safety Line (HI):* मास्क और चश्मा पहनें। खेत बर्बाद होने से पहले समय पर छिड़काव करें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved systemic fungicide for late blight in tomato."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 13: `tomato_yellow_leaf_curl_virus`
- **Crop:** Tomato | **Type:** Disease | **Scientific Cause:** *Tomato yellow leaf curl virus (TYLCV)* | **Referral Threshold:** `MODERATE`
- **Names:**
  - **English:** Tomato Yellow Leaf Curl Virus
  - **Telugu:** టమాటా ఆకుముడుత వైరస్ తెగులు
  - **Hindi:** टमाटर का पीला पत्ती मरोड़ विषाणु

#### Immediate Cultural Step
- **English:** There is NO chemical cure for the virus. Rogue out infected plants and spray to manage the whitefly vector.
- **Telugu:** ఈ వైరస్ కు రసాయన మందు లేదు. తెగులు సోకిన మొక్కలను పీకివేయండి మరియు తెల్లదోమను అరికట్టండి.
- **Hindi:** इस वायरस की कोई दवा नहीं है। ग्रसित पौधों को उखाड़कर नष्ट करें और सफेद मक्खी का नियंत्रण करें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIHR Bengaluru & PJTSAU. Provide regular light drip irrigations; avoid soil drought stress which accelerates whitefly feeding on plants. Avoid heavy chemical nitrogen top-dressing; apply balanced phosphorus and potassium. Raise nursery seedlings under 40-50 mesh insect-proof nylon nets; install yellow sticky traps (15-20 per acre) to monitor and capture whitefly vectors; grow 3 border barrier rows of maize or sorghum; rogue and bury infected plants.
- **Telugu:** మూలం: ICAR-IIHR & PJTSAU. డ్రిప్ ద్వారా క్రమం తప్పకుండా తేలికపాటి తడులు ఇవ్వండి; నేల ఎండిపోయేలా చేసి బెట్ట ఒత్తిడికి గురిచేయవద్దు, ఎందుకంటే ఎండ తీవ్రతకు తెల్ల దోమల దాడి పెరుగుతుంది. అధిక నత్రజని వేయకండి; భాస్వరం మరియు పొటాష్‌ను సమతుల్యంగా వేయండి. నారుమడిని 40-50 మెష్ కీటక నిరోధక నైలాన్ నెట్ కింద పెంచండి; తెల్ల దోమల నివారణకు ఎకరాకు 15-20 పసుపు రంగు జిగురు అట్టలను అమర్చండి; పొలం చుట్టూ 3 వరుసల మొక్కజొన్న లేదా జొన్న పంటను రక్షణగా వేయండి; తెగులు సోకిన మొక్కలను పీకి పూడ్చిపెట్టండి.
- **Hindi:** स्रोत: ICAR-IIHR और PJTSAU. ड्रिप द्वारा नियमित हल्की सिंचाई करें; खेत को सूखने न दें जिससे सफेद मक्खी का प्रकोप न बढ़े। अधिक यूरिया न डालें; फास्फोरस और पोटाश संतुलित दें। पौधशाला (नर्सरी) को 40-50 मेश की जाली के नीचे उगाएं; सफेद मक्खी की निगरानी व रोकथाम हेतु प्रति एकड़ 15-20 पीले चिपचिपे ट्रैप लगाएं; खेत के चारों तरफ मक्का या ज्वार की 3 कतारें लगाएं; रोगग्रस्त पौधों को उखाड़कर गहरा गाड़ दें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for upward cupping and curling of young leaflet margins, prominent interveinal chlorosis (yellowing), severe reduction in leaflet size, and stunted erect bushy plant growth.
- **Telugu:** పై లేత ఆకుల అంచులు పైకి దోనెలా ముడుచుకుపోవడం, ఈనెల మధ్య భాగం పసుపు రంగులోకి మారడం (క్లోరోసిస్), ఆకులు చాలా చిన్నవిగా మారిపోవడం మరియు మొక్క ఎదుగుదల ఆగిపోయి గుబురుగా మారడాన్ని గమనించండి.
- **Hindi:** ऊपरी नई पत्तियों का ऊपर की ओर मुड़ना, नसों के बीच का हिस्सा पीला पड़ना, पत्तियों का आकार बहुत छोटा होना और पौधे का बौना व झाड़ी जैसा दिखना देखें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer as soon as 5% of young plants show yellow leaf curling before flowering; viral infections are incurable and vector eradication and rogueing require officer guidance. Contact Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** పూత దశకు ముందే 5% మొక్కలలో పసుపు ఆకు ముడత లేదా గిడసబారడం కనిపించిన వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి; వైరస్ తెగుళ్లకు మందుల ద్వారా నివారణ ఉండదు కాబట్టి తెల్లదోమల నిర్మూలన మరియు రోగగ్రస్త మొక్కల తొలగింపు అధికారి సూచనల మేరకే చేయాలి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** फूल आने से पहले यदि 5% पौधों में पीला पत्ती मरोड़ या बौनापन दिखे तो तुरंत कृषि विस्तार अधिकारी से संपर्क करें; वायरल रोगों का कोई इलाज नहीं है और सफेद मक्खी नियंत्रण तथा पौधों को नष्ट करने की सलाह जरूरी है। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Raise seedlings under 40-mesh nylon insect net nursery tunnels to prevent viruliferous whitefly entry.
  - *Telugu:* నారుమడి దశలో తెల్లదోమలు రాకుండా 40 మెష్ నైలాన్ దోమతెర సొరంగాలను ఉపయోగించండి.
  - *Hindi:* नर्सरी में सफेद मक्खी से बचाव के लिए 40-मेष की जाली (नेट) के नीचे पौधे तैयार करें।
  - *Citation:* IIHR Tomato IPM ([Source Link](https://iihr.res.in)) — *"Nylon net nurseries completely exclude whitefly vectors during critical seedling stage."*
- **Step 2:** Install yellow sticky traps (15-20 per acre) and spray neem oil (5 ml/L).
  - *Telugu:* ఎకరాకు 15-20 పసుపు జిగురు అట్టలను ఏర్పాటు చేసి వేప నూనె (5 మి.లీ/లీ) పిచికారీ చేయండి.
  - *Hindi:* प्रति एकड़ 15-20 पीले कार्ड लगाएं और 5 मिली/लीटर नीम तेल छिड़कें।
  - *Citation:* NIPHM Advisory ([Source Link](https://niphm.gov.in)) — *"Sticky traps and neem reduce whitefly numbers."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Cyantraniliprole 10.26% OD or Spiromesifen 22.9% SC (VECTOR CONTROL ONLY)` (Example: `Benevia / Oberon`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Controls whiteflies only; will not reverse existing viral stunting. Wear protective gear.
  - *Safety Line (TE):* తెల్లదోమను మాత్రమే చంపుతుంది, వైరస్ ను నయం చేయదు. రక్షణ దుస్తులు ధరించండి.
  - *Safety Line (HI):* यह केवल सफेद मक्खी को मारती है, वायरस को ठीक नहीं कर सकती। मास्क पहनें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for whitefly management in tomato."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 14: `tomato_spider_mites`
- **Crop:** Tomato | **Type:** Pest | **Scientific Cause:** *Tetranychus urticae (Two-Spotted Spider Mite)* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Tomato Spider Mites
  - **Telugu:** టమాటా ఎర్ర నల్లి
  - **Hindi:** टमाटर का मकड़ी कीट (माइट्स)

#### Immediate Cultural Step
- **English:** Check leaf undersides with a hand lens. If webbing and mites appear on >10% of plants, spray forceful water jets and release predatory mites or apply neem oil.
- **Telugu:** ఆకుల అడుగున భూతద్దంతో నల్లి పురుగులను గమనించండి. 10% మొక్కలపై నల్లి కనిపిస్తే, నీటిని బలంగా పిచికారీ చేసి వేప నూనె లేదా మిత్ర పురుగులను వాడండి.
- **Hindi:** पत्तियों के नीचे लेंस से माइट्स देखें। 10% पौधों पर जाला दिखने पर तेज पानी की बौछार करें और नीम तेल या मित्र कीट छोड़ें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-NBAIR Bengaluru & NIPHM Hyderabad. Integrated Pest Management (IPM) steps: 1. Monitoring: Scout weekly; inspect undersides of 20 random middle leaves with a 10x hand lens. 2. Threshold (ETL): 5-10 mites per leaflet or visible leaf stippling on >10% of plants. 3. Cultural Control: Avoid drought stress (hot dry dusty weather triggers mite flares; light irrigation or misting washes mites and boosts humidity); keep field borders free of weeds. 4. Biological Control first: Conserve predatory mites (Neoseiulus longispinosus); spray NSKE 5% or neem oil. 5. Chemical Control last: CIB&RC registered acaricides (Spiromesifen 22.9% SC or Propargite 57% EC) only when ETL is exceeded.
- **Telugu:** మూలం: ICAR-NBAIR & NIPHM. సమగ్ర సస్యరక్షణ (IPM) పద్ధతులు: 1. గమనింపు: ప్రతి వారం పొలంలో తిరుగుతూ 20 మొక్కల మధ్య ఆకుల అడుగు భాగాన్ని భూతద్దంతో పరిశీలించండి. 2. ఆర్థిక నష్ట పరిమితి (ETL): ఆకుకు 5-10 నల్లులు లేదా 10% పైగా మొక్కలపై పసుపు చుక్కలు కనిపించడం. 3. సేద్య పద్ధతులు: బెట్ట పరిస్థితులు రానివ్వకండి (వేడి, పొడి మరియు దుమ్ము వాతావరణంలో నల్లి ఉధృతి పెరుగుతుంది; తేలికపాటి తడులు ఇవ్వడం లేదా నీరు చిలకరించడం వల్ల తేమ పెరిగి నల్లి తగ్గుతుంది); గట్లపై కలుపు లేకుండా ఉంచండి. 4. జీవ నియంత్రణ మొదట: సహజ మిత్ర పురుగులైన ప్రిడేటరీ నల్లులను కాపాడండి; 5% వేప గింజల కషాయం (NSKE) లేదా వేప నూనె పిచికారీ చేయండి. 5. రసాయన మందులు చివరగా: ETL దాటినప్పుడు మాత్రమే CIB&RC అనుమతించిన మైటిసైడ్లు (స్పైరోమెసిఫెన్ లేదా ప్రొపర్గైట్) లేబుల్ ప్రకారం పిచికారీ చేయండి.
- **Hindi:** स्रोत: ICAR-NBAIR और NIPHM. एकीकृत कीट प्रबंधन (IPM) उपाय: 1. निगरानी: साप्ताहिक रूप से 20 पौधों की बीच की पत्तियों की निचली सतह को हैंड लेंस से जांचें। 2. आर्थिक सीमा (ETL): प्रति पत्ती 5-10 माइट्स या 10% से अधिक पौधों पर धब्बे दिखना। 3. कृषि उपाय: सूखे का तनाव न आने दें (गर्म, शुष्क और धूल भरे मौसम में माइट्स तेजी से बढ़ते हैं; हल्की सिंचाई या फव्वारे से नमी बढ़ाएं); मेड़ों को खरपतवार मुक्त रखें। 4. जैविक नियंत्रण पहले: शिकारी माइट्स (Neoseiulus) का संरक्षण करें; 5% नीम गिरी का काढ़ा (NSKE) या नीम तेल छिड़कें। 5. रासायनिक नियंत्रण अंत में: केवल ETL पार होने पर ही CIB&RC पंजीकृत कीटनाशक (स्पाइरोमेसिफेन या प्रोपरगाइट) लेबल के अनुसार छिड़कें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for fine yellow-white speckled stippling on upper leaf surfaces; fine white silky webbing underneath leaves and on young growing tips; leaves turning pale bronzed with sand-paper texture.
- **Telugu:** ఆకుల పైభాగంలో చిన్న చిన్న పసుపు-తెలుపు రంగు చుక్కలు (స్టిప్లింగ్) ఏర్పడటం గమనించండి; ఆకుల అడుగున మరియు లేత చిగుళ్ళపై సన్నటి తెల్లటి సాలీడు గూడు వంటి దారాలు ఉండటం; ఆకులు ఇసుక అట్ట మాదిరిగా గరుకుగా మారి రాగి రంగులోకి మారడాన్ని పరిశీలించండి.
- **Hindi:** पत्तियों की ऊपरी सतह पर महीन पीले-सफेद बिंदुनुमा धब्बे देखें; पत्तियों के नीचे और कोमल शीर्ष पर बारीक सफेद रेशमी जाले देखें; पत्तियों का तांबे जैसा भूरा और रेगमाल जैसा खुरदुरा होना जांचें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if mite count exceeds ETL (>5-10 mites per leaflet) or fine webbing covers upper growing shoots and flower clusters. Contact Kisan Call Centre (Toll-Free 1800-180-1551) or local KVK.
- **Telugu:** నల్లి పురుగుల సంఖ్య ఆర్థిక నష్ట పరిమితి (ఆకుకు 5-10 నల్లులు) దాటినా లేదా పై చిగుళ్ళు మరియు పూత గుత్తులను సాలీడు గూడు వంటి తెల్లటి జాలం కప్పివేసినా వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా KVK ని సంప్రదించండి.
- **Hindi:** यदि माइट्स की संख्या ईटीएल (5-10 माइट्स प्रति पत्ती) से अधिक हो जाए या शीर्ष शाखाओं व फूलों पर जाले फैल जाएं, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Field Scouting & Cultural Control: Inspect underside of leaves weekly; remove and burn heavily infested lower leaves to prevent spread.
  - *Telugu:* క్షేత్ర పరిశీలన & సాంస్కృతిక నివారణ: ప్రతి వారం ఆకుల అడుగు భాగాన్ని పరిశీలించండి; నల్లి తీవ్రంగా ఉన్న అడుగు ఆకులను తుంచి కాల్చివేయండి.
  - *Hindi:* खेत निगरानी और सांस्कृतिक नियंत्रण: हर हफ्ते पत्तियों के नीचे जांचें; अत्यधिक ग्रसित निचली पत्तियों को तोड़कर नष्ट करें।
  - *Citation:* ICAR-IIHR Tomato IPM ([Source Link](https://iihr.res.in)) — *"Regular scouting of lower leaf surfaces and sanitation reduces spider mite infestation."*
- **Step 2:** Mechanical Washing: Spray forceful clean water jets onto the underside of foliage in morning hours to physically disrupt webs and dislodge mites.
  - *Telugu:* నీటి పిచికారీ: ఉదయం వేళల్లో ఆకుల అడుగున నీటిని బలంగా పిచికారీ చేసి బూజు గూళ్లను, నల్లి పురుగులను తొలగించండి.
  - *Hindi:* तेज पानी का छिड़काव: सुबह के समय पत्तियों के नीचे तेज पानी की धार मारकर जालों और कीटों को गिराएं।
  - *Citation:* ANGRAU Crop Production Guide ([Source Link](https://angrau.ac.in)) — *"Water spray dislodges spider mite colonies and reduces web accumulation."*
- **Step 3:** Biological Control: Spray Azadirachtin / Neem oil 1500 ppm (5 ml/L) or release predatory mites (Neoseiulus longispinosus) at 10 mites/plant.
  - *Telugu:* జీవ నియంత్రణ: వేప నూనె 1500 ppm (5 మి.లీ/లీ) పిచికారీ చేయండి లేదా వేటాడే మిత్ర నల్లులు (నియోసియులస్) ఎకరాకు విడుదల చేయండి.
  - *Hindi:* जैविक नियंत्रण: नीम तेल 1500 ppm (5 मिली/लीटर) का छिड़काव करें या परभक्षी माइट्स (नियोसियूलस) छोड़ें।
  - *Citation:* NBAIR Bengaluru ([Source Link](https://nbair.res.in)) — *"Predatory phytoseiid mites keep Tetranychus spider mites below economic threshold."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Spiromesifen 22.9% SC or Wettable Sulphur 80% WP` (Example: `Oberon / Sulfex`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Do not spray sulphur when temperature exceeds 32°C to prevent phytotoxicity. Wear goggles and protective clothing.
  - *Safety Line (TE):* ఎండ తీవ్రత 32°C దాటినప్పుడు గంధకం చల్లవద్దు. కళ్ళద్దాలు, రక్షణ దుస్తులు ధరించండి.
  - *Safety Line (HI):* 32°C से अधिक तापमान होने पर गंधक न छिड़कें। चश्मा और सुरक्षात्मक कपड़े पहनें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved miticide for spider mites in tomato."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 15: `tomato_healthy`
- **Crop:** Tomato | **Type:** Healthy | **Scientific Cause:** *None* | **Referral Threshold:** `NONE`
- **Names:**
  - **English:** Healthy Tomato
  - **Telugu:** ఆరోగ్యకరమైన టమాటా
  - **Hindi:** स्वस्थ टमाटर

#### Immediate Cultural Step
- **English:** No chemical application needed. Stake plants to keep foliage off the ground and maintain drip irrigation.
- **Telugu:** ఎటువంటి రసాయనాలు అవసరం లేదు. ఆకులు నేలకు తగలకుండా కర్రలు కట్టి బిందు సేద్యం ద్వారా నీరందించండి.
- **Hindi:** किसी दवा की जरूरत नहीं। पौधों को डंडों से सहारा दें ताकि पत्तियां जमीन से ऊपर रहें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIHR Bengaluru & PJTSAU GAP. Maintain uniform moisture via drip irrigation to avoid blossom-end rot caused by irregular watering. Apply recommended NPK with calcium nitrate at flowering and fruit set; incorporate FYM/vermicompost at planting. Stake indeterminate plants with bamboo trellises; prune lower sucker leaves touching soil; scout weekly for fruit borer eggs.
- **Telugu:** మూలం: ICAR-IIHR & PJTSAU GAP. నీటి తడులలో హెచ్చుతగ్గుల వల్ల వచ్చే కాయ వెనుక భాగం కుళ్ళును (బ్లాసమ్ ఎండ్ రాట్) నివారించడానికి డ్రిప్ ద్వారా స్థిరమైన తేమను అందించండి. సిఫార్సు చేసిన NPK ఎరువులతో పాటు పూత మరియు కాయ దశలో కాల్షియం నైట్రేట్ వేయండి; నాటేటప్పుడు పశువుల ఎరువు లేదా వర్మీకంపోస్ట్ కలపండి. వెదురు కర్రలతో మొక్కలను పైకి కట్టండి; నేలను తాకే క్రింది కొమ్మలను కత్తిరించండి; కాయ తొలుచు పురుగు గుడ్ల కోసం ప్రతి వారం పరిశీలించండి.
- **Hindi:** स्रोत: ICAR-IIHR और PJTSAU GAP. अनियमित पानी से होने वाले फल सड़न (ब्लॉसम-एंड रॉट) से बचने हेतु ड्रिप द्वारा एकसमान नमी बनाए रखें। संतुलित NPK के साथ फल बनते समय कैल्शियम नाइट्रेट दें; रोपाई के समय गोबर की खाद या वर्मीकम्पोस्ट डालें। बांस के डंडों से पौधों को सहारा दें; जमीन छूने वाली निचली टहनियों को काटें; फल छेदक के अंडों की साप्ताहिक जांच करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Foliage is dark green with abundant blossoms and clean fruit set. Regularly monitor for lower leaf target spots, water-soaked stem patches, whitefly vector presence, or fruit pinholes.
- **Telugu:** పైరు ముదురు ఆకుపచ్చగా విస్తారమైన పూత మరియు ఆరోగ్యకరమైన కాయలతో ఉంది. క్రింది ఆకులపై వలయాల మచ్చలు, కాండంపై నీటి మచ్చలు, తెల్ల దోమల ఉనికి లేదా కాయలపై చిన్న రంధ్రాలు ఏమైనా కనిపిస్తున్నాయేమో క్రమం తప్పకుండా గమనించండి.
- **Hindi:** फसल भरपूर फूलों और स्वस्थ फलों के साथ गहरी हरी है। निचली पत्तियों पर गोल छल्लों वाले धब्बों, तने पर जल-युक्त चकत्तों, सफेद मक्खी की उपस्थिति या फलों पर छेद की नियमित जांच करते रहें।

#### (c) When to Call Extension Officer
- **English:** Routine guidance only. Contact local KVK or Kisan Call Centre (Toll-Free 1800-180-1551) for seasonal advice on fruit set maximization, micronutrient booster sprays, and harvest grading.
- **Telugu:** సాధారణ సలహాల కొరకు మాత్రమే. కాయల దిగుబడిని పెంచడం, సూక్ష్మపోషకాల పిచికారీలు మరియు నాణ్యమైన కాయల గ్రేడింగ్ కొరకు స్థానిక KVK లేదా కిసాన్ కాల్ సెంటర్ (1800-180-1551) ను సంప్రదించండి.
- **Hindi:** केवल सामान्य मार्गदर्शन हेतु। फलों की पैदावार बढ़ाने, सूक्ष्म पोषक तत्वों के छिड़काव और ग्रेडिंग के तरीकों के लिए स्थानीय KVK या किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Stake plants with bamboo poles to improve air movement and prevent soil contact.
  - *Telugu:* గాలి వెలుతురు బాగా సోకేందుకు మరియు నేల తగలకుండా వెదురు కర్రలతో మొక్కలను నిలబెట్టండి.
  - *Hindi:* हवा के संचार के लिए और जमीन से बचाने के लिए बांस के डंडों से पौधों को बांधें।
  - *Citation:* IIHR Tomato Guide ([Source Link](https://iihr.res.in)) — *"Staking prevents contact-borne diseases in tomato."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 16: `maize_gray_leaf_spot`
- **Crop:** Maize | **Type:** Disease | **Scientific Cause:** *Cercospora zeae-maydis* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Maize Gray Leaf Spot
  - **Telugu:** మొక్కజొన్న బూడిద రంగు ఆకుమచ్చ తెగులు
  - **Hindi:** मक्का का ग्रे लीफ स्पॉट

#### Immediate Cultural Step
- **English:** Do not practice continuous maize cultivation on the same field; ensure proper spacing for aeration.
- **Telugu:** ఒకే పొలంలో పదే పదే మొక్కజొన్న వేయవద్దు; గాలి ఆడేలా తగిన దూరంలో మొక్కలను ఉంచండి.
- **Hindi:** उसी खेत में लगातार मक्का न लगाएं और पौधों के बीच उचित दूरी रखें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIMR Ludhiana & PJTSAU. Avoid standing water; ensure good surface furrow drainage; avoid late afternoon sprinkler irrigation. Apply balanced NPK fertilizers; split nitrogen into 3 equal doses (basal, knee-high, and tasseling); avoid high nitrogen without adequate potassium. Practice deep plowing to bury crop residues; rotate with non-host crops (soybean, groundnut, pulses) for at least one season; maintain 60x20 cm plant spacing.
- **Telugu:** మూలం: ICAR-IIMR & PJTSAU. పొలంలో నీరు నిల్వ ఉండకుండా చూడండి; సాళ్ల ద్వారా సరైన డ్రైనేజీ ఏర్పాటు చేయండి; సాయంత్రం వేళల్లో స్ప్రింక్లర్లతో నీరు పెట్టవద్దు. సమతుల్య NPK ఎరువులను వాడండి; నత్రజనిని 3 సమ భాగాలుగా వేయండి (ఆఖరి దుక్కిలో, మోకాలి లోతు దశలో మరియు వెన్ను దశలో); పొటాష్ లేకుండా అధిక నత్రజని వాడవద్దు. పంట వ్యర్థాలను లోతుగా కలియదున్నండి; సోయాచిక్కుడు, వేరుశనగ లేదా పప్పుధాన్యాల పంటలతో కనీసం ఒక కాలం పంట మార్పిడి చేయండి; 60x20 సెం.మీ మొక్కల దూరం పాటించండి.
- **Hindi:** स्रोत: ICAR-IIMR और PJTSAU. खेत में पानी जमा न होने दें; नालियों द्वारा जल निकासी रखें; शाम के समय फव्वारा सिंचाई से बचें। संतुलित NPK उर्वरक दें; नाइट्रोजन को 3 बराबर किस्तों में बांटें (बुवाई, घुटने की ऊंचाई और मंजरी निकलते समय); पोटाश के बिना अधिक नाइट्रोजन न दें। पिछली फसल के अवशेषों को गहरा दबाएं; सोयाबीन, मूंगफली या दलहन के साथ फसल चक्र अपनाएं; 60x20 सेमी पौधे की दूरी रखें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for small, narrow rectangular tan-to-gray spots strictly delimited by parallel leaf veins; lesions expand into long rectangular blocks turning paper-gray with fungal spores during warm, humid overcast spells.
- **Telugu:** ఆకుల ఈనెల సమాంతర గీతల మధ్య ఏర్పడే చిన్న, సన్నని దీర్ఘచతురస్రాకారపు బూడిద రంగు మచ్చలను గమనించండి; తేమ మరియు మబ్బులతో కూడిన వాతావరణంలో ఈ మచ్చలు కాగితం వంటి బూడిద రంగు పట్టీలుగా విస్తరిస్తున్నాయో లేదో పరిశీలించండి.
- **Hindi:** पत्तियों की नसों के समानांतर सीमित छोटे, संकरे आयताकार भूरे-धूसर धब्बे देखें; गर्म और नम मौसम में ये धब्बे लंबे आयताकार रूप में फैलकर फफूंद के कारण कागज जैसे धूसर हो जाते हैं।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if rectangular gray lesions advance to the ear leaf or the two leaves above it before grain dough stage. Contact Kisan Call Centre (Toll-Free 1800-180-1551) or local KVK.
- **Telugu:** గింజ గట్టిపడే దశకు ముందే దీర్ఘచతురస్రాకార బూడిద రంగు మచ్చలు కంకి ఆకుకు లేదా దాని పైన ఉన్న రెండు ఆకులకు పాకినట్లయితే వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) లేదా KVK ని సంప్రదించండి.
- **Hindi:** यदि दाना भरने की अवस्था से पहले आयताकार धूसर धब्बे भुट्टे वाली पत्ती या उसके ऊपर की दो पत्तियों तक पहुंच जाएं, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Deep plow infected crop residue after harvest to bury fungal conidia.
  - *Telugu:* పంట కోత తర్వాత పొలాన్ని లోతుగా దున్ని తెగులు సోకిన అవశేషాలను నేలలో కలపండి.
  - *Hindi:* कटाई के बाद खेत की गहरी जुताई करें ताकि बीमारी के अवशेष मिट्टी में दब जाएं।
  - *Citation:* ICAR-IIMR Ludhiana ([Source Link](https://iimr.icar.gov.in)) — *"Burying maize stubble reduces Cercospora primary inoculum."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Azoxystrobin 18.2% + Difenoconazole 11.4% SC` (Example: `Amistar Top`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear protective clothing and mask. Do not spray during noon heat.
  - *Safety Line (TE):* రక్షణ దుస్తులు, మాస్క్ ధరించండి. మధ్యాహ్నం ఎండలో పిచికారీ చేయవద్దు.
  - *Safety Line (HI):* सुरक्षात्मक कपड़े और मास्क पहनें। दोपहर की तेज धूप में छिड़काव न करें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Registered for foliar diseases in maize."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 17: `maize_common_rust`
- **Crop:** Maize | **Type:** Disease | **Scientific Cause:** *Puccinia sorghi* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Maize Common Rust
  - **Telugu:** మొక్కజొన్న కుంకుమ తెగులు
  - **Hindi:** मक्का का रतुआ रोग (रस्ट)

#### Immediate Cultural Step
- **English:** Monitor cool, moist weather conditions and apply registered triazole fungicide at first appearance of pustules.
- **Telugu:** చల్లని, తేమ వాతావరణాన్ని గమనిస్తూ బొబ్బలు కనిపించగానే నిర్దేశిత శిలీంద్రనాశినిని పిచికారీ చేయండి.
- **Hindi:** ठंडे और नम मौसम में निगरानी रखें और पहले दाने दिखते ही फफूंदनाशक का छिड़काव करें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIMR Ludhiana & PJTSAU. Avoid severe moisture stress; schedule irrigation to prevent high overnight canopy humidity; avoid excessive flood irrigation. Apply recommended nitrogen in splits; avoid excessive urea without potassium; ensure adequate potash to increase cell wall resistance. Plant early in season to escape peak airborne spores; destroy Oxalis weed hosts along field borders; plant rust-resistant hybrids.
- **Telugu:** మూలం: ICAR-IIMR & PJTSAU. తీవ్రమైన తేమ లోపం లేకుండా చూడండి; రాత్రిపూట ఆకులపై ఎక్కువ తేమ ఉండకుండా తడుల సమయాన్ని సర్దుబాటు చేయండి; అధికంగా నీరు నింపవద్దు. నత్రజనిని సమపాళ్లలో విభజించి వేయండి; పొటాష్ లేకుండా అధిక యూరియా వాడవద్దు; మొక్కల రోగనిరోధక శక్తి కోసం తగినంత పొటాష్ ఎరువు వేయండి. గాలి ద్వారా వ్యాపించే సిద్ధబీజాల తీవ్రత పెరగకముందే ముందస్తు విత్తనం వేయండి; గట్లపై ఉన్న ఆక్సాలిస్ కలుపు మొక్కలను నాశనం చేయండి; తుప్పు తెగులును తట్టుకునే రకాలను నాటండి.
- **Hindi:** स्रोत: ICAR-IIMR और PJTSAU. नमी की कमी न होने दें; रात के समय पत्तियों पर अधिक नमी न रहे इसलिए सिंचाई का समय सही रखें; अत्यधिक पानी न भरें। नाइट्रोजन किस्तों में दें; पोटाश के बिना अतिरिक्त यूरिया न डालें; रोग प्रतिरोधक क्षमता बढ़ाने हेतु पोटाश का उचित प्रयोग करें। हवा से फैलने वाले बीजाणुओं से बचने के लिए समय पर बुवाई करें; मेड़ों से ऑक्सालिस खरपतवार नष्ट करें; रतुआ प्रतिरोधी संकर किस्में लगाएं।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for small, circular to elongate powdery cinnamon-brown to golden-brown pustules scattered across both upper and lower leaf surfaces. Pustules rupture the epidermis releasing brown dust that rubs off onto fingers.
- **Telugu:** ఆకుల పై మరియు క్రింది భాగాలలో ఏర్పడే చిన్న, గుండ్రని లేదా పొడవాటి దాల్చిన చెక్క గోధుమ రంగు నుండి బంగారు గోధుమ రంగు పొడి బొబ్బలను (తుప్పు పొక్కులు) గమనించండి. వేలితో తాకితే చేతికి తుప్పు పొడి అంటుకుంటుందో లేదో పరిశీలించండి.
- **Hindi:** पत्तियों की दोनों सतहों पर फैले हुए छोटे, गोल या लंबे दालचीनी जैसे भूरे से सुनहरे भूरे रंग के उभरे हुए फफोलों (पस्ट्यूल्स) को देखें। पत्तियों को छूने पर उंगलियों पर भूरा पाउडर लगना जांचें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if rust pustules cover >10% of ear leaf area before blister/milking stage during cool, overcast, high-humidity weather. Contact Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** చల్లని, మేఘావృతమైన తేమ వాతావరణంలో పాలుపోసుకునే దశకు ముందే కంకి ఆకుపై 10% కంటే ఎక్కువ భాగం తుప్పు పొక్కులు కప్పివేసినట్లయితే వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** ठंडे और अधिक नमी वाले मौसम में दाना दूधिया होने से पहले यदि भुट्टे वाली पत्ती का 10% से अधिक हिस्सा रतुआ फफोलों से ढक जाए, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Plant rust-resistant certified hybrid seeds recommended for your agro-climatic zone.
  - *Telugu:* మీ ప్రాంతానికి అనువైన కుంకుమ తెగులును తట్టుకునే రకాలను ఎంపిక చేసుకోండి.
  - *Hindi:* रतुआ रोधी प्रमाणित संकर किस्मों के बीज ही बोएं।
  - *Citation:* ICAR-IIMR Hybrid Directory ([Source Link](https://iimr.icar.gov.in)) — *"Genetic resistance is the primary defence against common rust in maize."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Mancozeb 75% WP or Propiconazole 25% EC` (Example: `Indofil M-45 / Tilt`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear protective gear. Observe safety interval before green ear harvest.
  - *Safety Line (TE):* రక్షణ దుస్తులు ధరించండి. పచ్చి కంకెల కోతకు ముందు తగిన సమయం పాటించండి.
  - *Safety Line (HI):* सुरक्षात्मक किट पहनें। भुट्टा तोड़ने से पहले प्रतीक्षा समय का ध्यान रखें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved for rust management in maize."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 18: `maize_northern_leaf_blight`
- **Crop:** Maize | **Type:** Disease | **Scientific Cause:** *Exserohilum turcicum* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Maize Northern Leaf Blight
  - **Telugu:** మొక్కజొన్న తుప్పు తెగులు / ఆకు ఎండడం
  - **Hindi:** मक्का का उत्तरी पत्ती झुलसा

#### Immediate Cultural Step
- **English:** Avoid sprinkler irrigation that keeps leaves wet for hours, and spray systemic fungicide before silking stage.
- **Telugu:** ఆకులు ఎక్కువసేపు తడిగా ఉండేలా తుంపర సేద్యం వాడవద్దు, కంకి దశకు ముందే శిలీంద్రనాశిని పిచికారీ చేయండి.
- **Hindi:** फव्वारा सिंचाई न करें जिससे पत्तियां गीली रहें, और भुट्टा निकलने से पहले दवा का छिड़काव करें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIMR Ludhiana & PJTSAU. Maintain adequate furrow irrigation; avoid waterlogging; avoid irrigating late in evening. Apply balanced NPK based on soil test; avoid excessive nitrogen top-dressing; apply full basal potash to strengthen leaf tissues. Deep summer plowing to bury infected crop debris; rotate with non-grass crops (pulses, oilseeds); plant certified blight-resistant hybrids.
- **Telugu:** మూలం: ICAR-IIMR & PJTSAU. సాళ్ల ద్వారా తగినంత నీటి తడులు ఇవ్వండి; నీరు నిల్వ ఉండకుండా చూడండి; సాయంత్రం పొద్దుపోయాక నీరు పెట్టవద్దు. సాయిల్ టెస్ట్ ఆధారంగా సమతుల్య NPK ఎరువులు వేయండి; అధిక నత్రజని పైపాటుగా వేయవద్దు; ఆకు కణజాలం దృఢంగా ఉండటానికి ఆఖరి దుక్కిలో సిఫార్సు చేసిన పొటాష్ వేయండి. వేసవిలో లోతు దుక్కులు చేసి తెగులు సోకిన మోడులను పాతిపెట్టండి; పప్పుధాన్యాలు లేదా నూనెగింజల పంటలతో పంట మార్పిడి చేయండి; తెగులును తట్టుకునే ధృవీకృత విత్తనాలను వాడండి.
- **Hindi:** स्रोत: ICAR-IIMR और PJTSAU. नालियों द्वारा उचित सिंचाई करें; जलभराव न होने दें; देर शाम सिंचाई से बचें। मिट्टी की जांच अनुसार संतुलित NPK दें; अधिक यूरिया न डालें; पत्तियों की मजबूती हेतु पूरी पोटाश बेसल डोज में दें। गर्मियों में गहरी जुताई कर फसल अवशेषों को दबाएं; दलहन या तिलहन के साथ फसल चक्र अपनाएं; रोग प्रतिरोधी प्रमाणित संकर बीज ही बोएं।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for long, elliptical or cigar-shaped grayish-green to tan lesions (2.5 to 15 cm long) with smooth margins starting on lower leaves and progressing upward toward the ear leaf; inspect for dark olive-green fungal spores during damp mornings.
- **Telugu:** క్రింది ఆకులపై ప్రారంభమై క్రమంగా కంకి ఆకు వైపు పాకే 2.5 నుండి 15 సెం.మీ పొడవైన చుట్ట ఆకారపు (సిగార్ ఆకారపు) బూడిద-ఆకుపచ్చని పొడవాటి మచ్చలను గమనించండి; తేమతో కూడిన ఉదయాల్లో మచ్చలపై ముదురు ఆలివ్-ఆకుపచ్చని బూజు పొర కనిపిస్తుందో లేదో పరిశీలించండి.
- **Hindi:** निचली पत्तियों पर शुरू होने वाले 2.5 से 15 सेमी लंबे, सिगार के आकार के भूरे-हरे धब्बे देखें जो ऊपर भुट्टे वाली पत्ती तक फैलते हैं; नम सुबह में धब्बों पर गहरे जैतून-हरे बीजाणु जांचें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer if cigar-shaped blight lesions appear on or above the ear leaf prior to or during silking, or if >25% of canopy shows blighting. Contact Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** కంకి పీచు దశకు ముందే కంకి ఆకుపై లేదా దాని పై ఆకులపై చుట్ట ఆకారపు పెద్ద మచ్చలు కనిపించినా లేదా 25% కంటే ఎక్కువ ఆకులు ఎండిపోయినా వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** सिल्किंग (भुट्टे से बाल निकलने) से पहले या उस समय यदि सिगार के आकार के धब्बे भुट्टे वाली पत्ती या उसके ऊपर दिखें, या 25% से अधिक पत्तियां झुलस जाएं, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Spray Pseudomonas fluorescens (10 g/L) at 30 and 45 days after sowing.
  - *Telugu:* విత్తిన 30 మరియు 45 రోజులకు సూడోమోనాస్ ఫ్లోరోసెన్స్ (10 గ్రా/లీ) పిచికారీ చేయండి.
  - *Hindi:* बुवाई के 30 और 45 दिन बाद स्यूडोमोनास फ्लोरोसेंस (10 ग्राम/लीटर) छिड़कें।
  - *Citation:* ANGRAU Maize Package ([Source Link](https://angrau.ac.in)) — *"Bio-agent foliar application retards Exserohilum lesion expansion."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Mancozeb 75% WP or Azoxystrobin 18.2% + Difenoconazole 11.4% SC` (Example: `Indofil M-45 / Amistar Top`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Wear face mask and gloves. Avoid skin contact with spray liquid.
  - *Safety Line (TE):* మాస్క్, గ్లౌజులు ధరించండి. మందు ద్రావణం చర్మానికి తగలకుండా జాగ్రత్తపడండి.
  - *Safety Line (HI):* मास्क और दस्ताने पहनें। दवा को त्वचा पर न लगने दें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved for Northern leaf blight in maize."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 19: `maize_fall_armyworm`
- **Crop:** Maize | **Type:** Pest | **Scientific Cause:** *Spodoptera frugiperda* | **Referral Threshold:** `SEVERE`
- **Names:**
  - **English:** Maize Fall Armyworm
  - **Telugu:** మొక్కజొన్న కత్తెర పురుగు
  - **Hindi:** मक्का का फाल आर्मीवर्म कीट

#### Immediate Cultural Step
- **English:** Scout central whorls in a W-pattern. At 5-10% plant damage, apply dry sand mixed with wood ash or neem cake into whorls immediately.
- **Telugu:** పొలంలో W-ఆకారంలో సుడులను పరిశీలించండి. 5-10% మొక్కలపై నష్టం ఉంటే, సుడులలో ఇసుక + బూడిద లేదా వేప పిండి మిశ్రమాన్ని వేయండి.
- **Hindi:** खेत में W-पैटर्न में पौधों की गोभ जांचें। 5-10% नुकसान पर गोभ में रेत और राख या नीम की खली तुरंत डालें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-NBAIR Bengaluru, ICAR-IIMR & NIPHM. Integrated Pest Management (IPM) steps: 1. Monitoring: Install pheromone traps at 5 per acre; scout weekly by checking 20 consecutive plants across 5 spots (100 plants total). 2. Threshold (ETL): 5% damaged plants at seedling stage (up to 30 days) or 10% damaged plants at knee-high stage. 3. Cultural Control: Deep summer plowing; timely early planting; intercropping with pulses (cowpea/pigeonpea 2:1 or 4:1); place dry sand mixed with ash or lime (9:1) directly into central whorls; erect 10 bird perches per acre; handpick and destroy egg masses in small plots. 4. Biological Control first: Conserve egg parasitoids (Trichogramma); spray Azadirachtin 1500 ppm or Metarhizium rileyi. 5. Chemical Control last: Apply CIB&RC registered insecticides (Chlorantraniliprole 18.5% SC or Spinetoram 11.7% SC) directed into whorls only when ETL is exceeded.
- **Telugu:** మూలం: ICAR-NBAIR, ICAR-IIMR & NIPHM. సమగ్ర సస్యరక్షణ (IPM) పద్ధతులు: 1. గమనింపు: ఎకరాకు 5 లింగాకర్షక బుట్టలను అమర్చండి; పొలంలో 5 చోట్ల 20 వరుస మొక్కల చొప్పున (మొత్తం 100 మొక్కలు) ప్రతి వారం పరిశీలించండి. 2. ఆర్థిక నష్ట పరిమితి (ETL): మొలక దశలో (30 రోజుల వరకు) 5% మొక్కలకు లేదా మోకాలి లోతు దశలో 10% మొక్కలకు నష్టం కనిపించడం. 3. సేద్య పద్ధతులు: వేసవిలో లోతు దుక్కులు; సకాలంలో ముందస్తు విత్తడం; అలసంద లేదా కందితో అంతరపంట (2:1 లేదా 4:1); మొక్కల సుడులలో పొడి ఇసుక మరియు బూడిద లేదా సున్నం మిశ్రమం (9:1) వేయడం; ఎకరాకు 10 పక్షి స్థావరాలు ఏర్పాటు చేయడం; చిన్న కమతాలలో గ్రుడ్ల సముదాయాలను ఏరి నాశనం చేయడం. 4. జీవ నియంత్రణ మొదట: ట్రైకోగ్రామా గుడ్డు పరాన్నజీవులను విడుదల చేయండి; 1500 ppm అజాడిరక్టిన్ (వేప మందు) లేదా మెటారైజియం పిచికారీ చేయండి. 5. రసాయన మందులు చివరగా: ETL దాటినప్పుడు మాత్రమే CIB&RC అనుమతించిన పురుగుమందులను (క్లోరాంట్రానిలిప్రోల్ లేదా స్పినెటోరమ్) సుడులలో పడేలా పిచికారీ చేయండి.
- **Hindi:** स्रोत: ICAR-NBAIR, ICAR-IIMR और NIPHM. एकीकृत कीट प्रबंधन (IPM) उपाय: 1. निगरानी: प्रति एकड़ 5 फेरोमोन ट्रैप लगाएं; 5 स्थानों पर 20-20 पौधों (कुल 100 पौधे) का साप्ताहिक निरीक्षण करें। 2. आर्थिक सीमा (ETL): अंकुरण से 30 दिन तक 5% क्षतिग्रस्त पौधे या घुटने की ऊंचाई पर 10% क्षतिग्रस्त पौधे। 3. कृषि उपाय: गहरी ग्रीष्मकालीन जुताई; समय पर बुवाई; लोबिया/अरहर के साथ अंतरफसल (2:1 या 4:1); पौधों के पोंगों (गोभ) में सूखी रेत व राख/चूने का मिश्रण (9:1) डालें; प्रति एकड़ 10 पक्षी मचान लगाएं; अंडों और सूंडियों को हाथ से चुनकर नष्ट करें। 4. जैविक नियंत्रण पहले: ट्राइकोडर्मा/ट्राइकोग्रामा छोड़ें; एज़ाडिरैक्टीन 1500 ppm (नीम तेल) या मेटाराइजियम का छिड़काव करें। 5. रासायनिक नियंत्रण अंत में: केवल ETL पार होने पर ही CIB&RC पंजीकृत कीटनाशक (क्लोरेंट्रानिलिप्रोल या स्पिनेटोरम) सीधे गोभ में स्प्रे करें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Watch for pinhole punctures and papery transparent windowing on young whorl leaves caused by early instars; watch for coarse, moist sawdust-like fecal pellets (frass) accumulating inside central whorls and ragged torn leaves.
- **Telugu:** లేత ఆకులపై చిన్న రంధ్రాలు మరియు కాగితం వంటి పారదర్శక కిటికీ మచ్చలు (విండోయింగ్) ఏర్పడటం గమనించండి; మొక్క సుడుల (గోభ) లోపల చెక్క పొట్టు వంటి తడి మల విసర్జన పేరుకుపోవడం మరియు ఆకులు చిరిగిపోయినట్లు కనిపించడాన్ని పరిశీలించండి.
- **Hindi:** गोभ की नई पत्तियों पर सुंडियों द्वारा किए गए बारीक छेद और कागज जैसी पारदर्शी खिड़कियां (विंडोइंग) देखें; केंद्रीय गोभ के अंदर लकड़ी के बुरादे जैसा गीला मल जमा होना और फटी हुई पत्तियां जांचें।

#### (c) When to Call Extension Officer
- **English:** Call extension officer IMMEDIATELY if whorl infestation exceeds ETL (5% at seedling stage, 10% at vegetative stage), or large larvae (>1.5 cm) are detected feeding deep inside central whorls. Contact Kisan Call Centre (Toll-Free 1800-180-1551).
- **Telugu:** సుడుల నష్టం ఆర్థిక పరిమితి (మొలక దశలో 5%, పైరు దశలో 10%) దాటినా లేదా 1.5 సెం.మీ కంటే పెద్ద లార్వాలు సుడుల లోపల చేరి తింటున్నట్లు గుర్తించినా వెంటనే వ్యవసాయ విస్తరణ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.
- **Hindi:** यदि गोभ का प्रकोप ईटीएल (अंकुरण पर 5%, वानस्पतिक पर 10%) से अधिक हो जाए या 1.5 सेमी से बड़ी सूंडियां गोभ के अंदर नुकसान पहुंचाती मिलें, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Field Scouting & Cultural Control: Scout weekly in a W-pattern; handpick and crush egg masses and young larvae; install 10 bird perches per acre.
  - *Telugu:* క్షేత్ర పరిశీలన & సాంస్కృతిక చర్యలు: ప్రతి వారం W-పద్ధతిలో గమనించండి; గుడ్ల సముదాయాలు, లేత పురుగులను ఏరి నాశనం చేయండి; ఎకరాకు 10 పక్షి స్థావరాలు ఏర్పాటు చేయండి.
  - *Hindi:* खेत निगरानी और सांस्कृतिक नियंत्रण: हर हफ्ते W-पैटर्न में खेत का निरीक्षण करें; अंडे और सूंडियों को चुनकर नष्ट करें; प्रति एकड़ 10 पक्षी मचान लगाएं।
  - *Citation:* ICAR-NBAIR FAW Advisory ([Source Link](https://nbair.res.in)) — *"Regular scouting in W pattern and bird perches suppress early larval populations."*
- **Step 2:** Mechanical Whorl Barrier: Apply dry abrasive sand mixed with wood ash or neem cake (9:1 ratio) directly into central plant whorls at 15-20 days after emergence.
  - *Telugu:* సుడులలో ఇసుక పూరకం: మొలకెత్తిన 15-20 రోజులకు మొక్కల సుడులలో ఇసుక + బూడిద లేదా వేప పిండి మిశ్రమాన్ని (9:1 నిష్పత్తి) వేసి పురుగులను నలిపి చంపండి.
  - *Hindi:* गोभ में रेत का उपयोग: बुवाई के 15-20 दिन बाद पौधों की गोभ में सूखी रेत और लकड़ी की राख (9:1 अनुपात) डालें ताकि सूंडी नष्ट हो सके।
  - *Citation:* PJTSAU Maize Pest Advisory ([Source Link](https://pjtsau.edu.in)) — *"Whorl application of dry sand mixed with ash suffocates and abrades FAW larvae."*
- **Step 3:** Biological Monitoring & Bio-agents: Install pheromone traps (5/acre) to monitor adult moths; spray Metarhizium rileyi or Bacillus thuringiensis (Bt) at 2 g/L, or Azadirachtin 1500 ppm (5 ml/L).
  - *Telugu:* జీవ నియంత్రణ: రెక్కల పురుగుల నిఘా కోసం ఎకరాకు 5 లింగాకర్షక బుట్టలు అమర్చండి; మెటారైజియం లేదా బి.టి (2 గ్రా/లీ) లేదా వేప నూనె 1500 ppm (5 మి.లీ/లీ) పిచికారీ చేయండి.
  - *Hindi:* जैविक नियंत्रण: निगरानी के लिए प्रति एकड़ 5 फेरोमोन ट्रैप लगाएं; मेटाराइजियम या बीटी (2 ग्राम/लीटर) या नीम तेल (5 मिली/लीटर) का छिड़काव करें।
  - *Citation:* NIPHM FAW Management Protocol ([Source Link](https://niphm.gov.in)) — *"Pheromone monitoring and bio-pesticides (Bt / Metarhizium) are highly effective in early whorl stages."*

#### Chemical Treatments (CIB&RC Registered)
- **Step 1:** Active Ingredient: `Emamectin Benzoate 5% SG or Spinetoram 11.7% SC` (Example: `Proclaim / Delegate`)
  - *Dose Policy:* `follow the product label` (dose_verified: `False`)
  - *Safety Line (EN):* Direct spray nozzle strictly into plant whorls. Wear chemical-resistant gloves and mask. Avoid spraying during high heat.
  - *Safety Line (TE):* మందు నాజిల్ ను నేరుగా మొక్క సుడిలోకి పెట్టి పిచికారీ చేయండి. గ్లౌజులు, మాస్క్ ధరించండి. ఎండలో పిచికారీ చేయవద్దు.
  - *Safety Line (HI):* स्प्रे की धार सीधे पौधे के पोंगे (गोभ) में डालें। दस्ताने और मास्क पहनें। तेज धूप में छिड़काव न करें।
  - *Citation:* CIB&RC Major Uses ([Source Link](https://cibrc.nic.in)) — *"Approved specifically for Fall Armyworm management in maize."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---

### Class 20: `maize_healthy`
- **Crop:** Maize | **Type:** Healthy | **Scientific Cause:** *None* | **Referral Threshold:** `NONE`
- **Names:**
  - **English:** Healthy Maize
  - **Telugu:** ఆరోగ్యకరమైన మొక్కజొన్న
  - **Hindi:** स्वस्थ मक्का

#### Immediate Cultural Step
- **English:** No chemical application needed. Ensure timely irrigation, especially during critical flowering and grain-filling stages.
- **Telugu:** రసాయనాలు అవసరం లేదు. పూత మరియు గింజ పాలుపోసుకునే క్లిష్ట దశలలో సక్రమంగా నీటి తడులు ఇవ్వండి.
- **Hindi:** किसी दवा की जरूरत नहीं। भुट्टा और दाने बनते समय सही समय पर पानी दें।

#### (a) Prevention & Field Practice (Irrigation & Nutrients)
- **English:** Source: ICAR-IIMR Ludhiana & PJTSAU GAP. Irrigate at critical growth stages: knee-high, tasseling, silking, and grain filling; avoid drought stress during flowering. Apply fertilizers based on soil testing; split nitrogen into three doses (basal, knee-high, and pre-tasseling); apply zinc sulphate (25 kg/ha) at sowing. Maintain 60x20 cm plant spacing; earth up soil at knee-high stage; keep field weed-free for first 35 days.
- **Telugu:** మూలం: ICAR-IIMR & PJTSAU GAP. కీలక దశల్లో తడులు ఇవ్వండి: మోకాలి లోతు దశ, వెన్ను వేసే దశ, పీచు దశ మరియు గింజ పాలుపోసుకునే దశ; ముఖ్యంగా పూత సమయంలో నీటి ఎద్దడి రానివ్వకండి. సాయిల్ టెస్ట్ ప్రకారం ఎరువులు వేయండి; నత్రజనిని 3 దఫాలుగా వేయండి (ఆఖరి దుక్కిలో, మోకాలి లోతులో, పూతకు ముందు); విత్తే సమయంలో ఎకరాకు 10-12 కిలోల జింక్ సల్ఫేట్ వేయండి. 60x20 సెం.మీ మొక్కల దూరం పాటించండి; మోకాలి లోతు దశలో మొదళ్లకు మట్టిని ఎగదోయండి; మొదటి 35 రోజులు కలుపు లేకుండా చూసుకోండి.
- **Hindi:** स्रोत: ICAR-IIMR और PJTSAU GAP. महत्वपूर्ण अवस्थाओं (घुटने की ऊंचाई, मंजरी, सिल्किंग और दाना भरते समय) पर सिंचाई करें; फूल आते समय सूखे का तनाव न आने दें। मृदा परीक्षण अनुसार खाद दें; नाइट्रोजन 3 बार दें (बुवाई, घुटने की ऊंचाई, मंजरी पूर्व); बुवाई पर जिंक सल्फेट (25 किग्रा/हेक्टेयर) डालें। 60x20 सेमी दूरी रखें; घुटने की ऊंचाई पर पौधों पर मिट्टी चढ़ाएं; पहले 35 दिन खरपतवार मुक्त रखें।

#### (b) Early-Warning Signs (What to Watch)
- **English:** Foliage is dark green with sturdy stalks and clean central whorls. Regularly inspect whorls for pinholes or frass (fall armyworm), look for stem borer dead-hearts, and check leaves for rust pustules.
- **Telugu:** పైరు దృఢమైన కాండాలు మరియు శుభ్రమైన సుడులతో ముదురు ఆకుపచ్చగా ఆరోగ్యంగా ఉంది. సుడులలో రంధ్రాలు లేదా మల విసర్జన (కత్తెర పురుగు), కాండం తొలుచు పురుగు వల్ల మొవ్వు చావడం లేదా ఆకులపై తుప్పు పొక్కులు ఏమైనా కనిపిస్తున్నాయేమో క్రమం తప్పకుండా గమనించండి.
- **Hindi:** फसल मजबूत तनों और साफ गोभ के साथ गहरी हरी है। गोभ में छेद या बुरादा (फॉल आर्मीवर्म), तना छेदक से मृत गोभ (डेड-हार्ट) या पत्तियों पर रतुआ फफोलों की नियमित जांच करते रहें।

#### (c) When to Call Extension Officer
- **English:** Routine guidance only. Contact local KVK or Kisan Call Centre (Toll-Free 1800-180-1551) for advice on tasseling irrigation schedules, cob filling nutrition, and grain moisture management.
- **Telugu:** సాధారణ సలహాల కొరకు మాత్రమే. వెన్ను వేసే సమయంలో నీటి తడులు, కంకిలో గింజ సమృద్ధిగా ఏర్పడటానికి అవసరమైన పోషణ మరియు కోత నాణ్యత కొరకు స్థానిక KVK లేదా కిసాన్ కాల్ సెంటర్ (1800-180-1551) ను సంప్రదించండి.
- **Hindi:** केवल सामान्य मार्गदर्शन हेतु। मंजरी निकलते समय सिंचाई कार्यक्रम, भुट्टे में दाना भरने हेतु पोषण और कटाई पूर्व नमी प्रबंधन की सलाह के लिए स्थानीय KVK या किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।

#### Natural & Cultural Treatments
- **Step 1:** Scout 20 consecutive plants in 5 locations across the field weekly to maintain pest-free status.
  - *Telugu:* తెగుళ్ళ బెడద లేకుండా చూసుకోవడానికి పొలంలో వారానికోసారి 5 చోట్ల 20 మొక్కలను పరిశీలించండి.
  - *Hindi:* खेत में हर हफ्ते 5 अलग-अलग जगहों पर 20-20 पौधों की जांच करें ताकि फसल स्वस्थ रहे।
  - *Citation:* ICAR-IIMR Surveillance Guide ([Source Link](https://iimr.icar.gov.in)) — *"Systematic scouting prevents sudden pest outbreaks in maize."*

##### Review Sign-off Box:
- [ ] **Telugu Terminology:** Correct, local farmer-friendly, free of jargon.
- [ ] **Hindi Terminology:** Correct, standard agricultural usage, easily understood.
- [ ] **Agronomy & Field Practices:** Sound, university-endorsed, accurate irrigation/nutrient steps.
- [ ] **Pest IPM & Referral:** Appropriate thresholds and safe guidance.
- **Reviewer Name & Designation:** ________________________________________
- **Signature / Date:** __________________
- **Corrections / Notes:** __________________________________________________

---
