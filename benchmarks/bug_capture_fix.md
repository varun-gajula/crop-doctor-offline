# Bug Capture & Shutter Stability Report (Task BUG-CAPTURE)

**Date**: 2026-11-22  
**Target Reference Device**: Xiaomi POCO M4 5G (Model: `22041219PI`, Serial: `sseqjnucrkeeprtw`, Arm64-v8a, Android 14 / API 34)  
**Overall Status**: **RESOLVED & VERIFIED ON HARDWARE** (141 unit tests passing, 0 crashes, 0 hangs, 4/4 stress runs passed)

---

## 1. Problem Diagnosis & Root Causes (With Log Evidence)

### 1.1 Root Causes of App Close (Process Termination / Crash)

#### Cause 1A: Uncaught `RejectedExecutionException` on Main Thread (Hardware CameraX Lifecycle Race)
In `MainCameraScreen.kt`:
```kotlin
val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
DisposableEffect(lifecycleOwner) {
    onDispose {
        try {
            cameraExecutor.shutdown()
        } catch (_: Exception) {}
        cameraControl = null
        imageCapture = null
    }
}
```
- **Mechanism**: `remember { Executors.newSingleThreadExecutor() }` remained in composition memory, but `DisposableEffect(lifecycleOwner)` triggered `onDispose` during transient navigation or lifecycle interruptions (such as dialog popups, navigating to and from `ResultScreen`, or system configuration events). `cameraExecutor.shutdown()` permanently terminated the worker thread pool.
- **Evidence from Logcat**: When a user tapped the shutter, `capture.takePicture(cameraExecutor, ...)` was invoked without a try-catch on the Android Main UI thread:
```text
FATAL EXCEPTION: main
Process: com.cropdoctor, PID: 18452
java.util.concurrent.RejectedExecutionException: Task androidx.camera.core.ImageCapture$ImageCaptureRequest@4a3901b rejected from java.util.concurrent.ThreadPoolExecutor@8d451a0[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 1]
    at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2086)
    at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:848)
    at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1394)
    at androidx.camera.core.ImageCapture.takePicture(ImageCapture.java:780)
    at com.cropdoctor.ui.MainCameraScreenKt$MainCameraScreen$4$2$1.invoke(MainCameraScreen.kt:303)
```
- **HAL Buffer Exhaustion**: Prior to this fix, `isProcessing` was only set to `true` after CameraX completed JPEG decoding and passed bytes back to `MainActivity` (a window of 500 ms – 1500 ms). Tapping the shutter rapidly (300 ms intervals) queued multiple hardware requests to the MediaTek camera HAL, causing `ImageCaptureException: The image capture pipeline is unable to take pictures at this time` and buffer exhaustion in the native camera driver.

---

#### Cause 1B: Native `Fatal signal 11 (SIGSEGV)` in `re2::Regexp::Destroy()` (Llama Idle Model Release)
In `ExecuTorchLlamaAdvisor.kt`:
```kotlin
private fun scheduleIdleRelease() {
    idleJob = CoroutineScope(Dispatchers.Default).launch {
        delay(60_000L) // 60 seconds idle release (Rule H15)
        synchronized(modelLock) {
            llamaModule?.resetNative()
            llamaModule = null
...
```
- **Mechanism**: After 60 seconds of idle time following Llama model initialization, `scheduleIdleRelease()` called `llamaModule?.resetNative()`. In ExecuTorch 0.4.0 JNI (`libexecutorch_jni.so`), `LlmModule.resetNative()` delegates to `com.facebook.jni.HybridData.resetNative()`, destroying the C++ `ExecuTorchLlmJni` -> `example::Runner` -> `tokenizers::Tiktoken` -> `re2::RE2` instance. During regex pattern destruction, RE2 attempted to read an already unmapped or corrupted memory address (`SEGV_MAPERR`), causing an instantaneous native crash of the entire process.
- **Evidence from Logcat Crash Buffer (`logcat -b crash`)**:
```text
11-22 17:59:18.833 21135 26809 F libc    : Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x25bf6dfa4 in tid 26809 (DefaultDispatch), pid 21135 (com.cropdoctor)
11-22 17:59:19.932 26833 26833 F DEBUG   : Cmdline: com.cropdoctor
11-22 17:59:19.932 26833 26833 F DEBUG   : pid: 21135, tid: 26809, name: DefaultDispatch  >>> com.cropdoctor <<<
11-22 17:59:19.932 26833 26833 F DEBUG   : signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x000000025bf6dfa4
11-22 17:59:19.933 26833 26833 F DEBUG   : backtrace:
11-22 17:59:19.933 26833 26833 F DEBUG   :       #00 pc 0000000000d964c0  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (re2::Regexp::Destroy()+188)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #01 pc 0000000000d73408  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (re2::RE2::~RE2()+264)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #02 pc 0000000000419504  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (tokenizers::detail::BPETokenizerBase::~BPETokenizerBase()+148)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #03 pc 0000000000424948  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (tokenizers::Tiktoken::~Tiktoken()+16)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #04 pc 0000000000415c88  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (example::Runner::~Runner()+200)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #05 pc 0000000000415d0c  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (example::Runner::~Runner()+16)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #06 pc 000000000030f840  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libexecutorch_jni.so (executorch_jni::ExecuTorchLlmJni::~ExecuTorchLlmJni()+76)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #07 pc 000000000001f998  /data/app/~~fP6-wJYM4iC0BpkavWi9Nw==/com.cropdoctor-3dkSW7gcbc2R8Gy9ic3wMQ==/base.apk!libfbjni.so
11-22 17:59:19.933 26833 26833 F DEBUG   :       #13 pc 0000000000e23b54  .../base.vdex (com.facebook.jni.HybridData$Destructor.destruct+0)
11-22 17:59:19.933 26833 26833 F DEBUG   :       #17 pc 0000000000e23bf0  .../base.vdex (com.facebook.jni.HybridData.resetNative+0)
11-22 17:59:19.934 26833 26833 F DEBUG   :       #21 pc 00000000039a1bac  .../base.vdex (org.pytorch.executorch.extension.llm.LlmModule.resetNative+0)
11-22 17:59:19.934 26833 26833 F DEBUG   :       #25 pc 0000000002208074  .../base.vdex (com.cropdoctor.advisor.ExecuTorchLlamaAdvisor$scheduleIdleRelease$1.invokeSuspend+0)
```

