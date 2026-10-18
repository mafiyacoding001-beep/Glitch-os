package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.util.VideoSeoOptimizer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String, String) -> Unit
) {
    val details by viewModel.currentProjectDetails.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(projectId) {
        viewModel.loadProjectDetails(projectId)
    }

    var selectedTitleIndex by remember { mutableStateOf(0) }
    var isUploadingToYouTube by remember { mutableStateOf(false) }
    var frameCandidates by remember(projectId) {
        mutableStateOf<List<com.example.util.VideoFrameSample>>(emptyList())
    }
    var isAnalyzingFrames by remember(projectId) { mutableStateOf(false) }
    var frameAnalysisError by remember(projectId) { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Video Metadata & Project Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("details_back_button")
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
        if (details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else {
            val proj = details?.project
            val story = details?.story
            val prompts = details?.visualPrompts ?: emptyList()
            val images = details?.images ?: emptyList()

            LaunchedEffect(projectId, proj?.video, proj?.duration) {
                val project = proj ?: return@LaunchedEffect
                val videoFile = File(project.video.removePrefix("file://"))
                frameCandidates = emptyList()
                frameAnalysisError = null
                if (!videoFile.isFile || videoFile.length() == 0L) {
                    frameAnalysisError = "The rendered video file is unavailable for frame analysis."
                    return@LaunchedEffect
                }

                isAnalyzingFrames = true
                try {
                    frameCandidates = VideoSeoOptimizer.extractVideoFrameSamples(
                        videoFile = videoFile,
                        outputDir = File(context.cacheDir, "video_frame_samples/$projectId"),
                        durationSec = project.duration.toDouble()
                    )
                    if (frameCandidates.isEmpty()) {
                        frameAnalysisError = "No frames could be extracted from the rendered video."
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    frameAnalysisError = e.localizedMessage ?: "Frame analysis failed."
                } finally {
                    isAnalyzingFrames = false
                }
            }

            val seoTopic = proj?.topic?.takeIf { it.isNotBlank() }
                ?: proj?.title?.takeIf { it.isNotBlank() }
            val seoSuite = remember(proj, story, seoTopic) {
                val cat = proj?.category ?: "Technology"
                val lang = proj?.language ?: "Hindi"
                seoTopic?.let { topic ->
                    VideoSeoOptimizer.generateDeepVideoSeo(
                        topic = topic,
                        category = cat,
                        language = lang,
                        customSummary = story?.summary100
                    )
                }
            }

            val currentSelectedTitle = seoSuite?.titleVariations
                ?.getOrNull(selectedTitleIndex)
                ?.title
                ?: proj?.title.orEmpty()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Player / Hero Card
                item {
                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VIDEO METADATA DRAFTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = CyberCyan
                            )
                            if (seoSuite != null) {
                                val metadataChecklistScore = VideoSeoOptimizer.calculateMetadataChecklistScore(
                                    topic = seoSuite.topic,
                                    title = currentSelectedTitle,
                                    description = seoSuite.richDescription.fullDescriptionText,
                                    tags = seoSuite.suggestedTags
                                )
                                Surface(
                                    color = NeonGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Local metadata checks: ${metadataChecklistScore}/100 (not a performance prediction)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Add a topic or title to create metadata drafts.",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentSelectedTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${proj?.category} • ${proj?.duration}s • ${proj?.language} • ${proj?.style}",
                            fontSize = 12.sp,
                            color = CyberCyan
                        )

                        if (!proj?.video.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NeonButton(
                                    text = "WATCH PREVIEW",
                                    leadingIcon = Icons.Default.PlayArrow,
                                    onClick = {
                                        onNavigateToPreview(proj?.video ?: "", currentSelectedTitle)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (seoSuite != null) {
                                    Button(
                                        onClick = {
                                            if (isUploadingToYouTube) return@Button
                                            isUploadingToYouTube = true
                                            viewModel.publishProjectDirectWithCallbacks(
                                                projectId = projectId,
                                                customTitle = currentSelectedTitle,
                                                customDescription = seoSuite.richDescription.fullDescriptionText,
                                                customTags = seoSuite.suggestedTags,
                                                onSuccess = { vidId ->
                                                    isUploadingToYouTube = false
                                                    Toast.makeText(context, "Published to YouTube! ID: $vidId", Toast.LENGTH_LONG).show()
                                                },
                                                onError = { err ->
                                                    isUploadingToYouTube = false
                                                    Toast.makeText(context, "Upload note: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        if (isUploadingToYouTube) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Uploading...", fontSize = 12.sp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Direct Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                } else {
                                    Text(
                                        text = "Add a topic or title before uploading.",
                                        fontSize = 10.sp,
                                        color = TextMuted,
                                        modifier = Modifier.weight(1.1f)
                                    )
                                    }
                                }
                            }
                        }
                    }
                }

                story?.productionSettings?.let { settings ->
                    item {
                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SAVED VIDEO AGENT SETTINGS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Voice: ${settings.voice} • Aspect: ${settings.aspectRatio} • Captions: ${if (settings.captions) "On" else "Off"}",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${settings.category} • ${settings.language} • ${settings.durationSeconds}s",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (seoSuite != null) {
                    // 1. Topic-based title drafts
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
                                color = ElectricViolet
                            )
                            Text(
                                text = "${seoSuite.titleVariations.size} Options",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        seoSuite.titleVariations.forEachIndexed { idx, titleVar ->
                            val isSelected = selectedTitleIndex == idx
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedTitleIndex = idx },
                                color = if (isSelected) ElectricViolet.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${titleVar.emoji} ${titleVar.type}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CyberCyan else TextSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = titleVar.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
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
                                                clipboardManager.setText(AnnotatedString(titleVar.title))
                                                Toast.makeText(context, "Copied title to clipboard!", Toast.LENGTH_SHORT).show()
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

                    // 2. Video description draft
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
                                color = CyberCyan
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(seoSuite.richDescription.fullDescriptionText))
                                    Toast.makeText(context, "Copied full description with tags to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Description",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
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
                                    text = seoSuite.richDescription.summary,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = seoSuite.richDescription.questionPrompt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonYellow
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = seoSuite.richDescription.hashtags.joinToString(" "),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }

                    // 3. Topic-based metadata tags
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
                                    text = "${seoSuite.totalTagCharacters} / 500 characters • ${seoSuite.suggestedTags.size} tags",
                                    fontSize = 10.sp,
                                    color = if (seoSuite.totalTagCharacters <= 500) NeonGreen else CoralPink
                                )
                            }
                            IconButton(
                                onClick = {
                                    val commaSeparated = seoSuite.suggestedTags.joinToString(", ")
                                    clipboardManager.setText(AnnotatedString(commaSeparated))
                                    Toast.makeText(context, "Copied ${seoSuite.suggestedTags.size} tags to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Tags",
                                    tint = NeonYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Tags Flow / Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(seoSuite.suggestedTags) { tag ->
                                Surface(
                                    color = DarkSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow.copy(alpha = 0.3f))
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
                } else {
                    item {
                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Metadata drafts and upload are unavailable until this project has a non-blank topic or title.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // 4. Frames sampled from the rendered video
                item {
                    GlitchCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SAMPLED FRAMES • REVIEW ONLY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = when {
                                    isAnalyzingFrames -> "Analyzing rendered video..."
                                    frameCandidates.isEmpty() -> "No frames sampled"
                                    else -> "${frameCandidates.size} frames sampled"
                                },
                                fontSize = 10.sp,
                                color = if (frameCandidates.isNotEmpty()) NeonGreen else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(frameCandidates) { frame ->
                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = 1.dp,
                                            color = DarkSurfaceElevated,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(DarkSurfaceElevated)
                                        .padding(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkBackground)
                                    ) {
                                        val candidateUrl = frame.filePath?.let { "file://$it" }
                                        AsyncImage(
                                            model = candidateUrl,
                                            contentDescription = frame.label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = DarkBackground.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = "${frame.timestampSec.toInt()}s",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyberCyan,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = NeonGreen.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(topStart = 6.dp),
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        ) {
                                            Text(
                                                text = frame.contrastScore?.let { "Contrast heuristic: $it/100" }
                                                    ?: "Contrast not measured",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = frame.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = frame.hookType,
                                        fontSize = 9.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (frameCandidates.isEmpty()) {
                            Text(
                                text = frameAnalysisError
                                    ?: if (isAnalyzingFrames) "Sampling frames from the rendered video."
                                    else "Waiting for the rendered video to become available.",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Generated Images Gallery
                if (images.isNotEmpty()) {
                    item {
                        Text(
                            text = "Generated Visual Scenes (${images.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(images) { img ->
                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceElevated)
                                ) {
                                    AsyncImage(
                                        model = img.url,
                                        contentDescription = "Scene Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(DarkBackground.copy(alpha = 0.7f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Scene #${img.index}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Scene Prompts Timeline
                if (prompts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Scene Prompts Timeline",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    items(prompts) { vp ->
                        GlitchCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scene #${vp.index} (${vp.startSec}s - ${vp.endSec}s)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = vp.purpose,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = vp.prompt,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
