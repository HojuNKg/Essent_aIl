package com.HojuNK.essent_all.ui.components
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.HojuNK.essent_all.ui.editor.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.HojuNK.essent_all.ClipboardHistoryManager
import com.HojuNK.essent_all.ClipboardItem
import com.HojuNK.essent_all.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val NdotFontFamily = try {
    val field = R.font::class.java.getField("ndot55")
    val resId = field.getInt(null)
    FontFamily(Font(resId))
} catch (e: Exception) {
    FontFamily.Monospace
}

enum class ClipboardOverlayState {
    MINIMAL,   // ~100.dp (only handle bar + bottom buttons)
    COLLAPSED, // ~320.dp (standard keyboard size)
    EXPANDED   // ~90% screen height
}

enum class FormatType {
    BOLD, ITALIC, UNDERLINE, COLOR, SIZE, FONT, CLEAR
}

enum class NoteFilter(val title: String) {
    ALL("Все"),
    CLIPBOARD("Буфер"),
    NOTES("Заметки"),
    FAVORITES("Избранное"),
    LINKS("Ссылки"),
    FORMATTED("С форматированием"),
    PLAIN_TEXT("Чистый текст"),
    SHORT("Короткие"),
    LONG("Длинные")
}

fun hasLink(text: String): Boolean {
    val urlRegex = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
    return urlRegex.containsMatchIn(text)
}

fun hasFormatting(text: String): Boolean {
    val formatRegex = Regex("""<[^>]+>|•|☐|☑|\[\s*\]|\[x\]|^\d+\.\s""", RegexOption.MULTILINE)
    return formatRegex.containsMatchIn(text)
}

fun stripHtmlAndStructures(text: String): String {
    if (text.trim().startsWith("{") && text.trim().endsWith("}")) {
        try {
            val rootObj = org.json.JSONObject(text)
            val blocksArr = rootObj.optJSONArray("blocks")
            if (blocksArr != null) {
                val sb = StringBuilder()
                for (i in 0 until blocksArr.length()) {
                    val blockObj = blocksArr.getJSONObject(i)
                    val type = blockObj.optString("type", "text")
                    if (type == "text") {
                        val jsonStr = blockObj.optString("content", "")
                        val plainText = try {
                            val obj = org.json.JSONObject(jsonStr)
                            obj.optString("text", "")
                        } catch (e: Exception) {
                            jsonStr.replace(Regex("""<[^>]+>"""), "")
                        }
                        if (plainText.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n")
                            sb.append(plainText)
                        }
                    } else if (type == "image") {
                        if (sb.isNotEmpty()) sb.append("\n")
                        sb.append("[Изображение]")
                    }
                }
                return sb.toString()
            }
        } catch (e: Exception) {}
    }
    return text.replace(Regex("""<[^>]+>"""), "")
}

data class ColorPaletteItem(
    val name: String,
    val color: Color,
    val isClear: Boolean = false
)

val defaultColorPalette = listOf(
    ColorPaletteItem("Без цвета", Color.Unspecified, isClear = true),
    ColorPaletteItem("Белый", Color.White),
    ColorPaletteItem("Черный", Color.Black),
    ColorPaletteItem("Красный", Color(0xFFFF3B30)),
    ColorPaletteItem("Оранжевый", Color(0xFFFF9500)),
    ColorPaletteItem("Желтый", Color(0xFFFFCC00)),
    ColorPaletteItem("Зеленый", Color(0xFF34C759)),
    ColorPaletteItem("Голубой", Color(0xFF007AFF)),
    ColorPaletteItem("Фиолетовый", Color(0xFFAF52DE)),
    ColorPaletteItem("Розовый", Color(0xFFFF2D55))
)

