package com.cropdoctor

import com.cropdoctor.domain.CaseReport
import com.cropdoctor.domain.CaseReportRepository
import org.junit.Assert.*
import org.junit.Test

/**
 * Task G4 & Requirement F9: Field Report and Saved Cases Verification
 * - Report contains: crop, finding, type, severity, certainty, region, date, photo and note.
 * - Must NOT contain active ingredients, doses or product names unless dose_verified is true.
 * - Removes anything else not in F9 (e.g. offline marketing claims).
 */
class CaseReportTest {

    @Test
    fun testReportWithoutVerifiedDoseStrictlyExcludesChemicalsAndDoses() {
        val report = CaseReport(
            id = "case-unverified-001",
            timestamp = 1731650000000L,
            dateString = "15 Nov 2026, 12:45 PM",
            crop = "Rice",
            problemLabel = "Rice Bacterial Leaf Blight",
            type = "Disease",
            certaintyWord = "Sure",
            severity = "Moderate",
            region = "Telangana",
            photoPath = "/data/user/0/com.cropdoctor/files/case_photos/case_001.jpg",
            note = "Water-soaked lesions on leaf margins.",
            isUncertain = false,
            classId = "rice_bacterial_leaf_blight",
            doseVerified = false,
            activeIngredient = null,
            productExample = null,
            verifiedDose = null
        )

        val shareText = CaseReportRepository.formatShareText(report)

        // Must contain all F9 fields
        assertTrue("Report must have header", shareText.contains("CROP DOCTOR - FIELD REPORT"))
        assertTrue("Report must contain Date", shareText.contains("Date: 15 Nov 2026, 12:45 PM"))
        assertTrue("Report must contain Region", shareText.contains("Region: Telangana"))
        assertTrue("Report must contain Crop", shareText.contains("Crop: Rice"))
        assertTrue("Report must contain Finding", shareText.contains("Finding: Rice Bacterial Leaf Blight"))
        assertTrue("Report must contain Type", shareText.contains("Type: Disease"))
        assertTrue("Report must contain Certainty", shareText.contains("Certainty: Sure"))
        assertTrue("Report must contain Severity", shareText.contains("Severity: Moderate"))
        assertTrue("Report must contain Photo status", shareText.contains("Photo: Attached"))
        assertTrue("Report must contain Note", shareText.contains("Note: Water-soaked lesions on leaf margins."))

        // Must NOT contain chemical treatments, active ingredients, doses or product names
        assertFalse("Must not contain Treatment section when dose is unverified", shareText.contains("Treatment:"))
        assertFalse("Must not contain active ingredients", shareText.contains("Copper Oxychloride"))
        assertFalse("Must not contain product examples", shareText.contains("Blitox"))
        assertFalse("Must not contain dose values", shareText.contains("g/L") || shareText.contains("ml/L"))

        // Must NOT contain marketing claims not in F9
        assertFalse("Must not contain extraneous marketing text not in F9", shareText.contains("100% Offline AI Diagnosis"))
    }

    @Test
    fun testReportWithVerifiedDoseIncludesVerifiedTreatment() {
        val report = CaseReport(
            id = "case-verified-002",
            timestamp = 1731650500000L,
            dateString = "15 Nov 2026, 01:15 PM",
            crop = "Tomato",
            problemLabel = "Tomato Spider Mites",
            type = "Pest",
            certaintyWord = "Probably",
            severity = "Mild",
            region = "Andhra Pradesh",
            photoPath = null,
            note = "Fine webbing observed under middle leaves.",
            isUncertain = false,
            classId = "tomato_spider_mites",
            doseVerified = true,
            activeIngredient = "Spiromesifen 22.9% SC",
            productExample = "Oberon",
            verifiedDose = "follow the product label"
        )

        val shareText = CaseReportRepository.formatShareText(report)

        // Must contain standard F9 fields
        assertTrue("Report must contain Crop", shareText.contains("Crop: Tomato"))
        assertTrue("Report must contain Finding", shareText.contains("Finding: Tomato Spider Mites"))
        assertTrue("Report must contain Type", shareText.contains("Type: Pest"))
        assertTrue("Report must contain Certainty", shareText.contains("Certainty: Probably"))
        assertTrue("Report must contain Severity", shareText.contains("Severity: Mild"))
        assertTrue("Report must contain Photo status None when null", shareText.contains("Photo: None"))
        assertTrue("Report must contain Note", shareText.contains("Note: Fine webbing observed under middle leaves."))

        // When doseVerified is true, verified treatment is included
        assertTrue("Must contain Treatment section", shareText.contains("Treatment:"))
        assertTrue("Must contain active ingredient", shareText.contains("Spiromesifen 22.9% SC"))
        assertTrue("Must contain product example", shareText.contains("Oberon"))
        assertTrue("Must contain verified dose instruction", shareText.contains("Dose: follow the product label"))
    }

