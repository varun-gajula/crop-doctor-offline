package com.cropdoctor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.R
import com.cropdoctor.ui.theme.FarmGreenPrimary

@Composable
fun AdvisoryCard(
    advisoryText: String,
    isGenerating: Boolean,
    language: String,
    isFake: Boolean = true,
    isLowMemoryFallback: Boolean = false,
    onCancel: () -> Unit = {}
) {
    val cancelLabel = when (language) {
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
                if (isLowMemoryFallback) {
                    Text(
                        text = "Offline guide",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1565C0)
                    )
                } else if (isFake) {
                    Text(
                        text = "⚠️ " + stringResource(R.string.demo_text_not_ai),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
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
