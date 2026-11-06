package com.HojuNK.essent_all.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MacroAddPanel(
    initialMacro: Macro,
    apps: List<AppInfo>?,
    onDismiss: () -> Unit,
    onSave: (Macro) -> Unit
) {
    var currentMacro by rememberSaveable { mutableStateOf(initialMacro) }
    var undoStack by remember { mutableStateOf(listOf<Macro>()) }
    var redoStack by remember { mutableStateOf(listOf<Macro>()) }
    val isNew = remember { initialMacro.title.isEmpty() }

    val pagerState = rememberPagerState(pageCount = { 2 })

    fun pushToUndo() {
        undoStack = undoStack + currentMacro
        redoStack = emptyList()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack = redoStack + currentMacro
            currentMacro = undoStack.last()
            undoStack = undoStack.dropLast(1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack = undoStack + currentMacro
            currentMacro = redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> StandardMacroEditor(
                    macro = currentMacro,
                    apps = apps,
                    isNew = isNew,
                    onMacroChange = {
                        pushToUndo()
                        currentMacro = it
                    },
                    onDismiss = onDismiss,
                    onSave = { onSave(currentMacro) }
                )
                1 -> ProfessionalMacroEditor(
                    macro = currentMacro,
                    apps = apps,
                    onMacroChange = {
                        pushToUndo()
                        currentMacro = it
                    },
                    onUndo = ::undo,
                    onRedo = ::redo,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    onDismiss = onDismiss,
                    onSave = { onSave(currentMacro) }
                )
            }
        }
    }
}

