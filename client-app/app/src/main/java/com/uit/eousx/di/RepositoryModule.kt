package com.uit.eousx.di

import com.uit.eousx.data.repository.AiRepositoryImpl
import com.uit.eousx.data.repository.AuthRepositoryImpl
import com.uit.eousx.data.repository.BookingRepositoryImpl
import com.uit.eousx.data.repository.MovieRepositoryImpl
import com.uit.eousx.data.repository.PaymentRepositoryImpl
import com.uit.eousx.data.repository.SeatRepositoryImpl
import com.uit.eousx.data.repository.ShowtimeRepositoryImpl
import com.uit.eousx.data.repository.TicketRepositoryImpl
import com.uit.eousx.domain.repository.AiRepository
import com.uit.eousx.domain.repository.AuthRepository
import com.uit.eousx.domain.repository.BookingRepository
import com.uit.eousx.domain.repository.MovieRepository
import com.uit.eousx.domain.repository.PaymentRepository
import com.uit.eousx.domain.repository.SeatRepository
import com.uit.eousx.domain.repository.ShowtimeRepository
import com.uit.eousx.domain.repository.TicketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAiRepository(
        aiRepositoryImpl: AiRepositoryImpl
    ): AiRepository

    @Binds
    @Singleton
    abstract fun bindShowtimeRepository(
        showtimeRepositoryImpl: ShowtimeRepositoryImpl
    ): ShowtimeRepository

    @Binds
    @Singleton
    abstract fun bindSeatRepository(
        seatRepositoryImpl: SeatRepositoryImpl
    ): SeatRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(
        bookingRepositoryImpl: BookingRepositoryImpl
    ): BookingRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindTicketRepository(
        ticketRepositoryImpl: TicketRepositoryImpl
    ): TicketRepository
}
