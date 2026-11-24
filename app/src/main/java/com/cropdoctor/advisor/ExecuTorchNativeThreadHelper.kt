package com.cropdoctor.advisor

import android.util.Log
import com.sun.jna.NativeLibrary

object ExecuTorchNativeThreadHelper {
    private const val TAG = "ExecuTorchNativeThread"

    @Volatile
    private var currentConfiguredThreads: Long = -1L

    @Volatile
    var targetThreadCount: Long = 4L

    fun setThreadNum(numThreads: Long): Long {
        return try {
            val lib = NativeLibrary.getInstance("executorch")
            val setFn = lib.getFunction("_ZN10executorch9extension14set_thread_numEl")
            setFn.invoke(Void.TYPE, arrayOf(numThreads))
            val getFn = lib.getFunction("_ZN10executorch9extension14get_thread_numEv")
            val actual = (getFn.invoke(java.lang.Long.TYPE, emptyArray()) as Number).toLong()
            currentConfiguredThreads = actual
            Log.i(TAG, "set_thread_num($numThreads) -> active actual thread_num = $actual")
            actual
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to call set_thread_num: ${t.message}", t)
            -1L
        }
    }

    fun getThreadNum(): Long {
        return try {
            val lib = NativeLibrary.getInstance("executorch")
            val getFn = lib.getFunction("_ZN10executorch9extension14get_thread_numEv")
            val actual = (getFn.invoke(java.lang.Long.TYPE, emptyArray()) as Number).toLong()
            currentConfiguredThreads = actual
            actual
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to call get_thread_num: ${t.message}", t)
            -1L
        }
    }
}
