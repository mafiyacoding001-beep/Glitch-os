package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import com.example.pipeline.NotrackWebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class TitleVariation(
    val type: String,
    val title: String,
    val hookAngle: String,
    val emoji: String
)

data class RichVideoDescription(
    val summary: String,
    val questionPrompt: String,
    val hashtags: List<String>,
    val fullDescriptionText: String
)

data class VideoFrameSample(
    val index: Int,
    val timestampSec: Double,
    val label: String,
    val hookType: String,
    val filePath: String? = null,
    val contrastScore: Int? = null
)

data class DeepVideoSeoResult(
    val category: String,
    val topic: String,
    val language: String,
    val titleVariations: List<TitleVariation>,
    val selectedTitleIndex: Int = 0,
    val richDescription: RichVideoDescription,
    val suggestedTags: List<String>,
    val totalTagCharacters: Int
) {
    val currentSelectedTitle: String
        get() = titleVariations.getOrNull(selectedTitleIndex)?.title ?: titleVariations.firstOrNull()?.title ?: topic
}

object VideoSeoOptimizer {
    private const val MAX_TAG_CHARACTERS = 500

    /**
     * Generates local metadata drafts and basic validation checks; it does not measure SEO performance.
     */
    fun generateDeepVideoSeo(
        topic: String,
        category: String,
        language: String = "Hindi",
        customSummary: String? = null
    ): DeepVideoSeoResult {
        val cleanTopic = topic.trim()
        require(cleanTopic.isNotBlank()) {
            "A real topic must be supplied before generating SEO suggestions."
        }

        val titles = generateTitleVariations(cleanTopic, language)
        val description = generateRichDescription(cleanTopic, category, language, customSummary)
        val tags = generateClustered500CharTags(cleanTopic, category)
        val tagCharCount = tags.joinToString(", ").length
        return DeepVideoSeoResult(
            category = category,
            topic = cleanTopic,
            language = language,
            titleVariations = titles,
            selectedTitleIndex = 0,
            richDescription = description,
            suggestedTags = tags,
            totalTagCharacters = tagCharCount
        )
    }

    /**
     * Generates neutral title drafts. These are not CTR predictions.
     */
    fun generateTitleVariations(
        topic: String,
        language: String
    ): List<TitleVariation> {
        val isHindi = language.contains("Hindi", ignoreCase = true)
        return if (isHindi) {
            listOf(
                TitleVariation("विषय परिचय", "$topic: एक परिचय", "तटस्थ विषय परिचय", "📘"),
                TitleVariation("सरल व्याख्या", "$topic को सरल भाषा में समझें", "सरल भाषा में व्याख्या", "💡"),
                TitleVariation("विषय की पड़ताल", "$topic पर एक नज़र", "विषय का सामान्य अवलोकन", "🔎"),
                TitleVariation("मुख्य बातें", "$topic की मुख्य बातें", "मुख्य बिंदुओं का सार", "📝"),
                TitleVariation("चर्चा", "$topic: सवाल और जवाब", "दर्शकों के सवालों पर चर्चा", "💬")
            )
        } else {
            listOf(
                TitleVariation("Overview", "$topic: An Overview", "A neutral introduction to the topic", "📘"),
                TitleVariation("Explainer", "$topic Explained", "A plain-language explanation", "💡"),
                TitleVariation("Exploration", "Exploring $topic", "A general look at the topic", "🔎"),
                TitleVariation("Key Points", "Key Points About $topic", "A summary of selected points", "📝"),
                TitleVariation("Discussion", "$topic: Questions and Answers", "A discussion of audience questions", "💬")
            )
        }
    }

