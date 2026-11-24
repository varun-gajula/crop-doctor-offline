# Llama 3.2 1B Offline Advisory Model Installation Guide

This document describes how to obtain, verify, and install the **Meta Llama 3.2 1B-Instruct** model (`.pte`) and tokenizer (`.model`) for offline on-device AI explanations in **Agri Care AI** (Crop Doctor Offline).

---

## 1. Why Weights Are Not in the Git Repository

1. **Project Rule H2 & Git File Size Limits**:  
   The 4-bit SpinQuant quantized Llama 3.2 1B model is **~1.13 GB** (1,135,951,488 bytes). GitHub blocks any single file larger than 100 MB, and project rules strictly forbid committing dataset archives or large model weights.
2. **Licensing Compliance**:  
   Meta Llama 3.2 is distributed under the **Llama 3.2 Community License Agreement**. Reviewers and developers must individually accept the license directly from Meta / Hugging Face before accessing model checkpoints.
3. **Zero Credentials / Tokens**:  
   No API keys, access tokens, or credentials are required or stored in this repository.

---

## 2. Where to Obtain the Files

### Step 2.1: Accept the Llama 3.2 Community License
Visit the official Meta Llama portal or Hugging Face repository and accept the license:
- **Meta Llama Portal**: <https://llama.meta.com/>
- **Hugging Face Llama 3.2 1B**: <https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct>

### Step 2.2: Download Pre-Exported ExecuTorch INT4 SpinQuant Weights
The on-device advisory engine is built with ExecuTorch 0.6.0 with the CPU XNNPACK Arm64 backend. Download the pre-exported 4-bit SpinQuant `.pte` weights and tokenizer:
- **ExecuTorch Community Repository**:  
  <https://huggingface.co/executorch-community/>
- **Direct Model Artifact**:  
  `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` (~1.13 GB)
- **Tokenizer Model**:  
  `tokenizer.model` (~2.18 MB) from the Meta repository:  
  <https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct/resolve/main/original/tokenizer.model>

*(Alternatively, users may export directly from official Meta checkpoints using ExecuTorch's `examples.models.llama.export_llama` script with `--quantization spinquant_int4`.)*

---

## 3. Checksum & Size Verification (SHA-256)

Before installation, verify that your local files match the official reference builds:

| File | Expected Size | Reference SHA-256 Checksum |
|---|---|---|
| `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` | 1,135,951,488 bytes (~1.13 GB) | `8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b` |
| `tokenizer.model` | 2,183,982 bytes (~2.18 MB) | `82e9d31979e92ab929cd544440f129d9ecd797b69e327f80f17e1c50d5551b55` |

---

## 4. Installation Methods

### Method 1: Automated Script (`install_llama.py`)
Run the provided automated verification and push utility:

```bash
# Dry-run simulation (verifies hashes and checks target paths without file operations)
python scripts/setup/install_llama.py --model path/to/model.pte --tokenizer path/to/tokenizer.model --accept-license --dry-run

# Active install to connected phone
python scripts/setup/install_llama.py --model path/to/model.pte --tokenizer path/to/tokenizer.model --accept-license
```

### Method 2: Manual ADB Push (Developer Setup)
On Android 10+ (and modern Android 14 SELinux environments), external app storage does not require root permissions. Push directly to the app's external files directory:

```bash
# Push model weights and tokenizer to app-specific external storage
adb push Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte /sdcard/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte
adb push tokenizer.model /sdcard/Android/data/com.cropdoctor/files/tokenizer.model
```

### Method 3: Direct on Phone (In-App Installer / Zero ADB)
1. Transfer both files to the phone's storage (e.g. via USB, SD Card, or Bluetooth to the `Download/` folder).
2. Open **Agri Care AI**.
3. Tap **More Options (☰)** -> **"Install AI Model (.pte)"** (or tap **"⚙️ Install AI Model (Offline)"** on any diagnosis result screen).
4. Tap **"Select Model File (.pte)"** and choose the `.pte` file using the Android system file picker.
5. Tap **"Select Tokenizer (.model)"** and choose `tokenizer.model`.
6. The app copies them to its private storage and verifies the file integrity.

---

## 5. Verification & Verifiable Fallback Guarantee

- **When Models Are Present**:  
  The app UI displays a green checkmark banner:  
  *`"Llama 3.2 1B & Tokenizer are active. No further installation required."`*  
  On diagnosis screens, Llama streams grounded explanations labelled:  
  *`"Explained on this phone by Llama 3.2 1B"`*.
- **When Models Are Missing or Unloaded**:  
  Agri Care AI never breaks or hangs. It immediately displays the verified Agronomy Knowledge Base text labeled:  
  *`"Offline guide - AI explanation not available"`*.
- **Telugu Language Protection (Rule H6)**:  
  Because Llama 3.2 does not officially support Telugu, Telugu advice is always served 100% from reviewed, human-verified Knowledge Base text regardless of whether Llama is installed.
