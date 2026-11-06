package com.HojuNK.essent_all

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/** The three tap slots the Essential Key can be mapped to. */
@Parcelize
enum class TapSlot(val key: String, val labelRes: Int) : Parcelable {
    SINGLE("single", R.string.tap_single),
    LONG("long", R.string.tap_long),
    DOUBLE("double", R.string.tap_double),
    TRIPLE("triple", R.string.tap_triple),
    
    VOL_UP_SINGLE("vol_up_single", R.string.tap_vol_up_single),
    VOL_UP_LONG("vol_up_long", R.string.tap_vol_up_long),
    VOL_UP_DOUBLE("vol_up_double", R.string.tap_vol_up_double),
    VOL_UP_TRIPLE("vol_up_triple", R.string.tap_vol_up_triple),
    
    VOL_DOWN_SINGLE("vol_down_single", R.string.tap_vol_down_single),
    VOL_DOWN_LONG("vol_down_long", R.string.tap_vol_down_long),
    VOL_DOWN_DOUBLE("vol_down_double", R.string.tap_vol_down_double),
    VOL_DOWN_TRIPLE("vol_down_triple", R.string.tap_vol_down_triple),
    
    POWER_LONG("power_long", R.string.tap_power_long),
    SHORTCUT("shortcut", R.string.tap_shortcut),
}

/** View modes for the Events screen. */
enum class EventsViewMode { STRIPES, RECTANGLES }
enum class ListSortOption { CREATION_DATE, ALPHABETICAL, RANKING }
enum class ListGroupOption { GROUPED, UNGROUPED }

/** Types of conditions for an event. */
@Parcelize
enum class ConditionType : Parcelable { 
    APP, TIME, ALWAYS, ROTATION, LIGHT, BRIGHTNESS, WIFI, BLUETOOTH, LOCATION, MOBILE_DATA, BATTERY,
    CHARGING, SCREEN_LOCK, AIRPLANE_MODE, NFC, AUTO_ROTATE, HEADPHONES, DND, MOTION, DARK_MODE, SILENT_MODE, VIBRATE_MODE, HOTSPOT
}

/** A single condition in an event rule. */
@Parcelize
data class EventCondition(
    val type: ConditionType,
    val value: String = "" // package name, "HH:mm-HH:mm", or empty
) : Parcelable

/** A group of events. */
@Parcelize
data class EventGroup(
    val id: String,
    val title: String,
    val icon: String = "material:Folder"
) : Parcelable

