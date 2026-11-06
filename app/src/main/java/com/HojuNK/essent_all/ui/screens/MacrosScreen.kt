package com.HojuNK.essent_all.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import android.view.Surface as ViewSurface
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class MacroFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    SINGLE(R.string.tap_single),
    LONG(R.string.tap_long),
    DOUBLE(R.string.tap_double),
    TRIPLE(R.string.tap_triple)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onEditMacro: (Macro) -> Unit,
    onShowAccessibilityError: () -> Unit,
    isSelectionMode: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val radialMenuState = LocalRadialMenuState.current
    val showRadialMenu = LocalRadialMenu.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var fabPosition by remember { mutableStateOf(Offset.Zero) }
    var fabTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }

    var selectedFilters by remember { mutableStateOf(setOf<MacroFilter>()) }
    var showViewAndSortDialog by remember { mutableStateOf(false) }

    var localSelectionMode by remember { mutableStateOf(isSelectionMode) }
    LaunchedEffect(isSelectionMode) {
        localSelectionMode = isSelectionMode
    }
    val updateSelectionMode: (Boolean) -> Unit = { active ->
        localSelectionMode = active
        onSelectionModeChange(active)
    }

    var selectedMacroIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = localSelectionMode) {
        updateSelectionMode(false)
        selectedMacroIds = emptySet()
    }

    val sortedMacros = remember(keyMap.macros, keyMap.macrosSortOption, keyMap.macrosSortAscending) {
        val base = keyMap.macros.map { if (it.createdAt <= 0L) it.copy(createdAt = System.currentTimeMillis()) else it }
        val ascending = keyMap.macrosSortAscending
        when (keyMap.macrosSortOption) {
            ListSortOption.CREATION_DATE -> if (ascending) base.sortedBy { it.createdAt } else base.sortedByDescending { it.createdAt }
            ListSortOption.ALPHABETICAL -> if (ascending) base.sortedBy { it.title.lowercase() } else base.sortedByDescending { it.title.lowercase() }
            ListSortOption.RANKING -> if (ascending) base else base.reversed()
        }
    }

    val filteredMacros = remember(sortedMacros, selectedFilters) {
        sortedMacros.filter { macro ->
            val actionStr = "${Config.ACTION_MACRO_PREFIX}${macro.id}"
            if (selectedFilters.isEmpty()) true
            else selectedFilters.any { filter ->
                when (filter) {
                    MacroFilter.SINGLE -> keyMap.single == actionStr
                    MacroFilter.LONG -> keyMap.long == actionStr
                    MacroFilter.DOUBLE -> keyMap.double == actionStr
                    MacroFilter.TRIPLE -> keyMap.triple == actionStr
                    else -> false
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val rotation = ctx.display?.rotation ?: ViewSurface.ROTATION_0
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                    TopAppBar(
                        title = {
                            if (localSelectionMode) {
                                Text(stringResource(R.string.selected_count, selectedMacroIds.size), color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                TypewriterTitle(targetText = stringResource(R.string.nav_macros), animate = false)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                            if (localSelectionMode) {
                                IconButton(onClick = { updateSelectionMode(false); selectedMacroIds = emptySet() }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        },
                        actions = {
                            if (localSelectionMode) {
                                val isAllSelected = filteredMacros.isNotEmpty() && filteredMacros.all { selectedMacroIds.contains(it.id) }
                                IconButton(onClick = {
                                    selectedMacroIds = if (isAllSelected) emptySet() else filteredMacros.map { it.id }.toSet()
                                }) {
                                    Icon(if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll, null, tint = Color.White)
                                }
                            } else {
                                IconButton(onClick = { updateSelectionMode(true) }) {
                                    Icon(Icons.Default.Checklist, null, tint = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .combinedClickable(
                                            onClick = {
                                                val nextMode = if (keyMap.macrosViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                                scope.launch { Config.setMacrosViewMode(ctx, nextMode) }
                                            },
                                            onLongClick = { showViewAndSortDialog = true }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (keyMap.macrosViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, null)
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButtonPosition = if (isLandscape && rotation == ViewSurface.ROTATION_270) FabPosition.Start else FabPosition.End,
            floatingActionButton = {
                if (!localSelectionMode) {
                    val density = LocalDensity.current
                    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current

                    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

                    Box(modifier = Modifier
                        .padding(
                            bottom = if (isLandscape) 24.dp else 120.dp,
                            start = if (isLandscape && rotation == ViewSurface.ROTATION_270) 135.dp else 0.dp,
                            end = if (isLandscape && rotation == ViewSurface.ROTATION_90) 135.dp else 0.dp
                        )
                        .onGloballyPositioned {
                            val pos = it.positionInRoot()
                            val size = it.size
                            fabTopLeftInRoot = pos
                            fabPosition = pos + Offset(size.width / 2f, size.height / 2f)
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val actions = listOf(
                                    RadialAction(Icons.Default.TouchApp, ctx.getString(R.string.macro_type_tap)) {
                                        if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                            val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                                action = "com.HojuNK.essent_all.START_MACRO_REC"
                                                putExtra("mode", MacroRecordingMode.SINGLE_TAP.name)
                                            }
                                            ctx.startService(intent)
                                        } else {
                                            onShowAccessibilityError()
                                        }
                                    },
                                    RadialAction(Icons.Default.Swipe, ctx.getString(R.string.macro_type_swipe)) {
                                        if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                            val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                                action = "com.HojuNK.essent_all.START_MACRO_REC"
                                                putExtra("mode", MacroRecordingMode.SINGLE_SWIPE.name)
                                            }
                                            ctx.startService(intent)
                                        } else {
                                            onShowAccessibilityError()
                                        }
                                    },
                                    RadialAction(Icons.Default.Gesture, ctx.getString(R.string.macro_type_combo)) {
                                        if (isAccessibilityServiceEnabled(ctx, EssentialKeyService::class.java)) {
                                            val intent = Intent(ctx, EssentialKeyService::class.java).apply {
                                                action = "com.HojuNK.essent_all.START_MACRO_REC"
                                                putExtra("mode", MacroRecordingMode.COMBINATION.name)
                                            }
                                            ctx.startService(intent)
                                        } else {
                                            onShowAccessibilityError()
                                        }
                                    },
                                    RadialAction(Icons.Default.Add, ctx.getString(R.string.macro_type_manual)) {
                                        onEditMacro(Macro(UUID.randomUUID().toString(), "🤖", "", actions = emptyList(), isEnabled = true))
                                    }
                                )

                                var menuTriggered = false
                                var currentHoveredIndex = -1
                                var dismissOnExit = true

                                val fabRect = androidx.compose.ui.geometry.Rect(fabTopLeftInRoot, fabSize.toSize())
                                val fabContent = @Composable {
                                    FloatingActionButton(
                                        onClick = {},
                                        containerColor = Color.Gray,
                                        contentColor = Color.White,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                    }
                                }

                                val lpJob = scope.launch {
                                    delay(viewConfig.longPressTimeoutMillis)
                                    menuTriggered = true
                                    showRadialMenu(RadialMenuState(
                                        center = fabPosition, 
                                        actions = actions, 
                                        sourceId = "FAB_MACROS", 
                                        type = MenuType.RECTANGULAR,
                                        sourceBounds = fabRect,
                                        sourceContent = fabContent
                                    ))
                                }

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                        val change = event.changes.find { it.id == down.id }

                                        if (menuTriggered) {
                                            change?.consume()
                                            if (change != null) {
                                                val rootPos = change.position + fabTopLeftInRoot
                                                val bestIndex = calculateRectangularRadialHover(
                                                    currentPos = rootPos,
                                                    menuCenter = fabPosition,
                                                    itemCount = actions.size,
                                                    density = density,
                                                    screenWidthPx = screenWidthPx,
                                                    screenHeightPx = screenHeightPx
                                                )
                                                if (bestIndex != currentHoveredIndex) {
                                                    currentHoveredIndex = bestIndex
                                                    showRadialMenu(RadialMenuState(
                                                        center = fabPosition, 
                                                        actions = actions, 
                                                        hoveredIndex = bestIndex, 
                                                        sourceId = "FAB_MACROS", 
                                                        type = MenuType.RECTANGULAR,
                                                        sourceBounds = fabRect,
                                                        sourceContent = fabContent
                                                    ))
                                                }
                                            }

                                            if (isReleased) {
                                                if (currentHoveredIndex != -1) {
                                                    actions[currentHoveredIndex].onClick()
                                                }
                                                break
                                            }
                                        } else {
                                            if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                                lpJob.cancel()
                                                if (change != null && (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                                    menuTriggered = true
                                                    showRadialMenu(RadialMenuState(
                                                        center = fabPosition, 
                                                        actions = actions, 
                                                        hoveredIndex = -1, 
                                                        sourceId = "FAB_MACROS", 
                                                        type = MenuType.RECTANGULAR,
                                                        sourceBounds = fabRect,
                                                        sourceContent = fabContent
                                                    ))
                                                } else {
                                                    break
                                                }
                                            }
                                            if (isReleased) {
                                                lpJob.cancel()
                                                showRadialMenu(RadialMenuState(
                                                    center = fabPosition, 
                                                    actions = actions, 
                                                    hoveredIndex = -1, 
                                                    sourceId = "FAB_MACROS", 
                                                    type = MenuType.RECTANGULAR,
                                                    sourceBounds = fabRect,
                                                    sourceContent = fabContent
                                                ))
                                                dismissOnExit = false
                                                break
                                            }
                                        }
                                        if (change == null && !isReleased) break
                                    }
                                } finally {
                                    lpJob.cancel()
                                    if (dismissOnExit && menuTriggered) showRadialMenu(null)
                                }
                            }
                        }
                    ) {
                        val isActive = radialMenuState != null && radialMenuState.sourceId == "FAB_MACROS"
                        FloatingActionButton(
                            onClick = { /* Handled by pointerInput */ },
                            containerColor = Color.Gray,
                            contentColor = Color.White,
                            modifier = Modifier
                                .onGloballyPositioned { fabSize = it.size }
                                .graphicsLayer { alpha = if (isActive) 0f else 1.0f }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_macro))
                        }
                    }
                }
            }
        ) { inner ->
            val topPadding = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                inner.calculateTopPadding()
            } else {
                0.dp
            }
            
            val startPadding = if (isLandscape) {
                if (rotation == ViewSurface.ROTATION_270) 130.dp else 80.dp
            } else 0.dp
            
            val endPadding = if (isLandscape) {
                if (rotation == ViewSurface.ROTATION_90) 130.dp else 80.dp
            } else 0.dp

            if (filteredMacros.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(top = topPadding, start = startPadding, end = endPadding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.macros_empty_msg), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                val isStripeMode = keyMap.macrosViewMode == EventsViewMode.STRIPES
                val numColumns = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    if (isStripeMode) 2 else 4
                } else {
                    if (isStripeMode) 1 else 2
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(numColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp + startPadding, 
                        end = 16.dp + endPadding, 
                        top = topPadding + 16.dp, 
                        bottom = inner.calculateBottomPadding() + 16.dp + (if (isLandscape) 0.dp else 120.dp)
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(numColumns) }) {
                        val filters = listOf(MacroFilter.ALL, MacroFilter.SINGLE, MacroFilter.LONG, MacroFilter.DOUBLE, MacroFilter.TRIPLE)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters, key = { it.name }) { filter ->
                                val isAll = filter == MacroFilter.ALL
                                val isSelected = if (isAll) selectedFilters.isEmpty() else selectedFilters.contains(filter)
                                val showCross = isAll && selectedFilters.isNotEmpty()

                                Surface(
                                    onClick = {
                                        if (isAll) {
                                            selectedFilters = emptySet()
                                        } else {
                                            selectedFilters = if (selectedFilters.contains(filter)) {
                                                selectedFilters - filter
                                            } else {
                                                selectedFilters + filter
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color.White else Color(0xFF2B2B2B),
                                    border = if (!isSelected) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)) else null,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (showCross) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text(
                                                text = stringResource(filter.labelRes),
                                                color = if (isSelected) Color.Black else Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(filteredMacros, key = { it.id }) { macro ->
                        val isSelected = selectedMacroIds.contains(macro.id)
                        val onSelectToggle = {
                            selectedMacroIds = if (isSelected) selectedMacroIds - macro.id else selectedMacroIds + macro.id
                        }
                        if (isStripeMode) {
                            StripeMacroItem(
                                keyMap = keyMap,
                                macro = macro,
                                isSelectionMode = localSelectionMode,
                                isSelected = isSelected,
                                onSelectToggle = onSelectToggle,
                                onToggle = { enabled ->
                                    scope.launch {
                                        val updated = macro.copy(isEnabled = enabled)
                                        Config.updateMacro(ctx, updated)
                                    }
                                },
                                onEdit = { onEditMacro(macro) },
                                onDuplicate = {
                                    val newMacro = macro.copy(id = UUID.randomUUID().toString(), title = "${macro.title}${ctx.getString(R.string.copy_suffix)}", createdAt = System.currentTimeMillis())
                                    scope.launch { Config.updateMacro(ctx, newMacro) }
                                },
                                onDelete = {
                                    scope.launch { Config.deleteMacro(ctx, macro.id) }
                                }
                            )
                        } else {
                            RectangleMacroItem(
                                keyMap = keyMap,
                                macro = macro,
                                isSelectionMode = localSelectionMode,
                                isSelected = isSelected,
                                onSelectToggle = onSelectToggle,
                                onToggle = { enabled ->
                                    scope.launch {
                                        val updated = macro.copy(isEnabled = enabled)
                                        Config.updateMacro(ctx, updated)
                                    }
                                },
                                onEdit = { onEditMacro(macro) },
                                onDuplicate = {
                                    val newMacro = macro.copy(id = UUID.randomUUID().toString(), title = "${macro.title}${ctx.getString(R.string.copy_suffix)}", createdAt = System.currentTimeMillis())
                                    scope.launch { Config.updateMacro(ctx, newMacro) }
                                },
                                onDelete = {
                                    scope.launch { Config.deleteMacro(ctx, macro.id) }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (localSelectionMode) {
            val panelModifier = when {
                isLandscape && rotation == ViewSurface.ROTATION_90 -> Modifier.align(Alignment.CenterEnd)
                isLandscape && rotation == ViewSurface.ROTATION_270 -> Modifier.align(Alignment.CenterStart)
                else -> Modifier.align(Alignment.BottomCenter)
            }
            MacroSelectionBottomPanel(
                selectedCount = selectedMacroIds.size,
                isLandscape = isLandscape,
                rotation = rotation,
                onEnable = {
                    val ids = selectedMacroIds
                    scope.launch {
                        ids.forEach { id ->
                            keyMap.macros.find { it.id == id }?.let { m ->
                                Config.updateMacro(ctx, m.copy(isEnabled = true))
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedMacroIds = emptySet()
                },
                onDisable = {
                    val ids = selectedMacroIds
                    scope.launch {
                        ids.forEach { id ->
                            keyMap.macros.find { it.id == id }?.let { m ->
                                Config.updateMacro(ctx, m.copy(isEnabled = false))
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedMacroIds = emptySet()
                },
                onDuplicate = {
                    val idsToDup = selectedMacroIds
                    scope.launch {
                        idsToDup.forEach { id ->
                            keyMap.macros.find { it.id == id }?.let { m ->
                                val newMacro = m.copy(id = UUID.randomUUID().toString(), title = "${m.title}${ctx.getString(R.string.copy_suffix)}", createdAt = System.currentTimeMillis())
                                Config.updateMacro(ctx, newMacro)
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedMacroIds = emptySet()
                },
                onDelete = {
                    showBatchDeleteConfirm = true
                },
                modifier = panelModifier
            )
        }

        if (showBatchDeleteConfirm) {
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_selected_q, selectedMacroIds.size),
                onConfirm = { dontAskAgain ->
                    if (dontAskAgain) {
                        scope.launch { Config.setConfirmDeletion(ctx, false) }
                    }
                    val idsToDelete = selectedMacroIds
                    scope.launch {
                        idsToDelete.forEach { id -> Config.deleteMacro(ctx, id) }
                    }
                    showBatchDeleteConfirm = false
                    updateSelectionMode(false)
                    selectedMacroIds = emptySet()
                },
                onDismiss = { showBatchDeleteConfirm = false }
            )
        }

        if (showViewAndSortDialog) {
            ViewAndSortDialog(
                viewMode = keyMap.macrosViewMode,
                onViewModeChange = { scope.launch { Config.setMacrosViewMode(ctx, it) } },
                sortOption = keyMap.macrosSortOption,
                onSortOptionChange = { scope.launch { Config.setMacrosSortOption(ctx, it) } },
                sortAscending = keyMap.macrosSortAscending,
                onSortAscendingToggle = { scope.launch { Config.setMacrosSortAscending(ctx, !keyMap.macrosSortAscending) } },
                showRankingOption = false,
                onDismiss = { showViewAndSortDialog = false }
            )
        }
    }
}

@Composable
fun MacroSelectionBottomPanel(
    selectedCount: Int,
    isLandscape: Boolean,
    rotation: Int,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var panelWidth by remember { mutableStateOf(0f) }
    val rotatedModifier = when {
        isLandscape && rotation == ViewSurface.ROTATION_90 ->
            modifier
                .onGloballyPositioned { panelWidth = it.size.width.toFloat() }
                .graphicsLayer {
                    rotationZ = -90f
                    val currentCenterOffset = panelWidth / 2f
                    val targetCenterOffset = 83.dp.toPx()
                    translationX = (currentCenterOffset - targetCenterOffset)
                }
        isLandscape && rotation == ViewSurface.ROTATION_270 ->
            modifier
                .onGloballyPositioned { panelWidth = it.size.width.toFloat() }
                .graphicsLayer {
                    rotationZ = 90f
                    val currentCenterOffset = panelWidth / 2f
                    val targetCenterOffset = 83.dp.toPx()
                    translationX = -(currentCenterOffset - targetCenterOffset)
                }
        else -> modifier.padding(bottom = 48.dp)
    }

    Surface(
        color = Color(0xFF2B2B2B),
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(1.dp, Color.Black),
        modifier = rotatedModifier
            .width(300.dp)
            .height(68.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SelectionActionButton(
                icon = Icons.Default.CheckCircle,
                label = stringResource(R.string.batch_enable),
                enabled = selectedCount > 0,
                onClick = onEnable
            )
            SelectionActionButton(
                icon = Icons.Default.Block,
                label = stringResource(R.string.batch_disable),
                enabled = selectedCount > 0,
                onClick = onDisable
            )
            SelectionActionButton(
                icon = Icons.Default.ContentCopy,
                label = stringResource(R.string.batch_duplicate),
                enabled = selectedCount > 0,
                onClick = onDuplicate
            )
            SelectionActionButton(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.batch_delete),
                enabled = selectedCount > 0,
                onClick = onDelete
            )
        }
    }
}
