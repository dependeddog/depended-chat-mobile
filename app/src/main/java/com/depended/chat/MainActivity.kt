package com.depended.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.depended.chat.navigation.AppNavHost
import com.depended.chat.ui.theme.DependedChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DependedChatTheme {
                AppNavHost()
            }
        }
    }
}
