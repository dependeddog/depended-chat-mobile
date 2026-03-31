package com.depended.chat.data.repository

import com.depended.chat.data.auth.SessionManager
import com.depended.chat.data.remote.api.AuthApi
import com.depended.chat.data.remote.dto.AuthRequestDto
import com.depended.chat.domain.model.CurrentUser
import com.depended.chat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    private val currentUserFlow = MutableStateFlow<CurrentUser?>(null)

    override suspend fun login(username: String, password: String) {
        val response = api.login(AuthRequestDto(username, password))
        sessionManager.saveSession(response.accessToken, response.refreshToken, response.accessExpiresIn)
        fetchAndCacheCurrentUser()
    }

    override suspend fun register(username: String, password: String) {
        api.register(AuthRequestDto(username, password))

        runCatching {
            login(username, password)
        }.getOrElse { cause ->
            throw IllegalStateException("Registration succeeded, but automatic login failed", cause)
        }
    }

    override suspend fun logout() {
        runCatching { api.logout() }
        currentUserFlow.value = null
        sessionManager.clearSession()
    }

    override suspend fun refreshIfNeeded(): Boolean {
        val session = sessionManager.sessionFlow.first()
        if (!session.isAuthorized) return false
        if (session.accessExpiresAt > System.currentTimeMillis() + 15_000) return true

        return runCatching {
            val refreshed = api.refresh(mapOf("refresh_token" to session.refreshToken))
            sessionManager.saveSession(refreshed.accessToken, refreshed.refreshToken, refreshed.accessExpiresIn)
            true
        }.getOrElse {
            currentUserFlow.value = null
            sessionManager.clearSession()
            false
        }
    }

    override suspend fun getCurrentUser(forceRefresh: Boolean): CurrentUser {
        val cached = currentUserFlow.value
        if (!forceRefresh && cached != null) return cached
        return fetchAndCacheCurrentUser()
    }

    override fun observeCurrentUser(): Flow<CurrentUser?> = currentUserFlow

    private suspend fun fetchAndCacheCurrentUser(): CurrentUser {
        val token = sessionManager.getSession().accessToken
        check(token.isNotBlank()) { "Not authorized" }
        val dto = api.whoami("Bearer $token")
        return CurrentUser(
            id = dto.id,
            username = dto.username,
            bio = dto.bio,
            avatarUrl = dto.avatarUrl,
            hasAvatar = dto.hasAvatar,
            lastSeenAt = dto.lastSeenAt
        ).also { currentUserFlow.value = it }
    }
}
