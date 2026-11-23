package com.cropdoctor.advisor

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Enforces Rule H15 memory safety decision before attempting to allocate/load
 * the on-device Llama 3.2 1B-Instruct model (1.1 GB INT4 weights).
 *
 * If available system RAM is below [MIN_AVAILABLE_RAM_BYTES] or the OS has flagged
 * low memory, Llama loading is bypassed and the app falls back to verified Knowledge Base
 * guidance with the label "Offline guide".
 */
object MemoryBudgetDecider {

    private const val TAG = "MemoryBudgetDecider"

    // 1500 MB minimum available system RAM required to safely load the 1.1 GB model
    // without triggering OS LowMemoryKiller (LMK) on 6 GB reference devices.
    const val MIN_AVAILABLE_RAM_BYTES: Long = 1500L * 1024L * 1024L

    // 250 MB minimum available system RAM required when the model is already loaded in memory
    const val MIN_EXECUTION_RAM_BYTES: Long = 250L * 1024L * 1024L

    /**
     * Pure, unit-testable decision function.
     *
     * @param availMemBytes Available RAM in bytes (from [ActivityManager.MemoryInfo.availMem])
     * @param isLowMemory Flag indicating if the system is currently in low-memory state
     * @param minRequiredBytes Minimum threshold (defaults to 1500 MB)
     * @return true if safe to load Llama, false to skip and use "Offline guide"
     */
    fun shouldLoadLlama(
        availMemBytes: Long,
        isLowMemory: Boolean,
        minRequiredBytes: Long = MIN_AVAILABLE_RAM_BYTES
    ): Boolean {
        if (isLowMemory) {
            return false
        }
        return availMemBytes >= minRequiredBytes
    }

    /**
     * Queries [ActivityManager] and evaluates [shouldLoadLlama].
     */
    fun isMemorySufficient(context: Context, minRequiredBytes: Long = MIN_AVAILABLE_RAM_BYTES): Boolean {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return true
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024 * 1024)
            val decision = shouldLoadLlama(memInfo.availMem, memInfo.lowMemory, minRequiredBytes)
            Log.i(
                TAG,
                "Rule H15 RAM Check: Available=${availMb}MB, LowMemory=${memInfo.lowMemory}, SafeToLoad=$decision"
            )
            decision
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect system memory via ActivityManager", e)
            true
        }
    }

    /**
     * Decision function when model is ALREADY resident in RAM.
     * Checks that the OS is not in low-memory state and there is sufficient scratch RAM.
     */
    fun isMemorySufficientForExecution(context: Context, minRequiredBytes: Long = MIN_EXECUTION_RAM_BYTES): Boolean {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return true
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024 * 1024)
            val decision = !memInfo.lowMemory && memInfo.availMem >= minRequiredBytes
            Log.i(
                TAG,
                "Rule H15 RAM Check (Execution): Available=${availMb}MB, LowMemory=${memInfo.lowMemory}, SafeToExecute=$decision"
            )
            decision
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect system memory via ActivityManager", e)
            true
        }
    }

    fun getAvailableRamBytes(context: Context): Long {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return -1L
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            memInfo.availMem
        } catch (_: Exception) {
            -1L
        }
    }

    /**
     * Checks whether the Android OS has flagged a system-wide low memory condition.
     */
    fun isSystemInLowMemory(context: Context): Boolean {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            memInfo.lowMemory
        } catch (_: Exception) {
            false
        }
    }
}
