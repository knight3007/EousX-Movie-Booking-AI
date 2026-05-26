package com.uit.eousx.presentation.movie_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.domain.model.Movie
import com.uit.eousx.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie = _movie.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun getMovieDetail(movieId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getMovieById(movieId)
            result.onSuccess { movie ->
                _movie.value = movie
            }
            result.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Unable to load movie detail."
            }

            _isLoading.value = false
        }
    }
}
