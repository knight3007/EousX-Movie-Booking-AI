package com.uit.eousx.presentation.ai_chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    fun sendMessage(message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank() || _uiState.value.isLoading) return

        val userMessage = AiChatMessageUi(
            id = System.currentTimeMillis(),
            role = AiChatRole.USER,
            text = trimmedMessage
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = aiRepository.chat(trimmedMessage)) {
                is NetworkResult.Success -> {
                    val aiMessage = AiChatMessageUi(
                        id = System.currentTimeMillis() + 1,
                        role = AiChatRole.AI,
                        text = result.data.message.ifBlank {
                            "Mình chưa có gợi ý phù hợp lúc này. Bạn thử hỏi theo thể loại hoặc thời gian xem nhé."
                        },
                        recommendedMovies = result.data.recommendedMovies
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + aiMessage,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    val readableMessage = result.toReadableAiError()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = readableMessage
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private fun NetworkResult.Error.toReadableAiError(): String {
    return when (code) {
        401 -> "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
        in 500..599 -> "AI hiện chưa phản hồi được. Vui lòng thử lại."
        else -> when {
            message.contains("internet", ignoreCase = true) ||
                message.contains("network", ignoreCase = true) ||
                message.contains("timed out", ignoreCase = true) -> message
            else -> "AI hiện chưa phản hồi được. Vui lòng thử lại."
        }
    }
}
