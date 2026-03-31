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
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_base64") val avatarBase64: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateBioRequestDto(val bio: String?)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LastSeenTouchRequestDto(
    @SerialName("last_seen") val lastSeen: String? = null
)
