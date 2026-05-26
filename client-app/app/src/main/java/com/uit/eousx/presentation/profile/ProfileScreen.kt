package com.uit.eousx.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.domain.model.User
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.components.EousXLoadingState
import com.uit.eousx.ui.theme.EousBackground
import com.uit.eousx.ui.theme.EousBorder
import com.uit.eousx.ui.theme.EousCard
import com.uit.eousx.ui.theme.EousMuted
import com.uit.eousx.ui.theme.EousPanel
import com.uit.eousx.ui.theme.EousPrimary
import com.uit.eousx.ui.theme.EousText

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileContent(
        uiState = uiState,
        onRetryClick = viewModel::loadProfile,
        onLogoutClick = { viewModel.logout(onLogout) },
        modifier = modifier
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onRetryClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EousBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = EousText,
                fontWeight = FontWeight.Bold
            )
        }

        when {
            uiState.isLoading -> item {
                EousXLoadingState(message = "Loading profile")
            }
            uiState.errorMessage != null && uiState.user == null -> item {
                EousXErrorState(
                    title = "Unable to load profile",
                    message = uiState.errorMessage,
                    onRetryClick = onRetryClick
                )
            }
            else -> item {
                ProfileCard(user = uiState.user)
            }
        }

        item {
            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EousPrimary,
                    contentColor = EousBackground
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EousCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EousBorder, RoundedCornerShape(18.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(EousPanel, CircleShape)
                        .border(1.dp, EousBorder, CircleShape)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.fullName?.firstOrNull()?.uppercaseChar()?.toString() ?: "E",
                        color = EousPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.fullName?.ifBlank { "EousX User" } ?: "EousX User",
                        color = EousText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user?.email?.ifBlank { "-" } ?: "-",
                        color = EousMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ProfileLine(label = "Full name", value = user?.fullName)
            ProfileLine(label = "Email", value = user?.email)
            if (!user?.phone.isNullOrBlank()) {
                ProfileLine(label = "Phone", value = user?.phone)
            }
        }
    }
}

@Composable
private fun ProfileLine(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = EousMuted, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "-",
            color = EousText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
