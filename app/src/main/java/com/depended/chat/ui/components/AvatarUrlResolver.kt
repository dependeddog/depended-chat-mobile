package com.depended.chat.ui.components

import com.depended.chat.BuildConfig

object AvatarUrlResolver {
    fun resolve(avatarUrl: String?, userId: String?, hasAvatar: Boolean, avatarVersion: Long? = null): String? {
        val candidate = when {
            !avatarUrl.isNullOrBlank() -> avatarUrl
            !userId.isNullOrBlank() && hasAvatar -> "/users/$userId/avatar"
            !userId.isNullOrBlank() -> "/users/$userId/avatar"
            else -> null
        } ?: return null

        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            return withVersion(candidate, avatarVersion)
        }

        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = candidate.trimStart('/')
        val normalized = "$base/$path"
        return withVersion(normalized, avatarVersion)
    }

    private fun withVersion(url: String, avatarVersion: Long?): String {
        if (avatarVersion == null) return url
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}v=$avatarVersion"
    }
}
