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
        val messageId = message.data["message_id"]
        val senderId = message.data["sender_id"]

        if (chatId.isBlank()) {
            Log.w(TAG, "[onMessageReceived] Missing chat_id in payload")
            return
        }

        val currentChatId = navigationState.currentChatId.value
        if (currentChatId == chatId) {
            Log.d(TAG, "[onMessageReceived] Skip notification, user already in chat=$chatId")
            return
        }

        notifier.showMessageNotification(
            chatId = chatId,
            type = type,
            messageId = messageId,
            senderId = senderId
        )
    }

    private companion object {
        const val TAG = "ChatFCMService"
    }
}
