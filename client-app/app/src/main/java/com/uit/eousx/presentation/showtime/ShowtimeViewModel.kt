package com.uit.eousx.presentation.showtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Showtime
import com.uit.eousx.domain.repository.ShowtimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShowtimeUiState(
    val isLoading: Boolean = false,
    val selectedDate: String = LocalDate.now().toString(),
    val showtimes: List<Showtime> = emptyList(),
    val errorMessage: String? = null,
    val selectedShowtimeId: String? = null
)

@HiltViewModel
class ShowtimeViewModel @Inject constructor(
    private val showtimeRepository: ShowtimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShowtimeUiState())
    val uiState: StateFlow<ShowtimeUiState> = _uiState.asStateFlow()

    private var lastMovieId: String? = null
    private var lastDate: String? = null

    fun loadShowtimes(movieId: String, date: String? = null) {
        lastMovieId = movieId
        lastDate = date

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedDate = date ?: it.selectedDate,
                    errorMessage = null
                )
            }

            when (val result = showtimeRepository.getShowtimesByMovie(movieId, date)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showtimes = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showtimes = emptyList(),
                            errorMessage = result.message
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun selectDate(movieId: String, date: String) {
        loadShowtimes(movieId = movieId, date = date)
    }

    fun selectShowtime(showtimeId: String) {
        _uiState.update { it.copy(selectedShowtimeId = showtimeId) }
    }

    fun retry() {
        val movieId = lastMovieId ?: return
        loadShowtimes(movieId = movieId, date = lastDate)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
