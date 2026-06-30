package com.example.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movies.databinding.FragmentHomeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: MovieAdapter
    private var allMovies = mutableListOf<Movie>()
    private var filteredMovies = mutableListOf<Movie>()
    private var searchJob: Job? = null
    private var isSearching = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupSearchView()
        setupSwipeRefresh()
        loadMovies()

        return binding.root
    }

    private fun setupRecyclerView() {
        binding.recyclerMovies.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMovies.setHasFixedSize(true)

        adapter = MovieAdapter(mutableListOf()) { movie ->
            (requireActivity() as? HomeActivity)?.navigateToMovieDetails(movie.id)
        }
        binding.recyclerMovies.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()

                if (newText.isNullOrEmpty()) {
                    showAllMovies()
                    isSearching = false
                } else {
                    isSearching = true
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        filterMovies(newText)
                    }
                }
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshMovies()
        }
    }

    private fun loadMovies() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getPopularMovies()
                allMovies = response.results.toMutableList()
                filteredMovies = allMovies.toMutableList()

                adapter.updateMovies(allMovies)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error loading movies: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun refreshMovies() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getPopularMovies()
                allMovies = response.results.toMutableList()
                filteredMovies = allMovies.toMutableList()
                adapter.updateMovies(allMovies)
                binding.text.text = getString(R.string.movies)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error refreshing movies: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun filterMovies(query: String) {
        val filtered = allMovies.filter { movie ->
            movie.title.contains(query, ignoreCase = true) ||
                    movie.overview.contains(query, ignoreCase = true)
        }.toMutableList()

        if (filtered.isEmpty()) {
            searchInApi(query)
        } else {
            filteredMovies = filtered
            adapter.updateMovies(filtered)
            binding.text.text = "Search results: ${filtered.size}"
        }
    }

    private fun searchInApi(query: String) {
        if (query.length < 2) return

        lifecycleScope.launch {
            try {
                val searchResponse = RetrofitInstance.api.searchMovies(query)
                val searchResults = searchResponse.results

                if (searchResults.isNotEmpty()) {
                    filteredMovies = searchResults.toMutableList()
                    adapter.updateMovies(searchResults)
                    binding.text.text = "Search results: ${searchResults.size}"
                } else {
                    Toast.makeText(requireContext(), "No movies found", Toast.LENGTH_SHORT).show()
                    adapter.updateMovies(mutableListOf())
                    binding.text.text = "No results found"
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            showAllMovies()
            return
        }

        binding.text.text = "Searching..."
        searchInApi(query)
    }

    private fun showAllMovies() {
        filteredMovies = allMovies.toMutableList()
        adapter.updateMovies(allMovies)
        binding.text.text = getString(R.string.movies)
    }

    override fun onResume() {
        super.onResume()
        if (isSearching) {
            showAllMovies()
            isSearching = false
        }
    }
}
