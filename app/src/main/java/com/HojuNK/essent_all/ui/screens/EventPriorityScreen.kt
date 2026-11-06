package com.HojuNK.essent_all.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.KeyMap
import com.HojuNK.essent_all.EventRule
import com.HojuNK.essent_all.TapSlot
import com.HojuNK.essent_all.util.EventIconDisplay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun TapSlot.getShortPressLabelRes(): Int = when (this) {
    TapSlot.SINGLE, TapSlot.VOL_UP_SINGLE, TapSlot.VOL_DOWN_SINGLE -> R.string.press_single
    TapSlot.LONG, TapSlot.VOL_UP_LONG, TapSlot.VOL_DOWN_LONG, TapSlot.POWER_LONG -> R.string.press_hold
    TapSlot.DOUBLE, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_DOWN_DOUBLE -> R.string.press_double
    TapSlot.TRIPLE, TapSlot.VOL_UP_TRIPLE, TapSlot.VOL_DOWN_TRIPLE -> R.string.press_triple
    else -> labelRes
}

private data class ButtonCategory(
    val titleRes: Int,
    val slots: List<TapSlot>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventPriorityScreen(
    keyMap: KeyMap,
    onBack: () -> Unit,
    onSave: (List<EventRule>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var savedEvents by remember { mutableStateOf(keyMap.events) }
    var tempEvents by remember { mutableStateOf(keyMap.events) }
    var selectedSlot by remember { mutableStateOf(TapSlot.SINGLE) }

    val categories = remember {
        listOf(
            ButtonCategory(R.string.section_essential_key, listOf(TapSlot.SINGLE, TapSlot.LONG, TapSlot.DOUBLE, TapSlot.TRIPLE)),
            ButtonCategory(R.string.section_power_button, listOf(TapSlot.POWER_LONG)),
            ButtonCategory(R.string.section_volume_up, listOf(TapSlot.VOL_UP_SINGLE, TapSlot.VOL_UP_LONG, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE)),
            ButtonCategory(R.string.section_volume_down, listOf(TapSlot.VOL_DOWN_SINGLE, TapSlot.VOL_DOWN_LONG, TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE))
        )
    }

    val pagerState = rememberPagerState(initialPage = 0) { categories.size }

    LaunchedEffect(pagerState.currentPage) {
        val categorySlots = categories[pagerState.currentPage].slots
        if (selectedSlot !in categorySlots) {
            selectedSlot = categorySlots.first()
        }
    }

    val hasChanges = remember(tempEvents, savedEvents) { tempEvents != savedEvents }

    val filteredEvents = remember(tempEvents, selectedSlot) {
        tempEvents.filter { it.triggerGesture == selectedSlot }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_priority_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White)
            )
        },
        containerColor = Color.Black
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Button Slider (Top level)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    Surface(
                        onClick = { scope.launch { pagerState.animateScrollToPage(page) } },
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(categories[page].titleRes),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Press Type Filter (Second level)
                val currentCategorySlots = categories[pagerState.currentPage].slots
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentCategorySlots, key = { it.key }) { slot ->
                        val selected = selectedSlot == slot
                        Surface(
                            onClick = { selectedSlot = slot },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Color.White else Color(0xFF2B2B2B),
                            border = if (!selected) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)) else null,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(slot.getShortPressLabelRes()),
                                    color = if (selected) Color.Black else Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.priority_drag_hint),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(16.dp))

                var draggedIndex by remember { mutableIntStateOf(-1) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = if (hasChanges) 96.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredEvents, key = { _, item -> item.id }) { index, event ->
                        PriorityEventRow(
                            event = event,
                            index = index,
                            isDragged = draggedIndex == index,
                            onDragStart = { draggedIndex = index },
                            onDragEnd = { draggedIndex = -1 },
                            onMove = { from: Int, to: Int ->
                                if (to in filteredEvents.indices) {
                                    val itemToMove = filteredEvents[from]
                                    val targetItem = filteredEvents[to]

                                    val newMaster = tempEvents.toMutableList()
                                    val fromIdxMaster = newMaster.indexOf(itemToMove)
                                    val toIdxMaster = newMaster.indexOf(targetItem)

                                    if (fromIdxMaster != -1 && toIdxMaster != -1) {
                                        newMaster.removeAt(fromIdxMaster)
                                        newMaster.add(toIdxMaster, itemToMove)
                                        tempEvents = newMaster
                                        draggedIndex = to
                                    }
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = hasChanges,
                enter = slideInVertically(
                    initialOffsetY = { it * 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it * 2 },
                    animationSpec = tween(200)
                ) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Revert button
                    Surface(
                        onClick = { tempEvents = savedEvents },
                        color = Color(0xFF2B2B2B),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.revert),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Save button
                    Surface(
                        onClick = {
                            onSave(tempEvents)
                            savedEvents = tempEvents
                        },
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.save),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LazyItemScope.PriorityEventRow(
    event: EventRule,
    index: Int,
    isDragged: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val scale by animateFloatAsState(if (isDragged) 1.05f else 1f)
    val elevation by animateDpAsState(if (isDragged) 8.dp else 2.dp)
    val density = LocalDensity.current

    val currentIndex by rememberUpdatedState(index)
    val currentOnMove by rememberUpdatedState(onMove)

    var dragOffset by remember { mutableStateOf(0f) }

    val itemHeight = 64.dp
    val spacing = 8.dp
    val fullStepPx = with(density) { (itemHeight + spacing).toPx() }

    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = elevation,
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .animateItem(
                placementSpec = if (isDragged) null else spring(stiffness = Spring.StiffnessMediumLow)
            )
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isDragged) dragOffset else 0f
            }
            .pointerInput(event.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragOffset = 0f
                        onDragStart()
                    },
                    onDragEnd = {
                        dragOffset = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        onDragEnd()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y

                        val moveBy = (dragOffset / fullStepPx).roundToInt()
                        if (moveBy != 0) {
                            val targetIndex = currentIndex + moveBy
                            if (targetIndex != currentIndex) {
                                currentOnMove(currentIndex, targetIndex)
                                dragOffset -= moveBy * fullStepPx
                            }
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${index + 1}.", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(16.dp))
            EventIconDisplay(event.icon, displaySize = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(event.title, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Icon(Icons.Default.DragHandle, null, tint = Color.Gray)
        }
    }
}
