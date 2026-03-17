package com.depended.chat.data.remote.api

import com.depended.chat.data.remote.dto.AuthRequestDto
import com.depended.chat.data.remote.dto.AuthResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: AuthRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequestDto): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): AuthResponseDto

    @POST("auth/logout")
    suspend fun logout(): Unit
}
