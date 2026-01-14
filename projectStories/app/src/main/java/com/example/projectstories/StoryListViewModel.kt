package com.example.projectstories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StoryListViewModel(
    private val repository: StoryRepository = StoryRepository()
) : ViewModel() {

    private val _stories = MutableStateFlow<List<Stories>>(emptyList())
    val stories: StateFlow<List<Stories>> = _stories
    fun fetchStories() {
        viewModelScope.launch {
            try {
                val list = repository.getStories()
                _stories.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
