# Rules Engine Decision Matrix: Urgency from Field Spread

*Requirement F4, F11 / Task G3 / Rule H5 (Safety decisions in plain, unit-tested code).*

---

## 1. Overview
In accordance with Rule H5, all diagnostic safety and triage decisions (severity levels, field spread assessment, urgency classification, and extension worker referral) live strictly in plain, deterministic Kotlin code within `RulesEngine`. 

The urgency level is computed by combining:
1. **Leaf-Damage Severity Band** (derived from calibrated vision lesion area fraction or specific pathogen thresholds):
   - **MILD** (<10% leaf damage or early symptoms)
   - **MODERATE** (10%–30% leaf damage)
   - **SEVERE** (>30% leaf damage or systemic viral infection)
2. **Farmer Field Spread Assessment** (answered via a single one-tap question: *"How much of the field is affected?"*):
   - **FEW_PLANTS** (isolated or scattered plants)
   - **SOME** (patches or roughly up to half the field)
   - **MOST** (widespread across most or all of the field)

---

## 2. Complete Decision Table

| Leaf Damage Severity | Field Spread Answer | Urgency Level | Recommended Farmer Action | Extension Referral Priority |
| :--- | :--- | :--- | :--- | :--- |
| **MILD** | **FEW_PLANTS** | **EARLY_WARNING** | Monitor closely; field sanitation; watch for symptom spread in next 2–3 days. | Normal |
| **MILD** | **SOME** | **ACT_THIS_WEEK** | Plan natural/cultural interventions within 3–5 days to prevent spread. | Normal |
| **MILD** | **MOST** | **ACT_NOW** | Broad spread detected; take immediate protective action to halt epidemic. | High |
| **MODERATE** | **FEW_PLANTS** | **ACT_THIS_WEEK** | Contain isolated patches within 3–5 days before disease enters exponential phase. | Normal |
| **MODERATE** | **SOME** | **ACT_NOW** | High economic risk; apply recommended treatment immediately. | High |
| **MODERATE** | **MOST** | **ACT_NOW** | Critical economic threshold exceeded; immediate intervention needed. | High |
| **SEVERE** | **FEW_PLANTS** | **ACT_NOW** | Severe pathogen pressure; rogue out infected plants; spray perimeter immediately. | Highest (Extension button first) |
| **SEVERE** | **SOME** | **ACT_NOW** | High crop loss risk; emergency action and extension officer consultation. | Highest (Extension button first) |
| **SEVERE** | **MOST** | **ACT_NOW** | Catastrophic crop loss risk; emergency extension referral and containment. | Highest (Extension button first) |

---

## 3. Special Conditions & Edge Cases

1. **Uncertainty Gate (`CertaintyBand.NOT_SURE`):**
   - If certainty is `NOT_SURE` (e.g. out-of-distribution leaf, low confidence $< 0.60$, or rejected photo), the field spread question is **skipped entirely**.
   - Result screen shows `"Can't tell – ask an extension officer"`, no severity band, and routes to extension worker.
2. **Healthy Crops (`healthy` type):**
   - If the crop is healthy, field spread question is omitted.
   - Urgency is set to **EARLY_WARNING** (preventive maintenance), chemical treatments are omitted, and good agronomic practices are shown.
3. **UI Positioning by Urgency:**
   - **EARLY_WARNING:** Displays the *"What to watch for in the next days"* guidance panel.
   - **ACT_NOW:** Prioritizes and displays the **Extension Officer / Kisan Call Centre** call button at the top of the action section.
