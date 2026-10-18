package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.data.local.AppSettings
import com.example.data.local.ProjectEntity
import com.example.data.model.*
import com.example.data.repository.Resource
import com.example.data.youtube.YouTubeUploader
import com.example.pipeline.dom.DomAnimation10ScenePackage
import com.example.util.GoogleSignInHelper
import com.example.pipeline.VideoRequest
import com.example.worker.AutomationSchedulerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val isServerOnline: Boolean = false,
    val serverVersion: String = "",
    val activeJob: JobStatusResponse? = null,
    val recentProjects: List<ProjectSummary> = emptyList(),
    val totalVideosGenerated: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CreateJobUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdJobId: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MainApplication
    private val repository = app.videoRepository
    private val settingsManager = app.settingsManager

    // Flows
    val settingsFlow: StateFlow<AppSettings> = settingsManager.settingsFlow

    private val _agentSettingsFlow = MutableStateFlow(GlitchAgentSettings())
    val agentSettingsFlow: StateFlow<GlitchAgentSettings> = _agentSettingsFlow.asStateFlow()

    private val _isAgentSettingsLoading = MutableStateFlow(false)
    val isAgentSettingsLoading: StateFlow<Boolean> = _isAgentSettingsLoading.asStateFlow()

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _createJobState = MutableStateFlow(CreateJobUiState())
    val createJobState: StateFlow<CreateJobUiState> = _createJobState.asStateFlow()

    private val _currentJobProgress = MutableStateFlow<JobStatusResponse?>(null)
    val currentJobProgress: StateFlow<JobStatusResponse?> = _currentJobProgress.asStateFlow()

    private val _libraryProjects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val libraryProjects: StateFlow<List<ProjectSummary>> = _libraryProjects.asStateFlow()

    private val _currentProjectDetails = MutableStateFlow<ProjectDetailsResponse?>(null)
    val currentProjectDetails: StateFlow<ProjectDetailsResponse?> = _currentProjectDetails.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs: StateFlow<List<String>> = _serverLogs.asStateFlow()

    val networkMonitor = com.example.util.NetworkMonitor(app)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val isPipelineGenerating: StateFlow<Boolean> = com.example.pipeline.PipelineExecutionCoordinator.isGenerating
    val activePipelineInfo = com.example.pipeline.PipelineExecutionCoordinator.activeJobInfo
    val livePipelineProgress: StateFlow<com.example.notification.LivePipelineProgress> = com.example.notification.PipelineNotificationManager.liveProgressFlow

    private val _appUpdateData = MutableStateFlow<com.example.util.AppUpdateData?>(null)
    val appUpdateData: StateFlow<com.example.util.AppUpdateData?> = _appUpdateData.asStateFlow()

    private val _isUpdateDismissed = MutableStateFlow(false)
    val isUpdateDismissed: StateFlow<Boolean> = _isUpdateDismissed.asStateFlow()

    private var pollingJob: Job? = null
    private var activeNativeJob: Job? = null

    init {
        checkServerHealth()
        loadProjects()
        observeLocalDatabase()
        observeAppUpdates()
    }

    fun dismissUpdateDialog() {
        _isUpdateDismissed.value = true
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            val update = com.example.util.FirestoreService.fetchAppUpdateDocument(app)
            _appUpdateData.value = update
            if (update?.shouldShowAlert == true) {
                val current = settingsManager.settingsFlow.value
                if (current.autoPublishEnabled) {
                    settingsManager.updateSettings(current.copy(autoPublishEnabled = false))
                    AutomationSchedulerManager.cancelRecurringAutomation(app)
                }
            }
        }
    }

    private fun observeAppUpdates() {
        // 1. Realtime listener flow
        viewModelScope.launch {
            com.example.util.FirestoreService.listenToAppUpdateFlow(app).collect { update ->
                if (update != null) {
                    _appUpdateData.value = update
                    if (update.shouldShowAlert) {
                        val current = settingsManager.settingsFlow.value
                        if (current.autoPublishEnabled) {
                            settingsManager.updateSettings(current.copy(autoPublishEnabled = false))
                            AutomationSchedulerManager.cancelRecurringAutomation(app)
                        }
                    }
                }
            }
        }

        // 2. Continuous 5-second polling ticker for instant sync
        viewModelScope.launch {
            while (isActive) {
                try {
                    val update = com.example.util.FirestoreService.fetchAppUpdateDocument(app)
                    if (update != null) {
                        _appUpdateData.value = update
                        if (update.shouldShowAlert) {
                            val current = settingsManager.settingsFlow.value
                            if (current.autoPublishEnabled) {
                                settingsManager.updateSettings(current.copy(autoPublishEnabled = false))
                                AutomationSchedulerManager.cancelRecurringAutomation(app)
                            }
                        }
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(5000) // Re-checks every 5 seconds
            }
        }
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            repository.localProjectsFlow.collect { localList ->
                if (_libraryProjects.value.isEmpty()) {
                    _libraryProjects.value = localList
                }
                _dashboardState.update {
                    it.copy(
                        recentProjects = localList.take(5),
                        totalVideosGenerated = localList.size
                    )
                }
            }
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true) }
            when (val res = repository.checkHealth()) {
                is Resource.Success -> {
                    _dashboardState.update {
                        it.copy(
                            isServerOnline = true,
                            serverVersion = res.data.version,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _dashboardState.update {
                        it.copy(
                            isServerOnline = false,
                            isLoading = false,
                            errorMessage = res.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            when (val res = repository.fetchAndSyncProjects()) {
                is Resource.Success -> {
                    _libraryProjects.value = res.data
                    _dashboardState.update {
                        it.copy(
                            recentProjects = res.data.take(5),
                            totalVideosGenerated = res.data.size
                        )
                    }
                }
                is Resource.Error -> {
                    // Handled gracefully by local fallback
                }
                else -> {}
            }
        }
    }

    suspend fun saveGeneratedDomVideo(
        packageResult: DomAnimation10ScenePackage,
        topic: String,
        language: String,
        category: String,
        voiceName: String,
        aspectRatio: String,
        showCaptions: Boolean
    ) = withContext(Dispatchers.IO) {
        require(topic.isNotBlank()) { "The project topic cannot be blank." }
        val videoFile = packageResult.finalRenderedVideoFile
            ?.takeIf { it.isFile && it.length() > 1_000L }
            ?: throw IllegalStateException("The rendered video file is unavailable; the project was not saved.")
        val scenes = packageResult.scenes.sortedBy { it.sceneNumber }
        require(scenes.map { it.sceneNumber } == (1..10).toList()) {
            "A completed 10-scene project with ordered scene numbers is required for library storage."
        }
        require(scenes.sumOf { it.script.durationSeconds } == packageResult.totalDurationSeconds) {
            "The saved scene durations do not match the rendered project duration."
        }
        require(scenes.all { it.script.narration.isNotBlank() && it.script.visualPrompt.isNotBlank() }) {
            "The completed project is missing scene narration or visual direction."
        }

        var startSecond = 0
        val promptsJson = org.json.JSONArray()
        scenes.forEach { scene ->
            val endSecond = startSecond + scene.script.durationSeconds
            promptsJson.put(
                org.json.JSONObject().apply {
                    put("index", scene.sceneNumber)
                    put("startSec", startSecond.toDouble())
                    put("endSec", endSecond.toDouble())
                    put("purpose", scene.script.sceneTitle)
                    put("prompt", scene.script.visualPrompt)
                    put("narration", scene.script.narration)
                    put("cameraMotion", scene.script.cameraMotion)
                    put("audioSfx", scene.script.audioSfx)
                }
            )
            startSecond = endSecond
        }
        val narration = scenes.joinToString(" ") { it.script.narration }
        val productionSettings = org.json.JSONObject().apply {
            put("category", category)
            put("language", language)
            put("voice", voiceName)
            put("aspectRatio", aspectRatio)
            put("captions", showCaptions)
            put("durationSeconds", packageResult.totalDurationSeconds)
        }
        val storyJson = org.json.JSONObject().apply {
            put("title", packageResult.title)
            put("topic", topic)
            put("summary100", packageResult.theme)
            put("description500", packageResult.moral)
            put("story", packageResult.theme)
            put("narration", narration)
            put("selectedTitle", packageResult.title)
            put("productionSettings", productionSettings)
        }.toString()
        val firstFrame = File(videoFile.parentFile, "scene_1_frame.jpg")
        val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val entity = ProjectEntity(
            id = packageResult.projectId,
            title = packageResult.title,
            topic = topic,
            language = language,
            category = category,
            duration = packageResult.totalDurationSeconds,
            style = "HTML/CSS/JavaScript animation",
            aspectRatio = aspectRatio,
            createdAt = createdAt,
            status = "completed",
            videoUrl = "file://${videoFile.absolutePath}",
            thumbnailUrl = if (firstFrame.isFile) "file://${firstFrame.absolutePath}" else "",
            storyJson = storyJson,
            promptsJson = promptsJson.toString(),
            pipelineStep = "DOM_VIDEO_COMPLETED"
        )
        repository.saveGeneratedDomProject(entity)
        val summary = entity.toProjectSummary()
        withContext(Dispatchers.Main) {
            _libraryProjects.update { current ->
                listOf(summary) + current.filterNot { it.id == summary.id }
            }
            _dashboardState.update { state ->
                val recent = listOf(summary) + state.recentProjects.filterNot { it.id == summary.id }
                state.copy(
                    recentProjects = recent.take(5),
                    totalVideosGenerated = maxOf(state.totalVideosGenerated, _libraryProjects.value.size)
                )
            }
        }
        summary
    }

    fun createVideo(request: CreateJobRequest, onNavigateToProgress: (String) -> Unit) {
        viewModelScope.launch {
            if (request.topic.isBlank()) {
                _createJobState.value = CreateJobUiState(
                    error = "Enter a topic before starting video generation."
                )
                return@launch
            }
            _createJobState.value = CreateJobUiState(isSubmitting = true)
            val currentMode = settingsManager.settingsFlow.value.engineMode

            if (currentMode == "Smart Auto" || currentMode == "On-Device Studio Engine") {
                val localJobId = "local_" + System.currentTimeMillis()
                _createJobState.value = CreateJobUiState(isSubmitting = false, createdJobId = localJobId)
                onNavigateToProgress(localJobId)
                startNativePipelineExecution(localJobId, request)
                return@launch
            }

            when (val res = repository.createJob(request)) {
                is Resource.Success -> {
                    val jobId = res.data.id
                    _createJobState.value = CreateJobUiState(isSubmitting = false, createdJobId = jobId)
                    onNavigateToProgress(jobId)
                    startPollingJob(jobId)
                }
                is Resource.Error -> {
                    val localJobId = "local_" + System.currentTimeMillis()
                    _createJobState.value = CreateJobUiState(isSubmitting = false, createdJobId = localJobId)
                    onNavigateToProgress(localJobId)
                    startNativePipelineExecution(localJobId, request)
                }
                else -> {}
            }
        }
    }

    private fun startNativePipelineExecution(jobId: String, request: CreateJobRequest) {
        activeNativeJob?.cancel()
        activeNativeJob = viewModelScope.launch {
            val lockAcquired = com.example.pipeline.PipelineExecutionCoordinator.tryAcquireExecution(
                jobId = jobId,
                title = request.topic,
                isWorkerTask = false
            )

            if (!lockAcquired) {
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    progress = 0,
                    step = "Another video generation is already active (${com.example.pipeline.PipelineExecutionCoordinator.getActiveJobId()}). Please wait for it to complete.",
                    error = JobError(message = "Concurrent generation blocked. Only one video pipeline can run at a time.")
                )
                return@launch
            }

            com.example.pipeline.PipelineExecutionCoordinator.registerJobHandle(this.coroutineContext[Job]!!)

            _currentJobProgress.value = JobStatusResponse(
                id = jobId,
                status = "processing",
                progress = 5,
                step = "Step 1/8: Script & Viral Storyboard via AI Engine"
            )

            try {
                val videoReq = VideoRequest(
                    topic = request.topic,
                    language = request.language,
                    category = request.category,
                    targetDurationSec = request.durationSec,
                    style = request.style,
                    aspect = request.aspectRatio,
                    platform = request.platform,
                    audience = request.audience,
                    voiceName = request.voiceName,
                    imageDurationSec = request.imageDurationSec,
                    showCaptions = request.showCaptions,
                    captionStyle = request.captionStyle,
                    captionHighlightColor = request.captionHighlightColor,
                    captionFontFamily = request.captionFontFamily,
                    captionStrokeWidth = request.captionStrokeWidth,
                    captionGlow = request.captionGlow,
                    captionWordsPerChunk = request.captionWordsPerChunk,
                    captionUppercase = request.captionUppercase,
                    bgmEnabled = request.bgmEnabled,
                    bgmTrackId = request.bgmTrackId,
                    bgmVolume = request.bgmVolume,
                    autoDucking = request.autoDucking,
                    visualRenderMode = settingsManager.settingsFlow.value.visualRenderMode,
                    htmlTheme = settingsManager.settingsFlow.value.htmlTheme,
                    customNarration = request.customNarration,
                    customHook = request.customHook
                )

                val result = repository.runNativePipeline(videoReq) { prog, stepMsg ->
                    _currentJobProgress.value = JobStatusResponse(
                        id = jobId,
                        status = "processing",
                        progress = prog,
                        step = stepMsg
                    )
                    com.example.notification.PipelineNotificationManager.showProgressNotification(
                        context = app,
                        title = "GLITCH AI: Generating Video",
                        statusMessage = "[$prog%] $stepMsg",
                        progressPercent = prog
                    )
                }

                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "completed",
                    progress = 100,
                    step = "Video completed",
                    output = JobOutput(
                        videoUrl = "file://${result.video.absolutePath}",
                        thumbnailUrl = "file://${result.thumbnail.absolutePath}",
                        title = result.metadata.title,
                        visualCount = result.visualCount,
                        audioDurationSec = result.audioDuration
                    )
                )

                com.example.notification.PipelineNotificationManager.showCompletionNotification(
                    context = app,
                    title = result.metadata.title,
                    youtubeUrl = null,
                    nextRunTimeText = null
                )

                loadProjects()
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.i("MainViewModel", "Pipeline execution cancelled by user: $jobId")
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    progress = 0,
                    step = "Cancelled by user"
                )
            } catch (e: Exception) {
                if (settingsManager.settingsFlow.value.vpnAutoSwitchOnFailure) {
                    try {
                        com.example.vpn.VpnGateManager.switchToNextServer(app)
                    } catch (_: Exception) {}
                }
                com.example.notification.PipelineNotificationManager.showErrorNotification(
                    context = app,
                    title = "Video Generation Failed",
                    errorMessage = e.message ?: "Unknown error"
                )
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    progress = 0,
                    step = "Pipeline Execution Failed",
                    error = JobError(message = e.localizedMessage ?: "Unknown pipeline error")
                )
            } finally {
                com.example.pipeline.PipelineExecutionCoordinator.releaseExecution(jobId)
            }
        }
    }

    fun startPollingJob(jobId: String) {
        if (jobId.startsWith("local_") || jobId.startsWith("autopilot_") || jobId.startsWith("worker_") || jobId.isBlank()) {
            pollingJob?.cancel()
            pollingJob = viewModelScope.launch {
                livePipelineProgress.collect { live ->
                    _currentJobProgress.value = JobStatusResponse(
                        id = if (live.jobId.isNotBlank()) live.jobId else jobId,
                        status = if (live.isCompleted) "completed" else if (live.error != null) "failed" else "processing",
                        progress = live.progressPercent,
                        step = live.statusMessage.ifBlank { live.stage }.ifBlank { "Processing video..." }
                    )
                    if (live.isCompleted || live.error != null) {
                        loadProjects()
                    }
                }
            }
            return
        }

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            try {
                while (true) {
                    when (val res = repository.getJobStatus(jobId)) {
                        is Resource.Success -> {
                            _currentJobProgress.value = res.data
                            if (res.data.status == "completed" || res.data.status == "failed") {
                                loadProjects()
                                break
                            }
                        }
                        is Resource.Error -> {
                            // ignore and retry
                        }
                        else -> {}
                    }
                    delay(2000)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected on cancellation
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Error in startPollingJob for $jobId: ${e.message}")
            }
        }
    }

    /**
     * Updates unified category across all three surfaces:
     * 1. Settings (AppSettings & GlitchAgentSettings)
     * 2. YouTube Studio Channel Profile
     * 3. Main Screen Auto-Pilot & Scheduler
     */
    fun updateUnifiedCategory(newCategory: String, categoryId: String = "") {
        viewModelScope.launch {
            val effCatId = if (categoryId.isNotBlank()) categoryId else com.example.util.UnifiedCategories.getCategoryId(newCategory)
            val currentApp = settingsManager.settingsFlow.value
            val updatedApp = currentApp.copy(
                autoCategory = newCategory,
                defaultCategory = newCategory,
                defaultCategoryId = effCatId
            )
            settingsManager.updateSettings(updatedApp)

            val currentAgent = _agentSettingsFlow.value
            val updatedAgent = currentAgent.copy(
                general = currentAgent.general.copy(category = newCategory)
            )
            _agentSettingsFlow.value = updatedAgent

            try {
                com.example.util.FirestoreService.saveAgentSettings(app, updatedAgent)
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Firestore sync failed for category: ${e.message}")
            }

            if (updatedApp.autoPublishEnabled) {
                com.example.worker.AutomationSchedulerManager.scheduleRecurringAutomation(
                    context = app,
                    intervalHours = updatedApp.scheduleIntervalHours,
                    delaySeconds = updatedApp.scheduleDelaySeconds,
                    category = newCategory,
                    language = updatedApp.defaultLanguage,
                    autoUpload = true
                )
            }
        }
    }

    fun cancelActiveJob(jobId: String) {
        viewModelScope.launch {
            com.example.pipeline.PipelineExecutionCoordinator.cancelActiveJob(jobId)
            activeNativeJob?.cancel()
            activeNativeJob = null

            if (jobId.startsWith("local_") || jobId.startsWith("autopilot_")) {
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    step = "Cancelled by user"
                )
                return@launch
            }
            repository.cancelJob(jobId)
            pollingJob?.cancel()
            _currentJobProgress.value = null
        }
    }

    fun loadProjectDetails(projectId: String) {
        viewModelScope.launch {
            _currentProjectDetails.value = null
            when (val res = repository.getProjectDetails(projectId)) {
                is Resource.Success -> {
                    _currentProjectDetails.value = res.data
                }
                is Resource.Error -> {}
                else -> {}
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            loadProjects()
        }
    }

    fun uploadProjectToYouTube(
        project: ProjectSummary,
        onStatus: (Int, String) -> Unit
    ) {
        viewModelScope.launch {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(app)
            val savedUser = com.example.util.GoogleSignInHelper.currentUser.value
            val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

            if (account == null && savedUser == null && authUser == null) {
                onStatus(-1, "Please connect your YouTube/Google account first in Channel Management tab")
                return@launch
            }

            val cleanPath = project.video.removePrefix("file://")
            var videoFile = File(cleanPath)
            if (!videoFile.exists() || videoFile.length() <= 0) {
                val fileName = cleanPath.substringAfterLast("/")
                val inFiles = File(app.filesDir, fileName)
                val inCache = File(app.cacheDir, fileName)
                if (inFiles.exists() && inFiles.length() > 0) {
                    videoFile = inFiles
                } else if (inCache.exists() && inCache.length() > 0) {
                    videoFile = inCache
                }
            }

            onStatus(5, "Connecting to YouTube channel...")

            try {
                val result = com.example.util.GoogleSignInHelper.uploadVideoToYouTube(
                    context = app,
                    videoFile = videoFile,
                    title = project.title.ifBlank { project.topic },
                    description = "${project.topic}\n\nAutomated with GLITCH OS AI Video Studio. #Shorts #AI #Trending",
                    tags = listOf(project.category, "Shorts", "AI", "GlitchOS"),
                    privacyStatus = "public",
                    categoryId = settingsManager.settingsFlow.value.defaultCategoryId.ifBlank { "24" },
                    durationSec = if (project.duration > 0) project.duration.toDouble() else 60.0,
                    onProgress = { msg, fraction ->
                        val percent = (fraction * 100).toInt().coerceIn(0, 100)
                        onStatus(percent, msg)
                    }
                )

                result.onSuccess { url ->
                    onStatus(100, "Published successfully to YouTube: $url")
                }.onFailure { e ->
                    onStatus(-1, "Publish failed: ${e.message}")
                }
            } catch (e: Exception) {
                onStatus(-1, "Upload exception: ${e.message}")
            }
        }
    }

    fun updateScheduler(
        enabled: Boolean,
        intervalHours: Int,
        category: String,
        delaySeconds: Long = 0L,
        onAlert: ((String) -> Unit)? = null
    ) {
        if (enabled) {
            val hasYtScope = GoogleSignInHelper.hasValidYouTubePermissions(app)
            val hasGoogleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(app) != null
            val hasNotifPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasGoogleAccount || !hasYtScope || !hasNotifPerm) {
                val reason = when {
                    !hasGoogleAccount -> "Google Account Sign-In required."
                    !hasYtScope -> "YouTube channel upload scope missing."
                    !hasNotifPerm -> "Notification permission required."
                    else -> "Permissions missing."
                }
                onAlert?.invoke("⚠️ Auto-Pilot Blocked: $reason")
                val current = settingsManager.settingsFlow.value
                val updated = current.copy(autoPublishEnabled = false)
                settingsManager.updateSettings(updated)
                AutomationSchedulerManager.cancelRecurringAutomation(app)
                return
            }
        }

        val current = settingsManager.settingsFlow.value
        val effectiveDelaySec = if (delaySeconds > 0L) {
            delaySeconds
        } else if (current.scheduleDelaySeconds > 0L) {
            current.scheduleDelaySeconds
        } else {
            (intervalHours.coerceAtLeast(1) * 3600L)
        }

        val updated = current.copy(
            autoPublishEnabled = enabled,
            scheduleIntervalHours = intervalHours,
            scheduleDelaySeconds = effectiveDelaySec,
            smartTimingMode = "FIXED_INTERVAL", // Honor user's selected interval/delay
            autoCategory = category
        )
        settingsManager.updateSettings(updated)

        if (enabled) {
            onAlert?.invoke("🚀 Auto-pilot activated! Triggering first video generation...")
            val latestProject = _libraryProjects.value.firstOrNull()
            val lastVideoTs = AutomationSchedulerManager.parseTimestamp(latestProject?.createdAt)
            AutomationSchedulerManager.scheduleRecurringAutomation(
                context = app,
                intervalHours = intervalHours,
                delaySeconds = effectiveDelaySec,
                category = category,
                language = current.defaultLanguage,
                autoUpload = true,
                lastVideoTimestamp = lastVideoTs
            )
            triggerAutopilotFirstRun(category, current.defaultLanguage, onAlert = onAlert)
        } else {
            AutomationSchedulerManager.cancelRecurringAutomation(app)
            onAlert?.invoke("Auto-pilot paused")
        }
    }

    fun triggerAutoBotNow(
        category: String? = null,
        language: String? = null,
        onNavigateToProgress: ((String) -> Unit)? = null,
        onAlert: ((String) -> Unit)? = null
    ) {
        val current = settingsManager.settingsFlow.value
        val targetCat = category ?: current.autoCategory
        val targetLang = language ?: current.defaultLanguage
        onAlert?.invoke("🤖 AI Auto-Bot activated! Generating viral $targetCat short video & auto-uploading...")
        triggerAutopilotFirstRun(targetCat, targetLang, onNavigateToProgress, onAlert)
    }

    private fun triggerAutopilotFirstRun(
        category: String,
        language: String,
        onNavigateToProgress: ((String) -> Unit)? = null,
        onAlert: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val topic = GoogleSignInHelper.fetchTrendingYouTubeVideos(app)
                .getOrElse { error ->
                    android.util.Log.e("MainViewModel", "Live topic discovery failed; automation was not started.", error)
                    withContext(Dispatchers.Main) {
                        onAlert?.invoke("Auto-pilot stopped: couldn't load live YouTube topics (${error.localizedMessage ?: "unknown error"}).")
                    }
                    return@launch
                }
                .firstOrNull()
                ?.title
                ?.takeIf { it.isNotBlank() }
            if (topic == null) {
                withContext(Dispatchers.Main) {
                    onAlert?.invoke("Auto-pilot stopped: YouTube returned no popular videos.")
                }
                return@launch
            }
            val localJobId = "autopilot_" + System.currentTimeMillis()
            android.util.Log.d("MainViewModel", "Autopilot activated: triggering immediate video generation: $localJobId for category $category")

            val lockAcquired = com.example.pipeline.PipelineExecutionCoordinator.tryAcquireExecution(
                jobId = localJobId,
                title = topic,
                isWorkerTask = false
            )

            if (!lockAcquired) {
                android.util.Log.w("MainViewModel", "Another video pipeline is already running. Skipping autopilot run to avoid duplication.")
                return@launch
            }

            withContext(Dispatchers.Main) {
                onNavigateToProgress?.invoke(localJobId)
            }

            try {
                com.example.notification.PipelineNotificationManager.showProgressNotification(
                    context = app,
                    title = "🚀 GLITCH Autopilot: Generating Video",
                    statusMessage = "Starting pipeline for $category...",
                    progressPercent = 5
                )

                val currentSet = settingsManager.settingsFlow.value
                val videoReq = VideoRequest(
                    topic = topic,
                    language = language,
                    category = category,
                    targetDurationSec = currentSet.defaultDuration,
                    style = currentSet.defaultStyle,
                    aspect = currentSet.defaultAspect,
                    voiceName = if (language.equals("Hindi", ignoreCase = true)) "hi-IN-MadhurNeural" else "en-US-GuyNeural",
                    imageDurationSec = currentSet.imageDurationSec,
                    showCaptions = currentSet.showCaptions,
                    bgmEnabled = true,
                    bgmTrackId = com.example.util.FreeMusicManager.getCategoryDefaultTrackId(category),
                    bgmVolume = 0.18f,
                    autoDucking = true,
                    visualRenderMode = currentSet.visualRenderMode,
                    htmlTheme = currentSet.htmlTheme
                )

                val result = repository.runNativePipeline(videoReq) { prog, stepMsg ->
                    android.util.Log.d("MainViewModel", "Autopilot Run [$prog%] $stepMsg")
                    com.example.notification.PipelineNotificationManager.showProgressNotification(
                        context = app,
                        title = "🚀 GLITCH Autopilot: Generating Video",
                        statusMessage = "[$prog%] $stepMsg",
                        progressPercent = prog
                    )
                }

                // Upload to YouTube if permissions are present
                val db = com.example.data.local.VideoDatabase.getDatabase(app)
                val user = db.authDao().getUser()
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(app)
                val token = if (account != null) {
                    com.example.util.GoogleSignInHelper.getOAuthAccessToken(app, account) ?: user?.oauthToken
                } else {
                    user?.oauthToken
                }

                var youtubeUrl: String? = null
                if (!token.isNullOrBlank()) {
                    com.example.notification.PipelineNotificationManager.showProgressNotification(
                        context = app,
                        title = "GLITCH Autopilot: Uploading Video",
                        statusMessage = "Uploading ${result.metadata.title} to YouTube...",
                        progressPercent = 95
                    )
                    val uploader = com.example.data.youtube.YouTubeUploader(app)
                    try {
                        youtubeUrl = uploader.uploadVideoResumable(
                            videoFile = result.video,
                            title = result.metadata.title,
                            description = "${result.metadata.description}\n\n#Shorts ${result.metadata.hashtags.joinToString(" ")}",
                            tags = result.metadata.tags,
                            accessToken = token,
                            privacyStatus = "public",
                            categoryId = settingsManager.settingsFlow.value.defaultCategoryId.ifBlank { "28" },
                            onProgress = { p: Int, msg: String ->
                                android.util.Log.d("MainViewModel", "Autopilot Upload [$p%] $msg")
                            }
                        )
                        if (!youtubeUrl.isNullOrBlank()) {
                            com.example.util.FirestoreService.saveYouTubeUpload(
                                context = app,
                                videoTitle = result.metadata.title,
                                description = result.metadata.description,
                                youtubeUrl = youtubeUrl,
                                videoId = youtubeUrl.substringAfterLast("/"),
                                durationSec = result.audioDuration
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "YouTube Upload failed in autopilot first run", e)
                    }
                }

                val now = System.currentTimeMillis()
                val nextRun = AutomationSchedulerManager.getEffectiveNextPublishTimestamp(
                    lastVideoTimestamp = now,
                    lastAutoPublishTimestamp = now,
                    intervalHours = settingsManager.settingsFlow.value.scheduleIntervalHours
                )
                settingsManager.updateSettings(
                    settingsManager.settingsFlow.value.copy(
                        lastAutoPublishTimestamp = now,
                        nextAutoPublishTimestamp = nextRun
                    )
                )

                val nextRunFormatted = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(nextRun))
                com.example.notification.PipelineNotificationManager.showCompletionNotification(
                    context = app,
                    title = result.metadata.title,
                    youtubeUrl = youtubeUrl,
                    nextRunTimeText = nextRunFormatted
                )

                loadProjects()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Autopilot first run execution error", e)
                com.example.notification.PipelineNotificationManager.showErrorNotification(
                    context = app,
                    title = "GLITCH Autopilot Generation Failed",
                    errorMessage = e.message ?: "Unknown error"
                )
            } finally {
                com.example.pipeline.PipelineExecutionCoordinator.releaseExecution(localJobId)
            }
        }
    }

    fun loadLogs() {
        viewModelScope.launch {
            when (val res = repository.getLogs()) {
                is Resource.Success -> {
                    _serverLogs.value = res.data
                }
                is Resource.Error -> {}
                else -> {}
            }
        }
    }

    suspend fun publishProjectToYouTubeDirect(
        projectId: String,
        customTitle: String? = null,
        customDescription: String? = null,
        customTags: List<String>? = null,
        customThumbnailFile: File? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val db = com.example.data.local.VideoDatabase.getDatabase(app)
            val user = db.authDao().getUser()
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(app)
            val token = if (account != null) {
                com.example.util.GoogleSignInHelper.getOAuthAccessToken(app, account) ?: user?.oauthToken
            } else {
                user?.oauthToken
            } ?: return@withContext Result.failure(Exception("YouTube account not authenticated or token missing"))

            val project = _libraryProjects.value.find { it.id == projectId }
                ?: return@withContext Result.failure(Exception("Project not found in library"))

            val videoFile = File(project.video)
            if (!videoFile.exists() || videoFile.length() <= 0L) {
                return@withContext Result.failure(Exception("Video file not found at: ${project.video}"))
            }

            val uploader = YouTubeUploader(app)
            val title = customTitle?.ifBlank { null } ?: project.title.ifBlank { "Viral Short #${project.category}" }
            val description = customDescription?.ifBlank { null } ?: "${project.topic}\n\n#Shorts #${project.category.replace(" ", "")}"
            val tags = customTags?.filter { it.isNotBlank() }?.ifEmpty { null } ?: listOf(project.category, "Shorts", "Viral", "Trending")

            val youtubeUrl = uploader.uploadVideoResumable(
                videoFile = videoFile,
                title = title,
                description = description,
                tags = tags,
                accessToken = token,
                privacyStatus = "public",
                categoryId = settingsManager.settingsFlow.value.defaultCategoryId.ifBlank { "28" },
                onProgress = { p, msg ->
                    android.util.Log.d("MainViewModel", "Publishing direct [$p%] $msg")
                }
            )

            if (!youtubeUrl.isNullOrBlank()) {
                val videoId = youtubeUrl.substringAfterLast("/")
                com.example.util.FirestoreService.saveYouTubeUpload(
                    context = app,
                    videoTitle = title,
                    description = description,
                    youtubeUrl = youtubeUrl,
                    videoId = videoId,
                    durationSec = project.duration.toDouble()
                )

                // Auto-Add into Category Playlist
                try {
                    com.example.util.AutoPlaylistManager.autoAddVideoToCategoryPlaylist(
                        context = app,
                        category = project.category,
                        videoId = videoId
                    )
                } catch (pe: Exception) {
                    android.util.Log.w("MainViewModel", "Playlist addition warning: ${pe.message}")
                }

                Result.success(youtubeUrl)
            } else {
                Result.failure(Exception("Upload returned empty response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun publishProjectDirectWithCallbacks(
        projectId: String,
        customTitle: String? = null,
        customDescription: String? = null,
        customTags: List<String>? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = publishProjectToYouTubeDirect(projectId, customTitle, customDescription, customTags)
            if (res.isSuccess) {
                onSuccess(res.getOrNull() ?: "")
            } else {
                onError(res.exceptionOrNull()?.message ?: "Publishing failed")
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsManager.updateSettings(newSettings)
    }

    fun updateAppSettings(newSettings: AppSettings) {
        settingsManager.updateSettings(newSettings)
    }

    fun loadAgentSettings() {
        viewModelScope.launch {
            _isAgentSettingsLoading.value = true
            try {
                val cloudSettings = com.example.util.FirestoreService.fetchAgentSettingsFromCloud(app)
                if (cloudSettings != null) {
                    _agentSettingsFlow.value = cloudSettings
                    val delaySec = cloudSettings.schedulingAndAutomation.delayBetweenVideosSeconds
                    val calculatedHours = if (delaySec >= 3600) (delaySec / 3600).coerceAtLeast(1) else 1
                    val currentApp = settingsManager.settingsFlow.value
                    if (currentApp.scheduleIntervalHours != calculatedHours) {
                        settingsManager.updateSettings(currentApp.copy(scheduleIntervalHours = calculatedHours))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("MainViewModel", "Failed loading cloud agent settings: ${e.message}")
            } finally {
                _isAgentSettingsLoading.value = false
            }
        }
    }

    fun saveAgentConfiguration(
        agentSettings: GlitchAgentSettings,
        appSettings: AppSettings,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _agentSettingsFlow.value = agentSettings
            val delaySec = agentSettings.schedulingAndAutomation.delayBetweenVideosSeconds
            val calculatedHours = if (delaySec >= 3600) (delaySec / 3600).coerceAtLeast(1) else 1
            val syncedAppSettings = appSettings.copy(
                autoPublishEnabled = agentSettings.publishing.autoPublish,
                defaultLanguage = agentSettings.languageAndVoice.language,
                defaultCategory = agentSettings.general.category,
                autoCategory = agentSettings.general.category,
                defaultDuration = agentSettings.videoFormat.maxDurationSeconds,
                defaultStyle = agentSettings.contentAndStyle.visualStyle,
                defaultAspect = agentSettings.videoFormat.aspectRatio,
                showCaptions = agentSettings.captionsAndSubtitles.captionsEnabled,
                scheduleIntervalHours = calculatedHours,
                scheduleDelaySeconds = delaySec.toLong(),
                smartTimingMode = "FIXED_INTERVAL"
            )
            settingsManager.updateSettings(syncedAppSettings)

            if (syncedAppSettings.autoPublishEnabled) {
                com.example.worker.AutomationSchedulerManager.scheduleRecurringAutomation(
                    context = app,
                    intervalHours = calculatedHours,
                    delaySeconds = delaySec.toLong(),
                    category = syncedAppSettings.defaultCategory,
                    language = syncedAppSettings.defaultLanguage,
                    autoUpload = true
                )
            }

            val success = com.example.util.FirestoreService.saveAgentSettings(app, agentSettings)
            onComplete(success)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
