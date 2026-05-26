package com.uit.eousx.data.repository

import android.util.Log
import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.LockSeatsRequestDto
import com.uit.eousx.data.remote.dto.backend.LockSeatsResponseDto
import com.uit.eousx.data.remote.dto.backend.SeatDto
import com.uit.eousx.domain.model.Seat
import com.uit.eousx.domain.model.SeatLockResult
import com.uit.eousx.domain.model.SeatStatus
import com.uit.eousx.domain.repository.SeatRepository
import javax.inject.Inject

class SeatRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi
) : SeatRepository {

    override suspend fun getSeatMap(showtimeId: String): NetworkResult<List<Seat>> {
        return try {
            val response = backendApi.getShowtimeSeats(showtimeId)
            NetworkResult.Success(response.seats.orEmpty().map { it.toDomain() })
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun lockSeats(
        showtimeId: String,
        seatIds: List<String>
    ): NetworkResult<SeatLockResult> {
        return try {
            val response = backendApi.lockSeats(
                showtimeId = showtimeId,
                request = LockSeatsRequestDto(seatIds = seatIds)
            )
            NetworkResult.Success(response.toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun releaseSeatLock(lockId: String): NetworkResult<Unit> {
        return try {
            backendApi.releaseSeatLock(lockId)
            NetworkResult.Success(Unit)
        } catch (throwable: Throwable) {
            val error = ApiErrorHandler.parse(throwable)
            if (error.code == 404 || error.code == 409) {
                Log.w("SeatRepository", "Seat lock $lockId was already released or expired.")
                NetworkResult.Success(Unit)
            } else {
                error
            }
        }
    }

    override suspend fun releaseSeatLocks(lockIds: List<String>): NetworkResult<Unit> {
        var firstError: NetworkResult.Error? = null
        lockIds.forEach { lockId ->
            when (val result = releaseSeatLock(lockId)) {
                is NetworkResult.Success -> Unit
                is NetworkResult.Error -> {
                    Log.w("SeatRepository", "Unable to release seat lock $lockId: ${result.message}")
                    if (firstError == null) firstError = result
                }
                NetworkResult.Loading -> Unit
            }
        }
        return firstError ?: NetworkResult.Success(Unit)
    }
}

private fun SeatDto.toDomain(): Seat {
    val resolvedCode = code ?: label ?: id.orEmpty()
    val resolvedRow = row ?: rowLabel ?: resolvedCode.takeWhile { it.isLetter() }.ifBlank { "-" }
    val resolvedNumber = number ?: resolvedCode.filter { it.isDigit() }.toIntOrNull() ?: 0
    val resolvedStatus = if (isActive == false) {
        SeatStatus.MAINTENANCE
    } else {
        status.toSeatStatus()
    }

    return Seat(
        id = id.orEmpty(),
        code = resolvedCode,
        row = resolvedRow,
        number = resolvedNumber,
        type = type.orEmpty(),
        status = resolvedStatus,
        price = price
    )
}

private fun String?.toSeatStatus(): SeatStatus {
    return when (this?.uppercase()) {
        "AVAILABLE" -> SeatStatus.AVAILABLE
        "LOCKED", "HELD", "RESERVED" -> SeatStatus.LOCKED
        "SOLD", "BOOKED" -> SeatStatus.SOLD
        "MAINTENANCE", "BLOCKED", "INACTIVE" -> SeatStatus.MAINTENANCE
        else -> SeatStatus.MAINTENANCE
    }
}

private fun LockSeatsResponseDto.toDomain(): SeatLockResult {
    val durationSeconds = lockDurationSeconds ?: lockDurationMinutes?.let { it * 60 }
    return SeatLockResult(
        lockIds = locks.orEmpty().mapNotNull { it.id },
        lockedUntil = lockedUntil ?: locks.orEmpty().firstNotNullOfOrNull { it.lockedUntil },
        totalAmount = totalAmount ?: 0,
        selectedSeats = locks.orEmpty().map { lock ->
            Seat(
                id = lock.seatId.orEmpty(),
                code = lock.seatCode ?: lock.seatId.orEmpty(),
                row = (lock.seatCode ?: "").takeWhile { it.isLetter() }.ifBlank { "-" },
                number = (lock.seatCode ?: "").filter { it.isDigit() }.toIntOrNull() ?: 0,
                type = "",
                status = SeatStatus.LOCKED,
                price = null
            )
        },
        lockDurationSeconds = durationSeconds
    )
}
