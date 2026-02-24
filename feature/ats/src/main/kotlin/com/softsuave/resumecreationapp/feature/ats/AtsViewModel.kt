package com.softsuave.resumecreationapp.feature.ats

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.domain.model.AtsResult
import com.softsuave.resumecreationapp.core.domain.model.Result
import com.softsuave.resumecreationapp.core.domain.usecase.ats.AnalyseAtsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val analyseAtsUseCase: AnalyseAtsUseCase,
    @ApplicationContext private val context: Context,
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
            // Resolve Uri → ByteArray + fileName in the ViewModel layer
            val (pdfBytes, fileName) = readPdfFromUri(pdfUri)
                ?: run {
                    _uiState.value = AtsUiState.Error("Could not read the PDF file.")
                    return@launch
                }

            val params = AnalyseAtsUseCase.Params(
                pdfBytes = pdfBytes,
                fileName = fileName,
                jobDescription = jobDescription,
                provider = provider,
            )

            when (val result = analyseAtsUseCase(params)) {
                is Result.Success -> _uiState.value = AtsUiState.Success(result.data)
                is Result.Error   -> _uiState.value = AtsUiState.Error(result.exception.message ?: "Unknown error")
                is Result.Loading -> Unit
            }
        }
    }

    fun reset() {
        _uiState.value = AtsUiState.Idle
    }

    /**
     * Reads a content URI into raw bytes + display name.
     * Returns null if the URI cannot be resolved.
     */
    private fun readPdfFromUri(uri: Uri): Pair<ByteArray, String>? {
        var fileName = "resume.pdf"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) fileName = cursor.getString(idx)
            }
        }
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            bytes to fileName
        } catch (_: Exception) {
            null
        }
    }
}
