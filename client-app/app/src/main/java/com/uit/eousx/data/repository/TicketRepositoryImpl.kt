package com.uit.eousx.data.repository

import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.BookingSeatDto
import com.uit.eousx.data.remote.dto.backend.TicketDto
import com.uit.eousx.domain.model.Ticket
import com.uit.eousx.domain.repository.TicketRepository
import javax.inject.Inject

class TicketRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi
) : TicketRepository {

    override suspend fun getTicketByBooking(bookingId: String): NetworkResult<Ticket> {
        return try {
            NetworkResult.Success(backendApi.getTicketByBooking(bookingId).toDomain(bookingId))
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }
}

private fun TicketDto.toDomain(fallbackBookingId: String): Ticket {
    val bookingDto = booking
    return Ticket(
        id = id.orEmpty(),
        bookingId = bookingId ?: bookingDto?.id ?: fallbackBookingId,
        qrCode = qrCode ?: qrContent.orEmpty(),
        status = status.orEmpty(),
        movieTitle = bookingDto?.movieTitle ?: bookingDto?.showtime?.movie?.title,
        roomName = bookingDto?.roomName ?: bookingDto?.showtime?.room?.name,
        startTime = bookingDto?.startTime ?: bookingDto?.showtime?.startTime,
        seatCodes = bookingDto?.seats.orEmpty().mapNotNull { it.codeOrNull() },
        totalAmount = bookingDto?.totalAmount ?: 0
    )
}

private fun BookingSeatDto.codeOrNull(): String? {
    return code ?: label ?: seat?.code ?: seat?.label
}
