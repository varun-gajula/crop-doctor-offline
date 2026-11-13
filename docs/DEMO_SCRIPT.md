# 60-Second Demo Script & Airplane Mode Checklist

**Competition:** Arm Bharat AI-SoC Challenge 2026-27  
**Problem Statement:** PS1 – Offline Crop Disease/Pest Detection and Advisory for Farmers  
**App:** Crop Doctor Offline (`com.cropdoctor`)  
**Target Device:** Any Android phone with 6 GB RAM and 64-bit Arm CPU (arm64-v8a)  
**Total Duration:** Exactly 60 seconds (Stopwatch calibrated)

---

## 1. Stopwatch-Calibrated 60-Second Demo Flow (R8)

```
[00:00 - 00:10] Step 1: Proof of Offline & Photograph Affected Crop
[00:10 - 00:22] Step 2: Local Vision Detection (Disease Label & Severity Band)
[00:22 - 00:34] Step 3: Switch Language Chip (Instant Offline Advisory in Telugu / Hindi)
[00:34 - 00:46] Step 4: Explain Uncertainty & Route to Extension Worker (1800-180-1551)
[00:46 - 01:00] Step 5: Save Case Report Locally & Share via Android Share Sheet
```

---

### Step 1: Photograph Affected Crop [00:00 – 00:10] (10s)
* **Action:**
  1. Pull down Android quick settings shade: show **Airplane Mode: ON** (Wi-Fi: OFF, Mobile Data: OFF).
  2. Open **Crop Doctor** from home screen (instant cold launch under 2s).
  3. Tap **"Scan Leaf"** (56dp touch target) on the Home screen.
  4. Point camera at an affected crop leaf (e.g. Rice with Bacterial Leaf Blight) inside the alignment frame.
  5. Press the large green shutter button.
* **Narration:**
  > *"Farmers need answers before connectivity arrives. With Airplane Mode on and zero internet permission, Crop Doctor launches instantly. We tap 'Scan Leaf' and photograph an affected crop."*

---

### Step 2: Local Vision Detection [00:10 – 00:22] (12s)
* **Action:**
  1. Result screen appears in under 1.5 seconds.
  2. Point to the **Certainty Card**: shows icon + color + word: **"Sure"** (>= 80% certainty).
  3. Point to the **Disease Finding**: **"Rice Bacterial Leaf Blight"** (pathogen: *Xanthomonas oryzae*).
  4. Point to the **Severity Card**: **"Moderate"** (10–30% lesion coverage) with early action alert: *"Immediate action required: Take corrective action now to prevent crop loss."*
  5. Show **Two-Tier Grounded Advisory**: Tier 1 Natural/Bio opened first, Tier 2 Chemical with approved active ingredients (*Copper Hydroxide*, *Streptocycline*) and safety PPE lines.
* **Narration:**
  > *"In under 1.5 seconds directly on the Arm CPU, our local vision model detects the problem: Rice Bacterial Leaf Blight. The rules engine calculates moderate severity and opens grounded, two-tier treatment steps verified by ICAR."*

---

### Step 3: Switch Language Offline [00:22 – 00:34] (12s)
* **Action:**
  1. Tap the persistent **Language Chip** (`[EN]`) in the top bar.
  2. Select **"తెలుగు" (Telugu)**.
  3. The entire diagnosis, severity, and treatment plan switch to Telugu instantly on-the-fly without reloading or restarting.
  4. Tap the **Speaker Icon** (`వినండి 🔊`) to play offline local Text-To-Speech for farmers who cannot read.
  5. Tap Language Chip again and switch to **"हिन्दी" (Hindi)** to show instant Devanagari parity.
* **Narration:**
  > *"With one tap on the language chip, the exact same advice switches to Telugu or Hindi offline. Even farmers who cannot read can tap the speaker button to hear the advice spoken aloud."*

---

