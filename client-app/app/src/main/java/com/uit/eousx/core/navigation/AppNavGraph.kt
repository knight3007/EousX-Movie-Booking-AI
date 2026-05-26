package com.uit.eousx.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uit.eousx.presentation.ai_chat.AiChatScreen
import com.uit.eousx.presentation.auth.LoginScreen
import com.uit.eousx.presentation.auth.RegisterScreen
import com.uit.eousx.presentation.checkout.CheckoutScreen
import com.uit.eousx.presentation.home.HomeScreen
import com.uit.eousx.presentation.movie_detail.MovieDetailScreen
import com.uit.eousx.presentation.payment.PaymentScreen
import com.uit.eousx.presentation.seat.SeatMapScreen
import com.uit.eousx.presentation.showtime.MovieScheduleScreen
import com.uit.eousx.presentation.splash.SplashScreen
import com.uit.eousx.presentation.ticket.TicketScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onOpenMovieDetail = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId)) {
                        launchSingleTop = true
                    }
                },
                onOpenTicket = { bookingId ->
                    navController.navigate(Screen.Ticket.createTicketRoute(bookingId)) {
                        launchSingleTop = true
                    }
                },
                onOpenAiChat = {
                    navController.navigate(Screen.AiChat.route) {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.AiChat.route) {
            AiChatScreen(
                onBack = { navController.popBackStack() },
                onOpenMovie = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId)) {
                        launchSingleTop = true
                    }
                },
                onBookMovie = { movieId ->
                    navController.navigate(Screen.MovieSchedule.createMovieScheduleRoute(movieId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Detail.route) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId").orEmpty()
            MovieDetailScreen(
                movieId = movieId,
                onBackClick = { navController.popBackStack() },
                onBookClick = {
                    navController.navigate(Screen.MovieSchedule.createMovieScheduleRoute(movieId))
                }
            )
        }

        composable(route = Screen.MovieSchedule.route) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId").orEmpty()
            MovieScheduleScreen(
                movieId = movieId,
                onBackClick = { navController.popBackStack() },
                onShowtimeClick = { showtimeId ->
                    navController.navigate(Screen.SeatMap.createSeatMapRoute(showtimeId))
                }
            )
        }

        composable(route = Screen.SeatMap.route) { backStackEntry ->
            val showtimeId = backStackEntry.arguments?.getString("showtimeId").orEmpty()
            SeatMapScreen(
                showtimeId = showtimeId,
                onBackClick = { navController.popBackStack() },
                onLockSuccess = { result ->
                    navController.navigate(
                        Screen.Checkout.createCheckoutRoute(
                            showtimeId = showtimeId,
                            lockIds = result.lockIds,
                            totalAmount = result.totalAmount,
                            lockedUntil = result.lockedUntil,
                            seatCodes = result.selectedSeats.map { it.code }
                        )
                    )
                }
            )
        }

        composable(route = Screen.Checkout.route) { backStackEntry ->
            val showtimeId = Uri.decode(backStackEntry.arguments?.getString("showtimeId").orEmpty())
            val lockIds = decodeCsvArgument(backStackEntry.arguments?.getString("lockIds"))
            val totalAmount = backStackEntry.arguments?.getString("totalAmount")?.toIntOrNull() ?: 0
            val lockedUntil = Uri.decode(backStackEntry.arguments?.getString("lockedUntil").orEmpty())
                .takeIf { it.isNotBlank() }
            val seatCodes = decodeCsvArgument(backStackEntry.arguments?.getString("seatCodes"))

            CheckoutScreen(
                showtimeId = showtimeId,
                lockIds = lockIds,
                totalAmount = totalAmount,
                lockedUntil = lockedUntil,
                seatCodes = seatCodes,
                onReleasedAndBack = { navController.popBackStack() },
                onBookingCreated = { booking ->
                    navController.navigate(Screen.Payment.createPaymentRoute(booking.id, lockIds)) {
                        popUpTo(Screen.Checkout.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Payment.route) { backStackEntry ->
            val bookingId = Uri.decode(backStackEntry.arguments?.getString("bookingId").orEmpty())
            val lockIds = decodeCsvArgument(backStackEntry.arguments?.getString("lockIds"))
            PaymentScreen(
                bookingId = bookingId,
                lockIds = lockIds,
                onCancelDone = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                onPaymentSuccess = { paidBookingId ->
                    navController.navigate(Screen.Ticket.createTicketRoute(paidBookingId)) {
                        popUpTo(Screen.Payment.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = Screen.Ticket.route) { backStackEntry ->
            val bookingId = Uri.decode(backStackEntry.arguments?.getString("bookingId").orEmpty())
            TicketScreen(
                bookingId = bookingId,
                onBackClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private fun decodeCsvArgument(value: String?): List<String> {
    return Uri.decode(value.orEmpty())
        .takeIf { it.isNotBlank() && it != "none" }
        ?.split(",")
        ?.filter { it.isNotBlank() }
        .orEmpty()
}
