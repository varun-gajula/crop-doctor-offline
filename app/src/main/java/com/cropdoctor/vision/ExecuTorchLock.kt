package com.cropdoctor.vision

import kotlinx.coroutines.sync.Mutex

/**
 * Process-wide execution gate for all native ExecuTorch forward passes.
 * In libexecutorch on Android, Module.forward (vision) and LlmModule.generate (Llama)
 * share a single native runtime and XNNPACK thread pool. Running them concurrently
 * causes native thread contention and deadlocks.
 */
object ExecuTorchLock {
    val mutex = Mutex()
}

