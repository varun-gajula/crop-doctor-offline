package com.cropdoctor.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cropdoctor.ui.theme.FarmGreenPrimary
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun MainCameraScreen(
    language: String,
    isProcessing: Boolean,
    onImageCaptured: (Bitmap) -> Unit,
    onChangeLanguageClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        Log.e("MainCameraScreen", "Camera binding failed (common on emulator without camera)", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Top Overlay: Title & Language Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🌱 Crop Doctor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0xCC1B5E20), RoundedCornerShape(12.dp))
                    .clickable { onChangeLanguageClick() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "🌐 " + language.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Center Guide Box (Target area for leaf)
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .border(2.dp, Color(0x88FFFFFF), RoundedCornerShape(20.dp))
        ) {
            Text(
                text = when (language) {
                    "te" -> "ఆకును బాక్సు మధ్యలో ఉంచండి"
                    "hi" -> "पत्ती को बॉक्स के बीच में रखें"
                    else -> "Align leaf inside box"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Bottom Controls: Shutter Button and Test Sample Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .background(Color(0xDD000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = FarmGreenPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = when (language) {
                                "te" -> "పరీక్షిస్తోంది (Arm CPU)..."
                                "hi" -> "जांच जारी है (Arm CPU)..."
                                else -> "Analysing leaf on Arm CPU..."
                            },
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Sample Button (Critical for emulator and test rigs)
                    Button(
                        onClick = {
                            // Generate a simple 224x224 bitmap for testing
                            val sample = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
                            onImageCaptured(sample)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC333333)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(
                            text = when (language) { "te" -> "నమూనా"; "hi" -> "नमूना"; else -> "Sample" },
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // Shutter Button (76dp circular touch target)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                val capture = imageCapture
                                if (capture != null) {
                                    capture.takePicture(
                                        cameraExecutor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val buffer = image.planes[0].buffer
                                                val bytes = ByteArray(buffer.remaining())
                                                buffer.get(bytes)
                                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                image.close()
                                                if (bitmap != null) {
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        onImageCaptured(bitmap)
                                                    }
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                Log.e("MainCameraScreen", "Capture failed, generating fallback bitmap", exception)
                                                val sample = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
                                                ContextCompat.getMainExecutor(context).execute {
                                                    onImageCaptured(sample)
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    val sample = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
                                    onImageCaptured(sample)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(FarmGreenPrimary)
                        )
                    }

                    // Spacer for symmetrical alignment
                    Spacer(modifier = Modifier.width(60.dp))
                }
            }
        }
    }
}
