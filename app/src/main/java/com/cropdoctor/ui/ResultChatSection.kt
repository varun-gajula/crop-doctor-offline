package com.cropdoctor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.advisor.Advisor
import com.cropdoctor.chat.ChatEngine
import com.cropdoctor.chat.PassageStore
import com.cropdoctor.chat.QuestionKeywordMatcher
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class UiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val label: String? = null,
    val source: String? = null,
    val isNoMatch: Boolean = false
)

/**
 * Chat Box on the Result Screen (Task CHAT).
 *
 * Implements:
 * 1. Fully offline SQLite FTS5 passage retrieval for (detected class, question keywords, language).
 * 2. Llama 3.2 1B answers ONLY from retrieved passages in English and Hindi (guardrail validated).
 * 3. Telugu: Llama does not write Telugu; shows reviewed Telugu passage text + question chips.
 * 4. No-match fallback with Kisan Call Centre (1800-180-1551) Call button.
 * 5. Source line under every answer + "Clear chat" button.
 * 6. Chat is disabled for Can't tell and Out of class outcomes.
 * 7. Cancels generation when leaving screen or submitting a new question.
 */
@Composable
fun ResultChatSection(
    classId: String,
    crop: String,
    problemName: String,
    severity: String,
    certainty: String,
    language: String,
    passageStore: PassageStore,
    advisor: Advisor,
    isUncertainOrOutOfClass: Boolean = false,
    onCallOfficer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var messages by rememberSaveable { mutableStateOf<List<UiChatMessage>>(emptyList()) }
    var inputText by rememberSaveable { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var currentJob by remember { mutableStateOf<Job?>(null) }
    var expanded by rememberSaveable { mutableStateOf(true) }

    // Cancel running generation when leaving screen or component disposed (Requirement 6)
    DisposableEffect(Unit) {
        onDispose {
            currentJob?.cancel()
            advisor.stop()
        }
    }

    fun submitQuestion(queryText: String) {
        val trimmed = queryText.trim()
        if (trimmed.isBlank() || isGenerating) return

        focusManager.clearFocus()
        inputText = ""

        // Cancel previous generation if still active
        currentJob?.cancel()
        advisor.stop()

        // Append user question
        val userMsg = UiChatMessage(isUser = true, text = trimmed)
        messages = messages + userMsg

        // Asynchronously process response
        currentJob = coroutineScope.launch {
            isGenerating = true
            try {
                val response = ChatEngine.processQuery(
                    query = trimmed,
                    detectedClass = classId,
                    crop = crop,
                    problemName = problemName,
                    severity = severity,
                    certainty = certainty,
                    language = language,
                    passageStore = passageStore,
                    isUncertainOrOutOfClass = isUncertainOrOutOfClass,
                    llamaGenerator = if (advisor.isModelAvailable) {
                        { prompt -> advisor.answerChatPrompt(prompt) }
                    } else null
                )

                val botMsg = UiChatMessage(
                    isUser = false,
                    text = response.text,
                    label = response.label,
                    source = response.source,
                    isNoMatch = response.isNoMatch
                )
                messages = messages + botMsg
            } catch (e: Exception) {
                val errorMsg = UiChatMessage(
                    isUser = false,
                    text = ChatEngine.getNoMatchText(language),
                    label = ChatEngine.getOfflineGuideLabel(language),
                    source = "National Kisan Call Centre (Toll-Free 1800-180-1551)",
                    isNoMatch = true
                )
                messages = messages + errorMsg
            } finally {
                isGenerating = false
            }
        }
    }

    val title = when (language.lowercase()) {
        "te" -> "ఈ సమస్య గురించి సందేహాలు అడగండి"
        "hi" -> "इस समस्या के बारे में पूछें"
        else -> "Ask about this problem"
    }

    val hintText = when (language.lowercase()) {
        "te" -> "మీ ప్రశ్నను టైప్ చేయండి..."
        "hi" -> "अपना सवाल यहां लिखें..."
        else -> "Ask a question about this..."
    }

    val clearLabel = when (language.lowercase()) {
        "te" -> "చాట్ క్లియర్ చేయండి"
        "hi" -> "चैट साफ करें"
        else -> "Clear chat"
    }

    val chips = QuestionKeywordMatcher.getQuestionChips(language)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title, Clear Button, Collapse Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (messages.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                currentJob?.cancel()
                                advisor.stop()
                                isGenerating = false
                                messages = emptyList()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = clearLabel,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = Color.Gray
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Chat Disabled Notice for Can't tell / Out of class
                    if (isUncertainOrOutOfClass) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = when (language.lowercase()) {
                                        "te" -> "⚠️ తెగులు నిర్ధారణ కానప్పుడు లేదా పంట కానప్పుడు చాట్ అందుబాటులో ఉండదు. సహాయం కొరకు విస్తరణ అధికారిని సంప్రదించండి."
                                        "hi" -> "⚠️ समस्या अनिश्चित होने या गैर-फसल होने पर चैट उपलब्ध नहीं है। सहायता हेतु कृषि अधिकारी से संपर्क करें।"
                                        else -> "⚠️ Chat is disabled when condition is uncertain or out of class. Please contact an agricultural officer."
                                    },
                                    fontSize = 15.sp,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onCallOfficer,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call Officer (1800-180-1551)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Quick-question chips (The 8 question types)
                        Text(
                            text = when (language.lowercase()) {
                                "te" -> "త్వరిత ప్రశ్నలు:"
                                "hi" -> "त्वरित प्रश्न:"
                                else -> "Quick questions:"
                            },
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chips.forEach { (_, chipTitle) ->
                                SuggestionChip(
                                    onClick = { submitQuestion(chipTitle) },
                                    label = { Text(chipTitle, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color(0xFFE8F5E9),
                                        labelColor = FarmGreenDark
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Message bubbles
                        if (messages.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                messages.forEach { msg ->
                                    if (msg.isUser) {
                                        // User Bubble (Right-aligned)
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Surface(
                                                color = FarmGreenPrimary,
                                                shape = RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp),
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Bot Answer Bubble (Left-aligned)
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Surface(
                                                color = Color(0xFFF1F8E9),
                                                shape = RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp),
                                                border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    // Source Badge / Advisor Label
                                                    if (!msg.label.isNullOrBlank()) {
                                                        val isLlama = msg.label.contains("Llama", ignoreCase = true)
                                                        Surface(
                                                            color = if (isLlama) Color(0xFFEDE7F6) else Color(0xFFE8F5E9),
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, if (isLlama) Color(0xFFB39DDB) else Color(0xFFA5D6A7)),
                                                            modifier = Modifier.padding(bottom = 6.dp)
                                                        ) {
                                                            Text(
                                                                text = msg.label,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isLlama) Color(0xFF512DA8) else Color(0xFF2E7D32),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    // Text
                                                    Text(
                                                        text = msg.text,
                                                        color = Color(0xFF1B5E20),
                                                        fontSize = 15.sp,
                                                        lineHeight = 22.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )

                                                    // Source Line
                                                    if (!msg.source.isNullOrBlank()) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = "Source: ${msg.source}",
                                                            fontSize = 12.sp,
                                                            color = Color.DarkGray,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    // Call Extension Officer button if no match
                                                    if (msg.isNoMatch) {
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Button(
                                                            onClick = onCallOfficer,
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "Call Officer (1800-180-1551)",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Generating Indicator
                        if (isGenerating) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = FarmGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when (language.lowercase()) {
                                        "te" -> "ఆఫ్‌లైన్ సమాచారం శోధిస్తోంది..."
                                        "hi" -> "ऑफलाइन जानकारी खोजी जा रही है..."
                                        else -> "Retrieving sourced answer offline..."
                                    },
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Input Field Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text(hintText, fontSize = 14.sp, color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FarmGreenPrimary,
                                    unfocusedBorderColor = Color(0xFFBDBDBD)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { submitQuestion(inputText) },
                                enabled = inputText.isNotBlank() && !isGenerating,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (inputText.isNotBlank() && !isGenerating) FarmGreenPrimary else Color(0xFFE0E0E0)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank() && !isGenerating) Color.White else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