    @Test
    fun testReportIgnoresChemicalFieldsIfDoseVerifiedIsFalse() {
        // Even if chemical fields are accidentally set, doseVerified=false must suppress them
        val report = CaseReport(
            id = "case-suppress-003",
            timestamp = 1731651000000L,
            dateString = "15 Nov 2026, 01:30 PM",
            crop = "Maize",
            problemLabel = "Maize Fall Armyworm",
            type = "Pest",
            certaintyWord = "Sure",
            severity = "Severe",
            region = "Telangana",
            photoPath = "/path/to/photo.jpg",
            note = "Larvae in central whorls.",
            isUncertain = false,
            classId = "maize_fall_armyworm",
            doseVerified = false, // UNVERIFIED DOSE
            activeIngredient = "Chlorantraniliprole 18.5% SC",
            productExample = "Coragen",
            verifiedDose = "0.4 ml/L"
        )

        val shareText = CaseReportRepository.formatShareText(report)

        assertFalse("Must NOT contain active ingredient when doseVerified is false", shareText.contains("Chlorantraniliprole"))
        assertFalse("Must NOT contain product example when doseVerified is false", shareText.contains("Coragen"))
        assertFalse("Must NOT contain dose when doseVerified is false", shareText.contains("0.4 ml/L"))
        assertFalse("Must NOT contain Treatment header when doseVerified is false", shareText.contains("Treatment:"))
    }

    @Test
    fun testUncertainCaseReportFormat() {
        val report = CaseReport(
            id = "case-uncertain-004",
            timestamp = 1731651500000L,
            dateString = "15 Nov 2026, 01:45 PM",
            crop = "—",
            problemLabel = "Can't tell – no disease identified",
            type = "N/A",
            certaintyWord = "Not sure",
            severity = "N/A",
            region = "Telangana",
            photoPath = null,
            note = "Leaf covered in dust near farm road.",
            isUncertain = true,
            classId = "unknown"
        )

        val shareText = CaseReportRepository.formatShareText(report)

        assertTrue(shareText.contains("Finding: Can't tell – no disease identified"))
        assertTrue(shareText.contains("Type: N/A"))
        assertTrue(shareText.contains("Certainty: Not sure"))
        assertFalse("Uncertain case must not show severity", shareText.contains("Severity:"))
        assertFalse("Uncertain case must not have treatment", shareText.contains("Treatment:"))
        assertTrue(shareText.contains("Photo: None"))
        assertTrue(shareText.contains("Note: Leaf covered in dust near farm road."))
    }

    @Test
    fun testFarmerNoteSanitizesAccidentalDosageWhenUnverified() {
        val report = CaseReport(
            id = "case-note-005",
            timestamp = 1731652000000L,
            dateString = "15 Nov 2026, 02:00 PM",
            crop = "Chilli",
            problemLabel = "Chilli Bacterial Spot",
            type = "Disease",
            certaintyWord = "Sure",
            severity = "Moderate",
            region = "Telangana",
            photoPath = null,
            note = "Farmer asked if 2.5 g/L copper spray is okay",
            isUncertain = false,
            classId = "chilli_bacterial_spot",
            doseVerified = false
        )

        val shareText = CaseReportRepository.formatShareText(report)

        assertFalse("Unverified report note must sanitize raw dosage number", shareText.contains("2.5 g/L"))
        assertTrue("Sanitized note replaces raw dose with label instruction", shareText.contains("follow the product label"))
    }

    @Test
    fun testCaseReportAdvisorSourceField() {
        val reportLlama = CaseReport(
            id = "case-adv-001",
            dateString = "21 Nov 2026, 10:00 AM",
            crop = "Tomato",
            problemLabel = "Tomato Early Blight",
            certaintyWord = "Sure",
            severity = "Mild",
            region = "Telangana",
            advisorSource = "LLAMA"
        )
        assertEquals("LLAMA", reportLlama.advisorSource)
        val shareTextLlama = CaseReportRepository.formatShareText(reportLlama)
        assertTrue(shareTextLlama.contains("Advisor: LLAMA"))

        val reportOffline = CaseReport(
            id = "case-adv-002",
            dateString = "21 Nov 2026, 10:05 AM",
            crop = "Rice",
            problemLabel = "Rice Blast",
            certaintyWord = "Probably",
            severity = "Moderate",
            region = "Andhra Pradesh",
            advisorSource = "OFFLINE_GUIDE"
        )
        assertEquals("OFFLINE_GUIDE", reportOffline.advisorSource)
        val shareTextOffline = CaseReportRepository.formatShareText(reportOffline)
        assertTrue(shareTextOffline.contains("Advisor: OFFLINE_GUIDE"))
    }
}
