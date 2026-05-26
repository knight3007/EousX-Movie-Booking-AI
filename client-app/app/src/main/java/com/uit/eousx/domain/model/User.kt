package com.uit.eousx.domain.model

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val avatarUrl: String?
)
