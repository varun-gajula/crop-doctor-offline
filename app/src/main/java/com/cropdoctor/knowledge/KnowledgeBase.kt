package com.cropdoctor.knowledge

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

object KnowledgeBase {

    private var entriesMap: Map<String, KbEntry> = emptyMap()
    private var isLoaded: Boolean = false

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Loads knowledge_base.json from the application's assets folder synchronously.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isLoaded) return

        try {
            context.assets.open("kb/knowledge_base.json").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val content = reader.readText()
                    val root = json.decodeFromString<KnowledgeBaseRoot>(content)
                    entriesMap = root.classes.associateBy { it.classId }
                    isLoaded = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getEntry(classId: String): KbEntry? {
        return entriesMap[classId]
    }

    fun getAllEntries(): List<KbEntry> {
        return entriesMap.values.toList()
    }
}
