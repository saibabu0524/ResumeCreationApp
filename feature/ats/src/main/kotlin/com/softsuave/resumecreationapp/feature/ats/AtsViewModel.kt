package com.softsuave.resumecreationapp.feature.ats

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

sealed class AtsUiState {
    object Idle : AtsUiState()
    data class Loading(val stepMessage: String = "Analysing your resume…") : AtsUiState()
    data class Success(val result: AtsResult) : AtsUiState()
    data class Error(val message: String) : AtsUiState()
}

@HiltViewModel
class AtsViewModel @Inject constructor(
    private val repository: AtsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AtsUiState>(AtsUiState.Idle)
    val uiState: StateFlow<AtsUiState> = _uiState.asStateFlow()

    fun analyse(pdfUri: Uri, jobDescription: String, provider: String) {
        if (jobDescription.isBlank()) {
            _uiState.value = AtsUiState.Error("Job description cannot be empty.")
            return
        }
        _uiState.value = AtsUiState.Loading("Scanning with $provider…")

        viewModelScope.launch {
            when (val result = repository.analyseAts(pdfUri, jobDescription, provider)) {
                is Result.Success -> _uiState.value = AtsUiState.Success(result.data)
                is Result.Error   -> _uiState.value = AtsUiState.Error(result.exception.message ?: "Unknown error")
                is Result.Loading -> Unit
            }
        }
    }

    fun reset() {
        _uiState.value = AtsUiState.Idle
    }
}
