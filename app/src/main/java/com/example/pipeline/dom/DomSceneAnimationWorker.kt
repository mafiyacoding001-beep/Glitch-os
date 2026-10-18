package com.example.pipeline.dom

import android.content.Context
import android.util.Log
import com.example.pipeline.NotrackWebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Generates one scene's HTML/CSS/JavaScript through an isolated Notrack WebView session.
 */
class DomSceneAnimationWorker(
    private val workerId: Int,
    private val context: Context
) {
    private val TAG = "DomWorker-$workerId"

    suspend fun generateSceneAnimation(
        scene: DomSceneScript,
        outputDir: File,
        promptTopic: String,
        options: DomVideoOptions
    ): DomAnimatedScene = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting isolated Notrack generation for scene ${scene.sceneNumber}: ${scene.sceneTitle}")
        val client = NotrackWebViewClient.create(context)
        val rawGeneratedCode = try {
            client.askNotrackForText(buildDomPrompt(scene, promptTopic, options), timeout = 90_000)
        } finally {
            client.close()
        }
        require(rawGeneratedCode.isNotBlank()) {
            "Notrack returned no animation code for scene ${scene.sceneNumber}."
        }

        val animationCode = HtmlAnimationSanitizer.sanitizeAndExtractCode(rawGeneratedCode, scene)

        val sceneHtmlFile = File(outputDir, "scene_${scene.sceneNumber}_anim.html")
        FileOutputStream(sceneHtmlFile).use { fos ->
            fos.write(animationCode.fullHtmlDocument.toByteArray(Charsets.UTF_8))
        }
        require(sceneHtmlFile.length() > 0L) {
            "Could not save generated HTML for scene ${scene.sceneNumber}."
        }

        Log.i(TAG, "Generated scene ${scene.sceneNumber} (${sceneHtmlFile.length()} bytes)")

        return@withContext DomAnimatedScene(
            sceneNumber = scene.sceneNumber,
            script = scene,
            animationCode = animationCode,
            status = DomWorkerStatus.COMPLETED,
            localHtmlFile = sceneHtmlFile
        )
    }

    private fun buildDomPrompt(
        scene: DomSceneScript,
        topic: String,
        options: DomVideoOptions
    ): String {
        return """
You are generating animation source code for one scene in a rendered video.
Topic: ${JSONObject.quote(topic)}
Category: ${JSONObject.quote(options.category)}
Narration language: ${JSONObject.quote(options.language)}
Scene ${scene.sceneNumber} of 10: ${JSONObject.quote(scene.sceneTitle)}
Scene duration: ${scene.durationSeconds} seconds
Narration: ${JSONObject.quote(scene.narration)}
Visual direction: ${JSONObject.quote(scene.visualPrompt)}
Camera movement: ${JSONObject.quote(scene.cameraMotion)}
Atmosphere: ${JSONObject.quote(scene.weatherAtmosphere)}
Aspect ratio: ${options.aspectRatio}

Return one complete, self-contained HTML5 document with meaningful scene-specific markup,
inline CSS keyframe animations, and JavaScript animation code. Target the ${options.aspectRatio}
viewport. Make the animation deterministic from elapsed time so still-frame capture works.
Use no network requests, external fonts, external images, external libraries, APIs, or remote
resources. Keep all code local to this scene. Do not include markdown or explanatory prose.
Do not output placeholders or ellipses. Include a full <html>, <body>, <style>, and <script>.
        """.trimIndent()
    }
}
