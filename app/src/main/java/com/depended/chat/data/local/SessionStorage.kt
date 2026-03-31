package com.depended.chat.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("session")

@Singleton
class SessionStorage @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val expiresAt = longPreferencesKey("access_expires_at")
        val userId = stringPreferencesKey("user_id")
    }

    val sessionFlow: Flow<UserSession> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSession() }

    suspend fun save(session: UserSession) {
        context.dataStore.edit {
            it[Keys.accessToken] = session.accessToken
            it[Keys.refreshToken] = session.refreshToken
            it[Keys.expiresAt] = session.accessExpiresAt
            session.userId?.let { id -> it[Keys.userId] = id }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun updateAccess(accessToken: String, accessExpiresAt: Long) {
        context.dataStore.edit {
            it[Keys.accessToken] = accessToken
            it[Keys.expiresAt] = accessExpiresAt
        }
    }

    private fun Preferences.toSession() = UserSession(
        accessToken = this[Keys.accessToken].orEmpty(),
        refreshToken = this[Keys.refreshToken].orEmpty(),
        accessExpiresAt = this[Keys.expiresAt] ?: 0,
        userId = this[Keys.userId]
    )
}

data class UserSession(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresAt: Long,
    val userId: String? = null
) {
    val isAuthorized: Boolean get() = accessToken.isNotBlank() && refreshToken.isNotBlank()
}
