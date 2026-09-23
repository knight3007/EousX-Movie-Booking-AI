package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MockPaymentRequestDto(
    @Json(name = "bookingId") val bookingId: String,
    @Json(name = "provider") val provider: String = "MOCK"
)

@JsonClass(generateAdapter = true)
data class MockPaymentResponseDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "booking") val booking: BookingDto? = null,
    @Json(name = "payment") val payment: PaymentDto? = null,
    @Json(name = "ticket") val ticket: TicketDto? = null
)

@JsonClass(generateAdapter = true)
data class CreateSePayPaymentRequest(
    @Json(name = "bookingId") val bookingId: String
)

@JsonClass(generateAdapter = true)
data class SePayPaymentResponse(
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "paymentCode") val paymentCode: String? = null,
    @Json(name = "bookingId") val bookingId: String? = null,
    @Json(name = "amount") val amount: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "paymentStatus") val paymentStatus: String? = null,
    @Json(name = "qrImageUrl") val qrImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SePayPaymentStatusResponse(
    @Json(name = "paymentCode") val paymentCode: String,
    @Json(name = "paymentStatus") val paymentStatus: String,
    @Json(name = "bookingStatus") val bookingStatus: String,
    @Json(name = "bookingId") val bookingId: String,
    @Json(name = "hasTicket") val hasTicket: Boolean
)

@JsonClass(generateAdapter = true)
data class PaymentDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "bookingId") val bookingId: String? = null,
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "transactionId") val transactionId: String? = null,
    @Json(name = "amount") val amount: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "paidAt") val paidAt: String? = null
)
