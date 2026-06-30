package com.example.chatbot

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val conversationId: String,
    val text: String,
    val imageUrl: String? = null,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)