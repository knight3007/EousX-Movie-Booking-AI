package com.uit.eousx.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
//  7. TICKET CARD
// ─────────────────────────────────────────────

/**
 * Data model for a booked ticket.
 *
 * @param bookingId   e.g. "EOUSX-240518-7A2B"
 * @param movieTitle  e.g. "Dune: Part Two"
 * @param date        e.g. "Sat, 18 May 2024"
 * @param time        e.g. "07:30 PM"
 * @param screen      e.g. "IMAX 1"
 * @param seats       e.g. "B8, B9"
 * @param isPaid      Controls the status chip (PAID vs LOCKED)
 * @param qrContent   String to encode into a QR code (render yourself via ZXing)
 */
data class EousXTicketData(
    val bookingId:  String,
    val movieTitle: String,
    val date:       String,
    val time:       String,
    val screen:     String,
    val seats:      String,
    val isPaid:     Boolean = true,
    val qrContent:  String  = ""
)

/**
 * 🎟️ Ticket Card — physical cinema ticket feel with torn-edge divider
 *
 * Usage:
 *   EousXTicketCard(
 *       ticket = EousXTicketData(
 *           bookingId  = "EOUSX-240518-7A2B",
 *           movieTitle = "Dune: Part Two",
 *           date       = "Sat, 18 May 2024",
 *           time       = "07:30 PM",
 *           screen     = "IMAX 1",
 *           seats      = "B8, B9",
 *           isPaid     = true
 *       )
 *   )
 *
 * For QR rendering, pass a composable via [qrContent] slot or use
 * ZXing BarcodeEncoder to produce a Bitmap and show it via Image().
 */
@Composable
fun EousXTicketCard(
    ticket: EousXTicketData,
    modifier: Modifier = Modifier,
    qrSlot: @Composable () -> Unit = { QrPlaceholder() },
) {
    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(EousXRadius.xl),
        color           = EousXColors.SurfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column {

            // ── Header ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start  = EousXSpacing.xl,
                        end    = EousXSpacing.xl,
                        top    = EousXSpacing.xl,
                        bottom = EousXSpacing.sm
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = "EOUSX",
                    style = EousXTypography.H2.copy(
                        color         = EousXColors.PrimaryOrange,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
                EousXStatusChip(
                    status = if (ticket.isPaid) EousXChipStatus.PAID else EousXChipStatus.LOCKED
                )
            }

            // ── Movie title ──────────────────────────────────
            Text(
                text     = ticket.movieTitle,
                style    = EousXTypography.H1.copy(fontSize = 22.sp),
                modifier = Modifier.padding(horizontal = EousXSpacing.xl, vertical = EousXSpacing.xs)
            )

            // ── Tear divider ─────────────────────────────────
            TicketTearDivider()

            // ── Info grid + QR ───────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EousXSpacing.xl, vertical = EousXSpacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(EousXSpacing.lg)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.xxxl)) {
                        TicketInfoBlock(label = "Ngày",   value = ticket.date)
                        TicketInfoBlock(label = "Giờ",    value = ticket.time)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(EousXSpacing.xxxl)) {
                        TicketInfoBlock(label = "Phòng",  value = ticket.screen)
                        TicketInfoBlock(label = "Ghế",    value = ticket.seats)
                    }
                }
                // QR code slot — caller provides content
                Box(modifier = Modifier.size(80.dp)) { qrSlot() }
            }

            // ── Booking ID footer ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EousXColors.OrangeAlpha12)
                    .padding(horizontal = EousXSpacing.xl, vertical = EousXSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Mã đặt vé",   style = EousXTypography.Caption)
                    Text(
                        text  = ticket.bookingId,
                        style = EousXTypography.Label.copy(
                            color         = EousXColors.PrimaryOrange,
                            letterSpacing = 1.sp
                        )
                    )
                }
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EousXColors.PrimaryOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EX",
                        style = EousXTypography.Caption.copy(
                            color = EousXColors.PrimaryOrange,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Internal helpers
// ─────────────────────────────────────────────

/** Dashed "tear here" divider with side notch cutouts */
@Composable
private fun TicketTearDivider() {
    val bgColor   = EousXColors.Charcoal
    val dashColor = EousXColors.Divider

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = (-12).dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(bgColor)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = 12.dp)
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .background(bgColor)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            drawLine(
                color       = dashColor,
                start       = Offset(24.dp.toPx(), size.height / 2),
                end         = Offset(size.width - 24.dp.toPx(), size.height / 2),
                strokeWidth = 1.dp.toPx(),
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }
    }
}

@Composable
private fun TicketInfoBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = EousXTypography.Caption)
        Text(text = value, style = EousXTypography.Label)
    }
}

/**
 * Default QR placeholder shown when no bitmap is supplied.
 *
 * Replace with real QR using ZXing:
 *   val bmp = BarcodeEncoder().encodeBitmap(ticket.qrContent, BarcodeFormat.QR_CODE, 200, 200)
 *   Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR")
 */
@Composable
private fun QrPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(EousXRadius.sm))
            .background(EousXColors.Surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = "QR",
            style = EousXTypography.Caption.copy(
                color      = EousXColors.PrimaryOrange,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Preview(name = "EousX Ticket Card", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXTicketCardPreview() {
    EousXTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl)
        ) {
            EousXTicketCard(
                ticket = EousXTicketData(
                    bookingId = "EOUSX-240518-7A2B",
                    movieTitle = "Dune: Part Two",
                    date = "18/05/2024",
                    time = "19:30",
                    screen = "IMAX 01",
                    seats = "B8, B9",
                    isPaid = true
                )
            )
        }
    }
}
