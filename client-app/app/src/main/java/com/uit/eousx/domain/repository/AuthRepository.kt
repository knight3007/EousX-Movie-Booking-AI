package com.uit.eousx.domain.repository

import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): NetworkResult<User>
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String?
    ): NetworkResult<User>
    suspend fun googleLogin(idToken: String): NetworkResult<User>
    suspend fun getMe(): NetworkResult<User>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
}
