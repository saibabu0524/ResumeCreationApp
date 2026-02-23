package com.softsuave.resumecreationapp.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile ViewModel.
 *
 * Uses [SavedStateHandle] to survive process death (restores userId argument).
 * Reference implementation for:
 * - Reading type-safe navigation arguments from [SavedStateHandle]
 * - Edit-mode state management
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<ProfileUiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        analyticsTracker.track(AnalyticsEvent.ScreenView(screenName = "profile"))
        loadProfile()
    }

    fun onEvent(event: ProfileUserIntent) {
        when (event) {
            is ProfileUserIntent.Retry -> loadProfile()
            is ProfileUserIntent.EditClicked -> enterEditMode()
            is ProfileUserIntent.SaveClicked -> saveProfile()
            is ProfileUserIntent.CancelEdit -> cancelEdit()
            is ProfileUserIntent.DisplayNameChanged -> _uiState.update {
                it.copy(editDisplayName = event.name)
            }
            is ProfileUserIntent.BackClicked -> viewModelScope.launch {
                _uiEvent.send(ProfileUiEvent.NavigateBack)
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = userRepository.getUserById(userId)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        user = result.data,
                        isLoading = false,
                        editDisplayName = result.data.displayName,
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message,
                    )
                }
                is Result.Loading -> { /* handled above */ }
            }
        }
    }

    private fun enterEditMode() {
        _uiState.update {
            it.copy(
                isEditing = true,
                editDisplayName = it.user?.displayName.orEmpty(),
            )
        }
    }

    private fun cancelEdit() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editDisplayName = it.user?.displayName.orEmpty(),
            )
        }
    }

    private fun saveProfile() {
        val user = _uiState.value.user ?: return
        val newName = _uiState.value.editDisplayName.trim()

        if (newName.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val updatedUser = user.copy(displayName = newName)
            when (val result = userRepository.updateUser(updatedUser)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(user = updatedUser, isEditing = false, isLoading = false)
                    }
                    _uiEvent.send(ProfileUiEvent.ShowSnackbar("Profile updated"))
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.send(ProfileUiEvent.ShowSnackbar(result.exception.message))
                }
                is Result.Loading -> { /* handled above */ }
            }
        }
    }
}
