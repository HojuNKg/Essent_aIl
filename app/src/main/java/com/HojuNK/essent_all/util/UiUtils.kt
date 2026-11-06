package com.HojuNK.essent_all.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.core.graphics.drawable.toBitmap
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.model.MacroActionTime
import java.util.UUID

private const val NOTHING_NOISE_AGSL = """
    uniform shader content;
    uniform float amount;
    uniform float seed;
    float random(vec2 uv) {
        return fract(sin(dot(uv.xy, vec2(12.9898, 78.233))) * 43758.5453123);
    }
    vec4 main(vec2 fragCoord) {
        vec4 color = content.eval(fragCoord);
        float n = (random(fragCoord + seed) - 0.5) * amount;
        return vec4(color.rgb + n, color.a);
    }
"""

private const val NOTHING_WAVE_AGSL = """
    uniform shader content;
    uniform float amount;
    uniform float seed;
    uniform float progress;
    uniform vec2 center;
    uniform vec2 layerOffset;
    uniform float rotation;
    uniform vec2 pivot;
    uniform float maxDist;
    
    float random(vec2 uv) {
        return fract(sin(dot(uv.xy, vec2(12.9898, 78.233))) * 43758.5453123);
    }
    
    vec4 main(vec2 fragCoord) {
        vec4 color = content.eval(fragCoord);
        
        // Map local coord to global screen space accounting for rotation
        vec2 p = fragCoord - pivot;
        float s = sin(rotation);
        float c = cos(rotation);
        vec2 rotated = vec2(p.x * c - p.y * s, p.x * s + p.y * c);
        vec2 globalCoord = rotated + pivot + layerOffset;
        
        float d = distance(globalCoord, center);
        float wavePos = progress * maxDist;
        
        // Noise
        float n = (random(fragCoord + seed) - 0.5) * amount;
        
        // Subtle wave highlight (ring)
        float thickness = 180.0;
        float ring = smoothstep(wavePos - thickness, wavePos, d) * smoothstep(wavePos + thickness, wavePos, d);
        
        return vec4(color.rgb + n + (ring * 0.08), color.a);
    }
"""

fun createNothingBlurEffect(radius: Float, noiseAmount: Float = 0.12f): android.graphics.RenderEffect {
    val blurRadius = radius.coerceAtLeast(0.1f)
    val blur = android.graphics.RenderEffect.createBlurEffect(
        blurRadius, blurRadius, android.graphics.Shader.TileMode.DECAL
    )
    
    val noiseShader = android.graphics.RuntimeShader(NOTHING_NOISE_AGSL)
    noiseShader.setFloatUniform("amount", noiseAmount)
    noiseShader.setFloatUniform("seed", (System.currentTimeMillis() % 1000).toFloat())
    
    val noiseEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(noiseShader, "content")
    return android.graphics.RenderEffect.createChainEffect(noiseEffect, blur)
}

fun createNothingWaveEffect(
    radius: Float,
    progress: Float,
    center: Offset,
    maxDist: Float,
    layerOffset: Offset = Offset.Zero,
    rotationRad: Float = 0f,
    pivot: Offset = Offset.Zero,
    noiseAmount: Float = 0.12f
): android.graphics.RenderEffect {
    val blurRadius = radius.coerceAtLeast(0.1f)
    val blur = android.graphics.RenderEffect.createBlurEffect(
        blurRadius, blurRadius, android.graphics.Shader.TileMode.DECAL
    )
    
    val waveShader = android.graphics.RuntimeShader(NOTHING_WAVE_AGSL)
    waveShader.setFloatUniform("amount", noiseAmount)
    waveShader.setFloatUniform("seed", (System.currentTimeMillis() % 1000).toFloat())
    waveShader.setFloatUniform("progress", progress)
    waveShader.setFloatUniform("center", center.x, center.y)
    waveShader.setFloatUniform("layerOffset", layerOffset.x, layerOffset.y)
    waveShader.setFloatUniform("rotation", rotationRad)
    waveShader.setFloatUniform("pivot", pivot.x, pivot.y)
    waveShader.setFloatUniform("maxDist", maxDist)
    
    val waveEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(waveShader, "content")
    return android.graphics.RenderEffect.createChainEffect(waveEffect, blur)
}

