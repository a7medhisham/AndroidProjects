package com.example.projectstories

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PageActivity : AppCompatActivity() {
    private lateinit var media:MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Toast.makeText(this, getString(R.string.welcome_the_party_d), Toast.LENGTH_SHORT).show()
        val text: TextView = findViewById(R.id.titleText)
        text.setOnClickListener {
            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
        }
        val playBtn:Button=findViewById(R.id.play_btn)
        val pauseBtn:Button=findViewById(R.id.pause_btn)
        val stopBtn: Button =findViewById(R.id.stop_btn)
        val media=MediaPlayer.create(this,R.raw.sound)
        playBtn.setOnClickListener {
            media.start()
        }
        pauseBtn.setOnClickListener {
            media.pause()
        }
        stopBtn.setOnClickListener {
            media.stop()
            media.prepare()
        }
    }
    override fun onPause() {
        super.onPause()
        media.release()
    }
}