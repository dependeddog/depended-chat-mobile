package com.depended.chat.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenSyncCoordinator: PushTokenSyncCoordinator

    @Inject
    lateinit var notifier: ChatPushNotifier

    @Inject
    lateinit var navigationState: NotificationNavigationState

    override fun onCreate() {
        super.onCreate()
        notifier.ensureChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "[onNewToken] tokenUpdated")
        pushTokenSyncCoordinator.syncFromNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"].orEmpty()
        val chatId = message.data["chat_id"].orEmpty()

        val title = message.notification?.title.orEmpty()
        val body = message.notification?.body.orEmpty()

        if (chatId.isBlank()) {
            Log.w(TAG, "[onMessageReceived] Missing chat_id in payload")
            return
        }

        if (type != "new_message") {
            Log.d(TAG, "[onMessageReceived] Unsupported type=$type")
            return
        }

        val currentChatId = navigationState.currentChatId.value
        if (currentChatId == chatId) {
            Log.d(TAG, "[onMessageReceived] Skip notification, user already in chat=$chatId")
            return
        }

        notifier.showMessageNotification(
            chatId = chatId,
            title = title,
            body = body
        )
    }

    private companion object {
        const val TAG = "ChatFCMService"
    }
}
