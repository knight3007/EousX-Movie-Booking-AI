package com.uit.eousx.domain.model

data class Ticket(
    val id: String,
    val bookingId: String,
    val qrCode: String,
    val status: String,
    val movieTitle: String?,
    val roomName: String?,
    val startTime: String?,
    val seatCodes: List<String>,
    val totalAmount: Int
)
