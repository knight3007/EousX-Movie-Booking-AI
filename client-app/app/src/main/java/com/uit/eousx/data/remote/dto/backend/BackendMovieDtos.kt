package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackendMovieDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "originalTitle") val originalTitle: String? = null,
    @Json(name = "overview") val overview: String = "",
    @Json(name = "posterUrl") val posterUrl: String? = null,
    @Json(name = "backdropUrl") val backdropUrl: String? = null,
    @Json(name = "trailerKey") val trailerKey: String? = null,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "rating") val rating: Double = 0.0,
    @Json(name = "ageRating") val ageRating: String = "",
    @Json(name = "status") val status: String = ""
)
