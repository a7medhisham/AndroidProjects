package com.example.projectstories

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Toast.makeText(this, "Welcome Kids ", Toast.LENGTH_SHORT).show()

        val t1: TextView = findViewById(R.id.text)
        t1.setOnClickListener {
            val i = Intent(this, PageActivity::class.java)
            startActivity(i)
            finish()
        }

        val btn: Button = findViewById(R.id.btn)
        val text: TextView = findViewById(R.id.text)
        btn.setOnClickListener {
            val et = text.text.toString()
            val i = Intent(this, SettingActivity::class.java)
            i.putExtra("text", et)
            startActivity(i)
        }

        val btn2: Button = findViewById(R.id.btnChangeLanguage)
        btn2.setOnClickListener {
            val i = Intent(this, LanguageActivity::class.java)
            startActivity(i)
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val rc: RecyclerView = findViewById(R.id.rv)
        rc.layoutManager = LinearLayoutManager(this)
        rc.setHasFixedSize(true)

        val storyIds = listOf(
            "story_1", "story_2", "story_3", "story_4", "story_5",
            "story_6", "story_7", "story_8", "story_9", "story_10"
        )

        val stories = storyIds.mapIndexed { index, id ->
            Story(
                id = id,
                text1 = when(index) {
                    0 -> getString(R.string.the_story_of_a_duck)
                    1 -> getString(R.string.the_story_of_a_lion)
                    2 -> getString(R.string.the_story_of_a_cat)
                    3 -> getString(R.string.the_story_of_a_dog)
                    4 -> getString(R.string.the_story_of_a_giraffe)
                    5 -> getString(R.string.the_story_of_an_elephant)
                    6 -> getString(R.string.the_story_of_a_horse)
                    7 -> getString(R.string.the_story_of_a_monkey)
                    8 -> getString(R.string.the_story_of_a_rabbit)
                    else -> getString(R.string.the_story_of_a_penguin)
                },
                picture = when(index) {
                    0 -> R.drawable.duck
                    1 -> R.drawable.lion
                    2 -> R.drawable.cat
                    3 -> R.drawable.dog
                    4 -> R.drawable.giraffe
                    5 -> R.drawable.elephant
                    6 -> R.drawable.horse
                    7 -> R.drawable.monkey
                    8 -> R.drawable.rabbit
                    else -> R.drawable.penguin
                }
            )
        }

        val adapter = AdapterSories(this, ArrayList(stories)) { position ->
            val storyId = storyIds[position]

            val i = Intent(this@MainActivity, HomeActivity::class.java)
            i.putExtra("story_id", storyId)

            android.util.Log.d("MAIN_ACTIVITY", "Opening story: $storyId")

            startActivity(i)
        }

        rc.adapter = adapter
    }
    override fun onBackPressed() {
        val exit = ExitDialog()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }
}