package com.uit.eousx.data.repository

import android.util.Log
import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.CreateSePayPaymentRequest
import com.uit.eousx.data.remote.dto.backend.MockPaymentRequestDto
import com.uit.eousx.data.remote.dto.backend.PaymentDto
import com.uit.eousx.data.remote.dto.backend.SePayPaymentResponse
import com.uit.eousx.data.remote.dto.backend.SePayPaymentStatusResponse
import com.uit.eousx.domain.model.Payment
import com.uit.eousx.domain.model.SePayPayment
import com.uit.eousx.domain.model.SePayPaymentStatus
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

    override suspend fun createSePayPayment(bookingId: String): NetworkResult<SePayPayment> {
        return try {
            val response = backendApi.createSePayPayment(
                CreateSePayPaymentRequest(bookingId = bookingId)
            )
            NetworkResult.Success(response.toDomain())
        } catch (throwable: Throwable) {
            Log.e(
                TAG,
                "createSePayPayment failed for bookingId=$bookingId: " +
                    "${throwable.javaClass.simpleName}: ${throwable.message}",
                throwable
            )
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun getSePayPaymentStatus(
        paymentCode: String
    ): NetworkResult<SePayPaymentStatus> {
        return try {
            val response = backendApi.getSePayPaymentStatus(paymentCode)
            NetworkResult.Success(response.toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    private companion object {
        const val TAG = "PaymentRepository"
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

private fun SePayPaymentResponse.toDomain(): SePayPayment {
    val resolvedPaymentCode = paymentCode.orEmpty()
    val resolvedBookingId = bookingId.orEmpty()
    val resolvedQrImageUrl = qrImageUrl.orEmpty()
    val resolvedStatus = status ?: paymentStatus ?: "PENDING"

    if (resolvedPaymentCode.isBlank() || resolvedBookingId.isBlank() || resolvedQrImageUrl.isBlank()) {
        Log.w(
            "PaymentRepository",
            "SePay response has missing fields: " +
                "paymentCodeBlank=${resolvedPaymentCode.isBlank()}, " +
                "bookingIdBlank=${resolvedBookingId.isBlank()}, " +
                "qrImageUrlBlank=${resolvedQrImageUrl.isBlank()}, " +
                "status=$resolvedStatus"
        )
    }

    return SePayPayment(
        provider = provider ?: "SEPAY",
        paymentCode = resolvedPaymentCode,
        bookingId = resolvedBookingId,
        amount = amount ?: 0L,
        status = resolvedStatus,
        qrImageUrl = resolvedQrImageUrl
    )
}

private fun SePayPaymentStatusResponse.toDomain(): SePayPaymentStatus {
    return SePayPaymentStatus(
        paymentCode = paymentCode,
        paymentStatus = paymentStatus,
        bookingStatus = bookingStatus,
        bookingId = bookingId,
        hasTicket = hasTicket
    )
}
