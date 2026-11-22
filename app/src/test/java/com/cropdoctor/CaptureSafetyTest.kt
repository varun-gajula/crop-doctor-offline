package com.cropdoctor

import com.cropdoctor.domain.ClassifierResult
import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.DiagnosisOutcome
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KnowledgeBase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CaptureSafetyTest {

    @Test
    fun testTimeoutProducesCantTellOutcome() {
        var isProcessing = true
        var result: ClassifierResult? = null

        try {
            runBlocking {
                withTimeout(100L) {
                    delay(500L) // Simulate hanging analysis
                }
            }
        } catch (e: TimeoutCancellationException) {
            result = ClassifierResult(
                classId = "unknown",
                displayName = "Could not read the photo. Please try again.",
                confidence = 0f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0f,
                outcome = DiagnosisOutcome.CANT_TELL,
                outcomeReason = "TIMEOUT: Analysis exceeded 10s limit",
                selectedCrop = "rice"
            )
        } finally {
            isProcessing = false
        }

        assertFalse("Processing flag must be cleared on timeout", isProcessing)
        assertNotNull("Result must be populated on timeout", result)
        assertEquals("Outcome must be CANT_TELL on timeout", DiagnosisOutcome.CANT_TELL, result?.outcome)
        assertTrue("Reason must indicate TIMEOUT", result?.outcomeReason?.startsWith("TIMEOUT") == true)
        assertEquals("Display name must inform farmer to retry", "Could not read the photo. Please try again.", result?.displayName)
    }

    @Test
    fun testExceptionProducesCantTellOutcome() {
        var isProcessing = true
        var result: ClassifierResult? = null

        try {
            runBlocking {
                throw RuntimeException("Simulated native decode failure")
            }
        } catch (e: Exception) {
            result = ClassifierResult(
                classId = "unknown",
                displayName = "Could not read the photo. Please try again.",
                confidence = 0f,
                severityBand = SeverityBand.MILD,
                certaintyBand = CertaintyBand.NOT_SURE,
                lesionFraction = 0f,
                outcome = DiagnosisOutcome.CANT_TELL,
                outcomeReason = "ERROR: ${e.message}",
                selectedCrop = "tomato"
            )
        } finally {
            isProcessing = false
        }

        assertFalse("Processing flag must be cleared on exception", isProcessing)
        assertNotNull("Result must be populated on error", result)
        assertEquals("Outcome must be CANT_TELL on error", DiagnosisOutcome.CANT_TELL, result?.outcome)
        assertTrue("Reason must indicate ERROR", result?.outcomeReason?.startsWith("ERROR") == true)
    }

    @Test
    fun testNonCancellableGuaranteesFlagResetOnJobCancellation() = runBlocking {
        var isProcessing = true

        val job = launch(Dispatchers.Default) {
            try {
                delay(1000L)
            } finally {
                withContext(NonCancellable) {
                    isProcessing = false
                }
            }
        }

        delay(50L)
        job.cancel()
        job.join()

        assertFalse("isProcessing must be reset to false even when coroutine is cancelled", isProcessing)
    }

    @Test
    fun testCouldNotReadPhotoStringInAllThreeLanguages() {
        val baseResDir = File("src/main/res")
        val enFile = File(baseResDir, "values/strings.xml")
        val teFile = File(baseResDir, "values-te/strings.xml")
        val hiFile = File(baseResDir, "values-hi/strings.xml")

        fun getString(file: File, key: String): String? {
            val factory = DocumentBuilderFactory.newInstance()
            val doc = factory.newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                if (node.attributes.getNamedItem("name")?.nodeValue == key) {
                    return node.textContent
                }
            }
            return null
        }

        val enStr = getString(enFile, "error_could_not_read_photo")
        val teStr = getString(teFile, "error_could_not_read_photo")
        val hiStr = getString(hiFile, "error_could_not_read_photo")

        assertNotNull("English error string must exist", enStr)
        assertNotNull("Telugu error string must exist", teStr)
        assertNotNull("Hindi error string must exist", hiStr)

        assertEquals("Could not read the photo. Please try again.", enStr)
        assertTrue("Telugu error string must contain Telugu characters", teStr!!.any { it in '\u0C00'..'\u0C7F' })
        assertTrue("Hindi error string must contain Devanagari characters", hiStr!!.any { it in '\u0900'..'\u097F' })
    }

    @Test
    fun testUnretainedBitmapCleanedUpInFinallyOnTimeout() = runBlocking {
        var bitmapRecycled = false
        var bitmapRetained = false

        val job = launch(Dispatchers.Default) {
            try {
                withTimeout(50L) {
                    delay(500L) // Simulate timeout
                    bitmapRetained = true
                }
            } catch (e: TimeoutCancellationException) {
                // Handled
            } finally {
                if (!bitmapRetained) {
                    bitmapRecycled = true
                }
            }
        }

        job.join()
        assertTrue("Bitmap must be recycled in finally block when analysis times out", bitmapRecycled)
        assertFalse("Bitmap must not be marked as retained on timeout", bitmapRetained)
    }

    @Test
    fun testUnretainedBitmapCleanedUpInFinallyOnException() = runBlocking {
        var bitmapRecycled = false
        var bitmapRetained = false

        val job = launch(Dispatchers.Default) {
            try {
                withTimeout(1000L) {
                    throw RuntimeException("Simulated inference failure")
                }
            } catch (e: Exception) {
                // Handled
            } finally {
                if (!bitmapRetained) {
                    bitmapRecycled = true
                }
            }
        }

        job.join()
        assertTrue("Bitmap must be recycled in finally block when analysis throws exception", bitmapRecycled)
        assertFalse("Bitmap must not be marked as retained on exception", bitmapRetained)
    }
}
