package com.HojuNK.essent_all.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import android.content.Intent
import android.view.Surface as ViewSurface
import androidx.compose.foundation.BorderStroke
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.util.nothingBlur
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConfigScreen(
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    onOpenSettings: () -> Unit,
    onActionPickerStateChange: ((Boolean) -> Unit)? = null,
    onOpenBlockDetail: (String, List<TapSlot>) -> Unit
) {
    val radialMenuState = LocalRadialMenuState.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val setLock = LocalOrientationLock.current
    var picker by remember { mutableStateOf<TapSlot?>(null) }
    var showAssistantPermissionDialog by remember { mutableStateOf(false) }

    var showTouchForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showSwipeForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showBrightnessForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showFullAppPickerForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showVolumeForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showMediaForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showUrlForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showThemeForSlot by remember { mutableStateOf<TapSlot?>(null) }
    var showAutoBrightnessForSlot by remember { mutableStateOf<TapSlot?>(null) }

    val essentialTitle = stringResource(R.string.section_essential_key)
    val powerTitle = stringResource(R.string.section_power_button)
    val volUpTitle = stringResource(R.string.section_volume_up)
    val volDownTitle = stringResource(R.string.section_volume_down)

    val anyPickerOpen = picker != null || showTouchForSlot != null || showSwipeForSlot != null || 
                       showBrightnessForSlot != null || showFullAppPickerForSlot != null || 
                       showVolumeForSlot != null || showMediaForSlot != null || 
                       showUrlForSlot != null || showThemeForSlot != null || 
                       showAutoBrightnessForSlot != null

    LaunchedEffect(anyPickerOpen) {
        setLock(anyPickerOpen)
        onActionPickerStateChange?.invoke(anyPickerOpen)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nothingBlur(enabled = radialMenuState != null),
        topBar = {
            if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                TopAppBar(
                    title = { TypewriterTitle() },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .drawBehind {
                            val blurHeight = 45.dp.toPx()
                            // Размытие и затухание в шапке: градиент от полностью непрозрачного черного к мягкому прозрачному
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
                        }
                        .graphicsLayer {
                            if (radialMenuState != null) {
                                renderEffect = createNothingBlurEffect(45f).asComposeRenderEffect()
                                alpha = 0.6f
                            } else {
                                renderEffect = null
                                alpha = 1.0f
                            }
                        },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, null)
                        }
                    }
                )
            }
        }
    ) { inner ->
        val topPadding = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            inner.calculateTopPadding()
        } else {
            0.dp
        }
        
        val rotation = ctx.display?.rotation ?: ViewSurface.ROTATION_0
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        val startPadding = if (isLandscape) {
            if (rotation == ViewSurface.ROTATION_270) 130.dp else 80.dp
        } else 0.dp
        
        val endPadding = if (isLandscape) {
            if (rotation == ViewSurface.ROTATION_90) 130.dp else 80.dp
        } else 0.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp + startPadding,
                end = 16.dp + endPadding,
                top = topPadding + 16.dp,
                bottom = inner.calculateBottomPadding() + 16.dp + (if (isLandscape) 0.dp else 120.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(stringResource(R.string.assign_action_hint), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                HorizontalDivider()
            }

            // Block 1: Essential Key
            item {
                ActionBlockCard(
                    title = essentialTitle,
                    slots = listOf(TapSlot.SINGLE, TapSlot.LONG, TapSlot.DOUBLE, TapSlot.TRIPLE),
                    keyMap = keyMap,
                    apps = apps,
                    ctx = ctx,
                    scope = scope,
                    onClick = { onOpenBlockDetail(essentialTitle, listOf(TapSlot.SINGLE, TapSlot.LONG, TapSlot.DOUBLE, TapSlot.TRIPLE)) },
                    onPick = { picker = it },
                    onAppPick = { showFullAppPickerForSlot = it },
                    onPowerCheck = { showAssistantPermissionDialog = true }
                )
            }

            // Block 2: Power Button
            item {
                ActionBlockCard(
                    title = powerTitle,
                    slots = listOf(TapSlot.POWER_LONG),
                    keyMap = keyMap,
                    apps = apps,
                    ctx = ctx,
                    scope = scope,
                    onClick = { onOpenBlockDetail(powerTitle, listOf(TapSlot.POWER_LONG)) },
                    onPick = { picker = it },
                    onAppPick = { showFullAppPickerForSlot = it },
                    onPowerCheck = { showAssistantPermissionDialog = true }
                )
            }

            // Block 3: Volume Up Key
            item {
                ActionBlockCard(
                    title = volUpTitle,
                    slots = listOf(TapSlot.VOL_UP_SINGLE, TapSlot.VOL_UP_LONG, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE),
                    keyMap = keyMap,
                    apps = apps,
                    ctx = ctx,
                    scope = scope,
                    onClick = { onOpenBlockDetail(volUpTitle, listOf(TapSlot.VOL_UP_SINGLE, TapSlot.VOL_UP_LONG, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE)) },
                    onPick = { picker = it },
                    onAppPick = { showFullAppPickerForSlot = it },
                    onPowerCheck = { showAssistantPermissionDialog = true }
                )
            }

            // Block 4: Volume Down Key
            item {
                ActionBlockCard(
                    title = volDownTitle,
                    slots = listOf(TapSlot.VOL_DOWN_SINGLE, TapSlot.VOL_DOWN_LONG, TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE),
                    keyMap = keyMap,
                    apps = apps,
                    ctx = ctx,
                    scope = scope,
                    onClick = { onOpenBlockDetail(volDownTitle, listOf(TapSlot.VOL_DOWN_SINGLE, TapSlot.VOL_DOWN_LONG, TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE)) },
                    onPick = { picker = it },
                    onAppPick = { showFullAppPickerForSlot = it },
                    onPowerCheck = { showAssistantPermissionDialog = true }
                )
            }
            }
        }

    if (picker != null) {
        val slotToUpdate = picker!!
        AppPickerDialog(title = ctx.getString(slotToUpdate.labelRes), slot = slotToUpdate, current = keyMap.forSlot(slotToUpdate),
            onPick = { pkg -> 
                if (pkg == "action:pick_touch") showTouchForSlot = slotToUpdate
                else if (pkg == "action:pick_swipe") showSwipeForSlot = slotToUpdate
                else if (pkg == "action:pick_brightness") showBrightnessForSlot = slotToUpdate
                else if (pkg == "action:pick_volume") showVolumeForSlot = slotToUpdate
                else if (pkg == "action:pick_media") showMediaForSlot = slotToUpdate
                else if (pkg == "action:pick_url") showUrlForSlot = slotToUpdate
                else if (pkg == "action:pick_theme") showThemeForSlot = slotToUpdate
                else if (pkg == "action:pick_auto_brightness") showAutoBrightnessForSlot = slotToUpdate
                else if (pkg == "action:full_app_list") showFullAppPickerForSlot = slotToUpdate
                else {
                    if (slotToUpdate == TapSlot.POWER_LONG && !VoiceAssistantService.isAppCurrentAssistant(ctx)) {
                        showAssistantPermissionDialog = true
                    }
                    scope.launch { Config.set(ctx, slotToUpdate, pkg) }
                }
                picker = null 
            },
            onDismiss = { picker = null }
        )
    }

    if (showAssistantPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAssistantPermissionDialog = false },
            title = { Text(stringResource(R.string.assistant_permission_title), color = Color.White) },
            text = { Text(stringResource(R.string.assistant_permission_msg), color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    showAssistantPermissionDialog = false
                    VoiceAssistantService.openDefaultAssistantSettings(ctx)
                }) {
                    Text(stringResource(R.string.open_settings_btn), color = Color(0xFFFFD700))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssistantPermissionDialog = false }) {
                    Text(stringResource(android.R.string.cancel), color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    showFullAppPickerForSlot?.let { slot ->
        UniversalAppPickerDialog(
            apps = (apps ?: emptyList()).filter { it.isLaunchable },
            selected = listOfNotNull(keyMap.forSlot(slot).takeIf { it != null && !it.startsWith("action:") }),
            multiSelect = false,
            onConfirmed = { picked ->
                val pkg = picked.firstOrNull()
                if (pkg != null) {
                    scope.launch { Config.set(ctx, slot, pkg) }
                }
                showFullAppPickerForSlot = null
            },
            onDismiss = { showFullAppPickerForSlot = null }
        )
    }

    // Handle pickers triggered by setting existing action
    LaunchedEffect(keyMap) {
        TapSlot.entries.forEach { slot ->
            val value = keyMap.forSlot(slot)
            if (value == "action:pick_touch") showTouchForSlot = slot
            if (value == "action:pick_swipe") showSwipeForSlot = slot
            if (value == "action:pick_brightness") showBrightnessForSlot = slot
            if (value == "action:pick_volume") showVolumeForSlot = slot
            if (value == "action:pick_media") showMediaForSlot = slot
            if (value == "action:pick_url") showUrlForSlot = slot
            if (value == "action:pick_theme") showThemeForSlot = slot
            if (value == "action:pick_auto_brightness") showAutoBrightnessForSlot = slot
        }
    }

    showTouchForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_TOUCH_PREFIX)
        TouchPointPickerDialog(initialValue = if (existing == "action:pick_touch") null else existing, onDismiss = { showTouchForSlot = null }, onConfirmed = { x, y, hold ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold") }
            showTouchForSlot = null
        })
    }
    showSwipeForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_SWIPE_PREFIX)
        SwipePointPickerDialog(initialValue = if (existing == "action:pick_swipe") null else existing, onDismiss = { showSwipeForSlot = null }, onConfirmed = { x1, y1, x2, y2, hS, hE ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE") }
            showSwipeForSlot = null
        })
    }
    showBrightnessForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)?.toIntOrNull() ?: 50
        BrightnessPickerDialog(initialValue = existing, onDismiss = { showBrightnessForSlot = null }, onConfirmed = { pct ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_BRIGHTNESS_PREFIX}$pct") }
            showBrightnessForSlot = null
        })
    }
    showVolumeForSlot?.let { slot ->
        VolumePickerDialog(onDismiss = { showVolumeForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_VOLUME_PREFIX}$data") }
            showVolumeForSlot = null
        })
    }
    showMediaForSlot?.let { slot ->
        MediaPickerDialog(onDismiss = { showMediaForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_MEDIA_PREFIX}$data") }
            showMediaForSlot = null
        })
    }
    showUrlForSlot?.let { slot ->
        val existing = keyMap.forSlot(slot)?.removePrefix(Config.ACTION_URL_PREFIX) ?: ""
        UrlPickerDialog(initialValue = existing, onDismiss = { showUrlForSlot = null }, onConfirmed = { url ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_URL_PREFIX}$url") }
            showUrlForSlot = null
        })
    }
    showThemeForSlot?.let { slot ->
        ThemePickerDialog(onDismiss = { showThemeForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_THEME_PREFIX}$data") }
            showThemeForSlot = null
        })
    }
    showAutoBrightnessForSlot?.let { slot ->
        AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessForSlot = null }, onConfirmed = { data ->
            scope.launch { Config.set(ctx, slot, "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$data") }
            showAutoBrightnessForSlot = null
        })
    }
}

