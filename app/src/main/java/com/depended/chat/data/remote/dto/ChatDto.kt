package com.depended.chat.data.remote.dto

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CompanionDto(
    val id: String,
    val username: String,
    val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("has_avatar") val hasAvatar: Boolean = false,
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("is_own") val isOwn: Boolean,
    @SerialName("read_by_companion") val readByCompanion: Boolean
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class WsMessageCreatedDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String
)

@SuppressLint("UnsafeOptInUsageError")
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

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatDetailsDto(
    @SerialName("chat_id") val chatId: String,
    val type: String,
    val companion: CompanionDto,
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int
)


@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ChatMessagesResponseDto(val items: List<MessageDto>, val limit: Int, val offset: Int)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MessageCreateRequestDto(val text: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MessageUpdateRequestDto(val text: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class CreateDirectChatRequestDto(val username: String)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MarkReadResponseDto(
    val status: String,
    @SerialName("read_up_to_message_id") val readUpToMessageId: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DirectChatResponseDto(
    @SerialName("chat_id") val chatId: String,
    val type: String,
    val companion: CompanionDto
)
