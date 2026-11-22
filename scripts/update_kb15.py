# -*- coding: utf-8 -*-
"""
Script to update knowledge_base.json for Task KB15:
- Adds top-level source, source_url, date_read to all entries.
- Adds complete entries for rice_stem_borer, rice_hispa, tomato_septoria_leaf_spot.
- Syncs to app/src/main/assets/kb/knowledge_base.json.
"""

import json
import shutil
from pathlib import Path

KB_DATA_PATH = Path("data/kb/knowledge_base.json")
KB_ASSET_PATH = Path("app/src/main/assets/kb/knowledge_base.json")

TOP_SOURCES = {
    "rice_bacterial_leaf_blight": {
        "source": "ICAR-IIRR Hyderabad & ANGRAU Crop Production Guide",
        "source_url": "https://icar-iirr.org",
        "date_read": "2026-11-22"
    },
    "rice_brown_spot": {
        "source": "TNAU Agritech Portal & ICAR-IIRR Rice Pathology",
        "source_url": "https://agritech.tnau.ac.in",
        "date_read": "2026-11-22"
    },
    "rice_leaf_blast": {
        "source": "ICAR-IIRR Hyderabad & PJTSAU Vyavasaya Darsini",
        "source_url": "https://icar-iirr.org",
        "date_read": "2026-11-22"
    },
    "rice_sheath_blight": {
        "source": "ANGRAU Guidelines & ICAR-IIRR Package of Practices",
        "source_url": "https://angrau.ac.in",
        "date_read": "2026-11-22"
    },
    "rice_healthy": {
        "source": "ICAR-IIRR Package of Practices for Rice & PJTSAU",
        "source_url": "https://icar-iirr.org",
        "date_read": "2026-11-22"
    },
    "cotton_bacterial_blight": {
        "source": "ICAR-CICR Cotton Advisory & PJTSAU Vyavasaya Darsini",
        "source_url": "https://cicr.icar.gov.in",
        "date_read": "2026-11-22"
    },
    "cotton_fusarium_wilt": {
        "source": "ICAR-CICR Cotton Advisory & ANGRAU Crop Guide",
        "source_url": "https://cicr.icar.gov.in",
        "date_read": "2026-11-22"
    },
    "cotton_healthy": {
        "source": "NIPHM Cotton IPM & ICAR-CICR Package of Practices",
        "source_url": "https://niphm.gov.in",
        "date_read": "2026-11-22"
    },
    "chilli_bacterial_spot": {
        "source": "ICAR-IIHR Bengaluru Solanaceous Crop IPM & PJTSAU",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "chilli_leaf_curl_virus": {
        "source": "ICAR-IIHR Bengaluru IPM for Chilli & PJTSAU Vyavasaya Darsini",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "chilli_healthy": {
        "source": "ICAR-IIHR Chilli Production Guide & ANGRAU",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "tomato_early_blight": {
        "source": "ICAR-IIHR Package for Solanaceous Crops & NIPHM",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "tomato_late_blight": {
        "source": "ICAR-IIHR Bengaluru & TNAU Agritech Portal",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "tomato_yellow_leaf_curl_virus": {
        "source": "ICAR-IIHR Tomato IPM & NIPHM Advisory",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "tomato_spider_mites": {
        "source": "ICAR-IIHR Tomato IPM, ANGRAU & NBAIR Bengaluru",
        "source_url": "https://nbair.res.in",
        "date_read": "2026-11-22"
    },
    "tomato_healthy": {
        "source": "ICAR-IIHR Tomato Production Guide & TNAU",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22"
    },
    "maize_gray_leaf_spot": {
        "source": "ICAR-IIMR Ludhiana Maize Pathology Bulletin & PJTSAU",
        "source_url": "https://iimr.icar.gov.in",
        "date_read": "2026-11-22"
    },
    "maize_common_rust": {
        "source": "ICAR-IIMR Maize Pathology Directory & PJTSAU",
        "source_url": "https://iimr.icar.gov.in",
        "date_read": "2026-11-22"
    },
    "maize_northern_leaf_blight": {
        "source": "ANGRAU Maize Package & ICAR-IIMR Disease Management",
        "source_url": "https://angrau.ac.in",
        "date_read": "2026-11-22"
    },
    "maize_fall_armyworm": {
        "source": "ICAR-NBAIR FAW Advisory, PJTSAU Maize Pest Advisory & NIPHM",
        "source_url": "https://nbair.res.in",
        "date_read": "2026-11-22"
    },
    "maize_healthy": {
        "source": "ICAR-IIMR Good Agricultural Practices for Maize & ANGRAU",
        "source_url": "https://iimr.icar.gov.in",
        "date_read": "2026-11-22"
    }
}

NEW_ENTRIES = [
    {
        "class_id": "rice_stem_borer",
        "index": 21,
        "crop": "rice",
        "type": "pest",
        "pathogen": "Scirpophaga incertulas",
        "stage": "Vegetative",
        "source": "ICAR-IIRR Integrated Pest Management in Rice, PJTSAU & NBAIR",
        "source_url": "https://icar-iirr.org",
        "date_read": "2026-11-22",
        "names": {
            "en": "Rice Yellow Stem Borer",
            "te": "వరి కాండం తొలుచు పురుగు",
            "hi": "धान का पीला तना छेदक कीट"
        },
        "needs_expert_review": True,
        "referral_threshold": "SEVERE",
        "severity_advice": {
            "MILD": {
                "en": "Occasional dead hearts (<2% tillers) or egg masses scattered. Install pheromone traps and conserve natural parasitoids.",
                "te": "అక్కడక్కడా 2% కంటే తక్కువ ఎండిన పిలకలు లేదా గుడ్ల గుంపులు. లింగాకర్షక బుట్టలు అమర్చండి మరియు సహజ మిత్రపురుగులను కాపాడండి.",
                "hi": "2% से कम तने सूखे या इक्का-दुक्का अंडे दिखे। फेरोमोन जाल लगाएं और मित्र कीटों की रक्षा करें।"
            },
            "MODERATE": {
                "en": "Dead hearts 2-5% or 1 egg mass per square meter. Apply biocontrol egg parasitoids or neem formulations to whorls.",
                "te": "ఎండిన పిలకలు 2-5% లేదా చదరపు మీటరుకు ఒక గుడ్ల సముదాయం. ట్రైకోగ్రామా కార్డులు లేదా వేప నూనెను ఆకుల సుడులలో వాడండి.",
                "hi": "2-5% मृत केंद्र या 1 अंडा समूह प्रति वर्ग मीटर। ट्राइकोग्रामा परजीवी कार्ड या नीम आधारित दवा का प्रयोग करें।"
            },
            "SEVERE": {
                "en": "Dead hearts >10% or whiteheads appearing at panicle emergence. Widespread crop damage; consult KVK within 24 hours.",
                "te": "ఎండిన పిలకలు 10% మించినా లేదా కంకులు తెల్లబడి గింజ పాలుపోయకుండా ఉంటే పంట నష్టం తీవ్రం. 24 గంటల్లో వ్యవసాయాధికారిని సంప్రదించండి.",
                "hi": "10% से अधिक मृत केंद्र या बालियां सफेद निकलने लगें। भारी नुकसान की आशंका; 24 घंटे में कृषि विज्ञान केंद्र से संपर्क करें।"
            }
        },
        "immediate_step": {
            "en": "Set up pheromone traps for pest monitoring; do not apply excessive nitrogen which attracts moth egg-laying.",
            "te": "పురుగు ఉధృతిని పర్యవేక్షించడానికి లింగాకర్షక బుట్టలు అమర్చండి; అధిక నత్రజని వాడకం పురుగు ఉధృతిని పెంచుతుంది కాబట్టి యూరియా వేయకండి.",
            "hi": "कीट निगरानी के लिए फेरोमोन ट्रैप लगाएं; अधिक यूरिया न डालें क्योंकि इससे मादा तितली अंडे देने के लिए आकर्षित होती है।"
        },
        "explain_text": {
            "en": "A major monophagous lepidopteran pest of rice. The larva bores into the central tiller or stem, cutting off vascular bundles and causing dead hearts in the vegetative stage and empty whiteheads at heading.",
            "te": "వరి పంటను ఆశించే ప్రధాన పురుగు. లార్వా కాండం లోపలికి తొలిచి నాళికా పుంజాలను కత్తిరించడం వలన పిలకల దశలో తీగ చనిపోవడం (డెడ్ హార్ట్) మరియు చిరుపొట్ట దశలో తెల్ల కంకులు ఏర్పడతాయి.",
            "hi": "धान का एक प्रमुख कीट। इसकी इल्ली तने के अंदर छेद करके संवहनी ऊतकों को काट देती है, जिससे कल्ले फूटते समय मृत केंद्र (डेड हार्ट) और बाली निकलते समय दाना-रहित सफेद बाली बन जाती है।"
        },
        "treatment": {
            "natural": [
                {
                    "step": 1,
                    "action": {
                        "en": "Clip seedling leaf tips before transplanting to destroy egg masses and install pheromone traps at 5 per hectare for monitoring.",
                        "te": "నాట్లు వేసే ముందు నారు చివరలను తుంచి గుడ్ల సముదాయాలను నాశనం చేయండి మరియు పర్యవేక్షణకు హెక్టారుకు 5 లింగాకర్షక బుట్టలు పెట్టండి.",
                        "hi": "रोपाई से पहले पौध के ऊपरी सिरों को काट दें ताकि अंडे नष्ट हों और निगरानी हेतु 5 फेरोमोन ट्रैप लगाएं।"
                    },
                    "source": "ICAR-IIRR Hyderabad",
                    "source_url": "https://icar-iirr.org",
                    "source_quote": "Clipping seedling tips removes over 80% of Scirpophaga egg masses."
                },
                {
                    "step": 2,
                    "action": {
                        "en": "Release Trichogramma japonicum egg parasitoid @ 100,000 adults/ha weekly starting 30 days after transplanting.",
                        "te": "నాటిన 30 రోజుల నుండి వారం వ్యవధిలో ట్రైకోగ్రామా జపోనికమ్ గుడ్ల పరాన్నజీవులను హెక్టారుకు లక్ష చొప్పున విడుదల చేయండి.",
                        "hi": "रोपाई के 30 दिन बाद से ट्राइकोग्रामा जेपोनिकम परजीवी कार्ड (1 लाख प्रति हेक्टेयर) हर हफ्ते लगाएं।"
                    },
                    "source": "ICAR-NBAIR Bengaluru",
                    "source_url": "https://nbair.res.in",
                    "source_quote": "Trichogramma japonicum provides effective biological control of yellow stem borer eggs in irrigated rice."
                },
                {
                    "step": 3,
                    "action": {
                        "en": "Spray neem oil 3000 ppm (3-5 ml/L) or NSKE 5% at early moth flight to deter egg laying.",
                        "te": "రెక్కల పురుగులు కనిపించిన ప్రారంభంలో గుడ్లు పెట్టకుండా వేపనూనె (3-5 మి.లీ/లీ) లేదా వేప కషాయం పిచికారీ చేయండి.",
                        "hi": "शुरुआत में मादा कीट को अंडे देने से रोकने हेतु नीम का तेल 3000 ppm (3-5 मिली/लीटर) छिड़कें।"
                    },
                    "source": "NIPHM Hyderabad",
                    "source_url": "https://niphm.gov.in",
                    "source_quote": "Botanical extracts and neem deter Scirpophaga oviposition and reduce early larval penetration."
                }
            ],
            "chemical": [
                {
                    "step": 1,
                    "active_ingredient": "Chlorantraniliprole 0.4% GR",
                    "product_example": "Ferterra / Rynaxypyr",
                    "dose_verified": False,
                    "dose": "follow the product label",
                    "phi_days": 30,
                    "safety_line": {
                        "en": "Wear rubber gloves during application. Maintain standing water of 2-3 cm for 3-4 days after application. Follow label.",
                        "te": "మందు చల్లేటప్పుడు రబ్బరు గ్లౌజులు ధరించండి. మందు వేసిన తర్వాత పొలంలో 3-4 రోజులు 2-3 సెం.మీ నీరు ఉంచండి. లేబుల్ పాటించండి.",
                        "hi": "दवा डालते समय दस्ताने पहनें। दवा डालने के बाद 3-4 दिनों तक खेत में 2-3 सेमी पानी बनाए रखें। लेबल पढ़ें।"
                    },
                    "source": "CIB&RC Major Uses",
                    "source_url": "https://cibrc.nic.in",
                    "source_quote": "Chlorantraniliprole 0.4% GR is registered by CIB&RC for stem borer in paddy."
                }
            ]
        },
        "regional": {
            "telangana": {
                "source": "PJTSAU",
                "differs": False
            },
            "andhra_pradesh": {
                "source": "ANGRAU",
                "differs": False
            },
            "other": {
                "source": "ICAR",
                "differs": False
            }
        },
        "prevention_and_field_practice": {
            "en": "Source: ICAR-IIRR, NIPHM & PJTSAU. Practice pest monitoring with 5 pheromone traps per hectare. Clip seedling leaf tips before transplanting to eliminate stem borer egg masses. Observe economic threshold of 2% dead hearts or 1 egg mass/m2 before intervention. Conserve biological agents like Trichogramma japonicum parasitoids (release at 100,000/ha). Avoid excessive nitrogenous fertilizers. Drain standing water briefly during peak egg hatching. Apply targeted chemical granular treatment only if threshold is exceeded.",
            "te": "మూలం: ICAR-IIRR, NIPHM & PJTSAU. హెక్టారుకు 5 లింగాకర్షక బుట్టలు అమర్చి పురుగు పర్యవేక్షణ (మోనిటరింగ్) చేయండి. నాట్లు వేసే ముందు నారు కొనలను తుంచడం ద్వారా గుడ్ల గుంపులను తొలగించండి. ఆర్ధిక నష్ట పరిమితి (2% ఎండిన పిలకలు లేదా చ.మీ.కు 1 గుడ్ల సముదాయం) గమనించండి. ట్రైకోగ్రామా జపోనికమ్ వంటి జీవ నియంత్రణ మిత్ర పురుగులను (హెక్టారుకు 1,00,000) వినియోగించండి. అధిక నత్రజని ఎరువులను వేయవద్దు. పురుగు ఉధృతి పరిమితి దాటితేనే సిఫార్సు చేసిన రసాయన మందులను వాడండి.",
            "hi": "स्रोत: ICAR-IIRR, NIPHM और PJTSAU. खेत में कीट निगरानी (मॉनिटरिंग) हेतु 5 फेरोमोन ट्रैप प्रति हेक्टेयर लगाएं। रोपाई से पहले पौध के ऊपरी सिरों को तोड़ दें ताकि अंडों के गुच्छे नष्ट हो जाएं। आर्थिक नुकसान की सीमा (2% डेड हार्ट या 1 अंडा समूह प्रति वर्ग मीटर) पार होने पर ही कदम उठाएं। ट्राइकोग्रामा जेपोनिकम जैसे जैविक नियंत्रण परजीवी (1 लाख प्रति हेक्टेयर) छोड़ें। अधिक यूरिया का प्रयोग न करें। सीमा पार होने पर ही लक्षित रासायनिक कीटनाशक का प्रयोग करें।"
        },
        "early_warning_signs": {
            "en": "Scout for yellow-brown egg masses covered with buff-colored anal hairs near leaf tips in nursery and main field. Central shoots wilting, drying, and easily pulled out by hand (dead hearts).",
            "te": "నర్సరీ మరియు ప్రధాన పొలంలో ఆకుల కొనల వద్ద పసుపు-గోధుమ రంగు గుడ్ల సముదాయాలు గమనించండి. మధ్య పిలకలు ఎండిపోయి, చేత్తో లాగితే సులభంగా ఊడివచ్చే డెడ్ హార్ట్ లక్షణాలను పర్యవేక్షించండి.",
            "hi": "नर्सरी और मुख्य खेत में पत्तियों के सिरों पर भूरे रंग के बालों से ढके अंडों के समूह देखें। मुख्य तना सूखने लगता है और हाथ से खींचने पर आसानी से बाहर आ जाता है (डेड हार्ट)।"
        },
        "when_to_call_extension_officer": {
            "en": "Call extension officer if dead hearts exceed 5% during tillering or whiteheads appear at flowering, threatening severe yield loss. Call Kisan Call Centre (Toll-Free 1800-180-1551).",
            "te": "పిలక దశలో 5% పైగా పిలకలు ఎండిపోయినా లేదా చిరుపొట్ట దశలో తెల్ల కంకులు కనిపిస్తే పంట దిగుబడి తీవ్రంగా తగ్గే ప్రమాదం ఉన్నందున వెంటనే వ్యవసాయ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.",
            "hi": "यदि कल्ले फूटते समय 5% से अधिक डेड हार्ट हों या बाली निकलते समय सफेद बालियां दिखें, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर कॉल करें।"
        }
    },
    {
        "class_id": "rice_hispa",
        "index": 22,
        "crop": "rice",
        "type": "pest",
        "pathogen": "Dicladispa armigera",
        "stage": "Vegetative",
        "source": "ANGRAU Vyavasaya Darsini, ICAR-IIRR Rice Hispa Advisory & NIPHM",
        "source_url": "https://angrau.ac.in",
        "date_read": "2026-11-22",
        "names": {
            "en": "Rice Hispa",
            "te": "వరి హిస్పా (ముళ్ల పురుగు)",
            "hi": "धान का हिस्पा कीट (कांटेदार कीट)"
        },
        "needs_expert_review": True,
        "referral_threshold": "SEVERE",
        "severity_advice": {
            "MILD": {
                "en": "Occasional beetles (<1 per hill) or light streaking on outer leaves. Sweep net collection and clip damaged tips.",
                "te": "దుబ్బుకు 1 కంటే తక్కువ పురుగులు లేదా బయటి ఆకులపై తేలికపాటి చారలు. వలలతో పురుగులను పట్టి నాశనం చేయండి మరియు ఆకు కొనలను తుంచండి.",
                "hi": "प्रति पौधा 1 से कम कीड़ा या पत्तियों पर हल्की धारियां। कीट पकड़ने वाले जाल (स्वीप नेट) से कीड़ों को नष्ट करें।"
            },
            "MODERATE": {
                "en": "1-2 adults per hill or >1 damaged leaf per hill. Sweep netting, release biocontrol agents, and spray neem extracts.",
                "te": "దుబ్బుకు 1-2 పురుగులు లేదా పాడైన ఆకులు. వలలతో పురుగులను ఏరివేయండి, వేప కషాయం పిచికారీ చేయండి.",
                "hi": "1-2 वयस्क कीट प्रति पौधा या प्रति पौधा 1 क्षतिग्रस्त पत्ती। जाल से पकड़ें और नीम आधारित कीटनाशक छिड़कें।"
            },
            "SEVERE": {
                "en": ">2 adults per hill or widespread drying resembling fire scorching. Heavy defoliation; consult KVK agronomist immediately.",
                "te": "దుబ్బుకు 2 కంటే ఎక్కువ పురుగులు ఉండి, ఆకులన్నీ ఎండిపోయి పొలం కాలిపోయినట్లు కనిపిస్తే నష్టం తీవ్రం. వెంటనే వ్యవసాయాధికారిని సంప్రదించండి.",
                "hi": "प्रति पौधा 2 से अधिक कीड़े और पत्तियां झुलसकर सूख रही हों। गंभीर नुकसान; तुरंत कृषि अधिकारी से संपर्क करें।"
            }
        },
        "immediate_step": {
            "en": "Use a sweep net to collect and destroy adult spiny beetles from field corners and bunds; avoid over-fertilizing with nitrogen.",
            "te": "పొలం గట్లు మరియు మూలల వద్ద తిరుగుతున్న ముళ్ళ పురుగులను వలతో పట్టి నాశనం చేయండి; అధిక యూరియా వేయవద్దు.",
            "hi": "कीट पकड़ने वाले जाल से वयस्क कीड़ों को पकड़कर नष्ट करें; अधिक यूरिया का छिड़काव तुरंत बंद करें।"
        },
        "explain_text": {
            "en": "A destructive spiny beetle pest of rice. The adult beetle scrapes chlorophyll from upper leaf surfaces in parallel white streaks, while the grub mines between the epidermal layers creating translucent blister patches, giving the crop a withered, burnt appearance.",
            "te": "వరిని ఆశించే ముళ్ల పురుగు (బీటిల్). తల్లి పురుగులు ఆకులపై ఉన్న పత్రహరితాన్ని సమాంతరంగా గోకడం వలన తెల్లని చారలు ఏర్పడతాయి; పిల్ల పురుగులు ఆకు పొరల మధ్య చొరబడి తినడం వలన తెల్లటి పొక్కులు ఏర్పడి పంట ఎండిపోయినట్లు కనిపిస్తుంది.",
            "hi": "धान की पत्तियों को नुकसान पहुंचाने वाला कांटेदार भृंग (कीट)। वयस्क कीट पत्तियों की ऊपरी सतह से क्लोरोफिल खुरचकर सफेद समानांतर धारियां बना देते हैं और इसकी सूंडी पत्तियों के भीतर सुरंग बनाकर सुखा देती है।"
        },
        "treatment": {
            "natural": [
                {
                    "step": 1,
                    "action": {
                        "en": "Clip leaf tips of seedlings before transplanting and pass a sweep net across the field to catch and destroy adult beetles.",
                        "te": "నాట్లకు ముందు నారు కొనలను తుంచి నాశనం చేయండి మరియు చేనులో వల తిప్పి తల్లి పురుగులను పట్టి నాశనం చేయండి.",
                        "hi": "रोपाई से पहले पौध की ऊपरी पत्तियां काट दें और खेत में जाल घुमाकर वयस्क कीड़ों को पकड़कर नष्ट करें।"
                    },
                    "source": "ANGRAU Crop Production Guide",
                    "source_url": "https://angrau.ac.in",
                    "source_quote": "Clipping infested leaf tips destroys grubs and sweep netting suppresses adult beetle populations effectively."
                },
                {
                    "step": 2,
                    "action": {
                        "en": "Spray Neem Seed Kernel Extract (NSKE 5%) or Azadirachtin 1500 ppm (3-5 ml/L) to deter adult feeding and egg-laying.",
                        "te": "తల్లి పురుగులు గుడ్లు పెట్టకుండా మరియు ఆకులను తినకుండా వేప గింజల కషాయం (NSKE 5%) లేదా వేపనూనె (3-5 మి.లీ/లీ) పిచికారీ చేయండి.",
                        "hi": "वयस्क कीड़ों को खाने और अंडे देने से रोकने हेतु नीम बीज का काढ़ा (NSKE 5%) या नीम का तेल (3-5 मिली/लीटर) छिड़कें।"
                    },
                    "source": "NIPHM Hyderabad",
                    "source_url": "https://niphm.gov.in",
                    "source_quote": "Neem formulations show strong anti-feedant and repellent action against Dicladispa armigera."
                }
            ],
            "chemical": [
                {
                    "step": 1,
                    "active_ingredient": "Chlorpyrifos 20% EC",
                    "product_example": "Dursban / Classic",
                    "dose_verified": False,
                    "dose": "follow the product label",
                    "phi_days": 30,
                    "safety_line": {
                        "en": "Wear mask and goggles. Avoid spraying during high heat or strong wind. Keep away from water bodies. Follow product label.",
                        "te": "మాస్క్ మరియు కళ్లద్దాలు ధరించండి. తీవ్రమైన ఎండ లేదా గాలి ఉన్నప్పుడు పిచికారీ చేయవద్దు. నీటి వనరులకు దూరంగా ఉంచండి. లేబుల్ పాటించండి.",
                        "hi": "मास्क और चश्मा पहनें। तेज धूप या तेज हवा में छिड़काव न करें। जल स्रोतों से दूर रखें। उत्पाद लेबल पढ़ें।"
                    },
                    "source": "CIB&RC Major Uses",
                    "source_url": "https://cibrc.nic.in",
                    "source_quote": "Chlorpyrifos 20% EC is registered by CIB&RC for rice hispa control in paddy."
                }
            ]
        },
        "regional": {
            "telangana": {
                "source": "PJTSAU",
                "differs": False
            },
            "andhra_pradesh": {
                "source": "ANGRAU",
                "differs": False
            },
            "other": {
                "source": "ICAR",
                "differs": False
            }
        },
        "prevention_and_field_practice": {
            "en": "Source: ANGRAU, ICAR-IIRR & NIPHM. Practice regular pest monitoring in nurseries and early planted crops. Clip and burn infested leaf tips before transplanting to destroy leaf-mining grubs. Observe economic threshold of 1-2 adults or 1 damaged leaf per hill before applying treatments. Promote biological control and protect natural predators such as reduviid bugs and damselflies. Spray botanical neem seed kernel extract (NSKE 5%) or azadirachtin at first sign. If infestation crosses threshold, apply registered chemical insecticide to foliage.",
            "te": "మూలం: ANGRAU, ICAR-IIRR & NIPHM. నారుమళ్లు మరియు పిలక దశలో పురుగు పర్యవేక్షణ (మోనిటరింగ్) చేపట్టండి. నాట్లు వేసే సమయంలో ఆకుల కొనలను తుంచి కాల్చివేయడం ద్వారా లోపల ఉన్న పురుగులను నాశనం చేయండి. ఆర్థిక నష్ట పరిమితి (దుబ్బుకు 1-2 తల్లి పురుగులు లేదా 1 దెబ్బతిన్న ఆకు) ఆధారంగా చర్యలు తీసుకోండి. సహజ శత్రువులైన సూది తుమ్మెదలను కాపాడే జీవ నియంత్రణ పద్ధతులను ప్రోత్సహించండి. వేప గింజల కషాయం (NSKE 5%) పిచికారీ చేయండి. తీవ్రత పరిమితి మించితే మాత్రమే సిఫార్సు చేసిన రసాయన మందులను వాడండి.",
            "hi": "स्रोत: ANGRAU, ICAR-IIRR और NIPHM. नर्सरी और रोपाई के शुरुआती दिनों में नियमित कीट निगरानी (मॉनिटरिंग) करें। रोपाई से पहले ग्रसित पत्तियों के सिरों को काटकर नष्ट करें ताकि अंदर छिपी इल्लियां खत्म हों। आर्थिक सीमा (1-2 वयस्क प्रति पौधा या 1 क्षतिग्रस्त पत्ती) की जांच करें। प्राकृतिक मित्र कीटों का संरक्षण करते हुए जैविक नियंत्रण अपनाएं। नीम का काढ़ा (NSKE 5%) छिड़कें। यदि कीट सीमा पार करे, तो ही अनुमोदित रासायनिक कीटनाशक का छिड़काव करें।"
        },
        "early_warning_signs": {
            "en": "Look for small spiny black-blue beetles on leaves and characteristic whitish parallel streaks along leaf veins. White papery patches where leaf tissue has been mined, starting from leaf tips.",
            "te": "ఆకులపై నలుపు-నీలం రంగులో ముళ్ళు కలిగిన చిన్న పురుగులు మరియు ఈనెల వెంట తెల్లటి సమాంతర చారలు గమనించండి. ఆకు కొనల వద్ద కాగితంలా మారిన తెల్లటి పొక్కులను పర్యవేక్షించండి.",
            "hi": "पत्तियों पर नीले-काले रंग के कांटेदार छोटे कीड़े और शिराओं के समानांतर सफेद धारियां देखें। पत्तियों के सिरों पर सफेद झिल्लीदार धब्बे दिखाई देते हैं।"
        },
        "when_to_call_extension_officer": {
            "en": "Call extension officer if leaf scorching spreads across more than 20% of the field or adults exceed 2 per hill. Call Kisan Call Centre (Toll-Free 1800-180-1551).",
            "te": "పొలంలో 20% పైగా ఆకులు తెల్లబడి ఎండిపోయినా లేదా దుబ్బుకు 2 కంటే ఎక్కువ తల్లి పురుగులు ఉంటే పంట రక్షణకు వెంటనే వ్యవసాయ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.",
            "hi": "यदि खेत में 20% से अधिक पत्तियां सूखने लगें या प्रति पौधा 2 से अधिक कीड़े दिखें, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर कॉल करें।"
        }
    },
    {
        "class_id": "tomato_septoria_leaf_spot",
        "index": 23,
        "crop": "tomato",
        "type": "disease",
        "pathogen": "Septoria lycopersici",
        "stage": None,
        "source": "ICAR-IIHR Bengaluru Tomato Disease Management & TNAU",
        "source_url": "https://iihr.res.in",
        "date_read": "2026-11-22",
        "names": {
            "en": "Tomato Septoria Leaf Spot",
            "te": "టమాటా సెప్టోరియా ఆకుమచ్చ తెగులు",
            "hi": "टमाटर का सेप्टोरिया पत्ती धब्बा रोग"
        },
        "needs_expert_review": True,
        "referral_threshold": "SEVERE",
        "severity_advice": {
            "MILD": {
                "en": "A few spots restricted to lowest crown leaves. Prune infected lower leaves and mulch soil surface.",
                "te": "కింది ఆకులపై మాత్రమే కొన్ని మచ్చలు. ఆ తెగులు సోకిన కింది ఆకులను తుంచి నాశనం చేయండి మరియు నేలపై రక్షక కవచం (మల్చింగ్) వేయండి.",
                "hi": "निचली पत्तियों पर कुछ सीमित धब्बे। ग्रसित निचली पत्तियों को तोड़कर नष्ट करें और जमीन पर मल्चिंग करें।"
            },
            "MODERATE": {
                "en": "Spots spreading to middle foliage with some yellowing and leaf drop. Apply bio-fungicide or registered protective spray.",
                "te": "మచ్చలు మధ్య ఆకులకు వ్యాపించి, ఆకులు పసుపు రంగులోకి మారి రాలుతున్నాయి. జీవ శిలీంధ్రనాశిని లేదా సిఫార్సు చేసిన రసాయన మందు చల్లండి.",
                "hi": "धब्बे बीच की पत्तियों तक फैल रहे हैं और पत्तियां पीली होकर गिर रही हैं। तुरंत अनुशंसित कवकनाशी का छिड़काव करें।"
            },
            "SEVERE": {
                "en": "Extensive defoliation exposing green fruits to severe sunscald. Consult KVK agronomist within 24 hours.",
                "te": "ఎక్కువ శాతం ఆకులు రాలిపోయి టమాటా కాయలు ఎండదెబ్బకు గురయ్యే ప్రమాదం ఉంది. 24 గంటల్లో వ్యవసాయాధికారిని సంప్రదించండి.",
                "hi": "अधिकतर पत्तियां झड़ चुकी हैं और फल धूप से झुलस रहे हैं। 24 घंटे के भीतर कृषि विशेषज्ञ से सलाह लें।"
            }
        },
        "immediate_step": {
            "en": "Remove and bury infected lower leaves touching the soil; avoid overhead sprinkler watering which splashes fungal spores.",
            "te": "నేలను తాకుతున్న తెగులు సోకిన కింది ఆకులను తుంచి భూమిలో పాతిపెట్టండి; ఆకులపై నీరు చిమ్మే పద్ధతిలో నీరు పెట్టవద్దు.",
            "hi": "जमीन को छूने वाली ग्रसित निचली पत्तियों को तोड़कर जमीन में गाड़ दें; फव्वारे से पानी न दें जिससे फफूंद के बीजाणु फैलते हैं।"
        },
        "explain_text": {
            "en": "A destructive fungal leaf spot disease of tomato caused by Septoria lycopersici. Circular water-soaked lesions appear on lower older leaves, turning brown with pale gray centers and tiny black fruiting bodies (pycnidia), causing severe defoliation and sunscald of fruits.",
            "te": "సెప్టోరియా లైకోపెర్సిసి అనే శిలీంధ్రం వల్ల కలిగే తీవ్రమైన ఆకుమచ్చ తెగులు. మొదట కింది ఆకులపై గుండ్రని చిన్న మచ్చలు ఏర్పడి, మధ్యలో బూడిద రంగు మరియు చుట్టూ ముదురు గోధుమ రంగు అంచులతో చిన్న నల్లటి చుక్కలు (పైక్నీడియా) కనిపిస్తాయి. ఆకులు రాలిపోయి కాయలు ఎండదెబ్బకు గురవుతాయి.",
            "hi": "सेप्टोरिया लाइकोपरसिकी नामक फफूंद से होने वाला टमाटर का प्रमुख रोग। निचली पुरानी पत्तियों पर छोटे गोल धब्बे बनते हैं, जिनका केंद्र राख जैसा धूसर और किनारे गहरे भूरे होते हैं तथा बीच में काले दाने (पाइक्नीडिया) दिखते हैं, जिससे पत्तियां झड़ जाती हैं।"
        },
        "treatment": {
            "natural": [
                {
                    "step": 1,
                    "action": {
                        "en": "Prune and destroy infected lower leaves and apply straw or plastic mulch around the plant base to prevent rain splash.",
                        "te": "తెగులు సోకిన కింది ఆకులను తుంచి నాశనం చేయండి మరియు వర్షపు తుంపర్ల ద్వారా తెగులు వ్యాపించకుండా మొదళ్ల వద్ద మల్చింగ్ వేయండి.",
                        "hi": "ग्रसित निचली पत्तियों को काटकर नष्ट करें और मिट्टी से फफूंद के छींटे रोकने हेतु पुआल की मल्चिंग करें।"
                    },
                    "source": "ICAR-IIHR Bengaluru",
                    "source_url": "https://iihr.res.in",
                    "source_quote": "Lower canopy sanitation and mulching significantly reduce primary inoculum of Septoria lycopersici."
                },
                {
                    "step": 2,
                    "action": {
                        "en": "Foliar spray of Trichoderma harzianum bio-agent (5 g/L) or copper-based botanical spray at first appearance of spots.",
                        "te": "మచ్చలు కనిపించిన వెంటనే ట్రైకోడెర్మా హార్జియానమ్ జీవ శిలీంధ్రనాశిని (5 గ్రా/లీ) ఆకులపై పిచికారీ చేయండి.",
                        "hi": "धब्बे दिखने पर ट्राइकोडर्मा हरजिएनम (5 ग्राम/लीटर) का पत्तियों पर छिड़काव करें।"
                    },
                    "source": "TNAU Agritech Portal",
                    "source_url": "https://agritech.tnau.ac.in",
                    "source_quote": "Trichoderma harzianum spray provides preventative suppression of Septoria and early foliar spots in tomato."
                }
            ],
            "chemical": [
                {
                    "step": 1,
                    "active_ingredient": "Mancozeb 75% WP",
                    "product_example": "Dithane M-45 / Indofil M-45",
                    "dose_verified": False,
                    "dose": "follow the product label",
                    "phi_days": 15,
                    "safety_line": {
                        "en": "Wear protective face mask and gloves. Observe pre-harvest interval of 15 days. Follow product label.",
                        "te": "రక్షణ మాస్క్ మరియు చేతి తొడుగులు ధరించండి. కోతకు 15 రోజుల ముందు మందు చల్లడం ఆపండి. లేబుల్ పాటించండి.",
                        "hi": "सुरक्षात्मक मास्क और दस्ताने पहनें। तुड़ाई से 15 दिन पहले छिड़काव बंद करें। उत्पाद लेबल पढ़ें।"
                    },
                    "source": "CIB&RC Major Uses",
                    "source_url": "https://cibrc.nic.in",
                    "source_quote": "Mancozeb 75% WP is registered by CIB&RC for leaf spots in tomato."
                }
            ]
        },
        "regional": {
            "telangana": {
                "source": "PJTSAU",
                "differs": False
            },
            "andhra_pradesh": {
                "source": "ANGRAU",
                "differs": False
            },
            "other": {
                "source": "ICAR",
                "differs": False
            }
        },
        "prevention_and_field_practice": {
            "en": "Source: ICAR-IIHR Bengaluru & TNAU Agritech Portal. Practice 2 to 3 year crop rotation away from solanaceous crops (brinjal, chilli, potato). Stake tomato plants and prune bottom foliage up to 30 cm from ground to enhance air circulation. Use drip irrigation instead of furrow or overhead irrigation. Apply organic straw mulch around plant bases to prevent soil-splashing spores onto lower leaves. Apply Trichoderma harzianum bio-fungicide early.",
            "te": "మూలం: ICAR-IIHR Bengaluru & TNAU Agritech Portal. టమాటా, వంకాయ, మిరప వంటి సోలనేసి పంటలు కాకుండా 2-3 ఏళ్ళు ఇతర పంటలతో పంట మార్పిడి చేయండి. మొక్కలకు కర్రల మద్దతు (స్టేకింగ్) ఇచ్చి, నేల నుండి 30 సెం.మీ ఎత్తు వరకు కింది ఆకులను తొలగించి గాలి ప్రసరణ పెంచండి. బిందు సేద్యం (డ్రిప్) వాడండి. నేల నుండి శిలీంధ్రం ఆకులపై పడకుండా మొక్క మొదళ్ల వద్ద ఎండుగడ్డితో మల్చింగ్ చేయండి. ట్రైకోడెర్మా హార్జియానమ్ వాడండి.",
            "hi": "स्रोत: ICAR-IIHR बेंगलुरु और TNAU एग्रीटेक पोर्टल। बैंगन, मिर्च, आलू जैसे सोलेनेसी कुल की फसलों के साथ 2-3 साल का फसल चक्र अपनाएं। पौधों को डंडों से सहारा दें (स्टेकिंग) और जमीन से 30 सेमी तक निचली पत्तियों को हटाकर हवा का आवागमन बढ़ाएं। ड्रिप सिंचाई का प्रयोग करें। पत्तियों पर मिट्टी के छींटे रोकने के लिए पुआल की मल्चिंग करें। शुरुआत में ट्राइकोडर्मा का प्रयोग करें।"
        },
        "early_warning_signs": {
            "en": "Watch for numerous tiny circular spots (1.5-3 mm) on the lowest leaves near the ground after wet, humid spells. Lesions have dark brown margins with grayish-white centers containing pinprick-sized black specks.",
            "te": "వర్షాలు లేదా అధిక తేమ ఉన్నప్పుడు భూమికి దగ్గరగా ఉండే కింది ఆకులపై 1.5-3 మి.మీ పరిమాణంలో చిన్న గుండ్రని మచ్చలు, మధ్యలో బూడిద రంగు మరియు సూది మొనల వంటి నల్లని చుక్కలను గమనించండి.",
            "hi": "जमीन के पास वाली निचली पत्तियों पर 1.5 से 3 मिमी के छोटे गोल धब्बे देखें, जिनके केंद्र में भूरापन और सुई की नोक जैसे काले दाने दिखते हैं।"
        },
        "when_to_call_extension_officer": {
            "en": "Call extension officer if leaf spotting and defoliation rise into the upper third of the canopy during flowering or fruit set. Call Kisan Call Centre (Toll-Free 1800-180-1551).",
            "te": "పూత మరియు కాయల దశలో ఆకుమచ్చలు పైభాగపు ఆకులకు వ్యాపించి తీవ్రంగా ఆకులు రాలుతుంటే వెంటనే వ్యవసాయ అధికారిని సంప్రదించండి. కిసాన్ కాల్ సెంటర్ (1800-180-1551) కు కాల్ చేయండి.",
            "hi": "यदि फूल और फल आते समय पत्ती धब्बा रोग ऊपरी पत्तियों तक फैल जाए और भारी पत्ती झड़ाव हो, तो तुरंत कृषि अधिकारी से संपर्क करें। किसान कॉल सेंटर (1800-180-1551) पर कॉल करें।"
        }
    }
]

def main():
    print("Loading existing KB...")
    with open(KB_DATA_PATH, "r", encoding="utf-8") as f:
        kb_data = json.load(f)

    classes = kb_data.get("classes", [])
    print(f"Current class count: {len(classes)}")

    # 1. Update top sources on existing classes
    existing_map = {c["class_id"]: c for c in classes}
    for cid, s_info in TOP_SOURCES.items():
        if cid in existing_map:
            existing_map[cid]["source"] = s_info["source"]
            existing_map[cid]["source_url"] = s_info["source_url"]
            existing_map[cid]["date_read"] = s_info["date_read"]

    # 2. Append new classes if not already present
    for entry in NEW_ENTRIES:
        cid = entry["class_id"]
        if cid in existing_map:
            print(f"Updating existing {cid}...")
            # update in-place
            idx = existing_map[cid]["index"]
            entry["index"] = idx
            existing_map[cid].update(entry)
        else:
            print(f"Adding new {cid} at index {len(classes)}...")
            entry["index"] = len(classes)
            classes.append(entry)
            existing_map[cid] = entry

    kb_data["classes"] = classes
    kb_data["total_classes"] = len(classes)
    print(f"New total class count: {len(classes)}")

    # Save to data/kb/knowledge_base.json
    print(f"Writing to {KB_DATA_PATH}...")
    with open(KB_DATA_PATH, "w", encoding="utf-8") as f:
        json.dump(kb_data, f, indent=2, ensure_ascii=False)

    # Sync to app/src/main/assets/kb/knowledge_base.json
    print(f"Syncing to {KB_ASSET_PATH}...")
    shutil.copy2(KB_DATA_PATH, KB_ASSET_PATH)

    print("Done updating and syncing KB!")

if __name__ == "__main__":
    main()
