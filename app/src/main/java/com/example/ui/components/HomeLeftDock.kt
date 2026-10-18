package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class HomeDockItem(
    val id: String,
    val icon: ImageVector,
    val description: String,
    val activeColor: Color
) {
    AUTOPILOT("autopilot", Icons.Default.SmartToy, "YouTube Auto-Pilot (Default)", NeonGreen),
    SEO_STUDIO("seo_studio", Icons.Default.Search, "SEO Studio Lab", CyberCyan),
    PLAYLISTS("playlists", Icons.Default.PlaylistPlay, "Playlists Hub", ElectricViolet),
    COMMUNITY("community", Icons.Default.Forum, "Community Engagement", NeonPink),
    ANALYTICS("analytics", Icons.Default.Insights, "Analytics Intelligence", Color(0xFFFFD700)),
    BACKGROUND("background", Icons.Default.Bolt, "Background 24/7 Engine", NeonGreen),
    VPN("vpn", Icons.Default.VpnKey, "VPN Gate Shield", CyberCyan),
    STUDIO("studio", Icons.Default.Dashboard, "Glitch Studio Management", ElectricViolet),
    LEARN("learn", Icons.Default.MenuBook, "Learn Agent Manual", CyberCyan),
    LEGAL("legal", Icons.Default.Policy, "Privacy Policy & Legal Terms", Color(0xFFFFD700)),
    VIDEOS("videos", Icons.Default.VideoLibrary, "Creations & Video Gallery", NeonPink),
    ADS("ads", Icons.Default.Block, "Universal AI Ad Blocker Shield", Color(0xFFFF5252)),
    SETTING("setting", Icons.Default.Settings, "App Settings", ElectricViolet),
    ACCOUNT("account", Icons.Default.AccountCircle, "Google Account Profile", CyberCyan),
    KEYBOARD("keyboard", Icons.Default.Keyboard, "Glitch AI Keyboard", NeonGreen),
    TREND_PILOT("trend_pilot", Icons.Default.Whatshot, "Trend Pilot Viral Repurposer", Color(0xFFFF5252)),
    CHATBOT("chatbot", Icons.Default.Chat, "AI Chatbot Assistant", NeonGreen)
}

