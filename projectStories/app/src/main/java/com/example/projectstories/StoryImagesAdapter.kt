package com.example.projectstories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.projectstories.databinding.ItemStoryImageBinding

class StoryImagesAdapter(private val storyId: String) :
    RecyclerView.Adapter<StoryImagesAdapter.ImageViewHolder>() {

    private val images = getImagesForStory(storyId)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemStoryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(
        private val binding: ItemStoryImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageRes: Int) {
            binding.imageView.setImageResource(imageRes)
            binding.imageView.adjustViewBounds = true
            binding.imageView.scaleType =
                android.widget.ImageView.ScaleType.FIT_CENTER
        }
    }

    private fun getImagesForStory(storyId: String): List<Int> {
        return when (storyId) {
            "story_1" -> listOf(R.drawable.duck, R.drawable.duck_2, R.drawable.duck_3)
            "story_2" -> listOf(R.drawable.lion, R.drawable.lion_2, R.drawable.lion_3)
            "story_3" -> listOf(R.drawable.cat, R.drawable.cat_2, R.drawable.cat_3)
            "story_4" -> listOf(R.drawable.dog, R.drawable.dog_2, R.drawable.dog_3)
            "story_5" -> listOf(R.drawable.giraffe, R.drawable.giraffe_2, R.drawable.giraffe_3)
            "story_6" -> listOf(R.drawable.elephant, R.drawable.elephant_2, R.drawable.elephant_3)
            "story_7" -> listOf(R.drawable.horse, R.drawable.horse_2, R.drawable.horse_3)
            "story_8" -> listOf(R.drawable.monkey, R.drawable.monkey_2, R.drawable.monkey_3)
            "story_9" -> listOf(R.drawable.rabbit, R.drawable.rabbit_2, R.drawable.rabbit_3)
            "story_10" -> listOf(R.drawable.penguin, R.drawable.penguin_2, R.drawable.penguin_3)
            "story_11" -> listOf(R.drawable.tiger, R.drawable.tiger_2, R.drawable.tiger_3)
            "story_12" -> listOf(R.drawable.turtle, R.drawable.turtle_2, R.drawable.turtle_3)
            "story_13" -> listOf(R.drawable.dub, R.drawable.dub_2, R.drawable.dub_3)
            "story_14" -> listOf(R.drawable.deer, R.drawable.deer_2, R.drawable.deer_3)
            "story_15" -> listOf(R.drawable.owl, R.drawable.owl_2, R.drawable.owl_3)
            "story_16" -> listOf(R.drawable.fox, R.drawable.fox_2, R.drawable.fox_3)
            "story_17" -> listOf(R.drawable.sloth, R.drawable.sloth_2, R.drawable.sloth_3)
            "story_18" -> listOf(R.drawable.wolf, R.drawable.wolf_2, R.drawable.wolf_3)
            "story_19" -> listOf(R.drawable.ant, R.drawable.ant_2, R.drawable.ant_3)
            "story_20" -> listOf(R.drawable.bee, R.drawable.bee_2, R.drawable.bee_3)
            else -> listOf(R.drawable.duck, R.drawable.duck_2, R.drawable.duck_3)
        }
    }
}
