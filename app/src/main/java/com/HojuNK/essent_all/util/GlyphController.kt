package com.HojuNK.essent_all.util

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import com.HojuNK.essent_all.Config
import com.HojuNK.essent_all.Macro
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

object GlyphController {
    private const val TAG = "GlyphController"
    
    private var glyphManager: GlyphManager? = null
    private var glyphOn = false
    private var glyphRunnerJob: Job? = null
    
    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _glyphActive = MutableStateFlow(false)
    val glyphActive: StateFlow<Boolean> = _glyphActive.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (glyphManager != null) return
        
        Log.d(TAG, "Initializing Nothing SDK...")
        glyphManager = GlyphManager.getInstance(context.applicationContext)
        glyphManager?.init(object : GlyphManager.Callback {
            override fun onServiceConnected(componentName: ComponentName) {
                Log.i(TAG, "GlyphManager service connected!")
                var success = false
                try {
                    success = glyphManager?.register() == true
                    Log.i(TAG, "Parameterless register() result: $success")
                } catch (e: Exception) {
                    Log.d(TAG, "Parameterless register exception: ${e.message}")
                }

                if (!success) {
                    val devices = listOf(
                        Glyph.DEVICE_25111,
                        Glyph.DEVICE_25111p,
                        Glyph.DEVICE_25131,
                        Glyph.DEVICE_24111,
                        Glyph.DEVICE_23111,
                        Glyph.DEVICE_22111,
                        Glyph.DEVICE_20111
                    )
                    for (dev in devices) {
                        try {
                            if (glyphManager?.register(dev) == true) {
                                Log.i(TAG, "Registered glyph device via fallback: $dev")
                                success = true
                                break
                            }
                        } catch (e: Exception) {}
                    }
                }

                try {
                    glyphManager?.openSession()
                    Log.i(TAG, "Glyph session opened successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Glyph session error: ${e.message}")
                }
            }
            override fun onServiceDisconnected(componentName: ComponentName) {
                Log.w(TAG, "GlyphManager service disconnected")
                try { glyphManager?.closeSession() } catch (e: Exception) {}
            }
        })
    }

    private fun updateGlyphStatus(active: Boolean) {
        glyphOn = active
        _glyphActive.value = active || glyphRunnerJob != null
    }

    fun toggleGlyph() {
        stopGlyphRunner()
        val gm = glyphManager
        if (gm == null) {
            Log.e(TAG, "toggleGlyph: glyphManager is null")
            return
        }
        try {
            if (!glyphOn) {
                val builder = gm.getGlyphFrameBuilder()
                if (builder == null) {
                    Log.e(TAG, "toggleGlyph: getGlyphFrameBuilder() returned null (session not ready?)")
                    return
                }
                builder.buildChannelA().buildChannelB().buildChannelC()
                gm.toggle(builder.build())
                updateGlyphStatus(true)
                Log.d(TAG, "toggleGlyph: turned ON")
            } else {
                gm.turnOff()
                updateGlyphStatus(false)
                Log.d(TAG, "toggleGlyph: turned OFF")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle glyph: ${e.message}", e)
        }
    }

    fun turnOnGlyphs() {
        stopGlyphRunner()
        val gm = glyphManager
        if (gm == null) {
            Log.e(TAG, "turnOnGlyphs: glyphManager is null")
            return
        }
        try {
            val builder = gm.getGlyphFrameBuilder()
            if (builder == null) {
                Log.e(TAG, "turnOnGlyphs: getGlyphFrameBuilder() returned null")
                return
            }
            builder.buildChannelA().buildChannelB().buildChannelC()
            gm.toggle(builder.build())
            updateGlyphStatus(true)
            Log.d(TAG, "turnOnGlyphs: success")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn on glyph: ${e.message}", e)
        }
    }

    fun turnOffGlyphs() {
        stopGlyphRunner()
        val gm = glyphManager
        if (gm == null) {
            Log.e(TAG, "turnOffGlyphs: glyphManager is null")
            return
        }
        try {
            gm.turnOff()
            updateGlyphStatus(false)
            Log.d(TAG, "turnOffGlyphs: success")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off glyph: ${e.message}", e)
        }
    }

    fun playGlyphPattern(pattern: Macro) {
        stopGlyphRunner()
        glyphRunnerJob = controllerScope.launch {
            _glyphActive.value = true
            try {
                pattern.actions.forEach { action ->
                    if (action.startsWith(Config.ACTION_DELAY_PREFIX)) {
                        val ms = action.removePrefix(Config.ACTION_DELAY_PREFIX).toLongOrNull() ?: 500L
                        delay(ms)
                    } else if (action.startsWith(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX)) {
                        val parts = action.removePrefix(Config.ACTION_GLYPH_STEP_RUNNER_PREFIX).split(":")
                        // Format: startLedId:speed:reverse:tail:tailLen:fadeSpeed
                        val startLedId = parts.getOrNull(0)?.toIntOrNull() ?: 20
                        val speed = parts.getOrNull(1)?.toFloatOrNull() ?: 1.0f
                        val reverse = parts.getOrNull(2)?.toBoolean() ?: false
                        val tail = parts.getOrNull(3)?.toBoolean() ?: false
                        val tailLen = parts.getOrNull(4)?.toIntOrNull() ?: 3
                        val fadeSpeed = parts.getOrNull(5)?.toFloatOrNull() ?: 1.0f

                        val startLed = GlyphLayout.leds.find { it.id == startLedId } ?: return@forEach
                        
                        var indices = when (startLed.zone) {
                            1 -> (20..30).toList()
                            2 -> (31..35).toList()
                            3 -> (0..19).toList()
                            else -> emptyList()
                        }
                        
                        if (reverse) indices = indices.reversed()
                        
                        val startIndex = indices.indexOf(startLedId).coerceAtLeast(0)
                        val sequence = indices.subList(startIndex, indices.size) + indices.subList(0, startIndex)

                        val stepTime = (150 / speed).toLong()
                        val activeLeds = mutableMapOf<Int, Float>()

                        for (idx in sequence) {
                            activeLeds[idx] = 100f
                            
                            val builder = glyphManager?.getGlyphFrameBuilder() ?: break
                            activeLeds.forEach { (id, intensity) ->
                                builder.buildChannel(id).buildCycles(intensity.toInt())
                            }
                            glyphManager?.toggle(builder.build())
                            
                            delay(stepTime)

                            val it = activeLeds.entries.iterator()
                            while (it.hasNext()) {
                                val entry = it.next()
                                if (entry.key == idx) continue
                                
                                val decay = if (tail) (20f * fadeSpeed) else 100f
                                entry.setValue(entry.value - decay)
                                if (entry.value <= 0) it.remove()
                            }
                            
                            if (tail && activeLeds.size > tailLen + 1) {
                                val oldest = activeLeds.keys.first()
                                activeLeds.remove(oldest)
                            }
                        }
                        glyphManager?.turnOff()
                    } else if (action.startsWith(Config.ACTION_GLYPH_STATIC_PREFIX)) {
                        val parts = action.removePrefix(Config.ACTION_GLYPH_STATIC_PREFIX).split(":")
                        val areasStr = parts.getOrNull(0) ?: "1"
                        val duration = parts.getOrNull(1)?.toLongOrNull() ?: 500L
                        
                        val builder = glyphManager?.getGlyphFrameBuilder() ?: return@forEach
                        val areas = areasStr.split(",").mapNotNull { it.toIntOrNull() }
                        
                        areas.forEach { area ->
                            when (area) {
                                1 -> builder.buildChannelA()
                                2 -> builder.buildChannelB()
                                3 -> builder.buildChannelC()
                            }
                        }
                        glyphManager?.toggle(builder.build())
                        delay(duration)
                        glyphManager?.turnOff()
                    } else if (action.startsWith(Config.ACTION_GLYPH_WAVE_PREFIX)) {
                        val parts = action.removePrefix(Config.ACTION_GLYPH_WAVE_PREFIX).split(":")
                        val startLedId = parts.getOrNull(0)?.toIntOrNull() ?: 20
                        val count = parts.getOrNull(1)?.toIntOrNull() ?: 3
                        val thickness = parts.getOrNull(2)?.toFloatOrNull() ?: 0.1f
                        val distance = parts.getOrNull(3)?.toFloatOrNull() ?: 0.2f
                        val speed = parts.getOrNull(4)?.toFloatOrNull() ?: 1.0f
                        val allZones = parts.getOrNull(5)?.toBoolean() ?: true
                        
                        val startLed = GlyphLayout.leds.find { it.id == startLedId } ?: return@forEach
                        val totalSteps = (1.5f / (0.05f * speed)).toInt()
                        
                        for (step in 0 until totalSteps) {
                            val progress = step * 0.05f * speed
                            val builder = glyphManager?.getGlyphFrameBuilder() ?: break
                            
                            var hasAny = false
                            GlyphLayout.leds.forEach { led ->
                                if (!allZones && led.zone != startLed.zone) return@forEach
                                
                                val dx = led.pos.x - startLed.pos.x
                                val dy = led.pos.y - startLed.pos.y
                                val dist = sqrt(dx * dx + dy * dy)
                                
                                for (i in 0 until count) {
                                    val currentWavePos = progress - (i * distance)
                                    if (currentWavePos > 0) {
                                        val diff = Math.abs(dist - currentWavePos)
                                        if (diff < 0.05f) {
                                            builder.buildChannel(led.id)
                                            hasAny = true
                                        }
                                    }
                                }
                            }
                            if (hasAny) glyphManager?.toggle(builder.build())
                            else glyphManager?.turnOff()
                            delay(50)
                        }
                        glyphManager?.turnOff()
                    } else if (action.startsWith(Config.ACTION_GLYPH_DRAW_PREFIX)) {
                        val data = action.removePrefix(Config.ACTION_GLYPH_DRAW_PREFIX)
                        val parts = data.split(":")
                        
                        // New format: duration:fade:fadeSpeed:ids
                        // Old format: ids
                        val hasSettings = parts.size >= 4
                        
                        val duration = if (hasSettings) parts[0].toLongOrNull() ?: 1000L else 1000L
                        val fade = if (hasSettings) parts[1].toBoolean() else false
                        val fadeSpeed = if (hasSettings) parts[2].toFloatOrNull() ?: 1.0f else 1.0f
                        val idsStr = if (hasSettings) parts[3] else parts[0]
                        
                        val ids = idsStr.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
                        
                        if (fade) {
                            // Fade in (optional, but let's do fade out as requested)
                            val builder = glyphManager?.getGlyphFrameBuilder() ?: return@forEach
                            ids.forEach { builder.buildChannel(it) }
                            glyphManager?.toggle(builder.build())
                            delay(duration)
                            
                            // Fade out
                            for (i in 100 downTo 0 step (5 * fadeSpeed).toInt().coerceAtLeast(1)) {
                                val fadeBuilder = glyphManager?.getGlyphFrameBuilder() ?: break
                                ids.forEach { fadeBuilder.buildChannel(it).buildCycles(i) }
                                glyphManager?.toggle(fadeBuilder.build())
                                delay(30)
                            }
                        } else {
                            val builder = glyphManager?.getGlyphFrameBuilder() ?: return@forEach
                            ids.forEach { builder.buildChannel(it) }
                            glyphManager?.toggle(builder.build())
                            delay(duration)
                        }
                        glyphManager?.turnOff()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pattern playback error: ${e.message}")
            } finally {
                glyphManager?.turnOff()
                _glyphActive.value = false
                glyphRunnerJob = null
                updateGlyphStatus(glyphOn)
            }
        }
    }

    fun toggleGlyphRunner() {
        if (glyphRunnerJob != null) {
            stopGlyphRunner()
            updateGlyphStatus(false)
            glyphManager?.turnOff()
        } else {
            glyphRunnerJob = controllerScope.launch {
                _glyphActive.value = true
                try {
                    while (isActive) {
                        for (i in 0..100 step 5) {
                            val builder = glyphManager?.getGlyphFrameBuilder() ?: break
                            builder.buildChannelC().buildCycles(i)
                            glyphManager?.toggle(builder.build())
                            delay(30)
                        }
                        for (i in 100 downTo 0 step 5) {
                            val builder = glyphManager?.getGlyphFrameBuilder() ?: break
                            builder.buildChannelC().buildCycles(i)
                            glyphManager?.toggle(builder.build())
                            delay(30)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Glyph Runner error: ${e.message}")
                } finally {
                    glyphManager?.turnOff()
                }
            }
        }
    }

    fun stopGlyphRunner() {
        glyphRunnerJob?.cancel()
        glyphRunnerJob = null
        glyphManager?.turnOff()
        updateGlyphStatus(glyphOn)
    }

    fun turnOff() {
        stopGlyphRunner()
        glyphManager?.turnOff()
        updateGlyphStatus(false)
    }
    
    fun uninit() {
        controllerScope.cancel()
        glyphManager?.closeSession()
        glyphManager?.unInit()
        glyphManager = null
    }
}
