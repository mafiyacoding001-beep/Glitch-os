package com.example.pipeline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidVoiceGeneratorTest {

    @Test
    fun blankNarrationIsRejectedWithoutCreatingSyntheticAudio() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(context.cacheDir, "blank-narration-test.wav")
        output.delete()

        val error = try {
            AndroidVoiceGenerator(context).generate(
                text = "  \n",
                language = "English",
                output = output
            )
            null
        } catch (e: Exception) {
            e
        }

        if (error !is IllegalArgumentException) {
            throw AssertionError("Expected narration validation to fail.", error)
        }
        assertTrue(error.message.orEmpty().contains("Narration text is required"))
        assertFalse(output.exists())
    }

    @Test
    fun unsupportedLanguageIsRejectedInsteadOfFallingBackToEnglish() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val output = File(context.cacheDir, "unsupported-language-test.wav")
        output.delete()

        val error = try {
            AndroidVoiceGenerator(context).generate(
                text = "A real narration script.",
                language = "Italian",
                output = output
            )
            null
        } catch (e: Exception) {
            e
        }

        if (error !is IllegalArgumentException) {
            throw AssertionError("Expected unsupported narration language to be rejected.", error)
        }
        assertTrue(error.message.orEmpty().contains("Unsupported narration language"))
        assertFalse(output.exists())
    }
}
