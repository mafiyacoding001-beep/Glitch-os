package com.example.data.repository

import com.example.data.local.ProjectEntity
import com.example.data.local.UserEntity
import com.example.data.network.FirestoreProject
import com.example.data.network.FirestoreUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun syncUser(user: UserEntity) {
        val firestoreUser = FirestoreUser(
            email = user.email,
            name = user.name,
            avatarUrl = user.avatarUrl,
            lastLogin = user.lastLogin
        )
        val authUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!authUid.isNullOrBlank()) {
            try {
                firestore.collection("users").document(authUid).set(firestoreUser, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreRepository", "Sync to users/$authUid failed: ${e.message}")
            }
        }
        if (user.email.isNotBlank()) {
            try {
                firestore.collection("users").document(user.email).set(firestoreUser, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreRepository", "Sync to users/${user.email} failed: ${e.message}")
            }
        }
    }

    suspend fun saveProject(project: ProjectEntity, userEmail: String) {
        val firestoreProject = FirestoreProject(
            id = project.id,
            userId = userEmail,
            title = project.title,
            topic = project.topic,
            language = project.language,
            category = project.category,
            duration = project.duration,
            style = project.style,
            aspectRatio = project.aspectRatio,
            status = project.status,
            videoUrl = project.videoUrl,
            thumbnailUrl = project.thumbnailUrl,
            storyJson = project.storyJson,
            promptsJson = project.promptsJson,
            imagesJson = project.imagesJson,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("projects").document(project.id).set(firestoreProject).await()
    }

    suspend fun getProjectsForUser(userEmail: String): List<FirestoreProject> {
        val snapshot = firestore.collection("projects")
            .whereEqualTo("userId", userEmail)
            .get()
            .await()
        return snapshot.toObjects(FirestoreProject::class.java)
    }
}
