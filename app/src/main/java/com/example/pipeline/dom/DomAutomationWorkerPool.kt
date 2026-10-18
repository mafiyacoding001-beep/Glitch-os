package com.example.pipeline.dom

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Generates exactly ten isolated Notrack scene sessions in parallel.
 */
class DomAutomationWorkerPool(
    private val context: Context
) {
    private val tag = "DomWorkerPool"

    suspend fun execute10ScenePipeline(
        topic: String,
        storyScenes: List<DomSceneScript>,
        outputDirectory: File,
        title: String,
        englishTitle: String,
        theme: String,
        moral: String,
        options: DomVideoOptions,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): DomAnimation10ScenePackage = withContext(Dispatchers.IO) {
        require(storyScenes.size == 10) {
            "The story provider must return exactly 10 scenes."
        }
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw IllegalStateException("Could not create video project directory.")
        }

        val projectId = UUID.randomUUID().toString()
        val completedCount = AtomicInteger(0)
        val workers = (1..10).map { DomSceneAnimationWorker(it, context) }
        Log.i(tag, "Starting ten isolated Notrack scene sessions for '$topic'.")
        onProgress(15, "Sending ten scene-specific HTML/CSS/JavaScript prompts to Notrack in parallel.")

        val generatedScenes = storyScenes.mapIndexed { index, scene ->
            async {
                val generated = workers[index].generateSceneAnimation(
                    scene = scene,
                    outputDir = outputDirectory,
                    promptTopic = topic,
                    options = options
                )
                val completed = completedCount.incrementAndGet()
                onProgress(15 + completed * 40 / 10, "Animation code generated for $completed/10 scenes.")
                generated
            }
        }.awaitAll()

        require(generatedScenes.size == 10 && generatedScenes.all { scene ->
            scene.status == DomWorkerStatus.COMPLETED &&
                scene.localHtmlFile?.exists() == true &&
                scene.animationCode.html.isNotBlank() &&
                scene.animationCode.css.isNotBlank() &&
                scene.animationCode.javascript.isNotBlank()
        }) {
            "One or more scene animations were not generated successfully."
        }

        val scenePackage = DomAnimation10ScenePackage(
            projectId = projectId,
            title = title,
            englishTitle = englishTitle,
            theme = theme,
            moral = moral,
            totalScenes = 10,
            totalDurationSeconds = generatedScenes.sumOf { it.script.durationSeconds },
            scenes = generatedScenes,
            rawJsonOutput = ""
        )
        val jsonString = scenePackage.toCleanStructuredJsonObject().toString(2)
        File(outputDirectory, "story_10_scenes_animation.json")
            .writeText(jsonString, Charsets.UTF_8)
        Log.i(tag, "Saved generated scene package in ${outputDirectory.absolutePath}.")
        scenePackage.copy(rawJsonOutput = jsonString)
    }
}
