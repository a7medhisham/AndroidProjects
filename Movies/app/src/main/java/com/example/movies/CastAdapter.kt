package com.example.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movies.databinding.ItemCastBinding

class CastAdapter(private val castList: List<Cast>) :
    RecyclerView.Adapter<CastAdapter.CastViewHolder>() {

    class CastViewHolder(val binding: ItemCastBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastViewHolder {
        val binding = ItemCastBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CastViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastViewHolder, position: Int) {
        val cast = castList[position]

        holder.binding.txtActorName.text = cast.name
        holder.binding.txtCharacter.text = cast.character

        if (!cast.profile_path.isNullOrEmpty()) {
            Glide.with(holder.itemView)
                .load("https://image.tmdb.org/t/p/w200${cast.profile_path}")
                .circleCrop()
                .into(holder.binding.imgActor)
        }
    }

    override fun getItemCount(): Int = castList.size
}