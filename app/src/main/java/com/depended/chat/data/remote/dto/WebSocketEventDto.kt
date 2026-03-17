package com.depended.chat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class WebSocketEventDto(val event: String, val data: JsonObject)

@Serializable
data class ChatReadEventDto(
    @SerialName("chat_id") val chatId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("read_up_to_message_id") val readUpToMessageId: String? = null
)