fun Modifier.nothingBlur(radius: Float = 45f, noiseAmount: Float = 0.12f, enabled: Boolean = true): Modifier = this.graphicsLayer {
    if (enabled) {
        renderEffect = createNothingBlurEffect(radius, noiseAmount).asComposeRenderEffect()
        alpha = 0.6f
    } else {
        renderEffect = null
        alpha = 1.0f
    }
}

fun Modifier.nothingNoise(amount: Float = 0.08f): Modifier = this.graphicsLayer {
    val noiseShader = android.graphics.RuntimeShader(NOTHING_NOISE_AGSL)
    noiseShader.setFloatUniform("amount", amount)
    noiseShader.setFloatUniform("seed", (System.currentTimeMillis() % 1000).toFloat())
    renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(noiseShader, "content").asComposeRenderEffect()
}

fun openAccessibilitySettings(ctx: Context) {
    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
    val expectedId = ComponentName(context, service).flattenToString()
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        if (colonSplitter.next().equals(expectedId, ignoreCase = true)) return true
    }
    return false
}

fun showAccessibilityNotification(context: Context) {
    val channelId = "accessibility_check"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    val channel = NotificationChannel(channelId, context.getString(R.string.accessibility_setup_title), NotificationManager.IMPORTANCE_HIGH)
    notificationManager.createNotificationChannel(channel)
    
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(context.getString(R.string.accessibility_permission_required))
        .setContentText(context.getString(R.string.accessibility_enable_msg))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
        
    notificationManager.notify(1001, notification)
}

fun AppLanguage.getLabel(ctx: Context): String = when (this) {
    AppLanguage.SYSTEM -> ctx.getString(R.string.lang_system)
    AppLanguage.ENGLISH -> ctx.getString(R.string.lang_english)
    AppLanguage.RUSSIAN -> ctx.getString(R.string.lang_russian)
}

