package com.example.util

import android.content.Context
import android.content.Intent
import com.example.pipeline.NotrackWebViewClient

data class CommunityPostItem(
    val id: String,
    val type: String,
    val title: String,
    val textContent: String,
    val pollOptions: List<String> = emptyList(),
    val hashtags: List<String> = emptyList()
)

object CommunityPostManager {

    suspend fun generateAutonomousCommunityPosts(
        category: String,
        channelName: String,
        notrackClient: NotrackWebViewClient?
    ): List<CommunityPostItem> {
        val client = notrackClient
            ?: throw IllegalStateException("AI provider unavailable; community post drafts were not generated.")
        val posts = client.askNotrackForCommunityPosts(category, channelName)
        if (posts.isEmpty()) {
            throw IllegalStateException("AI provider returned no community post drafts.")
        }
        return posts
    }

    fun sharePostContent(context: Context, post: CommunityPostItem) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, getFormattedPostText(post))
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Community Post to YouTube"))
    }

    fun publishDirectlyToYouTube(context: Context, post: CommunityPostItem) {
        val fullText = getFormattedPostText(post)

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("YouTube Community Post", fullText))
            android.widget.Toast.makeText(
                context,
                "Post draft copied. Review it before publishing in YouTube.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.util.Log.w("CommunityPostManager", "Could not copy community post draft: ${e.message}")
        }

        val packageManager = context.packageManager
        val studioIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fullText)
            setPackage("com.google.android.apps.youtube.creator")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val youtubeIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fullText)
            setPackage("com.google.android.youtube")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        when {
            studioIntent.resolveActivity(packageManager) != null -> context.startActivity(studioIntent)
            youtubeIntent.resolveActivity(packageManager) != null -> context.startActivity(youtubeIntent)
            else -> context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://studio.youtube.com")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }

    private fun getFormattedPostText(post: CommunityPostItem): String = buildString {
        appendLine(post.title)
        appendLine()
        appendLine(post.textContent)
        if (post.pollOptions.isNotEmpty()) {
            appendLine()
            appendLine("POLL CHOICES:")
            post.pollOptions.forEachIndexed { index, option ->
                appendLine("${index + 1}. $option")
            }
        }
        if (post.hashtags.isNotEmpty()) {
            appendLine()
            appendLine(post.hashtags.joinToString(" "))
        }
    }
}
