package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class KaomojiCategory {
    KAOMOJI_HAPPY,
    KAOMOJI_COOL,
    KAOMOJI_SHRUG,
    KAOMOJI_RAGE,
    KAOMOJI_LOVE,
    MATH_SCIENCE,
    CURRENCY,
    ARROWS_GLYPHS,
    ASCII_MEMES
}

@Composable
fun KaomojiSymbolsPanelView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme
    var selectedCat by remember { mutableStateOf(KaomojiCategory.KAOMOJI_HAPPY) }

    val itemsList = when (selectedCat) {
        KaomojiCategory.KAOMOJI_HAPPY -> listOf(
            "(✿◠‿◠)", "(◕‿◕✿)", "(づ｡◕‿‿◕｡)づ", "(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧",
            "ヽ(^o^)ノ", "(＾▽＾)", "╰(▔∀▔)╯", "(* ^ ω ^)",
            "(★ω★)", "o(≧▽≦)o", "(⌒▽⌒)☆", "(✧ω✧)"
        )
        KaomojiCategory.KAOMOJI_COOL -> listOf(
            "(⌐■_■)", "(•_•)>⌐■-■", "(¬‿¬)", "(¬_¬)",
            "ᕦ(ò_óˇ)ᕤ", "ᕙ(⇀‸↼‶)ᕗ", "( •̀ᴗ•́ )و ̑̑", "ദ്ദി(˵ •̀ ᴗ - ˵ ) ✧"
        )
        KaomojiCategory.KAOMOJI_SHRUG -> listOf(
            "¯\\_(ツ)_/¯", "¯\\(°_o)/¯", "╮(─▽─)╭", "┐(￣∀￣)┌",
            "┐(︶▽︶)┌", "ヽ(ー_ー )ノ", "¯\\_(⊙_ʖ⊙)_/¯"
        )
        KaomojiCategory.KAOMOJI_RAGE -> listOf(
            "(ノಠ益ಠ)ノ彡┻━┻", "(╯°□°)╯︵ ┻━┻", "(ง'̀-'́)ง", "凸(¬‿¬)",
            "(；￣Д￣)", "(╬ Ò﹏Ó)", "ヽ( `д´*)ノ", "(҂ `з´ )"
        )
        KaomojiCategory.KAOMOJI_LOVE -> listOf(
            "(♥ω♥*)", "(♡-_-♡)", "(─‿‿─)♡", "(◕‿◕)♡",
            "(´♡‿♡`)", "(✿ ♡‿♡)", "(｡・//ε//・｡)", "(´∀｀)♡"
        )
        KaomojiCategory.MATH_SCIENCE -> listOf(
            "±", "×", "÷", "√", "∞", "π", "∑", "∫",
            "≈", "≠", "≤", "≥", "°", "µ", "∆", "Ω",
            "λ", "θ", "α", "β", "‰", "∂", "∇", "∈"
        )
        KaomojiCategory.CURRENCY -> listOf(
            "₹", "$", "€", "£", "¥", "₿", "₩", "₽",
            "¢", "₺", "₫", "₴", "₦", "₲", "₭", "₮"
        )
        KaomojiCategory.ARROWS_GLYPHS -> listOf(
            "➔", "➜", "➤", "▲", "▼", "◄", "►", "✦",
            "⚡", "★", "⦿", "❖", "✓", "✗", "❄", "✿",
            "♠", "♣", "♥", "♦", "♪", "♫", "⚓", "⚔"
        )
        KaomojiCategory.ASCII_MEMES -> listOf(
            "( ͡° ͜ʖ ͡°)", "ʕ•ᴥ•ʔ", "ಠ_ಠ", "(งツ)ว",
            "(=^･ω･^=)", "(•‿•)", "( ͠° ͟ʖ ͡°)", "ʕっ•ᴥ•ʔっ"
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = theme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            // Category Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryPill("😊 Happy", selectedCat == KaomojiCategory.KAOMOJI_HAPPY, { selectedCat = KaomojiCategory.KAOMOJI_HAPPY }, theme)
                    CategoryPill("😎 Cool", selectedCat == KaomojiCategory.KAOMOJI_COOL, { selectedCat = KaomojiCategory.KAOMOJI_COOL }, theme)
                    CategoryPill("🤷 Shrug", selectedCat == KaomojiCategory.KAOMOJI_SHRUG, { selectedCat = KaomojiCategory.KAOMOJI_SHRUG }, theme)
                    CategoryPill("❤️ Love", selectedCat == KaomojiCategory.KAOMOJI_LOVE, { selectedCat = KaomojiCategory.KAOMOJI_LOVE }, theme)
                    CategoryPill("🔥 Rage", selectedCat == KaomojiCategory.KAOMOJI_RAGE, { selectedCat = KaomojiCategory.KAOMOJI_RAGE }, theme)
                    CategoryPill("📐 Math", selectedCat == KaomojiCategory.MATH_SCIENCE, { selectedCat = KaomojiCategory.MATH_SCIENCE }, theme)
                    CategoryPill("💰 Money", selectedCat == KaomojiCategory.CURRENCY, { selectedCat = KaomojiCategory.CURRENCY }, theme)
                    CategoryPill("➔ Glyphs", selectedCat == KaomojiCategory.ARROWS_GLYPHS, { selectedCat = KaomojiCategory.ARROWS_GLYPHS }, theme)
                    CategoryPill("🐱 Memes", selectedCat == KaomojiCategory.ASCII_MEMES, { selectedCat = KaomojiCategory.ASCII_MEMES }, theme)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "QWERTY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = { stateHolder.switchPanel(ActivePanel.KEYBOARD) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Kaomoji",
                            tint = theme.keySubContent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Grid Items
            val isSymbolType = selectedCat == KaomojiCategory.MATH_SCIENCE ||
                    selectedCat == KaomojiCategory.CURRENCY ||
                    selectedCat == KaomojiCategory.ARROWS_GLYPHS

            val columns = if (isSymbolType) GridCells.Fixed(8) else GridCells.Fixed(3)

            LazyVerticalGrid(
                columns = columns,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(itemsList) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = theme.keyBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSymbolType) 38.dp else 46.dp)
                            .clickable {
                                stateHolder.insertText(item)
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(4.dp)
                        ) {
                            Text(
                                text = item,
                                fontSize = if (isSymbolType) 16.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.keyContent,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent
        )
    }
}
