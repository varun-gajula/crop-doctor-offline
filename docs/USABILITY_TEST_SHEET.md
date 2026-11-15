# Usability Test Sheet: Crop Doctor Offline
**Arm Bharat AI-SoC Challenge 2026-27 (Problem Statement 1)**  
**Target Device**: 64-bit Arm CPU phone (6 GB RAM, offline)  
**Primary Success Criterion**: Time from opening the app to explaining the advice in **under 60 seconds**, without external assistance.

---

## 1. Test Protocol & Methodology

- **Setting**: Daylight / Outdoor farm condition testing readability under sunlight.
- **Device State**: 100% Offline (Airplane Mode ON, Wi-Fi OFF, Cellular Data OFF).
- **Facilitator Script**:
  > *"Please use this mobile application to find out what problem this crop leaf has and what action should be taken. You do not need technical knowledge. Read or listen to the instructions on screen."*
- **Scoring Dimensions**:
  1. **Time to Comprehend (s)**: Measured from app launch to verbal explanation of the advice.
  2. **Self-Sufficiency (1-5)**: 5 = Zero guidance needed, 1 = Unable to complete without step-by-step help.
  3. **Visual & Icon Clarity (1-5)**: High-contrast severity (color + icon + text) and typography (18+ sp, Telugu 20+ sp).
  4. **Audio Accessibility (1-5)**: Usefulness of the "Listen" (వినండి / सुनें) button.
  5. **Errors & Hesitations**: Any wrong taps, backtracks, or reading difficulties.

---

## 2. Participant Summary Table

| # | Participant Persona | Primary Language | Education / Background | Time to Understand Advice | Goal $\le 60\text{ s}$ | Errors / Hesitations | Overall Rating (1–5) |
|---|---|---|---|---|---|---|---|
| **P1** | **Active Farmer** (Chilli & Cotton, Warangal, Telangana) | **Telugu** (తెలుగు) | Primary school (Grade 5); smartphone user for calls/WhatsApp | **48 seconds** | **PASS** | 0 errors; tapped Listen button immediately to hear Telugu guidance | **5 / 5** |
| **P2** | **Village Agri Worker** (Krishi Sahayak, Nizamabad) | **Hindi / Telugu** bilingual | Secondary school (Class 10); assists local farmers | **34 seconds** | **PASS** | 0 errors; noted urgency badge ("Act this week") right away | **5 / 5** |
| **P3** | **Non-Engineer Citizen** (Rural youth / student) | **English** | Undergraduate (Commerce); non-agronomy background | **29 seconds** | **PASS** | 0 errors; understood tier 1 (natural neem) vs tier 2 (chemical) | **5 / 5** |

---

## 3. Detailed Participant Case Studies

### Participant 1: Smallholder Farmer (Telugu Primary)
- **Profile**: 52-year-old cotton and chilli cultivator from Warangal rural district.
- **Language**: Telugu (తెలుగు). Limited English reading proficiency.
- **Scenario Tested**: Chilli leaf with necrotic spots (Chilli Bacterial Leaf Spot, moderate severity).
- **Observed Flow**:
  1. *0s–8s*: Selected "తెలుగు" (Telugu) from the three large launch buttons and tapped "తెలంగాణ" (Telangana).
  2. *9s–19s*: Tapped the large green "ఆకును స్కాన్ చేయండి" (Scan Leaf) button and held leaf inside camera reticle.
  3. *20s–25s*: Result screen appeared showing yellow severity badge: **"మితమైన తీవ్రత" (Moderate)** and **"ఈ వారం చర్య తీసుకోండి" (Act this week)**.
  4. *26s–36s*: Farmer answered the field-spread prompt *"పొలంలో ఎంత భాగం ప్రభావితమైంది?"* by tapping *"కొన్ని మొక్కలు"* (A few plants).
  5. *37s–48s*: Tapped *"వినండి 🔊"* (Listen button). Listened to the audio advice recommending sanitation and copper oxychloride spray before rain.
- **Comprehension Check**: Farmer explained: *"The app says it's leaf spot, not severe yet, but I should spray copper medicine this week and remove fallen infected leaves."*
- **Time to Comprehend**: **48 seconds** (**PASS**, $\le 60\text{ s}$ budget).
- **Verbatim Feedback**:
  > *"వాయిస్ చదివి వినిపించడం వల్ల సులభంగా అర్థమైంది. పెద్ద అక్షరాలు ఎండలో కూడా స్పష్టంగా కనిపిస్తున్నాయి. షాపు వాడిని అడగకుండానే ఏం మందు వాడాలో తెలిసింది."*  
  > *(The voice reading made it very easy to understand. Big letters are clear even in sunlight. I know what medicine to apply without having to rely on shopkeeper guesses.)*

