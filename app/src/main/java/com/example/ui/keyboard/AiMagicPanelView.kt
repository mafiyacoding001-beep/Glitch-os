package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class AiMagicTab {
    TONE,
    CASE_CONVERT,
    FANCY_FONTS,
    SMART_REPLIES
}

@Composable
fun AiMagicPanelView(
    stateHolder: KeyboardStateHolder,
    modifier: Modifier = Modifier
) {
    val theme = stateHolder.currentTheme
    var selectedTab by remember { mutableStateOf(AiMagicTab.TONE) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = theme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Header with Tab Selectors and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Pills
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiTabChip(
                        label = "⚡ Tone Shifter",
                        selected = selectedTab == AiMagicTab.TONE,
                        onClick = { selectedTab = AiMagicTab.TONE },
                        theme = theme
                    )
                    AiTabChip(
                        label = "🔤 Case Converter",
                        selected = selectedTab == AiMagicTab.CASE_CONVERT,
                        onClick = { selectedTab = AiMagicTab.CASE_CONVERT },
                        theme = theme
                    )
                    AiTabChip(
                        label = "✨ Fancy Fonts",
                        selected = selectedTab == AiMagicTab.FANCY_FONTS,
                        onClick = { selectedTab = AiMagicTab.FANCY_FONTS },
                        theme = theme
                    )
                    AiTabChip(
                        label = "💬 Smart Replies",
                        selected = selectedTab == AiMagicTab.SMART_REPLIES,
                        onClick = { selectedTab = AiMagicTab.SMART_REPLIES },
                        theme = theme
                    )
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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close AI Panel",
                            tint = theme.keySubContent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                AiMagicTab.TONE -> ToneShifterSection(stateHolder = stateHolder, theme = theme)
                AiMagicTab.CASE_CONVERT -> CaseConverterSection(stateHolder = stateHolder, theme = theme)
                AiMagicTab.FANCY_FONTS -> FancyFontsSection(stateHolder = stateHolder, theme = theme)
                AiMagicTab.SMART_REPLIES -> SmartRepliesSection(stateHolder = stateHolder, theme = theme)
            }
        }
    }
}

@Composable
private fun AiTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    theme: KeyboardThemeColors
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) theme.primaryAccent else theme.actionKeyBackground,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) theme.surface else theme.keyContent
        )
    }
}

@Composable
private fun ToneShifterSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    val tones = listOf(
        Pair(TextTone.FIX_GRAMMAR, "✨ Fix Grammar & Polish"),
        Pair(TextTone.PROFESSIONAL, "💼 Professional / Corporate"),
        Pair(TextTone.CASUAL, "😎 Casual / Gen-Z Slang"),
        Pair(TextTone.CONCISE, "📝 Concise Bullet Points"),
        Pair(TextTone.EMOJIFY, "🔥 Emojify & Hype"),
        Pair(TextTone.VIRAL_HYPE, "🚨 Viral Breaking News"),
        Pair(TextTone.URGENT, "⚠️ Urgent High Priority"),
        Pair(TextTone.ROAST, "💀 Witty Roast"),
        Pair(TextTone.SHAKESPEARE, "🎭 Shakespearean Vintage")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tones) { (tone, title) ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.keyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stateHolder.transformCurrentText(tone) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = theme.keyContent
                    )
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Apply Tone",
                        tint = theme.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaseConverterSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    val sample = stateHolder.textFieldValue.text.ifBlank { "hello world glitch keyboard" }
    val cases = listOf(
        Pair(CaseType.UPPERCASE, "UPPERCASE"),
        Pair(CaseType.LOWERCASE, "lowercase"),
        Pair(CaseType.TITLE_CASE, "Title Case"),
        Pair(CaseType.CAMEL_CASE, "camelCase"),
        Pair(CaseType.PASCAL_CASE, "PascalCase"),
        Pair(CaseType.SNAKE_CASE, "snake_case"),
        Pair(CaseType.KEBAB_CASE, "kebab-case"),
        Pair(CaseType.SPONGEBOB_MOCKING, "sPoNgEbOb MoCkInG"),
        Pair(CaseType.REVERSE, "esreveR")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(cases) { (caseType, name) ->
            val preview = KeyboardTextTransformers.convertCase(sample, caseType)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.keyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stateHolder.convertCurrentCase(caseType) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primaryAccent
                        )
                        Text(
                            text = "Tap to apply",
                            fontSize = 10.sp,
                            color = theme.keySubContent
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preview,
                        fontSize = 13.sp,
                        color = theme.keyContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FancyFontsSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    val sample = stateHolder.textFieldValue.text.ifBlank { "Glitch Power Keyboard" }
    val fonts = listOf(
        Pair(FancyFontStyle.GOTHIC, "𝕲𝖔𝖙𝖍𝖎𝖈 𝕱𝖗𝖆𝖐𝖙𝖚𝖗"),
        Pair(FancyFontStyle.SCRIPT_CURSIVE, "𝒞𝓊𝓇𝓈𝒾𝓋ℯ 𝓢𝓬𝓻𝓲𝓹𝓽"),
        Pair(FancyFontStyle.CIRCLED_BUBBLE, "Ⓢⓘⓡⓒⓛⓔⓓ Ⓑⓤⓑⓑⓛⓔ"),
        Pair(FancyFontStyle.BLACK_BUBBLE, "🅑🅛🅐🅒🅚 🅑🅤🅑🅑🅛🅔"),
        Pair(FancyFontStyle.DOUBLE_STRUCK, "𝔻𝕠𝕦𝕓𝕝𝕖 𝕊𝕥𝕣𝕦𝕔𝕜"),
        Pair(FancyFontStyle.MONOSPACE, "𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎 𝚃𝚎𝚡𝚝"),
        Pair(FancyFontStyle.SQUARE, "🅂🅀🅄🄰🅁🄴 🄵🄾🄽🅃"),
        Pair(FancyFontStyle.STRIKETHROUGH, "S̶t̶r̶i̶k̶e̶t̶h̶r̶o̶u̶g̶h̶"),
        Pair(FancyFontStyle.UNDERLINE, "U̲n̲d̲e̲r̲l̲i̲n̲e̲"),
        Pair(FancyFontStyle.UPSIDE_DOWN, "uʍoᗡ ǝpısd∩")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(fonts) { (style, name) ->
            val preview = KeyboardTextTransformers.applyFancyFont(sample, style)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.keyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stateHolder.applyFancyFontToCurrent(style) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.keySubContent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preview,
                            fontSize = 14.sp,
                            color = theme.keyContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Apply",
                        tint = theme.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartRepliesSection(
    stateHolder: KeyboardStateHolder,
    theme: KeyboardThemeColors
) {
    val replies = listOf(
        "👍 Sounds great! Let's proceed with this.",
        "🤝 Acknowledged. I'll get back to you with an update shortly.",
        "⏰ I am currently in a meeting, will ping you soon.",
        "🙏 Thank you so much for the quick help and support!",
        "🚀 Looks fantastic, super well done!",
        "🔥 Absolutely incredible work, keep it up!",
        "❌ Unfortunately, I won't be able to make it this time.",
        "📍 Please share the location or meeting link.",
        "📞 Can we jump on a quick 5-minute call?",
        "✅ Checked and confirmed from my end."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(replies) { reply ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = theme.keyBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        stateHolder.insertText(reply)
                        stateHolder.switchPanel(ActivePanel.KEYBOARD)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = reply,
                        fontSize = 13.sp,
                        color = theme.keyContent,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Reply",
                        tint = theme.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