fun slotDisplay(ctx: Context, value: String?, events: List<EventRule>, macros: List<Macro>, apps: List<AppInfo>?, glyphPatterns: List<Macro> = emptyList()): String = when {
    value == null -> ctx.getString(R.string.default_action)
    value.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
        val coords = value.removePrefix(Config.ACTION_TOUCH_PREFIX).replace("_", ", ")
        "${ctx.getString(R.string.action_touch)} ($coords)"
    }
    value.startsWith(Config.ACTION_PATH_PREFIX) -> {
        val pts = value.removePrefix(Config.ACTION_PATH_PREFIX).split(";").filter { it.isNotEmpty() }
        if (pts.size >= 2) {
            val first = pts.first().split(",")
            val last = pts.last().split(",")
            if (first.size >= 2 && last.size >= 2) {
                "${ctx.getString(R.string.action_swipe_precise)} (${first[0]},${first[1]} -> ${last[0]},${last[1]})"
            } else ctx.getString(R.string.action_swipe_precise)
        } else ctx.getString(R.string.action_swipe_precise)
    }
    value.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
        val parts = value.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
        if (parts.size >= 4) {
            "${ctx.getString(R.string.action_swipe)} (${parts[0]},${parts[1]} -> ${parts[2]},${parts[3]})"
        } else ctx.getString(R.string.action_swipe)
    }
    value.startsWith(Config.ACTION_DELAY_PREFIX) -> {
        val sec = value.removePrefix(Config.ACTION_DELAY_PREFIX)
        "${ctx.getString(R.string.action_delay)} ($sec ${ctx.getString(R.string.unit_seconds)})"
    }
    value.startsWith(Config.ACTION_BRIGHTNESS_PREFIX) -> {
        val pct = value.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)
        "${ctx.getString(R.string.action_brightness)}: $pct%"
    }
    value == Config.ACTION_FAVORITES_PREFIX -> ctx.getString(R.string.favorites)
    value == Config.ACTION_CONTROLS -> ctx.getString(R.string.control_panel)
    value == Config.ACTION_HOME -> ctx.getString(R.string.action_home)
    value == Config.ACTION_NOTIFICATIONS -> ctx.getString(R.string.action_notifications)
    value == Config.ACTION_FLASHLIGHT || value.startsWith(Config.ACTION_FLASHLIGHT_PREFIX) -> {
        val mode = if (value.startsWith(Config.ACTION_FLASHLIGHT_PREFIX)) {
            when (value.removePrefix(Config.ACTION_FLASHLIGHT_PREFIX)) {
                "on" -> "вкл"
                "off" -> "выкл"
                else -> "переключить"
            }
        } else "переключить"
        "${ctx.getString(R.string.action_flashlight_toggle)} ($mode)"
    }
    value == Config.ACTION_GLYPH || value == Config.ACTION_GLYPH_TOGGLE || value.startsWith(Config.ACTION_GLYPH_PREFIX) -> {
        val mode = if (value.startsWith(Config.ACTION_GLYPH_PREFIX)) {
            when (value.removePrefix(Config.ACTION_GLYPH_PREFIX)) {
                "on" -> "вкл"
                "off" -> "выкл"
                else -> "переключить"
            }
        } else "переключить"
        "${ctx.getString(R.string.action_glyph_toggle)} ($mode)"
    }
    value == Config.ACTION_ROTATION_TOGGLE -> ctx.getString(R.string.action_rotation_lock_title)
    value == Config.ACTION_RING_VIBRATE -> ctx.getString(R.string.action_ring_vibrate_title)
    value == Config.ACTION_VOICE_ASSISTANT -> ctx.getString(R.string.action_voice_assistant_title)
    value == Config.ACTION_WAKE_SCREEN -> ctx.getString(R.string.action_wake_screen_title)
    value == Config.ACTION_POWER_MENU -> ctx.getString(R.string.action_power_menu_title)
    value == Config.ACTION_SET_TIMER -> ctx.getString(R.string.action_set_a_timer_title)
    value == Config.ACTION_EXTRA_DIM -> ctx.getString(R.string.action_extra_dim_title)
    value == Config.ACTION_QUICK_SETTINGS -> ctx.getString(R.string.action_quick_settings_title)
    value == Config.ACTION_WIFI_TOGGLE -> ctx.getString(R.string.action_internet_title)
    value == Config.ACTION_BLUETOOTH_TOGGLE -> ctx.getString(R.string.action_bluetooth_title)
    value == Config.ACTION_RECENTS -> ctx.getString(R.string.action_recent_apps_title)
    value == Config.ACTION_CALCULATOR -> ctx.getString(R.string.action_calculator_title)
    value == Config.ACTION_OPEN_CAMERA -> ctx.getString(R.string.action_open_camera_title)
    value == Config.ACTION_SCREEN_RECORD -> ctx.getString(R.string.action_screen_recording_title)
    value == Config.ACTION_CAMERA_SHUTTER -> ctx.getString(R.string.action_camera_shutter_title)
    value == "action:system:media:play_pause" -> ctx.getString(R.string.action_play_pause_title)
    value == "action:system:media:next" -> ctx.getString(R.string.action_next_track_title)
    value == "action:system:media:prev" -> ctx.getString(R.string.action_previous_track_title)
    value == "action:system:media:mute" -> ctx.getString(R.string.action_mute_media_title)
    value.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_AUTO_BRIGHTNESS_PREFIX)
        "${ctx.getString(R.string.action_auto_brightness)} ($data)"
    }
    value == Config.ACTION_SCREENSHOT -> ctx.getString(R.string.action_screenshot)
    value == Config.ACTION_LOCK_SCREEN -> ctx.getString(R.string.action_lock_screen)
    value == Config.ACTION_SPLIT_SCREEN -> ctx.getString(R.string.action_split_screen)
    value == Config.ACTION_DND_TOGGLE -> ctx.getString(R.string.control_dnd)
    value == Config.ACTION_CLEAR_NOTIFICATIONS -> ctx.getString(R.string.action_clear_notifications)
    value.startsWith(Config.ACTION_VOLUME_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_VOLUME_PREFIX)
        if (data.contains("set:")) {
            val stream = data.substringBefore(":")
            val pct = data.substringAfter("set:")
            "${ctx.getString(R.string.action_volume)} ($stream: $pct%)"
        } else {
            "${ctx.getString(R.string.action_volume)} ($data)"
        }
    }
    value.startsWith(Config.ACTION_MEDIA_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_MEDIA_PREFIX)
        "${ctx.getString(R.string.action_media)} ($data)"
    }
    value.startsWith(Config.ACTION_URL_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_URL_PREFIX)
        "${ctx.getString(R.string.action_url)}: $data"
    }
    value.startsWith(Config.ACTION_THEME_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_THEME_PREFIX)
        "${ctx.getString(R.string.action_theme)} ($data)"
    }
    value.startsWith(Config.ACTION_MACRO_PREFIX) -> {
        val id = value.removePrefix(Config.ACTION_MACRO_PREFIX)
        val macro = macros.find { it.id == id }
        if (macro != null) "${ctx.getString(R.string.action_macro)}: ${macro.title}" else "${ctx.getString(R.string.action_macro)}: (${ctx.getString(R.string.action_macro_not_selected)})"
    }
    value.startsWith(Config.ACTION_GLYPH_PATTERN_PREFIX) -> {
        val id = value.removePrefix(Config.ACTION_GLYPH_PATTERN_PREFIX)
        val pattern = glyphPatterns.find { it.id == id }
        if (pattern != null) "${ctx.getString(R.string.action_play_glyph_pattern)}: ${pattern.title}" else "${ctx.getString(R.string.action_play_glyph_pattern)}: ($id)"
    }
    value.startsWith(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX) -> {
        val parts = value.removePrefix(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX).split(":")
        val startLedId = parts.getOrNull(0) ?: "20"
        val speed = parts.getOrNull(1) ?: "1.0"
        val reverse = parts.getOrNull(2)?.toBoolean() ?: false
        val tail = parts.getOrNull(3)?.toBoolean() ?: false
        
        val led = GlyphLayout.leds.find { it.id == startLedId.toIntOrNull() }
        val areaText = when(led?.zone) {
            1 -> ctx.getString(R.string.glyph_area_1)
            2 -> ctx.getString(R.string.glyph_area_2)
            3 -> ctx.getString(R.string.glyph_area_3)
            else -> "???"
        }
        
        val revText = if (reverse) ", ${ctx.getString(R.string.glyph_reverse_label)}" else ""
        val tailText = if (tail) ", шлейф" else ""
        "${ctx.getString(R.string.glyph_action_runner)} ($areaText, ${speed}x$revText$tailText)"
    }
    value.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX) -> {
        val parts = value.removePrefix(Config.ACTION_GLYPH_STATIC_PREFIX).split(":")
        val areasStr = parts.getOrNull(0) ?: "1"
        val ms = parts.getOrNull(1) ?: "500"
        val areas = areasStr.split(",")
        val areaTexts = areas.map { a ->
            when(a) {
                "1" -> ctx.getString(R.string.glyph_area_1)
                "2" -> ctx.getString(R.string.glyph_area_2)
                "3" -> ctx.getString(R.string.glyph_area_3)
                else -> "Area $a"
            }
        }
        val combinedText = areaTexts.joinToString(", ")
        "${ctx.getString(R.string.glyph_action_static)} ($combinedText, $ms ms)"
    }
    value.startsWith(Config.ACTION_GLYPH_WAVE_PREFIX) -> {
        "Волна"
    }
    value.startsWith(Config.ACTION_GLYPH_DRAW_PREFIX) -> {
        val data = value.removePrefix(Config.ACTION_GLYPH_DRAW_PREFIX)
        val parts = data.split(":")
        val hasSettings = parts.size >= 4
        val duration = if (hasSettings) parts[0] else "1000"
        val fade = if (hasSettings) parts[1].toBoolean() else false
        val fadeText = if (fade) ", затухание" else ""
        "${ctx.getString(R.string.glyph_action_static).replace("Статичный", "Рисунок")} ($duration ms$fadeText)"
    }
    value.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX) -> ctx.getString(R.string.action_wait_confirmation)
    value == Config.ACTION_KILL_ESSENTIAL -> ctx.getString(R.string.kill_essential_space)
    value == Config.ACTION_KILL_NOTES -> ctx.getString(R.string.kill_notes)
    else -> apps?.find { it.pkg == value }?.label ?: value
}

