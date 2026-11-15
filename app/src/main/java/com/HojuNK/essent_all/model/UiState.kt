package com.HojuNK.essent_all.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TouchApp
import com.HojuNK.essent_all.R

enum class MenuType { RADIAL, RECTANGULAR }

data class RadialMenuState(
    val center: Offset,
    val actions: List<RadialAction>,
    val hoveredIndex: Int = -1,
    val sourceId: String? = null,
    val type: MenuType = MenuType.RADIAL,
    val sourceBounds: Rect? = null,
    val sourceContent: @Composable () -> Unit = {}
)

val LocalRadialMenu = compositionLocalOf <((RadialMenuState?) -> Unit)> { { } }
val LocalRadialMenuState = compositionLocalOf<RadialMenuState?> { null }
val LocalOrientationLock = compositionLocalOf <((Boolean) -> Unit)> { { } }

enum class NavScreen(val labelRes: Int, val icon: ImageVector, val title: String) {
    KEY(R.string.nav_key, Icons.Filled.TouchApp, "Key"),
    EVENTS(R.string.nav_events, Icons.Filled.Bolt, "Events"),
    MACROS(R.string.nav_macros, Icons.Filled.AutoGraph, "Macros"),
    GLYPHS(R.string.more_glyphs, Icons.Filled.Lightbulb, "Glyphs"),
    NOTES(R.string.more_notes, Icons.AutoMirrored.Filled.Assignment, "Notes"),
    MORE(R.string.nav_more, Icons.Filled.GridView, "More");

    val isCustomizable: Boolean get() = this != KEY && this != MORE
}

enum class LoadingStage {
    INITIAL,
    CORE_READY,
    TRANSITIONING,
    FULLY_LOADED
}

data class RadialAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

data class MacroActionTime(
    val id: String,
    val index: Int,
    val type: String,
    val startTime: Long,
    val duration: Long
)
