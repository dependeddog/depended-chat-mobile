package com.depended.chat.notifications

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class NotificationNavigationState @Inject constructor() {
    private val _openChatEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openChatEvents: SharedFlow<String> = _openChatEvents.asSharedFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    fun emitOpenChat(chatId: String) {
        _openChatEvents.tryEmit(chatId)
    }

    fun setCurrentChat(chatId: String?) {
        _currentChatId.value = chatId
    }
}