@Composable
fun EventIconDisplay(
    icon: String,
    displaySize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    grayscale: Boolean = false
) {
    val ctx = LocalContext.current
    var appIcon by remember(icon) { mutableStateOf<ImageBitmap?>(null) }

    // Check if it's a material icon
    val materialIcon = remember(icon) {
        if (icon.startsWith("material:")) {
            val name = icon.removePrefix("material:")
            allMaterialIcons.find { it.first == name }?.second
        } else null
    }

    LaunchedEffect(icon) {
        if (icon.contains(".") && !icon.startsWith("material:")) {
            try {
                val pm = ctx.packageManager
                appIcon = pm.getApplicationIcon(icon).toBitmap(256, 256).asImageBitmap()
            } catch (e: Exception) { appIcon = null }
        } else {
            appIcon = null
        }
    }

    // Powerful Grayscale: -100% saturation for everything (icons, symbols, emojis)
    val filterModifier = if (grayscale) {
        val matrix = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
        Modifier
            .graphicsLayer { alpha = 0.35f } // Even darker
            .drawWithCache {
                val paint = androidx.compose.ui.graphics.Paint().apply {
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(matrix)
                }
                onDrawWithContent {
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
                        drawContent()
                        canvas.restore()
                    }
                }
            }
    } else Modifier

    val finalModifier = if (modifier == Modifier) Modifier.size(displaySize) else modifier

    BoxWithConstraints(modifier = finalModifier.then(filterModifier), contentAlignment = Alignment.Center) {
        if (materialIcon != null) {
            Icon(
                imageVector = materialIcon,
                contentDescription = null,
                tint = if (grayscale) Color.Gray else Color.White,
                modifier = Modifier.fillMaxSize()
            )
        } else if (appIcon != null) {
            Image(
                bitmap = appIcon!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val effectiveSize = if (modifier == Modifier) displaySize else this.maxWidth
            Text(
                icon,
                fontSize = (effectiveSize.value * 0.75f).sp,
                color = if (grayscale) Color.Gray else Color.White
            )
        }
    }
}

