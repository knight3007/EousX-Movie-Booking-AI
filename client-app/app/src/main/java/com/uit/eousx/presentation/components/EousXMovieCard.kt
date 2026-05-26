package com.uit.eousx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ─────────────────────────────────────────────
//  7. MOVIE CARD
// ─────────────────────────────────────────────

/**
 * Data model for a movie entry.
 *
 * @param id          Unique identifier
 * @param title       Movie title
 * @param posterUrl   Remote image URL (uses Coil for async loading)
 * @param rating      IMDb-style rating 0.0–10.0
 * @param genres      Comma-separated genres, e.g. "Sci-Fi • Adventure"
 * @param year        Release year
 * @param duration    Human-readable duration, e.g. "2h 46m"
 * @param status      Current showing status
 */
data class EousXMovieData(
    val id:         String,
    val title:      String,
    val posterUrl:  String,
    val rating:     Float,
    val genres:     String,
    val year:       Int,
    val duration:   String,
    val status:     EousXChipStatus = EousXChipStatus.NOW_SHOWING,
)

/**
 * 🎬 Movie Card — vertical poster card with rating, status chip and CTA
 *
 * Requires Coil in build.gradle:
 *   implementation("io.coil-kt:coil-compose:2.6.0")
 *
 * Usage:
 *   EousXMovieCard(
 *       movie      = EousXMovieData(
 *           id       = "1",
 *           title    = "Dune: Part Two",
 *           posterUrl= "https://…",
 *           rating   = 8.7f,
 *           genres   = "Sci-Fi • Adventure",
 *           year     = 2024,
 *           duration = "2h 46m"
 *       ),
 *       onBookNow  = { movie -> /* navigate to booking */ },
 *       onFavourite= { movie, isFav -> /* toggle wishlist */ }
 *   )
 */
@Composable
fun EousXMovieCard(
    movie: EousXMovieData,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 160.dp,
    onBookNow: ((EousXMovieData) -> Unit)? = null,
    onFavourite: ((EousXMovieData, Boolean) -> Unit)? = null,
    onClick: ((EousXMovieData) -> Unit)? = null,
) {
    var isFavourite by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(EousXRadius.lg)

    Card(
        modifier = modifier
            .width(cardWidth)
            .clickable(enabled = onClick != null) { onClick?.invoke(movie) },
        shape    = shape,
        colors   = CardDefaults.cardColors(containerColor = EousXColors.SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // ── Poster image with overlaid status + favourite ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(EousXColors.SurfaceRaised)
            ) {
                // Poster
                AsyncImage(
                    model             = movie.posterUrl,
                    contentDescription = movie.title,
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = EousXRadius.lg, topEnd = EousXRadius.lg))
                )

                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(EousXColors.Transparent, EousXColors.SurfaceVariant)
                            )
                        )
                )

                // Status chip — top left
                EousXStatusChip(
                    status   = movie.status,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(EousXSpacing.sm)
                )

                // Favourite button — top right
                if (onFavourite != null) {
                    IconButton(
                        onClick  = {
                            isFavourite = !isFavourite
                            onFavourite(movie, isFavourite)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(EousXSpacing.xs)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector        = if (isFavourite) Icons.Default.Favorite
                                                 else Icons.Default.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint               = if (isFavourite) EousXColors.PrimaryOrange
                                                 else EousXColors.SoftIvory,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }

                // Rating badge — bottom left
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(EousXSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint               = EousXColors.PrimaryOrange,
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text  = movie.rating.toString(),
                        style = EousXTypography.Caption.copy(
                            color      = EousXColors.SoftIvory,
                            fontSize   = 12.sp,
                        )
                    )
                }

                // Duration badge — bottom right
                Text(
                    text     = movie.duration,
                    style    = EousXTypography.Caption.copy(
                        color    = EousXColors.OnSurfaceDim,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(EousXSpacing.sm)
                )
            }

            // ── Text info area ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EousXSpacing.md, vertical = EousXSpacing.sm)
            ) {
                Text(
                    text     = movie.title,
                    style    = EousXTypography.Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "${movie.genres} • ${movie.year}",
                    style = EousXTypography.Caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (onBookNow != null) {
                    Spacer(Modifier.height(EousXSpacing.md))
                    EousXPrimaryButton(
                        text       = "Đặt vé",
                        modifier   = Modifier.fillMaxWidth(),
                        showArrow  = false,
                        onClick    = { onBookNow(movie) }
                    )
                    Spacer(Modifier.height(EousXSpacing.sm))
                }
            }
        }
    }
}

/**
 * Wide / Featured variant — landscape format for the hero carousel
 *
 * Usage:
 *   EousXFeaturedMovieCard(movie = featuredMovie, onBookNow = { … })
 */
@Composable
fun EousXFeaturedMovieCard(
    movie: EousXMovieData,
    modifier: Modifier = Modifier,
    onBookNow: ((EousXMovieData) -> Unit)? = null,
    onClick: ((EousXMovieData) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(EousXRadius.xl)

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke(movie) },
        shape     = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            // Background poster
            AsyncImage(
                model              = movie.posterUrl,
                contentDescription = movie.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxSize()
                    .background(EousXColors.SurfaceRaised)
            )

            // Gradient overlay from left
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                EousXColors.Charcoal.copy(alpha = 0.9f),
                                EousXColors.Transparent
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(EousXSpacing.xl)
                    .fillMaxWidth(0.6f)
            ) {
                EousXStatusChip(status = movie.status)
                Spacer(Modifier.height(EousXSpacing.sm))
                Text(text = movie.title, style = EousXTypography.H2)
                Spacer(Modifier.height(EousXSpacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star, null,
                        tint     = EousXColors.PrimaryOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text  = "${movie.rating}",
                        style = EousXTypography.Caption.copy(color = EousXColors.SoftIvory)
                    )
                }
                if (onBookNow != null) {
                    Spacer(Modifier.height(EousXSpacing.md))
                    EousXPrimaryButton(
                        text    = "Đặt vé",
                        onClick = { onBookNow(movie) }
                    )
                }
            }
        }
    }
}

@Preview(name = "EousX Movie Cards", showBackground = true, backgroundColor = 0xFF0B0D12)
@Composable
private fun EousXMovieCardsPreview() {
    val movie = EousXMovieData(
        id = "dune-2",
        title = "Dune: Part Two",
        posterUrl = "",
        rating = 8.7f,
        genres = "Sci-Fi - Adventure",
        year = 2024,
        duration = "2h 46m"
    )

    EousXTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(EousXColors.Charcoal)
                .padding(EousXSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(EousXSpacing.xl)
        ) {
            EousXMovieCard(
                movie = movie,
                onBookNow = {},
                onFavourite = { _, _ -> }
            )
            EousXFeaturedMovieCard(movie = movie, onBookNow = {})
        }
    }
}
