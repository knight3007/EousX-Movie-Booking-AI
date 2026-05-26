package com.uit.eousx.presentation.profile

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.getMe()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, user = result.data, errorMessage = null)
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = ProfileUiState()
            onLoggedOut()
        }
    }
}
