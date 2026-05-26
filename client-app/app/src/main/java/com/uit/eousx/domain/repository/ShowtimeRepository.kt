package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.Showtime

interface ShowtimeRepository {
    suspend fun getShowtimesByMovie(
        movieId: String,
        date: String? = null
    ): NetworkResult<List<Showtime>>
}
