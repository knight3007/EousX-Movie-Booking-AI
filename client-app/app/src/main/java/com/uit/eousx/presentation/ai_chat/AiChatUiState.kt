package com.uit.eousx.presentation.ai_chat

import com.uit.eousx.domain.model.AiRecommendedMovie

data class AiChatUiState(
    val messages: List<AiChatMessageUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val suggestedPrompts: List<String> = defaultSuggestedPrompts
)

data class AiChatMessageUi(
    val id: Long,
    val role: AiChatRole,
    val text: String,
    val recommendedMovies: List<AiRecommendedMovie> = emptyList()
)

enum class AiChatRole {
    USER,
    AI
}

private val defaultSuggestedPrompts = listOf(
    "Hôm nay rạp chiếu gì?",
    "Tối nay có phim vui không?",
    "Gợi ý phim đi với gia đình",
    "Tôi muốn xem phim nhẹ nhàng"
)
