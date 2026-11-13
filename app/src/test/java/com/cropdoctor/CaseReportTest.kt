package com.cropdoctor

import com.cropdoctor.domain.CaseReport
import com.cropdoctor.domain.CaseReportRepository
import org.junit.Assert.*
import org.junit.Test

class CaseReportTest {

    @Test
    fun testFormatShareTextCertainCase() {
        val report = CaseReport(
            id = "test-123",
            timestamp = 1731484800000L,
            dateString = "13 Nov 2026, 11:55 AM",
            crop = "Rice",
            problemLabel = "Rice Bacterial Leaf Blight",
            certaintyWord = "Sure",
            severity = "Moderate",
            region = "Telangana",
            note = "Noticed water-soaked stripes on top leaves.",
            isUncertain = false,
            classId = "rice_bacterial_leaf_blight"
        )

        val shareText = CaseReportRepository.formatShareText(report)

        assertTrue(shareText.contains("CROP DOCTOR - CASE REPORT"))
        assertTrue(shareText.contains("Date: 13 Nov 2026, 11:55 AM"))
        assertTrue(shareText.contains("Region: Telangana"))
        assertTrue(shareText.contains("Crop: Rice"))
        assertTrue(shareText.contains("Finding: Rice Bacterial Leaf Blight"))
        assertTrue(shareText.contains("Certainty: Sure"))
        assertTrue(shareText.contains("Severity: Moderate"))
        assertTrue(shareText.contains("Farmer Note: Noticed water-soaked stripes on top leaves."))
        assertTrue(shareText.contains("100% Offline AI Diagnosis"))
    }

    @Test
    fun testFormatShareTextUncertainCase() {
        val report = CaseReport(
            id = "test-456",
            timestamp = 1731484800000L,
            dateString = "13 Nov 2026, 12:05 PM",
            crop = "—",
            problemLabel = "Can't tell – no disease identified",
            certaintyWord = "Not sure",
            severity = "N/A",
            region = "Andhra Pradesh",
            note = "Leaf damaged by heavy rains.",
            isUncertain = true,
            classId = "unknown"
        )

        val shareText = CaseReportRepository.formatShareText(report)

        assertTrue(shareText.contains("Finding: Can't tell – no disease identified"))
        assertTrue(shareText.contains("Certainty: Not sure"))
        assertFalse("Uncertain case must not show false severity in report", shareText.contains("Severity:"))
        assertTrue("Must include Kisan Call Centre referral", shareText.contains("1800-180-1551"))
        assertTrue(shareText.contains("Farmer Note: Leaf damaged by heavy rains."))
    }

    @Test
    fun testFormatShareTextSevereCaseRefersExtension() {
        val report = CaseReport(
            id = "test-789",
            timestamp = 1731484800000L,
            dateString = "13 Nov 2026, 12:15 PM",
            crop = "Cotton",
            problemLabel = "Cotton Bacterial Blight",
            certaintyWord = "Sure",
            severity = "Severe",
            region = "Telangana",
            note = "",
            isUncertain = false,
            classId = "cotton_bacterial_blight"
        )

        val shareText = CaseReportRepository.formatShareText(report)

        assertTrue(shareText.contains("Severity: Severe"))
        assertTrue("Severe case must include Kisan Call Centre referral", shareText.contains("1800-180-1551"))
    }
}

