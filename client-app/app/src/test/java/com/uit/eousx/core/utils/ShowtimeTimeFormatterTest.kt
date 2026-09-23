package com.uit.eousx.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class ShowtimeTimeFormatterTest {
    @Test
    fun formatShowtimeTime_convertsOffsetTimeToDisplayZone() {
        val displayZone = ZoneId.of("Europe/London")

        val result = formatShowtimeTime("2026-06-01T04:00:00Z", displayZone)

        assertEquals("05:00", result)
    }

    @Test
    fun formatShowtimeTime_keepsLocalDateTimeWithoutOffset() {
        val result = formatShowtimeTime("2026-06-01T17:00:00", ZoneId.of("Europe/London"))

        assertEquals("17:00", result)
    }

    @Test
    fun formatShowtimeTime_keepsLocalTimeWithoutOffset() {
        val result = formatShowtimeTime("17:00", ZoneId.of("Europe/London"))

        assertEquals("17:00", result)
    }
}
