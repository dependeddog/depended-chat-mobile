package com.depended.chat.data.repository

import com.depended.chat.data.remote.api.ProfileApi
import com.depended.chat.data.remote.dto.UpdateBioRequestDto
import com.depended.chat.data.remote.dto.UserProfileDto
import com.depended.chat.domain.model.UserProfile
import com.depended.chat.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,

) : ProfileRepository {
    private var cachedMyProfile: UserProfile? = null

    private val _myProfile: MutableStateFlow<UserProfile?> = MutableStateFlow(null)
    val myProfile: StateFlow<UserProfile?> = _myProfile

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
        require(bytes.isNotEmpty()) { "Avatar bytes are empty" }

        val normalizedMimeType = mimeType
            .trim()
            .lowercase()
            .takeIf { it.startsWith("image/") && it.contains('/') }
            ?: "image/jpeg"

        val requestBody = bytes.toRequestBody(normalizedMimeType.toMediaTypeOrNull())

        val extension = when (normalizedMimeType.substringAfter('/')) {
            "jpeg", "jpg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            "gif" -> "gif"
            else -> "jpg"
        }

        val avatarPart = MultipartBody.Part.createFormData(
            name = "avatar",
            filename = "avatar.$extension",
            body = requestBody
        )

        val response = api.uploadMyAvatar(avatarPart)
        val now = System.currentTimeMillis()

        val current = cachedMyProfile

        val updatedProfile = if (current != null) {
            current.copy(
                hasAvatar = response.hasAvatar,
                avatarUrl = response.avatarUrl ?: "/users/${current.id}/avatar",
                avatarMimeType = response.avatarMimeType ?: normalizedMimeType,
                avatarVersion = now
            )
        } else {
            UserProfile(
                id = "",
                username = "",
                bio = null,
                avatarUrl = response.avatarUrl,
                hasAvatar = response.hasAvatar,
                avatarMimeType = response.avatarMimeType ?: normalizedMimeType,
                lastSeenAt = null,
                avatarVersion = now
            )
        }

        val finalProfile = if (updatedProfile.id.isBlank() || updatedProfile.username.isBlank()) {
            getMyProfile(forceRefresh = true).copy(
                avatarVersion = now
            )
        } else {
            updatedProfile
        }

        cachedMyProfile = finalProfile
        _myProfile.value = finalProfile

        return finalProfile
    }

    override suspend fun deleteAvatar(): UserProfile {
        api.deleteMyAvatar()
        return getMyProfile(forceRefresh = true)
    }

    private fun UserProfileDto.toDomain() = UserProfile(
        id = id,
        username = username,
        bio = bio,
        avatarUrl = avatarUrl,
        hasAvatar = hasAvatar,
        avatarMimeType = avatarMimeType,
        lastSeenAt = lastSeenAt,
        avatarVersion = null
    )
}
