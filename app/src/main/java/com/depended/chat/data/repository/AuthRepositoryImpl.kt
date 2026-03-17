package com.depended.chat.data.repository

import com.depended.chat.data.auth.SessionManager
import com.depended.chat.data.remote.api.AuthApi
import com.depended.chat.data.remote.dto.AuthRequestDto
import com.depended.chat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {
    override suspend fun login(username: String, password: String) {
        val response = api.login(AuthRequestDto(username, password))
        sessionManager.saveSession(response.accessToken, response.refreshToken, response.accessExpiresIn)
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
            sessionManager.clearSession()
            false
        }
    }
}
