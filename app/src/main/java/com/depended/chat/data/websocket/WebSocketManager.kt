package com.depended.chat.data.websocket

import com.depended.chat.BuildConfig
import com.depended.chat.data.auth.TokenProvider
import com.depended.chat.data.remote.dto.WebSocketEventDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient,
    private val tokenProvider: TokenProvider,
    private val json: Json
) {
    private var globalSocket: WebSocket? = null
    private val chatSockets = mutableMapOf<String, WebSocket>()

    fun connectGlobal(): Flow<WebSocketEventDto> = callbackFlow {
        val token = runBlocking { tokenProvider.accessToken() }
        val req = Request.Builder().url("${BuildConfig.WS_BASE_URL}/ws/events?token=$token").build()
        globalSocket = client.newWebSocket(req, listener { trySend(it).isSuccess })
        awaitClose { globalSocket?.close(1000, null) }
    }

    fun connectChat(chatId: String): Flow<WebSocketEventDto> = callbackFlow {
        val token = runBlocking { tokenProvider.accessToken() }
        val req = Request.Builder().url("${BuildConfig.WS_BASE_URL}/ws/chats/$chatId?token=$token").build()
        val socket = client.newWebSocket(req, listener { trySend(it).isSuccess })
        chatSockets[chatId] = socket
        awaitClose {
            socket.close(1000, null)
            chatSockets.remove(chatId)
        }
    }

    fun disconnectAll() {
        globalSocket?.close(1000, null)
        globalSocket = null
        chatSockets.values.forEach { it.close(1000, null) }
        chatSockets.clear()
    }

    fun disconnectChat(chatId: String) {
        chatSockets.remove(chatId)?.close(1000, null)
    }

    private fun listener(onEvent: (WebSocketEventDto) -> Unit) = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            runCatching { json.decodeFromString<WebSocketEventDto>(text) }.getOrNull()?.let(onEvent)
        }
    }
}
