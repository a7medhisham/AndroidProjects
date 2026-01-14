package com.example.projectstories

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projectstories.databinding.ActivityLanguageBinding

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnArabic.setOnClickListener { saveLangAndGo("ar") }
        binding.btnEnglish.setOnClickListener { saveLangAndGo("en") }
        binding.btnGerman.setOnClickListener { saveLangAndGo("de") }
        binding.btnFrench.setOnClickListener { saveLangAndGo("fr") }
    }

    private fun saveLangAndGo(lang: String) {
        LanguageManager.saveLanguage(this, lang)

        val intent = Intent(this,MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
