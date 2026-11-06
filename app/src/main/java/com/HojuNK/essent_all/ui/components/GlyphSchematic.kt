package com.HojuNK.essent_all.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.util.GlyphLayout
import com.HojuNK.essent_all.util.GlyphLed
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun GlyphSchematic(
    modifier: Modifier = Modifier,
    activeLeds: Set<Int> = emptySet(),
    onLedsInteraction: ((Set<Int>, Boolean) -> Unit)? = null, // Set of IDs, true to add, false to remove
    highlightLed: Int? = null,
    waveCenter: Offset? = null,
    waveProgress: Float = 0f,
    waveCount: Int = 3,
    waveDistance: Float = 0.2f
) {
    var interactionState by remember { mutableStateOf<Boolean?>(null) } // null = idle, true = turning on, false = turning off
    val interactedThisGesture = remember { mutableStateOf(mutableSetOf<Int>()) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Black)
            .pointerInput(onLedsInteraction, activeLeds) {
                if (onLedsInteraction == null) return@pointerInput
                
                detectDragGestures(
                    onDragStart = { offset ->
                        interactedThisGesture.value.clear()
                        val ledId = findNearestLed(offset, size.width.toFloat(), size.height.toFloat())
                        if (ledId != null) {
                            val newState = !activeLeds.contains(ledId)
                            interactionState = newState
                            interactedThisGesture.value.add(ledId)
                            onLedsInteraction(setOf(ledId), newState)
                        }
                    },
                    onDragEnd = { interactionState = null },
                    onDragCancel = { interactionState = null },
                    onDrag = { change, _ ->
                        val state = interactionState ?: return@detectDragGestures
                        val ledId = findNearestLed(change.position, size.width.toFloat(), size.height.toFloat())
                        if (ledId != null && !interactedThisGesture.value.contains(ledId)) {
                            interactedThisGesture.value.add(ledId)
                            onLedsInteraction(setOf(ledId), state)
                        }
                    }
                )
            }
            .pointerInput(onLedsInteraction, activeLeds) {
                if (onLedsInteraction == null) return@pointerInput
                detectTapGestures { offset ->
                    val ledId = findNearestLed(offset, size.width.toFloat(), size.height.toFloat())
                    if (ledId != null) {
                        val newState = !activeLeds.contains(ledId)
                        onLedsInteraction(setOf(ledId), newState)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Подложка (тусклые контуры)
            GlyphLayout.leds.forEach { led ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = 4.dp.toPx(),
                    center = Offset(led.pos.x * w, led.pos.y * h)
                )
            }

            // 2. Активные и эффекты
            GlyphLayout.leds.forEach { led ->
                val pos = Offset(led.pos.x * w, led.pos.y * h)
                val isActive = activeLeds.contains(led.id)
                val isHighlighted = highlightLed == led.id

                var waveAlpha = 0f
                if (waveCenter != null) {
                    val dist = sqrt((led.pos.x - waveCenter.x)*(led.pos.x - waveCenter.x) + (led.pos.y - waveCenter.y)*(led.pos.y - waveCenter.y))
                    for (i in 0 until waveCount) {
                        val wp = waveProgress - (i * waveDistance)
                        if (wp > 0) {
                            val diff = Math.abs(dist - wp)
                            if (diff < 0.06f) {
                                waveAlpha = Math.max(waveAlpha, (1f - diff / 0.06f) * (1f - wp))
                            }
                        }
                    }
                }

                if (isActive || isHighlighted) {
                    // Просто белый круг для активных, без желтого свечения
                    drawCircle(
                        color = Color.White,
                        radius = 6.5.dp.toPx(),
                        center = pos
                    )
                } else if (waveAlpha > 0.05f) {
                    // Для волны оставляем золотистое свечение
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = waveAlpha * 0.4f),
                        radius = 14.dp.toPx(),
                        center = pos
                    )
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = waveAlpha),
                        radius = 6.5.dp.toPx(),
                        center = pos
                    )
                }
            }
        }
    }
}

private fun findNearestLed(offset: Offset, width: Float, height: Float): Int? {
    val nx = offset.x / width
    val ny = offset.y / height
    var bestLed: Int? = null
    var minDist = 0.1f
    
    GlyphLayout.leds.forEach { led ->
        val d = sqrt((led.pos.x - nx)*(led.pos.x - nx) + (led.pos.y - ny)*(led.pos.y - ny))
        if (d < minDist) {
            minDist = d
            bestLed = led.id
        }
    }
    return bestLed
}