/** A rule defining special behavior based on conditions. */
@Parcelize
data class EventRule(
    val id: String,
    val icon: String,
    val title: String,
    val isEnabled: Boolean = true,
    val triggerGesture: TapSlot = TapSlot.SINGLE,
    val conditions: List<EventCondition> = emptyList(),
    val conditionLogicAll: Boolean = true, // true = ALL (AND), false = ANY (OR)
    val actions: List<String> = emptyList(),
    val useShortcut: Boolean = false,
    val exclusions: List<EventCondition> = emptyList(),
    val exclusionLogicAll: Boolean = false,
    val delayBefore: Float = 0f,
    val keepVisibleOnClick: Boolean = false,
    val keepVisibleOnOutsideClick: Boolean = false,
    val keepVisibleOnBack: Boolean = false,
    val keepVisibleOnKey: Boolean = false,
    val autoHideSeconds: Int = 5, // 0 for never
    val groupId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/** A reusable sequence of actions. */
@Parcelize
data class Macro(
    val id: String,
    val icon: String,
    val title: String,
    val actions: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val mediaItems: List<MediaItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class MacroRecordingMode {
    SINGLE_TAP,
    SINGLE_SWIPE,
    COMBINATION
}

/** Theme options. */
enum class AppTheme { AUTO, LIGHT, DARK }

/** Side options for the custom panel. */
enum class PanelSide { LEFT, RIGHT }

/** App language options. */
enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    RUSSIAN
}

/** Action types for the Customizable panel. */
enum class CustomActionType { NONE, FAVORITES, CONTROLS }

/** Macro playback modes. */
enum class MacroMode(val labelRes: Int) {
    SINGLE(R.string.macro_single),
    LOOP(R.string.macro_loop)
}

enum class MediaType { PHOTO, VIDEO }

@Parcelize
data class MediaItem(
    val id: String,
    val type: MediaType,
    val path: String,
    val startTime: Long,
    val duration: Long,
    val startOffset: Long = 0L,
    val fullDuration: Long = 0L
) : Parcelable

/** A simple point for gesture recording. */
data class GesturePoint(val x: Float, val y: Float, val time: Long)

/** Types of controls in the Control Panel. */
enum class ControlType(val labelRes: Int) {
    EDIT(R.string.control_edit),
    FLASHLIGHT(R.string.control_flashlight),
    ROTATION(R.string.control_rotation),
    VOLUME(R.string.control_volume),
    DND(R.string.control_dnd),
    SETTINGS(R.string.control_settings),
    APP(R.string.control_app),
    URL(R.string.control_url)
}

/** A single item in the control panel grid. */
data class ControlItem(
    val type: ControlType,
    val x: Int, // 0-3
    val y: Int, // 0-3
    val w: Int = 1,
    val h: Int = 1,
    val extra: String? = null // Пакет приложения или URL
)

/** Configuration for a custom action slot. */
data class CustomConfig(
    val type: CustomActionType = CustomActionType.NONE,
    val favoriteApps: List<String> = emptyList(),
    val macroMode: MacroMode = MacroMode.SINGLE,
    val controlItems: List<ControlItem> = emptyList(),
    val keepVisibleOnClick: Boolean = false,
    val keepVisibleOnOutsideClick: Boolean = false,
    val keepVisibleOnBack: Boolean = false,
    val keepVisibleOnKey: Boolean = false,
    val autoHideSeconds: Int = 5
)

/** Persisted mapping. A null slot means "do nothing". */
data class KeyMap(
    val single: String? = null,
    val long: String? = null,
    val double: String? = null,
    val triple: String? = null,
    val volUpSingle: String? = null,
    val volUpLong: String? = null,
    val volUpDouble: String? = null,
    val volUpTriple: String? = null,
    val volDownSingle: String? = null,
    val volDownLong: String? = null,
    val volDownDouble: String? = null,
    val volDownTriple: String? = null,
    val powerLong: String? = null,
    val theme: AppTheme = AppTheme.AUTO,
    val panelSide: PanelSide = PanelSide.RIGHT,
    val panelYOffset: Float = 0.5f,
    val customConfigs: Map<TapSlot, CustomConfig> = emptyMap(),
    val events: List<EventRule> = emptyList(),
    val eventGroups: List<EventGroup> = emptyList(),
    val macros: List<Macro> = emptyList(),
    val disableAnimations: Boolean = false,
    val eventsViewMode: EventsViewMode = EventsViewMode.STRIPES,
    val eventsSortOption: ListSortOption = ListSortOption.CREATION_DATE,
    val eventsSortAscending: Boolean = false,
    val eventsGroupOption: ListGroupOption = ListGroupOption.GROUPED,
    val macrosViewMode: EventsViewMode = EventsViewMode.STRIPES,
    val macrosSortOption: ListSortOption = ListSortOption.CREATION_DATE,
    val macrosSortAscending: Boolean = false,
    val macrosGroupOption: ListGroupOption = ListGroupOption.GROUPED,
    val glyphPatternsViewMode: EventsViewMode = EventsViewMode.STRIPES,
    val glyphPatternsSortOption: ListSortOption = ListSortOption.CREATION_DATE,
    val glyphPatternsSortAscending: Boolean = false,
    val glyphPatternsGroupOption: ListGroupOption = ListGroupOption.GROUPED,
    val glyphPatterns: List<Macro> = emptyList(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    val confirmDeletion: Boolean = true
) {
    fun forSlot(slot: TapSlot): String? = when (slot) {
        TapSlot.SINGLE -> single
        TapSlot.LONG -> long
        TapSlot.DOUBLE -> double
        TapSlot.TRIPLE -> triple
        TapSlot.VOL_UP_SINGLE -> volUpSingle
        TapSlot.VOL_UP_LONG -> volUpLong
        TapSlot.VOL_UP_DOUBLE -> volUpDouble
        TapSlot.VOL_UP_TRIPLE -> volUpTriple
        TapSlot.VOL_DOWN_SINGLE -> volDownSingle
        TapSlot.VOL_DOWN_LONG -> volDownLong
        TapSlot.VOL_DOWN_DOUBLE -> volDownDouble
        TapSlot.VOL_DOWN_TRIPLE -> volDownTriple
        TapSlot.POWER_LONG -> powerLong
        else -> null
    }

    fun hasActionOrEvent(slot: TapSlot): Boolean {
        if (forSlot(slot) != null) return true
        return events.any { it.isEnabled && it.triggerGesture == slot }
    }

    fun hasAnyActionOrEvent(vararg slots: TapSlot): Boolean {
        return slots.any { hasActionOrEvent(it) }
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

    /**
     * Custom action: Show the bottom control panel.
     */
    const val ACTION_CONTROLS = "action:controls"

    /**
     * Custom action prefix for screen touch.
     */
    const val ACTION_TOUCH_PREFIX = "action:touch:"

    /**
     * Custom action prefix for screen swipe.
     */
    const val ACTION_SWIPE_PREFIX = "action:swipe:"

    /**
     * Custom action prefix for complex path gesture.
     */
    const val ACTION_PATH_PREFIX = "action:path:"

    /**
     * Custom action prefix for delay.
     */
    const val ACTION_DELAY_PREFIX = "action:delay:"
    const val ACTION_WAIT_CONFIRMATION_PREFIX = "action:wait_confirmation:"
    const val ACTION_GLYPH_STEP_RUNNER_PREFIX = "action:glyph_run:"
    const val ACTION_HOME = "action:system:home"
    const val ACTION_NOTIFICATIONS = "action:system:notifications"
    const val ACTION_BRIGHTNESS_PREFIX = "action:system:brightness:"
    const val ACTION_FAVORITES_PREFIX = "action:favorites:"
    const val ACTION_CONTROLS_PREFIX = "action:controls:"
    const val ACTION_SHORTCUT_PREFIX = "action:shortcut:"
    const val ACTION_MACRO_PREFIX = "action:macro:"
    const val ACTION_GLYPH_PATTERN_PREFIX = "action:glyph_pattern:"
    const val ACTION_GLYPH_STATIC_PREFIX = "action:glyph_static:"
    const val ACTION_GLYPH_WAVE_PREFIX = "action:glyph_wave:"
    const val ACTION_GLYPH_DRAW_PREFIX = "action:glyph_draw:"
    const val ACTION_GLYPH = "action:glyph"
    const val ACTION_GLYPH_TOGGLE = "action:glyph_toggle"
    const val ACTION_FLASHLIGHT_PREFIX = "action:flashlight:"
    const val ACTION_GLYPH_PREFIX = "action:glyph:"

    const val ACTION_SCREENSHOT = "action:system:screenshot"
    const val ACTION_LOCK_SCREEN = "action:system:lock_screen"
    const val ACTION_SPLIT_SCREEN = "action:system:split_screen"
    const val ACTION_VOLUME_PREFIX = "action:system:volume:" // e.g. volume:music:up
    const val ACTION_MEDIA_PREFIX = "action:system:media:" // e.g. media:play_pause
    const val ACTION_URL_PREFIX = "action:url:"
    const val ACTION_DND_TOGGLE = "action:system:dnd_toggle"
    const val ACTION_THEME_PREFIX = "action:system:theme:" // theme:light, theme:dark, theme:toggle
    const val ACTION_CLEAR_NOTIFICATIONS = "action:system:clear_notifications"
    const val ACTION_AUTO_BRIGHTNESS_PREFIX = "action:system:auto_brightness:" // auto_brightness:on, auto_brightness:off, auto_brightness:toggle
    const val ACTION_ROTATION_TOGGLE = "action:system:rotation_toggle"
    const val ACTION_RING_VIBRATE = "action:system:ring_vibrate"
    const val ACTION_VOICE_ASSISTANT = "action:system:voice_assistant"
    const val ACTION_CLIPBOARD = "action:clipboard"
    const val ACTION_WAKE_SCREEN = "action:system:wake_screen"
    const val ACTION_POWER_MENU = "action:system:power_menu"
    const val ACTION_SET_TIMER = "action:system:set_timer"
    const val ACTION_BLUETOOTH_TOGGLE = "action:system:bluetooth_toggle"
    const val ACTION_WIFI_TOGGLE = "action:system:wifi_toggle"
    const val ACTION_RECENTS = "action:system:recents"
    const val ACTION_READ_SCREEN = "action:system:read_screen"
    const val ACTION_READ_ALOUD = "action:system:read_aloud"
    const val ACTION_COPY = "action:system:copy"
    const val ACTION_PASTE = "action:system:paste"
    const val ACTION_CALCULATOR = "action:system:calculator"
    const val ACTION_OPEN_CAMERA = "action:system:open_camera"
    const val ACTION_SCREEN_RECORD = "action:system:screen_record"
    const val ACTION_CAMERA_SHUTTER = "action:system:camera_shutter"
    const val ACTION_EXTRA_DIM = "action:system:extra_dim"
    const val ACTION_QUICK_SETTINGS = "action:system:quick_settings"
    const val ACTION_WALLET = "action:system:wallet"
    const val ACTION_CHATGPT = "action:system:chatgpt"

    /** Seed the chosen mapping once, on first run, leaving later edits untouched. */
    suspend fun seedDefaultsIfEmpty(ctx: Context) {
        ctx.dataStore.edit { p ->
            val seeded = booleanPreferencesKey("seeded")
            if (p[seeded] != true) {
                // Key mappings
                p.remove(stringPreferencesKey(TapSlot.SINGLE.key))
                p[stringPreferencesKey(TapSlot.DOUBLE.key)] = "com.HojuNK.essent_all"
                p[stringPreferencesKey(TapSlot.TRIPLE.key)] = ACTION_FLASHLIGHT
                
                // Panel position
                p[floatPreferencesKey("panelYOffset")] = 0.5715227f
                
                // Default Events
                val eventsArr = JSONArray()
                
                // Яркость 15%
                eventsArr.put(serializeEvent(EventRule(
                    id = "706a3ef6-fd74-4ac3-91f8-7649fb842e65", icon = "☀️", title = "Яркость 15%",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "0-35"), EventCondition(ConditionType.BRIGHTNESS, "15-100")),
                    actions = listOf(ACTION_KILL_NOTES, "action:system:brightness:10")
                )))
                
                // Яркость 50%(Увеличение)
                eventsArr.put(serializeEvent(EventRule(
                    id = "b98b3457-580f-44f8-9e09-806b5eeb7825", icon = "☀️", title = "Яркость 50%(Увеличение)",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "35-75"), EventCondition(ConditionType.BRIGHTNESS, "0-30")),
                    actions = listOf("action:system:brightness:50", ACTION_KILL_NOTES)
                )))
                
                // Яркость 50%(Уменьшение)
                eventsArr.put(serializeEvent(EventRule(
                    id = "d0b421c2-e1f6-4073-8ddc-6bfd77512da2", icon = "☀️", title = "Яркость 50%(Уменьшение)",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "35-75"), EventCondition(ConditionType.BRIGHTNESS, "60-100")),
                    actions = listOf("action:system:brightness:50", ACTION_KILL_NOTES)
                )))
                
                // Яркость 100%
                eventsArr.put(serializeEvent(EventRule(
                    id = "6ed211a4-4461-4b64-97f3-07078672319f", icon = "☀️", title = "Яркость 100%",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "75-100"), EventCondition(ConditionType.BRIGHTNESS, "0-60")),
                    actions = listOf("action:system:brightness:100", ACTION_KILL_NOTES)
                )))
                
                // Ночной фонарик
                eventsArr.put(serializeEvent(EventRule(
                    id = "3b6d11e7-a975-4113-a70f-9d52e51c4efe", icon = "💥", title = "Ночной фонарик",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(
                        EventCondition(ConditionType.LIGHT, "0-60"), 
                        EventCondition(ConditionType.BRIGHTNESS, "0-45"),
                        EventCondition(ConditionType.ROTATION, "FACE_UP"),
                        EventCondition(ConditionType.TIME, "23:00-06:00")
                    ),
                    actions = listOf(ACTION_KILL_ESSENTIAL, "action:wait_confirmation:true,false,false,0", ACTION_FLASHLIGHT)
                )))
                
                p[stringPreferencesKey("events")] = eventsArr.toString()
                
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
            volUpSingle = p[stringPreferencesKey(TapSlot.VOL_UP_SINGLE.key)],
            volUpLong = p[stringPreferencesKey(TapSlot.VOL_UP_LONG.key)],
            volUpDouble = p[stringPreferencesKey(TapSlot.VOL_UP_DOUBLE.key)],
            volUpTriple = p[stringPreferencesKey(TapSlot.VOL_UP_TRIPLE.key)],
            volDownSingle = p[stringPreferencesKey(TapSlot.VOL_DOWN_SINGLE.key)],
            volDownLong = p[stringPreferencesKey(TapSlot.VOL_DOWN_LONG.key)],
            volDownDouble = p[stringPreferencesKey(TapSlot.VOL_DOWN_DOUBLE.key)],
            volDownTriple = p[stringPreferencesKey(TapSlot.VOL_DOWN_TRIPLE.key)],
            powerLong = p[stringPreferencesKey(TapSlot.POWER_LONG.key)],
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
            eventsViewMode = try {
                EventsViewMode.valueOf(p[stringPreferencesKey("eventsViewMode")] ?: EventsViewMode.STRIPES.name)
            } catch (e: Exception) {
                EventsViewMode.STRIPES
            },
            eventsSortOption = try {
                ListSortOption.valueOf(p[stringPreferencesKey("eventsSortOption")] ?: ListSortOption.CREATION_DATE.name)
            } catch (e: Exception) {
                ListSortOption.CREATION_DATE
            },
            eventsSortAscending = p[booleanPreferencesKey("eventsSortAscending")] ?: false,
            eventsGroupOption = try {
                ListGroupOption.valueOf(p[stringPreferencesKey("eventsGroupOption")] ?: ListGroupOption.GROUPED.name)
            } catch (e: Exception) {
                ListGroupOption.GROUPED
            },
            macrosViewMode = try {
                EventsViewMode.valueOf(p[stringPreferencesKey("macrosViewMode")] ?: EventsViewMode.STRIPES.name)
            } catch (e: Exception) {
                EventsViewMode.STRIPES
            },
            macrosSortOption = try {
                ListSortOption.valueOf(p[stringPreferencesKey("macrosSortOption")] ?: ListSortOption.CREATION_DATE.name)
            } catch (e: Exception) {
                ListSortOption.CREATION_DATE
            },
            macrosSortAscending = p[booleanPreferencesKey("macrosSortAscending")] ?: false,
            macrosGroupOption = try {
                ListGroupOption.valueOf(p[stringPreferencesKey("macrosGroupOption")] ?: ListGroupOption.GROUPED.name)
            } catch (e: Exception) {
                ListGroupOption.GROUPED
            },
            glyphPatternsViewMode = try {
                EventsViewMode.valueOf(p[stringPreferencesKey("glyphPatternsViewMode")] ?: EventsViewMode.STRIPES.name)
            } catch (e: Exception) {
                EventsViewMode.STRIPES
            },
            glyphPatternsSortOption = try {
                ListSortOption.valueOf(p[stringPreferencesKey("glyphPatternsSortOption")] ?: ListSortOption.CREATION_DATE.name)
            } catch (e: Exception) {
                ListSortOption.CREATION_DATE
            },
            glyphPatternsSortAscending = p[booleanPreferencesKey("glyphPatternsSortAscending")] ?: false,
            glyphPatternsGroupOption = try {
                ListGroupOption.valueOf(p[stringPreferencesKey("glyphPatternsGroupOption")] ?: ListGroupOption.GROUPED.name)
            } catch (e: Exception) {
                ListGroupOption.GROUPED
            },
            disableAnimations = p[booleanPreferencesKey("disableAnimations")] ?: false,
            language = try {
                AppLanguage.valueOf(p[stringPreferencesKey("language")] ?: AppLanguage.SYSTEM.name)
            } catch (e: Exception) {
                AppLanguage.SYSTEM
            },
            confirmDeletion = p[booleanPreferencesKey("confirmDeletion")] ?: true,
            events = p[stringPreferencesKey("events")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    List(arr.length()) { i -> parseEvent(arr.getJSONObject(i)) }
                } catch (e: Exception) {
                    // Fallback to old format parsing logic if JSON fails
                    jsonStr.split("###").filter { it.isNotEmpty() }.mapNotNull { ruleStr ->
                        val pts = ruleStr.split("@@@")
                        if (pts.size >= 8) {
                            val conds = pts[5].split("$$$").filter { it.isNotEmpty() }.mapNotNull { cStr ->
                                val cPts = cStr.split("::")
                                if (cPts.size >= 2) {
                                    try { EventCondition(ConditionType.valueOf(cPts[0]), cPts.subList(1, cPts.size).joinToString("::")) } catch(e: Exception) { null }
                                } else null
                            }
                            val excl = if (pts.size >= 10) pts[8].split("$$$").filter { it.isNotEmpty() }.mapNotNull { cStr ->
                                val cPts = cStr.split("::")
                                if (cPts.size >= 2) {
                                    try { EventCondition(ConditionType.valueOf(cPts[0]), cPts.subList(1, cPts.size).joinToString("::")) } catch(e: Exception) { null }
                                } else null
                            } else emptyList()
                            
                            val isOldShortcut = pts[4] == "shortcut"
                            EventRule(
                                id = pts[0], icon = pts[1], title = pts[2], isEnabled = pts[3].toBoolean(),
                                triggerGesture = if (isOldShortcut) TapSlot.SINGLE else (TapSlot.entries.find { s -> s.key == pts[4] } ?: TapSlot.SINGLE),
                                conditions = conds, conditionLogicAll = pts[6].toBoolean(), actions = pts[7].split("%%%").filter { it.isNotEmpty() },
                                useShortcut = isOldShortcut,
                                exclusions = excl, exclusionLogicAll = if (pts.size >= 10) pts[9].toBoolean() else false,
                                delayBefore = if (pts.size >= 11) pts[10].toFloatOrNull() ?: 0f else 0f,
                                keepVisibleOnClick = if (pts.size >= 12) pts[11].toBoolean() else false,
                                keepVisibleOnOutsideClick = if (pts.size >= 13) pts[12].toBoolean() else false,
                                keepVisibleOnBack = if (pts.size >= 14) pts[13].toBoolean() else false,
                                keepVisibleOnKey = if (pts.size >= 15) pts[14].toBoolean() else false,
                                autoHideSeconds = if (pts.size >= 16) pts[15].toIntOrNull() ?: 5 else 5
                            )
                        } else null
                    }
                }
            } ?: emptyList(),
            eventGroups = p[stringPreferencesKey("eventGroups")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    List(arr.length()) { i -> parseEventGroup(arr.getJSONObject(i)) }
                } catch (e: Exception) { emptyList() }
            } ?: emptyList(),
            macros = p[stringPreferencesKey("macros")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    List(arr.length()) { i -> parseMacro(arr.getJSONObject(i)) }
                } catch (e: Exception) {
                    jsonStr.split("###").filter { it.isNotEmpty() }.mapNotNull { macroStr ->
                        val pts = macroStr.split("@@@")
                        if (pts.size >= 5) {
                            Macro(
                                id = pts[0], icon = pts[1], title = pts[2],
                                actions = pts[3].split("%%%").filter { it.isNotEmpty() },
                                isEnabled = pts[4].toBoolean()
                            )
                        } else null
                    }
                }
            } ?: emptyList(),
            glyphPatterns = p[stringPreferencesKey("glyphPatterns")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    List(arr.length()) { i -> parseMacro(arr.getJSONObject(i)) }
                } catch (e: Exception) { emptyList() }
            } ?: emptyList(),
            customConfigs = TapSlot.entries.associateWith { slot ->
                val prefix = "${slot.key}_"
                CustomConfig(
                    type = try {
                        CustomActionType.valueOf(p[stringPreferencesKey("${prefix}type")] ?: CustomActionType.NONE.name)
                    } catch (e: Exception) {
                        CustomActionType.NONE
                    },
                    favoriteApps = p[stringPreferencesKey("${prefix}favs")]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                    macroMode = try {
                        MacroMode.valueOf(p[stringPreferencesKey("${prefix}macroMode")] ?: MacroMode.SINGLE.name)
                    } catch (e: Exception) {
                        MacroMode.SINGLE
                    },
                    controlItems = p[stringPreferencesKey("${prefix}controls")]?.split(";")?.filter { it.isNotEmpty() }?.mapNotNull {
                        val pts = it.split(",")
                        if (pts.size >= 5) {
                            try {
                                ControlItem(
                                    type = ControlType.valueOf(pts[0]),
                                    x = pts[1].toInt(),
                                    y = pts[2].toInt(),
                                    w = pts[3].toInt(),
                                    h = pts[4].toInt(),
                                    extra = if (pts.size > 5) pts.subList(5, pts.size).joinToString(",") else null
                                )
                            } catch (e: Exception) { null }
                        } else null
                    } ?: emptyList(),
                    keepVisibleOnClick = p[booleanPreferencesKey("${prefix}keepOnClick")] ?: false,
                    keepVisibleOnOutsideClick = p[booleanPreferencesKey("${prefix}keepOnOutside")] ?: false,
                    keepVisibleOnBack = p[booleanPreferencesKey("${prefix}keepOnBack")] ?: false,
                    keepVisibleOnKey = p[booleanPreferencesKey("${prefix}keepOnKey")] ?: false,
                    autoHideSeconds = p[androidx.datastore.preferences.core.intPreferencesKey("${prefix}autoHide")] ?: 5
                )
            }
        )
    }

    suspend fun setTheme(ctx: Context, theme: AppTheme) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("theme")] = theme.name
        }
    }

    suspend fun setLanguage(ctx: Context, language: AppLanguage) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("language")] = language.name
        }
    }

    suspend fun setConfirmDeletion(ctx: Context, confirm: Boolean) {
        ctx.dataStore.edit { p ->
            p[booleanPreferencesKey("confirmDeletion")] = confirm
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

    suspend fun setMacroMode(ctx: Context, slot: TapSlot, mode: MacroMode) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("${slot.key}_macroMode")] = mode.name
        }
    }

    suspend fun setControlItems(ctx: Context, slot: TapSlot, items: List<ControlItem>) {
        ctx.dataStore.edit { p ->
            p[stringPreferencesKey("${slot.key}_controls")] = items.joinToString(";") { 
                "${it.type.name},${it.x},${it.y},${it.w},${it.h}${if (it.extra != null) ",${it.extra}" else ""}" 
            }
        }
    }

    suspend fun setCustomVisibility(ctx: Context, slot: TapSlot, onClick: Boolean, onOutside: Boolean, onBack: Boolean, onKey: Boolean, autoHide: Int) {
        ctx.dataStore.edit { p ->
            p[booleanPreferencesKey("${slot.key}_keepOnClick")] = onClick
            p[booleanPreferencesKey("${slot.key}_keepOnOutside")] = onOutside
            p[booleanPreferencesKey("${slot.key}_keepOnBack")] = onBack
            p[booleanPreferencesKey("${slot.key}_keepOnKey")] = onKey
            p[androidx.datastore.preferences.core.intPreferencesKey("${slot.key}_autoHide")] = autoHide
        }
    }

    suspend fun set(ctx: Context, slot: TapSlot, pkg: String?) {
        ctx.dataStore.edit { p ->
            val key = stringPreferencesKey(slot.key)
            if (pkg == null) p.remove(key) else p[key] = pkg
        }
    }

    suspend fun setEvents(ctx: Context, events: List<EventRule>) {
        ctx.dataStore.edit { p ->
            val arr = JSONArray()
            events.forEach { arr.put(serializeEvent(it)) }
            p[stringPreferencesKey("events")] = arr.toString()
        }
    }

    suspend fun updateEvent(ctx: Context, rule: EventRule) {
        ctx.dataStore.edit { p ->
            val current = p[stringPreferencesKey("events")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    MutableList(arr.length()) { i -> parseEvent(arr.getJSONObject(i)) }
                } catch (e: Exception) { null }
            } ?: mutableListOf()
            
            val index = current.indexOfFirst { it.id == rule.id }
            if (index != -1) current[index] = rule else current.add(rule)
            
            val arr = JSONArray()
            current.forEach { arr.put(serializeEvent(it)) }
            p[stringPreferencesKey("events")] = arr.toString()
        }
    }

    suspend fun updateEventById(ctx: Context, id: String, transform: (EventRule) -> EventRule) {
        ctx.dataStore.edit { p ->
            val current = p[stringPreferencesKey("events")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    MutableList(arr.length()) { i -> parseEvent(arr.getJSONObject(i)) }
                } catch (e: Exception) { null }
            } ?: mutableListOf()

            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = transform(current[index])
            }

            val arr = JSONArray()
            current.forEach { arr.put(serializeEvent(it)) }
            p[stringPreferencesKey("events")] = arr.toString()
        }
    }

    suspend fun deleteEvent(ctx: Context, id: String) {
        ctx.dataStore.edit { p ->
            val jsonStr = p[stringPreferencesKey("events")] ?: return@edit
            try {
                val arr = JSONArray(jsonStr)
                val newList = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("id") != id) newList.put(obj)
                }
                p[stringPreferencesKey("events")] = newList.toString()
            } catch (e: Exception) {}
        }
    }

    suspend fun setMacros(ctx: Context, macros: List<Macro>) {
        ctx.dataStore.edit { p ->
            val arr = JSONArray()
            macros.forEach { arr.put(serializeMacro(it)) }
            p[stringPreferencesKey("macros")] = arr.toString()
        }
    }

    suspend fun setGlyphPatterns(ctx: Context, patterns: List<Macro>) {
        ctx.dataStore.edit { p ->
            val arr = JSONArray()
            patterns.forEach { arr.put(serializeMacro(it)) }
            p[stringPreferencesKey("glyphPatterns")] = arr.toString()
        }
    }

    suspend fun updateGlyphPattern(ctx: Context, pattern: Macro) {
        ctx.dataStore.edit { p ->
            val current = p[stringPreferencesKey("glyphPatterns")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    MutableList(arr.length()) { i -> parseMacro(arr.getJSONObject(i)) }
                } catch (e: Exception) { null }
            } ?: mutableListOf()
            
            val index = current.indexOfFirst { it.id == pattern.id }
            if (index != -1) current[index] = pattern else current.add(pattern)
            
            val arr = JSONArray()
            current.forEach { arr.put(serializeMacro(it)) }
            p[stringPreferencesKey("glyphPatterns")] = arr.toString()
        }
    }

    suspend fun deleteGlyphPattern(ctx: Context, id: String) {
        ctx.dataStore.edit { p ->
            val jsonStr = p[stringPreferencesKey("glyphPatterns")] ?: return@edit
            try {
                val arr = JSONArray(jsonStr)
                val newList = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("id") != id) newList.put(obj)
                }
                p[stringPreferencesKey("glyphPatterns")] = newList.toString()
            } catch (e: Exception) {}
        }
    }

    suspend fun updateMacro(ctx: Context, macro: Macro) {
        ctx.dataStore.edit { p ->
            val current = p[stringPreferencesKey("macros")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    MutableList(arr.length()) { i -> parseMacro(arr.getJSONObject(i)) }
                } catch (e: Exception) { null }
            } ?: mutableListOf()
            
            val index = current.indexOfFirst { it.id == macro.id }
            if (index != -1) current[index] = macro else current.add(macro)
            
            val arr = JSONArray()
            current.forEach { arr.put(serializeMacro(it)) }
            p[stringPreferencesKey("macros")] = arr.toString()
        }
    }

    suspend fun deleteMacro(ctx: Context, id: String) {
        ctx.dataStore.edit { p ->
            val jsonStr = p[stringPreferencesKey("macros")] ?: return@edit
            try {
                val arr = JSONArray(jsonStr)
                val newList = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("id") != id) newList.put(obj)
                }
                p[stringPreferencesKey("macros")] = newList.toString()
            } catch (e: Exception) {}
        }
    }

    private fun serializeEvent(it: EventRule) = JSONObject().apply {
        put("id", it.id); put("icon", it.icon); put("title", it.title); put("enabled", it.isEnabled)
        put("gesture", it.triggerGesture.key); put("logicAll", it.conditionLogicAll)
        put("actions", JSONArray(it.actions)); put("useShortcut", it.useShortcut)
        put("exclLogicAll", it.exclusionLogicAll)
        put("delay", it.delayBefore); put("vClick", it.keepVisibleOnClick)
        put("vOutside", it.keepVisibleOnOutsideClick); put("vBack", it.keepVisibleOnBack)
        put("vKey", it.keepVisibleOnKey); put("autoHide", it.autoHideSeconds)
        if (it.groupId != null) put("groupId", it.groupId)
        put("createdAt", it.createdAt)
        put("conds", JSONArray().apply { it.conditions.forEach { c -> put(JSONObject().apply { put("t", c.type.name); put("v", c.value) }) } })
        put("excls", JSONArray().apply { it.exclusions.forEach { e -> put(JSONObject().apply { put("t", e.type.name); put("v", e.value) }) } })
    }

    private fun parseEvent(obj: JSONObject): EventRule {
        val gestureKey = obj.optString("gesture", "single")
        val isOldShortcut = gestureKey == "shortcut"
        return EventRule(
            id = obj.getString("id"), icon = obj.getString("icon"), title = obj.getString("title"),
            isEnabled = obj.optBoolean("enabled", true),
            triggerGesture = if (isOldShortcut) TapSlot.SINGLE else (TapSlot.entries.find { it.key == gestureKey } ?: TapSlot.SINGLE),
            conditionLogicAll = obj.optBoolean("logicAll", true),
            exclusionLogicAll = obj.optBoolean("exclLogicAll", false),
            delayBefore = obj.optDouble("delay", 0.0).toFloat(),
            keepVisibleOnClick = obj.optBoolean("vClick", false),
            keepVisibleOnOutsideClick = obj.optBoolean("vOutside", false),
            keepVisibleOnBack = obj.optBoolean("vBack", false),
            keepVisibleOnKey = obj.optBoolean("vKey", false),
            autoHideSeconds = obj.optInt("autoHide", 5),
            groupId = obj.optString("groupId", "").takeIf { it.isNotEmpty() },
            createdAt = obj.optLong("createdAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis(),
            actions = obj.optJSONArray("actions")?.let { List(it.length()) { i -> it.getString(i) } } ?: emptyList(),
            useShortcut = obj.optBoolean("useShortcut", isOldShortcut),
            conditions = obj.optJSONArray("conds")?.let { List(it.length()) { i -> val o = it.getJSONObject(i); EventCondition(ConditionType.valueOf(o.getString("t")), o.optString("v", "")) } } ?: emptyList(),
            exclusions = obj.optJSONArray("excls")?.let { List(it.length()) { i -> val o = it.getJSONObject(i); EventCondition(ConditionType.valueOf(o.getString("t")), o.optString("v", "")) } } ?: emptyList()
        )
    }

    private fun serializeEventGroup(it: EventGroup) = JSONObject().apply {
        put("id", it.id)
        put("title", it.title)
        put("icon", it.icon)
    }

    private fun parseEventGroup(obj: JSONObject) = EventGroup(
        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
        title = obj.optString("title", "Group"),
        icon = obj.optString("icon", "material:Folder")
    )

    suspend fun setEventGroups(ctx: Context, groups: List<EventGroup>) {
        ctx.dataStore.edit { p ->
            val arr = JSONArray()
            groups.forEach { arr.put(serializeEventGroup(it)) }
            p[stringPreferencesKey("eventGroups")] = arr.toString()
        }
    }

    suspend fun updateEventGroup(ctx: Context, group: EventGroup) {
        ctx.dataStore.edit { p ->
            val current = p[stringPreferencesKey("eventGroups")]?.let { jsonStr ->
                try {
                    val arr = JSONArray(jsonStr)
                    MutableList(arr.length()) { i -> parseEventGroup(arr.getJSONObject(i)) }
                } catch (e: Exception) { null }
            } ?: mutableListOf()

            val index = current.indexOfFirst { it.id == group.id }
            if (index != -1) current[index] = group else current.add(group)

            val arr = JSONArray()
            current.forEach { arr.put(serializeEventGroup(it)) }
            p[stringPreferencesKey("eventGroups")] = arr.toString()
        }
    }

    suspend fun deleteEventGroup(ctx: Context, groupId: String) {
        ctx.dataStore.edit { p ->
            val jsonStr = p[stringPreferencesKey("eventGroups")]
            if (jsonStr != null) {
                try {
                    val arr = JSONArray(jsonStr)
                    val newList = JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.optString("id") != groupId) newList.put(obj)
                    }
                    p[stringPreferencesKey("eventGroups")] = newList.toString()
                } catch (e: Exception) {}
            }
            val eventsJsonStr = p[stringPreferencesKey("events")]
            if (eventsJsonStr != null) {
                try {
                    val arr = JSONArray(eventsJsonStr)
                    val newEvents = JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.optString("groupId") == groupId) {
                            obj.remove("groupId")
                        }
                        newEvents.put(obj)
                    }
                    p[stringPreferencesKey("events")] = newEvents.toString()
                } catch (e: Exception) {}
            }
        }
    }

    private fun serializeMacro(it: Macro) = JSONObject().apply {
        put("id", it.id); put("icon", it.icon); put("title", it.title)
        put("actions", JSONArray(it.actions)); put("enabled", it.isEnabled)
        put("createdAt", it.createdAt)
        put("media", JSONArray().apply { it.mediaItems.forEach { m -> put(serializeMediaItem(m)) } })
    }

    private fun parseMacro(obj: JSONObject) = Macro(
        id = obj.getString("id"), icon = obj.getString("icon"), title = obj.getString("title"),
        actions = obj.optJSONArray("actions")?.let { List(it.length()) { i -> it.getString(i) } } ?: emptyList(),
        isEnabled = obj.optBoolean("enabled", true),
        createdAt = obj.optLong("createdAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis(),
        mediaItems = obj.optJSONArray("media")?.let { List(it.length()) { i -> parseMediaItem(it.getJSONObject(i)) } } ?: emptyList()
    )

    private fun serializeMediaItem(it: MediaItem) = JSONObject().apply {
        put("id", it.id); put("type", it.type.name); put("path", it.path)
        put("start", it.startTime); put("duration", it.duration)
    }

    private fun parseMediaItem(obj: JSONObject) = MediaItem(
        id = obj.getString("id"),
        type = MediaType.valueOf(obj.getString("type")),
        path = obj.getString("path"),
        startTime = obj.getLong("start"),
        duration = obj.getLong("duration")
    )

    suspend fun setEventsViewMode(ctx: Context, mode: EventsViewMode) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("eventsViewMode")] = mode.name }
    }
    suspend fun setEventsSortOption(ctx: Context, option: ListSortOption) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("eventsSortOption")] = option.name }
    }
    suspend fun setEventsSortAscending(ctx: Context, ascending: Boolean) {
        ctx.dataStore.edit { p -> p[booleanPreferencesKey("eventsSortAscending")] = ascending }
    }
    suspend fun setEventsGroupOption(ctx: Context, option: ListGroupOption) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("eventsGroupOption")] = option.name }
    }

    suspend fun setMacrosViewMode(ctx: Context, mode: EventsViewMode) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("macrosViewMode")] = mode.name }
    }
    suspend fun setMacrosSortOption(ctx: Context, option: ListSortOption) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("macrosSortOption")] = option.name }
    }
    suspend fun setMacrosSortAscending(ctx: Context, ascending: Boolean) {
        ctx.dataStore.edit { p -> p[booleanPreferencesKey("macrosSortAscending")] = ascending }
    }
    suspend fun setGlyphPatternsSortAscending(ctx: Context, ascending: Boolean) {
        ctx.dataStore.edit { p -> p[booleanPreferencesKey("glyphPatternsSortAscending")] = ascending }
    }
    suspend fun setMacrosGroupOption(ctx: Context, option: ListGroupOption) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("macrosGroupOption")] = option.name }
    }

    suspend fun setGlyphPatternsViewMode(ctx: Context, mode: EventsViewMode) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("glyphPatternsViewMode")] = mode.name }
    }
    suspend fun setGlyphPatternsSortOption(ctx: Context, option: ListSortOption) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("glyphPatternsSortOption")] = option.name }
    }
    suspend fun setGlyphPatternsGroupOption(ctx: Context, option: ListGroupOption) {
        ctx.dataStore.edit { p -> p[stringPreferencesKey("glyphPatternsGroupOption")] = option.name }
    }

    suspend fun setDisableAnimations(ctx: Context, disable: Boolean) {
        ctx.dataStore.edit { p ->
            p[booleanPreferencesKey("disableAnimations")] = disable
        }
    }

    suspend fun exportConfig(ctx: Context): String {
        val p = ctx.dataStore.data.first()
        val json = JSONObject()
        p.asMap().forEach { (key, value) ->
            json.put(key.name, value)
        }
        return json.toString(4)
    }

    suspend fun importConfig(ctx: Context, jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            ctx.dataStore.edit { p ->
                p.clear()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val keyName = keys.next()
                    val value = json.get(keyName)
                    when (value) {
                        is String -> p[stringPreferencesKey(keyName)] = value
                        is Boolean -> p[booleanPreferencesKey(keyName)] = value
                        is Float -> p[floatPreferencesKey(keyName)] = value
                        is Double -> p[floatPreferencesKey(keyName)] = value.toFloat()
                        is Int -> {
                            // Some float values might be stored as Int if they are whole numbers
                            if (keyName == "panelYOffset") p[floatPreferencesKey(keyName)] = value.toFloat()
                            else p[stringPreferencesKey(keyName)] = value.toString()
                        }
                    }
                }
                // Ensure seeded is true after import
                p[booleanPreferencesKey("seeded")] = true
            }
            true
        } catch (e: Exception) {
            Log.e("Config", "Import failed", e)
            false
        }
    }

    /** Synchronous read for the accessibility service (re-read per event is fine). */
    suspend fun applySpecificDefaults(ctx: Context) {
        ctx.dataStore.edit { p ->
            // Panel position (Only if not set)
            val yKey = floatPreferencesKey("panelYOffset")
            if (p[yKey] == null) p[yKey] = 0.5715227f
            
            // Merge events
            val currentEventsJson = p[stringPreferencesKey("events")]
            val currentEvents = currentEventsJson?.let {
                try {
                    val arr = JSONArray(it)
                    MutableList(arr.length()) { i -> parseEvent(arr.getJSONObject(i)) }
                } catch (e: Exception) { mutableListOf() }
            } ?: mutableListOf()

            val defaultsToAdd = listOf(
                EventRule(
                    id = "706a3ef6-fd74-4ac3-91f8-7649fb842e65", icon = "☀️", title = "Яркость 15%",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "0-35"), EventCondition(ConditionType.BRIGHTNESS, "15-100")),
                    actions = listOf(ACTION_KILL_NOTES, "action:system:brightness:10")
                ),
                EventRule(
                    id = "b98b3457-580f-44f8-9e09-806b5eeb7825", icon = "☀️", title = "Яркость 50%(Увеличение)",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "35-75"), EventCondition(ConditionType.BRIGHTNESS, "0-30")),
                    actions = listOf("action:system:brightness:50", ACTION_KILL_NOTES)
                ),
                EventRule(
                    id = "d0b421c2-e1f6-4073-8ddc-6bfd77512da2", icon = "☀️", title = "Яркость 50%(Уменьшение)",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "35-75"), EventCondition(ConditionType.BRIGHTNESS, "60-100")),
                    actions = listOf("action:system:brightness:50", ACTION_KILL_NOTES)
                ),
                EventRule(
                    id = "6ed211a4-4461-4b64-97f3-07078672319f", icon = "☀️", title = "Яркость 100%",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(EventCondition(ConditionType.LIGHT, "75-100"), EventCondition(ConditionType.BRIGHTNESS, "0-60")),
                    actions = listOf("action:system:brightness:100", ACTION_KILL_NOTES)
                ),
                EventRule(
                    id = "3b6d11e7-a975-4113-a70f-9d52e51c4efe", icon = "💥", title = "Ночной фонарик",
                    triggerGesture = TapSlot.SINGLE,
                    conditions = listOf(
                        EventCondition(ConditionType.LIGHT, "0-60"), 
                        EventCondition(ConditionType.BRIGHTNESS, "0-45"),
                        EventCondition(ConditionType.ROTATION, "FACE_UP"),
                        EventCondition(ConditionType.TIME, "23:00-06:00")
                    ),
                    actions = listOf(ACTION_KILL_ESSENTIAL, "action:wait_confirmation:true,false,false,0", ACTION_FLASHLIGHT)
                )
            )

            defaultsToAdd.forEach { default ->
                if (currentEvents.none { it.id == default.id }) {
                    currentEvents.add(default)
                }
            }

            val arr = JSONArray()
            currentEvents.forEach { arr.put(serializeEvent(it)) }
            p[stringPreferencesKey("events")] = arr.toString()
        }
    }

    fun readBlocking(ctx: Context): KeyMap = runBlocking { flow(ctx).first() }
}
