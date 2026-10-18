package com.example.util

import android.content.Context
import android.util.Log
import com.example.pipeline.DeepAIWebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DeepAiImageService {

    private const val TAG = "DeepAiImageService"

    /**
     * Generates a genuine AI-rendered vertical image via DeepAI DOM automation scraper.
     * Flow: Open URL -> Paste Prompt into DOM -> Click Submit -> Live Check DOM -> Fetch & Download Image.
     */
    suspend fun generateDeepAiImage(
        context: Context,
        sceneNumber: Int,
        totalScenes: Int,
        prompt: String,
        cameraAngle: String,
        outputFile: File,
        onLog: (tag: String, message: String) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()

            onLog("AI_IMAGE_START", "🎨 [1/3] Launching DOM Automation Image Scraper for Scene #$sceneNumber/$totalScenes...")

            val cleanPrompt = extractCoreScenePrompt(prompt, cameraAngle)
            val masterPrompt = "Masterpiece 8K vertical 9:16 portrait photograph, cinematic dramatic lighting, photorealistic: $cleanPrompt. No text, no watermark, ultra detailed."

            onLog("AI_IMAGE_PROMPT", "📋 [2/3] DOM Prompt: ${masterPrompt.take(80)}...")

            val client = DeepAIWebViewClient.create(context)
            client.generateImage(
                prompt = masterPrompt,
                output = outputFile,
                onStepLog = { logMsg ->
                    onLog("DOM_SCRAPER", logMsg)
                }
            )

            if (!outputFile.exists() || outputFile.length() <= 5_000) {
                throw IllegalStateException("DeepAI DOM Scraper failed to save image for scene $sceneNumber")
            }

            onLog("AI_IMAGE_SUCCESS", "✅ [3/3] AI Visual Ready: Scene #$sceneNumber (${outputFile.name}, ${outputFile.length()} bytes, 9:16 HD)")
            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating image via DOM automation for scene $sceneNumber: ${e.message}")
            Result.failure(e)
        }
    }

    private fun extractCoreScenePrompt(rawPrompt: String, cameraAngle: String): String {
        var clean = rawPrompt
            .replace(Regex("""^Scene \d+:\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""9:16 vertical (hyper-realistic shot,?)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""photorealistic details,?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""8k render,?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""octane render,?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""cinematic storytelling visual\.?""", RegexOption.IGNORE_CASE), "")
            .trim()

        if (clean.isBlank()) {
            clean = "mysterious cinematic scene, dramatic lighting, 8k render"
        }

        return "$clean, $cameraAngle, vertical 9:16 cinematic lighting, hyper-realistic 8k render"
    }
}
