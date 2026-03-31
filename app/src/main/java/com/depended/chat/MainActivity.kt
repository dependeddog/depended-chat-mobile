package com.depended.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.depended.chat.notifications.ChatPushNotifier
import com.depended.chat.notifications.NotificationNavigationState
import com.depended.chat.navigation.AppNavHost
import com.depended.chat.ui.theme.DependedChatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationState: NotificationNavigationState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
        setContent {
            DependedChatTheme {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val chatId = intent?.takeIf { it.action == ChatPushNotifier.ACTION_OPEN_CHAT_FROM_PUSH }
            ?.getStringExtra(ChatPushNotifier.EXTRA_CHAT_ID)
            .orEmpty()
        if (chatId.isNotBlank()) {
            navigationState.emitOpenChat(chatId)
            intent?.action = null
        }
    }
}
