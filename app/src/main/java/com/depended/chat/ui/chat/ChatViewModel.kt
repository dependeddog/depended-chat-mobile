package com.depended.chat.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.MessageEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatsRepository: ChatsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private val _events = MutableSharedFlow<ChatUiEvent>()
    val events: SharedFlow<ChatUiEvent> = _events.asSharedFlow()

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
                        currentUserId = currentUserId,
                        companionName = details.companion.username,
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

        chatEventsJob = viewModelScope.launch {
            chatsRepository.chatEvents(chatId, currentUserId).collect { event ->
                when (event) {
                    is MessageEvent.Created -> upsertMessage(event.message)
                    is MessageEvent.Updated -> upsertMessage(event.message)
                    is MessageEvent.Deleted -> removeMessage(event.messageId)
                    is MessageEvent.ChatDeleted -> {
                        if (event.chatId == _state.value.chatId) {
                            _events.emit(ChatUiEvent.ChatDeleted)
                        }
                    }
                    is MessageEvent.ReadUpTo -> applyReadStatus(event.readUpToMessageId)
                }
            }
        }
    }

    private fun upsertMessage(incoming: Message) {
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
            viewModelScope.launch {
                runCatching { chatsRepository.markRead(incoming.chatId) }
                    .onFailure {
                        Log.e("WS_CHAT_VM", "[markRead.onIncoming.failed] chatId=${incoming.chatId}", it)
                    }
            }
        }
    }

    private fun removeMessage(messageId: String) {
        _state.update { current ->
            current.copy(messages = current.messages.filterNot { it.id == messageId })
        }
    }

    private fun applyReadStatus(readUpToId: String?) {
        if (readUpToId.isNullOrBlank()) return

        _state.update { current ->
            val index = current.messages.indexOfFirst { it.id == readUpToId }
            if (index < 0) return@update current

            val updated = current.messages.mapIndexed { i, msg ->
                if (msg.isMine && i <= index) msg.copy(status = MessageStatus.READ) else msg
            }
            current.copy(messages = updated)
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
                emitError("Не удалось отправить сообщение")
            }
    }

    fun updateMessage(messageId: String, originalText: String, newText: String) = viewModelScope.launch {
        val chatId = _state.value.chatId
        val trimmed = newText.trim()
        if (chatId.isBlank()) return@launch
        if (trimmed.isBlank()) {
            emitError("Текст сообщения не должен быть пустым")
            return@launch
        }
        if (trimmed == originalText) return@launch

        _state.update { it.copy(editingInProgress = true) }
        runCatching { chatsRepository.updateMessage(chatId, messageId, trimmed) }
            .onSuccess { updated ->
                upsertMessage(updated)
            }
            .onFailure { emitError(it.message ?: "Не удалось изменить сообщение") }
        _state.update { it.copy(editingInProgress = false) }
    }

    fun deleteMessage(messageId: String) = viewModelScope.launch {
        val chatId = _state.value.chatId
        if (chatId.isBlank()) return@launch

        _state.update { it.copy(deleteMessageInProgress = true) }
        runCatching { chatsRepository.deleteMessage(chatId, messageId) }
            .onSuccess { removeMessage(messageId) }
            .onFailure { emitError(it.message ?: "Не удалось удалить сообщение") }
        _state.update { it.copy(deleteMessageInProgress = false) }
    }

    fun deleteChat() = viewModelScope.launch {
        val chatId = _state.value.chatId
        if (chatId.isBlank()) return@launch

        _state.update { it.copy(deleteChatInProgress = true) }
        runCatching { chatsRepository.deleteChat(chatId) }
            .onSuccess {
                _events.emit(ChatUiEvent.ChatDeleted)
            }
            .onFailure { emitError(it.message ?: "Не удалось удалить чат") }
        _state.update { it.copy(deleteChatInProgress = false) }
    }

    private fun emitError(message: String) {
        viewModelScope.launch { _events.emit(ChatUiEvent.Error(message)) }
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
    val companionName: String = "",
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val messages: List<Message> = emptyList(),
    val editingInProgress: Boolean = false,
    val deleteMessageInProgress: Boolean = false,
    val deleteChatInProgress: Boolean = false
)

sealed class ChatUiEvent {
    data class Error(val message: String) : ChatUiEvent()
    data object ChatDeleted : ChatUiEvent()
}
