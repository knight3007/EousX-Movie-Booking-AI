package com.uit.eousx.domain.model

data class Seat(
    val id: String,
    val code: String,
    val row: String,
    val number: Int,
    val type: String,
    val status: SeatStatus,
    val price: Int?
)

enum class SeatStatus {
    AVAILABLE,
    SELECTED,
    LOCKED,
    SOLD,
    MAINTENANCE
}
