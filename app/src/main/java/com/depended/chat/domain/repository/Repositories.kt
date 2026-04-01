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
    fun updateCurrentUserFromProfile(profile: UserProfile)
}

interface ProfileRepository {
    suspend fun getMyProfile(forceRefresh: Boolean = false): UserProfile
    fun observeMyProfile(): Flow<UserProfile?>
    suspend fun getUserProfile(userId: String): UserProfile
    suspend fun getUserLastSeen(userId: String): String?
    suspend fun updateBio(bio: String?): UserProfile
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): UserProfile
    suspend fun deleteAvatar(): UserProfile
}

interface ChatsRepository {
    suspend fun getChats(): List<ChatItem>
    suspend fun createDirectChat(username: String): String
    suspend fun getChatDetails(chatId: String): ChatDetails
    suspend fun getMessages(chatId: String): List<Message>
    suspend fun sendMessage(chatId: String, text: String): Message
    suspend fun updateMessage(chatId: String, messageId: String, text: String): Message
    suspend fun deleteMessage(chatId: String, messageId: String)
    suspend fun deleteChat(chatId: String)
    suspend fun markRead(chatId: String)
    fun globalEvents(currentUserId: String): Flow<ChatListEvent>
    fun chatEvents(chatId: String, currentUserId: String): Flow<MessageEvent>
    suspend fun connectGlobal()
    suspend fun connectChat(chatId: String)
    suspend fun disconnectChat(chatId: String)
    suspend fun disconnectAllSockets()

    suspend fun getCurrentUserId(): String
}

interface PushTokenRepository {
    suspend fun syncToken(token: String): Boolean
    suspend fun deleteToken(token: String): Boolean
    suspend fun getLastSyncedToken(): String?
    suspend fun clearLastSyncedToken()
}

sealed class ChatListEvent {
    data class Upsert(val item: ChatItem) : ChatListEvent()
    data class Deleted(val chatId: String) : ChatListEvent()
}

sealed class MessageEvent {
    data class Created(val message: Message) : MessageEvent()
    data class Updated(val message: Message) : MessageEvent()
    data class Deleted(val messageId: String) : MessageEvent()
    data class ChatDeleted(val chatId: String) : MessageEvent()
    data class ReadUpTo(val readUpToMessageId: String?) : MessageEvent()
}
