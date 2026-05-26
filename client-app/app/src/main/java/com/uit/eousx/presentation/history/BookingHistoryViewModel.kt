package com.uit.eousx.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Booking
import com.uit.eousx.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingHistoryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val bookings: List<Booking> = emptyList(),
    val selectedStatusFilter: String? = null,
    val errorMessage: String? = null
) {
    val filteredBookings: List<Booking>
        get() = selectedStatusFilter?.let { filter ->
            bookings.filter { it.status.equals(filter, ignoreCase = true) }
        } ?: bookings
}

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingHistoryUiState(isLoading = true))
    val uiState: StateFlow<BookingHistoryUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.bookings.isEmpty(), errorMessage = null) }
            when (val result = bookingRepository.getMyBookings()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            bookings = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            when (val result = bookingRepository.getMyBookings()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            bookings = result.data,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, errorMessage = result.message) }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun setFilter(status: String?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
