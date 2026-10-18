package com.example.util

import com.example.pipeline.NotrackWebViewClient

data class ThumbnailPromptResult(
    val mainVisualPrompt: String,
    val highCtrTextOverlay: String,
    val colorPalette: String,
    val emotionalAngle: String,
    val bannerVisualPrompt: String
)

object ThumbnailBannerPromptGenerator {

    /**
     * Generates 8K High CTR Visual Prompts for YouTube Thumbnails & Channel Banners.
     * Fully autonomous, zero manual click required.
     */
    fun generateVisualPrompts(
        topic: String,
        category: String,
        channelName: String = "Glitch Creator Studio"
    ): ThumbnailPromptResult {
        val cleanTopic = topic.trim()

        val niche = NotrackWebViewClient.resolveNiche(category)

        val (thumbnailPrompt, textOverlay, palette, angle) = when (niche) {
            NotrackWebViewClient.Companion.VideoNiche.HORROR_THRILLER -> Quadruple(
                "8K ultra-realistic cinematic horror thumbnail, $cleanTopic, terrifying dark atmosphere, cold blue mist, glowing red eyes in deep shadow, hyper-detailed facial fear expression, dramatic chiaroscuro lighting, octane render, 9:16 vertical, photorealistic, dramatic hook",
                "😱 DON'T LOOK BEHIND YOU!",
                "Cold Navy, Crimson Blood Red, Shadowy Black",
                "Fear & Uncanny Suspense"
            )
            NotrackWebViewClient.Companion.VideoNiche.MYSTERY_CRIME -> Quadruple(
                "8K hyper-detailed crime scene investigation thumbnail, $cleanTopic, noir aesthetic, dramatic yellow police tape, dimly lit detective magnifying glass on mysterious red evidence, dark foggy city alley, intense high-contrast shadows, 9:16 vertical, photorealistic 8k render",
                "⚠️ UNSOLVED CASE FILE",
                "Amber Gold, Noir Black, Caution Yellow",
                "Curiosity & Forensic Mystery"
            )
            NotrackWebViewClient.Companion.VideoNiche.HISTORY_CULTURE -> Quadruple(
                "8K epic historical cinematic thumbnail, $cleanTopic, magnificent golden ancient temple ruins, dramatic sunset god rays filtering through crumbling stone arches, mysterious glowing ancient inscriptions, photorealistic, cinematic movie poster quality, 9:16 vertical, ultra HD",
                "🏛️ BURIED FOR 4,000 YEARS",
                "Ancient Gold, Terracotta, Royal Lapis Blue",
                "Awe & Forgotten Wonder"
            )
            NotrackWebViewClient.Companion.VideoNiche.MOTIVATION_FACTS -> Quadruple(
                "8K motivational masterpiece thumbnail, $cleanTopic, intense hyper-focused human eyes close-up, dramatic side lighting, gold chess king piece in foreground, rainy city skyline backdrop, ultra photorealistic, 8k resolution, 9:16 vertical",
                "🔥 THE 1% WINNER RULE",
                "Electric Gold, Onyx Black, Radiant Cyan",
                "Elite Power & Unstoppable Will"
            )
            NotrackWebViewClient.Companion.VideoNiche.AI_FUTURISTIC -> Quadruple(
                "8K cyberpunk futuristic thumbnail, $cleanTopic, sleek translucent humanoid android with glowing neon blue neural synapses, holographic floating code interfaces, neon dark futuristic city, 8k resolution, Unreal Engine 5 render, 9:16 vertical",
                "🤖 AI REACHED CONSCIOUSNESS?",
                "Neon Cyan, Electric Violet, Void Black",
                "Futuristic Shock & Technology Awe"
            )
            NotrackWebViewClient.Companion.VideoNiche.SCIENCE_SPACE -> Quadruple(
                "8K cosmic space phenomenon thumbnail, $cleanTopic, massive swirling black hole with glowing accretion disk, deep nebula cosmos background, subatomic quantum particle explosion, hyper-detailed, NASA imagery aesthetic, 9:16 vertical",
                "🌌 SCIENCE CAN'T EXPLAIN THIS",
                "Deep Cosmic Blue, Magenta Nebula, Gold Stardust",
                "Cosmic Wonder & Mind-Bending Revelation"
            )
            NotrackWebViewClient.Companion.VideoNiche.BUSINESS_FINANCE -> Quadruple(
                "8K luxury financial thumbnail, $cleanTopic, high-rise Manhattan skyscraper boardroom at night, stack of gold bullion bars reflecting electronic trading chart green line surge, sharp focus, cinematic 8k render, 9:16 vertical",
                "💵 0.01% WEALTH SECRET",
                "Emerald Green, Gold Foil, Deep Slate",
                "Financial Sovereignty & Wealth Status"
            )
            NotrackWebViewClient.Companion.VideoNiche.GAMING -> Quadruple(
                "8K epic gaming thumbnail, $cleanTopic, high-tech esports stadium battlestation, neon purple liquid cooling RGB, glowing legendary item drop in dark gaming atmosphere, hyper-detailed 3D render, 9:16 vertical",
                "🎮 SECRET GLITCH FOUND!",
                "Neon Purple, Cyber Green, Obsidian",
                "Gamer Hype & Exclusive Easter Egg"
            )
            NotrackWebViewClient.Companion.VideoNiche.ENTERTAINMENT -> Quadruple(
                "8K Hollywood cinema premiere thumbnail, $cleanTopic, dramatic red carpet spotlights blinding beam, golden film reel, secret director breakdown notebook, ultra photorealistic studio quality, 9:16 vertical",
                "🎬 MOVIE DETAIL YOU MISSED",
                "Red Carpet Crimson, Spotlight White, Gold",
                "Behind-the-Scenes Insider Fascination"
            )
            NotrackWebViewClient.Companion.VideoNiche.TECHNOLOGY -> Quadruple(
                "8K microchip silicon architecture thumbnail, $cleanTopic, macro close-up of glowing laser-engraved futuristic processor chip, translucent circuit board, high-tech engineering aesthetic, 8k render, 9:16 vertical",
                "⚡ NEXT-GEN BREAKTHROUGH",
                "Laser Blue, Silver Aluminum, Tech Orange",
                "Engineering Marvel & Future Hype"
            )
            NotrackWebViewClient.Companion.VideoNiche.FITNESS_HEALTH -> Quadruple(
                "8K high-performance fitness thumbnail, $cleanTopic, elite muscular athlete silhouette on misty mountain peak at sunrise, glowing biological vitals HUD overlay, intense sweat texture, dramatic 8k camera shot, 9:16 vertical",
                "⚡ RAPID METABOLIC RESET",
                "Solar Orange, Steel Blue, Chalk White",
                "Peak Performance & Transformation"
            )
            NotrackWebViewClient.Companion.VideoNiche.PSYCHOLOGY_SECRETS -> Quadruple(
                "8K psychological mystery thumbnail, $cleanTopic, extreme macro shot of human eye reflecting a secret chess match, moody chiaroscuro lighting, dark subconscious mind metaphor, dramatic contrast, 9:16 vertical",
                "🧠 SUB-CONSCIOUS MANIPULATION",
                "Deep Indigo, Crimson Glow, Shadow Black",
                "Subconscious Intrigue & Secret Knowledge"
            )
        }

        val bannerPrompt = "8K ultra-wide YouTube Channel Banner (16:9 panoramic header) for '$channelName' in $category niche. Sleek futuristic aesthetic, high-contrast dark theme background with vibrant $palette accents, professional typography zone, high-tech ambient glows, subtle geometric branding motifs, clean 8k graphic design render."

        return ThumbnailPromptResult(
            mainVisualPrompt = thumbnailPrompt,
            highCtrTextOverlay = textOverlay,
            colorPalette = palette,
            emotionalAngle = angle,
            bannerVisualPrompt = bannerPrompt
        )
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
