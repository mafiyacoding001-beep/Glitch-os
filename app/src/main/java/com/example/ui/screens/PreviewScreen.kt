package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectSummary
import com.example.ui.components.GlitchCard
import com.example.ui.components.NeonButton
import com.example.ui.components.ThumbnailEditorDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*
import com.example.ui.viewmodels.MainViewModel
import com.example.util.SocialShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    videoUrl: String,
    title: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showThumbnailDialog by remember { mutableStateOf(false) }

    val decodedUrl = remember(videoUrl) {
        try {
            java.net.URLDecoder.decode(videoUrl, "UTF-8")
        } catch (_: Exception) {
            videoUrl
        }
    }

    val decodedTitle = remember(title) {
        try {
            java.net.URLDecoder.decode(title, "UTF-8")
        } catch (_: Exception) {
            title
        }
    }

    if (showThumbnailDialog) {
        ThumbnailEditorDialog(
            initialTitle = decodedTitle,
            category = "Tech & AI",
            onDismiss = { showThumbnailDialog = false },
            onThumbnailSaved = { path ->
                Toast.makeText(context, "Thumbnail saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = decodedTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            SocialShareHelper.shareVideo(
                                context = context,
                                videoPath = decodedUrl,
                                title = decodedTitle,
                                caption = "Check out this AI Video!"
                            )
                        },
                        modifier = Modifier.testTag("share_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = CyberCyan
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Player Container (9:16 portrait style)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
            ) {
                VideoPlayerView(
                    videoUri = Uri.parse(decodedUrl),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Upload to YouTube Card
            GlitchCard(
                borderColor = ElectricViolet.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "YOUTUBE PUBLISHING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricViolet,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uploadStatusMessage ?: "Ready to publish to your connected YouTube Channel.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonButton(
                        text = "🎨 AI VIRAL THUMBNAIL",
                        color = CyberCyan,
                        onClick = {
                            showThumbnailDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "🚀 PUBLISH TO ALL",
                        color = NeonGreen,
                        onClick = {
                            SocialShareHelper.broadcastToAllSocialNetworks(
                                context = context,
                                videoPath = decodedUrl,
                                title = decodedTitle,
                                caption = "Viral AI Video • Generated via Glitch AI Studio #Shorts #Viral #AI"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                NeonButton(
                    text = if (isUploading) "UPLOADING TO YOUTUBE..." else "UPLOAD TO YOUTUBE SHORTS",
                    leadingIcon = Icons.Default.Upload,
                    color = ElectricViolet,
                    enabled = !isUploading,
                    onClick = {
                        isUploading = true
                        val project = ProjectSummary(
                            id = "preview_proj",
                            title = decodedTitle,
                            topic = decodedTitle,
                            video = decodedUrl
                        )
                        viewModel.uploadProjectToYouTube(project) { percent, msg ->
                            uploadStatusMessage = msg
                            if (percent == 100 || percent < 0) {
                                isUploading = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_youtube_button")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonButton(
                        text = "INSTAGRAM REEL",
                        leadingIcon = Icons.Default.CameraAlt,
                        color = NeonPink,
                        onClick = {
                            SocialShareHelper.publishToInstagram(
                                context = context,
                                videoPath = decodedUrl,
                                title = decodedTitle,
                                caption = "Check out this AI Video!"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "FACEBOOK",
                        leadingIcon = Icons.Default.Public,
                        color = CyberCyan,
                        onClick = {
                            SocialShareHelper.publishToFacebook(
                                context = context,
                                videoPath = decodedUrl,
                                title = decodedTitle,
                                caption = "Check out this AI Video!"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
