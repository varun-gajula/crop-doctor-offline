# Agri Care AI — Offline AI Models Setup Guide

> **Target Device**: Any Android phone with 6 GB RAM and an Arm 64-bit CPU (`arm64-v8a`), running Android 8.0+ (Android 10+ recommended).  
> **Privacy & Offline Commitment**: 100% offline. Zero network permissions (`INTERNET` permission is absent). No data or images are ever uploaded.

---

## 1. Overview: What Is Needed and Why

### The App Works Offline Without Any Extra Files
When you install the Agri Care AI APK, the following core features work immediately out of the box with **zero downloads or setup**:
- **Vision Disease & Pest Detection**: Local neural network (`vision.pte`, 14.25 MB) identifies 21 crop diseases and pests in < 1.5 seconds.
- **Urgency & Severity Triage**: Deterministic rules engine calculates lesion percentage and field spread (*Early warning / Act this week / Act now*).
- **Trilingual Sourced Advisory**: Official university-verified agronomic advice (PJTSAU, ANGRAU, ICAR) for natural and registered chemical treatments in **Telugu (తెలుగు)**, **Hindi (हिन्दी)**, and **English**.
- **Extension Officer Tools**: Emergency Kisan Call Centre referral (**1800-180-1551**) and structured offline **Case Report** generation.

### Why Are AI Model Files Installed Separately?
To enable on-device **generative AI explanations** (Llama 3.2 1B), **conversational offline chat**, and **voice question recognition** (Vosk), large AI weights are needed:
1. **Licensing Compliance**: Meta Llama 3.2 is distributed under the **Llama 3.2 Community License Agreement**. Reviewers and users accept the license directly from Meta / Hugging Face before obtaining weights.
2. **File Size Limits**: The 4-bit SpinQuant Llama model is **~1.13 GB**, which exceeds GitHub repository file limits (100 MB).
3. **User Choice**: Farmers and reviewers can choose whether to keep the app ultra-compact (~40 MB APK) or activate the on-device AI advisor.

---

## 2. Model Files, Checksums, and Official Sources

Before installing, obtain the required files. No API keys, credentials, or paid subscriptions are required.

