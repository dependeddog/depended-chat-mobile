package com.depended.chat.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AuthRequestDto(val username: String, val password: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UserReadDto(
    val id: String,
    val username: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("access_expires_in") val accessExpiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String
)
