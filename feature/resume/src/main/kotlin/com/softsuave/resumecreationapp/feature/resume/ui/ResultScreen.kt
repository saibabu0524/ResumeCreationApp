package com.softsuave.resumecreationapp.feature.resume.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    pdfBytes: ByteArray,
    onStartOver: () -> Unit
) {
    val context = LocalContext.current
    val tempPdfFile = remember {
        File(context.cacheDir, "preview_resume.pdf").apply {
            writeBytes(pdfBytes)
        }
    }

    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tempPdfFile) {
        try {
            val fileDescriptor = ParcelFileDescriptor.open(tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0)
                // Render at a decent resolution for preview
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pdfBitmap = bitmap
                page.close()
            }
            pdfRenderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tailored Resume") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pdfBitmap != null) {
                Text("Preview (Page 1)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Preview",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("Preview not available", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (saveStatus != null) {
                Text(saveStatus!!, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    try {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val saveFile = File(downloadsDir, "Tailored_Resume_${System.currentTimeMillis()}.pdf")
                        FileOutputStream(saveFile).use { it.write(pdfBytes) }
                        saveStatus = "Saved to Downloads!"
                    } catch (e: Exception) {
                        saveStatus = "Failed to save: ${e.message}"
                    }
                }) {
                    Text("Save to Device")
                }

                Button(onClick = {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempPdfFile)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Resume"))
                }) {
                    Text("Share PDF")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(onClick = onStartOver, modifier = Modifier.fillMaxWidth()) {
                Text("Start Over")
            }
        }
    }
}
