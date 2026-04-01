package com.depended.chat

import android.app.Application
import com.depended.chat.notifications.ChatPushNotifier
import com.depended.chat.notifications.PushTokenSyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChatApplication : Application() {

    @Inject
    lateinit var pushTokenSyncCoordinator: PushTokenSyncCoordinator

    @Inject
    lateinit var chatPushNotifier: ChatPushNotifier

    override fun onCreate() {
        super.onCreate()
        chatPushNotifier.ensureChannel()
        pushTokenSyncCoordinator.start()
    }
}
