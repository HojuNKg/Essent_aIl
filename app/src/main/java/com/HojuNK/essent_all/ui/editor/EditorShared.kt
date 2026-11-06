package com.HojuNK.essent_all.ui.editor

import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.ui.components.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun SmallActionButton(icon: ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.width(44.dp).height(70.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(22.dp)).border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(22.dp)).clickable { onClick() }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(10.dp))
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PreviewStyleIcon(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp).background(if (isSelected) Color(0xFFFFD700) else Color.Transparent, CircleShape)) { Icon(icon, null, tint = if (isSelected) Color.Black else Color.Gray, modifier = Modifier.size(20.dp)) }
}

@Composable
fun WallpaperPreview(localPath: String?) {
    var wallpaper by remember(localPath) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(localPath) { mutableStateOf(localPath != null) }
    LaunchedEffect(localPath) { if (localPath != null) { withContext(kotlinx.coroutines.Dispatchers.IO) { try { val bitmap = android.graphics.BitmapFactory.decodeFile(localPath)?.asImageBitmap(); withContext(kotlinx.coroutines.Dispatchers.Main) { wallpaper = bitmap; isLoading = false } } catch (e: Exception) { withContext(kotlinx.coroutines.Dispatchers.Main) { isLoading = false } } } } else { isLoading = false } }
    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaper != null) { Image(bitmap = wallpaper!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()); Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) } 
        else { MockDesktop(); if (!isLoading && localPath == null) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) { Icon(Icons.Default.Wallpaper, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp)); Spacer(Modifier.height(8.dp)); Text("Нажмите на иконку обоев еще раз,\nчтобы выбрать фоновое изображение", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) } } } }
        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.Center), color = Color(0xFFFFD700), strokeWidth = 2.dp)
    }
}

@Composable
fun VideoPreview(localPath: String?, itemStartTime: Long, playbackTime: Long, isPlaying: Boolean, speed: Float, startOffset: Long = 0L) {
    if (localPath == null) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) { Icon(Icons.Default.VideoLibrary, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(32.dp)); Spacer(Modifier.height(8.dp)); Text("Нажмите на иконку видео еще раз,\nчтобы выбрать файл", color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) } } } 
    else {
        var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
        LaunchedEffect(isPlaying, speed) { mediaPlayer?.let { mp -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { try { val params = mp.playbackParams; params.speed = speed; mp.playbackParams = params } catch (e: Exception) {} }; if (isPlaying) mp.start() else mp.pause() } }
        LaunchedEffect(playbackTime, itemStartTime, startOffset, speed) { mediaPlayer?.let { mp -> val relativeTime = (playbackTime - itemStartTime).coerceAtLeast(0L); val targetPos = (startOffset + relativeTime * speed).toInt(); val delta = (mp.currentPosition - targetPos).absoluteValue; val threshold = if (isPlaying) 500 else 100; if (delta > threshold) { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) { mp.seekTo(targetPos.toLong(), android.media.MediaPlayer.SEEK_CLOSEST) } else { mp.seekTo(targetPos) } } } }
        DisposableEffect(localPath) { val mp = android.media.MediaPlayer().apply { setDataSource(localPath); isLooping = true; prepareAsync(); setOnPreparedListener { it.setVolume(0f, 0f); if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { try { val params = it.playbackParams; params.speed = speed; it.playbackParams = params } catch (e: Exception) {} }; val relativeTime = (playbackTime - itemStartTime).coerceAtLeast(0L); it.seekTo((startOffset + relativeTime * speed).toInt()); if (isPlaying) it.start() else it.pause() } }; mediaPlayer = mp; onDispose { mp.release(); mediaPlayer = null } }
        androidx.compose.ui.viewinterop.AndroidView(factory = { context -> android.view.SurfaceView(context).apply { holder.addCallback(object : android.view.SurfaceHolder.Callback { override fun surfaceCreated(holder: android.view.SurfaceHolder) { mediaPlayer?.setDisplay(holder) } override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {} override fun surfaceDestroyed(holder: android.view.SurfaceHolder) { mediaPlayer?.setDisplay(null) } }) } }, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun MockDesktop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF000000)))))
        Canvas(modifier = Modifier.fillMaxSize()) { val dotSpacing = 20.dp.toPx(); val dotRadius = 1.dp.toPx(); for (x in 0..(size.width / dotSpacing).toInt()) { for (y in 0..(size.height / dotSpacing).toInt()) { drawCircle(Color.Gray.copy(alpha = 0.1f), dotRadius, Offset(x * dotSpacing, y * dotSpacing)) } } }
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(4) { Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)) } }
    }
}