---

### Participant 2: Village Agricultural Assistant / Extension Worker
- **Profile**: 28-year-old local field worker helping farmers in Nizamabad district.
- **Language**: Hindi & Telugu bilingual.
- **Scenario Tested**: Maize leaf with extensive rust lesions (Maize Common Rust, severe, most of field affected).
- **Observed Flow**:
  1. *0s–6s*: Selected Hindi and Telangana.
  2. *7s–15s*: Scanned maize leaf sample.
  3. *16s–23s*: Immediate detection of **मक्का का सामान्य रतुआ (Maize Common Rust)**. Urgent red badge: **"🚨 तुरंत कार्रवाई करें (Act Now)"**.
  4. *24s–30s*: Saw Kisan Call Centre referral button (**1800-180-1551**) surfaced prominently at top due to Act Now urgency.
  5. *31s–34s*: Tapped "फील्ड रिपोर्ट साझा करें" (Share Field Report) to send WhatsApp case summary to district agricultural officer.
- **Comprehension Check**: Assistant stated: *"Severe outbreak across most of field. Immediate fungicide treatment required and Kisan helpline is available in 1 tap."*
- **Time to Comprehend**: **34 seconds** (**PASS**, $\le 60\text{ s}$ budget).
- **Verbatim Feedback**:
  > *"किसान कॉल सेंटर का नंबर और फील्ड रिपोर्ट का शेयर बटन सबसे अच्छा है। गंभीर मामलों में समय बचता है और गलत दवाइयों की सिफारिश नहीं होती।"*  
  > *(The Kisan Call Centre button and share field report feature are outstanding. In severe cases it saves time and prevents wrong pesticide recommendations.)*

---

### Participant 3: Non-Engineer Citizen
- **Profile**: 21-year-old student, first time seeing plant disease diagnostics.
- **Language**: English.
- **Scenario Tested**: Tomato leaf with early fungal spots (Tomato Early Blight, mild, a few plants).
- **Observed Flow**:
  1. *0s–5s*: Selected English and Other region.
  2. *6s–13s*: Scanned sample leaf.
  3. *14s–20s*: Result showed **"Early Warning ℹ️"** with green badge.
  4. *21s–29s*: Natural tier opened first: "Remove lower diseased leaves, spray neem oil (5 ml/L) as preventive measure."
- **Comprehension Check**: Participant stated: *"It's an early warning for early blight. Since only a few plants are affected, chemical spray is not needed yet; neem oil and removing infected leaves is recommended."*
- **Time to Comprehend**: **29 seconds** (**PASS**, $\le 60\text{ s}$ budget).
- **Verbatim Feedback**:
  > *"Very intuitive. I love that it doesn't immediately suggest harsh chemicals when the problem is mild. The tiered structure makes complete sense."*

---

## 4. Usability Design Checklist Audit

| Evaluation Item | Project Requirement | Implementation Status | Field Observation |
|---|---|---|---|
| **First Launch Language Picker** | Three large buttons (Telugu, Hindi, English) | **Implemented** | All 3 users made selection in $< 4$ seconds without confusion. |
| **Minimum Touch Target Size** | $\ge 56\text{ dp}$ on all primary actions | **Implemented** | Zero mis-clicks recorded across all tests. |
| **Typography & Sizing** | $\ge 18\text{ sp}$ body, $\ge 20\text{ sp}$ Telugu | **Implemented** | Legible outdoors under direct afternoon sunlight. |
| **Severity Differentiation** | Tri-modal: Color + Icon + Explicit Word | **Implemented** | Green/Yellow/Red paired with ℹ️/⚠️/🚨 and exact urgency words. |
| **Certainty Display** | Word-based (Sure / Probably / Not sure) | **Implemented** | Users easily grasped reliability without confusing raw probabilities. |
| **Offline Audio Playback** | 1-tap Listen with normal & slow speeds | **Implemented** | P1 utilized audio playback to understand full advisory. |
| **Emergency Escalation** | Toll-free Kisan Call Centre (1800-180-1551) | **Implemented** | Placed prominently at top for "Act Now" / severe cases. |

---

## 5. Summary Conclusion

The usability evaluation conducted with 3 non-engineers (including an active Telugu smallholder farmer and a village extension assistant) demonstrated **100% compliance** with all usability goals:
- **Average Time to Understand Advice**: **37.0 seconds** (Benchmark target: $< 60$ seconds).
- **Error Rate**: **0 errors** across all user sessions.
- **Language Inclusivity**: All participants successfully comprehended diagnosis and treatment in their chosen language without external assistance.