@Composable
private fun FavoritesActionSettings(
    config: CustomConfig,
    apps: List<AppInfo>,
    onUpdateApps: (List<String>) -> Unit,
    onUpdateVisibility: (Boolean, Boolean, Boolean, Boolean, Int) -> Unit
) {
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.select_favorites_title), style = MaterialTheme.typography.labelLarge)
            apps.forEach { app ->
                val isSelected = config.favoriteApps.contains(app.pkg)
                Row(modifier = Modifier.fillMaxWidth().clickable {
                    onUpdateApps(if (isSelected) config.favoriteApps - app.pkg else config.favoriteApps + app.pkg)
                }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp)); Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.size(24.dp))
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
            Text(stringResource(R.string.display_settings), style = MaterialTheme.typography.titleMedium, color = Color.White)

            ShortcutToggle(stringResource(R.string.keep_on_click), config.keepVisibleOnClick) {
                onUpdateVisibility(it, config.keepVisibleOnOutsideClick, config.keepVisibleOnBack, config.keepVisibleOnKey, config.autoHideSeconds)
            }
            ShortcutToggle(stringResource(R.string.keep_on_outside), config.keepVisibleOnOutsideClick) {
                onUpdateVisibility(config.keepVisibleOnClick, it, config.keepVisibleOnBack, config.keepVisibleOnKey, config.autoHideSeconds)
            }
            ShortcutToggle(stringResource(R.string.keep_on_key), config.keepVisibleOnKey) {
                onUpdateVisibility(config.keepVisibleOnClick, config.keepVisibleOnOutsideClick, config.keepVisibleOnBack, it, config.autoHideSeconds)
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.auto_hide), style = MaterialTheme.typography.labelLarge, color = Color.Gray)

            val autoHideOptions = listOf(0 to stringResource(R.string.never_hide), 2 to "2 ${stringResource(R.string.unit_seconds)}", 5 to "5 ${stringResource(R.string.unit_seconds)}", 10 to "10 ${stringResource(R.string.unit_seconds)}", 20 to "20 ${stringResource(R.string.unit_seconds)}")
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(autoHideOptions) { (secs, label) ->
                    val selected = config.autoHideSeconds == secs
                    Surface(
                        onClick = { onUpdateVisibility(config.keepVisibleOnClick, config.keepVisibleOnOutsideClick, config.keepVisibleOnBack, config.keepVisibleOnKey, secs) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) Color.White else Color(0xFF2B2B2B),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, color = if (selected) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlActionSettings(items: List<ControlItem>, onUpdate: (List<ControlItem>) -> Unit) {
    var showGridEditor by remember { mutableStateOf(false) }
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Button(onClick = { showGridEditor = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.setup_control_panel)) }
            if (showGridEditor) {
                Dialog(onDismissRequest = { showGridEditor = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                    ControlPanelContent(
                        initialItems = items,
                        isEditing = true,
                        onSave = { updatedItems -> onUpdate(updatedItems) },
                        onControlClick = { false },
                        onDismiss = { showGridEditor = false }
                    )
                }
            }
        }
    }
}

