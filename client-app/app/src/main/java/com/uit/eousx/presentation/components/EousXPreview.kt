package com.uit.eousx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(
    name = "EousX UI Kit",
    showBackground = true,
    backgroundColor = 0xFF0B0D12
)
@Composable
fun EousXUIKitPreview() {
    val movie = EousXMovieData(
        id = "dune-2",
        title = "Dune: Hành Tinh Cát 2",
        posterUrl = "",
        rating = 8.7f,
        genres = "Khoa học viễn tưởng - Phiêu lưu",
        year = 2024,
        duration = "2g 46p",
        status = EousXChipStatus.NOW_SHOWING
    )

    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EousXColors.Charcoal)
                .verticalScroll(rememberScrollState())
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.xl)
        ) {
            PreviewSectionLabel("NÚT BẤM")
            EousXPrimaryButton(text = "Đặt vé ngay", modifier = Modifier.fillMaxWidth()) {}
            EousXSecondaryButton(text = "Xem chi tiết", modifier = Modifier.fillMaxWidth()) {}
            EousXOutlineButton(text = "Xem trailer", modifier = Modifier.fillMaxWidth()) {}
            EousXDangerButton(text = "Hủy đặt vé", modifier = Modifier.fillMaxWidth()) {}
            EousXIconButton(icon = Icons.Default.Favorite) {}

            PreviewDivider()

            PreviewSectionLabel("Ô NHẬP")
            var name by remember { mutableStateOf("Nguyễn An") }
            EousXTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Nhập tên của bạn"
            )

            var password by remember { mutableStateOf("") }
            EousXPasswordField(value = password, onValueChange = { password = it })

            var query by remember { mutableStateOf("Dune") }
            EousXSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = "Tìm phim, diễn viên...",
                onFilterClick = {}
            )

            PreviewDivider()

            PreviewSectionLabel("TRẠNG THÁI")
            Row(
                horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                EousXStatusChip(EousXChipStatus.NOW_SHOWING)
                EousXStatusChip(EousXChipStatus.UPCOMING)
                EousXStatusChip(EousXChipStatus.PAID)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(EousXSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                EousXStatusChip(EousXChipStatus.SOLD)
                EousXStatusChip(EousXChipStatus.AVAILABLE)
            }

            PreviewDivider()

            PreviewSectionLabel("THẺ PHIM")
            EousXMovieCard(
                movie = movie,
                onBookNow = {},
                onFavourite = { _, _ -> }
            )

            PreviewDivider()

            PreviewSectionLabel("PHIM NỔI BẬT")
            EousXFeaturedMovieCard(movie = movie, onBookNow = {})

            PreviewDivider()

            PreviewSectionLabel("VÉ ĐÃ ĐẶT")
            EousXTicketCard(
                ticket = EousXTicketData(
                    bookingId = "EOUSX-240518-7A2B",
                    movieTitle = "Dune: Hành Tinh Cát 2",
                    date = "18/05/2024",
                    time = "19:30",
                    screen = "IMAX 01",
                    seats = "B8, B9",
                    isPaid = true
                )
            )

            Spacer(Modifier.height(EousXSpacing.xxxl))
        }
    }
}

@Composable
private fun PreviewSectionLabel(text: String) {
    Text(
        text = text,
        style = EousXTypography.Caption.copy(color = EousXColors.PrimaryOrange)
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun PreviewDivider() {
    HorizontalDivider(color = EousXColors.Divider.copy(alpha = 0.7f))
}
