package com.cropdoctor

import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.SeverityCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class SeverityCalculatorTest {

    @Test
    fun testHealthyClassAlwaysReturnsMild() {
        assertEquals(SeverityBand.MILD, SeverityCalculator.calculate(0.0f, "rice_healthy"))
        assertEquals(SeverityBand.MILD, SeverityCalculator.calculate(0.85f, "rice_healthy"))
        assertEquals(SeverityBand.MILD, SeverityCalculator.calculate(0.50f, "tomato_healthy"))
    }

    @Test
    fun testStandardLesionThresholds() {
        val classId = "rice_bacterial_leaf_blight"
        // < 10% lesion is MILD
        assertEquals(SeverityBand.MILD, SeverityCalculator.calculate(0.05f, classId))
        assertEquals(SeverityBand.MILD, SeverityCalculator.calculate(0.09f, classId))

        // 10% - 30% lesion is MODERATE
        assertEquals(SeverityBand.MODERATE, SeverityCalculator.calculate(0.10f, classId))
        assertEquals(SeverityBand.MODERATE, SeverityCalculator.calculate(0.25f, classId))
        assertEquals(SeverityBand.MODERATE, SeverityCalculator.calculate(0.30f, classId))

        // > 30% lesion is SEVERE
        assertEquals(SeverityBand.SEVERE, SeverityCalculator.calculate(0.31f, classId))
        assertEquals(SeverityBand.SEVERE, SeverityCalculator.calculate(0.75f, classId))
    }

    @Test
    fun testVirusClassesMinimumSeverityIsModerate() {
        // Even small lesions on leaf curl virus warrant minimum MODERATE due to vector risk
        val cottonVirus = "cotton_leaf_curl_virus"
        assertEquals(SeverityBand.MODERATE, SeverityCalculator.calculate(0.02f, cottonVirus))
        assertEquals(SeverityBand.MODERATE, SeverityCalculator.calculate(0.20f, cottonVirus))
        assertEquals(SeverityBand.SEVERE, SeverityCalculator.calculate(0.35f, cottonVirus))

        val tomatoVirus = "tomato_yellow_leaf_curl_virus"
        assertEquals(SeverityBand.MODERATE, SeverityCalculator.calculate(0.05f, tomatoVirus))
        assertEquals(SeverityBand.SEVERE, SeverityCalculator.calculate(0.40f, tomatoVirus))
    }
}

