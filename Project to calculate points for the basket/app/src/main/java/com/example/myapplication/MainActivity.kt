package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.example.myapplication.databinding.ActivityMainBinding
import com.github.barteksc.pdfviewer.PDFView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Toast.makeText(this, "Welcome :D", Toast.LENGTH_SHORT).show()
        var viewModel =ViewModelProvider(this)[Counter::class.java]
        viewModel.newCounter.observe(this){
            binding.text1.text=it.toString()
        }
        binding.btn2.setOnClickListener {
            viewModel.increment()
        }
        binding.btn4.setOnClickListener {
            viewModel.increment12()
        }
        binding.btn6.setOnClickListener {
            viewModel.increment13()
        }

        var viewModel2 =ViewModelProvider(this)[Counter::class.java]
        viewModel.newCounter2.observe(this){
            binding.text2.text=it.toString()
        }
        binding.btn1.setOnClickListener {
            viewModel2.increment2()
        }
        binding.btn3.setOnClickListener {
            viewModel2.increment22()
        }
        binding.btn5.setOnClickListener {
            viewModel2.increment23()
        }
        binding.btn7.setOnClickListener {
            Toast.makeText(this, "${viewModel.determineWinner()}", Toast.LENGTH_SHORT).show()
        }
        binding.btn8.setOnClickListener {
            viewModel.increment10()
            viewModel2.increment20()
        }
    }
    override fun onBackPressed() {
        val exit = Exit()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }
}

