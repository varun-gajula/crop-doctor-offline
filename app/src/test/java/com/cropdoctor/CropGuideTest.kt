package com.cropdoctor

import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class CropGuideTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private lateinit var root: KnowledgeBaseRoot

    @Before
    fun setUp() {
        val candidates = listOf(
            File("src/main/assets/kb/knowledge_base.json"),
            File("../app/src/main/assets/kb/knowledge_base.json"),
            File("data/kb/knowledge_base.json"),
            File("../data/kb/knowledge_base.json")
        )
        val kbFile = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find knowledge_base.json")
        root = json.decodeFromString(kbFile.readText())
    }

    private fun getProblemsForCrop(crop: String): List<KbEntry> {
        return root.classes.filter {
            it.crop.equals(crop, ignoreCase = true) && it.type != "healthy"
        }
    }

    private fun groupProblemsByStage(problems: List<KbEntry>): Map<String, List<KbEntry>> {
        val stageOrder = listOf(
            "Seedling",
            "Vegetative",
            "Flowering",
            "Fruiting or Grain filling",
            "Any stage"
        )
        val map = LinkedHashMap<String, MutableList<KbEntry>>()
        for (st in stageOrder) map[st] = mutableListOf()

        for (p in problems) {
            val key = when (p.stage) {
                "Seedling" -> "Seedling"
                "Vegetative" -> "Vegetative"
                "Flowering" -> "Flowering"
                "Fruiting or Grain filling", "Grain filling", "Fruiting" -> "Fruiting or Grain filling"
                else -> "Any stage"
            }
            map.getOrPut(key) { mutableListOf() }.add(p)
        }
        return map.filter { it.value.isNotEmpty() }
    }

    @Test
    fun testAllFiveCropsHaveDocumentedProblems() {
        val expectedCounts = mapOf(
            "rice" to 4,
            "cotton" to 2,
            "chilli" to 2,
            "tomato" to 4,
            "maize" to 4
        )
        var totalProblems = 0
        for ((crop, expectedCount) in expectedCounts) {
            val problems = getProblemsForCrop(crop)
            assertEquals("Problem count mismatch for crop $crop", expectedCount, problems.size)
            totalProblems += problems.size
        }
        assertEquals("Total problems across 5 crops must be 16", 16, totalProblems)
    }

    @Test
    fun testHealthyClassesExcludedFromProblemList() {
        val crops = listOf("rice", "cotton", "chilli", "tomato", "maize")
        for (crop in crops) {
            val problems = getProblemsForCrop(crop)
            for (p in problems) {
                assertNotEquals("Healthy class must not be listed as a problem: ${p.classId}", "healthy", p.type)
                assertTrue("Problem type must be disease or pest: ${p.classId}", p.type == "disease" || p.type == "pest")
            }
        }
    }

    @Test
    fun testEntriesWithSourcedStage() {
        val expectedStages = mapOf(
            "rice_bacterial_leaf_blight" to "Seedling",
            "rice_brown_spot" to "Fruiting or Grain filling",
            "rice_sheath_blight" to "Fruiting or Grain filling",
            "cotton_bacterial_blight" to "Vegetative",
            "chilli_leaf_curl_virus" to "Flowering",
            "tomato_yellow_leaf_curl_virus" to "Seedling",
            "maize_fall_armyworm" to "Vegetative"
        )

        for ((classId, expectedStage) in expectedStages) {
            val entry = root.classes.firstOrNull { it.classId == classId }
            assertNotNull("Entry $classId must exist in KB", entry)
            assertEquals(
                "Entry $classId stage must match sourced official stage",
                expectedStage,
                entry?.stage
            )
        }
    }

    @Test
    fun testEntriesWithoutStageMapToAnyStage() {
        val unconstrainedClasses = listOf(
            "rice_leaf_blast",
            "cotton_fusarium_wilt",
            "chilli_bacterial_spot",
            "tomato_early_blight",
            "tomato_late_blight",
            "tomato_spider_mites",
            "maize_gray_leaf_spot",
            "maize_common_rust",
            "maize_northern_leaf_blight"
        )

        val problems = root.classes.filter { it.type != "healthy" }
        for (p in problems) {
            if (p.classId in unconstrainedClasses) {
                assertNull("Class ${p.classId} must have null stage per Rule H4", p.stage)
            }
        }

        // Test grouping
        val groupedAll = groupProblemsByStage(problems)
        val anyStageList = groupedAll["Any stage"]?.map { it.classId } ?: emptyList()
        for (cid in unconstrainedClasses) {
            assertTrue("Class $cid must be grouped under 'Any stage'", anyStageList.contains(cid))
        }
        assertEquals("Any stage group must contain exactly 9 unconstrained classes", 9, anyStageList.size)
    }

    @Test
    fun testGroupingPreservesAllEntriesWithoutLoss() {
        val crops = listOf("rice", "cotton", "chilli", "tomato", "maize")
        for (crop in crops) {
            val problems = getProblemsForCrop(crop)
            val grouped = groupProblemsByStage(problems)
            val flattenedCount = grouped.values.sumOf { it.size }
            assertEquals("Grouping must not drop or duplicate entries for crop $crop", problems.size, flattenedCount)
        }
    }

    @Test
    fun testTeluguTextPresentForEveryProblem() {
        val problems = root.classes.filter { it.type != "healthy" }
        for (p in problems) {
            assertFalse("Telugu name missing for ${p.classId}", p.names.te.isBlank())
            assertFalse("Telugu immediateStep missing for ${p.classId}", p.immediateStep.te.isBlank())
            assertFalse("Telugu explainText missing for ${p.classId}", p.explainText.te.isBlank())
            assertFalse("Telugu referral missing for ${p.classId}", p.whenToCallExtensionOfficer.te.isBlank())
        }
    }

    @Test
    fun testSourcesPresentForTreatmentTiers() {
        val problems = root.classes.filter { it.type != "healthy" }
        for (p in problems) {
            // Natural treatments must cite source
            assertTrue("${p.classId} must have natural treatments", p.treatment.natural.isNotEmpty())
            for (step in p.treatment.natural) {
                assertFalse("Natural treatment source missing for ${p.classId}", step.source.isBlank())
            }
            // Chemical treatments must cite source
            assertTrue("${p.classId} must have chemical treatments", p.treatment.chemical.isNotEmpty())
            for (step in p.treatment.chemical) {
                assertFalse("Chemical treatment source missing for ${p.classId}", step.source.isBlank())
            }
        }
    }
}
