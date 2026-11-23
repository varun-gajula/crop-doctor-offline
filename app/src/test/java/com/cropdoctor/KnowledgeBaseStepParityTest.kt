package com.cropdoctor

import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class KnowledgeBaseStepParityTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun loadKnowledgeBase(): KnowledgeBaseRoot {
        val candidates = listOf(
            File("src/main/assets/kb/knowledge_base.json"),
            File("../data/kb/knowledge_base.json"),
            File("data/kb/knowledge_base.json")
        )
        val kbFile = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find knowledge_base.json in candidates: $candidates")
        return json.decodeFromString(kbFile.readText())
    }

    @Test
    fun testAllClassesHaveMultilingualMetadata() {
        val root = loadKnowledgeBase()
        assertTrue("Must have classes in KB", root.classes.isNotEmpty())

        for (entry in root.classes) {
            val classId = entry.classId

            // Check names in en, te, hi
            assertFalse("names.en missing in $classId", entry.names.en.isBlank())
            assertFalse("names.te missing in $classId", entry.names.te.isBlank())
            assertFalse("names.hi missing in $classId", entry.names.hi.isBlank())

            // Check immediate_step
            assertFalse("immediate_step.en missing in $classId", entry.immediateStep.en.isBlank())
            assertFalse("immediate_step.te missing in $classId", entry.immediateStep.te.isBlank())
            assertFalse("immediate_step.hi missing in $classId", entry.immediateStep.hi.isBlank())

            // Check explain_text
            assertFalse("explain_text.en missing in $classId", entry.explainText.en.isBlank())
            assertFalse("explain_text.te missing in $classId", entry.explainText.te.isBlank())
            assertFalse("explain_text.hi missing in $classId", entry.explainText.hi.isBlank())

            // Check severity advice
            for (band in listOf("MILD", "MODERATE", "SEVERE")) {
                val bandObj = entry.severityAdvice[band]
                assertNotNull("severityAdvice[$band] missing in $classId", bandObj)
                assertFalse("severity_advice.$band.en missing in $classId", bandObj!!.en.isBlank())
                assertFalse("severity_advice.$band.te missing in $classId", bandObj.te.isBlank())
                assertFalse("severity_advice.$band.hi missing in $classId", bandObj.hi.isBlank())
            }
        }
    }

    @Test
    fun testTreatmentStepParityAcrossLanguages() {
        val root = loadKnowledgeBase()

        for (entry in root.classes) {
            val classId = entry.classId

            // Natural treatment steps
            val naturalSteps = entry.treatment.natural
            val naturalEnSteps = mutableListOf<Int>()
            val naturalTeSteps = mutableListOf<Int>()
            val naturalHiSteps = mutableListOf<Int>()

            for (step in naturalSteps) {
                assertFalse("Empty en action for $classId step ${step.step}", step.action.en.isBlank())
                assertFalse("Empty te action for $classId step ${step.step}", step.action.te.isBlank())
                assertFalse("Empty hi action for $classId step ${step.step}", step.action.hi.isBlank())

                naturalEnSteps.add(step.step)
                naturalTeSteps.add(step.step)
                naturalHiSteps.add(step.step)
            }

            assertEquals(
                "Natural step sequence must be identical for en and te in $classId",
                naturalEnSteps,
                naturalTeSteps
            )
            assertEquals(
                "Natural step sequence must be identical for en and hi in $classId",
                naturalEnSteps,
                naturalHiSteps
            )

            // Chemical treatment steps
            val chemicalSteps = entry.treatment.chemical
            val chemicalEnSteps = mutableListOf<Int>()
            val chemicalTeSteps = mutableListOf<Int>()
            val chemicalHiSteps = mutableListOf<Int>()

            for (step in chemicalSteps) {
                assertFalse("Empty en safety line for $classId step ${step.step}", step.safetyLine.en.isBlank())
                assertFalse("Empty te safety line for $classId step ${step.step}", step.safetyLine.te.isBlank())
                assertFalse("Empty hi safety line for $classId step ${step.step}", step.safetyLine.hi.isBlank())

                chemicalEnSteps.add(step.step)
                chemicalTeSteps.add(step.step)
                chemicalHiSteps.add(step.step)
            }

            assertEquals(
                "Chemical step sequence must be identical for en and te in $classId",
                chemicalEnSteps,
                chemicalTeSteps
            )
            assertEquals(
                "Chemical step sequence must be identical for en and hi in $classId",
                chemicalEnSteps,
                chemicalHiSteps
            )
        }
    }

    @Test
    fun testTaskKB15FourteenClassesCoverageAndProvenance() {
        val root = loadKnowledgeBase()
        val target14 = listOf(
            "rice_leaf_blast",
            "rice_healthy",
            "cotton_bacterial_blight",
            "cotton_healthy",
            "chilli_leaf_curl_virus",
            "chilli_healthy",
            "maize_northern_leaf_blight",
            "maize_common_rust",
            "maize_healthy",
            "maize_fall_armyworm",
            "rice_stem_borer",
            "rice_hispa",
            "tomato_healthy",
            "tomato_septoria_leaf_spot"
        )

        val kbMap = root.classes.associateBy { it.classId }

        val candidates = listOf(
            File("src/main/assets/kb/knowledge_base.json"),
            File("../data/kb/knowledge_base.json"),
            File("data/kb/knowledge_base.json")
        )
        val kbFile = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Could not find knowledge_base.json")
        val rawObj = Json.parseToJsonElement(kbFile.readText()).jsonObject
        val rawClasses = rawObj["classes"]!!.jsonArray.map { it.jsonObject }.associateBy { it["class_id"]!!.jsonPrimitive.content }

        for (classId in target14) {
            val entry = kbMap[classId]
            assertNotNull("Class $classId must exist in knowledge base", entry)
            val rawClass = rawClasses[classId]
            assertNotNull("Class $classId must exist in raw JSON", rawClass)

            // needs_expert_review must be true
            assertTrue("Class $classId must have needs_expert_review: true", entry!!.needsExpertReview)

            // Provenance source check
            val source = rawClass!!["source"]?.jsonPrimitive?.content
            val sourceUrl = rawClass["source_url"]?.jsonPrimitive?.content
            val dateRead = rawClass["date_read"]?.jsonPrimitive?.content

            if (source.isNullOrBlank()) {
                val immed = entry.immediateStep.en
                assertTrue(
                    "Entry $classId without source must show 'Advice not available, ask an extension officer'",
                    immed.contains("Advice not available, ask an extension officer", ignoreCase = true) ||
                            immed.contains("Advice for this problem is not available yet", ignoreCase = true)
                )
            } else {
                assertTrue("Source for $classId must cite recognized authority",
                    listOf("ICAR", "ANGRAU", "PJTSAU", "NIPHM", "NBAIR", "IIHR", "TNAU").any { source.contains(it) }
                )
                assertNotNull("source_url must not be null for $classId", sourceUrl)
                assertTrue("source_url must be a valid URL for $classId", sourceUrl!!.startsWith("http"))
                assertNotNull("date_read must not be null for $classId", dateRead)
                assertTrue("date_read must match YYYY-MM-DD for $classId", Regex("""^\d{4}-\d{2}-\d{2}$""").matches(dateRead!!))
            }

            // What it is (explain_text)
            assertFalse("explain_text.en missing in $classId", entry.explainText.en.isBlank())
            assertFalse("explain_text.te missing in $classId", entry.explainText.te.isBlank())
            assertFalse("explain_text.hi missing in $classId", entry.explainText.hi.isBlank())

            // Symptoms (early_warning_signs, severity_advice)
            assertFalse("early_warning_signs.en missing in $classId", entry.earlyWarningSigns.en.isBlank())
            assertFalse("early_warning_signs.te missing in $classId", entry.earlyWarningSigns.te.isBlank())
            assertFalse("early_warning_signs.hi missing in $classId", entry.earlyWarningSigns.hi.isBlank())

            // Prevention
            assertFalse("prevention_and_field_practice.en missing in $classId", entry.preventionAndFieldPractice.en.isBlank())
            assertFalse("prevention_and_field_practice.te missing in $classId", entry.preventionAndFieldPractice.te.isBlank())
            assertFalse("prevention_and_field_practice.hi missing in $classId", entry.preventionAndFieldPractice.hi.isBlank())

            // Natural / traditional
            assertTrue("Natural treatments must be present for $classId", entry.treatment.natural.isNotEmpty())
            for (step in entry.treatment.natural) {
                assertFalse("Natural step source missing for $classId", step.source.isBlank())
                assertFalse("Natural step en action missing for $classId", step.action.en.isBlank())
                assertFalse("Natural step te action missing for $classId", step.action.te.isBlank())
                assertFalse("Natural step hi action missing for $classId", step.action.hi.isBlank())
            }

            // Chemical tier
            if (entry.type == "healthy") {
                assertTrue("Healthy class $classId must have no chemical treatments", entry.treatment.chemical.isEmpty())
            } else {
                assertTrue("Non-healthy class $classId must have chemical treatments", entry.treatment.chemical.isNotEmpty())
                for (step in entry.treatment.chemical) {
                    assertFalse("Chemical active ingredient missing for $classId", step.activeIngredient.isBlank())
                    assertFalse("Chemical source missing for $classId", step.source.isBlank())
                    if (!step.doseVerified) {
                        assertEquals("Unverified dose must state 'follow the product label'", "follow the product label", step.dose)
                    }
                }
            }

            // Regional note
            val regional = entry.regional
            assertTrue("Regional telangana missing in $classId", regional.containsKey("telangana"))
            assertEquals("Telangana authority must be PJTSAU", "PJTSAU", regional["telangana"]?.source)
            assertTrue("Regional andhra_pradesh missing in $classId", regional.containsKey("andhra_pradesh"))
            assertEquals("Andhra authority must be ANGRAU", "ANGRAU", regional["andhra_pradesh"]?.source)
            assertTrue("Regional other missing in $classId", regional.containsKey("other"))
            assertEquals("Other authority must be ICAR", "ICAR", regional["other"]?.source)
        }
    }
}
