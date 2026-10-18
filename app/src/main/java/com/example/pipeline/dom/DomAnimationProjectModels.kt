package com.example.pipeline.dom

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Serializable

/**
 * Data contracts for 10-Scene DOM Animation Generation & Unified Video Rendering.
 */
data class DomSceneScript(
    val sceneNumber: Int,
    val sceneTitle: String,
    val durationSeconds: Int = 15,
    val narration: String = "",
    val narrationHindi: String = "",
    val narrationEnglish: String = "",
    val visualPrompt: String = "",
    val cameraMotion: String = "ZOOM_IN",
    val weatherAtmosphere: String = "NONE",
    val audioSfx: String = "NONE"
) : Serializable

data class DomVideoOptions(
    val category: String,
    val language: String,
    val durationSeconds: Int,
    val voiceName: String,
    val aspectRatio: String = "9:16",
    val showCaptions: Boolean = true
)

data class HtmlAnimationCode(
    val html: String,
    val css: String,
    val javascript: String,
    val fullHtmlDocument: String
) : Serializable

enum class DomWorkerStatus {
    IDLE,
    INITIALIZING,
    EXECUTING_DOM,
    CAPTURING_CODE,
    COMPLETED,
    FAILED
}

data class DomAnimatedScene(
    val sceneNumber: Int,
    val script: DomSceneScript,
    val animationCode: HtmlAnimationCode,
    val status: DomWorkerStatus = DomWorkerStatus.IDLE,
    val localHtmlFile: File? = null,
    val renderedClipFile: File? = null,
    val errorMessage: String? = null
) : Serializable

data class DomAnimation10ScenePackage(
    val projectId: String,
    val title: String,
    val englishTitle: String,
    val theme: String,
    val moral: String,
    val totalScenes: Int = 10,
    val totalDurationSeconds: Int = 150,
    val scenes: List<DomAnimatedScene>,
    val rawJsonOutput: String,
    val finalRenderedVideoFile: File? = null
) : Serializable {

    fun toCleanStructuredJsonObject(): JSONObject {
        val root = JSONObject()
        root.put("project_id", projectId)
        root.put("title", title)
        root.put("english_title", englishTitle)
        root.put("theme", theme)
        root.put("moral", moral)
        root.put("total_scenes", totalScenes)
        root.put("total_duration_seconds", totalDurationSeconds)

        val scenesArray = JSONArray()
        for (scene in scenes) {
            val scObj = JSONObject()
            scObj.put("scene_number", scene.sceneNumber)
            scObj.put("scene_title", scene.script.sceneTitle)
            scObj.put("duration_seconds", scene.script.durationSeconds)
            scObj.put("narration", scene.script.narration)
            scObj.put("narration_hindi", scene.script.narrationHindi)
            scObj.put("narration_english", scene.script.narrationEnglish)
            scObj.put("visual_prompt", scene.script.visualPrompt)
            scObj.put("camera_motion", scene.script.cameraMotion)
            scObj.put("weather_atmosphere", scene.script.weatherAtmosphere)
            scObj.put("audio_sfx", scene.script.audioSfx)

            val animObj = JSONObject()
            animObj.put("html", scene.animationCode.html)
            animObj.put("css", scene.animationCode.css)
            animObj.put("javascript", scene.animationCode.javascript)
            scObj.put("animation_code", animObj)

            scenesArray.put(scObj)
        }
        root.put("scenes", scenesArray)
        return root
    }
}
