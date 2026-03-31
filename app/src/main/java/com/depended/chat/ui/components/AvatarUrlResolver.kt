package com.depended.chat.ui.components

import com.depended.chat.BuildConfig

object AvatarUrlResolver {
    fun resolve(avatarUrl: String?, userId: String?, hasAvatar: Boolean): String? {
        val candidate = when {
            !avatarUrl.isNullOrBlank() -> avatarUrl
            !userId.isNullOrBlank() && hasAvatar -> "/users/$userId/avatar"
            !userId.isNullOrBlank() -> "/users/$userId/avatar"
            else -> null
        } ?: return null

        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            return candidate
        }

        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = candidate.trimStart('/')
        return "$base/$path"
    }
}
