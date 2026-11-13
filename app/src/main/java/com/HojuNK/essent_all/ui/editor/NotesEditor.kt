package com.HojuNK.essent_all.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.ClipboardHistoryManager
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.model.ClipboardItem
import com.HojuNK.essent_all.ui.components.ColorPaletteBar
import com.HojuNK.essent_all.ui.components.TypewriterTitle
import com.HojuNK.essent_all.ui.components.defaultColorPalette
import com.HojuNK.essent_all.ui.components.htmlToAnnotatedString
import com.HojuNK.essent_all.ui.components.preserveSpansOnTextChange
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun renumberLinesIfNeeded(text: String): String {
    val lines = text.split("\n")
    var hasNumbers = false
    val numberRegex = Regex("""^\d+\.\s+""")

    for (line in lines) {
        if (numberRegex.containsMatchIn(line)) {
            hasNumbers = true
            break
        }
    }

    if (!hasNumbers) return text

    val newLines = mutableListOf<String>()
    var numberCounter = 1

    for (line in lines) {
        if (numberRegex.containsMatchIn(line)) {
            val contentWithoutNumber = line.replace(numberRegex, "")
            newLines.add("${numberCounter++}. $contentWithoutNumber")
        } else {
            numberCounter = 1
            newLines.add(line)
        }
    }

    return newLines.joinToString("\n")
}

fun formatCheckboxesInAnnotatedString(annotatedString: AnnotatedString): AnnotatedString {
    val text = annotatedString.text
    if (!text.contains("☐") && !text.contains("☑")) return annotatedString

    val newSpans = annotatedString.spanStyles.toMutableList()
    var idx = 0
    while (idx < text.length) {
        val ch = text[idx]
        if (ch == '☐' || ch == '☑') {
            newSpans.add(
                AnnotatedString.Range(
                    SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    idx,
                    idx + 1
                )
            )
        }
        idx++
    }
    return AnnotatedString(text = text, spanStyles = newSpans)
}

enum class StructureType {
    BULLET,
    NUMBER,
    CHECKBOX
}

enum class FormatType {
    BOLD, ITALIC, UNDERLINE, COLOR, SIZE, FONT, CLEAR
}

val NdotFontFamily = try {
    val field = R.font::class.java.getField("ndot55")
    val resId = field.getInt(null)
    FontFamily(Font(resId))
} catch (e: Exception) {
    FontFamily.Monospace
}

@Composable
fun AdvancedColorPickerDialog(
    onDismiss: () -> Unit,
    onSelectColor: (Color) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            border = BorderStroke(1.dp, Color(0xFF444444)),
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Палитра цветов",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF555555)),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.background(Color.Black).padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Grayscale row (16 steps from White to Black)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            val graySteps = 16
                            for (i in 0 until graySteps) {
                                val fraction = i / (graySteps - 1f)
                                val v = (255 * (1f - fraction)).toInt()
                                val color = Color(v, v, v)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .background(color)
                                        .clickable {
                                            onSelectColor(color)
                                            onDismiss()
                                        }
                                )
                            }
                        }

                        // Color grid: 8 rows x 16 columns
                        val rows = 8
                        val cols = 16
                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                for (c in 0 until cols) {
                                    val hue = (c.toFloat() / (cols - 1)) * 360f
                                    val sat = 0.85f - (r * 0.05f)
                                    val valo = 0.3f + (r * 0.09f)
                                    val color = Color.hsv(hue, sat.coerceIn(0.1f, 1f), valo.coerceIn(0.2f, 1f))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(color)
                                            .clickable {
                                                onSelectColor(color)
                                                onDismiss()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Закрыть", color = Color(0xFFD700), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SizeSliderBar(
    currentSizeOffset: Int,
    onSizeChanged: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2C),
        border = BorderStroke(1.dp, Color(0xFF444444)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Размер: ${16 + currentSizeOffset}sp", color = Color.White, fontSize = 13.sp)
            IconButton(onClick = { onSizeChanged((currentSizeOffset - 5).coerceAtLeast(-6)) }) {
                Icon(Icons.Default.Remove, "Decrease", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Slider(
                value = currentSizeOffset.toFloat(),
                onValueChange = { onSizeChanged(it.toInt()) },
                valueRange = -6f..24f,
                steps = 5,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFD700), activeTrackColor = Color(0xFFD700)),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onSizeChanged((currentSizeOffset + 5).coerceAtMost(24)) }) {
                Icon(Icons.Default.Add, "Increase", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun FontSelectionBar(
    activeFont: String,
    onSelectFont: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2C),
        border = BorderStroke(1.dp, Color(0xFF444444)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FontOptionItem(title = "Системный", isSelected = activeFont == "system", onClick = { onSelectFont("system") })
            FontOptionItem(title = "Ndot 55", isSelected = activeFont == "ndot55", onClick = { onSelectFont("ndot55") })
        }
    }
}

@Composable
fun FontOptionItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color(0xFF3B3B3B),
        modifier = Modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

fun applyStructureToTextBlock(
    currentTfv: TextFieldValue,
    type: StructureType,
    customStart: Int = currentTfv.selection.start,
    customEnd: Int = currentTfv.selection.end
): TextFieldValue {
    val fullText = currentTfv.text
    if (fullText.isEmpty()) {
        val prefix = when (type) {
            StructureType.BULLET -> "• "
            StructureType.NUMBER -> "1. "
            StructureType.CHECKBOX -> "☐ "
        }
        val newText = prefix
        return TextFieldValue(
            annotatedString = AnnotatedString(newText),
            selection = TextRange(newText.length)
        )
    }

    val lines = fullText.split("\n")
    val lineStarts = IntArray(lines.size)
    val lineEnds = IntArray(lines.size)
    var currPos = 0
    for (i in lines.indices) {
        lineStarts[i] = currPos
        lineEnds[i] = currPos + lines[i].length
        currPos = lineEnds[i] + 1
    }

    val selStart = customStart.coerceIn(0, fullText.length)
    val selEnd = customEnd.coerceIn(0, fullText.length)
    val minSel = minOf(selStart, selEnd)
    val maxSel = maxOf(selStart, selEnd)

    val affectedLineIndices = mutableListOf<Int>()
    for (i in lines.indices) {
        val lStart = lineStarts[i]
        val lEnd = lineEnds[i]
        val intersects = if (minSel == maxSel) {
            minSel in lStart..lEnd
        } else {
            maxSel > lStart && minSel <= lEnd
        }
        if (intersects) {
            affectedLineIndices.add(i)
        }
    }

    if (affectedLineIndices.isEmpty()) return currentTfv

    val bulletRegex = Regex("""^(•|-|\*)\s+""")
    val numberRegex = Regex("""^(\d+\.|\d+\))\s+""")
    val checkboxRegex = Regex("""^(☐|☑|\[\s*\]|\[x\])\s+""")
    val anyPrefixRegex = Regex("""^(•|-|\*|\d+\.|\d+\)|☐|☑|\[\s*\]|\[x\])\s+""")

    val allAlreadyHaveTarget = affectedLineIndices.all { idx ->
        val line = lines[idx]
        when (type) {
            StructureType.BULLET -> bulletRegex.containsMatchIn(line)
            StructureType.NUMBER -> numberRegex.containsMatchIn(line)
            StructureType.CHECKBOX -> checkboxRegex.containsMatchIn(line)
        }
    }

    val newLines = lines.toMutableList()
    var numberCounter = 1

    for (i in lines.indices) {
        if (i in affectedLineIndices) {
            val oldLine = newLines[i]
            val strippedLine = oldLine.replace(anyPrefixRegex, "")

            if (allAlreadyHaveTarget) {
                newLines[i] = strippedLine
            } else {
                val prefix = when (type) {
                    StructureType.BULLET -> "• "
                    StructureType.CHECKBOX -> "☐ "
                    StructureType.NUMBER -> "${numberCounter++}. "
                }
                newLines[i] = prefix + strippedLine
            }
        }
    }

    val newFullText = renumberLinesIfNeeded(newLines.joinToString("\n"))
    val formattedAnnotated = formatCheckboxesInAnnotatedString(
        AnnotatedString(text = newFullText, spanStyles = currentTfv.annotatedString.spanStyles)
    )

    val newTfv = TextFieldValue(
        annotatedString = formattedAnnotated,
        selection = TextRange(newFullText.length)
    )
    return preserveSpansOnTextChange(currentTfv.annotatedString, newTfv)
}

@Composable
fun StructureSelectionBar(
    activeType: StructureType,
    onSelectType: (StructureType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2C),
        border = BorderStroke(1.dp, Color(0xFF444444)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StructureOptionItem(
                title = "Маркеры",
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                isSelected = activeType == StructureType.BULLET,
                onClick = { onSelectType(StructureType.BULLET) }
            )
            StructureOptionItem(
                title = "Номера",
                icon = Icons.Default.FormatListNumbered,
                isSelected = activeType == StructureType.NUMBER,
                onClick = { onSelectType(StructureType.NUMBER) }
            )
            StructureOptionItem(
                title = "Флажки",
                icon = Icons.Default.Checklist,
                isSelected = activeType == StructureType.CHECKBOX,
                onClick = { onSelectType(StructureType.CHECKBOX) }
            )
        }
    }
}

