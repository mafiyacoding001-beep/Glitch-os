package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorPanelView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = theme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            // Display Screen with live calculation & Quick Action Bar
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = theme.keyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stateHolder.calculatorExpression.ifBlank { "0" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.keyContent,
                            maxLines = 1
                        )
                        Text(
                            text = stateHolder.calculatorResult.ifBlank { "= 0" },
                            fontSize = 12.sp,
                            color = theme.primaryAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = theme.primaryAccent.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { stateHolder.switchPanel(ActivePanel.KEYBOARD) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Main Keyboard",
                                    tint = theme.primaryAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "QWERTY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primaryAccent
                                )
                            }
                        }

                        Button(
                            onClick = { stateHolder.insertCalculatorResult(insertEquation = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Insert", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = theme.surface)
                        }

                        IconButton(
                            onClick = { stateHolder.switchPanel(ActivePanel.KEYBOARD) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Calculator",
                                tint = theme.keySubContent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Quick Unit & Currency Conversion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickCalcChip("10% Off", onClick = { stateHolder.onCalculatorInput(" * 0.90") }, theme = theme)
                QuickCalcChip("18% GST", onClick = { stateHolder.onCalculatorInput(" * 1.18") }, theme = theme)
                QuickCalcChip("$ to ₹ (86.5)", onClick = { stateHolder.onCalculatorInput(" * 86.5") }, theme = theme)
                QuickCalcChip("€ to $ (1.08)", onClick = { stateHolder.onCalculatorInput(" * 1.08") }, theme = theme)
                QuickCalcChip("km to mi (0.621)", onClick = { stateHolder.onCalculatorInput(" * 0.621") }, theme = theme)
                QuickCalcChip("kg to lbs (2.204)", onClick = { stateHolder.onCalculatorInput(" * 2.204") }, theme = theme)
            }

            // Interactive Calculator Keypad Grid (5 Rows x 5 Cols)
            val buttons = listOf(
                listOf("C", "(", ")", "%", "÷"),
                listOf("7", "8", "9", "sqrt", "×"),
                listOf("4", "5", "6", "^", "-"),
                listOf("1", "2", "3", "π", "+"),
                listOf("0", ".", "⌫", "e", "=")
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                buttons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { token ->
                            val isOperator = token in listOf("+", "-", "×", "÷", "%", "^", "sqrt")
                            val isEqual = token == "="
                            val isClear = token == "C" || token == "⌫"

                            val btnBg = when {
                                isEqual -> theme.primaryAccent
                                isOperator -> theme.actionKeyBackground
                                isClear -> theme.actionKeyBackground.copy(alpha = 0.8f)
                                else -> theme.keyBackground
                            }

                            val btnTextColor = when {
                                isEqual -> theme.surface
                                isOperator -> theme.primaryAccent
                                isClear -> theme.actionKeyContent
                                else -> theme.keyContent
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = btnBg,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { stateHolder.onCalculatorInput(token) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = token,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = btnTextColor,
                                        textAlign = TextAlign.Center
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

@Composable
private fun QuickCalcChip(
    label: String,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = theme.keyContent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
