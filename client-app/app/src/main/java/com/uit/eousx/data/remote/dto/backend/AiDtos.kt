package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiChatRequest(
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class AiChatResponse(
    @Json(name = "message") val message: String? = null,
    @Json(name = "recommendedMovies") val recommendedMovies: List<AiRecommendedMovieDto>? = emptyList(),
    @Json(name = "nextAction") val nextAction: AiNextActionDto? = null
)

@JsonClass(generateAdapter = true)
data class AiRecommendedMovieDto(
    @Json(name = "movieId") val movieId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "showtimes") val showtimes: List<AiRecommendedShowtimeDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiRecommendedShowtimeDto(
    @Json(name = "showtimeId") val showtimeId: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "roomName") val roomName: String? = null,
    @Json(name = "roomType") val roomType: String? = null,
    @Json(name = "basePrice") val basePrice: Int? = null
)

@JsonClass(generateAdapter = true)
data class AiNextActionDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "movieId") val movieId: String? = null
)
