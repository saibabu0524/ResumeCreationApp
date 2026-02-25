package com.softsuave.resumecreationapp.feature.resume.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.softsuave.resumecreationapp.core.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ResultScreen(
    pdfBytes: ByteArray,
    onStartOver: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tempPdfFile = remember {
        File(context.cacheDir, "preview_resume.pdf").apply { writeBytes(pdfBytes) }
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
                val fd = ParcelFileDescriptor.open(tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val bitmaps = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    AndroidCanvas(bmp).drawColor(AndroidColor.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bmp)
                    page.close()
                }
                renderer.close()
                fd.close()
                pdfBitmaps = bitmaps
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

            // ── Custom Top Bar ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onStartOver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "THE DOCUMENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.7f)
                    )
                    if (pdfBitmaps.isNotEmpty()) {
                        Text(
                            "${pdfBitmaps.size} page${if (pdfBitmaps.size > 1) "s" else ""}  ·  pinch to zoom",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextMuted.copy(0.6f)
                        )
                    }
                }
                // Spacer for symmetry
                Box(Modifier.size(48.dp))
            }

            HorizontalDivider(color = Border, thickness = 0.5.dp)

            // ── PDF Viewer ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0A0908))
            ) {
                when {
                    isLoading -> RenderingIndicator()
                    pdfBitmaps.isEmpty() -> {
                        Text(
                            "PREVIEW NOT AVAILABLE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = ErrorRed,
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
                                        offset = if (scale > 1f) offset + pan else Offset.Zero
                                    }
                                }
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            pdfBitmaps.forEachIndexed { index, bitmap ->
                                // Page wrapper with editorial frame
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, BorderMid, RoundedCornerShape(1.dp))
                                ) {
                                    // Page number label
                                    Text(
                                        "P.${index + 1}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = Amber.copy(0.4f),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    )
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
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = Border, thickness = 0.5.dp)

            // ── Bottom Bar ────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save status
                AnimatedVisibility(
                    visible = saveStatus != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    saveStatus?.let { status ->
                        val isSuccess = status.startsWith("Saved")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isSuccess) Color(0xFF0F1F14) else Color(0xFF1F0F0D))
                                .border(
                                    0.5.dp,
                                    if (isSuccess) SuccessGreen.copy(0.4f) else ErrorRed.copy(0.4f),
                                    RoundedCornerShape(1.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (isSuccess) "✓" else "✗",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (isSuccess) SuccessGreen else ErrorRed
                            )
                            Text(
                                status.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = if (isSuccess) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Save button
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                saveStatus = null
                                withContext(Dispatchers.IO) {
                                    try {
                                        val fileName = "Tailored_Resume_${System.currentTimeMillis()}.pdf"
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val cv = ContentValues().apply {
                                                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                                                put(MediaStore.Downloads.IS_PENDING, 1)
                                            }
                                            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                                            if (uri != null) {
                                                context.contentResolver.openOutputStream(uri)?.use { it.write(pdfBytes) }
                                                cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                                                context.contentResolver.update(uri, cv, null, null)
                                                saveStatus = "Saved to Downloads"
                                            } else saveStatus = "Failed to create file"
                                        } else {
                                            val dir = context.getExternalFilesDir(null) ?: context.filesDir
                                            FileOutputStream(File(dir, fileName)).use { it.write(pdfBytes) }
                                            saveStatus = "Saved successfully"
                                        }
                                    } catch (e: Exception) {
                                        saveStatus = "Failed: ${e.message}"
                                    }
                                }
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = !isSaving && pdfBitmaps.isNotEmpty(),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber,
                            disabledContainerColor = SurfaceHigh,
                            contentColor = Canvas,
                            disabledContentColor = TextMuted
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = Canvas, strokeWidth = 1.5.dp)
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (isSaving) "SAVING..." else "SAVE PDF",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Share button
                    OutlinedButton(
                        onClick = {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempPdfFile)
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }, "Share Resume"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = pdfBitmaps.isNotEmpty(),
                        shape = RoundedCornerShape(2.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (pdfBitmaps.isNotEmpty()) Amber.copy(0.5f) else BorderMid),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "SHARE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Start over
                TextButton(
                    onClick = onStartOver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "← START OVER",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderingIndicator() {
    val rot by rememberInfiniteTransition(label = "renderRot").animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "rr"
    )
    val alpha by rememberInfiniteTransition(label = "alpha").animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "a"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .drawBehind {
                    rotate(rot) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color.Transparent, Amber.copy(0.4f), Amber)),
                            startAngle = 0f, sweepAngle = 260f, useCenter = false,
                            style = Stroke(4f, cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFF2A2218), Color(0xFF111109))),
                        radius = size.minDimension * 0.32f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "PDF",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = Amber.copy(alpha)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "RENDERING",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 4.sp,
            color = Amber.copy(alpha)
        )
    }
}
