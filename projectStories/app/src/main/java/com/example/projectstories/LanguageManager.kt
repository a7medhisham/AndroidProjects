package com.example.projectstories

import android.content.Context

object LanguageManager {

    fun saveLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString("lang", lang).apply()
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getString("lang", "en") ?: "en"
    }
}