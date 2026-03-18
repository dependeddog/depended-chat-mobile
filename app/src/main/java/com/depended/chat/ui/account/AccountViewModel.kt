package com.depended.chat.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.CurrentUser
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatsRepository: ChatsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state

    init {
        loadUser()
    }

    fun loadUser() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        runCatching { authRepository.getCurrentUser(forceRefresh = true) }
            .onSuccess { user -> _state.update { it.copy(loading = false, user = user) } }
            .onFailure { err -> _state.update { it.copy(loading = false, error = err.message ?: "Не удалось загрузить аккаунт") } }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        chatsRepository.disconnectAllSockets()
        authRepository.logout()
        onDone()
    }
}

data class AccountUiState(
    val loading: Boolean = false,
    val user: CurrentUser? = null,
    val error: String? = null
)
