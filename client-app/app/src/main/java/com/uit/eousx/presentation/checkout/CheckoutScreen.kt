package com.uit.eousx.presentation.checkout

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.domain.model.Booking
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXPrimaryButton
import com.uit.eousx.presentation.components.EousXSecondaryButton
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTypography
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun CheckoutScreen(
    showtimeId: String,
    lockIds: List<String>,
    totalAmount: Int,
    lockedUntil: String?,
    seatCodes: List<String>,
    onReleasedAndBack: () -> Unit,
    onBookingCreated: (Booking) -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.bookingCreated) {
        uiState.bookingCreated?.let { booking ->
            onBookingCreated(booking)
            viewModel.consumeBookingCreated()
        }
    }

    LaunchedEffect(uiState.shouldNavigateBackAfterRelease) {
        if (uiState.shouldNavigateBackAfterRelease) {
            viewModel.consumeNavigateBackAfterRelease()
            onReleasedAndBack()
        }
    }

    LaunchedEffect(lockedUntil, uiState.hasCreatedBooking, uiState.isCreatingBooking) {
        if (lockedUntil.isNullOrBlank() || uiState.hasCreatedBooking || uiState.isCreatingBooking) {
            return@LaunchedEffect
        }

        while (true) {
            val secondsRemaining = secondsUntil(lockedUntil) ?: return@LaunchedEffect
            if (secondsRemaining <= 0) {
                viewModel.handleLockExpired(lockIds)
                return@LaunchedEffect
            }
            delay(1_000L)
        }
    }

    BackHandler(enabled = !uiState.hasCreatedBooking && !uiState.isCreatingBooking) {
        showExitConfirm = true
    }

    CheckoutContent(
        showtimeId = showtimeId,
        lockIds = lockIds,
        totalAmount = totalAmount,
        lockedUntil = lockedUntil,
        seatCodes = seatCodes,
        uiState = uiState,
        showExitConfirm = showExitConfirm,
        onBackClick = {
            if (!uiState.hasCreatedBooking && !uiState.isCreatingBooking) showExitConfirm = true
        },
        onDismissConfirm = { showExitConfirm = false },
        onConfirmExit = {
            showExitConfirm = false
            viewModel.releaseLocksBeforeExit(lockIds)
        },
        onCreateBookingClick = { viewModel.createBooking(showtimeId, lockIds) },
        onRetryRelease = { viewModel.releaseLocksBeforeExit(lockIds) }
    )
}

@Composable
private fun CheckoutContent(
    showtimeId: String,
    lockIds: List<String>,
    totalAmount: Int,
    lockedUntil: String?,
    seatCodes: List<String>,
    uiState: CheckoutUiState,
    showExitConfirm: Boolean,
    onBackClick: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmExit: () -> Unit,
    onCreateBookingClick: () -> Unit,
    onRetryRelease: () -> Unit
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
            CheckoutTopBar(
                enabled = !uiState.isCreatingBooking && !uiState.isReleasing && !uiState.hasCreatedBooking,
                onBackClick = onBackClick
            )

            Text(text = "Checkout", style = EousXTypography.H1)
            Spacer(modifier = Modifier.height(EousXSpacing.xl))

            CheckoutSummaryCard(
                showtimeId = showtimeId,
                lockIds = lockIds,
                totalAmount = totalAmount,
                lockedUntil = lockedUntil,
                seatCodes = seatCodes
            )

            uiState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(EousXSpacing.lg))
                Text(
                    text = message,
                    style = EousXTypography.Caption.copy(color = EousXColors.DangerRed)
                )
                if (uiState.releaseError && uiState.errorMessage.isNotBlank() && !uiState.isCreatingBooking) {
                    Spacer(modifier = Modifier.height(EousXSpacing.sm))
                    EousXSecondaryButton(
                        text = "Retry release",
                        enabled = !uiState.isReleasing,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRetryRelease
                    )
                }
            }

            Spacer(modifier = Modifier.height(EousXSpacing.xxl))
            EousXPrimaryButton(
                text = if (uiState.isCreatingBooking) "Creating booking..." else "Create Booking",
                enabled = lockIds.isNotEmpty() &&
                    !uiState.isCreatingBooking &&
                    !uiState.isReleasing &&
                    !uiState.hasCreatedBooking &&
                    !uiState.lockExpired,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreateBookingClick
            )
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            containerColor = EousXColors.Surface,
            titleContentColor = EousXColors.SoftIvory,
            textContentColor = EousXColors.OnSurfaceDim,
            title = { Text(text = "Release selected seats?", style = EousXTypography.H2) },
            text = {
                Text(
                    text = "Leaving checkout will release your selected seats.",
                    style = EousXTypography.Body
                )
            },
            confirmButton = {
                EousXSecondaryButton(
                    text = "Release",
                    enabled = !uiState.isReleasing,
                    onClick = onConfirmExit
                )
            },
            dismissButton = {
                EousXSecondaryButton(
                    text = "Stay",
                    enabled = !uiState.isReleasing,
                    onClick = onDismissConfirm
                )
            }
        )
    }
}

@Composable
private fun CheckoutTopBar(
    enabled: Boolean,
    onBackClick: () -> Unit
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
            enabled = enabled,
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
}

@Composable
private fun CheckoutSummaryCard(
    showtimeId: String,
    lockIds: List<String>,
    totalAmount: Int,
    lockedUntil: String?,
    seatCodes: List<String>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EousXColors.SurfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            Text(text = "Seats locked", style = EousXTypography.H2)
            SummaryLine(label = "Reserved seats", value = lockIds.size.toString())
            SummaryLine(label = "Lock IDs", value = lockIds.size.toString())
            SummaryLine(label = "Showtime", value = showtimeId)
            if (seatCodes.isNotEmpty()) {
                SummaryLine(label = "Seats", value = seatCodes.joinToString(", "))
            }
            SummaryLine(label = "Amount", value = formatPrice(totalAmount))
            lockedUntil?.takeIf { it.isNotBlank() }?.let {
                SummaryLine(label = "Held until", value = it)
                CountdownLine(lockedUntil = it)
            }
            Text(
                text = "You can proceed to create booking.",
                style = EousXTypography.Body
            )
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = EousXTypography.Caption)
        Text(
            text = value,
            style = EousXTypography.Label.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun CountdownLine(lockedUntil: String) {
    var secondsRemaining by remember(lockedUntil) {
        mutableStateOf(secondsUntil(lockedUntil))
    }

    LaunchedEffect(lockedUntil) {
        while (secondsRemaining != null && secondsRemaining!! > 0) {
            delay(1_000L)
            secondsRemaining = secondsUntil(lockedUntil)
        }
    }

    secondsRemaining?.let { seconds ->
        SummaryLine(
            label = "Time remaining",
            value = formatDuration(seconds)
        )
    }
}

private fun secondsUntil(value: String): Long? {
    val targetInstant = parseInstant(value) ?: return null
    return Duration.between(Instant.now(), targetInstant).seconds.coerceAtLeast(0)
}

private fun parseInstant(value: String): Instant? {
    return try {
        Instant.parse(value)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatPrice(price: Int): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(price)
}
