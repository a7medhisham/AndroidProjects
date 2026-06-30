package com.example.movies

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movies.databinding.FragmentFavoritesBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {
    private lateinit var binding: FragmentFavoritesBinding
    private val favoritesList = mutableListOf<FavoriteMovie>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadFavorites()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.recyclerFavorites.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFavorites.setHasFixedSize(true)
    }

    internal fun loadFavorites() {
        lifecycleScope.launch {
            try {
                favoritesList.clear()

                val prefs = requireContext().getSharedPreferences("favorites", Context.MODE_PRIVATE)
                val gson = Gson()

                val allEntries = prefs.all

                allEntries.forEach { (key, value) ->
                    if (key.startsWith("movie_data_")) {
                        val movieJson = value as? String
                        movieJson?.let {
                            try {
                                val movie = gson.fromJson(it, FavoriteMovie::class.java)
                                favoritesList.add(movie)
                            } catch (e: Exception) {
                            }
                        }
                    }
                }

                if (favoritesList.isEmpty()) {
                    showEmptyState()
                } else {
                    showFavoritesList()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading favorites", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        }
    }

    private fun showFavoritesList() {
        binding.emptyState.visibility = View.GONE
        binding.recyclerFavorites.visibility = View.VISIBLE

        binding.recyclerFavorites.adapter = FavoritesAdapter(
            favorites = favoritesList,
            onItemClick = { favoriteMovie ->
                (requireActivity() as? HomeActivity)?.navigateToMovieDetails(favoriteMovie.id)
            }
        )
    }

    private fun showEmptyState() {
        binding.recyclerFavorites.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE

        binding.btnBrowse.setOnClickListener {
            activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)?.currentItem = 0
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}