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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.text.TextLayoutResult
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.content.Context
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
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

fun copyUriToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imagesDir = java.io.File(context.filesDir, "note_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val imageFile = java.io.File(imagesDir, "img_${java.util.UUID.randomUUID()}.png")
        java.io.FileOutputStream(imageFile).use { out ->
            inputStream.copyTo(out)
        }
        imageFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

class FontItem(
    val id: String,
    val displayName: String,
    val fontFamily: FontFamily?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FontItem) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

object FontManager {
    val Ndot55FontFamily = FontFamily.Monospace

    val Ndot57CapsFontFamily = try {
        FontFamily(Font(R.font.ndot57caps_regular))
    } catch (e: Throwable) {
        FontFamily.Monospace
    }

    val Ndot77JpFontFamily = FontFamily.Monospace

    fun getAvailableFonts(context: Context? = null): List<FontItem> {
        return listOf(
            FontItem("system", "Системный", null),
            FontItem("monospace", "Моноширинный", FontFamily.Monospace),
            FontItem("ndot55", "Ndot 55", Ndot55FontFamily),
            FontItem("ndot57caps_regular", "Ndot 57 Caps", Ndot57CapsFontFamily),
            FontItem("ndot77jpextended", "Ndot 77 JP", Ndot77JpFontFamily)
        )
    }

    fun getFontFamilyById(fontId: String, context: Context? = null): FontFamily? {
        return when (fontId) {
            "system" -> null
            "monospace" -> FontFamily.Monospace
            "ndot55" -> Ndot55FontFamily
            "ndot57caps_regular" -> Ndot57CapsFontFamily
            "ndot77jpextended" -> Ndot77JpFontFamily
            else -> null
        }
    }

    fun getFontIdByFamily(fontFamily: FontFamily?, context: Context? = null): String {
        return when (fontFamily) {
            null -> "system"
            FontFamily.Monospace -> "monospace"
            Ndot55FontFamily -> "ndot55"
            Ndot57CapsFontFamily -> "ndot57caps_regular"
            Ndot77JpFontFamily -> "ndot77jpextended"
            else -> "system"
        }
    }

    fun formatFontDisplayName(rawName: String): String {
        if (rawName.equals("ndot55", ignoreCase = true)) return "Ndot 55"
        if (rawName.equals("ndot57caps_regular", ignoreCase = true)) return "Ndot 57 Caps"
        if (rawName.equals("ndot77jpextended", ignoreCase = true)) return "Ndot 77 JP"
        if (rawName.equals("system", ignoreCase = true)) return "Системный"
        if (rawName.equals("monospace", ignoreCase = true)) return "Моноширинный"
        return rawName
    }
}

enum class StructureType {
    BULLET,
    NUMBER,
    CHECKBOX
}

fun renumberLinesIfNeeded(text: String): String {
    val lines = text.split("\n")
    var hasNumbers = false
    val numberRegex = Regex("""^\d+\.\s+""")

    for (line in lines) {
        if (numberRegex.containsMatchIn(line)) {
            hasNumbers = true
            break
        }
    }

    if (!hasNumbers) return text

    val newLines = mutableListOf<String>()
    var numberCounter = 1

    for (line in lines) {
        if (numberRegex.containsMatchIn(line)) {
            val contentWithoutNumber = line.replace(numberRegex, "")
            newLines.add("${numberCounter++}. $contentWithoutNumber")
        } else {
            numberCounter = 1
            newLines.add(line)
        }
    }

    return newLines.joinToString("\n")
}

fun isFormatFullyApplied(
    existingSpans: List<AnnotatedString.Range<SpanStyle>>,
    start: Int,
    end: Int,
    predicate: (SpanStyle) -> Boolean
): Boolean {
    if (start >= end) return false

    val matchingRanges = mutableListOf<Pair<Int, Int>>()
    for (range in existingSpans) {
        if (predicate(range.item)) {
            val oStart = maxOf(range.start, start)
            val oEnd = minOf(range.end, end)
            if (oStart < oEnd) {
                matchingRanges.add(oStart to oEnd)
            }
        }
    }

    if (matchingRanges.isEmpty()) return false

    matchingRanges.sortBy { it.first }

    var currentCoveredEnd = start
    for ((mStart, mEnd) in matchingRanges) {
        if (mStart > currentCoveredEnd) {
            return false
        }
        currentCoveredEnd = maxOf(currentCoveredEnd, mEnd)
    }

    return currentCoveredEnd >= end
}

fun formatCheckboxesInAnnotatedString(annotatedString: AnnotatedString): AnnotatedString {
    val text = annotatedString.text
    if (!text.contains("☐") && !text.contains("☑")) return annotatedString

    val newSpans = annotatedString.spanStyles.toMutableList()
    var idx = 0
    while (idx < text.length) {
        val ch = text[idx]
        if (ch == '☐' || ch == '☑') {
            newSpans.add(
                AnnotatedString.Range(
                    SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    idx,
                    idx + 1
                )
            )
        }
        idx++
    }
    return AnnotatedString(text = text, spanStyles = newSpans)
}

fun applyStructureToTextBlock(
    currentTfv: TextFieldValue,
    type: StructureType,
    customStart: Int = currentTfv.selection.start,
    customEnd: Int = currentTfv.selection.end
): TextFieldValue {
    val fullText = currentTfv.text
    if (fullText.isEmpty()) {
        val prefix = when (type) {
            StructureType.BULLET -> "• "
            StructureType.NUMBER -> "1. "
            StructureType.CHECKBOX -> "☐ "
        }
        val newText = prefix
        return TextFieldValue(
            annotatedString = AnnotatedString(newText),
            selection = TextRange(newText.length)
        )
    }

    val lines = fullText.split("\n")
    val lineStarts = IntArray(lines.size)
    val lineEnds = IntArray(lines.size)
    var currPos = 0
    for (i in lines.indices) {
        lineStarts[i] = currPos
        lineEnds[i] = currPos + lines[i].length
        currPos = lineEnds[i] + 1
    }

    val selStart = customStart.coerceIn(0, fullText.length)
    val selEnd = customEnd.coerceIn(0, fullText.length)
    val minSel = minOf(selStart, selEnd)
    val maxSel = maxOf(selStart, selEnd)

    val affectedLineIndices = mutableListOf<Int>()
    for (i in lines.indices) {
        val lStart = lineStarts[i]
        val lEnd = lineEnds[i]
        val intersects = if (minSel == maxSel) {
            minSel in lStart..lEnd
        } else {
            maxSel > lStart && minSel <= lEnd
        }
        if (intersects) {
            affectedLineIndices.add(i)
        }
    }

    if (affectedLineIndices.isEmpty()) return currentTfv

    val bulletRegex = Regex("""^(•|-|\*)\s+""")
    val numberRegex = Regex("""^(\d+\.|\d+\))\s+""")
    val checkboxRegex = Regex("""^(☐|☑|\[\s*\]|\[x\])\s+""")
    val anyPrefixRegex = Regex("""^((•|-|\*|\d+\.|\d+\)|☐|☑|\[\s*\]|\[x\])\s*)+""")

    val allAlreadyHaveTarget = affectedLineIndices.all { idx ->
        val line = lines[idx]
        when (type) {
            StructureType.BULLET -> bulletRegex.containsMatchIn(line)
            StructureType.NUMBER -> numberRegex.containsMatchIn(line)
            StructureType.CHECKBOX -> checkboxRegex.containsMatchIn(line)
        }
    }

    val newLines = lines.toMutableList()
    var numberCounter = 1

    for (i in lines.indices) {
        if (i in affectedLineIndices) {
            val oldLine = newLines[i]
            val strippedLine = oldLine.replace(anyPrefixRegex, "")

            if (allAlreadyHaveTarget) {
                newLines[i] = strippedLine
            } else {
                val prefix = when (type) {
                    StructureType.BULLET -> "• "
                    StructureType.CHECKBOX -> "☐ "
                    StructureType.NUMBER -> "${numberCounter++}. "
                }
                newLines[i] = prefix + strippedLine
            }
        }
    }

    val newFullText = renumberLinesIfNeeded(newLines.joinToString("\n"))
    val formattedAnnotated = formatCheckboxesInAnnotatedString(
        AnnotatedString(text = newFullText, spanStyles = currentTfv.annotatedString.spanStyles)
    )

    val newTfv = TextFieldValue(
        annotatedString = formattedAnnotated,
        selection = TextRange(newFullText.length)
    )
    return preserveSpansOnTextChange(currentTfv.annotatedString, newTfv)
}

@Composable
fun StructureSelectionBar(
    activeType: StructureType,
    onSelectType: (StructureType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2C),
        border = BorderStroke(1.dp, Color(0xFF444444)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StructureOptionItem(
                title = "Маркеры",
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                isSelected = activeType == StructureType.BULLET,
                onClick = { onSelectType(StructureType.BULLET) }
            )
            StructureOptionItem(
                title = "Номера",
                icon = Icons.Default.FormatListNumbered,
                isSelected = activeType == StructureType.NUMBER,
                onClick = { onSelectType(StructureType.NUMBER) }
            )
            StructureOptionItem(
                title = "Флажки",
                icon = Icons.Default.Checklist,
                isSelected = activeType == StructureType.CHECKBOX,
                onClick = { onSelectType(StructureType.CHECKBOX) }
            )
        }
    }
}

