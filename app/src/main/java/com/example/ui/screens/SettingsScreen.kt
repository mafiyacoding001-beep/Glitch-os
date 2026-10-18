package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.*
import com.example.ui.components.BackgroundPermissionsCard
import com.example.ui.components.GlitchCard
import com.example.ui.components.LegalAndComplianceCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogs: () -> Unit,
    hideTopBar: Boolean = false
) {
    val context = LocalContext.current
    val currentAppSettings by viewModel.settingsFlow.collectAsState()
    val currentAgentSettings by viewModel.agentSettingsFlow.collectAsState()
    val isAgentLoading by viewModel.isAgentSettingsLoading.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    // Load cloud settings on first launch
    LaunchedEffect(Unit) {
        viewModel.loadAgentSettings()
    }

    // Agent General
    var agentName by remember { mutableStateOf(currentAgentSettings.general.agentName) }
    var agentMode by remember { mutableStateOf(currentAgentSettings.general.mode) }
    var agentCategory by remember { mutableStateOf(currentAgentSettings.general.category) }

    // Language & Voice
    var language by remember { mutableStateOf(currentAgentSettings.languageAndVoice.language) }
    var voiceGender by remember { mutableStateOf(currentAgentSettings.languageAndVoice.voiceGender) }
    var voiceAccent by remember { mutableStateOf(currentAgentSettings.languageAndVoice.voiceAccent) }
    var speechRate by remember { mutableStateOf(currentAgentSettings.languageAndVoice.speechRate) }
    var pitch by remember { mutableStateOf(currentAgentSettings.languageAndVoice.pitch) }

    // Video Format
    var orientation by remember { mutableStateOf(currentAgentSettings.videoFormat.orientation) }
    var aspectRatio by remember { mutableStateOf(currentAgentSettings.videoFormat.aspectRatio) }
    var resolution by remember { mutableStateOf(currentAgentSettings.videoFormat.resolution) }
    var fps by remember { mutableStateOf(currentAgentSettings.videoFormat.fps) }
    var maxDurationSeconds by remember { mutableStateOf(currentAgentSettings.videoFormat.maxDurationSeconds) }

    // Captions & Subtitles
    var captionsEnabled by remember { mutableStateOf(currentAgentSettings.captionsAndSubtitles.captionsEnabled) }
    var captionStyle by remember { mutableStateOf(currentAgentSettings.captionsAndSubtitles.captionStyle) }
    var highlightColor by remember { mutableStateOf(currentAgentSettings.captionsAndSubtitles.highlightColor) }

    // Scheduling & Automation
    var autoGenerate by remember { mutableStateOf(currentAgentSettings.schedulingAndAutomation.autoGenerate) }
    var delayBetweenVideosSeconds by remember { mutableStateOf(currentAgentSettings.schedulingAndAutomation.delayBetweenVideosSeconds) }
    var dailyVideoLimit by remember { mutableStateOf(currentAgentSettings.schedulingAndAutomation.dailyVideoLimit) }

    // Content & Style
    var visualStyle by remember { mutableStateOf(currentAgentSettings.contentAndStyle.visualStyle) }
    var visualRenderMode by remember { mutableStateOf(currentAgentSettings.contentAndStyle.visualRenderMode.ifBlank { currentAppSettings.visualRenderMode }) }
    var htmlTheme by remember { mutableStateOf(currentAgentSettings.contentAndStyle.htmlTheme.ifBlank { currentAppSettings.htmlTheme }) }
    var backgroundMusic by remember { mutableStateOf(currentAgentSettings.contentAndStyle.backgroundMusic) }
    var bgMusicVolume by remember { mutableStateOf(currentAgentSettings.contentAndStyle.bgMusicVolume) }

    // Publishing
    var autoPublish by remember { mutableStateOf(currentAgentSettings.publishing.autoPublish) }
    var selectedPlatforms by remember { mutableStateOf(currentAgentSettings.publishing.platforms.toSet()) }
    var autoGenerateTitle by remember { mutableStateOf(currentAgentSettings.publishing.autoGenerateTitle) }
    var autoGenerateDescription by remember { mutableStateOf(currentAgentSettings.publishing.autoGenerateDescription) }
    var autoGenerateHashtags by remember { mutableStateOf(currentAgentSettings.publishing.autoGenerateHashtags) }

    // App/Server Settings
    var serverUrl by remember { mutableStateOf(currentAppSettings.serverUrl) }
    var apiKey by remember { mutableStateOf(currentAppSettings.apiKey) }
    var engineMode by remember { mutableStateOf(currentAppSettings.engineMode) }
    var imageDurationSec by remember { mutableStateOf(currentAppSettings.imageDurationSec) }
    var pollinationsEnabled by remember { mutableStateOf(currentAppSettings.pollinationsEnabled) }
    var globalFallbackEnabled by remember { mutableStateOf(currentAppSettings.globalFallbackEnabled) }
    var fullDeviceInternetCutoffEnabled by remember { mutableStateOf(currentAppSettings.fullDeviceInternetCutoffEnabled) }
    var vpnAutoConnectOnAutopilot by remember { mutableStateOf(currentAppSettings.vpnAutoConnectOnAutopilot) }

    // Dropdown Collapsible States (ALL CLOSED BY DEFAULT)
    var expandedPublishing by remember { mutableStateOf(false) }
    var expandedAgent by remember { mutableStateOf(false) }
    var expandedVoice by remember { mutableStateOf(false) }
    var expandedVisual by remember { mutableStateOf(false) }
    var expandedCaptions by remember { mutableStateOf(false) }
    var expandedEngine by remember { mutableStateOf(false) }
    var expandedFailSafe by remember { mutableStateOf(false) }
    var expandedKeyboard by remember { mutableStateOf(false) }
    var expandedNetworkShield by remember { mutableStateOf(false) }

    // Keyboard & Glitch Keyboard Branding Settings
    var keyboardName by remember { mutableStateOf(if (currentAppSettings.keyboardName.isEmpty() || currentAppSettings.keyboardName == "Glitch Keyboard") "GLITCH KEYBOARD" else currentAppSettings.keyboardName) }
    var keyboardPackage by remember { mutableStateOf(if (currentAppSettings.keyboardPackage.isEmpty() || currentAppSettings.keyboardPackage == "com.example.glitchboard.ime") context.packageName else currentAppSettings.keyboardPackage) }
    var autoSwitchKeyboard by remember { mutableStateOf(currentAppSettings.autoSwitchKeyboard) }
    var glitchKeyboardTheme by remember { mutableStateOf("Cyberpunk Neon") }
    var glitchHapticEnabled by remember { mutableStateOf(true) }
    var glitchSoundEnabled by remember { mutableStateOf(false) }
    var glitchAiCompletion by remember { mutableStateOf(true) }
    var testInputText by remember { mutableStateOf("") }

    // Sync UI when cloud settings finish loading
    LaunchedEffect(currentAgentSettings) {
        agentName = currentAgentSettings.general.agentName
        agentMode = currentAgentSettings.general.mode
        agentCategory = currentAgentSettings.general.category

        language = currentAgentSettings.languageAndVoice.language
        voiceGender = currentAgentSettings.languageAndVoice.voiceGender
        voiceAccent = currentAgentSettings.languageAndVoice.voiceAccent
        speechRate = currentAgentSettings.languageAndVoice.speechRate
        pitch = currentAgentSettings.languageAndVoice.pitch

        orientation = currentAgentSettings.videoFormat.orientation
        aspectRatio = currentAgentSettings.videoFormat.aspectRatio
        resolution = currentAgentSettings.videoFormat.resolution
        fps = currentAgentSettings.videoFormat.fps
        maxDurationSeconds = currentAgentSettings.videoFormat.maxDurationSeconds

        captionsEnabled = currentAgentSettings.captionsAndSubtitles.captionsEnabled
        captionStyle = currentAgentSettings.captionsAndSubtitles.captionStyle
        highlightColor = currentAgentSettings.captionsAndSubtitles.highlightColor

        autoGenerate = currentAgentSettings.schedulingAndAutomation.autoGenerate
        delayBetweenVideosSeconds = currentAgentSettings.schedulingAndAutomation.delayBetweenVideosSeconds
        dailyVideoLimit = currentAgentSettings.schedulingAndAutomation.dailyVideoLimit

        visualStyle = currentAgentSettings.contentAndStyle.visualStyle
        visualRenderMode = currentAgentSettings.contentAndStyle.visualRenderMode.ifBlank { currentAppSettings.visualRenderMode }
        htmlTheme = currentAgentSettings.contentAndStyle.htmlTheme.ifBlank { currentAppSettings.htmlTheme }
        backgroundMusic = currentAgentSettings.contentAndStyle.backgroundMusic
        bgMusicVolume = currentAgentSettings.contentAndStyle.bgMusicVolume

        autoPublish = currentAgentSettings.publishing.autoPublish
        selectedPlatforms = currentAgentSettings.publishing.platforms.toSet()
        autoGenerateTitle = currentAgentSettings.publishing.autoGenerateTitle
        autoGenerateDescription = currentAgentSettings.publishing.autoGenerateDescription
        autoGenerateHashtags = currentAgentSettings.publishing.autoGenerateHashtags
    }

    LaunchedEffect(currentAppSettings) {
        serverUrl = currentAppSettings.serverUrl
        apiKey = currentAppSettings.apiKey
        engineMode = currentAppSettings.engineMode
        imageDurationSec = currentAppSettings.imageDurationSec
        pollinationsEnabled = currentAppSettings.pollinationsEnabled
        globalFallbackEnabled = currentAppSettings.globalFallbackEnabled
        fullDeviceInternetCutoffEnabled = currentAppSettings.fullDeviceInternetCutoffEnabled
        vpnAutoConnectOnAutopilot = currentAppSettings.vpnAutoConnectOnAutopilot
        if (visualRenderMode.isBlank()) {
            visualRenderMode = currentAppSettings.visualRenderMode
        }
        if (htmlTheme.isBlank()) {
            htmlTheme = currentAppSettings.htmlTheme
        }
    }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Agent & Studio Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Glitch Agent Configuration & Auto-Publishing",
                                fontSize = 11.sp,
                                color = CyberCyan
                            )
                        }
                    },
                    navigationIcon = {},
                    actions = {
                        if (isAgentLoading) {
                            CircularProgressIndicator(
                                color = CyberCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 12.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BackgroundPermissionsCard()
            }

            // ----------------------------------------------------
            // 1. AUTO-PUBLISHING & AUTOPILOT CONTROLS (Dropdown - Default Closed)
            // ----------------------------------------------------
            item {
                GlitchCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (autoPublish) NeonGreen else CyberCyan
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedPublishing = !expandedPublishing }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (autoPublish) NeonGreen else CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "AUTO-PUBLISHING ENGINE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoPublish) NeonGreen else CyberCyan
                                )
                                Text(
                                    text = if (autoPublish) "Active Autopilot (${selectedPlatforms.size} platforms)" else "Tap to configure auto-upload & schedule",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = { expandedPublishing = !expandedPublishing }) {
                            Icon(
                                imageVector = if (expandedPublishing) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedPublishing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable Auto-Publishing",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Switch(
                                    checked = autoPublish,
                                    onCheckedChange = { autoPublish = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = NeonGreen,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    ),
                                    modifier = Modifier.testTag("auto_publish_switch")
                                )
                            }

                            Text(
                                text = "Publishing Target Platforms:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            val availablePlatforms = listOf(
                                "YouTube Shorts",
                                "Instagram Reels",
                                "TikTok",
                                "Facebook Reels"
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availablePlatforms.forEach { platform ->
                                    val isSelected = selectedPlatforms.contains(platform)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedPlatforms = if (isSelected) {
                                                if (selectedPlatforms.size > 1) selectedPlatforms - platform else selectedPlatforms
                                            } else {
                                                selectedPlatforms + platform
                                            }
                                        },
                                        label = { Text(platform, fontSize = 12.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = NeonGreen,
                                            containerColor = DarkSurfaceElevated,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) NeonGreen else DarkBorder,
                                            selectedBorderColor = NeonGreen
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = DarkBorder)

                            Text(
                                text = "AI Metadata & SEO Automation",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Auto-Generate Viral Title", color = TextPrimary, fontSize = 12.sp)
                                Switch(
                                    checked = autoGenerateTitle,
                                    onCheckedChange = { autoGenerateTitle = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Auto-Generate Trending Hashtags", color = TextPrimary, fontSize = 12.sp)
                                Switch(
                                    checked = autoGenerateHashtags,
                                    onCheckedChange = { autoGenerateHashtags = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Auto-Generate SEO Description", color = TextPrimary, fontSize = 12.sp)
                                Switch(
                                    checked = autoGenerateDescription,
                                    onCheckedChange = { autoGenerateDescription = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = DarkBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Autonomous Generation Loop", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Allows agent to auto-schedule video runs", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = autoGenerate,
                                    onCheckedChange = { autoGenerate = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Text(text = "Daily Video Limit: $dailyVideoLimit videos/day", color = TextPrimary, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 3, 5, 10, 20).forEach { limit ->
                                    val isSel = dailyVideoLimit == limit
                                    SelectablePill(
                                        text = "$limit",
                                        selected = isSel,
                                        onClick = { dailyVideoLimit = limit },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val delayLabel = when (delayBetweenVideosSeconds) {
                                30 -> "30s"
                                60 -> "1m"
                                120 -> "2m"
                                300 -> "5m"
                                600 -> "10m"
                                1800 -> "30m"
                                3600 -> "1h"
                                7200 -> "2h"
                                14400 -> "4h"
                                86400 -> "24h"
                                else -> "${delayBetweenVideosSeconds}s"
                            }

                            Text(text = "Delay Between Automated Videos: $delayLabel", color = TextPrimary, fontSize = 12.sp)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val delays = listOf(
                                    30 to "30s",
                                    60 to "1m",
                                    120 to "2m",
                                    300 to "5m",
                                    600 to "10m",
                                    1800 to "30m",
                                    3600 to "1h",
                                    7200 to "2h",
                                    14400 to "4h",
                                    86400 to "24h"
                                )
                                delays.forEach { (sec, label) ->
                                    val isSel = delayBetweenVideosSeconds == sec
                                    SelectablePill(
                                        text = label,
                                        selected = isSel,
                                        onClick = { delayBetweenVideosSeconds = sec }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. AGENT IDENTITY & MODE (Dropdown - Default Closed)
            // ----------------------------------------------------
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedAgent = !expandedAgent }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = "AI AGENT IDENTITY & MODE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Text(text = "$agentName ($agentCategory)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { expandedAgent = !expandedAgent }) {
                            Icon(
                                imageVector = if (expandedAgent) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedAgent) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            OutlinedTextField(
                                value = agentName,
                                onValueChange = { agentName = it },
                                label = { Text("Agent Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Text(text = "Operation Mode", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("autonomous", "semi_autonomous", "custom").forEach { mode ->
                                    val isSel = agentMode == mode
                                    SelectablePill(
                                        text = mode.replace("_", " ").capitalizeWords(),
                                        selected = isSel,
                                        onClick = { agentMode = mode },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(text = "Content Category Niche", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            val categories = com.example.util.UnifiedCategories.ALL
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                categories.forEach { cat ->
                                    val isSel = agentCategory.equals(cat, ignoreCase = true)
                                    SelectablePill(
                                        text = cat,
                                        selected = isSel,
                                        onClick = {
                                            agentCategory = cat
                                            viewModel.updateUnifiedCategory(cat)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 3. LANGUAGE, VOICE & SPEECH SYNTHESIS (Dropdown - Default Closed)
            // ----------------------------------------------------
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedVoice = !expandedVoice }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = "LANGUAGE, VOICE & SPEECH", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                                Text(text = "$language ($voiceGender, ${speechRate}x)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { expandedVoice = !expandedVoice }) {
                            Icon(
                                imageVector = if (expandedVoice) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedVoice) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            Text(text = "Narration Language", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Hindi", "English", "Hinglish", "Spanish").forEach { lang ->
                                    val isSel = language == lang
                                    SelectablePill(
                                        text = lang,
                                        selected = isSel,
                                        onClick = { language = lang },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(text = "Voice Gender", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("male", "female").forEach { g ->
                                    val isSel = voiceGender == g
                                    SelectablePill(
                                        text = g.capitalizeWords(),
                                        selected = isSel,
                                        onClick = { voiceGender = g },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(text = "Voice Accent / Regional Model", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Indian Standard", "American", "British", "Neutral Global").forEach { acc ->
                                    val isSel = voiceAccent == acc
                                    SelectablePill(
                                        text = acc,
                                        selected = isSel,
                                        onClick = { voiceAccent = acc }
                                    )
                                }
                            }

                            Text(text = "Speech Rate (${speechRate}x)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { rate ->
                                    val isSel = speechRate == rate
                                    SelectablePill(
                                        text = "${rate}x",
                                        selected = isSel,
                                        onClick = { speechRate = rate },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(text = "Voice Pitch", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("low", "medium", "high").forEach { p ->
                                    val isSel = pitch == p
                                    SelectablePill(
                                        text = p.capitalizeWords(),
                                        selected = isSel,
                                        onClick = { pitch = p },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 4. VISUAL STYLE & VIDEO FORMAT (Dropdown - Default Closed)
            // ----------------------------------------------------
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedVisual = !expandedVisual }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = "VISUAL ART STYLE & FORMAT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Text(text = "$visualStyle ($aspectRatio, $resolution)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { expandedVisual = !expandedVisual }) {
                            Icon(
                                imageVector = if (expandedVisual) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedVisual) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            // Visual Rendering Pipeline Mode
                            Text(text = "Visual Generation Mode", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isImg = visualRenderMode != "HTML_CAPTION_ANIMATION"
                                SelectablePill(
                                    text = "🖼️ AI Image Slideshow",
                                    selected = isImg,
                                    onClick = { visualRenderMode = "AI_IMAGES" },
                                    modifier = Modifier.weight(1f)
                                )
                                SelectablePill(
                                    text = "⚡ HTML Kinetic Animation",
                                    selected = !isImg,
                                    onClick = { visualRenderMode = "HTML_CAPTION_ANIMATION" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (visualRenderMode == "HTML_CAPTION_ANIMATION") {
                                Text(
                                    text = "HTML Kinetic Motion Theme",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Creates dynamic 60fps HTML5/CSS3 animated slides with neon badges, glowing headlines, and progress bars synced with voice-over.",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("Cyberpunk Dark", "Matrix Green", "OLED Minimal", "Neon Sunset").forEach { thm ->
                                        val isSel = htmlTheme == thm
                                        SelectablePill(
                                            text = thm,
                                            selected = isSel,
                                            onClick = { htmlTheme = thm }
                                        )
                                    }
                                }
                            } else {
                                Text(text = "Art / Image Generation Style", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("realistic", "cinematic", "anime", "3d_animation", "minimalist").forEach { st ->
                                        val isSel = visualStyle == st
                                        SelectablePill(
                                            text = st.replace("_", " ").capitalizeWords(),
                                            selected = isSel,
                                            onClick = { visualStyle = st }
                                        )
                                    }
                                }
                            }

                            Text(text = "Aspect Ratio & Orientation", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val ratios = listOf("9:16" to "vertical", "16:9" to "horizontal", "1:1" to "square")
                                ratios.forEach { (ratio, ori) ->
                                    val isSel = aspectRatio == ratio
                                    SelectablePill(
                                        text = "$ratio ($ori)",
                                        selected = isSel,
                                        onClick = {
                                            aspectRatio = ratio
                                            orientation = ori
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Resolution selection
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "Resolution", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("720p", "1080p", "4K").forEach { res ->
                                        val isSel = resolution == res
                                        SelectablePill(
                                            text = res,
                                            selected = isSel,
                                            onClick = { resolution = res },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // FPS selection
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "FPS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(30, 60).forEach { f ->
                                        val isSel = fps == f
                                        SelectablePill(
                                            text = "$f fps",
                                            selected = isSel,
                                            onClick = { fps = f },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Text(text = "Target Video Duration: ~${maxDurationSeconds}s", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 30, 60, 90).forEach { dur ->
                                    val isSel = maxDurationSeconds == dur
                                    SelectablePill(
                                        text = "${dur}s",
                                        selected = isSel,
                                        onClick = { maxDurationSeconds = dur },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Scene Pacing", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SelectablePill(text = "3s (Fast)", selected = imageDurationSec == 3, onClick = { imageDurationSec = 3 })
                                    SelectablePill(text = "6s (Relaxed)", selected = imageDurationSec == 6, onClick = { imageDurationSec = 6 })
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Ambient Background Music", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Subtle cinematic background audio stream", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = backgroundMusic,
                                    onCheckedChange = { backgroundMusic = it },
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

            // ----------------------------------------------------
            // 5. SUBTITLES & KARAOKE CAPTIONS (Dropdown - Default Closed)
            // ----------------------------------------------------
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCaptions = !expandedCaptions }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = "CAPTIONS & SUBTITLES", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                Text(text = if (captionsEnabled) "Enabled ($captionStyle)" else "Disabled", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { expandedCaptions = !expandedCaptions }) {
                            Icon(
                                imageVector = if (expandedCaptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedCaptions) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Enable Video Subtitles", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = captionsEnabled,
                                    onCheckedChange = { captionsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = Color(0xFFFFD700),
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            if (captionsEnabled) {
                                Text(text = "Caption Animation Style", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(
                                        "hormozi_bold" to "👑 Hormozi Viral",
                                        "mrbeast_punch" to "⚡ MrBeast Neon",
                                        "cyber_glow" to "🌌 Cyber Cyan",
                                        "fire_red" to "🔥 Fire Red",
                                        "minimalist_box" to "🖤 Dark Boxed",
                                        "classic_subtitles" to "🎤 Classic"
                                    ).forEach { (styleKey, label) ->
                                        val isSel = captionStyle == styleKey
                                        SelectablePill(
                                            text = label,
                                            selected = isSel,
                                            onClick = { captionStyle = styleKey }
                                        )
                                    }
                                }

                                Text(text = "Active Word Highlight Color", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                val colorPresets = listOf(
                                    "#FFE600" to Color(0xFFFFE600),
                                    "#00FF66" to Color(0xFF00FF66),
                                    "#00FFE0" to CyberCyan,
                                    "#FF007F" to Color(0xFFFF007F),
                                    "#FF3300" to Color(0xFFFF3300),
                                    "#FFFFFF" to Color.White
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    colorPresets.forEach { (hex, col) ->
                                        val isSel = highlightColor.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                                .border(
                                                    width = if (isSel) 3.dp else 1.dp,
                                                    color = if (isSel) TextPrimary else DarkBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable { highlightColor = hex },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = DarkBackground,
                                                    modifier = Modifier.size(20.dp)
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

            // ----------------------------------------------------
            // 6. ENGINE ARCHITECTURE & REMOTE BACKEND (Dropdown - Default Closed)
            // ----------------------------------------------------
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedEngine = !expandedEngine }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Column {
                                Text(text = "ENGINE ARCHITECTURE & SERVERS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Text(text = "Mode: $engineMode", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        IconButton(onClick = { expandedEngine = !expandedEngine }) {
                            Icon(
                                imageVector = if (expandedEngine) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedEngine) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            listOf("Smart Auto", "On-Device Studio Engine", "Remote Server Only").forEach { mode ->
                                val isSel = mode == engineMode
                                SelectablePill(
                                    text = mode,
                                    selected = isSel,
                                    onClick = { engineMode = mode },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                )
                            }

                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text("Server Base URL") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("Optional Server API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Pollinations AI Stream Fallback", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Instant high-speed fallback for image synthesis", color = TextSecondary, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = pollinationsEnabled,
                                    onCheckedChange = { pollinationsEnabled = it },
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

            // ----------------------------------------------------
            // 7. FAIL-SAFE & GLOBAL FALLBACK ENGINE (Master On/Off for All Fallbacks)
            // ----------------------------------------------------
            item {
                GlitchCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fallback_engine_card"),
                    borderColor = if (globalFallbackEnabled) NeonGreen else CoralPink
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedFailSafe = !expandedFailSafe }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background((if (globalFallbackEnabled) NeonGreen else CoralPink).copy(alpha = 0.15f))
                                    .border(1.dp, (if (globalFallbackEnabled) NeonGreen else CoralPink).copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (globalFallbackEnabled) NeonGreen else CoralPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "FAIL-SAFE & FALLBACKS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (globalFallbackEnabled) NeonGreen else CoralPink
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background((if (globalFallbackEnabled) NeonGreen else CoralPink).copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (globalFallbackEnabled) "BEST-EFFORT FALLBACKS ON" else "STRICT MODE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (globalFallbackEnabled) NeonGreen else CoralPink
                                        )
                                    }
                                }
                                Text(
                                    text = if (globalFallbackEnabled) {
                                        "Enabled fallbacks are best-effort and do not guarantee completion."
                                    } else {
                                        "Fallbacks controlled by this setting are disabled where supported."
                                    },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = { expandedFailSafe = !expandedFailSafe }) {
                            Icon(
                                imageVector = if (expandedFailSafe) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedFailSafe) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            // Master Switch Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, (if (globalFallbackEnabled) NeonGreen else CoralPink).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = "Master Fallback Controller",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (globalFallbackEnabled)
                                                "Enabled: features with a configured alternate may try it. Script and scene prompts require provider output; this setting never guarantees completion."
                                            else
                                                "Disabled: provider-only stages stop on failure. Some independently configured provider alternatives may still be available.",
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }

                                    Switch(
                                        checked = globalFallbackEnabled,
                                        onCheckedChange = { globalFallbackEnabled = it },
                                        modifier = Modifier.testTag("global_fallback_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkBackground,
                                            checkedTrackColor = NeonGreen,
                                            uncheckedThumbColor = CoralPink,
                                            uncheckedTrackColor = DarkSurfaceElevated
                                        )
                                    )
                                }
                            }

                            // Quick Preset Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (globalFallbackEnabled) NeonGreen.copy(alpha = 0.15f) else DarkSurfaceElevated)
                                        .border(
                                            width = if (globalFallbackEnabled) 1.5.dp else 1.dp,
                                            color = if (globalFallbackEnabled) NeonGreen else DarkBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { globalFallbackEnabled = true }
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "BEST-EFFORT FALLBACKS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (globalFallbackEnabled) NeonGreen else TextSecondary
                                        )
                                        Text(
                                            text = "Try configured alternatives",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (!globalFallbackEnabled) CoralPink.copy(alpha = 0.15f) else DarkSurfaceElevated)
                                        .border(
                                            width = if (!globalFallbackEnabled) 1.5.dp else 1.dp,
                                            color = if (!globalFallbackEnabled) CoralPink else DarkBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { globalFallbackEnabled = false }
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "STRICT MODE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!globalFallbackEnabled) CoralPink else TextSecondary
                                        )
                                        Text(
                                            text = "Disable supported fallbacks",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            // Component Fallback Matrix
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated.copy(alpha = 0.7f))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "MANAGED PIPELINE FAIL-SAFES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    letterSpacing = 0.8.sp
                                )

                                val pipelineItems = listOf(
                                    Triple("AI Script & Prompts", "Configured AI provider", "Provider-only; errors stop generation"),
                                    Triple("Image Generation", "Configured image provider", if (globalFallbackEnabled) "Configured Pollinations alternate may be tried" else "Fallback disabled"),
                                    Triple("Voice Synthesis", "Edge neural voice provider", if (globalFallbackEnabled) "Web/Android speech may be tried; no synthetic-tone fallback" else "Fallback disabled"),
                                    Triple("Background Audio", "Configured CDN track", if (globalFallbackEnabled) "Procedural ambient audio may be used" else "Fallback disabled")
                                )

                                pipelineItems.forEach { (title, primary, backup) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            Text(text = "$primary → $backup", fontSize = 10.sp, color = if (globalFallbackEnabled) TextSecondary else CoralPink.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 8. GLITCH KEYBOARD & INPUT METHOD (Branded Custom Keyboard & Permission Flow)
            // ----------------------------------------------------
            item {
                GlitchCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (keyboardName.contains("GLITCH", ignoreCase = true)) NeonGreen else CyberCyan
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedKeyboard = !expandedKeyboard }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = if (keyboardName.contains("GLITCH", ignoreCase = true)) NeonGreen else CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "GLITCH KEYBOARD", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    Box(
                                        modifier = Modifier
                                            .background(NeonGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("CUSTOM IME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                    }
                                }
                                Text(
                                    text = "Active: $keyboardName",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = { expandedKeyboard = !expandedKeyboard }) {
                            Icon(
                                imageVector = if (expandedKeyboard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedKeyboard) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = DarkBorder)

                            // Quick Select GLITCH KEYBOARD card banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated, shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, NeonGreen, shape = RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "GLITCH KEYBOARD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                        Text(
                                            text = "Custom-branded high-speed IME with AI Auto-Completion for Video Scripts & Hashtags",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            keyboardName = "GLITCH KEYBOARD"
                                            keyboardPackage = context.packageName
                                            try {
                                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                                imm?.showInputMethodPicker()
                                                Toast.makeText(context, "GLITCH KEYBOARD selected! Opening IME Picker...", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Picker Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkBackground),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SELECT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = "GLITCH KEYBOARD Permission & Setup Flow",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                            context.startActivity(intent)
                                            Toast.makeText(context, "Enable Glitch Keyboard in System IME list", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("1. Enable IME", fontSize = 10.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                            imm?.showInputMethodPicker()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricViolet),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                                ) {
                                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("2. Set Default", fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = DarkBorder)

                            Text(
                                text = "GLITCH KEYBOARD Branding & Styling",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(text = "Keyboard Visual Theme", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Cyberpunk Neon", "Glitch Dark", "OLED Black", "Matrix Green").forEach { thm ->
                                    val isSel = glitchKeyboardTheme == thm
                                    SelectablePill(
                                        text = thm,
                                        selected = isSel,
                                        onClick = { glitchKeyboardTheme = thm }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Haptic Vibration Feedback", color = TextPrimary, fontSize = 11.sp)
                                Switch(
                                    checked = glitchHapticEnabled,
                                    onCheckedChange = { glitchHapticEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = NeonGreen,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Keypress Audio Sound", color = TextPrimary, fontSize = 11.sp)
                                Switch(
                                    checked = glitchSoundEnabled,
                                    onCheckedChange = { glitchSoundEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = CyberCyan,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "AI Script Auto-Completion", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Predictive suggestions for prompt editing", color = TextSecondary, fontSize = 9.sp)
                                }
                                Switch(
                                    checked = glitchAiCompletion,
                                    onCheckedChange = { glitchAiCompletion = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = NeonGreen,
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = DarkSurfaceElevated
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Test Input Field for GLITCH KEYBOARD
                            OutlinedTextField(
                                value = testInputText,
                                onValueChange = { testInputText = it },
                                label = { Text("⌨️ Test GLITCH KEYBOARD typing here...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                             )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 8. NETWORK & FULL DEVICE INTERNET CONTROL (Default: OFF)
            // ----------------------------------------------------
            item {
                GlitchCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (fullDeviceInternetCutoffEnabled) NeonPink else DarkBorder
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedNetworkShield = !expandedNetworkShield }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnLock,
                                    contentDescription = null,
                                    tint = if (fullDeviceInternetCutoffEnabled) NeonPink else NeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "FULL DEVICE INTERNET SHIELD / ISOLATION",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fullDeviceInternetCutoffEnabled) NeonPink else TextPrimary
                                    )
                                    Text(
                                        text = if (fullDeviceInternetCutoffEnabled) "⚠️ Active: Phone internet routed through VPN Shield" else "✓ Inactive (Default): Normal Phone Internet Running",
                                        fontSize = 11.sp,
                                        color = if (fullDeviceInternetCutoffEnabled) NeonPink else NeonGreen
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (expandedNetworkShield) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        AnimatedVisibility(visible = expandedNetworkShield) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(color = DarkBorder)

                                // Main Toggle: Full Device Internet Cutoff / VPN Shield
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Cut Off / Shield Full Device Internet",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Jab ye option OFF rahega (Default), tab phone ka internet normal chalega. Jab ON karenge tab hi full phone traffic tunnel/shield hoga.",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = fullDeviceInternetCutoffEnabled,
                                        onCheckedChange = { isEnabled ->
                                            fullDeviceInternetCutoffEnabled = isEnabled
                                            val currentSet = viewModel.settingsFlow.value
                                            val updated = currentSet.copy(fullDeviceInternetCutoffEnabled = isEnabled)
                                            viewModel.updateAppSettings(updated)
                                            if (isEnabled) {
                                                (context as? com.example.MainActivity)?.checkAndRequestVpnPermission()
                                                    ?: com.example.vpn.VpnGateManager.autoConnectFastest(context)
                                                Toast.makeText(context, "Full Device Internet Shield Activated", Toast.LENGTH_SHORT).show()
                                            } else {
                                                com.example.vpn.VpnGateManager.disconnect(context)
                                                Toast.makeText(context, "Full Device Internet Restored to Normal", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkBackground,
                                            checkedTrackColor = NeonPink,
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = DarkSurfaceElevated
                                        )
                                    )
                                }

                                HorizontalDivider(color = DarkBorder)

                                // Secondary Toggle: Autopilot Auto-VPN
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Connect VPN on Autopilot Job",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Automatically activate VPN during background scheduled tasks (Default: OFF)",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = vpnAutoConnectOnAutopilot,
                                        onCheckedChange = { isEnabled ->
                                            vpnAutoConnectOnAutopilot = isEnabled
                                            val currentSet = viewModel.settingsFlow.value
                                            val updated = currentSet.copy(vpnAutoConnectOnAutopilot = isEnabled)
                                            viewModel.updateAppSettings(updated)
                                        },
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
            }

            // ----------------------------------------------------
            // 9. SYSTEM LOGS LINK
            // ----------------------------------------------------
            item {
                OutlinedButton(
                    onClick = onNavigateToLogs,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Studio & Generation Logs")
                }
            }

            // ----------------------------------------------------
            // 10. LEGAL, PRIVACY POLICY & EDUCATIONAL PURPOSE
            // ----------------------------------------------------
            item {
                LegalAndComplianceCard()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ----------------------------------------------------
            // 11. SAVE & SYNC BUTTON
            // ----------------------------------------------------

            item {
                NeonButton(
                    text = if (isSaving) "SYNCHRONIZING..." else "SAVE AGENT CONFIGURATION",
                    leadingIcon = if (isSaving) null else Icons.Default.Save,
                    enabled = !isSaving,
                    color = if (autoPublish) NeonGreen else CyberCyan,
                    onClick = {
                        isSaving = true

                        // Build updated GlitchAgentSettings object
                        val updatedAgentSettings = GlitchAgentSettings(
                            general = GeneralSettings(
                                agentName = agentName.trim(),
                                mode = agentMode,
                                category = agentCategory
                            ),
                            languageAndVoice = LanguageAndVoiceSettings(
                                language = language,
                                voiceGender = voiceGender,
                                voiceAccent = voiceAccent,
                                speechRate = speechRate,
                                pitch = pitch
                            ),
                            videoFormat = VideoFormatSettings(
                                orientation = orientation,
                                aspectRatio = aspectRatio,
                                resolution = resolution,
                                fps = fps,
                                maxDurationSeconds = maxDurationSeconds
                            ),
                            captionsAndSubtitles = CaptionsAndSubtitlesSettings(
                                captionsEnabled = captionsEnabled,
                                captionStyle = captionStyle,
                                highlightColor = highlightColor
                            ),
                            schedulingAndAutomation = SchedulingAndAutomationSettings(
                                autoGenerate = autoGenerate,
                                delayBetweenVideosSeconds = delayBetweenVideosSeconds,
                                dailyVideoLimit = dailyVideoLimit
                            ),
                            contentAndStyle = ContentAndStyleSettings(
                                visualStyle = visualStyle,
                                visualRenderMode = visualRenderMode,
                                htmlTheme = htmlTheme,
                                backgroundMusic = backgroundMusic,
                                bgMusicVolume = bgMusicVolume
                            ),
                            publishing = PublishingSettings(
                                autoPublish = autoPublish,
                                platforms = selectedPlatforms.toList(),
                                autoGenerateTitle = autoGenerateTitle,
                                autoGenerateDescription = autoGenerateDescription,
                                autoGenerateHashtags = autoGenerateHashtags
                            )
                        )

                        // Build updated AppSettings
                        val calculatedHours = if (delayBetweenVideosSeconds >= 3600) (delayBetweenVideosSeconds / 3600).coerceAtLeast(1) else 1
                        val updatedAppSettings = currentAppSettings.copy(
                            serverUrl = serverUrl.trim(),
                            apiKey = apiKey.trim(),
                            engineMode = engineMode,
                            defaultLanguage = language,
                            defaultCategory = agentCategory,
                            autoCategory = agentCategory,
                            defaultCategoryId = com.example.util.UnifiedCategories.getCategoryId(agentCategory),
                            defaultDuration = maxDurationSeconds,
                            defaultStyle = visualStyle,
                            defaultAspect = aspectRatio,
                            visualRenderMode = visualRenderMode,
                            htmlTheme = htmlTheme,
                            imageDurationSec = imageDurationSec,
                            showCaptions = captionsEnabled,
                            autoPublishEnabled = autoPublish,
                            pollinationsEnabled = pollinationsEnabled,
                            keyboardName = keyboardName,
                            keyboardPackage = keyboardPackage,
                            autoSwitchKeyboard = autoSwitchKeyboard,
                            scheduleIntervalHours = calculatedHours,
                            scheduleDelaySeconds = delayBetweenVideosSeconds.toLong(),
                            smartTimingMode = "FIXED_INTERVAL",
                            globalFallbackEnabled = globalFallbackEnabled,
                            fullDeviceInternetCutoffEnabled = fullDeviceInternetCutoffEnabled,
                            vpnAutoConnectOnAutopilot = vpnAutoConnectOnAutopilot
                        )

                        // Save to ViewModel & Firestore
                        viewModel.saveAgentConfiguration(
                            agentSettings = updatedAgentSettings,
                            appSettings = updatedAppSettings
                        ) { success ->
                            isSaving = false
                            val msg = if (success) {
                                "Agent settings & Auto-Publish synced to Cloud!"
                            } else {
                                "Saved locally (Cloud sync pending network/auth)"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button")
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

private fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
