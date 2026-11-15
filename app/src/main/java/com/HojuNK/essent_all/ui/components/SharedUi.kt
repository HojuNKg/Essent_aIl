package com.HojuNK.essent_all.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import android.content.res.Configuration
import android.view.Surface as ViewSurface
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.model.LoadingStage
import com.HojuNK.essent_all.model.NavScreen
import com.HojuNK.essent_all.util.createNothingBlurEffect
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.model.LocalRadialMenuState
import com.HojuNK.essent_all.util.EventIconDisplay
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor

// Глобальный флаг сессии, чтобы анимация не проигрывалась при каждом переходе на главный экран
private var hasPlayedTitleAnimation = false

@Composable
fun TypewriterTitle(
    baseText: String = stringResource(R.string.app_name),
    targetText: String = "Not essential space",
    animate: Boolean = true,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val useDark = com.HojuNK.essent_all.LocalUseDark.current
    val titleColor = if (useDark) Color.White else Color.Black

    // Анимируем только если передан флаг И анимация еще не проигрывалась в этой сессии
    val shouldAnimate = animate && !hasPlayedTitleAnimation

    if (shouldAnimate) {
        var text by remember { mutableStateOf(baseText) }
        LaunchedEffect(targetText) {
            text = baseText
            delay(2000)
            while (text.isNotEmpty()) {
                delay(100)
                text = text.dropLast(1)
            }
            delay(300)
            for (char in targetText) {
                delay(100)
                text += char
            }
            // Помечаем, что анимация завершена для всей текущей сессии
            hasPlayedTitleAnimation = true
        }
        Text(text, fontSize = fontSize, fontWeight = fontWeight, color = titleColor)
    } else {
        Text(targetText, fontSize = fontSize, fontWeight = fontWeight, color = titleColor)
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
private fun lerpDp(start: androidx.compose.ui.unit.Dp, stop: androidx.compose.ui.unit.Dp, fraction: Float): androidx.compose.ui.unit.Dp =
    androidx.compose.ui.unit.Dp(start.value + (stop.value - start.value) * fraction)

@Composable
fun PillBottomNavigation(
    pagerPosition: Float,
    loadingStage: LoadingStage,
    onScreenSelected: (NavScreen) -> Unit,
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onDragEnd: () -> Unit = {},
    isHidden: Boolean = false,
    screens: List<NavScreen> = listOf(NavScreen.KEY, NavScreen.EVENTS, NavScreen.MACROS, NavScreen.MORE),
    isEditing: Boolean = false,
    selectedEditSlot: Int = 1,
    onSelectEditSlot: (Int) -> Unit = {}
) {
    val isExpanded = !isHidden
    
    // Анимация состояния скрытия хотбара:
    // 0.0 = хотбар полностью виден
    // 0.35 = схлопнулся до круга (70.dp x 70.dp)
    // 1.0 = уехал вниз и растекся капли об нижнюю границу экрана
    val hideProgress by animateFloatAsState(
        targetValue = if (isHidden) 1f else 0f,
        animationSpec = if (isHidden) {
            tween(durationMillis = 550, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 500, easing = FastOutSlowInEasing)
        },
        label = "hotbarHideProgress"
    )

    // 1. Схлопывание ширины до круга 70.dp
    val fullBarWidth = 344.dp
    val widthProgress = (hideProgress / 0.35f).coerceIn(0f, 1f)
    val animatedWidthProgress = FastOutSlowInEasing.transform(widthProgress)
    val barWidth = lerpDp(fullBarWidth, 70.dp, animatedWidthProgress)
    
    val density = LocalDensity.current
    val safePagerPosition = pagerPosition.coerceIn(0f, 3f)
    val currentPagerPosition by rememberUpdatedState(safePagerPosition)
    var isDraggingInternal by remember { mutableStateOf(false) }
    
    // 2. Анимация уезда вниз и эффекта капли (жидкое вытягивание и растекание)
    val dropProgress = ((hideProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val dropDistancePx = with(density) { 140.dp.toPx() }
    val animTranslationY = dropProgress * dropDistancePx
    
    val scaleY: Float
    val scaleX: Float
    val surfaceAlpha: Float
    
    if (dropProgress <= 0.6f) {
        // Фаза падения: круг вытягивается по вертикали в капли (teardrop)
        val fallProgress = FastOutSlowInEasing.transform(dropProgress / 0.6f)
        scaleY = lerpFloat(1.0f, 1.4f, fallProgress)
        scaleX = lerpFloat(1.0f, 0.65f, fallProgress)
        surfaceAlpha = 1.0f
    } else {
        // Фаза касания нижней границы: капля расплющивается и растекается по нижней линии
        val meltProgress = FastOutSlowInEasing.transform((dropProgress - 0.6f) / 0.4f)
        scaleY = lerpFloat(1.4f, 0.15f, meltProgress)
        scaleX = lerpFloat(0.65f, 1.85f, meltProgress)
        surfaceAlpha = lerpFloat(1.0f, 0.0f, meltProgress)
    }

    // Прозрачность и масштаб контента (иконок и текста) при схлопывании в круг
    val contentAlpha = (1f - (hideProgress / 0.25f)).coerceIn(0f, 1f)
    val contentScale = lerpFloat(1f, 0.6f, (hideProgress / 0.3f).coerceIn(0f, 1f))

    // Эффект растягивания при перетаскивании
    val stretchScale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "stretchScale"
    )

    val useDark = com.HojuNK.essent_all.LocalUseDark.current
    val barColor = if (useDark) Color(0xFF2B2B2B) else Color(0xFFE5E5E5)
    val barBorderColor = if (useDark) Color.Black.copy(alpha = surfaceAlpha) else Color.Gray.copy(alpha = 0.3f * surfaceAlpha)
    val selectorColor = if (useDark) Color(0xFF1A1A1A) else Color.White

    Surface(
        color = barColor,
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke((1f * surfaceAlpha).dp, barBorderColor),
        modifier = modifier
            .width(barWidth)
            .height(70.dp)
            .graphicsLayer {
                translationY = animTranslationY
                this.scaleX = stretchScale * scaleX
                this.scaleY = scaleY
                alpha = surfaceAlpha
            }
            .pointerInput(isExpanded && !isEditing) {
                if (!isExpanded || hideProgress > 0.1f || isEditing) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val selectorCenterPx = with(density) { (82f + currentPagerPosition * 60f).dp.toPx() }
                        val hitHalfWidthPx = with(density) { 74f.dp.toPx() }
                        if (abs(offset.x - selectorCenterPx) <= hitHalfWidthPx) {
                            isDraggingInternal = true
                        }
                    },
                    onDrag = { _, dragAmount ->
                        if (isDraggingInternal) onDrag(dragAmount.x)
                    },
                    onDragEnd = { isDraggingInternal = false; onDragEnd() },
                    onDragCancel = { isDraggingInternal = false; onDragEnd() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    alpha = contentAlpha
                    this.scaleX = contentScale
                    this.scaleY = contentScale
                }
        ) {
            // Единый "перетекающий" селектор
            if (isExpanded && !isEditing) {
                Box(
                    modifier = Modifier
                        .offset(x = (safePagerPosition * 60f).dp)
                        .padding(vertical = 8.dp)
                        .width(148.dp)
                        .fillMaxHeight()
                        .background(selectorColor, RoundedCornerShape(50.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                screens.forEachIndexed { index, screen ->
                    val selectionProgress = if (isEditing) (if (index == selectedEditSlot) 1f else 0f) else (1f - abs(index - safePagerPosition)).coerceIn(0f, 1f)
                    val itemWidth = (56f + 92f * selectionProgress).dp

                    PillNavItem(
                        index = index,
                        pagerPosition = safePagerPosition,
                        screen = screen,
                        width = itemWidth,
                        isEditing = isEditing,
                        selectedEditSlot = selectedEditSlot,
                        onClick = {
                            if (isEditing) {
                                if (index == 1 || index == 2) {
                                    onSelectEditSlot(index)
                                }
                            } else if (isExpanded) {
                                onScreenSelected(screen)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PillNavItem(
    index: Int,
    pagerPosition: Float,
    screen: NavScreen,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    isEditing: Boolean = false,
    selectedEditSlot: Int = 1
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectionProgress = if (isEditing) (if (index == selectedEditSlot) 1f else 0f) else (1f - abs(index - pagerPosition)).coerceIn(0f, 1f)
    
    // Плавный расчет отступа для иконки
    val startPadding = (12f + 4f * selectionProgress).dp

    // Вычисляем направление "упругого" смещения.
    val diff = index - pagerPosition
    val direction = if (diff > 0) 1f else -1f

    val drift = if (isEditing) 0f else (1f - selectionProgress) * 12f * direction

    val isFixedSlot = (index == 0 || index == 3)
    val isSelectedSlot = (isEditing && index == selectedEditSlot)

    val borderModifier = when {
        isEditing && isFixedSlot -> Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
        isEditing && isSelectedSlot -> Modifier.border(2.dp, Color.White, RoundedCornerShape(50.dp))
        isEditing && !isFixedSlot -> Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
        else -> Modifier
    }

    val itemAlpha = when {
        isEditing && isFixedSlot -> 0.4f
        else -> 1.0f
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(54.dp)
            .clip(RoundedCornerShape(50.dp))
            .then(borderModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .padding(start = startPadding)
                .graphicsLayer {
                    translationX = drift
                    alpha = itemAlpha
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val useDark = com.HojuNK.essent_all.LocalUseDark.current
            val baseTint = if (useDark) {
                Color(
                    red = (128 + (255 - 128) * selectionProgress) / 255f,
                    green = (128 + (255 - 128) * selectionProgress) / 255f,
                    blue = (128 + (255 - 128) * selectionProgress) / 255f
                )
            } else {
                Color(
                    red = (120 * (1f - selectionProgress)) / 255f,
                    green = (120 * (1f - selectionProgress)) / 255f,
                    blue = (120 * (1f - selectionProgress)) / 255f
                )
            }

            val iconTint = if (isEditing && isSelectedSlot) (if (useDark) Color.White else Color.Black) else baseTint

            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = screen.title,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )

                if (isEditing && isFixedSlot) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 4.dp, y = (-2).dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .padding(1.dp)
                    )
                }
            }
            
            // Контейнер текста
            val textAlpha = if (isEditing) (if (isSelectedSlot) 1f else 0f) else ((selectionProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
            val textSpaceWidth = (92f * selectionProgress).dp
            
            Box(
                modifier = Modifier
                    .width(textSpaceWidth)
                    .graphicsLayer {
                        alpha = textAlpha
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = screen.title,
                        color = if (useDark) Color.White else Color.Black,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun RoundHeaderButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1A1A1A),
    borderColor: Color? = Color.Gray.copy(alpha = 0.5f),
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (borderColor != null) Modifier.border(2.dp, borderColor, CircleShape) else Modifier)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun LabeledPanel(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            EventIconDisplay(value, displaySize = 32.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableBlockItem(
    isEditing: Boolean,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!isEditing) {
        Box(modifier = modifier) { content() }
        return
    }

    val density = LocalDensity.current
    var offsetX by remember { mutableStateOf(0f) }
    var showActionPanel by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = when {
            isDeleting -> 1000f
            showActionPanel -> -140f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val alphaAnim by animateFloatAsState(
        targetValue = if (isDeleting) 0f else 1f,
        animationSpec = tween(200)
    )

    val actionPanelWidthPx = with(density) { 140.dp.toPx() }
    val deleteThresholdPx = with(density) { 100.dp.toPx() }
    val swipeLeftThresholdPx = with(density) { -60.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(
                translationX = if (isDeleting) animatedOffsetX else (offsetX + animatedOffsetX).coerceIn(-actionPanelWidthPx, 1000f),
                alpha = alphaAnim
            )
            .pointerInput(isEditing) {
                if (!isEditing) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        if (offsetX > deleteThresholdPx) {
                            isDeleting = true
                        } else if (offsetX < swipeLeftThresholdPx) {
                            showActionPanel = true
                            offsetX = 0f
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }

        if (showActionPanel || animatedOffsetX < -10f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(140.dp)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF222222), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF444444), RoundedCornerShape(16.dp))
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoveUp != null) {
                        IconButton(onClick = {
                            showActionPanel = false
                            onMoveUp()
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    IconButton(onClick = {
                        isDeleting = true
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5555))
                    }

                    if (onMoveDown != null) {
                        IconButton(onClick = {
                            showActionPanel = false
                            onMoveDown()
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            kotlinx.coroutines.delay(220)
            onDelete()
        }
    }
}

@Composable
fun ConnectingLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.Gray.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun EventItemBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D), RoundedCornerShape(28.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.padding(end = if (onDelete != null) 32.dp else 0.dp)) {
            content()
        }
        if (onDelete != null) {
            Icon(
                Icons.Default.Close,
                null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(20.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

@Composable
fun PanelAddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ShortcutToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PickRow(label: String, icon: ImageBitmap?, selected: Boolean, subLabel: String? = null, onClick: () -> Unit) {
    Surface(color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.clickable(onClick = onClick).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { androidx.compose.foundation.Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp)); Spacer(Modifier.width(12.dp)) }
            else Spacer(Modifier.size(36.dp).width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (subLabel != null) {
                    Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun rememberUseDark(keyMap: com.HojuNK.essent_all.KeyMap): Boolean {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    return remember(keyMap.theme, systemDark) {
        when (keyMap.theme) {
            com.HojuNK.essent_all.AppTheme.AUTO -> systemDark
            com.HojuNK.essent_all.AppTheme.LIGHT -> false
            com.HojuNK.essent_all.AppTheme.DARK -> true
        }
    }
}

@Composable
fun OrientationRotatable(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val ctx = LocalContext.current
    val rotation = ctx.display?.rotation ?: ViewSurface.ROTATION_0

    val rotationDegrees by animateFloatAsState(
        targetValue = if (isLandscape) {
            if (rotation == ViewSurface.ROTATION_90) 90f else -90f
        } else 0f,
        label = "orientation_rotation"
    )

    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotationDegrees },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