@Composable
fun SpeedCoefficientDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onConfirmed: (Float) -> Unit
) {
    var speed by remember { mutableFloatStateOf(currentSpeed) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Коэффициент скорости", color = Color.White) },
        text = {
            Column {
                Text("${String.format(Locale.US, "%.1f", speed)}x", color = Color.Gray)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.1f..3.0f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))
            }
        },
        confirmButton = { TextButton(onClick = { onConfirmed(speed) }) { Text("ОК", color = Color(0xFFFFD700)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ОТМЕНА", color = Color.Gray) } },
        containerColor = Color(0xFF1A1A1A)
    )
}

@Composable
fun LazyItemScope.ActionRow(
    action: String,
    index: Int,
    apps: List<AppInfo>,
    macros: List<Macro> = emptyList(),
    glyphPatterns: List<Macro> = emptyList(),
    isDragged: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onEdit: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onDelete: () -> Unit,
    onUpdateAction: ((String) -> Unit)? = null
) {
    val scale by animateFloatAsState(if (isDragged) 1.05f else 1f)
    val elevation by animateDpAsState(if (isDragged) 8.dp else 2.dp)
    val density = LocalDensity.current
    val ctx = LocalContext.current

    val currentIndex by rememberUpdatedState(index)
    val currentOnMove by rememberUpdatedState(onMove)

    var dragOffset by remember { mutableStateOf(0f) }
    var expanded by remember { mutableStateOf(false) }

    val matchingAction = remember(action) {
        ActionRegistry.allActions.find { item -> isTargetMatch(item, action) }
    }
    val hasInlineSettings = matchingAction != null && matchingAction.pickerType != null && matchingAction.pickerType != "app"

    val spacing = 12.dp
    val fullStepPx = with(density) { (110.dp + spacing).toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragged) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (isDragged) dragOffset else 0f
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expanded) Modifier.wrapContentHeight() else Modifier.height(110.dp))
                .animateItem(placementSpec = if (isDragged) null else spring(stiffness = Spring.StiffnessMediumLow))
                .background(Color.Black, RoundedCornerShape(24.dp))
                .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(6.dp)
                .pointerInput(action) {
                    if (!expanded) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragOffset = 0f; onDragStart() },
                            onDragEnd = { dragOffset = 0f; onDragEnd() },
                            onDragCancel = { dragOffset = 0f; onDragEnd() },
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
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("Действие ${index + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Close, null, tint = Color.White,
                    modifier = Modifier.size(16.dp).clickable { onDelete() }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.wrapContentHeight() else Modifier.weight(1f))
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (!hasInlineSettings) Modifier.clickable { onEdit() } else Modifier),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            slotDisplay(ctx, action, emptyList(), macros, apps, glyphPatterns),
                            color = Color.White, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (hasInlineSettings) {
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
                            }
                        }
                    }

                    if (expanded && matchingAction != null && onUpdateAction != null) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(8.dp))
                        val content = getActionContent(
                            action = matchingAction,
                            ctx = ctx,
                            keyMap = KeyMap(macros = macros),
                            testedActionId = action,
                            onUpdateId = { newAction: String ->
                                onUpdateAction(newAction)
                            }
                        )
                        if (content != null) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GestureMarker(
    action: String,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    progress: Float = 1f,
    onClick: () -> Unit = {}
) {
    val color = when {
        isHighlighted -> Color.White
        isSelected -> Color(0xFFFFD700)
        else -> Color.Gray.copy(alpha = 0.6f)
    }
    var size by remember { mutableStateOf(Size.Zero) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { size = Size(it.size.width.toFloat(), it.size.height.toFloat()) }) {
        if (size != Size.Zero) {
            val widthPx = 1080f
            val heightPx = 2400f

            when {
                action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                    val parts = action.removePrefix(Config.ACTION_TOUCH_PREFIX).split("_")
                    if (parts.size >= 2) {
                        val x = parts[0].toFloatOrNull() ?: 0f
                        val y = parts[1].toFloatOrNull() ?: 0f
                        // Tap animation: fade out as progress goes from 0 to 1
                        val alpha = if (isHighlighted) (1f - progress).coerceIn(0f, 1f) else 1f

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        ((x / widthPx) * size.width).toInt() - 16.dp.roundToPx(),
                                        ((y / heightPx) * size.height).toInt() - 16.dp.roundToPx()
                                    )
                                }
                                .graphicsLayer { this.alpha = alpha }
                                .size(if (isHighlighted) 48.dp else 32.dp)
                                .border(2.dp, color, CircleShape)
                                .background(color.copy(alpha = if (isHighlighted) 0.5f else 0.2f), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClick
                                )
                        )
                    }
                }
                action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                    val parts = action.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
                    if (parts.size >= 4) {
                        val x1 = parts[0].toFloatOrNull() ?: 0f
                        val y1 = parts[1].toFloatOrNull() ?: 0f
                        val x2 = parts[2].toFloatOrNull() ?: 0f
                        val y2 = parts[3].toFloatOrNull() ?: 0f

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val p1 = Offset((x1 / widthPx) * size.width, (y1 / heightPx) * size.height)
                            val p2 = Offset((x2 / widthPx) * size.width, (y2 / heightPx) * size.height)

                            // Trajectory drawn so far
                            val currentEnd = Offset(lerp(p1.x, p2.x, progress), lerp(p1.y, p2.y, progress))

                            // Main line (drawn full if not highlighted, otherwise up to progress)
                            drawLine(
                                color = color.copy(alpha = if (isHighlighted) 0.3f else 1f),
                                start = p1, end = p2,
                                strokeWidth = 4.dp.toPx()
                            )

                            if (isHighlighted) {
                                // Trail/active part
                                drawLine(color = Color.White, start = p1, end = currentEnd, strokeWidth = 6.dp.toPx())
                                drawCircle(color = Color.White, radius = 8.dp.toPx(), center = currentEnd)
                            }

                            drawCircle(color, 6.dp.toPx(), p1)
                            drawCircle(color, 4.dp.toPx(), p2)
                        }

                        // Hit area at start point
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        ((x1 / widthPx) * size.width).toInt() - 20.dp.roundToPx(),
                                        ((y1 / heightPx) * size.height).toInt() - 20.dp.roundToPx()
                                    )
                                }
                                .size(40.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClick
                                )
                        )
                    }
                }
                action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                    val pathData = action.removePrefix(Config.ACTION_PATH_PREFIX)
                    val pts = pathData.split(";").filter { it.isNotEmpty() }.mapNotNull {
                        val p = it.split(",")
                        if (p.size >= 2) {
                            val px = p[0].toFloatOrNull() ?: 0f
                            val py = p[1].toFloatOrNull() ?: 0f
                            Offset((px / widthPx) * size.width, (py / heightPx) * size.height)
                        } else null
                    }
                    if (pts.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (pts.size >= 2) {
                                val fullPath = Path()
                                fullPath.moveTo(pts[0].x, pts[0].y)
                                for (i in 1 until pts.size) fullPath.lineTo(pts[i].x, pts[i].y)

                                drawPath(
                                    fullPath,
                                    color.copy(alpha = if (isHighlighted) 0.3f else 1f),
                                    style = Stroke(width = 4.dp.toPx())
                                )

                                if (isHighlighted) {
                                    // Draw path segment by segment based on progress
                                    val totalSegments = pts.size - 1
                                    val targetIndex = (progress * totalSegments).toInt().coerceIn(0, totalSegments)

                                    val trailPath = Path()
                                    trailPath.moveTo(pts[0].x, pts[0].y)
                                    for (i in 1..targetIndex) {
                                        trailPath.lineTo(pts[i].x, pts[i].y)
                                    }

                                    // Add partial segment
                                    if (targetIndex < totalSegments) {
                                        val segProgress = (progress * totalSegments) - targetIndex
                                        val pStart = pts[targetIndex]
                                        val pEnd = pts[targetIndex + 1]
                                        val partialPos = Offset(lerp(pStart.x, pEnd.x, segProgress), lerp(pStart.y, pEnd.y, segProgress))
                                        trailPath.lineTo(partialPos.x, partialPos.y)
                                        drawCircle(Color.White, 8.dp.toPx(), partialPos)
                                    }

                                    drawPath(trailPath, Color.White, style = Stroke(width = 6.dp.toPx()))
                                }
                            }
                            drawCircle(color, 6.dp.toPx(), pts[0])
                            if (pts.size >= 2) drawCircle(color, 4.dp.toPx(), pts.last())
                        }

                        // Hit area at start point
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (pts[0].x).toInt() - 20.dp.roundToPx(),
                                        (pts[0].y).toInt() - 20.dp.roundToPx()
                                    )
                                }
                                .size(40.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvancedTimeline(
    timelineData: List<MacroActionTime>,
    scrollState: ScrollState,
    playbackTime: Long,
    totalDuration: Long,
    isPlaying: Boolean,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDurationChange: (Int, Long) -> Unit
) {
    val pxPerMs = 0.2f 
    val density = LocalDensity.current
    var timelineWidth by remember { mutableIntStateOf(0) }
    
    // Drag/Reorder State
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    
    var showQuickMenu by remember { mutableIntStateOf(-1) }

    val currentTimelineData by rememberUpdatedState(timelineData)
    val currentOnMove by rememberUpdatedState(onMove)

    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0A0A))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp) 
                .onGloballyPositioned { timelineWidth = it.size.width }
                .horizontalScroll(scrollState)
                .pointerInput(totalDuration) {
                    detectTapGestures { offset ->
                        val time = ((offset.x + scrollState.value) / (pxPerMs * density.density)).toLong()
                        onSeek(time.coerceIn(0, totalDuration))
                        showQuickMenu = -1
                    }
                }
        ) {
            val totalWidth = (totalDuration * pxPerMs).dp
            
            Box(modifier = Modifier.width(totalWidth).fillMaxHeight().padding(vertical = 4.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (t in 0..totalDuration step 1000L) {
                        val x = t * pxPerMs
                        drawLine(color = Color.White.copy(alpha = 0.15f), start = Offset(x.dp.toPx(), 0f), end = Offset(x.dp.toPx(), size.height), strokeWidth = 1f)
                    }
                }

                timelineData.forEach { data ->
                    key(data.id) {
                        val currentIdx = timelineData.indexOfFirst { it.id == data.id }
                        val isSelected = data.index == selectedIndex
                        val isDragged = data.id == draggedId

                        // Optimistic local state to prevent snap-backs
                        var localDuration by remember(data.id) { mutableLongStateOf(data.duration) }
                        var localStartTime by remember(data.id) { mutableLongStateOf(data.startTime) }
                        var isResizing by remember { mutableStateOf(false) }

                        // Sync with model ONLY when not actively resizing
                        if (!isResizing) {
                            localDuration = data.duration
                            localStartTime = data.startTime
                        }

                        val baseColor = when {
                            data.type.startsWith(Config.ACTION_TOUCH_PREFIX) -> Color.Gray
                            data.type.startsWith(Config.ACTION_SWIPE_PREFIX) -> Color.Gray.copy(alpha = 0.8f)
                            data.type.startsWith(Config.ACTION_PATH_PREFIX) -> Color.Gray.copy(alpha = 0.9f)
                            data.type.startsWith(Config.ACTION_DELAY_PREFIX) -> Color.Transparent
                            else -> Color.DarkGray
                        }

                        val color = if (isSelected || isDragged) Color(0xFFFFD700) else baseColor
                        val isDelay = data.type.startsWith(Config.ACTION_DELAY_PREFIX)

                        // SQUARE MODE: If dragging ANY item, all items shrink
                        val isGlobalDragging = draggedId != null
                        val targetWidth = if (isGlobalDragging) 40.dp else (localDuration * pxPerMs).dp.coerceAtLeast(if (isDelay) 4.dp else 24.dp)
                        val targetHeight = 40.dp

                        val animatedWidth by animateDpAsState(targetWidth, label = "timelineWidth")
                        val animatedHeight by animateDpAsState(targetHeight, label = "timelineHeight")
                        
                        Box(
                            modifier = Modifier
                                .offset { 
                                    if (isGlobalDragging) {
                                        val xBase = (currentIdx * 50.dp.toPx()).toInt()
                                        val xDrag = if (isDragged) dragOffsetPx.roundToInt() else 0
                                        IntOffset(x = xBase + xDrag, y = 0)
                                    } else {
                                        val xBase = (localStartTime * pxPerMs * density.density).toInt()
                                        IntOffset(x = xBase, y = 0)
                                    }
                                }
                                .size(animatedWidth, animatedHeight)
                                .align(Alignment.CenterStart)
                                .zIndex(if (isDragged || isSelected) 10f else 1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = if (isDragged) 0.8f else 1f))
                                .border(width = 1.dp, color = if (isSelected || isDragged) Color.White else if (isDelay) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(if (isDelay) 0.dp else 6.dp))
                                .combinedClickable(
                                    onClick = { onSelect(data.index); showQuickMenu = -1 },
                                    onDoubleClick = { showQuickMenu = data.index }
                                )
                                .pointerInput(data.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedId = data.id; dragOffsetPx = 0f; showQuickMenu = -1 },
                                        onDragEnd = { draggedId = null; dragOffsetPx = 0f },
                                        onDragCancel = { draggedId = null; dragOffsetPx = 0f },
                                        onDrag = { change, dragAmount -> 
                                            change.consume()
                                            dragOffsetPx += dragAmount.x
                                            
                                            val step = 50.dp.toPx()
                                            val moveBy = (dragOffsetPx / step).roundToInt()
                                            if (moveBy != 0) {
                                                val latestList = currentTimelineData
                                                val latestIdx = latestList.indexOfFirst { it.id == data.id }
                                                if (latestIdx != -1) {
                                                    val targetIdx = (latestIdx + moveBy).coerceIn(0, latestList.size - 1)
                                                    if (targetIdx != latestIdx) {
                                                        currentOnMove(latestIdx, targetIdx)
                                                        dragOffsetPx -= (targetIdx - latestIdx) * step
                                                    }
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isDelay) {
                                val icon = when {
                                    data.type.startsWith(Config.ACTION_TOUCH_PREFIX) -> Icons.Default.TouchApp
                                    data.type.startsWith(Config.ACTION_SWIPE_PREFIX) -> Icons.Default.Swipe
                                    data.type.startsWith(Config.ACTION_PATH_PREFIX) -> Icons.Default.Gesture
                                    else -> Icons.Default.Extension
                                }
                                Icon(icon, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }

                            if (isSelected && !isGlobalDragging) {
                                ResizeHandle(Alignment.CenterStart, onResize = { deltaMs -> 
                                    isResizing = true
                                    localStartTime += deltaMs
                                    localDuration -= deltaMs
                                }, onResizeEnd = {
                                    isResizing = false
                                    onDurationChange(data.index, localDuration)
                                })
                                ResizeHandle(Alignment.CenterEnd, onResize = { deltaMs -> 
                                    isResizing = true
                                    localDuration += deltaMs
                                }, onResizeEnd = {
                                    isResizing = false
                                    onDurationChange(data.index, localDuration)
                                })
                            }
                        }

                        if (showQuickMenu == data.index) {
                            QuickActionPopup(
                                x = (localStartTime * pxPerMs).dp,
                                onEdit = { onEdit(data.index); showQuickMenu = -1 },
                                onDelete = { onDelete(data.index); showQuickMenu = -1 }
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.offset(x = (playbackTime * pxPerMs).dp - 1.dp).width(2.dp).fillMaxHeight().background(Color.White))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaTrackTimeline(
    label: String, 
    color: Color, 
    totalDuration: Long,
    scrollState: ScrollState,
    mediaItems: List<MediaItem>,
    selectedMediaId: String?,
    onSelect: (String) -> Unit,
    onMediaChange: (String, MediaItem) -> Unit,
    onMoveOrder: (Int, Int) -> Unit,
    onDelete: (String) -> Unit,
    onSpeedClick: (() -> Unit)? = null
) {
    val pxPerMs = 0.2f
    val density = LocalDensity.current
    
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    val currentMediaItems by rememberUpdatedState(mediaItems)
    val currentOnMoveOrder by rememberUpdatedState(onMoveOrder)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color(0xFF0A0A0A))
            .horizontalScroll(scrollState)
    ) {
        val totalWidth = (totalDuration * pxPerMs).dp
        Box(modifier = Modifier.width(totalWidth).fillMaxHeight().padding(vertical = 4.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (t in 0..totalDuration step 1000L) {
                    val x = t * pxPerMs
                    drawLine(color = Color.White.copy(alpha = 0.15f), start = Offset(x.dp.toPx(), 0f), end = Offset(x.dp.toPx(), size.height), strokeWidth = 1f)
                }
            }

            mediaItems.forEach { item ->
                key(item.id) {
                    val idx = mediaItems.indexOfFirst { it.id == item.id }
                    val isSelected = item.id == selectedMediaId
                    val isDragged = item.id == draggedId
                    var showQuickMenu by remember { mutableStateOf(false) }

                    // Optimistic local state
                    var localDuration by remember(item.id) { mutableLongStateOf(item.duration) }
                    var localStartTime by remember(item.id) { mutableLongStateOf(item.startTime) }
                    var isResizing by remember { mutableStateOf(false) }

                    if (!isResizing) {
                        localDuration = item.duration
                        localStartTime = item.startTime
                    }

                    val isGlobalDragging = draggedId != null
                    val targetWidth = if (isGlobalDragging) 40.dp else (localDuration * pxPerMs).dp.coerceAtLeast(40.dp)
                    val targetHeight = 40.dp

                    val animatedWidth by animateDpAsState(targetWidth, label = "mediaWidth")
                    val animatedHeight by animateDpAsState(targetHeight, label = "mediaHeight")

                    Box(
                        modifier = Modifier
                            .offset {
                                if (isGlobalDragging) {
                                    val xPos = (idx * 50.dp.toPx()).toInt()
                                    val xDrag = if (isDragged) dragOffsetPx.roundToInt() else 0
                                    IntOffset(xPos + xDrag, 0)
                                } else {
                                    val xBase = (localStartTime * pxPerMs * density.density).toInt()
                                    IntOffset(xBase, 0)
                                }
                            }
                            .size(animatedWidth, animatedHeight)
                            .align(Alignment.CenterStart)
                            .zIndex(if (isDragged || isSelected) 10f else 1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = if (isDragged) 0.8f else if (isSelected) 0.6f else 0.4f))
                            .border(if (isDragged || isSelected) 2.dp else 1.dp, if (isDragged || isSelected) Color.White else color, RoundedCornerShape(6.dp))
                            .combinedClickable(
                                onClick = { onSelect(item.id); showQuickMenu = false },
                                onDoubleClick = { showQuickMenu = true }
                            )
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedId = item.id; dragOffsetPx = 0f },
                                    onDragEnd = { draggedId = null; dragOffsetPx = 0f },
                                    onDragCancel = { draggedId = null; dragOffsetPx = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx += dragAmount.x

                                        val step = 50.dp.toPx()
                                        val latestItems = currentMediaItems
                                        val latestIdx = latestItems.indexOfFirst { it.id == item.id }
                                        if (latestIdx != -1) {
                                            val moveBy = (dragOffsetPx / step).roundToInt()
                                            if (moveBy != 0) {
                                                val targetIdx = (latestIdx + moveBy).coerceIn(0, latestItems.size - 1)
                                                if (targetIdx != latestIdx) {
                                                    currentOnMoveOrder(latestIdx, targetIdx)
                                                    dragOffsetPx -= (targetIdx - latestIdx) * step
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val fileName = remember(item.path) { 
                            item.path.substringAfterLast("/").substringAfterLast("\\")
                        }
                        Text(
                            text = "${label.lowercase()}: $fileName", 
                            color = Color.White, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        if (isSelected && !isGlobalDragging) {
                            ResizeHandle(Alignment.CenterStart, onResize = { deltaMs ->
                                isResizing = true
                                // Max we can lengthen to the left is limited by how much original video we have before current startOffset
                                val maxLengthenMs = if (item.type == MediaType.VIDEO) item.startOffset else 3600000L
                                val oldDur = localDuration
                                localDuration = (localDuration - deltaMs).coerceIn(100L, item.duration + maxLengthenMs)
                                localStartTime += (oldDur - localDuration)
                            }, onResizeEnd = {
                                isResizing = false
                                val shiftMs = localStartTime - item.startTime
                                val newOffset = if (item.type == MediaType.VIDEO) item.startOffset + shiftMs else item.startOffset
                                onMediaChange(item.id, item.copy(
                                    startTime = localStartTime.coerceAtLeast(0L), 
                                    duration = localDuration.coerceAtLeast(100L),
                                    startOffset = newOffset.coerceAtLeast(0L)
                                ))
                            })
                            ResizeHandle(Alignment.CenterEnd, onResize = { deltaMs ->
                                isResizing = true
                                // Right limit is total video duration minus current start offset
                                val maxDur = if (item.type == MediaType.VIDEO) (item.fullDuration - item.startOffset) else 3600000L
                                localDuration = (localDuration + deltaMs).coerceIn(100L, maxDur)
                            }, onResizeEnd = {
                                isResizing = false
                                onMediaChange(item.id, item.copy(duration = localDuration.coerceAtLeast(100L)))
                            })
                        }
                    }

                    if (showQuickMenu) {
                        Row(
                            modifier = Modifier
                                .offset(x = (localStartTime * pxPerMs).dp, y = (-44).dp)
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onSpeedClick != null && item.type == MediaType.VIDEO) {
                                IconButton(onClick = { onSpeedClick(); showQuickMenu = false }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Speed, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { onDelete(item.id); showQuickMenu = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.ResizeHandle(
    alignment: Alignment,
    onResize: (Long) -> Unit,
    onResizeEnd: () -> Unit
) {
    val density = LocalDensity.current
    val pxPerMs = 0.2f
    var accum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .align(alignment)
            .width(36.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { accum = 0f },
                    onDragEnd = onResizeEnd,
                    onDragCancel = onResizeEnd,
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accum += dragAmount / density.density
                        val deltaMs = (accum / pxPerMs).toLong()
                        if (deltaMs != 0L) {
                            onResize(deltaMs)
                            accum -= deltaMs * pxPerMs
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight(0.6f)
                .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun QuickActionPopup(x: androidx.compose.ui.unit.Dp, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .offset(x = x, y = (-44).dp)
            .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
        }
    }
}
