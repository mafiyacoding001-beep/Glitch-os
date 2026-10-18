package com.example.pipeline.dom

import android.content.Context

/**
 * Compatibility wrapper for callers of the previous advanced pipeline entry point.
 */
class AdvancedDomOrchestrator(
    context: Context
) {
    private val orchestrator = DomAutomationOrchestrator(context)

    suspend fun executeAdvancedPipeline(
        topic: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): DomAnimation10ScenePackage = orchestrator.runComplete10SceneDomVideoPipeline(
        topic = topic,
        onProgressUpdate = onProgress
    )
}
