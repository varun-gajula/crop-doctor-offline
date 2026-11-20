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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.theme.FarmGreenPrimary
import com.cropdoctor.vision.ImageAnalysisMetadata
import java.io.InputStream
import java.util.concurrent.Executors
import kotlin.math.max

data class CropChipItem(
    val key: String,
    val nameRes: Int,
    val iconRes: Int
)

@Composable
fun MainCameraScreen(
    language: String,
    selectedCrop: String = "rice",
    isProcessing: Boolean,
    onImageCaptured: (Bitmap, String?) -> Unit,
    onBackClick: () -> Unit,
    onChangeCropClick: () -> Unit = {},
    onChangeLanguageClick: (String) -> Unit = {},
    onImageCapturedWithMetadata: ((Bitmap, String?, ImageAnalysisMetadata) -> Unit)? = null,
    onImageBytesCaptured: ((ByteArray, String?, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedCropFilter by remember(selectedCrop) { mutableStateOf(selectedCrop) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    fun dispatchCapturedImage(bitmap: Bitmap, crop: String?, meta: ImageAnalysisMetadata) {
        if (onImageCapturedWithMetadata != null) {
            onImageCapturedWithMetadata(bitmap, crop, meta)
        } else {
            onImageCaptured(bitmap, crop)
        }
    }

    val cropOptions = listOf(
        CropChipItem("all", R.string.crop_auto, R.drawable.ic_crop_auto),
        CropChipItem("chilli", R.string.crop_chilli, R.drawable.ic_crop_chilli),
        CropChipItem("tomato", R.string.crop_tomato, R.drawable.ic_crop_tomato),
        CropChipItem("cotton", R.string.crop_cotton, R.drawable.ic_crop_cotton),
        CropChipItem("rice", R.string.crop_rice, R.drawable.ic_crop_rice),
        CropChipItem("maize", R.string.crop_maize, R.drawable.ic_crop_maize)
    )

    // Gallery Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !isProcessing) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                    val bytes = stream.readBytes()
                    if (onImageBytesCaptured != null) {
                        onImageBytesCaptured(bytes, selectedCropFilter, "gallery")
                    } else {
                        val pre = com.cropdoctor.vision.ImagePreprocessor.prepare(bytes)
                        val meta = ImageAnalysisMetadata(
                            source = "gallery",
                            origWidth = pre.origWidth,
                            origHeight = pre.origHeight,
                            exifOrientation = pre.exifRotationDegrees,
                            leafValidatorPass = pre.leafValidation.isLikelyCrop,
                            leafValidatorFraction = pre.leafValidation.plantPixelFraction,
                            leafValidatorReason = pre.leafValidation.reason
                        )
                        dispatchCapturedImage(pre.bitmap512, selectedCropFilter, meta)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainCameraScreen", "Failed to decode gallery image", e)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
            cameraControl = null
            imageCapture = null
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
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

            // Visible Selected Crop Banner with "Change crop" Chip (Task P3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xCC000000),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF81C784)),
                    modifier = Modifier
                        .clickable { onChangeCropClick() }
                        .semantics { contentDescription = "Change selected crop" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentCropRes = when (selectedCropFilter.lowercase()) {
                            "cotton" -> R.string.crop_cotton
                            "chilli" -> R.string.crop_chilli
                            "tomato" -> R.string.crop_tomato
                            "maize" -> R.string.crop_maize
                            else -> R.string.crop_rice
                        }
                        val currentCropIcon = when (selectedCropFilter.lowercase()) {
                            "cotton" -> R.drawable.ic_crop_cotton
                            "chilli" -> R.drawable.ic_crop_chilli
                            "tomato" -> R.drawable.ic_crop_tomato
                            "maize" -> R.drawable.ic_crop_maize
                            else -> R.drawable.ic_crop_rice
                        }
                        Icon(
                            painter = painterResource(id = currentCropIcon),
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(currentCropRes),
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(FarmGreenPrimary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.btn_change_crop),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Scanning Viewfinder Overlay
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .align(Alignment.Center)
                    .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
            )

            // Bottom Control Panel
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
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
                            text = stringResource(R.string.camera_tip),
                            fontSize = 13.sp,
                            color = Color(0xFF333333)
                        )
                    }
                }

                if (isProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = FarmGreenPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.analyzing_leaf),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
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
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.btn_gallery),
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        // Shutter Button (Disabled during analysis, with spinner and progress)
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(if (isProcessing) Color(0xFF6B7280) else FarmGreenPrimary)
                                .clickable(enabled = !isProcessing) {
                                    if (isProcessing) return@clickable
                                    val capture = imageCapture
                                    if (capture != null) {
                                        capture.takePicture(
                                            cameraExecutor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    try {
                                                        val buffer = image.planes[0].buffer
                                                        val bytes = ByteArray(buffer.remaining())
                                                        buffer.get(bytes)

                                                        ContextCompat.getMainExecutor(context).execute {
                                                            if (onImageBytesCaptured != null) {
                                                                onImageBytesCaptured(bytes, selectedCropFilter, "camera")
                                                            } else {
                                                                val pre = com.cropdoctor.vision.ImagePreprocessor.prepare(bytes)
                                                                val meta = ImageAnalysisMetadata(
                                                                    source = "camera",
                                                                    origWidth = pre.origWidth,
                                                                    origHeight = pre.origHeight,
                                                                    exifOrientation = pre.exifRotationDegrees,
                                                                    leafValidatorPass = pre.leafValidation.isLikelyCrop,
                                                                    leafValidatorFraction = pre.leafValidation.plantPixelFraction,
                                                                    leafValidatorReason = pre.leafValidation.reason
                                                                )
                                                                dispatchCapturedImage(pre.bitmap512, selectedCropFilter, meta)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("MainCameraScreen", "Capture processing error", e)
                                                    } finally {
                                                        image.close() // Close ImageProxy in finally block (Rule P2.3)
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    Log.e("MainCameraScreen", "Capture fallback", exception)
                                                    val sample = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        onImageCaptured(sample, selectedCropFilter)
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        val sample = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                                        onImageCaptured(sample, selectedCropFilter)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(34.dp),
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
