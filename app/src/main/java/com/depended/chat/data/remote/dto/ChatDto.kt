package com.depended.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanionDto(val id: String, val username: String)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ChatListItemDto(
    val id: String,
    val type: String,
    val companion: CompanionDto,
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ChatDetailsDto(
    @SerialName("chat_id") val chatId: String,
    val type: String,
    val companion: CompanionDto,
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int
)

@Serializable
data class ChatMessagesResponseDto(val items: List<MessageDto>, val limit: Int, val offset: Int)

@Serializable
data class MessageCreateRequestDto(val text: String)

@Serializable
data class CreateDirectChatRequestDto(@SerialName("user_id") val userId: String)

@Serializable
data class MarkReadResponseDto(val status: String)

@Serializable
data class DirectChatResponseDto(
    @SerialName("chat_id") val chatId: String,
    val type: String,
    val companion: CompanionDto
)
