package com.depended.chat.ui.components

import com.depended.chat.BuildConfig

object AvatarUrlResolver {
    fun resolve(
        avatarUrl: String?,
        userId: String?,
        hasAvatar: Boolean,
        avatarVersion: Long? = null
    ): String? {
        val candidate = when {
            !avatarUrl.isNullOrBlank() -> avatarUrl
            hasAvatar && !userId.isNullOrBlank() -> "/users/$userId/avatar"
            else -> null
        } ?: return null

        val absolute = if (
            candidate.startsWith("http://") || candidate.startsWith("https://")
        ) {
            candidate
        } else {
            val base = BuildConfig.BASE_URL.trimEnd('/')
            val path = candidate.trimStart('/')
            "$base/$path"
        }

        return if (avatarVersion != null) {
            val separator = if ('?' in absolute) '&' else '?'
            "$absolute${separator}v=$avatarVersion"
        } else {
            absolute
        }
    }
}
