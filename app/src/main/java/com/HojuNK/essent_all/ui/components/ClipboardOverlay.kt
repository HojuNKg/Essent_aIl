package com.HojuNK.essent_all.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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

enum class ClipboardOverlayState {
    MINIMAL,   // ~100.dp (only handle bar + bottom buttons)
    COLLAPSED, // ~320.dp (standard keyboard size)
    EXPANDED   // ~90% screen height
}

enum class FormatType {
    BOLD, ITALIC, UNDERLINE, COLOR, CLEAR
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
            if (changePos <= start) {
                start += diff
                end += diff
            } else if (changePos in (start + 1)..end) {
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
            spanStyles = newSpans
        )
    )
}

fun applyStyleToAnnotatedString(
    current: AnnotatedString,
    selectionStart: Int,
    selectionEnd: Int,
    formatType: FormatType,
    colorValue: Color = Color.Unspecified
): AnnotatedString {
    val text = current.text
    if (text.isEmpty()) return current

    val targetStart = if (selectionStart < selectionEnd) selectionStart.coerceIn(0, text.length) else 0
    val targetEnd = if (selectionStart < selectionEnd) selectionEnd.coerceIn(0, text.length) else text.length

    if (targetStart >= targetEnd) return current

    val existingSpans = current.spanStyles.toMutableList()

    if (formatType == FormatType.CLEAR) {
        val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        for (range in existingSpans) {
            if (range.end <= targetStart || range.start >= targetEnd) {
                newSpans.add(range)
            } else {
                if (range.start < targetStart) {
                    newSpans.add(AnnotatedString.Range(range.item, range.start, targetStart))
                }
                if (range.end > targetEnd) {
                    newSpans.add(AnnotatedString.Range(range.item, targetEnd, range.end))
                }
            }
        }
        return AnnotatedString(text = text, spanStyles = newSpans)
    }

    val spansInRange = existingSpans.filter { it.start <= targetStart && it.end >= targetEnd }
    val isAlreadyApplied = when (formatType) {
        FormatType.BOLD -> spansInRange.any { it.item.fontWeight == FontWeight.Bold }
        FormatType.ITALIC -> spansInRange.any { it.item.fontStyle == FontStyle.Italic }
        FormatType.UNDERLINE -> spansInRange.any { it.item.textDecoration?.contains(TextDecoration.Underline) == true }
        FormatType.COLOR -> spansInRange.any { it.item.color == colorValue }
        else -> false
    }

    val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

    for (range in existingSpans) {
        val overlaps = (range.start < targetEnd && range.end > targetStart)
        if (!overlaps) {
            newSpans.add(range)
            continue
        }

        val style = range.item
        val shouldRemoveThisAttribute = isAlreadyApplied || (formatType == FormatType.COLOR)

        val modifiedStyle = when (formatType) {
            FormatType.BOLD -> if (shouldRemoveThisAttribute) style.copy(fontWeight = null) else style
            FormatType.ITALIC -> if (shouldRemoveThisAttribute) style.copy(fontStyle = null) else style
            FormatType.UNDERLINE -> if (shouldRemoveThisAttribute) style.copy(textDecoration = null) else style
            FormatType.COLOR -> style.copy(color = Color.Unspecified)
            FormatType.CLEAR -> SpanStyle()
        }

        if (range.start < targetStart) {
            newSpans.add(AnnotatedString.Range(style, range.start, targetStart))
        }
        if (range.end > targetEnd) {
            newSpans.add(AnnotatedString.Range(style, targetEnd, range.end))
        }

        if (hasAnyAttribute(modifiedStyle)) {
            newSpans.add(AnnotatedString.Range(modifiedStyle, maxOf(range.start, targetStart), minOf(range.end, targetEnd)))
        }
    }

    if (!isAlreadyApplied && formatType != FormatType.CLEAR) {
        val newStyle = when (formatType) {
            FormatType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            FormatType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            FormatType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
            FormatType.COLOR -> if (colorValue != Color.Unspecified) SpanStyle(color = colorValue) else null
            FormatType.CLEAR -> null
        }
        if (newStyle != null) {
            newSpans.add(AnnotatedString.Range(newStyle, targetStart, targetEnd))
        }
    }

    return AnnotatedString(text = text, spanStyles = mergeAdjacentSpans(newSpans))
}

