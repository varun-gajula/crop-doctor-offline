package com.cropdoctor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cropdoctor.vision.ImagePreprocessor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ImagePipelineParityTest {

    private fun createSyntheticLeafBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(34, 139, 34)) // Forest green background

        val paint = Paint().apply {
            color = Color.rgb(139, 69, 19) // Brown lesion spot
            style = Paint.Style.FILL
        }
        canvas.drawCircle(width / 2f, height / 2f, width / 4f, paint)

        // Draw an asymmetric feature (yellow stripe on top half) to detect orientation
        val stripePaint = Paint().apply {
            color = Color.rgb(255, 215, 0)
            strokeWidth = 20f
        }
        canvas.drawLine(0f, height * 0.2f, width.toFloat(), height * 0.2f, stripePaint)

        return bitmap
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, bos)
        return bos.toByteArray()
    }

    @Test
    fun testCameraAndGalleryProduceIdenticalTensors() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val origBmp = createSyntheticLeafBitmap(1024, 768)
        val jpegBytes = compressToJpeg(origBmp)

        // Camera path: bytes captured from camera sensor -> prepare
        val cameraPreprocessed = ImagePreprocessor.prepare(jpegBytes)

        // Gallery path: bytes read from gallery content stream -> prepare
        val galleryPreprocessed = ImagePreprocessor.prepare(jpegBytes)

        assertEquals(512, cameraPreprocessed.bitmap512.width)
        assertEquals(512, cameraPreprocessed.bitmap512.height)
        assertEquals(512, galleryPreprocessed.bitmap512.width)
        assertEquals(512, galleryPreprocessed.bitmap512.height)

        val camBuf = cameraPreprocessed.tensorBuffer
        val galBuf = galleryPreprocessed.tensorBuffer

        var maxDiff = 0.0f
        val totalElements = 3 * 512 * 512
        for (i in 0 until totalElements) {
            val diff = abs(camBuf.get(i) - galBuf.get(i))
            if (diff > maxDiff) maxDiff = diff
        }

        assertTrue("Camera and Gallery tensors must match within 1e-3 tolerance, got max diff: $maxDiff", maxDiff < 1e-3f)
        assertEquals(0.0f, maxDiff, 1e-6f)
    }

    @Test
    fun testExifOrientation6GivesSameTensorFromCameraAndGallery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val origBmp = createSyntheticLeafBitmap(800, 600)

        // Rotate bitmap 90 degrees CW and save with EXIF orientation 6
        val matrix = Matrix().apply { postRotate(90f) }
        val rotatedSideways = Bitmap.createBitmap(origBmp, 0, 0, origBmp.width, origBmp.height, matrix, true)

        val cacheFile = File(context.cacheDir, "test_exif_rot6.jpg")
        FileOutputStream(cacheFile).use { out ->
            rotatedSideways.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        val exif = ExifInterface(cacheFile.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        exif.saveAttributes()

        val exifBytes = cacheFile.readBytes()

        // Preprocess through camera path (bytes from ImageCapture)
        val cameraPreprocessed = ImagePreprocessor.prepare(exifBytes)

        // Preprocess through gallery path (bytes from contentResolver stream)
        val galleryPreprocessed = ImagePreprocessor.prepare(exifBytes)

        // Both must detect EXIF orientation 6 (90 degrees)
        assertEquals(90, cameraPreprocessed.exifRotationDegrees)
        assertEquals(90, galleryPreprocessed.exifRotationDegrees)

        assertEquals(512, cameraPreprocessed.bitmap512.width)
        assertEquals(512, cameraPreprocessed.bitmap512.height)
        assertEquals(512, galleryPreprocessed.bitmap512.width)
        assertEquals(512, galleryPreprocessed.bitmap512.height)

        // Compare tensors from camera and gallery: must be identical (< 1e-3)
        val camBuf = cameraPreprocessed.tensorBuffer
        val galBuf = galleryPreprocessed.tensorBuffer

        var maxDiff = 0.0f
        val totalElements = 3 * 512 * 512
        for (i in 0 until totalElements) {
            val diff = abs(camBuf.get(i) - galBuf.get(i))
            if (diff > maxDiff) maxDiff = diff
        }

        assertTrue("Camera and Gallery with EXIF orientation 6 must give the same tensor (< 1e-3), got max diff: $maxDiff", maxDiff < 1e-3f)
        assertEquals(0.0f, maxDiff, 1e-6f)
    }
}
