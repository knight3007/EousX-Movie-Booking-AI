package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Booking

interface BookingRepository {
    suspend fun getMyBookings(): NetworkResult<List<Booking>>

    suspend fun createBooking(
        showtimeId: String,
        lockIds: List<String>
    ): NetworkResult<Booking>

    suspend fun cancelBooking(bookingId: String): NetworkResult<Unit>
}
