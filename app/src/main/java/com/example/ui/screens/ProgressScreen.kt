package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JobStatusResponse
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.components.StepIndicator
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    jobId: String,
    viewModel: MainViewModel,
    onNavigateToPreview: (String, String) -> Unit,
    onNavigateHome: () -> Unit
) {
    val progressState by viewModel.currentJobProgress.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(jobId) {
        viewModel.startPollingJob(jobId)
    }

    val status = progressState?.status ?: "processing"
    val progress = progressState?.progress ?: 5
    val currentStep = progressState?.step ?: "Initializing Studio Pipeline..."
    val isCompleted = status == "completed"
    val isFailed = status == "failed"
    val isPausedOffline = !isOnline || currentStep.contains("PAUSED_OFFLINE") || currentStep.contains("WAITING_FOR_INTERNET")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pipeline Execution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Circular Progress Indicator
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .border(
                            width = 2.dp,
                            color = when {
                                isCompleted -> NeonGreen
                                isFailed -> ErrorRed
                                isPausedOffline -> WarningAmber.copy(alpha = pulseAlpha)
                                else -> CyberCyan.copy(alpha = pulseAlpha)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$progress%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isCompleted -> NeonGreen
                                isFailed -> ErrorRed
                                isPausedOffline -> WarningAmber
                                else -> CyberCyan
                            }
                        )
                        Text(
                            text = when {
                                isCompleted -> "READY"
                                isFailed -> "FAILED"
                                isPausedOffline -> "PAUSED (OFFLINE)"
                                else -> "GENERATING"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPausedOffline) WarningAmber else TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Current Step Box
            item {
                GlitchCard(
                    borderColor = if (isCompleted) NeonGreen.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CURRENT TASK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentStep,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Start
                    )

                    if (progressState?.currentImage != null && (progressState?.totalImages ?: 0) > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (progressState?.currentImage ?: 0).toFloat() / (progressState?.totalImages ?: 1).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyberCyan,
                            trackColor = DarkBorder
                        )
                    }
                }
            }

            // Pipeline 7 Steps Checklist
            item {
                GlitchCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PIPELINE PHASES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val steps = listOf(
                        "Step 1: Script & Viral Storyboard via AI Engine" to (progress >= 10),
                        "Step 2: Neural Voiceover Narration" to (progress >= 20),
                        "Step 3: Duration & Scene Breakdown" to (progress >= 25),
                        "Step 4: AI Visual Scene Prompts" to (progress >= 30),
                        "Step 5: Sequential High-Def Image Generation" to (progress >= 75),
                        "Step 6: Synced Subtitle Captions" to (progress >= 85),
                        "Step 7: Video Rendering & Muxing" to (progress >= 95),
                        "Step 8: Final Validation & Thumbnail" to (progress >= 100)
                    )

                    steps.forEachIndexed { idx, (name, completed) ->
                        val active = !completed && (idx == 0 || steps[idx - 1].second) && !isCompleted && !isFailed
                        StepIndicator(
                            stepName = name,
                            isActive = active,
                            isCompleted = completed,
                            stepNumber = idx + 1
                        )
                        if (idx < steps.size - 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // Actions (View or Cancel)
            item {
                if (isCompleted) {
                    val videoUrl = progressState?.output?.videoUrl?.takeIf { it.isNotBlank() }
                    if (videoUrl != null) {
                        NeonButton(
                            text = "WATCH COMPLETED VIDEO",
                            leadingIcon = Icons.Default.PlayArrow,
                            color = NeonGreen,
                            onClick = {
                                onNavigateToPreview(videoUrl, progressState?.output?.title.orEmpty())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watch_video_button")
                        )
                    } else {
                        Text(
                            text = "Generation completed, but no playable video URL was returned.",
                            color = WarningAmber,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (isFailed) {
                    NeonButton(
                        text = "RETURN HOME",
                        color = ErrorRed,
                        onClick = onNavigateHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("failed_return_home_button")
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            viewModel.cancelActiveJob(jobId)
                            onNavigateHome()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cancel_job_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ErrorRed
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Generation")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
