package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GLITCH", appName)
  }

  @Test
  fun `verify global fallback defaults to true and persists changes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settingsManager = SettingsManager(context)

    // Verify default value is true
    assertTrue("Global fallback should default to true", settingsManager.settingsFlow.value.globalFallbackEnabled)

    // Update to false
    val current = settingsManager.settingsFlow.value
    settingsManager.updateSettings(current.copy(globalFallbackEnabled = false))
    assertFalse("Global fallback should be false after updating", settingsManager.settingsFlow.value.globalFallbackEnabled)

    // Re-instantiate SettingsManager to verify SharedPreferences persistence
    val reloadedManager = SettingsManager(context)
    assertFalse("Global fallback should persist as false in SharedPreferences", reloadedManager.settingsFlow.value.globalFallbackEnabled)

    // Reset back to true
    reloadedManager.updateSettings(current.copy(globalFallbackEnabled = true))
    assertTrue("Global fallback should reset back to true", reloadedManager.settingsFlow.value.globalFallbackEnabled)
  }

  @Test
  fun testGetSubtitles() {
    try {
      com.example.util.NewPipeDownloader.init()
      val videoId = "dQw4w9WgXcQ"
      val videoUrl = "https://www.youtube.com/watch?v=$videoId"
      
      println("YT_OUTPUT --- Phase 1: NewPipeExtractor Test ---")
      try {
        val info = org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
            org.schabi.newpipe.extractor.ServiceList.YouTube,
            videoUrl
        )
        println("YT_OUTPUT Title = ${info.name}")
        val subtitles = info.subtitles ?: emptyList()
        println("YT_OUTPUT Subtitle count = ${subtitles.size}")
        subtitles.forEach {
          val lang = it.getLocale()?.getLanguage() ?: "unknown"
          val format = it.getFormat()
          val content = it.getUrl()
          println("YT_OUTPUT lang=${lang}, format=${format}, content=${content}")
        }
      } catch (e: Exception) {
        println("YT_OUTPUT Phase 1 failed: ${e.message}")
      }

      println("\nYT_OUTPUT --- Phase 2: Dual-Phase Fallback Test ---")
      kotlinx.coroutines.runBlocking {
        try {
          val cues = com.example.data.youtube.TrendPilotFetcher.fetchVideoTranscript(videoId, "Test Video")
          println("YT_OUTPUT Dual-Phase extracted cues count = ${cues.size}")
          cues.take(5).forEach { cue ->
            println("YT_OUTPUT [${cue.timestampFormatted}] ${cue.text}")
          }
        } catch (e: Exception) {
          println("YT_OUTPUT Phase 2 failed: ${e.message}")
        }
      }

    } catch (e: Exception) {
      println("YT_OUTPUT Error occurred: ${e.message}")
      e.printStackTrace()
    }
  }
}
