# -*- coding: utf-8 -*-
"""
Adds official regional advisory blocks to data/kb/knowledge_base.json and syncs to app assets.
Follows Task G2 / Requirement F6 / Rule H4 & R11.
"""

import json
import shutil
import os

SPECIFIC_REGIONAL = {
    "chilli_leaf_curl_virus": {
        "telangana": {
            "source": "PJTSAU",
            "differs": True,
            "text": {
                "en": "PJTSAU advisory for Telangana: Install 30-40 blue sticky traps per acre. Spray neem oil (10,000 ppm) @ 1 ml/L or NSKE 5% at flowering to check vector build-up in Telangana dry-zone tracts.",
                "te": "తెలంగాణ కోసం పిజెటిఎస్‌ఎయు (PJTSAU) సలహా: ఎకరానికి 30-40 నీలి రంగు జిగురు అట్టలను అమర్చండి. పూత దశలో 10,000 ppm వేపనూనె 1 మి.లీ/లీ లేదా 5% వేప గింజల కషాయం పిచికారీ చేసి తామర పురుగులను నివారించండి.",
                "hi": "तेलंगाना के लिए PJTSAU की सलाह: प्रति एकड़ 30-40 नीले चिपचिपे ट्रैप लगाएं। फूल आने पर 10,000 ppm नीम का तेल 1 मिली/लीटर या 5% नीम बीज का अर्क छिड़कें।"
            }
        },
        "andhra_pradesh": {
            "source": "ANGRAU",
            "differs": True,
            "text": {
                "en": "ANGRAU advisory for Andhra Pradesh: Grow 2-3 rows of barrier crops (maize or jowar) around chilli fields in coastal and Rayalaseema districts to prevent viruliferous vector migration.",
                "te": "ఆంధ్రప్రదేశ్ కోసం ఆంగ్రా (ANGRAU) సలహా: కోస్తా మరియు రాయలసీమ జిల్లాలలో రసం పీల్చే పురుగుల వ్యాప్తిని అరికట్టడానికి మిరప చేను చుట్టూ 2-3 వరుసల జొన్న లేదా మొక్కజొన్నను సరిహద్దు పంటగా వేయండి.",
                "hi": "आंध्र प्रदेश के लिए ANGRAU की सलाह: रस चूसक कीटों के प्रसार को रोकने के लिए मिर्च के खेत के चारों ओर 2-3 पंक्तियों में मक्का या ज्वार की अवरोधक फसल लगाएं।"
            }
        },
        "other": {
            "source": "ICAR",
            "differs": False
        }
    },
    "maize_fall_armyworm": {
        "telangana": {
            "source": "PJTSAU",
            "differs": True,
            "text": {
                "en": "PJTSAU Telangana advisory: Erect 4 pheromone traps per acre at 10-15 days after germination for monitoring. In Kharif/Rabi rainfed areas, apply sand and lime mixture (9:1) or whorl application of fine soil.",
                "te": "తెలంగాణ కోసం పిజెటిఎస్‌ఎయు (PJTSAU) సలహా: మొలకెత్తిన 10-15 రోజులలో ఎకరానికి 4 లింగాకర్షక బుట్టలను అమర్చండి. సుడిలో ఇసుక మరియు సున్నం మిశ్రమం (9:1) లేదా మెత్తటి మట్టిని వేయండి.",
                "hi": "तेलंगाना के लिए PJTSAU की सलाह: अंकुरण के 10-15 दिन बाद प्रति एकड़ 4 फेरोमोन ट्रैप लगाएं। तने के भंवर में रेत और चूने का मिश्रण (9:1) डालें।"
            }
        },
        "andhra_pradesh": {
            "source": "ANGRAU",
            "differs": True,
            "text": {
                "en": "ANGRAU Andhra Pradesh advisory: Apply neem cake @ 100 kg/acre at last ploughing. Intercrop maize with cowpea or redgram (2:1 ratio) in rainfed tracts of Rayalaseema to encourage natural predators.",
                "te": "ఆంధ్రప్రదేశ్ కోసం ఆంగ్రా (ANGRAU) సలహా: చివరి దుక్కిలో ఎకరానికి 100 కిలోల వేప పిండిని వేయండి. సహజ శత్రువుల వృద్ధికి మొక్కజొన్నతో పాటు అలసంద లేదా కందిని అంతరపంటగా (2:1 నిష్పత్తిలో) వేయండి.",
                "hi": "आंध्र प्रदेश के लिए ANGRAU की सलाह: अंतिम जुताई के समय 100 किग्रा/एकड़ नीम की खली डालें। प्राकृतिक परभक्षियों को बढ़ावा देने के लिए मक्का के साथ लोबिया या अरहर की अंतर-फसल लगाएं।"
            }
        },
        "other": {
            "source": "ICAR",
            "differs": False
        }
    }
}

def main():
    kb_path = "data/kb/knowledge_base.json"
    asset_path = "app/src/main/assets/kb/knowledge_base.json"
    
    with open(kb_path, "r", encoding="utf-8") as f:
        kb_data = json.load(f)
        
    for entry in kb_data["classes"]:
        cid = entry["class_id"]
        if cid in SPECIFIC_REGIONAL:
            entry["regional"] = SPECIFIC_REGIONAL[cid]
        else:
            entry["regional"] = {
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
            }
            
    with open(kb_path, "w", encoding="utf-8") as f:
        json.dump(kb_data, f, ensure_ascii=False, indent=2)
        
    shutil.copyfile(kb_path, asset_path)
    print(f"Updated {len(kb_data['classes'])} classes in {kb_path} and synced to {asset_path}")

if __name__ == "__main__":
    main()
