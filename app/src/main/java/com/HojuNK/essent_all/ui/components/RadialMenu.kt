package com.HojuNK.essent_all.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.model.LocalRadialMenu
import com.HojuNK.essent_all.model.RadialAction
import com.HojuNK.essent_all.util.createNothingBlurEffect
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.model.RadialMenuState
import com.HojuNK.essent_all.util.getDistance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun calculateRadialHover(
    currentPos: androidx.compose.ui.geometry.Offset,
    menuCenter: androidx.compose.ui.geometry.Offset,
    actions: List<RadialAction>,
    density: Density,
    screenWidthPx: Float,
    screenHeightPx: Float
): Int {
    val dirX = screenWidthPx / 2f - menuCenter.x
    val dirY = screenHeightPx / 2f - menuCenter.y
    val baseAngleDeg = Math.toDegrees(Math.atan2(dirY.toDouble(), dirX.toDouble())).toFloat()

    val radiusPx = with(density) { 100.dp.toPx() }
    val buttonRadiusPx = with(density) { 28.dp.toPx() }
    val triggerRadiusPx = buttonRadiusPx * 1.6f

    actions.forEachIndexed { index, _ ->
        val step = if (actions.size > 1) 140f / (actions.size - 1) else 0f
        val span = (actions.size - 1) * step
        val startAngle = baseAngleDeg - span / 2f
        val angle = startAngle + (index * step)

        val offsetX = (radiusPx * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
        val offsetY = (radiusPx * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()

        val buttonCenter = androidx.compose.ui.geometry.Offset(menuCenter.x + offsetX, menuCenter.y + offsetY)
        if ((currentPos - buttonCenter).getDistance() < triggerRadiusPx) return index
    }
    return -1
}

fun calculateRectangularRadialHover(
    currentPos: androidx.compose.ui.geometry.Offset,
    menuCenter: androidx.compose.ui.geometry.Offset,
    itemCount: Int,
    density: Density,
    screenWidthPx: Float,
    screenHeightPx: Float
): Int {
    val isRightSide = menuCenter.x > screenWidthPx / 2f
    val radiusPx = with(density) { 75.dp.toPx() } // Slightly reduced
    val triggerRadiusPx = with(density) { 55.dp.toPx() }
    val itemWidthPx = with(density) { 115.dp.toPx() }

    // Adjusted angles: more spread to prevent overlap with gap
    val angles = if (isRightSide) {
        listOf(82f, 162f, 198f, 278f)
    } else {
        listOf(98f, 18f, 342f, 262f)
    }

    for (index in 0 until itemCount) {
        val angle = angles.getOrNull(index) ?: 270f
        val angleRad = Math.toRadians(angle.toDouble())
        
        val ax = (radiusPx * cos(angleRad)).toFloat()
        val ay = (radiusPx * sin(angleRad)).toFloat()
        
        val buttonCenterX = if (isRightSide) {
            menuCenter.x + ax - itemWidthPx / 2f
        } else {
            menuCenter.x + ax + itemWidthPx / 2f
        }
        val buttonCenter = androidx.compose.ui.geometry.Offset(buttonCenterX, menuCenter.y + ay)

        if ((currentPos - buttonCenter).getDistance() < triggerRadiusPx) return index
    }
    return -1
}

@Composable
fun RadialActionMenu(state: RadialMenuState, onDismiss: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val setRadialMenu = LocalRadialMenu.current

    val currentState by rememberUpdatedState(state)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var currentHovered = -1
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                            val change = event.changes.firstOrNull()

                            if (change != null) {
                                val bestIndex = calculateRadialHover(
                                    change.position,
                                    currentState.center,
                                    currentState.actions,
                                    density,
                                    screenWidthPx,
                                    screenHeightPx
                                )
                                if (bestIndex != currentHovered) {
                                    currentHovered = bestIndex
                                    setRadialMenu(currentState.copy(hoveredIndex = bestIndex))
                                }

                                if (isReleased) {
                                    if (bestIndex != -1) {
                                        val action = currentState.actions[bestIndex]
                                        currentOnDismiss()
                                        action.onClick()
                                    } else {
                                        currentOnDismiss()
                                    }
                                    break
                                }
                            } else if (isReleased) {
                                currentOnDismiss()
                                break
                            }
                        }
                    } finally {
                        // Cleanup if needed
                    }
                }
            }
    ) {
        val center = state.center
        val actions = state.actions
        val dirX = screenWidthPx / 2f - center.x
        val dirY = screenHeightPx / 2f - center.y
        val baseAngleDeg = Math.toDegrees(Math.atan2(dirY.toDouble(), dirX.toDouble())).toFloat()

        var startAnim by remember { mutableStateOf(false) }
        LaunchedEffect(state.center) { startAnim = true }

        val currentRadius by animateDpAsState(
            targetValue = if (startAnim) 100.dp else 0.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )

        // 1. Global Background Dimming (Blur is handled by MainActivity wave)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))

        // 2. Render the Elevated "Caller" item
        state.sourceBounds?.let { bounds ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
                    .width(with(density) { bounds.width.toDp() })
                    .height(with(density) { bounds.height.toDp() })
            ) {
                state.sourceContent()
            }
        }

        actions.forEachIndexed { index, action ->
            val isHovered = state.hoveredIndex == index
            val step = if (actions.size > 1) 140f / (actions.size - 1) else 0f
            val span = (actions.size - 1) * step
            val startAngle = baseAngleDeg - span / 2f
            val angle = startAngle + (index * step)

            val offsetX = (currentRadius.value * Math.cos(Math.toRadians(angle.toDouble()))).toFloat()
            val offsetY = (currentRadius.value * Math.sin(Math.toRadians(angle.toDouble()))).toFloat()

            val animatedScale by animateFloatAsState(
                targetValue = if (startAnim) (if (isHovered) 1.25f else 1f) else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )

            val bgColor by animateColorAsState(if (isHovered) Color.White else Color(0xFF2A2A2A))
            val iconColor by animateColorAsState(if (isHovered) Color.Black else Color.White)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x + offsetX.dp.toPx()).roundToInt() - 28.dp.toPx().toInt(),
                            (center.y + offsetY.dp.toPx()).roundToInt() - 28.dp.toPx().toInt()
                        )
                    }
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = if (startAnim) 1f else 0f
                    }
                    .background(bgColor, CircleShape)
                    .border(1.dp, if (isHovered) Color.White else Color.Gray.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}
