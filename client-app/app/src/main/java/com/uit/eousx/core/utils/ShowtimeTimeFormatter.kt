package com.uit.eousx.core.utils

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM", Locale.US)

fun formatShowtimeTime(
    value: String,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (value.isBlank()) return "--:--"
    return parseShowtimeLocalDateTime(value, zoneId)?.toLocalTime()?.format(timeFormatter)
        ?: parseShowtimeLocalTime(value, zoneId)?.format(timeFormatter)
        ?: value.take(5)
}

fun formatShowtimeDateTime(
    value: String?,
    fallback: String,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (value.isNullOrBlank()) return fallback
    return parseShowtimeLocalDateTime(value, zoneId)?.format(dateTimeFormatter) ?: value
}

private fun parseShowtimeLocalDateTime(value: String, zoneId: ZoneId): LocalDateTime? {
    return try {
        OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDateTime()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private fun parseShowtimeLocalTime(value: String, zoneId: ZoneId): LocalTime? {
    return try {
        OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalTime()
    } catch (_: DateTimeParseException) {
        try {
            LocalTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
