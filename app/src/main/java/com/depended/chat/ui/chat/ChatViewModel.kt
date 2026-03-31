package com.depended.chat.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.MessageEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatsRepository: ChatsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private var chatEventsJob: Job? = null

    fun init(chatId: String) {
        if (_state.value.chatId == chatId) return
        val previousChatId = _state.value.chatId
        chatEventsJob?.cancel()
        if (previousChatId.isNotBlank()) {
            viewModelScope.launch { chatsRepository.disconnectChat(previousChatId) }
        }
        _state.update { it.copy(chatId = chatId) }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching {
                val currentUserId = chatsRepository.getCurrentUserId()
                val details = chatsRepository.getChatDetails(chatId)
                val messages = chatsRepository.getMessages(chatId)
                chatsRepository.markRead(chatId)

                Triple(currentUserId, details, messages)
            }.onSuccess { (currentUserId, details, messages) ->
                _state.update {
                    it.copy(
                        loading = false,
                        companionId = details.companion.id,
                        companionName = details.companion.username,
                        companionAvatarUrl = details.companion.avatarUrl,
                        companionHasAvatar = details.companion.hasAvatar,
                        companionAvatarVersion = details.companion.avatarVersion,
                        messages = messages,
                        error = null
                    )
                }
                observeSocket(chatId, currentUserId)
            }.onFailure { err ->
                _state.update { it.copy(loading = false, error = err.message ?: "Не удалось открыть чат") }
            }
        }
    }

    private fun observeSocket(chatId: String, currentUserId: String) {
        chatEventsJob?.cancel()

        Log.d("WS_CHAT_VM", "[observeSocket.start] chatId=$chatId currentUserId=$currentUserId")

        chatEventsJob = viewModelScope.launch {
            chatsRepository.chatEvents(chatId, currentUserId).collect { event ->
                Log.d("WS_CHAT_VM", "[collect] chatId=$chatId event=$event")

                when (event) {
                    is MessageEvent.Created -> {
                        val incoming = event.message

                        _state.update { current ->
                            val existing = current.messages.indexOfFirst { it.id == incoming.id }

                            if (existing >= 0) {
                                val updated = current.messages.toMutableList()
                                updated[existing] = incoming
                                current.copy(messages = updated)
                            } else {
                                current.copy(messages = current.messages + incoming)
                            }
                        }

                        if (!incoming.isMine) {
                            runCatching {
                                chatsRepository.markRead(chatId)
                            }.onFailure {
                                Log.e("WS_CHAT_VM", "[markRead.onIncoming.failed] chatId=$chatId", it)
                            }
                        }
                    }

                    is MessageEvent.ReadUpTo -> {
                        val readUpToId = event.readUpToMessageId
                        Log.d("WS_CHAT_VM", "[MessageEvent.ReadUpTo] chatId=$chatId readUpToId=$readUpToId")

                        if (!readUpToId.isNullOrBlank()) {
                            _state.update { current ->
                                val index = current.messages.indexOfFirst { it.id == readUpToId }
                                if (index < 0) return@update current

                                val updated = current.messages.mapIndexed { i, msg ->
                                    if (msg.isMine && i <= index) {
                                        msg.copy(status = MessageStatus.READ)
                                    } else {
                                        msg
                                    }
                                }

                                current.copy(messages = updated)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onInput(v: String) {
        _state.update { it.copy(input = v) }
    }

    fun send() = viewModelScope.launch {
        val chatId = _state.value.chatId
        val text = _state.value.input.trim()
        if (chatId.isBlank() || text.isBlank()) return@launch

        _state.update { it.copy(input = "") }
        runCatching { chatsRepository.sendMessage(chatId, text) }
            .onSuccess { sent ->
                _state.update { current ->
                    if (current.messages.any { it.id == sent.id }) current else current.copy(messages = current.messages + sent)
                }
            }
            .onFailure {
                _state.update { it.copy(input = text) }
            }
    }

    override fun onCleared() {
        chatEventsJob?.cancel()
        _state.value.chatId.takeIf { it.isNotBlank() }?.let { chatId ->
            runBlocking { chatsRepository.disconnectChat(chatId) }
        }
        super.onCleared()
    }
}

data class ChatUiState(
    val chatId: String = "",
    val currentUserId: String = "",
    val companionId: String = "",
    val companionName: String = "",
    val companionAvatarUrl: String? = null,
    val companionHasAvatar: Boolean = false,
    val companionAvatarVersion: Long? = null,
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val messages: List<Message> = emptyList()
)
