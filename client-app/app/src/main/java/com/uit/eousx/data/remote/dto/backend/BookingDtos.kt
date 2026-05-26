package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateBookingRequestDto(
    @Json(name = "showtimeId") val showtimeId: String,
    @Json(name = "lockIds") val lockIds: List<String>
)

@JsonClass(generateAdapter = true)
data class BookingResponseDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "booking") val booking: BookingDto? = null
)

@JsonClass(generateAdapter = true)
data class CancelBookingResponseDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "booking") val booking: BookingDto? = null
)

@JsonClass(generateAdapter = true)
data class BookingDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "totalAmount") val totalAmount: Int? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "movieTitle") val movieTitle: String? = null,
    @Json(name = "roomName") val roomName: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "showtimeId") val showtimeId: String? = null,
    @Json(name = "user") val user: BookingUserDto? = null,
    @Json(name = "showtime") val showtime: BookingShowtimeDto? = null,
    @Json(name = "seats") val seats: List<BookingSeatDto>? = null,
    @Json(name = "payment") val payment: PaymentDto? = null,
    @Json(name = "ticket") val ticket: TicketDto? = null
)

@JsonClass(generateAdapter = true)
data class BookingUserDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "fullName") val fullName: String? = null,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class BookingShowtimeDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "movie") val movie: BookingMovieDto? = null,
    @Json(name = "room") val room: BookingRoomDto? = null
)

@JsonClass(generateAdapter = true)
data class BookingMovieDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class BookingRoomDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class BookingSeatDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "seat") val seat: BookingSeatInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class BookingSeatInfoDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "code") val code: String? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "row") val row: String? = null,
    @Json(name = "number") val number: Int? = null
)
