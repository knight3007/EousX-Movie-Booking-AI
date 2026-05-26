package com.uit.eousx.domain.model

data class SeatLockResult(
    val lockIds: List<String>,
    val lockedUntil: String?,
    val totalAmount: Int,
    val selectedSeats: List<Seat>,
    val lockDurationSeconds: Int?
)
