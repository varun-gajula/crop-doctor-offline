package com.cropdoctor

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.util.Log
import com.cropdoctor.advisor.Advisor
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
import com.cropdoctor.chat.AndroidSqlitePassageDatabase
import com.cropdoctor.chat.KnowledgeBasePassageIndexer
import com.cropdoctor.chat.PassageStore
import com.cropdoctor.knowledge.KnowledgeBase
import com.cropdoctor.vision.Classifier
import com.cropdoctor.vision.ExecuTorchClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CropDoctorApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var classifier: Classifier
        private set

    lateinit var advisor: Advisor
        private set

    lateinit var passageStore: PassageStore
        private set

    fun getAdvisorIfInitialized(): Advisor? = if (::advisor.isInitialized) advisor else null
    fun getPassageStoreIfInitialized(): PassageStore? = if (::passageStore.isInitialized) passageStore else null

    var totalRamGb: Long = 4
        private set

    override fun onCreate() {
        super.onCreate()

        detectDeviceRam()

        // Immediate Knowledge Base initialization (~15 ms)
        KnowledgeBase.initialize(this)

        passageStore = AndroidSqlitePassageDatabase(this)

        // Asynchronous background initialization for vision & advisory engines
        applicationScope.launch {
            val root = KnowledgeBase.getRoot()
            if (root != null && passageStore.countPassages() < 576) {
                val passages = KnowledgeBasePassageIndexer.buildAllPassages(root)
                passageStore.indexPassages(passages)
                Log.i("CropDoctorApp", "Indexed ${passages.size} passages into SQLite FTS5 database.")
            }

            classifier = ExecuTorchClassifier(this@CropDoctorApplication)

            // On-device ExecuTorch Llama 3.2 Advisor
            Log.i("CropDoctorApp", "Initializing on-device ExecuTorch Llama 3.2 engine.")
            advisor = ExecuTorchLlamaAdvisor(this@CropDoctorApplication)
        }
    }

    private fun detectDeviceRam() {
        try {
            val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            totalRamGb = (memInfo.totalMem / (1024 * 1024 * 1024)) + 1
            Log.i("CropDoctorApp", "Detected device total RAM: ~$totalRamGb GB")
        } catch (e: Exception) {
            totalRamGb = 4
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::classifier.isInitialized) classifier.close()
        if (::advisor.isInitialized) advisor.close()
    }
}
