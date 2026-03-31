package com.depended.chat.data.remote.api

import com.depended.chat.data.remote.dto.LastSeenTouchRequestDto
import com.depended.chat.data.remote.dto.UpdateBioRequestDto
import com.depended.chat.data.remote.dto.UserProfileDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileApi {
    @GET("users/me/profile")
    suspend fun getMyProfile(): UserProfileDto

    @PATCH("users/me/profile")
    suspend fun updateMyProfile(@Body body: UpdateBioRequestDto): UserProfileDto

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadMyAvatar(@Part avatar: MultipartBody.Part): UserProfileDto

    @DELETE("users/me/avatar")
    suspend fun deleteMyAvatar(): UserProfileDto

    @GET("users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfileDto

    @POST("users/me/last-seen")
    suspend fun touchLastSeen(@Body body: LastSeenTouchRequestDto = LastSeenTouchRequestDto())
}
