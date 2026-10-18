package com.example.pipeline.dom

import com.example.util.StoryPromptEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DomVideoPromptAndSanitizerTest {

    @Test
    fun storyboardPromptUsesRequestedSettingsAndBalancedSceneDurations() {
        val prompt = StoryPromptEngine.build10SceneVideoMasterPrompt(
            topic = "A city rooftop garden",
            category = "Education",
            language = "English",
            durationSeconds = 17,
            aspectRatio = "16:9"
        )

        assertTrue(prompt.contains("A city rooftop garden"))
        assertTrue(prompt.contains("\"Education\""))
        assertTrue(prompt.contains("\"English\""))
        assertTrue(prompt.contains("Total target duration: 17 seconds"))
        assertTrue(prompt.contains("Aspect ratio: 16:9"))
        assertTrue(prompt.contains("2, 2, 2, 2, 2, 2, 2, 1, 1, 1 seconds"))
    }

    @Test
    fun storyboardPromptRejectsMissingTopicAndOutOfRangeDuration() {
        assertThrows(IllegalArgumentException::class.java) {
            StoryPromptEngine.build10SceneVideoMasterPrompt("", "Education", "English", 60, "9:16")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StoryPromptEngine.build10SceneVideoMasterPrompt("Garden", "Education", "English", 10, "9:16")
        }
    }

    @Test
    fun emotionalStoryPromptRequiresAnExplicitTopic() {
        assertThrows(IllegalArgumentException::class.java) {
            StoryPromptEngine.build10SceneEmotionalPrompt("")
        }
    }

    @Test
    fun sanitizerRemovesNestedStyleAndScriptBeforeCreatingStandaloneDocument() {
        val sanitized = HtmlAnimationSanitizer.sanitizeAndExtractCode(
            """
            <!doctype html>
            <html>
              <head><style>body { margin: 0; }</style></head>
              <body>
                <main class="scene">Rooftop garden</main>
                <style>.scene { color: green; }</style>
                <script>document.body.dataset.ready = "true";</script>
              </body>
            </html>
            """.trimIndent(),
            DomSceneScript(sceneNumber = 1, sceneTitle = "Rooftop garden")
        )

        assertTrue(sanitized.html.contains("Rooftop garden"))
        assertFalse(sanitized.html.contains("<script"))
        assertFalse(sanitized.html.contains("<style"))
        assertEquals(1, Regex("<style>").findAll(sanitized.fullHtmlDocument).count())
        assertEquals(1, Regex("<script>").findAll(sanitized.fullHtmlDocument).count())
    }

    @Test
    fun sanitizerRejectsIncompleteAnimationAndRemoteJavaScript() {
        val scene = DomSceneScript(sceneNumber = 2, sceneTitle = "Garden")
        assertThrows(IllegalArgumentException::class.java) {
            HtmlAnimationSanitizer.sanitizeAndExtractCode(
                "<html><body><div>Garden</div><style>.x{}</style></body></html>",
                scene
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            HtmlAnimationSanitizer.sanitizeAndExtractCode(
                """<html><body><script src="https://example.test/animation.js"></script><style>.x{}</style></body></html>""",
                scene
            )
        }
    }
}
