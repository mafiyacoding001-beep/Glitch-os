package com.example.util

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChannelSeoData(
    val category: String,
    val suggestedChannelName: String,
    val handle: String,
    val tagline: String,
    val optimizedDescription: String,
    val searchKeywords: List<String>,
    val uploadSchedule: String,
    val bannerPrompt: String,
    val avatarPrompt: String,
    val hashtags: List<String>
)

/**
 * Phase 1: AI Channel SEO & Branding Optimization Engine.
 * Analyzes channel niche and provides 100% search-optimized channel metadata.
 */
object YouTubeChannelSeoManager {
    private const val TAG = "YouTubeChannelSeoManager"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun generateChannelSeo(
        category: String,
        currentChannelName: String? = null,
        currentHandle: String? = null
    ): ChannelSeoData {
        val name = if (!currentChannelName.isNullOrBlank() && currentChannelName != "YouTube Channel" && currentChannelName != "YouTube Creator") {
            currentChannelName
        } else {
            getDefaultChannelNameForCategory(category)
        }

        val handle = if (!currentHandle.isNullOrBlank() && currentHandle != "@") {
            currentHandle
        } else {
            "@" + name.replace(" ", "").replace("&", "And").lowercase()
        }

        return when (category) {
            "Horror & Thriller" -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "Uncovering the Darkest Paranormal Mysteries & Bone-Chilling Stories 👻",
                optimizedDescription = """
                    Welcome to $name — Your ultimate destination for unexplained mysteries, true dark folklore, and spine-chilling horror stories! 🕯️💀

                    ⚡ WHAT WE CREATE:
                    • True Paranormal Encounters & Dark Legends
                    • Unsolved Supernatural Phenomena & Haunted Mysteries
                    • Immersive Visual Storytelling & Cinematic Shorts

                    📅 UPLOAD SCHEDULE:
                    New Viral Shorts Every Day at 6:00 PM (Daily Spooky Drops)!

                    🔔 Subscribe now & turn on all notifications if you dare to explore the unknown!
                    
                    📧 Business Inquiries / Collabs: contact.${handle.removePrefix("@")}@gmail.com

                    #Horror #ScaryStories #Shorts #Supernatural #TrueParanormal #Spooky #CreepyPasta
                """.trimIndent(),
                searchKeywords = listOf(
                    "horror stories", "scary shorts", "paranormal mysteries", "creepy facts", "unsolved horror",
                    "ghost encounters", "dark legends", "chilling tales", "urban legends", "supernatural caught on tape",
                    "horror documentary shorts", "spooky stories in hindi", "nightmare stories", "scariest places on earth",
                    "dark web mysteries", "true crime horror", "cursed objects", "mythological monsters", "unexplained phenomena"
                ),
                uploadSchedule = "Daily at 6:00 PM",
                bannerPrompt = "Dark eerie haunted Victorian mansion in midnight fog with glowing ghostly moon, cinematic 8k ultra-wide banner, ominous gothic atmosphere, neon crimson accents",
                avatarPrompt = "Minimalist glowing phantom neon skull emblem, dark cosmic nebula background, sharp cyber horror logo icon, vector art",
                hashtags = listOf("#Horror", "#ScaryStories", "#Paranormal", "#Shorts", "#Creepy", "#Supernatural", "#DarkTales")
            )

            "AI & Futuristic" -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "The Frontier of Artificial Intelligence, AGI & Quantum Futures 🤖⚡",
                optimizedDescription = """
                    Welcome to $name! We decode the rapid revolution of Artificial Intelligence, futuristic technologies, and autonomous innovations shaping humanity. 🚀

                    🧠 WHAT WE COVER:
                    • Breakthroughs in AI Models, LLMs & Autonomous Robotics
                    • Next-Gen Futuristic Gadgets & Sci-Fi Realities
                    • Quantum Computing, Cybernetics & Tech Predictions

                    📅 UPLOAD SCHEDULE:
                    Daily AI Insights & Deep Dives at 9:00 AM & 7:00 PM!

                    🔔 Subscribe to stay 10 steps ahead of the AI Revolution!
                    
                    📧 Inquiries: business.${handle.removePrefix("@")}@gmail.com

                    #AI #ArtificialIntelligence #TechShorts #FutureTech #GenerativeAI #Robotics #CyberTech
                """.trimIndent(),
                searchKeywords = listOf(
                    "artificial intelligence", "ai news", "chatgpt updates", "future technology", "autonomous robots",
                    "machine learning", "ai tools 2026", "deepfake tech", "neural networks", "quantum computing",
                    "ai video generation", "humanoid robotics", "agi timeline", "cyberpunk future", "tech breakthroughs"
                ),
                uploadSchedule = "Daily at 9:00 AM & 7:00 PM",
                bannerPrompt = "Futuristic neon cybernetic brain neural matrix with glowing blue and cyan fiber-optic data streams, 8k ultra-wide sci-fi banner, sleek holographic design",
                avatarPrompt = "Glowing cybernetic robotic eye with neon cyan circuitry, metallic obsidian sphere, modern tech logo, high-tech emblem",
                hashtags = listOf("#AI", "#ArtificialIntelligence", "#FutureTech", "#TechShorts", "#GenerativeAI", "#AGI")
            )

