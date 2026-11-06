package com.HojuNK.essent_all.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
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
import androidx.compose.material.icons.automirrored.filled.Input
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.view.Surface as ViewSurface
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import com.HojuNK.essent_all.ui.editor.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

sealed interface EventDisplayItem {
    data class GroupItem(val group: EventGroup) : EventDisplayItem
    data class RuleItem(val rule: EventRule) : EventDisplayItem
}

enum class EventFilter(val labelRes: Int) {
    ALL(R.string.filter_all),
    
    // Key tags
    KEY_ESSENTIAL(R.string.section_essential_key),
    KEY_POWER(R.string.section_power_button),
    KEY_VOL_UP(R.string.section_volume_up),
    KEY_VOL_DOWN(R.string.section_volume_down),
    
    // Press type tags
    PRESS_SINGLE(R.string.press_single),
    PRESS_HOLD(R.string.press_hold),
    PRESS_DOUBLE(R.string.press_double),
    PRESS_TRIPLE(R.string.press_triple),
    
    // Group and badge tags
    IN_GROUP(R.string.filter_in_group),
    OUT_GROUP(R.string.filter_out_group),
    WITH_BADGE(R.string.filter_with_badge)
}

fun TapSlot.getButtonFilter(): EventFilter = when (this) {
    TapSlot.SINGLE, TapSlot.LONG, TapSlot.DOUBLE, TapSlot.TRIPLE -> EventFilter.KEY_ESSENTIAL
    TapSlot.POWER_LONG -> EventFilter.KEY_POWER
    TapSlot.VOL_UP_SINGLE, TapSlot.VOL_UP_LONG, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE -> EventFilter.KEY_VOL_UP
    TapSlot.VOL_DOWN_SINGLE, TapSlot.VOL_DOWN_LONG, TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE -> EventFilter.KEY_VOL_DOWN
    TapSlot.SHORTCUT -> EventFilter.KEY_ESSENTIAL
}

fun TapSlot.getPressTypeFilter(): EventFilter = when (this) {
    TapSlot.SINGLE, TapSlot.VOL_UP_SINGLE, TapSlot.VOL_DOWN_SINGLE -> EventFilter.PRESS_SINGLE
    TapSlot.LONG, TapSlot.VOL_UP_LONG, TapSlot.VOL_DOWN_LONG, TapSlot.POWER_LONG -> EventFilter.PRESS_HOLD
    TapSlot.DOUBLE, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_DOWN_DOUBLE -> EventFilter.PRESS_DOUBLE
    TapSlot.TRIPLE, TapSlot.VOL_UP_TRIPLE, TapSlot.VOL_DOWN_TRIPLE -> EventFilter.PRESS_TRIPLE
    TapSlot.SHORTCUT -> EventFilter.PRESS_SINGLE
}

