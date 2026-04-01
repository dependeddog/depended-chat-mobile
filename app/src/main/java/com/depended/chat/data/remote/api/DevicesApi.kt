package com.depended.chat.data.remote.api

import com.depended.chat.data.remote.dto.DeviceTokenResponseDto
import com.depended.chat.data.remote.dto.FirebaseTokenDeleteRequestDto
import com.depended.chat.data.remote.dto.FirebaseTokenUpsertRequestDto
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

interface DevicesApi {
    @POST("devices/firebase-token")
    suspend fun upsertFirebaseToken(@Body body: FirebaseTokenUpsertRequestDto): DeviceTokenResponseDto

    @HTTP(method = "DELETE", path = "devices/firebase-token", hasBody = true)
    suspend fun deleteFirebaseToken(@Body body: FirebaseTokenDeleteRequestDto): DeviceTokenResponseDto
}
