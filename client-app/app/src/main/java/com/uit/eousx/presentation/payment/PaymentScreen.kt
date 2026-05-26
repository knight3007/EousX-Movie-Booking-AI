package com.uit.eousx.presentation.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXPrimaryButton
import com.uit.eousx.presentation.components.EousXSecondaryButton
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTypography

@Composable
fun PaymentScreen(
    bookingId: String,
    lockIds: List<String>,
    onCancelDone: () -> Unit,
    onPaymentSuccess: (String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.cancelDone) {
        if (uiState.cancelDone) {
            viewModel.consumeCancelDone()
            onCancelDone()
        }
    }

    LaunchedEffect(uiState.paymentSuccess, uiState.paidBookingId) {
        if (uiState.paymentSuccess) {
            val paidBookingId = uiState.paidBookingId ?: bookingId
            viewModel.consumePaymentSuccess()
            onPaymentSuccess(paidBookingId)
        }
    }

    fun requestLeavePayment() {
        if (!uiState.isCancelling && !uiState.isPaying && !uiState.paymentSuccess) {
            showCancelConfirm = true
        }
    }

    BackHandler(enabled = true) {
        requestLeavePayment()
    }

    PaymentContent(
        bookingId = bookingId,
        uiState = uiState,
        showCancelConfirm = showCancelConfirm,
        onBackClick = { requestLeavePayment() },
        onPayClick = { viewModel.payMock(bookingId) },
        onDismissConfirm = { showCancelConfirm = false },
        onConfirmCancel = {
            showCancelConfirm = false
            viewModel.cancelBookingAndReleaseLocks(bookingId, lockIds)
        }
    )
}

@Composable
private fun PaymentContent(
    bookingId: String,
    uiState: PaymentUiState,
    showCancelConfirm: Boolean,
    onBackClick: () -> Unit,
    onPayClick: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmCancel: () -> Unit
) {
    Scaffold(containerColor = EousXColors.Charcoal) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EousXColors.Charcoal)
                .padding(paddingValues)
                .padding(horizontal = EousXSpacing.xl),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = EousXSpacing.sm, bottom = EousXSpacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    enabled = !uiState.isCancelling && !uiState.isPaying && !uiState.paymentSuccess,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EousXColors.SurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = EousXColors.SoftIvory
                    )
                }
            }

            Spacer(modifier = Modifier.height(EousXSpacing.xxl))
            Text(text = "Payment", style = EousXTypography.H1)
            Spacer(modifier = Modifier.height(EousXSpacing.md))
            Text(text = "MOCK Payment", style = EousXTypography.H2)
            Spacer(modifier = Modifier.height(EousXSpacing.md))
            Text(text = "bookingId: $bookingId", style = EousXTypography.Body)

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(EousXSpacing.lg))
                Text(
                    text = message,
                    style = EousXTypography.Caption.copy(color = EousXColors.DangerRed)
                )
            }

            if (uiState.isCancelling) {
                Spacer(modifier = Modifier.height(EousXSpacing.lg))
                Text(text = "Cancelling booking...", style = EousXTypography.Caption)
            }

            Spacer(modifier = Modifier.height(EousXSpacing.xxl))
            EousXPrimaryButton(
                text = if (uiState.isPaying) "Processing..." else "Pay Now",
                enabled = !uiState.isPaying && !uiState.isCancelling && !uiState.paymentSuccess,
                modifier = Modifier.fillMaxWidth(),
                onClick = onPayClick
            )
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            containerColor = EousXColors.Surface,
            titleContentColor = EousXColors.SoftIvory,
            textContentColor = EousXColors.OnSurfaceDim,
            title = { Text(text = "Cancel booking?", style = EousXTypography.H2) },
            text = {
                Text(
                    text = "Leaving payment will cancel this booking and release selected seats.",
                    style = EousXTypography.Body
                )
            },
            confirmButton = {
                EousXSecondaryButton(
                    text = "Cancel booking",
                    enabled = !uiState.isCancelling,
                    onClick = onConfirmCancel
                )
            },
            dismissButton = {
                EousXSecondaryButton(
                    text = "Stay",
                    enabled = !uiState.isCancelling,
                    onClick = onDismissConfirm
                )
            }
        )
    }
}
