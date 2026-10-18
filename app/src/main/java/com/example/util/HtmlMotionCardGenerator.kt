package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

data class HtmlMotionSlide(
    val slideIndex: Int,
    val badgeText: String,
    val badgeColorType: String, // "cyan", "green", "pink", "purple"
    val headline: String,
    val headlineHighlight: String,
    val bodyParagraph: String,
    val bulletPoints: List<String> = emptyList(),
    val ctaText: String? = null,
    val startTimeSec: Double,
    val endTimeSec: Double
)

data class HtmlMotionPackage(
    val htmlContent: String,
    val slides: List<HtmlMotionSlide>,
    val totalDurationSec: Double,
    val topic: String,
    val category: String
)

object HtmlMotionCardGenerator {

    private const val TAG = "HtmlMotionCardGenerator"

    fun generateMotionSlides(
        topic: String,
        category: String,
        narrationText: String,
        durationSec: Int = 15,
        theme: String = "Cyberpunk Dark"
    ): HtmlMotionPackage {
        val cleanTopic = topic.ifBlank { "Next-Gen AI & Future Tech" }
        val sentences = narrationText
            .split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length > 5 }
            .ifEmpty {
                listOf(
                    "Discover the groundbreaking power of autonomous AI systems.",
                    "Self-executing code and continuous real-time learning workflows are changing everything.",
                    "Start building your futuristic autonomous pipelines today!"
                )
            }

        val slideCount = sentences.size.coerceIn(3, 6)
        val perSlideDuration = durationSec.toDouble() / slideCount.toDouble()

        val slides = mutableListOf<HtmlMotionSlide>()

        for (i in 0 until slideCount) {
            val start = i * perSlideDuration
            val end = (i + 1) * perSlideDuration
            val sentence = sentences.getOrElse(i) { "Continuous innovation powered by intelligence." }

            when (i) {
                0 -> {
                    // Hook / Title Slide
                    slides.add(
                        HtmlMotionSlide(
                            slideIndex = 1,
                            badgeText = category.uppercase(),
                            badgeColorType = "cyan",
                            headline = "The Era of",
                            headlineHighlight = cleanTopic.take(30),
                            bodyParagraph = sentence,
                            startTimeSec = start,
                            endTimeSec = end
                        )
                    )
                }
                slideCount - 1 -> {
                    // Conclusion / CTA Slide
                    slides.add(
                        HtmlMotionSlide(
                            slideIndex = i + 1,
                            badgeText = "NEXT GEN",
                            badgeColorType = "green",
                            headline = "Are You Ready?",
                            headlineHighlight = "Take Action",
                            bodyParagraph = sentence,
                            ctaText = "Subscribe & Follow",
                            startTimeSec = start,
                            endTimeSec = end
                        )
                    )
                }
                else -> {
                    // Keypoints / Feature Slide
                    val words = sentence.split(" ")
                    val half = words.size / 2
                    val part1 = words.take(half.coerceAtLeast(3)).joinToString(" ")
                    val part2 = words.drop(half.coerceAtLeast(3)).joinToString(" ")

                    slides.add(
                        HtmlMotionSlide(
                            slideIndex = i + 1,
                            badgeText = "TRENDING NOW",
                            badgeColorType = if (i % 2 == 1) "purple" else "cyan",
                            headline = "Key Breakthrough",
                            headlineHighlight = "$category Focus",
                            bodyParagraph = sentence,
                            bulletPoints = listOf(
                                "⚡ $part1",
                                if (part2.isNotBlank()) "🤖 $part2" else "🌐 High-Speed Processing",
                                "🔥 100% Autonomous Efficiency"
                            ),
                            startTimeSec = start,
                            endTimeSec = end
                        )
                    )
                }
            }
        }

        val htmlDoc = buildCompleteHtmlDocument(
            topic = cleanTopic,
            category = category,
            slides = slides,
            totalDurationSec = durationSec.toDouble(),
            theme = theme
        )

