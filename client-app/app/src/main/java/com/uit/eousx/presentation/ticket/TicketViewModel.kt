package com.uit.eousx.presentation.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Ticket
import com.uit.eousx.domain.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketUiState(
    val isLoading: Boolean = false,
    val ticket: Ticket? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TicketViewModel @Inject constructor(
    private val ticketRepository: TicketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    private var lastBookingId: String? = null

    fun loadTicket(bookingId: String) {
        lastBookingId = bookingId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = ticketRepository.getTicketByBooking(bookingId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, ticket = result.data, errorMessage = null)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun retry() {
        lastBookingId?.let { loadTicket(it) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
