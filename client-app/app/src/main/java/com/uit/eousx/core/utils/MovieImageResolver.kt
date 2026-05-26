package com.uit.eousx.core.utils

object MovieImageResolver {
    fun normalizeImageUrl(raw: String?): String? {
        val trimmed = raw?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            null
        }
    }

    fun heroImageUrl(backdropUrl: String?, posterUrl: String?): String? {
        return normalizeImageUrl(backdropUrl) ?: normalizeImageUrl(posterUrl)
    }

    fun posterImageUrl(posterUrl: String?, backdropUrl: String?): String? {
        return normalizeImageUrl(posterUrl) ?: normalizeImageUrl(backdropUrl)
    }
}
