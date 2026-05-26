package com.uit.eousx.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Booking
import com.uit.eousx.domain.repository.BookingRepository
import com.uit.eousx.domain.repository.SeatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val isCreatingBooking: Boolean = false,
    val isReleasing: Boolean = false,
    val errorMessage: String? = null,
    val bookingCreated: Booking? = null,
    val hasCreatedBooking: Boolean = false,
    val lockExpired: Boolean = false,
    val shouldNavigateBackAfterRelease: Boolean = false,
    val releaseError: Boolean = false
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val seatRepository: SeatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun createBooking(showtimeId: String, lockIds: List<String>) {
        if (lockIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "No locked seats found. Please select seats again.") }
            return
        }
        if (_uiState.value.hasCreatedBooking || _uiState.value.isCreatingBooking) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingBooking = true, errorMessage = null, releaseError = false) }
            when (val result = bookingRepository.createBooking(showtimeId, lockIds)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCreatingBooking = false,
                            bookingCreated = result.data,
                            hasCreatedBooking = true,
                            errorMessage = null,
                            releaseError = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCreatingBooking = false,
                            errorMessage = result.message.ifBlank {
                                "Unable to create booking. Please sign in and try again."
                            },
                            releaseError = false
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun releaseLocksBeforeExit(lockIds: List<String>) {
        releaseLocks(lockIds = lockIds, expired = false)
    }

    fun handleLockExpired(lockIds: List<String>) {
        val state = _uiState.value
        if (state.hasCreatedBooking || state.isCreatingBooking) return
        releaseLocks(lockIds = lockIds, expired = true)
    }

    private fun releaseLocks(lockIds: List<String>, expired: Boolean) {
        val state = _uiState.value
        if (
            state.isReleasing ||
            state.hasCreatedBooking ||
            state.shouldNavigateBackAfterRelease
        ) {
            return
        }
        if (state.isCreatingBooking) return

        if (lockIds.isEmpty()) {
            _uiState.update {
                it.copy(
                    lockExpired = it.lockExpired || expired,
                    shouldNavigateBackAfterRelease = true,
                    errorMessage = if (expired) "Seat hold expired." else null,
                    releaseError = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isReleasing = true,
                    lockExpired = it.lockExpired || expired,
                    errorMessage = if (expired) "Seat hold expired." else null,
                    releaseError = false
                )
            }
            when (val result = seatRepository.releaseSeatLocks(lockIds)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isReleasing = false,
                            shouldNavigateBackAfterRelease = true,
                            errorMessage = if (it.lockExpired) "Seat hold expired." else null,
                            releaseError = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isReleasing = false,
                            errorMessage = result.message,
                            releaseError = true
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, releaseError = false) }
    }

    fun consumeBookingCreated() {
        _uiState.update { it.copy(bookingCreated = null) }
    }

    fun consumeNavigateBackAfterRelease() {
        _uiState.update { it.copy(shouldNavigateBackAfterRelease = false) }
    }
}
