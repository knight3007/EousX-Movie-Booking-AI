package com.uit.eousx.presentation.seat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.uit.eousx.domain.model.Seat
import com.uit.eousx.domain.model.SeatLockResult
import com.uit.eousx.domain.model.SeatStatus
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXEmptyState
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.presentation.components.EousXPrimaryButton
import com.uit.eousx.presentation.components.EousXScreenIndicator
import com.uit.eousx.presentation.components.EousXSeat
import com.uit.eousx.presentation.components.EousXSeatLegend
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTheme
import com.uit.eousx.presentation.components.EousXTypography
import com.uit.eousx.presentation.components.SeatUiState
import java.text.NumberFormat
import java.util.Locale
import com.uit.eousx.presentation.components.SeatStatus as UiSeatStatus

@Composable
fun SeatMapScreen(
    showtimeId: String,
    onBackClick: () -> Unit,
    onLockSuccess: (SeatLockResult) -> Unit,
    viewModel: SeatMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(showtimeId) {
        viewModel.startPolling(showtimeId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    DisposableEffect(lifecycleOwner, showtimeId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.lockSuccess) {
        uiState.lockSuccess?.let { result ->
            onLockSuccess(result)
            viewModel.consumeLockSuccess()
        }
    }

    SeatMapContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefreshClick = viewModel::refreshNow,
        onRetryClick = viewModel::retry,
        onSeatClick = viewModel::toggleSeat,
        onContinueClick = viewModel::lockSelectedSeats
    )
}

@Composable
private fun SeatMapContent(
    uiState: SeatMapUiState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSeatClick: (String) -> Unit,
    onContinueClick: () -> Unit
) {
    Scaffold(
        containerColor = EousXColors.Charcoal,
        bottomBar = {
            if (!uiState.isLoading && uiState.seats.isNotEmpty()) {
                SeatSummaryBar(
                    uiState = uiState,
                    onContinueClick = onContinueClick
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EousXColors.Charcoal)
                .padding(paddingValues)
                .padding(horizontal = EousXSpacing.xl)
        ) {
            SeatTopBar(
                isRefreshing = uiState.isRefreshing,
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick
            )

            Text(text = "Select Seats", style = EousXTypography.H1)
            Spacer(modifier = Modifier.height(EousXSpacing.xs))
            Text(
                text = if (uiState.isRefreshing) "Refreshing seat map..." else "Auto refresh every 5s",
                style = EousXTypography.Caption
            )
            Spacer(modifier = Modifier.height(EousXSpacing.xl))

            when {
                uiState.isLoading -> {
                    EousXLoadingState(message = "Loading seat map")
                }
                uiState.errorMessage != null && uiState.seats.isEmpty() -> {
                    EousXErrorState(
                        title = "Unable to load seats",
                        message = uiState.errorMessage,
                        actionText = "Retry",
                        onRetryClick = onRetryClick
                    )
                }
                uiState.seats.isEmpty() -> {
                    EousXEmptyState(
                        title = "No seats",
                        message = "Seat map is not available for this showtime."
                    )
                }
                else -> {
                    SeatMapBody(
                        uiState = uiState,
                        onSeatClick = onSeatClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SeatTopBar(
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = EousXSpacing.sm, bottom = EousXSpacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
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

        IconButton(
            onClick = onRefreshClick,
            enabled = !isRefreshing,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EousXColors.SurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = if (isRefreshing) EousXColors.SlateGray else EousXColors.PrimaryOrange
            )
        }
    }
}

@Composable
private fun SeatMapBody(
    uiState: SeatMapUiState,
    onSeatClick: (String) -> Unit
) {
    val selectedSeatIds = uiState.selectedSeatIds
    val rows = uiState.seats
        .sortedWith(compareBy<Seat> { it.row }.thenBy { it.number })
        .groupBy { it.row }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(EousXSpacing.lg)
    ) {
        EousXScreenIndicator(label = "Screen")
        EousXSeatLegend(
            statuses = listOf(
                UiSeatStatus.AVAILABLE,
                UiSeatStatus.SELECTED,
                UiSeatStatus.LOCKED,
                UiSeatStatus.SOLD,
                UiSeatStatus.MAINTENANCE
            )
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = EousXTypography.Caption.copy(color = EousXColors.DangerRed)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { (_, seats) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        seats.forEach { seat ->
                            EousXSeat(
                                seat = seat.toUiState(selectedSeatIds),
                                onClick = { onSeatClick(seat.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatSummaryBar(
    uiState: SeatMapUiState,
    onContinueClick: () -> Unit
) {
    val selectedSeats = uiState.seats
        .filter { it.id in uiState.selectedSeatIds }
        .sortedWith(compareBy<Seat> { it.row }.thenBy { it.number })
    val selectedCodes = selectedSeats.joinToString(", ") { it.code }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EousXColors.Surface)
            .navigationBarsPadding()
            .padding(EousXSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${selectedSeats.size} selected",
                    style = EousXTypography.Label
                )
                Text(
                    text = selectedCodes.ifBlank { "No seats selected" },
                    style = EousXTypography.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatPrice(uiState.totalSelectedAmount),
                style = EousXTypography.Label.copy(
                    color = EousXColors.PrimaryOrange,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        EousXPrimaryButton(
            text = if (uiState.isLocking) "Locking..." else "Continue",
            enabled = selectedSeats.isNotEmpty() && !uiState.isLocking,
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClick
        )
    }
}

private fun Seat.toUiState(selectedSeatIds: Set<String>): SeatUiState {
    val uiStatus = if (id in selectedSeatIds) {
        UiSeatStatus.SELECTED
    } else {
        status.toUiStatus()
    }
    return SeatUiState(
        id = id,
        label = code,
        status = uiStatus
    )
}

private fun SeatStatus.toUiStatus(): UiSeatStatus {
    return when (this) {
        SeatStatus.AVAILABLE -> UiSeatStatus.AVAILABLE
        SeatStatus.SELECTED -> UiSeatStatus.SELECTED
        SeatStatus.LOCKED -> UiSeatStatus.LOCKED
        SeatStatus.SOLD -> UiSeatStatus.SOLD
        SeatStatus.MAINTENANCE -> UiSeatStatus.MAINTENANCE
    }
}

private fun formatPrice(price: Int): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(price)
}

@Preview(name = "Seat map", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun SeatMapContentPreview() {
    EousXTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            SeatMapContent(
                uiState = SeatMapUiState(
                    seats = listOf(
                        Seat("1", "A1", "A", 1, "STANDARD", SeatStatus.AVAILABLE, 100000),
                        Seat("2", "A2", "A", 2, "STANDARD", SeatStatus.LOCKED, 100000),
                        Seat("3", "A3", "A", 3, "STANDARD", SeatStatus.SOLD, 100000),
                        Seat("4", "B1", "B", 1, "VIP", SeatStatus.AVAILABLE, 150000)
                    ),
                    selectedSeatIds = setOf("1"),
                    totalSelectedAmount = 100000
                ),
                onBackClick = {},
                onRefreshClick = {},
                onRetryClick = {},
                onSeatClick = {},
                onContinueClick = {}
            )
        }
    }
}
