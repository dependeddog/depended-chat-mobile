package com.depended.chat.data.auth

import com.depended.chat.data.local.SessionStorage
import com.depended.chat.data.local.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val storage: SessionStorage
) {
    val sessionFlow: Flow<UserSession> = storage.sessionFlow

    suspend fun getSession(): UserSession = storage.sessionFlow.first()

    suspend fun saveSession(accessToken: String, refreshToken: String, expiresInSec: Long) {
        val expiresAt = System.currentTimeMillis() + expiresInSec * 1000L
        storage.save(UserSession(accessToken, refreshToken, expiresAt))
    }

    suspend fun updateAccessToken(accessToken: String, expiresInSec: Long) {
        storage.updateAccess(accessToken, System.currentTimeMillis() + expiresInSec * 1000L)
    }

    suspend fun clearSession() = storage.clear()
}
