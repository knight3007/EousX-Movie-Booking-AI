package com.uit.eousx.domain.repository

import com.uit.eousx.domain.model.Movie

interface MovieRepository {
    suspend fun getPopularMovies(page: Int): Result<List<Movie>>
    suspend fun getNowPlayingMovies(page: Int): Result<List<Movie>>
    suspend fun getMovieById(id: String): Result<Movie>
}
