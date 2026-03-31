package com.depended.chat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.UserProfile
import com.depended.chat.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UserProfileUiState())
    val state: StateFlow<UserProfileUiState> = _state

    fun load(userId: String) = viewModelScope.launch {
        if (_state.value.userId == userId && _state.value.profile != null) return@launch
        _state.update { it.copy(loading = true, error = null, userId = userId) }
        runCatching {
            profileRepository.touchLastSeen()
            profileRepository.getUserProfile(userId)
        }.onSuccess { profile ->
            _state.update { it.copy(loading = false, profile = profile) }
        }.onFailure { err ->
            _state.update { it.copy(loading = false, error = err.message ?: "Не удалось загрузить профиль") }
        }
    }
}

data class UserProfileUiState(
    val userId: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val profile: UserProfile? = null
)