| Model Component | Target Filename on Phone | Expected Size | Reference SHA-256 Checksum | Where to Get (Official Links) |
|---|---|---|---|---|
| **Llama 3.2 1B-Instruct** (SpinQuant INT4) | `llama-3.2-1b-spinquant.pte` | 1,135,951,488 bytes (~1.13 GB) | `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b` | 1. Accept license at [Meta Llama](https://llama.meta.com/) or [Hugging Face](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct)<br>2. Download from [ExecuTorch Community](https://huggingface.co/executorch-community/) |
| **Llama Tokenizer** | `tokenizer.model` | 2,183,982 bytes (~2.18 MB) | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` | [Meta Llama Hugging Face Tokenizer](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct/resolve/main/original/tokenizer.model) |
| **Vosk English Voice Model** | `vosk/en/` | 37,573,330 bytes (~35.8 MB zip) | `20663dcac4d5cb783a579c54d98339344a688e4ec6e1b4a4b059fd1235454cc7` | [AlphaCephei Indian English Model](https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip) |
| **Vosk Hindi Voice Model** | `vosk/hi/` | 44,458,845 bytes (~42.4 MB zip) | `7c50a10866889f0ac21d912c20537a055a597ed09fc1d3e5bcd798f9f0017e48` | [AlphaCephei Hindi Model](https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip) |
| **Vosk Telugu Voice Model** | `vosk/te/` | 60,544,249 bytes (~57.7 MB zip) | `3e77b05becc7599a596ccebd8b22cb95eb4eca0cfca524827cdfbfd66af09497` | [AlphaCephei Telugu Model](https://alphacephei.com/vosk/models/vosk-model-small-te-0.42.zip) |

---

## 3. Installation Method A: In-App File Picker (Recommended for Non-Technical Users)

This method requires **no computer terminal, no ADB commands, and no developer options**.

### Step A.1: Copy Files to the Phone's Downloads Folder
1. Connect your phone to your computer via USB cable. In the phone notification drawer, select **"File Transfer / Android Auto"** (MTP mode).
2. On your computer, open your phone's storage and copy the downloaded files into the phone's **`Download`** folder:
   - `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte`
   - `tokenizer.model`
   - (Optional) Speech zips: `vosk-model-small-en-in-0.4.zip`, `vosk-model-small-hi-0.22.zip`, `vosk-model-small-te-0.42.zip`

### Step A.2: Open the Model Install Screen in Agri Care AI
1. Launch **Agri Care AI** on the phone.
2. Tap the menu icon **☰ (More Options)** in the top-left corner of the Home screen.
3. Tap **"Install AI Model (.pte)"** from the menu.  
   *(Tip: You can also reach this screen from any diagnosis result screen by tapping "⚙️ Install AI Model (Offline)").*

### Step A.3: Select Model and Tokenizer Files
1. In the **Llama Model (.pte)** section, tap **"Select Model File (.pte)"**.
2. Android's file picker opens. Navigate to **Downloads** and tap `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte`.
3. The app safely copies the file into its private storage (`context.filesDir`) with a progress spinner. Once finished, a green checkmark will show: **"Installed (1083 MB)"**.
4. In the **Tokenizer (.model)** section, tap **"Select Tokenizer (.model)"**.
5. Select `tokenizer.model` from **Downloads**. Once copied, it shows: **"Installed (2132 KB)"**.
6. The top of the screen immediately updates to a green banner:  
   **"All AI Models Installed & Ready — Llama 3.2 1B & Tokenizer are active. No further installation required."**

### Step A.4: Install Offline Speech Models (Optional for Voice Chat)
1. On the same screen, scroll down to **"🎙️ Offline Speech Models (Vosk)"**.
2. Tap **"Install (.zip)"** next to your preferred language (English, Hindi, or Telugu).
3. Select the corresponding downloaded `.zip` file from **Downloads**.
4. The app automatically unpacks and verifies the model structure and size (> 15 MB) directly in private app storage.

---

## 4. Installation Method B: Direct ADB Push (For Developers & Lab Setup)

If you have USB debugging enabled and `adb` installed on your computer, you can push the files directly in seconds without opening file pickers.

### Option B.1: Using Bundled Automated Python Scripts
From the repository root on your computer:
```powershell
# 1. Verify license and push Llama 3.2 1B + Tokenizer
python scripts/setup/install_llama.py --model "<path-to-downloads>\Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte" --tokenizer "<path-to-downloads>\tokenizer.model" --accept-license

# 2. Download, verify, and push Vosk speech recognition models
python scripts/setup/download_vosk.py
```

### Option B.2: Manual Windows PowerShell / CMD Commands
Push the files directly to the app's external files directory (no root required; accessible on Android 10+):

```powershell
# Create target directories on the phone
adb shell mkdir -p /sdcard/Android/data/com.cropdoctor/files/vosk/en
adb shell mkdir -p /sdcard/Android/data/com.cropdoctor/files/vosk/hi
adb shell mkdir -p /sdcard/Android/data/com.cropdoctor/files/vosk/te

# Push Llama 3.2 1B model weights and tokenizer
adb push "Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte" /sdcard/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte
adb push "tokenizer.model" /sdcard/Android/data/com.cropdoctor/files/tokenizer.model

# (Optional) Push extracted Vosk speech directories
adb push "C:\crop-data\vosk_models\vosk-model-small-en-in-0.4\." /sdcard/Android/data/com.cropdoctor/files/vosk/en/
adb push "C:\crop-data\vosk_models\vosk-model-small-hi-0.22\." /sdcard/Android/data/com.cropdoctor/files/vosk/hi/
adb push "C:\crop-data\vosk_models\vosk-model-small-te-0.42\." /sdcard/Android/data/com.cropdoctor/files/vosk/te/

# Verify pushed files and sizes
adb shell ls -lh /sdcard/Android/data/com.cropdoctor/files/
```

---

## 5. How to Check It Worked

### 1. In the Install Screen
Open **More Options (☰)** -> **"Install AI Model (.pte)"**. You will see:
- A top green card: **"All AI Models Installed & Ready"**.
- Model status: **"Installed (1083 MB)"**.
- Tokenizer status: **"Installed (2132 KB)"**.
- Button changed to: **"Reinstall / Replace Model (.pte)"**.

### 2. On the Diagnosis Result Screen
Take a leaf photo or pick one from the gallery. Once the instant vision diagnosis appears (< 1.5 s), look at the bottom right of the advisory card:
- **Real Llama Badge**: Displays a green badge with the text:
  - English: **`Explained on this phone by Llama 3.2 1B`**
  - Hindi: **`इस फ़ोन पर Llama 3.2 1B द्वारा समझाया गया`**
- Notice the explanation text streams smoothly on-device without any internet connection.

### 3. In the Offline Chat & Voice Input
Open **More Options (☰)** -> **"Chat with Agri Care AI"**:
- **Offline Text Chat**: Type any query (e.g., *"How do I manage leaf blast without chemicals?"*). Llama generates a grounded, plain-language response.
- **Offline Voice Input**: Tap the 🎙️ microphone icon. The recognizer prompts: *"Listening... Speak your question"*, transcribes speech offline, and submits the question to the AI doctor.

---

## 6. What You See When Files Are NOT Installed (Verifiable Fallback)

In compliance with project safety rules (Rule H11 & PS1 requirements), **the app never fakes intelligence and never breaks when files are absent**:

1. **Advisory Card Badge**: Displays a blue badge clearly stating:
   - English: **`Offline guide - AI explanation not available`**
   - Hindi: **`ऑफ़लाइन गाइड - AI स्पष्टीकरण उपलब्ध नहीं है`**
   - Telugu: **`ఆఫ్‌లైన్ గైడ్ - AI వివరణ అందుబాటులో లేదు`**  
     *(Note: Per Rule H6, Telugu advice is ALWAYS served directly from reviewed university Knowledge Base entries, as Llama 3.2 does not officially support Telugu).*
2. **Instant Verified Advice**: Sourced cultural, natural, and registered chemical recommendations appear instantly from the internal Knowledge Base.
3. **Chat Mic Button**: If you tap the microphone without installing the Vosk speech model, the app displays:  
   *`"Speech model for English/Hindi/Telugu is not installed. Please install it in Settings."`*

---

## 7. Troubleshooting

| Issue / Symptom | Root Cause | Solution |
|---|---|---|
| **"Install failed: Insufficient storage"** | The phone has less than 2.5 GB free internal flash storage. | Free up at least 3 GB of space in phone Settings -> Storage and retry. |
| **"Warning: Model file seems too small"** | Download was interrupted; file is incomplete (< 100 MB). | Check the file size on your computer (should be ~1.13 GB / 1,135,951,488 bytes). Re-download and re-select. |
| **"Speech model install error / Failed to install voice model"** | Selected an incomplete zip or zip with wrong folder hierarchy (< 15 MB). | Ensure the downloaded zip matches the official AlphaCephei release and select the exact zip file. |
| **Model is installed, but advisory badge says "Offline guide"** | Phone available RAM is low (< 1.8 GB free). Rule H15 prevents OOM crashing. | Close heavy background applications (games, browser tabs) to free system RAM, then reopen the app. |
| **ADB push reports "Permission denied"** | Attempted to push into restricted system folders like `/data/data/` without root. | Push to the app's external directory: `/sdcard/Android/data/com.cropdoctor/files/` (requires no root on Android 10+). |
| **App language switched to Telugu, but badge still says "Offline guide"** | Expected behavior by design (Rule H6). | Llama 3.2 does not officially support Telugu. Telugu is always 100% human-verified agronomy text from PJTSAU/ANGRAU to ensure complete safety for farmers. |
