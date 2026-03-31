package com.depended.chat.data.auth

import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProvider @Inject constructor(private val sessionManager: SessionManager) {
    suspend fun accessToken(): String = sessionManager.sessionFlow.first().accessToken
    suspend fun refreshToken(): String = sessionManager.sessionFlow.first().refreshToken
}
