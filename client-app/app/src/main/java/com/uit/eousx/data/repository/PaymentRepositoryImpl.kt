package com.uit.eousx.data.repository

import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.MockPaymentRequestDto
import com.uit.eousx.data.remote.dto.backend.PaymentDto
import com.uit.eousx.domain.model.Payment
import com.uit.eousx.domain.repository.PaymentRepository
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi
) : PaymentRepository {

    override suspend fun payMock(bookingId: String): NetworkResult<Payment> {
        return try {
            val response = backendApi.mockPaymentSuccess(
                MockPaymentRequestDto(bookingId = bookingId)
            )
            val payment = response.payment
            if (payment != null) {
                NetworkResult.Success(payment.toDomain(bookingId))
            } else {
                NetworkResult.Success(
                    Payment(
                        id = "",
                        bookingId = response.booking?.id ?: bookingId,
                        provider = "MOCK",
                        transactionId = null,
                        amount = response.booking?.totalAmount ?: 0,
                        status = response.booking?.status.orEmpty(),
                        paidAt = null
                    )
                )
            }
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }
}

private fun PaymentDto.toDomain(fallbackBookingId: String): Payment {
    return Payment(
        id = id.orEmpty(),
        bookingId = bookingId ?: fallbackBookingId,
        provider = provider ?: "MOCK",
        transactionId = transactionId,
        amount = amount ?: 0,
        status = status.orEmpty(),
        paidAt = paidAt
    )
}
