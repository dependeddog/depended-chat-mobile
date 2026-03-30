package com.depended.chat.data.repository

import android.util.Log
import com.depended.chat.data.remote.api.ChatsApi
import com.depended.chat.data.remote.dto.ChatListItemDto
import com.depended.chat.data.remote.dto.ChatReadEventDto
import com.depended.chat.data.remote.dto.CreateDirectChatRequestDto
import com.depended.chat.data.remote.dto.MessageCreateRequestDto
import com.depended.chat.data.remote.dto.MessageDto
import com.depended.chat.data.remote.dto.WsMessageCreatedDto
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

    override fun globalEvents(currentUserId: String): Flow<ChatItem> = wsManager.connectGlobal().mapNotNull { ev ->
        if (ev.event != "chat.list.updated") return@mapNotNull null

        val chatId = ev.data["id"]?.toString()?.trim('"') ?: return@mapNotNull null
        val unread = ev.data["unread_count"]?.toString()?.toIntOrNull() ?: 0
        val lastMessage = ev.data["last_message"]?.let {
            json.decodeFromJsonElement<WsMessageCreatedDto>(it).toDomain(currentUserId)
        }

        ChatItem(chatId, ChatUser("", "Unknown"), lastMessage, unread, lastMessage?.createdAt.orEmpty())
    }

    override fun chatEvents(chatId: String): Flow<MessageEvent> = wsManager.connectChat(chatId).mapNotNull { ev ->
        Log.d("WS_CHAT_REPO", "[chatEvents] chatId=$chatId event=${ev.event} data=${ev.data}")

        runCatching {
            when (ev.event) {
                "message.created" -> {
                    val payload = ev.data["message"] ?: ev.data
                    Log.d("WS_CHAT_REPO", "[message.created] chatId=$chatId payload=$payload")

                    val dto = json.decodeFromJsonElement<WsMessageCreatedDto>(payload)
                    Log.d(
                        "WS_CHAT_REPO",
                        "[message.created.parsed] chatId=$chatId id=${dto.id} senderId=${dto.senderId} text=${dto.text}"
                    )

                    MessageEvent.Created(dto.toDomain(currentUserId = "TODO_CURRENT_USER_ID"))
                }

                "chat.read" -> {
                    val payload = ev.data["read"] ?: ev.data
                    Log.d("WS_CHAT_REPO", "[chat.read] chatId=$chatId payload=$payload")

                    val dto = json.decodeFromJsonElement<ChatReadEventDto>(payload)
                    Log.d(
                        "WS_CHAT_REPO",
                        "[chat.read.parsed] chatId=$chatId readUpTo=${dto.readUpToMessageId}"
                    )

                    MessageEvent.ReadUpTo(dto.readUpToMessageId)
                }

                else -> {
                    Log.d("WS_CHAT_REPO", "[ignored] chatId=$chatId event=${ev.event}")
                    null
                }
            }
        }.onFailure {
            Log.e("WS_CHAT_REPO", "[parseError] chatId=$chatId event=${ev.event}", it)
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

    private fun WsMessageCreatedDto.toDomain(currentUserId: String) = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        text = text,
        createdAt = createdAt,
        isMine = senderId == currentUserId,
        status = MessageStatus.SENT
    )
}
