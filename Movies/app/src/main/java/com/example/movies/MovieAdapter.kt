package com.example.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movies.databinding.ItemMovieBinding

class MovieAdapter(
    private var movies: MutableList<Movie> = mutableListOf(),
    private val onItemClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    inner class MovieViewHolder(val binding: ItemMovieBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MovieViewHolder(binding)
    }

    fun updateMovies(newMovies: List<Movie>) {
        movies.clear()
        movies.addAll(newMovies)
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        holder.binding.txtTitle.text = movie.title

        Glide.with(holder.itemView)
            .load("https://image.tmdb.org/t/p/w500" + (movie.poster_path ?: ""))
            .into(holder.binding.imgPoster)

        holder.itemView.setOnClickListener {
            onItemClick(movie)
        }
    }
    override fun getItemCount() = movies.size
}