fun ruleMatchesFilters(rule: EventRule, selectedFilters: Set<EventFilter>): Boolean {
    if (selectedFilters.isEmpty()) return true

    val buttonFilters = selectedFilters.filter { it in listOf(
        EventFilter.KEY_ESSENTIAL,
        EventFilter.KEY_POWER,
        EventFilter.KEY_VOL_UP,
        EventFilter.KEY_VOL_DOWN
    ) }

    val pressTypeFilters = selectedFilters.filter { it in listOf(
        EventFilter.PRESS_SINGLE,
        EventFilter.PRESS_HOLD,
        EventFilter.PRESS_DOUBLE,
        EventFilter.PRESS_TRIPLE
    ) }

    val groupFilters = selectedFilters.filter { it in listOf(EventFilter.IN_GROUP, EventFilter.OUT_GROUP) }
    val badgeFilters = selectedFilters.filter { it == EventFilter.WITH_BADGE }

    val ruleButton = rule.triggerGesture.getButtonFilter()
    val rulePressType = rule.triggerGesture.getPressTypeFilter()

    val buttonMatch = buttonFilters.isEmpty() || buttonFilters.contains(ruleButton)
    val pressTypeMatch = pressTypeFilters.isEmpty() || pressTypeFilters.contains(rulePressType)

    val groupMatch = groupFilters.isEmpty() || groupFilters.any { filter ->
        when (filter) {
            EventFilter.IN_GROUP -> rule.groupId != null
            EventFilter.OUT_GROUP -> rule.groupId == null
            else -> true
        }
    }

    val badgeMatch = badgeFilters.isEmpty() || rule.actions.any { it.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX) }

    return buttonMatch && pressTypeMatch && groupMatch && badgeMatch
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    keyMap: KeyMap, 
    apps: List<AppInfo>?, 
    onOpenSettings: () -> Unit,
    onEditEvent: (EventRule) -> Unit,
    onOpenPriority: () -> Unit = {},
    isSelectionMode: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenedGroupChanged: (EventGroup?) -> Unit = {},
    onSelectedCountChanged: (Int) -> Unit = {},
    onSelectAllActionChanged: (() -> Unit) -> Unit = {},
    onGroupEditActionChanged: (() -> Unit) -> Unit = {},
    onGroupDeleteActionChanged: (() -> Unit) -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var openedGroup by remember { mutableStateOf<EventGroup?>(null) }
    var targetRuleForGroup by remember { mutableStateOf<EventRule?>(null) }
    var showSelectGroupDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<EventGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<EventGroup?>(null) }
    var showViewAndSortDialog by remember { mutableStateOf(false) }

    var selectedFilters by remember { mutableStateOf(setOf<EventFilter>()) }

    // Keep openedGroup up-to-date if title or content changes in keyMap
    val currentOpenedGroup = openedGroup?.let { og ->
        keyMap.eventGroups.find { it.id == og.id }
    }

    LaunchedEffect(currentOpenedGroup) {
        onOpenedGroupChanged(currentOpenedGroup)
        if (currentOpenedGroup != null) {
            selectedFilters = selectedFilters - EventFilter.IN_GROUP - EventFilter.OUT_GROUP
        }
    }

    // Multi-selection states synced with parent
    var localSelectionMode by remember { mutableStateOf(isSelectionMode) }
    LaunchedEffect(isSelectionMode) {
        localSelectionMode = isSelectionMode
    }
    val updateSelectionMode: (Boolean) -> Unit = { active ->
        localSelectionMode = active
        onSelectionModeChange(active)
    }

    var selectedEventIds by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(localSelectionMode) {
        if (!localSelectionMode) {
            selectedEventIds = emptySet()
        }
    }
    LaunchedEffect(selectedEventIds) {
        onSelectedCountChanged(selectedEventIds.size)
    }

    val configuration = LocalConfiguration.current
    val rotation = ctx.display?.rotation ?: ViewSurface.ROTATION_0
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val sortedEvents = remember(keyMap.events, keyMap.eventsSortOption, keyMap.eventsSortAscending) {
        val base = keyMap.events.map { if (it.createdAt <= 0L) it.copy(createdAt = System.currentTimeMillis()) else it }
        val ascending = keyMap.eventsSortAscending
        when (keyMap.eventsSortOption) {
            ListSortOption.CREATION_DATE -> if (ascending) base.sortedBy { it.createdAt } else base.sortedByDescending { it.createdAt }
            ListSortOption.ALPHABETICAL -> if (ascending) base.sortedBy { it.title.lowercase() } else base.sortedByDescending { it.title.lowercase() }
            ListSortOption.RANKING -> if (ascending) base else base.reversed()
        }
    }

    val groupEvents = remember(sortedEvents, currentOpenedGroup, selectedFilters) {
        val base = if (currentOpenedGroup != null) sortedEvents.filter { it.groupId == currentOpenedGroup.id } else emptyList()
        base.filter { rule -> ruleMatchesFilters(rule, selectedFilters) }
    }

    val displayItems = remember(keyMap.eventGroups, sortedEvents, selectedFilters, keyMap.eventsGroupOption) {
        if (keyMap.eventsGroupOption == ListGroupOption.UNGROUPED) {
            sortedEvents
                .filter { ruleMatchesFilters(it, selectedFilters) }
                .map { EventDisplayItem.RuleItem(it) }
        } else {
            val groupFilters = selectedFilters.filter { it in listOf(EventFilter.IN_GROUP, EventFilter.OUT_GROUP) }
            val showGroups = groupFilters.isEmpty() || groupFilters.contains(EventFilter.IN_GROUP)

            val groups = if (!showGroups) emptyList() else keyMap.eventGroups
                .filter { group ->
                    if (selectedFilters.isEmpty()) true
                    else sortedEvents.any { rule ->
                        rule.groupId == group.id && ruleMatchesFilters(rule, selectedFilters)
                    }
                }
                .map { EventDisplayItem.GroupItem(it) }

            val rules = sortedEvents
                .filter { it.groupId == null }
                .filter { rule -> ruleMatchesFilters(rule, selectedFilters) }
                .map { EventDisplayItem.RuleItem(it) }

            groups + rules
        }
    }

    val activeEvents = remember(keyMap.events, currentOpenedGroup, displayItems, groupEvents) {
        if (currentOpenedGroup != null) groupEvents else displayItems.filterIsInstance<EventDisplayItem.RuleItem>().map { it.rule }
    }

    val handleSelectAll = {
        val isAllSelected = activeEvents.isNotEmpty() && activeEvents.all { selectedEventIds.contains(it.id) }
        selectedEventIds = if (isAllSelected) emptySet() else activeEvents.map { it.id }.toSet()
    }

    LaunchedEffect(activeEvents, selectedEventIds, localSelectionMode) {
        onSelectAllActionChanged(handleSelectAll)
    }

    LaunchedEffect(currentOpenedGroup) {
        onGroupEditActionChanged { currentOpenedGroup?.let { editingGroup = it } }
        onGroupDeleteActionChanged { currentOpenedGroup?.let { groupToDelete = it } }
    }

    var isBatchGroupAssign by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // Drag-to-select range state
    val itemPositions = remember { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var initialSelectionBeforeDrag by remember { mutableStateOf(setOf<String>()) }
    var dragTargetState by remember { mutableStateOf<Boolean?>(null) }

    // Handle system back gesture when in selection mode or inside a group
    BackHandler(enabled = localSelectionMode) {
        updateSelectionMode(false)
        selectedEventIds = emptySet()
    }

    BackHandler(enabled = openedGroup != null && !localSelectionMode) {
        openedGroup = null
    }

    val handleFilterToggle: (EventFilter) -> Unit = { filter ->
        selectedFilters = if (selectedFilters.contains(filter)) {
            selectedFilters - filter
        } else {
            selectedFilters + filter
        }
    }

    val handleResetToAll: () -> Unit = {
        selectedFilters = emptySet()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(localSelectionMode, activeEvents) {
                if (localSelectionMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        val startIndex = activeEvents.indices.find { i ->
                            itemPositions[i]?.contains(startPos) == true
                        }
                        if (startIndex != null) {
                            dragStartIndex = startIndex
                            initialSelectionBeforeDrag = selectedEventIds
                            dragTargetState = !selectedEventIds.contains(activeEvents[startIndex].id)
                            selectedEventIds = if (dragTargetState == true) selectedEventIds + activeEvents[startIndex].id else selectedEventIds - activeEvents[startIndex].id

                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        val currPos = change.position
                                        val currentIndex = activeEvents.indices.find { i ->
                                            itemPositions[i]?.contains(currPos) == true
                                        }
                                        if (currentIndex != null && dragStartIndex != null && dragTargetState != null) {
                                            val range = minOf(dragStartIndex!!, currentIndex)..maxOf(dragStartIndex!!, currentIndex)
                                            val newSel = mutableSetOf<String>().apply { addAll(initialSelectionBeforeDrag) }
                                            activeEvents.forEachIndexed { i, r ->
                                                if (i in range) {
                                                    if (dragTargetState == true) newSel.add(r.id) else newSel.remove(r.id)
                                                } else {
                                                    if (initialSelectionBeforeDrag.contains(r.id)) newSel.add(r.id) else newSel.remove(r.id)
                                                }
                                            }
                                            selectedEventIds = newSel
                                        }
                                    }
                                    if (event.changes.all { !it.pressed }) break
                                }
                            } finally {
                                dragStartIndex = null
                                dragTargetState = null
                            }
                        }
                    }
                }
            }
    ) {
        Scaffold(
            topBar = {
                if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                    TopAppBar(
                        title = {
                            if (localSelectionMode) {
                                Text(
                                    stringResource(R.string.selected_count, selectedEventIds.size),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (currentOpenedGroup != null) {
                                Text(currentOpenedGroup.title, color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                TypewriterTitle(targetText = stringResource(R.string.events), animate = false)
                            }
                        },
                        navigationIcon = {
                            if (localSelectionMode) {
                                IconButton(onClick = { updateSelectionMode(false); selectedEventIds = emptySet() }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                }
                            } else if (currentOpenedGroup != null) {
                                IconButton(onClick = { openedGroup = null }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                                }
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
                        actions = {
                            if (localSelectionMode) {
                                val isAllSelected = activeEvents.isNotEmpty() && activeEvents.all { selectedEventIds.contains(it.id) }
                                IconButton(onClick = handleSelectAll) {
                                    Icon(if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll, null, tint = Color.White)
                                }
                            } else {
                                if (currentOpenedGroup != null) {
                                    IconButton(onClick = { editingGroup = currentOpenedGroup }) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White)
                                    }
                                } else {
                                    IconButton(onClick = onOpenPriority) {
                                        Icon(Icons.Default.LowPriority, null, tint = Color.White)
                                    }
                                }
                                IconButton(onClick = { updateSelectionMode(true) }) {
                                    Icon(Icons.Default.Checklist, null, tint = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .combinedClickable(
                                            onClick = {
                                                val nextMode = if (keyMap.eventsViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                                scope.launch { Config.setEventsViewMode(ctx, nextMode) }
                                            },
                                            onLongClick = { showViewAndSortDialog = true }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (keyMap.eventsViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                if (currentOpenedGroup == null) {
                                    IconButton(onClick = onOpenSettings) {
                                        Icon(Icons.Default.Settings, null)
                                    }
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButtonPosition = if (isLandscape && rotation == ViewSurface.ROTATION_270) FabPosition.Start else FabPosition.End,
            floatingActionButton = {
                if (!localSelectionMode) {
                    FloatingActionButton(
                        onClick = {
                            val activeGroupId = currentOpenedGroup?.id
                            onEditEvent(EventRule(UUID.randomUUID().toString(), "⚡", "", groupId = activeGroupId))
                        },
                        containerColor = Color.Gray,
                        contentColor = Color.White,
                        modifier = Modifier
                            .padding(
                                bottom = if (isLandscape) 24.dp else 120.dp,
                                start = if (isLandscape && rotation == ViewSurface.ROTATION_270) 135.dp else 0.dp,
                                end = if (isLandscape && rotation == ViewSurface.ROTATION_90) 135.dp else 0.dp
                            )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
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

            val isStripeMode = keyMap.eventsViewMode == EventsViewMode.STRIPES
            val numColumns = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                if (isStripeMode) 2 else 4
            } else {
                if (isStripeMode) 1 else 2
            }

            if (currentOpenedGroup != null) {
                // Inside a Group Screen
                val groupEvents = groupEvents
                if (groupEvents.isEmpty() && keyMap.events.filter { it.groupId == currentOpenedGroup.id }.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(top = topPadding, start = startPadding, end = endPadding), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.group_empty_msg), color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp + startPadding, 
                            end = 16.dp + endPadding, 
                            top = topPadding + 16.dp, 
                            bottom = inner.calculateBottomPadding() + 16.dp + (if (isLandscape) 0.dp else 120.dp)
                        )
                    ) {
                        item {
                            EventFilterBar(
                                currentOpenedGroup = currentOpenedGroup,
                                selectedFilters = selectedFilters,
                                onFilterToggle = handleFilterToggle,
                                onResetToAll = handleResetToAll
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (colIndex in 0 until numColumns) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        groupEvents.forEachIndexed { index, rule ->
                                            if (index % numColumns == colIndex) {
                                                key(rule.id) {
                                                    val isSelected = selectedEventIds.contains(rule.id)
                                                    val onSelectToggle = {
                                                        selectedEventIds = if (isSelected) selectedEventIds - rule.id else selectedEventIds + rule.id
                                                    }
                                                    Box(
                                                        modifier = Modifier.onGloballyPositioned { coords ->
                                                            itemPositions[index] = androidx.compose.ui.geometry.Rect(coords.positionInRoot(), coords.size.toSize())
                                                        }
                                                    ) {
                                                        if (isStripeMode) {
                                                            StripeEventItem(
                                                                keyMap = keyMap,
                                                                rule = rule,
                                                                isSelectionMode = localSelectionMode,
                                                                isSelected = isSelected,
                                                                onSelectToggle = onSelectToggle,
                                                                onToggle = { enabled ->
                                                                    scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(isEnabled = enabled) } }
                                                                },
                                                                onEdit = { onEditEvent(rule) },
                                                                onAddToGroup = {
                                                                    targetRuleForGroup = rule
                                                                    if (keyMap.eventGroups.isEmpty()) showCreateGroupDialog = true else showSelectGroupDialog = true
                                                                },
                                                                onRemoveFromGroup = {
                                                                    scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(groupId = null) } }
                                                                },
                                                                onDuplicate = {
                                                                    scope.launch { 
                                                                        val newRule = rule.copy(id = UUID.randomUUID().toString(), title = "${rule.title}${ctx.getString(R.string.copy_suffix)}")
                                                                        Config.updateEvent(ctx, newRule)
                                                                    }
                                                                },
                                                                onDelete = {
                                                                    scope.launch { Config.deleteEvent(ctx, rule.id) }
                                                                }
                                                            )
                                                        } else {
                                                            RectangleEventItem(
                                                                keyMap = keyMap,
                                                                rule = rule,
                                                                isSelectionMode = localSelectionMode,
                                                                isSelected = isSelected,
                                                                onSelectToggle = onSelectToggle,
                                                                onToggle = { enabled ->
                                                                    scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(isEnabled = enabled) } }
                                                                },
                                                                onEdit = { onEditEvent(rule) },
                                                                onAddToGroup = {
                                                                    targetRuleForGroup = rule
                                                                    if (keyMap.eventGroups.isEmpty()) showCreateGroupDialog = true else showSelectGroupDialog = true
                                                                },
                                                                onRemoveFromGroup = {
                                                                    scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(groupId = null) } }
                                                                },
                                                                onDuplicate = {
                                                                    scope.launch { 
                                                                        val newRule = rule.copy(id = UUID.randomUUID().toString(), title = "${rule.title}${ctx.getString(R.string.copy_suffix)}")
                                                                        Config.updateEvent(ctx, newRule)
                                                                    }
                                                                },
                                                                onDelete = {
                                                                    scope.launch { Config.deleteEvent(ctx, rule.id) }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Top-Level Main Events Screen
                val displayItems = displayItems

                if (displayItems.isEmpty() && keyMap.events.isEmpty() && keyMap.eventGroups.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(top = topPadding, start = startPadding, end = endPadding), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.events_empty_msg), color = Color.Gray, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp + startPadding, 
                            end = 16.dp + endPadding, 
                            top = topPadding + 16.dp, 
                            bottom = inner.calculateBottomPadding() + 16.dp + (if (isLandscape) 0.dp else 120.dp)
                        )
                    ) {
                        item {
                            EventFilterBar(
                                currentOpenedGroup = currentOpenedGroup,
                                selectedFilters = selectedFilters,
                                onFilterToggle = handleFilterToggle,
                                onResetToAll = handleResetToAll
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (colIndex in 0 until numColumns) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        displayItems.forEachIndexed { index, displayItem ->
                                            if (index % numColumns == colIndex) {
                                                when (displayItem) {
                                                    is EventDisplayItem.GroupItem -> {
                                                        val group = displayItem.group
                                                        val eventsCount = keyMap.events.count { it.groupId == group.id }
                                                        key("group_${group.id}") {
                                                            if (isStripeMode) {
                                                                StripeGroupItem(
                                                                    group = group,
                                                                    eventsCount = eventsCount,
                                                                    onClick = { if (!localSelectionMode) openedGroup = group },
                                                                    onEditGroup = { editingGroup = group },
                                                                    onDeleteGroup = { groupToDelete = group }
                                                                )
                                                            } else {
                                                                RectangleGroupItem(
                                                                    group = group,
                                                                    eventsCount = eventsCount,
                                                                    onClick = { if (!localSelectionMode) openedGroup = group },
                                                                    onEditGroup = { editingGroup = group },
                                                                    onDeleteGroup = { groupToDelete = group }
                                                                )
                                                            }
                                                        }
                                                    }
                                                    is EventDisplayItem.RuleItem -> {
                                                        val rule = displayItem.rule
                                                        val isSelected = selectedEventIds.contains(rule.id)
                                                        val onSelectToggle = {
                                                            selectedEventIds = if (isSelected) selectedEventIds - rule.id else selectedEventIds + rule.id
                                                        }
                                                        key("rule_${rule.id}") {
                                                            Box(
                                                                modifier = Modifier.onGloballyPositioned { coords ->
                                                                    itemPositions[index] = androidx.compose.ui.geometry.Rect(coords.positionInRoot(), coords.size.toSize())
                                                                }
                                                            ) {
                                                                if (isStripeMode) {
                                                                    StripeEventItem(
                                                                        keyMap = keyMap,
                                                                        rule = rule,
                                                                        isSelectionMode = localSelectionMode,
                                                                        isSelected = isSelected,
                                                                        onSelectToggle = onSelectToggle,
                                                                        onToggle = { enabled ->
                                                                            scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(isEnabled = enabled) } }
                                                                        },
                                                                        onEdit = { onEditEvent(rule) },
                                                                        onAddToGroup = {
                                                                            targetRuleForGroup = rule
                                                                            if (keyMap.eventGroups.isEmpty()) showCreateGroupDialog = true else showSelectGroupDialog = true
                                                                        },
                                                                        onRemoveFromGroup = {
                                                                            scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(groupId = null) } }
                                                                        },
                                                                        onDuplicate = {
                                                                            scope.launch { 
                                                                                val newRule = rule.copy(id = UUID.randomUUID().toString(), title = "${rule.title}${ctx.getString(R.string.copy_suffix)}")
                                                                                Config.updateEvent(ctx, newRule)
                                                                            }
                                                                        },
                                                                        onDelete = {
                                                                            scope.launch { Config.deleteEvent(ctx, rule.id) }
                                                                        }
                                                                    )
                                                                } else {
                                                                    RectangleEventItem(
                                                                        keyMap = keyMap,
                                                                        rule = rule,
                                                                        isSelectionMode = localSelectionMode,
                                                                        isSelected = isSelected,
                                                                        onSelectToggle = onSelectToggle,
                                                                        onToggle = { enabled ->
                                                                            scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(isEnabled = enabled) } }
                                                                        },
                                                                        onEdit = { onEditEvent(rule) },
                                                                        onAddToGroup = {
                                                                            targetRuleForGroup = rule
                                                                            if (keyMap.eventGroups.isEmpty()) showCreateGroupDialog = true else showSelectGroupDialog = true
                                                                        },
                                                                        onRemoveFromGroup = {
                                                                            scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(groupId = null) } }
                                                                        },
                                                                        onDuplicate = {
                                                                            scope.launch { 
                                                                                val newRule = rule.copy(id = UUID.randomUUID().toString(), title = "${rule.title}${ctx.getString(R.string.copy_suffix)}")
                                                                                Config.updateEvent(ctx, newRule)
                                                                            }
                                                                        },
                                                                        onDelete = {
                                                                            scope.launch { Config.deleteEvent(ctx, rule.id) }
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Selection Panel for Batch Operations at hotbar level with rotation support
        if (localSelectionMode) {
            val allInGroup = selectedEventIds.isNotEmpty() && selectedEventIds.all { id ->
                keyMap.events.find { it.id == id }?.groupId != null
            }
            val groupActionLabel = if (allInGroup) stringResource(R.string.batch_remove_from_group) else stringResource(R.string.batch_add_to_group)
            val groupActionIcon = if (allInGroup) Icons.Default.Output else Icons.AutoMirrored.Filled.Input
            val onGroupAction = {
                if (allInGroup) {
                    val ids = selectedEventIds
                    scope.launch {
                        ids.forEach { id -> Config.updateEventById(ctx, id) { it.copy(groupId = null) } }
                    }
                    updateSelectionMode(false)
                    selectedEventIds = emptySet()
                } else {
                    isBatchGroupAssign = true
                    if (keyMap.eventGroups.isEmpty()) showCreateGroupDialog = true else showSelectGroupDialog = true
                }
            }

            val panelModifier = when {
                isLandscape && rotation == ViewSurface.ROTATION_90 -> Modifier.align(Alignment.CenterEnd)
                isLandscape && rotation == ViewSurface.ROTATION_270 -> Modifier.align(Alignment.CenterStart)
                else -> Modifier.align(Alignment.BottomCenter)
            }

            SelectionBottomPanel(
                selectedCount = selectedEventIds.size,
                isLandscape = isLandscape,
                rotation = rotation,
                onEnable = {
                    val ids = selectedEventIds
                    scope.launch {
                        ids.forEach { id -> Config.updateEventById(ctx, id) { it.copy(isEnabled = true) } }
                    }
                    updateSelectionMode(false)
                    selectedEventIds = emptySet()
                },
                onDisable = {
                    val ids = selectedEventIds
                    scope.launch {
                        ids.forEach { id -> Config.updateEventById(ctx, id) { it.copy(isEnabled = false) } }
                    }
                    updateSelectionMode(false)
                    selectedEventIds = emptySet()
                },
                onDuplicate = {
                    val idsToDup = selectedEventIds
                    scope.launch {
                        idsToDup.forEach { id ->
                            keyMap.events.find { it.id == id }?.let { rule ->
                                val newRule = rule.copy(
                                    id = UUID.randomUUID().toString(),
                                    title = "${rule.title}${ctx.getString(R.string.copy_suffix)}"
                                )
                                Config.updateEvent(ctx, newRule)
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedEventIds = emptySet()
                },
                onGroupAction = onGroupAction,
                groupActionLabel = groupActionLabel,
                groupActionIcon = groupActionIcon,
                onDelete = {
                    showBatchDeleteConfirm = true
                },
                modifier = panelModifier
            )
        }

        if (showSelectGroupDialog && (targetRuleForGroup != null || isBatchGroupAssign)) {
            GroupSelectionDialog(
                groups = keyMap.eventGroups,
                onSelectGroup = { selectedGroup ->
                    if (isBatchGroupAssign) {
                        val idsToAssign = selectedEventIds
                        scope.launch {
                            idsToAssign.forEach { id ->
                                Config.updateEventById(ctx, id) { it.copy(groupId = selectedGroup.id) }
                            }
                        }
                        isBatchGroupAssign = false
                        updateSelectionMode(false)
                        selectedEventIds = emptySet()
                    } else {
                        targetRuleForGroup?.let { rule ->
                            scope.launch { Config.updateEventById(ctx, rule.id) { it.copy(groupId = selectedGroup.id) } }
                        }
                        targetRuleForGroup = null
                    }
                    showSelectGroupDialog = false
                },
                onCreateNewGroup = {
                    showCreateGroupDialog = true
                },
                onDismiss = {
                    targetRuleForGroup = null
                    isBatchGroupAssign = false
                    showSelectGroupDialog = false
                }
            )
        }

        if (showCreateGroupDialog || editingGroup != null) {
            GroupCreationDialog(
                initialName = editingGroup?.title ?: "",
                initialIcon = editingGroup?.icon ?: "material:Folder",
                isEditing = editingGroup != null,
                apps = apps,
                onConfirm = { name, icon ->
                    if (editingGroup != null) {
                        val updated = editingGroup!!.copy(title = name, icon = icon)
                        scope.launch { Config.updateEventGroup(ctx, updated) }
                        if (openedGroup?.id == editingGroup?.id) {
                            openedGroup = updated
                        }
                        editingGroup = null
                    } else {
                        val newGroup = EventGroup(id = UUID.randomUUID().toString(), title = name, icon = icon)
                        val targetRule = targetRuleForGroup
                        val fromSelection = showSelectGroupDialog

                        if (isBatchGroupAssign) {
                            val idsToAssign = selectedEventIds
                            scope.launch {
                                Config.updateEventGroup(ctx, newGroup)
                                idsToAssign.forEach { id ->
                                    Config.updateEventById(ctx, id) { it.copy(groupId = newGroup.id) }
                                }
                            }
                            showCreateGroupDialog = false
                            isBatchGroupAssign = false
                            updateSelectionMode(false)
                            selectedEventIds = emptySet()
                            showSelectGroupDialog = false
                        } else if (fromSelection) {
                            scope.launch { Config.updateEventGroup(ctx, newGroup) }
                            showCreateGroupDialog = false
                        } else {
                            scope.launch {
                                Config.updateEventGroup(ctx, newGroup)
                                if (targetRule != null) {
                                    Config.updateEventById(ctx, targetRule.id) { it.copy(groupId = newGroup.id) }
                                }
                            }
                            showCreateGroupDialog = false
                            targetRuleForGroup = null
                            showSelectGroupDialog = false
                        }
                    }
                },
                onDismiss = {
                    showCreateGroupDialog = false
                    editingGroup = null
                }
            )
        }

        if (groupToDelete != null) {
            val deletingGroup = groupToDelete!!
            ConfirmDeleteDialog(
                title = deletingGroup.title,
                onConfirm = { dontAskAgain ->
                    scope.launch { Config.deleteEventGroup(ctx, deletingGroup.id) }
                    if (openedGroup?.id == deletingGroup.id) {
                        openedGroup = null
                    }
                    groupToDelete = null
                },
                onDismiss = { groupToDelete = null }
            )
        }

        if (showBatchDeleteConfirm) {
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_selected_q, selectedEventIds.size),
                onConfirm = { dontAskAgain ->
                    if (dontAskAgain) {
                        scope.launch { Config.setConfirmDeletion(ctx, false) }
                    }
                    val idsToDelete = selectedEventIds
                    scope.launch {
                        idsToDelete.forEach { id -> Config.deleteEvent(ctx, id) }
                    }
                    showBatchDeleteConfirm = false
                    updateSelectionMode(false)
                    selectedEventIds = emptySet()
                },
                onDismiss = { showBatchDeleteConfirm = false }
            )
        }

        if (showViewAndSortDialog) {
            ViewAndSortDialog(
                viewMode = keyMap.eventsViewMode,
                onViewModeChange = { scope.launch { Config.setEventsViewMode(ctx, it) } },
                sortOption = keyMap.eventsSortOption,
                onSortOptionChange = { scope.launch { Config.setEventsSortOption(ctx, it) } },
                sortAscending = keyMap.eventsSortAscending,
                onSortAscendingToggle = { scope.launch { Config.setEventsSortAscending(ctx, !keyMap.eventsSortAscending) } },
                groupOption = keyMap.eventsGroupOption,
                onGroupOptionChange = { scope.launch { Config.setEventsGroupOption(ctx, it) } },
                onDismiss = { showViewAndSortDialog = false }
            )
        }
    }
}

@Composable
fun EventFilterBar(
    currentOpenedGroup: EventGroup?,
    selectedFilters: Set<EventFilter>,
    onFilterToggle: (EventFilter) -> Unit,
    onResetToAll: () -> Unit
) {
    val buttonFilters = listOf(
        EventFilter.KEY_ESSENTIAL,
        EventFilter.KEY_POWER,
        EventFilter.KEY_VOL_UP,
        EventFilter.KEY_VOL_DOWN
    )
    val pressTypeFilters = listOf(
        EventFilter.PRESS_SINGLE,
        EventFilter.PRESS_HOLD,
        EventFilter.PRESS_DOUBLE,
        EventFilter.PRESS_TRIPLE
    )
    val filters = if (currentOpenedGroup != null) {
        listOf(EventFilter.ALL) + buttonFilters + pressTypeFilters + listOf(EventFilter.WITH_BADGE)
    } else {
        listOf(EventFilter.ALL) + buttonFilters + pressTypeFilters + listOf(EventFilter.IN_GROUP, EventFilter.OUT_GROUP, EventFilter.WITH_BADGE)
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters, key = { it.name }) { filter ->
            val isAll = filter == EventFilter.ALL
            val isSelected = if (isAll) selectedFilters.isEmpty() else selectedFilters.contains(filter)
            val showCross = isAll && selectedFilters.isNotEmpty()

            Surface(
                onClick = {
                    if (isAll) {
                        onResetToAll()
                    } else {
                        onFilterToggle(filter)
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

@Composable
fun SelectionBottomPanel(
    selectedCount: Int,
    isLandscape: Boolean,
    rotation: Int,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onDuplicate: () -> Unit,
    onGroupAction: () -> Unit,
    groupActionLabel: String,
    groupActionIcon: ImageVector,
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
            .width(344.dp)
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
                icon = groupActionIcon,
                label = groupActionLabel,
                enabled = selectedCount > 0,
                onClick = onGroupAction
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

@Composable
fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
fun StripeEventItem(
    keyMap: KeyMap,
    rule: EventRule,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onAddToGroup: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentRule by rememberUpdatedState(rule)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnAddToGroup by rememberUpdatedState(onAddToGroup)
    val currentOnRemoveFromGroup by rememberUpdatedState(onRemoveFromGroup)
    val currentOnDuplicate by rememberUpdatedState(onDuplicate)
    val currentOnDelete by rememberUpdatedState(onDelete)

    var isDeletingAnimating by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }

    val buildActions = {
        listOf(
            RadialAction(if (currentRule.isEnabled) Icons.Default.Block else Icons.Default.CheckCircle, "Toggle") { currentOnToggle(!currentRule.isEnabled) },
            RadialAction(Icons.Default.Edit, "Edit") { currentOnEdit() },
            RadialAction(
                if (currentRule.groupId == null) Icons.AutoMirrored.Filled.Input else Icons.Default.Output,
                if (currentRule.groupId == null) "Add to Group" else "Remove from Group"
            ) {
                if (currentRule.groupId == null) currentOnAddToGroup() else currentOnRemoveFromGroup()
            },
            RadialAction(Icons.Default.ContentCopy, "Duplicate") { currentOnDuplicate() },
            RadialAction(Icons.Default.Delete, "Delete") { 
                if (keyMap.confirmDeletion) showConfirmDelete = true 
                else isDeletingAnimating = true
            }
        )
    }

    val itemHeight = remember { Animatable(86f) }
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    val showRadialMenu = LocalRadialMenu.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(isDeletingAnimating) {
        if (isDeletingAnimating) {
            itemHeight.animateTo(0f, animationSpec = tween(500))
            currentOnDelete()
        }
    }

    if (itemHeight.value <= 0.1f && isDeletingAnimating) return

    val isActive = radialMenuState != null && radialMenuState.sourceId == rule.id

    val itemContent = @Composable {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (currentRule.isEnabled) Color(0xFF2B2B2B) else Color(0xFF101010),
            tonalElevation = if (currentRule.isEnabled) 2.dp else 0.dp,
            border = if (isSelected) BorderStroke(2.dp, Color.White) else if (!currentRule.isEnabled) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)) else null,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                EventIconDisplay(currentRule.icon, displaySize = 42.dp, grayscale = !currentRule.isEnabled)
                Spacer(Modifier.width(16.dp))
                Text(currentRule.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = if (currentRule.isEnabled) Color.White else Color.Gray)
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() })
                } else {
                    Switch(checked = currentRule.isEnabled, onCheckedChange = { currentOnToggle(it) })
                }
            }
        }
    }

    LaunchedEffect(currentRule) {
        if (isActive) {
            showRadialMenu(RadialMenuState(
                center = itemOffsetInRoot + Offset(itemSize.width / 2f, itemSize.height / 2f),
                actions = buildActions(),
                sourceId = rule.id,
                sourceBounds = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize()),
                sourceContent = itemContent,
                hoveredIndex = radialMenuState?.hoveredIndex ?: -1
            ))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight.value.dp)
            .graphicsLayer { alpha = if (isActive) 0f else 1.0f }
            .clip(RoundedCornerShape(24.dp))
            .onGloballyPositioned { 
                itemOffsetInRoot = it.positionInRoot()
                itemSize = it.size
            }
            .background(if (isDeletingAnimating) Color.Red else Color(0xFF1A1A1A))
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rule.id, isSelectionMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val menuCenter = down.position + itemOffsetInRoot

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        
                        val itemRect = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize())

                        val lpJob = scope.launch {
                            if (!isSelectionMode) {
                                delay(viewConfig.longPressTimeoutMillis)
                                menuTriggered = true
                                showRadialMenu(RadialMenuState(
                                    center = menuCenter, 
                                    actions = buildActions(), 
                                    sourceId = rule.id,
                                    sourceBounds = itemRect,
                                    sourceContent = itemContent
                                ))
                            }
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                val change = event.changes.find { it.id == down.id }

                                if (isSelectionMode) {
                                    if (isReleased) {
                                        onSelectToggle()
                                        break
                                    }
                                } else if (menuTriggered) {
                                    change?.consume()
                                    if (change != null) {
                                        val actions = buildActions()
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(
                                                center = menuCenter, 
                                                actions = actions, 
                                                hoveredIndex = bestIndex, 
                                                sourceId = rule.id,
                                                sourceBounds = itemRect,
                                                sourceContent = itemContent
                                            ))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            val finalActions = buildActions()
                                            showRadialMenu(null)
                                            dismissOnExit = false
                                            finalActions[currentHoveredIndex].onClick()
                                            break
                                        }

                                        val releasePos = change?.position
                                        if (releasePos != null) {
                                            val distance = (releasePos + itemOffsetInRoot - menuCenter).getDistance()
                                            val centerThreshold = with(density) { 70.dp.toPx() }
                                            if (distance < centerThreshold) {
                                                dismissOnExit = false
                                                break
                                            }
                                        }
                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        currentOnToggle(!currentRule.isEnabled)
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                }
        ) {
            itemContent()
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = currentRule.title,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    scope.launch { Config.setConfirmDeletion(ctx, false) }
                }
                isDeletingAnimating = true
                showConfirmDelete = false 
            },
            onDismiss = { showConfirmDelete = false }
        )
    }
}

@Composable
fun RectangleEventItem(
    keyMap: KeyMap,
    rule: EventRule,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onAddToGroup: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    val showRadialMenu = LocalRadialMenu.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentRule by rememberUpdatedState(rule)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnAddToGroup by rememberUpdatedState(onAddToGroup)
    val currentOnRemoveFromGroup by rememberUpdatedState(onRemoveFromGroup)
    val currentOnDuplicate by rememberUpdatedState(onDuplicate)
    val currentOnDelete by rememberUpdatedState(onDelete)

    var showConfirmDelete by remember { mutableStateOf(false) }

    val buildActions = {
        listOf(
            RadialAction(if (currentRule.isEnabled) Icons.Default.Block else Icons.Default.CheckCircle, "Toggle") { currentOnToggle(!currentRule.isEnabled) },
            RadialAction(Icons.Default.Edit, "Edit") { currentOnEdit() },
            RadialAction(
                if (currentRule.groupId == null) Icons.AutoMirrored.Filled.Input else Icons.Default.Output,
                if (currentRule.groupId == null) "Add to Group" else "Remove from Group"
            ) {
                if (currentRule.groupId == null) currentOnAddToGroup() else currentOnRemoveFromGroup()
            },
            RadialAction(Icons.Default.ContentCopy, "Duplicate") { currentOnDuplicate() },
            RadialAction(Icons.Default.Delete, "Delete") { 
                if (keyMap.confirmDeletion) showConfirmDelete = true 
                else currentOnDelete()
            }
        )
    }

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val isActive = radialMenuState != null && radialMenuState.sourceId == rule.id

    val itemContent = @Composable {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (currentRule.isEnabled) Color(0xFF2B2B2B) else Color(0xFF101010),
            tonalElevation = if (currentRule.isEnabled) 2.dp else 0.dp,
            border = if (isSelected) BorderStroke(2.dp, Color.White) else if (!currentRule.isEnabled) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)) else null,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectToggle() },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    EventIconDisplay(
                        currentRule.icon,
                        displaySize = 64.dp,
                        modifier = Modifier.fillMaxWidth(0.67f).aspectRatio(1f),
                        grayscale = !currentRule.isEnabled
                    )
                    Text(
                        currentRule.title,
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp),
                        color = if (currentRule.isEnabled) Color.White else Color.Gray,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    if (!currentRule.isEnabled && !isSelectionMode) {
                        Text(stringResource(R.string.state_off_label), style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    LaunchedEffect(currentRule) {
        if (isActive) {
            showRadialMenu(RadialMenuState(
                center = itemOffsetInRoot + Offset(itemSize.width / 2f, itemSize.height / 2f),
                actions = buildActions(),
                sourceId = rule.id,
                sourceBounds = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize()),
                sourceContent = itemContent,
                hoveredIndex = radialMenuState?.hoveredIndex ?: -1
            ))
        }
    }

    Box(modifier = Modifier.graphicsLayer { alpha = if (isActive) 0f else 1f }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { 
                    itemOffsetInRoot = it.positionInRoot() 
                    itemSize = it.size
                }
                .pointerInput(rule.id, isSelectionMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val menuCenter = down.position + itemOffsetInRoot

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        
                        val itemRect = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize())

                        val lpJob = scope.launch {
                            if (!isSelectionMode) {
                                delay(viewConfig.longPressTimeoutMillis)
                                menuTriggered = true
                                showRadialMenu(RadialMenuState(
                                    center = menuCenter, 
                                    actions = buildActions(), 
                                    sourceId = rule.id,
                                    sourceBounds = itemRect,
                                    sourceContent = itemContent
                                ))
                            }
                        }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                                val change = event.changes.find { it.id == down.id }

                                if (isSelectionMode) {
                                    if (isReleased) {
                                        onSelectToggle()
                                        break
                                    }
                                } else if (menuTriggered) {
                                    change?.consume()
                                    if (change != null) {
                                        val currentActions = buildActions()
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            currentActions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(
                                                center = menuCenter, 
                                                actions = currentActions,
                                                hoveredIndex = bestIndex, 
                                                sourceId = rule.id,
                                                sourceBounds = itemRect,
                                                sourceContent = itemContent
                                            ))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            val finalActions = buildActions()
                                            showRadialMenu(null)
                                            dismissOnExit = false
                                            finalActions[currentHoveredIndex].onClick()
                                            break
                                        }

                                        val releasePos = change?.position
                                        if (releasePos != null) {
                                            val distance = (releasePos + itemOffsetInRoot - menuCenter).getDistance()
                                            val centerThreshold = with(density) { 70.dp.toPx() }
                                            if (distance < centerThreshold) {
                                                dismissOnExit = false
                                                break
                                            }
                                        }
                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        currentOnToggle(!currentRule.isEnabled)
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            itemContent()
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = currentRule.title,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    scope.launch { Config.setConfirmDeletion(ctx, false) }
                }
                currentOnDelete()
                showConfirmDelete = false 
            },
            onDismiss = { showConfirmDelete = false }
        )
    }
}

@Composable
fun StripeGroupItem(
    group: EventGroup,
    eventsCount: Int,
    onClick: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val showRadialMenu = LocalRadialMenu.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val buildActions = {
        listOf(
            RadialAction(Icons.Default.Edit, "Edit Group") { onEditGroup() },
            RadialAction(Icons.Default.Delete, "Delete Group") { onDeleteGroup() }
        )
    }

    val isActive = radialMenuState != null && radialMenuState.sourceId == group.id

    val itemContent = @Composable {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventIconDisplay(group.icon, displaySize = 42.dp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }
    }

    LaunchedEffect(group) {
        if (isActive) {
            showRadialMenu(RadialMenuState(
                center = itemOffsetInRoot + Offset(itemSize.width / 2f, itemSize.height / 2f),
                actions = buildActions(),
                sourceId = group.id,
                sourceBounds = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize()),
                sourceContent = itemContent,
                hoveredIndex = radialMenuState?.hoveredIndex ?: -1
            ))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .graphicsLayer { alpha = if (isActive) 0f else 1.0f }
            .clip(RoundedCornerShape(24.dp))
            .onGloballyPositioned {
                itemOffsetInRoot = it.positionInRoot()
                itemSize = it.size
            }
            .background(Color(0xFF1A1A1A))
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(group.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val menuCenter = down.position + itemOffsetInRoot

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        val itemRect = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize())

                        val lpJob = scope.launch {
                            delay(viewConfig.longPressTimeoutMillis)
                            menuTriggered = true
                            showRadialMenu(RadialMenuState(
                                center = menuCenter,
                                actions = buildActions(),
                                sourceId = group.id,
                                sourceBounds = itemRect,
                                sourceContent = itemContent
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
                                        val actions = buildActions()
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(
                                                center = menuCenter,
                                                actions = actions,
                                                hoveredIndex = bestIndex,
                                                sourceId = group.id,
                                                sourceBounds = itemRect,
                                                sourceContent = itemContent
                                            ))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            val finalActions = buildActions()
                                            showRadialMenu(null)
                                            dismissOnExit = false
                                            finalActions[currentHoveredIndex].onClick()
                                            break
                                        }
                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        onClick()
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                }
        ) {
            itemContent()
        }
    }
}

@Composable
fun RectangleGroupItem(
    group: EventGroup,
    eventsCount: Int,
    onClick: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val showRadialMenu = LocalRadialMenu.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val scope = rememberCoroutineScope()

    val buildActions = {
        listOf(
            RadialAction(Icons.Default.Edit, "Edit Group") { onEditGroup() },
            RadialAction(Icons.Default.Delete, "Delete Group") { onDeleteGroup() }
        )
    }

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val isActive = radialMenuState != null && radialMenuState.sourceId == group.id

    val itemContent = @Composable {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EventIconDisplay(
                    group.icon,
                    displaySize = 64.dp,
                    modifier = Modifier.fillMaxWidth(0.67f).aspectRatio(1f)
                )
                Text(
                    group.title,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    LaunchedEffect(group) {
        if (isActive) {
            showRadialMenu(RadialMenuState(
                center = itemOffsetInRoot + Offset(itemSize.width / 2f, itemSize.height / 2f),
                actions = buildActions(),
                sourceId = group.id,
                sourceBounds = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize()),
                sourceContent = itemContent,
                hoveredIndex = radialMenuState?.hoveredIndex ?: -1
            ))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isActive) 0f else 1.0f }
            .clip(RoundedCornerShape(24.dp))
            .onGloballyPositioned {
                itemOffsetInRoot = it.positionInRoot()
                itemSize = it.size
            }
            .background(Color(0xFF1A1A1A))
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(group.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val menuCenter = down.position + itemOffsetInRoot

                        var menuTriggered = false
                        var currentHoveredIndex = -1
                        var dismissOnExit = true
                        val itemRect = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize())

                        val lpJob = scope.launch {
                            delay(viewConfig.longPressTimeoutMillis)
                            menuTriggered = true
                            showRadialMenu(RadialMenuState(
                                center = menuCenter,
                                actions = buildActions(),
                                sourceId = group.id,
                                sourceBounds = itemRect,
                                sourceContent = itemContent
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
                                        val actions = buildActions()
                                        val bestIndex = calculateRadialHover(
                                            change.position + itemOffsetInRoot,
                                            menuCenter,
                                            actions,
                                            density,
                                            screenWidthPx,
                                            screenHeightPx
                                        )
                                        if (bestIndex != currentHoveredIndex) {
                                            currentHoveredIndex = bestIndex
                                            showRadialMenu(RadialMenuState(
                                                center = menuCenter,
                                                actions = actions,
                                                hoveredIndex = bestIndex,
                                                sourceId = group.id,
                                                sourceBounds = itemRect,
                                                sourceContent = itemContent
                                            ))
                                        }
                                    }
                                    if (isReleased) {
                                        if (currentHoveredIndex != -1) {
                                            val finalActions = buildActions()
                                            showRadialMenu(null)
                                            dismissOnExit = false
                                            finalActions[currentHoveredIndex].onClick()
                                            break
                                        }
                                        break
                                    }
                                } else {
                                    if (change == null || (change.position - down.position).getDistance() > viewConfig.touchSlop) {
                                        lpJob.cancel()
                                        break
                                    }
                                    if (isReleased) {
                                        lpJob.cancel()
                                        onClick()
                                        break
                                    }
                                }
                                if (change == null && !isReleased) break
                            }
                        } finally {
                            lpJob.cancel()
                            if (dismissOnExit) showRadialMenu(null)
                        }
                    }
                }
        ) {
            itemContent()
        }
    }
}

@Composable
fun GroupSelectionDialog(
    groups: List<EventGroup>,
    onSelectGroup: (EventGroup) -> Unit,
    onCreateNewGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGroupId by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_group),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        val isSelected = selectedGroupId == group.id
                        Surface(
                            onClick = { selectedGroupId = group.id },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF3A3A3A) else Color(0xFF262626),
                            border = if (isSelected) BorderStroke(1.dp, Color.White) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EventIconDisplay(group.icon, displaySize = 28.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    group.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            onClick = onCreateNewGroup,
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF262626),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.create_group),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            selectedGroupId?.let { id ->
                                groups.find { it.id == id }?.let { onSelectGroup(it) }
                            }
                        },
                        enabled = selectedGroupId != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF333333),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCreationDialog(
    initialName: String = "",
    initialIcon: String = "material:Folder",
    isEditing: Boolean = false,
    apps: List<AppInfo>?,
    onConfirm: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleText by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var showIconPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEditing) stringResource(R.string.edit_group) else stringResource(R.string.create_group),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon preview/selector button
                Surface(
                    onClick = { showIconPicker = true },
                    shape = CircleShape,
                    color = Color(0xFF262626),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        EventIconDisplay(selectedIcon, displaySize = 32.dp)
                    }
                }

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(stringResource(R.string.group_name)) },
                    placeholder = { Text(stringResource(R.string.group_name_hint)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (titleText.isNotBlank()) {
                                onConfirm(titleText.trim(), selectedIcon)
                            }
                        },
                        enabled = titleText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF333333),
                            disabledContentColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEditing) stringResource(R.string.done) else stringResource(R.string.create),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        EventIconPickerDialog(
            currentIcon = selectedIcon,
            apps = apps,
            onDismiss = { showIconPicker = false },
            onIconPicked = { picked ->
                selectedIcon = picked
                showIconPicker = false
            }
        )
    }
}
