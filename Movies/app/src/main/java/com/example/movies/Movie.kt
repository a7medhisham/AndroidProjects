package com.example.movies

data class Movie (  val id: Int, val title: String, val poster_path: String?, val overview: String)

data class MovieResponse(val results: List<Movie>)

data class VideoResponse(
    val results: List<Video>
)

data class Video(
    val key: String,
    val site: String,
    val type: String
)

data class CreditsResponse(
    val cast: List<Cast>
)

data class Cast(
    val name: String,
    val character: String,
    val profile_path: String?
)