package com.depended.chat.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.model.CurrentUser
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatsRepository: ChatsRepository,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state

    private var globalEventsJob: Job? = null

    init {
        observeCurrentUser()
        viewModelScope.launch { profileRepository.touchLastSeen() }
        loadChats()
    }

    private fun observeCurrentUser() = viewModelScope.launch {
        authRepository.observeCurrentUser().collect { user ->
            _state.update { it.copy(currentUser = user) }

            val currentUserId = user?.id
            if (!currentUserId.isNullOrBlank()) {
                observeGlobalEventsOnce(currentUserId)
            }
        }
    }

    fun loadCurrentUser() = viewModelScope.launch {
        runCatching { authRepository.getCurrentUser(forceRefresh = true) }
            .onFailure { _state.update { s -> s.copy(error = it.message ?: "Не удалось загрузить пользователя") } }
    }

    fun loadChats() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }

        runCatching { chatsRepository.getChats() }
            .onSuccess { items ->
                _state.update {
                    it.copy(
                        loading = false,
                        items = items,
                        isEmpty = items.isEmpty()
                    )
                }

                val currentUserId = _state.value.currentUser?.id
                if (!currentUserId.isNullOrBlank()) {
                    observeGlobalEventsOnce(currentUserId)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Не удалось загрузить чаты"
                    )
                }
            }
    }

    private fun observeGlobalEventsOnce(currentUserId: String) {
        if (globalEventsJob != null) return

        globalEventsJob = viewModelScope.launch {
            chatsRepository.globalEvents(currentUserId).collect { updated ->
                _state.update { current ->
                    val existingIndex = current.items.indexOfFirst { it.id == updated.id }

                    val newItems = if (existingIndex >= 0) {
                        current.items.map { item ->
                            if (item.id == updated.id) {
                                item.copy(
                                    companion = if (updated.companion.username.isNotBlank()) updated.companion else item.companion,
                                    unreadCount = updated.unreadCount,
                                    lastMessage = updated.lastMessage,
                                    updatedAt = updated.updatedAt
                                )
                            } else {
                                item
                            }
                        }
                    } else {
                        listOf(updated) + current.items
                    }.sortedByDescending { it.updatedAt }

                    current.copy(
                        items = newItems,
                        isEmpty = newItems.isEmpty()
                    )
                }
            }
        }
    }

    fun onCreateDialogChanged(show: Boolean) {
        _state.update { it.copy(showCreateDialog = show, createError = null) }
    }

    fun onNewChatUsernameChanged(value: String) {
        _state.update { it.copy(newChatUsername = value, createError = null) }
    }

    fun createDirectChat(onOpenChat: (String) -> Unit) = viewModelScope.launch {
        val username = state.value.newChatUsername.trim()
        if (username.isBlank()) {
            _state.update { it.copy(createError = "Введите username") }
            return@launch
        }

        _state.update { it.copy(creatingChat = true, createError = null) }
        runCatching { chatsRepository.createDirectChat(username) }
            .onSuccess { chatId ->
                _state.update { it.copy(creatingChat = false, showCreateDialog = false, newChatUsername = "") }
                loadChats()
                onOpenChat(chatId)
            }
            .onFailure { error ->
                val msg = error.message.orEmpty()
                val mapped = when {
                    msg.contains("not found", ignoreCase = true) -> "Пользователь не найден"
                    msg.contains("self", ignoreCase = true) -> "Нельзя создать чат с самим собой"
                    else -> if (msg.isBlank()) "Не удалось создать чат" else msg
                }
                _state.update { it.copy(creatingChat = false, createError = mapped) }
            }
    }

    fun refreshChats() = viewModelScope.launch {
        _state.update { it.copy(isRefreshing = true, error = null) }

        runCatching { chatsRepository.getChats() }
            .onSuccess { items ->
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        items = items,
                        isEmpty = items.isEmpty()
                    )
                }

                val currentUserId = _state.value.currentUser?.id
                if (!currentUserId.isNullOrBlank()) {
                    observeGlobalEventsOnce(currentUserId)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = error.message ?: "Не удалось обновить чаты"
                    )
                }
            }
    }
}

data class ChatsUiState(
    val loading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<ChatItem> = emptyList(),
    val isEmpty: Boolean = false,
    val error: String? = null,
    val currentUser: CurrentUser? = null,
    val showCreateDialog: Boolean = false,
    val newChatUsername: String = "",
    val creatingChat: Boolean = false,
    val createError: String? = null
)
