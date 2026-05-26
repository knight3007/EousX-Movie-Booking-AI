package com.uit.eousx.domain.model

data class Booking(
    val id: String,
    val code: String,
    val status: String,
    val totalAmount: Int,
    val movieTitle: String?,
    val roomName: String?,
    val startTime: String?,
    val seatCodes: List<String>,
    val paymentStatus: String?,
    val ticketStatus: String?,
    val createdAt: String?
)
