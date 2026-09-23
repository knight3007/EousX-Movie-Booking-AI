package com.uit.eousx.core.network

import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

object ApiErrorHandler {

    private const val TAG = "ApiErrorHandler"

    fun parse(throwable: Throwable): NetworkResult.Error {
        return when (throwable) {
            is SocketTimeoutException -> NetworkResult.Error("Request timed out. Please try again.")
            is IOException -> NetworkResult.Error("No internet connection. Please check your network.")
            is HttpException -> parseHttpException(throwable)
            else -> {
                Log.e(TAG, "Unexpected API error: ${throwable.javaClass.simpleName}: ${throwable.message}", throwable)
                NetworkResult.Error(
                    throwable.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "Something went wrong. Please try again."
                )
            }
        }
    }

    private fun parseHttpException(exception: HttpException): NetworkResult.Error {
        val code = exception.code()
        val errorBody = exception.response()?.errorBody()?.string()
        val backendMessage = errorBody?.let(::parseBackendMessage)
        Log.e(
            TAG,
            "HTTP $code ${exception.response()?.raw()?.request?.method} " +
                "${exception.response()?.raw()?.request?.url?.encodedPath}: " +
                (backendMessage ?: errorBody.orEmpty()).take(500)
        )
        val message = when (code) {
            401 -> "Unauthorized. Please sign in again."
            404 -> backendMessage ?: "Requested resource was not found."
            in 500..599 -> backendMessage ?: "Server error. Please try again later."
            else -> backendMessage
                ?: errorBody?.takeIf { it.isNotBlank() }
                ?: "Request failed. Please try again."
        }
        return NetworkResult.Error(message = message, code = code)
    }

    private fun parseBackendMessage(errorBody: String): String? {
        return runCatching {
            val json = JSONObject(errorBody)
            when (val message = json.opt("message")) {
                is String -> message
                is JSONArray -> (0 until message.length())
                    .mapNotNull { index -> message.optString(index).takeIf { it.isNotBlank() } }
                    .joinToString(separator = "\n")
                    .takeIf { it.isNotBlank() }
                else -> json.optString("error").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }
}
