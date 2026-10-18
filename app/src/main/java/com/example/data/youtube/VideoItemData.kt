package com.example.data.youtube

data class VideoItemData(
    val id: String,
    val title: String,
    val views: String = "0",
    val likes: String = "0",
    val comments: String = "0",
    val duration: String = "0:59",
    val status: String = "PUBLISHED",
    val publishedAt: String = "",
    val tags: List<String> = emptyList(),
    val thumbnailUrl: String = "",
    val videoPath: String? = null
)
