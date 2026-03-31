package com.depended.chat.data.websocket

import android.util.Log
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
import okhttp3.Response
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

    private fun wsBase(): String = BuildConfig.WS_BASE_URL.trimEnd('/')

    fun connectGlobal(): Flow<WebSocketEventDto> = callbackFlow {
        val token = runBlocking { tokenProvider.accessToken() }
        val req = Request.Builder()
            .url("${wsBase()}/ws/events?token=$token")
            .build()

        globalSocket = client.newWebSocket(req, listener("global") { trySend(it).isSuccess })

        awaitClose {
            globalSocket?.close(1000, null)
            globalSocket = null
        }
    }

    fun connectChat(chatId: String): Flow<WebSocketEventDto> = callbackFlow {
        val token = runBlocking { tokenProvider.accessToken() }
        val url = "${BuildConfig.WS_BASE_URL.trimEnd('/')}/ws/chats/$chatId?token=$token"

        Log.d("WS_CHAT", "[connectChat] chatId=$chatId url=$url")

        val req = Request.Builder()
            .url(url)
            .build()

        val socket = client.newWebSocket(req, chatListener(chatId) { event ->
            trySend(event).isSuccess
        })

        chatSockets[chatId] = socket

        awaitClose {
            Log.d("WS_CHAT", "[awaitClose] closing chatId=$chatId")
            socket.close(1000, null)
            chatSockets.remove(chatId)
        }
    }

    fun disconnectChat(chatId: String) {
        Log.d("WS_CHAT", "[disconnectChat] chatId=$chatId")
        chatSockets.remove(chatId)?.close(1000, null)
    }

    private fun chatListener(
        chatId: String,
        onEvent: (WebSocketEventDto) -> Unit
    ) = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WS_CHAT", "[onOpen] chatId=$chatId code=${response.code}")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WS_CHAT", "[onMessage] chatId=$chatId raw=$text")

            runCatching { json.decodeFromString<WebSocketEventDto>(text) }
                .onSuccess {
                    Log.d("WS_CHAT", "[decoded] chatId=$chatId event=${it.event}")
                    onEvent(it)
                }
                .onFailure {
                    Log.e("WS_CHAT", "[decodeError] chatId=$chatId", it)
                }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WS_CHAT", "[onClosing] chatId=$chatId code=$code reason=$reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WS_CHAT", "[onClosed] chatId=$chatId code=$code reason=$reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(
                "WS_CHAT",
                "[onFailure] chatId=$chatId code=${response?.code} message=${response?.message}",
                t
            )
        }
    }

    fun disconnectAll() {
        globalSocket?.close(1000, null)
        globalSocket = null
        chatSockets.values.forEach { it.close(1000, null) }
        chatSockets.clear()
    }

    private fun listener(tag: String, onEvent: (WebSocketEventDto) -> Unit) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WS", "[$tag] onOpen code=${response.code} url=${response.request.url}")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WS", "[$tag] onMessage raw=$text")
            runCatching { json.decodeFromString<WebSocketEventDto>(text) }
                .onSuccess {
                    Log.d("WS", "[$tag] decoded event=${it.event}")
                    onEvent(it)
                }
                .onFailure {
                    Log.e("WS", "[$tag] decode failed", it)
                }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WS", "[$tag] onClosing code=$code reason=$reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WS", "[$tag] onClosed code=$code reason=$reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(
                "WS",
                "[$tag] onFailure code=${response?.code} message=${response?.message} url=${response?.request?.url}",
                t
            )
        }
    }
}
