# Task AUTO: Wrong-Crop Error Comparison Table

**Date:** 23 November 2026  
**Thresholds:** `energy_threshold=-8.0`, `auto_crop_mass_threshold=0.6`, `auto_margin_threshold=0.25`  

| Probe Set | Total Photos | Picker Wrong-Crop Errors | Auto Detection Wrong-Crop Errors | Picker Diagnosed | Auto Diagnosed |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **supported** | 107 | 0 | 1 | 100 | 101 |
| **unsupported** | 93 | 0 | 19 | 17 | 37 |
| **othercrop** | 48 | 30 | 31 | 30 | 31 |
| **nocrop** | 30 | 2 | 2 | 2 | 2 |
| **screen** | 20 | 0 | 2 | 11 | 13 |
| **TOTAL** | **298** | **32** | **55** | **160** | **184** |

### Key Findings & Analysis:
1. **Supported Crops (107 photos):**
   - Picker Wrong-Crop: **0** (0.0%).
   - Auto Wrong-Crop: **1** (diagnosed as a different crop).
2. **Unsupported Diseases / Off-Target Crop Problems (93 photos):**
   - Picker Wrong-Crop: **0** (0.0% - model is strictly constrained to the selected crop; rejected or diagnosed in-crop).
   - Auto Wrong-Crop: **19** (high confusion across crops because visual symptoms of unmodeled diseases resemble other crops' diseases).
3. **Overall Plant Leaf Probe Sets (supported + unsupported + screen = 220 photos):**
   - Picker Wrong-Crop Errors: **0 / 220 (0.0%)**.
   - Auto Wrong-Crop Errors: **22 / 220 (10.0%)**.
4. **Recommendation (per Requirement 4):**
   - Auto detection causes **significantly more wrong-crop errors** (55 total vs 32 total; and 22 vs 0 on crop leaves).
   - Therefore, **it is strongly recommended to keep the Crop Picker as the primary/default flow** (or default to Auto with immediate 1-tap crop override as provided by the 'Change' chip).
