package com.example.data.network

data class FirestoreUser(
    val email: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val lastLogin: Long = System.currentTimeMillis()
)

data class FirestoreProject(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val topic: String = "",
    val language: String = "English",
    val category: String = "Technology",
    val duration: Int = 0,
    val style: String = "Cinematic",
    val aspectRatio: String = "9:16",
    val status: String = "completed",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val storyJson: String = "",
    val promptsJson: String = "",
    val imagesJson: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