@Composable
fun GlyphDrawPicker(
    initialSelected: Set<Int>,
    initialDuration: Int = 1000,
    initialFade: Boolean = false,
    initialFadeSpeed: Float = 1.0f,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>, Int, Boolean, Float) -> Unit
) {
    var selected by remember { mutableStateOf(initialSelected) }
    var duration by remember { mutableIntStateOf(initialDuration) }
    var fade by remember { mutableStateOf(initialFade) }
    var fadeSpeed by remember { mutableFloatStateOf(initialFadeSpeed) }
    
    var step by remember { mutableIntStateOf(0) } // 0: Draw, 1: Settings

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (step == 0) stringResource(R.string.glyph_draw_title) else stringResource(R.string.macro_setup_title),
                    style = MaterialTheme.typography.titleLarge, 
                    color = Color.White
                )
                Spacer(Modifier.height(20.dp))
                
                if (step == 0) {
                    GlyphSchematic(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        activeLeds = selected,
                        onLedsInteraction = { ids, shouldAdd ->
                            selected = if (shouldAdd) selected + ids else selected - ids
                        }
                    )
                } else {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("${stringResource(R.string.action_delay)}: $duration ms", color = Color.Gray)
                        Slider(
                            value = duration.toFloat(),
                            onValueChange = { duration = it.toInt() },
                            valueRange = 100f..5000f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700))
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.glyph_fade_out), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(checked = fade, onCheckedChange = { fade = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                        }

                        if (fade) {
                            Text("${stringResource(R.string.glyph_fade_speed)}: ${String.format(Locale.US, "%.1f", fadeSpeed)}x", color = Color.Gray)
                            Slider(
                                value = fadeSpeed,
                                onValueChange = { fadeSpeed = it },
                                valueRange = 0.5f..3.0f,
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700))
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) }
                    Button(
                        onClick = { 
                            if (step == 0) step = 1
                            else onConfirm(selected, duration, fade, fadeSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) { Text(if (step == 0) "OK" else stringResource(R.string.save)) }
                }
            }
        }
    }
}

@Composable
fun GlyphWavePicker(
    initialStartLed: Int,
    initialCount: Int,
    initialThickness: Float,
    initialDistance: Float,
    initialSpeed: Float,
    initialAllZones: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Float, Float, Float, Boolean) -> Unit
) {
    var startLed by remember { mutableIntStateOf(initialStartLed) }
    var count by remember { mutableIntStateOf(initialCount) }
    val thickness by remember { mutableFloatStateOf(initialThickness) }
    var distance by remember { mutableFloatStateOf(initialDistance) }
    var speed by remember { mutableFloatStateOf(initialSpeed) }
    var allZones by remember { mutableStateOf(initialAllZones) }

    var step by remember { mutableIntStateOf(0) }

    val waveCenter = remember(startLed) { GlyphLayout.leds.find { it.id == startLed }?.pos ?: Offset(0.5f, 0.5f) }
    val infiniteTransition = rememberInfiniteTransition(label = "WavePreview")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween((1500 / speed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveProgress"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (step == 0) stringResource(R.string.glyph_select_start_led) else stringResource(R.string.glyph_wave_settings),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(20.dp))

                if (step == 0) {
                    GlyphSchematic(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        highlightLed = startLed,
                        onLedsInteraction = { ids, _ -> startLed = ids.first() },
                        waveCenter = waveCenter,
                        waveProgress = waveProgress,
                        waveCount = count,
                        waveDistance = distance
                    )
                } else {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${stringResource(R.string.glyph_wave_count)}: $count", color = Color.Gray)
                        Slider(value = count.toFloat(), onValueChange = { count = it.toInt() }, valueRange = 1f..10f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))

                        Text("${stringResource(R.string.glyph_speed_label)}: ${String.format(java.util.Locale.US, "%.1f", speed)}x", color = Color.Gray)
                        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..3f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))

                        Text("${stringResource(R.string.glyph_wave_distance)}: ${String.format(java.util.Locale.US, "%.2f", distance)}", color = Color.Gray)
                        Slider(value = distance, onValueChange = { distance = it }, valueRange = 0.05f..0.5f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.glyph_wave_scope), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(checked = allZones, onCheckedChange = { allZones = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                        }
                        Text(if (allZones) stringResource(R.string.glyph_wave_scope_all) else stringResource(R.string.glyph_wave_scope_one), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) }
                    Button(
                        onClick = {
                            if (step == 0) step = 1
                            else onConfirm(startLed, count, thickness, distance, speed, allZones)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) { Text(if (step == 0) "OK" else "SAVE") }
                }
            }
        }
    }
}

