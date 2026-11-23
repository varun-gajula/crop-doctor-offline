package com.cropdoctor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cropdoctor.R
import com.cropdoctor.domain.CropMapping
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.theme.FarmGreenPrimary
import com.cropdoctor.vision.ImagePreprocessor
import com.cropdoctor.vision.ImageAnalysisMetadata
import java.io.InputStream
import java.util.concurrent.Executors
import kotlin.math.max
import kotlinx.coroutines.delay

data class CropChipItem(
    val key: String,
    val nameRes: Int,
    val iconRes: Int
)

@Composable
fun MainCameraScreen(
    language: String,
    selectedCrop: String? = null,
    isProcessing: Boolean,
    onImageBytesCaptured: ((ByteArray, String?, String) -> Unit)? = null,
    onImageCaptured: (Bitmap, String?) -> Unit,
    onImageCapturedWithMetadata: ((Bitmap, String?, ImageAnalysisMetadata) -> Unit)? = null,
    onShutterClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onChangeCropClick: () -> Unit = {},
    onChangeLanguageClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isShutterBusy by remember { mutableStateOf(false) }
    val isBusy = isProcessing || isShutterBusy

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(Unit) {
        isShutterBusy = false
    }

    LaunchedEffect(isShutterBusy) {
        if (isShutterBusy) {
            delay(4000L)
            if (isShutterBusy) {
                Log.w("MainCameraScreen", "Watchdog: resetting stuck isShutterBusy after 4s")
                isShutterBusy = false
            }
        }
    }

    fun dispatchCapturedImage(bitmap: Bitmap, crop: String?, meta: ImageAnalysisMetadata) {
        if (onImageCapturedWithMetadata != null) {
            onImageCapturedWithMetadata(bitmap, crop, meta)
        } else {
            onImageCaptured(bitmap, crop)
        }
    }

    // Gallery Photo Picker (Single Image Pipeline)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !isProcessing) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                    val bytes = stream.readBytes()
                    if (onImageBytesCaptured != null) {
                        onImageBytesCaptured(bytes, selectedCrop, "gallery")
                    } else {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        val maxDim = max(options.outWidth, options.outHeight)
                        options.inSampleSize = if (maxDim > 1024) maxDim / 1024 else 1
                        options.inJustDecodeBounds = false
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        if (bitmap != null) {
                            onImageCaptured(bitmap, selectedCrop)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainCameraScreen", "Failed to decode gallery image", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.camera_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Persistent language chip on camera screen (T2)
                LanguageChip(
                    currentLanguage = language,
                    onLanguageSelected = { newLang ->
                        onChangeLanguageClick(newLang)
                    }
                )
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            val previewHeightDp = maxHeight
            val boxSizeDp = 290.dp
            val viewfinderFraction = (boxSizeDp.value / previewHeightDp.value).coerceIn(0.20f, 0.90f)

            // Camera Preview
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            Log.e("MainCameraScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            // Selected Crop Chip with "Change crop" action (Task P3)
            val cropDisplayName = if (selectedCrop != null) {
                CropMapping.getCropDisplayName(selectedCrop, context)
            } else {
                null
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { if (!isProcessing) onChangeCropClick() },
                color = Color(0x99000000),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cropDisplayName != null) {
                        Text(
                            text = "🌱 $cropDisplayName • ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "🌱 " + stringResource(R.string.scan_leaf) + " • ",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = stringResource(R.string.btn_change_crop),
                        color = Color(0xFF81C784),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Scanning Viewfinder Overlay (Original 290.dp rounded square)
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .align(Alignment.Center)
                    .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
            )

            // Bottom Control Panel (Original styling & colors)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0x99000000))
                    .padding(vertical = 18.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tip Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProcessing) stringResource(R.string.analyzing_leaf) else stringResource(R.string.camera_tip),
                            fontSize = 13.sp,
                            color = Color(0xFF333333)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(enabled = !isProcessing) {
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.btn_gallery),
                            tint = if (isProcessing) Color.Gray else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.btn_gallery),
                            color = if (isProcessing) Color.Gray else Color.White,
                            fontSize = 13.sp
                        )
                    }

                    // Shutter Button (76.dp circle, disabled during processing or in-flight capture)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(if (isBusy) Color.Gray else FarmGreenPrimary)
                            .clickable(enabled = !isBusy) {
                                if (isBusy) return@clickable
                                val capture = imageCapture
                                if (capture != null && !cameraExecutor.isShutdown) {
                                    isShutterBusy = true
                                    onShutterClick()
                                    try {
                                        capture.takePicture(
                                            cameraExecutor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    try {
                                                        val buffer = image.planes[0].buffer
                                                        val bytes = ByteArray(buffer.remaining())
                                                        buffer.get(bytes)
                                                        ContextCompat.getMainExecutor(context).execute {
                                                            isShutterBusy = false
                                                            if (onImageBytesCaptured != null) {
                                                                onImageBytesCaptured(bytes, selectedCrop, "camera:$viewfinderFraction")
                                                            } else {
                                                                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                                val maxDim = max(options.outWidth, options.outHeight)
                                                                options.inSampleSize = if (maxDim > 1024) maxDim / 1024 else 1
                                                                options.inJustDecodeBounds = false
                                                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                                if (bmp != null) {
                                                                    val pre = ImagePreprocessor.prepare(
                                                                        bmp,
                                                                        cropToViewfinder = true,
                                                                        viewfinderHeightFraction = viewfinderFraction
                                                                    )
                                                                    onImageCaptured(pre.bitmap512, selectedCrop)
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("MainCameraScreen", "Capture processing error", e)
                                                        ContextCompat.getMainExecutor(context).execute {
                                                            isShutterBusy = false
                                                        }
                                                    } finally {
                                                        image.close()
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    Log.e("MainCameraScreen", "Capture error from CameraX", exception)
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        isShutterBusy = false
                                                    }
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Log.e("MainCameraScreen", "Failed to trigger takePicture", e)
                                        isShutterBusy = false
                                    }
                                } else {
                                    Log.w("MainCameraScreen", "imageCapture is null or executor shutdown; cannot take photo")
                                    isShutterBusy = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.btn_capture),
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // Flash Toggle Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.btn_flash),
                            tint = if (isFlashOn) Color.Yellow else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.btn_flash),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