    /**
     * Generates a neutral description draft that needs review against the finished video.
     */
    fun generateRichDescription(
        topic: String,
        category: String,
        language: String,
        customSummary: String?
    ): RichVideoDescription {
        val isHindi = language.contains("Hindi", ignoreCase = true)
        val summary = customSummary?.ifBlank { null } ?: if (isHindi) {
            "$topic विषय पर एक वीडियो विवरण का मसौदा। प्रकाशित करने से पहले तथ्य, स्रोत और वीडियो के वास्तविक विवरण जोड़ें।"
        } else {
            "Draft description for a video about $topic. Add verified facts, sources, and details that match the finished video before publishing."
        }
        val questionPrompt = if (isHindi) {
            "$topic के बारे में आपका क्या सवाल है? नीचे साझा करें।"
        } else {
            "What would you like to know about $topic? Share a question below."
        }
        val cleanCategory = category.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim().replace(" ", "")
        val hashtags = listOf("#${cleanCategory.take(40)}").filter { it != "#" }

        val sb = StringBuilder()
        sb.appendLine(summary)
        sb.appendLine()
        sb.appendLine(questionPrompt)
        sb.appendLine()
        sb.appendLine(hashtags.joinToString(" "))

        return RichVideoDescription(
            summary = summary,
            questionPrompt = questionPrompt,
            hashtags = hashtags,
            fullDescriptionText = sb.toString()
        )
    }

    /**
     * Generates topic/category tag drafts. Search volume and competition are not measured.
     */
    fun generateClustered500CharTags(
        topic: String,
        category: String
    ): List<String> {
        val cleanTopic = topic.lowercase().replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
        val cleanCategory = category.lowercase().replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
        val rawTags = listOf(
            cleanTopic.take(45),
            "$cleanTopic explained",
            "$cleanTopic overview",
            cleanCategory,
            "$cleanCategory video",
        )

        // Deduplicate and filter to stay strictly under YouTube's 500-character ceiling
        val distinctTags = rawTags.distinct().filter { it.isNotBlank() }
        val finalTags = mutableListOf<String>()
        var currentLen = 0

        for (t in distinctTags) {
            val addition = if (finalTags.isEmpty()) t.length else (t.length + 2)
            if (currentLen + addition <= MAX_TAG_CHARACTERS) {
                finalTags.add(t)
                currentLen += addition
            }
        }

        return finalTags
    }

    /**
     * Counts basic metadata checks; it does not predict search ranking or click-through rate.
     */
    fun calculateMetadataChecklistScore(
        topic: String,
        title: String,
        description: String,
        tags: List<String>
    ): Int {
        val tagCharacterCount = tags.joinToString(", ").length
        val checks = listOf(
            title.isNotBlank() && title.length <= 100,
            title.contains(topic, ignoreCase = true),
            description.isNotBlank() && description.length <= 5000,
            tags.isNotEmpty(),
            tagCharacterCount <= MAX_TAG_CHARACTERS
        )
        return checks.count { it } * 20
    }

    /**
     * Computes five frame-sampling timestamps as percentages of the video duration.
     */
    fun computeFrameSamplePlans(durationSec: Double): List<VideoFrameSample> {
        if (!durationSec.isFinite() || durationSec <= 0.0) return emptyList()
        return listOf(
            VideoFrameSample(
                index = 1,
                timestampSec = durationSec * 0.12,
                label = "Candidate frame 1",
                hookType = "Runtime sample at 12%"
            ),
            VideoFrameSample(
                index = 2,
                timestampSec = durationSec * 0.35,
                label = "Candidate frame 2",
                hookType = "Runtime sample at 35%"
            ),
            VideoFrameSample(
                index = 3,
                timestampSec = durationSec * 0.58,
                label = "Candidate frame 3",
                hookType = "Runtime sample at 58%"
            ),
            VideoFrameSample(
                index = 4,
                timestampSec = durationSec * 0.78,
                label = "Candidate frame 4",
                hookType = "Runtime sample at 78%"
            ),
            VideoFrameSample(
                index = 5,
                timestampSec = durationSec * 0.92,
                label = "Candidate frame 5",
                hookType = "Runtime sample at 92%"
            )
        )
    }

