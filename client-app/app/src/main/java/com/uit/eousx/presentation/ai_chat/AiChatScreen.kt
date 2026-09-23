package com.uit.eousx.presentation.ai_chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.uit.eousx.domain.model.AiRecommendedMovie
import com.uit.eousx.domain.model.AiRecommendedShowtime
import com.uit.eousx.ui.theme.EousBackground
import com.uit.eousx.ui.theme.EousBorder
import com.uit.eousx.ui.theme.EousCard
import com.uit.eousx.ui.theme.EousMuted
import com.uit.eousx.ui.theme.EousPanel
import com.uit.eousx.ui.theme.EousPrimary
import com.uit.eousx.ui.theme.EousText
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onBookMovie: (String) -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    AiChatContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSendMessage = viewModel::sendMessage,
        onOpenMovie = onOpenMovie,
        onBookMovie = onBookMovie
    )
}

@Composable
private fun AiChatContent(
    uiState: AiChatUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenMovie: (String) -> Unit,
    onBookMovie: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = EousBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AiChatTopBar(onBack = onBack)
        },
        bottomBar = {
            AiInputRow(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = !uiState.isLoading,
                onSend = {
                    val message = inputText
                    if (message.isNotBlank() && !uiState.isLoading) {
                        inputText = ""
                        onSendMessage(message)
                    }
                },
                modifier = Modifier.imePadding()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(EousBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SuggestedPromptRow(
                    prompts = uiState.suggestedPrompts,
                    enabled = !uiState.isLoading,
                    onPromptClick = onSendMessage
                )
            }

            if (uiState.messages.isEmpty()) {
                item {
                    EmptyAiState()
                }
            }

            items(items = uiState.messages, key = { it.id }) { message ->
                AiMessageBubble(
                    message = message,
                    onOpenMovie = onOpenMovie,
                    onBookMovie = onBookMovie
                )
            }

            if (uiState.isLoading) {
                item {
                    AiLoadingBubble()
                }
            }
        }
    }
}

@Composable
private fun AiChatTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(EousBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EousPanel)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = EousText
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "EousX AI",
                style = MaterialTheme.typography.titleLarge,
                color = EousText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hỏi phim đang chiếu tại rạp",
                style = MaterialTheme.typography.bodySmall,
                color = EousMuted
            )
        }
    }
}

@Composable
private fun SuggestedPromptRow(
    prompts: List<String>,
    enabled: Boolean,
    onPromptClick: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(prompts) { prompt ->
            AssistChip(
                onClick = { onPromptClick(prompt) },
                enabled = enabled,
                label = {
                    Text(
                        text = prompt,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = EousPanel,
                    labelColor = EousText,
                    disabledContainerColor = EousPanel.copy(alpha = 0.6f),
                    disabledLabelColor = EousMuted
                ),
                border = BorderStroke(1.dp, EousBorder)
            )
        }
    }
}

@Composable
private fun EmptyAiState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = EousPanel,
        border = BorderStroke(1.dp, EousBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalMovies,
                contentDescription = null,
                tint = EousPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Bạn muốn xem phim gì hôm nay?",
                style = MaterialTheme.typography.titleMedium,
                color = EousText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hỏi theo thời gian, tâm trạng hoặc người đi cùng. EousX AI sẽ gợi ý phim có trong hệ thống.",
                style = MaterialTheme.typography.bodyMedium,
                color = EousMuted
            )
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiChatMessageUi,
    onOpenMovie: (String) -> Unit,
    onBookMovie: (String) -> Unit
) {
    val isUser = message.role == AiChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 1f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) EousPrimary else EousPanel,
                border = if (isUser) null else BorderStroke(1.dp, EousBorder)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) EousBackground else EousText
                )
            }

            message.recommendedMovies.forEach { movie ->
                AiRecommendedMovieCard(
                    movie = movie,
                    onOpenMovie = { onOpenMovie(movie.movieId) },
                    onBookMovie = { onBookMovie(movie.movieId) }
                )
            }
        }
    }
}

@Composable
private fun AiRecommendedMovieCard(
    movie: AiRecommendedMovie,
    onOpenMovie: () -> Unit,
    onBookMovie: () -> Unit
) {
    val nearestShowtime = movie.showtimes.firstOrNull()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMovie() },
        shape = RoundedCornerShape(16.dp),
        color = EousCard,
        border = BorderStroke(1.dp, EousBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleMedium,
                color = EousText,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!movie.reason.isNullOrBlank()) {
                Text(
                    text = movie.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EousMuted
                )
            }

            if (nearestShowtime != null) {
                ShowtimeSummary(showtime = nearestShowtime)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenMovie,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EousText,
                        containerColor = EousPanel
                    ),
                    border = BorderStroke(1.dp, EousBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Chi tiết", maxLines = 1)
                }
                Button(
                    onClick = onBookMovie,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EousPrimary,
                        contentColor = EousBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Đặt vé", maxLines = 1, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShowtimeSummary(showtime: AiRecommendedShowtime) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EousPanel)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        InfoLine(
            icon = Icons.Default.Schedule,
            text = formatShowtimeDateTime(showtime.startTime)
        )
        InfoLine(
            icon = Icons.Default.MeetingRoom,
            text = listOfNotNull(
                showtime.roomName?.takeIf { it.isNotBlank() },
                showtime.roomType?.takeIf { it.isNotBlank() }
            ).joinToString(" - ").ifBlank { "Phòng chiếu" }
        )
        if (showtime.basePrice != null) {
            Text(
                text = formatPrice(showtime.basePrice),
                style = MaterialTheme.typography.labelLarge,
                color = EousPrimary,
                fontWeight = FontWeight.Bold
            )
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
            tint = EousMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = EousMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AiLoadingBubble() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = EousPanel,
        border = BorderStroke(1.dp, EousBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = EousPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "EousX AI đang trả lời...",
                style = MaterialTheme.typography.bodyMedium,
                color = EousMuted
            )
        }
    }
}

@Composable
private fun AiInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EousBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(18.dp)),
            placeholder = {
                Text(text = "Hỏi EousX AI...", color = EousMuted)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = EousText),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = EousPanel,
                unfocusedContainerColor = EousPanel,
                disabledContainerColor = EousPanel,
                focusedIndicatorColor = EousPanel,
                unfocusedIndicatorColor = EousPanel,
                disabledIndicatorColor = EousPanel,
                cursorColor = EousPrimary,
                focusedTextColor = EousText,
                unfocusedTextColor = EousText,
                disabledTextColor = EousMuted
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (enabled && value.isNotBlank()) EousPrimary else EousPanel)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (enabled && value.isNotBlank()) EousBackground else EousMuted
            )
        }
    }
}

private fun formatShowtimeDateTime(value: String?): String {
    if (value.isNullOrBlank()) return "Suất chiếu gần nhất"
    val formatter = DateTimeFormatter.ofPattern("HH:mm, dd/MM", Locale.US)
    return parseDateTime(value)?.format(formatter) ?: value
}

private fun parseDateTime(value: String): LocalDateTime? {
    val parsers = listOf<() -> LocalDateTime>(
        { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() },
        { LocalDateTime.parse(value) }
    )
    return parsers.firstNotNullOfOrNull { parser ->
        try {
            parser()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private fun formatPrice(price: Int): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
    return formatter.format(price)
}
