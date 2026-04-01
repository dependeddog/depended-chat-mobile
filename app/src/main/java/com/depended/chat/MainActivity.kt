package com.depended.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // log / analytics / state update
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent()
        requestNotificationsPermissionIfNeeded()

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

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
