package com.HojuNK.essent_all.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.view.Surface as ViewSurface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import com.HojuNK.essent_all.ui.editor.GlyphAddPanel
import com.HojuNK.essent_all.EssentialKeyService
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GlyphPatternsScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onEditPattern: (Macro) -> Unit,
    onShowAccessibilityError: () -> Unit,
    isSelectionMode: Boolean = false,
    onSelectionModeChange: (Boolean) -> Unit = {}
) {
    val radialMenuState = LocalRadialMenuState.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val glyphActive by GlyphController.glyphActive.collectAsState()
    
    var showViewAndSortDialog by remember { mutableStateOf(false) }

    var localSelectionMode by remember { mutableStateOf(isSelectionMode) }
    LaunchedEffect(isSelectionMode) {
        localSelectionMode = isSelectionMode
    }
    val updateSelectionMode: (Boolean) -> Unit = { active ->
        localSelectionMode = active
        onSelectionModeChange(active)
    }

    var selectedPatternIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = localSelectionMode) {
        updateSelectionMode(false)
        selectedPatternIds = emptySet()
    }

    val sortedPatterns = remember(keyMap.glyphPatterns, keyMap.glyphPatternsSortOption, keyMap.glyphPatternsSortAscending) {
        val base = keyMap.glyphPatterns.map { if (it.createdAt <= 0L) it.copy(createdAt = System.currentTimeMillis()) else it }
        val ascending = keyMap.glyphPatternsSortAscending
        when (keyMap.glyphPatternsSortOption) {
            ListSortOption.CREATION_DATE -> if (ascending) base.sortedBy { it.createdAt } else base.sortedByDescending { it.createdAt }
            ListSortOption.ALPHABETICAL -> if (ascending) base.sortedBy { it.title.lowercase() } else base.sortedByDescending { it.title.lowercase() }
            ListSortOption.RANKING -> if (ascending) base else base.reversed()
        }
    }

    val configuration = LocalConfiguration.current
    val rotation = ctx.display?.rotation ?: ViewSurface.ROTATION_0
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nothingBlur(enabled = radialMenuState != null),
            topBar = {
                if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                    TopAppBar(
                        title = {
                            if (localSelectionMode) {
                                Text(stringResource(R.string.selected_count, selectedPatternIds.size), color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                TypewriterTitle(targetText = stringResource(R.string.games_title), animate = false)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .drawBehind {
                                val blurHeight = 45.dp.toPx()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Black.copy(alpha = 0.5f)),
                                        startY = 0f,
                                        endY = size.height
                                    ),
                                    size = size
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
                                        startY = size.height,
                                        endY = size.height + blurHeight
                                    ),
                                    topLeft = Offset(0f, size.height),
                                    size = Size(size.width, blurHeight)
                                )
                            },
                        navigationIcon = {
                            if (localSelectionMode) {
                                IconButton(onClick = { updateSelectionMode(false); selectedPatternIds = emptySet() }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        },
                        actions = {
                            if (localSelectionMode) {
                                val isAllSelected = sortedPatterns.isNotEmpty() && sortedPatterns.all { selectedPatternIds.contains(it.id) }
                                IconButton(onClick = {
                                    selectedPatternIds = if (isAllSelected) emptySet() else sortedPatterns.map { it.id }.toSet()
                                }) {
                                    Icon(if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll, null, tint = Color.White)
                                }
                            } else {
                                IconButton(onClick = { updateSelectionMode(true) }) {
                                    Icon(Icons.Default.Checklist, null, tint = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .combinedClickable(
                                            onClick = {
                                                val nextMode = if (keyMap.glyphPatternsViewMode == EventsViewMode.STRIPES) EventsViewMode.RECTANGLES else EventsViewMode.STRIPES
                                                scope.launch { Config.setGlyphPatternsViewMode(ctx, nextMode) }
                                            },
                                            onLongClick = { showViewAndSortDialog = true }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (keyMap.glyphPatternsViewMode == EventsViewMode.STRIPES) Icons.Default.ViewStream else Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    GlyphController.toggleGlyph()
                                }) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = if (glyphActive) Color(0xFFFFD700) else Color.White
                                    )
                                }
                                IconButton(onClick = onOpenSettings) {
                                    Icon(Icons.Default.Settings, null)
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButtonPosition = if (isLandscape && rotation == ViewSurface.ROTATION_270) FabPosition.Start else FabPosition.End,
            floatingActionButton = {
                if (!localSelectionMode) {
                    FloatingActionButton(
                        onClick = { 
                            onEditPattern(Macro(UUID.randomUUID().toString(), "✨", "", actions = emptyList(), isEnabled = true))
                        },
                        containerColor = Color.Gray,
                        contentColor = Color.White,
                        modifier = Modifier.padding(
                            bottom = if (isLandscape) 24.dp else 120.dp,
                            start = if (isLandscape && rotation == ViewSurface.ROTATION_270) 135.dp else 0.dp,
                            end = if (isLandscape && rotation == ViewSurface.ROTATION_90) 135.dp else 0.dp
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        ) { inner ->
            val topPadding = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                inner.calculateTopPadding()
            } else {
                0.dp
            }
            
            val startPadding = if (isLandscape) {
                if (rotation == ViewSurface.ROTATION_270) 130.dp else 80.dp
            } else 0.dp
            
            val endPadding = if (isLandscape) {
                if (rotation == ViewSurface.ROTATION_90) 130.dp else 80.dp
            } else 0.dp
            
            if (sortedPatterns.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(top = topPadding, start = startPadding, end = endPadding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_macros_msg), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                val isStripeMode = keyMap.glyphPatternsViewMode == EventsViewMode.STRIPES
                val numColumns = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                    if (isStripeMode) 2 else 4
                } else {
                    if (isStripeMode) 1 else 2
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(numColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp + startPadding, 
                        end = 16.dp + endPadding, 
                        top = topPadding + 16.dp, 
                        bottom = inner.calculateBottomPadding() + 16.dp + (if (isLandscape) 0.dp else 120.dp)
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedPatterns, key = { it.id }) { pattern ->
                        val isSelected = selectedPatternIds.contains(pattern.id)
                        val onSelectToggle = {
                            selectedPatternIds = if (isSelected) selectedPatternIds - pattern.id else selectedPatternIds + pattern.id
                        }
                        if (isStripeMode) {
                            StripeMacroItem(
                                keyMap = keyMap,
                                macro = pattern,
                                isSelectionMode = localSelectionMode,
                                isSelected = isSelected,
                                onSelectToggle = onSelectToggle,
                                onToggle = { enabled ->
                                    scope.launch {
                                        val updated = pattern.copy(isEnabled = enabled)
                                        Config.updateGlyphPattern(ctx, updated)
                                    }
                                },
                                onEdit = { onEditPattern(pattern) },
                                onDuplicate = {
                                    val newPattern = pattern.copy(id = UUID.randomUUID().toString(), title = "${pattern.title} (копия)", createdAt = System.currentTimeMillis())
                                    scope.launch { Config.updateGlyphPattern(ctx, newPattern) }
                                },
                                onDelete = {
                                    scope.launch { Config.deleteGlyphPattern(ctx, pattern.id) }
                                }
                            )
                        } else {
                            RectangleMacroItem(
                                keyMap = keyMap,
                                macro = pattern,
                                isSelectionMode = localSelectionMode,
                                isSelected = isSelected,
                                onSelectToggle = onSelectToggle,
                                onToggle = { enabled ->
                                    scope.launch {
                                        val updated = pattern.copy(isEnabled = enabled)
                                        Config.updateGlyphPattern(ctx, updated)
                                    }
                                },
                                onEdit = { onEditPattern(pattern) },
                                onDuplicate = {
                                    val newPattern = pattern.copy(id = UUID.randomUUID().toString(), title = "${pattern.title} (копия)", createdAt = System.currentTimeMillis())
                                    scope.launch { Config.updateGlyphPattern(ctx, newPattern) }
                                },
                                onDelete = {
                                    scope.launch { Config.deleteGlyphPattern(ctx, pattern.id) }
                                },
                                onPlay = {
                                    GlyphController.playGlyphPattern(pattern)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (localSelectionMode) {
            val panelModifier = when {
                isLandscape && rotation == ViewSurface.ROTATION_90 -> Modifier.align(Alignment.CenterEnd)
                isLandscape && rotation == ViewSurface.ROTATION_270 -> Modifier.align(Alignment.CenterStart)
                else -> Modifier.align(Alignment.BottomCenter)
            }
            MacroSelectionBottomPanel(
                selectedCount = selectedPatternIds.size,
                isLandscape = isLandscape,
                rotation = rotation,
                onEnable = {
                    val ids = selectedPatternIds
                    scope.launch {
                        ids.forEach { id ->
                            keyMap.glyphPatterns.find { it.id == id }?.let { p ->
                                Config.updateGlyphPattern(ctx, p.copy(isEnabled = true))
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedPatternIds = emptySet()
                },
                onDisable = {
                    val ids = selectedPatternIds
                    scope.launch {
                        ids.forEach { id ->
                            keyMap.glyphPatterns.find { it.id == id }?.let { p ->
                                Config.updateGlyphPattern(ctx, p.copy(isEnabled = false))
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedPatternIds = emptySet()
                },
                onDuplicate = {
                    val idsToDup = selectedPatternIds
                    scope.launch {
                        idsToDup.forEach { id ->
                            keyMap.glyphPatterns.find { it.id == id }?.let { p ->
                                val newPattern = p.copy(id = UUID.randomUUID().toString(), title = "${p.title} (копия)", createdAt = System.currentTimeMillis())
                                Config.updateGlyphPattern(ctx, newPattern)
                            }
                        }
                    }
                    updateSelectionMode(false)
                    selectedPatternIds = emptySet()
                },
                onDelete = {
                    showBatchDeleteConfirm = true
                },
                modifier = panelModifier
            )
        }

        if (showBatchDeleteConfirm) {
            ConfirmDeleteDialog(
                title = stringResource(R.string.delete_selected_q, selectedPatternIds.size),
                onConfirm = { dontAskAgain ->
                    if (dontAskAgain) {
                        scope.launch { Config.setConfirmDeletion(ctx, false) }
                    }
                    val idsToDelete = selectedPatternIds
                    scope.launch {
                        idsToDelete.forEach { id -> Config.deleteGlyphPattern(ctx, id) }
                    }
                    showBatchDeleteConfirm = false
                    updateSelectionMode(false)
                    selectedPatternIds = emptySet()
                },
                onDismiss = { showBatchDeleteConfirm = false }
            )
        }

        if (showViewAndSortDialog) {
            ViewAndSortDialog(
                viewMode = keyMap.glyphPatternsViewMode,
                onViewModeChange = { scope.launch { Config.setGlyphPatternsViewMode(ctx, it) } },
                sortOption = keyMap.glyphPatternsSortOption,
                onSortOptionChange = { scope.launch { Config.setGlyphPatternsSortOption(ctx, it) } },
                sortAscending = keyMap.glyphPatternsSortAscending,
                onSortAscendingToggle = { scope.launch { Config.setGlyphPatternsSortAscending(ctx, !keyMap.glyphPatternsSortAscending) } },
                showRankingOption = false,
                onDismiss = { showViewAndSortDialog = false }
            )
        }
    }
}
