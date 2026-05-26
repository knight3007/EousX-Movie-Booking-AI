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
