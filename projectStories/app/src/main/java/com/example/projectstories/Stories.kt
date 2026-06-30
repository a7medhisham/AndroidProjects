package com.example.projectstories

data class Stories( val id: String = "",
                    val title: Map<String, String> = emptyMap(),
                    val text: Map<String, String> = emptyMap(),
                    val images: List<String> = emptyList(),
                    val audio: Map<String, String> = emptyMap())
