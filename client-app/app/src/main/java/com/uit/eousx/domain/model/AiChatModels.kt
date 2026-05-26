package com.uit.eousx.domain.model

data class AiChatResponseModel(
    val message: String,
    val recommendedMovies: List<AiRecommendedMovie>,
    val nextAction: AiNextAction?
)

data class AiRecommendedMovie(
    val movieId: String,
    val title: String,
    val reason: String?,
    val showtimes: List<AiRecommendedShowtime>
)

data class AiRecommendedShowtime(
    val showtimeId: String?,
    val startTime: String?,
    val roomName: String?,
    val roomType: String?,
    val basePrice: Int?
)

data class AiNextAction(
    val type: AiNextActionType,
    val movieId: String?
)

enum class AiNextActionType {
    OPEN_MOVIE_DETAIL,
    OPEN_SHOWTIMES,
    NONE
}
