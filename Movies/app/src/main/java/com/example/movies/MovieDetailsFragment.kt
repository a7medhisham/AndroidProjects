package com.example.movies

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.movies.databinding.FragmentMovieDetailsBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class MovieDetailsFragment : Fragment() {
    private lateinit var binding: FragmentMovieDetailsBinding
    private var movieId: Int = 0
    private var isFavorite: Boolean = false
    private var youtubeKey: String? = null
    private val castList = mutableListOf<Cast>()
    private var movieTitle: String = ""
    private var movieOverview: String = ""
    private var movieRating: Double = 0.0
    private var movieReleaseDate: String = ""
    private var moviePosterPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMovieDetailsBinding.inflate(inflater, container, false)

        movieId = arguments?.getInt("movie_id") ?: 0

        setupButtons()
        setupCastRecyclerView()
        loadMovieDetails()

        return binding.root
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            navigateBackToHome()
        }

        binding.btnTrailer.setOnClickListener {
            playTrailer()
        }

        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        binding.btnFavoriteBig.setOnClickListener {
            toggleFavorite()
        }

        binding.btnShare.setOnClickListener {
            shareMovie()
        }
    }

    private fun setupCastRecyclerView() {
        binding.recyclerCast.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.recyclerCast.setHasFixedSize(true)
    }

    private fun loadMovieDetails() {
        if (movieId == 0) return

        lifecycleScope.launch {
            try {
                val movie = RetrofitInstance.api.getMovieDetails(movieId)
                movieTitle = movie.title
                movieOverview = movie.overview
                movieRating = movie.vote_average
                movieReleaseDate = movie.release_date
                moviePosterPath = movie.poster_path

                binding.txtTitle.text = movieTitle
                binding.txtOverview.text = movieOverview
                binding.txtRate.text = "⭐ $movieRating/10"
                binding.txtReleaseDate.text = "📅 $movieReleaseDate"

                Glide.with(requireContext())
                    .load("https://image.tmdb.org/t/p/w500${moviePosterPath}")
                    .into(binding.imgPoster)
                try {
                    val videosResponse = RetrofitInstance.api.getMovieVideos(movieId)
                    youtubeKey = videosResponse.results
                        .firstOrNull { it.site == "YouTube" && it.type == "Trailer" }
                        ?.key
                } catch (e: Exception) {

                }
                try {
                    val creditsResponse = RetrofitInstance.api.getMovieCredits(movieId)
                    castList.clear()
                    castList.addAll(creditsResponse.cast.take(10))
                    binding.recyclerCast.adapter = CastAdapter(castList)
                } catch (e: Exception) {
                }

                loadFavoriteStatus()

            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Error loading details: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFavoriteStatus() {
        val prefs = requireContext().getSharedPreferences("favorites", 0)
        isFavorite = prefs.getStringSet("favorite_ids", emptySet())
            ?.contains(movieId.toString()) ?: false

        updateFavoriteButton()
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite

        val prefs = requireContext().getSharedPreferences("favorites", 0)
        val favorites = prefs.getStringSet("favorite_ids", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        if (isFavorite) {
            favorites.add(movieId.toString())
            binding.favAnimationBig.playAnimation()
            binding.btnFavorite.playAnimation()
            saveMovieData()

        } else {
            favorites.remove(movieId.toString())
            binding.favAnimationBig.progress = 0f
            binding.btnFavorite.progress = 0f
            removeMovieData()
        }

        prefs.edit().putStringSet("favorite_ids", favorites).apply()
        updateFavoriteButton()

        (requireActivity() as? HomeActivity)?.updateFavorites()

        Toast.makeText(requireContext(),
            if (isFavorite) "Added to favorites" else "Removed from favorites",
            Toast.LENGTH_SHORT).show()
    }

    private fun saveMovieData() {
        val prefs = requireContext().getSharedPreferences("favorites", 0)
        val gson = Gson()

        val favoriteMovie = FavoriteMovie(
            id = movieId,
            title = movieTitle,
            posterPath = moviePosterPath,
            voteAverage = movieRating,
            overview = movieOverview,
            releaseDate = movieReleaseDate
        )

        val movieJson = gson.toJson(favoriteMovie)
        prefs.edit().putString("movie_data_$movieId", movieJson).apply()
    }

    private fun removeMovieData() {
        val prefs = requireContext().getSharedPreferences("favorites", 0)
        prefs.edit().remove("movie_data_$movieId").apply()
    }

    private fun updateFavoriteButton() {
        binding.btnFavorite.apply {
            if (isFavorite) {
                progress = 1f
                playAnimation()
            } else {
                progress = 0f
                cancelAnimation()
            }
        }

        binding.favAnimationBig.apply {
            if (isFavorite) {
                progress = 1f
                playAnimation()
            } else {
                progress = 0f
                cancelAnimation()
            }
        }
    }

    private fun playTrailer() {
        if (youtubeKey.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No trailer available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.youtube.com/watch?v=$youtubeKey")
            setPackage("com.google.android.youtube")
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.youtube.com/watch?v=$youtubeKey")
            }
            startActivity(webIntent)
        }
    }

    private fun shareMovie() {
        val title = binding.txtTitle.text.toString()
        val rating = binding.txtRate.text.toString()
        val overview = binding.txtOverview.text.toString()

        val shareText = buildString {
            append("🎬 *$title*\n\n")
            append("⭐ $rating\n")
            append("📅 ${binding.txtReleaseDate.text}\n\n")
            append("📖 ${overview.take(150)}...\n\n")
            youtubeKey?.let {
                append("\n🎥 Watch Trailer:\n")
                append("https://www.youtube.com/watch?v=$it\n")
            }

            append("\n#Movies #TMDB #MovieApp")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Check out this movie: $title")
            type = "text/plain"
        }
        try {
            startActivity(Intent.createChooser(shareIntent, "Share Movie via"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateBackToHome() {
        try {
            (requireActivity() as? HomeActivity)?.navigateBackToHome()
        } catch (e: Exception) {
            requireActivity().onBackPressed()
        }
    }
}