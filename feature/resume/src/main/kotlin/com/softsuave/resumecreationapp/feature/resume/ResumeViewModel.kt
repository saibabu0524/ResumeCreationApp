package com.softsuave.resumecreationapp.feature.resume

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResumeUiState {
    object Idle : ResumeUiState()
    data class Loading(val stepMessage: String) : ResumeUiState()
    data class Success(val pdfBytes: ByteArray) : ResumeUiState()
    data class Error(val message: String) : ResumeUiState()
}

@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val repository: ResumeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResumeUiState>(ResumeUiState.Idle)
    val uiState: StateFlow<ResumeUiState> = _uiState.asStateFlow()

    fun tailorResume(pdfUri: Uri, jobDescription: String, provider: String) {
        if (jobDescription.isBlank()) {
            _uiState.value = ResumeUiState.Error("Job description cannot be empty.")
            return
        }
        
        _uiState.value = ResumeUiState.Loading("Processing with $provider... This may take a minute.")
        
        viewModelScope.launch {
            val result = repository.tailorResume(pdfUri, jobDescription, provider)
            when (result) {
                is Result.Success -> {
                    _uiState.value = ResumeUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = ResumeUiState.Error(result.exception.message ?: "An unknown error occurred")
                }
                is Result.Loading -> {
                    // Not emitted by repository directly in this implementation
                }
            }
        }
    }

    fun reset() {
        _uiState.value = ResumeUiState.Idle
    }
}
