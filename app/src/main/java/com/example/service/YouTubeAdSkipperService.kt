package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class YouTubeAdSkipperService : AccessibilityService() {

    companion object {
        private const val TAG = "AdSkipperService"
        private const val PREFS_NAME = "ad_skipper_prefs"
        private const val KEY_TOTAL_SKIPPED = "total_ads_skipped"
        private const val KEY_STATS_VERSION = "skip_action_stats_version"
        private const val KEY_AUTO_MUTE = "auto_mute_ads"
        private const val KEY_TURBO_MODE = "turbo_mode"
        private const val KEY_SHORTS_AUTO_SKIP = "shorts_auto_skip"
        private const val KEY_YOUTUBE_MUSIC_AUTO_SKIP = "youtube_music_auto_skip"
        private const val KEY_INSTAGRAM_AUTO_SKIP = "instagram_auto_skip"
        private const val KEY_FACEBOOK_AUTO_SKIP = "facebook_auto_skip"
        private const val KEY_WEB_BROWSER_AUTO_SKIP = "web_browser_auto_skip"

        // State flows for real-time UI synchronization
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _totalAdsSkipped = MutableStateFlow(0)
        val totalAdsSkipped: StateFlow<Int> = _totalAdsSkipped.asStateFlow()

        private val _lastSkippedTime = MutableStateFlow("")
        val lastSkippedTime: StateFlow<String> = _lastSkippedTime.asStateFlow()

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${YouTubeAdSkipperService::class.java.name}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                    componentName.endsWith(YouTubeAdSkipperService::class.java.simpleName)
                ) {
                    return true
                }
            }
            return false
        }

        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun loadStats(context: Context) {
            val prefs = getPrefs(context)
            if (prefs.getInt(KEY_STATS_VERSION, 0) < 1) {
                prefs.edit()
                    .putInt(KEY_TOTAL_SKIPPED, 0)
                    .remove("total_seconds_saved")
                    .putInt(KEY_STATS_VERSION, 1)
                    .apply()
            }
            _totalAdsSkipped.value = prefs.getInt(KEY_TOTAL_SKIPPED, 0)
        }

        fun isAutoMuteEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_AUTO_MUTE, true)
        }

        fun setAutoMuteEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_AUTO_MUTE, enabled).apply()
        }

        fun isTurboModeEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_TURBO_MODE, true)
        }

        fun setTurboModeEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_TURBO_MODE, enabled).apply()
        }

        fun isShortsAutoSkipEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_SHORTS_AUTO_SKIP, true)
        }

        fun setShortsAutoSkipEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_SHORTS_AUTO_SKIP, enabled).apply()
        }

        fun isYouTubeMusicAutoSkipEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_YOUTUBE_MUSIC_AUTO_SKIP, true)
        }

        fun setYouTubeMusicAutoSkipEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_YOUTUBE_MUSIC_AUTO_SKIP, enabled).apply()
        }

        fun isInstagramAutoSkipEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_INSTAGRAM_AUTO_SKIP, true)
        }

        fun setInstagramAutoSkipEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_INSTAGRAM_AUTO_SKIP, enabled).apply()
        }

        fun isFacebookAutoSkipEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_FACEBOOK_AUTO_SKIP, true)
        }

        fun setFacebookAutoSkipEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_FACEBOOK_AUTO_SKIP, enabled).apply()
        }

        fun isWebBrowserAutoSkipEnabled(context: Context): Boolean {
            return getPrefs(context).getBoolean(KEY_WEB_BROWSER_AUTO_SKIP, true)
        }

        fun setWebBrowserAutoSkipEnabled(context: Context, enabled: Boolean) {
            getPrefs(context).edit().putBoolean(KEY_WEB_BROWSER_AUTO_SKIP, enabled).apply()
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    private var audioManager: AudioManager? = null
    private var originalVolume: Int = -1
    private var isCurrentlyMuted = false
    private var lastClickTimestamp: Long = 0L

    // Known Resource IDs for Skip Buttons in YouTube
    private val skipButtonResourceIds = listOf(
        "com.google.android.youtube:id/skip_ad_button",
        "com.google.android.youtube:id/skip_ad_button_text",
        "com.google.android.youtube:id/ad_skip_button",
        "com.google.android.youtube:id/ad_skip_button_compact",
        "com.google.android.youtube:id/modern_skip_ad_button",
        "com.google.android.youtube:id/player_overlay_skip_ad_button",
        "skip_ad_button",
        "skip_button"
    )

    // Known keywords across multiple languages
    private val skipKeywords = listOf(
        "skip ad",
        "skip ads",
        "skip in",
        "überspringen",
        "saltar",
        "passer",
        "pular",
        "пропустить",
        "skip",
        "विज्ञापन छोड़ें"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceActive.value = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        loadStats(this)
        Log.d(TAG, "⚡ Multi-Platform Ad Shield & Macro Engine Connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        if (isCurrentlyMuted) {
            restoreAudioVolume()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AdSkipperService Interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val isYouTubeMusic = packageName.contains("youtube.music") || packageName.contains("apps.youtube.music")
        val isYouTube = packageName.contains("youtube")
        val isInstagram = packageName.contains("instagram")
        val isFacebook = packageName.contains("facebook") || packageName.contains("katana")
        val isWebBrowser = packageName.contains("chrome") || packageName.contains("brave") ||
                packageName.contains("firefox") || packageName.contains("opera") ||
                packageName.contains("sbrowser") || packageName.contains("browser") ||
                packageName.contains("emmx")

        if (!isYouTube && !isInstagram && !isFacebook && !isYouTubeMusic && !isWebBrowser) {
            // Restore audio if user leaves YouTube/Music while muted
            if (isCurrentlyMuted) {
                restoreAudioVolume()
            }
            return
        }

        val rootNode = rootInActiveWindow ?: event.source ?: return
        try {
            if (isWebBrowser && isWebBrowserAutoSkipEnabled(this)) {
                processWebBrowserAd(rootNode)
            } else if (isYouTubeMusic && isYouTubeMusicAutoSkipEnabled(this)) {
                processYouTubeMusicRootNode(rootNode)
            } else if (isYouTube) {
                processYouTubeRootNode(rootNode)
            } else if (isInstagram && isInstagramAutoSkipEnabled(this)) {
                processSocialMediaAd(rootNode, "Instagram")
                if (event.source != null && event.source != rootNode) {
                    processSocialMediaAd(event.source!!, "Instagram")
                }
            } else if (isFacebook && isFacebookAutoSkipEnabled(this)) {
                processSocialMediaAd(rootNode, "Facebook")
                if (event.source != null && event.source != rootNode) {
                    processSocialMediaAd(event.source!!, "Facebook")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility node: ${e.message}")
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Recycle is handled automatically in modern Android
                } else {
                    @Suppress("DEPRECATION")
                    rootNode.recycle()
                }
            } catch (_: Exception) {}
        }
    }

    private fun processWebBrowserAd(rootNode: AccessibilityNodeInfo) {
        // 1. Web YouTube Video Skip Ad Buttons (m.youtube.com in Chrome/Brave/Firefox)
        val webSkipKeywords = listOf(
            "skip ad", "skip ads", "skip", "ytp-ad-skip-button", "ytp-ad-skip-button-modern", "विज्ञापन छोड़ें"
        )
        for (keyword in webSkipKeywords) {
            val textNodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!textNodes.isNullOrEmpty()) {
                for (node in textNodes) {
                    if (attemptClickOnNode(node)) {
                        Log.d(TAG, "⚡ Web Browser YouTube Skip Ad Clicked!")
                        onSkipActionSucceeded()
                        return
                    }
                }
            }
        }

        // 2. Web Banner Ad Close Buttons ('X', "Close ad", "Dismiss", "Close")
        val webCloseKeywords = listOf(
            "close ad", "close advertisement", "dismiss ad", "anzeige schließen", "close_ad", "ad_close", "close"
        )
        for (keyword in webCloseKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
                    val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
                    if (contentDesc.contains("close") || text.contains("close") || text == "x" || contentDesc == "x") {
                        if (attemptClickOnNode(node)) {
                            Log.d(TAG, "⚡ Web Browser Banner Close Button Clicked!")
                            onSkipActionSucceeded()
                            return
                        }
                    }
                }
            }
        }

        // 3. Recursive Tree Scan for Web Close 'X' Overlays
        val webClicked = recursiveScanForWebCloseButton(rootNode)
        if (webClicked) {
            onSkipActionSucceeded()
            return
        }
    }

    private fun recursiveScanForWebCloseButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        val isWebCloseBtn = (text == "x" || contentDesc == "x" || contentDesc.contains("close ad") || viewId.contains("close") || viewId.contains("ad_close")) &&
                (node.isClickable || node.parent?.isClickable == true)

        if (isWebCloseBtn) {
            if (attemptClickOnNode(node)) {
                return true
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val clicked = recursiveScanForWebCloseButton(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (clicked) return true
            }
        }
        return false
    }

    private fun processYouTubeMusicRootNode(rootNode: AccessibilityNodeInfo) {
        // 1. Search for Direct Skip Button in YouTube Music
        val ytMusicSkipIds = listOf(
            "com.google.android.apps.youtube.music:id/skip_ad_button",
            "com.google.android.apps.youtube.music:id/skip_ad_button_text",
            "com.google.android.apps.youtube.music:id/ad_skip_button",
            "skip_ad_button"
        )
        for (resId in ytMusicSkipIds) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByViewId(resId)
            if (!matchingNodes.isNullOrEmpty()) {
                for (node in matchingNodes) {
                    if (attemptClickOnNode(node)) {
                        onSkipActionSucceeded()
                        return
                    }
                }
            }
        }

        // 2. Scan Text-based Skip Keywords
        for (keyword in skipKeywords) {
            val textNodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!textNodes.isNullOrEmpty()) {
                for (node in textNodes) {
                    if (attemptClickOnNode(node)) {
                        onSkipActionSucceeded()
                        return
                    }
                }
            }
        }

        // 3. Check for Audio Ad playing in YT Music to auto-mute
        if (isAutoMuteEnabled(this)) {
            val hasAudioAd = checkForAdPresence(rootNode)
            if (hasAudioAd && !isCurrentlyMuted) {
                muteAudioVolume()
            } else if (!hasAudioAd && isCurrentlyMuted) {
                restoreAudioVolume()
            }
        }
    }

    private fun processYouTubeRootNode(rootNode: AccessibilityNodeInfo) {
        // 1. Search for Direct Skip Button by Resource IDs
        for (resId in skipButtonResourceIds) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByViewId(resId)
            if (!matchingNodes.isNullOrEmpty()) {
                for (node in matchingNodes) {
                    if (attemptClickOnNode(node)) {
                        onSkipActionSucceeded()
                        return
                    }
                }
            }
        }

        // 2. Search for Text-based Skip Buttons
        for (keyword in skipKeywords) {
            val textNodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!textNodes.isNullOrEmpty()) {
                for (node in textNodes) {
                    if (attemptClickOnNode(node)) {
                        onSkipActionSucceeded()
                        return
                    }
                }
            }
        }

        // 3. Recursive Tree Scan for Dynamic / Custom Overlays
        val foundAndClicked = recursiveScanForSkip(rootNode)
        if (foundAndClicked) {
            onSkipActionSucceeded()
            return
        }

        // 4. Check & Auto-Swipe YouTube Shorts Ads
        if (isShortsAutoSkipEnabled(this)) {
            // FIX: Strictly verify user is watching Shorts/Reels and NOT on channel/account page!
            if (!isNonShortsPage(rootNode) && isShortsContainerPresent(rootNode)) {
                val isShortsAd = checkForShortsAdPresence(rootNode)
                if (isShortsAd) {
                    swipeUpToSkipShortsAd(rootNode)
                    return
                }
            }
        }

        // 5. Check for Video In-Stream Ad Presence for Auto-Mute
        // FIX: Ignore companion banner ads below the player to prevent muting actual playing videos
        if (isAutoMuteEnabled(this)) {
            val hasInStreamVideoAd = checkForAdPresence(rootNode)
            if (hasInStreamVideoAd && !isCurrentlyMuted) {
                muteAudioVolume()
            } else if (!hasInStreamVideoAd && isCurrentlyMuted) {
                restoreAudioVolume()
            }
        }
    }

    private fun processSocialMediaAd(rootNode: AccessibilityNodeInfo, platformName: String) {
        if (!isNonShortsPage(rootNode) && isSocialReelContainerPresent(rootNode, platformName)) {
            val isSponsoredAd = checkForSocialMediaSponsoredAd(rootNode)
            if (isSponsoredAd) {
                Log.d(TAG, "⚡ $platformName Sponsored Reel/Feed Ad Detected -> Auto Swiping/Skipping")
                swipeUpToSkipShortsAd(rootNode, platformName)
            }
        }
    }

    private fun isNonShortsPage(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        val nonShortsKeywords = listOf(
            "channel_header", "channel_title", "channel_profile", "account_header",
            "my_account", "google account", "manage account", "channel_avatar",
            "subscribe_button", "channel_settings", "customize channel", "tabs_container",
            "search_edit_text", "settings_list", "switch account", "analytics",
            "manage videos", "library_page", "subscriptions_list", "account_container"
        )

        if (nonShortsKeywords.any { viewId.contains(it) || text.contains(it) || contentDesc.contains(it) }) {
            return true
        }

        val profileTextIndicators = listOf(
            "manage videos", "analytics", "switch account", "google account",
            "channel settings", "customize channel", "add account", "sign out"
        )
        if (profileTextIndicators.any { text == it || contentDesc == it }) {
            return true
        }

        return false
    }

    private fun isShortsContainerPresent(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        val shortsViewIds = listOf(
            "reel_player", "reel_recycler", "shorts_player", "shorts_container",
            "reel_video_view", "reel_active_item", "reel_player_overlay_layout"
        )
        if (shortsViewIds.any { viewId.contains(it) }) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = isShortsContainerPresent(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (found) return true
            }
        }
        return false
    }

    private fun isSocialReelContainerPresent(node: AccessibilityNodeInfo?, platformName: String): Boolean {
        if (node == null) return false
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        val reelViewIds = listOf(
            "clips_video_container", "reels_viewer_container", "reel_viewer",
            "reels_view_pager", "clips_view_pager", "reel_ad", "clips_cta_button",
            "sponsored_label", "sponsored_badge"
        )
        if (reelViewIds.any { viewId.contains(it) }) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = isSocialReelContainerPresent(child, platformName)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (found) return true
            }
        }
        return false
    }

    private fun checkForSocialMediaSponsoredAd(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase(Locale.getDefault())?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault())?.trim() ?: ""
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        // 1. Sponsored & Paid Partnership Text Indicators across languages
        val isSponsoredText = text == "sponsored" || contentDesc == "sponsored" ||
                text.contains("sponsored") || contentDesc.contains("sponsored") ||
                text.contains("paid partnership") || contentDesc.contains("paid partnership") ||
                text.contains("paid promotion") || contentDesc.contains("paid promotion") ||
                text == "promoted" || contentDesc == "promoted" ||
                text == "advertisement" || contentDesc == "advertisement" ||
                text == "ad" || contentDesc == "ad" ||
                text.startsWith("ad ·") || text.startsWith("ad •") ||
                // Multilingual indicators (Hindi, Spanish, French, German, Italian, Russian, etc.)
                text.contains("प्रायोजित") || contentDesc.contains("प्रायोजित") ||
                text.contains("विज्ञापन") || contentDesc.contains("विज्ञापन") ||
                text.contains("patrocinado") || text.contains("patrocinada") ||
                text.contains("sponsorisé") || text.contains("sponsorisée") ||
                text.contains("gesponsert") || text.contains("sponsorizzato") ||
                text.contains("реклама") || text.contains("publicité") ||
                text.contains("publicidad") || text.contains("anzeige") ||
                text.contains("赞助") || text.contains("贊助") || text == "광고"

        // 2. Instagram & Facebook Ad Call-To-Action (CTA) Indicators
        val isSocialCtaText = (
                text == "install now" || text == "shop now" || text == "learn more" ||
                text == "sign up" || text == "book now" || text == "download" ||
                text == "order now" || text == "get offer" || text == "apply now" ||
                text == "contact us" || text == "send message" || text == "get quote" ||
                text == "open app" || text == "play game" || text == "watch more" ||
                text == "subscribe" || text == "visit instagram profile" ||
                text == "visit website" || text == "visit site" ||
                text == "अभी इंस्टॉल करें" || text == "और जानें" || text == "शॉप करें" ||
                text == "अभी ऑर्डर करें" || text == "साइन अप करें" || text == "ऐप इंस्टॉल करें" ||
                contentDesc == "install now" || contentDesc == "shop now" || contentDesc == "learn more"
        ) && (node.isClickable || node.parent?.isClickable == true || viewId.contains("button") || viewId.contains("cta") || viewId.contains("action"))

        // 3. Instagram View Resource IDs for Reels and Feed Ads
        val isSponsoredViewId = viewId.contains("sponsored_label") ||
                viewId.contains("reel_viewer_subtitle") ||
                viewId.contains("clips_cta_button") ||
                viewId.contains("clips_author_container") ||
                viewId.contains("feed_cta_button") ||
                viewId.contains("row_feed_button_cta") ||
                viewId.contains("bottom_sheet_cta_button") ||
                viewId.contains("reel_viewer_tall_cta_button") ||
                viewId.contains("reel_ad") ||
                viewId.contains("sponsored_badge") ||
                viewId.contains("promoted_badge") ||
                viewId.contains("lead_gen") ||
                viewId.contains("organic_lead_gen") ||
                viewId.contains("ad_attribution") ||
                viewId.contains("advertiser") ||
                viewId.contains("carousel_footers") ||
                (viewId.contains("cta_button") && (text.isNotEmpty() || contentDesc.isNotEmpty()))

        if (isSponsoredText || isSocialCtaText || isSponsoredViewId) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val isAd = checkForSocialMediaSponsoredAd(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (isAd) return true
            }
        }
        return false
    }

    private fun recursiveScanForSkip(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        val isSkipText = skipKeywords.any { text.contains(it) || contentDesc.contains(it) }
        val isSkipId = skipButtonResourceIds.any { viewId.contains("skip", ignoreCase = true) }

        if (isSkipText || isSkipId) {
            if (attemptClickOnNode(node)) {
                return true
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val clicked = recursiveScanForSkip(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (clicked) return true
            }
        }
        return false
    }

    private fun checkForAdPresence(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        // BUG FIX: Strictly ignore static companion banner ads below the video player!
        // These static banner views contain "companion", "promoted_header", "banner", or "ad_attribution"
        if (viewId.contains("companion") || viewId.contains("banner") || viewId.contains("promoted_header") || viewId.contains("ad_attribution")) {
            return false
        }

        // Strictly check for IN-STREAM VIDEO ADS playing inside the video player
        val inStreamVideoAdKeywords = listOf(
            "ad 1 of", "ad 2 of", "video will play after ad", "ad · 0:", "ad · 1:",
            "ad_countdown", "ad_progress", "ad_break_banner", "video_ad_overlay",
            "ad_badge", "ad_text_overlay"
        )

        val isInStreamVideoAd = inStreamVideoAdKeywords.any {
            text.contains(it) || contentDesc.contains(it) || viewId.contains(it)
        }

        if (isInStreamVideoAd) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val hasAd = checkForAdPresence(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (hasAd) return true
            }
        }
        return false
    }

    private fun checkForShortsAdPresence(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val text = node.text?.toString()?.lowercase(Locale.getDefault())?.trim() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase(Locale.getDefault())?.trim() ?: ""
        val viewId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        // 1. Direct High-Confidence Sponsored Badge Keywords
        val isSponsoredTag = text == "sponsored" || contentDesc == "sponsored" ||
                text.startsWith("sponsored") || contentDesc.startsWith("sponsored") ||
                text.contains("sponsored ·") || contentDesc.contains("sponsored ·") ||
                text == "promoted" || contentDesc == "promoted" ||
                text == "advertisement" || contentDesc == "advertisement" ||
                text == "ad" || contentDesc == "ad" ||
                text.startsWith("ad ·") || contentDesc.startsWith("ad ·") ||
                text.contains("ad 1 of") || text.contains("ad 2 of") ||
                text.contains("visit advertiser") || contentDesc.contains("visit advertiser")

        // 2. Multilingual & Hindi Sponsored Badges
        val isMultilingualAd = text == "प्रायोजित" || contentDesc == "प्रायोजित" ||
                text == "विज्ञापन" || contentDesc == "विज्ञापन" ||
                text.contains("प्रायोजित") || contentDesc.contains("प्रायोजित") ||
                text == "sponsorisé" || text == "patrocinado" || text == "anzeige"

        // 3. Shorts Ad Call-To-Action (CTA) Button Indicators
        val isShortsAdCta = (text == "shop now" || text == "install" || text == "install now" ||
                text == "learn more" || text == "sign up" || text == "download" ||
                text == "get offer" || text == "order now" || text == "book now" ||
                text == "अभी इंस्टॉल करें" || text == "और जानें" || text == "साइट पर जाएं") &&
                (viewId.contains("button") || viewId.contains("cta") || viewId.contains("badge") || node.isClickable)

        // 4. View Resource IDs for YouTube Shorts & Reels Ad Layouts
        val isAdViewId = viewId.contains("reel_player_ad_badge") ||
                viewId.contains("reel_ad") ||
                viewId.contains("ad_badge") ||
                viewId.contains("sponsored_badge") ||
                viewId.contains("promoted_badge") ||
                viewId.contains("ad_attribution") ||
                viewId.contains("shorts_ad") ||
                viewId.contains("short_video_ad_badge") ||
                viewId.contains("reel_ad_cta_button") ||
                viewId.contains("ad_cta_button") ||
                viewId.contains("ad_headline") ||
                viewId.contains("advertiser_name")

        if (isSponsoredTag || isMultilingualAd || isShortsAdCta || isAdViewId) {
            return true
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val isShortsAd = checkForShortsAdPresence(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (isShortsAd) return true
            }
        }
        return false
    }

    private fun swipeUpToSkipShortsAd(rootNode: AccessibilityNodeInfo? = null, platformName: String = "Shorts") {
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp < 450) return
        lastClickTimestamp = now

        Log.d(TAG, "🚀 $platformName Ad Detected! Executing auto-wipe touch gesture...")

        // 1. Smooth Touch Gesture Auto-Wipe (Calibrated for ViewPager2 / Reels / Shorts Container)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val startX = width * 0.5f
            val startY = height * 0.80f
            val endY = height * 0.16f

            val swipePath = android.graphics.Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }

            val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(swipePath, 0L, 200L)
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(stroke)
                .build()

            dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "⚡ $platformName Ad Auto-Wiped via Smooth Touch Gesture (200ms)!")
                    onSkipActionSucceeded()
                }

                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Gesture cancelled, attempting fallback container scroll...")
                    rootNode?.let { tryScrollContainerForward(it) }
                }
            }, null)
        } else {
            // Fallback for older devices
            rootNode?.let { node ->
                tryScrollContainerForward(node)
            }
        }
    }

    private fun tryScrollContainerForward(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isScrollable) {
            val scrolled = node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (scrolled) {
                Log.d(TAG, "⚡ Reels / Shorts Ad Auto-Swiped via ACTION_SCROLL_FORWARD!")
                onSkipActionSucceeded()
                return true
            }
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val scrolled = tryScrollContainerForward(child)
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                } catch (_: Exception) {}
                if (scrolled) return true
            }
        }
        return false
    }

    private fun attemptClickOnNode(node: AccessibilityNodeInfo): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp < 800) return false

        if (node.isClickable) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) {
                lastClickTimestamp = now
                Log.d(TAG, "⚡ Skip Ad clicked via Node ACTION_CLICK: ${node.text ?: node.viewIdResourceName}")
                return true
            }
        }

        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 4) {
            if (parent.isClickable) {
                val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    lastClickTimestamp = now
                    Log.d(TAG, "⚡ Skip Ad clicked via Parent Node ACTION_CLICK: ${parent.viewIdResourceName}")
                    return true
                }
            }
            parent = parent.parent
            depth++
        }
        return false
    }

    private fun muteAudioVolume() {
        try {
            audioManager?.let { am ->
                if (!isCurrentlyMuted) {
                    originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    }
                    isCurrentlyMuted = true
                    Log.d(TAG, "🔇 Audio Muted during In-Stream Video Ad playback")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mute audio volume: ${e.message}")
        }
    }

    private fun restoreAudioVolume() {
        try {
            audioManager?.let { am ->
                if (isCurrentlyMuted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    } else {
                        if (originalVolume >= 0) {
                            @Suppress("DEPRECATION")
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                        }
                    }
                    isCurrentlyMuted = false
                    Log.d(TAG, "🔊 Audio Volume Restored after In-Stream Video Ad finished")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore audio volume: ${e.message}")
        }
    }

    private fun onSkipActionSucceeded() {
        val context = applicationContext
        val prefs = getPrefs(context)

        val newTotal = prefs.getInt(KEY_TOTAL_SKIPPED, 0) + 1

        prefs.edit()
            .putInt(KEY_TOTAL_SKIPPED, newTotal)
            .apply()

        _totalAdsSkipped.value = newTotal

        val now = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
        _lastSkippedTime.value = now
    }
}
