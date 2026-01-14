package com.example.projectstories

import android.app.Activity
import android.media.MediaPlayer
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class AdapterSories(
    private val activity: Activity,
    private val stories: ArrayList<Story>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<AdapterSories.Item>() {

    private var mediaPlayer: MediaPlayer? = null

    class Item(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val text: TextView = view.findViewById(R.id.text)
        val card: CardView = view.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item {
        val view = activity.layoutInflater.inflate(R.layout.list, parent, false)
        return Item(view)
    }

    override fun getItemCount() = stories.size

    override fun onBindViewHolder(holder: Item, position: Int) {
        val story = stories[position]

        holder.image.setImageResource(story.picture)
        holder.text.text = story.text1

        holder.card.setOnClickListener {

            mediaPlayer?.release()
            mediaPlayer = null

            val soundRes = getAnimalSound(story.id)
            mediaPlayer = MediaPlayer.create(activity, soundRes)
            mediaPlayer?.start()

            mediaPlayer?.setOnCompletionListener {
                onItemClick(position)
            }
        }
    }

    private fun getAnimalSound(storyId: String): Int {
        return when (storyId) {
            "story_1" -> R.raw.duck
            "story_2" -> R.raw.lion
            "story_3" -> R.raw.cat
            "story_4" -> R.raw.dog
            "story_5" -> R.raw.giraffe
            "story_6" -> R.raw.elephant
            "story_7" -> R.raw.horse
            "story_8" -> R.raw.monkey
            "story_9" -> R.raw.rabbit
            "story_10" -> R.raw.pinguin
            else -> R.raw.duck
        }
    }
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
