package com.HojuNK.essent_all.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Grain
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

enum class NavScreen(val labelRes: Int, val icon: ImageVector) {
    KEY(R.string.nav_key, Icons.Filled.TouchApp),
    EVENTS(R.string.nav_events, Icons.Filled.Bolt),
    MACROS(R.string.nav_macros, Icons.Filled.AutoGraph),
    GAMES(R.string.nav_games, Icons.Filled.Grain)
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
