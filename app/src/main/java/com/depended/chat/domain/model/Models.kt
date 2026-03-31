package com.depended.chat.domain.model

data class ChatUser(
    val id: String,
    val username: String,
    val avatarUrl: String? = null,
    val hasAvatar: Boolean = false,
    val bio: String? = null,
    val lastSeenAt: String? = null
)

enum class MessageStatus { SENT, READ }

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val createdAt: String,
    val isMine: Boolean,
    val status: MessageStatus
)

data class ChatItem(
    val id: String,
    val companion: ChatUser,
    val lastMessage: Message?,
    val unreadCount: Int,
    val updatedAt: String
)

data class ChatDetails(
    val chatId: String,
    val companion: ChatUser,
    val unreadCount: Int
)

data class CurrentUser(
    val id: String,
    val username: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val hasAvatar: Boolean = false,
    val lastSeenAt: String? = null
)

data class UserProfile(
    val id: String,
    val username: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val hasAvatar: Boolean = false,
    val avatarMimeType: String? = null,
    val lastSeenAt: String? = null,
    val avatarVersion: Long? = null
)
