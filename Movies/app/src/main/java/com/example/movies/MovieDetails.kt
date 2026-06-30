package com.example.movies

data class MovieDetails(
    val title: String,
    val overview: String,
    val poster_path: String?,
    val vote_average: Double,
    val release_date: String
)
