package com.uit.eousx.data.repository

import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.ShowtimeDto
import com.uit.eousx.domain.model.Showtime
import com.uit.eousx.domain.repository.ShowtimeRepository
import javax.inject.Inject

class ShowtimeRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi
) : ShowtimeRepository {

    override suspend fun getShowtimesByMovie(
        movieId: String,
        date: String?
    ): NetworkResult<List<Showtime>> {
        return try {
            val response = backendApi.getMovieShowtimes(movieId = movieId, date = date)
            NetworkResult.Success(
                response.showtimes.orEmpty().map { dto ->
                    dto.toDomain(
                        fallbackMovieId = response.movieId ?: movieId,
                        fallbackMovieTitle = response.movieTitle.orEmpty()
                    )
                }
            )
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }
}

private fun ShowtimeDto.toDomain(
    fallbackMovieId: String,
    fallbackMovieTitle: String
): Showtime {
    return Showtime(
        id = id.orEmpty(),
        movieId = movieId ?: fallbackMovieId,
        movieTitle = movieTitle ?: fallbackMovieTitle,
        roomId = roomId.orEmpty(),
        roomName = roomName.orEmpty(),
        roomType = roomType.orEmpty(),
        startTime = startTime.orEmpty(),
        endTime = endTime.orEmpty(),
        basePrice = basePrice ?: 0,
        status = status.orEmpty(),
        availableSeats = availableSeats ?: 0
    )
}
