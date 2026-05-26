package com.uit.eousx.data.remote.api

import com.uit.eousx.data.remote.dto.MovieResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    // API lấy danh sách phim phổ biến (Popular Movies)
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("language") language: String = "vi-VN", // Lấy data tiếng Việt
        @Query("page") page: Int = 1
    ): MovieResponse

    // API lấy danh sách phim đang chiếu (Now Playing)
    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(
        @Query("language") language: String = "vi-VN",
        @Query("page") page: Int = 1
    ): MovieResponse
}
