package com.cropdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropdoctor.advisor.ChatMessage
import com.cropdoctor.advisor.CropChatbotEngine
import com.cropdoctor.advisor.MessageSender
import com.cropdoctor.knowledge.KbEntry
import com.cropdoctor.ui.theme.FarmGreenDark
import com.cropdoctor.ui.theme.FarmGreenPrimary
import kotlinx.coroutines.launch

import androidx.compose.ui.platform.LocalContext
import com.cropdoctor.advisor.OfflineTtsHelper

@Composable
fun ChatbotScreen(
    currentKbEntry: KbEntry?,
    language: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val ttsHelper = remember { OfflineTtsHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    val initialGreeting = remember(currentKbEntry, language) {
        if (currentKbEntry != null) {
            val diseaseName = when (language) {
                "te" -> currentKbEntry.names.te
                "hi" -> currentKbEntry.names.hi
                else -> currentKbEntry.names.en
            }
            when (language) {
                "te" -> "నమస్కారం! నేను మీ అగ్రి AI డాక్టర్‌ని 👨‍🌾. మీ పంటలో **$diseaseName** గుర్తించబడింది. దీని నివారణ, పురుగుమందుల మోతాదు లేదా సేంద్రీయ పద్ధతుల గురించి మీరు నన్ను ఏమైనా అడగవచ్చు."
                "hi" -> "नमस्ते! मैं आपका एग्रीकेयर AI डॉक्टर हूँ 👨‍🌾। आपकी फसल में **$diseaseName** पाया गया है। इसके उपचार, जैविक दवा या छिड़काव की मात्रा के बारे में मुझसे पूछें।"
                else -> "Hello! I am your AgriCare AI Doctor 👨‍🌾. I see you just diagnosed **$diseaseName**. How can I help you manage this crop today? Ask me about organic treatments, chemical dosages, or prevention."
            }
        } else {
            when (language) {
                "te" -> "నమస్కారం! నేను మీ అగ్రి AI డాక్టర్‌ని 👨‍🌾. వరి, పత్తి, మిరప, టమోటా, మొక్కజొన్న పంటల సంరక్షణ మరియు తెగుళ్ల నివారణ గురించి నన్ను అడగండి."
                "hi" -> "नमस्ते! मैं आपका एग्रीकेयर AI डॉक्टर हूँ 👨‍🌾। धान, कपास, मिर्च, टमाटर और मक्का की खेती और बीमारियों के बारे में मुझसे पूछें।"
                else -> "Hello! I am your AgriCare AI Doctor 👨‍🌾. Ask me anything about crop diseases, fertilizers, and sprays for Rice, Cotton, Chilli, Tomato, and Maize."
            }
        }
    }

    var messages by remember {
        mutableStateOf(
            listOf(ChatMessage(MessageSender.BOT, initialGreeting))
        )
    }

    var textInput by remember { mutableStateOf("") }

    val quickQuestions = when (language) {
        "te" -> listOf(
            "సేంద్రీయ నివారణ పద్ధతులు?",
            "సిఫార్సు చేసిన రసాయన మందు మోతాదు?",
            "నీటి యాజమాన్యం ఎలా చేయాలి?",
            "మరుసటి పంటకు నివారణ జాగ్రత్తలు?"
        )
        "hi" -> listOf(
            "जैविक उपचार क्या है?",
            "दवा की सही मात्रा क्या है?",
            "सिंचाई कैसे करें?",
            "भविष्य में रोकथाम के उपाय?"
        )
        else -> listOf(
            "What are organic treatments?",
            "Recommended chemical dosage per acre?",
            "How to manage irrigation?",
            "How to prevent in next season?"
        )
    }

    fun handleSend(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        val userMsg = ChatMessage(MessageSender.USER, cleanQuery)
        val botAnswer = CropChatbotEngine.answerQuestion(cleanQuery, currentKbEntry, language)
        val botMsg = ChatMessage(MessageSender.BOT, botAnswer)

        messages = messages + userMsg + botMsg
        textInput = ""

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FarmGreenPrimary)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👨‍🌾", fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = when (language) {
                            "te" -> "అగ్రి AI డాక్టర్"
                            "hi" -> "एग्रीकेयर AI डॉक्टर"
                            else -> "AgriCare AI Doctor"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "100% Offline • ICAR / ANGRAU Knowledge",
                        fontSize = 12.sp,
                        color = Color(0xFFC8E6C9)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAF7))
                .padding(paddingValues)
        ) {
            // Quick Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickQuestions.forEach { chipText ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.clickable { handleSend(chipText) }
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = FarmGreenDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        message = msg,
                        onSpeak = { textToSpeak ->
                            ttsHelper.speak(textToSpeak, language)
                        }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp, max = 120.dp),
                        placeholder = {
                            Text(
                                text = when (language) {
                                    "te" -> "సలహా లేదా మందుల గురించి అడగండి..."
                                    "hi" -> "दवा या बीमारी के बारे में पूछें..."
                                    else -> "Ask about treatment, dosage, water..."
                                },
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F8E9),
                            unfocusedContainerColor = Color(0xFFF1F8E9),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { handleSend(textInput) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(FarmGreenPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeak: (String) -> Unit = {}
) {
    val isUser = message.sender == MessageSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👨‍🌾", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) FarmGreenPrimary else Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 295.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isUser) Color.White else Color(0xFF263238),
                    lineHeight = 22.sp
                )
                if (!isUser) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .clickable { onSpeak(message.text) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔊", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Listen",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FarmGreenDark
                        )
                    }
                }
            }
        }
    }
}

