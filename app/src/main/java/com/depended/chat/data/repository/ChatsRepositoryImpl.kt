package com.depended.chat.data.repository

import com.depended.chat.data.remote.api.ChatsApi
import com.depended.chat.data.remote.dto.ChatListItemDto
import com.depended.chat.data.remote.dto.MessageDto
import com.depended.chat.data.websocket.WebSocketManager
import com.depended.chat.domain.model.*
import com.depended.chat.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject

class ChatsRepositoryImpl @Inject constructor(
    private val api: ChatsApi,
    private val wsManager: WebSocketManager,
    private val json: Json
) : ChatsRepository {
    override suspend fun getChats(): List<ChatItem> = api.getChats().map { it.toDomain() }

    override suspend fun getChatDetails(chatId: String): ChatDetails {
        val dto = api.getChat(chatId)
        return ChatDetails(dto.chatId, ChatUser(dto.companion.id, dto.companion.username), dto.unreadCount)
    }

    override suspend fun getMessages(chatId: String): List<Message> = api.getMessages(chatId).items.map { it.toDomain() }

    override suspend fun sendMessage(chatId: String, text: String): Message =
        api.sendMessage(chatId, com.depended.chat.data.remote.dto.MessageCreateRequestDto(text)).toDomain(status = MessageStatus.SENT)

    override suspend fun markRead(chatId: String) {
        api.markRead(chatId)
    }

    override fun globalEvents(): Flow<ChatItem> = wsManager.connectGlobal().mapNotNull { ev ->
        if (ev.event != "chat.list.updated") return@mapNotNull null
        val chatId = ev.data["chat_id"]?.toString()?.trim('"') ?: return@mapNotNull null
        val unread = ev.data["unread_count"]?.toString()?.toIntOrNull() ?: 0
        val lastMessage = ev.data["last_message"]?.let { json.decodeFromJsonElement<MessageDto>(it).toDomain() }
        ChatItem(chatId, ChatUser("", "Unknown"), lastMessage, unread, lastMessage?.createdAt.orEmpty())
    }

    override fun chatEvents(chatId: String): Flow<Message> = wsManager.connectChat(chatId).mapNotNull { ev ->
        when (ev.event) {
            "message.created" -> json.decodeFromJsonElement<MessageDto>(ev.data).toDomain(status = MessageStatus.DELIVERED)
            else -> null
        }
    }

    override suspend fun connectGlobal() = Unit
    override suspend fun connectChat(chatId: String) = Unit

    override suspend fun disconnectChat(chatId: String) = wsManager.disconnectChat(chatId)

    override suspend fun disconnectAllSockets() = wsManager.disconnectAll()

    private fun ChatListItemDto.toDomain() = ChatItem(
        id = id,
        companion = ChatUser(companion.id, companion.username),
        lastMessage = lastMessage?.toDomain(),
        unreadCount = unreadCount,
        updatedAt = updatedAt
    )

    private fun MessageDto.toDomain(status: MessageStatus = MessageStatus.DELIVERED) = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        text = text,
        createdAt = createdAt,
        status = status
    )
}
