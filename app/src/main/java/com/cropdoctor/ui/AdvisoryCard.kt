package com.cropdoctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.advisor.OfflineAudioAdvisor
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun AdvisoryCard(
    advisoryText: String,
    isGenerating: Boolean,
    language: String,
    isFake: Boolean = true,
    isLowMemoryFallback: Boolean = false,
    isOfflineGuideFallback: Boolean = false,
    audioAdvisor: OfflineAudioAdvisor? = null,
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    val localAdvisor = remember { OfflineAudioAdvisor(context) }
    DisposableEffect(Unit) {
        onDispose {
            if (audioAdvisor == null) {
                localAdvisor.stop()
            }
        }
    }
    val activeAdvisor = audioAdvisor ?: localAdvisor
    val isPlaying = activeAdvisor.isPlaying.value
    val isPaused = activeAdvisor.isPaused.value
    val currentSpeed = activeAdvisor.playbackSpeed.value

    val cancelLabel = when (language.lowercase()) {
        "te" -> "రద్దు చేయి"
        "hi" -> "रद्द करें"
        else -> "Cancel"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        border = BorderStroke(1.5.dp, Color(0xFFC8E6C9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖 " + stringResource(R.string.advisory_card_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FarmGreenPrimary
                )

                if (isGenerating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✕ $cancelLabel",
                                fontSize = 13.sp,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = FarmGreenPrimary
                        )
                    }
                } else if (advisoryText.isNotBlank()) {
                    // Audio Playback Controls on Advisory Card (Requirement F10 / Task A1)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Speed Control (1.0x / 0.75x)
                        AssistChip(
                            onClick = { activeAdvisor.toggleSpeed() },
                            label = {
                                Text(
                                    text = if (currentSpeed == 1.0f) stringResource(R.string.speed_normal) else stringResource(R.string.speed_slow),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFE8F5E9),
                                labelColor = FarmGreenDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        )

                        // Listen / Pause / Resume Button
                        FilledTonalButton(
                            onClick = {
                                when {
                                    isPlaying -> activeAdvisor.pause()
                                    isPaused -> activeAdvisor.resume()
                                    else -> {
                                        val isLlama = !isOfflineGuideFallback && !isLowMemoryFallback && language.lowercase() != "te"
                                        activeAdvisor.playText(
                                            text = advisoryText,
                                            language = language,
                                            isLlamaOutput = isLlama
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isPlaying) Color(0xFFFFCDD2) else Color(0xFFE8F5E9)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = when {
                                    isPlaying -> stringResource(R.string.btn_pause) + " ⏸️"
                                    isPaused -> stringResource(R.string.btn_listen) + " ▶️"
                                    else -> stringResource(R.string.btn_listen) + " 🔊"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) Color(0xFFC62828) else FarmGreenDark
                            )
                        }

                        // Stop Button (visible while active)
                        if (isPlaying || isPaused) {
                            IconButton(
                                onClick = { activeAdvisor.stop() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "⏹️",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (advisoryText.isEmpty() && isGenerating) {
                    stringResource(R.string.advisory_generating)
                } else advisoryText + (if (isGenerating) " ▌" else ""),
                fontSize = 17.sp,
                color = Color(0xFF263238),
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOfflineGuideFallback || isLowMemoryFallback || language.lowercase() == "te") {
                    Text(
                        text = "Offline guide",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1565C0)
                    )
                } else if (isFake) {
                    Text(
                        text = "AI explanation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Text(
                        text = "AI explanation · Built with Llama 3.2 1B · ExecuTorch",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}