private fun hasAnyAttribute(style: SpanStyle): Boolean {
    return style.fontWeight != null || style.fontStyle != null || style.textDecoration != null || (style.color != Color.Unspecified)
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
private fun ColorPaletteBar(
    palette: List<ColorPaletteItem>,
    activeColor: Color,
    onSelectColor: (ColorPaletteItem) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF252525),
        border = BorderStroke(1.dp, Color(0xFF3B3B3B))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardOverlayContent(
    isScreenLocked: Boolean = false,
    activePackageName: String? = null,
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
    var editingTextFieldValue by remember { mutableStateOf(TextFieldValue()) }

    var undoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }
    var redoStack by remember { mutableStateOf(listOf<TextFieldValue>()) }
    var lastEditTimestamp by remember { mutableLongStateOf(0L) }

    fun pushUndoState() {
        if (undoStack.lastOrNull() != editingTextFieldValue) {
            undoStack = (undoStack + editingTextFieldValue).takeLast(30)
            redoStack = emptyList()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + editingTextFieldValue
            editingTextFieldValue = undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + editingTextFieldValue
            editingTextFieldValue = redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    LaunchedEffect(selectedDetailItem, isEditingDetail) {
        if (isEditingDetail && selectedDetailItem != null) {
            val annotated = htmlToAnnotatedString(selectedDetailItem!!.content)
            editingTextFieldValue = TextFieldValue(
                annotatedString = annotated,
                selection = TextRange(0, annotated.text.length)
            )
            undoStack = emptyList()
            redoStack = emptyList()
        }
    }

    LaunchedEffect(isCreatingNewItem) {
        if (isCreatingNewItem) {
            editingTextFieldValue = TextFieldValue(
                annotatedString = AnnotatedString(""),
                selection = TextRange(0)
            )
            undoStack = emptyList()
            redoStack = emptyList()
        }
    }

    LaunchedEffect(isEditingDetail, isCreatingNewItem, selectedDetailItem) {
        showColorPalette = false
    }

    var isCustomizableMode by remember { mutableStateOf(false) }
    var isCornerResizeActive by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    var expandedMenuItemId by remember { mutableStateOf<String?>(null) }
    var overlayState by remember { mutableStateOf(ClipboardOverlayState.COLLAPSED) }

    LaunchedEffect(isEditingDetail, isCreatingNewItem) {
        if (isEditingDetail || isCreatingNewItem) {
            overlayState = ClipboardOverlayState.EXPANDED
        }
        onKeyboardFocusRequested(isEditingDetail || isCreatingNewItem)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Недавние, 1: Все, 2: Заметки, 3: Избранное
    var visible by remember { mutableStateOf(false) }

    var isDragging by remember { mutableStateOf(false) }
    var dragHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(selectedTab, searchQuery, isSearchActive, selectedDetailItem, isCreatingNewItem, isCustomizableMode) {
        expandedMenuItemId = null
    }

    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    if (backDispatcherOwner != null) {
        val isBackActive = isSearchActive || selectedDetailItem != null || isCreatingNewItem || isCustomizableMode || isMinimizedToCircle
        BackHandler(enabled = isBackActive) {
            if (isMinimizedToCircle) {
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

    val tabFiltered = remember(displayItems, selectedTab) {
        when (selectedTab) {
            0 -> displayItems.filter { currentTime - itemTimestamp(it) <= 24 * 3600 * 1000L } // Недавние
            1 -> displayItems // Все (бывшая история)
            2 -> displayItems.filter { it.isNote } // Заметки
            3 -> displayItems.filter { it.isPinned } // Избранное
            else -> displayItems
        }
    }

    val filteredItems = remember(tabFiltered, searchQuery) {
        if (searchQuery.isBlank()) tabFiltered
        else tabFiltered.filter { it.content.contains(searchQuery, ignoreCase = true) }
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
                                        val finalContent = annotatedStringToHtml(editingTextFieldValue.annotatedString)
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

                            OutlinedTextField(
                                value = editingTextFieldValue,
                                onValueChange = { newValue ->
                                    val updated = preserveSpansOnTextChange(editingTextFieldValue.annotatedString, newValue)
                                    if (updated.text != editingTextFieldValue.text) {
                                        val now = System.currentTimeMillis()
                                        val isWordBoundary = updated.text.endsWith(" ") || updated.text.endsWith("\n") || updated.text.length < editingTextFieldValue.text.length
                                        val isPause = (now - lastEditTimestamp) > 700L
                                        if (isWordBoundary || isPause || undoStack.isEmpty()) {
                                            pushUndoState()
                                        }
                                        lastEditTimestamp = now
                                    }
                                    editingTextFieldValue = updated
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                placeholder = { Text("Введите текст записи...", color = Color.Gray, fontSize = 14.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = yellowColor,
                                    unfocusedBorderColor = grayBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = yellowColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else if (selectedDetailItem != null) {
                            val item = selectedDetailItem!!
                            if (isEditingDetail) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { isEditingDetail = false }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                    }

                                    Text(
                                        text = "Редактирование",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Button(
                                        onClick = {
                                            val finalContent = annotatedStringToHtml(editingTextFieldValue.annotatedString)
                                            if (finalContent.isNotBlank()) {
                                                val updatedItem = item.copy(content = finalContent)
                                                ClipboardHistoryManager.updateItem(context, updatedItem)
                                                history = ClipboardHistoryManager.getHistory(context)
                                                selectedDetailItem = null
                                                isEditingDetail = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Сохранить", color = Color.Black, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editingTextFieldValue,
                                    onValueChange = { newValue ->
                                        val updated = preserveSpansOnTextChange(editingTextFieldValue.annotatedString, newValue)
                                        if (updated.text != editingTextFieldValue.text) {
                                            val now = System.currentTimeMillis()
                                            val isWordBoundary = updated.text.endsWith(" ") || updated.text.endsWith("\n") || updated.text.length < editingTextFieldValue.text.length
                                            val isPause = (now - lastEditTimestamp) > 700L
                                            if (isWordBoundary || isPause || undoStack.isEmpty()) {
                                                pushUndoState()
                                            }
                                            lastEditTimestamp = now
                                        }
                                        editingTextFieldValue = updated
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = yellowColor,
                                        unfocusedBorderColor = grayBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = yellowColor
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { selectedDetailItem = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = yellowColor)
                                    }
                                    Text(
                                        text = formatTimestamp(item.timestamp),
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 13.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = {
                                            ClipboardHistoryManager.copyToClipboard(context, item)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text(stringResource(R.string.menu_copy), color = yellowColor, fontSize = 13.sp)
                                        }
                                        TextButton(onClick = {
                                            onPasteItem(item.content)
                                        }) {
                                            Text(stringResource(R.string.menu_paste), color = yellowColor, fontSize = 13.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF252525))
                                        .clickable {
                                            overlayState = ClipboardOverlayState.EXPANDED
                                            val annotated = htmlToAnnotatedString(item.content)
                                            editingTextFieldValue = TextFieldValue(
                                                annotatedString = annotated,
                                                selection = TextRange(0, annotated.text.length)
                                            )
                                            isEditingDetail = true
                                        }
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = htmlToAnnotatedString(item.content),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ScrollableTabRow(
                                                selectedTabIndex = selectedTab,
                                                containerColor = Color.Transparent,
                                                contentColor = Color.White,
                                                edgePadding = 0.dp,
                                                modifier = Modifier.weight(1f),
                                                indicator = { tabPositions ->
                                                    if (selectedTab < tabPositions.size) {
                                                        TabRowDefaults.SecondaryIndicator(
                                                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            ) {
                                                Tab(
                                                    selected = selectedTab == 0,
                                                    onClick = { selectedTab = 0 },
                                                    text = { Text("Недавние", color = if (selectedTab == 0) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp) }
                                                )
                                                Tab(
                                                    selected = selectedTab == 1,
                                                    onClick = { selectedTab = 1 },
                                                    text = { Text("Все", color = if (selectedTab == 1) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp) }
                                                )
                                                Tab(
                                                    selected = selectedTab == 2,
                                                    onClick = { selectedTab = 2 },
                                                    text = { Text("Заметки", color = if (selectedTab == 2) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp) }
                                                )
                                                Tab(
                                                    selected = selectedTab == 3,
                                                    onClick = { selectedTab = 3 },
                                                    text = { Text("Избранное", color = if (selectedTab == 3) Color.White else Color(0xFFAAAAAA), fontSize = 13.sp) }
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    expandedMenuItemId = null
                                                    isSearchActive = true
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = yellowColor
                                                )
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
                                                showStarIcon = selectedTab != 3,
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

                        if (isEditingOrCreating && showColorPalette) {
                            ColorPaletteBar(
                                palette = defaultColorPalette,
                                activeColor = activeColor,
                                onSelectColor = { paletteItem ->
                                    pushUndoState()
                                    val newColor = if (paletteItem.isClear) Color.Unspecified else paletteItem.color
                                    if (!paletteItem.isClear) {
                                        activeColor = newColor
                                    }
                                    val format = if (paletteItem.isClear) FormatType.CLEAR else FormatType.COLOR
                                    val newAnnotated = applyStyleToAnnotatedString(
                                        current = editingTextFieldValue.annotatedString,
                                        selectionStart = editingTextFieldValue.selection.start,
                                        selectionEnd = editingTextFieldValue.selection.end,
                                        formatType = format,
                                        colorValue = newColor
                                    )
                                    editingTextFieldValue = editingTextFieldValue.copy(annotatedString = newAnnotated)
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

                                    // 3. Text Color
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    pushUndoState()
                                                    val colorToApply = if (activeColor == Color.Unspecified) Color(0xFFFF3B30) else activeColor
                                                    val newAnnotated = applyStyleToAnnotatedString(
                                                        current = editingTextFieldValue.annotatedString,
                                                        selectionStart = editingTextFieldValue.selection.start,
                                                        selectionEnd = editingTextFieldValue.selection.end,
                                                        formatType = FormatType.COLOR,
                                                        colorValue = colorToApply
                                                    )
                                                    editingTextFieldValue = editingTextFieldValue.copy(annotatedString = newAnnotated)
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
                                            pushUndoState()
                                            val newAnnotated = applyStyleToAnnotatedString(
                                                current = editingTextFieldValue.annotatedString,
                                                selectionStart = editingTextFieldValue.selection.start,
                                                selectionEnd = editingTextFieldValue.selection.end,
                                                formatType = FormatType.ITALIC
                                            )
                                            editingTextFieldValue = editingTextFieldValue.copy(annotatedString = newAnnotated)
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
                                            pushUndoState()
                                            val newAnnotated = applyStyleToAnnotatedString(
                                                current = editingTextFieldValue.annotatedString,
                                                selectionStart = editingTextFieldValue.selection.start,
                                                selectionEnd = editingTextFieldValue.selection.end,
                                                formatType = FormatType.BOLD
                                            )
                                            editingTextFieldValue = editingTextFieldValue.copy(annotatedString = newAnnotated)
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
                                            pushUndoState()
                                            val newAnnotated = applyStyleToAnnotatedString(
                                                current = editingTextFieldValue.annotatedString,
                                                selectionStart = editingTextFieldValue.selection.start,
                                                selectionEnd = editingTextFieldValue.selection.end,
                                                formatType = FormatType.UNDERLINE
                                            )
                                            editingTextFieldValue = editingTextFieldValue.copy(annotatedString = newAnnotated)
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

                                    // 7. Clear Format (Eraser)
                                    Surface(
                                        onClick = {
                                            pushUndoState()
                                            val newAnnotated = applyStyleToAnnotatedString(
                                                current = editingTextFieldValue.annotatedString,
                                                selectionStart = editingTextFieldValue.selection.start,
                                                selectionEnd = editingTextFieldValue.selection.end,
                                                formatType = FormatType.CLEAR
                                            )
                                            editingTextFieldValue = editingTextFieldValue.copy(annotatedString = newAnnotated)
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 1. Minimize to Circle button (Arrow forward with spring collapse animation)
                                        Surface(
                                            onClick = {
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
                                            shape = CircleShape,
                                            color = Color(0xFF333333),
                                            border = null,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "Minimize to Circle",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 2. Customizable mode button
                                        Surface(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .combinedClickable(
                                                    onClick = {
                                                        expandedMenuItemId = null
                                                        isCustomizableMode = !isCustomizableMode
                                                        if (!isCustomizableMode) isCornerResizeActive = false
                                                        Toast.makeText(
                                                            context,
                                                            if (isCustomizableMode) "Режим перемещения включен" else "Обычный режим",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onLongClick = {
                                                        expandedMenuItemId = null
                                                        isCustomizableMode = true
                                                        isCornerResizeActive = true
                                                        Toast.makeText(context, "Режим изменения углов активирован", Toast.LENGTH_SHORT).show()
                                                    }
                                                ),
                                            shape = CircleShape,
                                            color = if (isCustomizableMode) yellowColor else Color(0xFF333333),
                                            border = if (isCornerResizeActive) BorderStroke(2.dp, Color.White) else null
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenWith,
                                                    contentDescription = "Move / Resize Mode",
                                                    tint = if (isCustomizableMode) Color.Black else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 3. Force Sync button
                                        Surface(
                                            onClick = {
                                                expandedMenuItemId = null
                                                try {
                                                    val synced = ClipboardHistoryManager.syncSystemClipboard(context)
                                                    if (synced) {
                                                        history = ClipboardHistoryManager.getHistory(context)
                                                        Toast.makeText(context, "Скопировано из буфера", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Буфер обмена пуст или это скриншот", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("ClipboardOverlay", "Sync failed", e)
                                                }
                                            },
                                            shape = CircleShape,
                                            color = Color(0xFF333333),
                                            border = null,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Sync,
                                                    contentDescription = "Sync",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 4. Plus button (Create new item)
                                        Surface(
                                            onClick = {
                                                expandedMenuItemId = null
                                                overlayState = ClipboardOverlayState.EXPANDED
                                                isCreatingNewItem = true
                                            },
                                            shape = CircleShape,
                                            color = Color(0xFF333333),
                                            border = null,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Close button
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardCard(
    item: ClipboardItem,
    showStarIcon: Boolean = true,
    isMenuExpanded: Boolean = false,
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
                        onTap()
                    },
                    onLongClick = onToggleMenu
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF252525),
            border = BorderStroke(1.dp, Color(0xFF333333))
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
                            maxLines = 3,
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
