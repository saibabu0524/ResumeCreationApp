package com.softsuave.resumecreationapp.feature.resume

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softsuave.resumecreationapp.core.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val repository: ResumeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResumeUiState>(ResumeUiState.Idle)
    val uiState: StateFlow<ResumeUiState> = _uiState.asStateFlow()

    fun tailorResume(pdfUri: Uri, jobDescription: String, provider: String) {
        if (jobDescription.isBlank()) {
            _uiState.value = ResumeUiState.Error("Job description cannot be empty.")
            return
        }

        viewModelScope.launch {
            // ── Step 1: Submit the job ────────────────────────────────────────
            _uiState.value = ResumeUiState.Loading("Uploading resume…")
            val submitResult = repository.submitTailorJob(pdfUri, jobDescription, provider)
            val jobId = when (submitResult) {
                is Result.Success -> submitResult.data
                is Result.Error -> {
                    _uiState.value = ResumeUiState.Error(
                        submitResult.exception.message ?: "Failed to submit job"
                    )
                    return@launch
                }
                is Result.Loading -> return@launch
            }

            // ── Step 2: Poll until completed / failed ─────────────────────────
            // Poll every 5 s, for up to 10 minutes (120 attempts).
            _uiState.value = ResumeUiState.Loading("Processing with $provider…")
            val maxAttempts = 120
            var attempt = 0

            while (attempt < maxAttempts) {
                delay(5_000L)
                attempt++

                val statusResult = repository.getJobStatus(jobId)
                when {
                    statusResult is Result.Error -> {
                        // Treat transient network errors as non-fatal during polling.
                        // Only abort if we keep failing after a few consecutive retries.
                        // For simplicity, log and retry.
                        _uiState.value = ResumeUiState.Loading(
                            "Processing with $provider… (${attempt * 5}s)"
                        )
                        continue
                    }

                    statusResult is Result.Success -> {
                        val status = statusResult.data.status

                        when (status) {
                            "completed" -> {
                                // ── Step 3: Download the PDF ──────────────────
                                _uiState.value = ResumeUiState.Loading("Downloading tailored resume…")
                                val downloadResult = repository.downloadTailoredResume(jobId)
                                when (downloadResult) {
                                    is Result.Success -> {
                                        _uiState.value = ResumeUiState.Success(downloadResult.data)
                                    }
                                    is Result.Error -> {
                                        _uiState.value = ResumeUiState.Error(
                                            downloadResult.exception.message ?: "Download failed"
                                        )
                                    }
                                    is Result.Loading -> Unit
                                }
                                return@launch
                            }

                            "failed" -> {
                                _uiState.value = ResumeUiState.Error(
                                    statusResult.data.error ?: "Resume processing failed. Please try again."
                                )
                                return@launch
                            }

                            else -> {
                                // queued or processing — update message with elapsed time
                                val elapsed = attempt * 5
                                _uiState.value = ResumeUiState.Loading(
                                    when {
                                        elapsed < 30 -> "Processing with $provider…"
                                        elapsed < 60 -> "Processing with $provider… (${elapsed}s)"
                                        else -> "Still processing… (${elapsed}s)"
                                    }
                                )
                            }
                        }
                    }

                    else -> Unit
                }
            }

            _uiState.value = ResumeUiState.Error("Job timed out after 10 minutes. Please try again.")
        }
    }

    fun reset() {
        _uiState.value = ResumeUiState.Idle
    }
}

