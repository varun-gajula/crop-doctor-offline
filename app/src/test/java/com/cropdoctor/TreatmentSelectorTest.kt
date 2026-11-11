package com.cropdoctor

import com.cropdoctor.domain.SeverityBand
import com.cropdoctor.domain.TreatmentSelector
import org.junit.Assert.assertEquals
import org.junit.Test

class TreatmentSelectorTest {

    @Test
    fun testMildSeverityOpensNaturalTabFirst() {
        val tab = TreatmentSelector.getInitialTab(SeverityBand.MILD, "rice_bacterial_leaf_blight")
        assertEquals(TreatmentSelector.TreatmentTab.NATURAL, tab)
    }

    @Test
    fun testModerateAndSevereOpenChemicalTabFirst() {
        val tabMod = TreatmentSelector.getInitialTab(SeverityBand.MODERATE, "rice_leaf_blast")
        assertEquals(TreatmentSelector.TreatmentTab.CHEMICAL, tabMod)

        val tabSev = TreatmentSelector.getInitialTab(SeverityBand.SEVERE, "tomato_late_blight")
        assertEquals(TreatmentSelector.TreatmentTab.CHEMICAL, tabSev)
    }

    @Test
    fun testHealthyAlwaysOpensNaturalTabFirst() {
        val tab = TreatmentSelector.getInitialTab(SeverityBand.MILD, "cotton_healthy")
        assertEquals(TreatmentSelector.TreatmentTab.NATURAL, tab)
    }
}