---

### 1.2 Root Causes of Infinite Spinner (Hang State)

#### Cause 2A: Suspending `withContext` in `finally` Without `NonCancellable`
In `MainActivity.kt`:
```kotlin
finally {
    if (thisRequestId == currentRequestId.get()) {
        withContext(Dispatchers.Main) {
            isProcessingImage = false
        }
    }
}
```
- **Mechanism**: When a previous diagnosis job was cancelled (e.g. by tapping the shutter again or navigating back to Camera), the coroutine context became cancelled. When the coroutine entered `finally`, calling suspending `withContext(Dispatchers.Main)` immediately threw `CancellationException`. This aborted the `finally` block before `isProcessingImage = false` could execute, leaving the UI permanently locked with `isBusy = true`.

#### Cause 2B: Unbounded Pipeline Execution Without Watchdog Timeout
- Neither image preprocessing nor ExecuTorch inference had an upper-bound timeout. If native forward execution stalled or an image buffer was dropped by the camera HAL without an error callback, the coroutine suspended forever.

#### Cause 2C: Concurrent Forward Execution in `ExecuTorchClassifier`
- ExecuTorch native `Module.forward()` is not re-entrant. When concurrent capture requests hit `ExecuTorchClassifier.classify()` simultaneously:
```text
11-22 17:59:17.233 21135 21164 E ExecuTorch: Inputs can not be set mid execution.
11-22 17:59:17.234 21135 21164 E ExecuTorchClassifier: Vision inference error caught: Execution of method forward failed with status 0x2
java.lang.Exception: Execution of method forward failed with status 0x2
    at org.pytorch.executorch.NativePeer.forward(Native Method)
    at org.pytorch.executorch.Module.forward(Module.java:75)
    at com.cropdoctor.vision.ExecuTorchClassifier$classify$6.invokeSuspend(ExecuTorchClassifier.kt:450)
```

---

## 2. Implemented Fixes

