package com.cropdoctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cropdoctor.vision.ExecuTorchClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisionPhoneSanityTest {

    private val TAG = "VisionPhoneSanityTest"

    @Test
    fun testVisionClassifierOnPhone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val classifier = ExecuTorchClassifier(context)

        // Create a 320x320 green test bitmap (simulating a leaf)
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
        for (x in 0 until 320) {
            for (y in 0 until 320) {
                bitmap.setPixel(x, y, Color.rgb(34, 139, 34))
            }
        }

        Log.i(TAG, "Calling classifier.classify() on physical phone...")
        val result = classifier.classify(bitmap, "tomato")
        Log.i(TAG, "Classification completed! Result: classId=${result.classId}, displayName=${result.displayName}, conf=${result.confidence}, time=${result.inferenceTimeMs}ms")

        assertNotNull(result)
        classifier.close()
    }
}

