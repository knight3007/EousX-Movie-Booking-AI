package com.uit.eousx.presentation.auth.google

sealed interface GoogleAuthResult {
    data class Success(val firebaseIdToken: String) : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}
