package com.uit.eousx.data.remote.dto.backend

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "phone") val phone: String?
)

@JsonClass(generateAdapter = true)
data class GoogleAuthRequest(
    @Json(name = "idToken") val idToken: String
)

@JsonClass(generateAdapter = true)
data class AuthUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "message") val message: String?,
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "user") val user: AuthUserDto
)
