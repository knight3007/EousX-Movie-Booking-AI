package com.uit.eousx.presentation.movie_detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.uit.eousx.core.utils.MovieImageResolver
import com.uit.eousx.domain.model.Movie
import com.uit.eousx.presentation.common.EousXImagePlaceholder
import com.uit.eousx.presentation.components.EousXErrorState
import com.uit.eousx.ui.theme.EousBackground
import com.uit.eousx.ui.theme.EousBorder
import com.uit.eousx.ui.theme.EousCard
import com.uit.eousx.ui.theme.EousMuted
import com.uit.eousx.ui.theme.EousPanel
import com.uit.eousx.ui.theme.EousPrimary
import com.uit.eousx.ui.theme.EousPrimaryDark
import com.uit.eousx.ui.theme.EousText
import com.uit.eousx.ui.theme.EousXTheme

@Composable
fun MovieDetailScreen(
    movieId: String,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val movie by viewModel.movie.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(movieId) {
        viewModel.getMovieDetail(movieId)
    }

    MovieDetailContent(
        movie = movie,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = onBackClick,
        onBookClick = onBookClick
    )
}

@Composable
fun MovieDetailContent(
    movie: Movie?,
    isLoading: Boolean,
    errorMessage: String? = null,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit
) {
    Scaffold(
        containerColor = EousBackground,
        bottomBar = {
            if (movie != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EousBackground)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onBookClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EousPrimary,
                            contentColor = EousBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("ĐẶT VÉ NGAY", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EousBackground)
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EousPrimary)
                    }
                }

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EousXErrorState(
                            title = "Unable to load movie",
                            message = errorMessage,
                            actionText = null,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

                movie != null -> {
                    MovieDetailBody(
                        movie = movie,
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieDetailBody(
    movie: Movie,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val trailerKey = movie.trailerKey?.trim().orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        MovieHero(
            movie = movie,
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium,
                color = EousText,
                fontWeight = FontWeight.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            MovieMetaRow(movie = movie)

            if (movie.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                GenreRow(genres = movie.genres)
            }

            if (trailerKey.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        val trailerUri = Uri.parse("https://www.youtube.com/watch?v=$trailerKey")
                        val intent = Intent(Intent.ACTION_VIEW, trailerUri)
                        try {
                            context.startActivity(intent)
                        } catch (exception: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                "Unable to open trailer on this device.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EousPanel,
                        contentColor = EousPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, EousPrimary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Text("XEM TRAILER", fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                color = EousText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = movie.overview.ifEmpty { "No overview available for this movie." },
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
                color = EousMuted
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun MovieHero(
    movie: Movie,
    onBackClick: () -> Unit
) {
    val heroImageUrl = MovieImageResolver.heroImageUrl(movie.backdropUrl, movie.posterUrl)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(EousBackground)
    ) {
        if (heroImageUrl != null) {
            SubcomposeAsyncImage(
                model = heroImageUrl,
                contentDescription = movie.title,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.12f),
                            0.58f to Color.Transparent,
                            1.0f to EousBackground
                        )
                    )
                )
        )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, start = 14.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.58f))
                .border(1.dp, EousBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = EousText
            )
        }
    }
}

@Composable
private fun MovieMetaRow(movie: Movie) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(EousPanel)
                .border(1.dp, EousBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = EousPrimary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(text = movie.voteAverage.toString(), color = EousText, fontWeight = FontWeight.Bold)
        }

        movie.runtime?.let {
            MetaChip(text = "${it}m")
        }
        movie.ageRating?.takeIf { it.isNotBlank() }?.let {
            MetaChip(text = it)
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        color = EousText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(EousPanel)
            .border(1.dp, EousBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun GenreRow(genres: List<String>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.forEach { genre ->
            Text(
                text = genre,
                color = EousPrimary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(EousPrimary.copy(alpha = 0.12f))
                    .border(1.dp, EousPrimary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Preview(name = "Movie detail", showBackground = true, showSystemUi = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun MovieDetailContentPreview() {
    EousXTheme {
        MovieDetailContent(
            movie = previewMovieDetail,
            isLoading = false,
            errorMessage = null,
            onBackClick = {},
            onBookClick = {}
        )
    }
}

@Preview(name = "Movie detail - Loading", showBackground = true, showSystemUi = true, backgroundColor = 0xFF0B0B0F)
@Composable
private fun MovieDetailLoadingPreview() {
    EousXTheme {
        MovieDetailContent(
            movie = null,
            isLoading = true,
            errorMessage = null,
            onBackClick = {},
            onBookClick = {}
        )
    }
}

private val previewMovieDetail = Movie(
    id = "preview-1",
    title = "EousX Preview Movie",
    overview = "A short preview description for checking the movie detail layout in Android Studio.",
    posterUrl = "",
    backdropUrl = "",
    releaseDate = "2026-05-10",
    voteAverage = 8.4,
    runtime = 120,
    genres = listOf("Action", "Drama", "Adventure"),
    ageRating = "T13",
    trailerKey = null
)