fun htmlToAnnotatedString(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")
    if (html.trim().startsWith("{") && html.trim().endsWith("}")) {
        return try {
            val rootObj = JSONObject(html)
            val blocksArr = rootObj.optJSONArray("blocks")
            if (blocksArr != null) {
                val sb = StringBuilder()
                val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
                for (i in 0 until blocksArr.length()) {
                    val blockObj = blocksArr.getJSONObject(i)
                    val type = blockObj.optString("type", "text")
                    if (type == "text") {
                        val jsonStr = blockObj.optString("content", "")
                        val annotated = jsonToAnnotatedStringInternal(jsonStr)
                        val startOffset = sb.length
                        sb.append(annotated.text)
                        for (span in annotated.spanStyles) {
                            spans.add(AnnotatedString.Range(span.item, span.start + startOffset, span.end + startOffset))
                        }
                        if (i < blocksArr.length() - 1) {
                            sb.append("\n")
                        }
                    } else if (type == "image") {
                        val startOffset = sb.length
                        sb.append("[Изображение]")
                        spans.add(AnnotatedString.Range(SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic), startOffset, sb.length))
                        if (i < blocksArr.length() - 1) {
                            sb.append("\n")
                        }
                    }
                }
                AnnotatedString(text = sb.toString(), spanStyles = spans)
            } else {
                jsonToAnnotatedStringInternal(html)
            }
        } catch (e: Exception) {
            jsonToAnnotatedStringInternal(html)
        }
    }
    return try {
        val spanned = if (html.contains("<") && html.contains(">")) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else null
        if (spanned != null) {
            buildAnnotatedString {
                val rawText = spanned.toString()
                append(rawText)
                spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                    val start = spanned.getSpanStart(span).coerceIn(0, rawText.length)
                    val end = spanned.getSpanEnd(span).coerceIn(0, rawText.length)
                    if (start < end) {
                        when (span) {
                            is StyleSpan -> {
                                when (span.style) {
                                    Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                                    Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                                    Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                                }
                            }
                            is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                            is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                            is AbsoluteSizeSpan -> addStyle(SpanStyle(fontSize = span.size.sp), start, end)
                            is TypefaceSpan -> {
                                if (span.family == "monospace" || span.family == "ndot55") {
                                    addStyle(SpanStyle(fontFamily = NdotFontFamily), start, end)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            AnnotatedString(html)
        }
    } catch (e: Exception) {
        AnnotatedString(html)
    }
}

private fun jsonToAnnotatedStringInternal(jsonStr: String): AnnotatedString {
    if (jsonStr.isBlank()) return AnnotatedString("")
    return try {
        val trimmed = jsonStr.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val obj = JSONObject(trimmed)
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
                val fontSizeVal = spanObj.optDouble("fontSize", -1.0)
                val fontFamilyStr = spanObj.optString("fontFamily", "")

                if (start < end) {
                    var spanStyle = SpanStyle()
                    if (isBold) spanStyle = spanStyle.copy(fontWeight = FontWeight.Bold)
                    if (isItalic) spanStyle = spanStyle.copy(fontStyle = FontStyle.Italic)
                    if (isUnderline) spanStyle = spanStyle.copy(textDecoration = TextDecoration.Underline)

                    val colorObj = spanObj.opt("color")
                    if (colorObj != null) {
                        try {
                            val colorVal = when (colorObj) {
                                is Number -> Color(colorObj.toLong().toULong())
                                is String -> {
                                    if (colorObj.startsWith("#")) Color(android.graphics.Color.parseColor(colorObj))
                                    else colorObj.toLongOrNull()?.let { Color(it.toULong()) } ?: Color.Unspecified
                                }
                                else -> Color.Unspecified
                            }
                            if (colorVal != Color.Unspecified) {
                                spanStyle = spanStyle.copy(color = colorVal)
                            }
                        } catch (e: Exception) {}
                    }

                    if (fontSizeVal > 0) {
                        spanStyle = spanStyle.copy(fontSize = fontSizeVal.toFloat().sp)
                    }
                    if (fontFamilyStr.isNotEmpty()) {
                        val fontFamily = FontManager.getFontFamilyById(fontFamilyStr)
                        if (fontFamily != null) {
                            spanStyle = spanStyle.copy(fontFamily = fontFamily)
                        }
                    }

                    spans.add(AnnotatedString.Range(spanStyle, start, end))
                }
            }
            AnnotatedString(text = text, spanStyles = spans)
        } else {
            AnnotatedString(jsonStr)
        }
    } catch (e: Exception) {
        try {
            val obj = JSONObject(jsonStr)
            AnnotatedString(obj.optString("text", jsonStr))
        } catch (e2: Exception) {
            AnnotatedString(jsonStr.replace(Regex("""<[^>]+>"""), ""))
        }
    }
}

fun annotatedStringToHtml(annotatedString: AnnotatedString): String {
    val text = annotatedString.text
    if (text.isEmpty()) return ""
    if (annotatedString.spanStyles.isEmpty()) return text

    return try {
        val ssb = SpannableStringBuilder(text)
        for (range in annotatedString.spanStyles) {
            val start = range.start.coerceIn(0, text.length)
            val end = range.end.coerceIn(0, text.length)
            if (start >= end) continue
            val style = range.item

            if (style.fontWeight == FontWeight.Bold && style.fontStyle == FontStyle.Italic) {
                ssb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (style.fontWeight == FontWeight.Bold) {
                ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (style.fontStyle == FontStyle.Italic) {
                ssb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (style.textDecoration?.contains(TextDecoration.Underline) == true) {
                ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (style.color != Color.Unspecified) {
                val colorInt = style.color.toArgb()
                ssb.setSpan(ForegroundColorSpan(colorInt), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (style.fontSize != TextUnit.Unspecified) {
                ssb.setSpan(AbsoluteSizeSpan(style.fontSize.value.toInt(), true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (style.fontFamily == NdotFontFamily) {
                ssb.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val rawHtml = Html.toHtml(ssb)
        cleanHtml(rawHtml)
    } catch (e: Exception) {
        text
    }
}

private fun cleanHtml(html: String): String {
    var result = html.trim()
    if (result.startsWith("<p dir=\"ltr\">") && result.endsWith("</p>")) {
        result = result.substring("<p dir=\"ltr\">".length, result.length - "</p>".length).trim()
    } else if (result.startsWith("<p>") && result.endsWith("</p>")) {
        result = result.substring("<p>".length, result.length - "</p>".length).trim()
    }
    return result
}

@Composable
fun BlinkingCaretAtRect(rect: Rect) {
    val infiniteTransition = rememberInfiniteTransition(label = "caret_blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caret_alpha"
    )
    val caretHeightDp = with(LocalDensity.current) { rect.height.toDp() }.coerceAtLeast(18.dp)

    Box(
        modifier = Modifier
            .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
            .width(2.5.dp)
            .height(caretHeightDp)
            .graphicsLayer { this.alpha = alpha }
            .background(Color.White)
    )
}

sealed interface NoteBlockItem {
    data class TextBlock(val textFieldValue: TextFieldValue = TextFieldValue()) : NoteBlockItem
    data class ImageBlock(val path: String, val isThumbnail: Boolean = false) : NoteBlockItem
}

sealed interface OverlayRenderGroup {
    data class Single(val index: Int, val block: NoteBlockItem) : OverlayRenderGroup
    data class ThumbnailRow(val items: List<Pair<Int, NoteBlockItem.ImageBlock>>) : OverlayRenderGroup
}

fun groupBlocksForOverlay(blocks: List<NoteBlockItem>): List<OverlayRenderGroup> {
    val result = mutableListOf<OverlayRenderGroup>()
    var currentThumbnails = mutableListOf<Pair<Int, NoteBlockItem.ImageBlock>>()

    for (i in blocks.indices) {
        val block = blocks[i]
        if (block is NoteBlockItem.ImageBlock && block.isThumbnail) {
            currentThumbnails.add(i to block)
            if (currentThumbnails.size == 4) {
                result.add(OverlayRenderGroup.ThumbnailRow(currentThumbnails.toList()))
                currentThumbnails = mutableListOf()
            }
        } else {
            if (currentThumbnails.isNotEmpty()) {
                result.add(OverlayRenderGroup.ThumbnailRow(currentThumbnails.toList()))
                currentThumbnails = mutableListOf()
            }
            result.add(OverlayRenderGroup.Single(i, block))
        }
    }
    if (currentThumbnails.isNotEmpty()) {
        result.add(OverlayRenderGroup.ThumbnailRow(currentThumbnails.toList()))
    }
    return result
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
                        val annotated = jsonToAnnotatedStringInternal(jsonStr)
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

fun annotatedStringToJson(annotated: AnnotatedString): String {
    val obj = JSONObject()
    obj.put("text", annotated.text)
    val spansArr = JSONArray()
    annotated.spanStyles.forEach { span ->
        val spanObj = JSONObject()
        spanObj.put("start", span.start)
        spanObj.put("end", span.end)
        spanObj.put("color", span.item.color.value.toLong())
        if (span.item.fontSize != TextUnit.Unspecified) {
            spanObj.put("fontSize", span.item.fontSize.value.toDouble())
        }
        spanObj.put("bold", span.item.fontWeight == FontWeight.Bold)
        spanObj.put("italic", span.item.fontStyle == FontStyle.Italic)
        spanObj.put("underline", span.item.textDecoration?.contains(TextDecoration.Underline) == true)
        val fontId = FontManager.getFontIdByFamily(span.item.fontFamily)
        if (fontId.isNotEmpty()) {
            spanObj.put("fontFamily", fontId)
        }
        spansArr.put(spanObj)
    }
    obj.put("spans", spansArr)
    return obj.toString()
}

@Composable
fun OverlayBlocksRenderer(
    itemId: String = "",
    content: String = "",
    blocksList: List<NoteBlockItem>? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    isEditing: Boolean = false,
    onBlocksChanged: (List<NoteBlockItem>) -> Unit = {},
    focusRequester: FocusRequester? = null,
    onTapTextToEdit: () -> Unit = {},
    onSelectionChanged: (TextRange) -> Unit = {}
) {
    val context = LocalContext.current
    val blocks = blocksList ?: parseNoteContentToBlocks(content)
    val renderGroups = groupBlocksForOverlay(blocks)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(renderGroups.size) { gIdx ->
            when (val group = renderGroups[gIdx]) {
                is OverlayRenderGroup.Single -> {
                    val idx = group.index
                    val block = group.block
                    when (block) {
                        is NoteBlockItem.TextBlock -> {
                            RichAndroidTextField(
                                textFieldValue = block.textFieldValue,
                                onValueChange = { newTfv ->
                                    val updatedTfv = preserveSpansOnTextChange(block.textFieldValue.annotatedString, newTfv)
                                    val structuredValue = handleEnterAndAutoStructure(block.textFieldValue, updatedTfv)
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
                                    val mutable = blocks.toMutableList()
                                    mutable[idx] = block.copy(textFieldValue = finalTfv)
                                    onBlocksChanged(mutable)
                                },
                                onSelectionChanged = { range ->
                                    onSelectionChanged(range)
                                },
                                readOnly = !isEditing,
                                onImageReceived = { uri ->
                                    val path = copyUriToInternalStorage(context, uri)
                                    if (path != null) {
                                        val mutable = blocks.toMutableList()
                                        mutable.add(idx + 1, NoteBlockItem.ImageBlock(path = path))
                                        onBlocksChanged(mutable)
                                        Toast.makeText(context, "Фото вставлено из клавиатуры", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClick = {
                                    if (!isEditing) {
                                        onTapTextToEdit()
                                    }
                                },
                                onToggleCheckboxAtLine = { lineIdx ->
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
                                        val mutable = blocks.toMutableList()
                                        mutable[idx] = block.copy(textFieldValue = newTfv)
                                        onBlocksChanged(mutable)
                                        val finalContent = buildContentFromBlocks(mutable)
                                        if (itemId.isNotEmpty()) {
                                            val historyItem = ClipboardHistoryManager.getHistory(context).find { it.id == itemId }
                                            if (historyItem != null) {
                                                val updated = historyItem.copy(content = finalContent)
                                                ClipboardHistoryManager.updateItem(context, updated)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "Введите текст записи..."
                            )
                        }
                        is NoteBlockItem.ImageBlock -> {
                            val bitmap = remember(block.path) {
                                try {
                                    android.graphics.BitmapFactory.decodeFile(block.path)
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
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                                    .clickable {
                                        onTapTextToEdit()
                                    }
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
                            }
                        }
                    }
                }
                is OverlayRenderGroup.ThumbnailRow -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        group.items.forEach { (_, block) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1A1A1A))
                            ) {
                                val bitmap = remember(block.path) {
                                    try { android.graphics.BitmapFactory.decodeFile(block.path) } catch (e: Exception) { null }
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        repeat(4 - group.items.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

fun preserveSpansOnTextChange(
    oldAnnotated: AnnotatedString,
    newTextValue: TextFieldValue
): TextFieldValue {
    val oldText = oldAnnotated.text
    val newText = newTextValue.text

    if (oldText == newText) {
        return newTextValue.copy(
            annotatedString = AnnotatedString(
                text = newText,
                spanStyles = oldAnnotated.spanStyles
            )
        )
    }

    val diff = newText.length - oldText.length
    val selStart = newTextValue.selection.start

    val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    for (range in oldAnnotated.spanStyles) {
        var start = range.start
        var end = range.end

        if (diff > 0) {
            val changePos = (selStart - diff).coerceAtLeast(0)
            if (changePos < start) {
                start += diff
                end += diff
            } else if (changePos in start until end) {
                end += diff
            }
        } else if (diff < 0) {
            val deletePos = selStart
            val deleteCount = -diff
            val deleteEnd = deletePos + deleteCount

            if (deleteEnd <= start) {
                start += diff
                end += diff
            } else if (deletePos >= end) {
                // deletion after span
            } else {
                if (deletePos <= start && deleteEnd >= end) {
                    continue
                }
                if (deletePos <= start) {
                    start = deletePos
                    end = (end + diff).coerceAtLeast(deletePos)
                } else if (deleteEnd >= end) {
                    end = deletePos
                } else {
                    end = (end + diff).coerceAtLeast(start)
                }
            }
        }

        val clampedStart = start.coerceIn(0, newText.length)
        val clampedEnd = end.coerceIn(clampedStart, newText.length)
        if (clampedStart < clampedEnd) {
            newSpans.add(AnnotatedString.Range(range.item, clampedStart, clampedEnd))
        }
    }

    return newTextValue.copy(
        annotatedString = AnnotatedString(
            text = newText,
            spanStyles = mergeAdjacentSpans(newSpans)
        )
    )
}

fun applyStyleToAnnotatedString(
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

    val startRaw = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val endRaw = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
    val start = if (startRaw == endRaw) 0 else startRaw
    val end = if (startRaw == endRaw) text.length else endRaw

    if (start >= end) return current

    if (formatType == FormatType.FONT) {
        return applyFontToAnnotatedString(current, selectionStart, selectionEnd, fontFamilyValue)
    }

    val existingSpans = current.spanStyles.toMutableList()

    if (formatType == FormatType.CLEAR) {
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
        return AnnotatedString(text = text, spanStyles = mergeAdjacentSpans(newSpans))
    }

    val overlappingSpans = existingSpans.filter { it.start < end && it.end > start }
    val isAlreadyApplied = when (formatType) {
        FormatType.BOLD -> overlappingSpans.isNotEmpty() && overlappingSpans.all { it.item.fontWeight == FontWeight.Bold }
        FormatType.ITALIC -> overlappingSpans.isNotEmpty() && overlappingSpans.all { it.item.fontStyle == FontStyle.Italic }
        FormatType.UNDERLINE -> overlappingSpans.isNotEmpty() && overlappingSpans.all { it.item.textDecoration?.contains(TextDecoration.Underline) == true }
        FormatType.COLOR -> overlappingSpans.isNotEmpty() && overlappingSpans.all { it.item.color == colorValue }
        FormatType.SIZE -> overlappingSpans.isNotEmpty() && overlappingSpans.all { it.item.fontSize == (16 + sizeOffset).sp }
        FormatType.FONT -> overlappingSpans.isNotEmpty() && overlappingSpans.all { it.item.fontFamily == fontFamilyValue }
        FormatType.CLEAR -> false
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
                FormatType.COLOR -> if (isAlreadyApplied) style.copy(color = Color.Unspecified) else style.copy(color = colorValue)
                FormatType.SIZE -> if (isAlreadyApplied) style.copy(fontSize = TextUnit.Unspecified) else style.copy(fontSize = (16 + sizeOffset).sp)
                FormatType.FONT -> if (isAlreadyApplied) style.copy(fontFamily = null) else style.copy(fontFamily = fontFamilyValue)
                FormatType.CLEAR -> SpanStyle()
            }

            if (hasAnyAttribute(modifiedStyle)) {
                newSpans.add(AnnotatedString.Range(modifiedStyle, overlapStart, overlapEnd))
            }
        }
    }

    if (!isAlreadyApplied && formatType != FormatType.CLEAR) {
        val targetStyle = when (formatType) {
            FormatType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            FormatType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            FormatType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
            FormatType.COLOR -> if (colorValue != Color.Unspecified) SpanStyle(color = colorValue) else null
            FormatType.SIZE -> SpanStyle(fontSize = (16 + sizeOffset).sp)
            FormatType.FONT -> if (fontFamilyValue != null) SpanStyle(fontFamily = fontFamilyValue) else null
            FormatType.CLEAR -> null
        }

        if (targetStyle != null) {
            val covered = mutableListOf<Pair<Int, Int>>()
            for (range in existingSpans) {
                val oStart = maxOf(range.start, start)
                val oEnd = minOf(range.end, end)
                if (oStart < oEnd) {
                    covered.add(oStart to oEnd)
                }
            }
            covered.sortBy { it.first }

            var currentGapStart = start
            for ((cStart, cEnd) in covered) {
                if (cStart > currentGapStart) {
                    newSpans.add(AnnotatedString.Range(targetStyle, currentGapStart, cStart))
                }
                currentGapStart = maxOf(currentGapStart, cEnd)
            }
            if (currentGapStart < end) {
                newSpans.add(AnnotatedString.Range(targetStyle, currentGapStart, end))
            }
        }
    }

    return AnnotatedString(text = text, spanStyles = mergeAdjacentSpans(newSpans))
}

private fun hasAnyAttribute(style: SpanStyle): Boolean {
    return style.fontWeight != null || style.fontStyle != null || style.textDecoration != null ||
            (style.color != Color.Unspecified) || (style.fontSize != TextUnit.Unspecified) || (style.fontFamily != null)
}

private fun mergeAdjacentSpans(spans: List<AnnotatedString.Range<SpanStyle>>): List<AnnotatedString.Range<SpanStyle>> {
    return spans.filter { it.start < it.end && hasAnyAttribute(it.item) }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("d MMMM HH:mm", Locale.forLanguageTag("ru"))
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

private fun formatHeaderDate(timestamp: Long): String {
    val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()

    val isToday = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    if (isToday) return "Сегодня"

    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = itemCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "Вчера"

    return try {
        val sdf = SimpleDateFormat("d MMMM", Locale.forLanguageTag("ru"))
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

private fun itemTimestamp(item: ClipboardItem): Long = item.timestamp

@Composable
internal fun ColorPaletteBar(
    palette: List<ColorPaletteItem>,
    activeColor: Color,
    onSelectColor: (ColorPaletteItem) -> Unit,
    onOpenAdvancedPalette: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF252525),
        border = BorderStroke(1.dp, Color(0xFF3B3B3B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(palette) { item ->
                    Surface(
                        onClick = { onSelectColor(item) },
                        shape = CircleShape,
                        color = if (item.isClear) Color(0xFF333333) else item.color,
                        border = if (item.color == activeColor && !item.isClear) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color(0xFF555555)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (item.isClear) {
                                Icon(
                                    imageVector = Icons.Filled.FormatClear,
                                    contentDescription = "Original",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (onOpenAdvancedPalette != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    onClick = onOpenAdvancedPalette,
                    shape = CircleShape,
                    color = Color(0xFF333333),
                    border = BorderStroke(1.dp, Color(0xFF555555)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.FormatColorText,
                            contentDescription = "Advanced Palette",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardOverlayContent(
    isScreenLocked: Boolean = false,
    activePackageName: String? = null,
    initialState: ClipboardOverlayState = ClipboardOverlayState.COLLAPSED,
    isFullScreen: Boolean = false,
    onDismiss: () -> Unit,
    onPasteItem: (String) -> Unit,
    onPinChanged: (Boolean) -> Unit = {},
    onHeightChanged: (Int) -> Unit = {},
    onKeyboardFocusRequested: (Boolean) -> Unit = {},
    onToggleCustomizableMode: (Boolean) -> Unit = {},
    onMoveOverlay: (Float, Float) -> Unit = { _, _ -> },
    onResizeCorner: (Float, Float, String) -> Unit = { _, _, _ -> },
    onSaveCustomLayout: () -> Unit = {},
    onResetCustomLayout: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()
    val transitionProgress = remember { Animatable(0f) }

    var sessionCreatedIds by remember { mutableStateOf(setOf<String>()) }
    var isMinimizedToCircle by remember { mutableStateOf(false) }

    var selectedDetailItem by remember { mutableStateOf<ClipboardItem?>(null) }
    var isEditingDetail by remember { mutableStateOf(false) }

    var isCreatingNewItem by remember { mutableStateOf(false) }

    var history by remember {
        mutableStateOf(
            try {
                ClipboardHistoryManager.getHistory(context)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(isScreenLocked) {
        if (isScreenLocked) {
            sessionCreatedIds = emptySet()
            selectedDetailItem = null
            isEditingDetail = false
            isCreatingNewItem = false
            isMinimizedToCircle = false
            scope.launch { transitionProgress.snapTo(0f) }
        }
    }

    var activeColor by remember { mutableStateOf<Color>(Color(0xFFFF3B30)) }
    var showColorPalette by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showStructureMenu by remember { mutableStateOf(false) }
    var activeStructureType by remember { mutableStateOf(StructureType.BULLET) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var currentSizeOffset by remember { mutableIntStateOf(0) }
    var activeFontName by remember { mutableStateOf("system") }
    var undoStack by remember { mutableStateOf(listOf<List<NoteBlockItem>>()) }
    var redoStack by remember { mutableStateOf(listOf<List<NoteBlockItem>>()) }
    var lastSelectedRange by remember { mutableStateOf(TextRange.Zero) }

    var currentBlocks by remember { mutableStateOf<List<NoteBlockItem>>(emptyList()) }

    LaunchedEffect(isEditingDetail, isCreatingNewItem, selectedDetailItem) {
        showColorPalette = false
        showSizeMenu = false
        showFontMenu = false
        if (isEditingDetail && selectedDetailItem != null) {
            currentBlocks = parseNoteContentToBlocks(selectedDetailItem!!.content)
        } else if (isCreatingNewItem) {
            currentBlocks = listOf(NoteBlockItem.TextBlock())
        }
    }

    fun pushUndoState() {
        if (undoStack.lastOrNull() != currentBlocks) {
            undoStack = (undoStack + listOf(currentBlocks)).takeLast(30)
            redoStack = emptyList()
        }
    }

    fun handleFormatAction(
        formatType: FormatType,
        colorValue: Color = Color.Unspecified,
        sizeOffset: Int = 0,
        fontFamilyValue: FontFamily? = null
    ) {
        pushUndoState()
        if (isEditingDetail || isCreatingNewItem) {
            if (currentBlocks.isEmpty()) {
                currentBlocks = listOf(NoteBlockItem.TextBlock())
            }
            val mutableBlocks = currentBlocks.toMutableList()
            val textBlockIdx = mutableBlocks.indexOfFirst { it is NoteBlockItem.TextBlock }
            if (textBlockIdx != -1) {
                val tb = mutableBlocks[textBlockIdx] as NoteBlockItem.TextBlock
                val tfv = tb.textFieldValue
                val newAnnotated = applyStyleToAnnotatedString(
                    current = tfv.annotatedString,
                    selectionStart = lastSelectedRange.start,
                    selectionEnd = lastSelectedRange.end,
                    formatType = formatType,
                    colorValue = colorValue,
                    sizeOffset = sizeOffset,
                    fontFamilyValue = fontFamilyValue
                )
                mutableBlocks[textBlockIdx] = tb.copy(textFieldValue = tfv.copy(annotatedString = newAnnotated))
                currentBlocks = mutableBlocks
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.last()
            redoStack = (redoStack + listOf(currentBlocks)).takeLast(30)
            undoStack = undoStack.dropLast(1)
            currentBlocks = previous
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.last()
            undoStack = (undoStack + listOf(currentBlocks)).takeLast(30)
            redoStack = redoStack.dropLast(1)
            currentBlocks = next
        }
    }

    LaunchedEffect(isEditingDetail, isCreatingNewItem, selectedDetailItem) {
        showColorPalette = false
        undoStack = emptyList()
        redoStack = emptyList()
    }

    var isCustomizableMode by remember { mutableStateOf(false) }
    var isCornerResizeActive by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var expandedMenuItemId by remember { mutableStateOf<String?>(null) }
    var overlayState by remember { mutableStateOf(if (isFullScreen) ClipboardOverlayState.EXPANDED else initialState) }

    LaunchedEffect(isEditingDetail, isCreatingNewItem) {
        if (isEditingDetail || isCreatingNewItem) {
            overlayState = ClipboardOverlayState.EXPANDED
        }
        kotlinx.coroutines.delay(50)
        onKeyboardFocusRequested(isEditingDetail || isCreatingNewItem)
    }

    var selectedFilters by remember { mutableStateOf(setOf<NoteFilter>()) }
    var visible by remember { mutableStateOf(false) }

    var isDragging by remember { mutableStateOf(false) }
    var dragHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(selectedFilters, searchQuery, isSearchActive, selectedDetailItem, isCreatingNewItem, isCustomizableMode, showSettingsMenu) {
        expandedMenuItemId = null
    }

    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    if (backDispatcherOwner != null) {
        val isBackActive = isSearchActive || selectedDetailItem != null || isCreatingNewItem || isCustomizableMode || isMinimizedToCircle || showSettingsMenu
        BackHandler(enabled = isBackActive) {
            if (showSettingsMenu) {
                showSettingsMenu = false
            } else if (isMinimizedToCircle) {
                isMinimizedToCircle = false
            } else if (isCustomizableMode) {
                isCustomizableMode = false
                isCornerResizeActive = false
            } else if (isEditingDetail) {
                isEditingDetail = false
            } else if (selectedDetailItem != null) {
                selectedDetailItem = null
            } else if (isCreatingNewItem) {
                isCreatingNewItem = false
            } else if (isSearchActive) {
                isSearchActive = false
                searchQuery = ""
            }
        }
    }

    val yellowColor = Color(0xFFD700)
    val darkBg = Color(0xFF1A1A1A)
    val grayBorder = Color(0xFF444444)

    val screenHeight = configuration.screenHeightDp.dp
    val minimalHeight = 100.dp
    val collapsedHeight = 320.dp
    val expandedHeight = screenHeight * 0.90f

    val minimalPx = with(density) { minimalHeight.toPx() }
    val collapsedPx = with(density) { collapsedHeight.toPx() }
    val expandedPx = with(density) { expandedHeight.toPx() }

    val targetHeightDp = when (overlayState) {
        ClipboardOverlayState.MINIMAL -> minimalHeight
        ClipboardOverlayState.COLLAPSED -> collapsedHeight
        ClipboardOverlayState.EXPANDED -> expandedHeight
    }

    val animatedHeightDp by animateDpAsState(
        targetValue = targetHeightDp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "clipboard_height"
    )

    val currentDisplayHeight = if (isDragging) with(density) { dragHeightPx.toDp() } else animatedHeightDp
    val isCompactMode = currentDisplayHeight < 200.dp
    val totalHeightPx = with(density) { (currentDisplayHeight + 36.dp).toPx() }.toInt()

    LaunchedEffect(totalHeightPx, isMinimizedToCircle) {
        if (!isCustomizableMode && !isMinimizedToCircle) {
            onHeightChanged(totalHeightPx)
        } else if (isMinimizedToCircle) {
            onHeightChanged(100)
        }
    }

    val currentTime = System.currentTimeMillis()
    val displayItems = remember(history, isScreenLocked, sessionCreatedIds) {
        if (isScreenLocked) {
            history.filter { it.id in sessionCreatedIds }
        } else {
            history
        }
    }

    val filteredItems = remember(displayItems, selectedFilters, searchQuery) {
        var items = displayItems

        if (selectedFilters.isNotEmpty()) {
            items = items.filter { item ->
                var matches = true
                for (filter in selectedFilters) {
                    val pass = when (filter) {
                        NoteFilter.ALL -> true
                        NoteFilter.CLIPBOARD -> !item.isNote
                        NoteFilter.NOTES -> item.isNote
                        NoteFilter.FAVORITES -> item.isPinned
                        NoteFilter.LINKS -> hasLink(item.content)
                        NoteFilter.FORMATTED -> hasFormatting(item.content)
                        NoteFilter.PLAIN_TEXT -> !hasFormatting(item.content)
                        NoteFilter.SHORT -> stripHtmlAndStructures(item.content).lines().filter { l -> l.isNotBlank() }.size <= 5
                        NoteFilter.LONG -> stripHtmlAndStructures(item.content).lines().filter { l -> l.isNotBlank() }.size > 5
                    }
                    if (!pass) {
                        matches = false
                        break
                    }
                }
                matches
            }
        }

        if (searchQuery.isNotBlank()) {
            items = items.filter { it.content.contains(searchQuery, ignoreCase = true) }
        }

        items
    }

    val groupedItems = remember(filteredItems) {
        filteredItems.groupBy { formatHeaderDate(it.timestamp) }
    }

    val isEditingOrCreating = isEditingDetail || isCreatingNewItem

    if (isMinimizedToCircle) {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

        var bubbleOffsetX by remember { mutableFloatStateOf((screenWidthPx - 80f).coerceAtLeast(16f)) }
        var bubbleOffsetY by remember { mutableFloatStateOf((screenHeightPx * 0.35f).coerceIn(100f, (screenHeightPx - 120f).coerceAtLeast(100f))) }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            bubbleOffsetX.toInt().coerceIn(0, (screenWidthPx - 56f).toInt().coerceAtLeast(0)),
                            bubbleOffsetY.toInt().coerceIn(0, (screenHeightPx - 56f).toInt().coerceAtLeast(0))
                        )
                    }
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .border(2.dp, Color(0xFFD700), CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                bubbleOffsetX = if (bubbleOffsetX < screenWidthPx / 2f) 16f else (screenWidthPx - 72f).coerceAtLeast(16f)
                                bubbleOffsetY = bubbleOffsetY.coerceIn(0f, screenHeightPx - 56f)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            bubbleOffsetX = (bubbleOffsetX + dragAmount.x).coerceIn(0f, screenWidthPx - 56f)
                            bubbleOffsetY = (bubbleOffsetY + dragAmount.y).coerceIn(0f, screenHeightPx - 56f)
                        }
                    }
                    .clickable {
                        scope.launch {
                            isMinimizedToCircle = false
                            transitionProgress.snapTo(1f)
                            transitionProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            overlayState = ClipboardOverlayState.COLLAPSED
                        }
                    },
                shape = CircleShape,
                color = Color(0xFF1A1A1A)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Expand",
                        tint = Color(0xFFD700),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .clickable { expandedMenuItemId = null },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            val widthFraction = (1f - transitionProgress.value).coerceIn(0.05f, 1f)
            val translationX = transitionProgress.value * 400f
            val cardAlpha = (1f - transitionProgress.value).coerceIn(0f, 1f)

            Surface(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .graphicsLayer {
                        this.translationX = translationX
                        this.alpha = cardAlpha
                    }
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp)
                    .height(currentDisplayHeight)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                color = darkBg,
                border = if (isCustomizableMode) BorderStroke(2.dp, yellowColor) else null,
                shadowElevation = 24.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .pointerInput(isCustomizableMode) {
                                    if (isCustomizableMode) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            onMoveOverlay(dragAmount.x, dragAmount.y)
                                        }
                                    } else {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                expandedMenuItemId = null
                                                isDragging = true
                                                dragHeightPx = when (overlayState) {
                                                    ClipboardOverlayState.MINIMAL -> minimalPx
                                                    ClipboardOverlayState.COLLAPSED -> collapsedPx
                                                    ClipboardOverlayState.EXPANDED -> expandedPx
                                                }
                                            },
                                            onDragEnd = {
                                                val distMin = abs(dragHeightPx - minimalPx)
                                                val distCol = abs(dragHeightPx - collapsedPx)
                                                val distExp = abs(dragHeightPx - expandedPx)

                                                overlayState = when {
                                                    distMin <= distCol && distMin <= distExp -> ClipboardOverlayState.MINIMAL
                                                    distCol <= distExp -> ClipboardOverlayState.COLLAPSED
                                                    else -> ClipboardOverlayState.EXPANDED
                                                }
                                                isDragging = false
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragHeightPx = (dragHeightPx - dragAmount).coerceIn(minimalPx, expandedPx)
                                            }
                                        )
                                    }
                                }
                                .clickable {
                                    if (!isCustomizableMode) {
                                        expandedMenuItemId = null
                                        overlayState = when (overlayState) {
                                            ClipboardOverlayState.MINIMAL -> ClipboardOverlayState.COLLAPSED
                                            ClipboardOverlayState.COLLAPSED -> ClipboardOverlayState.EXPANDED
                                            ClipboardOverlayState.EXPANDED -> ClipboardOverlayState.COLLAPSED
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isCustomizableMode) yellowColor else Color.Gray)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        if (isScreenLocked && displayItems.isEmpty() && !isCreatingNewItem && selectedDetailItem == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(text = "🔒", fontSize = 42.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Буфер обмена заблокирован",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Чтобы увидеть записи и заметки, разблокируйте телефон",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (isCreatingNewItem) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isCreatingNewItem = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }

                                Text(
                                    text = "Новая запись",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Button(
                                    onClick = {
                                        val finalContent = buildContentFromBlocks(currentBlocks)
                                        if (finalContent.isNotBlank()) {
                                            val newId = ClipboardHistoryManager.addItemAndGetId(context, finalContent, "text", isNote = true)
                                            if (isScreenLocked) {
                                                sessionCreatedIds = sessionCreatedIds + newId
                                            }
                                            history = ClipboardHistoryManager.getHistory(context)
                                            isCreatingNewItem = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Сохранить", color = Color.Black, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OverlayBlocksRenderer(
                                itemId = "new",
                                blocksList = currentBlocks,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                isEditing = true,
                                onBlocksChanged = { newBlocks ->
                                    currentBlocks = newBlocks
                                },
                                focusRequester = focusRequester,
                                onSelectionChanged = { range -> lastSelectedRange = range }
                            )

                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(150)
                                try {
                                    focusRequester.requestFocus()
                                } catch (e: Exception) {}
                            }
                        } else if (selectedDetailItem != null) {
                            val item = selectedDetailItem!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left back button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF333333))
                                        .clickable {
                                            if (isEditingDetail) {
                                                isEditingDetail = false
                                            } else {
                                                selectedDetailItem = null
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Title: "Заметка" vs "Редактирование"
                                Text(
                                    text = if (isEditingDetail) "Редактирование" else "Заметка",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Right button: Pencil (in reading) or Check (in editing)
                                if (isEditingDetail) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .clickable {
                                                val finalContent = buildContentFromBlocks(currentBlocks)
                                                if (finalContent.isNotBlank()) {
                                                    val updatedItem = item.copy(content = finalContent)
                                                    ClipboardHistoryManager.updateItem(context, updatedItem)
                                                    history = ClipboardHistoryManager.getHistory(context)
                                                    selectedDetailItem = updatedItem
                                                    isEditingDetail = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Save",
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF333333))
                                            .clickable {
                                                overlayState = ClipboardOverlayState.EXPANDED
                                                currentBlocks = parseNoteContentToBlocks(item.content)
                                                isEditingDetail = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isEditingDetail) {
                                OverlayBlocksRenderer(
                                    itemId = item.id,
                                    blocksList = currentBlocks,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    isEditing = true,
                                    onBlocksChanged = { newBlocks ->
                                        currentBlocks = newBlocks
                                    },
                                    focusRequester = focusRequester,
                                    onSelectionChanged = { range -> lastSelectedRange = range }
                                )

                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(150)
                                    try {
                                        focusRequester.requestFocus()
                                    } catch (e: Exception) {}
                                }
                            } else {
                                OverlayBlocksRenderer(
                                    content = item.content,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    onTapTextToEdit = {
                                        overlayState = ClipboardOverlayState.EXPANDED
                                        currentBlocks = parseNoteContentToBlocks(item.content)
                                        isEditingDetail = true
                                    }
                                )
                            }
                        } else if (showSettingsMenu) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF252525))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Функции оверлея",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF333333))
                                            .clickable { showSettingsMenu = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Settings",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Function item: Move / Resize (Перемещение)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCustomizableMode) yellowColor.copy(alpha = 0.2f) else Color(0xFF1E1E1E))
                                        .border(1.dp, if (isCustomizableMode) yellowColor else Color(0xFF333333), RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {
                                                isCustomizableMode = !isCustomizableMode
                                                if (!isCustomizableMode) isCornerResizeActive = false
                                                showSettingsMenu = false
                                                Toast.makeText(
                                                    context,
                                                    if (isCustomizableMode) "Режим перемещения включен" else "Обычный режим",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onLongClick = {
                                                isCustomizableMode = true
                                                isCornerResizeActive = true
                                                showSettingsMenu = false
                                                Toast.makeText(context, "Режим изменения углов активирован", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isCustomizableMode) yellowColor else Color(0xFF333333)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenWith,
                                            contentDescription = "Move",
                                            tint = if (isCustomizableMode) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Перемещение и размер",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Нажмите для перемещения, удержание — углы",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else if (!isCompactMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedContent(
                                    targetState = isSearchActive,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(220)) + expandHorizontally()).togetherWith(
                                            fadeOut(animationSpec = tween(180)) + shrinkHorizontally()
                                        )
                                    },
                                    label = "search_transition"
                                ) { searching ->
                                    if (searching) {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
                                            placeholder = { Text(stringResource(R.string.clipboard_search_hint), color = Color.Gray, fontSize = 14.sp) },
                                            leadingIcon = {
                                                IconButton(onClick = {
                                                    isSearchActive = false
                                                    searchQuery = ""
                                                }) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = yellowColor)
                                                }
                                            },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = yellowColor,
                                                unfocusedBorderColor = grayBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = yellowColor
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                    } else {
                                        LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            items(NoteFilter.entries.toTypedArray(), key = { it.name }) { filter ->
                                                val isAll = filter == NoteFilter.ALL
                                                val isSelected = if (isAll) selectedFilters.isEmpty() else selectedFilters.contains(filter)
                                                val showCross = isAll && selectedFilters.isNotEmpty()

                                                Box(
                                                    modifier = Modifier
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(if (isSelected) Color.White else Color(0xFF1E1E1E))
                                                        .border(1.dp, if (isSelected) Color.White else Color(0xFF333333), RoundedCornerShape(20.dp))
                                                        .clickable {
                                                            if (isAll) {
                                                                selectedFilters = emptySet()
                                                            } else {
                                                                selectedFilters = if (selectedFilters.contains(filter)) {
                                                                    selectedFilters - filter
                                                                } else {
                                                                    selectedFilters + filter
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = 14.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        if (showCross) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Reset filters",
                                                                tint = Color.Gray,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = filter.title,
                                                                color = if (isSelected) Color.Black else Color.White,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (filteredItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.clipboard_empty),
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    groupedItems.forEach { (dateHeader, itemsForDate) ->
                                        item(key = "header_$dateHeader") {
                                            Text(
                                                text = dateHeader,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                                            )
                                        }
                                        items(itemsForDate, key = { it.id }) { item ->
                                            ClipboardCard(
                                                item = item,
                                                showStarIcon = !selectedFilters.contains(NoteFilter.FAVORITES),
                                                isMenuExpanded = expandedMenuItemId == item.id,
                                                onToggleMenu = {
                                                    expandedMenuItemId = if (expandedMenuItemId == item.id) null else item.id
                                                },
                                                onTap = {
                                                    expandedMenuItemId = null
                                                    selectedDetailItem = item
                                                    isEditingDetail = false
                                                },
                                                onCopy = {
                                                    expandedMenuItemId = null
                                                    ClipboardHistoryManager.copyToClipboard(context, item)
                                                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                                },
                                                onPaste = {
                                                    expandedMenuItemId = null
                                                    onPasteItem(item.content)
                                                },
                                                onDelete = {
                                                    expandedMenuItemId = null
                                                    ClipboardHistoryManager.removeItem(context, item.id)
                                                    history = ClipboardHistoryManager.getHistory(context)
                                                },
                                                onPin = {
                                                    ClipboardHistoryManager.togglePin(context, item.id)
                                                    history = ClipboardHistoryManager.getHistory(context)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditingOrCreating && showSizeMenu) {
                            SizeSliderBar(
                                currentSizeOffset = currentSizeOffset,
                                onSizeChanged = { newOffset ->
                                    currentSizeOffset = newOffset
                                    handleFormatAction(FormatType.SIZE, sizeOffset = newOffset)
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (isEditingOrCreating && showFontMenu) {
                            FontSelectionBar(
                                activeFont = activeFontName,
                                onSelectFont = { fontItem ->
                                    activeFontName = fontItem.id
                                    handleFormatAction(FormatType.FONT, fontFamilyValue = fontItem.fontFamily)
                                    showFontMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (isEditingOrCreating && showStructureMenu) {
                            StructureSelectionBar(
                                activeType = activeStructureType,
                                onSelectType = { selectedType ->
                                    activeStructureType = selectedType
                                    val mutable = currentBlocks.toMutableList()
                                    val targetIdx = mutable.indexOfFirst { it is NoteBlockItem.TextBlock }
                                    if (targetIdx >= 0) {
                                        val block = mutable[targetIdx] as NoteBlockItem.TextBlock
                                        val newTfv = applyStructureToTextBlock(block.textFieldValue, selectedType)
                                        mutable[targetIdx] = block.copy(textFieldValue = newTfv)
                                        currentBlocks = mutable
                                    }
                                    showStructureMenu = false
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (isEditingOrCreating && showColorPalette) {
                            ColorPaletteBar(
                                palette = defaultColorPalette,
                                activeColor = activeColor,
                                onSelectColor = { paletteItem ->
                                    val newColor = if (paletteItem.isClear) Color.Unspecified else paletteItem.color
                                    if (!paletteItem.isClear) {
                                        activeColor = newColor
                                    }
                                    val format = if (paletteItem.isClear) FormatType.CLEAR else FormatType.COLOR
                                    handleFormatAction(format, colorValue = newColor)
                                    showColorPalette = false
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isCornerResizeActive) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCornerResizeActive) {
                                Button(
                                    onClick = {
                                        isCustomizableMode = false
                                        isCornerResizeActive = false
                                        onSaveCustomLayout()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = yellowColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("ОК", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        isCustomizableMode = false
                                        isCornerResizeActive = false
                                        onResetCustomLayout()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Сбросить", color = Color.White, fontSize = 14.sp)
                                }
                            } else if (isEditingOrCreating) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Undo
                                    Surface(
                                        onClick = { undo() },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        enabled = undoStack.isNotEmpty(),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Undo",
                                                tint = if (undoStack.isNotEmpty()) Color.White else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 2. Redo
                                    Surface(
                                        onClick = { redo() },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        enabled = redoStack.isNotEmpty(),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                                contentDescription = "Redo",
                                                tint = if (redoStack.isNotEmpty()) Color.White else Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Structure (Lists / Checkboxes)
                                    Surface(
                                        onClick = {
                                            showStructureMenu = !showStructureMenu
                                            showSizeMenu = false
                                            showFontMenu = false
                                            showColorPalette = false
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        border = if (showStructureMenu) BorderStroke(2.dp, yellowColor) else null,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (activeStructureType) {
                                                    StructureType.BULLET -> Icons.AutoMirrored.Filled.FormatListBulleted
                                                    StructureType.NUMBER -> Icons.Default.FormatListNumbered
                                                    StructureType.CHECKBOX -> Icons.Default.Checklist
                                                },
                                                contentDescription = "Structure",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 3. Text Color
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    val colorToApply = if (activeColor == Color.Unspecified) Color(0xFFFF3B30) else activeColor
                                                    handleFormatAction(FormatType.COLOR, colorValue = colorToApply)
                                                },
                                                onLongClick = {
                                                    showColorPalette = !showColorPalette
                                                }
                                            ),
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        border = if (showColorPalette) BorderStroke(2.dp, yellowColor) else null
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.FormatColorText,
                                                contentDescription = "Text Color",
                                                tint = if (activeColor != Color.Unspecified) activeColor else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 4. Italic
                                    Surface(
                                        onClick = {
                                            handleFormatAction(FormatType.ITALIC)
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.FormatItalic,
                                                contentDescription = "Italic",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 5. Bold
                                    Surface(
                                        onClick = {
                                            handleFormatAction(FormatType.BOLD)
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.FormatBold,
                                                contentDescription = "Bold",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 6. Underline
                                    Surface(
                                        onClick = {
                                            handleFormatAction(FormatType.UNDERLINE)
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.FormatUnderlined,
                                                contentDescription = "Underline",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // 7. Size
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    showSizeMenu = !showSizeMenu
                                                    showFontMenu = false
                                                    showColorPalette = false
                                                },
                                                onLongClick = {
                                                    showSizeMenu = !showSizeMenu
                                                    showFontMenu = false
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

                                    // 8. Font
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    showFontMenu = !showFontMenu
                                                    showSizeMenu = false
                                                    showColorPalette = false
                                                },
                                                onLongClick = {
                                                    showFontMenu = !showFontMenu
                                                    showSizeMenu = false
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

                                    // 9. Clear Format (Eraser)
                                    Surface(
                                        onClick = {
                                            handleFormatAction(FormatType.CLEAR)
                                            Toast.makeText(context, "Формат сброшен", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFF333333),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.CleaningServices,
                                                contentDescription = "Clear Format",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    onClick = {
                                        expandedMenuItemId = null
                                        visible = false
                                        onDismiss()
                                    },
                                    shape = CircleShape,
                                    color = Color(0xFF333333),
                                    border = null,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Left: Settings (Gear) + Minimize
                                    Row(
                                        modifier = Modifier.align(Alignment.CenterStart),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Settings button (Gear)
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (showSettingsMenu) yellowColor else Color(0xFF333333))
                                                .clickable {
                                                    expandedMenuItemId = null
                                                    showSettingsMenu = !showSettingsMenu
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = if (showSettingsMenu) Color.Black else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 2. Minimize to Circle button
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF333333))
                                                .clickable {
                                                    expandedMenuItemId = null
                                                    scope.launch {
                                                        overlayState = ClipboardOverlayState.MINIMAL
                                                        kotlinx.coroutines.delay(180)
                                                        transitionProgress.animateTo(
                                                            targetValue = 1f,
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        )
                                                        isMinimizedToCircle = true
                                                        transitionProgress.snapTo(0f)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Minimize to Circle",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Center: Plus button (strictly centered across panel width, with circular background and white icon)
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF333333))
                                            .clickable {
                                                expandedMenuItemId = null
                                                showSettingsMenu = false
                                                overlayState = ClipboardOverlayState.EXPANDED
                                                isCreatingNewItem = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Right: Close button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF333333))
                                            .clickable {
                                                expandedMenuItemId = null
                                                visible = false
                                                onDismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isCornerResizeActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(40.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        onResizeCorner(-drag.x, -drag.y, "TOP_LEFT")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(shape = CircleShape, color = yellowColor, modifier = Modifier.size(16.dp), border = BorderStroke(2.dp, Color.Black)) {}
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(40.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        onResizeCorner(drag.x, -drag.y, "TOP_RIGHT")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(shape = CircleShape, color = yellowColor, modifier = Modifier.size(16.dp), border = BorderStroke(2.dp, Color.Black)) {}
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(40.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        onResizeCorner(-drag.x, drag.y, "BOTTOM_LEFT")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(shape = CircleShape, color = yellowColor, modifier = Modifier.size(16.dp), border = BorderStroke(2.dp, Color.Black)) {}
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(40.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        onResizeCorner(drag.x, drag.y, "BOTTOM_RIGHT")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(shape = CircleShape, color = yellowColor, modifier = Modifier.size(16.dp), border = BorderStroke(2.dp, Color.Black)) {}
                        }
                    }
                }
            }
        }
    }
}

fun extractCleanNoteText(content: String): AnnotatedString {
    if (content.isBlank()) return AnnotatedString("")
    val trimmed = content.trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        try {
            val blocks = parseNoteContentToBlocks(trimmed)
            val textBlocks = blocks.filterIsInstance<NoteBlockItem.TextBlock>()
            if (textBlocks.isNotEmpty()) {
                val sb = StringBuilder()
                val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
                textBlocks.forEachIndexed { i, textBlock ->
                    val startOffset = sb.length
                    val annotated = textBlock.textFieldValue.annotatedString
                    sb.append(annotated.text)
                    for (span in annotated.spanStyles) {
                        spans.add(AnnotatedString.Range(span.item, span.start + startOffset, span.end + startOffset))
                    }
                    if (i < textBlocks.size - 1) {
                        sb.append("\n")
                    }
                }
                return AnnotatedString(text = sb.toString(), spanStyles = spans)
            }
        } catch (e: Exception) {}
    }
    return htmlToAnnotatedString(content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardCard(
    item: ClipboardItem,
    showStarIcon: Boolean = true,
    isMenuExpanded: Boolean = false,
    isNoteStyle: Boolean = false,
    maxLines: Int = 3,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onToggleMenu: () -> Unit,
    onTap: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            onSelectToggle()
                        } else {
                            onTap()
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            onToggleMenu()
                        }
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF252525),
            border = if (isSelected) BorderStroke(2.dp, Color.White) else BorderStroke(1.dp, Color(0xFF333333))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (item.type == "image") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🖼️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Скриншот / Изображение",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.content,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = htmlToAnnotatedString(item.content),
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimestamp(item.timestamp),
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                        if (showStarIcon && item.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                if (isSelectionMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.White,
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.Black
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isMenuExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 8.dp, end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E1E),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                    }

                    IconButton(onClick = onPaste) {
                        Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = "Paste", tint = Color.White)
                    }

                    IconButton(onClick = onPin) {
                        Icon(
                            imageVector = if (item.isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5555))
                    }
                }
            }
        }
    }
}
