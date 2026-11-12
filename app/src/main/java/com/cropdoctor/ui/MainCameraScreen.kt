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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cropdoctor.ui.theme.FarmGreenPrimary
import java.io.InputStream
import java.util.concurrent.Executors
import kotlin.math.max

@Composable
fun MainCameraScreen(
    language: String,
    isProcessing: Boolean,
    onImageCaptured: (Bitmap, String?) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedCropFilter by remember { mutableStateOf("all") }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val cropOptions = listOf(
        Pair("all", when (language) { "te" -> "🔍 ఆటో"; "hi" -> "🔍 ऑटो"; else -> "🔍 Auto" }),
        Pair("chilli", "🌶️ Chilli"),
        Pair("tomato", "🍅 Tomato"),
        Pair("cotton", "☁️ Cotton"),
        Pair("rice", "🌾 Rice"),
        Pair("maize", "🌽 Maize")
    )

    // Gallery Photo Picker (allows picking from gallery or WhatsApp images!)
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

    val screenTitle = when (language) {
        "te" -> "ఆకును స్కాన్ చేయండి"
        "hi" -> "पत्ती स्कैन करें"
        else -> "Scan Leaf"
    }

    val tipText = when (language) {
        "te" -> "సలహా: స్పష్టమైన వెలుతురులో ఆకు ఫోటో తీయండి."
        "hi" -> "सुझाव: रोगग्रस्त पत्ती की साफ फोटो लें।"
        else -> "Tip: Take a clear photo of the affected leaf."
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = screenTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
                                it.setSurfaceProvider(previewView.surfaceProvider)
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
                            Log.e("MainCameraScreen", "Camera binding fallback for emulator", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Scanning Target Bracket Overlay (exact match to reference photo)
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .align(Alignment.Center)
                    .border(3.dp, Color.White, RoundedCornerShape(24.dp))
            )

            // Crop Quick Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cropOptions.forEach { (key, label) ->
                    val isSelected = selectedCropFilter == key
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) FarmGreenPrimary else Color(0xB3000000),
                        modifier = Modifier.clickable { selectedCropFilter = key }
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Bottom Controls Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xD9000000))
                    .padding(top = 16.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tip Card (Yellow bulb callout from sample photo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9C4), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tipText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isProcessing) {
                    CircularProgressIndicator(
                        color = FarmGreenPrimary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (language) {
                            "te" -> "పరీక్షిస్తోంది (Arm CPU)..."
                            "hi" -> "जांच जारी है..."
                            else -> "Diagnosing on Arm CPU..."
                        },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Button (Pick from mobile gallery / WhatsApp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (language) { "te" -> "గ్యాలరీ"; "hi" -> "गैलरी"; else -> "Gallery" },
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        // Capture Shutter Button (Large green circular button)
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
                                        // Instant sample fallback for emulator
                                        val sample = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
                                        onImageCaptured(sample, selectedCropFilter)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture",
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
                                contentDescription = "Flash",
                                tint = if (isFlashOn) Color.Yellow else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (language) { "te" -> "ఫ్లాష్"; "hi" -> "फ्लैश"; else -> "Flash" },
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
