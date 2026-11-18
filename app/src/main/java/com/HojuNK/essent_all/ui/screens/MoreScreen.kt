package com.HojuNK.essent_all.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.AppInfo
import com.HojuNK.essent_all.EventRule
import com.HojuNK.essent_all.KeyMap
import com.HojuNK.essent_all.Macro
import com.HojuNK.essent_all.model.NavScreen
import com.HojuNK.essent_all.ui.components.TypewriterTitle
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.model.LocalRadialMenuState
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onEditPattern: (Macro) -> Unit,
    onShowAccessibilityError: () -> Unit,
    isSelectionMode: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {},
    onSubScreenChanged: (Boolean) -> Unit = {},
    isEditingHotbar: Boolean = false,
    onToggleEditHotbar: () -> Unit = {},
    hotbarMiddleScreens: List<NavScreen> = listOf(NavScreen.EVENTS, NavScreen.NOTES),
    onSwapScreen: (NavScreen) -> Unit = {},
    onEditEvent: (EventRule) -> Unit = {},
    onOpenPriority: () -> Unit = {},
    onEditMacro: (Macro) -> Unit = {},
    isEventsSelectionMode: Boolean = false,
    onEventsSelectionModeChange: (Boolean) -> Unit = {},
    isMacrosSelectionMode: Boolean = false,
    onMacrosSelectionModeChange: (Boolean) -> Unit = {},
    onNotesEditorToggle: (Boolean) -> Unit = {},
    isNotesSelectionMode: Boolean = false,
    onNotesSelectionModeChange: (Boolean) -> Unit = {},
    onNotesSelectedCountChanged: (Int) -> Unit = {},
    onNotesSelectAllActionChanged: (() -> Unit) -> Unit = {},
    isNotesSearchActive: Boolean = false,
    onNotesSearchActiveChange: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val radialMenuState = LocalRadialMenuState.current
    var activeSubScreen by remember { mutableStateOf<NavScreen?>(null) }

    val isAnySelectionActive = isSelectionMode || isEventsSelectionMode || isMacrosSelectionMode || isNotesSelectionMode
    BackHandler(enabled = isEditingHotbar || (activeSubScreen == null && (isAnySelectionActive || onBack != null))) {
        when {
            isEditingHotbar -> {
                onToggleEditHotbar()
            }
            isAnySelectionActive -> {
                if (isSelectionMode) onSelectionModeChange(false)
                if (isEventsSelectionMode) onEventsSelectionModeChange(false)
                if (isMacrosSelectionMode) onMacrosSelectionModeChange(false)
                if (isNotesSelectionMode) onNotesSelectionModeChange(false)
            }
            else -> {
                onBack?.invoke()
            }
        }
    }

    LaunchedEffect(activeSubScreen) {
        onSubScreenChanged(activeSubScreen != null)
    }

    val backAnim = remember { Animatable(0f) }
    var isBackGestureActive by remember { mutableStateOf(false) }

    PredictiveBackHandler(enabled = activeSubScreen != null) { progressFlow ->
        isBackGestureActive = true
        try {
            progressFlow.collect { backEvent ->
                backAnim.snapTo(backEvent.progress)
            }
            backAnim.animateTo(1f, animationSpec = tween(150, easing = LinearOutSlowInEasing))
            activeSubScreen = null
        } catch (_: CancellationException) {
            backAnim.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
        } finally {
            backAnim.snapTo(0f)
            isBackGestureActive = false
        }
    }

    val predictiveProgress = backAnim.value
    val subScreenModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            val scale = 1f - predictiveProgress * 0.08f
            scaleX = scale
            scaleY = scale
            translationX = predictiveProgress * 80f
            alpha = 1f - predictiveProgress * 0.2f
        }

    AnimatedContent(
        targetState = activeSubScreen,
        label = "MoreScreenTransition"
    ) { currentSubScreen ->
        Box(modifier = if (currentSubScreen != null) subScreenModifier else Modifier.fillMaxSize()) {
            when (currentSubScreen) {
                NavScreen.GLYPHS -> {
                    GlyphPatternsScreen(
                        keyMap = keyMap,
                        apps = apps,
                        onOpenSettings = onOpenSettings,
                        onEditPattern = onEditPattern,
                        onShowAccessibilityError = onShowAccessibilityError,
                        isSelectionMode = isSelectionMode,
                        onSelectionModeChange = onSelectionModeChange,
                        onBack = { activeSubScreen = null }
                    )
                }
                NavScreen.NOTES -> {
                    NotesScreen(
                        keyMap = keyMap,
                        apps = apps,
                        onOpenSettings = onOpenSettings,
                        onShowAccessibilityError = onShowAccessibilityError,
                        onBack = { activeSubScreen = null },
                        onEditorToggle = onNotesEditorToggle,
                        isSelectionMode = isNotesSelectionMode,
                        onSelectionModeChange = onNotesSelectionModeChange,
                        onSelectedCountChanged = onNotesSelectedCountChanged,
                        onSelectAllActionChanged = onNotesSelectAllActionChanged,
                        isSearchActive = isNotesSearchActive,
                        onSearchActiveChange = onNotesSearchActiveChange
                    )
                }
                NavScreen.EVENTS -> {
                    EventsScreen(
                        keyMap = keyMap,
                        apps = apps,
                        onOpenSettings = onOpenSettings,
                        onEditEvent = onEditEvent,
                        onOpenPriority = onOpenPriority,
                        isSelectionMode = isEventsSelectionMode,
                        onSelectionModeChange = onEventsSelectionModeChange,
                        onBack = { activeSubScreen = null }
                    )
                }
                NavScreen.MACROS -> {
                    MacrosScreen(
                        keyMap = keyMap,
                        apps = apps,
                        onOpenSettings = onOpenSettings,
                        onEditMacro = onEditMacro,
                        onShowAccessibilityError = onShowAccessibilityError,
                        isSelectionMode = isMacrosSelectionMode,
                        onSelectionModeChange = onMacrosSelectionModeChange,
                        onBack = { activeSubScreen = null }
                    )
                }
                else -> {
                    val configuration = LocalConfiguration.current
                    val gridScreens = remember(hotbarMiddleScreens) {
                        NavScreen.entries.filter { it.isCustomizable && !hotbarMiddleScreens.contains(it) }
                    }

                    val useDark = com.HojuNK.essent_all.LocalUseDark.current
                    val topBarBg = if (useDark) Color.Black else Color.White
                    val textColor = if (useDark) Color.White else Color.Black

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nothingBlur(enabled = radialMenuState != null),
                        topBar = {
                            if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                                TopAppBar(
                                    title = {
                                        TypewriterTitle(
                                            targetText = if (isEditingHotbar) "Редактирование хотбара" else stringResource(R.string.nav_more),
                                            animate = false
                                        )
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        actionIconContentColor = textColor,
                                        navigationIconContentColor = textColor,
                                        titleContentColor = textColor
                                    ),
                                    modifier = Modifier.drawBehind {
                                        val blurHeight = 45.dp.toPx()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(topBarBg, topBarBg.copy(alpha = 0.5f)),
                                                startY = 0f,
                                                endY = size.height
                                            ),
                                            size = size
                                        )
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(topBarBg.copy(alpha = 0.5f), Color.Transparent),
                                                startY = size.height,
                                                endY = size.height + blurHeight
                                            ),
                                            topLeft = Offset(0f, size.height),
                                            size = Size(size.width, blurHeight)
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = onToggleEditHotbar) {
                                            Icon(
                                                imageVector = if (isEditingHotbar) Icons.Default.Check else Icons.Default.Edit,
                                                contentDescription = if (isEditingHotbar) "Done" else "Edit Hotbar",
                                                tint = textColor
                                            )
                                        }
                                        if (!isEditingHotbar) {
                                            IconButton(onClick = onOpenSettings) {
                                                Icon(Icons.Default.Settings, null, tint = textColor)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(gridScreens.size) { index ->
                                    val screen = gridScreens[index]
                                    MoreGridTile(
                                        title = stringResource(screen.labelRes),
                                        icon = screen.icon,
                                        isEditing = isEditingHotbar,
                                        onClick = {
                                            if (isEditingHotbar) {
                                                onSwapScreen(screen)
                                            } else {
                                                activeSubScreen = screen
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreGridTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false
) {
    val useDark = com.HojuNK.essent_all.LocalUseDark.current
    val containerColor = if (isEditing) (if (useDark) Color(0xFF222222) else Color(0xFFE0E0E0)) else (if (useDark) Color(0xFF161616) else Color(0xFFF2F2F2))
    val borderColor = if (isEditing) (if (useDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)) else (if (useDark) Color.White.copy(alpha = 0.12f) else Color.LightGray)
    val tileIconColor = if (useDark) Color.White else Color.Black
    val tileTextColor = if (useDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tileIconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = tileTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isEditing) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    tint = tileIconColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(14.dp)
                )
            }
        }
    }
}
