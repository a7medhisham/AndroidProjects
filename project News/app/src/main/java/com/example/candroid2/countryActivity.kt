package com.example.candroid2

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.candroid2.databinding.ActivityCountryBinding

class countryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityCountryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.countryGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.us_rb -> changeCountry("us")
                R.id.de_rb -> changeCountry("de")
            }
        }
    }

    private fun changeCountry(countryCode: String) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE).edit()
        prefs.putString("code", countryCode)
        prefs.apply()
    }

}