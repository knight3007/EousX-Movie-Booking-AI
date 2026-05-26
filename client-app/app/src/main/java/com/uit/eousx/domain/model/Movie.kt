package com.uit.eousx.domain.model

data class Movie(
    val id: String,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseDate: String = "",
    val voteAverage: Double,
    val runtime: Int?,
    val genres: List<String>,
    val ageRating: String?,
    val trailerKey: String?,
    val originalTitle: String? = null
)
