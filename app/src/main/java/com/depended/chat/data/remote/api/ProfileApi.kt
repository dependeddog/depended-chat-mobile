package com.depended.chat.data.remote.api

import com.depended.chat.data.remote.dto.LastSeenReadDto
import com.depended.chat.data.remote.dto.UpdateBioRequestDto
import com.depended.chat.data.remote.dto.UserProfileDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileApi {
    @GET("users/me/profile")
    suspend fun getMyProfile(): UserProfileDto

    @PATCH("users/me/profile")
    suspend fun updateMyProfile(@Body body: UpdateBioRequestDto): UserProfileDto

    @Multipart
    @PUT("users/me/avatar")
    suspend fun uploadMyAvatar(@Part avatar: MultipartBody.Part): UserProfileDto

    @DELETE("users/me/avatar")
    suspend fun deleteMyAvatar(): UserProfileDto

    @GET("users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfileDto

    @GET("users/{userId}/last-seen")
    suspend fun getUserLastSeen(@Path("userId") userId: String): LastSeenReadDto

    @GET("users/{userId}/avatar")
    suspend fun getUserAvatar(@Path("userId") userId: String): ResponseBody
}
