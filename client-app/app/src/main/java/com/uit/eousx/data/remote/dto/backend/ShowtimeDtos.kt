package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MovieShowtimesResponseDto(
    @Json(name = "movieId") val movieId: String? = null,
    @Json(name = "movieTitle") val movieTitle: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "showtimes") val showtimes: List<ShowtimeDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShowtimeDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "movieId") val movieId: String? = null,
    @Json(name = "movieTitle") val movieTitle: String? = null,
    @Json(name = "roomId") val roomId: String? = null,
    @Json(name = "roomName") val roomName: String? = null,
    @Json(name = "roomType") val roomType: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "basePrice") val basePrice: Int? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "availableSeats") val availableSeats: Int? = null
)
