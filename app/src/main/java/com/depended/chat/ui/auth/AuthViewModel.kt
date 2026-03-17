package com.depended.chat.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun onUsername(v: String) { _state.value = _state.value.copy(username = v) }
    fun onPassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun toggleMode() { _state.value = _state.value.copy(isLoginMode = !_state.value.isLoginMode, error = null) }

    fun submit(onSuccess: () -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            if (_state.value.isLoginMode) authRepository.login(_state.value.username, _state.value.password)
            else authRepository.register(_state.value.username, _state.value.password)
        }.onSuccess {
            onSuccess()
        }.onFailure {
            _state.value = _state.value.copy(error = it.message ?: "Auth error")
        }
        _state.value = _state.value.copy(loading = false)
    }
}

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null
)
