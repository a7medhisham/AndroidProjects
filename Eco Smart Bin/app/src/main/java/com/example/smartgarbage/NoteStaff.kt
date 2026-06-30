package com.example.smartgarbage

data class NoteStaff(
    val notification_id: Int,
    val message: String,
    val status: String,
    val sent_at: String? = "Unknown"
)