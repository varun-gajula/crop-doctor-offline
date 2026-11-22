# Crop Doctor - Model Installation & On-Device AI Advisory Guide

## 1. Overview

Crop Doctor runs 100% offline with zero cloud dependency. In accordance with Rule H2, large model weights (such as the 1.13 GB Llama 3.2 1B SpinQuant INT4 ExecuTorch model) are not bundled inside the git repository or packaged directly inside the base APK (which must remain compact and under 50 MB).

To enable on-device generative advisory powered by Meta Llama 3.2 1B, the app provides two zero-network installation pathways:
1. **In-App SAF File Picker ("Install AI Model" Screen)**: For farmers, extension workers, or field evaluators transferring files via SD card, USB OTG, or local peer-to-peer sharing.
2. **Developer USB Sideload (`adb push`)**: Direct push to the app's private external storage during benchmarking or evaluation.

If the model files are not installed or if available system RAM is below the safety threshold, Crop Doctor seamlessly falls back to its deterministic, university-verified static knowledge base, displaying the badge `"Offline guide - AI explanation not available"`.

---

## 2. Model Files Specifications

| File Name | Exact File Size | SHA256 / Checksum Description | Format & Quantization |
|---|---|---|---|
| `llama-3.2-1b-spinquant.pte` | 1,135,951,488 bytes (~1.06 GB / 1083 MB) | SpinQuant INT4 quantized ExecuTorch program | ExecuTorch 0.6.0 Arm64 PTE |
| `tokenizer.model` | 2,183,982 bytes (~2.08 MB / 2132 KB) | TikToken / SentencePiece BPE vocabulary | Binary tokenizer model |

---

## 3. Storage Locations & Resolution Hierarchy

On Android 14+ (API 34/35), SELinux policy enforces strict boundaries on untrusted app processes (`u:r:untrusted_app:s0`). Specifically, untrusted apps cannot access `/data/local/tmp` unless directory permissions are explicitly widened, which standard production apps cannot assume.

Crop Doctor searches for model files using the following prioritized hierarchy in `ExecuTorchLlamaAdvisor.kt`:

1. **Internal Private App Storage (`context.filesDir`)**:
   - Model Path: `/data/user/0/com.cropdoctor/files/llama-3.2-1b-spinquant.pte`
   - Tokenizer Path: `/data/user/0/com.cropdoctor/files/tokenizer.model`
   - *Security*: Completely isolated to `com.cropdoctor`; inaccessible to other apps. Target directory for the in-app file picker.

2. **External App-Specific Storage (`context.getExternalFilesDir(null)`)**:
   - Model Path: `/storage/emulated/0/Android/data/com.cropdoctor/files/llama-3.2-1b-spinquant.pte`
   - Tokenizer Path: `/storage/emulated/0/Android/data/com.cropdoctor/files/tokenizer.model`
   - *Accessibility*: Accessible via USB `adb push` without root; readable by the app without runtime `READ_EXTERNAL_STORAGE` permissions on Android 10+.

3. **Development / Fallback Storage (`/data/local/tmp`)**:
   - Model Path: `/data/local/tmp/llama-3.2-1b-spinquant.pte`
   - Tokenizer Path: `/data/local/tmp/tokenizer.model`
   - Checked only if file exists, `canRead()` is true, and size validation passes.

---

## 4. In-App Model Installation (No Internet Required)

Crop Doctor includes a built-in **"Install AI Model"** screen (`ModelInstallScreen.kt`) that allows users to copy weights from internal storage, an SD card, or a USB OTG drive without root access or a computer:

### Step-by-Step Instructions:
1. **Access the Screen**:
   - Long-press the app title or navigate to the menu and select **"Install AI Model"** (accessible from the home or result screens).
2. **Install Model (`.pte`)**:
   - Tap **"Pick Model File (.pte)"**.
   - The Android Storage Access Framework (SAF) document picker opens.
   - Select `llama-3.2-1b-spinquant.pte` from your Downloads, SD card, or USB drive.
   - The app copies the file into `/data/user/0/com.cropdoctor/files/` and verifies the file size (`length > 100,000,000 bytes`).
3. **Install Tokenizer (`tokenizer.model`)**:
   - Tap **"Pick Tokenizer (tokenizer.model)"**.
   - Select `tokenizer.model` using the SAF document picker.
   - The app copies the file and verifies size (`length > 50,000 bytes`).
4. **Status & Verification**:
   - When both files are successfully copied, the screen displays green checkmarks:
     - `Llama 3.2 1B Model: Installed (1083 MB)`
     - `Tokenizer: Installed (2132 KB)`
   - The advisor automatically reloads its model registry.

---

## 5. Developer Sideloading via USB (`adb push`)

For benchmarking or lab testing on an Arm reference device (such as the 6 GB POCO M4 5G), sideload the weights directly to the app-specific external storage via `adb`:

```bash
# Ensure device is connected and recognized
adb devices

# Create app directory if not already created
adb shell "mkdir -p /storage/emulated/0/Android/data/com.cropdoctor/files"

# Push Llama 3.2 1B model weights (~1.06 GB)
adb push llama-3.2-1b-spinquant.pte /storage/emulated/0/Android/data/com.cropdoctor/files/

# Push Tokenizer vocabulary (~2.08 MB)
adb push tokenizer.model /storage/emulated/0/Android/data/com.cropdoctor/files/

# Verify file presence and exact byte sizes
adb shell "ls -la /storage/emulated/0/Android/data/com.cropdoctor/files/"
```

---

## 6. Verifiable Runtime Behavior & Audit Trail

### 6.1 Real Llama Execution
When the model and tokenizer are present and device RAM is sufficient:
- **UI Label**: `Explained on this phone by Llama 3.2 1B` (Badge with model icon)
- **Logcat Tag**: `CROPDOC_DECISION`
- **Audit Line**:
  ```text
  CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=LLAMA tokens=120 ttft=10892ms tok_per_sec=3.34
  ```
- **Case Report JSON**: Contains `"advisor_source": "LLAMA"`

### 6.2 Fallback to Offline Knowledge Base
When model files are absent, unreadable, or device memory is under pressure:
- **UI Label**: `Offline guide - AI explanation not available`
- **Behavior**: Static agronomic recommendations from PJTSAU/ANGRAU/ICAR rendered immediately in one frame with zero simulated delay.
- **Logcat Tag**: `CROPDOC_DECISION`
- **Audit Line**:
  ```text
  CROPDOC_DECISION: ADVISOR_COMPLETE: advisor_source=OFFLINE_GUIDE tokens=0 ttft=0ms tok_per_sec=0.00
  ```
- **Case Report JSON**: Contains `"advisor_source": "OFFLINE_GUIDE"`
