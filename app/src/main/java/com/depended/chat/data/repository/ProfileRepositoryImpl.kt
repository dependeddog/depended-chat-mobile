package com.depended.chat.data.repository

import com.depended.chat.data.remote.api.ProfileApi
import com.depended.chat.data.remote.dto.UpdateBioRequestDto
import com.depended.chat.data.remote.dto.UserProfileDto
import com.depended.chat.domain.model.UserProfile
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val authRepository: AuthRepository
) : ProfileRepository {
    private var cachedMyProfile: UserProfile? = null
    private val myProfileFlow = MutableStateFlow<UserProfile?>(null)

    override suspend fun getMyProfile(forceRefresh: Boolean): UserProfile {
        if (!forceRefresh) {
            cachedMyProfile?.let { return it }
        }
        val profile = api.getMyProfile().toDomain(
            avatarVersion = cachedMyProfile?.avatarVersion
        )
        publishMyProfile(profile)
        return profile
    }

    override fun observeMyProfile(): Flow<UserProfile?> = myProfileFlow

    override suspend fun getUserProfile(userId: String): UserProfile = api.getUserProfile(userId).toDomain()

    override suspend fun getUserLastSeen(userId: String): String? = api.getUserLastSeen(userId).lastSeenAt

    override suspend fun updateBio(bio: String?): UserProfile =
        api.updateMyProfile(UpdateBioRequestDto(bio = bio)).toDomain(
            avatarVersion = cachedMyProfile?.avatarVersion
        ).also { publishMyProfile(it) }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): UserProfile {
        val safeMimeType = mimeType.takeIf { it.startsWith("image/") && it.contains('/') } ?: "image/jpeg"
        val requestFile = bytes.toRequestBody(safeMimeType.toMediaTypeOrNull())
        val extension = when (safeMimeType.substringAfter('/')) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            else -> "jpg"
        }
        val avatarPart = MultipartBody.Part.createFormData("avatar", "avatar.$extension", requestFile)
        api.uploadMyAvatar(avatarPart)
        val refreshed = getMyProfile(forceRefresh = true).copy(avatarVersion = System.currentTimeMillis())
        publishMyProfile(refreshed)
        return refreshed
    }

    override suspend fun deleteAvatar(): UserProfile {
        api.deleteMyAvatar()
        val avatarVersion = System.currentTimeMillis()
        val refreshed = getMyProfile(forceRefresh = true).copy(
            hasAvatar = false,
            avatarUrl = null,
            avatarMimeType = null,
            avatarVersion = avatarVersion
        )
        publishMyProfile(refreshed)
        return refreshed
    }

    private fun publishMyProfile(profile: UserProfile) {
        cachedMyProfile = profile
        myProfileFlow.value = profile
        authRepository.updateCurrentUserFromProfile(profile)
    }

    private fun UserProfileDto.toDomain(avatarVersion: Long? = null) = UserProfile(
        id = id,
        username = username,
        bio = bio,
        avatarUrl = avatarUrl,
        hasAvatar = hasAvatar,
        avatarMimeType = avatarMimeType,
        lastSeenAt = lastSeenAt,
        avatarVersion = avatarVersion
    )
}
