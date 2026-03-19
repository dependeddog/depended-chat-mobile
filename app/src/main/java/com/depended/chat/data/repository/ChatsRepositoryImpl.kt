package com.depended.chat.data.repository

import com.depended.chat.data.remote.api.ChatsApi
import com.depended.chat.data.remote.dto.ChatListItemDto
import com.depended.chat.data.remote.dto.ChatReadEventDto
import com.depended.chat.data.remote.dto.CreateDirectChatRequestDto
import com.depended.chat.data.remote.dto.MessageCreateRequestDto
import com.depended.chat.data.remote.dto.MessageDto
import com.depended.chat.data.websocket.WebSocketManager
import com.depended.chat.domain.model.ChatDetails
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.model.ChatUser
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.MessageEvent
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

    override suspend fun createDirectChat(username: String): String {
        val response = api.createDirect(CreateDirectChatRequestDto(username))
        return response.chatId
    }

    override suspend fun getChatDetails(chatId: String): ChatDetails {
        val dto = api.getChat(chatId)
        return ChatDetails(dto.chatId, ChatUser(dto.companion.id, dto.companion.username), dto.unreadCount)
    }

    override suspend fun getMessages(chatId: String): List<Message> = api.getMessages(chatId).items.map { it.toDomain() }

    override suspend fun sendMessage(chatId: String, text: String): Message =
        api.sendMessage(chatId, MessageCreateRequestDto(text)).toDomain()

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

    override fun chatEvents(chatId: String): Flow<MessageEvent> = wsManager.connectChat(chatId).mapNotNull { ev ->
        runCatching {
            when (ev.event) {
                "message.created" -> {
                    val payload = ev.data["message"] ?: ev.data
                    MessageEvent.Created(json.decodeFromJsonElement<MessageDto>(payload).toDomain())
                }

                "chat.read" -> {
                    val payload = ev.data["read"] ?: ev.data
                    MessageEvent.ReadUpTo(json.decodeFromJsonElement<ChatReadEventDto>(payload).readUpToMessageId)
                }

                else -> null
            }
        }.getOrNull()
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

    private fun MessageDto.toDomain() = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        text = text,
        createdAt = createdAt,
        isMine = isOwn,
        status = if (isOwn && readByCompanion) MessageStatus.READ else MessageStatus.SENT
    )
}
