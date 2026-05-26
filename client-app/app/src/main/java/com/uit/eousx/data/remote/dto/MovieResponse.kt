package com.uit.eousx.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MovieResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "results") val results: List<MovieDto>,
    @Json(name = "total_pages") val totalPages: Int,
    @Json(name = "total_results") val totalResults: Int
)

@JsonClass(generateAdapter = true)
data class MovieDto(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "overview") val overview: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "release_date") val releaseDate: String?,
    @Json(name = "vote_average") val voteAverage: Double
)
