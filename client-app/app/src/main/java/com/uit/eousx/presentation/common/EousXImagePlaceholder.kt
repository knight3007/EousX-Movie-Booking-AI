package com.uit.eousx.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uit.eousx.ui.theme.EousCard
import com.uit.eousx.ui.theme.EousMuted
import com.uit.eousx.ui.theme.EousPanel
import com.uit.eousx.ui.theme.EousPrimaryDark

@Composable
fun EousXImagePlaceholder(
    modifier: Modifier = Modifier,
    text: String = "No image"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(EousCard, EousPanel)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (text == "EOUSX") EousPrimaryDark else EousMuted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp)
        )
    }
}
