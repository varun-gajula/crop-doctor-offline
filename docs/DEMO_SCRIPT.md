# 60-Second Video Demo Script

**Competition:** Arm Bharat AI-SoC Challenge 2026-27  
**Problem Statement:** PS1 – Offline Crop Disease/Pest Detection and Advisory  
**App:** Crop Doctor Offline (`com.cropdoctor`)  
**Hardware:** Arm Cortex-based Android Device (Motorola Edge 60 / MediaTek Dimensity 7400)  
**Total Target Duration:** 58 seconds (Strictly under 60 seconds)

---

## Shot-by-Shot Storyboard

### Scene 1: Proof of 100% Offline (00:00 – 00:08)
- **Visual:**
  - Pull down Android notification shade on screen.
  - Show **Airplane Mode: ON** (Wi-Fi: OFF, Mobile Data: OFF).
  - Launch **Crop Doctor** from home screen.
- **Voiceover / Caption:**
  *"Crop Doctor operates 100% offline. No cellular data, no Wi-Fi. All AI models run locally on the Arm CPU."*
- **On-Screen Text:** `Airplane Mode ON • 100% On-Device AI`

---

### Scene 2: Language Selection & Scanning Leaf (00:08 – 00:20)
- **Visual:**
  - Tap **"🌾 తెలుగు (Telugu)"** on the high-contrast startup screen.
  - Camera viewfinder opens instantly.
  - Point camera at a diseased rice leaf (Rice Bacterial Leaf Blight).
  - Press large central shutter button.
- **Voiceover / Caption:**
  *"Farmers select their mother tongue with one tap. Capturing a leaf with bacterial blight..."*
- **On-Screen Text:** `Sub-1.5s Latency • MobileNetV3-Large on Arm CPU`

---

### Scene 3: Instant Classification & Severity Assessment (00:20 – 00:32)
- **Visual:**
  - Result appears in **< 1.5 seconds**.
  - Highlights:
    - Disease Name: **వరి బ్యాక్టీరియల్ ఆకు ఎండు తెగులు** (Rice Bacterial Leaf Blight)
    - Severity Badge: **⚠ మధ్యస్థం (Moderate)** (Amber alert badge)
    - Confidence: **ఖచ్చితంగా (Sure)**
    - Immediate Step: *"నత్రజని ఎరువుల వాడకాన్ని వెంటనే ఆపండి..."*
- **Voiceover / Caption:**
  *"In under 1.5 seconds, the vision lane identifies the disease, evaluates damage severity, and gives an immediate first step."*
- **On-Screen Text:** `Deterministic Agronomic Rules Engine`

---

### Scene 4: Two-Tier Grounded Advisory (00:32 – 00:44)
- **Visual:**
  - Screen displays two tabs: **"🌿 సహజ నివారణ" (Natural/Bio)** and **"🧪 రసాయన మందులు" (Chemical/Advanced)**.
  - For moderate/severe, Chemical tab highlights first showing CIB&RC approved active (*Copper Oxychloride + Streptocycline*) with strict safety warning in orange box.
  - Below, **On-Device Llama Advisory** streams reasoning live on the Arm CPU.
  - Shows credit: *"Built with Llama 3.2 1B • ExecuTorch (CPU)"*.
- **Voiceover / Caption:**
  *"Recommendations are tiered by severity. Chemical treatments feature approved CIB&RC actives with safety warnings, while Llama 3.2 1B streams personalized field advice."*
- **On-Screen Text:** `Llama 3.2 1B via ExecuTorch XNNPACK`

---

### Scene 5: Instant Multilingual Switch & KVK Escalation (00:44 – 00:58)
- **Visual:**
  - Tap language toggle at top right -> Select **"हिन्दी (Hindi)"**.
  - All diagnosis and advisory instantly switch to Hindi offline without reloading or latency.
  - Scroll down to show red emergency button: **"📞 कृषि विज्ञान केंद्र / विशेषज्ञ से संपर्क करें"** (Call KVK).
  - Tap *"Scan Another Plant"*.
- **Voiceover / Caption:**
  *"Switch between Telugu, Hindi, and English seamlessly offline. Severe outbreaks are routed directly to local KVK scientists. Real AI, protecting India's crops at the edge."*
- **On-Screen Text:** `Telugu • Hindi • English • Ready for Indian Agriculture`

---

## Recording Tips for the Team
1. **Lighting:** Record outdoors or in good natural lighting with a real leaf or high-resolution tablet display showing leaf symptoms.
2. **Screen Recorder:** Use Android 15 built-in screen recorder with device audio + microphone enabled.
3. **No Fluff:** Keep transitions snappy. Do not exceed 60 seconds.

