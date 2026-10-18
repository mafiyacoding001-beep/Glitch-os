package com.example.pipeline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

interface FFmpegRenderer {
    suspend fun getDuration(audio: File): Double
    suspend fun render(
        images: List<File>,
        audio: File,
        captions: File,
        output: File,
        duration: Double,
        aspect: String,
        imageDurationSec: Int = 3,
        showCaptions: Boolean = true,
        captionStyle: String = "hormozi_bold",
        captionHighlightColor: String = "#FFE600",
        captionFontFamily: String = "THE_BOLD_FONT",
        captionStrokeWidth: Float = 12f,
        captionGlow: Boolean = true,
        captionWordsPerChunk: Int = 3,
        captionUppercase: Boolean = true
    )
    suspend fun createThumbnail(
        video: File,
        output: File
    )
}

class AndroidFFmpegRenderer : FFmpegRenderer {

    override suspend fun getDuration(audio: File): Double = withContext(Dispatchers.IO) {
        if (!audio.exists() || audio.length() < 44) return@withContext 30.0

        // Fast path for WAV files
        try {
            val bytes = ByteArray(44)
            java.io.FileInputStream(audio).use { it.read(bytes) }
            if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() && bytes[9] == 'A'.code.toByte() &&
                bytes[10] == 'V'.code.toByte() && bytes[11] == 'E'.code.toByte()
            ) {
                val bb = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                val channels = bb.getShort(22).toInt()
                val sampleRate = bb.getInt(24)
                val bitsPerSample = bb.getShort(34).toInt()
                val byteRate = sampleRate * channels * (bitsPerSample / 8)
                if (byteRate > 0) {
                    val dataLength = audio.length() - 44
                    val durationSec = dataLength.toDouble() / byteRate
                    if (durationSec > 0.5) return@withContext durationSec
                }
            }
        } catch (_: Exception) {}

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audio.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            if (durationMs != null && durationMs > 500L) {
                return@withContext durationMs / 1000.0
            }
        } catch (_: Exception) {}

        30.0
    }

    override suspend fun render(
        images: List<File>,
        audio: File,
        captions: File,
        output: File,
        duration: Double,
        aspect: String,
        imageDurationSec: Int,
        showCaptions: Boolean,
        captionStyle: String,
        captionHighlightColor: String,
        captionFontFamily: String,
        captionStrokeWidth: Float,
        captionGlow: Boolean,
        captionWordsPerChunk: Int,
        captionUppercase: Boolean
    ) = withContext(Dispatchers.IO) {
        output.parentFile?.mkdirs()
        val (width, height) = when (aspect) {
            "16:9" -> Pair(1280, 720)
            "1:1" -> Pair(720, 720)
            else -> Pair(720, 1280) // 9:16 default
        }

        val frameRate = 30
        val bitRate = 2_500_000
        val iFrameInterval = 1

        val validBitmaps = images.mapNotNull { file ->
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }

        val subtitleCues = parseSrtFile(captions)
        val tempVideoOnly = File(output.parentFile, "temp_video_only.mp4")

        if (validBitmaps.isEmpty()) {
            val fallback = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            renderVideoFromBitmaps(
                bitmaps = listOf(fallback),
                cues = subtitleCues,
                outputFile = tempVideoOnly,
                width = width,
                height = height,
                durationSec = duration,
                frameRate = frameRate,
                bitRate = bitRate,
                iFrameInterval = iFrameInterval,
                imageDurationSec = imageDurationSec,
                showCaptions = showCaptions,
                captionStyle = captionStyle,
                captionHighlightColor = captionHighlightColor,
                captionFontFamily = captionFontFamily,
                captionStrokeWidth = captionStrokeWidth,
                captionGlow = captionGlow,
                captionWordsPerChunk = captionWordsPerChunk,
                captionUppercase = captionUppercase
            )
        } else {
            renderVideoFromBitmaps(
                bitmaps = validBitmaps,
                cues = subtitleCues,
                outputFile = tempVideoOnly,
                width = width,
                height = height,
                durationSec = duration,
                frameRate = frameRate,
                bitRate = bitRate,
                iFrameInterval = iFrameInterval,
                imageDurationSec = imageDurationSec,
                showCaptions = showCaptions,
                captionStyle = captionStyle,
                captionHighlightColor = captionHighlightColor,
                captionFontFamily = captionFontFamily,
                captionStrokeWidth = captionStrokeWidth,
                captionGlow = captionGlow,
                captionWordsPerChunk = captionWordsPerChunk,
                captionUppercase = captionUppercase
            )
        }

        // Mux video track with narration audio track
        if (tempVideoOnly.exists() && tempVideoOnly.length() > 500) {
            tempVideoOnly.copyTo(output, overwrite = true)
            try {
                muxVideoAndAudio(tempVideoOnly, audio, output)
            } catch (_: Exception) {}
            try { tempVideoOnly.delete() } catch (_: Exception) {}
        } else if (tempVideoOnly.exists()) {
            tempVideoOnly.renameTo(output)
        }
        Unit
    }

    private fun renderVideoFromBitmaps(
        bitmaps: List<Bitmap>,
        cues: List<SubtitleCue>,
        outputFile: File,
        width: Int,
        height: Int,
        durationSec: Double,
        frameRate: Int,
        bitRate: Int,
        iFrameInterval: Int,
        imageDurationSec: Int,
        showCaptions: Boolean,
        captionStyle: String = "hormozi_bold",
        captionHighlightColor: String = "#FFE600",
        captionFontFamily: String = "THE_BOLD_FONT",
        captionStrokeWidth: Float = 12f,
        captionGlow: Boolean = true,
        captionWordsPerChunk: Int = 3,
        captionUppercase: Boolean = true
    ) {
        val mimeType = "video/avc"
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        try {
            encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            // Strictly adhere to audio durationSec (or requested length) - distribute available bitmaps evenly across this duration
            val targetDuration = if (durationSec >= 1.0) durationSec else (bitmaps.size * imageDurationSec.toDouble()).coerceAtLeast(10.0)
            val totalFrames = (targetDuration * frameRate).toInt().coerceAtLeast(30)
            val framesPerImage = (totalFrames.toDouble() / bitmaps.size.toDouble()).coerceAtLeast(1.0)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

            var writtenFrameCount = 0L

            for (frame in 0 until totalFrames) {
                val imageIndex = (frame / framesPerImage).toInt().coerceIn(0, bitmaps.size - 1)
                val currentBitmap = bitmaps[imageIndex]

                val canvas = try {
                    inputSurface.lockHardwareCanvas()
                } catch (_: Exception) {
                    inputSurface.lockCanvas(null)
                }
                canvas.drawColor(Color.BLACK)

                // Aspect fit/center bitmap on canvas with subtle dynamic motion
                val frameWithinImage = frame % framesPerImage
                val motionRatio = (frameWithinImage / framesPerImage).toFloat()
                val subtleZoom = 1.0f + (0.04f * motionRatio)

                val srcWidth = currentBitmap.width
                val srcHeight = currentBitmap.height
                val baseScale = Math.max(width.toFloat() / srcWidth, height.toFloat() / srcHeight)
                val finalScale = baseScale * subtleZoom
                val scaledW = srcWidth * finalScale
                val scaledH = srcHeight * finalScale
                val dx = (width - scaledW) / 2f
                val dy = (height - scaledH) / 2f

                canvas.save()
                canvas.translate(dx, dy)
                canvas.scale(finalScale, finalScale)
                canvas.drawBitmap(currentBitmap, 0f, 0f, paint)
                canvas.restore()

                // Calculate current frame timestamp in ms and overlay active subtitle cue
                val currentFrameMs = (frame.toDouble() / frameRate * 1000.0).toLong()
                if (showCaptions) {
                    val activeCue = cues.firstOrNull { currentFrameMs in it.startMs..it.endMs }
                    if (activeCue != null && activeCue.text.isNotBlank()) {
                        drawSubtitleOverlay(
                            canvas = canvas,
                            text = activeCue.text,
                            width = width,
                            height = height,
                            currentFrameMs = currentFrameMs,
                            cueStartMs = activeCue.startMs,
                            cueEndMs = activeCue.endMs,
                            captionStyle = captionStyle,
                            captionHighlightColor = captionHighlightColor,
                            captionFontFamily = captionFontFamily,
                            captionStrokeWidth = captionStrokeWidth,
                            captionGlow = captionGlow,
                            captionWordsPerChunk = captionWordsPerChunk,
                            captionUppercase = captionUppercase
                        )
                    }
                }

                inputSurface.unlockCanvasAndPost(canvas)

                // Drain encoder with deterministic PTS
                while (true) {
                    val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        videoTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputBufferIndex) ?: continue
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0 && muxerStarted) {
                            // Compute exact deterministic presentation time in microseconds
                            bufferInfo.presentationTimeUs = (writtenFrameCount * 1_000_000L) / frameRate
                            writtenFrameCount++
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    } else {
                        break
                    }
                }
            }

            encoder.signalEndOfInputStream()

            // Drain remaining with deterministic PTS
            while (true) {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputBufferIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex) ?: continue
                    if (bufferInfo.size != 0 && muxerStarted) {
                        bufferInfo.presentationTimeUs = (writtenFrameCount * 1_000_000L) / frameRate
                        writtenFrameCount++
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    data class SubtitleCue(
        val index: Int,
        val startMs: Long,
        val endMs: Long,
        val text: String
    )

    private fun parseSrtFile(file: File): List<SubtitleCue> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        val cues = mutableListOf<SubtitleCue>()
        try {
            val lines = file.readLines()
            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) {
                    i++
                    continue
                }
                val cueIndex = line.toIntOrNull()
                if (cueIndex != null && i + 1 < lines.size && lines[i + 1].contains("-->")) {
                    val timeLine = lines[i + 1].trim()
                    val times = timeLine.split("-->").map { it.trim() }
                    if (times.size == 2) {
                        val startMs = parseSrtTimestampToMs(times[0])
                        val endMs = parseSrtTimestampToMs(times[1])
                        val textLines = mutableListOf<String>()
                        var j = i + 2
                        while (j < lines.size && lines[j].trim().isNotEmpty()) {
                            val text = lines[j].trim()
                            if (text.toIntOrNull() == null) {
                                textLines.add(text)
                            } else {
                                break
                            }
                            j++
                        }
                        val fullText = textLines.joinToString(" ")
                        if (fullText.isNotBlank()) {
                            cues.add(SubtitleCue(cueIndex, startMs, endMs, fullText))
                        }
                        i = j
                        continue
                    }
                }
                i++
            }
        } catch (_: Exception) {}
        return cues
    }

    private fun parseSrtTimestampToMs(timestamp: String): Long {
        return try {
            val clean = timestamp.replace(',', '.')
            val parts = clean.split(":")
            if (parts.size == 3) {
                val hours = parts[0].trim().toLong()
                val minutes = parts[1].trim().toLong()
                val secParts = parts[2].trim().split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + millis
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun drawSubtitleOverlay(
        canvas: Canvas,
        text: String,
        width: Int,
        height: Int,
        currentFrameMs: Long,
        cueStartMs: Long,
        cueEndMs: Long,
        captionStyle: String = "hormozi_bold",
        captionHighlightColor: String = "#FFE600",
        captionFontFamily: String = "THE_BOLD_FONT",
        captionStrokeWidth: Float = 12f,
        captionGlow: Boolean = true,
        captionWordsPerChunk: Int = 3,
        captionUppercase: Boolean = true
    ) {
        val rawWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (rawWords.isEmpty()) return

        val words = if (captionUppercase) {
            rawWords.map { it.uppercase(java.util.Locale.ROOT) }
        } else {
            rawWords
        }

        val cueDuration = (cueEndMs - cueStartMs).coerceAtLeast(100L)
        val elapsed = (currentFrameMs - cueStartMs).coerceIn(0L, cueDuration)
        val activeWordIndex = ((elapsed.toDouble() / cueDuration) * words.size).toInt().coerceIn(0, words.size - 1)

        val chunkSize = captionWordsPerChunk.coerceIn(1, 6)
        val currentChunkIndex = activeWordIndex / chunkSize
        val chunkStart = currentChunkIndex * chunkSize
        val chunkEnd = (chunkStart + chunkSize).coerceAtMost(words.size)
        val chunkWords = words.subList(chunkStart, chunkEnd)
        val activeIndexInChunk = activeWordIndex - chunkStart

        val tf = when {
            captionFontFamily.contains("IMPACT", ignoreCase = true) -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            captionFontFamily.contains("MONTSERRAT", ignoreCase = true) -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            captionFontFamily.contains("BEBAS", ignoreCase = true) -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            captionFontFamily.contains("MONO", ignoreCase = true) -> Typeface.MONOSPACE
            else -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val parsedHighlightColor = try {
            Color.parseColor(captionHighlightColor)
        } catch (_: Exception) {
            Color.parseColor("#FFE600")
        }

        val baseFontSize = (width * 0.062f).coerceIn(34f, 62f)
        val activeFontSize = baseFontSize * 1.32f
        val strokePx = (baseFontSize * (captionStrokeWidth / 100f * 1.6f)).coerceAtLeast(6f)
        val activeStrokePx = (activeFontSize * (captionStrokeWidth / 100f * 1.6f)).coerceAtLeast(8f)

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseFontSize
            typeface = tf
            textAlign = Paint.Align.LEFT
            setShadowLayer(8f, 0f, 4f, Color.BLACK)
        }

        val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parsedHighlightColor
            textSize = activeFontSize
            typeface = tf
            textAlign = Paint.Align.LEFT
            if (captionGlow) {
                setShadowLayer(26f, 0f, 0f, parsedHighlightColor)
            } else {
                setShadowLayer(10f, 0f, 4f, Color.BLACK)
            }
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = baseFontSize
            typeface = tf
            textAlign = Paint.Align.LEFT
            style = Paint.Style.STROKE
            strokeWidth = strokePx
        }

        val activeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = activeFontSize
            typeface = tf
            textAlign = Paint.Align.LEFT
            style = Paint.Style.STROKE
            strokeWidth = activeStrokePx
        }

        val spaceWidth = normalPaint.measureText(" ")

        data class WordMeasure(val word: String, val isActive: Boolean, val width: Float)
        val measuredWords = chunkWords.mapIndexed { idx, w ->
            val isActive = (idx == activeIndexInChunk)
            val p = if (isActive) activePaint else normalPaint
            WordMeasure(w, isActive, p.measureText(w))
        }

        val totalWidth = measuredWords.sumOf { it.width.toDouble() }.toFloat() + (measuredWords.size - 1) * spaceWidth
        var startX = (width - totalWidth) / 2f
        val centerY = height * 0.78f
        val baselineY = centerY + (baseFontSize * 0.35f)

        if (captionStyle == "minimalist_box" || captionStyle == "dark_pill") {
            val padX = baseFontSize * 0.7f
            val padY = baseFontSize * 0.4f
            val pillRect = RectF(
                startX - padX,
                centerY - (activeFontSize * 0.75f) - padY,
                startX + totalWidth + padX,
                centerY + (activeFontSize * 0.75f) + padY
            )
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 12, 14, 20)
                style = Paint.Style.FILL
            }
            val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parsedHighlightColor
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(pillRect, 22f, 22f, bgPaint)
            canvas.drawRoundRect(pillRect, 22f, 22f, pillBorderPaint)
        }

        for (wm in measuredWords) {
            val sp = if (wm.isActive) activeStrokePaint else strokePaint
            val p = if (wm.isActive) activePaint else normalPaint
            canvas.drawText(wm.word, startX, baselineY, sp)
            canvas.drawText(wm.word, startX, baselineY, p)
            startX += wm.width + spaceWidth
        }
    }

    override suspend fun createThumbnail(video: File, output: File): Unit = withContext(Dispatchers.IO) {
        output.parentFile?.mkdirs()
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(video.absolutePath)
            
            // Phase 2: Cinematic Thumbnail Frame Selection (sample key climatic moments)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 10_000L
            val samplePoints = listOf(
                (durationMs * 0.15).toLong() * 1000L,
                (durationMs * 0.35).toLong() * 1000L,
                (durationMs * 0.55).toLong() * 1000L,
                (durationMs * 0.75).toLong() * 1000L
            )

            var bestBitmap: Bitmap? = null
            var bestScore = -1.0

            for (timeUs in samplePoints) {
                val candidate = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs)
                if (candidate != null) {
                    // Compute basic luminance variance
                    val sample = Bitmap.createScaledBitmap(candidate, 16, 16, false)
                    var sum = 0.0
                    val pixels = IntArray(256)
                    sample.getPixels(pixels, 0, 16, 0, 0, 16, 16)
                    sample.recycle()
                    for (p in pixels) {
                        val lum = (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p))
                        sum += lum
                    }
                    val avg = sum / 256.0
                    var variance = 0.0
                    for (p in pixels) {
                        val lum = (0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p))
                        variance += (lum - avg) * (lum - avg)
                    }

                    if (variance > bestScore) {
                        bestScore = variance
                        bestBitmap?.recycle()
                        bestBitmap = candidate
                    } else {
                        candidate.recycle()
                    }
                }
            }

            if (bestBitmap == null) {
                bestBitmap = retriever.getFrameAtTime(1_000_000) ?: retriever.frameAtTime
            }
            retriever.release()

            if (bestBitmap != null) {
                FileOutputStream(output).use { fos ->
                    bestBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                bestBitmap.recycle()
            }
        } catch (e: Exception) {
            android.util.Log.w("FFmpegRenderer", "Error extracting cinematic thumbnail: ${e.message}")
        }
    }

    private fun ensureAacAudio(inputFile: File, tempAacFile: File): File {
        if (!inputFile.exists() || inputFile.length() < 100L) return inputFile

        // Check if inputFile is already AAC in an MP4/M4A container
        try {
            val ext = MediaExtractor()
            ext.setDataSource(inputFile.absolutePath)
            var isAac = false
            for (i in 0 until ext.trackCount) {
                val format = ext.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.equals(MediaFormat.MIMETYPE_AUDIO_AAC, ignoreCase = true) || mime.contains("mp4a-latm")) {
                    isAac = true
                    break
                }
            }
            ext.release()
            if (isAac) {
                android.util.Log.d("FFmpegRenderer", "Input audio is already native AAC: ${inputFile.name}")
                return inputFile
            }
        } catch (_: Exception) {}

        // Convert audio (WAV / MP3 / PCM / OGG) to 16-bit PCM and encode as AAC M4A
        try {
            val decoded = com.example.util.FreeMusicManager.decodeAudioToPcm(inputFile)
            if (decoded != null && decoded.pcmData.isNotEmpty()) {
                val success = com.example.util.FreeMusicManager.encodePcmToAacM4a(
                    pcmData = decoded.pcmData,
                    sampleRate = decoded.sampleRate,
                    channelCount = decoded.channelCount,
                    outputFile = tempAacFile
                )
                if (success && tempAacFile.exists() && tempAacFile.length() > 500L) {
                    android.util.Log.d("FFmpegRenderer", "Converted audio (${inputFile.name}) to native AAC M4A: ${tempAacFile.length()} bytes")
                    return tempAacFile
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FFmpegRenderer", "Failed to convert audio to AAC: ${e.message}", e)
        }
        return inputFile
    }

    private fun muxVideoAndAudio(videoFile: File, audioFile: File, outputFile: File) {
        if (!audioFile.exists() || audioFile.length() < 100L) {
            android.util.Log.w("FFmpegRenderer", "Audio file missing or empty, copying video only")
            videoFile.copyTo(outputFile, overwrite = true)
            return
        }

        val tempAacFile = File(audioFile.parentFile ?: videoFile.parentFile, "temp_aac_${System.currentTimeMillis()}.m4a")
        val finalAudioFile = ensureAacAudio(audioFile, tempAacFile)

        var muxer: MediaMuxer? = null
        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxerStarted = false

        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            var videoTrackIdx = -1
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIdx = muxer.addTrack(format)
                    videoExtractor.selectTrack(i)
                    break
                }
            }

            audioExtractor = MediaExtractor().apply { setDataSource(finalAudioFile.absolutePath) }
            var audioTrackIdx = -1
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = muxer.addTrack(format)
                    audioExtractor.selectTrack(i)
                    break
                }
            }

            if (videoTrackIdx < 0) {
                throw IllegalStateException("No video track found in $videoFile")
            }

            muxer.start()
            muxerStarted = true

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Write video and audio samples interleaved by presentation timestamp
            var videoDone = videoTrackIdx < 0
            var audioDone = audioTrackIdx < 0

            while (!videoDone || !audioDone) {
                val videoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val audioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (!videoDone && (audioDone || videoTime <= audioTime)) {
                    bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        videoDone = true
                    } else {
                        bufferInfo.presentationTimeUs = videoExtractor.sampleTime
                        bufferInfo.flags = videoExtractor.sampleFlags
                        muxer.writeSampleData(videoTrackIdx, buffer, bufferInfo)
                        videoExtractor.advance()
                    }
                } else if (!audioDone) {
                    bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) {
                        audioDone = true
                    } else {
                        bufferInfo.presentationTimeUs = audioExtractor.sampleTime
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(audioTrackIdx, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }
            }

            android.util.Log.d("FFmpegRenderer", "Successfully muxed Video + Audio into ${outputFile.name} (${outputFile.length()} bytes, hasAudio=${audioTrackIdx >= 0})")
        } catch (e: Exception) {
            android.util.Log.e("FFmpegRenderer", "Error during muxVideoAndAudio: ${e.message}", e)
            videoFile.copyTo(outputFile, overwrite = true)
        } finally {
            try { videoExtractor?.release() } catch (_: Exception) {}
            try { audioExtractor?.release() } catch (_: Exception) {}
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (_: Exception) {}
            try {
                if (tempAacFile.exists()) {
                    tempAacFile.delete()
                }
            } catch (_: Exception) {}
        }
    }
}
