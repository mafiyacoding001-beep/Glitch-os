package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class DnsPreset(
    val name: String,
    val hostname: String,
    val description: String,
    val providerColor: Color
)

data class TrackerDomainTestResult(
    val domain: String,
    val category: String,
    var isBlocked: Boolean = false,
    var latencyMs: Long = -1,
    var statusMessage: String = "Not Tested"
)

@Composable
fun PrivateDnsShieldCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val dnsPresets = remember {
        listOf(
            DnsPreset("AdGuard DNS", "dns.adguard-dns.com", "Default #1 for blocking ads, popups & trackers", NeonGreen),
            DnsPreset("NextDNS", "dns.nextdns.io", "Customizable AI ad-blocking & privacy shield", CyberCyan),
            DnsPreset("Cloudflare Security", "security.cloudflare-dns.com", "Blocks malware, phishing & ad trackers", ElectricViolet),
            DnsPreset("Control D", "p2.freedns.controld.com", "Filters ads, telemetry & popups", Color(0xFFFFD700))
        )
    }

    var selectedPreset by remember { mutableStateOf(dnsPresets[0]) }
    var isTestingActive by remember { mutableStateOf(false) }
    var overallDnsProtected by remember { mutableStateOf<Boolean?>(null) }

    val trackerDomains = remember {
        mutableStateListOf(
            TrackerDomainTestResult("pagead2.googlesyndication.com", "Google Ad Network"),
            TrackerDomainTestResult("ad.doubleclick.net", "DoubleClick Ads"),
            TrackerDomainTestResult("graph.facebook.com", "Facebook Tracker"),
            TrackerDomainTestResult("aax.amazon-adsystem.com", "Amazon Ad System"),
            TrackerDomainTestResult("ads.unity3d.com", "Unity Game Ads"),
            TrackerDomainTestResult("ads.applovin.com", "AppLovin Ads")
        )
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to Clipboard: $text", Toast.LENGTH_SHORT).show()
    }

    fun openPrivateDnsSettings() {
        try {
            val intent = Intent("android.settings.NETWORK_OPERATOR_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Open Settings ➔ Network ➔ Private DNS", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun runDnsDiagnosticTest() {
        coroutineScope.launch {
            isTestingActive = true
            var blockedCount = 0

            withContext(Dispatchers.IO) {
                for (i in trackerDomains.indices) {
                    val domainItem = trackerDomains[i]
                    val startTime = System.currentTimeMillis()
                    try {
                        val address = InetAddress.getByName(domainItem.domain)
                        val endTime = System.currentTimeMillis()
                        val ip = address.hostAddress ?: ""

                        // If address resolves to 0.0.0.0 or 127.0.0.1 or invalid IP -> Blocked by DNS
                        if (ip == "0.0.0.0" || ip == "127.0.0.1" || ip.isEmpty()) {
                            trackerDomains[i] = domainItem.copy(
                                isBlocked = true,
                                latencyMs = endTime - startTime,
                                statusMessage = "🛡️ Blocked (Sinkholed)"
                            )
                            blockedCount++
                        } else {
                            trackerDomains[i] = domainItem.copy(
                                isBlocked = false,
                                latencyMs = endTime - startTime,
                                statusMessage = "⚠️ Accessible ($ip)"
                            )
                        }
                    } catch (e: Exception) {
                        // Connection refused or unknown host -> Domain blocked by Private DNS!
                        val endTime = System.currentTimeMillis()
                        trackerDomains[i] = domainItem.copy(
                            isBlocked = true,
                            latencyMs = endTime - startTime,
                            statusMessage = "🛡️ Blocked (DNS Filtered)"
                        )
                        blockedCount++
                    }
                }
            }

            overallDnsProtected = blockedCount > 0
            isTestingActive = false
            Toast.makeText(
                context,
                if (blockedCount > 0) "🛡️ DNS Diagnostic Complete: $blockedCount Ad Domains Filtered!" else "⚠️ DNS Test Complete: Set Private DNS to enable network blocking.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    GlitchCard(
        borderColor = if (overallDnsProtected == true) NeonGreen else CyberCyan.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
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
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = "DNS Shield",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "PRIVATE DNS TRACKER SHIELD",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            maxLines = 1
                        )
                        Text(
                            text = "Network-Level Ad & Tracker Blocker",
                            fontSize = 9.5.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = if (overallDnsProtected == true) NeonGreen.copy(alpha = 0.15f) else Color(0xFFFFD700).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (overallDnsProtected == true) NeonGreen.copy(alpha = 0.5f) else Color(0xFFFFD700).copy(alpha = 0.5f)
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
                                .background(if (overallDnsProtected == true) NeonGreen else Color(0xFFFFD700))
                        )
                        Text(
                            text = if (overallDnsProtected == true) "DNS SHIELD ACTIVE" else "READY TO CONFIGURE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overallDnsProtected == true) NeonGreen else Color(0xFFFFD700)
                        )
                    }
                }
            }

            // Setup Guide Banner
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1-Tap Private DNS Configuration",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Select a Private DNS provider below, copy the hostname, and paste it in Android Private DNS settings to block ads across all apps & websites at network level.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )

                    // Presets Grid
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        dnsPresets.forEach { preset ->
                            val isSelected = selectedPreset.name == preset.name
                            Surface(
                                color = if (isSelected) preset.providerColor.copy(alpha = 0.15f) else DarkSurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) preset.providerColor else DarkBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPreset = preset }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = preset.providerColor
                                        )
                                        Text(
                                            text = preset.hostname,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = preset.description,
                                            fontSize = 8.5.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { copyToClipboard(preset.hostname, preset.name) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy DNS Hostname",
                                            tint = preset.providerColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { copyToClipboard(selectedPreset.hostname, selectedPreset.name) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "COPY DNS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { openPrivateDnsSettings() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(36.dp)
                                .testTag("btn_open_private_dns_settings")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "OPEN DNS SETTINGS", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // Tracker Domain Test Dashboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📡 Domain Tracker Shield Diagnostic",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { runDnsDiagnosticTest() },
                        enabled = !isTestingActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        if (isTestingActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isTestingActive) "TESTING..." else "RUN TEST", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Domain Test Results Table
                trackerDomains.forEach { test ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = test.domain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = test.category,
                                fontSize = 8.5.sp,
                                color = TextSecondary
                            )
                        }

                        Surface(
                            color = if (test.isBlocked) NeonGreen.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = test.statusMessage,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (test.isBlocked) NeonGreen else Color(0xFFFFD700),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
