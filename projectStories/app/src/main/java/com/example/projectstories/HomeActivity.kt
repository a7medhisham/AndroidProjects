package com.example.projectstories

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val title: TextView = findViewById(R.id.text1)
        val story: TextView = findViewById(R.id.text)
        val image: ImageView = findViewById(R.id.image)
        title.text = intent.getStringExtra("stories")
        story.text = intent.getStringExtra("storie")
        image.setImageResource(intent.getIntExtra("images", -1))
        MediaPlayer
            .create(this,intent.getIntExtra("sounds",-2))
            .start()
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val size = prefs.getInt("size", 22).toFloat()
        story.textSize=size
        title.textSize=size + 8
    }
}