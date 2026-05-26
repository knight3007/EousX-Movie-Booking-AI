package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeatMapResponseDto(
    @Json(name = "showtimeId") val showtimeId: String? = null,
    @Json(name = "movieTitle") val movieTitle: String? = null,
    @Json(name = "roomName") val roomName: String? = null,
    @Json(name = "roomType") val roomType: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "seats") val seats: List<SeatDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class SeatDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "row") val row: String? = null,
    @Json(name = "rowLabel") val rowLabel: String? = null,
    @Json(name = "number") val number: Int? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "price") val price: Int? = null,
    @Json(name = "isActive") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class LockSeatsRequestDto(
    @Json(name = "seatIds") val seatIds: List<String>
)

@JsonClass(generateAdapter = true)
data class LockSeatsResponseDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "lockDurationMinutes") val lockDurationMinutes: Int? = null,
    @Json(name = "lockDurationSeconds") val lockDurationSeconds: Int? = null,
    @Json(name = "lockedUntil") val lockedUntil: String? = null,
    @Json(name = "totalAmount") val totalAmount: Int? = null,
    @Json(name = "locks") val locks: List<SeatLockDto>? = emptyList(),
    @Json(name = "showtimeId") val showtimeId: String? = null,
    @Json(name = "movieTitle") val movieTitle: String? = null,
    @Json(name = "roomName") val roomName: String? = null,
    @Json(name = "roomType") val roomType: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null
)

@JsonClass(generateAdapter = true)
data class SeatLockDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "seatId") val seatId: String? = null,
    @Json(name = "seatCode") val seatCode: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "lockedUntil") val lockedUntil: String? = null
)

@JsonClass(generateAdapter = true)
data class ReleaseSeatLockResponseDto(
    @Json(name = "message") val message: String? = null
)
