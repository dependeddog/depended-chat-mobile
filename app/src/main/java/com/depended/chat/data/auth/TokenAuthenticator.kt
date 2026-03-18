package com.depended.chat.data.auth

import com.depended.chat.data.remote.api.AuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val session = runBlocking { sessionManager.getSession() }
        if (session.refreshToken.isBlank()) return null

        val refreshed = runBlocking {
            runCatching { authApi.refresh(mapOf("refresh_token" to session.refreshToken)) }.getOrNull()
        } ?: run {
            runBlocking { sessionManager.clearSession() }
            return null
        }

        runBlocking {
            sessionManager.saveSession(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken,
                expiresInSec = refreshed.accessExpiresIn
            )
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${refreshed.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
