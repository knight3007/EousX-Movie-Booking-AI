package com.uit.eousx.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class PaymentProviderUi {
    MOCK,
    ZALOPAY
}

@Composable
fun EousXPriceRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) EousXTypography.Label else EousXTypography.Body
        )
        Text(
            text = value,
            style = if (emphasized) {
                EousXTypography.Label.copy(color = EousXColors.PrimaryOrange, fontWeight = FontWeight.Bold)
            } else {
                EousXTypography.Body.copy(color = EousXColors.SoftIvory)
            }
        )
    }
}

@Composable
fun EousXCheckoutSummaryCard(
    rows: List<Pair<String, String>>,
    totalLabel: String,
    totalValue: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EousXRadius.lg),
        color = EousXColors.SurfaceVariant,
        border = BorderStroke(1.dp, EousXColors.Divider)
    ) {
        Column(
            modifier = Modifier.padding(EousXSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.md)
        ) {
            rows.forEach { (label, value) ->
                EousXPriceRow(label = label, value = value)
            }
            Surface(color = EousXColors.Divider, modifier = Modifier.fillMaxWidth()) {}
            EousXPriceRow(label = totalLabel, value = totalValue, emphasized = true)
        }
    }
}

@Composable
fun EousXPaymentMethodCard(
    provider: PaymentProviderUi,
    selected: Boolean,
    onClick: (PaymentProviderUi) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val borderColor = if (selected) EousXColors.PrimaryOrange else EousXColors.Divider
    val icon = provider.icon()
    Surface(
        modifier = modifier
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
                Text(text = provider.label(), style = EousXTypography.Label)
                Text(text = provider.description(), style = EousXTypography.Caption)
            }
            if (selected) {
                EousXStatusChip(EousXChipStatus.AVAILABLE)
            }
        }
    }
}

private fun PaymentProviderUi.label(): String = when (this) {
    PaymentProviderUi.MOCK -> "Mock payment"
    PaymentProviderUi.ZALOPAY -> "ZaloPay"
}

private fun PaymentProviderUi.description(): String = when (this) {
    PaymentProviderUi.MOCK -> "Use for test checkout flows"
    PaymentProviderUi.ZALOPAY -> "Pay with ZaloPay wallet"
}

private fun PaymentProviderUi.icon(): ImageVector = when (this) {
    PaymentProviderUi.MOCK -> Icons.Default.CreditCard
    PaymentProviderUi.ZALOPAY -> Icons.Default.AccountBalanceWallet
}

@Preview(name = "EousX Checkout", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXCheckoutPreview() {
    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.lg)
        ) {
            EousXCheckoutSummaryCard(
                rows = listOf(
                    "Tickets" to "$24.00",
                    "Service fee" to "$1.20"
                ),
                totalLabel = "Total",
                totalValue = "$25.20"
            )
            EousXPaymentMethodCard(
                provider = PaymentProviderUi.MOCK,
                selected = true,
                onClick = {}
            )
            EousXPaymentMethodCard(
                provider = PaymentProviderUi.ZALOPAY,
                selected = false,
                onClick = {}
            )
        }
    }
}
