package com.depended.chat.domain.model

data class ChatUser(val id: String, val username: String)

enum class MessageStatus { SENT, DELIVERED, READ }

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val createdAt: String,
    val isMine: Boolean = false,
    val status: MessageStatus = MessageStatus.DELIVERED
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
