package com.uit.eousx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class EousXBottomNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun EousXBottomNavigationBar(
    items: List<EousXBottomNavItem>,
    selectedItemId: String,
    onItemSelected: (EousXBottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = EousXSpacing.lg, vertical = EousXSpacing.md),
        shape = RoundedCornerShape(EousXRadius.pill),
        color = EousXColors.SurfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EousXSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EousXSpacing.xs)
        ) {
            items.forEach { item ->
                val selected = item.id == selectedItemId
                EousXBottomNavigationItem(
                    item = item,
                    selected = selected,
                    onClick = { onItemSelected(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EousXBottomNavigationItem(
    item: EousXBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) EousXColors.OrangeAlpha20 else EousXColors.Transparent
    val contentColor = if (selected) EousXColors.PrimaryOrange else EousXColors.OnSurfaceDim

    Column(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(EousXRadius.pill))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = EousXSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = EousXTypography.Caption.copy(
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1
        )
    }
}

@Preview(name = "EousX Bottom Navigation", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXBottomNavigationPreview() {
    EousXTheme {
        EousXBottomNavigationBar(
            items = listOf(
                EousXBottomNavItem("home", "Home", Icons.Default.Home),
                EousXBottomNavItem("tickets", "Tickets", Icons.Default.ConfirmationNumber),
                EousXBottomNavItem("profile", "Profile", Icons.Default.Person)
            ),
            selectedItemId = "home",
            onItemSelected = {}
        )
    }
}
