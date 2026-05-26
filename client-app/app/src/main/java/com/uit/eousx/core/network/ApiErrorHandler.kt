package com.uit.eousx.core.network

import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

object ApiErrorHandler {

    fun parse(throwable: Throwable): NetworkResult.Error {
        return when (throwable) {
            is SocketTimeoutException -> NetworkResult.Error("Request timed out. Please try again.")
            is IOException -> NetworkResult.Error("No internet connection. Please check your network.")
            is HttpException -> parseHttpException(throwable)
            else -> NetworkResult.Error("Something went wrong. Please try again.")
        }
    }

    private fun parseHttpException(exception: HttpException): NetworkResult.Error {
        val code = exception.code()
        val message = when (code) {
            401 -> "Unauthorized. Please sign in again."
            404 -> "Requested resource was not found."
            in 500..599 -> "Server error. Please try again later."
            else -> exception.response()?.errorBody()?.string()
                ?.takeIf { it.isNotBlank() }
                ?: "Request failed. Please try again."
        }
        return NetworkResult.Error(message = message, code = code)
    }
}
