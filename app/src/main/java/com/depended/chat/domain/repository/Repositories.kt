package com.depended.chat.domain.repository

import com.depended.chat.domain.model.ChatDetails
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.model.CurrentUser
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String)
    suspend fun register(username: String, password: String)
    suspend fun logout()
    suspend fun refreshIfNeeded(): Boolean
    suspend fun getCurrentUser(forceRefresh: Boolean = false): CurrentUser
    fun observeCurrentUser(): Flow<CurrentUser?>
}

interface ProfileRepository {
    suspend fun getMyProfile(forceRefresh: Boolean = false): UserProfile
    suspend fun getUserProfile(userId: String): UserProfile
    suspend fun updateBio(bio: String?): UserProfile
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): UserProfile
    suspend fun deleteAvatar(): UserProfile
    suspend fun touchLastSeen()
}

interface ChatsRepository {
    suspend fun getChats(): List<ChatItem>
    suspend fun createDirectChat(username: String): String
    suspend fun getChatDetails(chatId: String): ChatDetails
    suspend fun getMessages(chatId: String): List<Message>
    suspend fun sendMessage(chatId: String, text: String): Message
    suspend fun markRead(chatId: String)
    fun globalEvents(currentUserId: String): Flow<ChatItem>
    fun chatEvents(chatId: String, currentUserId: String): Flow<MessageEvent>
    suspend fun connectGlobal()
    suspend fun connectChat(chatId: String)
    suspend fun disconnectChat(chatId: String)
    suspend fun disconnectAllSockets()

    suspend fun getCurrentUserId(): String
}

sealed class MessageEvent {
    data class Created(val message: Message) : MessageEvent()
    data class ReadUpTo(val readUpToMessageId: String?) : MessageEvent()
}
