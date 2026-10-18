package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.FloatingAdShieldService
import com.example.service.YouTubeAdSkipperService
import com.example.ui.theme.*

@Composable
fun YouTubeAdSkipperCard(
    modifier: Modifier = Modifier,
    onOpenYoutube: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isAccessibilityEnabled by remember {
        mutableStateOf(YouTubeAdSkipperService.isAccessibilityServiceEnabled(context))
    }
    val isServiceRunning by YouTubeAdSkipperService.isServiceActive.collectAsState()
    val totalSkipped by YouTubeAdSkipperService.totalAdsSkipped.collectAsState()
    val lastSkippedTime by YouTubeAdSkipperService.lastSkippedTime.collectAsState()

    var autoMuteState by remember { mutableStateOf(YouTubeAdSkipperService.isAutoMuteEnabled(context)) }
    var turboModeState by remember { mutableStateOf(YouTubeAdSkipperService.isTurboModeEnabled(context)) }
    var shortsAutoSkipState by remember { mutableStateOf(YouTubeAdSkipperService.isShortsAutoSkipEnabled(context)) }
    var ytMusicAutoSkipState by remember { mutableStateOf(YouTubeAdSkipperService.isYouTubeMusicAutoSkipEnabled(context)) }
    var instagramAutoSkipState by remember { mutableStateOf(YouTubeAdSkipperService.isInstagramAutoSkipEnabled(context)) }
    var facebookAutoSkipState by remember { mutableStateOf(YouTubeAdSkipperService.isFacebookAutoSkipEnabled(context)) }
    var webBrowserAutoSkipState by remember { mutableStateOf(YouTubeAdSkipperService.isWebBrowserAutoSkipEnabled(context)) }
    var showHowItWorks by remember { mutableStateOf(false) }

    // Recheck permission when component is focused/recomposed
    LaunchedEffect(Unit) {
        YouTubeAdSkipperService.loadStats(context)
        isAccessibilityEnabled = YouTubeAdSkipperService.isAccessibilityServiceEnabled(context)
    }

    val isActuallyActive = isAccessibilityEnabled || isServiceRunning

    GlitchCard(
        borderColor = if (isActuallyActive) NeonGreen else ElectricViolet.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActuallyActive) NeonGreen.copy(alpha = 0.15f) else ElectricViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Ad Blocker Shield",
                            tint = if (isActuallyActive) NeonGreen else ElectricViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "UNIVERSAL AI AD BLOCKER",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            maxLines = 1
                        )
                        Text(
                            text = "Ad Shield • YT, YT Music, Insta & FB",
                            fontSize = 9.5.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (isActuallyActive) NeonGreen.copy(alpha = 0.15f) else ElectricViolet.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isActuallyActive) NeonGreen.copy(alpha = 0.5f) else ElectricViolet.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isActuallyActive) NeonGreen else ElectricViolet)
                        )
                        Text(
                            text = if (isActuallyActive) "SHIELD ACTIVE" else "NEEDS SETUP",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActuallyActive) NeonGreen else ElectricViolet
                        )
                    }
                }
            }

            // Description / Status Banner
            if (!isActuallyActive) {
                Surface(
                    color = DarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = ElectricViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Enable Accessibility for Universal Ad Shield",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "GLITCH uses Accessibility Service to bypass, block & auto-skip video ads, music ads & sponsored reels instantly across YouTube, YouTube Music, Instagram and Facebook.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp
                        )

                        Button(
                            onClick = {
                                YouTubeAdSkipperService.openAccessibilitySettings(context)
                                Toast.makeText(
                                    context,
                                    "Tap 'Installed Apps' / 'GLITCH' and turn Accessibility ON",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("btn_enable_ad_skipper_accessibility"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "1-TAP ENABLE AD BLOCKER SHIELD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Active Metrics Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalSkipped",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen
                        )
                        Text(
                            text = "SUCCESSFUL ACTIONS",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(DarkBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (lastSkippedTime.isNotEmpty()) lastSkippedTime else "—",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricViolet,
                            maxLines = 1
                        )
                        Text(
                            text = "LAST ACTION",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Feature Switches Matrix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Turbo Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Turbo 0ms Auto-Skip",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Instant click as soon as skip button renders",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = turboModeState,
                        onCheckedChange = {
                            turboModeState = it
                            YouTubeAdSkipperService.setTurboModeEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Divider(color = DarkBorder, thickness = 0.5.dp)

                // Auto-Mute Unskippable Ads
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔇 Auto-Mute Video Ads",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mutes audio during countdown ads & restores automatically",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = autoMuteState,
                        onCheckedChange = {
                            autoMuteState = it
                            YouTubeAdSkipperService.setAutoMuteEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Divider(color = DarkBorder, thickness = 0.5.dp)

                // YouTube Shorts Auto-Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📱 Auto-Skip YouTube Shorts Ads",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-swipes sponsored video ads in Shorts reel instantly",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = shortsAutoSkipState,
                        onCheckedChange = {
                            shortsAutoSkipState = it
                            YouTubeAdSkipperService.setShortsAutoSkipEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF5252),
                            checkedTrackColor = Color(0xFFFF5252).copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Divider(color = DarkBorder, thickness = 0.5.dp)

                // YouTube Music Auto-Skip & Mute
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎵 YouTube Music Ad Blocker & Mute",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-skips & mutes audio ads in YouTube Music app",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = ytMusicAutoSkipState,
                        onCheckedChange = {
                            ytMusicAutoSkipState = it
                            YouTubeAdSkipperService.setYouTubeMusicAutoSkipEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFD700),
                            checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Divider(color = DarkBorder, thickness = 0.5.dp)

                // Instagram Reels & Sponsored Ads Skipper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📸 Instagram Reels & Ad Shield",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-swipes Instagram sponsored reels & ad posts",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = instagramAutoSkipState,
                        onCheckedChange = {
                            instagramAutoSkipState = it
                            YouTubeAdSkipperService.setInstagramAutoSkipEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonPink,
                            checkedTrackColor = NeonPink.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Divider(color = DarkBorder, thickness = 0.5.dp)

                // Facebook Reels & Feed Ad Shield
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📘 Facebook Reels & Feed Ad Shield",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-swipes Facebook sponsored video ads & posts",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = facebookAutoSkipState,
                        onCheckedChange = {
                            facebookAutoSkipState = it
                            YouTubeAdSkipperService.setFacebookAutoSkipEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Divider(color = DarkBorder, thickness = 0.5.dp)

                // Floating Ad-Shield Bubble Widget
                var floatingBubbleState by remember {
                    mutableStateOf(FloatingAdShieldService.isOverlayPermissionGranted(context))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🫧 Floating Ad-Shield Bubble Widget",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            text = "Draggable on-screen overlay with live block stats & turbo switch",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = floatingBubbleState,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (FloatingAdShieldService.isOverlayPermissionGranted(context)) {
                                    floatingBubbleState = true
                                    FloatingAdShieldService.startService(context)
                                    Toast.makeText(context, "🫧 Floating Shield Bubble Activated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please allow 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                                    FloatingAdShieldService.requestOverlayPermission(context)
                                }
                            } else {
                                floatingBubbleState = false
                                FloatingAdShieldService.stopService(context)
                                Toast.makeText(context, "Floating Shield Bubble Hidden", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌐 Web Browser Ad Shield (Chrome/Brave)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-closes popups & skips ads on m.youtube.com & web",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = webBrowserAutoSkipState,
                        onCheckedChange = {
                            webBrowserAutoSkipState = it
                            YouTubeAdSkipperService.setWebBrowserAutoSkipEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ElectricViolet,
                            checkedTrackColor = ElectricViolet.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }
            }

            // Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (onOpenYoutube != null) {
                            onOpenYoutube()
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open YouTube", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("btn_launch_youtube_app")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "LAUNCH YOUTUBE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
