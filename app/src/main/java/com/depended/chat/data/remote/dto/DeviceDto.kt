package com.depended.chat.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class FirebaseTokenUpsertRequestDto(
    val token: String,
    @SerialName("device_id") val deviceId: String? = null,
    val platform: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class FirebaseTokenDeleteRequestDto(
    val token: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeviceTokenResponseDto(
    val status: String
)
