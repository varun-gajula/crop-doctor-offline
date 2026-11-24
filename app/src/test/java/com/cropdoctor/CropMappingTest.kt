package com.cropdoctor

import com.cropdoctor.domain.CropMapping
import com.cropdoctor.vision.VisionMath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CropMappingTest {

    private fun findAsset(name: String): File {
        val candidates = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name"),
            File("../app/src/main/assets/$name")
        )
        val f = candidates.firstOrNull { it.exists() }
        assertNotNull("Could not find asset $name in any standard path", f)
        return f!!
    }

    private fun loadLabels(): List<String> {
        val file = findAsset("labels.json")
        val jsonArray = Json.parseToJsonElement(file.readText()).jsonArray
        return jsonArray.map { it.jsonPrimitive.content.trim() }
    }

    @Test
    fun testEveryClassHasExactlyOneCropExceptWeeds() {
        val labels = loadLabels()
        assertEquals("labels.json must contain exactly 15 classes", 15, labels.size)

        val supportedCrops: Set<String> = CropMapping.VALID_CROPS.toSet()
        assertEquals(setOf("rice", "cotton", "chilli", "tomato", "maize"), supportedCrops)

        var nonCropCount = 0
        for (label in labels) {
            val crop = CropMapping.getCropForClass(label)
            if (label == "other_not_a_crop" || label == "weeds_not_a_crop") {
                assertNull("$label must have null crop", crop)
                assertFalse("$label is not in any crop", CropMapping.isClassOfCrop(label, "rice"))
                assertFalse("$label is not in any crop", CropMapping.isClassOfCrop(label, "tomato"))
                nonCropCount++
            } else {
                assertNotNull("Class '$label' must have a mapped crop", crop)
                assertTrue("Class '$label' mapped to unexpected crop '$crop'", crop in supportedCrops)
                // Prefix check
                assertTrue("Class '$label' must start with crop prefix '$crop'", label.startsWith("${crop}_"))
                assertTrue("isClassOfCrop must be true for mapped crop", CropMapping.isClassOfCrop(label, crop!!))
                // Must not belong to any other crop
                for (otherCrop: String in supportedCrops) {
                    if (otherCrop != crop) {
                        assertFalse("Class '$label' must not belong to other crop '$otherCrop'", CropMapping.isClassOfCrop(label, otherCrop))
                    }
                }
            }
        }
        assertEquals("There must be exactly 1 non-crop class", 1, nonCropCount)
    }

    @Test
    fun testClassIndicesForCropDisjointAndComplete() {
        val labels = loadLabels()
        val allIndices = mutableSetOf<Int>()

        for (crop: String in CropMapping.VALID_CROPS) {
            val indices: List<Int> = CropMapping.getClassIndicesForCrop(crop, labels)
            assertFalse("Crop '$crop' must have non-empty indices", indices.isEmpty())
            for (idx: Int in indices) {
                assertFalse("Index $idx for crop '$crop' already assigned to another crop", allIndices.contains(idx))
                allIndices.add(idx)
            }
        }

        // other_not_a_crop index should be the only one not in allIndices
        val nonCropIndex = labels.indexOf("other_not_a_crop")
        assertTrue("other_not_a_crop must be in labels", nonCropIndex >= 0)
        assertFalse("other_not_a_crop index must not be in any crop indices", allIndices.contains(nonCropIndex))

        allIndices.add(nonCropIndex)
        assertEquals("Union of all crop indices plus non-crop index must cover all 15 classes", (0 until 15).toSet(), allIndices)
    }

    @Test
    fun testCropMaskRenormalizationAndCropMass() {
        val labels = loadLabels()
        val n = labels.size
        assertEquals(15, n)

        // Equal logits produces equal softmax probabilities of 1/15
        val scaledLogits = FloatArray(n) { 2.0f }
        val tomatoIndices: List<Int> = CropMapping.getClassIndicesForCrop("tomato", labels)
        assertEquals("Tomato has 2 classes in final model", 2, tomatoIndices.size)

        val result = VisionMath.maskAndRenormalizeWithMass(labels, scaledLogits, "tomato")
        val maskedProbs = result.probabilities

        // Total tomato mass originally: 2 * (1/15) = 2/15
        assertEquals(2.0f / 15.0f, result.cropMass, 1e-4f)

        // Non-tomato indices must have probability 0.0
        for (i in 0 until n) {
            if (i in tomatoIndices) {
                assertEquals(0.5f, maskedProbs[i], 1e-4f) // 2 equal items renormalized to 1.0
            } else {
                assertEquals(0.0f, maskedProbs[i], 1e-6f)
            }
        }

        // Renormalized sum over all classes must equal 1.0
        val sum = maskedProbs.sum()
        assertEquals(1.0f, sum, 1e-5f)
    }

    @Test
    fun testCropStringsPresentInAllThreeLanguages() {
        val baseResDir = File("src/main/res")
        fun loadKeys(file: File): Map<String, String> {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.getElementsByTagName("string")
            val map = mutableMapOf<String, String>()
            for (i in 0 until nodes.length) {
                val node = nodes.item(i)
                val name = node.attributes.getNamedItem("name")?.nodeValue
                if (name != null) map[name] = node.textContent
            }
            return map
        }

        val enMap = loadKeys(File(baseResDir, "values/strings.xml"))
        val teMap = loadKeys(File(baseResDir, "values-te/strings.xml"))
        val hiMap = loadKeys(File(baseResDir, "values-hi/strings.xml"))

        val requiredKeys = listOf(
            "select_crop_title",
            "select_crop_sub",
            "btn_change_crop",
            "crop_rice",
            "crop_cotton",
            "crop_chilli",
            "crop_tomato",
            "crop_maize"
        )

        for (key in requiredKeys) {
            assertTrue("Key '$key' must exist in English strings", enMap.containsKey(key) && enMap[key]!!.isNotBlank())
            assertTrue("Key '$key' must exist in Telugu strings", teMap.containsKey(key) && teMap[key]!!.isNotBlank())
            assertTrue("Key '$key' must exist in Hindi strings", hiMap.containsKey(key) && hiMap[key]!!.isNotBlank())
        }

        // Verify Telugu uses Telugu script
        val teluguRegex = Regex("[\\u0C00-\\u0C7F]")
        for (key in requiredKeys) {
            val teVal = teMap[key] ?: ""
            assertTrue("Telugu key '$key' must have Telugu script: '$teVal'", teluguRegex.containsMatchIn(teVal))
        }

        // Verify Hindi uses Devanagari script
        val devanagariRegex = Regex("[\\u0900-\\u097F]")
        for (key in requiredKeys) {
            val hiVal = hiMap[key] ?: ""
            assertTrue("Hindi key '$key' must have Devanagari script: '$hiVal'", devanagariRegex.containsMatchIn(hiVal))
        }
    }
}
