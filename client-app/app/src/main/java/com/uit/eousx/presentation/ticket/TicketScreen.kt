package com.uit.eousx.presentation.ticket

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.domain.model.Ticket
import com.uit.eousx.presentation.components.EousXColors
import com.uit.eousx.presentation.components.EousXEmptyState
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.presentation.components.EousXSpacing
import com.uit.eousx.presentation.components.EousXTypography
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TicketScreen(
    bookingId: String,
    onBackClick: () -> Unit,
    viewModel: TicketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId) {
        viewModel.loadTicket(bookingId)
    }

    TicketContent(
        bookingId = bookingId,
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = viewModel::retry
    )
}

@Composable
private fun TicketContent(
    bookingId: String,
    uiState: TicketUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
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
            TicketTopBar(onBackClick = onBackClick)
            Text(text = "Your Ticket", style = EousXTypography.H1)
            Spacer(modifier = Modifier.height(EousXSpacing.xl))

            when {
                uiState.isLoading -> EousXLoadingState(message = "Loading ticket")
                uiState.errorMessage != null -> {
                    EousXErrorState(
                        title = "Unable to load ticket",
                        message = uiState.errorMessage,
                        actionText = "Retry",
                        onRetryClick = onRetryClick
                    )
                }
                uiState.ticket == null -> {
                    EousXEmptyState(
                        title = "Ticket not found",
                        message = "bookingId: $bookingId"
                    )
                }
                else -> TicketCard(ticket = uiState.ticket)
            }
        }
    }
}

@Composable
private fun TicketTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = EousXSpacing.sm, bottom = EousXSpacing.xl),
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
private fun TicketCard(ticket: Ticket) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EousXColors.SurfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            Text(text = "QR Code", style = EousXTypography.H2)
            Text(
                text = ticket.qrCode.ifBlank { "No QR code returned." },
                style = EousXTypography.Body,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            SummaryLine("Status", ticket.status.ifBlank { "-" })
            SummaryLine("Movie", ticket.movieTitle ?: "-")
            SummaryLine("Room", ticket.roomName ?: "-")
            SummaryLine("Showtime", ticket.startTime ?: "-")
            SummaryLine("Seats", ticket.seatCodes.joinToString(", ").ifBlank { "-" })
            SummaryLine("Total", formatPrice(ticket.totalAmount))
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
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

private fun formatPrice(price: Int): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(price)
}
