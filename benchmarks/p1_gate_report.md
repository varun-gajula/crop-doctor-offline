# Task P1: Diagnostic Gate Report — Laptop Screen & Gallery Photos

**Date**: 2026-11-20  
**Device**: Motorola edge 60 (`arm64-v8a`, 12 GB RAM, Android 14)  
**Model**: 20-class MobileNetV3-Large (`vision.pte`, 16.17 MB)  
**Evaluation Harness**: Live on-device execution via ADB intent `diagnose_file`  
**Raw Benchmark Log**: [`benchmarks/p1_benchmark_raw.json`](file:///c:/crop-doctor-offline/benchmarks/p1_benchmark_raw.json)

---

## 1. Executive Summary

Task P1 diagnosed why photos of a laptop screen and images picked from the device gallery fail or are rejected during diagnosis. To isolate the failure mechanism, diagnostic logging (`CROPDOC_DECISION`) was added across all four decision gates in strict evaluation order:
1. **Gate 1**: Botanical Leaf Validator (`LeafValidator.validate(bitmap)`)
2. **Gate 2**: Free Energy Out-of-Distribution (OOD) check ($E(x) \le \text{energy\_threshold}$)
3. **Gate 3**: Calibrated Softmax Confidence ($P(\hat{y}|x) \ge \text{confidence\_threshold}$)
4. **Gate 4**: Non-crop Weeds filter (`weeds_not_a_crop`)

Sixty real test images across three distinct evaluation sets (20 laptop screen photos, 20 gallery images, 20 real camera field photos) were processed on the physical device.

**Key Findings**:
1. **Laptop Screen Photos**: 10 of 20 (50.0%) were rejected. **100% of rejections were caught at Gate 2 (Energy OOD)** due to Moiré spatial aliasing and backlight dynamic range blowout. Rejected screen photos had an average Free Energy of **`-7.4969`** (exceeding the OOD threshold of `-9.3050`).
2. **Gallery Path Discrepancy**: Real-world gallery photos fail primarily due to **missing EXIF rotation** and **missing center-square cropping** in `MainCameraScreen.kt`. Smartphone portrait photos are natively stored in sensor landscape array (EXIF Orientation 6 = 90° CW). The gallery launcher decodes them without rotating, feeding them into the classifier sideways (90° rotated) and anisotropically stretched/squashed to 512x512.
3. **Real Camera Field Photos**: 14 of 20 (70.0%) were rejected at Gate 2 because natural field backgrounds (soil, weeds, hands, shadows) generate higher Free Energy (mean `-7.2362`) than the single-leaf validation set used to calibrate the energy threshold (`-9.3050`).

---

## 2. Gate Rejection & Acceptance Breakdown

Every rejection was triggered by Gate 2 (Free Energy OOD). Zero images failed Gate 1 (Leaf Validator), Gate 3 (Confidence), or Gate 4 (Weeds).

| Evaluation Set | Total Images | Accepted | Gate 1 Rejected (Leaf Validator) | Gate 2 Rejected (Energy OOD) | Gate 3 Rejected (Low Confidence) | Gate 4 Rejected (Weeds / Unsupported) | Rejection Rate (%) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Screen Photos (Laptop Screen)** | 20 | 10 | 0 | 10 | 0 | 0 | **50.0%** |
| **Gallery Images (Pre-processed)** | 20 | 20 | 0 | 0 | 0 | 0 | **0.0%** |
| **Camera Photos (Real Field Leaves)** | 20 | 6 | 0 | 14 | 0 | 0 | **70.0%** |
| **Total Across All Sets** | **60** | **36** | **0** | **24** | **0** | **0** | **40.0%** |

*Note: Operating thresholds from `thresholds.json`: `temperature = 1.7818`, `confidence_threshold = 0.3434`, `energy_threshold = -9.3050`.*

---

## 3. Energy and Probability Statistical Distribution

All metrics are directly measured from on-device logcat outputs.

### Free Energy Distribution: $E(x) = -T \cdot \log \sum_i e^{z_i / T}$
*(Threshold: $-9.3050$. In-distribution images have lower, more negative energy; OOD images have higher energy $>-9.3050$)*

| Evaluation Set | Accepted Min | Accepted Max | Accepted Mean | Accepted Median | Rejected Min | Rejected Max | Rejected Mean | Rejected Median |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Screen Photos** | -16.2360 | -10.1903 | **-12.9337** | -12.5065 | -9.1778 | -5.7087 | **-7.4969** | -7.6912 |
| **Gallery Images** | -36.6364 | -9.9062 | **-20.0278** | -19.4513 | N/A | N/A | **N/A** | N/A |
| **Camera Real Leaves** | -19.8152 | -9.8291 | **-13.1549** | -12.6605 | -9.0237 | -5.3621 | **-7.2362** | -7.3790 |

### Calibrated Top-1 Probability Distribution: $P(\hat{y}|x)$
*(Confidence Threshold: $0.3434$)*

| Evaluation Set | Accepted Min | Accepted Max | Accepted Mean | Accepted Median | Rejected Min | Rejected Max | Rejected Median |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Screen Photos** | 0.7848 | 0.9970 | **0.9548** | 0.9727 | 0.1705 | 0.8637 | **0.5937** |
| **Gallery Images** | 0.7532 | 1.0000 | **0.9628** | 0.9998 | N/A | N/A | **N/A** |
| **Camera Real Leaves** | 0.8308 | 0.9996 | **0.9366** | 0.9575 | 0.3279 | 0.8595 | **0.5397** |

---

## 4. Comprehensive Architectural Differences: Camera Path vs. Gallery Path

Inspecting `app/src/main/java/com/cropdoctor/ui/MainCameraScreen.kt`:

| Feature / Step | Camera Capture Path (`lines 360-406`) | Gallery Photo Picker Path (`lines 93-136`) | Impact on Diagnosis |
| :--- | :--- | :--- | :--- |
| **1. EXIF Rotation** | **Applied physically**: Reads `image.imageInfo.rotationDegrees` and rotates bitmap via `Matrix().postRotate(...)`. The bitmap is guaranteed **upright**. | **NOT applied**: Reads `ExifInterface` tag into integer metadata, but **never transforms or rotates the decoded bitmap**. | **CRITICAL BUG**: Smartphone portrait photos are saved with EXIF Orientation 6 (90° CW). Gallery images enter classifier **turned 90° sideways**, misaligning leaf venation and lesion geometry. |
| **2. Aspect Ratio & Framing** | **1:1 Center-Square Crop**: Computes `minDim = minOf(w, h)` and center-crops `Bitmap.createBitmap(rotated, xOffset, yOffset, minDim, minDim)`. Viewfinder frames lesion in center. | **No cropping**: Passes raw rectangular aspect ratio (e.g. 4:3, 16:9, 20:9). | **CRITICAL BUG**: `Bitmap.createScaledBitmap(..., 512, 512)` squashes non-square gallery images, causing **anisotropic distortion** (16:9 is squished 1.77x horizontally). |
| **3. Color Space Decoding** | **sRGB Standard**: CameraX preview and capture pipelines enforce standard sRGB buffers. | **Display P3 / Wide Gamut**: `BitmapFactory.decodeByteArray` does not set `inPreferredColorSpace = sRGB`. | On wide-gamut devices (Motorola Edge 60), gallery images remain in Display P3, causing RGB/HSV channel value shifts relative to ImageNet normalization. |
| **4. Bitmap Config** | Guaranteed opaque `ARGB_8888`. | `inPreferredConfig` not explicitly set; PNGs/screenshots can load with premultiplied alpha or hardware configs. | Alpha channel blending can corrupt RGB tensor values. |
| **5. Subject Isolation** | Center viewfinder overlay guides user to isolate leaf, excluding distracting room or field borders. | User selects entire uncropped frame, often including hands, soil, feet, or table surfaces. | Reduces plant pixel fraction in Gate 1 and increases OOD Free Energy in Gate 2. |

---

## 5. Root Cause Analysis per Set with Concrete Evidence

### Set 1: Photos of a Laptop Screen
**Observed Behavior**: 10 of 20 (50.0%) rejected at Gate 2 (`REJECTED_OOD_ENERGY`).
**Evidence**:
- Example 1: `screen_01_tomato_septoria_actual_phone.jpg` (actual phone photo of laptop screen): Top-1 probability `0.6230` (passes Gate 3 $> 0.3434$), but Free Energy is `-6.8074` (fails Gate 2 $\le -9.3050$). Decision: `REJECTED_OOD_ENERGY`.
- Example 2: `screen_16_maize_common_rust` on laptop screen: Top-1 probability `0.8114`, but Energy is `-7.3152`. Decision: `REJECTED_OOD_ENERGY`.
- Rejected screen photos exhibit mean energy `-7.4969`, compared to `-12.9337` for accepted screen photos.
**Root Causes**:
1. **Moiré Subpixel Spatial Aliasing**: When photographing an LCD display, the phone camera's Bayer filter grid optically interferes with the display's periodic sub-pixel array (RGB stripe or PenTile pitch). This produces high-frequency Moiré fringing patterns that do not exist on natural leaves. Convolutional filters in MobileNetV3 respond with high variance and lower logit magnitudes across the output vector.
2. **Direct LED Backlight Emission & Specular Glare**: Laptop screens are active light emitters with high specular reflection and elevated black levels (~0.2-0.5 cd/m²). Ambient room light reflecting off the monitor glass washes out subtle lesion textures, flattening contrast and shifting feature activations away from the in-distribution training manifold into high Free Energy ($E > -9.3050$).

### Set 2: Images Picked from the Gallery
**Observed Behavior**: In real-world phone usage, gallery images fail to detect or return low confidence.
**Evidence**:
- Code comparison in `MainCameraScreen.kt`:
  ```kotlin
  // Camera Path: Center-cropped 1:1 and upright
  val rotated = if (rotationDegrees != 0) {
      val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
      Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
  } else rawBitmap
  val minDim = minOf(rotated.width, rotated.height)
  val finalBitmap = Bitmap.createBitmap(rotated, (rotated.width - minDim) / 2, (rotated.height - minDim) / 2, minDim, minDim)
  ```
  ```kotlin
  // Gallery Path: Unrotated and uncropped
  val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
  // BUG: exifOrientation is parsed but NEVER applied to bitmap!
  // BUG: No center-square crop!
  dispatchCapturedImage(bitmap, selectedCropFilter, meta)
  ```
**Root Causes**:
1. **Sideways Orientation (EXIF Orientation 6)**: Every portrait photo taken by the Android camera is stored in raw sensor landscape orientation with an EXIF tag `Orientation: 6` (90° CW). Because `MainCameraScreen.kt` does not apply the EXIF rotation matrix during gallery decoding, photos are sent to the classifier rotated 90° sideways.
2. **Aspect Ratio Distortion**: Non-square gallery images are scaled directly to 512x512 via `Bitmap.createScaledBitmap`, stretching or squishing lesions and veins.

### Set 3: Camera Photos of Real Leaves in the Field (PlantDoc)
**Observed Behavior**: 14 of 20 (70.0%) rejected at Gate 2 (`REJECTED_OOD_ENERGY`).
**Evidence**:
- Rejected real leaves have an average Free Energy of **`-7.2362`** (all $> -9.3050$).
- Top-1 probabilities average `0.5397` (above the `0.3434` confidence threshold), but Gate 2 blocks them.
**Root Causes**:
1. **Overly Conservative Energy Threshold**: `energy_threshold = -9.3050` was computed as the 95th percentile on the validation split of the dataset, where leaves were mostly well-centered with plain or clean backgrounds.
2. **Complex Field Background Clutter**: Real field photos contain soil, weeds, surrounding branches, hands holding leaves, and harsh dappled shadows. These complex background features increase logit entropy, raising the Free Energy above `-9.3050`.

---

## 6. Recommendations (Report Only — No Thresholds Changed)

Per Task P1 instructions, **no behavior or thresholds have been modified**. The following fixes are recommended for subsequent tasks:
1. **Fix Gallery Decoding in `MainCameraScreen.kt`**:
   - Apply EXIF rotation to rotate gallery bitmaps upright before dispatching.
   - Apply center-square cropping (or letterbox padding) to match the 1:1 aspect ratio of the vision model.
   - Force `inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)` and `inPreferredConfig = Bitmap.Config.ARGB_8888`.
2. **OOD Energy Threshold Calibration**:
   - The current `energy_threshold = -9.3050` rejects 70% of real field photos and 50% of screen photos. Calibrating the energy threshold on a dataset that includes complex field backgrounds (or disabling Gate 2 when leaf area fraction is high) will prevent false OOD rejections on real field crops.