@Composable
fun StructureOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color(0xFF3D3D3D),
        contentColor = if (isSelected) Color.Black else Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}

@Composable
fun ReadingTextBlockContent(
    annotatedString: AnnotatedString,
    onToggleCheckboxAtLine: (Int) -> Unit
) {
    val fullText = annotatedString.text
    if (fullText.isEmpty()) {
        Text(
            text = "Нажмите, чтобы начать редактирование...",
            color = Color.Gray,
            fontSize = 15.sp
        )
        return
    }

    val lines = fullText.split("\n")
    val checkboxRegex = Regex("""^(☐|☑)\s+""")

    var lineStart = 0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEachIndexed { lineIdx, line ->
            val lineEnd = lineStart + line.length
            val lineAnnotated = annotatedString.subSequence(
                lineStart.coerceIn(0, fullText.length),
                lineEnd.coerceIn(0, fullText.length)
            )
            lineStart = lineEnd + 1

            val match = checkboxRegex.find(line)
            if (match != null) {
                val isChecked = match.value.startsWith("☑")
                val prefixLength = match.value.length
                val contentAnnotated = lineAnnotated.subSequence(
                    prefixLength.coerceIn(0, lineAnnotated.length),
                    lineAnnotated.length
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(32.dp)
                            .clickable { onToggleCheckboxAtLine(lineIdx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isChecked) "☑" else "☐",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = contentAnnotated,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            } else {
                Text(
                    text = lineAnnotated.ifEmpty { AnnotatedString(" ") },
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}

fun handleEnterAndAutoStructure(oldTfv: TextFieldValue, newTfv: TextFieldValue): TextFieldValue {
    val oldText = oldTfv.text
    val newText = newTfv.text

    val newSelStart = newTfv.selection.start
    if (newText.length == oldText.length + 1 && newSelStart > 0 && newText[newSelStart - 1] == '\n') {
        val insertPos = newSelStart - 1

        val paraStart = oldText.lastIndexOf('\n', (insertPos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val paraEnd = oldText.indexOf('\n', insertPos).let { if (it == -1) oldText.length else it }

        val prevPara = oldText.substring(paraStart, paraEnd)

        val bulletRegex = Regex("""^[•\-\*]\s+""")
        val numberRegex = Regex("""^(\d+)\.\s+""")
        val checkboxRegex = Regex("""^(☐|☑|\[\s*\]|\[x\])\s+""")

        val bulletMatch = bulletRegex.find(prevPara)
        val numberMatch = numberRegex.find(prevPara)
        val checkboxMatch = checkboxRegex.find(prevPara)

        val match = bulletMatch ?: numberMatch ?: checkboxMatch

        if (match != null) {
            val newPrefix = when {
                bulletMatch != null -> "• "
                checkboxMatch != null -> "☐ "
                numberMatch != null -> {
                    val num = numberMatch.groupValues[1].toIntOrNull() ?: 1
                    "${num + 1}. "
                }
                else -> "• "
            }

            val textBefore = oldText.substring(0, insertPos)
            val textAfter = oldText.substring(insertPos)
            val resultText = textBefore + "\n" + newPrefix + textAfter
            val newCursor = insertPos + 1 + newPrefix.length

            val oldSpans = oldTfv.annotatedString.spanStyles
            val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
            val addedLength = 1 + newPrefix.length

            for (range in oldSpans) {
                var s = range.start
                var e = range.end
                if (s >= insertPos) {
                    s += addedLength
                    e += addedLength
                } else if (e > insertPos) {
                    e += addedLength
                }
                newSpans.add(AnnotatedString.Range(range.item, s.coerceIn(0, resultText.length), e.coerceIn(0, resultText.length)))
            }

            return TextFieldValue(
                annotatedString = AnnotatedString(text = resultText, spanStyles = newSpans),
                selection = TextRange(newCursor.coerceIn(0, resultText.length))
            )
        }
    }
    return newTfv
}

fun isFormatActive(tfv: TextFieldValue, formatType: FormatType): Boolean {
    val start = minOf(tfv.selection.start, tfv.selection.end)
    val end = maxOf(tfv.selection.start, tfv.selection.end)
    val annotated = tfv.annotatedString
    if (annotated.isEmpty()) return false

    val checkIdx = if (start == end) start.coerceAtMost(annotated.length - 1).coerceAtLeast(0) else start
    if (checkIdx >= annotated.length) return false

    for (range in annotated.spanStyles) {
        val inRange = if (start == end) {
            checkIdx in range.start until range.end
        } else {
            range.start < end && range.end > start
        }
        if (inRange) {
            when (formatType) {
                FormatType.BOLD -> if (range.item.fontWeight == FontWeight.Bold || (range.item.fontWeight?.weight ?: 400) >= 700) return true
                FormatType.ITALIC -> if (range.item.fontStyle == FontStyle.Italic) return true
                FormatType.UNDERLINE -> if (range.item.textDecoration == TextDecoration.Underline) return true
                FormatType.COLOR -> if (range.item.color != Color.Unspecified && range.item.color != Color.White) return true
                else -> {}
            }
        }
    }
    return false
}

fun isStructureActive(tfv: TextFieldValue): Boolean {
    val text = tfv.text
    val cursorPos = tfv.selection.start.coerceIn(0, text.length)
    val lines = text.split("\n")
    var charCount = 0
    val prefixRegex = Regex("""^([•\-\*]|\d+\.|\d+\)|☐|☑|\[\s*\]|\[x\])\s+""")
    for (line in lines) {
        val start = charCount
        val end = charCount + line.length
        charCount = end + 1
        if (cursorPos in start..end + 1) {
            return prefixRegex.containsMatchIn(line)
        }
    }
    return false
}

sealed interface NoteBlockItem {
    val id: String

    data class TextBlock(
        override val id: String = UUID.randomUUID().toString(),
        var textFieldValue: TextFieldValue = TextFieldValue("")
    ) : NoteBlockItem

    data class ImageBlock(
        override val id: String = UUID.randomUUID().toString(),
        val path: String,
        val isThumbnail: Boolean = false
    ) : NoteBlockItem
}

sealed interface NoteBlockRenderGroup {
    data class Single(val index: Int, val block: NoteBlockItem) : NoteBlockRenderGroup
    data class ThumbnailRow(val items: List<Pair<Int, NoteBlockItem.ImageBlock>>) : NoteBlockRenderGroup
}

fun groupBlocksForRendering(blocks: List<NoteBlockItem>): List<NoteBlockRenderGroup> {
    val result = mutableListOf<NoteBlockRenderGroup>()
    var currentThumbnails = mutableListOf<Pair<Int, NoteBlockItem.ImageBlock>>()

    blocks.forEachIndexed { index, block ->
        if (block is NoteBlockItem.ImageBlock && block.isThumbnail) {
            currentThumbnails.add(Pair(index, block))
            if (currentThumbnails.size == 5) {
                result.add(NoteBlockRenderGroup.ThumbnailRow(currentThumbnails.toList()))
                currentThumbnails = mutableListOf()
            }
        } else {
            if (currentThumbnails.isNotEmpty()) {
                result.add(NoteBlockRenderGroup.ThumbnailRow(currentThumbnails.toList()))
                currentThumbnails = mutableListOf()
            }
            result.add(NoteBlockRenderGroup.Single(index, block))
        }
    }

    if (currentThumbnails.isNotEmpty()) {
        result.add(NoteBlockRenderGroup.ThumbnailRow(currentThumbnails.toList()))
    }

    return result
}

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
    val imagesDir = File(context.filesDir, "note_images")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val imageFile = File(imagesDir, "img_${UUID.randomUUID()}.png")
    FileOutputStream(imageFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return imageFile.absolutePath
}

fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imagesDir = File(context.filesDir, "note_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val imageFile = File(imagesDir, "img_${UUID.randomUUID()}.png")
        FileOutputStream(imageFile).use { out ->
            inputStream.copyTo(out)
        }
        imageFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun annotatedStringToJson(annotatedString: AnnotatedString): String {
    val obj = JSONObject()
    obj.put("text", annotatedString.text)
    val spansArr = JSONArray()
    for (range in annotatedString.spanStyles) {
        val spanObj = JSONObject()
        spanObj.put("start", range.start)
        spanObj.put("end", range.end)
        val style = range.item
        if (style.fontWeight == FontWeight.Bold || (style.fontWeight?.weight ?: 400) >= 700) {
            spanObj.put("bold", true)
        }
        if (style.fontStyle == FontStyle.Italic) {
            spanObj.put("italic", true)
        }
        if (style.textDecoration?.contains(TextDecoration.Underline) == true) {
            spanObj.put("underline", true)
        }
        if (style.color != Color.Unspecified) {
            spanObj.put("color", String.format(java.util.Locale.US, "#%06X", (0xFFFFFF and style.color.toArgb())))
        }
        spansArr.put(spanObj)
    }
    obj.put("spans", spansArr)
    return obj.toString()
}

fun jsonToAnnotatedString(jsonStr: String): AnnotatedString {
    if (jsonStr.isBlank()) return AnnotatedString("")
    return try {
        if (jsonStr.trim().startsWith("{") && jsonStr.trim().endsWith("}")) {
            val obj = JSONObject(jsonStr)
            val text = obj.optString("text", "")
            val spansArr = obj.optJSONArray("spans") ?: JSONArray()
            val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

            for (i in 0 until spansArr.length()) {
                val spanObj = spansArr.getJSONObject(i)
                val start = spanObj.optInt("start", 0).coerceIn(0, text.length)
                val end = spanObj.optInt("end", 0).coerceIn(0, text.length)

                val isBold = spanObj.optBoolean("bold", false)
                val isItalic = spanObj.optBoolean("italic", false)
                val isUnderline = spanObj.optBoolean("underline", false)
                val colorStr = spanObj.optString("color", "")

                if (start < end) {
                    var spanStyle = SpanStyle()
                    if (isBold) spanStyle = spanStyle.copy(fontWeight = FontWeight.Bold)
                    if (isItalic) spanStyle = spanStyle.copy(fontStyle = FontStyle.Italic)
                    if (isUnderline) spanStyle = spanStyle.copy(textDecoration = TextDecoration.Underline)
                    if (colorStr.isNotEmpty()) {
                        try {
                            spanStyle = spanStyle.copy(color = Color(android.graphics.Color.parseColor(colorStr)))
                        } catch (e: Exception) {}
                    }
                    spans.add(AnnotatedString.Range(spanStyle, start, end))
                }
            }
            AnnotatedString(text = text, spanStyles = spans)
        } else {
            htmlToAnnotatedString(jsonStr)
        }
    } catch (e: Exception) {
        htmlToAnnotatedString(jsonStr)
    }
}

fun mergeAdjacentSpans(spans: List<AnnotatedString.Range<SpanStyle>>): List<AnnotatedString.Range<SpanStyle>> {
    if (spans.size <= 1) return spans
    val sorted = spans.sortedBy { it.start }
    val merged = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    var current = sorted[0]

    for (i in 1 until sorted.size) {
        val next = sorted[i]
        if (current.end == next.start && current.item == next.item) {
            current = AnnotatedString.Range(current.item, current.start, next.end)
        } else {
            merged.add(current)
            current = next
        }
    }
    merged.add(current)
    return merged
}

fun applyStyleToNotesString(
    current: AnnotatedString,
    selectionStart: Int,
    selectionEnd: Int,
    formatType: FormatType,
    colorValue: Color = Color.Unspecified,
    sizeOffset: Int = 0,
    fontFamilyValue: FontFamily? = null
): AnnotatedString {
    val text = current.text
    if (text.isEmpty()) return current

    val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val end = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    if (start >= end) return current

    val existingSpans = current.spanStyles.toMutableList()

    if (formatType == FormatType.CLEAR) {
        val lines = text.split("\n")
        val lineStarts = IntArray(lines.size)
        val lineEnds = IntArray(lines.size)
        var currPos = 0
        for (i in lines.indices) {
            lineStarts[i] = currPos
            lineEnds[i] = currPos + lines[i].length
            currPos = lineEnds[i] + 1
        }

        val prefixRegex = Regex("""^(•|-|\*|\d+\.|\d+\)|☐|☑|\[\s*\]|\[x\])\s+""")
        val newLines = lines.toMutableList()
        val affectedIndices = mutableListOf<Int>()
        for (i in lines.indices) {
            val lStart = lineStarts[i]
            val lEnd = lineEnds[i]
            val intersects = end > lStart && start <= lEnd
            if (intersects) {
                affectedIndices.add(i)
            }
        }

        for (idx in affectedIndices) {
            newLines[idx] = newLines[idx].replace(prefixRegex, "")
        }
        val cleanedText = newLines.joinToString("\n")

        val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        for (range in existingSpans) {
            if (range.end <= start || range.start >= end) {
                newSpans.add(range)
            } else {
                if (range.start < start) {
                    newSpans.add(AnnotatedString.Range(range.item, range.start, start))
                }
                if (range.end > end) {
                    newSpans.add(AnnotatedString.Range(range.item, end, range.end))
                }
            }
        }
        return AnnotatedString(text = cleanedText, spanStyles = newSpans)
    }

    val overlappingSpans = existingSpans.filter { it.start < end && it.end > start }
    val isAlreadyApplied = when (formatType) {
        FormatType.BOLD -> overlappingSpans.any { it.item.fontWeight == FontWeight.Bold }
        FormatType.ITALIC -> overlappingSpans.any { it.item.fontStyle == FontStyle.Italic }
        FormatType.UNDERLINE -> overlappingSpans.any { it.item.textDecoration?.contains(TextDecoration.Underline) == true }
        FormatType.COLOR -> overlappingSpans.any { it.item.color == colorValue }
        FormatType.SIZE -> overlappingSpans.any { it.item.fontSize == (16 + sizeOffset).sp }
        FormatType.FONT -> overlappingSpans.any { it.item.fontFamily == fontFamilyValue }
        else -> false
    }

    val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

    for (range in existingSpans) {
        val rStart = range.start
        val rEnd = range.end
        val style = range.item

        val noOverlap = rEnd <= start || rStart >= end
        if (noOverlap) {
            newSpans.add(range)
        } else {
            if (rStart < start) {
                newSpans.add(AnnotatedString.Range(style, rStart, start))
            }
            if (rEnd > end) {
                newSpans.add(AnnotatedString.Range(style, end, rEnd))
            }

            val overlapStart = maxOf(rStart, start)
            val overlapEnd = minOf(rEnd, end)

            val modifiedStyle = when (formatType) {
                FormatType.BOLD -> if (isAlreadyApplied) style.copy(fontWeight = null) else style.copy(fontWeight = FontWeight.Bold)
                FormatType.ITALIC -> if (isAlreadyApplied) style.copy(fontStyle = null) else style.copy(fontStyle = FontStyle.Italic)
                FormatType.UNDERLINE -> if (isAlreadyApplied) style.copy(textDecoration = null) else style.copy(textDecoration = TextDecoration.Underline)
                FormatType.COLOR -> style.copy(color = colorValue)
                FormatType.SIZE -> style.copy(fontSize = (16 + sizeOffset).sp)
                FormatType.FONT -> style.copy(fontFamily = fontFamilyValue)
                FormatType.CLEAR -> SpanStyle()
            }

            if (hasAnyAttribute(modifiedStyle)) {
                newSpans.add(AnnotatedString.Range(modifiedStyle, overlapStart, overlapEnd))
            }
        }
    }

    if (!isAlreadyApplied && formatType != FormatType.CLEAR) {
        var baseStyle = SpanStyle()
        for (range in overlappingSpans) {
            baseStyle = range.item
            break
        }
        val newStyle = when (formatType) {
            FormatType.BOLD -> baseStyle.copy(fontWeight = FontWeight.Bold)
            FormatType.ITALIC -> baseStyle.copy(fontStyle = FontStyle.Italic)
            FormatType.UNDERLINE -> baseStyle.copy(textDecoration = TextDecoration.Underline)
            FormatType.COLOR -> if (colorValue != Color.Unspecified) baseStyle.copy(color = colorValue) else baseStyle
            FormatType.SIZE -> baseStyle.copy(fontSize = (16 + sizeOffset).sp)
            FormatType.FONT -> if (fontFamilyValue != null) baseStyle.copy(fontFamily = fontFamilyValue) else baseStyle
            FormatType.CLEAR -> null
        }
        if (newStyle != null && hasAnyAttribute(newStyle)) {
            newSpans.removeIf { it.start >= start && it.end <= end }
            newSpans.add(AnnotatedString.Range(newStyle, start, end))
        }
    }

    return AnnotatedString(text = text, spanStyles = mergeAdjacentSpans(newSpans))
}

private fun hasAnyAttribute(style: SpanStyle): Boolean {
    return style.fontWeight != null || style.fontStyle != null || style.textDecoration != null || (style.color != Color.Unspecified) || style.fontSize != TextUnit.Unspecified || style.fontFamily != null
}

fun parseNoteContentToBlocks(content: String): List<NoteBlockItem> {
    val blocks = mutableListOf<NoteBlockItem>()
    if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
        try {
            val rootObj = JSONObject(content)
            val blocksArr = rootObj.optJSONArray("blocks")
            if (blocksArr != null) {
                for (i in 0 until blocksArr.length()) {
                    val blockObj = blocksArr.getJSONObject(i)
                    val type = blockObj.optString("type", "text")
                    if (type == "image") {
                        val path = blockObj.optString("path", "")
                        val isThumb = blockObj.optBoolean("thumb", false)
                        if (path.isNotEmpty()) {
                            blocks.add(NoteBlockItem.ImageBlock(path = path, isThumbnail = isThumb))
                        }
                    } else {
                        val jsonStr = blockObj.optString("content", "")
                        val annotated = jsonToAnnotatedString(jsonStr)
                        blocks.add(NoteBlockItem.TextBlock(textFieldValue = TextFieldValue(annotatedString = annotated)))
                    }
                }
            }
        } catch (e: Exception) {}
    }

    if (blocks.isEmpty()) {
        val regex = Regex("""<img src="file://([^"]+)"(?:\s+data-thumb="(true|false)")?\s*/?>""")
        var lastIndex = 0
        regex.findAll(content).forEach { matchResult ->
            val textBefore = content.substring(lastIndex, matchResult.range.first)
            if (textBefore.isNotBlank()) {
                blocks.add(
                    NoteBlockItem.TextBlock(
                        textFieldValue = TextFieldValue(annotatedString = htmlToAnnotatedString(textBefore))
                    )
                )
            }
            val imgPath = matchResult.groupValues[1]
            val isThumb = matchResult.groupValues.getOrNull(2) == "true"
            if (imgPath.isNotEmpty()) {
                blocks.add(NoteBlockItem.ImageBlock(path = imgPath, isThumbnail = isThumb))
            }
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex < content.length) {
            val remainingText = content.substring(lastIndex)
            if (remainingText.isNotBlank()) {
                blocks.add(
                    NoteBlockItem.TextBlock(
                        textFieldValue = TextFieldValue(annotatedString = htmlToAnnotatedString(remainingText))
                    )
                )
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(NoteBlockItem.TextBlock(textFieldValue = TextFieldValue(annotatedString = htmlToAnnotatedString(content))))
        }
    }
    return blocks
}

fun buildContentFromBlocks(blocks: List<NoteBlockItem>): String {
    val rootObj = JSONObject()
    val blocksArr = JSONArray()
    blocks.forEach { block ->
        val blockObj = JSONObject()
        when (block) {
            is NoteBlockItem.TextBlock -> {
                blockObj.put("type", "text")
                blockObj.put("content", annotatedStringToJson(block.textFieldValue.annotatedString))
            }
            is NoteBlockItem.ImageBlock -> {
                blockObj.put("type", "image")
                blockObj.put("path", block.path)
                blockObj.put("thumb", block.isThumbnail)
            }
        }
        blocksArr.put(blockObj)
    }
    rootObj.put("blocks", blocksArr)
    return rootObj.toString()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NotesEditor(
    noteItem: ClipboardItem? = null,
    isCreatingNew: Boolean = noteItem == null,
    onBack: () -> Unit,
    onNoteSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var isEditingMode by remember { mutableStateOf(isCreatingNew) }
    var activeNoteBlocks by remember { mutableStateOf<List<NoteBlockItem>>(listOf(NoteBlockItem.TextBlock())) }
    var focusedBlockIndex by remember { mutableIntStateOf(0) }
    var lastSelectedRange by remember { mutableStateOf(TextRange.Zero) }

    LaunchedEffect(noteItem, isCreatingNew) {
        if (noteItem != null) {
            activeNoteBlocks = parseNoteContentToBlocks(noteItem.content)
            focusedBlockIndex = 0
            isEditingMode = false
        } else if (isCreatingNew) {
            activeNoteBlocks = listOf(NoteBlockItem.TextBlock())
            focusedBlockIndex = 0
            isEditingMode = true
        }
    }

    val yellowColor = Color(0xFFD700)
    val grayBorder = Color(0xFF333333)

    var activeColor by remember { mutableStateOf<Color>(Color(0xFFFF3B30)) }
    var showColorPalette by remember { mutableStateOf(false) }
    var showStructureMenu by remember { mutableStateOf(false) }
    var activeStructureType by remember { mutableStateOf(StructureType.BULLET) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var currentSizeOffset by remember { mutableIntStateOf(0) }
    var showFontMenu by remember { mutableStateOf(false) }
    var activeFontName by remember { mutableStateOf("system") }
    var showAdvancedColorPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showCanvasDialog by remember { mutableStateOf(false) }
    var zoomImagePath by remember { mutableStateOf<String?>(null) }
    var activeControlsBlockId by remember { mutableStateOf<String?>(null) }
    var fullScreenViewerIndex by remember { mutableIntStateOf(-1) }

    var undoStack by remember { mutableStateOf(listOf<List<NoteBlockItem>>()) }
    var redoStack by remember { mutableStateOf(listOf<List<NoteBlockItem>>()) }

    val saveCurrentNote = {
        val finalContent = buildContentFromBlocks(activeNoteBlocks)
        if (finalContent.isNotBlank()) {
            if (noteItem != null) {
                val updated = noteItem.copy(
                    content = finalContent,
                    timestamp = System.currentTimeMillis()
                )
                ClipboardHistoryManager.updateItem(context, updated)
            } else if (isCreatingNew) {
                ClipboardHistoryManager.addItemAndGetId(context, finalContent, "text", isNote = true)
            }
            keyboardController?.hide()
            Toast.makeText(context, "Заметка сохранена", Toast.LENGTH_SHORT).show()
            onNoteSaved?.invoke() ?: onBack()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyUriToInternalStorage(context, uri)
            if (savedPath != null) {
                activeNoteBlocks = activeNoteBlocks + NoteBlockItem.ImageBlock(path = savedPath)
                Toast.makeText(context, "Фото добавлено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pushUndoState() {
        undoStack = (undoStack + listOf(activeNoteBlocks)).takeLast(30)
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.last()
            redoStack = (redoStack + listOf(activeNoteBlocks)).takeLast(30)
            undoStack = undoStack.dropLast(1)
            activeNoteBlocks = previous
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.last()
            undoStack = (undoStack + listOf(activeNoteBlocks)).takeLast(30)
            redoStack = redoStack.dropLast(1)
            activeNoteBlocks = next
        }
    }

    BackHandler(enabled = true) {
        if (zoomImagePath != null) {
            zoomImagePath = null
        } else if (showCanvasDialog) {
            showCanvasDialog = false
        } else if (showAttachmentMenu) {
            showAttachmentMenu = false
        } else if (isEditingMode && noteItem != null && !isCreatingNew) {
            isEditingMode = false
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    val screenTitle = when {
                        isCreatingNew -> "Создаем заметку"
                        isEditingMode -> "Редактирование"
                        else -> "Заметка"
                    }
                    TypewriterTitle(targetText = screenTitle, animate = false)
                },
                modifier = Modifier
                    .drawBehind {
                        val blurHeight = 45.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Black.copy(alpha = 0.5f)),
                                startY = 0f,
                                endY = size.height
                            ),
                            size = size
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    },
                navigationIcon = {
                    IconButton(onClick = {
                        if (zoomImagePath != null) {
                            zoomImagePath = null
                        } else if (showCanvasDialog) {
                            showCanvasDialog = false
                        } else if (showAttachmentMenu) {
                            showAttachmentMenu = false
                        } else if (isEditingMode && noteItem != null && !isCreatingNew) {
                            isEditingMode = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditingMode || isCreatingNew) {
                        IconButton(onClick = saveCurrentNote) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Save Note",
                                        tint = Color.Black,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            if (isEditingMode || isCreatingNew) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color(0xFF222222),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    val activeTargetBlock = activeNoteBlocks.getOrNull(focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1))
                    val activeTfv = (activeTargetBlock as? NoteBlockItem.TextBlock)?.textFieldValue ?: TextFieldValue("")
                    val isColorActive = remember(activeTfv) { isFormatActive(activeTfv, FormatType.COLOR) }
                    val isItalicActive = remember(activeTfv) { isFormatActive(activeTfv, FormatType.ITALIC) }
                    val isBoldActive = remember(activeTfv) { isFormatActive(activeTfv, FormatType.BOLD) }
                    val isUnderlineActive = remember(activeTfv) { isFormatActive(activeTfv, FormatType.UNDERLINE) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (showAdvancedColorPicker) {
                            AdvancedColorPickerDialog(
                                onDismiss = { showAdvancedColorPicker = false },
                                onSelectColor = { color ->
                                    pushUndoState()
                                    activeColor = color
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.COLOR, color)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                        activeNoteBlocks = mutable
                                    }
                                    showAdvancedColorPicker = false
                                    showColorPalette = false
                                }
                            )
                        }

                        if (showColorPalette) {
                            ColorPaletteBar(
                                palette = defaultColorPalette,
                                activeColor = activeColor,
                                onSelectColor = { paletteItem ->
                                    if (paletteItem.isClose) {
                                        showColorPalette = false
                                    } else {
                                        pushUndoState()
                                        val newColor = paletteItem.color
                                        activeColor = newColor
                                        val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                        val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                        if (targetBlock is NoteBlockItem.TextBlock) {
                                            val currentTfv = targetBlock.textFieldValue
                                            val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                            val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                            val effStart = if (start == end) 0 else start
                                            val effEnd = if (start == end) currentTfv.text.length else end

                                            val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.COLOR, newColor)
                                            val mutable = activeNoteBlocks.toMutableList()
                                            mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                            activeNoteBlocks = mutable
                                        }
                                        showColorPalette = false
                                    }
                                },
                                onOpenAdvancedPalette = {
                                    showAdvancedColorPicker = true
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (showStructureMenu) {
                            StructureSelectionBar(
                                activeType = activeStructureType,
                                onSelectType = { selectedType ->
                                    pushUndoState()
                                    activeStructureType = selectedType
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newTfv = applyStructureToTextBlock(currentTfv, selectedType, effStart, effEnd)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = newTfv)
                                        activeNoteBlocks = mutable
                                    }
                                    showStructureMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (showSizeMenu) {
                            SizeSliderBar(
                                currentSizeOffset = currentSizeOffset,
                                onSizeChanged = { newOffset ->
                                    pushUndoState()
                                    currentSizeOffset = newOffset
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.SIZE, sizeOffset = newOffset)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                        activeNoteBlocks = mutable
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (showFontMenu) {
                            FontSelectionBar(
                                activeFont = activeFontName,
                                onSelectFont = { fontName ->
                                    pushUndoState()
                                    activeFontName = fontName
                                    val fontFamily = if (fontName == "ndot55") NdotFontFamily else null
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.FONT, fontFamilyValue = fontFamily)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                        activeNoteBlocks = mutable
                                    }
                                    showFontMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Кнопка + (Вложения)
                            Surface(
                                onClick = { showAttachmentMenu = true },
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, "Attachment", tint = Color.Black, modifier = Modifier.size(26.dp))
                                }
                            }

                            Surface(
                                onClick = { undo() },
                                shape = CircleShape,
                                color = Color(0xFF333333),
                                enabled = undoStack.isNotEmpty(),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (undoStack.isNotEmpty()) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(
                                onClick = { redo() },
                                shape = CircleShape,
                                color = Color(0xFF333333),
                                enabled = redoStack.isNotEmpty(),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (redoStack.isNotEmpty()) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(
                                modifier = Modifier.size(40.dp).combinedClickable(
                                    onClick = {
                                        pushUndoState()
                                        val colorToApply = if (activeColor == Color.Unspecified) Color(0xFFFF3B30) else activeColor
                                        val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                        val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                        if (targetBlock is NoteBlockItem.TextBlock) {
                                            val currentTfv = targetBlock.textFieldValue
                                            val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                            val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                            val effStart = if (start == end) 0 else start
                                            val effEnd = if (start == end) currentTfv.text.length else end

                                            val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.COLOR, colorToApply)
                                            val mutable = activeNoteBlocks.toMutableList()
                                            mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                            activeNoteBlocks = mutable
                                        }
                                    },
                                    onLongClick = {
                                        showColorPalette = !showColorPalette
                                        showStructureMenu = false
                                    }
                                ),
                                shape = CircleShape,
                                color = if (isColorActive || showColorPalette) Color.White else Color(0xFF333333)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.FormatColorText, "Text Color", tint = if (isColorActive || showColorPalette) Color.Black else if (activeColor != Color.Unspecified) activeColor else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(
                                onClick = {
                                    pushUndoState()
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.ITALIC)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                        activeNoteBlocks = mutable
                                    }
                                },
                                shape = CircleShape,
                                color = if (isItalicActive) Color.White else Color(0xFF333333),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.FormatItalic, "Italic", tint = if (isItalicActive) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(
                                onClick = {
                                    pushUndoState()
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.BOLD)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                        activeNoteBlocks = mutable
                                    }
                                },
                                shape = CircleShape,
                                color = if (isBoldActive) Color.White else Color(0xFF333333),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.FormatBold, "Bold", tint = if (isBoldActive) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(
                                onClick = {
                                    pushUndoState()
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val newAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.UNDERLINE)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = newAnnotated))
                                        activeNoteBlocks = mutable
                                    }
                                },
                                shape = CircleShape,
                                color = if (isUnderlineActive) Color.White else Color(0xFF333333),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.FormatUnderlined, "Underline", tint = if (isUnderlineActive) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            // Кнопка Структурного форматирования (списки/маркеры/флажки)
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .combinedClickable(
                                        onClick = {
                                            pushUndoState()
                                            val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                            val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                            if (targetBlock is NoteBlockItem.TextBlock) {
                                                val currentTfv = targetBlock.textFieldValue
                                                val newTfv = applyStructureToTextBlock(currentTfv, activeStructureType)
                                                val mutable = activeNoteBlocks.toMutableList()
                                                mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = newTfv)
                                                activeNoteBlocks = mutable
                                            }
                                        },
                                        onLongClick = {
                                            showStructureMenu = !showStructureMenu
                                            showColorPalette = false
                                        }
                                    ),
                                shape = CircleShape,
                                color = if (showStructureMenu) yellowColor else Color(0xFF333333)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (activeStructureType) {
                                            StructureType.BULLET -> Icons.AutoMirrored.Filled.FormatListBulleted
                                            StructureType.NUMBER -> Icons.Default.FormatListNumbered
                                            StructureType.CHECKBOX -> Icons.Default.Checklist
                                        },
                                        contentDescription = "Structural Formatting",
                                        tint = if (showStructureMenu) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Size Button
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .combinedClickable(
                                        onClick = {
                                            pushUndoState()
                                            showSizeMenu = !showSizeMenu
                                            showFontMenu = false
                                            showStructureMenu = false
                                            showColorPalette = false
                                        },
                                        onLongClick = {
                                            showSizeMenu = !showSizeMenu
                                            showFontMenu = false
                                            showStructureMenu = false
                                            showColorPalette = false
                                        }
                                    ),
                                shape = CircleShape,
                                color = if (showSizeMenu) yellowColor else Color(0xFF333333)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Text Size",
                                        tint = if (showSizeMenu) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Font Button
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .combinedClickable(
                                        onClick = {
                                            pushUndoState()
                                            showFontMenu = !showFontMenu
                                            showSizeMenu = false
                                            showStructureMenu = false
                                            showColorPalette = false
                                        },
                                        onLongClick = {
                                            showFontMenu = !showFontMenu
                                            showSizeMenu = false
                                            showStructureMenu = false
                                            showColorPalette = false
                                        }
                                    ),
                                shape = CircleShape,
                                color = if (showFontMenu) yellowColor else Color(0xFF333333)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = "Font Family",
                                        tint = if (showFontMenu) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Кнопка Очистить формат (Ластик) - Последняя в списке
                            Surface(
                                onClick = {
                                    pushUndoState()
                                    val targetBlockIndex = focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1)
                                    val targetBlock = activeNoteBlocks.getOrNull(targetBlockIndex)
                                    if (targetBlock is NoteBlockItem.TextBlock) {
                                        val currentTfv = targetBlock.textFieldValue
                                        val start = minOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val end = maxOf(lastSelectedRange.start, lastSelectedRange.end).coerceIn(0, currentTfv.text.length)
                                        val effStart = if (start == end) 0 else start
                                        val effEnd = if (start == end) currentTfv.text.length else end

                                        val cleanedAnnotated = applyStyleToNotesString(currentTfv.annotatedString, effStart, effEnd, FormatType.CLEAR)
                                        val mutable = activeNoteBlocks.toMutableList()
                                        mutable[targetBlockIndex] = targetBlock.copy(textFieldValue = currentTfv.copy(annotatedString = cleanedAnnotated))
                                        activeNoteBlocks = mutable
                                        Toast.makeText(context, "Формат сброшен", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = CircleShape,
                                color = Color(0xFF333333),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.CleaningServices, "Clear Format", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        val topPadding = paddingValues.calculateTopPadding()
        val bottomPadding = paddingValues.calculateBottomPadding()

        val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        LaunchedEffect(focusedBlockIndex, imeBottomPadding > 0.dp) {
            if (isEditingMode && imeBottomPadding > 0.dp && activeNoteBlocks.isNotEmpty()) {
                delay(120)
                listState.animateScrollToItem(focusedBlockIndex.coerceIn(0, activeNoteBlocks.size - 1))
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val renderGroups = remember(activeNoteBlocks) { groupBlocksForRendering(activeNoteBlocks) }
            val allImageBlocks = remember(activeNoteBlocks) { activeNoteBlocks.filterIsInstance<NoteBlockItem.ImageBlock>() }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topPadding + 16.dp,
                    bottom = bottomPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(renderGroups.size, key = { gIdx ->
                    when (val group = renderGroups[gIdx]) {
                        is NoteBlockRenderGroup.Single -> group.block.id
                        is NoteBlockRenderGroup.ThumbnailRow -> group.items.firstOrNull()?.second?.id ?: gIdx.toString()
                    }
                }) { gIdx ->
                    when (val group = renderGroups[gIdx]) {
                        is NoteBlockRenderGroup.Single -> {
                            val idx = group.index
                            when (val block = group.block) {
                                is NoteBlockItem.TextBlock -> {
                                    if (isEditingMode) {
                                        OutlinedTextField(
                                            value = block.textFieldValue,
                                            onValueChange = { newValue ->
                                                var imagePasted = false
                                                try {
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                    val clip = cm?.primaryClip
                                                    if (clip != null && clip.itemCount > 0) {
                                                        val item = clip.getItemAt(0)
                                                        val uri = item.uri
                                                        if (uri != null) {
                                                            val path = copyUriToInternalStorage(context, uri)
                                                            if (path != null) {
                                                                imagePasted = true
                                                                val mutable = activeNoteBlocks.toMutableList()
                                                                mutable.add(idx + 1, NoteBlockItem.ImageBlock(path = path))
                                                                activeNoteBlocks = mutable
                                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                                                                Toast.makeText(context, "Фото вставлено из буфера", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }

                                                if (!imagePasted) {
                                                    val structuredValue = handleEnterAndAutoStructure(block.textFieldValue, newValue)
                                                    val renumberedText = renumberLinesIfNeeded(structuredValue.text)
                                                    val finalTfv = if (renumberedText != structuredValue.text) {
                                                        structuredValue.copy(
                                                            annotatedString = formatCheckboxesInAnnotatedString(
                                                                AnnotatedString(text = renumberedText, spanStyles = structuredValue.annotatedString.spanStyles)
                                                            )
                                                        )
                                                    } else {
                                                        structuredValue.copy(
                                                            annotatedString = formatCheckboxesInAnnotatedString(structuredValue.annotatedString)
                                                        )
                                                    }
                                                    val oldAnnotated = block.textFieldValue.annotatedString
                                                    val newAnnotatedString = if (finalTfv.text == oldAnnotated.text) {
                                                        oldAnnotated
                                                    } else {
                                                        preserveSpansOnTextChange(oldAnnotated, finalTfv).annotatedString
                                                    }
                                                    val updatedValue = TextFieldValue(
                                                        annotatedString = newAnnotatedString,
                                                        selection = newValue.selection
                                                    )
                                                    val mutable = activeNoteBlocks.toMutableList()
                                                    if (idx < mutable.size) {
                                                        mutable[idx] = block.copy(textFieldValue = updatedValue)
                                                        activeNoteBlocks = mutable
                                                        focusedBlockIndex = idx
                                                        lastSelectedRange = updatedValue.selection
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { if (it.isFocused) focusedBlockIndex = idx }
                                                .defaultMinSize(minHeight = 120.dp),
                                            placeholder = { Text("Введите текст области...", color = Color.Gray) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = yellowColor,
                                                unfocusedBorderColor = grayBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = Color.White,
                                                focusedContainerColor = Color(0xFF1A1A1A),
                                                unfocusedContainerColor = Color(0xFF1A1A1A)
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                    } else {
                                        Surface(
                                            onClick = { isEditingMode = true },
                                            shape = RoundedCornerShape(24.dp),
                                            color = Color(0xFF1A1A1A),
                                            border = BorderStroke(1.dp, grayBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(modifier = Modifier.padding(16.dp)) {
                                                ReadingTextBlockContent(
                                                    annotatedString = block.textFieldValue.annotatedString,
                                                    onToggleCheckboxAtLine = { lineIdx ->
                                                        pushUndoState()
                                                        val text = block.textFieldValue.text
                                                        val lines = text.split("\n").toMutableList()
                                                        if (lineIdx in lines.indices) {
                                                            val line = lines[lineIdx]
                                                            if (line.startsWith("☐")) {
                                                                lines[lineIdx] = line.replaceFirst("☐", "☑")
                                                            } else if (line.startsWith("☑")) {
                                                                lines[lineIdx] = line.replaceFirst("☑", "☐")
                                                            }
                                                            val newText = lines.joinToString("\n")
                                                            val newTfv = block.textFieldValue.copy(text = newText)
                                                            val mutable = activeNoteBlocks.toMutableList()
                                                            mutable[idx] = block.copy(textFieldValue = newTfv)
                                                            activeNoteBlocks = mutable
                                                            val finalContent = buildContentFromBlocks(activeNoteBlocks)
                                                            if (noteItem != null) {
                                                                val updated = noteItem.copy(content = finalContent, timestamp = System.currentTimeMillis())
                                                                ClipboardHistoryManager.updateItem(context, updated)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                is NoteBlockItem.ImageBlock -> {
                                    val bitmap = remember(block.path) {
                                        try {
                                            BitmapFactory.decodeFile(block.path)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    val aspectRatio = if (bitmap != null && bitmap.height > 0) {
                                        bitmap.width.toFloat() / bitmap.height.toFloat()
                                    } else 1.77f

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(aspectRatio.coerceIn(0.5f, 3.0f))
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFF1A1A1A))
                                            .border(1.dp, grayBorder, RoundedCornerShape(24.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (activeControlsBlockId != null) {
                                                        activeControlsBlockId = null
                                                    } else {
                                                        val imgIdx = allImageBlocks.indexOfFirst { it.id == block.id }
                                                        if (imgIdx >= 0) fullScreenViewerIndex = imgIdx
                                                    }
                                                },
                                                onLongClick = {
                                                    if (isEditingMode) {
                                                        activeControlsBlockId = if (activeControlsBlockId == block.id) null else block.id
                                                    }
                                                }
                                            )
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Inserted Image",
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Ошибка загрузки картинки", color = Color.Gray)
                                            }
                                        }

                                        if (isEditingMode && activeControlsBlockId == block.id) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                                    .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (idx > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            val mutable = activeNoteBlocks.toMutableList()
                                                            val item = mutable.removeAt(idx)
                                                            mutable.add(idx - 1, item)
                                                            activeNoteBlocks = mutable
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                if (idx < activeNoteBlocks.size - 1) {
                                                    IconButton(
                                                        onClick = {
                                                            val mutable = activeNoteBlocks.toMutableList()
                                                            val item = mutable.removeAt(idx)
                                                            mutable.add(idx + 1, item)
                                                            activeNoteBlocks = mutable
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val mutable = activeNoteBlocks.toMutableList()
                                                        mutable[idx] = block.copy(isThumbnail = true)
                                                        activeNoteBlocks = mutable
                                                        activeControlsBlockId = null
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.CropSquare, contentDescription = "Thumbnail Size", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val mutable = activeNoteBlocks.toMutableList()
                                                        mutable.removeAt(idx)
                                                        activeNoteBlocks = mutable
                                                        activeControlsBlockId = null
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Image", tint = Color(0xFFFF5555), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is NoteBlockRenderGroup.ThumbnailRow -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.items.forEach { (idx, block) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1A1A1A))
                                            .border(1.dp, grayBorder, RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (activeControlsBlockId != null) {
                                                        activeControlsBlockId = null
                                                    } else {
                                                        val imgIdx = allImageBlocks.indexOfFirst { it.id == block.id }
                                                        if (imgIdx >= 0) fullScreenViewerIndex = imgIdx
                                                    }
                                                },
                                                onLongClick = {
                                                    if (isEditingMode) {
                                                        activeControlsBlockId = if (activeControlsBlockId == block.id) null else block.id
                                                    }
                                                }
                                            )
                                    ) {
                                        val bitmap = remember(block.path) {
                                            try { BitmapFactory.decodeFile(block.path) } catch (e: Exception) { null }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Thumbnail Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text("?", color = Color.Gray)
                                            }
                                        }

                                        if (isEditingMode && activeControlsBlockId == block.id) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.85f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    Row {
                                                        if (idx > 0) {
                                                            IconButton(onClick = {
                                                                val mutable = activeNoteBlocks.toMutableList()
                                                                val item = mutable.removeAt(idx)
                                                                mutable.add(idx - 1, item)
                                                                activeNoteBlocks = mutable
                                                            }, modifier = Modifier.size(28.dp)) {
                                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                        if (idx < activeNoteBlocks.size - 1) {
                                                            IconButton(onClick = {
                                                                val mutable = activeNoteBlocks.toMutableList()
                                                                val item = mutable.removeAt(idx)
                                                                mutable.add(idx + 1, item)
                                                                activeNoteBlocks = mutable
                                                            }, modifier = Modifier.size(28.dp)) {
                                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                            }
                                                        }
                                                    }
                                                    Row {
                                                        IconButton(onClick = {
                                                            val mutable = activeNoteBlocks.toMutableList()
                                                            mutable[idx] = block.copy(isThumbnail = false)
                                                            activeNoteBlocks = mutable
                                                            activeControlsBlockId = null
                                                        }, modifier = Modifier.size(28.dp)) {
                                                            Icon(Icons.Default.AspectRatio, "Large", tint = Color.White, modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(onClick = {
                                                            val mutable = activeNoteBlocks.toMutableList()
                                                            mutable.removeAt(idx)
                                                            activeNoteBlocks = mutable
                                                            activeControlsBlockId = null
                                                        }, modifier = Modifier.size(28.dp)) {
                                                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF5555), modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                repeat(5 - group.items.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Всплывающее меню вложений
    if (showAttachmentMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentMenu = false },
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Добавить вложение", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                ListItem(
                    headlineContent = { Text("Область", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.CropSquare, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        showAttachmentMenu = false
                        activeNoteBlocks = activeNoteBlocks + NoteBlockItem.TextBlock()
                        Toast.makeText(context, "Текстовая область добавлена", Toast.LENGTH_SHORT).show()
                    }
                )
                ListItem(
                    headlineContent = { Text("Фото", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        showAttachmentMenu = false
                        photoPickerLauncher.launch("image/*")
                    }
                )
                ListItem(
                    headlineContent = { Text("Рисунок", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Brush, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        showAttachmentMenu = false
                        showCanvasDialog = true
                    }
                )
                ListItem(
                    headlineContent = { Text("Голосовое", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        showAttachmentMenu = false
                        Toast.makeText(context, "Функция в разработке", Toast.LENGTH_SHORT).show()
                    }
                )
                ListItem(
                    headlineContent = { Text("Файл", color = Color.White) },
                    leadingContent = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.clickable {
                        showAttachmentMenu = false
                        Toast.makeText(context, "Функция в разработке", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Экран рисования (Холст)
    if (showCanvasDialog) {
        DrawingCanvasDialog(
            onDismiss = { showCanvasDialog = false },
            onSaveImage = { bitmap ->
                val savedPath = saveBitmapToInternalStorage(context, bitmap)
                activeNoteBlocks = activeNoteBlocks + NoteBlockItem.ImageBlock(path = savedPath)
                showCanvasDialog = false
                Toast.makeText(context, "Рисунок добавлен", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Полноэкранный просмотрщик изображений с пролистыванием и приближением
    if (fullScreenViewerIndex >= 0) {
        val allImages = activeNoteBlocks.filterIsInstance<NoteBlockItem.ImageBlock>()
        ImageZoomViewerDialog(
            imagePaths = allImages.map { it.path },
            initialIndex = fullScreenViewerIndex,
            onDismiss = { fullScreenViewerIndex = -1 }
        )
    }
}

data class DrawPathData(
    val path: androidx.compose.ui.graphics.Path,
    val color: Color,
    val strokeWidth: Float,
    val isMarkerTool: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasDialog(
    onDismiss: () -> Unit,
    onSaveImage: (Bitmap) -> Unit
) {
    var paths by remember { mutableStateOf(listOf<DrawPathData>()) }
    var redoStack by remember { mutableStateOf(listOf<DrawPathData>()) }

    var currentPath by remember { mutableStateOf<androidx.compose.ui.graphics.Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.White) }
    var isMarkerTool by remember { mutableStateOf(false) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    val colors = listOf(
        Color.White, Color(0xFFFFD700), Color(0xFFFF3B30),
        Color(0xFF34C759), Color(0xFF007AFF), Color(0xFFAF52DE)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text("Холст", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (paths.isNotEmpty()) {
                                    redoStack = (redoStack + paths.last()).takeLast(30)
                                    paths = paths.dropLast(1)
                                }
                            },
                            enabled = paths.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (paths.isNotEmpty()) Color.White else Color.Gray)
                        }

                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val last = redoStack.last()
                                    paths = paths + last
                                    redoStack = redoStack.dropLast(1)
                                }
                            },
                            enabled = redoStack.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) Color.White else Color.Gray)
                        }

                        Button(
                            onClick = {
                                val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                canvas.drawColor(android.graphics.Color.BLACK)

                                val paint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                }

                                paths.forEach { pathData ->
                                    paint.color = pathData.color.toArgb()
                                    paint.strokeWidth = pathData.strokeWidth * 2.5f
                                    paint.alpha = if (pathData.isMarkerTool) 100 else 255
                                    canvas.drawPath(pathData.path.asAndroidPath(), paint)
                                }

                                onSaveImage(bitmap)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Добавить", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFF121212))
                        .border(1.dp, Color(0xFF333333))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val newPath = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(offset.x, offset.y)
                                    }
                                    currentPath = newPath
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    currentPath?.let { p ->
                                        paths = paths + DrawPathData(
                                            path = p,
                                            color = currentColor,
                                            strokeWidth = strokeWidth,
                                            isMarkerTool = isMarkerTool
                                        )
                                        redoStack = emptyList()
                                    }
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { pathData ->
                            drawPath(
                                path = pathData.path,
                                color = pathData.color.copy(alpha = if (pathData.isMarkerTool) 0.4f else 1f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = pathData.strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                        currentPath?.let { p ->
                            drawPath(
                                path = p,
                                color = currentColor.copy(alpha = if (isMarkerTool) 0.4f else 1f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isMarkerTool,
                            onClick = { isMarkerTool = false; strokeWidth = 6f },
                            label = { Text("Ручка") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        FilterChip(
                            selected = isMarkerTool,
                            onClick = { isMarkerTool = true; strokeWidth = 22f },
                            label = { Text("Маркер") },
                            leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        colors.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (currentColor == col) 2.dp else 0.dp, Color.Yellow, CircleShape)
                                    .clickable { currentColor = col }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageZoomViewerDialog(
    imagePaths: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (imagePaths.isEmpty()) {
        onDismiss()
        return
    }

    val startIndex = initialIndex.coerceIn(0, imagePaths.size - 1)
    val pagerState = rememberPagerState(initialPage = startIndex) { imagePaths.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(10f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val path = imagePaths.getOrNull(page) ?: ""
                var scale by remember(page) { mutableFloatStateOf(1f) }
                var offset by remember(page) { mutableStateOf(Offset.Zero) }

                val bitmap = remember(path) {
                    try {
                        BitmapFactory.decodeFile(path)
                    } catch (e: Exception) {
                        null
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Zoomed Image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(scale > 1.05f) {
                                    if (scale > 1.05f) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            if (scale > 1f) {
                                                offset += pan
                                            } else {
                                                offset = Offset.Zero
                                            }
                                        }
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    } else {
                        Text("Не удалось загрузить изображение", color = Color.Gray)
                    }
                }
            }
        }
    }
}
