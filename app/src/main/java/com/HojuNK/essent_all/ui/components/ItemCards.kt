package com.HojuNK.essent_all.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.HojuNK.essent_all.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.HojuNK.essent_all.Macro
import com.HojuNK.essent_all.KeyMap
import com.HojuNK.essent_all.Config
import com.HojuNK.essent_all.EssentialKeyService
import android.content.Intent
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StripeMacroItem(
    keyMap: KeyMap,
    macro: Macro,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onToggle: (Boolean) -> Unit = {},
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onPlay: (() -> Unit)? = null
) {
    val radialMenuState = LocalRadialMenuState.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var isDeletingAnimating by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    val itemHeight = remember { Animatable(86f) }

    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    val showRadialMenu = LocalRadialMenu.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    LaunchedEffect(isDeletingAnimating) {
        if (isDeletingAnimating) {
            itemHeight.animateTo(0f, animationSpec = tween(500))
            onDelete()
        }
    }

    if (itemHeight.value <= 0.1f && isDeletingAnimating) return

    val isActive = radialMenuState != null && radialMenuState.sourceId == macro.id

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
        val itemContent = @Composable {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (macro.isEnabled) Color(0xFF2B2B2B) else Color(0xFF101010),
                tonalElevation = if (macro.isEnabled) 2.dp else 0.dp,
                border = if (isSelected) BorderStroke(2.dp, Color.White) else if (!macro.isEnabled) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)) else null,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    EventIconDisplay(macro.icon, displaySize = 42.dp, grayscale = !macro.isEnabled)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(macro.title, style = MaterialTheme.typography.bodyLarge, color = if (macro.isEnabled) Color.White else Color.Gray)
                        if (macro.actions.isNotEmpty()) {
                            Text(
                                text = "${macro.actions.size} шагов",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    if (isSelectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() })
                    } else {
                        Switch(checked = macro.isEnabled, onCheckedChange = { onToggle(it) })
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSelectionMode) {
                        Modifier.clickable { onSelectToggle() }
                    } else {
                        Modifier.pointerInput(macro.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val menuCenter = down.position + itemOffsetInRoot
                                val actions = mutableListOf(
                                    RadialAction(Icons.Default.Edit, "Edit") { onEdit() },
                                    RadialAction(Icons.Default.ContentCopy, "Duplicate") { onDuplicate() },
                                    RadialAction(Icons.Default.Delete, "Delete") {
                                        if (keyMap.confirmDeletion) showConfirmDelete = true 
                                        else isDeletingAnimating = true
                                    }
                                ).apply {
                                    if (onPlay != null) {
                                        add(0, RadialAction(Icons.Default.PlayArrow, "Play") { onPlay() })
                                    }
                                }

                                var menuTriggered = false
                                var currentHoveredIndex = -1
                                var dismissOnExit = true
                                
                                val itemRect = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize())

                                val lpJob = scope.launch {
                                    delay(viewConfig.longPressTimeoutMillis)
                                    menuTriggered = true
                                    showRadialMenu(RadialMenuState(
                                        center = menuCenter, 
                                        actions = actions, 
                                        sourceId = macro.id,
                                        sourceBounds = itemRect,
                                        sourceContent = { itemContent() }
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
                                                        sourceId = macro.id,
                                                        sourceBounds = itemRect,
                                                        sourceContent = { itemContent() }
                                                    ))
                                                }
                                            }
                                            if (isReleased) {
                                                if (currentHoveredIndex != -1) {
                                                    showRadialMenu(null)
                                                    dismissOnExit = false
                                                    scope.launch { 
                                                        delay(50)
                                                        actions[currentHoveredIndex].onClick() 
                                                    }
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
                                                onEdit()
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
                    }
                )
        ) {
            itemContent()
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = macro.title,
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
fun RectangleMacroItem(
    keyMap: KeyMap,
    macro: Macro,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: () -> Unit = {},
    onToggle: (Boolean) -> Unit = {},
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onPlay: (() -> Unit)? = null
) {
    val radialMenuState = LocalRadialMenuState.current
    var showConfirmDelete by remember { mutableStateOf(false) }
    var itemOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }

    val showRadialMenu = LocalRadialMenu.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val isActive = radialMenuState != null && radialMenuState.sourceId == macro.id

    Box(modifier = Modifier.graphicsLayer { alpha = if (isActive) 0f else 1f }) {
        val itemContent = @Composable {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (macro.isEnabled) Color(0xFF2B2B2B) else Color(0xFF101010),
                tonalElevation = if (macro.isEnabled) 2.dp else 0.dp,
                border = if (isSelected) BorderStroke(2.dp, Color.White) else if (!macro.isEnabled) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)) else null,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        EventIconDisplay(
                            macro.icon,
                            displaySize = 64.dp,
                            grayscale = !macro.isEnabled,
                            modifier = Modifier.fillMaxWidth(0.67f).aspectRatio(1f).align(Alignment.Center)
                        )
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onSelectToggle() },
                                modifier = Modifier.align(Alignment.TopEnd)
                            )
                        }
                    }
                    Text(
                        macro.title,
                        style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp),
                        color = if (macro.isEnabled) Color.White else Color.Gray,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    if (macro.actions.isNotEmpty()) {
                        Text(
                            text = "${macro.actions.size} шагов",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { 
                    itemOffsetInRoot = it.positionInRoot() 
                    itemSize = it.size
                }
                .then(
                    if (isSelectionMode) {
                        Modifier.clickable { onSelectToggle() }
                    } else {
                        Modifier.pointerInput(macro.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val menuCenter = down.position + itemOffsetInRoot
                                val actions = mutableListOf(
                                    RadialAction(Icons.Default.Edit, "Edit") { onEdit() },
                                    RadialAction(Icons.Default.ContentCopy, "Duplicate") { onDuplicate() },
                                    RadialAction(Icons.Default.Delete, "Delete") {
                                        if (keyMap.confirmDeletion) showConfirmDelete = true 
                                        else onDelete()
                                    }
                                ).apply {
                                    if (onPlay != null) {
                                        add(0, RadialAction(Icons.Default.PlayArrow, "Play") { onPlay() })
                                    }
                                }

                                var menuTriggered = false
                                var currentHoveredIndex = -1
                                var dismissOnExit = true
                                
                                val itemRect = androidx.compose.ui.geometry.Rect(itemOffsetInRoot, itemSize.toSize())

                                val lpJob = scope.launch {
                                    delay(viewConfig.longPressTimeoutMillis)
                                    menuTriggered = true
                                    showRadialMenu(RadialMenuState(
                                        center = menuCenter, 
                                        actions = actions, 
                                        sourceId = macro.id,
                                        sourceBounds = itemRect,
                                        sourceContent = { itemContent() }
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
                                                        sourceId = macro.id,
                                                        sourceBounds = itemRect,
                                                        sourceContent = { itemContent() }
                                                    ))
                                                }
                                            }
                                            if (isReleased) {
                                                if (currentHoveredIndex != -1) {
                                                    showRadialMenu(null)
                                                    dismissOnExit = false
                                                    scope.launch { 
                                                        delay(50)
                                                        actions[currentHoveredIndex].onClick() 
                                                    }
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
                                                onEdit()
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
                    }
                ),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2B2B2B),
            tonalElevation = 2.dp
        ) {
            itemContent()
        }
    }

    if (showConfirmDelete) {
        ConfirmDeleteDialog(
            title = macro.title,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) {
                    scope.launch { Config.setConfirmDeletion(ctx, false) }
                }
                onDelete()
            },
            onDismiss = { showConfirmDelete = false }
        )
    }
}