    /**
     * Extracts frames from the rendered video at five runtime percentages.
     */
    suspend fun extractVideoFrameSamples(
        videoFile: File,
        outputDir: File,
        durationSec: Double
    ): List<VideoFrameSample> = withContext(Dispatchers.IO) {
        if (!videoFile.isFile || videoFile.length() < 1000L) return@withContext emptyList()
        if (!outputDir.isDirectory && !outputDir.mkdirs()) {
            throw IllegalStateException("Could not create the frame analysis cache directory.")
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val actualDurationSec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.div(1000.0)
                ?.takeIf { it.isFinite() && it > 0.0 }
                ?: durationSec
            val plans = computeFrameSamplePlans(actualDurationSec)
            val extracted = mutableListOf<VideoFrameSample>()

            plans.forEach { plan ->
                val timeMicros = (plan.timestampSec * 1_000_000).toLong()
                val bitmap = retriever.getFrameAtTime(timeMicros, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeMicros)

                if (bitmap != null) {
                    try {
                        val frameFile = File(outputDir, "video_frame_${plan.index}.jpg")
                        FileOutputStream(frameFile).use { fos ->
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)) {
                                throw IllegalStateException("Could not encode an extracted video frame.")
                            }
                        }
                        extracted.add(
                            plan.copy(
                                filePath = frameFile.absolutePath,
                                contrastScore = evaluateFrameVisualContrast(bitmap)
                            )
                        )
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }
            }
            extracted
        } catch (e: Exception) {
            android.util.Log.e("VideoSeoOptimizer", "Failed to extract candidate frames", e)
            throw e
        } finally {
            retriever.release()
        }
    }

    /**
     * Estimates frame contrast from sampled pixel brightness; this does not assess content suitability.
     */
    private fun evaluateFrameVisualContrast(bitmap: Bitmap): Int {
        val sampleW = 32
        val sampleH = 32
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, false)
        var sumBrightness = 0.0
        val pixels = IntArray(sampleW * sampleH)
        try {
            scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        } finally {
            if (scaled !== bitmap && !scaled.isRecycled) scaled.recycle()
        }

        val brightnessValues = DoubleArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val lum = (0.299 * r + 0.587 * g + 0.114 * b)
            brightnessValues[i] = lum
            sumBrightness += lum
        }

        val mean = sumBrightness / pixels.size
        var varianceSum = 0.0
        for (b in brightnessValues) {
            varianceSum += (b - mean) * (b - mean)
        }
        val stdDev = Math.sqrt(varianceSum / pixels.size)
        return (stdDev * 1.5).roundToInt().coerceIn(0, 100)
    }

    /**
     * Renders a custom cinematic thumbnail overlay with YouTube Shorts branding hook.
     */
    suspend fun createCinematicThumbnailWithOverlay(
        sourceFrame: File,
        badgeText: String,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        if (!sourceFrame.exists()) return@withContext sourceFrame
        outputFile.parentFile?.mkdirs()

        try {
            val original = BitmapFactory.decodeFile(sourceFrame.absolutePath) ?: return@withContext sourceFrame
            val width = original.width
            val height = original.height
            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)

            // 1. Draw base frame
            canvas.drawBitmap(original, 0f, 0f, null)
            original.recycle()

            // 2. Draw subtle dark top/bottom vignette gradients for text readability
            val vignettePaint = Paint().apply {
                color = Color.argb(160, 0, 0, 0)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height * 0.18f, vignettePaint)
            canvas.drawRect(0f, height * 0.78f, width.toFloat(), height.toFloat(), vignettePaint)

            // 3. Draw Badge Tag (e.g. "😱 SHOCKING TRUTH" or "⚠️ 99% FAIL")
            if (badgeText.isNotBlank()) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFE600")
                    textSize = (height * 0.045f)
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = (height * 0.045f)
                    isFakeBoldText = true
                    style = Paint.Style.STROKE
                    strokeWidth = (height * 0.012f)
                    textAlign = Paint.Align.CENTER
                }

                val textY = height * 0.12f
                val textX = width / 2f
                canvas.drawText(badgeText, textX, textY, badgeStrokePaint)
                canvas.drawText(badgeText, textX, textY, badgePaint)
            }

            FileOutputStream(outputFile).use { fos ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
            resultBitmap.recycle()
            outputFile
        } catch (e: Exception) {
            android.util.Log.w("VideoSeoOptimizer", "Error generating thumbnail overlay: ${e.message}")
            sourceFrame
        }
    }
}
