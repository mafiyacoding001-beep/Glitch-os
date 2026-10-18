package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ImageItem
import com.example.data.model.ProjectDetailsResponse
import com.example.data.model.ProjectSummary
import com.example.data.model.StoryContent
import com.example.data.model.VisualPromptItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val topic: String,
    val language: String,
    val category: String,
    val duration: Int,
    val style: String,
    val aspectRatio: String,
    val createdAt: String,
    val status: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val storyJson: String = "",
    val promptsJson: String = "",
    val imagesJson: String = "",
    val pipelineStep: String = "IDLE"
) {
    fun toProjectSummary(): ProjectSummary {
        return ProjectSummary(
            id = id,
            title = title,
            topic = topic,
            language = language,
            category = category,
            duration = duration,
            style = style,
            aspectRatio = aspectRatio,
            createdAt = createdAt,
            status = status,
            video = videoUrl,
            thumbnail = thumbnailUrl
        )
    }

    fun toProjectDetails(): ProjectDetailsResponse {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val story = try {
            if (storyJson.isNotBlank()) moshi.adapter(StoryContent::class.java).fromJson(storyJson) else null
        } catch (e: Exception) { null }

        val prompts = try {
            if (promptsJson.isNotBlank()) {
                val type = Types.newParameterizedType(List::class.java, VisualPromptItem::class.java)
                moshi.adapter<List<VisualPromptItem>>(type).fromJson(promptsJson) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }

        val images = try {
            if (imagesJson.isNotBlank()) {
                val type = Types.newParameterizedType(List::class.java, ImageItem::class.java)
                moshi.adapter<List<ImageItem>>(type).fromJson(imagesJson) ?: emptyList()
            } else emptyList()
        } catch (e: Exception) { emptyList() }

        return ProjectDetailsResponse(
            project = toProjectSummary(),
            story = story,
            visualPrompts = prompts,
            images = images
        )
    }

    companion object {
        fun fromProjectSummary(summary: ProjectSummary): ProjectEntity {
            return ProjectEntity(
                id = summary.id,
                title = summary.title,
                topic = summary.topic,
                language = summary.language,
                category = summary.category,
                duration = summary.duration,
                style = summary.style,
                aspectRatio = summary.aspectRatio,
                createdAt = summary.createdAt,
                status = summary.status,
                videoUrl = summary.video,
                thumbnailUrl = summary.thumbnail,
                pipelineStep = "IDLE"
            )
        }
    }
}
