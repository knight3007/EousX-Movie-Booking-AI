package com.uit.eousx.data.repository

import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.AiChatRequest
import com.uit.eousx.data.remote.dto.backend.AiChatResponse
import com.uit.eousx.data.remote.dto.backend.AiNextActionDto
import com.uit.eousx.data.remote.dto.backend.AiRecommendedMovieDto
import com.uit.eousx.data.remote.dto.backend.AiRecommendedShowtimeDto
import com.uit.eousx.domain.model.AiChatResponseModel
import com.uit.eousx.domain.model.AiNextAction
import com.uit.eousx.domain.model.AiNextActionType
import com.uit.eousx.domain.model.AiRecommendedMovie
import com.uit.eousx.domain.model.AiRecommendedShowtime
import com.uit.eousx.domain.repository.AiRepository
import javax.inject.Inject

class AiRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi
) : AiRepository {

    override suspend fun chat(message: String): NetworkResult<AiChatResponseModel> {
        return try {
            val response = backendApi.chatWithAi(AiChatRequest(message = message))
            NetworkResult.Success(response.toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }
}

private fun AiChatResponse.toDomain(): AiChatResponseModel {
    return AiChatResponseModel(
        message = message.orEmpty(),
        recommendedMovies = recommendedMovies.orEmpty()
            .mapNotNull { it.toDomainOrNull() },
        nextAction = nextAction?.toDomain()
    )
}

private fun AiRecommendedMovieDto.toDomainOrNull(): AiRecommendedMovie? {
    val safeMovieId = movieId?.takeIf { it.isNotBlank() } ?: return null
    return AiRecommendedMovie(
        movieId = safeMovieId,
        title = title.orEmpty().ifBlank { "Phim EousX" },
        reason = reason?.takeIf { it.isNotBlank() },
        showtimes = showtimes.orEmpty().map { it.toDomain() }
    )
}

private fun AiRecommendedShowtimeDto.toDomain(): AiRecommendedShowtime {
    return AiRecommendedShowtime(
        showtimeId = showtimeId,
        startTime = startTime,
        roomName = roomName,
        roomType = roomType,
        basePrice = basePrice
    )
}

private fun AiNextActionDto.toDomain(): AiNextAction {
    return AiNextAction(
        type = when (type) {
            "OPEN_MOVIE_DETAIL" -> AiNextActionType.OPEN_MOVIE_DETAIL
            "OPEN_SHOWTIMES" -> AiNextActionType.OPEN_SHOWTIMES
            else -> AiNextActionType.NONE
        },
        movieId = movieId?.takeIf { it.isNotBlank() }
    )
}
