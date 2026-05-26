
package com.uit.eousx.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.uit.eousx.R
import com.uit.eousx.core.utils.MovieImageResolver
import com.uit.eousx.domain.model.Movie
import com.uit.eousx.presentation.common.EousXImagePlaceholder
import com.uit.eousx.presentation.components.EousXEmptyState
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.presentation.history.BookingHistoryScreen
import com.uit.eousx.presentation.profile.ProfileScreen
import com.uit.eousx.ui.theme.EousBackground
import com.uit.eousx.ui.theme.EousBorder
import com.uit.eousx.ui.theme.EousCard
import com.uit.eousx.ui.theme.EousMuted
import com.uit.eousx.ui.theme.EousPanel
import com.uit.eousx.ui.theme.EousPrimary
import com.uit.eousx.ui.theme.EousText
import com.uit.eousx.ui.theme.EousXTheme

@Composable
fun HomeScreen(
    onOpenMovieDetail: (String) -> Unit,
    onOpenTicket: (String) -> Unit,
    onOpenAiChat: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MovieViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = EousBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAiChat,
                containerColor = EousPrimary,
                contentColor = EousBackground,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(58.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon_chatai),
                    contentDescription = "Chat AI",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }
        },
        bottomBar = {
            EousBottomTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(EousBackground)
        ) {
            when (selectedTab) {
                0 -> MovieListContent(
                    uiState = uiState,
                    onRefresh = viewModel::refreshMovies,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::clearSearch,
                    onRetryClick = viewModel::retry,
                    onMovieClick = onOpenMovieDetail
                )

                1 -> BookingHistoryScreen(onOpenTicket = onOpenTicket)
                2 -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun EousBottomTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(EousPanel)
            .border(1.dp, EousBorder, RoundedCornerShape(28.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EousTabItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.icon_home,
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            EousTabItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.icon_ticket,
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
            EousTabItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.icon_canhan,
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListContent(
    uiState: HomeMovieUiState,
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onRetryClick: () -> Unit = {},
    onMovieClick: (String) -> Unit
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EousBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EousPrimary)
            }
        }

        uiState.errorMessage != null && uiState.allMovies.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EousBackground),
                contentAlignment = Alignment.Center
            ) {
                EousXErrorState(
                    title = "Unable to load movies",
                    message = uiState.errorMessage,
                    onRetryClick = onRetryClick,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .background(EousBackground)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EousBackground),
                    contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HomeHeader(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            onClearSearch = onClearSearch
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Text(
                                text = uiState.errorMessage,
                                color = EousPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EousPanel)
                                    .border(1.dp, EousBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            )
                        }
                    }

                    if (uiState.visibleMovies.isEmpty()) {
                        item {
                            EousXEmptyState(
                                title = if (uiState.searchQuery.isBlank()) {
                                    "No movies"
                                } else {
                                    "Không tìm thấy phim phù hợp"
                                },
                                message = if (uiState.searchQuery.isBlank()) {
                                    "Pull down to refresh the movie list."
                                } else {
                                    "Thử tìm theo tên phim, thể loại hoặc độ tuổi."
                                },
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    } else {
                        items(items = uiState.visibleMovies, key = { it.id }) { movie ->
                            MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "EOUSX",
                    style = MaterialTheme.typography.headlineLarge,
                    color = EousPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Smart Movie Booking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EousMuted
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(EousPanel)
                    .border(1.dp, EousBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "EX", color = EousPrimary, fontWeight = FontWeight.Bold)
            }
        }

        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EousPanel)
                .border(1.dp, EousBorder, RoundedCornerShape(16.dp)),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search movies",
                    color = EousMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = EousMuted, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = EousPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = EousPrimary, modifier = Modifier.size(20.dp))
                }
            },
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
                unfocusedTextColor = EousText
            )
        )

        Text(
            text = "Now Showing",
            style = MaterialTheme.typography.titleLarge,
            color = EousText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EousCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, EousBorder, RoundedCornerShape(18.dp))
        ) {
            MoviePoster(
                posterUrl = movie.posterUrl,
                backdropUrl = movie.backdropUrl,
                title = movie.title,
                modifier = Modifier
                    .width(104.dp)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = EousText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = movie.genres.take(2).joinToString(" / ").ifBlank { "Cinema" },
                        style = MaterialTheme.typography.bodySmall,
                        color = EousMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (movie.runtime != null || !movie.ageRating.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = listOfNotNull(
                                movie.runtime?.let { "${it}m" },
                                movie.ageRating
                            ).joinToString(" | "),
                            style = MaterialTheme.typography.bodySmall,
                            color = EousMuted
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = EousPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = movie.voteAverage.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EousText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MoviePoster(
    posterUrl: String?,
    backdropUrl: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    val imageUrl = MovieImageResolver.posterImageUrl(posterUrl, backdropUrl)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
            .background(EousPanel),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
                    else -> EousXImagePlaceholder(text = "EOUSX")
                }
            }
        } else {
            EousXImagePlaceholder(text = "EOUSX")
        }
    }
}

@Composable
fun EousTabItem(modifier: Modifier, iconRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) EousPrimary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(42.dp)
        )
    }
}

@Preview(name = "Home - Movie list", showBackground = true, showSystemUi = true)
@Composable
private fun MovieListContentPreview() {
    EousXTheme {
        MovieListContent(
            uiState = HomeMovieUiState(
                allMovies = previewMovies,
                visibleMovies = previewMovies
            ),
            onMovieClick = {}
        )
    }
}

@Preview(name = "Home - Loading", showBackground = true, showSystemUi = true)
@Composable
private fun MovieListLoadingPreview() {
    EousXTheme {
        MovieListContent(
            uiState = HomeMovieUiState(isLoading = true),
            onMovieClick = {}
        )
    }
}

@Preview(name = "Movie card", showBackground = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun MovieCardPreview() {
    EousXTheme {
        Box(
            modifier = Modifier
                .background(EousBackground)
                .padding(16.dp)
        ) {
            MovieCard(
                movie = previewMovies.first(),
                onClick = {}
            )
        }
    }
}

private val previewMovies = listOf(
    Movie(
        id = "preview-1",
        title = "EousX Preview Movie",
        overview = "Preview overview",
        posterUrl = "",
        backdropUrl = "",
        releaseDate = "2026-05-10",
        voteAverage = 8.4,
        runtime = 120,
        genres = listOf("Action", "Drama"),
        ageRating = "T13",
        trailerKey = null
    ),
    Movie(
        id = "preview-2",
        title = "Second Preview Movie With A Longer Title",
        overview = "Preview overview",
        posterUrl = "",
        backdropUrl = "",
        releaseDate = "2026-05-11",
        voteAverage = 7.8,
        runtime = 95,
        genres = listOf("Drama"),
        ageRating = "T16",
        trailerKey = null
    )
)
