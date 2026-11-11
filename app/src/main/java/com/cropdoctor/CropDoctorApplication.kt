package com.cropdoctor

import android.app.Application
import com.cropdoctor.advisor.Advisor
import com.cropdoctor.advisor.ExecuTorchLlamaAdvisor
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

    override fun onCreate() {
        super.onCreate()

        // Background asynchronous initialization to guarantee cold start under 2.0s
        applicationScope.launch {
            KnowledgeBase.initialize(this@CropDoctorApplication)
            classifier = ExecuTorchClassifier(this@CropDoctorApplication)
            advisor = ExecuTorchLlamaAdvisor(this@CropDoctorApplication)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::classifier.isInitialized) classifier.close()
        if (::advisor.isInitialized) advisor.close()
    }
}
