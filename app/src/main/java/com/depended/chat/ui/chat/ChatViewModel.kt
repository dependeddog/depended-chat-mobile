package com.depended.chat.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.Message
import com.depended.chat.domain.model.MessageStatus
import com.depended.chat.domain.repository.ChatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatsRepository: ChatsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    fun init(chatId: String) {
        if (_state.value.chatId == chatId) return
        _state.update { it.copy(chatId = chatId) }
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val details = chatsRepository.getChatDetails(chatId)
            val messages = chatsRepository.getMessages(chatId)
            chatsRepository.markRead(chatId)
            _state.update {
                it.copy(
                    loading = false,
                    companionName = details.companion.username,
                    messages = messages
                )
            }
            observeSocket(chatId)
        }
    }

    private fun observeSocket(chatId: String) = viewModelScope.launch {
        chatsRepository.chatEvents(chatId).collect { incoming ->
            _state.update { current ->
                val existing = current.messages.indexOfFirst { it.id == incoming.id }
                if (existing >= 0) {
                    val updated = current.messages.toMutableList()
                    updated[existing] = updated[existing].copy(status = MessageStatus.DELIVERED)
                    current.copy(messages = updated)
                } else current.copy(messages = current.messages + incoming)
            }
        }
    }

    fun onInput(v: String) { _state.update { it.copy(input = v) } }

    fun send() = viewModelScope.launch {
        val chatId = _state.value.chatId
        val text = _state.value.input.trim()
        if (chatId.isBlank() || text.isBlank()) return@launch

        _state.update { it.copy(input = "") }
        runCatching { chatsRepository.sendMessage(chatId, text) }
            .onSuccess { sent ->
                _state.update { current ->
                    if (current.messages.any { it.id == sent.id }) current
                    else current.copy(messages = current.messages + sent.copy(status = MessageStatus.SENT))
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            _state.value.chatId.takeIf { it.isNotBlank() }?.let { chatsRepository.disconnectChat(it) }
        }
    }
}

data class ChatUiState(
    val chatId: String = "",
    val companionName: String = "",
    val input: String = "",
    val loading: Boolean = false,
    val messages: List<Message> = emptyList()
)
