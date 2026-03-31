package com.depended.chat.data.repository

import com.depended.chat.data.remote.api.ProfileApi
import com.depended.chat.data.remote.dto.UpdateBioRequestDto
import com.depended.chat.data.remote.dto.UserProfileDto
import com.depended.chat.domain.model.UserProfile
import com.depended.chat.domain.repository.ProfileRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi
) : ProfileRepository {
    private var cachedMyProfile: UserProfile? = null

    override suspend fun getMyProfile(forceRefresh: Boolean): UserProfile {
        if (!forceRefresh) {
            cachedMyProfile?.let { return it }
        }
        return api.getMyProfile().toDomain().also { cachedMyProfile = it }
    }

    override suspend fun getUserProfile(userId: String): UserProfile = api.getUserProfile(userId).toDomain()

    override suspend fun getUserLastSeen(userId: String): String? = api.getUserLastSeen(userId).lastSeenAt

    override suspend fun updateBio(bio: String?): UserProfile =
        api.updateMyProfile(UpdateBioRequestDto(bio = bio)).toDomain().also { cachedMyProfile = it }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): UserProfile {
        val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val avatarPart = MultipartBody.Part.createFormData("avatar", "avatar", requestFile)
        return api.uploadMyAvatar(avatarPart).toDomain().also { cachedMyProfile = it }
    }

    override suspend fun deleteAvatar(): UserProfile = api.deleteMyAvatar().toDomain().also { cachedMyProfile = it }

    private fun UserProfileDto.toDomain() = UserProfile(
        id = id,
        username = username,
        bio = bio,
        avatarUrl = avatarUrl,
        hasAvatar = hasAvatar,
        avatarMimeType = avatarMimeType,
        lastSeenAt = lastSeenAt
    )
}
