package com.cropdoctor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import org.junit.Assert.*
import org.junit.Test

class RulesEngineTest {

    @Test
    fun testCertaintyCalculation() {
        assertEquals(CertaintyBand.SURE, RulesEngine.calculateCertainty(0.95f))
        assertEquals(CertaintyBand.SURE, RulesEngine.calculateCertainty(0.80f))
        assertEquals(CertaintyBand.PROBABLY, RulesEngine.calculateCertainty(0.79f))
        assertEquals(CertaintyBand.PROBABLY, RulesEngine.calculateCertainty(0.60f))
        assertEquals(CertaintyBand.NOT_SURE, RulesEngine.calculateCertainty(0.59f))
        assertEquals(CertaintyBand.NOT_SURE, RulesEngine.calculateCertainty(0.0f))
    }

    @Test
    fun testSeverityCalculation() {
        // Healthy plants always mild
        assertEquals(SeverityBand.MILD, RulesEngine.calculateSeverity(0.45f, "rice_healthy"))

        // Standard lesion thresholds: < 10% mild, 10-30% moderate, > 30% severe
        assertEquals(SeverityBand.MILD, RulesEngine.calculateSeverity(0.05f, "rice_blast"))
        assertEquals(SeverityBand.MILD, RulesEngine.calculateSeverity(0.099f, "rice_blast"))
        assertEquals(SeverityBand.MODERATE, RulesEngine.calculateSeverity(0.10f, "rice_blast"))
        assertEquals(SeverityBand.MODERATE, RulesEngine.calculateSeverity(0.25f, "rice_blast"))
        assertEquals(SeverityBand.MODERATE, RulesEngine.calculateSeverity(0.30f, "rice_blast"))
        assertEquals(SeverityBand.SEVERE, RulesEngine.calculateSeverity(0.31f, "rice_blast"))
        assertEquals(SeverityBand.SEVERE, RulesEngine.calculateSeverity(0.75f, "rice_blast"))
    }

    @Test
    fun testDetermineOpenTier() {
        // mild -> natural tier opened first
        assertEquals(RulesEngine.OpenTier.NATURAL_FIRST, RulesEngine.determineOpenTier(SeverityBand.MILD))
        // moderate -> both shown
        assertEquals(RulesEngine.OpenTier.BOTH_SHOWN, RulesEngine.determineOpenTier(SeverityBand.MODERATE))
        // severe -> chemical tier + extension worker first
        assertEquals(RulesEngine.OpenTier.CHEMICAL_FIRST, RulesEngine.determineOpenTier(SeverityBand.SEVERE))
    }

    @Test
    fun testExtensionWorkerReferral() {
        // Not sure or severe -> red button
        assertTrue(RulesEngine.shouldShowExtensionWorker(CertaintyBand.NOT_SURE, SeverityBand.MILD))
        assertTrue(RulesEngine.shouldShowExtensionWorker(CertaintyBand.NOT_SURE, SeverityBand.MODERATE))
        assertTrue(RulesEngine.shouldShowExtensionWorker(CertaintyBand.NOT_SURE, SeverityBand.SEVERE))
        assertTrue(RulesEngine.shouldShowExtensionWorker(CertaintyBand.SURE, SeverityBand.SEVERE))
        assertTrue(RulesEngine.shouldShowExtensionWorker(CertaintyBand.PROBABLY, SeverityBand.SEVERE))

        // Sure/Probably with Mild/Moderate -> no forced extension button
        assertFalse(RulesEngine.shouldShowExtensionWorker(CertaintyBand.SURE, SeverityBand.MILD))
        assertFalse(RulesEngine.shouldShowExtensionWorker(CertaintyBand.SURE, SeverityBand.MODERATE))
        assertFalse(RulesEngine.shouldShowExtensionWorker(CertaintyBand.PROBABLY, SeverityBand.MILD))
        assertFalse(RulesEngine.shouldShowExtensionWorker(CertaintyBand.PROBABLY, SeverityBand.MODERATE))
    }

    @Test
    fun testExtensionOfficerMetadata() {
        assertEquals("18001801551", RulesEngine.EXTENSION_OFFICER_NUMBER)
        assertEquals("Kisan Call Centre", RulesEngine.EXTENSION_OFFICER_LABEL)
        assertFalse("Must not be labelled KVK", RulesEngine.EXTENSION_OFFICER_LABEL.contains("KVK"))
    }

    @Test
    fun testActionSentenceMapping() {
        assertEquals(R.string.severity_early_warning, RulesEngine.getSeverityActionSentence(SeverityBand.MILD))
        assertEquals(R.string.severity_act_now, RulesEngine.getSeverityActionSentence(SeverityBand.MODERATE))
        assertEquals(R.string.severity_act_now, RulesEngine.getSeverityActionSentence(SeverityBand.SEVERE))
    }

    @Test
    fun testNotSureSuppressesSeverityAndEarlyWarning() {
        // T2b Fix (1): When certainty is "Not sure", do not show severity or early warning
        assertFalse("Severity must not be shown when Not sure", RulesEngine.shouldShowSeverity(CertaintyBand.NOT_SURE))
        assertFalse("Early warning must not be shown when Not sure", RulesEngine.shouldShowEarlyWarning(CertaintyBand.NOT_SURE, SeverityBand.MILD))
        assertFalse("Early warning must not be shown when Not sure (moderate)", RulesEngine.shouldShowEarlyWarning(CertaintyBand.NOT_SURE, SeverityBand.MODERATE))
        assertFalse("Early warning must not be shown when Not sure (severe)", RulesEngine.shouldShowEarlyWarning(CertaintyBand.NOT_SURE, SeverityBand.SEVERE))

        // When certainty is Not sure, guidance sentence must be "Can't tell - no disease identified"
        assertEquals(R.string.cant_tell_no_disease, RulesEngine.getGuidanceSentence(CertaintyBand.NOT_SURE, SeverityBand.MILD))
        assertEquals(R.string.cant_tell_no_disease, RulesEngine.getGuidanceSentence(CertaintyBand.NOT_SURE, SeverityBand.MODERATE))
        assertEquals(R.string.cant_tell_no_disease, RulesEngine.getGuidanceSentence(CertaintyBand.NOT_SURE, SeverityBand.SEVERE))

        // When certainty is SURE or PROBABLY, severity is shown
        assertTrue("Severity must be shown when SURE", RulesEngine.shouldShowSeverity(CertaintyBand.SURE))
        assertTrue("Severity must be shown when PROBABLY", RulesEngine.shouldShowSeverity(CertaintyBand.PROBABLY))
        assertTrue("Early warning must be shown for MILD when SURE", RulesEngine.shouldShowEarlyWarning(CertaintyBand.SURE, SeverityBand.MILD))
        assertTrue("Early warning must be shown for MILD when PROBABLY", RulesEngine.shouldShowEarlyWarning(CertaintyBand.PROBABLY, SeverityBand.MILD))
        assertFalse("Early warning not shown for MODERATE", RulesEngine.shouldShowEarlyWarning(CertaintyBand.SURE, SeverityBand.MODERATE))

        assertEquals(R.string.severity_early_warning, RulesEngine.getGuidanceSentence(CertaintyBand.SURE, SeverityBand.MILD))
        assertEquals(R.string.severity_act_now, RulesEngine.getGuidanceSentence(CertaintyBand.SURE, SeverityBand.MODERATE))
        assertEquals(R.string.severity_act_now, RulesEngine.getGuidanceSentence(CertaintyBand.SURE, SeverityBand.SEVERE))
    }
}
