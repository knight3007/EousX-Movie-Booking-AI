package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Ticket

interface TicketRepository {
    suspend fun getTicketByBooking(bookingId: String): NetworkResult<Ticket>
}
