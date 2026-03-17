package com.depended.chat.domain.repository

import com.depended.chat.domain.model.ChatDetails
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String)
    suspend fun register(username: String, password: String)
    suspend fun logout()
    suspend fun refreshIfNeeded(): Boolean
}

interface ChatsRepository {
    suspend fun getChats(): List<ChatItem>
    suspend fun getChatDetails(chatId: String): ChatDetails
    suspend fun getMessages(chatId: String): List<Message>
    suspend fun sendMessage(chatId: String, text: String): Message
    suspend fun markRead(chatId: String)
    fun globalEvents(): Flow<ChatItem>
    fun chatEvents(chatId: String): Flow<Message>
    suspend fun connectGlobal()
    suspend fun connectChat(chatId: String)
    suspend fun disconnectChat(chatId: String)
    suspend fun disconnectAllSockets()
}
