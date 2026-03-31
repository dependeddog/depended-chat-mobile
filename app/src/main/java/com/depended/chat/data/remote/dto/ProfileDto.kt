package com.depended.chat.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    val bio: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("has_avatar") val hasAvatar: Boolean = false,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_mime_type") val avatarMimeType: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AvatarUploadResponseDto(
    @SerialName("has_avatar") val hasAvatar: Boolean,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_mime_type") val avatarMimeType: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateBioRequestDto(val bio: String?)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LastSeenReadDto(
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)
