package com.uit.eousx.domain.model

data class Payment(
    val id: String,
    val bookingId: String,
    val provider: String,
    val transactionId: String?,
    val amount: Int,
    val status: String,
    val paidAt: String?
)

data class SePayPayment(
    val provider: String,
    val paymentCode: String,
    val bookingId: String,
    val amount: Long,
    val status: String,
    val qrImageUrl: String
)

data class SePayPaymentStatus(
    val paymentCode: String,
    val paymentStatus: String,
    val bookingStatus: String,
    val bookingId: String,
    val hasTicket: Boolean
)
