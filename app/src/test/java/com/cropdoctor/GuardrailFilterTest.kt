package com.cropdoctor

import com.cropdoctor.advisor.GuardrailFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GuardrailFilterTest {

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
}
