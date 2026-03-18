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
    fun onConfirmPassword(v: String) { _state.value = _state.value.copy(confirmPassword = v) }

    fun toggleMode() {
        _state.value = _state.value.copy(
            isLoginMode = !_state.value.isLoginMode,
            error = null,
            confirmPassword = ""
        )
    }

    fun submit(onSuccess: () -> Unit) = viewModelScope.launch {
        val current = _state.value
        if (current.username.isBlank() || current.password.isBlank()) {
            _state.value = current.copy(error = "Заполните username и password")
            return@launch
        }
        if (!current.isLoginMode) {
            if (current.confirmPassword.isBlank()) {
                _state.value = current.copy(error = "Подтвердите пароль")
                return@launch
            }
            if (current.password != current.confirmPassword) {
                _state.value = current.copy(error = "Пароли не совпадают")
                return@launch
            }
        }

        _state.value = current.copy(loading = true, error = null)
        runCatching {
            if (current.isLoginMode) authRepository.login(current.username, current.password)
            else authRepository.register(current.username, current.password)
        }.onSuccess {
            onSuccess()
        }.onFailure {
            val fallback = if (current.isLoginMode) "Login failed" else "Registration failed"
            _state.value = _state.value.copy(error = it.message ?: fallback)
        }
        _state.value = _state.value.copy(loading = false)
    }
}

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoginMode: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null
)
