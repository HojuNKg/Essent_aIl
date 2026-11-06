package com.HojuNK.essent_all.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
    animate: Boolean = true
) {
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
        Text(text)
    } else {
        Text(targetText)
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
    isHidden: Boolean = false
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

    Surface(
        color = Color(0xFF2B2B2B), // Серый фон панели
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke((1f * surfaceAlpha).dp, Color.Black.copy(alpha = surfaceAlpha)),
        modifier = modifier
            .width(barWidth)
            .height(70.dp)
            .graphicsLayer {
                translationY = animTranslationY
                this.scaleX = stretchScale * scaleX
                this.scaleY = scaleY
                alpha = surfaceAlpha
            }
            .pointerInput(isExpanded) {
                if (!isExpanded || hideProgress > 0.1f) return@pointerInput
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
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .offset(x = (safePagerPosition * 60f).dp)
                        .padding(vertical = 8.dp)
                        .width(148.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(50.dp))
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavScreen.entries.forEachIndexed { index, screen ->
                    val selectionProgress = (1f - abs(index - safePagerPosition)).coerceIn(0f, 1f)
                    val itemWidth = (56f + 92f * selectionProgress).dp

                    PillNavItem(
                        index = index,
                        pagerPosition = safePagerPosition,
                        screen = screen,
                        width = itemWidth,
                        onClick = { if (isExpanded) onScreenSelected(screen) }
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
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectionProgress = (1f - abs(index - pagerPosition)).coerceIn(0f, 1f)
    
    // Плавный расчет отступа для иконки
    val startPadding = (12f + 4f * selectionProgress).dp

    // Вычисляем направление "упругого" смещения.
    // Элементы слева от селектора должны смещаться влево, справа - вправо.
    val diff = index - pagerPosition
    val direction = if (diff > 0) 1f else -1f

    // drift - это величина смещения, которая уменьшается до 0 при выборе пункта.
    // Мы делаем ее симметричной, чтобы иконки с обеих сторон "раздвигались" селектором.
    val drift = (1f - selectionProgress) * 12f * direction

    Box(
        modifier = Modifier
            .width(width)
            .height(54.dp)
            .clip(RoundedCornerShape(50.dp))
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
                    // Применяем смещение ко всей группе (иконка + текст)
                    translationX = drift
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Плавный переход цвета (Grey -> White)
            val iconTint = Color(
                red = (128 + (255 - 128) * selectionProgress) / 255f,
                green = (128 + (255 - 128) * selectionProgress) / 255f,
                blue = (128 + (255 - 128) * selectionProgress) / 255f
            )

            Icon(
                imageVector = screen.icon,
                contentDescription = stringResource(screen.labelRes),
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            
            // Контейнер текста
            val textAlpha = ((selectionProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
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
                        text = stringResource(screen.labelRes),
                        color = Color.White,
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