| Fix ID | Requirement | Implementation Summary |
|---|---|---|
| **F1** | **Busy State Cleared in Finally** | 1. Wrapped all UI state resets in `withContext(NonCancellable + Dispatchers.Main) { isProcessingImage = false }` so completion is guaranteed on success, cancellation, or exception.<br>2. Added immediate local `var isShutterBusy by remember { mutableStateOf(false) }` in `MainCameraScreen.kt` with a 4.0-second safety watchdog (`LaunchedEffect(isShutterBusy)`).<br>3. In `MainActivity.navigateTo(AppScreen.CAMERA)`, explicitly reset `isProcessingImage = false` and `isGeneratingAdvice = false`. |
| **F2** | **ImageProxy & Bitmap Recycling in Finally** | 1. `image.close()` guaranteed in a strict `finally` block inside CameraX `onCaptureSuccess`.<br>2. Intermediate `bitmap512` instances, un-retained captured bitmaps, and pending preview bitmaps are recycled in `finally` blocks across all error and timeout paths. |
| **F3** | **10-Second Analysis Timeout** | Wrapped the entire analysis pipeline (`prepare` + `classify`) in `withTimeout(10_000L)`. On timeout, cancels the coroutine, clears the spinner, and displays localized `"Could not read the photo. Please try again."` (`R.string.error_could_not_read_photo`) with `DiagnosisOutcome.CANT_TELL` and action button `"Take another photo"`. |
| **F4** | **Graceful Catching of All Exceptions** | All exceptions in image decode, rotation, scaling, and ExecuTorch inference are caught and routed to `DiagnosisOutcome.CANT_TELL` with user-friendly retry guidance, never crashing the app. |
| **F5** | **Cancel Llama Generation on New Capture** | 1. Invokes `(app.advisor as? ExecuTorchLlamaAdvisor)?.stop()` and cancels `currentAdviceJob` immediately on shutter click.<br>2. Cancels background advice jobs when navigating to Camera. |
| **F6** | **Camera Bound Once & Native Execution Mutex** | 1. Changed executor lifecycle to `DisposableEffect(Unit)` so `cameraExecutor` is bound once and not shut down during navigation.<br>2. Added `kotlinx.coroutines.sync.Mutex` (`forwardMutex`) inside `ExecuTorchClassifier` to serialize native forward calls, preventing `Inputs can not be set mid execution`.<br>3. Removed dangerous `llamaModule?.resetNative()` calls in `ExecuTorchLlamaAdvisor` to eliminate SIGSEGV in RE2, replacing with `llamaModule?.stop()` and GC release. |

---

## 3. Real Shutter Verification Results (On Physical POCO Device)

Executed via `scripts/run_real_shutter_test.py` on the connected Xiaomi POCO M4 5G (`22041219PI`) across 120 live camera captures (30 fast taps x 2 runs + 30 slow taps x 2 runs):

### Measured Test Results Table

| Test Run | Tap Pattern | Spacing | Total Taps | Crashes | ANRs | Longest Spinner Time | Initial PSS | Peak PSS (RAM) | Result |
|---|---|---|---|---|---|---|---|---|---|
| **Run 1** | Fast Taps | 300 ms | 30 | **0** | **0** | **1.268 s** | 147.9 MB | **225.3 MB** | **PASS** |
| **Run 2** | Fast Taps | 300 ms | 30 | **0** | **0** | **0.892 s** | 218.8 MB | **229.8 MB** | **PASS** |
| **Run 3** | Slow Taps | 2000 ms | 30 | **0** | **0** | **1.069 s** | 222.0 MB | **235.0 MB** | **PASS** |
| **Run 4** | Slow Taps | 2000 ms | 30 | **0** | **0** | **1.097 s** | 216.8 MB | **230.3 MB** | **PASS** |

### Verification Evidence & Analysis
1. **0 Crashes Across 120 Captures**: No `RejectedExecutionException`, no camera HAL crashes, and 0 native `SIGSEGV` signals.
2. **0 ANRs**: Android OS recorded 0 Application Not Responding events (`dumpsys activity anrs`).
3. **0 Hangs**: In every tap, the loading indicator cleanly completed within 0.89 s – 1.27 s (well within the 1.5 s budget of Rule U2).
4. **RAM Footprint (Rule H15)**: Peak memory usage peaked at **235.0 MB PSS**, well below the 500 MB budget for the app without Llama.

---

## 4. Before vs. After Comparison

| Behavior / Scenario | Before Fix | After Fix |
|---|---|---|
| **Rapid Shutter Taps (300 ms spacing)** | Uncaught `RejectedExecutionException` crashed app; camera HAL buffer exhaustion. | `isShutterBusy` lock ignores rapid redundant taps; active capture finishes cleanly. **0 crashes**. |
| **Capture Cancellation & Navigation** | Suspending `withContext` in `finally` threw `CancellationException`, leaving spinner frozen forever. | `withContext(NonCancellable + Dispatchers.Main)` guarantees `isProcessingImage = false`. **0 hangs**. |
| **Corrupted or Stalled Buffer** | Indefinite hang on loading spinner. | Strict 10 s timeout via `withTimeout(10_000L)`; displays "Could not read the photo. Please try again." with retry button. |
| **Llama Idle Release (60s)** | Explicit `resetNative()` caused `Fatal signal 11 (SIGSEGV)` in `re2::Regexp::Destroy()`. | Safe `stop()` and reference nulling with garbage collection. **0 native crashes**. |
| **Concurrent Vision Inference** | `Inputs can not be set mid execution` (ExecuTorch error status 0x2). | `forwardMutex.withLock` serializes native forward calls cleanly. |
| **Unit Test Suite** | 122 tests | **141 tests** passing (`CaptureSafetyTest`, `RulesEngineTest`, `VisionMathTest`, etc.). |
