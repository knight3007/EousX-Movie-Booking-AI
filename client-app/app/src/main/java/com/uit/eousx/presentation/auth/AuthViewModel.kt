package com.uit.eousx.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uit.eousx.core.network.NetworkResult
import com.uit.eousx.domain.model.User
import com.uit.eousx.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val sessionChecked: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(email.trim(), password)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true,
                            sessionChecked = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message, isAuthenticated = false)
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun register(fullName: String, email: String, password: String, phone: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val result = authRepository.register(
                    fullName = fullName.trim(),
                    email = email.trim(),
                    password = password,
                    phone = phone?.trim()
                )
            ) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true,
                            sessionChecked = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message, isAuthenticated = false)
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun completeGoogleLogin(firebaseIdToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.googleLogin(firebaseIdToken)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true,
                            sessionChecked = true,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message, isAuthenticated = false)
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun showAuthError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message, isAuthenticated = false) }
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val hasToken = authRepository.isLoggedIn().first()
            if (!hasToken) {
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = false, sessionChecked = true)
                }
                return@launch
            }

            when (val result = authRepository.getMe()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = result.data,
                            isAuthenticated = true,
                            sessionChecked = true
                        )
                    }
                }
                is NetworkResult.Error -> {
                    authRepository.logout()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            isAuthenticated = false,
                            sessionChecked = true
                        )
                    }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState(sessionChecked = true)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
