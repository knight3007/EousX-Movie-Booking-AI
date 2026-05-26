package com.uit.eousx.domain.model

data class Showtime(
    val id: String,
    val movieId: String,
    val movieTitle: String,
    val roomId: String,
    val roomName: String,
    val roomType: String,
    val startTime: String,
    val endTime: String,
    val basePrice: Int,
    val status: String,
    val availableSeats: Int
)
