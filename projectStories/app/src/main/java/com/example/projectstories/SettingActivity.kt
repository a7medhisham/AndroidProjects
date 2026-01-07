package com.example.projectstories

import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val small = findViewById<RadioButton>(R.id.st)
        val medium = findViewById<RadioButton>(R.id.mt)
        val large = findViewById<RadioButton>(R.id.lt)
        val group: RadioGroup =findViewById(R.id.group)
        val text:TextView=findViewById(R.id.text)
        val et=intent.getStringExtra("text")
        text.text=et

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val savedSize = prefs.getInt("size", 22)
        text.textSize = savedSize.toFloat()

        when (savedSize) {
            16 -> small.isChecked = true
            22 -> medium.isChecked = true
            28 -> large.isChecked = true
        }
        group.setOnCheckedChangeListener { radioGroup, i ->
            val newSize = when(i) {
                R.id.st -> 16
                R.id.mt -> 22
                R.id.lt -> 28
                else -> 22
            }
            text.textSize = newSize.toFloat()
            changeSize(newSize)
            Toast.makeText(this, "Size changed!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun changeSize(i: Int) {
        val prefs=getSharedPreferences("settings", MODE_PRIVATE).edit()
        prefs.putInt("size",i)
        prefs.apply()
    }
}