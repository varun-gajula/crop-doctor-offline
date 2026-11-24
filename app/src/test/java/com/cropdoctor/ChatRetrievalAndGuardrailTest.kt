package com.cropdoctor

import com.cropdoctor.chat.ChatEngine
import com.cropdoctor.chat.ChatGuardrail
import com.cropdoctor.chat.KnowledgeBasePassageIndexer
import com.cropdoctor.chat.QuestionKeywordMatcher
import com.cropdoctor.chat.QuestionSection
import com.cropdoctor.chat.SqliteJdbcPassageStore
import com.cropdoctor.knowledge.KnowledgeBaseRoot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verification test suite for Task CHAT (Offline Chat Box on Result Screen).
 *
 * Verifies:
 * 1. Offline SQLite FTS5 passage retrieval across 5 key classes x 8 question types (40 test points).
 * 2. Guardrail rejection: ungrounded chemical products or doses trigger guardrail failure and fallback to offline guide.
 * 3. No-passage fallback: out-of-domain questions return standard Kisan Call Centre (1800-180-1551) message.
 * 4. Telugu: keyword chip mapping works, and Llama is strictly bypassed (Rule H6).
 * 5. Outcome gating: Chat is disabled for uncertain and out-of-class diagnoses.
 */
class ChatRetrievalAndGuardrailTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private lateinit var passageStore: SqliteJdbcPassageStore
    private lateinit var kbRoot: KnowledgeBaseRoot

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

    @Before
    fun setUp() {
        passageStore = SqliteJdbcPassageStore()
        kbRoot = loadKnowledgeBase()
        val allPassages = KnowledgeBasePassageIndexer.buildAllPassages(kbRoot)
        passageStore.indexPassages(allPassages)
    }

    @After
    fun tearDown() {
        passageStore.close()
    }

    @Test
    fun testPassageIndexCompleteness() {
        val count = passageStore.countPassages()
        // 24 classes x 8 sections x 3 languages = 576 passages
        assertEquals("Expected 576 indexed passages (24 classes * 8 sections * 3 languages)", 576, count)
    }

    /**
     * Requirement 7: Retrieval for 5 classes x 8 question types = 40 combinations.
     * Classes: rice_leaf_blast, cotton_bacterial_blight, chilli_leaf_curl_virus, tomato_septoria_leaf_spot, maize_fall_armyworm
     */
    @Test
    fun testRetrievalFiveClassesEightQuestionTypes() {
        val testClasses = listOf(
            "rice_leaf_blast",
            "cotton_bacterial_blight",
            "chilli_leaf_curl_virus",
            "tomato_septoria_leaf_spot",
            "maize_fall_armyworm"
        )

        val testQuestionTypes = listOf(
            QuestionSection.WHAT_IS_IT to "what is it",
            QuestionSection.SYMPTOMS to "symptoms",
            QuestionSection.WHAT_TO_DO_NOW to "what to do now",
            QuestionSection.PREVENTION to "prevention",
            QuestionSection.NATURAL_REMEDY to "natural remedy",
            QuestionSection.CHEMICAL to "chemical",
            QuestionSection.SAFETY_WAITING_PERIOD to "safety/waiting period",
            QuestionSection.WHEN_TO_CALL_OFFICER to "when to call the officer"
        )

        var totalAssertions = 0

        for (classId in testClasses) {
            for ((expectedSection, query) in testQuestionTypes) {
                val passages = passageStore.getBestPassages(
                    detectedClass = classId,
                    questionKeywords = query,
                    language = "en",
                    limit = 3
                )

                assertFalse("Must find passages for class=$classId section=$expectedSection query='$query'", passages.isEmpty())
                assertTrue("Passages size must be between 1 and 3", passages.size in 1..3)

                val top = passages.first()
                assertEquals("Class ID mismatch for $classId", classId, top.classId)
                assertEquals("Language must be English", "en", top.language)
                assertEquals("Section must match expected $expectedSection for query '$query'", expectedSection, top.section)
                assertFalse("Passage text must not be blank", top.text.isBlank())
                assertFalse("Source attribution must not be blank", top.source.isBlank())

                totalAssertions++
            }
        }

        assertEquals("Must verify all 40 combinations (5 classes * 8 question types)", 40, totalAssertions)
    }

    /**
     * Requirement 2: Guardrail rejects any dose, product or number not in passages.
     * On rejection, show passage text with label "Offline guide".
     */
    @Test
    fun testGuardrailRejectionHallucinatedChemical() = runBlocking {
        val passages = passageStore.getBestPassages(
            detectedClass = "rice_leaf_blast",
            questionKeywords = "chemical",
            language = "en",
            limit = 1
        )
        assertFalse("Must retrieve chemical passage", passages.isEmpty())

        // Hallucinated chemical: Monocrotophos is NOT in rice_leaf_blast passage
        val hallucinatedResponse = "You should spray Monocrotophos across the field to treat it."

        val validation = ChatGuardrail.validate(
            generatedAnswer = hallucinatedResponse,
            passages = passages,
            findingText = "Crop: Rice Problem: Leaf Blast Severity: SEVERE Certainty: SURE"
        )
        assertFalse("Guardrail must reject hallucinated chemical", validation.isValid)
        assertTrue("Failure reason must mention chemical product", validation.failureReason?.contains("chemical product") == true)

        // Verify end-to-end ChatEngine fallback
        val chatResponse = ChatEngine.processQuery(
            query = "What chemical to spray?",
            detectedClass = "rice_leaf_blast",
            crop = "rice",
            problemName = "Rice Leaf Blast",
            severity = "SEVERE",
            certainty = "SURE",
            language = "en",
            passageStore = passageStore,
            llamaGenerator = { hallucinatedResponse }
        )

        assertEquals("Advisor source must be OFFLINE_GUIDE on guardrail rejection", "OFFLINE_GUIDE", chatResponse.advisorSource)
        assertEquals("Label must be Offline guide", "Offline guide", chatResponse.label)
        assertFalse("guardrailPassed must be false", chatResponse.guardrailPassed)
        val expectedFallback = chatResponse.retrievedPassages.joinToString("\n\n") { it.text }
        assertEquals("Returned text must match fallback passage text", expectedFallback, chatResponse.text)
    }

    @Test
    fun testGuardrailRejectionHallucinatedDose() = runBlocking {
        val passages = passageStore.getBestPassages(
            detectedClass = "rice_leaf_blast",
            questionKeywords = "chemical",
            language = "en",
            limit = 1
        )
        assertFalse("Must retrieve chemical passage", passages.isEmpty())

        // Hallucinated dose: 5.5 g/L is not in passage (real dose is 0.6 g/L or 2.0 ml/L)
        val hallucinatedDoseResponse = "Spray Tricyclazole 75% WP at 5.5 g/L."

        val validation = ChatGuardrail.validate(
            generatedAnswer = hallucinatedDoseResponse,
            passages = passages,
            findingText = "Crop: Rice Problem: Leaf Blast Severity: SEVERE Certainty: SURE"
        )
        assertFalse("Guardrail must reject hallucinated dose rate", validation.isValid)

        // Verify ChatEngine fallback
        val chatResponse = ChatEngine.processQuery(
            query = "How much dose?",
            detectedClass = "rice_leaf_blast",
            crop = "rice",
            problemName = "Rice Leaf Blast",
            severity = "SEVERE",
            certainty = "SURE",
            language = "en",
            passageStore = passageStore,
            llamaGenerator = { hallucinatedDoseResponse }
        )

        assertEquals("OFFLINE_GUIDE", chatResponse.advisorSource)
        assertEquals("Offline guide", chatResponse.label)
        assertFalse(chatResponse.guardrailPassed)
    }

    @Test
    fun testGuardrailAcceptanceGroundedAnswer() = runBlocking {
        val passages = passageStore.getBestPassages(
            detectedClass = "rice_leaf_blast",
            questionKeywords = "chemical",
            language = "en",
            limit = 1
        )
        assertFalse("Must retrieve chemical passage", passages.isEmpty())

        // Grounded answer containing verified active ingredients and facts strictly from the passage
        val groundedResponse = "For blast, spray Tricyclazole 75% WP following the product label. Observe the 30 days pre-harvest interval strictly."

        val validation = ChatGuardrail.validate(
            generatedAnswer = groundedResponse,
            passages = passages,
            findingText = "Crop: Rice Problem: Leaf Blast Severity: SEVERE Certainty: SURE"
        )
        assertTrue("Guardrail must accept fully grounded response", validation.isValid)

        val chatResponse = ChatEngine.processQuery(
            query = "chemical spray",
            detectedClass = "rice_leaf_blast",
            crop = "rice",
            problemName = "Rice Leaf Blast",
            severity = "SEVERE",
            certainty = "SURE",
            language = "en",
            passageStore = passageStore,
            llamaGenerator = { groundedResponse }
        )

        assertEquals("LLAMA", chatResponse.advisorSource)
        assertEquals("Explained on this phone by Llama 3.2 1B", chatResponse.label)
        assertTrue(chatResponse.guardrailPassed)
        assertEquals(groundedResponse, chatResponse.text)
    }

    @Test
    fun testFreeTextUserQueriesMatchAppropriatePassages() = runBlocking {
        // 1. "why it occurred" on maize_common_rust
        val whyPassages = passageStore.getBestPassages(
            detectedClass = "maize_common_rust",
            questionKeywords = "why it occurred",
            language = "en",
            limit = 1
        )
        assertFalse("Must find passage for 'why it occurred'", whyPassages.isEmpty())
        assertEquals(QuestionSection.WHAT_IS_IT, whyPassages.first().section)

        val whyResponse = ChatEngine.processQuery(
            query = "why it occurred",
            detectedClass = "maize_common_rust",
            crop = "maize",
            problemName = "Maize Common Rust",
            severity = "MODERATE",
            certainty = "SURE",
            language = "en",
            passageStore = passageStore,
            llamaGenerator = { "Airborne fungal rust erupts during cool humid nights, weakening plants." }
        )
        assertFalse("why it occurred must NOT trigger no-match fallback", whyResponse.isNoMatch)
        assertEquals("LLAMA", whyResponse.advisorSource)
        assertEquals("Explained on this phone by Llama 3.2 1B", whyResponse.label)

        // 2. "how to treat this"
        val treatPassages = passageStore.getBestPassages(
            detectedClass = "maize_common_rust",
            questionKeywords = "how to treat this",
            language = "en",
            limit = 1
        )
        assertFalse("Must find passage for 'how to treat this'", treatPassages.isEmpty())
        assertEquals(QuestionSection.WHAT_TO_DO_NOW, treatPassages.first().section)

        // 3. "which spray to use"
        val sprayPassages = passageStore.getBestPassages(
            detectedClass = "maize_common_rust",
            questionKeywords = "which spray to use",
            language = "en",
            limit = 1
        )
        assertFalse("Must find passage for 'which spray to use'", sprayPassages.isEmpty())
        assertEquals(QuestionSection.CHEMICAL, sprayPassages.first().section)

        // 4. Hindi: "यह क्यों हुआ"
        val hiPassages = passageStore.getBestPassages(
            detectedClass = "maize_common_rust",
            questionKeywords = "यह क्यों हुआ",
            language = "hi",
            limit = 1
        )
        assertFalse("Must find passage for Hindi 'यह क्यों हुआ'", hiPassages.isEmpty())
        assertEquals(QuestionSection.WHAT_IS_IT, hiPassages.first().section)

        // 5. Telugu: "ఎందుకు వచ్చింది"
        val tePassages = passageStore.getBestPassages(
            detectedClass = "maize_common_rust",
            questionKeywords = "ఎందుకు వచ్చింది",
            language = "te",
            limit = 1
        )
        assertFalse("Must find passage for Telugu 'ఎందుకు వచ్చింది'", tePassages.isEmpty())
        assertEquals(QuestionSection.WHAT_IS_IT, tePassages.first().section)
    }

    /**
     * Requirement 4: If no passage matches: answer "I do not have a sourced answer for this.
     * Please call the extension officer (1800-180-1551)." Never guess.
     */
    @Test
    fun testNoPassageFallback() = runBlocking {
        val outOfDomainQuery = "What is the stock price of bitcoin on crypto exchange today?"

        val chatResponse = ChatEngine.processQuery(
            query = outOfDomainQuery,
            detectedClass = "rice_leaf_blast",
            crop = "rice",
            problemName = "Rice Leaf Blast",
            severity = "SEVERE",
            certainty = "SURE",
            language = "en",
            passageStore = passageStore,
            llamaGenerator = { "Gold price is 7000 rupees." }
        )

        assertTrue("isNoMatch must be true for out-of-domain query", chatResponse.isNoMatch)
        assertEquals(
            "I do not have a sourced answer for this. Please call the extension officer (1800-180-1551).",
            chatResponse.text
        )
        assertEquals("OFFLINE_GUIDE", chatResponse.advisorSource)
        assertTrue("Source line must reference Kisan Call Centre", chatResponse.source.contains("1800-180-1551"))
    }

    /**
     * Requirement 3: Telugu: Llama does NOT write Telugu.
     * Show reviewed Telugu passage text + quick-question chips.
     * A typed or spoken Telugu question is matched to a chip by keywords.
     */
    @Test
    fun testTeluguChipMappingAndLlamaBypass() = runBlocking {
        val teluguChips = QuestionKeywordMatcher.getQuestionChips("te")
        assertEquals("Must have 8 Telugu quick-question chips", 8, teluguChips.size)

        // Verify all 8 Telugu chip titles map to the 8 sections
        val expectedSections = listOf(
            QuestionSection.WHAT_IS_IT,
            QuestionSection.SYMPTOMS,
            QuestionSection.WHAT_TO_DO_NOW,
            QuestionSection.PREVENTION,
            QuestionSection.NATURAL_REMEDY,
            QuestionSection.CHEMICAL,
            QuestionSection.SAFETY_WAITING_PERIOD,
            QuestionSection.WHEN_TO_CALL_OFFICER
        )
        for (section in expectedSections) {
            assertTrue("Telugu chips must contain section $section", teluguChips.any { it.first == section })
        }

        // Test Telugu keyword matching for typed questions
        assertEquals(QuestionSection.SYMPTOMS, QuestionKeywordMatcher.matchSection("లక్షణాలు ఏమిటి?", "te"))
        assertEquals(QuestionSection.CHEMICAL, QuestionKeywordMatcher.matchSection("మందులు ఏమి పిచికారీ చేయాలి?", "te"))
        assertEquals(QuestionSection.NATURAL_REMEDY, QuestionKeywordMatcher.matchSection("సేంద్రీయ పద్ధతులు ఏమిటి?", "te"))
        assertEquals(QuestionSection.WHAT_TO_DO_NOW, QuestionKeywordMatcher.matchSection("వెంటనే ఏమి చేయాలి?", "te"))
        assertEquals(QuestionSection.WHEN_TO_CALL_OFFICER, QuestionKeywordMatcher.matchSection("అధికారిని ఎప్పుడు సంప్రదించాలి?", "te"))

        // Verify Llama is NEVER invoked for Telugu queries (Rule H6)
        var llamaCalled = false
        val chatResponse = ChatEngine.processQuery(
            query = "లక్షణాలు ఏమిటి?",
            detectedClass = "cotton_bacterial_blight",
            crop = "cotton",
            problemName = "పత్తి బాక్టీరియల్ బ్లైట్",
            severity = "MODERATE",
            certainty = "PROBABLY",
            language = "te",
            passageStore = passageStore,
            llamaGenerator = {
                llamaCalled = true
                "Hallucinated Telugu text"
            }
        )

        assertFalse("Llama generator must NEVER be called for Telugu queries (Rule H6)", llamaCalled)
        assertEquals("Advisor source must be OFFLINE_GUIDE for Telugu", "OFFLINE_GUIDE", chatResponse.advisorSource)
        assertEquals("Label must be reviewed offline guide", "ఆఫ్‌లైన్ గైడ్", chatResponse.label)
        assertFalse("Returned Telugu text must not be empty", chatResponse.text.isBlank())
        assertTrue("Retrieved passages must have Telugu language", chatResponse.retrievedPassages.all { it.language == "te" })
    }

    /**
     * Requirement 5: Chat is disabled for Can't tell and Out of class outcomes.
     */
    @Test
    fun testChatDisabledForUncertainAndOutOfClass() = runBlocking {
        val response = ChatEngine.processQuery(
            query = "what is it",
            detectedClass = "unknown",
            crop = "unknown",
            problemName = "Unknown",
            severity = "MILD",
            certainty = "NOT_SURE",
            language = "en",
            passageStore = passageStore,
            isUncertainOrOutOfClass = true
        )

        assertTrue("isNoMatch must be true when chat is disabled", response.isNoMatch)
        assertEquals("OFFLINE_GUIDE", response.advisorSource)
        assertTrue("Message must explain chat is unavailable", response.text.contains("unavailable") || response.text.contains("consult"))
    }
}
