package com.cropdoctor

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.util.Log
import com.cropdoctor.advisor.Advisor
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
import com.cropdoctor.advisor.FakeAdvisor
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

    var totalRamGb: Long = 4
        private set

    override fun onCreate() {
        super.onCreate()

        detectDeviceRam()

        // Asynchronous initialization guaranteeing sub-2s cold launch
        applicationScope.launch {
            KnowledgeBase.initialize(this@CropDoctorApplication)
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
