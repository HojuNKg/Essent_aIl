package com.aiden.essentialmapper

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/** The three tap slots the Essential Key can be mapped to. */
enum class TapSlot(val key: String, val label: String) {
    SINGLE("single", "Одинарное нажатие"),
    LONG("long", "Долгое нажатие"),
    DOUBLE("double", "Двойное нажатие"),
    TRIPLE("triple", "Тройное нажатие"),
}

/** Theme options. */
enum class AppTheme { AUTO, LIGHT, DARK }

/** Side options for the custom panel. */
enum class PanelSide { LEFT, RIGHT }

/** Action types for the Customizable panel. */
enum class CustomActionType { NONE, FAVORITES, RECORDING }

/** Macro playback modes. */
enum class MacroMode(val label: String) {
    SINGLE("Одиночный"),
    LOOP("Цикличный")
}

/** A simple point for gesture recording. */
data class GesturePoint(val x: Float, val y: Float, val time: Long)

/** Configuration for a custom action slot. */
data class CustomConfig(
    val type: CustomActionType = CustomActionType.NONE,
    val favoriteApps: List<String> = emptyList(),
    val recordedGesture: List<GesturePoint> = emptyList(),
    val macroMode: MacroMode = MacroMode.SINGLE,
)

/** Persisted mapping. A null slot means "do nothing". */
data class KeyMap(
    val single: String? = null,
    val long: String? = null,
    val double: String? = null,
    val triple: String? = null,
    val theme: AppTheme = AppTheme.AUTO,
    val panelSide: PanelSide = PanelSide.RIGHT,
    val panelYOffset: Float = 0.5f,
    val customConfigs: Map<TapSlot, CustomConfig> = emptyMap(),
) {
    fun forSlot(slot: TapSlot): String? = when (slot) {
        TapSlot.SINGLE -> single
        TapSlot.LONG -> long
        TapSlot.DOUBLE -> double
        TapSlot.TRIPLE -> triple
    }
}

private val Context.dataStore by preferencesDataStore(name = "config")

object Config {
    /**
     * Sentinel stored in a slot to mean "toggle the flashlight" rather than launch an
     * app. Slots hold either a real package name or this token.
     */
    const val ACTION_FLASHLIGHT = "action:flashlight"

    /**
     * Custom action: Kill Nothing Essential Space app (for double/triple taps).
     */
    const val ACTION_KILL_ESSENTIAL = "action:kill_essential"

    /**
     * Custom action: Kill Nothing Notes/Screenshot overlay (for single tap).
     */
    const val ACTION_KILL_NOTES = "action:kill_notes"

    /**
     * Custom action: Perform system BACK gesture.
     */
    const val ACTION_BACK = "action:back"

    /**
     * Custom action: Show the yellow custom panel.
     */
    const val ACTION_CUSTOM_PANEL = "action:custom_panel"

    /** Seed the chosen mapping once, on first run, leaving later edits untouched. */
    suspend fun seedDefaultsIfEmpty(ctx: Context) {
        ctx.dataStore.edit { p ->
            val seeded = booleanPreferencesKey("seeded")
            if (p[seeded] != true) {
                p[stringPreferencesKey(TapSlot.SINGLE.key)] = "com.anthropic.claude"
                p[stringPreferencesKey(TapSlot.DOUBLE.key)] = "com.google.android.apps.walletnfcrel"
                p[stringPreferencesKey(TapSlot.TRIPLE.key)] = ACTION_FLASHLIGHT
                p[seeded] = true
            }
        }
    }

    fun flow(ctx: Context): Flow<KeyMap> = ctx.dataStore.data.map { p ->
        KeyMap(
            single = p[stringPreferencesKey(TapSlot.SINGLE.key)],
            long = p[stringPreferencesKey(TapSlot.LONG.key)],
            double = p[stringPreferencesKey(TapSlot.DOUBLE.key)],
            triple = p[stringPreferencesKey(TapSlot.TRIPLE.key)],
            theme = try {
                AppTheme.valueOf(p[stringPreferencesKey("theme")] ?: AppTheme.AUTO.name)
            } catch (e: Exception) {
                AppTheme.AUTO
            },
            panelSide = try {
                PanelSide.valueOf(p[stringPreferencesKey("panelSide")] ?: PanelSide.RIGHT.name)
            } catch (e: Exception) {
                PanelSide.RIGHT
            },
            panelYOffset = p[androidx.datastore.preferences.core.floatPreferencesKey("panelYOffset")] ?: 0.5f,
            customConfigs = TapSlot.entries.associateWith { slot ->
                val prefix = "${slot.key}_"
                CustomConfig(
                    type = try {
                        CustomActionType.valueOf(p[stringPreferencesKey("${prefix}type")] ?: CustomActionType.NONE.name)
                    } catch (e: Exception) {
                        CustomActionType.NONE
                    },
                    favoriteApps = p[stringPreferencesKey("${prefix}favs")]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                    recordedGesture = p[stringPreferencesKey("${prefix}gesture")]?.split(";")?.filter { it.isNotEmpty() }?.mapNotNull {
                        val parts = it.split(",")
                        if (parts.size == 3) GesturePoint(parts[0].toFloat(), parts[1].toFloat(), parts[2].toLong()) else null
                    } ?: emptyList(),
                    macroMode = try {
                        MacroMode.valueOf(p[stringPreferencesKey("${prefix}macroMode")] ?: MacroMode.SINGLE.name)
                    } catch (e: Exception) {
                        MacroMode.SINGLE
                    }
                )
            }
        )
    }

    suspend fun setTheme(ctx: Context, theme: AppTheme) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("theme")] = theme.name
        }
    }

    suspend fun setPanelConfig(ctx: Context, side: PanelSide, yOffset: Float) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("panelSide")] = side.name
            p[androidx.datastore.preferences.core.floatPreferencesKey("panelYOffset")] = yOffset
        }
    }

    suspend fun setCustomActionType(ctx: Context, slot: TapSlot, type: CustomActionType) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("${slot.key}_type")] = type.name
        }
    }

    suspend fun setFavoriteApps(ctx: Context, slot: TapSlot, apps: List<String>) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("${slot.key}_favs")] = apps.joinToString(",")
        }
    }

    suspend fun setRecordedGesture(ctx: Context, slot: TapSlot, gesture: List<GesturePoint>) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("${slot.key}_gesture")] = gesture.joinToString(";") { "${it.x},${it.y},${it.time}" }
        }
    }

    suspend fun setMacroMode(ctx: Context, slot: TapSlot, mode: MacroMode) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("${slot.key}_macroMode")] = mode.name
        }
    }

    suspend fun set(ctx: Context, slot: TapSlot, pkg: String?) {
        ctx.dataStore.edit { p ->
            val key = stringPreferencesKey(slot.key)
            if (pkg == null) p.remove(key) else p[key] = pkg
        }
    }

    /** Synchronous read for the accessibility service (re-read per event is fine). */
    fun readBlocking(ctx: Context): KeyMap = runBlocking { flow(ctx).first() }
}
