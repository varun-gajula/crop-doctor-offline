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

/**
 * Advisory Card displaying on-device guidance (Task A-REAL).
 *
 * Rules & Verifications:
 * - When real Llama streams tokens: shows label "Explained on this phone by Llama 3.2 1B".
 * - When real Llama cannot run (weights missing, low memory, load failure, Telugu): shows
 *   static verified knowledge-base advice immediately with visible label:
 *   "Offline guide - AI explanation not available", never typed out word by word and never labelled as AI.
 */
@Composable
fun AdvisoryCard(
    advisoryText: String,
    isGenerating: Boolean,
    language: String,
    advisorSource: String = "OFFLINE_GUIDE", // "LLAMA" or "OFFLINE_GUIDE"
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

    val isLlama = advisorSource == "LLAMA"

    val cancelLabel = when (language.lowercase()) {
        "te" -> "రద్దు చేయి"
        "hi" -> "रद्द करें"
        else -> "Cancel"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLlama) Color(0xFFF1F8E9) else Color(0xFFF4F7FB)
        ),
        border = BorderStroke(1.5.dp, if (isLlama) Color(0xFFC8E6C9) else Color(0xFFBBDEFB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLlama) {
                        "🤖 " + stringResource(R.string.advisory_card_title)
                    } else {
                        "📖 " + stringResource(R.string.advisory_card_title_offline)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLlama) FarmGreenPrimary else Color(0xFF1565C0)
                )

                if (isGenerating && isLlama) {
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
                    // Audio Playback Controls
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
                                containerColor = if (isLlama) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                labelColor = if (isLlama) FarmGreenDark else Color(0xFF0D47A1)
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
                                        activeAdvisor.playText(
                                            text = advisoryText,
                                            language = language,
                                            isLlamaOutput = isLlama
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isPlaying) Color(0xFFFFCDD2) else (if (isLlama) Color(0xFFE8F5E9) else Color(0xFFE3F2FD))
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
                                color = if (isPlaying) Color(0xFFC62828) else (if (isLlama) FarmGreenDark else Color(0xFF0D47A1))
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

            // Body text
            Text(
                text = if (advisoryText.isEmpty() && isGenerating && isLlama) {
                    stringResource(R.string.advisory_generating)
                } else advisoryText + (if (isGenerating && isLlama) " ▌" else ""),
                fontSize = 17.sp,
                color = Color(0xFF263238),
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visible source badge row (Rule A-REAL: truthful label)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLlama) Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                    border = BorderStroke(1.dp, if (isLlama) Color(0xFFA5D6A7) else Color(0xFF90CAF9))
                ) {
                    Text(
                        text = if (isLlama) {
                            stringResource(R.string.advisory_label_llama)
                        } else {
                            stringResource(R.string.advisory_label_offline_guide)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLlama) Color(0xFF2E7D32) else Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
