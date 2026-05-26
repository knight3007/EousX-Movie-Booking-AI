package com.uit.eousx.data.repository

import android.util.Log
import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.BookingDto
import com.uit.eousx.data.remote.dto.backend.BookingSeatDto
import com.uit.eousx.data.remote.dto.backend.CreateBookingRequestDto
import com.uit.eousx.domain.model.Booking
import com.uit.eousx.domain.repository.BookingRepository
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi
) : BookingRepository {

    override suspend fun getMyBookings(): NetworkResult<List<Booking>> {
        return try {
            val bookings = backendApi.getMyBookings()
                .map { it.toDomain() }
                .sortedByDescending { it.createdAt.orEmpty() }
            NetworkResult.Success(bookings)
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun createBooking(
        showtimeId: String,
        lockIds: List<String>
    ): NetworkResult<Booking> {
        return try {
            val response = backendApi.createBooking(
                CreateBookingRequestDto(
                    showtimeId = showtimeId,
                    lockIds = lockIds
                )
            )
            val booking = response.booking
            if (booking == null) {
                NetworkResult.Error("Booking response was empty.")
            } else {
                NetworkResult.Success(booking.toDomain())
            }
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun cancelBooking(bookingId: String): NetworkResult<Unit> {
        return try {
            backendApi.cancelBooking(bookingId)
            NetworkResult.Success(Unit)
        } catch (throwable: Throwable) {
            val error = ApiErrorHandler.parse(throwable)
            if (error.code == 404 || error.code == 409) {
                Log.w("BookingRepository", "Booking $bookingId was already cancelled or expired.")
                NetworkResult.Success(Unit)
            } else {
                error
            }
        }
    }
}

private fun BookingDto.toDomain(): Booking {
    return Booking(
        id = id.orEmpty(),
        code = code ?: id.orEmpty(),
        status = status.orEmpty(),
        totalAmount = totalAmount ?: 0,
        movieTitle = movieTitle ?: showtime?.movie?.title,
        roomName = roomName ?: showtime?.room?.name,
        startTime = startTime ?: showtime?.startTime,
        seatCodes = seats.orEmpty().mapNotNull { it.codeOrNull() },
        paymentStatus = payment?.status,
        ticketStatus = ticket?.status,
        createdAt = createdAt
    )
}

private fun BookingSeatDto.codeOrNull(): String? {
    return code ?: label ?: seat?.code ?: seat?.label
}
