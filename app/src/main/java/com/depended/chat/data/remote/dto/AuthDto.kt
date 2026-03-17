package com.depended.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestDto(val username: String, val password: String)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("access_expires_in") val accessExpiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String
)
