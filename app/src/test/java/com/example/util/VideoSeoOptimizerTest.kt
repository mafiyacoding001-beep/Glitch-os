package com.example.util

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
class VideoSeoOptimizerTest {

    @Test
    fun metadataSuggestionsUseTheSuppliedTopicWithoutPerformanceClaims() {
        val result = VideoSeoOptimizer.generateDeepVideoSeo(
            topic = "Urban gardening",
            category = "Education",
            language = "English"
        )

        assertTrue(result.titleVariations.all { it.title.contains("Urban gardening", ignoreCase = true) })
        assertTrue(result.titleVariations.all { it.type.isNotBlank() })
        assertFalse(result.suggestedTags.any { it.equals("shorts", ignoreCase = true) })
        assertEquals(
            100,
            VideoSeoOptimizer.calculateMetadataChecklistScore(
                topic = result.topic,
                title = result.currentSelectedTitle,
                description = result.richDescription.fullDescriptionText,
                tags = result.suggestedTags
            )
        )
    }

    @Test
    fun metadataGenerationRejectsBlankTopics() {
        assertThrows(IllegalArgumentException::class.java) {
            VideoSeoOptimizer.generateDeepVideoSeo(
                topic = "  ",
                category = "Education",
                language = "English"
            )
        }
    }

    @Test
    fun frameSamplePlansUseTheActualDuration() {
        val plans = VideoSeoOptimizer.computeFrameSamplePlans(10.0)

        assertEquals(5, plans.size)
        assertEquals(listOf(1.2, 3.5, 5.8, 7.8, 9.2), plans.map { it.timestampSec })
        assertTrue(plans.all { it.timestampSec in 0.0..10.0 })
    }

    @Test
    fun frameSamplePlansDoNotInventADuration() {
        assertTrue(VideoSeoOptimizer.computeFrameSamplePlans(0.0).isEmpty())
        assertTrue(VideoSeoOptimizer.computeFrameSamplePlans(Double.NaN).isEmpty())
    }
}
