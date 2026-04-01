package com.depended.chat.notifications

import android.util.Log
import com.depended.chat.data.auth.SessionManager
import com.depended.chat.domain.repository.PushTokenRepository
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class PushTokenSyncCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val pushTokenRepository: PushTokenRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            sessionManager.sessionFlow
                .map { it.isAuthorized }
                .distinctUntilChanged()
                .collect { isAuthorized ->
                    if (isAuthorized) {
                        syncCurrentFirebaseToken("session_authorized")
                    } else {
                        pushTokenRepository.clearLastSyncedToken()
                    }
                }
        }
    }

    fun syncFromNewToken(token: String) {
        scope.launch {
            val authorized = sessionManager.getSession().isAuthorized
            if (!authorized) {
                Log.d(TAG, "[syncFromNewToken] Skip, unauthorized")
                return@launch
            }

            runCatching {
                pushTokenRepository.syncToken(token)
            }.onFailure {
                Log.e(TAG, "[syncFromNewToken] Failed", it)
            }
        }
    }

    suspend fun deleteOnLogout() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .getOrElse {
                Log.w(TAG, "[deleteOnLogout] Failed to fetch current firebase token", it)
                pushTokenRepository.getLastSyncedToken().orEmpty()
            }
            .orEmpty()

        if (token.isBlank()) return

        runCatching {
            pushTokenRepository.deleteToken(token)
        }.onFailure {
            Log.e(TAG, "[deleteOnLogout] Failed", it)
        }
    }

    private suspend fun syncCurrentFirebaseToken(source: String) {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                pushTokenRepository.syncToken(token)
                Log.d(TAG, "[syncCurrentFirebaseToken] Synced from $source")
            }
        }.onFailure {
            Log.e(TAG, "[syncCurrentFirebaseToken] Failed from $source", it)
        }
    }

    private companion object {
        const val TAG = "PushTokenSync"
    }
}
