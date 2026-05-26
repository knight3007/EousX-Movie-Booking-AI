package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Seat
import com.uit.eousx.domain.model.SeatLockResult

interface SeatRepository {
    suspend fun getSeatMap(showtimeId: String): NetworkResult<List<Seat>>

    suspend fun lockSeats(
        showtimeId: String,
        seatIds: List<String>
    ): NetworkResult<SeatLockResult>

    suspend fun releaseSeatLock(lockId: String): NetworkResult<Unit>

    suspend fun releaseSeatLocks(lockIds: List<String>): NetworkResult<Unit>
}
