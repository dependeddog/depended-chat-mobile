package com.depended.chat.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state

    fun bootstrap() = viewModelScope.launch {
        _state.value = SplashState.Result(authRepository.refreshIfNeeded())
    }
}

sealed class SplashState {
    data object Loading : SplashState()
    data class Result(val authorized: Boolean) : SplashState()
}
