package com.example.projectstories

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingActivity : AppCompatActivity() {

    private lateinit var tvPreview: TextView
    private lateinit var group: RadioGroup
    private lateinit var small: RadioButton
    private lateinit var medium: RadioButton
    private lateinit var large: RadioButton
    private var selectedSize = 22

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvPreview = findViewById(R.id.tvPreview)
        small = findViewById(R.id.st)
        medium = findViewById(R.id.mt)
        large = findViewById(R.id.lt)
        group = findViewById(R.id.group)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        selectedSize = prefs.getInt("size", 22)

        tvPreview.textSize = selectedSize.toFloat()

        when (selectedSize) {
            16 -> small.isChecked = true
            22 -> medium.isChecked = true
            28 -> large.isChecked = true
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            handleSizeChange(checkedId)
        }
    }
    fun onSmallClicked(view: View) {
        small.isChecked = true
        handleSizeChange(R.id.st)
    }

    fun onMediumClicked(view: View) {
        medium.isChecked = true
        handleSizeChange(R.id.mt)
    }

    fun onLargeClicked(view: View) {
        large.isChecked = true
        handleSizeChange(R.id.lt)
    }

    private fun handleSizeChange(checkedId: Int) {
        selectedSize = when(checkedId) {
            R.id.st -> {
                Toast.makeText(this, getString(R.string.small_selected), Toast.LENGTH_SHORT).show()
                16
            }
            R.id.mt -> {
                Toast.makeText(this, getString(R.string.medium_selected), Toast.LENGTH_SHORT).show()
                22
            }
            R.id.lt -> {
                Toast.makeText(this, getString(R.string.large_selected), Toast.LENGTH_SHORT).show()
                28
            }
            else -> 22
        }

        tvPreview.textSize = selectedSize.toFloat()
        saveSize(selectedSize)
        sendBroadcast(Intent("TEXT_SIZE_CHANGED"))
    }

    private fun saveSize(size: Int) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE).edit()
        prefs.putInt("size", size)
        prefs.apply()
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }
}