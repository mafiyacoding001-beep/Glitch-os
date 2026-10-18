package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

@Composable
fun KeyboardView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme
    val baseHeight = when (stateHolder.activePanel) {
        ActivePanel.AUTO_TYPE -> 265.dp
        ActivePanel.APK_EXPLORER -> 265.dp
        ActivePanel.AI_MAGIC -> 255.dp
        ActivePanel.CALCULATOR -> 255.dp
        ActivePanel.SNIPPETS -> 255.dp
        ActivePanel.KAOMOJI_SYMBOLS -> 255.dp
        ActivePanel.SETTINGS -> 255.dp
        ActivePanel.KEYBOARD -> {
            var h = 220.dp
            if (stateHolder.showNumberRow) h += 32.dp
            if (stateHolder.showArrowRow) h += 30.dp
            h
        }
        else -> 230.dp
    }
    val scaledHeight = (baseHeight.value * stateHolder.keyboardHeightScale).dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = theme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // SmartBar Toolbar & Super Tools
            SmartBarView(stateHolder = stateHolder)

            HorizontalDivider(color = theme.keySubContent.copy(alpha = 0.2f))

            // Main Active Panel View with One-Handed Mode Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scaledHeight)
                    .background(theme.surface)
            ) {
                // Left Side One-Handed Controller Bar
                if (stateHolder.oneHandedMode == OneHandedMode.LEFT) {
                    OneHandedSideBar(stateHolder = stateHolder, alignRight = false)
                }

                Box(
                    modifier = Modifier
                        .weight(if (stateHolder.oneHandedMode != OneHandedMode.OFF) 0.82f else 1f)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    when (stateHolder.activePanel) {
                        ActivePanel.KEYBOARD -> MainKeypadView(stateHolder = stateHolder)
                        ActivePanel.EMOJI -> EmojiPanelView(stateHolder = stateHolder)
                        ActivePanel.CLIPBOARD -> ClipboardPanelView(stateHolder = stateHolder)
                        ActivePanel.CURSOR_PAD -> CursorPadView(stateHolder = stateHolder)
                        ActivePanel.AUTO_TYPE -> AutoTypePanelView(stateHolder = stateHolder)
                        ActivePanel.AI_MAGIC -> AiMagicPanelView(stateHolder = stateHolder)
                        ActivePanel.CALCULATOR -> CalculatorPanelView(stateHolder = stateHolder)
                        ActivePanel.SNIPPETS -> SnippetsPanelView(stateHolder = stateHolder)
                        ActivePanel.KAOMOJI_SYMBOLS -> KaomojiSymbolsPanelView(stateHolder = stateHolder)
                        ActivePanel.SETTINGS -> KeyboardSettingsPanelView(stateHolder = stateHolder)
                        ActivePanel.APK_EXPLORER -> ApkExplorerPanelView(stateHolder = stateHolder)
                    }

                    // Canvas Gesture Glide Trail Overlay
                    if (stateHolder.glideTrailPoints.isNotEmpty()) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val path = androidx.compose.ui.graphics.Path()
                            stateHolder.glideTrailPoints.forEachIndexed { index, offset ->
                                if (index == 0) path.moveTo(offset.x, offset.y)
                                else path.lineTo(offset.x, offset.y)
                            }
                            drawPath(
                                path = path,
                                color = theme.trailColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 12f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Long Press Accented Character Selector Popup Overlay
                    stateHolder.activeLongPressKey?.let { longPressKey ->
                        Popup(
                            alignment = Alignment.Center,
                            onDismissRequest = { stateHolder.dismissLongPress() }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = theme.actionKeyBackground,
                                tonalElevation = 8.dp,
                                shadowElevation = 12.dp,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    longPressKey.longPressOptions.forEach { option ->
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(theme.primaryAccent)
                                                .clickable { stateHolder.selectLongPressOption(option) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.keyContent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Side One-Handed Controller Bar
                if (stateHolder.oneHandedMode == OneHandedMode.RIGHT) {
                    OneHandedSideBar(stateHolder = stateHolder, alignRight = true)
                }
            }
        }
    }
}

@Composable
private fun OneHandedSideBar(
    stateHolder: KeyboardStateHolder,
    alignRight: Boolean
) {
    val theme = stateHolder.currentTheme
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(48.dp)
            .background(theme.actionKeyBackground),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                stateHolder.setOneHanded(if (alignRight) OneHandedMode.LEFT else OneHandedMode.RIGHT)
            }
        ) {
            Icon(
                imageVector = if (alignRight) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Switch Side",
                tint = theme.keyContent
            )
        }
        IconButton(
            onClick = { stateHolder.setOneHanded(OneHandedMode.OFF) }
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Full Width",
                tint = theme.primaryAccent
            )
        }
    }
}

@Composable
private fun MainKeypadView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme
    val layout = stateHolder.currentLayout

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Filter rows based on number row setting: In QWERTY mode, row 0 is the full-sized number row (1..0).
        // If showNumberRow is disabled, row 0 is hidden (keyboard starts directly with 'q').
        // If showNumberRow is enabled, row 0 is shown as a single full-sized row matching key styling.
        // The duplicate small cramped 30dp number row is completely removed.
        val effectiveRows = if (!stateHolder.showNumberRow && stateHolder.keyboardMode == KeyboardMode.QWERTY) {
            layout.rows.drop(1)
        } else {
            layout.rows
        }

        // Standard Keyboard Layout Rows
        effectiveRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                row.keys.forEach { keyModel ->
                    KeyView(
                        keyModel = keyModel,
                        stateHolder = stateHolder,
                        modifier = Modifier
                            .weight(keyModel.weight)
                            .fillMaxHeight()
                    )
                }
            }
        }

        // Dedicated Bottom Arrow Navigation Row (← ↑ ↓ →)
        if (stateHolder.showArrowRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ArrowNavButton(Icons.Default.KeyboardArrowLeft, "Left", { stateHolder.moveCursor(-1) }, theme, Modifier.weight(1f))
                ArrowNavButton(Icons.Default.KeyboardArrowUp, "Up", { stateHolder.moveCursor(-20) }, theme, Modifier.weight(1f))
                ArrowNavButton(Icons.Default.KeyboardArrowDown, "Down", { stateHolder.moveCursor(20) }, theme, Modifier.weight(1f))
                ArrowNavButton(Icons.Default.KeyboardArrowRight, "Right", { stateHolder.moveCursor(1) }, theme, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ArrowNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
    theme: KeyboardThemeColors,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = theme.actionKeyBackground,
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = theme.primaryAccent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ApkExplorerPanelView(stateHolder: KeyboardStateHolder) {
    val theme = stateHolder.currentTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = "App Explorer",
            tint = theme.primaryAccent,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "App & Package Explorer",
            fontWeight = FontWeight.Bold,
            color = theme.keyContent,
            fontSize = 14.sp
        )
        Text(
            text = "Inspect installed system and user packages",
            color = theme.keyContent.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}
