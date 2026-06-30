package com.example.generatechatvhdlcode
//FIX VARIABLE
enum class Role { USER, BOT }

data class Message(val text: String, val role: Role)