package com.HojuNK.essent_all

import android.content.Context
import com.HojuNK.essent_all.R
import kotlin.math.max

enum class ActionCategory(val titleRes: Int) {
    QUICK_SYSTEM(R.string.group_quick_system),
    CAPTURE_READING(R.string.group_capture_reading),
    MEDIA(R.string.group_media),
    NAVIGATION(R.string.group_navigation),
    LAUNCH(R.string.group_launch)
}

data class ActionItem(
    val id: String,
    val titleRes: Int,
    val category: ActionCategory,
    val pickerType: String? = null
)

object ActionRegistry {
    val allActions = listOf(
        // Quick & System
        ActionItem(Config.ACTION_FLASHLIGHT, R.string.action_flashlight_toggle, ActionCategory.QUICK_SYSTEM, "flashlight"),
        ActionItem(Config.ACTION_GLYPH, R.string.action_glyph_toggle, ActionCategory.QUICK_SYSTEM, "glyph"),
        ActionItem(Config.ACTION_ROTATION_TOGGLE, R.string.action_rotation_lock_title, ActionCategory.QUICK_SYSTEM),
        ActionItem("action:pick_auto_brightness", R.string.action_auto_brightness_dots, ActionCategory.QUICK_SYSTEM, "auto_brightness"),
        ActionItem(Config.ACTION_RING_VIBRATE, R.string.action_ring_vibrate_title, ActionCategory.QUICK_SYSTEM),
        ActionItem(Config.ACTION_DND_TOGGLE, R.string.action_dnd, ActionCategory.QUICK_SYSTEM),
        ActionItem(Config.ACTION_LOCK_SCREEN, R.string.action_lock_screen, ActionCategory.QUICK_SYSTEM),
        ActionItem("action:pick_brightness", R.string.action_set_brightness, ActionCategory.QUICK_SYSTEM, "brightness"),
        ActionItem("action:pick_theme", R.string.action_night_light_title, ActionCategory.QUICK_SYSTEM, "theme"),
        ActionItem("action:pick_delay", R.string.action_delay, ActionCategory.QUICK_SYSTEM, "delay"),
        ActionItem("action:pick_wait_confirmation", R.string.action_wait_confirmation, ActionCategory.QUICK_SYSTEM, "wait_confirmation"),
        ActionItem(Config.ACTION_WIFI_TOGGLE, R.string.action_internet_title, ActionCategory.QUICK_SYSTEM),
        ActionItem(Config.ACTION_BLUETOOTH_TOGGLE, R.string.action_bluetooth_title, ActionCategory.QUICK_SYSTEM),
        ActionItem(Config.ACTION_POWER_MENU, R.string.action_power_menu_title, ActionCategory.QUICK_SYSTEM),
        ActionItem(Config.ACTION_EXTRA_DIM, R.string.action_extra_dim_title, ActionCategory.QUICK_SYSTEM),

        // Capture & Reading
        ActionItem(Config.ACTION_SCREENSHOT, R.string.action_screenshot, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_READ_SCREEN, R.string.action_read_screen_title, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_READ_ALOUD, R.string.action_read_aloud_title, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_COPY, R.string.action_copy_title, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_PASTE, R.string.action_paste_title, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_CLIPBOARD, R.string.action_clipboard_title, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_OPEN_CAMERA, R.string.action_open_camera_title, ActionCategory.CAPTURE_READING),
        ActionItem(Config.ACTION_CAMERA_SHUTTER, R.string.action_camera_shutter_title, ActionCategory.CAPTURE_READING),

        // Sound & Media
        ActionItem("action:pick_volume", R.string.action_volume_control, ActionCategory.MEDIA, "volume"),
        ActionItem("action:system:media:play_pause", R.string.action_play_pause_title, ActionCategory.MEDIA),
        ActionItem("action:system:media:next", R.string.action_next_track_title, ActionCategory.MEDIA),
        ActionItem("action:system:media:prev", R.string.action_previous_track_title, ActionCategory.MEDIA),
        ActionItem("action:system:media:mute", R.string.action_mute_media_title, ActionCategory.MEDIA),

        // Navigation
        ActionItem(Config.ACTION_BACK, R.string.action_back, ActionCategory.NAVIGATION),
        ActionItem(Config.ACTION_HOME, R.string.action_home, ActionCategory.NAVIGATION),
        ActionItem(Config.ACTION_NOTIFICATIONS, R.string.action_notifications, ActionCategory.NAVIGATION),
        ActionItem(Config.ACTION_RECENTS, R.string.action_recent_apps_title, ActionCategory.NAVIGATION),
        ActionItem(Config.ACTION_QUICK_SETTINGS, R.string.action_quick_settings_title, ActionCategory.NAVIGATION),
        ActionItem(Config.ACTION_CLEAR_NOTIFICATIONS, R.string.action_clear_notifications, ActionCategory.NAVIGATION),

        // Launch & Shortcuts
        ActionItem("action:full_app_list", R.string.open_app, ActionCategory.LAUNCH, "app"),
        ActionItem(Config.ACTION_CALCULATOR, R.string.action_calculator_title, ActionCategory.LAUNCH),
        ActionItem(Config.ACTION_SHORTCUT_PREFIX, R.string.action_app_shortcut_title, ActionCategory.LAUNCH),
        ActionItem(Config.ACTION_CONTROLS, R.string.control_panel, ActionCategory.LAUNCH),
        ActionItem(Config.ACTION_FAVORITES_PREFIX, R.string.favorites, ActionCategory.LAUNCH),
        ActionItem(Config.ACTION_MACRO_PREFIX, R.string.action_macro, ActionCategory.LAUNCH, "macro"),
        ActionItem("action:pick_url", R.string.action_open_url, ActionCategory.LAUNCH, "url"),
        ActionItem("action:pick_glyph_pattern", R.string.action_play_glyph_pattern, ActionCategory.LAUNCH),
        ActionItem("action:pick_touch", R.string.action_touch_screen, ActionCategory.LAUNCH, "touch"),
        ActionItem("action:pick_swipe", R.string.action_swipe_screen, ActionCategory.LAUNCH, "swipe")
    )
}

