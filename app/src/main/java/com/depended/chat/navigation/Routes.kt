package com.depended.chat.navigation

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Auth : Route("auth")
    data object Chats : Route("chats")
    data object Account : Route("account")
    data object Chat : Route("chat/{chatId}") {
        fun create(chatId: String) = "chat/$chatId"
    }
}
