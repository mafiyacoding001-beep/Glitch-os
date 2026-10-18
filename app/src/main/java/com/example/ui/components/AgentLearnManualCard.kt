package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Learn component (Agent User Manual) in an expandable dropdown.
 * Explains all aspects of operating the AI Agent from start to end:
 * - Login and account setup
 * - Turning the Agent ON / OFF & Auto-Pilot
 * - Video creation pipeline start-to-end
 * - Keyboard features (1234567890 row, 2-sec symbol press, voice typing, clipboard)
 * - Troubleshooting & system resilience
 */
@Composable
fun AgentLearnManualCard(
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated.copy(alpha = 0.9f))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.6f), ElectricViolet.copy(alpha = 0.6f))),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dropdown Header
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.2f), ElectricViolet.copy(alpha = 0.2f)))
                        )
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Agent Manual",
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LEARN: AGENT MANUAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricViolet.copy(alpha = 0.2f))
                                .border(0.5.dp, ElectricViolet.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "GUIDE",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricViolet
                            )
                        }
                    }
                    Text(
                        text = "एजेंट कैसे चलाएं - ऑटो-पायलट, वीडियो व कीबोर्ड गाइड",
                        fontSize = 9.5.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "CLOSE" else "MANUAL",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse Manual" else "Expand Manual",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Quick overview summary
                Text(
                    text = "इस गाइड में ऐप और एजेंट को सुरक्षित और कुशलतापूर्वक चलाने के सभी नियम और चरण विस्तार से बताए गए हैं।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )

                // 1. YouTube Auto-Pilot on Home Screen (Top Featured)
                ManualSectionItem(
                    stepNumber = "1",
                    icon = Icons.Default.SmartToy,
                    iconTint = CyberCyan,
                    title = "YouTube Auto-Pilot & Hands-Free Engine / मुख्य स्क्रीन पर ऑटो-पायलट",
                    description = "• Instant Access at Top: मुख्य होम स्क्रीन पर सबसे ऊपर 'YOUTUBE AUTO-PILOT' कार्ड दिया गया है, ताकि आप तुरंत 1-टैप में ऑटोमेशन ऑन/ऑफ कर सकें।\n" +
                            "• Niche & Category Selection: अपनी पसंद की केटेगरी (Sci-Fi, Motivation, Crime & Mystery, Tech, Facts, Horror, History, Finance) सीधे चिप्स से चुनें। यह तुरंत पूरे ऐप, स्टूडियो और ऑटो-पायलट में सिंक हो जाती है।\n" +
                            "• Automated Scheduling: अपनी सुविधानुसार अंतराल चुनें (जैसे हर 30 मिनट, 1 घंटा, 2 घंटे या 24 घंटे)। एजेंट बैकग्राउंड में तय समय पर नया वीडियो बनाएगा और सीधे YouTube चैनल पर पब्लिश करेगा।\n" +
                            "• Real-Time Status & Next Run: कार्ड में आपको अगली वीडियो अपलोड का सटीक समय और वर्तमान स्थिति दिखाई देती है।"
                )

                // 2. Full Duration (15s, 30s, 60s) & Voiceover Pipeline
                ManualSectionItem(
                    stepNumber = "2",
                    icon = Icons.Default.Timer,
                    iconTint = NeonGreen,
                    title = "Video Duration & Complete 60s Script / 60 सेकंड वीडियो और वॉइसओवर",
                    description = "• 60s Full Length Generation: जब आप 60 सेकंड चुनते हैं, तो AI स्क्रिप्ट जनरेटर और न्यूरल वॉयस इंजन पूरे 60 सेकंड की समृद्ध कहानी और वॉइसओवर तैयार करता है।\n" +
                            "• Multi-Scene Dynamic Visuals: 60s की वीडियो के लिए AI 15-20 अलग-अलग हाई-डेफिनिशन दृश्य उत्पन्न करता है, जिससे वीडियो कभी भी बीच में नहीं कटती।\n" +
                            "• Subtitle Word Sync: Whisper सबटाइटल इंजन वॉइसओवर की गति के साथ शब्द-दर-शब्द स्क्रीन पर कैप्शन्स रेंडर करता है।"
                )

                // 3. Complete Video Creation Pipeline
                ManualSectionItem(
                    stepNumber = "3",
                    icon = Icons.Default.PlayArrow,
                    iconTint = ElectricViolet,
                    title = "Video Creation Pipeline (Start to End) / मैन्युअल वीडियो निर्माण",
                    description = "• Step A: नीचे दिए गए '+ CREATE VIDEO' बटन पर टैप करें।\n" +
                            "• Step B: अपना पसंदीदा टॉपिक लिखें या ट्रेंडिंग केटेगरी चुनें।\n" +
                            "• Step C: अवधि (15s, 30s, 60s) और आस्पेक्ट रेश्यो (Shorts के लिए 9:16 Portrait, YouTube के लिए 16:9 Landscape) चुनें।\n" +
                            "• Step D: 'GENERATE VIDEO' दबाएं। 6 चरणों का रियल पाइपलाइन चलेगा:\n" +
                            "   1. AI Metadata (वायरल टाइटल, SEO विवरण और स्क्रिप्ट)\n" +
                            "   2. Neural Voiceover (हिंदी/अंग्रेजी प्राकृतिक आवाज़)\n" +
                            "   3. Scene Prompts (हर 3-4 सेकंड के लिए दृश्य)\n" +
                            "   4. AI Art Generation (उच्च गुणवत्ता वाली तस्वीरें)\n" +
                            "   5. Synchronized Subtitles (स्क्रीन पर कैप्शन्स)\n" +
                            "   6. FFmpeg Master Render (अंतिम MP4 वीडियो रेंडर)\n" +
                            "• Step E: रेंडर होने के बाद वीडियो को डाउनलोड करें या सीधे YouTube पर पब्लिश करें।"
                )

                // 4. Glitch Smart Keyboard Guide
                ManualSectionItem(
                    stepNumber = "4",
                    icon = Icons.Default.Keyboard,
                    iconTint = NeonPink,
                    title = "Glitch Smart Keyboard Guide / कीबोर्ड टूल्स और शॉर्टकट",
                    description = "• Dedicated Number Row: अक्षरों के ऊपर 1234567890 की पूरी अलग पंक्ति मौजूद है।\n" +
                            "• 2-Second Long Press for Symbols: किसी भी अक्षर की को 2 सेकंड तक दबाए रखने पर उसका सिंबल (@, #, $, %, &, *, ?) सीधे टाइप हो जाता है।\n" +
                            "• Continuous Spacebar: स्पेसबार दबाकर रखने पर लगातार स्पेस टाइप होते रहेंगे।\n" +
                            "• Direct Enter: बिना किसी रुकावट के सीधे नई लाइन में जाने के लिए एंटर बटन दबाएं।\n" +
                            "• Real Voice Typing: माइक 🎤 आइकन दबाकर हिंदी या अंग्रेजी में बोलकर तेज टाइपिंग करें।\n" +
                            "• Auto Clipboard: कॉपी किया गया कोई भी टेक्स्ट कीबोर्ड के क्लिपबोर्ड पैनल में तुरंत उपलब्ध रहता है।"
                )

                // 5. Account Setup, Privacy & Educational Purpose
                ManualSectionItem(
                    stepNumber = "5",
                    icon = Icons.Default.Security,
                    iconTint = CyberCyan,
                    title = "Privacy, Terms & Educational Purpose / सुरक्षा व शैक्षणिक उद्देश्य",
                    description = "• 100% Privacy First: आपकी API Keys और प्रोजेक्ट्स सुरक्षित रूप से डिवाइस पर एन्क्रिप्टेड रहते हैं।\n" +
                            "• Educational Research Tool: यह ऐप छात्रों और क्रिएटर्स के लिए AI ऑटोमेशन, FFmpeg प्रोसेसिंग और प्रॉम्प्ट इंजीनियरिंग सीखने के उद्देश्य से तैयार किया गया है।\n" +
                            "• YouTube Policy Compliance: स्वचालित अपलोड्स YouTube व Google Play की कम्युनिटी गाइडलाइन्स का पूर्णतः पालन करते हैं।\n" +
                            "• Background 24/7 Engine: बैकग्राउंड में रेंडरिंग चालू रखने के लिए 'Background 24/7 Engine' कार्ड से बैटरी ऑप्टिमाइज़ेशन को Disable रखें।"
                )

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

                // Footer tip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "किसी भी समय इस मैनुअल को खोलने या बंद करने के लिए ऊपर दिए गए हेडर पर टैप करें।",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualSectionItem(
    stepNumber: String,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.6f))
            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.2f))
                    .border(1.dp, iconTint.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )

            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = description,
            fontSize = 10.5.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
    }
}
