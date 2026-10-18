package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CursorPadView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Row: Selection, Clipboard Operations and Return to Keyboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = theme.primaryAccent,
                modifier = Modifier
                    .height(28.dp)
                    .clickable { stateHolder.switchPanel(ActivePanel.KEYBOARD) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(imageVector = Icons.Default.Keyboard, contentDescription = "Main Keyboard", tint = theme.surface, modifier = Modifier.size(13.dp))
                    Text(text = "QWERTY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.surface)
                }
            }
            PadActionButton("Select", Icons.Default.SelectAll, onClick = { stateHolder.selectAllText() }, theme = theme, modifier = Modifier.weight(1f))
            PadActionButton("Copy", Icons.Default.ContentCopy, onClick = { stateHolder.copySelectedText() }, theme = theme, modifier = Modifier.weight(1f))
            PadActionButton("Cut", Icons.Default.ContentCut, onClick = { stateHolder.cutSelectedText() }, theme = theme, modifier = Modifier.weight(1f))
            PadActionButton("Paste", Icons.Default.ContentPaste, onClick = { stateHolder.pasteFromClipboard() }, theme = theme, modifier = Modifier.weight(1f))
            PadActionButton("Clear", Icons.Default.ClearAll, onClick = { stateHolder.clearAllText() }, theme = theme, modifier = Modifier.weight(1f))
        }

        // D-Pad and Jump Controls Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Quick Action Column (Jump to Start, Delete Word)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.actionKeyBackground,
                    modifier = Modifier.clickable {
                        stateHolder.moveCursor(-10000)
                    }
                ) {
                    Text(
                        text = "⏮ Start",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.actionKeyContent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.actionKeyBackground,
                    modifier = Modifier.clickable {
                        stateHolder.deleteWordBackward()
                    }
                ) {
                    Text(
                        text = "⌫ Word",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.actionKeyContent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Center Precision D-Pad Navigation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Up Arrow
                IconButton(
                    onClick = { stateHolder.moveCursor(-20) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(theme.primaryAccent, CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = theme.surface)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Arrow
                    IconButton(
                        onClick = { stateHolder.moveCursor(-1) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(theme.primaryAccent, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = theme.surface)
                    }

                    // Center D-Pad Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(theme.actionKeyBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenWith,
                            contentDescription = null,
                            tint = theme.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Right Arrow
                    IconButton(
                        onClick = { stateHolder.moveCursor(1) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(theme.primaryAccent, CircleShape)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = theme.surface)
                    }
                }

                // Down Arrow
                IconButton(
                    onClick = { stateHolder.moveCursor(20) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(theme.primaryAccent, CircleShape)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = theme.surface)
                }
            }

            // Right Quick Action Column (Jump to End, Switch Keyboard)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.actionKeyBackground,
                    modifier = Modifier.clickable {
                        stateHolder.moveCursor(10000)
                    }
                ) {
                    Text(
                        text = "End ⏭",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.actionKeyContent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = theme.primaryAccent,
                    modifier = Modifier.clickable {
                        stateHolder.switchPanel(ActivePanel.KEYBOARD)
                    }
                ) {
                    Text(
                        text = "⌨️ Back",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.surface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PadActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    theme: KeyboardThemeColors,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = theme.actionKeyBackground,
        modifier = modifier
            .height(28.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = theme.primaryAccent, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = theme.keyContent)
        }
    }
}
