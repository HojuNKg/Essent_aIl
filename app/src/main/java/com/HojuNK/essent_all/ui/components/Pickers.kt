package com.HojuNK.essent_all.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun PreciseSwipePickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    val density = LocalDensity.current
    var pathPoints by remember {
        val pts = initialValue?.split(";")?.filter { it.isNotEmpty() }?.mapNotNull {
            val p = it.split(",")
            if (p.size >= 2) Offset(p[0].toFloat(), p[1].toFloat()) else null
        } ?: emptyList()
        mutableStateOf(pts)
    }

    var isDrawing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "precise_swipe")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "progress"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.9f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isDrawing) {
                        if (isDrawing) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                pathPoints = listOf(down.position)
                                do {
                                    val event = awaitPointerEvent()
                                    val pos = event.changes.first().position
                                    pathPoints = pathPoints + pos
                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                                isDrawing = false
                            }
                        } else {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                pathPoints = pathPoints.map { Offset(it.x + dragAmount.x, it.y + dragAmount.y) }
                            }
                        }
                    }
                ) {
                    if (pathPoints.size >= 2) {
                        val path = Path()
                        path.moveTo(pathPoints[0].x, pathPoints[0].y)
                        for (i in 1 until pathPoints.size) {
                            path.lineTo(pathPoints[i].x, pathPoints[i].y)
                        }
                        drawPath(path, Color.Gray, style = Stroke(width = 8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))))

                        // Running dot
                        val totalSegments = pathPoints.size - 1
                        val currentSegment = (progress * totalSegments).toInt().coerceIn(0, totalSegments - 1)
                        val segmentProgress = (progress * totalSegments) - currentSegment
                        val p1 = pathPoints[currentSegment]
                        val p2 = pathPoints[currentSegment + 1]
                        val currentPos = Offset(lerp(p1.x, p2.x, segmentProgress), lerp(p1.y, p2.y, segmentProgress))
                        drawCircle(Color.White, 20f, currentPos)

                        // Markers
                        drawCircle(Color.White, 12f, pathPoints[0])
                        drawCircle(Color.White, 8f, pathPoints.last())
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isDrawing) "РИСУЙТЕ ТРАЕКТОРИЮ" else "РЕДАКТИРОВАНИЕ ТРАЕКТОРИИ",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!isDrawing) {
                        Text("Тяните для перемещения", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text(stringResource(R.string.cancel))
                    }

                    IconButton(onClick = {
                        // Simplify path: ~2 pts per 1cm.
                        // Assuming 1cm is ~150px (typical for 400-500 dpi phones),
                        // 2 pts/cm means a point every ~75px.
                        if (pathPoints.size > 2) {
                            val simplified = mutableListOf<Offset>()
                            simplified.add(pathPoints.first())
                            var lastAdded = pathPoints.first()
                            val thresholdPx = 75f
                            for (i in 1 until pathPoints.size - 1) {
                                val dist = (pathPoints[i] - lastAdded).getDistance()
                                if (dist > thresholdPx) {
                                    simplified.add(pathPoints[i])
                                    lastAdded = pathPoints[i]
                                }
                            }
                            simplified.add(pathPoints.last())
                            pathPoints = simplified
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoFixHigh, null, tint = Color.White)
                            Text("ВЫРОВНЯТЬ", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(onClick = {
                        if (pathPoints.isNotEmpty()) {
                            val centerX = pathPoints.map { it.x }.average().toFloat()
                            pathPoints = pathPoints.map { Offset(centerX - (it.x - centerX), it.y) }
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Flip, null, tint = Color.White)
                            Text("ОТРАЗИТЬ", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(onClick = { isDrawing = true; pathPoints = emptyList() }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Gesture, null, tint = Color.White)
                            Text("ПЕРЕРИСОВАТЬ", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Button(
                        onClick = {
                            // Generate timestamps: total duration 500ms distributed among points
                            val totalDur = 500L
                            val res = pathPoints.indices.joinToString(";") { i ->
                                val p = pathPoints[i]
                                val t = if (pathPoints.size > 1) (i * totalDur) / (pathPoints.size - 1) else 0L
                                "${p.x.toInt()},${p.y.toInt()},$t"
                            }
                            onConfirmed(res)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
fun UniversalAppPickerDialog(
    apps: List<AppInfo>,
    selected: List<String>,
    multiSelect: Boolean,
    onConfirmed: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var currentSelected by remember { mutableStateOf(selected) }

    val filtered = remember(search, apps, showSystem) {
        apps.filter { app ->
            val matchesSearch = app.label.contains(search, ignoreCase = true) || app.pkg.contains(search, ignoreCase = true)
            val matchesSystem = showSystem || !app.isSystem
            matchesSearch && matchesSystem
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (selected.isNotEmpty()) {
            val idx = filtered.indexOfFirst { it.pkg == selected.first() }
            if (idx >= 0) listState.scrollToItem(idx)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.select_app_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                // Search and Toggle Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_hint), color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                        trailingIcon = if (search.isNotEmpty()) {
                            { IconButton(onClick = { search = "" }) { Icon(Icons.Default.Clear, null, tint = Color.Gray) } }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { showSystem = !showSystem }) {
                    Checkbox(
                        checked = showSystem,
                        onCheckedChange = { showSystem = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black, uncheckedColor = Color.Gray)
                    )
                    Text("Показывать системные", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    items(filtered, key = { it.pkg }) { a ->
                        val isItemSelected = currentSelected.contains(a.pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (multiSelect) {
                                        currentSelected = if (isItemSelected) currentSelected - a.pkg else currentSelected + a.pkg
                                    } else {
                                        currentSelected = listOf(a.pkg)
                                    }
                                }
                                .background(if (isItemSelected && !multiSelect) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(bitmap = a.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                                Text(a.pkg, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            if (multiSelect) {
                                Checkbox(
                                    checked = isItemSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = Color.White, checkmarkColor = Color.Black)
                                )
                            } else if (isItemSelected) {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmed(currentSelected) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun RegistryActionRow(
    label: String,
    selected: Boolean,
    rightLabel: String? = null,
    expanded: Boolean = false,
    onClick: () -> Unit,
    onRightLabelClick: (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    Surface(
        color = if (selected) Color(0xFF3A3A3A) else Color(0xFF242424),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (rightLabel != null) {
                    Text(
                        text = rightLabel,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .clickable { (onRightLabelClick ?: onClick).invoke() }
                            .padding(4.dp)
                    )
                }
            }
            if (expanded && content != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
fun SettingPillRow(
    options: List<Pair<String, String>>,
    selectedVal: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = selectedVal == value
            Surface(
                color = if (isSelected) Color.White else Color(0xFF333333),
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(value) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun getDefaultActionTarget(action: ActionItem, keyMap: KeyMap, currentTarget: String?): String {
    if (currentTarget != null && isTargetMatch(action, currentTarget)) {
        return currentTarget
    }
    return when (action.pickerType) {
        "flashlight" -> "${Config.ACTION_FLASHLIGHT_PREFIX}toggle"
        "glyph" -> "${Config.ACTION_GLYPH_PREFIX}toggle"
        "auto_brightness" -> "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}toggle"
        "theme" -> "${Config.ACTION_THEME_PREFIX}toggle"
        "brightness" -> "${Config.ACTION_BRIGHTNESS_PREFIX}50"
        "volume" -> "${Config.ACTION_VOLUME_PREFIX}media:set:50"
        "delay" -> "action:delay:500"
        "url" -> Config.ACTION_URL_PREFIX
        "macro" -> "${Config.ACTION_MACRO_PREFIX}${keyMap.macros.firstOrNull()?.id ?: ""}"
        else -> action.id
    }
}

fun isTargetMatch(action: ActionItem, currentTarget: String?): Boolean {
    if (currentTarget == null) return false
    return when (action.pickerType) {
        "flashlight" -> currentTarget == Config.ACTION_FLASHLIGHT || currentTarget.startsWith(Config.ACTION_FLASHLIGHT_PREFIX)
        "glyph" -> currentTarget == Config.ACTION_GLYPH || currentTarget == Config.ACTION_GLYPH_TOGGLE || currentTarget.startsWith(Config.ACTION_GLYPH_PREFIX)
        "auto_brightness" -> currentTarget.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX)
        "theme" -> currentTarget.startsWith(Config.ACTION_THEME_PREFIX)
        "brightness" -> currentTarget.startsWith(Config.ACTION_BRIGHTNESS_PREFIX)
        "volume" -> currentTarget.startsWith(Config.ACTION_VOLUME_PREFIX)
        "delay" -> currentTarget.startsWith("action:delay:")
        "url" -> currentTarget.startsWith(Config.ACTION_URL_PREFIX)
        "macro" -> currentTarget.startsWith(Config.ACTION_MACRO_PREFIX)
        "app" -> !currentTarget.startsWith("action:")
        else -> currentTarget == action.id
    }
}

@Composable
fun getActionContent(
    action: ActionItem,
    ctx: Context,
    keyMap: KeyMap,
    testedActionId: String?,
    onUpdateId: (String) -> Unit
): @Composable (() -> Unit)? {
    return when (action.pickerType) {
        "flashlight" -> {
            {
                val currentMode = when {
                    testedActionId?.startsWith(Config.ACTION_FLASHLIGHT_PREFIX) == true -> testedActionId.removePrefix(Config.ACTION_FLASHLIGHT_PREFIX)
                    testedActionId == Config.ACTION_FLASHLIGHT -> "toggle"
                    else -> "toggle"
                }
                SettingPillRow(
                    options = listOf("on" to "Включить", "off" to "Выключить", "toggle" to "Переключить"),
                    selectedVal = currentMode,
                    onSelect = { mode ->
                        onUpdateId("${Config.ACTION_FLASHLIGHT_PREFIX}$mode")
                    }
                )
            }
        }
        "glyph" -> {
            {
                val currentMode = when {
                    testedActionId?.startsWith(Config.ACTION_GLYPH_PREFIX) == true -> testedActionId.removePrefix(Config.ACTION_GLYPH_PREFIX)
                    testedActionId == Config.ACTION_GLYPH || testedActionId == Config.ACTION_GLYPH_TOGGLE -> "toggle"
                    else -> "toggle"
                }
                SettingPillRow(
                    options = listOf("on" to "Включить", "off" to "Выключить", "toggle" to "Переключить"),
                    selectedVal = currentMode,
                    onSelect = { mode ->
                        onUpdateId("${Config.ACTION_GLYPH_PREFIX}$mode")
                    }
                )
            }
        }
        "delay" -> {
            {
                var delayMs by remember(testedActionId) {
                    mutableStateOf(testedActionId?.removePrefix("action:delay:")?.toIntOrNull() ?: 500)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Задержка: $delayMs мс", color = Color.White)
                    Slider(
                        value = delayMs.toFloat(),
                        onValueChange = { delayMs = it.toInt() },
                        onValueChangeFinished = {
                            onUpdateId("action:delay:$delayMs")
                        },
                        valueRange = 100f..5000f
                    )
                }
            }
        }
        "auto_brightness" -> {
            {
                val currentMode = testedActionId?.removePrefix(Config.ACTION_AUTO_BRIGHTNESS_PREFIX) ?: "toggle"
                SettingPillRow(
                    options = listOf("on" to "Включить", "off" to "Выключить", "toggle" to "Переключить"),
                    selectedVal = currentMode,
                    onSelect = { mode ->
                        onUpdateId("${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$mode")
                    }
                )
            }
        }
        "theme" -> {
            {
                val currentMode = testedActionId?.removePrefix(Config.ACTION_THEME_PREFIX) ?: "toggle"
                SettingPillRow(
                    options = listOf("light" to "Светлая", "dark" to "Темная", "toggle" to "Переключить"),
                    selectedVal = currentMode,
                    onSelect = { mode ->
                        onUpdateId("${Config.ACTION_THEME_PREFIX}$mode")
                    }
                )
            }
        }
        "brightness" -> {
            {
                val initialLevel = testedActionId?.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)?.toFloatOrNull() ?: 50f
                var level by remember(testedActionId) { mutableStateOf(initialLevel) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Уровень яркости: ${level.toInt()}%", color = Color.White)
                    Slider(
                        value = level,
                        onValueChange = { level = it },
                        onValueChangeFinished = {
                            onUpdateId("${Config.ACTION_BRIGHTNESS_PREFIX}${level.toInt()}")
                        },
                        valueRange = 1f..100f
                    )
                }
            }
        }
        "volume" -> {
            {
                val parts = testedActionId?.removePrefix(Config.ACTION_VOLUME_PREFIX)?.split(":") ?: emptyList()
                val initialStream = if (parts.isNotEmpty() && parts[0].isNotEmpty()) parts[0] else "media"
                val initialVol = if (parts.size >= 3) parts[2].toFloatOrNull() ?: 50f else 50f
                var stream by remember(testedActionId) { mutableStateOf(initialStream) }
                var volLevel by remember(testedActionId) { mutableStateOf(initialVol) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingPillRow(
                        options = listOf("media" to "Медиа", "ring" to "Звонок"),
                        selectedVal = stream,
                        onSelect = { s ->
                            stream = s
                            onUpdateId("${Config.ACTION_VOLUME_PREFIX}$s:set:${volLevel.toInt()}")
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Громкость: ${volLevel.toInt()}%", color = Color.White)
                    Slider(
                        value = volLevel,
                        onValueChange = { volLevel = it },
                        onValueChangeFinished = {
                            onUpdateId("${Config.ACTION_VOLUME_PREFIX}$stream:set:${volLevel.toInt()}")
                        },
                        valueRange = 0f..100f
                    )
                }
            }
        }
        "url" -> {
            {
                var urlInput by remember(testedActionId) {
                    mutableStateOf(testedActionId?.removePrefix(Config.ACTION_URL_PREFIX) ?: "")
                }
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        onUpdateId("${Config.ACTION_URL_PREFIX}$it")
                    },
                    placeholder = { Text("https://...", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        "macro" -> {
            {
                val macros = keyMap.macros
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (macros.isEmpty()) {
                        Text("Нет созданных макросов", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 150.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(macros) { macro ->
                                val isSelected = testedActionId == "${Config.ACTION_MACRO_PREFIX}${macro.id}"
                                Surface(
                                    color = if (isSelected) Color.White else Color(0xFF333333),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdateId("${Config.ACTION_MACRO_PREFIX}${macro.id}")
                                        }
                                ) {
                                    Text(
                                        text = macro.title,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> null
    }
}

@Composable
fun UniversalActionListContent(
    keyMap: KeyMap = KeyMap(),
    apps: List<AppInfo>? = null,
    currentActionId: String?,
    slot: TapSlot? = null,
    showDefaultOption: Boolean = false,
    autoConfirmSimpleSelection: Boolean = true,
    onActionSelected: (String?) -> Unit
) {
    val ctx = LocalContext.current
    var selectedActionId by remember(currentActionId) { mutableStateOf(currentActionId) }
    var recentIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var expandedActionId by remember { mutableStateOf<String?>(null) }
    var showAppPickerForTest by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var loadedApps by remember(apps) { mutableStateOf(apps ?: emptyList()) }
    LaunchedEffect(apps) {
        if (apps == null) {
            loadedApps = com.HojuNK.essent_all.loadLaunchableApps(ctx)
        }
    }

    val isPowerOrVolume = slot != null && (
        slot == TapSlot.POWER_LONG ||
        slot.name.startsWith("VOL_UP_") ||
        slot.name.startsWith("VOL_DOWN_")
    )

    val killActionItem = remember(slot) {
        if (isPowerOrVolume) {
            null
        } else if (slot == TapSlot.SINGLE || slot == TapSlot.LONG) {
            ActionItem(Config.ACTION_KILL_NOTES, R.string.kill_notes, ActionCategory.QUICK_SYSTEM)
        } else {
            ActionItem(Config.ACTION_KILL_ESSENTIAL, R.string.kill_essential_space, ActionCategory.QUICK_SYSTEM)
        }
    }

    LaunchedEffect(Unit) {
        recentIds = ActionHistory.getRecentActions(ctx)
    }

    if (showAppPickerForTest) {
        UniversalAppPickerDialog(
            apps = loadedApps.filter { it.isLaunchable },
            selected = listOfNotNull(selectedActionId?.takeIf { !it.startsWith("action:") }),
            multiSelect = false,
            onConfirmed = { picked ->
                val pkg = picked.firstOrNull()
                if (pkg != null) {
                    selectedActionId = pkg
                    ActionHistory.addAction(ctx, pkg)
                    onActionSelected(pkg)
                }
                showAppPickerForTest = false
            },
            onDismiss = { showAppPickerForTest = false }
        )
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_action_hint), color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            trailingIcon = if (isSearching || searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = {
                        isSearching = false
                        searchQuery = ""
                    }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            } else null,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (state.isFocused) isSearching = true
                }
        )

        Spacer(Modifier.height(12.dp))

        val baseActions = remember(killActionItem, slot) {
            val list = ActionRegistry.allActions.toMutableList()
            if (slot != null) {
                list.removeAll { 
                    it.id == "action:pick_delay" || it.pickerType == "delay" || 
                    it.id == "action:pick_wait_confirmation" || it.pickerType == "wait_confirmation" 
                }
            }
            if (killActionItem != null) {
                val quickIndex = list.indexOfFirst { it.category == ActionCategory.QUICK_SYSTEM }
                if (quickIndex >= 0) {
                    list.add(quickIndex, killActionItem)
                } else {
                    list.add(0, killActionItem)
                }
            }
            list.toList()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val query = searchQuery

            if (showDefaultOption && query.isBlank()) {
                item {
                    Text("⚙️ Системное", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                item {
                    RegistryActionRow(
                        label = stringResource(R.string.default_action),
                        selected = selectedActionId == null,
                        onClick = {
                            selectedActionId = null
                            onActionSelected(null)
                        }
                    )
                }
            }

            val filteredActions = if (query.isBlank()) {
                baseActions
            } else {
                baseActions.filter { action ->
                    val title = ctx.getString(action.titleRes)
                    fuzzyMatch(query, title)
                }
            }

            val handleActionRowClick: (ActionItem) -> Unit = { action ->
                if (action.pickerType == "app") {
                    showAppPickerForTest = true
                } else {
                    val targetToAssign = getDefaultActionTarget(action, keyMap, selectedActionId)
                    selectedActionId = targetToAssign
                    ActionHistory.addAction(ctx, targetToAssign)
                    onActionSelected(targetToAssign)
                    if (action.pickerType != null) {
                        expandedActionId = if (expandedActionId == action.id) null else action.id
                    }
                }
            }

            val handleActionRightLabelClick: (ActionItem) -> Unit = { action ->
                if (action.pickerType == "app") {
                    showAppPickerForTest = true
                } else {
                    val targetToAssign = getDefaultActionTarget(action, keyMap, selectedActionId)
                    selectedActionId = targetToAssign
                    ActionHistory.addAction(ctx, targetToAssign)
                    onActionSelected(targetToAssign)
                    expandedActionId = if (expandedActionId == action.id) null else action.id
                }
            }

            val recentActions = recentIds.mapNotNull { id -> baseActions.find { it.id == id } }
            if (isSearching && query.isBlank() && recentActions.size in 2..5) {
                item {
                    Text("⏳ Недавнее", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(recentActions) { action ->
                    val isExpanded = expandedActionId == action.id || isTargetMatch(action, selectedActionId)
                    val isAppPicker = action.pickerType == "app"
                    val isAppSelected = selectedActionId != null && !selectedActionId!!.startsWith("action:")
                    val rightLabelText = when {
                        isAppPicker -> if (isAppSelected) "изменить" else "выбрать"
                        action.pickerType != null -> if (isExpanded) "Скрыть" else "Развернуть"
                        else -> null
                    }
                    RegistryActionRow(
                        label = stringResource(action.titleRes),
                        selected = false,
                        rightLabel = rightLabelText,
                        expanded = isExpanded,
                        onClick = { handleActionRowClick(action) },
                        onRightLabelClick = { handleActionRightLabelClick(action) },
                        content = getActionContent(action, ctx, keyMap, selectedActionId) { newId ->
                            selectedActionId = newId
                            ActionHistory.addAction(ctx, newId)
                            if (autoConfirmSimpleSelection) {
                                onActionSelected(newId)
                            }
                        }
                    )
                }
            }

            if (query.isBlank()) {
                baseActions.groupBy { it.category }.forEach { (category, items) ->
                    item {
                        Text(stringResource(category.titleRes), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(items) { action ->
                        val isExpanded = expandedActionId == action.id || isTargetMatch(action, selectedActionId)
                        val isAppPicker = action.pickerType == "app"
                        val isAppSelected = selectedActionId != null && !selectedActionId!!.startsWith("action:")
                        val rightLabelText = when {
                            isAppPicker -> if (isAppSelected) "изменить" else "выбрать"
                            action.pickerType != null -> if (isExpanded) "Скрыть" else "Развернуть"
                            else -> null
                        }
                        RegistryActionRow(
                            label = stringResource(action.titleRes),
                            selected = selectedActionId == action.id || isTargetMatch(action, selectedActionId),
                            rightLabel = rightLabelText,
                            expanded = isExpanded,
                            onClick = { handleActionRowClick(action) },
                            onRightLabelClick = { handleActionRightLabelClick(action) },
                            content = getActionContent(action, ctx, keyMap, selectedActionId) { newId ->
                                selectedActionId = newId
                                ActionHistory.addAction(ctx, newId)
                                if (autoConfirmSimpleSelection) {
                                    onActionSelected(newId)
                                }
                            }
                        )
                    }
                }
            } else {
                items(filteredActions) { action ->
                    val isExpanded = expandedActionId == action.id || isTargetMatch(action, selectedActionId)
                    val isAppPicker = action.pickerType == "app"
                    val isAppSelected = selectedActionId != null && !selectedActionId!!.startsWith("action:")
                    val rightLabelText = when {
                        isAppPicker -> if (isAppSelected) "изменить" else "выбрать"
                        action.pickerType != null -> if (isExpanded) "Скрыть" else "Развернуть"
                        else -> null
                    }
                    RegistryActionRow(
                        label = stringResource(action.titleRes),
                        selected = selectedActionId == action.id || isTargetMatch(action, selectedActionId),
                        rightLabel = rightLabelText,
                        expanded = isExpanded,
                        onClick = { handleActionRowClick(action) },
                        onRightLabelClick = { handleActionRightLabelClick(action) },
                        content = getActionContent(action, ctx, keyMap, selectedActionId) { newId ->
                            selectedActionId = newId
                            ActionHistory.addAction(ctx, newId)
                            if (autoConfirmSimpleSelection) {
                                onActionSelected(newId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var dontAskAgain by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.delete_event_q), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.delete_event_msg, title), color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { dontAskAgain = !dontAskAgain },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.Gray, checkmarkColor = Color.Black)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.do_not_ask_again), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirm(dontAskAgain) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)) { Text(stringResource(R.string.delete_btn)) }
                }
            }
        }
    }
}

@Composable
fun MacroPickerDialog(
    macros: List<Macro>,
    onDismiss: () -> Unit,
    onMacroPicked: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1A1A1A), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_macro), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(macros) { macro ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { onMacroPicked(macro.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EventIconDisplay(macro.icon, displaySize = 32.dp)
                            Spacer(Modifier.width(16.dp))
                            Text(macro.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                        }
                    }
                }
                if (macros.isEmpty()) {
                    Text(stringResource(R.string.no_macros_msg), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
fun AppPickerDialog(
    title: String,
    slot: TapSlot,
    current: String?,
    keyMap: KeyMap = KeyMap(),
    apps: List<AppInfo>? = null,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.85f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                UniversalActionListContent(
                    keyMap = keyMap,
                    apps = apps,
                    currentActionId = current,
                    slot = slot,
                    showDefaultOption = true,
                    onActionSelected = { pkg ->
                        onPick(pkg)
                    }
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Готово", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun EventActionPickerDialog(
    triggerGesture: TapSlot,
    keyMap: KeyMap = KeyMap(),
    apps: List<AppInfo>? = null,
    allowMacros: Boolean = true,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212),
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.85f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_action_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                UniversalActionListContent(
                    keyMap = keyMap,
                    apps = apps,
                    currentActionId = null,
                    slot = null,
                    showDefaultOption = false,
                    onActionSelected = { pkg ->
                        onPick(pkg)
                    }
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Готово", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GlyphPatternPickerDialog(
    patterns: List<Macro>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.nav_games),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (patterns.isEmpty()) {
                    Text(stringResource(R.string.no_macros_msg), color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
                }
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(patterns) { pattern ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(Config.ACTION_GLYPH_PATTERN_PREFIX + pattern.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EventIconDisplay(pattern.icon, displaySize = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(pattern.title, color = Color.White)
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun EventConditionPickerDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(stringResource(R.string.select_condition_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.wrapContentHeight()) {
                    item { PickRow(stringResource(R.string.always), null, false) { onPick("condition:always") } }
                    item { PickRow(stringResource(R.string.cond_app_label), null, false) { onPick("condition:app") } }
                    item { PickRow(stringResource(R.string.cond_time_label), null, false) { onPick("condition:time") } }
                    item { PickRow(stringResource(R.string.cond_rotation_label), null, false) { onPick("condition:rotation") } }
                    item { PickRow(stringResource(R.string.cond_light_label), null, false) { onPick("condition:light") } }
                    item { PickRow(stringResource(R.string.cond_brightness_label), null, false) { onPick("condition:brightness") } }
                    item { PickRow(stringResource(R.string.cond_battery_label), null, false) { onPick("condition:battery") } }
                    item { PickRow(stringResource(R.string.cond_wifi_label), null, false) { onPick("condition:wifi") } }
                    item { PickRow(stringResource(R.string.cond_bt_label), null, false) { onPick("condition:bluetooth") } }
                    item { PickRow(stringResource(R.string.cond_loc_label), null, false) { onPick("condition:location") } }
                    item { PickRow(stringResource(R.string.cond_data_label), null, false) { onPick("condition:mobile_data") } }
                    item { PickRow(stringResource(R.string.cond_charging_label), null, false) { onPick("condition:charging") } }
                    item { PickRow(stringResource(R.string.cond_lock_label), null, false) { onPick("condition:screen_lock") } }
                    item { PickRow(stringResource(R.string.cond_airplane_label), null, false) { onPick("condition:airplane_mode") } }
                    item { PickRow(stringResource(R.string.cond_nfc_label), null, false) { onPick("condition:nfc") } }
                    item { PickRow(stringResource(R.string.cond_auto_rotate_label), null, false) { onPick("condition:auto_rotate") } }
                    item { PickRow(stringResource(R.string.cond_headphones_label), null, false) { onPick("condition:headphones") } }
                    item { PickRow(stringResource(R.string.cond_dnd_label), null, false) { onPick("condition:dnd") } }
                    item { PickRow(stringResource(R.string.cond_motion_label), null, false) { onPick("condition:motion") } }
                    item { PickRow(stringResource(R.string.cond_dark_mode_label), null, false) { onPick("condition:dark_mode") } }
                    item { PickRow(stringResource(R.string.cond_silent_label), null, false) { onPick("condition:silent_mode") } }
                    item { PickRow(stringResource(R.string.cond_vibrate_label), null, false) { onPick("condition:vibrate_mode") } }
                    item { PickRow(stringResource(R.string.cond_hotspot_label), null, false) { onPick("condition:hotspot") } }
                }
            }
        }
    }
}

@Composable
fun EventIconPickerDialog(currentIcon: String, apps: List<AppInfo>?, onDismiss: () -> Unit, onIconPicked: (String) -> Unit) {
    val initialTab = when {
        currentIcon.startsWith("material:") -> 0
        currentIcon.contains(".") -> 1
        else -> 2
    }
    var tabIndex by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf(
        "Системные",
        stringResource(R.string.tab_apps),
        stringResource(R.string.tab_symbols)
    )
    val ctx = LocalContext.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            "Все", "Общие", "Медиа", "Связь", "Устройства", "Навигация",
            "Файлы", "Соцсети", "Фигуры", "Спорт", "Система", "Текст", "Магазин"
        )
    }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0D0D0D),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск иконок...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }
                    } else null
                )

                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {},
                    indicator = { tabPositions ->
                        if (tabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                                color = Color.White
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (tabIndex) {
                        0 -> { // System Icons (Material)
                            Column {
                                // Categories Scroll
                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(categories) { category ->
                                        val isSelected = (selectedCategory == category) || (category == "Все" && selectedCategory == null)
                                        Surface(
                                            onClick = { selectedCategory = if (category == "Все") null else category },
                                            color = if (isSelected) Color.White else Color(0xFF2B2B2B),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                                Text(category, color = if (isSelected) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                val filteredIcons = remember(searchQuery, selectedCategory) {
                                    var list = if (searchQuery.isEmpty()) allMaterialIcons
                                    else allMaterialIcons.filter { it.first.contains(searchQuery, ignoreCase = true) }

                                    if (selectedCategory != null) {
                                        // Filter based on the structure of MaterialIconsList.kt
                                        val commonIdx = allMaterialIcons.indexOfFirst { it.first == "Home" }
                                        val mediaIdx = allMaterialIcons.indexOfFirst { it.first == "PlayArrow" }
                                        val deviceIdx = allMaterialIcons.indexOfFirst { it.first == "Wifi" }
                                        val navIdx = allMaterialIcons.indexOfFirst { it.first == "Map" }
                                        val commIdx = allMaterialIcons.indexOfFirst { it.first == "Chat" }
                                        val fileIdx = allMaterialIcons.indexOfFirst { it.first == "Folder" }
                                        val socialIdx = allMaterialIcons.indexOfFirst { it.first == "Groups" }
                                        val shapesIdx = allMaterialIcons.indexOfFirst { it.first == "Circle" }
                                        val lifestyleIdx = allMaterialIcons.indexOfFirst { it.first == "SportsEsports" }
                                        val systemIdx = allMaterialIcons.indexOfFirst { it.first == "LockOpen" }
                                        val arrowIdx = allMaterialIcons.indexOfFirst { it.first == "ArrowBack" }
                                        val textIdx = allMaterialIcons.indexOfFirst { it.first == "FormatBold" }
                                        val shopIdx = allMaterialIcons.indexOfFirst { it.first == "ShoppingBag" }
                                        val healthIdx = allMaterialIcons.indexOfFirst { it.first == "LocalHospital" }

                                        list = when(selectedCategory) {
                                            "Общие" -> allMaterialIcons.subList(commonIdx, mediaIdx)
                                            "Медиа" -> allMaterialIcons.subList(mediaIdx, deviceIdx)
                                            "Устройства" -> allMaterialIcons.subList(deviceIdx, navIdx)
                                            "Навигация" -> allMaterialIcons.subList(navIdx, commIdx)
                                            "Связь" -> allMaterialIcons.subList(commIdx, fileIdx)
                                            "Файлы" -> allMaterialIcons.subList(fileIdx, socialIdx)
                                            "Соцсети" -> allMaterialIcons.subList(socialIdx, shapesIdx)
                                            "Фигуры" -> allMaterialIcons.subList(shapesIdx, lifestyleIdx)
                                            "Спорт" -> allMaterialIcons.subList(lifestyleIdx, systemIdx)
                                            "Система" -> allMaterialIcons.subList(systemIdx, arrowIdx)
                                            "Текст" -> allMaterialIcons.subList(textIdx, shopIdx)
                                            "Магазин" -> allMaterialIcons.subList(shopIdx, healthIdx)
                                            else -> list
                                        }

                                        if (searchQuery.isNotEmpty()) {
                                            list = list.filter { it.first.contains(searchQuery, ignoreCase = true) }
                                        }
                                    }
                                    list
                                }

                                LazyVerticalGrid(columns = GridCells.Adaptive(72.dp)) {
                                    items(filteredIcons) { (name, icon) ->
                                        val isCurrent = currentIcon == "material:$name"
                                        Column(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .aspectRatio(1f)
                                                .background(if (isCurrent) Color.White.copy(alpha = 0.1f) else Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                                .border(if (isCurrent) 2.dp else 0.dp, if (isCurrent) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                                .clickable { onIconPicked("material:$name") }
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(imageVector = icon, contentDescription = name, tint = Color.White, modifier = Modifier.size(28.dp))
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                name,
                                                color = if (isCurrent) Color.White else Color.Gray,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Apps
                            if (apps == null) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
                            } else {
                                val filteredApps = apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                                LazyColumn {
                                    items(filteredApps) { app ->
                                        val isCurrent = currentIcon == app.pkg
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isCurrent) Color.White.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                                                .border(if (isCurrent) 1.dp else 0.dp, if (isCurrent) Color.White.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp))
                                                .clickable { onIconPicked(app.pkg) }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text(app.label, color = if (isCurrent) Color.White else Color.Gray)
                                            if (isCurrent) {
                                                Spacer(Modifier.weight(1f))
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Symbols
                            var customText by remember { mutableStateOf(if (initialTab == 2) currentIcon else "") }
                            Column {
                                TextField(
                                    value = customText,
                                    onValueChange = { if (it.length <= 3) customText = it },
                                    placeholder = { Text(stringResource(R.string.symbol_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1A1A1A),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { if (customText.isNotEmpty()) onIconPicked(customText) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) { Text(stringResource(R.string.select_btn)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RotationPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    val options = listOf(
        "PORTRAIT" to stringResource(R.string.rotation_portrait_desc),
        "FACE_UP" to stringResource(R.string.rotation_face_up_desc),
        "FACE_DOWN" to stringResource(R.string.rotation_face_down_desc),
        "LANDSCAPE_LEFT" to stringResource(R.string.rotation_landscape_left_desc),
        "LANDSCAPE_RIGHT" to stringResource(R.string.rotation_landscape_right_desc)
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.cond_rotation_label).substring(0, 11), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                options.forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth().clickable { onConfirmed(key) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = initialValue == key, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun LightRangePickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    val initialParts = initialValue.split("-")
    val p1 = initialParts.getOrNull(0)?.toFloatOrNull() ?: 0f
    val p2 = initialParts.getOrNull(1)?.toFloatOrNull() ?: 100f

    val startVal = p1.coerceIn(0f, 100f)
    val endVal = p2.coerceIn(0f, 100f).coerceAtLeast(startVal)

    var range by remember { mutableStateOf(startVal..endVal) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.light_threshold), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.light_condition_desc), color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.range_percent_label, range.start.toInt(), range.endInclusive.toInt()), color = Color.White, style = MaterialTheme.typography.headlineMedium)

                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed("${range.start.toInt()}-${range.endInclusive.toInt()}") }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun RangePickerDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    val initialParts = initialValue.split("-")
    val p1 = initialParts.getOrNull(0)?.toFloatOrNull() ?: 0f
    val p2 = initialParts.getOrNull(1)?.toFloatOrNull() ?: 100f

    val startVal = p1.coerceIn(0f, 100f)
    val endVal = p2.coerceIn(0f, 100f).coerceAtLeast(startVal)

    var range by remember { mutableStateOf(startVal..endVal) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.range_condition_desc), color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.range_percent_label, range.start.toInt(), range.endInclusive.toInt()), color = Color.White, style = MaterialTheme.typography.headlineMedium)

                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )

                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed("${range.start.toInt()}-${range.endInclusive.toInt()}") }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun BinaryStatePickerDialog(
    title: String,
    current: String,
    onLabel: String = stringResource(R.string.state_on_label),
    offLabel: String = stringResource(R.string.state_off_label),
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(16.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth().clickable { onConfirmed("ON") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = current == "ON", onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(onLabel, color = Color.White)
                }
                Row(Modifier.fillMaxWidth().clickable { onConfirmed("OFF") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = current == "OFF", onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(offLabel, color = Color.White)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
fun BrightnessPickerDialog(initialValue: Int, onDismiss: () -> Unit, onConfirmed: (Int) -> Unit) {
    var percent by remember { mutableFloatStateOf(initialValue.toFloat()) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.brightness_screen_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(32.dp))
                Text("${percent.toInt()}%", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Slider(
                    value = percent,
                    onValueChange = { percent = it },
                     valueRange = 1f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                )
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed(percent.toInt()) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun VolumePickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    var stream by remember { mutableStateOf("media") }
    var direction by remember { mutableStateOf("up") }
    var showLevelPicker by remember { mutableStateOf(false) }
    var level by remember { mutableFloatStateOf(50f) }

    if (showLevelPicker) {
        Dialog(onDismissRequest = { showLevelPicker = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.action_volume), color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(32.dp))
                    Text("${level.toInt()}%", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Slider(
                        value = level,
                        onValueChange = { level = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                    )
                    Spacer(Modifier.height(32.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { showLevelPicker = false }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onConfirmed("$stream:set:${level.toInt()}"); showLevelPicker = false; onDismiss() }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_volume), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.stream_label), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("media" to stringResource(R.string.stream_media), "ring" to stringResource(R.string.stream_ring), "notif" to stringResource(R.string.stream_notif)).forEach { (key, label) ->
                        FilterChip(
                            selected = stream == key,
                            onClick = { stream = key },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.action_label), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("up" to stringResource(R.string.action_up), "down" to stringResource(R.string.action_down)).forEach { (key, label) ->
                            FilterChip(
                                selected = direction == key,
                                onClick = { direction = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("mute" to stringResource(R.string.action_mute), "set" to stringResource(R.string.action_set_percent)).forEach { (key, label) ->
                            FilterChip(
                                selected = direction == key,
                                onClick = {
                                    if (key == "set") {
                                        showLevelPicker = true
                                    } else {
                                        direction = key
                                    }
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed("$stream:$direction") }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun MediaPickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.media_buttons_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                listOf("play_pause" to "Play / Pause", "next" to stringResource(R.string.media_next), "prev" to stringResource(R.string.media_prev), "stop" to stringResource(R.string.media_stop)).forEach { (key, label) ->
                    TextButton(onClick = { onConfirmed(key) }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
fun UrlPickerDialog(initialValue: String, onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    var url by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_open_url), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.White, focusedLabelColor = Color.White)
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirmed(url) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun ThemePickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_change_theme), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                listOf("light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark_short), "toggle" to stringResource(R.string.toggle)).forEach { (key, label) ->
                    TextButton(onClick = { onConfirmed(key) }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
fun AutoBrightnessPickerDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.action_auto_brightness), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                listOf("on" to stringResource(R.string.turn_on), "off" to stringResource(R.string.turn_off), "toggle" to stringResource(R.string.toggle)).forEach { (key, label) ->
                    TextButton(onClick = { onConfirmed(key) }, modifier = Modifier.fillMaxWidth()) { Text(label, color = Color.White) }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

@Composable
fun TimeRangePickerDialog(
    start: String?,
    end: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String, String) -> Unit
) {
    var sHour by remember { mutableStateOf(start?.split(":")?.get(0) ?: "00") }
    var sMin by remember { mutableStateOf(start?.split(":")?.get(1) ?: "00") }
    var eHour by remember { mutableStateOf(end?.split(":")?.get(0) ?: "23") }
    var eMin by remember { mutableStateOf(end?.split(":")?.get(1) ?: "59") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_time), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.time_from), color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeField(sHour) { sHour = it }
                    Text(" : ", color = Color.White)
                    TimeField(sMin) { sMin = it }
                }

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.time_to), color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeField(eHour) { eHour = it }
                    Text(" : ", color = Color.White)
                    TimeField(eMin) { eMin = it }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onConfirmed("$sHour:$sMin", "$eHour:$eMin") }) { Text("OK", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun TimeField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) onValueChange(it) },
        modifier = Modifier.width(64.dp),
        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchPointPickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (Float, Float, String) -> Unit
) {
    val density = LocalDensity.current
    val markerSize = 80.dp
    val markerRadiusPx = with(density) { (markerSize / 2).toPx() }

    val parts = initialValue?.split("_")
    var pos by remember {
        val initial = if (parts != null && parts.size >= 2) Offset(parts[0].toFloat(), parts[1].toFloat()) else Offset(540f, 1100f)
        mutableStateOf(initial)
    }
    var holdDuration by remember {
        mutableStateOf(if (parts != null && parts.size >= 3) parts[2] else "200")
    }
    var showHoldMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.7f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sw = this.constraints.maxWidth.toFloat()
                val sh = this.constraints.maxHeight.toFloat()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showHoldMenu) {
                        Box(Modifier.fillMaxSize().clickable { showHoldMenu = false })
                    }

                    Text(
                        stringResource(R.string.touch_picker_hint),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pos.x - markerRadiusPx).roundToInt(),
                                    (pos.y - markerRadiusPx - (sh * 0.05f)).roundToInt() // Visual shift -5%
                                )
                            }
                            .size(markerSize)
                            .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .pointerInput(Unit) { detectTapGestures(onTap = { showHoldMenu = true }) }
                            .pointerInput(sw, sh) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    pos = Offset((pos.x + dragAmount.x).coerceIn(0f, sw), (pos.y + dragAmount.y).coerceIn(0f, sh))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(12.dp).background(Color.White, CircleShape))
                    }

                    if (showHoldMenu) {
                        HoldMenu(
                            value = holdDuration,
                            onDismiss = { showHoldMenu = false },
                            onConfirm = { holdDuration = it; showHoldMenu = false }
                        )
                    }

                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onConfirmed(pos.x, pos.y, holdDuration) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(stringResource(R.string.confirm_btn)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipePointPickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (Float, Float, Float, Float, String, String) -> Unit
) {
    val density = LocalDensity.current
    val markerSize = 80.dp
    val markerRadiusPx = with(density) { (markerSize / 2).toPx() }

    val parts = initialValue?.split("_")
    var startPos by remember {
        val initial = if (parts != null && parts.size >= 4) Offset(parts[0].toFloat(), parts[1].toFloat()) else Offset(300f, 1100f)
        mutableStateOf(initial)
    }
    var endPos by remember {
        val initial = if (parts != null && parts.size >= 4) Offset(parts[2].toFloat(), parts[3].toFloat()) else Offset(700f, 1100f)
        mutableStateOf(initial)
    }
    var holdStart by remember { mutableStateOf(if (parts != null && parts.size >= 5) parts[4] else "0") }
    var holdEnd by remember { mutableStateOf(if (parts != null && parts.size >= 6) parts[5] else "0") }

    var showHoldStart by remember { mutableStateOf(false) }
    var showHoldEnd by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "swipe")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing)),
        label = "progress"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.8f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sw = this.constraints.maxWidth.toFloat()
                val sh = this.constraints.maxHeight.toFloat()

                Box(modifier = Modifier.fillMaxSize()) {
                    if (showHoldStart || showHoldEnd) {
                        Box(Modifier.fillMaxSize().clickable { showHoldStart = false; showHoldEnd = false })
                    }

                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(sw, sh) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            startPos = Offset((startPos.x + dragAmount.x).coerceIn(0f, sw), (startPos.y + dragAmount.y).coerceIn(0f, sh))
                            endPos = Offset((endPos.x + dragAmount.x).coerceIn(0f, sw), (endPos.y + dragAmount.y).coerceIn(0f, sh))
                        }
                    }) {
                        val visualShift = size.height * 0.05f
                        val vStart = Offset(startPos.x, startPos.y - visualShift)
                        val vEnd = Offset(endPos.x, endPos.y - visualShift)

                        drawLine(color = Color.Gray, start = vStart, end = vEnd, strokeWidth = 8f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                        val currentPos = Offset(lerp(vStart.x, vEnd.x, progress), lerp(vStart.y, vEnd.y, progress))
                        drawCircle(color = Color.White, radius = 20f, center = currentPos)
                    }

                    Text(stringResource(R.string.swipe_picker_hint), color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp), style = MaterialTheme.typography.titleMedium)

                    // Start Marker
                    Box(modifier = Modifier.offset { IntOffset((startPos.x - markerRadiusPx).roundToInt(), (startPos.y - markerRadiusPx - (sh * 0.05f)).roundToInt()) }.size(markerSize)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape).border(2.dp, Color.White, CircleShape)
                        .pointerInput(Unit) { detectTapGestures(onTap = { showHoldStart = true }) }
                        .pointerInput(sw, sh) { detectDragGestures { change, dragAmount -> change.consume(); startPos = Offset((startPos.x + dragAmount.x).coerceIn(0f, sw), (startPos.y + dragAmount.y).coerceIn(0f, sh)) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Color.White)
                    }

                    // End Marker
                    Box(modifier = Modifier.offset { IntOffset((endPos.x - markerRadiusPx).roundToInt(), (endPos.y - markerRadiusPx - (sh * 0.05f)).roundToInt()) }.size(markerSize)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape).border(2.dp, Color.White, CircleShape)
                        .pointerInput(Unit) { detectTapGestures(onTap = { showHoldEnd = true }) }
                        .pointerInput(sw, sh) { detectDragGestures { change, dragAmount -> change.consume(); endPos = Offset((endPos.x + dragAmount.x).coerceIn(0f, sw), (endPos.y + dragAmount.y).coerceIn(0f, sh)) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("B", color = Color.Black)
                    }

                    if (showHoldStart) {
                        HoldMenu(holdStart, onDismiss = { showHoldStart = false }, onConfirm = { holdStart = it; showHoldStart = false })
                    }
                    if (showHoldEnd) {
                        HoldMenu(holdEnd, onDismiss = { showHoldEnd = false }, onConfirm = { holdEnd = it; showHoldEnd = false })
                    }

                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = { onConfirmed(startPos.x, startPos.y, endPos.x, endPos.y, holdStart, holdEnd) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text(stringResource(R.string.confirm_btn)) }
                    }
                }
            }
        }
    }
}

@Composable
fun HoldMenu(value: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(0.85f), shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.hold_ms_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { if (it.all { c -> c.isDigit() }) text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color.White)
                )
                Spacer(Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = { onConfirm(text.ifEmpty { "0" }) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun DelayPickerDialog(
    initialValue: String?,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue ?: "1.0") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${stringResource(R.string.action_delay)} (${stringResource(R.string.unit_seconds)})", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                    colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) }
                    TextButton(onClick = { onConfirmed(text) }) { Text("OK", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun WaitConfirmationDialog(
    initialValue: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val prefix = Config.ACTION_WAIT_CONFIRMATION_PREFIX
    val parts = initialValue?.removePrefix(prefix)?.split(",")
    var keepOnClick by remember { mutableStateOf(parts?.getOrNull(0)?.toBoolean() ?: false) }
    var keepOnOutside by remember { mutableStateOf(parts?.getOrNull(1)?.toBoolean() ?: false) }
    var keepOnKey by remember { mutableStateOf(parts?.getOrNull(2)?.toBoolean() ?: false) }
    var autoHideSeconds by remember { mutableStateOf(parts?.getOrNull(3)?.toIntOrNull() ?: 5) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A)) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(R.string.shortcut_settings), color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                ShortcutToggle(stringResource(R.string.keep_on_click), keepOnClick) { keepOnClick = it }
                ShortcutToggle(stringResource(R.string.keep_on_outside), keepOnOutside) { keepOnOutside = it }
                ShortcutToggle(stringResource(R.string.keep_on_key), keepOnKey) { keepOnKey = it }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.auto_hide), style = MaterialTheme.typography.labelLarge, color = Color.Gray)

                val autoHideOptions = listOf(
                    0 to stringResource(R.string.never_hide),
                    5 to "5 ${stringResource(R.string.unit_seconds)}",
                    10 to "10 ${stringResource(R.string.unit_seconds)}",
                    20 to "20 ${stringResource(R.string.unit_seconds)}"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(autoHideOptions) { (secs, label) ->
                        val selected = autoHideSeconds == secs
                        Surface(
                            onClick = { autoHideSeconds = secs },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color.White else Color(0xFF2B2B2B),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onDismiss, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text(stringResource(R.string.cancel)) }
                    Button(onClick = {
                        val canHide = !keepOnClick || !keepOnOutside || !keepOnKey || autoHideSeconds > 0
                        if (canHide) {
                            onConfirm("$prefix$keepOnClick,$keepOnOutside,$keepOnKey,$autoHideSeconds")
                        } else {
                            android.widget.Toast.makeText(ctx, ctx.getString(R.string.choose_hide_method_msg), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun SpeedCoefficientDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onConfirmed: (Float) -> Unit
) {
    var text by remember { mutableStateOf(currentSpeed.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Скорость видео", color = Color.White) },
        text = {
            Column {
                Text("Введите коэффициент (например, 0.5 или 2.0)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = text.toFloatOrNull() ?: 1.0f
                onConfirmed(s.coerceIn(0.1f, 5.0f))
            }) {
                Text("ОК", color = Color(0xFFFFD700))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ОТМЕНА", color = Color.Gray)
            }
        }
    )
}
