package com.depended.chat.data.repository

import android.os.Build
import android.util.Log
import com.depended.chat.data.local.PushTokenStorage
import com.depended.chat.data.remote.api.DevicesApi
import com.depended.chat.data.remote.dto.FirebaseTokenDeleteRequestDto
import com.depended.chat.data.remote.dto.FirebaseTokenUpsertRequestDto
import com.depended.chat.domain.repository.PushTokenRepository
import javax.inject.Inject

class PushTokenRepositoryImpl @Inject constructor(
    private val devicesApi: DevicesApi,
    private val pushTokenStorage: PushTokenStorage
) : PushTokenRepository {

    override suspend fun syncToken(token: String): Boolean {
        val lastSynced = pushTokenStorage.getLastSyncedToken()
        if (lastSynced == token) {
            Log.d(TAG, "[syncToken] Skip, already synced")
            return true
        }

        devicesApi.upsertFirebaseToken(
            FirebaseTokenUpsertRequestDto(
                token = token,
                deviceId = pushTokenStorage.getOrCreateDeviceId(),
                platform = "android-${Build.VERSION.SDK_INT}"
            )
        )
        pushTokenStorage.setLastSyncedToken(token)
        Log.d(TAG, "[syncToken] Token synced")
        return true
    }

    override suspend fun deleteToken(token: String): Boolean {
        devicesApi.deleteFirebaseToken(FirebaseTokenDeleteRequestDto(token))
        val lastSynced = pushTokenStorage.getLastSyncedToken()
        if (lastSynced == token) {
            pushTokenStorage.setLastSyncedToken(null)
        }
        Log.d(TAG, "[deleteToken] Token deleted")
        return true
    }

    override suspend fun getLastSyncedToken(): String? = pushTokenStorage.getLastSyncedToken()

    override suspend fun clearLastSyncedToken() {
        pushTokenStorage.setLastSyncedToken(null)
    }

    private companion object {
        const val TAG = "PushTokenRepository"
    }
}
