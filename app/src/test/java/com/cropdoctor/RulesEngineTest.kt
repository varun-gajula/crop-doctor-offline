package com.cropdoctor

import com.cropdoctor.domain.CertaintyBand
import com.cropdoctor.domain.RulesEngine
import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.LocalizedText
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

    @Test
    fun testBuildAdvisoryTextNotSureRemovesSeverityAndImmediateStep() {
        val entry = KbEntry(
            classId = "rice_bacterial_leaf_blight",
            index = 0,
            crop = "rice",
            type = "disease",
            pathogen = "Xanthomonas oryzae",
            names = LocalizedText(
                en = "Rice Bacterial Leaf Blight",
                te = "వరి బాక్టీరియల్ ఆకు ఎండు తెగులు",
                hi = "धान का जीवाणु पत्ती झुलसा"
            ),
            immediateStep = LocalizedText(
                en = "Drain excess water immediately.",
                te = "వెంటనే అదనపు నీటిని బయటకు పంపండి.",
                hi = "तुरंत अतिरिक्त पानी निकालें।"
            ),
            explainText = LocalizedText(
                en = "The condition could not be identified with certainty. Please ask an agriculture extension officer or visit your nearest Krishi Vigyan Kendra.",
                te = "పరిస్థితిని ఖచ్చితంగా గుర్తించలేకపోయాము. దయచేసి వ్యవసాయ విస్తరణ అధికారిని లేదా కృషి విజ్ఞాన కేంద్రాన్ని సంప్రదించండి.",
                hi = "स्थिति की निश्चित पहचान नहीं हो सकी। कृपया कृषि विस्तार अधिकारी या नजदीकी कृषि विज्ञान केंद्र से संपर्क करें।"
            ),
            severityAdvice = mapOf(
                "MILD" to LocalizedText(en = "Monitor field weekly.", te = "వారానికి ఒకసారి పొలాన్ని గమనించండి.", hi = "साप्ताहिक निरीक्षण करें।"),
                "MODERATE" to LocalizedText(en = "Apply bio-control agents.", te = "జీవ నియంత్రణ మందులు వాడండి.", hi = "जैव नियंत्रण एजेंटों का उपयोग करें।"),
                "SEVERE" to LocalizedText(en = "Spray recommended bactericide.", te = "సిఫార్సు చేసిన బాక్టీరియా నాశిని వాడండి.", hi = "अनुशंसित जीवाणुनाशक का छिड़काव करें।")
            )
        )

        // 1. English: When certainty is NOT_SURE, NO severity line (including "Severity Guidance (MILD)") and NO immediate step
        val enAdvice = RulesEngine.buildAdvisoryText(
            classId = "unknown",
            certaintyBand = CertaintyBand.NOT_SURE,
            severityBand = SeverityBand.MILD,
            language = "en",
            kbEntry = entry
        )
        assertEquals(entry.explainText.en, enAdvice)
        assertFalse("Advisory must NOT contain 'Severity Guidance' when Not sure", enAdvice.contains("Severity Guidance", ignoreCase = true))
        assertFalse("Advisory must NOT contain 'Severity Guidance (MILD)' when Not sure", enAdvice.contains("Severity Guidance (MILD)", ignoreCase = true))
        assertFalse("Advisory must NOT contain 'Immediate Step' when Not sure", enAdvice.contains("Immediate Step", ignoreCase = true))
        assertFalse("Advisory must NOT contain 'MILD' when Not sure", enAdvice.contains("MILD"))

        // 2. Telugu: When certainty is NOT_SURE
        val teAdvice = RulesEngine.buildAdvisoryText(
            classId = "unknown",
            certaintyBand = CertaintyBand.NOT_SURE,
            severityBand = SeverityBand.MILD,
            language = "te",
            kbEntry = entry
        )
        assertEquals(entry.explainText.te, teAdvice)
        assertFalse("Advisory must NOT contain 'నివారణ సలహా' when Not sure", teAdvice.contains("నివారణ సలహా"))
        assertFalse("Advisory must NOT contain 'తక్షణ చర్య' when Not sure", teAdvice.contains("తక్షణ చర్య"))

        // 3. Hindi: When certainty is NOT_SURE
        val hiAdvice = RulesEngine.buildAdvisoryText(
            classId = "unknown",
            certaintyBand = CertaintyBand.NOT_SURE,
            severityBand = SeverityBand.MILD,
            language = "hi",
            kbEntry = entry
        )
        assertEquals(entry.explainText.hi, hiAdvice)
        assertFalse("Advisory must NOT contain 'सलाह' when Not sure", hiAdvice.contains("सलाह"))
        assertFalse("Advisory must NOT contain 'तत्काल कदम' when Not sure", hiAdvice.contains("तत्काल कदम"))

        // 4. When certainty is SURE: advisory DOES contain Immediate Step and Severity Guidance
        val sureAdvice = RulesEngine.buildAdvisoryText(
            classId = "rice_bacterial_leaf_blight",
            certaintyBand = CertaintyBand.SURE,
            severityBand = SeverityBand.MILD,
            language = "en",
            kbEntry = entry
        )
        assertTrue("Advisory must contain Immediate Step when SURE", sureAdvice.contains("Immediate Step: Drain excess water immediately."))
        assertTrue("Advisory must contain Severity Guidance when SURE", sureAdvice.contains("Severity Guidance (MILD): Monitor field weekly."))
    }
}
