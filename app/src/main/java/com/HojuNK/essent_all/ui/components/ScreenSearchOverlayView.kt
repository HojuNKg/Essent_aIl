package com.HojuNK.essent_all.ui.components

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HojuNK.essent_all.ui.components.*
import com.HojuNK.essent_all.util.ScreenTextItem
import com.HojuNK.essent_all.util.ScreenTranslatorHelper
import com.HojuNK.essent_all.util.SupportedLanguage
import com.HojuNK.essent_all.util.TranslationLanguages
import com.HojuNK.essent_all.util.cropBitmap
import com.HojuNK.essent_all.util.searchImageWithGoogleLens

@Composable
fun ScreenSearchOverlayContent(
    screenshot: Bitmap?,
    textItems: List<ScreenTextItem>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf<ScreenTextItem?>(null) }
    var translatedText by remember { mutableStateOf<String?>(null) }
    var detectedSourceLangTag by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    var targetLanguage by remember {
        mutableStateOf(TranslationLanguages.getSavedTargetLanguage(context))
    }
    var showLanguagePicker by remember { mutableStateOf(false) }

    val translatorHelper = remember { ScreenTranslatorHelper() }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialized
            }
        }
        ttsEngine = tts
        onDispose {
            translatorHelper.close()
            ttsEngine?.stop()
            ttsEngine?.shutdown()
        }
    }

    val accentColor = Color.White

    fun performTranslation(text: String, target: SupportedLanguage) {
        isTranslating = true
        translatedText = null
        translatorHelper.translateWithAutoDetect(text, target.mlKitCode) { sourceTag, result ->
            detectedSourceLangTag = sourceTag
            translatedText = result
            isTranslating = false
        }
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val rotation = try {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay?.rotation ?: Surface.ROTATION_0
    } catch (e: Exception) {
        Surface.ROTATION_0
    }

    val contentScale = if (isPortrait) ContentScale.FillWidth else ContentScale.FillHeight
    val alignment = when {
        isPortrait -> Alignment.BottomCenter
        rotation == Surface.ROTATION_270 -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }

        var imgX = 0f
        var imgY = 0f
        var scaleX = 1f
        var scaleY = 1f

        if (screenshot != null && screenshot.width > 0 && screenshot.height > 0) {
            val bmpW = screenshot.width.toFloat()
            val bmpH = screenshot.height.toFloat()

            if (isPortrait) {
                val scale = containerW / bmpW
                scaleX = scale
                scaleY = scale
                val scaledH = bmpH * scale
                imgX = 0f
                imgY = containerH - scaledH
            } else {
                val scale = containerH / bmpH
                scaleX = scale
                scaleY = scale
                val scaledW = bmpW * scale
                imgY = 0f
                imgX = if (rotation == Surface.ROTATION_270) {
                    containerW - scaledW
                } else {
                    0f
                }
            }
        }

        // Screenshot background
        screenshot?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                alignment = alignment,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay canvas & tap detection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(textItems, imgX, imgY, scaleX, scaleY) {
                    detectTapGestures { tapOffset ->
                        val tapped = textItems.find { item ->
                            val r = item.boundingBox
                            val left = imgX + r.left * scaleX
                            val right = imgX + r.right * scaleX
                            val top = imgY + r.top * scaleY
                            val bottom = imgY + r.bottom * scaleY

                            tapOffset.x >= left && tapOffset.x <= right &&
                                    tapOffset.y >= top && tapOffset.y <= bottom
                        }
                        selectedItem = tapped
                        translatedText = null
                        detectedSourceLangTag = null
                        isTranslating = false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                textItems.forEach { item ->
                    val r = item.boundingBox
                    val isSelected = selectedItem?.id == item.id
                    val strokeWidth = if (isSelected) 3.5.dp.toPx() else 1.5.dp.toPx()
                    val fillColor = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                    val strokeColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)

                    val drawLeft = imgX + r.left * scaleX
                    val drawTop = imgY + r.top * scaleY
                    val drawW = r.width() * scaleX
                    val drawH = r.height() * scaleY

                    drawRect(
                        color = fillColor,
                        topLeft = Offset(drawLeft, drawTop),
                        size = Size(drawW, drawH)
                    )
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(drawLeft, drawTop),
                        size = Size(drawW, drawH),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }

        // Top Header Bar
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ПОИСК ПО ЭКРАНУ",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Найдено текстовых блоков: ${textItems.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (screenshot != null) {
                                val bitmapToSearch = selectedItem?.let { item ->
                                    cropBitmap(screenshot, item.boundingBox)
                                } ?: screenshot
                                searchImageWithGoogleLens(context, bitmapToSearch)
                                onClose()
                            } else {
                                Toast.makeText(context, "Скриншот недоступен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Search Image (Lens)",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Card for Selected Text
        selectedItem?.let { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    color = Color(0xFF1B1B1B),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header row of Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "РАСПОЗНАННЫЙ ТЕКСТ",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            IconButton(
                                onClick = { selectedItem = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Deselect",
                                    tint = Color.Gray
                                )
                            }
                        }

                        // Text Content
                        Text(
                            text = item.text,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Language selector pill & status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.clickable { showLanguagePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Перевод на: ${targetLanguage.name}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Translated Text display
                        if (isTranslating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("Определение языка и перевод...", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else if (translatedText != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val langHeader = if (detectedSourceLangTag != null) {
                                        "ПЕРЕВОД ($detectedSourceLangTag ➔ ${targetLanguage.code.uppercase()})"
                                    } else {
                                        "ПЕРЕВОД (${targetLanguage.code.uppercase()})"
                                    }
                                    Text(
                                        text = langHeader,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = translatedText!!,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Translate button
                            Button(
                                onClick = {
                                    performTranslation(item.text, targetLanguage)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Перевести", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(Modifier.width(8.dp))

                            // Copy button
                            IconButton(
                                onClick = {
                                    val textToCopy = translatedText ?: item.text
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Recognized Text", textToCopy)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Color.White
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Web Search button (Auto closes overlay so browser is visible)
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                            putExtra(SearchManager.QUERY, item.text)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                        onClose()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Не удалось открыть поиск", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Image Search (Google Lens) button
                            IconButton(
                                onClick = {
                                    if (screenshot != null) {
                                        val cropped = cropBitmap(screenshot, item.boundingBox)
                                        searchImageWithGoogleLens(context, cropped)
                                        onClose()
                                    }
                                },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Search Image",
                                    tint = Color.White
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            // Speak / TTS button
                            IconButton(
                                onClick = {
                                    val textToSpeak = translatedText ?: item.text
                                    ttsEngine?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "screen_search_tts")
                                },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Language Selector Dialog
        if (showLanguagePicker) {
            AlertDialog(
                onDismissRequest = { showLanguagePicker = false },
                containerColor = Color(0xFF222222),
                title = {
                    Text(
                        text = "Выберите язык перевода",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(TranslationLanguages.ALL) { lang ->
                            val isSelected = lang.code == targetLanguage.code
                            Surface(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        targetLanguage = lang
                                        TranslationLanguages.saveTargetLanguage(context, lang.code)
                                        showLanguagePicker = false
                                        selectedItem?.let { item ->
                                            performTranslation(item.text, lang)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = lang.name,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguagePicker = false }) {
                        Text("Отмена", color = Color.White)
                    }
                }
            )
        }
    }
}
