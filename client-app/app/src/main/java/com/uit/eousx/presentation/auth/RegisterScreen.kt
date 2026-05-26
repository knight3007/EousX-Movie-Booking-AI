package com.uit.eousx.presentation.auth

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.presentation.components.EousXPasswordField
import com.uit.eousx.presentation.components.EousXPrimaryButton
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTextField
import com.uit.eousx.presentation.components.EousXTheme
import com.uit.eousx.presentation.components.EousXTypography

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onRegisterSuccess()
        }
    }

    RegisterContent(
        uiState = uiState,
        onRegister = viewModel::register,
        onLoginClick = onLoginClick,
        onClearError = viewModel::clearError
    )
}

@Composable
private fun RegisterContent(
    uiState: AuthUiState,
    onRegister: (String, String, String, String?) -> Unit,
    onLoginClick: () -> Unit,
    onClearError: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val errorMessage = localError ?: uiState.errorMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EousXColors.Charcoal)
            .padding(EousXSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create account", style = EousXTypography.H1, textAlign = TextAlign.Center)
        Text(
            text = "Set up your EousX account.",
            style = EousXTypography.Body,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = EousXSpacing.sm)
        )

        Spacer(Modifier.height(28.dp))

        EousXTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                localError = null
                onClearError()
            },
            placeholder = "Full name"
        )
        Spacer(Modifier.height(EousXSpacing.md))
        EousXTextField(
            value = email,
            onValueChange = {
                email = it
                localError = null
                onClearError()
            },
            placeholder = "Email"
        )
        Spacer(Modifier.height(EousXSpacing.md))
        EousXTextField(
            value = phone,
            onValueChange = {
                phone = it
                localError = null
                onClearError()
            },
            placeholder = "Phone optional"
        )
        Spacer(Modifier.height(EousXSpacing.md))
        EousXPasswordField(
            value = password,
            onValueChange = {
                password = it
                localError = null
                onClearError()
            },
            placeholder = "Password"
        )
        Spacer(Modifier.height(EousXSpacing.md))
        EousXPasswordField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = null
                onClearError()
            },
            placeholder = "Confirm password"
        )

        if (errorMessage != null) {
            EousXErrorState(
                title = "Registration failed",
                message = errorMessage,
                actionText = null,
                modifier = Modifier.padding(top = EousXSpacing.md)
            )
        }

        Spacer(Modifier.height(EousXSpacing.xl))

        if (uiState.isLoading) {
            EousXLoadingState(message = "Creating account")
        } else {
            EousXPrimaryButton(
                text = "Register",
                modifier = Modifier.fillMaxWidth(),
                enabled = fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                onClick = {
                    if (password != confirmPassword) {
                        localError = "Passwords do not match."
                    } else {
                        onRegister(fullName, email, password, phone)
                    }
                }
            )
        }

        Spacer(Modifier.height(EousXSpacing.lg))

        Text(
            text = "Back to login",
            style = EousXTypography.Label.copy(color = EousXColors.PrimaryOrange),
            modifier = Modifier.clickable(onClick = onLoginClick)
        )
    }
}

@Preview(name = "Register Screen", showBackground = true, showSystemUi = true)
@Composable
private fun RegisterScreenPreview() {
    EousXTheme {
        RegisterContent(
            uiState = AuthUiState(),
            onRegister = { _, _, _, _ -> },
            onLoginClick = {},
            onClearError = {}
        )
    }
}
