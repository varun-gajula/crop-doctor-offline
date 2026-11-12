package com.cropdoctor

import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.serialization.json.Json
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
}
