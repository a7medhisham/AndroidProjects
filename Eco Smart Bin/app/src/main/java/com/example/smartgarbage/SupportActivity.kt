package com.example.smartgarbage
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.candroid1.AdapterSupport

class SupportActivity : AppCompatActivity() {
    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_support)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val emergency = arrayListOf<Emergency>()
        emergency.add(Emergency(R.drawable.ambulance, getString(R.string.ambulance), "123"))
        emergency.add(Emergency(R.drawable.police, getString(R.string.police), "122"))
        emergency.add(Emergency(R.drawable.firestation, getString(R.string.fire_station), "180"))
        emergency.add(Emergency(R.drawable.workteam, getString(R.string.support_team), "19544"))
        val adapter = AdapterSupport(this, emergency)
        val rc: RecyclerView = findViewById(R.id.rv)
        rc.adapter = adapter
    }
}