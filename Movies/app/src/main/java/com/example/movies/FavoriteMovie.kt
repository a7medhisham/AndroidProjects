package com.example.movies

data class FavoriteMovie(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val voteAverage: Double,
    val overview: String,
    val releaseDate: String? = null
)