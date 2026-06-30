package com.example.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movies.databinding.ItemFavoriteBinding

class FavoritesAdapter(
    private val favorites: List<FavoriteMovie>,
    private val onItemClick: (FavoriteMovie) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {

    class FavoriteViewHolder(val binding: ItemFavoriteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val favorite = favorites[position]

        holder.binding.txtTitle.text = favorite.title
        holder.binding.txtRating.text = "⭐ ${favorite.voteAverage}"
        holder.binding.txtOverview.text = favorite.overview

        Glide.with(holder.itemView)
            .load("https://image.tmdb.org/t/p/w500${favorite.posterPath}")
            .into(holder.binding.imgPoster)

        holder.itemView.setOnClickListener {
            onItemClick(favorite)
        }
    }

    override fun getItemCount(): Int = favorites.size

}