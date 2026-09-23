package com.uit.eousx.data.remote.api

import com.uit.eousx.data.remote.dto.backend.AuthResponse
import com.uit.eousx.data.remote.dto.backend.AuthUserDto
import com.uit.eousx.data.remote.dto.backend.AiChatRequest
import com.uit.eousx.data.remote.dto.backend.AiChatResponse
import com.uit.eousx.data.remote.dto.backend.BackendMovieDto
import com.uit.eousx.data.remote.dto.backend.BookingDto
import com.uit.eousx.data.remote.dto.backend.BookingResponseDto
import com.uit.eousx.data.remote.dto.backend.CancelBookingResponseDto
import com.uit.eousx.data.remote.dto.backend.CreateBookingRequestDto
import com.uit.eousx.data.remote.dto.backend.CreateSePayPaymentRequest
import com.uit.eousx.data.remote.dto.backend.GoogleAuthRequest
import com.uit.eousx.data.remote.dto.backend.LockSeatsRequestDto
import com.uit.eousx.data.remote.dto.backend.LockSeatsResponseDto
import com.uit.eousx.data.remote.dto.backend.LoginRequest
import com.uit.eousx.data.remote.dto.backend.MockPaymentRequestDto
import com.uit.eousx.data.remote.dto.backend.MockPaymentResponseDto
import com.uit.eousx.data.remote.dto.backend.MovieShowtimesResponseDto
import com.uit.eousx.data.remote.dto.backend.RegisterRequest
import com.uit.eousx.data.remote.dto.backend.ReleaseSeatLockResponseDto
import com.uit.eousx.data.remote.dto.backend.SeatMapResponseDto
import com.uit.eousx.data.remote.dto.backend.SePayPaymentResponse
import com.uit.eousx.data.remote.dto.backend.SePayPaymentStatusResponse
import com.uit.eousx.data.remote.dto.backend.TicketDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): AuthUserDto

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @POST("ai/chat")
    suspend fun chatWithAi(@Body request: AiChatRequest): AiChatResponse

    @GET("movies")
    suspend fun getMovies(): List<BackendMovieDto>

    @GET("movies/{id}")
    suspend fun getMovieById(@Path("id") id: String): BackendMovieDto

    @GET("movies/{movieId}/showtimes")
    suspend fun getMovieShowtimes(
        @Path("movieId") movieId: String,
        @Query("date") date: String? = null
    ): MovieShowtimesResponseDto

    @GET("showtimes/{showtimeId}/seats")
    suspend fun getShowtimeSeats(
        @Path("showtimeId") showtimeId: String
    ): SeatMapResponseDto

    @POST("showtimes/{showtimeId}/seat-locks")
    suspend fun lockSeats(
        @Path("showtimeId") showtimeId: String,
        @Body request: LockSeatsRequestDto
    ): LockSeatsResponseDto

    @DELETE("seat-locks/{lockId}")
    suspend fun releaseSeatLock(
        @Path("lockId") lockId: String
    ): ReleaseSeatLockResponseDto

    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequestDto): BookingResponseDto

    @GET("bookings/me")
    suspend fun getMyBookings(): List<BookingDto>

    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") id: String): BookingDto

    @PATCH("bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: String): CancelBookingResponseDto

    @POST("payments/mock-success")
    suspend fun mockPaymentSuccess(@Body request: MockPaymentRequestDto): MockPaymentResponseDto

    @POST("payments/sepay/create")
    suspend fun createSePayPayment(
        @Body request: CreateSePayPaymentRequest
    ): SePayPaymentResponse

    @GET("payments/sepay/status/{paymentCode}")
    suspend fun getSePayPaymentStatus(
        @Path("paymentCode") paymentCode: String
    ): SePayPaymentStatusResponse

    @GET("bookings/{bookingId}/ticket")
    suspend fun getTicketByBooking(@Path("bookingId") bookingId: String): TicketDto
}
