package com.example.projectstories

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot

class StoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collectionRef = db.collection("Stories")

    suspend fun getStories(): List<Stories> {
        val snapshot = collectionRef.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toStory()
        }
    }

    suspend fun getStoryById(id: String): Stories? {
        val doc = collectionRef.document(id).get().await()
        return if (doc.exists()) doc.toStory() else null
    }
}
fun DocumentSnapshot.toStory(): Stories? {
    return try {
        Stories(
            id = id,
            title = get("title") as? Map<String, String> ?: emptyMap(),
            text = get("text") as? Map<String, String> ?: emptyMap(),
            images = get("images") as? List<String> ?: emptyList(),
            audio = get("audio") as? Map<String, String> ?: emptyMap()
        )
    } catch (e: Exception) {
        null
    }
}