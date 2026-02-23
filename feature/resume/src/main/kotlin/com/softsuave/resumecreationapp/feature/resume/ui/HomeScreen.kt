package com.softsuave.resumecreationapp.feature.resume.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.softsuave.resumecreationapp.feature.resume.ResumeUiState
import com.softsuave.resumecreationapp.feature.resume.ResumeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ResumeViewModel = hiltViewModel(),
    onNavigateToResult: (ByteArray) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedPdfUri by remember<MutableState<Uri?>> { mutableStateOf(null) }
    var jobDescription by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf("gemini") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPdfUri = uri
    }

    // Effect to navigate on success
    LaunchedEffect(uiState) {
        if (uiState is ResumeUiState.Success) {
            onNavigateToResult((uiState as ResumeUiState.Success).pdfBytes)
            viewModel.reset() // Reset after navigation
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tailor Resume") })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is ResumeUiState.Loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = (uiState as ResumeUiState.Loading).stepMessage)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // PDF Picker
                    Button(
                        onClick = { pdfPickerLauncher.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedPdfUri != null) "Change Selected PDF" else "Select Resume PDF")
                    }
                    if (selectedPdfUri != null) {
                        Text("PDF Selected", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Job Description Input
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Job Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        maxLines = 15
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // AI Provider Selection
                    Text("Select AI Provider", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedProvider == "gemini",
                                onClick = { selectedProvider = "gemini" }
                            )
                            Text("Gemini (Cloud)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedProvider == "ollama",
                                onClick = { selectedProvider = "ollama" }
                            )
                            Text("Ollama (Local)")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState is ResumeUiState.Error) {
                        Text(
                            text = (uiState as ResumeUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            selectedPdfUri?.let { uri ->
                                viewModel.tailorResume(uri, jobDescription, selectedProvider)
                            }
                        },
                        enabled = selectedPdfUri != null && jobDescription.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tailor Resume")
                    }
                }
            }
        }
    }
}
