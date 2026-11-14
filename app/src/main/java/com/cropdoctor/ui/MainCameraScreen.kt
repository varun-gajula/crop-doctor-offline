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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cropdoctor.R
import com.cropdoctor.ui.components.LanguageChip
import com.cropdoctor.ui.theme.FarmGreenPrimary
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
    isProcessing: Boolean,
    onImageCaptured: (Bitmap, String?) -> Unit,
    onBackClick: () -> Unit,
    onChangeLanguageClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedCropFilter by remember { mutableStateOf("all") }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

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
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                    val bytes = stream.readBytes()
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    val maxDim = max(options.outWidth, options.outHeight)
                    options.inSampleSize = if (maxDim > 1024) maxDim / 1024 else 1
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    if (bitmap != null) {
                        onImageCaptured(bitmap, selectedCropFilter)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainCameraScreen", "Failed to decode gallery image", e)
            }
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

            // Top Crop Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cropOptions.forEach { item ->
                    val isSelected = selectedCropFilter == item.key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) FarmGreenPrimary else Color(0x99000000)
                            )
                            .clickable { selectedCropFilter = item.key }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = stringResource(item.nameRes),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(item.nameRes),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
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

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(FarmGreenPrimary)
                                .clickable {
                                    val capture = imageCapture
                                    if (capture != null) {
                                        capture.takePicture(
                                            cameraExecutor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val rotationDegrees = image.imageInfo.rotationDegrees
                                                    val buffer = image.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)

                                                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                    val maxDim = max(options.outWidth, options.outHeight)
                                                    options.inSampleSize = if (maxDim > 1024) maxDim / 1024 else 1
                                                    options.inJustDecodeBounds = false

                                                    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                                    image.close()

                                                    if (rawBitmap != null) {
                                                        val finalBitmap = if (rotationDegrees != 0) {
                                                            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                                        } else {
                                                            rawBitmap
                                                        }
                                                        ContextCompat.getMainExecutor(context).execute {
                                                            onImageCaptured(finalBitmap, selectedCropFilter)
                                                        }
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    Log.e("MainCameraScreen", "Capture fallback", exception)
                                                    val sample = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        onImageCaptured(sample, selectedCropFilter)
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        val sample = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
                                        onImageCaptured(sample, selectedCropFilter)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.btn_capture),
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
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
}
