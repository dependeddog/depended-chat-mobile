package com.depended.chat.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.ChatItem
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatsRepository: ChatsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatsUiState())
    val state: StateFlow<ChatsUiState> = _state

    init { loadChats() }

    fun loadChats() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        runCatching { chatsRepository.getChats() }
            .onSuccess { items ->
                _state.update { it.copy(loading = false, items = items) }
                observeGlobalEvents()
            }
            .onFailure { error ->
                _state.update { state ->
                    state.copy(loading = false, error = error.message)
                }
            }
    }

    private fun observeGlobalEvents() = viewModelScope.launch {
        chatsRepository.globalEvents().collect { updated ->
            _state.update { current ->
                current.copy(items = current.items.map { if (it.id == updated.id) it.copy(unreadCount = updated.unreadCount, lastMessage = updated.lastMessage) else it })
            }
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
    val error: String? = null
)
