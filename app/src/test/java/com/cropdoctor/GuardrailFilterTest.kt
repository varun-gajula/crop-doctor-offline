package com.cropdoctor

import com.cropdoctor.advisor.GuardrailFilter
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.ChemicalStep
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.LocalizedText
import com.cropdoctor.knowledge.TreatmentPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardrailFilterTest {

    private val mockKbEntry = KbEntry(
        classId = "rice_bacterial_leaf_blight",
        index = 0,
        crop = "rice",
        type = "disease",
        pathogen = "Xanthomonas oryzae",
        names = LocalizedText(en = "Bacterial Leaf Blight", te = "బాక్టీరియల్ ఆకు ఎండు తెగులు", hi = "जीवाणु पत्ती झुलसा"),
        immediateStep = LocalizedText(en = "Drain standing water immediately", te = "నిలిచిన నీటిని తీసివేయండి", hi = "खेत से पानी निकालें"),
        explainText = LocalizedText(en = "Bacterial infection causing wilting", te = "బాక్టీరియా వల్ల ఆకులు ఎండుతాయి", hi = "जीवाणु संक्रमण"),
        treatment = TreatmentPlan(
            natural = emptyList(),
            chemical = listOf(
                ChemicalStep(
                    step = 1,
                    activeIngredient = "Copper oxychloride",
                    productExample = "Blitox 50 WP",
                    dose = "follow the product label",
                    doseVerified = false,
                    safetyLine = LocalizedText(en = "Wear mask", te = "మాస్క్ ధరించండి", hi = "मास्क पहनें"),
                    source = "ICAR Rice Handbook",
                    sourceUrl = "https://icar.gov.in",
                    sourceQuote = "Spray copper oxychloride"
                )
            )
        )
    )

    @Test
    fun testTextWithoutDosesIsUnchanged() {
        val safeText = "This crop shows bacterial blight. Please spray copper oxychloride and drain standing water."
        assertEquals(safeText, GuardrailFilter.sanitize(safeText))
    }

    @Test
    fun testFiltersMilliliterPerLiter() {
        val hallucinated = "Spray copper oxychloride at 2.5 ml/L of water early in the morning."
        val cleaned = GuardrailFilter.sanitize(hallucinated)
        assertFalse(cleaned.contains("2.5 ml/L"))
        assertEquals("Spray copper oxychloride at follow the product label early in the morning.", cleaned)
    }

    @Test
    fun testFiltersKilogramPerAcre() {
        val hallucinated = "Apply carbendazim at 2 kg per acre as a soil drench."
        val cleaned = GuardrailFilter.sanitize(hallucinated)
        assertFalse(cleaned.contains("2 kg per acre"))
        assertEquals("Apply carbendazim at follow the product label as a soil drench.", cleaned)
    }

    @Test
    fun testTeluguAndHindiTextPassThroughSafely() {
        val teText = "పంటపై కాపర్ ఆక్సీక్లోరైడ్ పిచికారీ చేయండి."
        assertEquals(teText, GuardrailFilter.sanitize(teText))

        val hiText = "फसल पर कॉपर ऑक्सीक्लोराइड का छिड़काव करें।"
        assertEquals(hiText, GuardrailFilter.sanitize(hiText))
    }

    @Test
    fun testValidationPassesForApprovedAdvice() {
        val safeAdvice = "The rice leaf shows bacterial blight. Drain standing water and spray copper oxychloride as per the product label."
        val result = GuardrailFilter.validateAdvice(safeAdvice, mockKbEntry, SeverityBand.MODERATE)
        assertTrue(result.isValid)
    }

    @Test
    fun testValidationRejectsHallucinatedDose() {
        val unsafeAdvice = "Spray copper oxychloride at 3 ml per liter of water."
        val result = GuardrailFilter.validateAdvice(unsafeAdvice, mockKbEntry, SeverityBand.MODERATE)
        assertFalse(result.isValid)
        assertTrue(result.failureReason?.contains("dosage") == true)
    }

    @Test
    fun testValidationRejectsUnapprovedChemical() {
        // Carbendazim is NOT in mockKbEntry (only Copper oxychloride is)
        val unsafeAdvice = "Apply carbendazim immediately to control the infection."
        val result = GuardrailFilter.validateAdvice(unsafeAdvice, mockKbEntry, SeverityBand.MODERATE)
        assertFalse(result.isValid)
        assertTrue(result.failureReason?.contains("unapproved") == true)
    }

    @Test
    fun testValidationRejectsSeverityContradiction() {
        // Declaring extreme catastrophe when severity is MILD
        val contradictoryAdvice = "This is a severe emergency! Destroy all plants in the field immediately!"
        val result = GuardrailFilter.validateAdvice(contradictoryAdvice, mockKbEntry, SeverityBand.MILD)
        assertFalse(result.isValid)
        assertTrue(result.failureReason?.contains("MILD") == true)

        // Declaring harmlessness when severity is SEVERE
        val harmlessAdvice = "This is a harmless condition, no action needed, do not worry."
        val severeResult = GuardrailFilter.validateAdvice(harmlessAdvice, mockKbEntry, SeverityBand.SEVERE)
        assertFalse(severeResult.isValid)
        assertTrue(severeResult.failureReason?.contains("SEVERE") == true)
    }
}