@Composable
fun HomeLeftDock(
    selectedItem: HomeDockItem,
    onItemSelected: (HomeDockItem) -> Unit,
    modifier: Modifier = Modifier,
    isVpnActive: Boolean = false,
    isAdSkipperActive: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Inner Ring (Row 1 near Close Button - 5 Core Items)
    val innerItems = remember {
        listOf(
            HomeDockItem.AUTOPILOT,
            HomeDockItem.SEO_STUDIO,
            HomeDockItem.PLAYLISTS,
            HomeDockItem.COMMUNITY,
            HomeDockItem.ANALYTICS
        )
    }
    val innerAngles = remember { listOf(-64f, -32f, 0f, 32f, 64f) }

    // Outer Ring (Row 2 Last Arc further out - 11 Remaining Items)
    val outerItems = remember {
        listOf(
            HomeDockItem.BACKGROUND,
            HomeDockItem.VPN,
            HomeDockItem.STUDIO,
            HomeDockItem.LEARN,
            HomeDockItem.LEGAL,
            HomeDockItem.VIDEOS,
            HomeDockItem.ADS,
            HomeDockItem.SETTING,
            HomeDockItem.KEYBOARD,
            HomeDockItem.TREND_PILOT,
            HomeDockItem.CHATBOT
        )
    }
    val outerAngles = remember { listOf(-80f, -64f, -48f, -32f, -16f, 0f, 16f, 32f, 48f, 64f, 80f) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        if (!isExpanded) {
            // FLOATING COLLAPSED BUTTON (Glowing Cyberpunk Circular Trigger)
            Surface(
                onClick = { isExpanded = true },
                color = DarkSurface.copy(alpha = 0.95f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(2.dp, CyberCyan),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("floating_menu_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Open Radial Arc Menu",
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            // RADIAL ARC DOME OVERLAY (Left-Anchored Half-Circle Dome)
            val containerWidth = 240.dp
            val containerHeight = 360.dp
            val centerX = 28.dp
            val centerY = 180.dp

            Box(
                modifier = Modifier
                    .size(width = containerWidth, height = containerHeight)
                    .shadow(22.dp, shape = RoundedCornerShape(topEnd = 180.dp, bottomEnd = 180.dp))
                    .testTag("radial_arc_menu_overlay")
            ) {
                // Background Left-Anchored Glassmorphic Arc Dome Path with Glowing Cyan Border
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = 28.dp.toPx()
                    val cy = h / 2f
                    val rCutout = 34.dp.toPx()

                    val path = Path().apply {
                        moveTo(0f, 20f)
                        quadraticTo(0f, 0f, 20f, 0f)
                        // Outer right semi-circle dome
                        arcTo(
                            rect = Rect(cx - (w - cx), 0f, cx + (w - cx), h),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        quadraticTo(0f, h, 0f, h - 20f)
                        lineTo(0f, cy + rCutout)
                        // Inner cutout arc around Close 'X' button
                        arcTo(
                            rect = Rect(cx - rCutout, cy - rCutout, cx + rCutout, cy + rCutout),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )
                        lineTo(0f, 20f)
                        close()
                    }

                    // Fill Dark Glassmorphic Surface
                    drawPath(path = path, color = DarkSurface.copy(alpha = 0.98f))
                    // Draw Neon Cyan Stroke
                    drawPath(path = path, color = CyberCyan, style = Stroke(width = 2.dp.toPx()))
                }

                // PROMINENT LARGE ROUND 'X' CLOSE BUTTON
                Box(
                    modifier = Modifier
                        .offset(x = centerX - 24.dp, y = centerY - 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.22f))
                        .border(2.dp, CyberCyan, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = CyberCyan)
                        ) {
                            isExpanded = false
                        }
                        .testTag("close_radial_menu_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // INNER CIRCULAR RING (7 Items - Radius 92dp)
                val innerRadius = 92.dp
                innerItems.forEachIndexed { index, item ->
                    val angle = innerAngles[index]
                    val angleRad = Math.toRadians(angle.toDouble())
                    val itemX = centerX + (innerRadius.value * cos(angleRad)).dp - 19.dp
                    val itemY = centerY + (innerRadius.value * sin(angleRad)).dp - 19.dp

                    Box(
                        modifier = Modifier.offset(x = itemX, y = itemY)
                    ) {
                        ArcMenuItemRoundBadge(
                            item = item,
                            isSelected = selectedItem == item,
                            isVpnActive = isVpnActive,
                            isAdSkipperActive = isAdSkipperActive,
                            onClick = {
                                onItemSelected(item)
                                isExpanded = false
                            }
                        )
                    }
                }

                // OUTER CIRCULAR RING (7 Items - Radius 158dp)
                val outerRadius = 158.dp
                outerItems.forEachIndexed { index, item ->
                    val angle = outerAngles[index]
                    val angleRad = Math.toRadians(angle.toDouble())
                    val itemX = centerX + (outerRadius.value * cos(angleRad)).dp - 19.dp
                    val itemY = centerY + (outerRadius.value * sin(angleRad)).dp - 19.dp

                    Box(
                        modifier = Modifier.offset(x = itemX, y = itemY)
                    ) {
                        ArcMenuItemRoundBadge(
                            item = item,
                            isSelected = selectedItem == item,
                            isVpnActive = isVpnActive,
                            isAdSkipperActive = isAdSkipperActive,
                            onClick = {
                                onItemSelected(item)
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcMenuItemRoundBadge(
    item: HomeDockItem,
    isSelected: Boolean,
    isVpnActive: Boolean,
    isAdSkipperActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) item.activeColor.copy(alpha = 0.32f) else DarkSurfaceElevated
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) item.activeColor else CyberCyan.copy(alpha = 0.40f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = item.activeColor)
            ) {
                onClick()
            }
            .testTag("dock_item_${item.id}"),
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (item == HomeDockItem.VPN && isVpnActive) {
                    Badge(
                        containerColor = NeonGreen,
                        modifier = Modifier.size(5.dp)
                    )
                } else if (item == HomeDockItem.ADS && isAdSkipperActive) {
                    Badge(
                        containerColor = NeonGreen,
                        modifier = Modifier.size(5.dp)
                    )
                }
            }
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.description,
                tint = if (isSelected) item.activeColor else TextPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