@Composable
fun SlotRow(slot: TapSlot, valueLabel: String, icon: ImageBitmap?, enabled: Boolean, isCustom: Boolean, expanded: Boolean, onExpandToggle: () -> Unit, onClick: () -> Unit, onIconClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(slot.labelRes), style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(valueLabel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onIconClick() }
            )
        }
        if (isCustom || valueLabel.startsWith(stringResource(R.string.shortcut_prefix))) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onExpandToggle, modifier = Modifier.size(28.dp)) {
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun MacroSelectionSettings(
    currentId: String,
    macroMode: MacroMode,
    macros: List<Macro>,
    onSelect: (String) -> Unit,
    onUpdateMode: (MacroMode) -> Unit
) {
    Surface(tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.macro_execution_mode), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUpdateMode(mode) }) {
                        RadioButton(selected = macroMode == mode, onClick = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(mode.labelRes))
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            Text(stringResource(R.string.select_macro), style = MaterialTheme.typography.labelLarge)
            if (macros.isEmpty()) {
                Text(stringResource(R.string.macros_empty), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            macros.forEach { macro ->
                val isSelected = currentId == macro.id
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(macro.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    EventIconDisplay(macro.icon, displaySize = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(macro.title, modifier = Modifier.weight(1f), color = if (isSelected) Color.White else Color.Gray)
                    RadioButton(selected = isSelected, onClick = null)
                }
            }
        }
    }
}

@Composable
fun SlotRowComposable(
    slot: TapSlot,
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    ctx: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onPick: (TapSlot) -> Unit,
    onAppPick: (TapSlot) -> Unit,
    onPowerCheck: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val value = keyMap.forSlot(slot)
    val customConfig = keyMap.customConfigs[slot] ?: CustomConfig()

    val matchingAction = remember(value) {
        if (value != null) {
            ActionRegistry.allActions.find { action ->
                isTargetMatch(action, value)
            }
        } else null
    }

    val isCustom = value == Config.ACTION_CONTROLS ||
                   value?.startsWith(Config.ACTION_FAVORITES_PREFIX) == true ||
                   value?.startsWith(Config.ACTION_TOUCH_PREFIX) == true ||
                   value?.startsWith(Config.ACTION_SWIPE_PREFIX) == true ||
                   value?.startsWith(Config.ACTION_FLASHLIGHT_PREFIX) == true ||
                   value == Config.ACTION_FLASHLIGHT ||
                   value?.startsWith(Config.ACTION_GLYPH_PREFIX) == true ||
                   value == Config.ACTION_GLYPH ||
                   value == Config.ACTION_GLYPH_TOGGLE ||
                   (matchingAction != null && matchingAction.pickerType != null && matchingAction.pickerType != "app")

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SlotRow(
            slot = slot, valueLabel = slotDisplay(ctx, value, keyMap.events, keyMap.macros, apps),
            icon = apps?.find { it.pkg == value }?.icon,
            enabled = apps != null,
            isCustom = isCustom,
            expanded = expanded, onExpandToggle = { expanded = !expanded },
            onClick = {
                if (slot == TapSlot.POWER_LONG && !VoiceAssistantService.isAppCurrentAssistant(ctx)) {
                    onPowerCheck()
                }
                onPick(slot)
            },
            onIconClick = {
                if (slot == TapSlot.POWER_LONG && !VoiceAssistantService.isAppCurrentAssistant(ctx)) {
                    onPowerCheck()
                }
                onAppPick(slot)
            }
        )

        if (expanded && value != null) {
            val content = if (matchingAction != null) {
                getActionContent(
                    action = matchingAction,
                    ctx = ctx,
                    keyMap = keyMap,
                    testedActionId = value,
                    onUpdateId = { newActionId ->
                        scope.launch { Config.set(ctx, slot, newActionId) }
                    }
                )
            } else null

            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                if (content != null) {
                    content()
                } else when {
                    value == Config.ACTION_CONTROLS -> {
                        ControlActionSettings(
                            items = customConfig.controlItems,
                            onUpdate = { scope.launch { Config.setControlItems(ctx, slot, it) } }
                        )
                    }
                    value.startsWith(Config.ACTION_FAVORITES_PREFIX) -> {
                        FavoritesActionSettings(
                            config = customConfig,
                            apps = apps ?: emptyList(),
                            onUpdateApps = { scope.launch { Config.setFavoriteApps(ctx, slot, it) } },
                            onUpdateVisibility = { onClick, onOutside, onBack, onKey, autoHide ->
                                scope.launch { Config.setCustomVisibility(ctx, slot, onClick, onOutside, onBack, onKey, autoHide) }
                            }
                        )
                    }
                    value.startsWith(Config.ACTION_TOUCH_PREFIX) -> {
                        Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_touch") } }) { Text(stringResource(R.string.change_coords)) }
                    }
                    value.startsWith(Config.ACTION_SWIPE_PREFIX) -> {
                        Button(onClick = { scope.launch { Config.set(ctx, slot, "action:pick_swipe") } }) { Text(stringResource(R.string.change_swipe)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionBlockCard(
    title: String,
    slots: List<TapSlot>,
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    ctx: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onClick: () -> Unit,
    onPick: (TapSlot) -> Unit,
    onAppPick: (TapSlot) -> Unit,
    onPowerCheck: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Block Header Row: Title on the left, Arrow icon on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        // Inner solid grey area container with divider lines
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
        ) {
            slots.forEachIndexed { index, slot ->
                SlotRowComposable(
                    slot = slot,
                    keyMap = keyMap,
                    apps = apps,
                    ctx = ctx,
                    scope = scope,
                    onPick = onPick,
                    onAppPick = onAppPick,
                    onPowerCheck = onPowerCheck
                )
                if (index < slots.size - 1) {
                    HorizontalDivider(
                        color = Color.Gray.copy(alpha = 0.3f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}



@Composable
fun InlineActionPicker(
    slot: TapSlot,
    currentValue: String?,
    ctx: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    onPick: (String?) -> Unit,
    onPowerCheck: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.select_action_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Spacer(Modifier.height(8.dp))
        UniversalActionListContent(
            currentActionId = currentValue,
            slot = slot,
            onActionSelected = { pkg ->
                if (slot == TapSlot.POWER_LONG && !VoiceAssistantService.isAppCurrentAssistant(ctx)) {
                    onPowerCheck()
                }
                scope.launch { Config.set(ctx, slot, pkg) }
            }
        )
    }
}




