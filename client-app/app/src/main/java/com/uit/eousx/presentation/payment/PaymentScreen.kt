package com.uit.eousx.presentation.payment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uit.eousx.domain.model.SePayPayment
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXPrimaryButton
import com.uit.eousx.presentation.components.EousXRadius
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
        if (
            !uiState.isCancelling &&
            !uiState.isPaying &&
            !uiState.isCheckingStatus &&
            !uiState.paymentSuccess
        ) {
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
        onProviderClick = viewModel::selectProvider,
        onPayMockClick = { viewModel.payMock(bookingId) },
        onCreateSePayClick = { viewModel.createSePayPayment(bookingId) },
        onCheckSePayStatusClick = { viewModel.checkSePayStatus() },
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
    onProviderClick: (PaymentProvider) -> Unit,
    onPayMockClick: () -> Unit,
    onCreateSePayClick: () -> Unit,
    onCheckSePayStatusClick: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmCancel: () -> Unit
) {
    val interactionEnabled = !uiState.isCancelling &&
        !uiState.isPaying &&
        !uiState.isCheckingStatus &&
        !uiState.paymentSuccess

    Scaffold(containerColor = EousXColors.Charcoal) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EousXColors.Charcoal)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
                    enabled = interactionEnabled,
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
            Spacer(modifier = Modifier.height(EousXSpacing.sm))
            Text(text = "bookingId: $bookingId", style = EousXTypography.Body)

            Spacer(modifier = Modifier.height(EousXSpacing.xl))
            Text(text = "Payment method", style = EousXTypography.H2)
            Spacer(modifier = Modifier.height(EousXSpacing.md))
            PaymentMethodCard(
                provider = PaymentProvider.MOCK,
                title = "MOCK Payment",
                description = "Fast demo payment",
                selected = uiState.selectedProvider == PaymentProvider.MOCK,
                enabled = interactionEnabled,
                onClick = onProviderClick
            )
            Spacer(modifier = Modifier.height(EousXSpacing.md))
            PaymentMethodCard(
                provider = PaymentProvider.SEPAY,
                title = "SePay QR Payment",
                description = "Pay by QR via SePay Test Mode",
                selected = uiState.selectedProvider == PaymentProvider.SEPAY,
                enabled = interactionEnabled,
                onClick = onProviderClick
            )

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
            when (uiState.selectedProvider) {
                PaymentProvider.MOCK -> MockPaymentSection(
                    uiState = uiState,
                    onPayMockClick = onPayMockClick
                )
                PaymentProvider.SEPAY -> SePayPaymentSection(
                    uiState = uiState,
                    onCreateSePayClick = onCreateSePayClick,
                    onCheckSePayStatusClick = onCheckSePayStatusClick
                )
            }
            Spacer(modifier = Modifier.height(EousXSpacing.xxl))
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

@Composable
private fun PaymentMethodCard(
    provider: PaymentProvider,
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: (PaymentProvider) -> Unit
) {
    val borderColor = if (selected) EousXColors.PrimaryOrange else EousXColors.Divider
    val icon = when (provider) {
        PaymentProvider.MOCK -> Icons.Default.CreditCard
        PaymentProvider.SEPAY -> Icons.Default.QrCode
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick(provider) },
        shape = RoundedCornerShape(EousXRadius.lg),
        color = if (selected) EousXColors.OrangeAlpha12 else EousXColors.SurfaceVariant,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(EousXSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(EousXSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) EousXColors.PrimaryOrange else EousXColors.OnSurfaceDim
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = EousXTypography.Label)
                Text(text = description, style = EousXTypography.Caption)
            }
            if (selected) {
                StatusPill(text = "Selected")
            }
        }
    }
}

@Composable
private fun MockPaymentSection(
    uiState: PaymentUiState,
    onPayMockClick: () -> Unit
) {
    Text(text = "MOCK Payment", style = EousXTypography.H2)
    Spacer(modifier = Modifier.height(EousXSpacing.md))
    EousXPrimaryButton(
        text = if (uiState.isPaying) "Processing..." else "Pay MOCK",
        enabled = !uiState.isPaying &&
            !uiState.isCheckingStatus &&
            !uiState.isCancelling &&
            !uiState.paymentSuccess,
        modifier = Modifier.fillMaxWidth(),
        onClick = onPayMockClick
    )
}

@Composable
private fun SePayPaymentSection(
    uiState: PaymentUiState,
    onCreateSePayClick: () -> Unit,
    onCheckSePayStatusClick: () -> Unit
) {
    Text(text = "SePay QR Payment", style = EousXTypography.H2)
    Spacer(modifier = Modifier.height(EousXSpacing.md))
    EousXPrimaryButton(
        text = if (uiState.isPaying) "Creating QR..." else "Create SePay QR",
        enabled = !uiState.isPaying &&
            !uiState.isCheckingStatus &&
            !uiState.isCancelling &&
            !uiState.paymentSuccess,
        modifier = Modifier.fillMaxWidth(),
        onClick = onCreateSePayClick
    )

    uiState.sePayPayment?.let { payment ->
        Spacer(modifier = Modifier.height(EousXSpacing.xl))
        SePayQrCard(
            payment = payment,
            statusText = uiState.sePayStatusText,
            isCheckingStatus = uiState.isCheckingStatus,
            onCheckSePayStatusClick = onCheckSePayStatusClick
        )
    }
}

@Composable
private fun SePayQrCard(
    payment: SePayPayment,
    statusText: String,
    isCheckingStatus: Boolean,
    onCheckSePayStatusClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EousXRadius.lg),
        color = EousXColors.SurfaceVariant,
        border = BorderStroke(1.dp, EousXColors.Divider)
    ) {
        Column(
            modifier = Modifier.padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(220.dp),
                shape = RoundedCornerShape(EousXRadius.md),
                color = EousXColors.SoftIvory
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = payment.qrImageUrl,
                        contentDescription = "SePay QR code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(EousXSpacing.sm),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            PaymentDetailRow(label = "Amount", value = "${payment.amount} VND")
            PaymentDetailRow(label = "Payment Code", value = payment.paymentCode)
            PaymentDetailRow(label = "Status", value = statusText.ifBlank { "PENDING" })

            Text(
                text = "Open SePay Test Mode and simulate an incoming transaction with the exact amount and payment code.",
                style = EousXTypography.Caption
            )
            Text(
                text = payment.qrImageUrl,
                style = EousXTypography.Caption.copy(color = EousXColors.OnSurfaceDim)
            )
            EousXSecondaryButton(
                text = if (isCheckingStatus) "Checking..." else "Check Payment Status",
                enabled = !isCheckingStatus,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCheckSePayStatusClick
            )
        }
    }
}

@Composable
private fun PaymentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = EousXTypography.Caption)
        Text(
            text = value,
            style = EousXTypography.Label.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = EousXColors.PrimaryOrange.copy(alpha = 0.16f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = EousXTypography.Caption.copy(color = EousXColors.PrimaryOrange)
        )
    }
}
