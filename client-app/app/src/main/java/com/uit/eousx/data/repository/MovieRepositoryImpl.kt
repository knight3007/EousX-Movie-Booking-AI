package com.uit.eousx.data.repository

import android.util.Log
import com.uit.eousx.core.network.ApiErrorHandler
import com.uit.eousx.core.utils.MovieImageResolver
import com.uit.eousx.data.remote.api.BackendApi
import com.uit.eousx.data.remote.dto.backend.BackendMovieDto
import com.uit.eousx.domain.model.Movie
import com.uit.eousx.domain.repository.MovieRepository
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val api: BackendApi
) : MovieRepository {

    override suspend fun getPopularMovies(page: Int): Result<List<Movie>> {
        return try {
            Result.success(api.getMovies().map { it.toDomain() })
        } catch (throwable: Throwable) {
            Result.failure(Exception(ApiErrorHandler.parse(throwable).message, throwable))
        }
    }

    override suspend fun getNowPlayingMovies(page: Int): Result<List<Movie>> {
        return getPopularMovies(page)
    }

    override suspend fun getMovieById(id: String): Result<Movie> {
        return try {
            Result.success(api.getMovieById(id).toDomain())
        } catch (throwable: Throwable) {
            Result.failure(Exception(ApiErrorHandler.parse(throwable).message, throwable))
        }
    }
}

private fun BackendMovieDto.toDomain(): Movie {
    val normalizedPosterUrl = MovieImageResolver.normalizeImageUrl(posterUrl)
    val normalizedBackdropUrl = MovieImageResolver.normalizeImageUrl(backdropUrl)

    if (normalizedPosterUrl == null || normalizedBackdropUrl == null) {
        Log.d("MovieImage", "Movie $title: poster=$normalizedPosterUrl backdrop=$normalizedBackdropUrl")
    }

    return Movie(
        id = id,
        title = title,
        overview = overview,
        posterUrl = normalizedPosterUrl,
        backdropUrl = normalizedBackdropUrl,
        voteAverage = rating,
        runtime = runtime,
        genres = genres,
        ageRating = ageRating.takeIf { it.isNotBlank() },
        trailerKey = trailerKey,
        originalTitle = originalTitle?.takeIf { it.isNotBlank() }
    )
}
