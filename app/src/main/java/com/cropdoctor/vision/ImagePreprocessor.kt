package com.cropdoctor.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Single, unified image preparation pipeline for both Camera and Gallery sources.
 *
 * Enforces:
 * 1. Decode with inSampleSize to prevent out-of-memory errors on high-res camera captures.
 * 2. EXIF orientation detection and upright rotation.
 * 3. Conversion to standard sRGB ARGB_8888 (no hardware bitmaps, no alpha channel corruption).
 * 4. 512x512 resize using direct anisotropic bilinear scaling (squash) to match the training notebook
 *    (torchvision transforms.Resize((512, 512))).
 * 5. ImageNet normalization (mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]) into planar [1, 3, 512, 512].
 * 6. Direct buffer reuse and immediate intermediate bitmap recycling to prevent GC pressure and memory leaks.
 */
object ImagePreprocessor {

    const val TARGET_SIZE = 512
    val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    data class PreprocessedImage(
        val bitmap512: Bitmap,
        val tensorBuffer: FloatBuffer,
        val origWidth: Int,
        val origHeight: Int,
        val exifRotationDegrees: Int,
        val leafValidation: LeafValidationResult
    )

    /**
     * Reads EXIF orientation from JPEG bytes and returns rotation degrees (0, 90, 180, 270).
     */
    fun getExifRotation(bytes: ByteArray): Int {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Reads EXIF orientation from InputStream and returns rotation degrees.
     */
    fun getExifRotation(stream: InputStream): Int {
        return try {
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Calculates appropriate inSampleSize so decoding stays within memory bounds while
     * keeping resolution >= TARGET_SIZE.
     */
    fun calculateInSampleSize(origWidth: Int, origHeight: Int, targetSize: Int = TARGET_SIZE): Int {
        var sampleSize = 1
        while (origWidth / (sampleSize * 2) >= targetSize && origHeight / (sampleSize * 2) >= targetSize) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Primary preparation entry point from raw JPEG / PNG bytes.
     * Reuses reusableBuffer if provided; otherwise allocates a native direct FloatBuffer.
     * When [cropToViewfinder] is true, crops strictly to the center square corresponding
     * to the camera viewfinder box (fraction of height given by [viewfinderHeightFraction]).
     */
    fun prepare(
        bytes: ByteArray,
        reusableBuffer: FloatBuffer? = null,
        cropToViewfinder: Boolean = false,
        viewfinderHeightFraction: Float = 0.37f
    ): PreprocessedImage {
        val exifRotation = getExifRotation(bytes)

        // 1. Decode bounds
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight

        // 2. Decode with inSampleSize, sRGB, ARGB_8888
        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = calculateInSampleSize(origWidth, origHeight, TARGET_SIZE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            }
        }
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IllegalArgumentException("Failed to decode image bytes (${bytes.size} bytes)")

        return processBitmapInternal(
            rawBitmap = rawBitmap,
            origWidth = origWidth,
            origHeight = origHeight,
            exifRotation = exifRotation,
            reusableBuffer = reusableBuffer,
            cropToViewfinder = cropToViewfinder,
            viewfinderHeightFraction = viewfinderHeightFraction
        )
    }

    /**
     * Preparation entry point from an existing Bitmap and explicit EXIF rotation.
     */
    fun prepare(
        bitmap: Bitmap,
        exifRotationDegrees: Int = 0,
        reusableBuffer: FloatBuffer? = null,
        cropToViewfinder: Boolean = false,
        viewfinderHeightFraction: Float = 0.37f
    ): PreprocessedImage {
        return processBitmapInternal(
            rawBitmap = bitmap,
            origWidth = bitmap.width,
            origHeight = bitmap.height,
            exifRotation = exifRotationDegrees,
            reusableBuffer = reusableBuffer,
            allowRecycleRaw = false,
            cropToViewfinder = cropToViewfinder,
            viewfinderHeightFraction = viewfinderHeightFraction
        )
    }

    data class ViewfinderCrop(val left: Int, val top: Int, val size: Int)

    /**
     * Calculates the center square bounding box corresponding to the camera viewfinder box.
     */
    fun calculateViewfinderCrop(width: Int, height: Int, fraction: Float): ViewfinderCrop {
        val maxPossible = minOf(width, height)
        val minPossible = minOf(TARGET_SIZE, maxPossible)
        val boxSize = (height * fraction).toInt().coerceIn(minPossible, maxPossible)
        val left = ((width - boxSize) / 2).coerceIn(0, maxOf(0, width - boxSize))
        val top = ((height - boxSize) / 2).coerceIn(0, maxOf(0, height - boxSize))
        return ViewfinderCrop(left, top, boxSize)
    }

    private fun processBitmapInternal(
        rawBitmap: Bitmap,
        origWidth: Int,
        origHeight: Int,
        exifRotation: Int,
        reusableBuffer: FloatBuffer?,
        allowRecycleRaw: Boolean = true,
        cropToViewfinder: Boolean = false,
        viewfinderHeightFraction: Float = 0.37f
    ): PreprocessedImage {
        // 1. Ensure ARGB_8888 and non-hardware config
        val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rawBitmap.config == Bitmap.Config.HARDWARE
        val argbBitmap = if (rawBitmap.config != Bitmap.Config.ARGB_8888 || isHardware) {
            val copy = rawBitmap.copy(Bitmap.Config.ARGB_8888, true)
            if (allowRecycleRaw && copy != rawBitmap) rawBitmap.recycle()
            copy
        } else {
            rawBitmap
        }

        // 2. Apply EXIF rotation to ensure upright orientation
        val uprightBitmap = if (exifRotation != 0) {
            val matrix = Matrix().apply { postRotate(exifRotation.toFloat()) }
            val rotated = Bitmap.createBitmap(argbBitmap, 0, 0, argbBitmap.width, argbBitmap.height, matrix, true)
            if (rotated != argbBitmap) {
                if (allowRecycleRaw || argbBitmap != rawBitmap) {
                    argbBitmap.recycle()
                }
            }
            rotated
        } else {
            argbBitmap
        }

        // 2b. Viewfinder crop: if captured from camera with viewfinder framing,
        // crop strictly to the square region matching the viewfinder box
        val squareCropBitmap = if (cropToViewfinder) {
            val crop = calculateViewfinderCrop(uprightBitmap.width, uprightBitmap.height, viewfinderHeightFraction)
            val cropped = Bitmap.createBitmap(uprightBitmap, crop.left, crop.top, crop.size, crop.size)
            if (cropped != uprightBitmap) {
                if (allowRecycleRaw || uprightBitmap != rawBitmap) {
                    uprightBitmap.recycle()
                }
            }
            cropped
        } else {
            uprightBitmap
        }

        // 3. Resize to 512x512 matching training exactly (Bilinear interpolation to 512x512)
        val final512 = if (squareCropBitmap.width != TARGET_SIZE || squareCropBitmap.height != TARGET_SIZE) {
            val scaled = Bitmap.createScaledBitmap(squareCropBitmap, TARGET_SIZE, TARGET_SIZE, true)
            if (scaled != squareCropBitmap) {
                if (allowRecycleRaw || squareCropBitmap != rawBitmap) {
                    squareCropBitmap.recycle()
                }
            }
            scaled
        } else {
            squareCropBitmap
        }

        // 4. Botanical Leaf Validation (Gate 1)
        val leafValidation = LeafValidator.validate(final512)

        // 5. ImageNet Normalization into planar 1x3x512x512 tensor buffer
        val buffer = reusableBuffer ?: ByteBuffer.allocateDirect(3 * TARGET_SIZE * TARGET_SIZE * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val pixels = IntArray(TARGET_SIZE * TARGET_SIZE)
        final512.getPixels(pixels, 0, TARGET_SIZE, 0, 0, TARGET_SIZE, TARGET_SIZE)
        normalizePixels(pixels, buffer)

        return PreprocessedImage(
            bitmap512 = final512,
            tensorBuffer = buffer,
            origWidth = origWidth,
            origHeight = origHeight,
            exifRotationDegrees = exifRotation,
            leafValidation = leafValidation
        )
    }

    /**
     * Normalizes an array of ARGB_8888 pixels of size 512x512 into planar [1, 3, 512, 512]
     * using standard ImageNet mean and std.
     */
    fun normalizePixels(pixels: IntArray, buffer: FloatBuffer) {
        val area = TARGET_SIZE * TARGET_SIZE
        require(pixels.size == area) { "Pixel array size (${pixels.size}) must match area ($area)" }
        buffer.rewind()

        val m0 = IMAGENET_MEAN[0]
        val m1 = IMAGENET_MEAN[1]
        val m2 = IMAGENET_MEAN[2]
        val s0 = IMAGENET_STD[0]
        val s1 = IMAGENET_STD[1]
        val s2 = IMAGENET_STD[2]

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) / 255.0f
            val g = ((p shr 8) and 0xFF) / 255.0f
            val b = (p and 0xFF) / 255.0f

            buffer.put(i, (r - m0) / s0)
            buffer.put(area + i, (g - m1) / s1)
            buffer.put(2 * area + i, (b - m2) / s2)
        }
        buffer.rewind()
    }
}
