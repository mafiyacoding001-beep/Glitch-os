package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmartBarView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme
    val context = LocalContext.current
    val hasSuggestions = stateHolder.suggestions.isNotEmpty() && !stateHolder.isVoiceTypingActive

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        color = theme.surface,
        tonalElevation = 1.dp
    ) {
        if (hasSuggestions) {
            // Dedicated Full-Width Suggestions Bar (Upper tools row is completely hidden)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horizontally scrollable suggestions spanning full width
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stateHolder.suggestions.forEach { suggestion ->
                        val isAutoCorrect = suggestion.isAutoCorrect
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isAutoCorrect) theme.primaryAccent else theme.actionKeyBackground,
                            border = if (isAutoCorrect) null else androidx.compose.foundation.BorderStroke(1.dp, theme.keySubContent.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    stateHolder.playFeedback()
                                    stateHolder.applySuggestion(suggestion)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = suggestion.word,
                                    fontSize = 14.sp,
                                    fontWeight = if (isAutoCorrect) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isAutoCorrect) theme.surface else theme.keyContent,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Dismiss Suggestions Button (restores top tools row)
                IconButton(
                    onClick = { stateHolder.dismissSuggestions() },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hide suggestions",
                        tint = theme.keySubContent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Normal Super-Tools Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If subpanel is open, show prominent Return to Keyboard button first
                if (stateHolder.activePanel != ActivePanel.KEYBOARD) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.primaryAccent,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { stateHolder.switchPanel(ActivePanel.KEYBOARD) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Main Keyboard",
                                tint = theme.surface,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "QWERTY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.surface
                            )
                        }
                    }
                }

                // Horizontal scrollable super-tools bar
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // AI Magic Tools
                    SmartBarButton(
                        icon = Icons.Default.AutoFixHigh,
                        label = "AI Magic",
                        isSelected = stateHolder.activePanel == ActivePanel.AI_MAGIC,
                        onClick = { stateHolder.switchPanel(ActivePanel.AI_MAGIC) },
                        theme = theme
                    )

                    // Calculator
                    SmartBarButton(
                        icon = Icons.Default.Calculate,
                        label = "Calc",
                        isSelected = stateHolder.activePanel == ActivePanel.CALCULATOR,
                        onClick = { stateHolder.switchPanel(ActivePanel.CALCULATOR) },
                        theme = theme
                    )

                    // Snippets
                    SmartBarButton(
                        icon = Icons.Default.ElectricBolt,
                        label = "Snippets",
                        isSelected = stateHolder.activePanel == ActivePanel.SNIPPETS,
                        onClick = { stateHolder.switchPanel(ActivePanel.SNIPPETS) },
                        theme = theme
                    )

                    // Kaomoji & Symbols
                    SmartBarButton(
                        icon = Icons.Default.Face,
                        label = "Kaomoji",
                        isSelected = stateHolder.activePanel == ActivePanel.KAOMOJI_SYMBOLS,
                        onClick = { stateHolder.switchPanel(ActivePanel.KAOMOJI_SYMBOLS) },
                        theme = theme
                    )

                    // Auto Type Automation
                    SmartBarButton(
                        icon = Icons.Default.Bolt,
                        label = "AutoType",
                        isSelected = stateHolder.activePanel == ActivePanel.AUTO_TYPE,
                        onClick = { stateHolder.switchPanel(ActivePanel.AUTO_TYPE) },
                        theme = theme
                    )

                    // Clipboard History
                    SmartBarButton(
                        icon = Icons.Default.ContentPaste,
                        label = "Clipboard",
                        isSelected = stateHolder.activePanel == ActivePanel.CLIPBOARD,
                        onClick = { stateHolder.switchPanel(ActivePanel.CLIPBOARD) },
                        theme = theme
                    )

                    // Cursor Controller D-Pad
                    SmartBarButton(
                        icon = Icons.Default.OpenWith,
                        label = "Cursor",
                        isSelected = stateHolder.activePanel == ActivePanel.CURSOR_PAD,
                        onClick = { stateHolder.switchPanel(ActivePanel.CURSOR_PAD) },
                        theme = theme
                    )

                    // Emojis
                    SmartBarButton(
                        icon = Icons.Default.SentimentSatisfiedAlt,
                        label = "Emoji",
                        isSelected = stateHolder.activePanel == ActivePanel.EMOJI,
                        onClick = { stateHolder.switchPanel(ActivePanel.EMOJI) },
                        theme = theme
                    )

                    // Hinglish Switcher
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (stateHolder.isHinglishModeEnabled) theme.primaryAccent else theme.actionKeyBackground,
                        modifier = Modifier.clickable { stateHolder.toggleHinglishMode() }
                    ) {
                        Text(
                            text = if (stateHolder.isHinglishModeEnabled) "हि / HI" else "EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stateHolder.isHinglishModeEnabled) theme.surface else theme.keyContent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Voice Typing
                    SmartBarButton(
                        icon = Icons.Default.Mic,
                        label = if (stateHolder.isVoiceTypingActive) "Listening" else "Voice",
                        isSelected = stateHolder.isVoiceTypingActive,
                        onClick = { stateHolder.toggleVoiceTyping(context) },
                        theme = theme
                    )

                    // APK Explorer / Apps Inspector
                    SmartBarButton(
                        icon = Icons.Default.Android,
                        label = "APK",
                        isSelected = stateHolder.activePanel == ActivePanel.APK_EXPLORER,
                        onClick = { stateHolder.switchPanel(ActivePanel.APK_EXPLORER) },
                        theme = theme
                    )

                    // Keyboard Preferences
                    SmartBarButton(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        isSelected = stateHolder.activePanel == ActivePanel.SETTINGS,
                        onClick = { stateHolder.switchPanel(ActivePanel.SETTINGS) },
                        theme = theme
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartBarButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) theme.primaryAccent else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) theme.surface else theme.keyContent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) theme.surface else theme.keyContent
            )
        }
    }
}
