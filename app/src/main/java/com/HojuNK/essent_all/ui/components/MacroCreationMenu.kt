package com.HojuNK.essent_all.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.model.LocalRadialMenu
import com.HojuNK.essent_all.util.createNothingBlurEffect
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.model.RadialMenuState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MacroCreationMenu(
    state: RadialMenuState,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val setRadialMenu = LocalRadialMenu.current

    val currentState by rememberUpdatedState(state)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        startAnim = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val isReleased = event.type == PointerEventType.Release || event.changes.all { !it.pressed }
                        val change = event.changes.firstOrNull()

                        if (change != null) {
                            val pos = change.position
                            val bestIndex = calculateRectangularRadialHover(
                                currentPos = pos,
                                menuCenter = currentState.center,
                                itemCount = currentState.actions.size,
                                density = density,
                                screenWidthPx = screenWidthPx,
                                screenHeightPx = screenHeightPx
                            )
                            
                            if (bestIndex != currentState.hoveredIndex) {
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
                }
            }
    ) {
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

        val actions = state.actions
        val center = state.center
        
        // Layout Config
        val itemHeight = 56.dp
        val radiusBase = 95.dp // Slightly closer to FAB

        val isRightSide = center.x > screenWidthPx / 2f
        
        // Adjusted angles: more spread to prevent overlap with gap
        val angles = if (isRightSide) {
            listOf(100f, 160f, 200f, 260f)
        } else {
            listOf(100f, 20f, 340f, 260f)
        }

        actions.forEachIndexed { index, action ->
            val isHovered = state.hoveredIndex == index
            val angle = angles.getOrNull(index) ?: 270f
            val angleRad = Math.toRadians(angle.toDouble())
            
            val animatedRadius by animateDpAsState(
                targetValue = if (startAnim) radiusBase else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )

            val ax = (animatedRadius.value * cos(angleRad) * density.density).toFloat()
            val ay = (animatedRadius.value * sin(angleRad) * density.density).toFloat()

            val animatedScale by animateFloatAsState(
                targetValue = if (startAnim) (if (isHovered) 1.25f else 1f) else 0.4f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            
            val animatedAlpha by animateFloatAsState(
                targetValue = if (startAnim) 1f else 0f,
                animationSpec = tween(300, delayMillis = index * 30)
            )

            val bgColor by animateColorAsState(if (isHovered) Color.White else Color(0xFF2A2A2A))
            val contentColor by animateColorAsState(if (isHovered) Color.Black else Color.White)
            val borderColor by animateColorAsState(if (isHovered) Color.White else Color.Gray.copy(alpha = 0.3f))

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (center.x + ax).roundToInt(),
                            (center.y + ay).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                        // Anchor based on side
                        if (isRightSide) {
                            translationX = -size.width
                        } else {
                            translationX = 0f
                        }
                        translationY = -size.height / 2f
                    }
                    .height(itemHeight)
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 115.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(1.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = action.label,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
