package com.example.pipeline.dom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.util.FFmpegVideoSynthesizer
import com.example.util.VideoSceneConfig
import com.example.util.VideoSynthesisConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream

/**
 * Captures each generated HTML animation and renders the scene frames with the narration track.
 */
class HtmlToVideoFrameRenderer(
    private val context: Context
) {
    private val tag = "HtmlToVideoRenderer"
    private val animationFrameCount = 8
    private val animationFrameRate = 5

    suspend fun renderAndStitch10Scenes(
        pkg: DomAnimation10ScenePackage,
        outputDir: File,
        audioFile: File,
        options: DomVideoOptions,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        require(pkg.scenes.size == 10) { "Exactly 10 generated scenes are required for rendering." }
        require(audioFile.exists() && audioFile.length() > 1_000L) {
            "A valid narration audio file is required for rendering."
        }
        val (width, height) = when (options.aspectRatio) {
            "16:9" -> 1920 to 1080
            "1:1" -> 1080 to 1080
            else -> 1080 to 1920
        }
        val sceneConfigs = pkg.scenes.mapIndexed { index, scene ->
            val sceneNumber = scene.sceneNumber
            onProgress(5 + index * 7, "Capturing animated HTML/CSS/JS for scene $sceneNumber/10...")
            val htmlFile = scene.localHtmlFile
            require(htmlFile != null && htmlFile.exists() && htmlFile.length() > 0L) {
                "Generated HTML for scene $sceneNumber is missing."
            }

            val frameDir = File(outputDir, "scene_$sceneNumber")
            val animationFrames = captureHtmlAnimationFrames(
                htmlFile = htmlFile,
                outputDir = frameDir,
                sceneNumber = sceneNumber,
                width = width,
                height = height
            )
            val stillFrame = File(outputDir, "scene_${sceneNumber}_frame.jpg")
            animationFrames.first().copyTo(stillFrame, overwrite = true)
            require(stillFrame.length() > 500L) {
                "Could not save the first animation frame for scene $sceneNumber."
            }

            VideoSceneConfig(
                index = sceneNumber,
                imagePathOrUrl = stillFrame.absolutePath,
                animationFramePaths = animationFrames.map { it.absolutePath },
                animationFrameRate = animationFrameRate,
                durationMs = scene.script.durationSeconds * 1_000L,
                caption = if (options.showCaptions) scene.script.narration else "",
                cameraMotion = scene.script.cameraMotion,
                transition = "random",
                visualPrompt = scene.script.visualPrompt
            )
        }

        val synthesisConfig = VideoSynthesisConfig(
            title = pkg.title,
            description = pkg.moral,
            hashtags = emptyList(),
            tags = emptyList(),
            scenes = sceneConfigs,
            audioPath = audioFile.absolutePath,
            outputWidth = width,
            outputHeight = height,
            fps = 30,
            enableKenBurns = true,
            enableVignette = true,
            enableKineticCaptions = options.showCaptions,
            captionHighlightColorHex = "#FFE600",
            autoRandomTransitions = true
        )
        onProgress(75, "Encoding ten animated scenes and mixing the narration track...")
        val renderedVideo = FFmpegVideoSynthesizer.synthesizeFinalVideo(
            context = context,
            config = synthesisConfig,
            onProgress = { progress, status ->
                onProgress(75 + (progress * 25).toInt(), status)
            }
        ).getOrElse { throw it }
        require(renderedVideo.exists() && renderedVideo.length() > 1_000L) {
            "Video renderer did not create a valid playable output."
        }
        onProgress(100, "Ten-scene animated video with narration is ready: ${renderedVideo.name}")
        Log.i(tag, "Rendered video: ${renderedVideo.absolutePath} (${renderedVideo.length()} bytes)")
        renderedVideo
    }

    private suspend fun captureHtmlAnimationFrames(
        htmlFile: File,
        outputDir: File,
        sceneNumber: Int,
        width: Int,
        height: Int
    ): List<File> {
        val pageReady = CompletableDeferred<WebView>()
        val htmlContent = htmlFile.readText(Charsets.UTF_8)
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw IllegalStateException("Could not create animation frame directory for scene $sceneNumber.")
        }

        val webView = withContext(Dispatchers.Main) {
            WebView(context).apply {
                setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, width, height)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (view != null && !pageReady.isCompleted) pageReady.complete(view)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true && !pageReady.isCompleted) {
                            pageReady.completeExceptionally(
                                IllegalStateException(
                                    "Could not load HTML for scene $sceneNumber: ${error?.description}"
                                )
                            )
                        }
                    }
                }
                loadDataWithBaseURL(
                    "https://glitch.local/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }

        try {
            val webView = withTimeoutOrNull(15_000) { pageReady.await() }
                ?: throw IllegalStateException("Timed out loading HTML for scene $sceneNumber.")
            return buildList(animationFrameCount) {
                repeat(animationFrameCount) { frameIndex ->
                    delay(200L)
                    val bitmap = withContext(Dispatchers.Main) {
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { frame ->
                            webView.draw(Canvas(frame))
                        }
                    }
                    val frameFile = File(outputDir, "animation_${frameIndex.toString().padStart(2, '0')}.jpg")
                    try {
                        val saved = FileOutputStream(frameFile).use { output ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
                        }
                        require(saved && frameFile.exists() && frameFile.length() > 500L) {
                            "Could not save animation frame ${frameIndex + 1} for scene $sceneNumber."
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    add(frameFile)
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }
}
