package com.uit.eousx.presentation.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.repository.BookingRepository
import com.uit.eousx.domain.repository.PaymentRepository
import com.uit.eousx.domain.repository.SeatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isPaying: Boolean = false,
    val isCancelling: Boolean = false,
    val cancelDone: Boolean = false,
    val errorMessage: String? = null,
    val paymentSuccess: Boolean = false,
    val paidBookingId: String? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val bookingRepository: BookingRepository,
    private val seatRepository: SeatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun payMock(bookingId: String) {
        val state = _uiState.value
        if (state.isPaying || state.paymentSuccess || state.isCancelling) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, errorMessage = null) }
            when (val result = paymentRepository.payMock(bookingId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isPaying = false,
                            paymentSuccess = true,
                            paidBookingId = result.data.bookingId,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isPaying = false,
                            errorMessage = result.message.ifBlank {
                                "Unable to complete payment. Please try again."
                            }
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun cancelBookingAndReleaseLocks(
        bookingId: String,
        lockIds: List<String>
    ) {
        val state = _uiState.value
        if (state.isCancelling || state.cancelDone || state.paymentSuccess || state.isPaying) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
            when (val cancelResult = bookingRepository.cancelBooking(bookingId)) {
                is NetworkResult.Success -> {
                    releaseFallback(lockIds)
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isCancelling = false, errorMessage = cancelResult.message)
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    private suspend fun releaseFallback(lockIds: List<String>) {
        when (val releaseResult = seatRepository.releaseSeatLocks(lockIds)) {
            is NetworkResult.Success -> {
                _uiState.update {
                    it.copy(isCancelling = false, cancelDone = true, errorMessage = null)
                }
            }
            is NetworkResult.Error -> {
                _uiState.update {
                    it.copy(isCancelling = false, cancelDone = true, errorMessage = null)
                }
            }
            NetworkResult.Loading -> Unit
        }
    }

    fun consumeCancelDone() {
        _uiState.update { it.copy(cancelDone = false) }
    }

    fun consumePaymentSuccess() {
        _uiState.update { it.copy(paymentSuccess = false, paidBookingId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