object ActionHistory {
    private const val PREF_KEY = "recent_action_history"

    fun getRecentActions(ctx: Context): List<String> {
        val prefs = ctx.getSharedPreferences("action_history_pref", Context.MODE_PRIVATE)
        val saved = prefs.getString(PREF_KEY, "") ?: ""
        return if (saved.isEmpty()) emptyList() else saved.split(",").filter { it.isNotEmpty() }
    }

    fun addAction(ctx: Context, actionId: String) {
        val current = getRecentActions(ctx).toMutableList()
        current.remove(actionId)
        current.add(0, actionId)
        val updated = current.take(5)
        val prefs = ctx.getSharedPreferences("action_history_pref", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY, updated.joinToString(",")).apply()
    }
}

fun levenshtein(s1: String, s2: String): Int {
    val a = s1.lowercase()
    val b = s2.lowercase()
    val memo = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) memo[i][0] = i
    for (j in 0..b.length) memo[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            memo[i][j] = minOf(
                memo[i - 1][j] + 1,
                memo[i][j - 1] + 1,
                memo[i - 1][j - 1] + cost
            )
        }
    }
    return memo[a.length][b.length]
}

fun fuzzyMatch(query: String, target: String): Boolean {
    val q = query.trim().lowercase()
    val t = target.trim().lowercase()
    if (q.isEmpty()) return true
    if (t.contains(q)) return true
    val dist = levenshtein(q, t)
    val allowedErrors = (max(q.length, t.length) * 0.2).toInt()
    return dist <= allowedErrors || t.split(" ").any { word -> levenshtein(q, word) <= (max(q.length, word.length) * 0.2).toInt() }
}