### Step 4: Uncertainty & Extension Worker Routing [00:34 – 00:46] (12s)
* **Action:**
  1. Tap Back -> Tap **"Scan Leaf"** -> Capture an unrecognizable / blurry background object (e.g. carpet or non-leaf).
  2. The app triggers the uncertain path (Rule H11: Never invent diseases or guess when uncertain).
  3. Show the **Result Screen**:
     - Finding: **"Can't tell – no disease identified"** (strictly no guessed disease).
     - Certainty: **"⚠️ Not sure"** (< 60%).
     - Severity: Suppressed (zero false severity lines, no "Severity Guidance").
     - Red Emergency Action Button: **"📞 Kisan Call Centre (1800-180-1551)"**.
  4. Tap the Kisan Call Centre button to show it launches the system dialer with `tel:18001801551` ready to call.
* **Narration:**
  > *"Safety first: when confidence is low or the image is unclear, the app never guesses. It displays 'Not sure – no disease identified', hides false severity, and routes the farmer directly to the government Kisan Call Centre at 1800-180-1551."*

---

### Step 5: Save Case Report & Share Sheet [00:46 – 01:00] (14s)
* **Action:**
  1. On the Result screen, tap the prominent button: **"📝 Save case report"**.
  2. The **Save Case Report Screen** opens showing:
     - Captured leaf photo thumbnail
     - Crop, Finding, Certainty word, Severity, Date, and Region
  3. Type a brief farmer note in the text field (e.g. *"Field 2 north corner"*).
  4. Tap **"💾 Save Locally"** -> Button updates to **"✓ Saved Locally"** and saves to offline private storage.
  5. Tap **"📤 Share Report"** -> Native Android share sheet opens (`Intent.ACTION_SEND`), showing formatted report ready to send via WhatsApp, SMS, or Bluetooth with zero internet permissions.
  6. Tap Back -> Navigate to **"📁 Saved Case Reports"** to show the saved case stored locally with photo thumbnail and note.
* **Narration:**
  > *"Finally, we tap 'Save case report'. The farmer can add a field note and save it locally on the device. Tapping 'Share' opens the Android share sheet to send the full diagnosis to an agriculture officer over SMS or WhatsApp. 100% offline, zero internet permissions, built for Indian agriculture."*

---

## 2. Airplane Mode Checklist (Pre-Flight Verification)

Before recording the 60-second video or presenting to challenge judges, execute every check in this checklist:

| Check # | Requirement | Verification Method | Status |
| :--- | :--- | :--- | :--- |
| **C1** | **Zero Internet Permission** | Run `aapt2 dump permissions app-release.apk` (or debug). Verify `android.permission.INTERNET` is completely absent. | ✅ PROVEN |
| **C2** | **Full Airplane Mode Enabled** | Pull down quick settings: Airplane Mode **ON**; verify Wi-Fi icon is OFF, Mobile Data is OFF, Bluetooth is OFF. | ✅ MANDATORY |
| **C3** | **Cold Launch Latency** | Cold start the app from an app kill (`am force-stop com.cropdoctor`). Must display interactive UI in <= 2.0s on a 6 GB phone. | ✅ PROVEN (< 1.2s) |
| **C4** | **Language Switching On-Device** | Switch between English, Telugu, and Hindi. UI must render bundled Noto Sans fonts without external network calls. | ✅ PROVEN |
| **C5** | **Offline Audio (TTS)** | Tap "Listen 🔊". Offline Android TTS engine speaks local advice without requesting network audio streams. | ✅ PROVEN |
| **C6** | **Judge Benchmark Panel** | Long-press the "Result" title in top bar to reveal live on-device inference timer, memory (PSS), and 0 KB network proof. | ✅ PROVEN |
| **C7** | **Local Case Persistence** | Save a case report with note, close app completely, reopen -> open "Saved Case Reports". Verify report & photo thumbnail persist. | ✅ PROVEN |
| **C8** | **Zero Leakage / No Cloud APIs** | Verify no Firebase, analytics, external font CDN, or remote inference APIs are invoked during operation. | ✅ PROVEN |
