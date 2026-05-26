package com.uit.eousx.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.core.storage.TokenManager
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.AuthUserDto
import com.uit.eousx.data.remote.dto.backend.GoogleAuthRequest
import com.uit.eousx.data.remote.dto.backend.LoginRequest
import com.uit.eousx.data.remote.dto.backend.RegisterRequest
import com.uit.eousx.domain.model.User
import com.uit.eousx.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl @Inject constructor(
    private val backendApi: BackendApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun login(email: String, password: String): NetworkResult<User> {
        return try {
            val response = backendApi.login(LoginRequest(email = email, password = password))
            tokenManager.saveAccessToken(response.accessToken)
            NetworkResult.Success(response.user.toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String?
    ): NetworkResult<User> {
        return try {
            val response = backendApi.register(
                RegisterRequest(
                    fullName = fullName,
                    email = email,
                    password = password,
                    phone = phone?.takeIf { it.isNotBlank() }
                )
            )
            tokenManager.saveAccessToken(response.accessToken)
            NetworkResult.Success(response.user.toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun googleLogin(idToken: String): NetworkResult<User> {
        return try {
            val response = backendApi.loginWithGoogle(GoogleAuthRequest(idToken = idToken))
            tokenManager.saveAccessToken(response.accessToken)
            NetworkResult.Success(response.user.toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun getMe(): NetworkResult<User> {
        return try {
            NetworkResult.Success(backendApi.getMe().toDomain())
        } catch (throwable: Throwable) {
            ApiErrorHandler.parse(throwable)
        }
    }

    override suspend fun logout() {
        tokenManager.clearToken()
        runCatching { FirebaseAuth.getInstance().signOut() }
        runCatching { context.googleSignInClient().signOut().await() }
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.getAccessToken().map { token ->
            !token.isNullOrBlank()
        }
    }
}

private fun Context.googleSignInClient() = GoogleSignIn.getClient(this, googleSignInOptions())

private fun Context.googleSignInOptions(): GoogleSignInOptions {
    val webClientId = defaultWebClientId()
    return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .apply {
            if (!webClientId.isNullOrBlank()) {
                requestIdToken(webClientId)
            }
            requestEmail()
        }
        .build()
}

private fun Context.defaultWebClientId(): String? {
    val resourceId = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (resourceId == 0) null else getString(resourceId).takeIf { it.isNotBlank() }
}

private suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Google task failed."))
            }
        }
    }
}

private fun AuthUserDto.toDomain(): User {
    return User(
        id = id,
        fullName = fullName,
        email = email,
        phone = phone,
        avatarUrl = avatarUrl
    )
}
