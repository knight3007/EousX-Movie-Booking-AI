package com.uit.eousx.presentation.showtime

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.core.utils.formatShowtimeTime
import com.uit.eousx.domain.model.Showtime
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXCustomChip
import com.uit.eousx.presentation.components.EousXDateChip
import com.uit.eousx.presentation.components.EousXEmptyState
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.presentation.components.EousXRadius
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTheme
import com.uit.eousx.presentation.components.EousXTypography
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MovieScheduleScreen(
    movieId: String,
    onBackClick: () -> Unit,
    onShowtimeClick: (String) -> Unit,
    viewModel: ShowtimeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateOptions = remember { nextSixDates() }

    LaunchedEffect(movieId) {
        val initialDate = dateOptions.first().apiValue
        viewModel.loadShowtimes(movieId = movieId, date = initialDate)
    }

    MovieScheduleContent(
        uiState = uiState,
        dateOptions = dateOptions,
        onBackClick = onBackClick,
        onDateClick = { date -> viewModel.selectDate(movieId, date) },
        onRetryClick = viewModel::retry,
        onShowtimeClick = { showtime ->
            viewModel.selectShowtime(showtime.id)
            onShowtimeClick(showtime.id)
        }
    )
}

@Composable
private fun MovieScheduleContent(
    uiState: ShowtimeUiState,
    dateOptions: List<DateChipUiModel>,
    onBackClick: () -> Unit,
    onDateClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onShowtimeClick: (Showtime) -> Unit
) {
    Scaffold(
        containerColor = EousXColors.Charcoal
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EousXColors.Charcoal)
                .padding(paddingValues)
                .padding(horizontal = EousXSpacing.xl)
        ) {
            ScheduleTopBar(onBackClick = onBackClick)

            Text(text = "Select Showtime", style = EousXTypography.H1)
            Spacer(modifier = Modifier.height(EousXSpacing.lg))

            DateSelector(
                dates = dateOptions,
                selectedDate = uiState.selectedDate,
                onDateClick = onDateClick
            )

            Spacer(modifier = Modifier.height(EousXSpacing.xl))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    uiState.isLoading -> {
                        EousXLoadingState(
                            message = "Loading showtimes",
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                    uiState.errorMessage != null -> {
                        EousXErrorState(
                            title = "Unable to load showtimes",
                            message = uiState.errorMessage,
                            actionText = "Retry",
                            onRetryClick = onRetryClick,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                    uiState.showtimes.isEmpty() -> {
                        EousXEmptyState(
                            title = "No showtimes",
                            message = "Please choose another date.",
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                    else -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md),
                            modifier = Modifier.padding(bottom = EousXSpacing.xxl)
                        ) {
                            uiState.showtimes.forEach { showtime ->
                                ShowtimeCard(
                                    showtime = showtime,
                                    selected = uiState.selectedShowtimeId == showtime.id,
                                    onClick = { onShowtimeClick(showtime) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = EousXSpacing.sm, bottom = EousXSpacing.lg),
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
    }
}

@Composable
private fun DateSelector(
    dates: List<DateChipUiModel>,
    selectedDate: String,
    onDateClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm)
    ) {
        dates.forEach { date ->
            EousXDateChip(
                label = date.label,
                supportingText = date.dayOfMonth,
                selected = selectedDate == date.apiValue,
                onClick = { onDateClick(date.apiValue) }
            )
        }
    }
}

@Composable
private fun ShowtimeCard(
    showtime: Showtime,
    selected: Boolean,
    onClick: () -> Unit
) {
    val isOpen = showtime.status.equals("OPEN", ignoreCase = true) && showtime.availableSeats > 0
    val borderColor = if (selected) EousXColors.PrimaryOrange else EousXColors.Divider
    val alpha = if (isOpen) 1f else 0.62f
    val statusLabel = when {
        !showtime.status.equals("OPEN", ignoreCase = true) -> showtime.status.ifBlank { "UNAVAILABLE" }
        showtime.availableSeats <= 0 -> "SOLD OUT"
        else -> "OPEN"
    }

    Surface(
        onClick = onClick,
        enabled = isOpen,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape = RoundedCornerShape(EousXRadius.lg),
        color = EousXColors.SurfaceVariant,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = EousXColors.PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(EousXSpacing.sm))
                    Text(
                        text = formatShowtimeTime(showtime.startTime),
                        style = EousXTypography.H2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                EousXCustomChip(
                    label = statusLabel,
                    contentColor = if (isOpen) EousXColors.SuccessGreen else EousXColors.DangerRed,
                    borderColor = if (isOpen) EousXColors.SuccessGreen else EousXColors.DangerRed,
                    containerColor = if (isOpen) EousXColors.SuccessAlpha16 else EousXColors.DangerAlpha12
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(EousXSpacing.xs),
                    modifier = Modifier.weight(1f)
                ) {
                    InfoLine(
                        icon = Icons.Default.MeetingRoom,
                        text = showtime.roomName.ifBlank { "Cinema room" }
                    )
                    Text(
                        text = showtime.roomType.ifBlank { "Standard" },
                        style = EousXTypography.Caption.copy(color = EousXColors.NeonLime)
                    )
                    Text(
                        text = formatPrice(showtime.basePrice),
                        style = EousXTypography.Label.copy(color = EousXColors.PrimaryOrange)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = null,
                        tint = EousXColors.OnSurfaceDim,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(EousXSpacing.xs))
                    Text(
                        text = "${showtime.availableSeats} seats",
                        style = EousXTypography.Caption.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EousXColors.OnSurfaceDim,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(EousXSpacing.xs))
        Text(text = text, style = EousXTypography.Label)
    }
}

private data class DateChipUiModel(
    val apiValue: String,
    val label: String,
    val dayOfMonth: String
)

private fun nextSixDates(): List<DateChipUiModel> {
    val today = LocalDate.now()
    val apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val labelFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)
    val dayFormatter = DateTimeFormatter.ofPattern("dd", Locale.US)

    return (0..5).map { offset ->
        val date = today.plusDays(offset.toLong())
        DateChipUiModel(
            apiValue = date.format(apiFormatter),
            label = if (offset == 0) "Today" else date.format(labelFormatter),
            dayOfMonth = date.format(dayFormatter)
        )
    }
}

private fun formatPrice(price: Int): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
    return formatter.format(price)
}

@Preview(name = "Movie schedule", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun MovieScheduleContentPreview() {
    EousXTheme {
        MovieScheduleContent(
            uiState = ShowtimeUiState(
                selectedDate = LocalDate.now().toString(),
                showtimes = listOf(
                    Showtime(
                        id = "st-1",
                        movieId = "movie-1",
                        movieTitle = "Preview Movie",
                        roomId = "room-1",
                        roomName = "Room 01",
                        roomType = "IMAX",
                        startTime = "2026-05-26T19:30:00",
                        endTime = "2026-05-26T21:30:00",
                        basePrice = 120000,
                        status = "OPEN",
                        availableSeats = 48
                    )
                )
            ),
            dateOptions = nextSixDates(),
            onBackClick = {},
            onDateClick = {},
            onRetryClick = {},
            onShowtimeClick = {}
        )
    }
}
