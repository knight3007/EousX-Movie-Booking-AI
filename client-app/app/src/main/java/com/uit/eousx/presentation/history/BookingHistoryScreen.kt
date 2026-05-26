package com.uit.eousx.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.domain.model.Booking
import com.uit.eousx.presentation.components.EousXEmptyState
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.ui.theme.EousBackground
import com.uit.eousx.ui.theme.EousBorder
import com.uit.eousx.ui.theme.EousCard
import com.uit.eousx.ui.theme.EousMuted
import com.uit.eousx.ui.theme.EousPanel
import com.uit.eousx.ui.theme.EousPrimary
import com.uit.eousx.ui.theme.EousSuccess
import com.uit.eousx.ui.theme.EousText
import java.text.NumberFormat
import java.util.Locale

private val filters = listOf(
    null to "ALL",
    "WAITING_PAYMENT" to "WAITING",
    "PAID" to "PAID",
    "CHECKED_IN" to "CHECKED IN",
    "CANCELLED" to "CANCELLED",
    "EXPIRED" to "EXPIRED"
)

@Composable
fun BookingHistoryScreen(
    onOpenTicket: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BookingHistoryContent(
        uiState = uiState,
        onOpenTicket = onOpenTicket,
        onRefreshClick = viewModel::refresh,
        onFilterClick = viewModel::setFilter,
        modifier = modifier
    )
}

@Composable
private fun BookingHistoryContent(
    uiState: BookingHistoryUiState,
    onOpenTicket: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onFilterClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EousBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Tickets",
                    style = MaterialTheme.typography.headlineSmall,
                    color = EousText,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefreshClick, enabled = !uiState.isRefreshing) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = EousPrimary
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters, key = { it.second }) { (status, label) ->
                    FilterChip(
                        label = label,
                        selected = uiState.selectedStatusFilter == status,
                        onClick = { onFilterClick(status) }
                    )
                }
            }
        }

        when {
            uiState.isLoading -> item {
                EousXLoadingState(message = "Loading bookings")
            }
            uiState.errorMessage != null && uiState.bookings.isEmpty() -> item {
                EousXErrorState(
                    title = "Unable to load bookings",
                    message = uiState.errorMessage,
                    onRetryClick = onRefreshClick
                )
            }
            uiState.filteredBookings.isEmpty() -> item {
                EousXEmptyState(
                    title = "No bookings yet",
                    message = "Your movie tickets will appear here.",
                    actionText = "Refresh",
                    onActionClick = onRefreshClick
                )
            }
            else -> items(uiState.filteredBookings, key = { it.id }) { booking ->
                BookingCard(
                    booking = booking,
                    onClick = {
                        if (booking.canOpenTicket()) {
                            onOpenTicket(booking.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = if (selected) EousBackground else EousMuted,
                fontWeight = FontWeight.Bold
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) EousPrimary else EousPanel
        ),
        border = BorderStroke(1.dp, if (selected) EousPrimary else EousBorder)
    )
}

@Composable
private fun BookingCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val enabled = booking.canOpenTicket()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EousCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EousBorder, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.movieTitle ?: "Movie",
                        style = MaterialTheme.typography.titleMedium,
                        color = EousText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Code: ${booking.code.ifBlank { booking.id }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EousMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(status = booking.status)
            }

            SummaryLine("Room", booking.roomName ?: "-")
            SummaryLine("Showtime", booking.startTime ?: "-")
            SummaryLine("Seats", booking.seatCodes.joinToString(", ").ifBlank { "-" })
            SummaryLine("Total", formatPrice(booking.totalAmount))
            SummaryLine("Payment", booking.paymentStatus ?: "-")
            SummaryLine("Ticket", booking.ticketStatus ?: "-")

            if (booking.status.equals("WAITING_PAYMENT", ignoreCase = true)) {
                Text(
                    text = "Payment pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = EousMuted
                )
            } else if (enabled) {
                Text(
                    text = "Tap to open ticket",
                    style = MaterialTheme.typography.bodySmall,
                    color = EousPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when {
        status.equals("PAID", ignoreCase = true) -> EousSuccess
        status.equals("CHECKED_IN", ignoreCase = true) -> EousSuccess
        status.equals("WAITING_PAYMENT", ignoreCase = true) -> EousPrimary
        status.equals("CANCELLED", ignoreCase = true) -> Color(0xFFFF6B6B)
        status.equals("EXPIRED", ignoreCase = true) -> Color(0xFFFF6B6B)
        else -> EousMuted
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = status.ifBlank { "UNKNOWN" },
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = EousMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = EousText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun Booking.canOpenTicket(): Boolean {
    return ticketStatus != null &&
        (status.equals("PAID", ignoreCase = true) || status.equals("CHECKED_IN", ignoreCase = true))
}

private fun formatPrice(price: Int): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(price)
}
