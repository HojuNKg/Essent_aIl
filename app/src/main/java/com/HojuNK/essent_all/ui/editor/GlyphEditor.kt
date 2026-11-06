package com.HojuNK.essent_all.ui.editor

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlyphAddPanel(
    initialPattern: Macro,
    apps: List<AppInfo>?,
    onDismiss: () -> Unit,
    onSave: (Macro) -> Unit
) {
    var currentPattern by rememberSaveable { mutableStateOf(initialPattern) }
    var undoStack by remember { mutableStateOf(listOf<Macro>()) }
    var redoStack by remember { mutableStateOf(listOf<Macro>()) }
    val isNew = remember { initialPattern.title.isEmpty() }

    val pagerState = rememberPagerState(pageCount = { 2 })

    fun pushToUndo() {
        undoStack = undoStack + currentPattern
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + currentPattern
            currentPattern = undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + currentPattern
            currentPattern = redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> StandardGlyphEditor(
                    pattern = currentPattern,
                    apps = apps,
                    isNew = isNew,
                    onPatternChange = {
                        pushToUndo()
                        currentPattern = it
                    },
                    onDismiss = onDismiss,
                    onSave = { onSave(currentPattern) }
                )
                1 -> ProfessionalGlyphEditor(
                    pattern = currentPattern,
                    apps = apps,
                    onPatternChange = {
                        pushToUndo()
                        currentPattern = it
                    },
                    onUndo = ::undo,
                    onRedo = ::redo,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onDismiss = onDismiss,
                    onSave = { onSave(currentPattern) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardGlyphEditor(
    pattern: Macro,
    apps: List<AppInfo>?,
    isNew: Boolean = false,
    onPatternChange: (Macro) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var showIconPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Header animation logic
    val collapseProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset.toFloat() / 200f).coerceIn(0f, 1f)
        }
    }
    val isCollapsed by remember {
        derivedStateOf { collapseProgress > 0.1f }
    }

    var editingActionIndex by remember { mutableIntStateOf(-1) }
    var showDelayPicker by remember { mutableStateOf(false) }
    var showRunnerPicker by remember { mutableStateOf(false) }
    var showStaticPicker by remember { mutableStateOf(false) }
    var showWavePicker by remember { mutableStateOf(false) }
    var showDrawPicker by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }

    // Stable keys for actions reordering
    var actionIds by rememberSaveable { mutableStateOf(List(pattern.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != pattern.actions.size) {
        if (actionIds.size < pattern.actions.size) {
            actionIds = actionIds + List(pattern.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(pattern.actions.size)
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        // Fixed Header Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f)
                .drawBehind {
                    val blurHeight = 90.dp.toPx()
                    drawRect(color = Color.Black, size = size)
                    if (collapseProgress > 0f) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = collapseProgress), Color.Transparent),
                                startY = size.height,
                                endY = size.height + blurHeight
                            ),
                            topLeft = Offset(0f, size.height),
                            size = Size(size.width, blurHeight)
                        )
                    }
                }
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundHeaderButton(icon = Icons.Default.Close, onClick = onDismiss)

                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = if (isNew) "СОЗДАЕМ УЗОР" else "РЕДАКТИРУЕМ УЗОР",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            alpha = collapseProgress
                            scaleX = 0.8f + (collapseProgress * 0.2f)
                            scaleY = 0.8f + (collapseProgress * 0.2f)
                        }
                    )
                }

                RoundHeaderButton(
                    icon = Icons.Default.Check,
                    onClick = { if (pattern.title.isNotEmpty()) onSave() },
                    backgroundColor = Color(0xFF2B2B2B),
                    borderColor = null
                )
            }

            AnimatedVisibility(
                visible = isCollapsed,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .graphicsLayer { alpha = collapseProgress },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.75f)
                                .fillMaxHeight()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                .clickable { scope.launch { scrollState.animateScrollToItem(0) } }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = pattern.title.ifEmpty { "Узор без названия" },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.25f)
                                .fillMaxHeight()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                .clickable { showIconPicker = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EventIconDisplay(pattern.icon.ifEmpty { "✨" }, displaySize = 24.dp)
                        }
                    }
                }
            }
        }

        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(120.dp * collapseProgress))
            }

            item {
                Column(modifier = Modifier.graphicsLayer {
                    alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                    translationY = -collapseProgress * 40f
                }) {
                    Text(
                        text = if (isNew) "СОЗДАЕМ НОВЫЙ\nУЗОР" else "РЕДАКТИРУЕМ\nУЗОР",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .graphicsLayer {
                            alpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
                            translationY = -collapseProgress * 30f
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight()
                            .background(Color.Black, RoundedCornerShape(24.dp))
                            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Название узора", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = pattern.title,
                                onValueChange = { onPatternChange(pattern.copy(title = it)) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (pattern.title.isEmpty()) {
                                        Text("Узор без названия", color = Color.Gray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    LabeledPanel(
                        label = "Значёк",
                        value = pattern.icon.ifEmpty { "✨" },
                        modifier = Modifier.weight(0.25f).fillMaxHeight(),
                        onClick = { showIconPicker = true }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            item {
                Text(stringResource(R.string.macro_actions_label), color = Color.White, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
            }

            itemsIndexed(pattern.actions, key = { index, _ -> actionIds.getOrElse(index) { index.toString() } }) { index, action ->
                ActionRow(
                    action = action,
                    index = index,
                    apps = apps ?: emptyList(),
                    glyphPatterns = listOf(pattern),
                    isDragged = draggedIndex == index,
                    onDragStart = { draggedIndex = index },
                    onDragEnd = { draggedIndex = -1 },
                    onEdit = {
                        editingActionIndex = index
                        when {
                            action.startsWith(Config.ACTION_DELAY_PREFIX) -> showDelayPicker = true
                            action.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX) -> showStaticPicker = true
                            action.startsWith(Config.ACTION_GLYPH_WAVE_PREFIX) -> showWavePicker = true
                            action.startsWith(Config.ACTION_GLYPH_DRAW_PREFIX) -> showDrawPicker = true
                            else -> showRunnerPicker = true
                        }
                    },
                    onMove = { from, to ->
                        if (to in pattern.actions.indices) {
                            val newList = pattern.actions.toMutableList()
                            val item = newList.removeAt(from)
                            newList.add(to, item)

                            val newIds = actionIds.toMutableList()
                            val id = newIds.removeAt(from)
                            newIds.add(to, id)

                            onPatternChange(pattern.copy(actions = newList))
                            actionIds = newIds
                            draggedIndex = to
                        }
                    },
                    onDelete = {
                        val newList = pattern.actions.toMutableList().apply { removeAt(index) }
                        val newIds = actionIds.toMutableList().apply { removeAt(index) }
                        onPatternChange(pattern.copy(actions = newList))
                        actionIds = newIds
                        if (editingActionIndex == index) editingActionIndex = -1
                    }
                )

                Spacer(Modifier.height(8.dp))
                if (index < pattern.actions.size - 1) {
                    ConnectingLine()
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelAddButton(
                        label = "ЗАДЕРЖКА",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_DELAY_PREFIX}0.5"
                            onPatternChange(pattern.copy(actions = pattern.actions + action))
                        }
                    )
                    PanelAddButton(
                        label = "БЕГУНОК",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_GLYPH_STEP_RUNNER_PREFIX}20:1.0:false:false:3:1.0"
                            val newList = pattern.actions + action
                            onPatternChange(pattern.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showRunnerPicker = true
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelAddButton(
                        label = "СТАТИЧНЫЙ",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_GLYPH_STATIC_PREFIX}1:500"
                            val newList = pattern.actions + action
                            onPatternChange(pattern.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showStaticPicker = true
                        }
                    )
                    PanelAddButton(
                        label = "ВОЛНА",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_GLYPH_WAVE_PREFIX}20:3:0.1:0.2:1.0:true"
                            val newList = pattern.actions + action
                            onPatternChange(pattern.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showWavePicker = true
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                PanelAddButton(
                    label = "РИСУНОК",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val action = Config.ACTION_GLYPH_DRAW_PREFIX
                        val newList = pattern.actions + action
                        onPatternChange(pattern.copy(actions = newList))
                        editingActionIndex = newList.size - 1
                        showDrawPicker = true
                    }
                )

                Spacer(Modifier.height(120.dp))
            }
        }

        if (showDelayPicker && editingActionIndex != -1) {
            val action = pattern.actions[editingActionIndex]
            DelayPickerDialog(
                initialValue = action.removePrefix(Config.ACTION_DELAY_PREFIX),
                onDismiss = { showDelayPicker = false; editingActionIndex = -1 },
                onConfirmed = { sec ->
                    val newList = pattern.actions.toMutableList()
                    newList[editingActionIndex] = "${Config.ACTION_DELAY_PREFIX}$sec"
                    onPatternChange(pattern.copy(actions = newList))
                    showDelayPicker = false; editingActionIndex = -1
                }
            )
        }

        if (showRunnerPicker && editingActionIndex != -1) {
            val action = pattern.actions[editingActionIndex]
            val parts = action.substringAfter(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX).split(":")
            val startLedId = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val speed = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
            val reverse = parts.getOrNull(2)?.toBoolean() ?: false
            val tail = parts.getOrNull(3)?.toBoolean() ?: false
            val tailLen = parts.getOrNull(4)?.toIntOrNull() ?: 3
            val fadeSpeed = parts.getOrNull(5)?.toFloatOrNull() ?: 1.0f
            
            GlyphRunnerPicker(
                initialStartLed = startLedId,
                initialSpeed = speed,
                initialReverse = reverse,
                initialTail = tail,
                initialTailLen = tailLen,
                initialFadeSpeed = fadeSpeed,
                onDismiss = { showRunnerPicker = false; editingActionIndex = -1 },
                onConfirm = { startId, s, r, t, tl, fs ->
                    val newList = pattern.actions.toMutableList()
                    newList[editingActionIndex] = "${Config.ACTION_GLYPH_STEP_RUNNER_PREFIX}$startId:$s:$r:$t:$tl:$fs"
                    onPatternChange(pattern.copy(actions = newList))
                    showRunnerPicker = false
                    editingActionIndex = -1
                }
            )
        }

        if (showStaticPicker && editingActionIndex != -1) {
            val action = pattern.actions[editingActionIndex]
            val parts = action.removePrefix(Config.ACTION_GLYPH_STATIC_PREFIX).split(":")
            val areasStr = parts.getOrNull(0) ?: "1"
            val areas = areasStr.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            val time = parts.getOrNull(1)?.toIntOrNull() ?: 500
            
            GlyphStaticPicker(
                initialAreas = areas,
                initialTime = time,
                onDismiss = { showStaticPicker = false; editingActionIndex = -1 },
                onConfirm = { aSet, t ->
                    val newList = pattern.actions.toMutableList()
                    val aStr = aSet.sorted().joinToString(",")
                    newList[editingActionIndex] = "${Config.ACTION_GLYPH_STATIC_PREFIX}$aStr:$t"
                    onPatternChange(pattern.copy(actions = newList))
                    showStaticPicker = false
                    editingActionIndex = -1
                }
            )
        }

        if (showDrawPicker && editingActionIndex != -1) {
            val action = pattern.actions[editingActionIndex]
            val data = action.removePrefix(Config.ACTION_GLYPH_DRAW_PREFIX)
            val parts = data.split(":")
            
            val hasSettings = parts.size >= 4
            val duration = if (hasSettings) parts[0].toIntOrNull() ?: 1000 else 1000
            val fade = if (hasSettings) parts[1].toBoolean() else false
            val fadeSpeed = if (hasSettings) parts[2].toFloatOrNull() ?: 1.0f else 1.0f
            val idsStr = if (hasSettings) parts[3] else parts[0]
            
            val activeLeds = idsStr.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }.toSet()
            
            GlyphDrawPicker(
                initialSelected = activeLeds,
                initialDuration = duration,
                initialFade = fade,
                initialFadeSpeed = fadeSpeed,
                onDismiss = { showDrawPicker = false; editingActionIndex = -1 },
                onConfirm = { selected, dur, f, fs ->
                    val newList = pattern.actions.toMutableList()
                    newList[editingActionIndex] = "${Config.ACTION_GLYPH_DRAW_PREFIX}$dur:$f:$fs:${selected.joinToString(",")}"
                    onPatternChange(pattern.copy(actions = newList))
                    showDrawPicker = false
                    editingActionIndex = -1
                }
            )
        }

        if (showWavePicker && editingActionIndex != -1) {
            val action = pattern.actions[editingActionIndex]
            val parts = action.removePrefix(Config.ACTION_GLYPH_WAVE_PREFIX).split(":")
            val startLed = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val count = parts.getOrNull(1)?.toIntOrNull() ?: 3
            val thickness = parts.getOrNull(2)?.toFloatOrNull() ?: 0.1f
            val distance = parts.getOrNull(3)?.toFloatOrNull() ?: 0.2f
            val speed = parts.getOrNull(4)?.toFloatOrNull() ?: 1.0f
            val allZones = parts.getOrNull(5)?.toBoolean() ?: true

            GlyphWavePicker(
                initialStartLed = startLed,
                initialCount = count,
                initialThickness = thickness,
                initialDistance = distance,
                initialSpeed = speed,
                initialAllZones = allZones,
                onDismiss = { showWavePicker = false; editingActionIndex = -1 },
                onConfirm = { sl, c, t, d, s, az ->
                    val newList = pattern.actions.toMutableList()
                    newList[editingActionIndex] = "${Config.ACTION_GLYPH_WAVE_PREFIX}$sl:$c:$t:$d:$s:$az"
                    onPatternChange(pattern.copy(actions = newList))
                    showWavePicker = false
                    editingActionIndex = -1
                }
            )
        }

        if (showIconPicker) {
            EventIconPickerDialog(
                currentIcon = pattern.icon,
                apps = apps,
                onDismiss = { showIconPicker = false },
                onIconPicked = { icon ->
                    onPatternChange(pattern.copy(icon = icon))
                    showIconPicker = false
                }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfessionalGlyphEditor(
    pattern: Macro,
    apps: List<AppInfo>?,
    onPatternChange: (Macro) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedActionIndex by remember { mutableIntStateOf(-1) }
    var selectedMediaId by remember { mutableStateOf<String?>(null) }
    
    // Pickers
    var showDelayPicker by remember { mutableStateOf(false) }
    var showRunnerPicker by remember { mutableStateOf(false) }
    var showStaticPicker by remember { mutableStateOf(false) }
    var showWavePicker by remember { mutableStateOf(false) }
    var showDrawPicker by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    // Playback State
    var isPlaying by remember { mutableStateOf(false) }
    var playbackTime by remember { mutableLongStateOf(0L) }
    var videoSpeed by remember { mutableFloatStateOf(1f) }

    // Stable keys for actions reordering
    var actionIds by rememberSaveable { mutableStateOf(List(pattern.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != pattern.actions.size) {
        if (actionIds.size < pattern.actions.size) {
            actionIds = actionIds + List(pattern.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(pattern.actions.size)
        }
    }
    
    val timelineData = remember(pattern.actions, actionIds) { calculateMacroTimeline(pattern.actions, actionIds) }
    val actionsDuration = timelineData.lastOrNull()?.let { it.startTime + it.duration } ?: 0L
    val mediaDuration = pattern.mediaItems.sumOf { it.duration }
    val totalDuration = maxOf(actionsDuration, mediaDuration)

    LaunchedEffect(totalDuration) {
        if (playbackTime > totalDuration) playbackTime = totalDuration
    }

    val timelineScrollState = rememberScrollState()
    var showMediaSpeedDialog by remember { mutableStateOf(false) }
    
    // Preview Styles
    var previewStyle by remember { mutableIntStateOf(0) } // 0: Mock, 1: Wallpaper, 2: Video
    var aspectError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val pickWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { sourceUri ->
                scope.launch {
                    try {
                        val fileName = "media_${UUID.randomUUID()}.jpg"
                        val destFile = java.io.File(ctx.filesDir, fileName)
                        ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        
                        val newItem = MediaItem(id = UUID.randomUUID().toString(), type = MediaType.PHOTO, path = destFile.absolutePath, startTime = 0L, duration = 1000L, fullDuration = 3600000L)
                        var newList = listOf(newItem) + pattern.mediaItems
                        var currentT = 0L
                        newList = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                        onPatternChange(pattern.copy(mediaItems = newList))
                        previewStyle = 1
                    } catch (e: Exception) { aspectError = "Ошибка при сохранении изображения" }
                }
            }
        }
    )

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { sourceUri ->
                scope.launch {
                    try {
                        val fileName = "media_${UUID.randomUUID()}.mp4"
                        val destFile = java.io.File(ctx.filesDir, fileName)
                        ctx.contentResolver.openInputStream(sourceUri)?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(destFile.absolutePath)
                        val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 5000L
                        retriever.release()

                        val newItem = MediaItem(id = UUID.randomUUID().toString(), type = MediaType.VIDEO, path = destFile.absolutePath, startTime = 0L, duration = duration, fullDuration = duration)
                        var newList = listOf(newItem) + pattern.mediaItems
                        var currentT = 0L
                        newList = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                        onPatternChange(pattern.copy(mediaItems = newList))
                        previewStyle = 2
                    } catch (e: Exception) { aspectError = "Ошибка при сохранении видео" }
                }
            }
        }
    )

    if (aspectError != null) {
        AlertDialog(
            onDismissRequest = { aspectError = null },
            title = { Text("Ошибка") },
            text = { Text(aspectError!!) },
            confirmButton = { TextButton(onClick = { aspectError = null }) { Text("ОК") } },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.Gray
        )
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startSysTime = System.currentTimeMillis()
            val initialPlaybackTime = playbackTime
            while (isPlaying && playbackTime < totalDuration) {
                val elapsed = System.currentTimeMillis() - startSysTime
                playbackTime = (initialPlaybackTime + elapsed).coerceAtMost(totalDuration)
                if (playbackTime >= totalDuration) isPlaying = false
                delay(16)
            }
        }
    }

    var timelineExpanded by remember { mutableStateOf(false) }
    val extraTimelineHeight by animateDpAsState(if (timelineExpanded) 80.dp else 0.dp)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
        // 1. Top Bar
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)) {
            RoundHeaderButton(icon = Icons.Default.Close, onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart))
            
            Row(modifier = Modifier.align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (canUndo) Color.White else Color.Gray)
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (canRedo) Color.White else Color.Gray)
                }
            }

            Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.align(Alignment.TopEnd).width(48.dp)) {
                RoundHeaderButton(icon = Icons.Default.Check, onClick = { if (pattern.title.isNotEmpty()) onSave() }, backgroundColor = Color(0xFF2B2B2B), borderColor = null)
                Text(text = String.format(Locale.US, "%02d:%02d / %02d:%02d", (playbackTime / 1000) / 60, (playbackTime / 1000) % 60, (totalDuration / 1000) / 60, (totalDuration / 1000) % 60), color = Color.Gray, style = MaterialTheme.typography.labelLarge, softWrap = false, modifier = Modifier.padding(top = 110.dp).wrapContentWidth(unbounded = true).graphicsLayer { rotationZ = 90f })
            }
        }

        // 2. Interactive Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left: Glyph Action Buttons
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 24.dp).width(48.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom), horizontalAlignment = Alignment.CenterHorizontally) {
                SmallActionButton(icon = Icons.Default.Timer, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onPatternChange(pattern.copy(actions = pattern.actions + "${Config.ACTION_DELAY_PREFIX}0.5"))
                })
                SmallActionButton(icon = Icons.Default.FlashlightOn, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onPatternChange(pattern.copy(actions = pattern.actions + "${Config.ACTION_GLYPH_STEP_RUNNER_PREFIX}20:1.0:false:false:3:1.0"))
                })
                SmallActionButton(icon = Icons.Default.Square, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onPatternChange(pattern.copy(actions = pattern.actions + "${Config.ACTION_GLYPH_STATIC_PREFIX}1:500"))
                })
                SmallActionButton(icon = Icons.Default.Waves, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onPatternChange(pattern.copy(actions = pattern.actions + "${Config.ACTION_GLYPH_WAVE_PREFIX}20:3:0.1:0.2:1.0:true"))
                })
                SmallActionButton(icon = Icons.Default.Draw, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onPatternChange(pattern.copy(actions = pattern.actions + Config.ACTION_GLYPH_DRAW_PREFIX))
                })
            }

            // Center: Preview (Schematic)
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.TopCenter) {
                Box(modifier = Modifier.offset(y = (-55).dp).aspectOf(9f / 19.5f, scale = 1.1f).border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(Color(0xFF0D0D0D))) {
                    val activeItem = pattern.mediaItems.find { playbackTime >= it.startTime && playbackTime <= (it.startTime + it.duration) }
                    when (previewStyle) {
                        0 -> MockDesktop()
                        1 -> WallpaperPreview(activeItem?.takeIf { it.type == MediaType.PHOTO }?.path)
                        2 -> VideoPreview(localPath = activeItem?.takeIf { it.type == MediaType.VIDEO }?.path, itemStartTime = activeItem?.startTime ?: 0L, playbackTime = playbackTime, isPlaying = isPlaying, speed = videoSpeed, startOffset = activeItem?.startOffset ?: 0L)
                    }
                    
                    GlyphSchematic(
                        modifier = Modifier.fillMaxSize(),
                        activeLeds = emptySet() 
                    )
                }
            }

            // Right: Playback and Style Controls
            Column(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp).width(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)) {
                Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { pickWallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(36.dp).background(Color(0xFF2B2B2B), CircleShape)) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }, modifier = Modifier.size(36.dp).background(Color(0xFF2B2B2B), CircleShape)) { Icon(Icons.Default.VideoCall, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
                Column(modifier = Modifier.width(44.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(22.dp)).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PreviewStyleIcon(icon = Icons.Default.Dashboard, isSelected = previewStyle == 0, onClick = { previewStyle = 0 })
                    PreviewStyleIcon(icon = Icons.Default.Wallpaper, isSelected = previewStyle == 1, onClick = { previewStyle = 1 })
                    PreviewStyleIcon(icon = Icons.Default.Videocam, isSelected = previewStyle == 2, onClick = { previewStyle = 2 })
                }
                IconButton(onClick = { if (playbackTime >= totalDuration) playbackTime = 0; isPlaying = !isPlaying }, modifier = Modifier.size(44.dp).background(Color(0xFFFFD700), CircleShape)) { Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = Color.Black) }
            }
        }

        // 3. Bottom: Timeline Strip
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF121212), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).padding(bottom = 92.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(32.dp).pointerInput(Unit) { detectVerticalDragGestures { _, dragAmount -> if (dragAmount < -10) timelineExpanded = true; if (dragAmount > 10) timelineExpanded = false } }.clickable { timelineExpanded = !timelineExpanded }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(40.dp, 4.dp).background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
            }
            if (extraTimelineHeight > 0.dp) {
                Box(modifier = Modifier.fillMaxWidth().height(extraTimelineHeight)) {
                    val itemsToShow = when (previewStyle) { 1 -> pattern.mediaItems.filter { it.type == MediaType.PHOTO }; 2 -> pattern.mediaItems.filter { it.type == MediaType.VIDEO }; else -> emptyList() }
                    MediaTrackTimeline(label = if (previewStyle == 1) "фото" else "видео", color = Color.Gray, totalDuration = totalDuration, scrollState = timelineScrollState, mediaItems = itemsToShow, selectedMediaId = selectedMediaId, onSelect = { selectedMediaId = it }, onDelete = { id -> onPatternChange(pattern.copy(mediaItems = pattern.mediaItems.filter { it.id != id })); if (selectedMediaId == id) selectedMediaId = null }, onMediaChange = { id, updatedItem -> val newList = pattern.mediaItems.map { if (it.id == id) updatedItem else it }; var currentT = 0L; val finalItems = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }; onPatternChange(pattern.copy(mediaItems = finalItems)) }, onMoveOrder = { from, to -> val newList = pattern.mediaItems.toMutableList(); val item = newList.removeAt(from); newList.add(to, item); var currentT = 0L; val finalItems = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }; onPatternChange(pattern.copy(mediaItems = finalItems)) }, onSpeedClick = if (previewStyle == 2) ({ showMediaSpeedDialog = true }) else null)
                }
            }
            Spacer(Modifier.height(8.dp))
            AdvancedTimeline(
                timelineData = timelineData, scrollState = timelineScrollState, playbackTime = playbackTime, totalDuration = totalDuration, isPlaying = isPlaying, selectedIndex = selectedActionIndex, onSelect = { selectedActionIndex = it; timelineData.getOrNull(it)?.let { data -> playbackTime = data.startTime } }, 
                onEdit = { index ->
                    selectedActionIndex = index
                    val action = pattern.actions[index]
                    when {
                        action.startsWith(Config.ACTION_DELAY_PREFIX) -> showDelayPicker = true
                        action.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX) -> showStaticPicker = true
                        action.startsWith(Config.ACTION_GLYPH_WAVE_PREFIX) -> showWavePicker = true
                        action.startsWith(Config.ACTION_GLYPH_DRAW_PREFIX) -> showDrawPicker = true
                        else -> showRunnerPicker = true
                    }
                }, 
                onDelete = { index -> val newList = pattern.actions.toMutableList().apply { removeAt(index) }; val newIds = actionIds.toMutableList().apply { removeAt(index) }; actionIds = newIds; onPatternChange(pattern.copy(actions = newList)); selectedActionIndex = -1 }, 
                onSeek = { playbackTime = it; isPlaying = false }, 
                onMove = { from, to -> if (to in pattern.actions.indices) { val newList = pattern.actions.toMutableList(); val item = newList.removeAt(from); newList.add(to, item); val newIds = actionIds.toMutableList(); val id = newIds.removeAt(from); newIds.add(to, id); actionIds = newIds; onPatternChange(pattern.copy(actions = newList)); selectedActionIndex = to } }, 
                onDurationChange = { index, newDuration ->
                    val action = pattern.actions[index]
                    val newAction = when {
                        action.startsWith(Config.ACTION_DELAY_PREFIX) -> "${Config.ACTION_DELAY_PREFIX}${newDuration / 1000f}"
                        action.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX) -> {
                            val pts = action.removePrefix(Config.ACTION_GLYPH_STATIC_PREFIX).split(":")
                            "${Config.ACTION_GLYPH_STATIC_PREFIX}${pts.getOrNull(0) ?: "1"}:$newDuration"
                        }
                        else -> action
                    }
                    val newList = pattern.actions.toMutableList().apply { set(index, newAction) }
                    onPatternChange(pattern.copy(actions = newList))
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showMediaSpeedDialog) SpeedCoefficientDialog(currentSpeed = videoSpeed, onDismiss = { showMediaSpeedDialog = false }, onConfirmed = { videoSpeed = it; showMediaSpeedDialog = false })

    // Pickers
    if (showDelayPicker && selectedActionIndex != -1) {
        val action = pattern.actions[selectedActionIndex]
        DelayPickerDialog(initialValue = action.removePrefix(Config.ACTION_DELAY_PREFIX), onDismiss = { showDelayPicker = false; selectedActionIndex = -1 }, onConfirmed = { sec -> val newList = pattern.actions.toMutableList(); newList[selectedActionIndex] = "${Config.ACTION_DELAY_PREFIX}$sec"; onPatternChange(pattern.copy(actions = newList)); showDelayPicker = false; selectedActionIndex = -1 })
    }
    if (showRunnerPicker && selectedActionIndex != -1) {
        val action = pattern.actions[selectedActionIndex]; val parts = action.substringAfter(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX).split(":")
        val startLedId = parts.getOrNull(0)?.toIntOrNull() ?: 20; val speed = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f; val reverse = parts.getOrNull(2)?.toBoolean() ?: false; val tail = parts.getOrNull(3)?.toBoolean() ?: false; val tailLen = parts.getOrNull(4)?.toIntOrNull() ?: 3; val fadeSpeed = parts.getOrNull(5)?.toFloatOrNull() ?: 1.0f
        GlyphRunnerPicker(initialStartLed = startLedId, initialSpeed = speed, initialReverse = reverse, initialTail = tail, initialTailLen = tailLen, initialFadeSpeed = fadeSpeed, onDismiss = { showRunnerPicker = false; selectedActionIndex = -1 }, onConfirm = { startId, s, r, t, tl, fs -> val newList = pattern.actions.toMutableList(); newList[selectedActionIndex] = "${Config.ACTION_GLYPH_STEP_RUNNER_PREFIX}$startId:$s:$r:$t:$tl:$fs"; onPatternChange(pattern.copy(actions = newList)); showRunnerPicker = false; selectedActionIndex = -1 })
    }
    if (showStaticPicker && selectedActionIndex != -1) {
        val action = pattern.actions[selectedActionIndex]; val parts = action.removePrefix(Config.ACTION_GLYPH_STATIC_PREFIX).split(":"); val areasStr = parts.getOrNull(0) ?: "1"; val areas = areasStr.split(",").mapNotNull { it.toIntOrNull() }.toSet(); val time = parts.getOrNull(1)?.toIntOrNull() ?: 500
        GlyphStaticPicker(initialAreas = areas, initialTime = time, onDismiss = { showStaticPicker = false; selectedActionIndex = -1 }, onConfirm = { aSet, t -> val newList = pattern.actions.toMutableList(); val aStr = aSet.sorted().joinToString(","); newList[selectedActionIndex] = "${Config.ACTION_GLYPH_STATIC_PREFIX}$aStr:$t"; onPatternChange(pattern.copy(actions = newList)); showStaticPicker = false; selectedActionIndex = -1 })
    }
    if (showDrawPicker && selectedActionIndex != -1) {
        val action = pattern.actions[selectedActionIndex]; val data = action.removePrefix(Config.ACTION_GLYPH_DRAW_PREFIX); val parts = data.split(":"); val hasSettings = parts.size >= 4; val duration = if (hasSettings) parts[0].toIntOrNull() ?: 1000 else 1000; val fade = if (hasSettings) parts[1].toBoolean() else false; val fadeSpeed = if (hasSettings) parts[2].toFloatOrNull() ?: 1.0f else 1.0f; val idsStr = if (hasSettings) parts[3] else parts[0]; val activeLeds = idsStr.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }.toSet()
        GlyphDrawPicker(initialSelected = activeLeds, initialDuration = duration, initialFade = fade, initialFadeSpeed = fadeSpeed, onDismiss = { showDrawPicker = false; selectedActionIndex = -1 }, onConfirm = { selected, dur, f, fs -> val newList = pattern.actions.toMutableList(); newList[selectedActionIndex] = "${Config.ACTION_GLYPH_DRAW_PREFIX}$dur:$f:$fs:${selected.joinToString(",")}"; onPatternChange(pattern.copy(actions = newList)); showDrawPicker = false; selectedActionIndex = -1 })
    }
    if (showWavePicker && selectedActionIndex != -1) {
        val action = pattern.actions[selectedActionIndex]; val parts = action.removePrefix(Config.ACTION_GLYPH_WAVE_PREFIX).split(":"); val startLed = parts.getOrNull(0)?.toIntOrNull() ?: 20; val count = parts.getOrNull(1)?.toIntOrNull() ?: 3; val thickness = parts.getOrNull(2)?.toFloatOrNull() ?: 0.1f; val distance = parts.getOrNull(3)?.toFloatOrNull() ?: 0.2f; val speed = parts.getOrNull(4)?.toFloatOrNull() ?: 1.0f; val allZones = parts.getOrNull(5)?.toBoolean() ?: true
        GlyphWavePicker(initialStartLed = startLed, initialCount = count, initialThickness = thickness, initialDistance = distance, initialSpeed = speed, initialAllZones = allZones, onDismiss = { showWavePicker = false; selectedActionIndex = -1 }, onConfirm = { sl, c, t, d, s, az -> val newList = pattern.actions.toMutableList(); newList[selectedActionIndex] = "${Config.ACTION_GLYPH_WAVE_PREFIX}$sl:$c:$t:$d:$s:$az"; onPatternChange(pattern.copy(actions = newList)); showWavePicker = false; selectedActionIndex = -1 })
    }
}
