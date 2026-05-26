package com.uit.eousx.presentation.seat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Seat
import com.uit.eousx.domain.model.SeatLockResult
import com.uit.eousx.domain.model.SeatStatus
import com.uit.eousx.domain.repository.SeatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeatMapUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLocking: Boolean = false,
    val seats: List<Seat> = emptyList(),
    val selectedSeatIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val lockSuccess: SeatLockResult? = null,
    val totalSelectedAmount: Int = 0,
    val showtimeId: String = ""
)

@HiltViewModel
class SeatMapViewModel @Inject constructor(
    private val seatRepository: SeatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeatMapUiState())
    val uiState: StateFlow<SeatMapUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var activePollingShowtimeId: String? = null

    fun loadSeats(showtimeId: String, showFullLoading: Boolean = true) {
        viewModelScope.launch {
            loadSeatsInternal(showtimeId = showtimeId, showFullLoading = showFullLoading)
        }
    }

    fun startPolling(showtimeId: String) {
        if (pollingJob?.isActive == true && activePollingShowtimeId == showtimeId) return

        stopPolling()
        activePollingShowtimeId = showtimeId
        pollingJob = viewModelScope.launch {
            loadSeatsInternal(showtimeId = showtimeId, showFullLoading = true)
            while (true) {
                delay(POLLING_INTERVAL_MS)
                if (_uiState.value.isLocking) continue
                loadSeatsInternal(showtimeId = showtimeId, showFullLoading = false)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        activePollingShowtimeId = null
    }

    fun refreshNow() {
        val showtimeId = _uiState.value.showtimeId.takeIf { it.isNotBlank() } ?: return
        loadSeats(showtimeId = showtimeId, showFullLoading = false)
    }

    fun toggleSeat(seatId: String) {
        val currentState = _uiState.value
        val seat = currentState.seats.firstOrNull { it.id == seatId } ?: return
        val isSelected = seatId in currentState.selectedSeatIds
        if (!isSelected && seat.status != SeatStatus.AVAILABLE) return

        val nextSelectedIds = if (isSelected) {
            currentState.selectedSeatIds - seatId
        } else {
            currentState.selectedSeatIds + seatId
        }
        _uiState.update { state ->
            state.copy(
                selectedSeatIds = nextSelectedIds,
                totalSelectedAmount = calculateTotal(state.seats, nextSelectedIds),
                errorMessage = null
            )
        }
    }

    fun lockSelectedSeats() {
        val state = _uiState.value
        if (state.selectedSeatIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one seat.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLocking = true, errorMessage = null) }
            when (
                val result = seatRepository.lockSeats(
                    showtimeId = state.showtimeId,
                    seatIds = state.selectedSeatIds.toList()
                )
            ) {
                is NetworkResult.Success -> {
                    val selectedSeats = state.seats.filter { it.id in state.selectedSeatIds }
                    _uiState.update {
                        it.copy(
                            isLocking = false,
                            selectedSeatIds = emptySet(),
                            totalSelectedAmount = 0,
                            lockSuccess = result.data.copy(
                                selectedSeats = result.data.selectedSeats.ifEmpty { selectedSeats },
                                totalAmount = result.data.totalAmount.takeIf { amount -> amount > 0 }
                                    ?: calculateTotal(selectedSeats, selectedSeats.map { seat -> seat.id }.toSet())
                            )
                        )
                    }
                    refreshNow()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLocking = false,
                            errorMessage = result.message.ifBlank {
                                "Unable to lock seats. Please sign in and try again."
                            }
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeLockSuccess() {
        _uiState.update { it.copy(lockSuccess = null) }
    }

    fun retry() {
        val showtimeId = _uiState.value.showtimeId.takeIf { it.isNotBlank() } ?: return
        loadSeats(showtimeId = showtimeId, showFullLoading = true)
    }

    private suspend fun loadSeatsInternal(showtimeId: String, showFullLoading: Boolean) {
        _uiState.update {
            it.copy(
                showtimeId = showtimeId,
                isLoading = showFullLoading,
                isRefreshing = !showFullLoading,
                errorMessage = null
            )
        }

        when (val result = seatRepository.getSeatMap(showtimeId)) {
            is NetworkResult.Success -> {
                _uiState.update { state ->
                    val availableIds = result.data
                        .filter { it.status == SeatStatus.AVAILABLE }
                        .map { it.id }
                        .toSet()
                    val retainedSelectedIds = state.selectedSeatIds.intersect(availableIds)

                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        seats = result.data,
                        selectedSeatIds = retainedSelectedIds,
                        totalSelectedAmount = calculateTotal(result.data, retainedSelectedIds),
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

    private fun calculateTotal(seats: List<Seat>, selectedSeatIds: Set<String>): Int {
        return seats
            .filter { it.id in selectedSeatIds }
            .sumOf { it.price ?: 0 }
    }

    companion object {
        private const val POLLING_INTERVAL_MS = 5_000L
    }
}
