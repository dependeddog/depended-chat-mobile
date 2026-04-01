package com.depended.chat.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pushDataStore by preferencesDataStore("push")

@Singleton
class PushTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val deviceId = stringPreferencesKey("device_id")
        val lastSyncedToken = stringPreferencesKey("last_synced_fcm_token")
    }

    val lastSyncedTokenFlow: Flow<String?> = context.pushDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.lastSyncedToken] }

    suspend fun getOrCreateDeviceId(): String {
        val current = context.pushDataStore.data.first()[Keys.deviceId]
        if (!current.isNullOrBlank()) return current

        val generated = UUID.randomUUID().toString()
        context.pushDataStore.edit { prefs ->
            prefs[Keys.deviceId] = generated
        }
        return generated
    }

    suspend fun getLastSyncedToken(): String? = context.pushDataStore.data.first()[Keys.lastSyncedToken]

    suspend fun setLastSyncedToken(token: String?) {
        context.pushDataStore.edit { prefs ->
            if (token.isNullOrBlank()) {
                prefs.remove(Keys.lastSyncedToken)
            } else {
                prefs[Keys.lastSyncedToken] = token
            }
        }
    }
}
