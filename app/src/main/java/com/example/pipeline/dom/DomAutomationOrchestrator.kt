package com.example.pipeline.dom

import android.content.Context
import android.util.Log
import com.example.data.local.SettingsManager
import com.example.util.EdgeNeuralTtsService
import com.example.util.StoryPromptEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Generates a validated ten-scene story, ten parallel HTML animations, narration audio,
 * and a final playable video.
 */
class DomAutomationOrchestrator(
    private val context: Context
) {
    private val tag = "DomOrchestrator"
    private val workerPool = DomAutomationWorkerPool(context)
    private val frameRenderer = HtmlToVideoFrameRenderer(context)

    suspend fun runComplete10SceneDomVideoPipeline(
        topic: String,
        options: DomVideoOptions? = null,
        outputDirectory: File? = null,
        onProgressUpdate: (Int, String) -> Unit = { _, _ -> }
    ): DomAnimation10ScenePackage = withContext(Dispatchers.IO) {
        require(topic.isNotBlank()) { "Enter a real topic before starting video generation." }
        val saved = SettingsManager(context).settingsFlow.value
        val defaultLanguage = if (saved.defaultLanguage.startsWith("Hindi", ignoreCase = true)) {
            "Hindi"
        } else {
            "English"
        }
        val expectedVoicePrefix = if (defaultLanguage == "Hindi") "hi-" else "en-"
        val preferredVoice = EdgeNeuralTtsService.AVAILABLE_VOICES.firstOrNull {
            it.id == saved.ttsVoice && it.id.startsWith(expectedVoicePrefix)
        }?.id
        val videoOptions = options ?: DomVideoOptions(
            category = saved.defaultCategory,
            language = defaultLanguage,
            durationSeconds = saved.defaultDuration,
            voiceName = preferredVoice
                ?: if (defaultLanguage == "Hindi") "hi-IN-MadhurNeural" else "en-IN-PrabhatNeural",
            aspectRatio = saved.defaultAspect,
            showCaptions = saved.showCaptions
        )
        require(videoOptions.durationSeconds in 15..300) {
            "Video duration must be between 15 and 300 seconds."
        }
        require(videoOptions.language.isNotBlank() && videoOptions.category.isNotBlank()) {
            "Choose a language and category before starting video generation."
        }
        val selectedVoice = EdgeNeuralTtsService.AVAILABLE_VOICES
            .firstOrNull { it.id == videoOptions.voiceName }
            ?: throw IllegalArgumentException("Choose a supported narration voice.")
        require(
            (videoOptions.language.startsWith("Hindi", ignoreCase = true) &&
                selectedVoice.id.startsWith("hi-", ignoreCase = true)) ||
                (videoOptions.language.startsWith("English", ignoreCase = true) &&
                    selectedVoice.id.startsWith("en-", ignoreCase = true))
        ) {
            "Choose a voice that matches the selected narration language."
        }
        require(videoOptions.aspectRatio in setOf("9:16", "16:9", "1:1")) {
            "Choose a supported aspect ratio."
        }
        val targetDir = outputDirectory ?: File(
            context.filesDir,
            "dom_pipeline_${System.currentTimeMillis()}"
        )
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw IllegalStateException("Could not create video project directory.")
        }

        onProgressUpdate(5, "Connecting to Notrack to generate the full 10-scene master storyboard...")
        val storyClient = com.example.pipeline.NotrackWebViewClient.create(context)
        val story = try {
            val prompt = StoryPromptEngine.build10SceneVideoMasterPrompt(
                topic = topic,
                category = videoOptions.category,
                language = videoOptions.language,
                durationSeconds = videoOptions.durationSeconds,
                aspectRatio = videoOptions.aspectRatio
            )
            val response = storyClient.askNotrackForJsonObject(prompt, timeout = 120_000)
            parseStory(response, videoOptions)
        } finally {
            storyClient.close()
        }

        onProgressUpdate(15, "Validated ten scenes. Generating narration and ten independent animations...")
        val narration = story.scenes.joinToString(separator = " ") { it.narration }
        val audioFile = File(targetDir, "narration.mp3")
        if (audioFile.exists() && !audioFile.delete()) {
            throw IllegalStateException("Could not remove stale narration audio before synthesis.")
        }
        val audio = EdgeNeuralTtsService.synthesizeSpeech(
            context = context,
            text = narration,
            voiceName = videoOptions.voiceName,
            outputFile = audioFile
        ).getOrElse { throw it }
        require(audio.exists() && audio.length() > 1_000L) {
            "Speech generation returned no valid narration audio."
        }

        val scenePackage = workerPool.execute10ScenePipeline(
            topic = topic,
            storyScenes = story.scenes,
            outputDirectory = targetDir,
            title = story.title,
            englishTitle = story.englishTitle,
            theme = story.theme,
            moral = story.moral,
            options = videoOptions,
            onProgress = onProgressUpdate
        )
        onProgressUpdate(58, "Ten scene animations are ready. Rendering frames and mixing narration...")
        val renderedVideo = frameRenderer.renderAndStitch10Scenes(
            pkg = scenePackage,
            outputDir = targetDir,
            audioFile = audioFile,
            options = videoOptions,
            onProgress = { percent, status ->
                onProgressUpdate(58 + percent * 42 / 100, status)
            }
        )
        require(renderedVideo.exists() && renderedVideo.length() > 1_000L) {
            "Video rendering completed without a valid playable output."
        }
        onProgressUpdate(100, "Ten-scene video with narration is ready: ${renderedVideo.name}")
        Log.i(tag, "Generated video: ${renderedVideo.absolutePath}")
        scenePackage.copy(finalRenderedVideoFile = renderedVideo)
    }

    private fun parseStory(response: String, options: DomVideoOptions): StoryData {
        val json = JSONObject(response)
        require(json.optInt("total_scenes", -1) == 10) {
            "The story provider must declare exactly 10 scenes."
        }
        require(json.optInt("total_duration_seconds", -1) == options.durationSeconds) {
            "The story provider declared a duration that does not match the requested video duration."
        }
        val scenes = json.optJSONArray("scenes")
            ?: throw IllegalArgumentException("The story provider did not return a scenes array.")
        require(scenes.length() == 10) {
            "The story provider returned ${scenes.length()} scenes; exactly 10 are required."
        }
        val requestedDurations = List(10) { index ->
            options.durationSeconds / 10 +
                if (index < options.durationSeconds % 10) 1 else 0
        }
        val parsedScenes = (0 until scenes.length()).map { index ->
            val item = scenes.getJSONObject(index)
            val sceneNumber = item.optInt("scene_number", -1)
            val title = item.optString("scene_title").trim()
            val narration = item.optString("narration").trim()
            val visual = item.optString("visual_description").trim()
            val duration = item.optInt("duration_seconds", -1)
            require(sceneNumber == index + 1) {
                "The story provider returned an invalid scene number at position ${index + 1}."
            }
            require(title.isNotBlank() && narration.isNotBlank() && visual.isNotBlank()) {
                "Story scene ${index + 1} is missing its title, narration, or visual description."
            }
            require(duration == requestedDurations[index]) {
                "Story scene ${index + 1} has an invalid duration; expected ${requestedDurations[index]} seconds."
            }
            DomSceneScript(
                sceneNumber = sceneNumber,
                sceneTitle = title,
                durationSeconds = duration,
                narration = narration,
                narrationHindi = if (options.language.startsWith("Hindi", ignoreCase = true)) narration else "",
                narrationEnglish = if (options.language.startsWith("English", ignoreCase = true)) narration else "",
                visualPrompt = visual,
                cameraMotion = item.optString("camera_motion", "STATIC").trim().ifBlank { "STATIC" },
                weatherAtmosphere = item.optString("weather_atmosphere", "NONE").trim().ifBlank { "NONE" },
                audioSfx = item.optString("audio_sfx", "NONE").trim().ifBlank { "NONE" }
            )
        }
        val title = json.optString("title").trim()
        require(title.isNotBlank() && title.length <= 100) {
            "The story provider returned a missing or invalid video title."
        }
        return StoryData(
            title = title,
            englishTitle = json.optString("english_title", title).trim().ifBlank { title },
            theme = json.optString("theme").trim(),
            moral = json.optString("moral").trim(),
            scenes = parsedScenes
        )
    }

    private data class StoryData(
        val title: String,
        val englishTitle: String,
        val theme: String,
        val moral: String,
        val scenes: List<DomSceneScript>
    )
}
