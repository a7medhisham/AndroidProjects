package com.example.projectstories

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        Toast.makeText(this, getString(R.string.welcome_kids), Toast.LENGTH_SHORT).show()

        val t1: TextView = findViewById(R.id.text)
        t1.setOnClickListener {
            val i = Intent(this, PageActivity::class.java)
            startActivity(i)
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
        rc.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)
        rc.setHasFixedSize(true)

        val storyIds = listOf(
            "story_1","story_2","story_3", "story_4", "story_5",
            "story_6", "story_7", "story_8", "story_9", "story_10",
            "story_11","story_12","story_13","story_14","story_15",
            "story_16","story_17","story_18","story_19","story_20"
        )

        val stories = storyIds.mapIndexed { index, id ->
            Story(
                id = id,
                text1 = when(id) {
                    "story_1" -> getString(R.string.the_story_of_a_duck)
                    "story_2" -> getString(R.string.the_story_of_a_lion)
                    "story_3" -> getString(R.string.the_story_of_a_cat)
                    "story_4" -> getString(R.string.the_story_of_a_dog)
                    "story_5" -> getString(R.string.the_story_of_a_giraffe)
                    "story_6" -> getString(R.string.the_story_of_an_elephant)
                    "story_7" -> getString(R.string.the_story_of_a_horse)
                    "story_8" -> getString(R.string.the_story_of_a_monkey)
                    "story_9" -> getString(R.string.the_story_of_a_rabbit)
                    "story_10" -> getString(R.string.the_story_of_a_penguin)
                    "story_11" -> getString(R.string.the_story_of_a_tiger)
                    "story_12" -> getString(R.string.the_story_of_a_turtle)
                    "story_13" -> getString(R.string.the_story_of_a_bear)
                    "story_14" -> getString(R.string.the_story_of_a_dear)
                    "story_15" -> getString(R.string.the_story_of_a_owl)
                    "story_16" -> getString(R.string.the_story_of_a_fox)
                    "story_17" -> getString(R.string.the_story_of_a_sloth)
                    "story_18" -> getString(R.string.the_story_of_a_wolf)
                    "story_19" -> getString(R.string.the_story_of_a_ant)
                    else -> getString(R.string.the_story_of_a_bee)
                },
                picture = when(id) {
                    "story_1" -> R.drawable.duck
                    "story_2" -> R.drawable.lion
                    "story_3" -> R.drawable.cat
                    "story_4" -> R.drawable.dog
                    "story_5" -> R.drawable.giraffe
                    "story_6" -> R.drawable.elephant
                    "story_7" -> R.drawable.horse
                    "story_8" -> R.drawable.monkey
                    "story_9" -> R.drawable.rabbit
                    "story_10" -> R.drawable.penguin
                    "story_11" -> R.drawable.tiger
                    "story_12" -> R.drawable.turtle_4
                    "story_13" -> R.drawable.dub
                    "story_14" -> R.drawable.deer
                    "story_15" -> R.drawable.owl
                    "story_16" -> R.drawable.fox
                    "story_17" -> R.drawable.sloth
                    "story_18" -> R.drawable.wolf
                    "story_19" -> R.drawable.ant
                    else -> R.drawable.bee
                },
                isNew = id in listOf(
                    "story_11","story_12","story_13","story_14","story_15",
                    "story_16","story_17","story_18","story_19","story_20"
                )
            )
        }

        val adapter = AdapterSories(this, ArrayList(stories)) { position ->
            openStory(storyIds[position])
        }

        rc.adapter = adapter
    }

    private fun openStory(storyId: String) {
        val i = Intent(this@MainActivity, HomeActivity::class.java)
        i.putExtra("story_id", storyId)
        android.util.Log.d("MAIN_ACTIVITY", "Opening story: $storyId")
        startActivity(i)
    }

    override fun onBackPressed() {
        val exit = ExitDialog()
        exit.isCancelable = false
        exit.show(supportFragmentManager, null)
    }
}