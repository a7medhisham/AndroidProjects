package com.example.projectstories

data class Story(
    val id: String,
    val text1: String,
    val picture: Int,
    val isNew: Boolean = false
)