@Composable
fun StructureOptionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color(0xFF3D3D3D),
        contentColor = if (isSelected) Color.Black else Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.Black else Color.White
            )
        }
    }
}

fun handleEnterAndAutoStructure(oldTfv: TextFieldValue, newTfv: TextFieldValue): TextFieldValue {
    val oldText = oldTfv.text
    val newText = newTfv.text

    val newSelStart = newTfv.selection.start
    if (newText.length == oldText.length + 1 && newSelStart > 0 && newText[newSelStart - 1] == '\n') {
        val insertPos = newSelStart - 1

        val paraStart = oldText.lastIndexOf('\n', (insertPos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val paraEnd = oldText.indexOf('\n', insertPos).let { if (it == -1) oldText.length else it }

        val prevPara = oldText.substring(paraStart, paraEnd)

        val bulletRegex = Regex("""^[•\-\*]\s+""")
        val numberRegex = Regex("""^(\d+)\.\s+""")
        val checkboxRegex = Regex("""^(☐|☑|\[\s*\]|\[x\])\s+""")

        val bulletMatch = bulletRegex.find(prevPara)
        val numberMatch = numberRegex.find(prevPara)
        val checkboxMatch = checkboxRegex.find(prevPara)

        val match = bulletMatch ?: numberMatch ?: checkboxMatch

        if (match != null) {
            val newPrefix = when {
                bulletMatch != null -> "• "
                checkboxMatch != null -> "☐ "
                numberMatch != null -> {
                    val num = numberMatch.groupValues[1].toIntOrNull() ?: 1
                    "${num + 1}. "
                }
                else -> "• "
            }

            val textBefore = oldText.substring(0, insertPos)
            val textAfter = oldText.substring(insertPos)
            val resultText = textBefore + "\n" + newPrefix + textAfter
            val newCursor = insertPos + 1 + newPrefix.length

            val oldSpans = oldTfv.annotatedString.spanStyles
            val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
            val addedLength = 1 + newPrefix.length

            for (range in oldSpans) {
                var s = range.start
                var e = range.end
                if (s >= insertPos) {
                    s += addedLength
                    e += addedLength
                } else if (e > insertPos) {
                    e += addedLength
                }
                newSpans.add(AnnotatedString.Range(range.item, s.coerceIn(0, resultText.length), e.coerceIn(0, resultText.length)))
            }

            return TextFieldValue(
                annotatedString = AnnotatedString(text = resultText, spanStyles = newSpans),
                selection = TextRange(newCursor.coerceIn(0, resultText.length))
            )
        }
    }
    return newTfv
}

fun isStructureActive(tfv: TextFieldValue): Boolean {
    val text = tfv.text
    val cursorPos = tfv.selection.start.coerceIn(0, text.length)
    val lines = text.split("\n")
    var charCount = 0
    val prefixRegex = Regex("""^([•\-\*]|\d+\.|\d+\)|☐|☑|\[\s*\]|\[x\])\s+""")
    for (line in lines) {
        val start = charCount
        val end = charCount + line.length
        charCount = end + 1
        if (cursorPos in start..end + 1) {
            return prefixRegex.containsMatchIn(line)
        }
    }
    return false
}

fun annotatedStringToSpannable(annotatedString: AnnotatedString, context: Context): SpannableStringBuilder {
    val text = annotatedString.text
    val ssb = SpannableStringBuilder(text)
    if (text.isEmpty()) return ssb
    if (annotatedString.spanStyles.isEmpty()) return ssb

    for (range in annotatedString.spanStyles) {
        val start = range.start.coerceIn(0, text.length)
        val end = range.end.coerceIn(0, text.length)
        if (start >= end) continue
        val style = range.item

        if (style.fontWeight == FontWeight.Bold && style.fontStyle == FontStyle.Italic) {
            ssb.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else if (style.fontWeight == FontWeight.Bold) {
            ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else if (style.fontStyle == FontStyle.Italic) {
            ssb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (style.textDecoration?.contains(TextDecoration.Underline) == true) {
            ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (style.color != Color.Unspecified) {
            val colorInt = style.color.toArgb()
            ssb.setSpan(ForegroundColorSpan(colorInt), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (style.fontSize != TextUnit.Unspecified) {
            ssb.setSpan(AbsoluteSizeSpan(style.fontSize.value.toInt(), true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (style.fontFamily != null) {
            val fontId = FontManager.getFontIdByFamily(style.fontFamily, context)
            val typeface = when (fontId) {
                "monospace" -> Typeface.MONOSPACE
                "ndot55" -> try { ResourcesCompat.getFont(context, R.font.ndot55) } catch (_: Exception) { Typeface.MONOSPACE }
                "ndot57caps_regular" -> try { ResourcesCompat.getFont(context, R.font.ndot57caps_regular) } catch (_: Exception) { Typeface.MONOSPACE }
                "ndot77jpextended" -> try { ResourcesCompat.getFont(context, R.font.ndot77jpextended) } catch (_: Exception) { Typeface.MONOSPACE }
                else -> null
            }
            if (typeface != null) {
                ssb.setSpan(TypefaceSpan(typeface), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
    return ssb
}

@Composable
fun RichAndroidTextField(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onImageReceived: ((android.net.Uri) -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    onToggleCheckboxAtLine: ((Int) -> Unit)? = null,
    onSelectionChanged: ((TextRange) -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1A1A1A),
        border = BorderStroke(1.dp, Color(0xFF333333)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            var isUserEditing by remember { mutableStateOf(false) }
            var lastAnnotatedString by remember { mutableStateOf(textFieldValue.annotatedString) }

            val content = @Composable {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(readOnly, onClick, onToggleCheckboxAtLine) {
                            if (!readOnly) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (up != null) {
                                    val duration = up.uptimeMillis - down.uptimeMillis
                                    if (duration < 300) {
                                        val offset = up.position
                                        val layout = layoutResult
                                        if (layout != null && onToggleCheckboxAtLine != null) {
                                            val lineIdx = layout.getLineForVerticalPosition(offset.y)
                                            val lineStartOffset = layout.getLineStart(lineIdx)
                                            val text = textFieldValue.text
                                            if (lineStartOffset in text.indices) {
                                                val lineText = text.substring(lineStartOffset)
                                                if ((lineText.startsWith("☐") || lineText.startsWith("☑")) && offset.x < 120f) {
                                                    onToggleCheckboxAtLine.invoke(lineIdx)
                                                    return@awaitEachGesture
                                                }
                                            }
                                        }
                                        onClick?.invoke()
                                    }
                                }
                            }
                        }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            RichEditText(ctx).apply {
                                val spannable = annotatedStringToSpannable(textFieldValue.annotatedString, ctx)
                                setText(spannable)
                                setHint(placeholder.ifEmpty { "Нажмите, чтобы начать редактирование..." })
                                setTextColor(android.graphics.Color.WHITE)
                                setHintTextColor(android.graphics.Color.GRAY)
                                background = null
                                textSize = 15f
                                setPadding(0, 0, 0, 0)
                                isFocusable = !readOnly
                                isFocusableInTouchMode = !readOnly
                                this.onImageReceived = { uri ->
                                    onImageReceived?.invoke(uri)
                                }
                                this.onSelectionChangedCallback = { start, end ->
                                    if (this.hasFocus()) {
                                        onSelectionChanged?.invoke(TextRange(start, end))
                                    }
                                }
                                addTextChangedListener(object : android.text.TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                        val newStr = s?.toString() ?: ""
                                        if (newStr != textFieldValue.text) {
                                            isUserEditing = true
                                            val selStart = selectionStart
                                            val selEnd = selectionEnd
                                            val newTfv = TextFieldValue(
                                                text = newStr,
                                                selection = TextRange(selStart.coerceIn(0, newStr.length), selEnd.coerceIn(0, newStr.length))
                                            )
                                            onValueChange(newTfv)
                                        }
                                    }
                                    override fun afterTextChanged(s: android.text.Editable?) {}
                                })
                            }
                        },
                        update = { view ->
                            view.isFocusable = !readOnly
                            view.isFocusableInTouchMode = !readOnly

                            if (isUserEditing) {
                                lastAnnotatedString = textFieldValue.annotatedString
                                isUserEditing = false
                            } else {
                                val spansChanged = textFieldValue.annotatedString != lastAnnotatedString
                                val externalTextChanged = view.text.toString() != textFieldValue.text

                                if (spansChanged || externalTextChanged) {
                                    lastAnnotatedString = textFieldValue.annotatedString
                                    val spannable = annotatedStringToSpannable(textFieldValue.annotatedString, view.context)
                                    val selStart = textFieldValue.selection.start
                                    val selEnd = textFieldValue.selection.end
                                    view.setText(spannable)
                                    if (selStart >= 0 && selEnd >= 0) {
                                        view.setSelection(
                                            selStart.coerceIn(0, spannable.length),
                                            selEnd.coerceIn(0, spannable.length)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (readOnly) {
                SelectionContainer {
                    content()
                }
            } else {
                content()
            }
        }
    }
}


