package com.depended.chat.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.depended.chat.domain.model.UserProfile
import com.depended.chat.domain.repository.AuthRepository
import com.depended.chat.domain.repository.ChatsRepository
import com.depended.chat.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatsRepository: ChatsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state

    init {
        loadProfile()
    }

    fun loadProfile() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null, updateSuccess = null) }
        runCatching {
            profileRepository.touchLastSeen()
            profileRepository.getMyProfile(forceRefresh = true)
        }.onSuccess { profile ->
            _state.update {
                it.copy(
                    loading = false,
                    profile = profile,
                    bioInput = profile.bio.orEmpty()
                )
            }
        }.onFailure { err ->
            _state.update { it.copy(loading = false, error = err.message ?: "Не удалось загрузить аккаунт") }
        }
    }

    fun onBioChanged(value: String) {
        _state.update { it.copy(bioInput = value, error = null, updateSuccess = null) }
    }

    fun saveBio() = viewModelScope.launch {
        val bio = state.value.bioInput.trim().ifBlank { "" }
        _state.update { it.copy(savingBio = true, error = null, updateSuccess = null) }
        runCatching { profileRepository.updateBio(bio = bio.ifBlank { null }) }
            .onSuccess { profile ->
                _state.update {
                    it.copy(
                        savingBio = false,
                        profile = profile,
                        bioInput = profile.bio.orEmpty(),
                        updateSuccess = "Описание обновлено"
                    )
                }
            }
            .onFailure { err -> _state.update { it.copy(savingBio = false, error = err.message ?: "Не удалось обновить описание") } }
    }

    fun uploadAvatar(bytes: ByteArray, mimeType: String) = viewModelScope.launch {
        _state.update { it.copy(uploadingAvatar = true, error = null, updateSuccess = null) }
        runCatching { profileRepository.uploadAvatar(bytes, mimeType) }
            .onSuccess { profile -> _state.update { it.copy(uploadingAvatar = false, profile = profile, updateSuccess = "Аватар обновлён") } }
            .onFailure { err -> _state.update { it.copy(uploadingAvatar = false, error = err.message ?: "Не удалось загрузить аватар") } }
    }

    fun deleteAvatar() = viewModelScope.launch {
        _state.update { it.copy(uploadingAvatar = true, error = null, updateSuccess = null) }
        runCatching { profileRepository.deleteAvatar() }
            .onSuccess { profile -> _state.update { it.copy(uploadingAvatar = false, profile = profile, updateSuccess = "Аватар удалён") } }
            .onFailure { err -> _state.update { it.copy(uploadingAvatar = false, error = err.message ?: "Не удалось удалить аватар") } }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        chatsRepository.disconnectAllSockets()
        authRepository.logout()
        onDone()
    }
}

data class AccountUiState(
    val loading: Boolean = false,
    val savingBio: Boolean = false,
    val uploadingAvatar: Boolean = false,
    val profile: UserProfile? = null,
    val bioInput: String = "",
    val error: String? = null,
    val updateSuccess: String? = null
)