@Composable
fun GlyphRunnerPicker(
    initialStartLed: Int,
    initialSpeed: Float,
    initialReverse: Boolean,
    initialTail: Boolean = false,
    initialTailLen: Int = 3,
    initialFadeSpeed: Float = 1.0f,
    onDismiss: () -> Unit,
    onConfirm: (Int, Float, Boolean, Boolean, Int, Float) -> Unit
) {
    var startLed by remember { mutableIntStateOf(initialStartLed) }
    var speed by remember { mutableFloatStateOf(initialSpeed) }
    var reverse by remember { mutableStateOf(initialReverse) }
    var tail by remember { mutableStateOf(initialTail) }
    var tailLen by remember { mutableIntStateOf(initialTailLen) }
    var fadeSpeed by remember { mutableFloatStateOf(initialFadeSpeed) }
    
    var step by remember { mutableIntStateOf(0) } // 0: Select Start LED, 1: Settings

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (step == 0) "Выбор точки старта" else stringResource(R.string.glyph_action_runner),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(20.dp))

                if (step == 0) {
                    GlyphSchematic(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        highlightLed = startLed,
                        onLedsInteraction = { ids, _ -> startLed = ids.first() }
                    )
                } else {
                    Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${stringResource(R.string.glyph_speed_label)}: ${String.format(Locale.US, "%.1f", speed)}x", color = Color.Gray)
                        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..5f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.glyph_reverse_label), color = Color.White, modifier = Modifier.weight(1f))
                            Switch(checked = reverse, onCheckedChange = { reverse = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Шлейф", color = Color.White, modifier = Modifier.weight(1f))
                            Switch(checked = tail, onCheckedChange = { tail = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFFD700)))
                        }

                        if (tail) {
                            Text("Длина шлейфа: $tailLen", color = Color.Gray)
                            Slider(value = tailLen.toFloat(), onValueChange = { tailLen = it.toInt() }, valueRange = 1f..10f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))

                            Text("Скорость исчезновения: ${String.format(Locale.US, "%.1f", fadeSpeed)}x", color = Color.Gray)
                            Slider(value = fadeSpeed, onValueChange = { fadeSpeed = it }, valueRange = 0.5f..3f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) }
                    Button(
                        onClick = { 
                            if (step == 0) step = 1
                            else onConfirm(startLed, speed, reverse, tail, tailLen, fadeSpeed)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) { Text(if (step == 0) "OK" else stringResource(R.string.save)) }
                }
            }
        }
    }
}

@Composable
fun GlyphStaticPicker(
    initialAreas: Set<Int>,
    initialTime: Int,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>, Int) -> Unit
) {
    var selectedZones by remember { mutableStateOf(initialAreas) }
    var time by remember { mutableIntStateOf(initialTime) }

    val activeLeds = remember(selectedZones) {
        GlyphLayout.leds.filter { it.zone in selectedZones }.map { it.id }.toSet()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1A1A)
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.glyph_action_static), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(20.dp))

                GlyphSchematic(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    activeLeds = activeLeds,
                    onLedsInteraction = { ids, _ ->
                        val clickedZones = GlyphLayout.leds.filter { it.id in ids }.map { it.zone }.distinct()
                        val newZones = selectedZones.toMutableSet()
                        clickedZones.forEach { z ->
                            if (newZones.contains(z)) newZones.remove(z) else newZones.add(z)
                        }
                        selectedZones = newZones
                    }
                )

                Spacer(Modifier.height(20.dp))

                Text("${stringResource(R.string.action_delay)}: $time ms", color = Color.Gray, modifier = Modifier.fillMaxWidth())
                Slider(value = time.toFloat(), onValueChange = { time = it.toInt() }, valueRange = 100f..5000f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700)))

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Gray) }
                    Button(
                        onClick = { onConfirm(selectedZones, time) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}