            "Mystery & Crime" -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "Investigating Unsolved Secrets, Cold Cases & Deep Truths 🔍🕵️",
                optimizedDescription = """
                    Welcome to $name — Digging deep into history's most baffling mysteries, unsolved true crime cases, and bizarre secrets hidden from the world. 🕵️‍♂️📂

                    🔎 ON THIS CHANNEL:
                    • Unsolved Cold Cases & Missing Person Mysteries
                    • Historical Conspiracies & Declassified Government Files
                    • Forensic Breakthroughs & Mastermind Heists

                    📅 NEW EPISODES: Daily at 5:30 PM!
                    
                    🔔 Subscribe and help us uncover the truth behind the shadows!

                    #TrueCrime #Mystery #Unsolved #ColdCase #Investigation #DocumentaryShorts
                """.trimIndent(),
                searchKeywords = listOf(
                    "true crime shorts", "unsolved mysteries", "cold cases documentary", "mysterious vanishings",
                    "detective investigation", "crime files", "bizarre occurrences", "conspiracy theories",
                    "historical mysteries", "forensic science", "mastermind criminal cases", "dark secrets"
                ),
                uploadSchedule = "Daily at 5:30 PM",
                bannerPrompt = "Moody detective corkboard with red connecting strings, vintage magnifying glass, dark noir lighting, ultra-wide 8k resolution cinematic banner",
                avatarPrompt = "Minimalist noir detective silhouette inside a luminous golden magnifying glass, dark vintage texture, sleek vector icon",
                hashtags = listOf("#TrueCrime", "#Mystery", "#Unsolved", "#ColdCase", "#CrimeShorts")
            )

            "Motivation & Facts" -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "Mastering Mindset, Unstoppable Discipline & Shocking Life Facts 🧠🔥",
                optimizedDescription = """
                    Welcome to $name! Fuel your mind with elite discipline, stoic psychology, high-performance habits, and mind-expanding human facts. ⚡🏆

                    🔥 WHAT YOU'LL GET:
                    • Powerful Stoic Wisdom & Mindset Hacks
                    • Psychological Facts & High-Performance Principles
                    • Daily Inspiration to Level Up Your Life

                    ⏰ DAILY FUEL: Every Morning at 7:00 AM!
                    
                    🎯 Subscribe to become 1% better every single day.

                    #Motivation #Mindset #Discipline #PsychologyFacts #Stoicism #Success #SelfImprovement
                """.trimIndent(),
                searchKeywords = listOf(
                    "daily motivation", "mindset shorts", "stoic quotes", "discipline over motivation",
                    "psychology facts", "success habits", "human behavior facts", "high value mindset",
                    "inspirational speeches", "morning routine wisdom", "self mastery", "life advice"
                ),
                uploadSchedule = "Daily at 7:00 AM & 8:00 PM",
                bannerPrompt = "Golden glowing lion emblem with geometric sacred architecture, rising morning sun over mountain peaks, inspirational cinematic 8k banner",
                avatarPrompt = "Golden geometric roaring lion head logo, matte black background, luxury minimalist vector emblem",
                hashtags = listOf("#Motivation", "#Mindset", "#Discipline", "#Stoic", "#Facts", "#Shorts")
            )

            "Science" -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "Exploring Quantum Paradoxes, Deep Space & Scientific Wonders 🌌🔬",
                optimizedDescription = """
                    Welcome to $name — Your portal to the wonders of astrophysics, quantum mechanics, biology, and the deepest secrets of our universe! 🔭✨

                    🚀 DISCOVER:
                    • Black Holes, Supernovas & Deep Space Missions
                    • Mind-Bending Quantum Paradoxes & Physics Experiments
                    • Futuristic Evolutionary Science & Nano-Technology

                    📅 UPLOADS: Daily at 4:00 PM!
                    
                    🔔 Subscribe to feed your curious mind!

                    #Science #Cosmos #Astronomy #Physics #Quantum #SpaceExploration #ScientificFacts
                """.trimIndent(),
                searchKeywords = listOf(
                    "science shorts", "astronomy facts", "black hole mysteries", "quantum physics explained",
                    "space exploration", "james webb telescope", "universe wonders", "physics experiments",
                    "biology secrets", "scientific discoveries", "cosmology shorts"
                ),
                uploadSchedule = "Daily at 4:00 PM",
                bannerPrompt = "Epic spiral galaxy with glowing nebulae and orbiting ringed planet, James Webb style deep space view, ultra-wide 8k cosmic banner",
                avatarPrompt = "Stylized glowing atomic structure orbiting a vibrant planetary core, neon electric purple and cyan palette, clean vector icon",
                hashtags = listOf("#Science", "#Space", "#Physics", "#Cosmos", "#Astronomy", "#Shorts")
            )

            "Business & Finance" -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "Wealth Building, Startup Case Studies & Financial Freedom 💰📈",
                optimizedDescription = """
                    Welcome to $name! Master the game of money, startup strategies, investing insights, and financial independence. 💼📊

                    📈 WHAT WE DECODE:
                    • Billionaire Wealth Strategies & Startup Breakdown
                    • Smart Investing, Real Estate & Crypto Insights
                    • Economic Trends & Side-Hustle Blueprints

                    ⏰ DAILY DROPS: 11:00 AM & 6:30 PM!
                    
                    🔔 Subscribe & build generational wealth with us.

                    #Business #Finance #Investing #MoneyMindset #Startups #WealthBuilding #SideHustle
                """.trimIndent(),
                searchKeywords = listOf(
                    "business shorts", "finance tips", "how to invest", "billionaire mindset", "startup stories",
                    "stock market basics", "money making ideas", "passive income", "economic breakdown", "wealth creation"
                ),
                uploadSchedule = "Daily at 11:00 AM & 6:30 PM",
                bannerPrompt = "Sleek wall street financial district skyline at dusk with glowing neon candlestick charts in the sky, 8k luxury corporate banner",
                avatarPrompt = "Minimalist golden geometric bull and upward trajectory arrow emblem, matte emerald green and gold luxury badge",
                hashtags = listOf("#Business", "#Finance #Wealth", "#Investing", "#Startups", "#Shorts")
            )

            else -> ChannelSeoData(
                category = category,
                suggestedChannelName = name,
                handle = handle,
                tagline = "High-Impact $category Stories, Insights & Viral Breakthroughs 🎬🚀",
                optimizedDescription = """
                    Welcome to $name — Delivering daily viral insights, visual storytelling, and trending breakthroughs in the $category niche! ✨

                    🎯 ON THIS CHANNEL:
                    • Top Highlights & Trending Secrets in $category
                    • Fast-Paced Cinematic Shorts & Breakdown
                    • High-Engagement Community Discussions

                    📅 NEW CONTENT DAILY!
                    
                    🔔 Subscribe and join thousands of curious creators.

                    #$category #Shorts #Trending #Viral #Highlights
                """.trimIndent(),
                searchKeywords = listOf(
                    "$category shorts", "trending $category", "top $category facts", "viral shorts",
                    "best $category videos", "expert insights", "$category 2026", "breakthrough moments"
                ),
                uploadSchedule = "Daily at 6:00 PM",
                bannerPrompt = "Dynamic modern cinematic abstract digital art in $category style, 8k ultra-wide neon aesthetic banner",
                avatarPrompt = "Modern high-contrast glowing geometric monogram for $name, dark cyber background, vector icon",
                hashtags = listOf("#$category", "#Shorts", "#Trending", "#Viral")
            )
        }
    }

    private fun getDefaultChannelNameForCategory(category: String): String {
        return when (category) {
            "Horror & Thriller" -> "Dark Horizon Tales"
            "AI & Futuristic" -> "CyberMind AI"
            "Mystery & Crime" -> "Unsolved Case Files"
            "Motivation & Facts" -> "Apex Mindset"
            "Science" -> "Cosmic Spectrum"
            "Business & Finance" -> "Wealth Blueprint"
            "Gaming" -> "Pixel Realm"
            "History & Culture" -> "Chronicles of Time"
            "Entertainment" -> "Pop Culture Vault"
            else -> "Nexus Tech Studio"
        }
    }

    /**
     * Generates live AI Channel Branding SEO using Notrack engine with MasterPrompt and JSON parsing.
     */
    suspend fun generateChannelSeoWithNotrack(
        context: Context,
        notrack: com.example.pipeline.NotrackWebViewClient? = null,
        category: String,
        currentChannelName: String? = null,
        currentHandle: String? = null,
        subscriberCount: String? = null
    ): ChannelSeoData = withContext(Dispatchers.IO) {
        val fallbackSeo = generateChannelSeo(category, currentChannelName, currentHandle)
        if (notrack == null) return@withContext fallbackSeo

        val channelName = currentChannelName?.ifBlank { null } ?: fallbackSeo.suggestedChannelName
        val handle = currentHandle?.ifBlank { null } ?: fallbackSeo.handle

        val masterPrompt = """
            You are an elite YouTube Creator Channel Branding Strategist.
            Optimize YouTube Channel Branding for maximum subscriber conversion and SEO discoverability.

            USER CHANNEL INFORMATION:
            - Channel Name: $channelName
            - Category / Niche: $category
            - Handle: $handle
            - Subscribers: ${subscriberCount ?: "N/A"}

            TASK INSTRUCTION:
            Generate a high-converting, viral YouTube Channel Profile:
            1. An engaging 150-250 word YouTube Channel Description/Bio packed with high-value SEO keywords, upload schedule, and call to subscribe.
            2. 12-15 high-volume search keywords/tags for the channel.
            3. An 8K cinematic YouTube Channel Banner visual prompt.
            4. A high-impact YouTube Channel Avatar/Logo visual prompt.
            5. A catchy 1-line Tagline.

            CRITICAL: Return ONLY a valid JSON object starting with '{' and ending with '}'.
            JSON Schema:
            {
              "tagline": "...",
              "optimized_description": "...",
              "search_keywords": ["keyword1", "keyword2", ...],
              "banner_prompt": "...",
              "avatar_prompt": "..."
            }
        """.trimIndent()

        try {
            val responseJson = notrack.askNotrackForJsonObject(masterPrompt, timeout = 60_000)
            val json = JSONObject(responseJson)

            val tagline = json.optString("tagline", fallbackSeo.tagline)
            val desc = json.optString("optimized_description", fallbackSeo.optimizedDescription)
            val keywordsArray = json.optJSONArray("search_keywords")
            val parsedKeywords = mutableListOf<String>()
            if (keywordsArray != null) {
                for (i in 0 until keywordsArray.length()) {
                    val k = keywordsArray.optString(i).trim()
                    if (k.isNotBlank()) parsedKeywords.add(k)
                }
            }
            val finalKeywords = if (parsedKeywords.size >= 5) parsedKeywords else fallbackSeo.searchKeywords
            val banner = json.optString("banner_prompt", fallbackSeo.bannerPrompt)
            val avatar = json.optString("avatar_prompt", fallbackSeo.avatarPrompt)

            ChannelSeoData(
                category = category,
                suggestedChannelName = channelName,
                handle = handle,
                tagline = tagline,
                optimizedDescription = desc,
                searchKeywords = finalKeywords,
                uploadSchedule = fallbackSeo.uploadSchedule,
                bannerPrompt = banner,
                avatarPrompt = avatar,
                hashtags = fallbackSeo.hashtags
            )
        } catch (e: Exception) {
            Log.w(TAG, "Notrack channel branding generation fallback to local engine: ${e.message}")
            fallbackSeo
        }
    }

    /**
     * Updates channel branding/description via YouTube Data API v3
     */
    suspend fun updateChannelBrandingViaApi(
        context: Context,
        newDescription: String,
        keywords: List<String>,
        customToken: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val token = customToken ?: run {
                if (account == null) {
                    return@withContext Result.failure(Exception("🔑 Google/YouTube Sign-In required. Please tap LOGIN TO YOUTUBE to authenticate."))
                }
                GoogleSignInHelper.getOAuthAccessToken(context, account)
            } ?: return@withContext Result.failure(Exception("🔑 Missing YouTube OAuth Token. Please tap RE-AUTHENTICATE YOUTUBE to grant permissions."))

            // 1. Fetch channel ID
            val infoRes = GoogleSignInHelper.fetchYouTubeChannelInfo(context)
            if (infoRes.isFailure) {
                val err = infoRes.exceptionOrNull()?.message ?: "Failed to load YouTube channel"
                return@withContext Result.failure(Exception("🔑 YouTube Login Error: $err. Please tap LOGIN TO YOUTUBE."))
            }
            val channelInfo = infoRes.getOrNull()
            val channelId = channelInfo?.channelId

            if (channelId.isNullOrBlank()) {
                return@withContext Result.failure(Exception("🔑 No active YouTube channel found for this account. Please create a channel on YouTube first."))
            }

            // 2. Prepare payload for updating channel branding
            // CRITICAL FIX: YouTube Data API v3 channels.update requires "id" field in the JSON request body!
            val formattedKeywords = keywords.joinToString(" ") { tag ->
                if (tag.contains(" ")) "\"$tag\"" else tag
            }

            val jsonPayload = JSONObject().apply {
                put("id", channelId)
                put("brandingSettings", JSONObject().apply {
                    put("channel", JSONObject().apply {
                        put("description", newDescription)
                        put("keywords", formattedKeywords)
                    })
                })
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/channels?part=brandingSettings")
                .addHeader("Authorization", "Bearer $token")
                .put(body)
                .build()

            val response = client.newCall(request).execute()
            val resBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.d(TAG, "Channel SEO successfully updated on YouTube: $resBody")
                Result.success(true)
            } else {
                Log.e(TAG, "Channel update API returned error ${response.code}: $resBody")
                val errorDetail = try {
                    JSONObject(resBody).optJSONObject("error")?.optString("message") ?: resBody
                } catch (_: Exception) {
                    resBody
                }
                if (response.code == 400 && errorDetail.contains("brandingSettings", ignoreCase = true)) {
                    Result.failure(Exception("YouTube API Error (400): Invalid brandingSettings parameter. Please check channel permissions."))
                } else if (response.code == 401 || response.code == 403 || resBody.contains("unauthorized", ignoreCase = true) || resBody.contains("invalid_grant", ignoreCase = true)) {
                    Result.failure(Exception("🔑 YouTube Permission Expired (HTTP ${response.code}): Please tap RE-AUTHENTICATE YOUTUBE to refresh permissions."))
                } else {
                    Result.failure(Exception("YouTube API Error (${response.code}): $errorDetail"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating channel branding", e)
            Result.failure(e)
        }
    }
}
