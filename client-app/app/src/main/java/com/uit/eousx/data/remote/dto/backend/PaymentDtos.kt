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
data class PaymentDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "bookingId") val bookingId: String? = null,
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "transactionId") val transactionId: String? = null,
    @Json(name = "amount") val amount: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "paidAt") val paidAt: String? = null
)
