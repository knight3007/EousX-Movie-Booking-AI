package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.AiChatResponseModel

interface AiRepository {
    suspend fun chat(message: String): NetworkResult<AiChatResponseModel>
}
