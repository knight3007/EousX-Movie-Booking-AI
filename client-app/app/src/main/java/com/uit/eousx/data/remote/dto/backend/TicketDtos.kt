package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TicketDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "bookingId") val bookingId: String? = null,
    @Json(name = "qrCode") val qrCode: String? = null,
    @Json(name = "qrContent") val qrContent: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "checkedInAt") val checkedInAt: String? = null,
    @Json(name = "booking") val booking: BookingDto? = null
)
