package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.auth.LoginActivity
import com.example.data.model.ProjectSummary
import com.example.service.YouTubeAdSkipperService
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BackgroundPermissionsCard
import com.example.ui.components.GlitchCard
import com.example.ui.components.AgentLearnManualCard
import com.example.ui.components.HomeDockItem
import com.example.ui.components.HomeLeftDock
import com.example.ui.components.LegalAndComplianceCard
import com.example.ui.components.NeonButton
import com.example.ui.components.PrivateDnsShieldCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.YouTubeAdSkipperCard
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.util.SocialShareHelper
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCreate: () -> Unit = {},
    onNavigateToProgress: (String) -> Unit,
    onNavigateToPreview: (String, String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToYouTubeManager: () -> Unit,
    onNavigateToKeyboardPlayground: () -> Unit = {},
    onNavigateToVpnShield: () -> Unit = {},
    onNavigateToTrendPilot: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isPipelineGenerating by viewModel.isPipelineGenerating.collectAsState()
    val activePipelineInfo by viewModel.activePipelineInfo.collectAsState()
    val livePipelineProgress by viewModel.livePipelineProgress.collectAsState()
    val appUpdateData by viewModel.appUpdateData.collectAsState()
    val isUpdateDismissed by viewModel.isUpdateDismissed.collectAsState()
    val vpnStatus by com.example.vpn.VpnGateManager.status.collectAsState()
    val isAdSkipperRunning by YouTubeAdSkipperService.isServiceActive.collectAsState()
    var isAdSkipperPermitted by remember { mutableStateOf(false) }
    var selectedDockItem by remember { mutableStateOf(HomeDockItem.AUTOPILOT) }

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val isLoggedIn = currentUser != null

    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.checkServerHealth()
        viewModel.loadProjects()
        viewModel.checkForAppUpdates()
        currentUser = FirebaseAuth.getInstance().currentUser
        isAdSkipperPermitted = YouTubeAdSkipperService.isAccessibilityServiceEnabled(context)
    }

    // Intercept system Back press to cleanly return to Auto-Pilot
    if (selectedDockItem != HomeDockItem.AUTOPILOT) {
        BackHandler {
            selectedDockItem = HomeDockItem.AUTOPILOT
        }
    }

    val (headerTitle, headerSubtitle, headerColor) = when (selectedDockItem) {
        HomeDockItem.AUTOPILOT -> Triple("AUTONOMOUS STUDIO", "AI Auto-Pilot & Video Engine", NeonGreen)
        HomeDockItem.STUDIO -> Triple("STUDIO MANAGEMENT", "Autonomous Channel & Video Studio", CyberCyan)
        HomeDockItem.SEO_STUDIO -> Triple("SEO VIRAL LAB", "Keywords, Tags & SEO Scores", ElectricViolet)
        HomeDockItem.PLAYLISTS -> Triple("SMART PLAYLISTS", "Binge-Watch Series Hub", NeonPink)
        HomeDockItem.COMMUNITY -> Triple("COMMUNITY ENGAGEMENT", "Interactive Polls & Posts", Color(0xFFF59E0B))
        HomeDockItem.ANALYTICS -> Triple("PERFORMANCE ANALYTICS", "Intelligence & Retention Metrics", CyberCyan)
        HomeDockItem.BACKGROUND -> Triple("BACKGROUND 24/7 ENGINE", "Autonomous Schedule & Optimization", NeonGreen)
        HomeDockItem.VPN -> Triple("VPN SHIELD", "High-Speed Encrypted Tunnel & Proxy", ElectricViolet)
        HomeDockItem.LEARN -> Triple("AGENT LEARN MANUAL", "Autonomous Documentation & Guide", Color(0xFF38BDF8))
        HomeDockItem.LEGAL -> Triple("LEGAL & COMPLIANCE", "Privacy & Safety Declarations", TextSecondary)
        HomeDockItem.VIDEOS -> Triple("VIDEO GALLERY", "Creations & Export Hub", NeonPink)
        HomeDockItem.ADS -> Triple("AD BLOCKER SHIELD", "Private DNS & Tracker Filter", NeonGreen)
        HomeDockItem.SETTING -> Triple("AGENT SETTINGS", "Content Niche & App Preferences", ElectricViolet)
        HomeDockItem.ACCOUNT -> Triple("GOOGLE ACCOUNT", "Connected Profiles & Auth", CyberCyan)
        HomeDockItem.KEYBOARD -> Triple("GLITCH KEYBOARD", "Smart AI Tools & Typing", CyberCyan)
        HomeDockItem.TREND_PILOT -> Triple("TREND PILOT", "Viral Video & Podcast Repurposer", Color(0xFFFF5252))
        HomeDockItem.CHATBOT -> Triple("AI CHATBOT ASSISTANT", "Interactive Chat & Script Generator", NeonGreen)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(headerColor)
                            )
                            Text(
                                text = headerTitle,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.8.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = headerSubtitle,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    if (selectedDockItem != HomeDockItem.AUTOPILOT) {
                        IconButton(onClick = { selectedDockItem = HomeDockItem.AUTOPILOT }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedDockItem = HomeDockItem.CHATBOT },
                        modifier = Modifier.testTag("open_chatbot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "AI Chatbot Assistant",
                            tint = NeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {},
        containerColor = DarkBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isCompact = maxWidth < 380.dp
            val horizPadding = if (isCompact) 6.dp else 10.dp

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. MAIN SCREEN CONTENT AREA (100% Full Width Edge-to-Edge)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizPadding)
                ) {
                    when (selectedDockItem) {
                        HomeDockItem.AUTOPILOT -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. YouTube Auto-Pilot Card (TOP PRIORITY COMPONENT)
                item {
                    Spacer(modifier = Modifier.height(2.dp))

                    GlitchCard(
                        borderColor = if (settings.autoPublishEnabled) NeonGreen else ElectricViolet,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val effectiveDelaySec = if (settings.scheduleDelaySeconds > 0L) {
                            settings.scheduleDelaySeconds
                        } else {
                            (settings.scheduleIntervalHours * 3600L).coerceAtLeast(30L)
                        }

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
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (settings.autoPublishEnabled) NeonGreen else ElectricViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "YOUTUBE AUTO-PILOT",
                                        fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (settings.autoPublishEnabled) NeonGreen else ElectricViolet,
                                        letterSpacing = 0.5.sp,
                                        maxLines = 1
                                    )
                                    Surface(
                                        color = if (settings.autoPublishEnabled) NeonGreen.copy(alpha = 0.15f) else ElectricViolet.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (settings.autoPublishEnabled) "AUTONOMOUS" else "STANDBY",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (settings.autoPublishEnabled) NeonGreen else ElectricViolet,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                val latestProject = dashboardState.recentProjects.firstOrNull()
                                val lastVideoTs = com.example.worker.AutomationSchedulerManager.parseTimestamp(latestProject?.createdAt)
                                val effectiveNextTs = com.example.worker.AutomationSchedulerManager.getEffectiveNextPublishTimestamp(
                                    lastVideoTimestamp = lastVideoTs,
                                    lastAutoPublishTimestamp = settings.lastAutoPublishTimestamp,
                                    intervalHours = settings.scheduleIntervalHours,
                                    delaySeconds = effectiveDelaySec
                                )
                                val schedulerText = if (settings.autoPublishEnabled) {
                                    val nextRunDate = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(effectiveNextTs))
                                    val intervalText = when {
                                        effectiveDelaySec < 60L -> "${effectiveDelaySec}s"
                                        effectiveDelaySec < 3600L -> "${effectiveDelaySec / 60}m"
                                        effectiveDelaySec == 3600L -> "1 hour"
                                        else -> "${effectiveDelaySec / 3600}h"
                                    }
                                    "Every $intervalText (Next: $nextRunDate)"
                                } else {
                                    "Automated scheduling is currently paused"
                                }
                                Text(
                                    text = schedulerText,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Switch(
                                checked = settings.autoPublishEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.updateScheduler(
                                        enabled = enabled,
                                        intervalHours = settings.scheduleIntervalHours,
                                        category = settings.autoCategory,
                                        delaySeconds = effectiveDelaySec,
                                        onAlert = { msg ->
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkBackground,
                                    checkedTrackColor = NeonGreen,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurfaceElevated
                                ),
                                modifier = Modifier.testTag("scheduler_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DarkSurfaceElevated, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "ACTIVE NICHE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            Text(
                                text = settings.autoCategory,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(com.example.util.UnifiedCategories.ALL) { cat ->
                                val isSelected = settings.autoCategory.equals(cat, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.updateUnifiedCategory(cat)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Unified category set to $cat 🎯",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    label = { Text(cat, fontSize = 10.5.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(11.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricViolet.copy(alpha = 0.35f),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = NeonGreen,
                                        containerColor = DarkSurfaceElevated,
                                        labelColor = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) ElectricViolet else DarkSurfaceElevated
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { selectedDockItem = HomeDockItem.STUDIO },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("youtube_manager_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CyberCyan
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(CyberCyan, ElectricViolet))
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open YouTube Studio Manager",
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Trend Pilot Container Card (Right below YouTube Auto Pilot)
                        GlitchCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trend_pilot_card"),
                            borderColor = NeonGreen
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Whatshot,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "TREND PILOT",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    Surface(
                                        color = ElectricViolet.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "VIRAL REPURPOSER",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricViolet,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Discover & repurpose viral trending YouTube videos, podcasts & transcripts into Shorts.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                NeonButton(
                                    text = "View Trend Pilot ➔",
                                    onClick = { onNavigateToTrendPilot() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("view_trend_pilot_button")
                                )
                            }
                        }
                    }
                }
            }
        }

                        HomeDockItem.SETTING -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToLogs = {},
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.KEYBOARD -> {
                            KeyboardPlaygroundScreen(
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.STUDIO -> {
                            YouTubeManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToPreview = onNavigateToPreview,
                                initialTab = 0,
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.SEO_STUDIO -> {
                            YouTubeManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToPreview = onNavigateToPreview,
                                initialTab = 1,
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.PLAYLISTS -> {
                            YouTubeManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToPreview = onNavigateToPreview,
                                initialTab = 2,
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.COMMUNITY -> {
                            YouTubeManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToPreview = onNavigateToPreview,
                                initialTab = 3,
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.ANALYTICS -> {
                            YouTubeManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToPreview = onNavigateToPreview,
                                initialTab = 4,
                                hideTopBar = true
                            )
                        }

                        HomeDockItem.LEARN -> {
                            // AGENT LEARN MANUAL VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    AgentLearnManualCard()
                                }
                            }
                        }

                        HomeDockItem.LEGAL -> {
                            // LEGAL, PRIVACY & COMPLIANCE VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    LegalAndComplianceCard()
                                }
                            }
                        }

                        HomeDockItem.VIDEOS -> {
                            // VIDEOS GALLERY & CREATIONS VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (dashboardState.recentProjects.isEmpty()) {
                                    item {
                                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Default.MovieCreation, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No video creations yet", fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                NeonButton(
                                                    text = "Launch YouTube Auto-Pilot",
                                                    onClick = { selectedDockItem = HomeDockItem.AUTOPILOT }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(dashboardState.recentProjects) { project ->
                                        RecentProjectRow(
                                            project = project,
                                            onClick = {
                                                if (project.video.isNotBlank()) {
                                                    onNavigateToPreview(project.video, project.title)
                                                }
                                            },
                                            onPublish = {
                                                viewModel.uploadProjectToYouTube(project) { percent, msg ->
                                                    android.widget.Toast.makeText(context, "[$percent%] $msg", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDelete = {
                                                viewModel.deleteProject(project.id)
                                                android.widget.Toast.makeText(context, "Video deleted", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HomeDockItem.VPN -> {
                            // VPN GATE TSUKUBA SHIELD VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    GlitchCard(
                                        borderColor = if (vpnStatus.state == com.example.vpn.VpnConnectionState.CONNECTED) NeonGreen else CyberCyan,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "🛡️ VPN GATE STATUS",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan
                                                )
                                                Surface(
                                                    color = if (vpnStatus.state == com.example.vpn.VpnConnectionState.CONNECTED) NeonGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (vpnStatus.state == com.example.vpn.VpnConnectionState.CONNECTED) "CONNECTED" else "DISCONNECTED",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (vpnStatus.state == com.example.vpn.VpnConnectionState.CONNECTED) NeonGreen else TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (vpnStatus.state == com.example.vpn.VpnConnectionState.CONNECTED)
                                                    "Secured via ${vpnStatus.currentServer?.countryLong} • Masked IP: ${vpnStatus.maskedIp}"
                                                else
                                                    "Connect to volunteer academic VPN relays from University of Tsukuba to protect channel uploads.",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            NeonButton(
                                                text = "Open Full VPN Control Panel",
                                                leadingIcon = Icons.Default.Shield,
                                                onClick = onNavigateToVpnShield
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeDockItem.BACKGROUND -> {
                            // 24/7 BACKGROUND ENGINE VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    BackgroundPermissionsCard(
                                        title = "Background 24/7 Engine Status"
                                    )
                                }
                            }
                        }

                        HomeDockItem.ADS -> {
                            // UNIVERSAL AI AD BLOCKER & PRIVATE DNS SHIELD VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    YouTubeAdSkipperCard()
                                }
                                item {
                                    PrivateDnsShieldCard()
                                }
                            }
                        }

                        HomeDockItem.ACCOUNT -> {
                            // GOOGLE ACCOUNT & PROFILE VIEW
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (currentUser?.photoUrl != null) {
                                                AsyncImage(
                                                    model = currentUser?.photoUrl,
                                                    contentDescription = "User Profile",
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .border(2.dp, CyberCyan, CircleShape)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(54.dp)
                                                )
                                            }

                                            Text(
                                                text = currentUser?.displayName ?: (if (isLoggedIn) "Google User" else "Not Signed In"),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = currentUser?.email ?: "Sign in with Google to enable 1-tap direct YouTube uploads",
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                textAlign = TextAlign.Center
                                            )

                                            NeonButton(
                                                text = if (isLoggedIn) "Switch / Manage Account" else "Sign In with Google",
                                                leadingIcon = Icons.Default.Login,
                                                onClick = {
                                                    context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                                        putExtra("FORCE_LOGIN", true)
                                                    })
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeDockItem.TREND_PILOT -> {
                            TrendPilotScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToCreateWithTopic = { _ ->
                                    onNavigateToCreate()
                                }
                            )
                        }

                        HomeDockItem.CHATBOT -> {
                            ChatbotScreen(
                                viewModel = viewModel,
                                onNavigateBack = { selectedDockItem = HomeDockItem.AUTOPILOT },
                                onNavigateToPreview = onNavigateToPreview,
                                hideTopBar = true
                            )
                        }

                        else -> {
                            // Default fallback to AUTOPILOT
                            selectedDockItem = HomeDockItem.AUTOPILOT
                        }
                    }
                }

                // 2. FLOATING OVERLAY MENU BUTTON & PANEL (Floating on the side over content)
                HomeLeftDock(
                    selectedItem = selectedDockItem,
                    onItemSelected = { item ->
                        selectedDockItem = item
                    },
                    isVpnActive = vpnStatus.state == com.example.vpn.VpnConnectionState.CONNECTED,
                    isAdSkipperActive = isAdSkipperRunning || isAdSkipperPermitted,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DockSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onReturnHome: () -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = iconColor,
                    letterSpacing = 0.5.sp
                )
            }
            TextButton(
                onClick = onReturnHome,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AUTO-PILOT", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
            }
        }
    }
}

@Composable
fun RecentProjectRow(
    project: ProjectSummary,
    onClick: () -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text("Delete Video?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${project.title.ifBlank { project.topic }}\"?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    GlitchCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceElevated)
            ) {
                if (project.thumbnail.isNotBlank()) {
                    AsyncImage(
                        model = project.thumbnail,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.title.ifBlank { project.topic },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${project.category} • ${project.duration}s • ${project.language}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = project.status)
            }

            IconButton(
                onClick = { onClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Play",
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("project_menu_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Watch / Preview", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberCyan)
                        },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Publish to YouTube", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = ElectricViolet)
                        },
                        onClick = {
                            showMenu = false
                            onPublish()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Publish to Instagram", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NeonPink)
                        },
                        onClick = {
                            showMenu = false
                            SocialShareHelper.publishToInstagram(
                                context = context,
                                videoPath = project.video,
                                title = project.title.ifBlank { project.topic },
                                caption = "Check out this AI generated video!"
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Publish to Facebook", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Public, contentDescription = null, tint = CyberCyan)
                        },
                        onClick = {
                            showMenu = false
                            SocialShareHelper.publishToFacebook(
                                context = context,
                                videoPath = project.video,
                                title = project.title.ifBlank { project.topic },
                                caption = "Check out this AI generated video!"
                            )
                        }
                    )
                    Divider(color = DarkBorder, thickness = 0.5.dp)
                    DropdownMenuItem(
                        text = { Text("Delete Video", color = ErrorRed) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                        },
                        onClick = {
                            showMenu = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }
}
