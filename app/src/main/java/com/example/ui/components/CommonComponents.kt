package com.example.ui.components

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun GlitchCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkBorder,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .clip(RoundedCornerShape(14.dp))
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(CardGradientStart, CardGradientEnd)
            )
        )
        .border(1.dp, borderColor, RoundedCornerShape(14.dp))

    val finalModifier = if (onClick != null) {
        baseModifier.clickable { onClick() }
    } else {
        baseModifier
    }

    Column(
        modifier = finalModifier.padding(contentPadding),
        content = content
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (status.lowercase()) {
        "completed" -> Triple(NeonGreen, Icons.Default.CheckCircle, "Ready")
        "processing", "running" -> Triple(CyberCyan, Icons.Default.Info, "Generating")
        "failed", "error" -> Triple(ErrorRed, Icons.Default.Error, "Failed")
        "queued", "pending" -> Triple(WarningAmber, Icons.Default.Warning, "Queued")
        else -> Triple(TextSecondary, Icons.Default.Info, status.capitalize())
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    color: Color = CyberCyan
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = DarkBackground,
            disabledContainerColor = DarkSurfaceElevated,
            disabledContentColor = TextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StepIndicator(
    stepName: String,
    isActive: Boolean,
    isCompleted: Boolean,
    stepNumber: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val circleColor = when {
        isCompleted -> NeonGreen
        isActive -> CyberCyan.copy(alpha = glowAlpha)
        else -> DarkBorder
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(circleColor.copy(alpha = 0.2f))
                .border(1.5.dp, circleColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    color = if (isActive) CyberCyan else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = stepName,
            color = when {
                isCompleted -> TextPrimary
                isActive -> CyberCyan
                else -> TextSecondary
            },
            fontSize = 13.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WarningNoticeBanner(
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    var isSpeaking by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val languages = remember {
        listOf(
            "English" to Locale.US,
            "Hindi" to Locale("hi", "IN"),
            "Spanish" to Locale("es", "ES"),
            "French" to Locale("fr", "FR"),
            "German" to Locale("de", "DE")
        )
    }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine = tts
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val speakWarning = {
        val targetLocale = languages.firstOrNull { it.first == selectedLanguage }?.second ?: Locale.US
        val tts = ttsEngine
        if (tts != null) {
            if (tts.isSpeaking) {
                tts.stop()
                isSpeaking = false
            } else {
                tts.language = targetLocale
                val textToSpeak = when (selectedLanguage) {
                    "Hindi" -> "चेतावनी: यह ऐप व्यावसायिक उपयोग के लिए नहीं है और केवल शैक्षणिक उद्देश्यों के लिए है। इस ऐप को आज़माने के लिए वीपीएन का उपयोग करना अनुशंसित है।"
                    "Spanish" -> "Advertencia: Esta aplicación no es para uso comercial y es solo para fines educativos. Se recomienda usar una VPN para probar esta aplicación."
                    "French" -> "Avertissement: Cette application n'est pas destinée à un usage commercial et est uniquement à des fins éducatives. Il est recommandé d'utiliser un VPN pour essayer cette application."
                    "German" -> "Warnung: Diese App ist nicht für die kommerzielle Nutzung bestimmt und dient nur zu Bildungszwecken. Es wird empfohlen, ein VPN zu verwenden, um diese App auszuprobieren."
                    else -> "Warning: This app is not for commercial use and is intended strictly for educational purposes. Using a VPN is highly recommended to try this app."
                }
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "WarningSpeechId")
                isSpeaking = true
            }
        } else {
            Toast.makeText(context, "Text to speech engine is initializing...", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated.copy(alpha = 0.85f))
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Warning Icon + Title Dropdown Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning Disclaimer",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "WARNING & DISCLAIMER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = "चेतावनी सूचना (शैक्षणिक उद्देश्य / VPN अनुशंसित)",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isExpanded) "CLOSE" else "OPEN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse Warning" else "Expand Warning",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // English Disclaimer Card
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ENGLISH:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Text(
                        text = "• This app is NOT for commercial use.\n• It is built strictly for educational purposes only.\n• Using a VPN is highly recommended to try and use this app.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Hindi Disclaimer Card
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "HINDI / हिंदी:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Text(
                        text = "• यह ऐप व्यावसायिक उपयोग (Commercial Use) के लिए नहीं है।\n• इसे सिर्फ शैक्षणिक उद्देश्यों (Educational Purpose) के लिए बनाया गया है।\n• इस ऐप को ट्राई (Try) करने के लिए VPN का उपयोग करना अनुशंसित (Recommended) है।",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 15.sp
                    )
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Voice Button with Language Selection Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Voice Button
                        Button(
                            onClick = { speakWarning() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeaking) NeonPink else Color(0xFFFFD700),
                                contentColor = DarkBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Voice Reader",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) "STOP" else "VOICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Language Select Dropdown ahead of Voice Button
                        Box {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFFFFD700), CyberCyan))
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = selectedLanguage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Language",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(DarkSurfaceElevated)
                            ) {
                                languages.forEach { (langName, _) ->
                                    DropdownMenuItem(
                                        text = { Text(langName, color = TextPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            selectedLanguage = langName
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Listen Warnings",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
