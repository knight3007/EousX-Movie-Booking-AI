package com.uit.eousx.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.presentation.auth.google.GoogleAuthManager
import com.uit.eousx.presentation.auth.google.GoogleAuthResult
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.presentation.components.EousXPasswordField
import com.uit.eousx.presentation.components.EousXPrimaryButton
import com.uit.eousx.presentation.components.EousXSecondaryButton
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTextField
import com.uit.eousx.presentation.components.EousXTheme
import com.uit.eousx.presentation.components.EousXTypography
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleAuthManager = remember(context.applicationContext) {
        GoogleAuthManager(context.applicationContext)
    }
    var isGoogleSignInRunning by remember { mutableStateOf(false) }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            when (val authResult = googleAuthManager.handleSignInResult(result.data)) {
                is GoogleAuthResult.Success -> viewModel.completeGoogleLogin(authResult.firebaseIdToken)
                is GoogleAuthResult.Error -> viewModel.showAuthError(authResult.message)
            }
            isGoogleSignInRunning = false
        }
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    LoginContent(
        uiState = uiState,
        isGoogleSignInRunning = isGoogleSignInRunning,
        onLogin = viewModel::login,
        onGoogleLoginClick = {
            isGoogleSignInRunning = true
            viewModel.clearError()
            scope.launch {
                try {
                    googleSignInLauncher.launch(googleAuthManager.getFreshSignInIntent())
                } catch (throwable: Throwable) {
                    isGoogleSignInRunning = false
                    viewModel.showAuthError(throwable.message ?: "Unable to start Google sign-in.")
                }
            }
        },
        onRegisterClick = onRegisterClick,
        onClearError = viewModel::clearError
    )
}

@Composable
private fun LoginContent(
    uiState: AuthUiState,
    isGoogleSignInRunning: Boolean,
    onLogin: (String, String) -> Unit,
    onGoogleLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EousXColors.Charcoal)
            .padding(EousXSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome back", style = EousXTypography.H1, textAlign = TextAlign.Center)
        Text(
            text = "Sign in to continue booking movies.",
            style = EousXTypography.Body,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = EousXSpacing.sm)
        )

        Spacer(Modifier.height(32.dp))

        EousXTextField(
            value = email,
            onValueChange = {
                email = it
                onClearError()
            },
            placeholder = "Email"
        )
        Spacer(Modifier.height(EousXSpacing.md))
        EousXPasswordField(
            value = password,
            onValueChange = {
                password = it
                onClearError()
            },
            placeholder = "Password"
        )

        if (uiState.errorMessage != null) {
            EousXErrorState(
                title = "Sign in failed",
                message = uiState.errorMessage,
                actionText = null,
                modifier = Modifier.padding(top = EousXSpacing.md)
            )
        }

        Spacer(Modifier.height(EousXSpacing.xl))

        if (uiState.isLoading || isGoogleSignInRunning) {
            EousXLoadingState(
                message = if (isGoogleSignInRunning) "Signing in with Google" else "Signing in"
            )
        } else {
            EousXPrimaryButton(
                text = "Login",
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank(),
                onClick = { onLogin(email, password) }
            )
        }

        Spacer(Modifier.height(EousXSpacing.md))

        EousXSecondaryButton(
            text = "Continue with Google",
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && !isGoogleSignInRunning,
            onClick = onGoogleLoginClick
        )

        Spacer(Modifier.height(EousXSpacing.lg))

        Text(
            text = "Create an account",
            style = EousXTypography.Label.copy(color = EousXColors.PrimaryOrange),
            modifier = Modifier.clickable(onClick = onRegisterClick)
        )
    }
}

@Preview(name = "Login Screen", showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    EousXTheme {
        LoginContent(
            uiState = AuthUiState(),
            isGoogleSignInRunning = false,
            onLogin = { _, _ -> },
            onGoogleLoginClick = {},
            onRegisterClick = {},
            onClearError = {}
        )
    }
}
