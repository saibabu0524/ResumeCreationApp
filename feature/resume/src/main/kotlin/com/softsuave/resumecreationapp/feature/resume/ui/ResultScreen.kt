package com.softsuave.resumecreationapp.feature.resume.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    pdfBytes: ByteArray,
    onStartOver: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tempPdfFile = remember {
        File(context.cacheDir, "preview_resume.pdf").apply {
            writeBytes(pdfBytes)
        }
    }

    // Render ALL pages
    var pdfBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Zoom/pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(tempPdfFile) {
        withContext(Dispatchers.IO) {
            try {
                val fileDescriptor = ParcelFileDescriptor.open(
                    tempPdfFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                val pdfRenderer = PdfRenderer(fileDescriptor)
                val bitmaps = mutableListOf<Bitmap>()

                for (i in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val scale = 2 // Render at 2x for sharpness
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888
                    )
                    // Fill white background before rendering
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }
                pdfRenderer.close()
                fileDescriptor.close()
                pdfBitmaps = bitmaps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tailored Resume",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onStartOver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save status message
                    AnimatedVisibility(
                        visible = saveStatus != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        saveStatus?.let { status ->
                            Surface(
                                color = if (status.startsWith("Saved"))
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (status.startsWith("Saved")) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (status.startsWith("Saved"))
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Save to Device button
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    saveStatus = null
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val fileName = "Tailored_Resume_${System.currentTimeMillis()}.pdf"
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                // Android 10+ — use MediaStore (no permission needed)
                                                val contentValues = ContentValues().apply {
                                                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                                                    put(MediaStore.Downloads.IS_PENDING, 1)
                                                }
                                                val resolver = context.contentResolver
                                                val uri = resolver.insert(
                                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                                    contentValues
                                                )
                                                if (uri != null) {
                                                    resolver.openOutputStream(uri)?.use { it.write(pdfBytes) }
                                                    contentValues.clear()
                                                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                                                    resolver.update(uri, contentValues, null, null)
                                                    saveStatus = "Saved to Downloads!"
                                                } else {
                                                    saveStatus = "Failed to create file."
                                                }
                                            } else {
                                                // Android 9 and below — write directly
                                                val downloadsDir = context.getExternalFilesDir(null)
                                                    ?: context.filesDir
                                                val saveFile = File(downloadsDir, fileName)
                                                FileOutputStream(saveFile).use { it.write(pdfBytes) }
                                                saveStatus = "Saved to ${saveFile.absolutePath}"
                                            }
                                        } catch (e: Exception) {
                                            saveStatus = "Failed: ${e.message}"
                                        }
                                    }
                                    isSaving = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving && pdfBitmaps.isNotEmpty()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Saving…")
                            } else {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Save")
                            }
                        }

                        // Share button
                        Button(
                            onClick = {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    tempPdfFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Share Resume")
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = pdfBitmaps.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }

                    OutlinedButton(
                        onClick = onStartOver,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Over")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Rendering preview…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                pdfBitmaps.isEmpty() -> {
                    Text(
                        "Preview not available",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    // Scrollable + zoomable PDF viewer
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) {
                                        offset += pan
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            },
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))

                        pdfBitmaps.forEachIndexed { index, bitmap ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        ),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }

                        if (pdfBitmaps.size > 1) {
                            Text(
                                "${pdfBitmaps.size} pages · Pinch to zoom",
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Pinch to zoom",
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