fun calculateMacroTimeline(actions: List<String>, ids: List<String>? = null): List<MacroActionTime> {
    val timeline = mutableListOf<MacroActionTime>()
    var currentTime = 0L
    val gap = 150L

    actions.forEachIndexed { index, action ->
        val duration = when {
            action.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                val parts = action.removePrefix(Config.ACTION_TOUCH_PREFIX).split("_")
                if (parts.size >= 3) (parts[2].toLongOrNull() ?: 200L).coerceAtLeast(50L) else 200L
            }
            action.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                val parts = action.removePrefix(Config.ACTION_SWIPE_PREFIX).split("_")
                if (parts.size >= 7) (parts[6].toLongOrNull() ?: 350L).coerceAtLeast(100L) else 350L
            }
            action.startsWith(Config.ACTION_PATH_PREFIX) -> {
                val pathData = action.removePrefix(Config.ACTION_PATH_PREFIX)
                val pts = pathData.split(";").filter { it.isNotEmpty() }
                if (pts.isNotEmpty()) {
                    val lastTime = pts.last().split(",").getOrNull(2)?.toLongOrNull() ?: 0L
                    if (lastTime <= 0) (pts.size * 30L).coerceIn(200L, 2000L) else lastTime
                } else 500L
            }
            action.startsWith(Config.ACTION_DELAY_PREFIX) -> {
                val sec = action.removePrefix(Config.ACTION_DELAY_PREFIX).toFloatOrNull() ?: 0f
                (sec * 1000).toLong().coerceAtLeast(0L)
            }
            else -> 200L
        }

        // Use provided stable ID or fallback to positional ID
        val stableId = ids?.getOrNull(index) ?: "${index}_${action.hashCode()}"
        timeline.add(MacroActionTime(stableId, index, action, currentTime, duration))
        currentTime += duration + gap
    }
    return timeline
}

fun Offset.getDistance() = kotlin.math.sqrt(x * x + y * y)
operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)

fun Modifier.aspectOf(ratio: Float, scale: Float = 1f): Modifier = layout { measurable, constraints ->
    val h = constraints.maxHeight
    val targetHeight = (h * scale).toInt()
    val targetWidth = (targetHeight * ratio).toInt()
    val placeable = measurable.measure(Constraints.fixed(targetWidth, targetHeight))
    layout(targetWidth, targetHeight) {
        placeable.placeRelative(0, 0)
    }
}
