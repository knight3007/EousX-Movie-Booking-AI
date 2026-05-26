package com.uit.eousx.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SplashAuthState {
    data object Checking : SplashAuthState
    data object Authenticated : SplashAuthState
    data object Unauthenticated : SplashAuthState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<SplashAuthState>(SplashAuthState.Checking)
    val authState: StateFlow<SplashAuthState> = _authState.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {
            try {
                val hasToken = authRepository.isLoggedIn().first()
                if (!hasToken) {
                    _authState.value = SplashAuthState.Unauthenticated
                    return@launch
                }

                val result = withTimeoutOrNull(5_000) {
                    authRepository.getMe()
                }

                when (result) {
                    is NetworkResult.Success -> _authState.value = SplashAuthState.Authenticated
                    is NetworkResult.Error, null -> {
                        authRepository.logout()
                        _authState.value = SplashAuthState.Unauthenticated
                    }
                    NetworkResult.Loading -> Unit
                }
            } catch (throwable: Throwable) {
                runCatching { authRepository.logout() }
                _authState.value = SplashAuthState.Unauthenticated
            }
        }
    }
}
