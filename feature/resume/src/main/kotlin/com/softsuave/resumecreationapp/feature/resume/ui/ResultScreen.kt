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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Elegant Theme Palette
private val LuxBg = androidx.compose.ui.graphics.Color(0xFFFAF9F6)
private val LuxTextPrimary = androidx.compose.ui.graphics.Color(0xFF111111)
private val LuxAccent = androidx.compose.ui.graphics.Color(0xFFB59A70)
private val LuxBorder = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
private val LuxSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

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

    var pdfBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var saveStatus by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

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
                    val rndScale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * rndScale,
                        page.height * rndScale,
                        Bitmap.Config.ARGB_8888
                    )
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
            Column(
                modifier = Modifier.fillMaxWidth().background(LuxBg)
            ) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onStartOver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LuxTextPrimary)
                    }
                    Text(
                        text = "THE DOCUMENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        color = LuxTextPrimary
                    )
                    Box(modifier = Modifier.size(48.dp))
                }
                HorizontalDivider(color = LuxBorder, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LuxBg)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(visible = saveStatus != null) {
                    saveStatus?.let { status ->
                        Text(
                            text = status.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (status.startsWith("Saved")) LuxAccent else androidx.compose.ui.graphics.Color(0xFFD32F2F),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                saveStatus = null
                                withContext(Dispatchers.IO) {
                                    try {
                                        val fileName = "Tailored_Resume_${System.currentTimeMillis()}.pdf"
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val contentValues = ContentValues().apply {
                                                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                                                put(MediaStore.Downloads.IS_PENDING, 1)
                                            }
                                            val resolver = context.contentResolver
                                            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
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
                                            val downloadsDir = context.getExternalFilesDir(null) ?: context.filesDir
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
                        modifier = Modifier.weight(1f).height(60.dp),
                        enabled = !isSaving && pdfBitmaps.isNotEmpty(),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxTextPrimary,
                            disabledContainerColor = LuxBorder,
                            contentColor = LuxSurface
                        )
                    ) {
                        Text(if (isSaving) "SAVING..." else "SAVE PDF", fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempPdfFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Resume"))
                        },
                        modifier = Modifier.weight(1f).height(60.dp),
                        enabled = pdfBitmaps.isNotEmpty(),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LuxTextPrimary),
                        border = BorderStroke(1.dp, LuxTextPrimary)
                    ) {
                        Text("SHARE", fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LuxBg.copy(alpha = 0.95f))
        ) {
            when {
                isLoading -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "rendering")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                        label = "p"
                    )
                    Text(
                        "RENDERING...",
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        letterSpacing = 6.sp,
                        color = LuxAccent,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .alpha(alpha)
                    )
                }
                pdfBitmaps.isEmpty() -> {
                    Text(
                        "PREVIEW NOT AVAILABLE",
                        fontFamily = FontFamily.Monospace,
                        color = androidx.compose.ui.graphics.Color(0xFFD32F2F),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
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
                            }
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        pdfBitmaps.forEachIndexed { index, bitmap ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 12.dp,
                                color = LuxSurface
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
                    }
                }
            }
        }
    }
}
