package com.uit.eousx.core.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object Home : Screen("home_screen")
    object Detail : Screen("detail_screen/{movieId}") {
        fun createRoute(movieId: String) = "detail_screen/$movieId"
    }
    object MovieSchedule : Screen("movie_schedule/{movieId}") {
        fun createMovieScheduleRoute(movieId: String) = "movie_schedule/$movieId"
    }
    object SeatMap : Screen("seat_map/{showtimeId}") {
        fun createSeatMapRoute(showtimeId: String) = "seat_map/$showtimeId"
    }
    object Checkout : Screen(
        "checkout/{showtimeId}?lockIds={lockIds}&totalAmount={totalAmount}&lockedUntil={lockedUntil}&seatCodes={seatCodes}"
    ) {
        fun createCheckoutRoute(
            showtimeId: String,
            lockIds: List<String>,
            totalAmount: Int,
            lockedUntil: String?,
            seatCodes: List<String>
        ): String {
            val encodedShowtimeId = Uri.encode(showtimeId)
            val encodedLockIds = Uri.encode(lockIds.joinToString(",").ifBlank { "none" })
            val encodedLockedUntil = Uri.encode(lockedUntil.orEmpty())
            val encodedSeatCodes = Uri.encode(seatCodes.joinToString(","))
            return "checkout/$encodedShowtimeId?lockIds=$encodedLockIds&totalAmount=$totalAmount" +
                "&lockedUntil=$encodedLockedUntil&seatCodes=$encodedSeatCodes"
        }
    }
    object Payment : Screen("payment/{bookingId}?lockIds={lockIds}") {
        fun createPaymentRoute(bookingId: String, lockIds: List<String>): String {
            val encodedBookingId = Uri.encode(bookingId)
            val encodedLockIds = Uri.encode(lockIds.joinToString(",").ifBlank { "none" })
            return "payment/$encodedBookingId?lockIds=$encodedLockIds"
        }
    }
    object Ticket : Screen("ticket/{bookingId}") {
        fun createTicketRoute(bookingId: String) = "ticket/${Uri.encode(bookingId)}"
    }
    object AiChat : Screen("ai_chat")
}
