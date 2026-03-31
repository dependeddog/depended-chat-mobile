package com.depended.chat.data.repository

import android.util.Log
import com.depended.chat.data.remote.api.ChatsApi
import com.depended.chat.data.remote.dto.ChatDeletedEventDto
import com.depended.chat.data.remote.dto.ChatListItemDto
import com.depended.chat.data.remote.dto.ChatReadEventDto
import com.depended.chat.data.remote.dto.CreateDirectChatRequestDto
import com.depended.chat.data.remote.dto.MessageCreateRequestDto
import com.depended.chat.data.remote.dto.MessageDeletedEventDto
import com.depended.chat.data.remote.dto.MessageDto
import com.depended.chat.data.remote.dto.MessageUpdateRequestDto
import com.depended.chat.data.remote.dto.WsMessageCreatedDto
import com.depended.chat.data.websocket.WebSocketManager
import com.depended.chat.domain.model.ChatDetails
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.model.ChatUser
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatListEvent
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.MessageEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import kotlinx.serialization.json.Json

class ChatsRepositoryImpl @Inject constructor(
    private val api: ChatsApi,
    private val wsManager: WebSocketManager,
    private val json: Json,
    private val authRepository: AuthRepository
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

    override suspend fun updateMessage(chatId: String, messageId: String, text: String): Message =
        api.updateMessage(chatId, messageId, MessageUpdateRequestDto(text)).toDomain()

    override suspend fun deleteMessage(chatId: String, messageId: String) {
        api.deleteMessage(chatId, messageId)
    }

    override suspend fun deleteChat(chatId: String) {
        api.deleteChat(chatId)
    }

    override suspend fun markRead(chatId: String) {
        api.markRead(chatId)
    }

    override fun globalEvents(currentUserId: String): Flow<ChatListEvent> =
        wsManager.connectGlobal().mapNotNull { ev ->
            runCatching {
                when (ev.event) {
                    "chat.list.updated" -> {
                        val dto = json.decodeFromJsonElement<ChatListItemDto>(ev.data)
                        ChatListEvent.Upsert(dto.toDomain())
                    }

                    "chat.deleted" -> {
                        val dto = json.decodeFromJsonElement<ChatDeletedEventDto>(ev.data)
                        ChatListEvent.Deleted(dto.chatId)
                    }

                    else -> null
                }
            }.onFailure {
                Log.e("WS_GLOBAL_REPO", "[parseError] event=${ev.event}", it)
            }.getOrNull()
        }

    override fun chatEvents(chatId: String, currentUserId: String): Flow<MessageEvent> = wsManager.connectChat(chatId).mapNotNull { ev ->
        Log.d("WS_CHAT_REPO", "[chatEvents] chatId=$chatId event=${ev.event} data=${ev.data}")

        runCatching {
            when (ev.event) {
                "message.created" -> {
                    val payload = ev.data["message"] ?: ev.data
                    MessageEvent.Created(payload.toMessage(currentUserId))
                }

                "message.updated" -> {
                    val payload = ev.data["message"] ?: return@runCatching null
                    MessageEvent.Updated(payload.toMessage(currentUserId))
                }

                "message.deleted" -> {
                    val dto = json.decodeFromJsonElement<MessageDeletedEventDto>(ev.data)
                    MessageEvent.Deleted(dto.messageId)
                }

                "chat.deleted" -> {
                    val dto = json.decodeFromJsonElement<ChatDeletedEventDto>(ev.data)
                    MessageEvent.ChatDeleted(dto.chatId)
                }

                "chat.read" -> {
                    val payload = ev.data["read"] ?: ev.data
                    val dto = json.decodeFromJsonElement<ChatReadEventDto>(payload)
                    MessageEvent.ReadUpTo(dto.readUpToMessageId)
                }

                else -> null
            }
        }.onFailure {
            Log.e("WS_CHAT_REPO", "[parseError] chatId=$chatId event=${ev.event}", it)
        }.getOrNull()
    }

    override suspend fun connectGlobal() = Unit
    override suspend fun connectChat(chatId: String) = Unit

    override suspend fun disconnectChat(chatId: String) = wsManager.disconnectChat(chatId)

    override suspend fun disconnectAllSockets() = wsManager.disconnectAll()

    override suspend fun getCurrentUserId(): String {
        return authRepository.getCurrentUser(forceRefresh = false).id
    }

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
        status = if (isOwn && readByCompanion) MessageStatus.READ else MessageStatus.SENT,
        isEdited = isEdited,
        editedAt = editedAt
    )

    private fun kotlinx.serialization.json.JsonElement.toMessage(currentUserId: String): Message {
        return runCatching {
            json.decodeFromJsonElement<MessageDto>(this).toDomain()
        }.getOrElse {
            json.decodeFromJsonElement<WsMessageCreatedDto>(this).toDomain(currentUserId)
        }
    }

    private fun WsMessageCreatedDto.toDomain(currentUserId: String) = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        text = text,
        createdAt = createdAt,
        isMine = senderId == currentUserId,
        status = MessageStatus.SENT,
        isEdited = false,
        editedAt = null
    )
}
