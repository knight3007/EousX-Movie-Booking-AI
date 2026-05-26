package com.uit.eousx.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.domain.model.Movie
import com.uit.eousx.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeMovieUiState(
    val allMovies: List<Movie> = emptyList(),
    val visibleMovies: List<Movie> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeMovieUiState())
    val uiState: StateFlow<HomeMovieUiState> = _uiState.asStateFlow()

    init {
        loadMovies()
    }

    fun loadMovies() {
        fetchMovies(isRefresh = false)
    }

    fun refreshMovies() {
        if (_uiState.value.isRefreshing || _uiState.value.isLoading) return
        fetchMovies(isRefresh = true)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                visibleMovies = state.allMovies.filterMovies(query)
            )
        }
    }

    fun clearSearch() {
        onSearchQueryChange("")
    }

    fun retry() {
        fetchMovies(isRefresh = false)
    }

    private fun fetchMovies(isRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = !isRefresh && state.allMovies.isEmpty(),
                    isRefreshing = isRefresh,
                    errorMessage = null
                )
            }

            val result = repository.getPopularMovies(page = 1)
            result.onSuccess { movies ->
                _uiState.update { state ->
                    state.copy(
                        allMovies = movies,
                        visibleMovies = movies.filterMovies(state.searchQuery),
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null
                    )
                }
            }
            result.onFailure { exception ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = exception.message ?: "Unable to load movies."
                    )
                }
            }
        }
    }
}

private fun List<Movie>.filterMovies(query: String): List<Movie> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return this

    return filter { movie ->
        listOf(
            movie.title,
            movie.originalTitle.orEmpty(),
            movie.ageRating.orEmpty(),
            movie.overview,
            movie.genres.joinToString(" ")
        ).any { value -> value.lowercase().contains(normalizedQuery) }
    }
}