        return HtmlMotionPackage(
            htmlContent = htmlDoc,
            slides = slides,
            totalDurationSec = durationSec.toDouble(),
            topic = cleanTopic,
            category = category
        )
    }

    fun buildCompleteHtmlDocument(
        topic: String,
        category: String,
        slides: List<HtmlMotionSlide>,
        totalDurationSec: Double,
        theme: String = "Cyberpunk Dark"
    ): String {
        val totalSec = totalDurationSec.coerceAtLeast(5.0)
        val numSlides = slides.size.coerceAtLeast(1)
        val slideDur = totalSec / numSlides.toDouble()

        val themeColors = when (theme.lowercase()) {
            "matrix green" -> ThemeColorSet("#050f07", "#00ff66", "#2ecc71", "#ffffff", "#00ff66")
            "neon sunset" -> ThemeColorSet("#120718", "#ff007f", "#ff9900", "#ffffff", "#ff007f")
            "oled minimal" -> ThemeColorSet("#000000", "#ffffff", "#888888", "#ffffff", "#ffffff")
            else -> ThemeColorSet("#07090e", "#00f2ff", "#2ecc71", "#ffffff", "#00f2ff")
        }

        val sb = StringBuilder()
        sb.append("""
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${escapeHtml(topic)} - GLITCH Motion Video</title>
  <style>
    :root {
      --bg-color: ${themeColors.bg};
      --cyan-color: ${themeColors.primary};
      --green-color: ${themeColors.accent};
      --text-main: ${themeColors.text};
      --font-family: 'Segoe UI', Roboto, -apple-system, sans-serif;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      background-color: var(--bg-color);
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      font-family: var(--font-family);
      overflow: hidden;
    }

    /* Video Frame Setup (9:16 Shorts/Reels Format) */
    .video-card {
      position: relative;
      width: 100vw;
      height: 100vh;
      max-width: 540px;
      max-height: 960px;
      background: radial-gradient(circle at center, #121f2d 0%, #05080c 100%);
      border: 2px solid rgba(0, 242, 255, 0.3);
      box-shadow: 0 0 40px rgba(0, 242, 255, 0.25);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
    }

    /* Top Video Progress Bar */
    .progress-bar-container {
      position: absolute;
      top: 24px;
      left: 6%;
      width: 88%;
      height: 6px;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 3px;
      z-index: 20;
    }

    .progress-bar {
      width: 0%;
      height: 100%;
      background: var(--cyan-color);
      box-shadow: 0 0 12px var(--cyan-color);
      animation: loadProgress ${totalSec}s linear infinite;
    }

    /* Base Slide Styles */
    .slide {
      position: absolute;
      width: 100%;
      height: 100%;
      padding: 60px 30px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      text-align: center;
      opacity: 0;
      pointer-events: none;
    }

        """.trimIndent())

        // Add Slide Keyframes & Classes dynamically
        for (i in slides.indices) {
            val delaySec = i * slideDur
            sb.append("""
    .slide-${i + 1} {
      animation: slideFadeInOut ${totalSec}s infinite;
      animation-delay: ${delaySec}s;
    }
            """.trimIndent())
        }

        sb.append("""

    /* Keyframes for Slide Transitions */
    @keyframes slideFadeInOut {
      0% {
        opacity: 0;
        transform: scale(0.92) translateY(24px);
      }
      3% {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
      ${((1.0 / numSlides.toDouble()) * 90).toInt()}% {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
      ${((1.0 / numSlides.toDouble()) * 98).toInt()}% {
        opacity: 0;
        transform: scale(1.04) translateY(-20px);
      }
      100% {
        opacity: 0;
      }
    }

    @keyframes loadProgress {
      0% { width: 0%; }
      100% { width: 100%; }
    }

    /* UI Badges Styling */
    .badge {
      background: rgba(0, 242, 255, 0.12);
      border: 1.5px solid var(--cyan-color);
      color: var(--cyan-color);
      padding: 8px 18px;
      border-radius: 24px;
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 2.5px;
      margin-bottom: 24px;
      box-shadow: 0 0 15px rgba(0, 242, 255, 0.3);
    }

    .badge.cyan { border-color: var(--cyan-color); color: var(--cyan-color); }
    .badge.green { border-color: var(--green-color); color: var(--green-color); }
    .badge.purple { border-color: #b026ff; color: #b026ff; }

    h1 {
      color: var(--text-main);
      font-size: 34px;
      line-height: 1.25;
      font-weight: 900;
      margin-bottom: 12px;
    }

    h1 span, h2 span {
      color: var(--cyan-color);
      text-shadow: 0 0 20px rgba(0, 242, 255, 0.85);
    }

    h2 {
      color: var(--text-main);
      font-size: 28px;
      font-weight: 800;
      margin-bottom: 16px;
    }

    p {
      color: #cbd5e1;
      font-size: 17px;
      line-height: 1.5;
      margin-top: 14px;
      max-width: 90%;
    }

    .feature-list {
      list-style: none;
      margin-top: 18px;
      text-align: left;
      width: 100%;
      max-width: 420px;
    }

    .feature-list li {
      color: #f1f5f9;
      font-size: 16px;
      font-weight: 600;
      margin: 12px 0;
      background: rgba(255, 255, 255, 0.07);
      padding: 12px 18px;
      border-radius: 12px;
      border-left: 4px solid var(--cyan-color);
      backdrop-filter: blur(10px);
    }

    .cta-btn {
      margin-top: 28px;
      padding: 14px 34px;
      background: var(--green-color);
      color: #000000;
      font-weight: 900;
      border: none;
      border-radius: 30px;
      font-size: 17px;
      letter-spacing: 1px;
      cursor: pointer;
      box-shadow: 0 0 25px rgba(46, 204, 113, 0.55);
    }
  </style>
</head>
<body>

  <!-- Video Card Frame -->
  <div class="video-card">
    
    <!-- Top Progress Bar -->
    <div class="progress-bar-container">
      <div class="progress-bar"></div>
    </div>

        """.trimIndent())

        // Insert Slides Markup
        for ((idx, slide) in slides.withIndex()) {
            val slideClass = "slide slide-${idx + 1}"
            val badgeColor = slide.badgeColorType.ifBlank { "cyan" }
            sb.append("\n    <!-- Slide ${idx + 1} -->\n")
            sb.append("    <div class=\"$slideClass\">\n")
            sb.append("      <div class=\"badge $badgeColor\">${escapeHtml(slide.badgeText)}</div>\n")
            
            if (slide.headline.isNotBlank()) {
                if (slide.headlineHighlight.isNotBlank()) {
                    sb.append("      <h1>${escapeHtml(slide.headline)}<br><span>${escapeHtml(slide.headlineHighlight)}</span></h1>\n")
                } else {
                    sb.append("      <h1>${escapeHtml(slide.headline)}</h1>\n")
                }
            }

            if (slide.bulletPoints.isNotEmpty()) {
                sb.append("      <ul class=\"feature-list\">\n")
                for (pt in slide.bulletPoints) {
                    sb.append("        <li>${escapeHtml(pt)}</li>\n")
                }
                sb.append("      </ul>\n")
            } else if (slide.bodyParagraph.isNotBlank()) {
                sb.append("      <p>${escapeHtml(slide.bodyParagraph)}</p>\n")
            }

            if (!slide.ctaText.isNullOrBlank()) {
                sb.append("      <button class=\"cta-btn\">${escapeHtml(slide.ctaText)}</button>\n")
            }

            sb.append("    </div>\n")
        }

        sb.append("""

  </div>

</body>
</html>
        """.trimIndent())

        return sb.toString()
    }

    /**
     * Renders high quality slide bitmap cards directly for the video synthesizer.
     */
    fun createSlideBitmap(
        slide: HtmlMotionSlide,
        width: Int = 1080,
        height: Int = 1920,
        theme: String = "Cyberpunk Dark"
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val themeColors = when (theme.lowercase()) {
            "matrix green" -> ThemeColorSet("#050f07", "#00ff66", "#2ecc71", "#ffffff", "#00ff66")
            "neon sunset" -> ThemeColorSet("#120718", "#ff007f", "#ff9900", "#ffffff", "#ff007f")
            "oled minimal" -> ThemeColorSet("#000000", "#ffffff", "#888888", "#ffffff", "#ffffff")
            else -> ThemeColorSet("#07090e", "#00f2ff", "#2ecc71", "#ffffff", "#00f2ff")
        }

        // Draw Deep Dark Cyber Background
        val bgPaint = Paint().apply {
            color = Color.parseColor(themeColors.bg)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw Card Container Border
        val borderMargin = 40f
        val cardRect = RectF(borderMargin, borderMargin, width - borderMargin, height - borderMargin)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(themeColors.primary)
            alpha = 60
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        // Draw Progress Bar at Top
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(themeColors.primary)
            style = Paint.Style.FILL
        }
        val progressTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 40
            style = Paint.Style.FILL
        }
        val pBarRect = RectF(borderMargin + 40f, borderMargin + 40f, width - borderMargin - 40f, borderMargin + 52f)
        canvas.drawRoundRect(pBarRect, 6f, 6f, progressTrackPaint)

        val filledWidth = (pBarRect.width() * (slide.slideIndex / 4.0)).toFloat().coerceIn(40f, pBarRect.width())
        val fillRect = RectF(pBarRect.left, pBarRect.top, pBarRect.left + filledWidth, pBarRect.bottom)
        canvas.drawRoundRect(fillRect, 6f, 6f, progressPaint)

        // Draw Category Badge
        val badgeText = slide.badgeText.uppercase()
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(themeColors.primary)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(themeColors.primary)
            alpha = 35
            style = Paint.Style.FILL
        }
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(themeColors.primary)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val badgeWidth = badgePaint.measureText(badgeText) + 80f
        val badgeRect = RectF(
            (width / 2f) - (badgeWidth / 2f),
            height * 0.22f,
            (width / 2f) + (badgeWidth / 2f),
            height * 0.22f + 70f
        )
        canvas.drawRoundRect(badgeRect, 35f, 35f, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 35f, 35f, badgeBorderPaint)
        canvas.drawText(badgeText, width / 2f, badgeRect.centerY() + 12f, badgePaint)

        // Draw Headline
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 62f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(slide.headline, width / 2f, height * 0.36f, headlinePaint)

        // Draw Highlight
        if (slide.headlineHighlight.isNotBlank()) {
            val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(themeColors.primary)
                textSize = 72f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(25f, 0f, 0f, Color.parseColor(themeColors.primary))
            }
            canvas.drawText(slide.headlineHighlight, width / 2f, height * 0.42f, hlPaint)
        }

        // Draw Bullet Points or Paragraph
        if (slide.bulletPoints.isNotEmpty()) {
            var yOffset = height * 0.50f
            val itemBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 20
                style = Paint.Style.FILL
            }
            val itemBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(themeColors.primary)
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(themeColors.text)
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }

            for (point in slide.bulletPoints) {
                val itemRect = RectF(100f, yOffset, width - 100f, yOffset + 110f)
                canvas.drawRoundRect(itemRect, 20f, 20f, itemBgPaint)
                canvas.drawLine(itemRect.left, itemRect.top, itemRect.left, itemRect.bottom, itemBorderPaint)
                canvas.drawText(point, itemRect.left + 30f, itemRect.centerY() + 15f, textPaint)
                yOffset += 140f
            }
        } else {
            // Paragraph wrap
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 46f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            val wrapped = wrapText(slide.bodyParagraph, 30)
            var yOffset = height * 0.52f
            for (line in wrapped) {
                canvas.drawText(line, width / 2f, yOffset, bodyPaint)
                yOffset += 60f
            }
        }

        // Draw CTA Button if present
        if (!slide.ctaText.isNullOrBlank()) {
            val ctaBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor(themeColors.accent)
                style = Paint.Style.FILL
            }
            val ctaTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val ctaRect = RectF(
                width * 0.2f,
                height * 0.78f,
                width * 0.8f,
                height * 0.85f
            )
            canvas.drawRoundRect(ctaRect, 45f, 45f, ctaBgPaint)
            canvas.drawText(slide.ctaText, width / 2f, ctaRect.centerY() + 16f, ctaTextPaint)
        }

        return bitmap
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (w in words) {
            if (currentLine.length + w.length + 1 > maxCharsPerLine) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(w)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(w)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private data class ThemeColorSet(
        val bg: String,
        val primary: String,
        val accent: String,
        val text: String,
        val glow: String
    )
}