@Composable
fun StandardMacroEditor(
    macro: Macro,
    apps: List<AppInfo>?,
    isNew: Boolean = false,
    onMacroChange: (Macro) -> Unit,
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
    var showTouchPicker by remember { mutableStateOf(false) }
    var showSwipePicker by remember { mutableStateOf(false) }
    var showPreciseSwipePicker by remember { mutableStateOf(false) }
    var showDelayPickerInList by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var showFullAppPicker by remember { mutableStateOf(false) }

    var showActionPicker by remember { mutableStateOf(false) }
    var showBrightnessPicker by remember { mutableStateOf(false) }
    var showVolumePicker by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showUrlPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showWaitConfirmationPicker by remember { mutableStateOf(false) }
    var showAutoBrightnessPicker by remember { mutableStateOf(false) }

    // Stable keys for actions reordering
    var actionIds by rememberSaveable { mutableStateOf(List(macro.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != macro.actions.size) {
        if (actionIds.size < macro.actions.size) {
            actionIds = actionIds + List(macro.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(macro.actions.size)
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
                        text = if (isNew) stringResource(R.string.macro_editor_create_collapsed) else stringResource(R.string.macro_editor_edit_collapsed),
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
                    onClick = { if (macro.title.isNotEmpty()) onSave() },
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
                                text = macro.title.ifEmpty { "Макрос без названия" },
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
                            EventIconDisplay(macro.icon.ifEmpty { "🤖" }, displaySize = 24.dp)
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
                // Dynamic spacer to push content below the expanded fixed header
                Spacer(Modifier.height(120.dp * collapseProgress))
            }

            item {
                Column(modifier = Modifier.graphicsLayer {
                    alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                    translationY = -collapseProgress * 40f
                }) {
                    Text(
                        text = if (isNew) stringResource(R.string.macro_editor_create_expanded) else stringResource(R.string.macro_editor_edit_expanded),
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
                            Text("Название макроса", color = Color.White, style = MaterialTheme.typography.labelSmall)
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
                                value = macro.title,
                                onValueChange = { onMacroChange(macro.copy(title = it)) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (macro.title.isEmpty()) {
                                        Text("Макрос без названия", color = Color.Gray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    LabeledPanel(
                        label = "Значёк",
                        value = macro.icon.ifEmpty { "🤖" },
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

            itemsIndexed(macro.actions, key = { index, _ -> actionIds.getOrElse(index) { index.toString() } }) { index, action ->
                ActionRow(
                    action = action,
                    index = index,
                    apps = apps ?: emptyList(),
                    isDragged = draggedIndex == index,
                    onDragStart = { draggedIndex = index },
                    onDragEnd = { draggedIndex = -1 },
                    onEdit = {
                        editingActionIndex = index
                        when {
                            action.startsWith(Config.ACTION_TOUCH_PREFIX) -> showTouchPicker = true
                            action.startsWith(Config.ACTION_SWIPE_PREFIX) -> showSwipePicker = true
                            action.startsWith(Config.ACTION_PATH_PREFIX) -> showPreciseSwipePicker = true
                            action.startsWith(Config.ACTION_DELAY_PREFIX) -> showDelayPickerInList = true
                            else -> showFullAppPicker = true
                        }
                    },
                    onMove = { from, to ->
                        if (to in macro.actions.indices) {
                            val newList = macro.actions.toMutableList()
                            val item = newList.removeAt(from)
                            newList.add(to, item)

                            val newIds = actionIds.toMutableList()
                            val id = newIds.removeAt(from)
                            newIds.add(to, id)

                            onMacroChange(macro.copy(actions = newList))
                            actionIds = newIds
                            draggedIndex = to
                        }
                    },
                    onDelete = {
                        val newList = macro.actions.toMutableList().apply { removeAt(index) }
                        val newIds = actionIds.toMutableList().apply { removeAt(index) }
                        onMacroChange(macro.copy(actions = newList))
                        actionIds = newIds
                        if (editingActionIndex == index) editingActionIndex = -1
                    }
                )

                Spacer(Modifier.height(8.dp))
                if (index < macro.actions.size - 1) {
                    ConnectingLine()
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Spacer(Modifier.height(12.dp))

                // First Row: Tap & Swipe
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelAddButton(
                        label = stringResource(R.string.touch_btn),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_TOUCH_PREFIX}540_1100_200"
                            val newList = macro.actions + action
                            onMacroChange(macro.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showTouchPicker = true
                        }
                    )
                    PanelAddButton(
                        label = stringResource(R.string.swipe_btn),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_SWIPE_PREFIX}300_1100_700_1100_0_0"
                            val newList = macro.actions + action
                            onMacroChange(macro.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showSwipePicker = true
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Second Row: Precise Swipe & Delay
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelAddButton(
                        label = "ТОЧНЫЙ С.",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val action = "${Config.ACTION_PATH_PREFIX}540,1100,0;540,1300,500"
                            val newList = macro.actions + action
                            onMacroChange(macro.copy(actions = newList))
                            editingActionIndex = newList.size - 1
                            showPreciseSwipePicker = true
                        }
                    )
                    PanelAddButton(
                        label = "ЗАДЕРЖКА",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_DELAY_PREFIX}0.5"))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Third Row: Wide Action Button
                PanelAddButton(
                    label = "ДОБАВИТЬ ДЕЙСТВИЕ",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showActionPicker = true }
                )

                Spacer(Modifier.height(120.dp))
            }
        }

        if (showActionPicker) {
            EventActionPickerDialog(
                triggerGesture = TapSlot.SINGLE,
                allowMacros = false,
                onDismiss = { showActionPicker = false },
                onPick = { pkg ->
                    when (pkg) {
                        "action:pick_touch" -> { editingActionIndex = -1; showTouchPicker = true }
                        "action:pick_swipe" -> { editingActionIndex = -1; showSwipePicker = true }
                        "action:pick_delay" -> { editingActionIndex = -1; showDelayPickerInList = true }
                        "action:pick_brightness" -> showBrightnessPicker = true
                        "action:pick_volume" -> showVolumePicker = true
                        "action:pick_media" -> showMediaPicker = true
                        "action:pick_url" -> showUrlPicker = true
                        "action:pick_theme" -> showThemePicker = true
                        "action:pick_wait_confirmation" -> showWaitConfirmationPicker = true
                        "action:pick_auto_brightness" -> showAutoBrightnessPicker = true
                        "action:full_app_list" -> showFullAppPicker = true
                        else -> if (pkg != null) onMacroChange(macro.copy(actions = macro.actions + pkg))
                    }
                    showActionPicker = false
                }
            )
        }

        if (showBrightnessPicker) {
            BrightnessPickerDialog(initialValue = 50, onDismiss = { showBrightnessPicker = false }, onConfirmed = { b ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_BRIGHTNESS_PREFIX}$b"))
                showBrightnessPicker = false
            })
        }
        if (showVolumePicker) {
            VolumePickerDialog(onDismiss = { showVolumePicker = false }, onConfirmed = { v ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_VOLUME_PREFIX}$v"))
                showVolumePicker = false
            })
        }
        if (showMediaPicker) {
            MediaPickerDialog(onDismiss = { showMediaPicker = false }, onConfirmed = { m ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_MEDIA_PREFIX}$m"))
                showMediaPicker = false
            })
        }
        if (showUrlPicker) {
            UrlPickerDialog(initialValue = "", onDismiss = { showUrlPicker = false }, onConfirmed = { u ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_URL_PREFIX}$u"))
                showUrlPicker = false
            })
        }
        if (showThemePicker) {
            ThemePickerDialog(onDismiss = { showThemePicker = false }, onConfirmed = { t ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_THEME_PREFIX}$t"))
                showThemePicker = false
            })
        }
        if (showWaitConfirmationPicker) {
            WaitConfirmationDialog(initialValue = null, onDismiss = { showWaitConfirmationPicker = false }, onConfirm = { w ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_WAIT_CONFIRMATION_PREFIX}$w"))
                showWaitConfirmationPicker = false
            })
        }
        if (showAutoBrightnessPicker) {
            AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessPicker = false }, onConfirmed = { a ->
                onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$a"))
                showAutoBrightnessPicker = false
            })
        }

        if (showPreciseSwipePicker && editingActionIndex != -1) {
            PreciseSwipePickerDialog(
                initialValue = macro.actions[editingActionIndex].removePrefix(Config.ACTION_PATH_PREFIX),
                onDismiss = { showPreciseSwipePicker = false; editingActionIndex = -1 },
                onConfirmed = { path ->
                    val newList = macro.actions.toMutableList()
                    newList[editingActionIndex] = "${Config.ACTION_PATH_PREFIX}$path"
                    onMacroChange(macro.copy(actions = newList))
                    showPreciseSwipePicker = false; editingActionIndex = -1
                }
            )
        }

        if (showTouchPicker) {
            val existing = macro.actions.getOrNull(editingActionIndex)
            TouchPointPickerDialog(
                initialValue = existing?.removePrefix(Config.ACTION_TOUCH_PREFIX),
                onDismiss = { showTouchPicker = false; editingActionIndex = -1 },
                onConfirmed = { x, y, hold ->
                    val action = "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold"
                    val newList = macro.actions.toMutableList()
                    if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                        newList[editingActionIndex] = action
                    } else {
                        newList.add(action)
                    }
                    onMacroChange(macro.copy(actions = newList))
                    showTouchPicker = false; editingActionIndex = -1
                }
            )
        }
        if (showSwipePicker) {
            val existing = macro.actions.getOrNull(editingActionIndex)
            SwipePointPickerDialog(
                initialValue = existing?.removePrefix(Config.ACTION_SWIPE_PREFIX),
                onDismiss = { showSwipePicker = false; editingActionIndex = -1 },
                onConfirmed = { x1, y1, x2, y2, hS, hE ->
                    val action = "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE"
                    val newList = macro.actions.toMutableList()
                    if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                        newList[editingActionIndex] = action
                    } else {
                        newList.add(action)
                    }
                    onMacroChange(macro.copy(actions = newList))
                    showSwipePicker = false; editingActionIndex = -1
                }
            )
        }

        if (showDelayPickerInList) {
            val existing = macro.actions.getOrNull(editingActionIndex)
            DelayPickerDialog(
                initialValue = existing?.removePrefix(Config.ACTION_DELAY_PREFIX),
                onDismiss = { showDelayPickerInList = false; editingActionIndex = -1 },
                onConfirmed = { sec ->
                    val action = "${Config.ACTION_DELAY_PREFIX}$sec"
                    val newList = macro.actions.toMutableList()
                    if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                        newList[editingActionIndex] = action
                    }
                    onMacroChange(macro.copy(actions = newList))
                    showDelayPickerInList = false; editingActionIndex = -1
                }
            )
        }

        if (showFullAppPicker || (editingActionIndex != -1 && editingActionIndex < macro.actions.size && !macro.actions[editingActionIndex].startsWith("action:"))) {
            val selected = if (editingActionIndex != -1 && editingActionIndex < macro.actions.size) listOf(macro.actions[editingActionIndex]) else emptyList()
            UniversalAppPickerDialog(
                apps = (apps ?: emptyList()).filter { it.isLaunchable },
                selected = selected,
                multiSelect = false,
                onConfirmed = { picked ->
                    val pkg = picked.firstOrNull()
                    if (pkg != null) {
                        val newList = macro.actions.toMutableList()
                        if (editingActionIndex != -1 && editingActionIndex < newList.size) {
                            newList[editingActionIndex] = pkg
                        } else {
                            newList.add(pkg)
                        }
                        onMacroChange(macro.copy(actions = newList))
                    }
                    showFullAppPicker = false; editingActionIndex = -1
                },
                onDismiss = { showFullAppPicker = false; editingActionIndex = -1 }
            )
        }

        if (showIconPicker) {
            EventIconPickerDialog(
                currentIcon = macro.icon,
                apps = apps,
                onDismiss = { showIconPicker = false },
                onIconPicked = { icon ->
                    onMacroChange(macro.copy(icon = icon))
                    showIconPicker = false
                }
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfessionalMacroEditor(
    macro: Macro,
    apps: List<AppInfo>?,
    onMacroChange: (Macro) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var selectedActionIndex by remember { mutableIntStateOf(-1) }
    var selectedMediaId by remember { mutableStateOf<String?>(null) }
    var showTouchPicker by remember { mutableStateOf(false) }
    var showSwipePicker by remember { mutableStateOf(false) }
    var showDelayPicker by remember { mutableStateOf(false) }
    var showPreciseSwipePicker by remember { mutableStateOf(false) }
    
    // New Action Pickers
    var showActionPicker by remember { mutableStateOf(false) }
    var showMacroPicker by remember { mutableStateOf(false) }
    var showBrightnessPicker by remember { mutableStateOf(false) }
    var showVolumePicker by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showUrlPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showWaitConfirmationPicker by remember { mutableStateOf(false) }
    var showAutoBrightnessPicker by remember { mutableStateOf(false) }
    var showFullAppPickerPro by remember { mutableStateOf(false) }

    val ctx = LocalContext.current

    // Playback State
    var isPlaying by remember { mutableStateOf(false) }
    var playbackTime by remember { mutableLongStateOf(0L) }
    var videoSpeed by remember { mutableFloatStateOf(1f) }

    // Stable keys for actions reordering
    var actionIds by rememberSaveable { mutableStateOf(List(macro.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != macro.actions.size) {
        if (actionIds.size < macro.actions.size) {
            actionIds = actionIds + List(macro.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(macro.actions.size)
        }
    }
    
    val timelineData = remember(macro.actions, actionIds) { calculateMacroTimeline(macro.actions, actionIds) }
    val actionsDuration = timelineData.lastOrNull()?.let { it.startTime + it.duration } ?: 0L
    val mediaDuration = macro.mediaItems.sumOf { it.duration }
    val totalDuration = maxOf(actionsDuration, mediaDuration)

    // Cap playback time if total duration shrinks
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
                        
                        val newItem = MediaItem(
                            id = UUID.randomUUID().toString(),
                            type = MediaType.PHOTO,
                            path = destFile.absolutePath,
                            startTime = 0L, 
                            duration = 1000L,
                            fullDuration = 3600000L // 1 hour max for photo
                        )
                        // Insert at beginning
                        var newList = listOf(newItem) + macro.mediaItems
                        if (newList.size == 1) {
                            newList = listOf(newList[0].copy(startTime = 0L, duration = totalDuration.coerceAtLeast(1000L)))
                        } else {
                            var currentT = 0L
                            newList = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                        }
                        onMacroChange(macro.copy(mediaItems = newList))
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

                        val newItem = MediaItem(
                            id = UUID.randomUUID().toString(),
                            type = MediaType.VIDEO,
                            path = destFile.absolutePath,
                            startTime = 0L, 
                            duration = duration,
                            fullDuration = duration
                        )
                        // Insert at beginning
                        var newList = listOf(newItem) + macro.mediaItems
                        var currentT = 0L
                        newList = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                        onMacroChange(macro.copy(mediaItems = newList))
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
                if (playbackTime >= totalDuration) {
                    isPlaying = false
                }
                delay(16) // ~60fps
            }
        }
    }

    // Timeline Expand Logic
    var timelineExpanded by remember { mutableStateOf(false) }
    val extraTimelineHeight by animateDpAsState(if (timelineExpanded) 80.dp else 0.dp)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
        // 1. Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 4.dp)
        ) {
            // Left: Close
            RoundHeaderButton(
                icon = Icons.Default.Close, 
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            // Center: Undo/Redo
            Row(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(16.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (canUndo) Color.White else Color.Gray)
                }
                IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (canRedo) Color.White else Color.Gray)
                }
            }

            // Right: Save + Time
            Box(
                contentAlignment = Alignment.TopCenter, 
                modifier = Modifier.align(Alignment.TopEnd).width(48.dp)
            ) {
                RoundHeaderButton(
                    icon = Icons.Default.Check,
                    onClick = { if (macro.title.isNotEmpty()) onSave() },
                    backgroundColor = Color(0xFF2B2B2B),
                    borderColor = null
                )
                
                // Rotated Time Counter
                Text(
                    text = String.format("%02d:%02d / %02d:%02d",
                        (playbackTime / 1000) / 60, (playbackTime / 1000) % 60,
                        (totalDuration / 1000) / 60, (totalDuration / 1000) % 60
                    ),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelLarge,
                    softWrap = false,
                    modifier = Modifier
                        .padding(top = 110.dp) // Lowered by ~1/4 of text length
                        .wrapContentWidth(unbounded = true)
                        .graphicsLayer { 
                            rotationZ = 90f
                        }
                )
            }
        }

        // 2. Interactive Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            
            // Left: Action Buttons (Bottom to Top)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 24.dp)
                    .width(48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Bottom to Top
                // Delay
                SmallActionButton(icon = Icons.Default.HourglassEmpty, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_DELAY_PREFIX}0.5"))
                })
                
                // Action
                SmallActionButton(icon = Icons.Default.Extension, onClick = { showActionPicker = true })

                // Precise Swipe
                SmallActionButton(icon = Icons.Default.Gesture, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_PATH_PREFIX}540,1100,0;540,1300,500"))
                })

                // Swipe
                SmallActionButton(icon = Icons.Default.Swipe, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_SWIPE_PREFIX}300_1100_700_1100_0_0"))
                })

                // Tap
                SmallActionButton(icon = Icons.Default.TouchApp, onClick = {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_TOUCH_PREFIX}540_1100_200"))
                })
            }

            // Center: Preview Silhouette (Force lift with offset)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp), 
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = (-55).dp)
                        .aspectOf(9f / 19.5f, scale = 1.1f)
                        .border(2.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF0D0D0D))
                ) {
                    val activeItem = macro.mediaItems.find { 
                        playbackTime >= it.startTime && playbackTime <= (it.startTime + it.duration)
                    }

                    when (previewStyle) {
                        0 -> MockDesktop()
                        1 -> WallpaperPreview(activeItem?.takeIf { it.type == MediaType.PHOTO }?.path)
                        2 -> VideoPreview(
                            localPath = activeItem?.takeIf { it.type == MediaType.VIDEO }?.path,
                            itemStartTime = activeItem?.startTime ?: 0L,
                            playbackTime = playbackTime,
                            isPlaying = isPlaying,
                            speed = videoSpeed,
                            startOffset = activeItem?.startOffset ?: 0L
                        )
                    }

                    // Gesture Overlays
                    macro.actions.forEachIndexed { index, action ->
                        val timeData = timelineData.getOrNull(index)
                        val isActive = timeData != null && playbackTime >= timeData.startTime && playbackTime <= (timeData.startTime + timeData.duration)
                        val isSelected = index == selectedActionIndex
                        
                        val progress = if (isActive && timeData != null && timeData.duration > 0) {
                            ((playbackTime - timeData.startTime).toFloat() / timeData.duration).coerceIn(0f, 1f)
                        } else 1f

                        if (!isPlaying || isActive) {
                            GestureMarker(
                                action = action,
                                isSelected = isSelected || isActive,
                                isHighlighted = isActive,
                                progress = progress,
                                onClick = {
                                    selectedActionIndex = index
                                    timeData?.let { playbackTime = it.startTime }
                                    isPlaying = false
                                }
                            )
                        }
                    }
                }
            }

            // Right: Playback and Style Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 24.dp)
                    .width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
            ) {
                // TOP: Media Add Buttons
                Column(
                    modifier = Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { pickWallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.size(36.dp).background(Color(0xFF2B2B2B), CircleShape)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                        modifier = Modifier.size(36.dp).background(Color(0xFF2B2B2B), CircleShape)
                    ) {
                        Icon(Icons.Default.VideoCall, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // MIDDLE: Format Switcher
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(22.dp))
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreviewStyleIcon(icon = Icons.Default.Dashboard, isSelected = previewStyle == 0, onClick = { previewStyle = 0 })
                    PreviewStyleIcon(icon = Icons.Default.Wallpaper, isSelected = previewStyle == 1, onClick = { previewStyle = 1 })
                    PreviewStyleIcon(icon = Icons.Default.Videocam, isSelected = previewStyle == 2, onClick = { previewStyle = 2 })
                }

                // BOTTOM: Play/Stop circle
                IconButton(
                    onClick = { 
                        if (playbackTime >= totalDuration) playbackTime = 0
                        isPlaying = !isPlaying 
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFFD700), CircleShape)
                ) {
                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = Color.Black)
                }
            }
        }

        // 3. Bottom: Timeline Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF121212), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(bottom = 92.dp)
        ) {
            // White Bar (Feature separator & Draggable Handle)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount < -10) timelineExpanded = true
                            if (dragAmount > 10) timelineExpanded = false
                        }
                    }
                    .clickable { timelineExpanded = !timelineExpanded },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
            }
            
            // Secondary Media Timeline (Revealed when expanded)
            if (extraTimelineHeight > 0.dp) {
                Box(modifier = Modifier.fillMaxWidth().height(extraTimelineHeight)) {
                    val itemsToShow = when (previewStyle) {
                        1 -> macro.mediaItems.filter { it.type == MediaType.PHOTO }
                        2 -> macro.mediaItems.filter { it.type == MediaType.VIDEO }
                        else -> emptyList()
                    }
                    
                    MediaTrackTimeline(
                        label = if (previewStyle == 1) "фото" else "видео",
                        color = Color.Gray,
                        totalDuration = totalDuration,
                        scrollState = timelineScrollState,
                        mediaItems = itemsToShow,
                        selectedMediaId = selectedMediaId,
                        onSelect = { selectedMediaId = it },
                        onDelete = { id ->
                            val newList = macro.mediaItems.filter { it.id != id }
                            onMacroChange(macro.copy(mediaItems = newList))
                            if (selectedMediaId == id) selectedMediaId = null
                        },
                        onMediaChange = { id, updatedItem ->
                            val newList = macro.mediaItems.map { 
                                if (it.id == id) updatedItem else it 
                            }
                            // Re-calculate start times only if needed to keep them sequential
                            // but allow the change to persist. 
                            // Actually, let's keep it sequential: Item N starts where N-1 ends.
                            var currentT = 0L
                            val finalItems = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                            onMacroChange(macro.copy(mediaItems = finalItems))
                        },
                        onMoveOrder = { from, to ->
                            val newList = macro.mediaItems.toMutableList()
                            val item = newList.removeAt(from)
                            newList.add(to, item)

                            // Re-calculate times based on order
                            var currentT = 0L
                            val finalItems = newList.map { it.copy(startTime = currentT).also { m -> currentT += m.duration } }
                            onMacroChange(macro.copy(mediaItems = finalItems))
                        },
                        onSpeedClick = if (previewStyle == 2) ({ showMediaSpeedDialog = true }) else null
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AdvancedTimeline(
                timelineData = timelineData,
                scrollState = timelineScrollState,
                playbackTime = playbackTime,
                totalDuration = totalDuration,
                isPlaying = isPlaying,
                selectedIndex = selectedActionIndex,
                onSelect = {
                    selectedActionIndex = it
                    timelineData.getOrNull(it)?.let { data -> playbackTime = data.startTime }
                },
                onEdit = { index ->
                    selectedActionIndex = index
                    val action = macro.actions[index]
                    when {
                        action.startsWith(Config.ACTION_TOUCH_PREFIX) -> showTouchPicker = true
                        action.startsWith(Config.ACTION_SWIPE_PREFIX) -> showSwipePicker = true
                        action.startsWith(Config.ACTION_PATH_PREFIX) -> showPreciseSwipePicker = true
                        action.startsWith(Config.ACTION_DELAY_PREFIX) -> showDelayPicker = true
                    }
                },
                onDelete = { index ->
                    val newList = macro.actions.toMutableList().apply { removeAt(index) }
                    val newIds = actionIds.toMutableList().apply { removeAt(index) }
                    actionIds = newIds
                    onMacroChange(macro.copy(actions = newList))
                    selectedActionIndex = -1
                },
                onSeek = {
                    playbackTime = it
                    isPlaying = false
                },
                onMove = { from, to ->
                    if (to in macro.actions.indices) {
                        val newList = macro.actions.toMutableList()
                        val item = newList.removeAt(from)
                        newList.add(to, item)

                        val newIds = actionIds.toMutableList()
                        val id = newIds.removeAt(from)
                        newIds.add(to, id)
                        actionIds = newIds

                        onMacroChange(macro.copy(actions = newList))
                        selectedActionIndex = to
                    }
                },
                onDurationChange = { index, newDuration ->
                    val action = macro.actions[index]
                    val newAction = when {
                        action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                            val pts = action.removePrefix(Config.ACTION_TOUCH_PREFIX).split("_")
                            "${Config.ACTION_TOUCH_PREFIX}${pts[0]}_${pts[1]}_$newDuration"
                        }
                        action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                            val pts = action.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
                            val x1 = pts.getOrNull(0) ?: "0"
                            val y1 = pts.getOrNull(1) ?: "0"
                            val x2 = pts.getOrNull(2) ?: "0"
                            val y2 = pts.getOrNull(3) ?: "0"
                            val hS = pts.getOrNull(4) ?: "0"
                            val hE = pts.getOrNull(5) ?: "0"
                            "${Config.ACTION_SWIPE_PREFIX}${x1}_${y1}_${x2}_${y2}_${hS}_${hE}_$newDuration"
                        }
                        action.startsWith(Config.ACTION_DELAY_PREFIX) -> {
                            "${Config.ACTION_DELAY_PREFIX}${newDuration / 1000f}"
                        }
                        action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                            // Precise Swipe: Scale all timestamps by ratio
                            val oldDur = timelineData.getOrNull(index)?.duration ?: 1L
                            val ratio = newDuration.toFloat() / oldDur.coerceAtLeast(1L)
                            val pathData = action.removePrefix(Config.ACTION_PATH_PREFIX)
                            val newPath = pathData.split(";").filter { it.isNotEmpty() }.joinToString(";") { pt ->
                                val parts = pt.split(",")
                                if (parts.size >= 3) {
                                    val t = parts[2].toLongOrNull() ?: 0L
                                    "${parts[0]},${parts[1]},${(t * ratio).toLong()}"
                                } else pt
                            }
                            "${Config.ACTION_PATH_PREFIX}$newPath"
                        }
                        else -> action
                    }
                    val newList = macro.actions.toMutableList().apply { set(index, newAction) }
                    onMacroChange(macro.copy(actions = newList))
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showMediaSpeedDialog) {
        SpeedCoefficientDialog(
            currentSpeed = videoSpeed,
            onDismiss = { showMediaSpeedDialog = false },
            onConfirmed = {
                videoSpeed = it
                showMediaSpeedDialog = false
            }
        )
    }

    // Pickers
    if (showActionPicker) {
        EventActionPickerDialog(triggerGesture = TapSlot.SINGLE, allowMacros = false, onPick = { pkg ->
            when (pkg) {
                "action:pick_touch" -> { selectedActionIndex = -1; showTouchPicker = true }
                "action:pick_swipe" -> { selectedActionIndex = -1; showSwipePicker = true }
                "action:pick_delay" -> { selectedActionIndex = -1; showDelayPicker = true }
                "action:pick_macro" -> showMacroPicker = true
                "action:pick_brightness" -> showBrightnessPicker = true
                "action:pick_volume" -> showVolumePicker = true
                "action:pick_media" -> showMediaPicker = true
                "action:pick_url" -> showUrlPicker = true
                "action:pick_theme" -> showThemePicker = true
                "action:pick_wait_confirmation" -> showWaitConfirmationPicker = true
                "action:pick_auto_brightness" -> showAutoBrightnessPicker = true
                "action:full_app_list" -> showFullAppPickerPro = true
                else -> if (pkg != null) {
                    actionIds = actionIds + UUID.randomUUID().toString()
                    onMacroChange(macro.copy(actions = macro.actions + pkg))
                }
            }
            showActionPicker = false
        }, onDismiss = { showActionPicker = false })
    }
    if (showMacroPicker) {
        val currentKeyMap by Config.flow(ctx).collectAsState(initial = KeyMap())
        MacroPickerDialog(macros = currentKeyMap.macros, onDismiss = { showMacroPicker = false }, onMacroPicked = { id ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_MACRO_PREFIX}$id"))
            showMacroPicker = false
        })
    }
    if (showBrightnessPicker) {
        BrightnessPickerDialog(initialValue = 50, onDismiss = { showBrightnessPicker = false }, onConfirmed = { b ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_BRIGHTNESS_PREFIX}$b"))
            showBrightnessPicker = false
        })
    }
    if (showVolumePicker) {
        VolumePickerDialog(onDismiss = { showVolumePicker = false }, onConfirmed = { v ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_VOLUME_PREFIX}$v"))
            showVolumePicker = false
        })
    }
    if (showMediaPicker) {
        MediaPickerDialog(onDismiss = { showMediaPicker = false }, onConfirmed = { m ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_MEDIA_PREFIX}$m"))
            showMediaPicker = false
        })
    }
    if (showUrlPicker) {
        UrlPickerDialog(initialValue = "", onDismiss = { showUrlPicker = false }, onConfirmed = { u ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_URL_PREFIX}$u"))
            showUrlPicker = false
        })
    }
    if (showThemePicker) {
        ThemePickerDialog(onDismiss = { showThemePicker = false }, onConfirmed = { t ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_THEME_PREFIX}$t"))
            showThemePicker = false
        })
    }
    if (showWaitConfirmationPicker) {
        WaitConfirmationDialog(initialValue = null, onDismiss = { showWaitConfirmationPicker = false }, onConfirm = { w ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_WAIT_CONFIRMATION_PREFIX}$w"))
            showWaitConfirmationPicker = false
        })
    }
    if (showAutoBrightnessPicker) {
        AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessPicker = false }, onConfirmed = { a ->
            actionIds = actionIds + UUID.randomUUID().toString()
            onMacroChange(macro.copy(actions = macro.actions + "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$a"))
            showAutoBrightnessPicker = false
        })
    }
    if (showFullAppPickerPro) {
        UniversalAppPickerDialog(apps = (apps ?: emptyList()).filter { it.isLaunchable }, selected = emptyList(), multiSelect = false, onConfirmed = { picked ->
            picked.firstOrNull()?.let { pkg -> 
                actionIds = actionIds + UUID.randomUUID().toString()
                onMacroChange(macro.copy(actions = macro.actions + pkg)) 
            }
            showFullAppPickerPro = false
        }, onDismiss = { showFullAppPickerPro = false })
    }
    if (showTouchPicker) {
        val existing = if (selectedActionIndex != -1) macro.actions.getOrNull(selectedActionIndex) else null
        TouchPointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_TOUCH_PREFIX), onDismiss = { showTouchPicker = false; selectedActionIndex = -1 }, onConfirmed = { x, y, hold ->
            val action = "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold"
            val newList = macro.actions.toMutableList()
            if (selectedActionIndex != -1 && selectedActionIndex < newList.size) newList[selectedActionIndex] = action else newList.add(action)
            onMacroChange(macro.copy(actions = newList)); showTouchPicker = false; selectedActionIndex = -1
        })
    }
    if (showSwipePicker) {
        val existing = if (selectedActionIndex != -1) macro.actions.getOrNull(selectedActionIndex) else null
        SwipePointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_SWIPE_PREFIX), onDismiss = { showSwipePicker = false; selectedActionIndex = -1 }, onConfirmed = { x1, y1, x2, y2, hS, hE ->
            val action = "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE"
            val newList = macro.actions.toMutableList()
            if (selectedActionIndex != -1 && selectedActionIndex < newList.size) newList[selectedActionIndex] = action else newList.add(action)
            onMacroChange(macro.copy(actions = newList)); showSwipePicker = false; selectedActionIndex = -1
        })
    }
    if (showDelayPicker) {
        val existing = if (selectedActionIndex != -1) macro.actions.getOrNull(selectedActionIndex) else null
        DelayPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_DELAY_PREFIX), onDismiss = { showDelayPicker = false; selectedActionIndex = -1 }, onConfirmed = { sec ->
            val action = "${Config.ACTION_DELAY_PREFIX}$sec"
            val newList = macro.actions.toMutableList()
            if (selectedActionIndex != -1 && selectedActionIndex < newList.size) newList[selectedActionIndex] = action else newList.add(action)
            onMacroChange(macro.copy(actions = newList)); showDelayPicker = false; selectedActionIndex = -1
        })
    }
    if (showPreciseSwipePicker && selectedActionIndex != -1) {
        PreciseSwipePickerDialog(
            initialValue = macro.actions[selectedActionIndex].removePrefix(Config.ACTION_PATH_PREFIX),
            onDismiss = { showPreciseSwipePicker = false },
            onConfirmed = { path ->
                val newList = macro.actions.toMutableList()
                newList[selectedActionIndex] = "${Config.ACTION_PATH_PREFIX}$path"
                onMacroChange(macro.copy(actions = newList))
                showPreciseSwipePicker = false
            }
        )
    }
}
