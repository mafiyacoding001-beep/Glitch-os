package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreateJobRequest
import com.example.data.youtube.VideoItemData
import com.example.pipeline.dom.*
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateVideoScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProgress: (String) -> Unit
) {
    val settings by viewModel.settingsFlow.collectAsState()
    val createJobState by viewModel.createJobState.collectAsState()

    val categories = com.example.util.UnifiedCategories.ALL

    var topic by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(settings.defaultLanguage) }
    var selectedCategory by remember {
        val defaultCat = settings.defaultCategory
        val matched = categories.firstOrNull { it.equals(defaultCat, ignoreCase = true) }
            ?: when {
                defaultCat.contains("tech", ignoreCase = true) -> "Technology"
                defaultCat.contains("ai", ignoreCase = true) -> "AI & Futuristic"
                defaultCat.contains("horror", ignoreCase = true) -> "Horror & Thriller"
                defaultCat.contains("mystery", ignoreCase = true) || defaultCat.contains("crime", ignoreCase = true) -> "Mystery & Crime"
                defaultCat.contains("history", ignoreCase = true) -> "History & Culture"
                defaultCat.contains("motivation", ignoreCase = true) -> "Motivation & Facts"
                defaultCat.contains("science", ignoreCase = true) -> "Science"
                defaultCat.contains("business", ignoreCase = true) || defaultCat.contains("money", ignoreCase = true) -> "Business & Finance"
                defaultCat.contains("game", ignoreCase = true) -> "Gaming"
                defaultCat.contains("entertainment", ignoreCase = true) || defaultCat.contains("vlog", ignoreCase = true) -> "Entertainment"
                else -> "Technology"
            }
        mutableStateOf(matched)
    }
    var selectedDuration by remember { mutableStateOf(settings.defaultDuration) }
    var selectedStyle by remember { mutableStateOf(settings.defaultStyle) }
    var selectedAspect by remember { mutableStateOf(settings.defaultAspect) }
    var selectedVoice by remember { mutableStateOf(settings.ttsVoice.ifBlank { "hi-IN-MadhurNeural" }) }
    var imageDurationSec by remember { mutableStateOf(settings.imageDurationSec) }
    var showCaptions by remember { mutableStateOf(settings.showCaptions) }
    var captionStyle by remember { mutableStateOf("hormozi_bold") }
    var captionHighlightColor by remember { mutableStateOf("#FFE600") } // Hormozi Yellow
    var captionFontFamily by remember { mutableStateOf("THE_BOLD_FONT") }
    var captionStrokeWidth by remember { mutableStateOf(14f) }
    var captionGlow by remember { mutableStateOf(true) }
    var captionWordsPerChunk by remember { mutableStateOf(3) }
    var captionUppercase by remember { mutableStateOf(true) }

    // Free BGM Music Studio State
    var bgmEnabled by remember { mutableStateOf(true) }
    var bgmTrackId by remember { mutableStateOf("dark_suspense_pulse") }
    var bgmVolume by remember { mutableStateOf(0.18f) }
    var autoDucking by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val channelAccounts by com.example.util.ChannelProfileManager.getAllChannelsFlow(context).collectAsState(initial = emptyList())
    var showChannelSwitcherDialog by remember { mutableStateOf(false) }
    val activeChannel = channelAccounts.firstOrNull { it.isActiveProfile } ?: channelAccounts.firstOrNull()

    // ⚡ LIVE NOTRACK GENERATION STATE
    var notrackLiveScript by remember { mutableStateOf<com.example.util.ViralScriptBreakdown?>(null) }
    var isGeneratingLiveScript by remember { mutableStateOf(false) }
    var liveScriptStatusMessage by remember { mutableStateOf("") }
    var popularChartVideos by remember { mutableStateOf<List<VideoItemData>>(emptyList()) }
    var isLoadingPopularChart by remember { mutableStateOf(false) }
    var popularChartError by remember { mutableStateOf<String?>(null) }

    // Sync with persistent saved category from SettingsManager / Database
    LaunchedEffect(settings.defaultCategory) {
        val defaultCat = settings.defaultCategory
        if (defaultCat.isNotBlank()) {
            val matched = categories.firstOrNull { it.equals(defaultCat, ignoreCase = true) } ?: defaultCat
            selectedCategory = matched
            bgmTrackId = com.example.util.FreeMusicManager.getCategoryDefaultTrackId(matched)
        }
    }

    fun refreshPopularChart() {
        coroutineScope.launch {
            isLoadingPopularChart = true
            popularChartError = null
            val result = com.example.util.GoogleSignInHelper.fetchTrendingYouTubeVideos(context)
            result.onSuccess { videos ->
                popularChartVideos = videos
                if (videos.isEmpty()) {
                    popularChartError = "YouTube returned no popular videos for this region."
                }
            }.onFailure { error ->
                popularChartError = error.localizedMessage ?: "Could not load YouTube's popular chart."
            }
            isLoadingPopularChart = false
        }
    }

    fun generateLiveScriptFromNotrack(targetTopic: String? = null) {
        val resolvedTopic = (targetTopic ?: topic).trim()
        if (resolvedTopic.isBlank()) {
            notrackLiveScript = null
            liveScriptStatusMessage = "Enter a topic or select a live YouTube chart video first."
            return
        }
        coroutineScope.launch {
            isGeneratingLiveScript = true
            liveScriptStatusMessage = "⚡ Connecting to https://notrack.ai live..."
            try {
                val client = com.example.pipeline.NotrackWebViewClient.create(context)
                liveScriptStatusMessage = "⚡ Generating neural viral script for \"$resolvedTopic\"..."
                val res = client.askNotrackForViralScriptBreakdown(
                    topic = resolvedTopic,
                    category = selectedCategory,
                    language = selectedLanguage,
                    targetDurationSec = selectedDuration,
                    globalFallbackEnabled = false
                )
                notrackLiveScript = res
                liveScriptStatusMessage = "✅ Generated Live from NoTrack.ai!"
                Toast.makeText(context, "⚡ Generated Live from NoTrack for \"$resolvedTopic\"", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                notrackLiveScript = null
                liveScriptStatusMessage = "Script generation failed: ${e.localizedMessage ?: "provider unavailable"}"
                Toast.makeText(context, liveScriptStatusMessage, Toast.LENGTH_LONG).show()
            } finally {
                isGeneratingLiveScript = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPopularChart()
    }

    DisposableEffect(Unit) {
        onDispose {
            com.example.util.FreeMusicManager.stopPreview()
        }
    }

    val languages = listOf("Hindi", "English", "Spanish", "French", "German")
    val durations = listOf(30, 60, 90)
    val styles = listOf("Cinematic", "Anime/Cyberpunk", "Photorealistic", "Digital Art", "Fantasy Art")
    val voices = listOf(
        "hi-IN-MadhurNeural" to "Madhur (Male)",
        "hi-IN-SwaraNeural" to "Swara (Female)",
        "en-US-GuyNeural" to "Guy (US Male)",
        "en-US-JennyNeural" to "Jenny (US Female)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Connected YouTube Channel Card
            if (channelAccounts.isNotEmpty()) {
                item {
                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📺", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "TARGET YOUTUBE CHANNEL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = activeChannel?.channelTitle ?: "Connected YouTube Channel",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${activeChannel?.subscriberCount ?: "0"} subs • Niche: ${activeChannel?.defaultCategory ?: selectedCategory}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (channelAccounts.size > 1) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showChannelSwitcherDialog = true },
                                    color = ElectricViolet.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet)
                                ) {
                                    Text(
                                        text = "SWITCH",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricViolet,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                    }
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Topic Field
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TOPIC OR VIRAL CONCEPT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        placeholder = {
                            Text(
                                text = "e.g. 5 Mind-Blowing AI tools in 2026",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("topic_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = CyberCyan
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category: $selectedCategory",
                            fontSize = 11.sp,
                            color = CyberCyan
                        )
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isGeneratingLiveScript) {
                                    generateLiveScriptFromNotrack(topic)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = NeonPink.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isGeneratingLiveScript) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = NeonPink,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NOTRACK LIVE...",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPink
                                    )
                                } else {
                                    Text(
                                        text = "⚡ GENERATE LIVE FROM NOTRACK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonPink
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 📝 AUTONOMOUS AI VIRAL SCRIPT & OUTLINE PLAYGROUND
            item {
                val context = LocalContext.current
                val viralScript = notrackLiveScript
                if (viralScript == null) {
                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = liveScriptStatusMessage.ifBlank {
                                "Enter a topic and generate a script from the connected provider."
                            },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📝", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTONOMOUS VIRAL SCRIPT & OUTLINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (notrackLiveScript != null) CyberCyan.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (notrackLiveScript != null) CyberCyan.copy(alpha = 0.8f) else ElectricViolet.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "🟢 LIVE NOTRACK.AI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { generateLiveScriptFromNotrack(topic) },
                                enabled = !isGeneratingLiveScript,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh from NoTrack Live",
                                    tint = if (isGeneratingLiveScript) TextMuted else NeonPink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (isGeneratingLiveScript) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyberCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🌐 Querying https://notrack.ai live...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = liveScriptStatusMessage.ifBlank { "Generating fresh, non-templated viral script & storyboard..." },
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hook Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "⚡ VIRAL HOOK (0-5s Pattern Interrupt):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viralScript.viralHook,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scene Breakdown List
                    Text(
                        text = "🎬 SCENE-BY-SCENE BREAKDOWN (${viralScript.sceneBreakdown.size} SCENES):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    viralScript.sceneBreakdown.forEach { scene ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Scene ${scene.sceneNumber} (${scene.timestampRange})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = scene.retentionAngle,
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "👁️ Visual: ${scene.visualCue}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "🗣️ Voiceover: ${scene.voiceoverText}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "🔊 SFX: ${scene.sfxCue}",
                                    fontSize = 10.sp,
                                    color = ElectricViolet
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Viral Script", viralScript.fullNarrationText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Full Script copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Script",
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            }

            // 🎨 AUTONOMOUS AI 8K THUMBNAIL & BANNER VISUAL PROMPT GENERATOR
            item {
                val context = LocalContext.current
                val visualPrompts = remember(topic, selectedCategory) {
                    com.example.util.ThumbnailBannerPromptGenerator.generateVisualPrompts(
                        topic = topic,
                        category = selectedCategory
                    )
                }

                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTONOMOUS 8K THUMBNAIL & BANNER PROMPTS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CyberCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "8K HIGH-CTR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text Overlay Badge
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "HIGH CTR TEXT OVERLAY:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonYellow
                                )
                                Text(
                                    text = visualPrompts.highCtrTextOverlay,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = visualPrompts.emotionalAngle,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 8K Thumbnail Prompt
                    Text(
                        text = "🖼️ 8K THUMBNAIL VISUAL PROMPT:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = DarkBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = visualPrompts.mainVisualPrompt,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Banner Prompt
                    Text(
                        text = "🚩 16:9 CHANNEL BANNER PROMPT:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = DarkBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = visualPrompts.bannerVisualPrompt,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Visual Prompts", "Thumbnail Prompt: ${visualPrompts.mainVisualPrompt}\n\nBanner Prompt: ${visualPrompts.bannerVisualPrompt}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Visual Prompts copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Prompts",
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy 8K Prompts", fontSize = 11.sp, color = CyberCyan)
                        }
                    }
                }
            }

            // Category Selection
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NICHE & CATEGORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        // Active category badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberCyan.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(CyberCyan)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ACTIVE: $selectedCategory",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = cat == selectedCategory
                            SelectablePill(
                                text = cat,
                                selected = selected,
                                onClick = {
                                    val previousCat = selectedCategory
                                    selectedCategory = cat
                                    bgmTrackId = com.example.util.FreeMusicManager.getCategoryDefaultTrackId(cat)
                                    viewModel.updateUnifiedCategory(cat)
                                    if (topic.isNotBlank() && previousCat != cat) {
                                        generateLiveScriptFromNotrack(topic)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📊", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE YOUTUBE POPULAR CHART:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { refreshPopularChart() },
                                enabled = !isLoadingPopularChart
                            ) {
                                Text(
                                    text = if (isLoadingPopularChart) "Loading..." else "Refresh",
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        popularChartError?.let { error ->
                            Text(text = error, fontSize = 10.sp, color = NeonPink)
                        }
                        popularChartVideos.take(7).forEachIndexed { index, video ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        topic = video.title
                                        generateLiveScriptFromNotrack(video.title)
                                    },
                                color = DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (topic == video.title) CyberCyan else DarkBorder)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(
                                        text = "#${index + 1}  ${video.title}",
                                        fontSize = 11.sp,
                                        fontWeight = if (topic == video.title) FontWeight.Bold else FontWeight.Normal,
                                        color = if (topic == video.title) CyberCyan else TextPrimary,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Views: ${video.views}  •  Likes: ${video.likes}  •  Comments: ${video.comments}",
                                        fontSize = 9.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language & Duration
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlitchCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LANGUAGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        languages.forEach { lang ->
                            val selected = lang == selectedLanguage
                            SelectablePill(
                                text = lang,
                                selected = selected,
                                onClick = { selectedLanguage = lang },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    GlitchCard(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        durations.forEach { dur ->
                            val selected = dur == selectedDuration
                            SelectablePill(
                                text = "${dur}s",
                                selected = selected,
                                onClick = {
                                    selectedDuration = dur
                                    generateLiveScriptFromNotrack(topic)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Visual Style
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "VISUAL AESTHETIC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styles.forEach { st ->
                            val selected = st == selectedStyle
                            SelectablePill(
                                text = st,
                                selected = selected,
                                onClick = { selectedStyle = st }
                            )
                        }
                    }
                }
            }

            // Voice & Scene Duration
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "VOICEOVER & PACING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    voices.forEach { (key, label) ->
                        val selected = key == selectedVoice
                        SelectablePill(
                            text = label,
                            selected = selected,
                            onClick = { selectedVoice = key },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scene Pacing",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectablePill(
                                text = "3s Scenes",
                                selected = imageDurationSec == 3,
                                onClick = { imageDurationSec = 3 }
                            )
                            SelectablePill(
                                text = "6s Scenes",
                                selected = imageDurationSec == 6,
                                onClick = { imageDurationSec = 6 }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Viral Hormozi / MrBeast Captions",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Word-by-word active glow, heavy stroke & kinetic pop",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = showCaptions,
                            onCheckedChange = { showCaptions = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = CyberCyan,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }
                }
            }

            // Viral Hormozi / MrBeast Animated Caption Studio
            if (showCaptions) {
                item {
                    ViralCaptionStudioCard(
                        captionStyle = captionStyle,
                        onCaptionStyleChange = { captionStyle = it },
                        highlightColor = captionHighlightColor,
                        onHighlightColorChange = { captionHighlightColor = it },
                        fontFamily = captionFontFamily,
                        onFontFamilyChange = { captionFontFamily = it },
                        strokeWidth = captionStrokeWidth,
                        onStrokeWidthChange = { captionStrokeWidth = it },
                        glowEffect = captionGlow,
                        onGlowEffectChange = { captionGlow = it },
                        wordsPerChunk = captionWordsPerChunk,
                        onWordsPerChunkChange = { captionWordsPerChunk = it },
                        uppercase = captionUppercase,
                        onUppercaseChange = { captionUppercase = it }
                    )
                }
            }

            // Royalty-Free BGM Music Studio Card
            item {
                BgmMusicStudioCard(
                    bgmEnabled = bgmEnabled,
                    onBgmEnabledChange = { bgmEnabled = it },
                    selectedTrackId = bgmTrackId,
                    onTrackSelect = { bgmTrackId = it },
                    volume = bgmVolume,
                    onVolumeChange = { bgmVolume = it },
                    autoDucking = autoDucking,
                    onAutoDuckingChange = { autoDucking = it }
                )
            }

            // Submit Section with 10-Scene Story Button right beside submit
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🎬 Prompt Engineering Action Button: 10-Scene DOM Video Pipeline Generator
                    Surface(
                        onClick = {
                            if (topic.isBlank()) {
                                Toast.makeText(context, "Enter a topic or choose a live chart video first.", Toast.LENGTH_SHORT).show()
                            } else coroutineScope.launch {
                                isGeneratingLiveScript = true
                                liveScriptStatusMessage = "⚡ Generating 10 scene animations in parallel..."
                                try {
                                    val orchestrator = DomAutomationOrchestrator(context)
                                    val res = orchestrator.runComplete10SceneDomVideoPipeline(
                                        topic = topic.trim(),
                                        onProgressUpdate = { p, status ->
                                            liveScriptStatusMessage = "[$p%] $status"
                                        }
                                    )
                                    topic = res.title
                                    val allNarration = res.scenes.joinToString(" ") { it.script.narrationHindi.ifBlank { it.script.narrationEnglish } }
                                    val sceneItems = res.scenes.map { sc ->
                                        com.example.util.SceneBreakdownItem(
                                            sceneNumber = sc.sceneNumber,
                                            timestampRange = "${(sc.sceneNumber - 1) * 15}s - ${sc.sceneNumber * 15}s",
                                            visualCue = sc.script.visualPrompt,
                                            voiceoverText = sc.script.narrationHindi.ifBlank { sc.script.narrationEnglish },
                                            sfxCue = sc.script.audioSfx,
                                            retentionAngle = sc.script.cameraMotion
                                        )
                                    }
                                    notrackLiveScript = com.example.util.ViralScriptBreakdown(
                                        topic = res.title,
                                        category = selectedCategory,
                                        language = selectedLanguage,
                                        viralHook = res.scenes.firstOrNull()?.script?.narrationHindi ?: "",
                                        totalEstimatedDurationSec = res.totalDurationSeconds,
                                        fullNarrationText = allNarration,
                                        sceneBreakdown = sceneItems,
                                        endScreenCallToAction = res.moral,
                                        visualPrompt8k = res.scenes.firstOrNull()?.script?.visualPrompt ?: ""
                                    )
                                    liveScriptStatusMessage = "✅ 10-Scene DOM Video Stitched Successfully!"
                                    Toast.makeText(context, "🎉 10-Scene DOM Pipeline Generated & Rendered!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    notrackLiveScript = null
                                    liveScriptStatusMessage = "Story generation failed: ${e.localizedMessage ?: "provider unavailable"}"
                                    Toast.makeText(context, liveScriptStatusMessage, Toast.LENGTH_LONG).show()
                                } finally {
                                    isGeneratingLiveScript = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = NeonPink.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonPink),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("create_10scene_story_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = "10-Scene Story",
                                tint = NeonPink,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "10-SCENE STORY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonPink
                                )
                                Text(
                                    text = "JSON Prompt Engine",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    NeonButton(
                        text = if (createJobState.isSubmitting) "STARTING PIPELINE..." else "GENERATE VIDEO",
                        leadingIcon = Icons.Default.AutoAwesome,
                        enabled = !createJobState.isSubmitting,
                        onClick = {
                            val finalTopic = topic.trim()
                            if (finalTopic.isBlank()) {
                                Toast.makeText(context, "Enter a topic or choose a live chart video first.", Toast.LENGTH_SHORT).show()
                            } else {
                                val request = CreateJobRequest(
                                    topic = finalTopic,
                                    language = selectedLanguage,
                                    category = selectedCategory,
                                    durationSec = selectedDuration,
                                    style = selectedStyle,
                                    aspectRatio = selectedAspect,
                                    voiceName = selectedVoice,
                                    imageDurationSec = imageDurationSec,
                                    showCaptions = showCaptions,
                                    captionStyle = captionStyle,
                                    captionHighlightColor = captionHighlightColor,
                                    captionFontFamily = captionFontFamily,
                                    captionStrokeWidth = captionStrokeWidth,
                                    captionGlow = captionGlow,
                                    captionWordsPerChunk = captionWordsPerChunk,
                                    captionUppercase = captionUppercase,
                                    bgmEnabled = bgmEnabled,
                                    bgmTrackId = bgmTrackId,
                                    bgmVolume = bgmVolume,
                                    autoDucking = autoDucking,
                                    customNarration = notrackLiveScript?.fullNarrationText,
                                    customHook = notrackLiveScript?.viralHook
                                )
                                viewModel.createVideo(request, onNavigateToProgress)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_generate_button")
                    )
                }
                createJobState.error?.let { error ->
                    Text(
                        text = error,
                        color = NeonPink,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showChannelSwitcherDialog) {
            AlertDialog(
                onDismissRequest = { showChannelSwitcherDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📺", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Target YouTube Channel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
                    ) {
                        items(channelAccounts.size) { idx ->
                            val ch = channelAccounts[idx]
                            val isSelected = ch.channelId == (activeChannel?.channelId ?: "")
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            com.example.util.ChannelProfileManager.switchActiveProfile(context, ch.channelId)
                                            selectedCategory = ch.defaultCategory
                                            bgmTrackId = com.example.util.FreeMusicManager.getCategoryDefaultTrackId(ch.defaultCategory)
                                            selectedLanguage = ch.defaultLanguage
                                            showChannelSwitcherDialog = false
                                            Toast.makeText(context, "Switched to ${ch.channelTitle} (${ch.defaultCategory}) 🎯", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                color = if (isSelected) CyberCyan.copy(alpha = 0.12f) else DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyberCyan else DarkBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.channelTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) CyberCyan else TextPrimary
                                        )
                                        Text(
                                            text = "${ch.subscriberCount} subs • Category: ${ch.defaultCategory}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Text("ACTIVE", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChannelSwitcherDialog = false }) {
                        Text("Close", color = CyberCyan)
                    }
                },
                containerColor = DarkSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pill_glow_transition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pill_border_width"
    )

    val activeBorderColor = CyberCyan.copy(alpha = glowAlpha)
    val activeBackgroundColor = CyberCyan.copy(alpha = 0.15f * glowAlpha + 0.08f)

    val bg = if (selected) activeBackgroundColor else DarkSurfaceElevated
    val border = if (selected) activeBorderColor else DarkBorder
    val textColor = if (selected) CyberCyan else TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(borderWidth, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = glowAlpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViralCaptionStudioCard(
    captionStyle: String,
    onCaptionStyleChange: (String) -> Unit,
    highlightColor: String,
    onHighlightColorChange: (String) -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    glowEffect: Boolean,
    onGlowEffectChange: (Boolean) -> Unit,
    wordsPerChunk: Int,
    onWordsPerChunkChange: (Int) -> Unit,
    uppercase: Boolean,
    onUppercaseChange: (Boolean) -> Unit
) {
    val presets = listOf(
        CaptionPreset("👑 Hormozi Viral", "hormozi_bold", "#FFE600", "THE_BOLD_FONT", 14f, true, 3, true),
        CaptionPreset("⚡ MrBeast Neon", "mrbeast_punch", "#00FF66", "MONTSERRAT_BLACK", 12f, true, 2, true),
        CaptionPreset("🌌 Cyber Cyan", "cyber_glow", "#00FFE0", "THE_BOLD_FONT", 10f, true, 3, true),
        CaptionPreset("🔥 Fire Red", "fire_red", "#FF3300", "IMPACT", 14f, true, 1, true),
        CaptionPreset("🖤 Dark Boxed", "minimalist_box", "#FFE600", "THE_BOLD_FONT", 10f, true, 3, true),
        CaptionPreset("🎤 Clean Classic", "classic_subtitles", "#FFFFFF", "MONTSERRAT_BLACK", 6f, false, 6, false)
    )

    val colorPalette = listOf(
        "#FFE600" to "Hormozi Gold",
        "#00FF66" to "Beast Green",
        "#00FFE0" to "Cyber Cyan",
        "#FF007F" to "Neon Pink",
        "#FF3300" to "Flame Red",
        "#FFFFFF" to "Pure White"
    )

    val fontOptions = listOf(
        "THE_BOLD_FONT" to "The Bold Font",
        "IMPACT" to "Impact Punch",
        "MONTSERRAT_BLACK" to "Montserrat Black",
        "BEBAS_NEUE" to "Bebas Neue"
    )

    val strokeOptions = listOf(
        14f to "Heavy 3D (Hormozi)",
        8f to "Medium Outline",
        0f to "No Stroke"
    )

    val chunkOptions = listOf(
        1 to "1 Word (Pop)",
        2 to "2 Words (Punch)",
        3 to "3 Words (Viral)",
        6 to "Full Line"
    )

    // Animated Preview State
    val previewWords = remember(uppercase) {
        val words = listOf("THIS", "SECRET", "CHANGED", "EVERYTHING", "FOREVER")
        if (uppercase) words.map { it.uppercase() } else listOf("This", "secret", "changed", "everything", "forever")
    }

    var currentPreviewWordIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            currentPreviewWordIndex = (currentPreviewWordIndex + 1) % previewWords.size
        }
    }

    val parsedHighlightColor = remember(highlightColor) {
        try {
            Color(android.graphics.Color.parseColor(highlightColor))
        } catch (_: Exception) {
            Color(0xFFFFE600)
        }
    }

    GlitchCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("viral_caption_studio_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥 VIRAL CAPTION STUDIO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = parsedHighlightColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, parsedHighlightColor.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "LIVE RENDER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = parsedHighlightColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LIVE ANIMATED PREVIEW BOX
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF080B10))
                    .border(
                        1.dp,
                        if (glowEffect) parsedHighlightColor.copy(alpha = 0.5f) else DarkBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle vignette
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    parsedHighlightColor.copy(alpha = if (glowEffect) 0.08f else 0.02f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Render current preview chunk
                val previewChunkStart = (currentPreviewWordIndex / wordsPerChunk.coerceAtLeast(1)) * wordsPerChunk.coerceAtLeast(1)
                val previewChunkWords = previewWords.subList(
                    previewChunkStart.coerceIn(0, previewWords.size - 1),
                    (previewChunkStart + wordsPerChunk.coerceAtLeast(1)).coerceAtMost(previewWords.size)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewChunkWords.forEach { word ->
                        val isWordActive = previewWords.indexOf(word) == currentPreviewWordIndex
                        val scale by animateFloatAsState(
                            targetValue = if (isWordActive) 1.28f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "word_scale"
                        )

                        val wordColor = if (isWordActive) parsedHighlightColor else Color.White
                        val glowShadow = if (isWordActive && glowEffect) {
                            Modifier.background(
                                parsedHighlightColor.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .then(glowShadow)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = word,
                                fontSize = (18 * scale).sp,
                                fontWeight = FontWeight.Black,
                                color = wordColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Small badge at bottom of preview
                Text(
                    text = "Live preview: active word highlights in sync with AI speech",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PRESETS SELECTOR
            Text(
                text = "VIRAL PRESETS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = captionStyle == preset.styleCode
                    SelectablePill(
                        text = preset.name,
                        selected = isSelected,
                        onClick = {
                            onCaptionStyleChange(preset.styleCode)
                            onHighlightColorChange(preset.highlightColor)
                            onFontFamilyChange(preset.fontFamily)
                            onStrokeWidthChange(preset.strokeWidth)
                            onGlowEffectChange(preset.glow)
                            onWordsPerChunkChange(preset.wordsPerChunk)
                            onUppercaseChange(preset.uppercase)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ACTIVE WORD HIGHLIGHT COLOR
            Text(
                text = "ACTIVE WORD HIGHLIGHT COLOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorPalette.forEach { (colorHex, label) ->
                    val isSelected = highlightColor.equals(colorHex, ignoreCase = true)
                    val c = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (_: Exception) {
                        Color.Yellow
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else DarkBorder,
                                shape = CircleShape
                            )
                            .clickable { onHighlightColorChange(colorHex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // FONT STYLE & CASE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CUSTOM FONT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    fontOptions.forEach { (key, label) ->
                        SelectablePill(
                            text = label,
                            selected = fontFamily == key,
                            onClick = { onFontFamilyChange(key) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WORDS PER BURST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    chunkOptions.forEach { (count, label) ->
                        SelectablePill(
                            text = label,
                            selected = wordsPerChunk == count,
                            onClick = { onWordsPerChunkChange(count) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // STROKE & GLOW CONTROLS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Neon Glow Halo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Luminous aura around spoken word",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = glowEffect,
                    onCheckedChange = { onGlowEffectChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = parsedHighlightColor,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALL CAPS Text",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "High-impact Hormozi style uppercase",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uppercase,
                    onCheckedChange = { onUppercaseChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }
    }
}

data class CaptionPreset(
    val name: String,
    val styleCode: String,
    val highlightColor: String,
    val fontFamily: String,
    val strokeWidth: Float,
    val glow: Boolean,
    val wordsPerChunk: Int,
    val uppercase: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BgmMusicStudioCard(
    bgmEnabled: Boolean,
    onBgmEnabledChange: (Boolean) -> Unit,
    selectedTrackId: String,
    onTrackSelect: (String) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    autoDucking: Boolean,
    onAutoDuckingChange: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tracks = remember { com.example.util.FreeMusicManager.curatedTracks }
    val currentlyPlayingId by com.example.util.FreeMusicManager.currentlyPlayingTrackId.collectAsState()

    var selectedCategory by remember { mutableStateOf("ALL") }

    val categories = remember {
        listOf("ALL", "SUSPENSE & HORROR", "MOTIVATION & VIRAL", "CYBERPUNK & TECH", "LO-FI & CHILL", "DRAMATIC & EPIC")
    }

    val filteredTracks = remember(selectedCategory) {
        if (selectedCategory == "ALL") tracks
        else tracks.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    GlitchCard(
        borderColor = if (bgmEnabled) NeonPink else DarkBorder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row with Switch & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🎵 ROYALTY-FREE MUSIC STUDIO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (bgmEnabled) NeonPink else TextSecondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "100% FREE & SAFE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                    Text(
                        text = "Copyright-free background music with smart voice auto-ducking",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = bgmEnabled,
                    onCheckedChange = onBgmEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = NeonPink,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            if (bgmEnabled) {
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Category Filter Tabs
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories.size) { idx ->
                        val cat = categories[idx]
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonPink.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                .border(1.dp, if (isSelected) NeonPink else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NeonPink else TextSecondary
                            )
                        }
                    }
                }

                // Track Selection List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredTracks.forEach { track ->
                        val isSelected = selectedTrackId == track.id
                        val isPlaying = currentlyPlayingId == track.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonPink.copy(alpha = 0.12f) else DarkSurfaceElevated)
                                .border(1.dp, if (isSelected) NeonPink else DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onTrackSelect(track.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            com.example.util.FreeMusicManager.playPreviewTrack(context, track.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Stop Preview" else "Play Preview",
                                            tint = if (isPlaying) NeonPink else CyberCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = track.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) TextPrimary else TextSecondary
                                        )
                                        Text(
                                            text = "${track.mood} • ${track.bpm} BPM • ${track.license}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonPink.copy(alpha = 0.25f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("SELECTED", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonPink)
                                    }
                                }
                            }
                        }
                    }
                }

                // Volume Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("BGM MIX VOLUME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text("${(volume * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonPink)
                    }
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0.05f..0.50f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonPink,
                            activeTrackColor = NeonPink,
                            inactiveTrackColor = DarkSurfaceElevated
                        )
                    )
                }

                // Auto-Ducking Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Smart Voice Auto-Ducking",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automatically lowers music volume while narration speaks",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoDucking,
                        onCheckedChange = onAutoDuckingChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBackground,
                            checkedTrackColor = CyberCyan,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }
            }
        }
    }
}
