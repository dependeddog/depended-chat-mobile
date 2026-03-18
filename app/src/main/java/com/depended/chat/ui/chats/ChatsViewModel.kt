package com.depended.chat.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatsRepository: ChatsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state

    private var globalEventsJob: Job? = null

    init { loadChats() }

    fun loadChats() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { chatsRepository.getChats() }
            .onSuccess { items ->
                _state.update { it.copy(loading = false, items = items, isEmpty = items.isEmpty()) }
                observeGlobalEventsOnce()
            }
            .onFailure { error ->
                _state.update { state ->
                    state.copy(loading = false, error = error.message ?: "Не удалось загрузить чаты")
                }
            }
    }

    private fun observeGlobalEventsOnce() {
        if (globalEventsJob != null) return
        globalEventsJob = viewModelScope.launch {
            chatsRepository.globalEvents().collect { updated ->
                _state.update { current ->
                    current.copy(items = current.items.map { if (it.id == updated.id) it.copy(unreadCount = updated.unreadCount, lastMessage = updated.lastMessage) else it })
                }
            }
        }
    }

    fun onCreateDialogChanged(show: Boolean) {
        _state.update { it.copy(showCreateDialog = show, createError = null) }
    }

    fun onNewChatUserIdChanged(value: String) {
        _state.update { it.copy(newChatUserId = value, createError = null) }
    }

    fun createDirectChat(onOpenChat: (String) -> Unit) = viewModelScope.launch {
        val userId = state.value.newChatUserId.trim()
        if (userId.isBlank()) {
            _state.update { it.copy(createError = "Введите user_id") }
            return@launch
        }
        val isUuid = runCatching { UUID.fromString(userId) }.isSuccess
        if (!isUuid) {
            _state.update { it.copy(createError = "user_id должен быть валидным UUID") }
            return@launch
        }

        _state.update { it.copy(creatingChat = true, createError = null) }
        runCatching { chatsRepository.createDirectChat(userId) }
            .onSuccess { chatId ->
                _state.update { it.copy(creatingChat = false, showCreateDialog = false, newChatUserId = "") }
                loadChats()
                onOpenChat(chatId)
            }
            .onFailure { error ->
                _state.update { it.copy(creatingChat = false, createError = error.message ?: "Не удалось создать чат") }
            }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        chatsRepository.disconnectAllSockets()
        authRepository.logout()
        onDone()
    }
}

data class ChatsUiState(
    val loading: Boolean = false,
    val items: List<ChatItem> = emptyList(),
    val isEmpty: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newChatUserId: String = "",
    val creatingChat: Boolean = false,
    val createError: String? = null
)
