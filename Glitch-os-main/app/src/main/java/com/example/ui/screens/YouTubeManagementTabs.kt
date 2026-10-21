package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.UUID
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.data.youtube.VideoItemData
import com.example.ui.components.BackgroundPermissionsCard
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.util.AutoPlaylistManager
import com.example.util.CommunityPostItem
import com.example.util.CommunityPostManager
import com.example.util.FirestoreService
import com.example.util.GoogleSignInHelper
import com.example.util.GoogleUser
import com.example.util.YouTubeChannelInfo
import com.example.util.YouTubeChannelSeoManager
import com.example.util.YouTubePlaylistItem
import com.example.util.SelfLearningEngine
import com.example.util.SelfLearningReport
import com.example.util.PeakTimingOption
import com.example.util.YouTubeAutonomousManager
import com.example.util.RealYouTubeComment
import com.example.util.SmartReplyOption
import com.example.util.AutonomousContentQueueManager
import com.example.util.ViralTrendIdea
import com.example.util.AutonomousPerformanceWatchdog
import com.example.util.ChannelWatchdogSummary
import com.example.util.VideoAuditReport
import com.example.util.ChannelProfileManager
import com.example.data.local.ChannelProfileEntity
import com.example.util.SmartPlaylistSuggestion
import com.example.data.local.AutonomousQueueEntity
import com.example.data.local.AppDatabase
import com.example.worker.AutonomousChannelWorker
import com.example.data.local.QuotaLedgerEntity
import com.example.pipeline.NotrackWebViewClient
import com.example.worker.AutomationSchedulerManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun YouTubeManagementScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    initialTab: Int = 0,
    hideTopBar: Boolean = false,
    onNavigateToPreview: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settingsFlow.collectAsState()
    val libraryProjects by viewModel.libraryProjects.collectAsState()

    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: Auto-Bot, 1: Channel SEO, 2: Playlists, 3: Community, 4: Analytics

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    var channelInfo by remember { mutableStateOf<YouTubeChannelInfo?>(null) }
    var channelVideos by remember { mutableStateOf<List<VideoItemData>>(emptyList()) }
    var userPlaylists by remember { mutableStateOf<List<YouTubePlaylistItem>>(emptyList()) }

    var isLoadingChannel by remember { mutableStateOf(false) }
    var isLoadingVideos by remember { mutableStateOf(false) }
    var isLoadingPlaylists by remember { mutableStateOf(false) }
    var isCreatingPlaylists by remember { mutableStateOf(false) }
    var isApplyingSeo by remember { mutableStateOf(false) }

    var channelError by remember { mutableStateOf<String?>(null) }
    var videosError by remember { mutableStateOf<String?>(null) }
    var playlistMessage by remember { mutableStateOf<String?>(null) }
    var seoStatusMessage by remember { mutableStateOf<String?>(null) }

    var liveCategories by remember { mutableStateOf(GoogleSignInHelper.getDefaultYouTubeCategories()) }
    var isFetchingYtCategories by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isFetchingYtCategories = true
            val res = GoogleSignInHelper.fetchYouTubeVideoCategories(context)
            res.onSuccess { fetched ->
                if (fetched.isNotEmpty()) {
                    liveCategories = fetched
                }
            }
            isFetchingYtCategories = false
        }
    }

    val categories = com.example.util.UnifiedCategories.ALL
    val intervalOptions = listOf(1, 2, 4, 6, 12, 24)

    var selectedInterval by remember { mutableStateOf(settings.scheduleIntervalHours) }
    var selectedCategory by remember { mutableStateOf(settings.autoCategory) }
    var publishingProjectId by remember { mutableStateOf<String?>(null) }

    // ── Step 5: Multi-Channel Fleet & Profile Manager State ──
    val channelProfiles by ChannelProfileManager.getAllProfilesFlow(context).collectAsState(initial = emptyList())
    val channelAccounts by ChannelProfileManager.getAllChannelsFlow(context).collectAsState(initial = emptyList())
    var showAddChannelDialog by remember { mutableStateOf(false) }
    var editingCategoryChannel by remember { mutableStateOf<com.example.data.local.YouTubeChannelAccountEntity?>(null) }
    var newChannelTitleInput by remember { mutableStateOf("") }
    var newChannelHandleInput by remember { mutableStateOf("") }
    var newChannelIdInput by remember { mutableStateOf("") }
    var newChannelCategoryInput by remember { mutableStateOf("Technology") }
    var newChannelLanguageInput by remember { mutableStateOf("Hindi") }
    var newChannelAutoPilotInput by remember { mutableStateOf(false) }

    LaunchedEffect(channelInfo) {
        if (channelInfo != null) {
            ChannelProfileManager.saveOrSyncCurrentChannelProfile(
                context = context,
                channelInfo = channelInfo,
                defaultCategory = selectedCategory,
                autoPilotEnabled = settings.autoPublishEnabled
            )
        }
    }

    // Phase 1 Channel SEO & Community dynamic generators
    var activeSeoData by remember {
        mutableStateOf(
            YouTubeChannelSeoManager.generateChannelSeo(
                category = settings.autoCategory,
                currentChannelName = channelInfo?.channelName,
                currentHandle = channelInfo?.channelHandle
            )
        )
    }

    var communityPosts by remember { mutableStateOf(emptyList<CommunityPostItem>()) }

    // Phase 2 Video Level Deep SEO state
    var seoSubTab by remember { mutableStateOf(0) } // 0 = Channel SEO, 1 = Video SEO Studio
    var videoSeoTopicInput by remember { mutableStateOf("") }
    var activeVideoSeoData by remember { mutableStateOf<com.example.util.DeepVideoSeoResult?>(null) }

    // Phase 3 Self-Learning Loop & Smart Timing state
    var isAutoPivotEnabled by remember { mutableStateOf(settings.smartAutoPivotEnabled) }
    var selectedPeakHour by remember { mutableStateOf(settings.smartPeakHour) }
    var smartTimingMode by remember { mutableStateOf(settings.smartTimingMode) }
    var analyticsSubTab by remember { mutableStateOf(0) } // 0 = Live & Auto-Pivot, 1 = Best Time to Post, 2 = Video Rankings
    var isOptimizingNotrackBranding by remember { mutableStateOf(false) }

    // Strict Auto-Pilot Permission Alert States
    var showAutopilotPermissionAlert by remember { mutableStateOf(false) }
    var autopilotAlertTitle by remember { mutableStateOf("⚠️ YouTube Auto-Pilot Locked") }
    var autopilotAlertMessage by remember { mutableStateOf("") }

    // Autonomous YouTube Channel Guardian & NoTrack Smart Replies State
    var liveComments by remember { mutableStateOf<List<RealYouTubeComment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(false) }
    var commentsLoadError by remember { mutableStateOf<String?>(null) }
    var isPostingCommentReply by remember { mutableStateOf<String?>(null) }
    var todayQuota by remember { mutableStateOf<QuotaLedgerEntity?>(null) }
    var isRunningAutonomousScan by remember { mutableStateOf(false) }
    var autonomousScanResult by remember { mutableStateOf<String?>(null) }
    var autonomousBotPeriodicEnabled by remember { mutableStateOf(false) }
    var notrackClientInstance by remember { mutableStateOf<NotrackWebViewClient?>(null) }
    var isGeneratingNoTrackReply by remember { mutableStateOf<String?>(null) }
    var commentGeneratedReplies by remember { mutableStateOf<Map<String, List<SmartReplyOption>>>(emptyMap()) }

    fun loadCommentsAndQuota() {
        coroutineScope.launch {
            isLoadingComments = true
            commentsLoadError = null
            liveComments = emptyList()
            try {
                todayQuota = YouTubeAutonomousManager.getOrCreateTodayQuota(context)
                val res = YouTubeAutonomousManager.fetchRealChannelComments(
                    context = context,
                    channelId = channelInfo?.channelId,
                    videoList = channelVideos,
                    maxResults = 15
                )
                res.onSuccess { fetched ->
                    liveComments = fetched
                }.onFailure { error ->
                    commentsLoadError = error.localizedMessage ?: "Could not load comments from YouTube."
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                commentsLoadError = e.localizedMessage ?: "Could not load comments from YouTube."
            } finally {
                isLoadingComments = false
            }
        }
    }

    fun generateNoTrackReplyForComment(comment: RealYouTubeComment) {
        coroutineScope.launch {
            isGeneratingNoTrackReply = comment.threadId
            try {
                if (notrackClientInstance == null) {
                    notrackClientInstance = NotrackWebViewClient.create(context)
                }
                val client = notrackClientInstance ?: NotrackWebViewClient.create(context)
                val replies = client.askNotrackForSmartReplies(
                    commentText = comment.text,
                    videoTitle = comment.videoTitle,
                    category = selectedCategory
                )
                commentGeneratedReplies = commentGeneratedReplies + (comment.threadId to replies)
                Toast.makeText(context, "AI reply suggestions are ready.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                commentGeneratedReplies = commentGeneratedReplies - comment.threadId
                Toast.makeText(context, "AI reply generation failed: ${e.localizedMessage ?: "Provider unavailable"}", Toast.LENGTH_LONG).show()
            } finally {
                isGeneratingNoTrackReply = null
            }
        }
    }

    fun postReplyToYouTube(threadId: String, replyText: String, videoId: String, authorName: String, originalText: String) {
        coroutineScope.launch {
            isPostingCommentReply = threadId
            val res = YouTubeAutonomousManager.postCommentReply(
                context = context,
                threadId = threadId,
                replyText = replyText,
                videoId = videoId,
                authorName = authorName,
                originalText = originalText
            )
            res.onSuccess {
                Toast.makeText(context, "🚀 Reply posted successfully to YouTube!", Toast.LENGTH_LONG).show()
                liveComments = liveComments.map {
                    if (it.threadId == threadId) it.copy(isReplied = true) else it
                }
                todayQuota = YouTubeAutonomousManager.getOrCreateTodayQuota(context)
            }.onFailure { err ->
                Toast.makeText(context, "❌ Post failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
            isPostingCommentReply = null
        }
    }

    fun triggerAutonomousScan() {
        coroutineScope.launch {
            isRunningAutonomousScan = true
            autonomousScanResult = null
            try {
                if (notrackClientInstance == null) {
                    notrackClientInstance = NotrackWebViewClient.create(context)
                }
                val summary = YouTubeAutonomousManager.runAutonomousCommentScanAndReply(
                    context = context,
                    notrackClient = notrackClientInstance
                )
                autonomousScanResult = summary.details
                todayQuota = YouTubeAutonomousManager.getOrCreateTodayQuota(context)
                loadCommentsAndQuota()
                Toast.makeText(context, "✅ ${summary.details}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                autonomousScanResult = "Error: ${e.message}"
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isRunningAutonomousScan = false
            }
        }
    }

    // ── Step 2: Autonomous Viral Trend Discovery & 7-Day Content Queue State ──
    var viralTrendsList by remember { mutableStateOf<List<ViralTrendIdea>>(emptyList()) }
    var isScanningTrends by remember { mutableStateOf(false) }
    var isGenerating7DayQueue by remember { mutableStateOf(false) }
    val queuedContentList by AppDatabase.getInstance(context).autonomousChannelDao().getAllQueuedContentFlow().collectAsState(initial = emptyList())

    fun scanViralTrends() {
        coroutineScope.launch {
            isScanningTrends = true
            try {
                val trends = AutonomousContentQueueManager.discoverViralTrends(
                    context = context,
                    category = selectedCategory
                )
                viralTrendsList = trends
                Toast.makeText(context, "Loaded ${trends.size} live YouTube popular-chart videos.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Trend scan error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isScanningTrends = false
            }
        }
    }

    fun generate7DayContentQueue() {
        coroutineScope.launch {
            isGenerating7DayQueue = true
            try {
                val res = AutonomousContentQueueManager.generate7DayAutonomousQueue(
                    context = context,
                    category = selectedCategory,
                    delaySeconds = selectedInterval * 3600L
                )
                res.onSuccess { items ->
                    Toast.makeText(context, "⚡ Queued ${items.size} distinct live-chart topics.", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Queue creation failed: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isGenerating7DayQueue = false
            }
        }
    }

    fun deleteQueueItem(queueId: String) {
        coroutineScope.launch {
            AutonomousContentQueueManager.deleteQueueItem(context, queueId)
            Toast.makeText(context, "Removed from autonomous queue.", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearPublishedQueueItems() {
        coroutineScope.launch {
            AutonomousContentQueueManager.clearPublishedItems(context)
            Toast.makeText(context, "Cleared published items.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Step 3: Autonomous Community Tab Studio State ──
    var isGeneratingCommunityPostsFromNotrack by remember { mutableStateOf(false) }

    fun generateCommunityPostsWithNotrack() {
        coroutineScope.launch {
            isGeneratingCommunityPostsFromNotrack = true
            communityPosts = emptyList()
            try {
                if (notrackClientInstance == null) {
                    notrackClientInstance = NotrackWebViewClient.create(context)
                }
                val posts = CommunityPostManager.generateAutonomousCommunityPosts(
                    category = selectedCategory,
                    channelName = channelInfo?.channelName ?: "YouTube Channel",
                    notrackClient = notrackClientInstance
                )
                communityPosts = posts
                Toast.makeText(context, "Generated ${posts.size} editable community post drafts.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not generate community post drafts: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isGeneratingCommunityPostsFromNotrack = false
            }
        }
    }

    // ── Step 4: Autonomous Retention & Performance Watchdog State ──
    var watchdogSummary by remember { mutableStateOf<ChannelWatchdogSummary?>(null) }
    var isRunningWatchdogDiagnosis by remember { mutableStateOf(false) }

    fun runWatchdogDiagnosis() {
        coroutineScope.launch {
            isRunningWatchdogDiagnosis = true
            try {
                if (notrackClientInstance == null) {
                    notrackClientInstance = NotrackWebViewClient.create(context)
                }
                val report = AutonomousPerformanceWatchdog.auditChannelPerformance(
                    context = context,
                    videos = channelVideos,
                    category = selectedCategory,
                    notrackClient = notrackClientInstance
                )
                watchdogSummary = report
                Toast.makeText(context, "🎯 View-count review complete: ${report.totalVideosAudited} videos reviewed.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Audit error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isRunningWatchdogDiagnosis = false
            }
        }
    }

    // ── Step 6: Autonomous Smart Playlists & Binge Loop Studio State ──
    var smartPlaylistSuggestions by remember { mutableStateOf<List<SmartPlaylistSuggestion>>(emptyList()) }
    var isGeneratingSmartPlaylists by remember { mutableStateOf(false) }

    fun generateSmartPlaylists() {
        coroutineScope.launch {
            isGeneratingSmartPlaylists = true
            try {
                if (notrackClientInstance == null) {
                    notrackClientInstance = NotrackWebViewClient.create(context)
                }
                val recentTitles = channelVideos.map { it.title }
                val results = AutoPlaylistManager.generateAutonomousPlaylistsWithNotrack(
                    context = context,
                    category = selectedCategory,
                    channelName = channelInfo?.channelName ?: "YouTube Channel",
                    recentTitles = recentTitles,
                    notrackClient = notrackClientInstance
                )
                smartPlaylistSuggestions = results
                Toast.makeText(context, "⚡ Generated ${results.size} high-retention series playlists via Glitch AI! 🎬", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Playlist generation error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingSmartPlaylists = false
            }
        }
    }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted! 🟢", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $label to clipboard! 📋", Toast.LENGTH_SHORT).show()
    }

    fun loadPlaylists() {
        coroutineScope.launch {
            isLoadingPlaylists = true
            playlistMessage = null
            val res = AutoPlaylistManager.fetchUserPlaylists(context)
            res.onSuccess { list ->
                userPlaylists = list
            }.onFailure { err ->
                playlistMessage = "Note: ${err.message}"
            }
            isLoadingPlaylists = false
        }
    }

    fun refreshChannelData() {
        coroutineScope.launch {
            isLoadingChannel = true
            channelError = null
            val infoRes = GoogleSignInHelper.fetchYouTubeChannelInfo(context)
            infoRes.onSuccess { info ->
                channelInfo = info
                activeSeoData = YouTubeChannelSeoManager.generateChannelSeo(
                    category = selectedCategory,
                    currentChannelName = info.channelName,
                    currentHandle = info.channelHandle
                )
                communityPosts = emptyList()

                val gUser = GoogleSignInHelper.currentUser.value ?: GoogleUser(
                    displayName = currentUser?.displayName,
                    email = currentUser?.email,
                    photoUrl = currentUser?.photoUrl?.toString(),
                    idToken = null,
                    hasYouTubePermission = GoogleSignInHelper.hasValidYouTubePermissions(context)
                )
                FirestoreService.saveYouTubeChannelProfile(
                    context,
                    gUser,
                    mapOf(
                        "title" to info.channelName,
                        "handle" to info.channelHandle,
                        "subscriber_count" to info.subscriberCount,
                        "view_count" to info.viewCount,
                        "video_count" to info.videoCount
                    )
                )
                if (!info.uploadsPlaylistId.isNullOrBlank()) {
                    isLoadingVideos = true
                    videosError = null
                    val vidsRes = GoogleSignInHelper.fetchYouTubeChannelVideos(context, info.uploadsPlaylistId)
                    vidsRes.onSuccess { vids ->
                        channelVideos = vids
                    }.onFailure { err ->
                        videosError = err.message
                    }
                    isLoadingVideos = false
                }
                loadPlaylists()
                loadCommentsAndQuota()
            }.onFailure { err ->
                channelError = err.message
            }
            isLoadingChannel = false
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) {
            loadCommentsAndQuota()
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null || GoogleSignInHelper.hasValidYouTubePermissions(context)) {
            refreshChannelData()
        }
    }

    LaunchedEffect(settings.scheduleIntervalHours) {
        selectedInterval = settings.scheduleIntervalHours
    }

    LaunchedEffect(settings.autoCategory) {
        selectedCategory = settings.autoCategory
        activeSeoData = YouTubeChannelSeoManager.generateChannelSeo(
            category = settings.autoCategory,
            currentChannelName = channelInfo?.channelName,
            currentHandle = channelInfo?.channelHandle
        )
        communityPosts = emptyList()
    }

    Scaffold(
        topBar = {
            if (!hideTopBar) {
                val (dynamicTitle, dynamicSubtitle, badgeText) = when (selectedTab) {
                    0 -> Triple("AUTOPILOT STUDIO", "Autonomous Video Creator & Queue", "AUTONOMOUS")
                    1 -> Triple("SEO VIRAL LAB", "Channel & Video Optimization Lab", "VIRAL AI")
                    2 -> Triple("SMART PLAYLISTS", "Binge-Watch Series & Content Hub", "SERIES")
                    3 -> Triple("COMMUNITY ENGAGEMENT", "Interactive Polls & Subscriber Retention", "COMMUNITY")
                    4 -> Triple("CHANNEL ANALYTICS", "Performance Intelligence & Metrics", "ANALYTICS")
                    5 -> Triple("AI CHANNEL CHATBOT", "NoTrack Live Scraper Intelligence", "NOTRACK LIVE")
                    else -> Triple("STUDIO MANAGEMENT", "YouTube Channel Manager", "STUDIO")
                }

                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dynamicTitle,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = ElectricViolet.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricViolet,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = dynamicSubtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground
                    )
                )
            }
        },
        bottomBar = {},
        containerColor = DarkBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isCompact = maxWidth < 380.dp
            val tabPadding = if (isCompact) 10.dp else 14.dp
            val contentSpacing = if (isCompact) 10.dp else 14.dp

            Column(modifier = Modifier.fillMaxSize()) {
                val studioTabs = listOf(
                    Triple(0, "🚀 Auto-Pilot", NeonGreen),
                    Triple(1, "🔍 SEO Lab", ElectricViolet),
                    Triple(2, "📑 Playlists", NeonPink),
                    Triple(3, "💬 Community", Color(0xFFF59E0B)),
                    Triple(4, "📊 Analytics", CyberCyan),
                    Triple(5, "🤖 AI Chatbot", Color(0xFF00F0FF))
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated.copy(alpha = 0.5f))
                        .padding(horizontal = tabPadding, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(studioTabs) { (tabIdx, tabName, tabCol) ->
                        val isSel = selectedTab == tabIdx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) tabCol.copy(alpha = 0.18f) else DarkBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) tabCol else DarkBorder
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = tabIdx }
                        ) {
                            Text(
                                text = tabName,
                                fontSize = if (isCompact) 11.sp else 12.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                                color = if (isSel) tabCol else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                0 -> {
                    // TAB 0: DASHBOARD (Auto-Pilot Scheduler & Direct Publish)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tabPadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(contentSpacing)
                    ) {
                        // ⚡ STEP 5: MULTI-CHANNEL FLEET & PROFILE SWITCHER
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(text = "🌐", fontSize = 16.sp)
                                            Column {
                                                Text(
                                                    text = "MULTI-CHANNEL FLEET",
                                                    fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Manage multiple channels autonomously",
                                                    fontSize = 9.5.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Surface(
                                            color = NeonGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "STEP 5 READY",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Add Channel Action
                                    NeonButton(
                                        text = "➕ CONNECT YOUTUBE CHANNEL",
                                        onClick = {
                                            newChannelTitleInput = ""
                                            newChannelHandleInput = ""
                                            newChannelIdInput = "UC" + UUID.randomUUID().toString().replace("-", "").take(22)
                                            newChannelCategoryInput = selectedCategory
                                            newChannelLanguageInput = "Hindi"
                                            newChannelAutoPilotInput = false
                                            showAddChannelDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val allChannelsList = if (channelAccounts.isNotEmpty()) {
                                        channelAccounts
                                    } else {
                                        channelProfiles.map {
                                            com.example.data.local.YouTubeChannelAccountEntity(
                                                channelId = it.channelId,
                                                channelTitle = it.channelTitle,
                                                channelHandle = it.channelHandle,
                                                avatarUrl = it.avatarUrl,
                                                subscriberCount = it.subscriberCount,
                                                videoCount = it.videoCount,
                                                defaultCategory = it.defaultCategory,
                                                autoPilotEnabled = it.autoPilotEnabled,
                                                isActiveProfile = it.isActiveProfile
                                            )
                                        }
                                    }

                                    if (allChannelsList.isEmpty()) {
                                        Text(
                                            text = "Current channel auto-registered: ${channelInfo?.channelName ?: "YouTube Channel"}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            allChannelsList.forEach { profile ->
                                                val isCurrent = profile.channelId == (channelInfo?.channelId ?: profile.channelId) || profile.isActiveProfile
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp)),
                                                    color = if (isCurrent) CyberCyan.copy(alpha = 0.12f) else DarkSurfaceElevated,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isCurrent) CyberCyan else DarkBorder
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.weight(1f)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .clip(CircleShape)
                                                                        .background(if (isCurrent) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                                                        .border(1.dp, if (isCurrent) CyberCyan else DarkBorder, CircleShape),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = profile.channelTitle.take(1).uppercase(),
                                                                        fontWeight = FontWeight.Black,
                                                                        color = if (isCurrent) CyberCyan else TextPrimary,
                                                                        fontSize = 16.sp
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                Column {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Text(
                                                                            text = profile.channelTitle,
                                                                            fontSize = 13.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isCurrent) CyberCyan else TextPrimary
                                                                        )
                                                                        if (isCurrent) {
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .clip(RoundedCornerShape(4.dp))
                                                                                    .background(NeonGreen.copy(alpha = 0.2f))
                                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                            ) {
                                                                                Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                                                            }
                                                                        }
                                                                    }
                                                                    Text(
                                                                        text = "${profile.channelHandle} • ${profile.subscriberCount} subs • ${profile.videoCount} videos",
                                                                        fontSize = 10.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    coroutineScope.launch {
                                                                        ChannelProfileManager.deleteProfile(context, profile.channelId)
                                                                        Toast.makeText(context, "Channel removed.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Remove",
                                                                    tint = TextMuted,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }

                                                        // Channel specific Category and Controls
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Category Badge with edit button
                                                            Surface(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .clickable {
                                                                        editingCategoryChannel = profile
                                                                    },
                                                                color = DarkSurfaceVariant,
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text("🎯 Niche: ", fontSize = 10.sp, color = TextSecondary)
                                                                    Text(profile.defaultCategory, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricViolet)
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("✏️", fontSize = 9.sp)
                                                                }
                                                            }

                                                            // Switch to this channel button if not active
                                                            if (!isCurrent) {
                                                                Button(
                                                                    onClick = {
                                                                        coroutineScope.launch {
                                                                            ChannelProfileManager.switchActiveProfile(context, profile.channelId)
                                                                            selectedCategory = profile.defaultCategory
                                                                            Toast.makeText(context, "Switched active channel to ${profile.channelTitle} (${profile.defaultCategory}) 🎯", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    },
                                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    modifier = Modifier.height(28.dp)
                                                                ) {
                                                                    Text("SET ACTIVE", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            } else {
                                                                Text("Target for Autopilot", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                            }
                                                        }
                                                    }
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "AUTO-PILOT SCHEDULER & PIPELINE",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                color = CyberCyan
                                            )
                                        }
                                        Text(
                                            text = if (settings.autoPublishEnabled) "Active • Creating & Uploading" else "Paused • Manual Only",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (settings.autoPublishEnabled) NeonGreen else TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = settings.autoPublishEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                val hasGoogleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context) != null
                                                val hasYtScope = GoogleSignInHelper.hasValidYouTubePermissions(context)
                                                val hasNotif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                } else true

                                                if (!hasGoogleAccount || !hasYtScope || !hasNotif) {
                                                    val missing = mutableListOf<String>()
                                                    if (!hasGoogleAccount) {
                                                        missing.add("1. Google Account Sign-In (Login required)")
                                                    } else if (!hasYtScope) {
                                                        missing.add("1. YouTube Channel Upload Permission (OAuth scope required)")
                                                    }
                                                    if (!hasNotif) {
                                                        missing.add("2. Background Notification Permission (For background status updates)")
                                                    }

                                                    autopilotAlertTitle = "⚠️ YouTube Auto-Pilot Locked"
                                                    autopilotAlertMessage = "Aap jab tak sabhi permissions allow nahi karenge tab tak Auto-Pilot ON nahi ho sakta:\n\n" + missing.joinToString("\n\n") + "\n\nKripya pehle sabhi required permissions allow karein!"
                                                    showAutopilotPermissionAlert = true
                                                    viewModel.updateScheduler(false, selectedInterval, selectedCategory)
                                                    return@Switch
                                                }
                                            }
                                            viewModel.updateScheduler(enabled, selectedInterval, selectedCategory)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = NeonGreen
                                        )
                                    )
                                }
                            }
                        }

                        // Category Pill Selector (YouTube Live Category Sync)
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "TARGET CONTENT CATEGORY (YOUTUBE API LIVE)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "Fetched directly from YouTube. New videos will be generated & uploaded under this default category.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isFetchingYtCategories = true
                                                val res = GoogleSignInHelper.fetchYouTubeVideoCategories(context)
                                                res.onSuccess { fetched ->
                                                    if (fetched.isNotEmpty()) {
                                                        liveCategories = fetched
                                                        Toast.makeText(context, "Synced ${fetched.size} live YouTube categories!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                isFetchingYtCategories = false
                                            }
                                        },
                                        enabled = !isFetchingYtCategories
                                    ) {
                                        if (isFetchingYtCategories) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberCyan, strokeWidth = 2.dp)
                                        } else {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Categories", tint = CyberCyan)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(liveCategories) { catItem ->
                                        val isSelected = catItem.title.equals(selectedCategory, ignoreCase = true) || settings.defaultCategoryId == catItem.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedCategory = catItem.title
                                                viewModel.updateUnifiedCategory(catItem.title, catItem.id)
                                                activeSeoData = YouTubeChannelSeoManager.generateChannelSeo(
                                                    category = catItem.title,
                                                    currentChannelName = channelInfo?.channelName,
                                                    currentHandle = channelInfo?.channelHandle
                                                )
                                                communityPosts = emptyList()
                                                Toast.makeText(
                                                    context,
                                                    "Unified Category synced across App: ${catItem.title} (ID: ${catItem.id}) 🎯",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            label = { Text("${catItem.title} (ID: ${catItem.id})", fontSize = 12.sp) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan.copy(alpha = 0.25f),
                                                selectedLabelColor = CyberCyan,
                                                containerColor = DarkSurfaceElevated,
                                                labelColor = TextSecondary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) CyberCyan else DarkBorder,
                                                selectedBorderColor = CyberCyan
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Schedule Interval Selector
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "PUBLISHING CADENCE / INTERVAL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    intervalOptions.forEach { hours ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedInterval = hours
                                                    val delaySec = hours * 3600L
                                                    if (settings.autoPublishEnabled) {
                                                        viewModel.updateScheduler(
                                                            enabled = true,
                                                            intervalHours = hours,
                                                            category = selectedCategory,
                                                            delaySeconds = delaySec
                                                        )
                                                    } else {
                                                        viewModel.updateSettings(
                                                            settings.copy(
                                                                scheduleIntervalHours = hours,
                                                                scheduleDelaySeconds = delaySec,
                                                                smartTimingMode = "FIXED_INTERVAL"
                                                            )
                                                        )
                                                    }
                                                },
                                            color = if (selectedInterval == hours) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selectedInterval == hours) CyberCyan else DarkBorder
                                            )
                                        ) {
                                            Text(
                                                text = "${hours}h",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedInterval == hours) CyberCyan else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 🔥 STEP 2: AUTONOMOUS VIRAL TREND SCANNER & 7-DAY QUEUE (NOTRACK AI)
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "🔥", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "VIRAL TREND ENGINE & 7-DAY QUEUE",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan
                                                )
                                                Text(
                                                    text = "Autonomous Content Planning via Glitch AI",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Surface(
                                            color = NeonGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "STEP 2 READY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Scans top breakout hooks and retention formulas for '$selectedCategory', then automatically queues a 7-day production pipeline without manual intervention.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    // Action Buttons Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { scanViralTrends() },
                                            enabled = !isScanningTrends,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = DarkSurfaceElevated,
                                                contentColor = CyberCyan
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            if (isScanningTrends) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = CyberCyan)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Scanning Trends...", fontSize = 10.sp)
                                            } else {
                                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("🤖 SCAN TRENDS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Button(
                                            onClick = { generate7DayContentQueue() },
                                            enabled = !isGenerating7DayQueue,
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .height(38.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CyberCyan,
                                                contentColor = Color.Black
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            if (isGenerating7DayQueue) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Synthesizing 7-Day Plan...", fontSize = 10.sp, color = Color.Black)
                                            } else {
                                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("⚡ ARM 7-DAY QUEUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                            }
                                        }
                                    }

                                    // Display Live Detected Breakout Trends
                                    if (viralTrendsList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "🎯 TOP BREAKOUT TREND CONCEPTS (${viralTrendsList.size}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )

                                        viralTrendsList.forEach { trend ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                color = DarkBackground,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = trend.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            color = ElectricViolet.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "CHART #${trend.chartRank}",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = ElectricViolet,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Views: ${trend.views}  •  Likes: ${trend.likes}  •  Comments: ${trend.comments}",
                                                        fontSize = 10.sp,
                                                        color = TextSecondary
                                                    )
                                                    if (trend.publishedAt.isNotBlank()) {
                                                        Text(
                                                            text = "Published: ${trend.publishedAt}",
                                                            fontSize = 10.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                        }
                                    }
                                }

                                    // 7-Day Autonomous Video Schedule Queue
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📅 AUTONOMOUS CONTENT QUEUE (${queuedContentList.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                        if (queuedContentList.any { it.status == "PUBLISHED" }) {
                                            TextButton(
                                                onClick = { clearPublishedQueueItems() },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Clear Published", fontSize = 10.sp, color = TextMuted)
                                            }
                                        }
                                    }

                                    if (queuedContentList.isNotEmpty()) {
                                        queuedContentList.forEachIndexed { index, queueItem ->
                                            val isFirst = index == 0
                                            val statusColor = when (queueItem.status) {
                                                "READY", "GENERATED" -> NeonGreen
                                                "PUBLISHED" -> CyberCyan
                                                else -> Color(0xFFFFB300)
                                            }

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isFirst) DarkSurfaceElevated else DarkBackground,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isFirst) NeonGreen.copy(alpha = 0.5f) else DarkBorder
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Day ${index + 1} • ${AutonomousContentQueueManager.formatScheduleDate(queueItem.scheduledPublishEpochMs)}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CyberCyan
                                                        )
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                color = statusColor.copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = queueItem.status,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = statusColor,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { deleteQueueItem(queueItem.queueId) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Remove",
                                                                    tint = TextMuted,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = queueItem.topicTitle,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )

                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = queueItem.hookFormula,
                                                        fontSize = 10.sp,
                                                        color = TextSecondary,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Tags: ${queueItem.targetKeywords}",
                                                        fontSize = 9.sp,
                                                        color = TextMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            color = DarkBackground,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "🕒 Autonomous Queue is currently empty",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextSecondary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Tap '⚡ ARM 7-DAY QUEUE' above to generate and schedule your next week of autonomous viral content.",
                                                    fontSize = 10.sp,
                                                    color = TextMuted,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            BackgroundPermissionsCard()
                        }

                        // Direct Publish from Library
                        item {
                            Text(
                                text = "LIBRARY VIDEOS (READY TO PUBLISH)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (libraryProjects.isEmpty()) {
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "No saved videos in library. Create a video or let the Auto-Pilot Bot generate one!",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            items(libraryProjects) { project ->
                                val isPublishing = publishingProjectId == project.id
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = project.title.ifBlank { "Untitled Video" },
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${project.category} • ${project.duration}s • ${project.aspectRatio}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    publishingProjectId = project.id
                                                    val res = viewModel.publishProjectToYouTubeDirect(project.id)
                                                    publishingProjectId = null
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Published to YouTube & Organized into Playlist! 🚀", Toast.LENGTH_LONG).show()
                                                        refreshChannelData()
                                                    } else {
                                                        Toast.makeText(context, "Publish failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            enabled = !isPublishing,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ElectricViolet,
                                                disabledContainerColor = ElectricViolet.copy(alpha = 0.5f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (isPublishing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Publishing...", fontSize = 12.sp)
                                            } else {
                                                Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Publish", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: CHANNEL SEO (PHASE 1) & VIDEO LEVEL DEEP SEO (PHASE 2)
                    val hasPermissions = GoogleSignInHelper.hasValidYouTubePermissions(context)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tabPadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(contentSpacing)
                    ) {
                        // Modernized Segmented Control
                        item {
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isChannel = seoSubTab == 0
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isChannel) CyberCyan else Color.Transparent)
                                            .clickable { seoSubTab = 0 }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = if (isChannel) Color.Black else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Channel Branding",
                                                fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isChannel) Color.Black else TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    val isVideo = seoSubTab == 1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isVideo) ElectricViolet else Color.Transparent)
                                            .clickable { seoSubTab = 1 }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = if (isVideo) Color.White else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Video SEO Lab",
                                                fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isVideo) Color.White else TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (seoSubTab == 0) {
                        // Channel Profile Header
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "GLITCH CREATOR PROFILE",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            color = CyberCyan
                                        )
                                        if (currentUser != null) {
                                            Surface(
                                                color = NeonGreen.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "● LIVE SYNC",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonGreen,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (currentUser != null) {
                                        IconButton(
                                            onClick = { refreshChannelData() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                if (currentUser != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        val displayAvatar = channelInfo?.avatarUrl ?: currentUser?.photoUrl?.toString()
                                        if (!displayAvatar.isNullOrBlank()) {
                                            AsyncImage(
                                                model = displayAvatar,
                                                contentDescription = "Avatar",
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .border(2.dp, CyberCyan, CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.3f), ElectricViolet.copy(alpha = 0.3f))))
                                                    .border(2.dp, CyberCyan, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SmartToy,
                                                    contentDescription = null,
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            val displayName = channelInfo?.channelName?.ifBlank { null }
                                                ?: currentUser?.displayName?.ifBlank { null }
                                                ?: channelInfo?.channelHandle?.removePrefix("@")?.replace(".", " ")?.replace("_", " ")?.capitalize()
                                                ?: "Nexus Tech Studio"
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified",
                                                    tint = CyberCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = channelInfo?.channelHandle ?: activeSeoData.handle,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CyberCyan
                                            )
                                            if (!currentUser?.email.isNullOrBlank()) {
                                                Text(
                                                    text = currentUser?.email ?: "",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Connect your Google/YouTube account to sync live metadata and perform 1-tap SEO updates.",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    NeonButton(
                                        text = "CONNECT YOUTUBE ACCOUNT",
                                        leadingIcon = Icons.Default.AccountCircle,
                                        onClick = {
                                            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                                putExtra("FORCE_LOGIN", true)
                                            })
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // AI Channel Description & Bio
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🚀 AI-OPTIMIZED CHANNEL BIO",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Niche: ${activeSeoData.category}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "• ${activeSeoData.optimizedDescription.length} chars",
                                                fontSize = 10.sp,
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                    Surface(
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            copyToClipboard(activeSeoData.optimizedDescription, "Channel Bio")
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                            Text("Copy Bio", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = activeSeoData.optimizedDescription,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        // High Search Volume Keywords / Channel Tags
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🎯 TOP HIGH-SEARCH CHANNEL KEYWORDS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "${activeSeoData.searchKeywords.size} High-Traffic Ranking Tags",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Surface(
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable {
                                            copyToClipboard(activeSeoData.searchKeywords.joinToString(", "), "Channel Keywords")
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy All", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                            Text("Copy All", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    activeSeoData.searchKeywords.forEach { tag ->
                                        Surface(
                                            color = DarkSurfaceElevated,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                                            modifier = Modifier.clickable {
                                                copyToClipboard(tag, "Tag: $tag")
                                            }
                                        ) {
                                            Text(
                                                text = tag,
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // AI Banner & Avatar Visual Prompts
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "🎨 AI CHANNEL BRANDING PROMPTS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricViolet
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Channel Banner Prompt (8K Wide):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        copyToClipboard(activeSeoData.bannerPrompt, "Banner Prompt")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = activeSeoData.bannerPrompt, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Logo / Avatar Prompt:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        copyToClipboard(activeSeoData.avatarPrompt, "Avatar Prompt")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = activeSeoData.avatarPrompt, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // AI Refresh & Optimize Branding Button
                        item {
                            NeonButton(
                                text = if (isOptimizingNotrackBranding) "🤖 AI OPTIMIZING BRANDING..." else "✨ REFRESH & AI OPTIMIZE BRANDING",
                                leadingIcon = Icons.Default.AutoAwesome,
                                onClick = {
                                    coroutineScope.launch {
                                        isOptimizingNotrackBranding = true
                                        seoStatusMessage = "🤖 Contacting AI Neural Engine via MasterPrompt for Channel SEO..."
                                        val notrack = com.example.pipeline.NotrackWebViewClient.create(context)
                                        val freshSeo = YouTubeChannelSeoManager.generateChannelSeoWithNotrack(
                                            context = context,
                                            notrack = notrack,
                                            category = selectedCategory,
                                            currentChannelName = channelInfo?.channelName,
                                            currentHandle = channelInfo?.channelHandle,
                                            subscriberCount = channelInfo?.subscriberCount
                                        )
                                        activeSeoData = freshSeo
                                        isOptimizingNotrackBranding = false
                                        seoStatusMessage = "✅ Live Channel Branding AI Optimized!"
                                        Toast.makeText(context, "Channel Branding AI Optimized! 🎉", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Apply to YouTube Channel Button
                        item {
                            NeonButton(
                                text = if (isApplyingSeo) "APPLYING BRANDING VIA API..." else "⚡ SYNC BRANDING TO YOUTUBE CHANNEL",
                                leadingIcon = Icons.Default.AutoFixHigh,
                                onClick = {
                                    coroutineScope.launch {
                                        isApplyingSeo = true
                                        seoStatusMessage = null
                                        val res = YouTubeChannelSeoManager.updateChannelBrandingViaApi(
                                            context = context,
                                            newDescription = activeSeoData.optimizedDescription,
                                            keywords = activeSeoData.searchKeywords
                                        )
                                        isApplyingSeo = false
                                        if (res.isSuccess) {
                                            seoStatusMessage = "✅ YouTube Channel Branding & Keywords Updated Successfully!"
                                            Toast.makeText(context, "Channel Branding Updated on YouTube! 🎉", Toast.LENGTH_LONG).show()
                                        } else {
                                            val err = res.exceptionOrNull()?.message ?: "Direct API update failed"
                                            seoStatusMessage = err
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (seoStatusMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = seoStatusMessage ?: "",
                                    fontSize = 11.sp,
                                    color = if (seoStatusMessage?.startsWith("✅") == true) NeonGreen else Color(0xFFFF5252),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (seoStatusMessage?.contains("🔑") == true || seoStatusMessage?.contains("Login") == true || seoStatusMessage?.contains("Permission") == true || seoStatusMessage?.contains("Sign-In") == true || seoStatusMessage?.contains("OAuth") == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                                putExtra("FORCE_LOGIN", true)
                                            })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🔑 RE-AUTHENTICATE / LOGIN TO YOUTUBE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        } else {
                            // SUB-TAB 1: topic-based video metadata drafts
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "VIDEO METADATA DRAFTS",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            color = ElectricViolet
                                        )
                                        Surface(
                                            color = ElectricViolet.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "LOCAL TEMPLATES",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricViolet,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = videoSeoTopicInput,
                                        onValueChange = { videoSeoTopicInput = it },
                                        label = { Text("Video Topic or Story Hook", fontSize = 12.sp) },
                                        placeholder = { Text("Enter the actual video topic", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                                            focusedLabelColor = CyberCyan
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    NeonButton(
                                        text = "GENERATE TITLE & DESCRIPTION DRAFTS",
                                        leadingIcon = Icons.Default.AutoAwesome,
                                        onClick = {
                                            val topic = videoSeoTopicInput.trim()
                                            if (topic.isBlank()) {
                                                activeVideoSeoData = null
                                                Toast.makeText(context, "Enter a real video topic first.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                activeVideoSeoData = com.example.util.VideoSeoOptimizer.generateDeepVideoSeo(
                                                    topic = topic,
                                                    category = selectedCategory,
                                                    language = settings.defaultLanguage
                                                )
                                                Toast.makeText(context, "Generated local metadata drafts. Review before use.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            val seoData = activeVideoSeoData
                            if (seoData == null) {
                                item {
                                    Text(
                                        text = "Enter a topic and generate drafts. No video performance or search-volume data is measured here.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
                                    )
                                }
                            } else {
                            // 1. Title drafts
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TITLE DRAFTS • NO CTR PREDICTION",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "Local metadata checks: ${
                                                com.example.util.VideoSeoOptimizer.calculateMetadataChecklistScore(
                                                    topic = seoData.topic,
                                                    title = seoData.currentSelectedTitle,
                                                    description = seoData.richDescription.fullDescriptionText,
                                                    tags = seoData.suggestedTags
                                                )
                                            }/100 (not a performance prediction)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    seoData.titleVariations.forEach { titleVar ->
                                        Surface(
                                            color = DarkSurfaceElevated,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${titleVar.emoji} ${titleVar.type}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CyberCyan
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = titleVar.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = titleVar.hookAngle,
                                                        fontSize = 10.sp,
                                                        color = TextSecondary,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            copyToClipboard(titleVar.title, "Title")
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copy",
                                                            tint = CyberCyan,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Full-Structured Description
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "VIDEO DESCRIPTION DRAFT",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricViolet
                                        )
                                        IconButton(
                                            onClick = {
                                                copyToClipboard(seoData.richDescription.fullDescriptionText, "Video Description Draft")
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy All", tint = CyberCyan)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = DarkSurfaceElevated,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = seoData.richDescription.summary,
                                                fontSize = 12.sp,
                                                color = TextPrimary,
                                                lineHeight = 18.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = seoData.richDescription.questionPrompt,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonYellow
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = seoData.richDescription.hashtags.joinToString(" "),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. 500-Character Viral Tags
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "TOPIC-BASED TAGS • SEARCH VOLUME NOT MEASURED",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonYellow
                                            )
                                            Text(
                                                text = "${seoData.totalTagCharacters} / 500 characters",
                                                fontSize = 10.sp,
                                                color = if (seoData.totalTagCharacters <= 500) NeonGreen else CoralPink
                                            )
                                        }
                                        Surface(
                                            color = NeonYellow.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.clickable {
                                                val commaSeparated = seoData.suggestedTags.joinToString(", ")
                                                copyToClipboard(commaSeparated, "Topic Tags")
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Tags", tint = NeonYellow, modifier = Modifier.size(14.dp))
                                                Text("Copy All", fontSize = 11.sp, color = NeonYellow, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        seoData.suggestedTags.forEach { tag ->
                                            Surface(
                                                color = DarkSurfaceElevated,
                                                shape = RoundedCornerShape(6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow.copy(alpha = 0.3f)),
                                                modifier = Modifier.clickable {
                                                    copyToClipboard(tag, "Tag: $tag")
                                                }
                                            ) {
                                                Text(
                                                    text = tag,
                                                    fontSize = 11.sp,
                                                    color = TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Cinematic Thumbnail & Dramatic Frames
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "VIDEO FRAME ANALYSIS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Frame analysis requires a rendered video. Open a saved project's details to inspect frames sampled from its actual video.",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: AUTO-PLAYLIST MANAGER (PHASE 1 & STEP 6)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tabPadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(contentSpacing)
                    ) {
                        // ⚡ STEP 6: AUTONOMOUS SMART BINGE-WATCH PLAYLIST STUDIO (NOTRACK AI)
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(text = "🎬", fontSize = 16.sp)
                                            Column {
                                                 Text(
                                                    text = "BINGE-LOOP STUDIO",
                                                    fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "AI High-Retention Series & Playlists",
                                                    fontSize = 9.5.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Surface(
                                            color = NeonGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "STEP 6 READY",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Synthesizes viral multi-part series, cliffhanger playlists, and binge-watch hooks for '$selectedCategory' to maximize session watch time.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    Button(
                                        onClick = { generateSmartPlaylists() },
                                        enabled = !isGeneratingSmartPlaylists,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        if (isGeneratingSmartPlaylists) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generating Binge Series via Glitch AI...", fontSize = 10.sp, color = Color.Black)
                                        } else {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🤖 GENERATE VIRAL BINGE PLAYLISTS (GLITCH AI)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                        }
                                    }

                                    if (smartPlaylistSuggestions.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            smartPlaylistSuggestions.forEach { suggestion ->
                                                Surface(
                                                    color = DarkSurfaceElevated,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = suggestion.title,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = NeonGreen,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                color = ElectricViolet.copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = suggestion.estimatedRetentionMultiplier,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = ElectricViolet,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text = suggestion.description,
                                                            fontSize = 10.sp,
                                                            color = TextSecondary
                                                        )

                                                        Text(
                                                            text = "🪝 Binge Hook: ${suggestion.bingeWatchHook}",
                                                            fontSize = 10.sp,
                                                            color = CyberCyan
                                                        )

                                                        if (suggestion.recommendedVideoTitles.isNotEmpty()) {
                                                            Text(
                                                                text = "📺 Series Sequence: ${suggestion.recommendedVideoTitles.joinToString(" ➔ ")}",
                                                                fontSize = 9.sp,
                                                                color = TextMuted
                                                            )
                                                        }

                                                        Button(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    val res = AutoPlaylistManager.createPlaylist(
                                                                        context = context,
                                                                        title = suggestion.title,
                                                                        description = suggestion.description
                                                                    )
                                                                    if (res.isSuccess) {
                                                                        Toast.makeText(context, "Playlist '${suggestion.title}' created on YouTube! 🎬", Toast.LENGTH_SHORT).show()
                                                                        loadPlaylists()
                                                                    } else {
                                                                        Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(30.dp),
                                                            shape = RoundedCornerShape(4.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = NeonGreen,
                                                                contentColor = Color.Black
                                                            )
                                                        ) {
                                                            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("CREATE ON YOUTUBE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "📑 AUTOMATIC PLAYLISTS ENGINE",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "Organizes uploaded shorts into category playlists for high algorithm watch time.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(onClick = { loadPlaylists() }) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyan)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                NeonButton(
                                    text = if (isCreatingPlaylists) "CREATING PLAYLISTS..." else "⚡ AUTO-CREATE 10 NICHE PLAYLISTS",
                                    leadingIcon = Icons.Default.PlaylistAdd,
                                    onClick = {
                                        coroutineScope.launch {
                                            isCreatingPlaylists = true
                                            var createdCount = 0
                                            for (cat in categories) {
                                                val (plTitle, plDesc) = AutoPlaylistManager.getCategoryConfig(cat)
                                                val res = AutoPlaylistManager.createPlaylist(context, plTitle, plDesc)
                                                if (res.isSuccess) {
                                                    createdCount++
                                                }
                                            }
                                            isCreatingPlaylists = false
                                            Toast.makeText(context, "Created $createdCount niche playlists on YouTube! 🎬", Toast.LENGTH_LONG).show()
                                            loadPlaylists()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item {
                            Text(
                                text = "CHANNEL PLAYLISTS (${userPlaylists.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (isLoadingPlaylists) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = CyberCyan)
                                }
                            }
                        } else if (userPlaylists.isEmpty()) {
                            item {
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = if (currentUser != null) "No playlists found yet. Tap 'Auto-Create 10 Niche Playlists' above to set them up instantly!" else "Connect your YouTube account to view and manage playlists.",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            items(userPlaylists) { pl ->
                                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (!pl.thumbnailUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = pl.thumbnailUrl,
                                                contentDescription = pl.title,
                                                modifier = Modifier
                                                    .size(60.dp, 45.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(DarkSurfaceElevated)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp, 45.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(DarkSurfaceElevated),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = null, tint = CyberCyan)
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pl.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${pl.itemCount} videos • ${pl.privacyStatus.uppercase()}",
                                                fontSize = 11.sp,
                                                color = NeonGreen
                                            )
                                        }

                                        IconButton(onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=${pl.id}"))
                                            context.startActivity(intent)
                                        }) {
                                            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open", tint = CyberCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: AI COMMUNITY POST STUDIO (PHASE 1)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = tabPadding, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(contentSpacing)
                    ) {
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(text = "💬", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "AUTONOMOUS COMMUNITY STUDIO",
                                                    fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "AI-generated editable drafts for $selectedCategory",
                                                    fontSize = 9.5.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Surface(
                                            color = NeonGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "STEP 3 READY",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { generateCommunityPostsWithNotrack() },
                                        enabled = !isGeneratingCommunityPostsFromNotrack,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        if (isGeneratingCommunityPostsFromNotrack) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Synthesizing Viral Polls with Glitch AI...", fontSize = 10.sp, color = Color.Black)
                                        } else {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🤖 GENERATE COMMUNITY POST DRAFTS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = Color.Black, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // 🛡️ AUTONOMOUS CHANNEL GUARDIAN & QUOTA LEDGER (STEP 1)
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(text = "🛡️", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "AUTONOMOUS GUARDIAN",
                                                    fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = CyberCyan,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Zero-Interaction AI Comment & Growth Bot",
                                                    fontSize = 9.5.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Surface(
                                            color = NeonGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "STEP 1 ACTIVE",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreen,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Daily Quota Ledger
                                    val usedUnits = todayQuota?.usedUnits ?: 0
                                    val quotaFraction = (usedUnits.toFloat() / 10000f).coerceIn(0f, 1f)
                                    val quotaColor = when {
                                        usedUnits > 8000 -> ElectricViolet
                                        usedUnits > 5000 -> Color(0xFFFFB300)
                                        else -> NeonGreen
                                    }

                                    Surface(
                                        color = DarkBackground,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "📊 API QUOTA LEDGER",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "$usedUnits / 10,000 u",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = quotaColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { quotaFraction },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = quotaColor,
                                                trackColor = DarkSurfaceElevated
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Up: ${todayQuota?.uploadCountToday ?: 0}",
                                                    fontSize = 8.5.sp,
                                                    color = TextMuted
                                                )
                                                Text(
                                                    text = "Replies: ${todayQuota?.commentsRepliedToday ?: 0}",
                                                    fontSize = 8.5.sp,
                                                    color = TextMuted
                                                )
                                                Text(
                                                    text = "Left: ${10000 - usedUnits}u",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberCyan
                                                )
                                            }
                                        }
                                    }

                                    // Periodic Bot Toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurfaceElevated)
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Background Auto-Reply Bot",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (autonomousBotPeriodicEnabled) "🟢 Active: Scans & replies every 30m via Glitch AI" else "⚪ Disabled (Periodic background scan off)",
                                                fontSize = 10.sp,
                                                color = if (autonomousBotPeriodicEnabled) NeonGreen else TextSecondary
                                            )
                                        }
                                        Switch(
                                            checked = autonomousBotPeriodicEnabled,
                                            onCheckedChange = { enabled ->
                                                autonomousBotPeriodicEnabled = enabled
                                                if (enabled) {
                                                    AutonomousChannelWorker.schedulePeriodic(context, 30)
                                                    Toast.makeText(context, "Autonomous Channel Guardian activated! 🤖", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    AutonomousChannelWorker.cancel(context)
                                                    Toast.makeText(context, "Autonomous Channel Guardian paused.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NeonGreen,
                                                checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                                                uncheckedThumbColor = TextMuted,
                                                uncheckedTrackColor = DarkBackground
                                            )
                                        )
                                    }

                                    // Manual Autonomous Trigger Button
                                    NeonButton(
                                        text = if (isRunningAutonomousScan) "GLITCH AI IS SCANNING & REPLYING..." else "⚡ RUN AUTONOMOUS GLITCH SCAN NOW",
                                        leadingIcon = Icons.Default.AutoAwesome,
                                        enabled = !isRunningAutonomousScan,
                                        onClick = {
                                            triggerAutonomousScan()
                                        }
                                    )

                                    if (!autonomousScanResult.isNullOrBlank()) {
                                        Surface(
                                            color = CyberCyan.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "🤖 $autonomousScanResult",
                                                fontSize = 11.sp,
                                                color = CyberCyan,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 💬 LIVE COMMENTS & GLITCH CONVERSATION ENGINE
                        item {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "💬", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "CHANNEL COMMENTS & GLITCH AI",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CyberCyan
                                                )
                                                Text(
                                                    text = "Powered by Glitch Neural Engine for high-conversion replies",
                                                    fontSize = 9.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { loadCommentsAndQuota() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh Comments",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    if (isLoadingComments) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = CyberCyan
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Scanning YouTube Data API for live comments...",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Display Real Live Comments from YouTube
                                    if (liveComments.isNotEmpty()) {
                                        Text(
                                            text = "🎯 ${liveComments.size} Live Comments Found on Recent Videos:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonGreen
                                        )

                                        liveComments.forEach { item ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                color = DarkBackground,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (item.isReplied) NeonGreen.copy(alpha = 0.4f) else DarkBorder
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(
                                                                text = "👤 ${item.authorName}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = CyberCyan
                                                            )
                                                            if (item.publishedAt.isNotBlank()) {
                                                                Text(
                                                                    text = " • ${item.publishedAt}",
                                                                    fontSize = 9.sp,
                                                                    color = TextMuted
                                                                )
                                                            }
                                                        }
                                                        Surface(
                                                            color = if (item.isReplied) NeonGreen.copy(alpha = 0.2f) else CyberCyan.copy(alpha = 0.15f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = if (item.isReplied) "✅ REPLIED" else "⚡ UNREPLIED",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (item.isReplied) NeonGreen else CyberCyan,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Video: ${item.videoTitle}",
                                                        fontSize = 9.sp,
                                                        color = TextMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "\"${item.text}\"",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextPrimary
                                                    )

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Generate via Glitch AI Button
                                                    val isGeneratingThis = isGeneratingNoTrackReply == item.threadId
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "⚡ GLITCH AI REPLIES:",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NeonGreen
                                                        )
                                                        TextButton(
                                                            onClick = { generateNoTrackReplyForComment(item) },
                                                            enabled = !isGeneratingThis,
                                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            if (isGeneratingThis) {
                                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = CyberCyan)
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Thinking...", fontSize = 9.sp, color = CyberCyan)
                                                            } else {
                                                                Text("🤖 Ask Glitch AI", fontSize = 9.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }

                                                    // Replies (from NoTrack cache or generator)
                                                    val repliesToShow = commentGeneratedReplies[item.threadId]
                                                        ?: item.smartReplies

                                                    if (repliesToShow.isEmpty()) {
                                                        Text(
                                                            text = "No provider-generated replies available. Select Ask Glitch AI to try again.",
                                                            fontSize = 10.sp,
                                                            color = TextMuted
                                                        )
                                                    }

                                                    repliesToShow.forEach { reply ->
                                                        val isPostingThis = isPostingCommentReply == item.threadId
                                                        Surface(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 3.dp),
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = DarkSurfaceElevated,
                                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CyberCyan.copy(alpha = 0.3f))
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Text(text = reply.emoji, fontSize = 11.sp)
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = reply.category.uppercase(),
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = CyberCyan
                                                                        )
                                                                    }
                                                                    IconButton(
                                                                        onClick = { copyToClipboard(reply.replyText, "Comment Reply") },
                                                                        modifier = Modifier.size(20.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.ContentCopy,
                                                                            contentDescription = "Copy",
                                                                            tint = TextSecondary,
                                                                            modifier = Modifier.size(12.dp)
                                                                        )
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = reply.replyText,
                                                                    fontSize = 11.sp,
                                                                    color = TextPrimary
                                                                )

                                                                Spacer(modifier = Modifier.height(6.dp))

                                                                // POST DIRECTLY TO YOUTUBE BUTTON
                                                                Button(
                                                                    onClick = {
                                                                        postReplyToYouTube(
                                                                            threadId = item.threadId,
                                                                            replyText = reply.replyText,
                                                                            videoId = item.videoId,
                                                                            authorName = item.authorName,
                                                                            originalText = item.text
                                                                        )
                                                                    },
                                                                    enabled = !isPostingThis,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(30.dp),
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = if (item.isReplied) DarkSurfaceElevated else CyberCyan.copy(alpha = 0.2f),
                                                                        contentColor = if (item.isReplied) TextMuted else CyberCyan
                                                                    ),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                ) {
                                                                    if (isPostingThis) {
                                                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = CyberCyan)
                                                                        Spacer(modifier = Modifier.width(6.dp))
                                                                        Text("Posting to YouTube...", fontSize = 9.sp)
                                                                    } else {
                                                                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(10.dp))
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text(
                                                                            text = if (item.isReplied) "RE-POST REPLY VIA API (50u)" else "🚀 POST TO YOUTUBE VIA API (50u)",
                                                                            fontSize = 9.sp,
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
                                    } else if (!isLoadingComments) {
                                        // Do not present generated examples as live channel comments.
                                        Surface(
                                            color = DarkSurfaceElevated,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = commentsLoadError?.let { "Could not load live YouTube comments: $it" }
                                                        ?: "ℹ️ No live comments were returned for the selected channel/videos.",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "Refresh the list or check channel access and YouTube permissions. AI replies are only available after a live comment is loaded.",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (communityPosts.isEmpty()) {
                            item {
                                Text(
                                    text = "No community post drafts loaded. Generate drafts with the configured AI provider; review claims and details before sharing.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)
                                )
                            }
                        }

                        items(communityPosts) { post ->
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = post.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = when (post.type) {
                                                "POLL" -> CyberCyan
                                                "TEASER" -> Color(0xFFFF5722)
                                                else -> NeonGreen
                                            }
                                        )
                                        Surface(
                                            color = DarkSurfaceElevated,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "AI DRAFT • REVIEW BEFORE SHARING",
                                                fontSize = 9.sp,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = post.textContent,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 17.sp
                                    )

                                    if (post.pollOptions.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            post.pollOptions.forEach { opt ->
                                                Surface(
                                                    color = DarkSurfaceElevated,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "◽ $opt",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (post.hashtags.isNotEmpty()) {
                                        Text(
                                            text = post.hashtags.joinToString(" "),
                                            fontSize = 11.sp,
                                            color = CyberCyan
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    copyToClipboard(
                                                        post.textContent + if (post.pollOptions.isNotEmpty()) "\n\n" + post.pollOptions.joinToString("\n") else "",
                                                        "Community Post"
                                                    )
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Copy", fontSize = 11.sp, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    CommunityPostManager.sharePostContent(context, post)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Share", fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                CommunityPostManager.publishDirectlyToYouTube(context, post)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🚀 PUBLISH TO YOUTUBE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: ADVANCED ANALYTICS & PUBLISHED VIDEOS
                    if (currentUser == null && !GoogleSignInHelper.hasValidYouTubePermissions(context)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = tabPadding, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "YouTube Account Not Connected",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isCompact) 14.sp else 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Connect your YouTube channel to view real-time subscriber counts, video views, audience engagement rates, and live performance metrics.",
                                        fontSize = if (isCompact) 11.sp else 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                                putExtra("FORCE_LOGIN", true)
                                            })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                    ) {
                                        Text("Connect YouTube Account", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else if (isLoadingChannel || isLoadingVideos) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyberCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Fetching live channel analytics...", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    } else {
                        val realSubscribers = channelInfo?.subscriberCount ?: "0"
                        val realLifetimeViews = channelInfo?.viewCount ?: "0"
                        val totalVideosCount = channelInfo?.videoCount ?: "${channelVideos.size}"

                        var totalVideoViews = 0L
                        var totalVideoLikes = 0L
                        var totalVideoComments = 0L

                        for (v in channelVideos) {
                            totalVideoViews += v.views.toLongOrNull() ?: 0L
                            totalVideoLikes += v.likes.toLongOrNull() ?: 0L
                            totalVideoComments += v.comments.toLongOrNull() ?: 0L
                        }

                        val loadedCount = channelVideos.size
                        val avgViewsPerVideo = if (loadedCount > 0) totalVideoViews / loadedCount else 0L
                        val engagementRate = if (totalVideoViews > 0) {
                            ((totalVideoLikes + totalVideoComments).toDouble() / totalVideoViews * 100).let {
                                String.format(Locale.getDefault(), "%.1f%%", it)
                            }
                        } else {
                            "0.0%"
                        }

                        val realViewsData = channelVideos.reversed().map { (it.views.toLongOrNull() ?: 0L).toFloat() }
                        val realLikesData = channelVideos.reversed().map { (it.likes.toLongOrNull() ?: 0L).toFloat() }

                        // Phase 3: Self-Learning Engine Report
                        val selfLearningReport = remember(channelVideos, selectedCategory) {
                            SelfLearningEngine.analyzeChannelPerformance(
                                videos = channelVideos,
                                fallbackCategory = selectedCategory
                            )
                        }

                        val peakTimingOptions = remember { SelfLearningEngine.getPeakTimingOptions() }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = tabPadding, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(contentSpacing)
                        ) {
                            // Header
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "CREATOR PERFORMANCE INTELLIGENCE",
                                                fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                color = CyberCyan,
                                                maxLines = 1
                                            )
                                            Surface(
                                                color = NeonGreen.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "AI LOOP",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeonGreen,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${channelInfo?.channelName ?: "YouTube Channel"} • Live Analytics",
                                            fontSize = 9.5.sp,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { coroutineScope.launch { refreshChannelData() } },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Sub-Tabs Navigation (Auto-Pivot, Best Timing, Leaderboard)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val tabs = listOf(
                                        "🧠 Auto-Pivot" to 0,
                                        "⏰ Best Time" to 1,
                                        "🏆 Rankings" to 2
                                    )
                                    tabs.forEach { (title, idx) ->
                                        val isSelected = analyticsSubTab == idx
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                                                .clickable { analyticsSubTab = idx }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) CyberCyan else TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            when (analyticsSubTab) {
                                0 -> {
                                    // SUB-TAB 0: LIVE ANALYTICS & SMART AUTO-PIVOT
                                    item {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            MetricCard("Subscribers", realSubscribers, "Channel total", NeonGreen, Modifier.weight(1f))
                                            MetricCard("Lifetime Views", realLifetimeViews, "Channel total", CyberCyan, Modifier.weight(1f))
                                        }
                                    }

                                    item {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            MetricCard("Avg Engagement", engagementRate, "$totalVideoLikes likes", ElectricViolet, Modifier.weight(1f))
                                            MetricCard("Avg Short Views", "$avgViewsPerVideo", "$loadedCount videos", Color(0xFFFFD700), Modifier.weight(1f))
                                        }
                                    }

                                    // ⚡ STEP 4: AUTONOMOUS RETENTION & PERFORMANCE WATCHDOG (NOTRACK AI)
                                    item {
                                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Text(text = "🎯", fontSize = 16.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = "RETENTION WATCHDOG",
                                                                fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = CyberCyan,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = "AI Underperformance Audit & Hook Revitalizer",
                                                                fontSize = 9.5.sp,
                                                                color = TextSecondary,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        color = NeonGreen.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = "STEP 4 READY",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NeonGreen,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = { runWatchdogDiagnosis() },
                                                    enabled = !isRunningWatchdogDiagnosis,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(38.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = CyberCyan,
                                                        contentColor = Color.Black
                                                    )
                                                ) {
                                                    if (isRunningWatchdogDiagnosis) {
                                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color.Black)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Running AI Retention & Hook Audit...", fontSize = 10.sp, color = Color.Black)
                                                    } else {
                                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("🤖 RUN GLITCH RETENTION AUDIT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                                    }
                                                }

                                                if (watchdogSummary != null) {
                                                    Surface(
                                                        color = DarkSurfaceElevated,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "VIEW-COUNT REVIEW",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = NeonGreen
                                                                )
                                                                Text(
                                                                    text = "${watchdogSummary?.totalVideosAudited ?: 0} Videos Audited",
                                                                    fontSize = 10.sp,
                                                                    color = TextMuted
                                                                )
                                                            }

                                                            Text(
                                                                text = "💡 Strategy: ${watchdogSummary?.overallRetentionStrategy}",
                                                                fontSize = 10.sp,
                                                                color = TextSecondary,
                                                                lineHeight = 14.sp
                                                            )

                                                            watchdogSummary?.topOpportunityVideo?.let { topVid ->
                                                                Divider(color = DarkBorder, thickness = 0.5.dp)
                                                                Text(
                                                                    text = "⚡ BELOW-AVERAGE VIEW OPPORTUNITY:",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFFF5722)
                                                                )
                                                                Text(
                                                                    text = "Video: ${topVid.title}",
                                                                    fontSize = 10.sp,
                                                                    color = TextPrimary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "💡 Title suggestion: ${topVid.optimizedTitle}",
                                                                    fontSize = 10.sp,
                                                                    color = CyberCyan,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Text(
                                                                    text = "🎨 Thumbnail Hook: ${topVid.thumbnailHookSuggestion}",
                                                                    fontSize = 10.sp,
                                                                    color = TextSecondary
                                                                )
                                                                Text(
                                                                    text = "💬 Pinned Booster: ${topVid.pinnedCommentBooster}",
                                                                    fontSize = 10.sp,
                                                                    color = TextMuted
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // SMART TOPIC ADAPTATION (AUTO-PIVOT) CARD
                                    item {
                                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "SMART AUTO-PIVOT ENGINE",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NeonGreen
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(NeonGreen.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("AI LOOP ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                                        }
                                                    }
                                                    Text(
                                                        text = "Auto-detects high-performing genres & increases winning format weight",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                                Switch(
                                                    checked = isAutoPivotEnabled,
                                                    onCheckedChange = {
                                                        isAutoPivotEnabled = it
                                                        val updated = settings.copy(smartAutoPivotEnabled = it)
                                                        viewModel.updateSettings(updated)
                                                        Toast.makeText(context, if (it) "Auto-Pivot AI Enabled!" else "Auto-Pivot Disabled", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = NeonGreen,
                                                        checkedTrackColor = NeonGreen.copy(alpha = 0.3f)
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Winning Genre Banner
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkSurfaceVariant)
                                                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "🏆 WINNING FORMAT & GENRE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeonGreen
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = selfLearningReport.winningGenre,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "This format achieves the highest retention & likes ratio on your channel. Next generation weight dynamically boosted.",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Dynamic Category Weight Distribution
                                            Text(
                                                text = "DYNAMIC CATEGORY ALLOCATION WEIGHTS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            selfLearningReport.categoryWeights.take(4).forEach { catPerf ->
                                                Column(modifier = Modifier.padding(vertical = 3.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = catPerf.category,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (catPerf.category == selfLearningReport.winningGenre) NeonGreen else TextPrimary
                                                        )
                                                        Text(
                                                            text = "${catPerf.weightPercentage}% weight • ${catPerf.totalViews} views",
                                                            fontSize = 10.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    LinearProgressIndicator(
                                                        progress = { (catPerf.weightPercentage / 100f).coerceIn(0.05f, 1f) },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(6.dp)
                                                            .clip(RoundedCornerShape(3.dp)),
                                                        color = if (catPerf.category == selfLearningReport.winningGenre) NeonGreen else CyberCyan,
                                                        trackColor = DarkBackground
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Recommended Next Topic
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkBackground)
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text("🔥 AI ADAPTED NEXT TOPIC:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(selfLearningReport.recommendedNextTopic, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Action Button to Auto-Pivot Autopilot immediately
                                            NeonButton(
                                                text = "Adapt Autopilot To Winning Genre (${selfLearningReport.winningGenre})",
                                                onClick = {
                                                    val updated = settings.copy(
                                                        autoCategory = selfLearningReport.winningGenre,
                                                        topPerformingCategory = selfLearningReport.winningGenre,
                                                        smartAutoPivotEnabled = true
                                                    )
                                                    viewModel.updateSettings(updated)
                                                    selectedCategory = selfLearningReport.winningGenre
                                                    Toast.makeText(context, "Autopilot adapted to ${selfLearningReport.winningGenre}! 🚀", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // View Performance Chart
                                    item {
                                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                            Text(text = "Recent Video Views Performance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = "Real views per published video", fontSize = 10.sp, color = TextSecondary)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            if (realViewsData.size >= 2) {
                                                SimpleLineChart(
                                                    data = realViewsData,
                                                    lineColor = CyberCyan,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                )
                                            } else if (realViewsData.isNotEmpty()) {
                                                Text("1 video analyzed: ${realViewsData.first().toLong()} views", color = TextSecondary, fontSize = 12.sp)
                                            } else {
                                                Text("No published videos found on this channel yet.", color = TextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    // SUB-TAB 1: BEST TIME TO POST (SMART PUBLISHING TIMING)
                                    item {
                                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("BEST TIME TO POST (SMART TIMING)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                                    Text("Target peak audience active hours for maximum algorithmic momentum", fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Timing Mode Selection
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkSurfaceVariant)
                                                    .padding(4.dp)
                                            ) {
                                                listOf("SMART_PEAK" to "Golden Hour Peak", "FIXED_INTERVAL" to "Fixed Interval").forEach { (mode, label) ->
                                                    val isSelected = smartTimingMode == mode
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSelected) CyberCyan.copy(alpha = 0.25f) else Color.Transparent)
                                                            .clickable {
                                                                smartTimingMode = mode
                                                                val updated = settings.copy(smartTimingMode = mode)
                                                                viewModel.updateSettings(updated)
                                                            }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) CyberCyan else TextSecondary
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Text("SELECT AUDIENCE PEAK WINDOW:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            peakTimingOptions.forEach { opt ->
                                                val isSelected = selectedPeakHour == opt.hour
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.12f) else DarkBackground)
                                                        .border(
                                                            width = if (isSelected) 1.5.dp else 1.dp,
                                                            color = if (isSelected) CyberCyan else DarkSurfaceVariant,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            selectedPeakHour = opt.hour
                                                        }
                                                        .padding(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(text = opt.timeDisplay, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                if (opt.isRecommended) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(4.dp))
                                                                            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
                                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    ) {
                                                                        Text("SCHEDULE PRESET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                                                                    }
                                                                }
                                                            }
                                                            Text(text = opt.description, fontSize = 10.sp, color = TextSecondary)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(CyberCyan.copy(alpha = 0.15f))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(opt.evidenceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Calculated Next Post Window
                                            val nextPeakTs = remember(selectedPeakHour) {
                                                SelfLearningEngine.calculateNextPeakPublishTimestamp(selectedPeakHour)
                                            }
                                            val formattedNextPeak = remember(nextPeakTs) {
                                                SimpleDateFormat("EEEE, dd MMM 'at' hh:mm a", Locale.getDefault()).format(Date(nextPeakTs))
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(DarkBackground)
                                                    .padding(12.dp)
                                            ) {
                                                Column {
                                                    Text("📅 NEXT SCHEDULED PEAK UPLOAD:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(formattedNextPeak, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                                                    Text("Autopilot will generate and launch your video right into this high-velocity viewing window.", fontSize = 10.sp, color = TextSecondary)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            NeonButton(
                                                text = "Save & Apply Peak Time Scheduling",
                                                onClick = {
                                                    val updated = settings.copy(
                                                        smartTimingMode = "SMART_PEAK",
                                                        smartPeakHour = selectedPeakHour,
                                                        autoPublishEnabled = true
                                                    )
                                                    viewModel.updateSettings(updated)
                                                    AutomationSchedulerManager.scheduleRecurringAutomation(
                                                        context = context,
                                                        intervalHours = settings.scheduleIntervalHours,
                                                        category = selectedCategory,
                                                        language = settings.defaultLanguage,
                                                        autoUpload = true
                                                    )
                                                    Toast.makeText(context, "Scheduled at $selectedPeakHour:00 Peak Window! ⏰", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                2 -> {
                                    // SUB-TAB 2: VIDEO PERFORMANCE RANKINGS (LEADERBOARD)
                                    item {
                                        Text(
                                            text = "PUBLISHED VIDEOS LEADERBOARD (${channelVideos.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    if (channelVideos.isEmpty()) {
                                        item {
                                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                                Text(text = "No published videos found on this channel yet.", color = TextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    } else {
                                        val rankedVideos = channelVideos.sortedByDescending { it.views.toLongOrNull() ?: 0L }
                                        items(rankedVideos.size) { index ->
                                            val video = rankedVideos[index]
                                            GlitchCard(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    // Rank Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when (index) {
                                                                    0 -> Color(0xFFFFD700)
                                                                    1 -> Color(0xFFC0C0C0)
                                                                    2 -> Color(0xFFCD7F32)
                                                                    else -> DarkSurfaceVariant
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = when (index) {
                                                                0 -> "🥇"
                                                                1 -> "🥈"
                                                                2 -> "🥉"
                                                                else -> "#${index + 1}"
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (index > 2) TextPrimary else Color.Black
                                                        )
                                                    }

                                                    if (video.thumbnailUrl.isNotBlank()) {
                                                        AsyncImage(
                                                            model = video.thumbnailUrl,
                                                            contentDescription = "Thumbnail",
                                                            modifier = Modifier
                                                                .size(55.dp, 75.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(DarkSurface)
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = video.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "👁 ${video.views} views • 👍 ${video.likes} likes • 💬 ${video.comments}",
                                                            fontSize = 11.sp,
                                                            color = CyberCyan
                                                        )
                                                        Text(
                                                            text = "Published: ${video.publishedAt}",
                                                            fontSize = 10.sp,
                                                            color = TextSecondary
                                                        )
                                                    }

                                                    IconButton(onClick = {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
                                                        context.startActivity(intent)
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.OpenInNew,
                                                            contentDescription = "View on YouTube",
                                                            tint = CyberCyan
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
                }
                5 -> {
                    // TAB 5: YOUTUBE AI CHATBOT (NOTRACK LIVE SCRAPER ENGINE)
                    YouTubeAiChatbotScreen(
                        viewModel = viewModel,
                        onNavigateBack = { selectedTab = 0 },
                        onNavigateToPreview = onNavigateToPreview,
                        hideTopBar = true
                    )
                }
            }
        }
    }
}
}

    if (showAutopilotPermissionAlert) {
        AlertDialog(
            onDismissRequest = { showAutopilotPermissionAlert = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ElectricViolet
                    )
                    Text(
                        text = autopilotAlertTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = autopilotAlertMessage,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                val hasGoogleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context) != null
                val hasYtScope = GoogleSignInHelper.hasValidYouTubePermissions(context)
                val hasNotif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!hasGoogleAccount || !hasYtScope) {
                        Button(
                            onClick = {
                                showAutopilotPermissionAlert = false
                                context.startActivity(Intent(context, LoginActivity::class.java).apply {
                                    putExtra("FORCE_LOGIN", true)
                                })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pehle Sign-In/Scope Permission dekar Account connect karein", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    if (!hasNotif && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = {
                                showAutopilotPermissionAlert = false
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Notification Permission Allow Karein", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    TextButton(
                        onClick = { showAutopilotPermissionAlert = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close", color = TextMuted)
                    }
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Add New Channel Dialog ──
    if (showAddChannelDialog) {
        val categoriesList = listOf(
            "Technology", "AI & Futuristic", "Horror & Thriller", "Mystery & Crime",
            "History & Culture", "Motivation & Success", "Science & Facts",
            "Business & Money", "Gaming", "Entertainment & Cinema", "Health & Fitness", "Anime & Manga"
        )
        AlertDialog(
            onDismissRequest = { showAddChannelDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🌐", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add YouTube Channel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Connect another YouTube channel for multi-channel autonomous publishing.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newChannelTitleInput,
                        onValueChange = { newChannelTitleInput = it },
                        label = { Text("Channel Name / Title") },
                        placeholder = { Text("e.g. AI Horizons, Dark Mystery Stories") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = newChannelHandleInput,
                        onValueChange = { newChannelHandleInput = it },
                        label = { Text("Channel Handle (Optional)") },
                        placeholder = { Text("@aihorizons") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Text("🎯 Target Niche / Category for this channel:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categoriesList.forEach { cat ->
                            val selected = cat == newChannelCategoryInput
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { newChannelCategoryInput = cat },
                                color = if (selected) CyberCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) CyberCyan else DarkBorder)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) CyberCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable AutoPilot for this Channel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Auto-generate & upload videos to this channel", fontSize = 9.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = newChannelAutoPilotInput,
                            onCheckedChange = { newChannelAutoPilotInput = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(alpha = 0.5f))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newChannelTitleInput.trim().ifEmpty { "YouTube Channel ${channelAccounts.size + 1}" }
                        val handle = newChannelHandleInput.trim().let { if (it.isEmpty() || it.startsWith("@")) it else "@$it" }
                        val channelId = newChannelIdInput.trim().ifEmpty { "UC" + UUID.randomUUID().toString().replace("-", "").take(22) }

                        coroutineScope.launch {
                            val entity = com.example.data.local.YouTubeChannelAccountEntity(
                                channelId = channelId,
                                channelTitle = title,
                                channelHandle = handle.ifEmpty { "@channel_${channelId.takeLast(6)}" },
                                avatarUrl = "",
                                subscriberCount = "0",
                                videoCount = "0",
                                defaultCategory = newChannelCategoryInput,
                                defaultLanguage = newChannelLanguageInput,
                                autoPilotEnabled = newChannelAutoPilotInput,
                                isActiveProfile = channelAccounts.isEmpty()
                            )
                            ChannelProfileManager.saveChannelAccount(context, entity)
                            showAddChannelDialog = false
                            Toast.makeText(context, "Added YouTube channel: $title ($newChannelCategoryInput) 🚀", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save Channel", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChannelDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Edit Channel Category Dialog ──
    editingCategoryChannel?.let { targetChannel ->
        val categoriesList = listOf(
            "Technology", "AI & Futuristic", "Horror & Thriller", "Mystery & Crime",
            "History & Culture", "Motivation & Success", "Science & Facts",
            "Business & Money", "Gaming", "Entertainment & Cinema", "Health & Fitness", "Anime & Manga"
        )
        AlertDialog(
            onDismissRequest = { editingCategoryChannel = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Change Channel Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select primary niche for ${targetChannel.channelTitle}:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categoriesList.forEach { cat ->
                            val selected = cat == targetChannel.defaultCategory
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            val updated = targetChannel.copy(defaultCategory = cat)
                                            ChannelProfileManager.saveChannelAccount(context, updated)
                                            if (targetChannel.isActiveProfile) {
                                                selectedCategory = cat
                                            }
                                            editingCategoryChannel = null
                                            Toast.makeText(context, "${targetChannel.channelTitle} category updated to $cat 🎯", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                color = if (selected) ElectricViolet.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ElectricViolet else DarkBorder)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) ElectricViolet else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingCategoryChannel = null }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    GlitchCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = label, color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, subtitle: String, accentColor: Color, modifier: Modifier = Modifier) {
    GlitchCard(modifier = modifier) {
        Column {
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = TextPrimary)
        }
    }
}

@Composable
fun SimpleLineChart(data: List<Float>, lineColor: Color, modifier: Modifier = Modifier) {
    if (data.isEmpty()) {
        Text("No data points available", color = TextSecondary, fontSize = 11.sp)
        return
    }
    val maxVal = data.maxOrNull() ?: 100f
    val minVal = data.minOrNull() ?: 0f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Peak: ${maxVal.toLong()}",
                fontSize = 10.sp,
                color = lineColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Min: ${minVal.toLong()}",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)

            // Draw subtle baseline
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, height - (height * 0.1f)),
                end = Offset(width, height - (height * 0.1f)),
                strokeWidth = 1.dp.toPx()
            )

            val path = Path()
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - minVal) / range) * (height * 0.75f) - (height * 0.1f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Draw line graph
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Draw points
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - minVal) / range) * (height * 0.75f) - (height * 0.1f)
                drawCircle(
                    color = lineColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
    }
}
