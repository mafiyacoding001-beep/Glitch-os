package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeneratedDomProjectDetailsTest {

    @Test
    fun productionSettingsAndStoryboardPersistThroughLocalProjectDetails() {
        val project = ProjectEntity(
            id = "dom-project",
            title = "Rooftop garden",
            topic = "A city rooftop garden",
            language = "English",
            category = "Education",
            duration = 60,
            style = "HTML/CSS/JavaScript animation",
            aspectRatio = "16:9",
            createdAt = "2026-10-18 06:00:00",
            status = "completed",
            videoUrl = "file:///data/user/0/com.example/files/rendered.mp4",
            thumbnailUrl = "",
            storyJson = """
                {
                  "title": "Rooftop garden",
                  "productionSettings": {
                    "category": "Education",
                    "language": "English",
                    "voice": "en-IN-PrabhatNeural",
                    "aspectRatio": "16:9",
                    "captions": true,
                    "durationSeconds": 60
                  }
                }
            """.trimIndent(),
            promptsJson = """[{"index":1,"startSec":0.0,"endSec":6.0,"purpose":"Opening","prompt":"A rooftop garden at sunrise"}]"""
        )

        val details = project.toProjectDetails()

        assertNotNull(details.story?.productionSettings)
        assertEquals("en-IN-PrabhatNeural", details.story?.productionSettings?.voice)
        assertTrue(details.story?.productionSettings?.captions == true)
        assertEquals("16:9", details.project?.aspectRatio)
        assertEquals(1, details.visualPrompts.size)
        assertEquals("A rooftop garden at sunrise", details.visualPrompts.first().prompt)
    }
}
