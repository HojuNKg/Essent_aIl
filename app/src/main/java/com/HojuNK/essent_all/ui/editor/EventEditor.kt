package com.HojuNK.essent_all.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.model.*
import com.HojuNK.essent_all.util.*
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EventAddPanel(
    initialRule: EventRule,
    macros: List<Macro>,
    glyphPatterns: List<Macro> = emptyList(),
    apps: List<AppInfo>?,
    onDismiss: () -> Unit,
    onSave: (EventRule) -> Unit
) {
    var currentRule by rememberSaveable { mutableStateOf(initialRule) }
    var showIconPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // States for pickers
    var showAppMultiPicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRotationPicker by remember { mutableStateOf(false) }
    var showLightPicker by remember { mutableStateOf(false) }
    var showTouchPicker by remember { mutableStateOf(false) }
    var showSwipePicker by remember { mutableStateOf(false) }
    var showDelayPicker by remember { mutableStateOf(false) }
    var showBrightnessPicker by remember { mutableStateOf(false) }
    var showMacroPicker by remember { mutableStateOf(false) }
    var showBrightnessRangePicker by remember { mutableStateOf(false) }
    var showBatteryRangePicker by remember { mutableStateOf(false) }
    var showWifiPicker by remember { mutableStateOf(false) }
    var showBtPicker by remember { mutableStateOf(false) }
    var showLocPicker by remember { mutableStateOf(false) }
    var showDataPicker by remember { mutableStateOf(false) }
    var showChargingPicker by remember { mutableStateOf(false) }
    var showLockPicker by remember { mutableStateOf(false) }
    var showAirplanePicker by remember { mutableStateOf(false) }
    var showNfcPicker by remember { mutableStateOf(false) }
    var showAutoRotatePicker by remember { mutableStateOf(false) }
    var showHeadphonesPicker by remember { mutableStateOf(false) }
    var showDndPicker by remember { mutableStateOf(false) }
    var showMotionPicker by remember { mutableStateOf(false) }
    var showDarkModePicker by remember { mutableStateOf(false) }
    var showSilentPicker by remember { mutableStateOf(false) }
    var showVibratePicker by remember { mutableStateOf(false) }
    var showHotspotPicker by remember { mutableStateOf(false) }
    var showVolumePicker by remember { mutableStateOf(false) }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showWaitConfirmationPicker by remember { mutableStateOf(false) }
    var showUrlPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showAutoBrightnessPicker by remember { mutableStateOf(false) }
    var showFullAppPicker by remember { mutableStateOf(false) }
    var showGlyphPatternPicker by remember { mutableStateOf(false) }

    var editingConditionIndex by remember { mutableIntStateOf(-1) }
    var editingExclusionIndex by remember { mutableIntStateOf(-1) }
    var editingActionIndex by remember { mutableIntStateOf(-1) }
    var pickingForExclusion by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var actionIds by rememberSaveable { mutableStateOf(List(currentRule.actions.size) { UUID.randomUUID().toString() }) }
    if (actionIds.size != currentRule.actions.size) {
        if (actionIds.size < currentRule.actions.size) {
            actionIds = actionIds + List(currentRule.actions.size - actionIds.size) { UUID.randomUUID().toString() }
        } else {
            actionIds = actionIds.take(currentRule.actions.size)
        }
    }

    // Header animation logic
    val collapseProgress by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 1f
            else (scrollState.firstVisibleItemScrollOffset.toFloat() / 200f).coerceIn(0f, 1f)
        }
    }
    val isCollapsed by remember {
        derivedStateOf { collapseProgress > 0.1f }
    }

    Surface(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fixed Header Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f)
                    .drawBehind {
                        val blurHeight = 90.dp.toPx()
                        drawRect(color = Color.Black, size = size)
                        if (collapseProgress > 0f) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = collapseProgress), Color.Transparent),
                                    startY = size.height,
                                    endY = size.height + blurHeight
                                ),
                                topLeft = Offset(0f, size.height),
                                size = Size(size.width, blurHeight)
                            )
                        }
                    }
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundHeaderButton(icon = Icons.Default.Close, onClick = onDismiss)

                    AnimatedVisibility(
                        visible = isCollapsed,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = if (initialRule.title.isEmpty()) "СОЗДАЕМ ИВЕНТ" else "РЕДАКТИРУЕМ ИВЕНТ",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.graphicsLayer {
                                alpha = collapseProgress
                                scaleX = 0.8f + (collapseProgress * 0.2f)
                                scaleY = 0.8f + (collapseProgress * 0.2f)
                            }
                        )
                    }

                    RoundHeaderButton(
                        icon = Icons.Default.Check,
                        onClick = { if (currentRule.title.isNotEmpty()) onSave(currentRule) },
                        backgroundColor = Color(0xFF2B2B2B),
                        borderColor = null
                    )
                }

                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .graphicsLayer { alpha = collapseProgress },
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.75f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                    .clickable { scope.launch { scrollState.animateScrollToItem(0) } }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = currentRule.title.ifEmpty { "Ивент без названия" },
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(0.25f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1A1A1A), RoundedCornerShape(28.dp))
                                    .clickable { showIconPicker = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EventIconDisplay(currentRule.icon.ifEmpty { "⚡" }, displaySize = 24.dp)
                            }
                        }
                    }
                }
            }

            // Scrollable Content
            androidx.compose.foundation.lazy.LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp, start = 16.dp, end = 16.dp)
            ) {
                item {
                    // Dynamic spacer to push content below the expanded fixed header
                    Spacer(Modifier.height(120.dp * collapseProgress))
                }

                item {
                    Column(modifier = Modifier
                        .graphicsLayer {
                            alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                            translationY = -collapseProgress * 20f
                        }
                    ) {
                        val isNew = initialRule.title.isEmpty()
                        Text(
                            text = if (isNew) "СОЗДАЕМ НОВЫЙ\nИВЕНТ" else "РЕДАКТИРУЕМ\nИВЕНТ",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max)
                            .graphicsLayer {
                                alpha = (1f - collapseProgress * 2f).coerceIn(0f, 1f)
                                translationY = -collapseProgress * 10f
                            },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(0.75f)
                                .fillMaxHeight()
                                .background(Color.Black, RoundedCornerShape(24.dp))
                                .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Название ивента", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = currentRule.title,
                                    onValueChange = { currentRule = currentRule.copy(title = it) },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        if (currentRule.title.isEmpty()) {
                                            Text("Ивент без названия", color = Color.Gray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge)
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        LabeledPanel(
                            label = "Значёк",
                            value = currentRule.icon.ifEmpty { "⚡" },
                            modifier = Modifier.weight(0.25f).fillMaxHeight(),
                            onClick = { showIconPicker = true }
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Content from Logic Step
                eventLogicItems(
                    rule = currentRule,
                    onRuleChange = { currentRule = it },
                    apps = apps ?: emptyList(),
                    macros = macros,
                    glyphPatterns = glyphPatterns,
                    editingConditionIndex = { editingConditionIndex = it },
                    editingActionIndex = { editingActionIndex = it },
                    editingExclusionIndex = { editingExclusionIndex = it },
                    pickingForExclusion = { pickingForExclusion = it },
                    draggedIndex = draggedIndex,
                    onDraggedIndexChange = { draggedIndex = it },
                    actionIds = actionIds,
                    onActionIdsChange = { actionIds = it },
                    onShowTouchPicker = { showTouchPicker = true },
                    onShowSwipePicker = { showSwipePicker = true },
                    onShowDelayPicker = { showDelayPicker = true },
                    onShowBrightnessPicker = { showBrightnessPicker = true },
                    onShowMacroPicker = { showMacroPicker = true },
                    onShowVolumePicker = { showVolumePicker = true },
                    onShowMediaPicker = { showMediaPicker = true },
                    onShowUrlPicker = { showUrlPicker = true },
                    onShowThemePicker = { showThemePicker = true },
                    onShowWaitConfirmationPicker = { showWaitConfirmationPicker = true },
                    onShowAutoBrightnessPicker = { showAutoBrightnessPicker = true },
                    onShowAppMultiPicker = { showAppMultiPicker = true },
                    onShowTimePicker = { showTimePicker = true },
                    onShowRotationPicker = { showRotationPicker = true },
                    onShowLightPicker = { showLightPicker = true },
                    onShowBrightnessRangePicker = { showBrightnessRangePicker = true },
                    onShowBatteryRangePicker = { showBatteryRangePicker = true },
                    onShowWifiPicker = { showWifiPicker = true },
                    onShowBtPicker = { showBtPicker = true },
                    onShowLocPicker = { showLocPicker = true },
                    onShowDataPicker = { showDataPicker = true },
                    onShowChargingPicker = { showChargingPicker = true },
                    onShowLockPicker = { showLockPicker = true },
                    onShowAirplanePicker = { showAirplanePicker = true },
                    onShowNfcPicker = { showNfcPicker = true },
                    onShowAutoRotatePicker = { showAutoRotatePicker = true },
                    onShowHeadphonesPicker = { showHeadphonesPicker = true },
                    onShowDndPicker = { showDndPicker = true },
                    onShowMotionPicker = { showMotionPicker = true },
                    onShowDarkModePicker = { showDarkModePicker = true },
                    onShowSilentPicker = { showSilentPicker = true },
                    onShowVibratePicker = { showVibratePicker = true },
                    onShowHotspotPicker = { showHotspotPicker = true },
                    onShowFullAppPicker = { showFullAppPicker = true },
                    onShowGlyphPatternPicker = { showGlyphPatternPicker = true }
                )
            }
        }

        if (showIconPicker) {
            EventIconPickerDialog(
                currentIcon = currentRule.icon,
                apps = apps,
                onDismiss = { showIconPicker = false },
                onIconPicked = { icon ->
                    currentRule = currentRule.copy(icon = icon)
                    showIconPicker = false
                }
            )
        }

        if (showTouchPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_TOUCH_PREFIX))) {
            val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
            TouchPointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_TOUCH_PREFIX), onDismiss = { showTouchPicker = false; editingActionIndex = -1 }, onConfirmed = { x, y, hold ->
                val action = "${Config.ACTION_TOUCH_PREFIX}${x.toInt()}_${y.toInt()}_$hold"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showTouchPicker = false; editingActionIndex = -1
            })
        }
        if (showSwipePicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_SWIPE_PREFIX))) {
            val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
            SwipePointPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_SWIPE_PREFIX), onDismiss = { showSwipePicker = false; editingActionIndex = -1 }, onConfirmed = { x1, y1, x2, y2, hS, hE ->
                val action = "${Config.ACTION_SWIPE_PREFIX}${x1.toInt()}_${y1.toInt()}_${x2.toInt()}_${y2.toInt()}_${hS}_$hE"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showSwipePicker = false; editingActionIndex = -1
            })
        }
        if (showDelayPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_DELAY_PREFIX))) {
            val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
            DelayPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_DELAY_PREFIX), onDismiss = { showDelayPicker = false; editingActionIndex = -1 }, onConfirmed = { seconds ->
                val action = "${Config.ACTION_DELAY_PREFIX}$seconds"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showDelayPicker = false; editingActionIndex = -1
            })
        }
        if (showBrightnessPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_BRIGHTNESS_PREFIX))) {
            val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
            BrightnessPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_BRIGHTNESS_PREFIX)?.toIntOrNull() ?: 50, onDismiss = { showBrightnessPicker = false; editingActionIndex = -1 }, onConfirmed = { percent ->
                val action = "${Config.ACTION_BRIGHTNESS_PREFIX}$percent"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showBrightnessPicker = false; editingActionIndex = -1
            })
        }
        if (showMacroPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_MACRO_PREFIX))) {
            MacroPickerDialog(macros = macros, onDismiss = { showMacroPicker = false; editingActionIndex = -1 }, onMacroPicked = { macroId ->
                val action = "${Config.ACTION_MACRO_PREFIX}$macroId"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showMacroPicker = false; editingActionIndex = -1
            })
        }
        if (showVolumePicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_VOLUME_PREFIX))) {
            VolumePickerDialog(onDismiss = { showVolumePicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
                val action = "${Config.ACTION_VOLUME_PREFIX}$data"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showVolumePicker = false; editingActionIndex = -1
            })
        }
        if (showMediaPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_MEDIA_PREFIX))) {
            MediaPickerDialog(onDismiss = { showMediaPicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
                val action = "${Config.ACTION_MEDIA_PREFIX}$data"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showMediaPicker = false; editingActionIndex = -1
            })
        }
        if (showGlyphPatternPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_GLYPH_PATTERN_PREFIX))) {
            GlyphPatternPickerDialog(patterns = glyphPatterns, onDismiss = { showGlyphPatternPicker = false; editingActionIndex = -1 }, onPick = { patternIdWithPrefix ->
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, patternIdWithPrefix) } else currentRule.actions + patternIdWithPrefix
                currentRule = currentRule.copy(actions = newList)
                showGlyphPatternPicker = false; editingActionIndex = -1
            })
        }

        if (showUrlPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_URL_PREFIX))) {
            val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
            UrlPickerDialog(initialValue = existing?.removePrefix(Config.ACTION_URL_PREFIX) ?: "", onDismiss = { showUrlPicker = false; editingActionIndex = -1 }, onConfirmed = { url ->
                val action = "${Config.ACTION_URL_PREFIX}$url"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showUrlPicker = false; editingActionIndex = -1
            })
        }
        if (showThemePicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_THEME_PREFIX))) {
            ThemePickerDialog(onDismiss = { showThemePicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
                val action = "${Config.ACTION_THEME_PREFIX}$data"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showThemePicker = false; editingActionIndex = -1
            })
        }
        if (showWaitConfirmationPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX))) {
            val existing = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) currentRule.actions[editingActionIndex] else null
            WaitConfirmationDialog(initialValue = existing, onDismiss = { showWaitConfirmationPicker = false; editingActionIndex = -1 }, onConfirm = { action ->
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showWaitConfirmationPicker = false; editingActionIndex = -1
            })
        }
        if (showAutoBrightnessPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && currentRule.actions[editingActionIndex].startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX))) {
            AutoBrightnessPickerDialog(onDismiss = { showAutoBrightnessPicker = false; editingActionIndex = -1 }, onConfirmed = { data ->
                val action = "${Config.ACTION_AUTO_BRIGHTNESS_PREFIX}$data"
                val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, action) } else currentRule.actions + action
                currentRule = currentRule.copy(actions = newList)
                showAutoBrightnessPicker = false; editingActionIndex = -1
            })
        }
        if (showFullAppPicker || (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size && !currentRule.actions[editingActionIndex].startsWith("action:"))) {
            val selected = if (editingActionIndex != -1 && editingActionIndex < currentRule.actions.size) listOf(currentRule.actions[editingActionIndex]) else emptyList()
            UniversalAppPickerDialog(
                apps = (apps ?: emptyList()).filter { it.isLaunchable },
                selected = selected,
                multiSelect = false,
                onConfirmed = { picked ->
                    val pkg = picked.firstOrNull()
                    if (pkg != null) {
                        val newList = if (editingActionIndex != -1) currentRule.actions.toMutableList().apply { set(editingActionIndex, pkg) } else currentRule.actions + pkg
                        currentRule = currentRule.copy(actions = newList)
                    }
                    showFullAppPicker = false; editingActionIndex = -1
                },
                onDismiss = { showFullAppPicker = false; editingActionIndex = -1 }
            )
        }

        if (showAppMultiPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.APP) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.APP)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            val selectedApps = if (index != -1 && index < targetList.size) targetList[index].value.split(",").filter { it.isNotEmpty() } else emptyList()
            UniversalAppPickerDialog(
                apps = apps ?: emptyList(),
                selected = selectedApps,
                multiSelect = true,
                onConfirmed = { pickedApps ->
                    if (pickedApps.isNotEmpty()) {
                        val cond = EventCondition(ConditionType.APP, pickedApps.joinToString(","))
                        if (pickingForExclusion) {
                            val newList = currentRule.exclusions.toMutableList()
                            if (index != -1) newList[index] = cond else newList.add(cond)
                            currentRule = currentRule.copy(exclusions = newList)
                        } else {
                            val newList = currentRule.conditions.toMutableList()
                            if (index != -1) newList[index] = cond else newList.add(cond)
                            currentRule = currentRule.copy(conditions = newList)
                        }
                    }
                    showAppMultiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
                },
                onDismiss = { showAppMultiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }
            )
        }
        if (showRotationPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.ROTATION) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.ROTATION)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            val currentVal = if (index >= 0 && index < targetList.size) targetList[index].value else ""
            RotationPickerDialog(initialValue = if (currentVal.isNotEmpty()) currentVal else "PORTRAIT", onDismiss = { showRotationPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rot ->
                val cond = EventCondition(ConditionType.ROTATION, rot)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showRotationPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showLightPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.LIGHT) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.LIGHT)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            LightRangePickerDialog(initialValue = if (index != -1 && index < targetList.size) targetList[index].value else "0-100", onDismiss = { showLightPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rangeStr ->
                val cond = EventCondition(ConditionType.LIGHT, rangeStr)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showLightPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showTimePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.TIME) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.TIME)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            TimeRangePickerDialog(start = if (index != -1 && index < targetList.size) targetList[index].value.split("-").getOrNull(0) else null, end = if (index != -1 && index < targetList.size) targetList[index].value.split("-").getOrNull(1) else null, onDismiss = { showTimePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { s, e ->
                val cond = EventCondition(ConditionType.TIME, "$s-$e")
                if (pickingForExclusion) { if (index != -1) { val newList = currentRule.exclusions.toMutableList().apply { set(index, cond) }; currentRule = currentRule.copy(exclusions = newList) } else currentRule = currentRule.copy(exclusions = currentRule.exclusions + cond) }
                else { if (index != -1) { val newList = currentRule.conditions.toMutableList().apply { set(index, cond) }; currentRule = currentRule.copy(conditions = newList) } else currentRule = currentRule.copy(conditions = currentRule.conditions + cond) }
                showTimePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showBrightnessRangePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.BRIGHTNESS) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.BRIGHTNESS)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            RangePickerDialog(title = stringResource(R.string.brightness_screen_title), initialValue = if (index != -1 && index < targetList.size) targetList[index].value else "0-100", onDismiss = { showBrightnessRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rangeStr ->
                val cond = EventCondition(ConditionType.BRIGHTNESS, rangeStr)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showBrightnessRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showBatteryRangePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.BATTERY) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.BATTERY)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            RangePickerDialog(title = stringResource(R.string.battery_charge_title), initialValue = if (index != -1 && index < targetList.size) targetList[index].value else "0-100", onDismiss = { showBatteryRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { rangeStr ->
                val cond = EventCondition(ConditionType.BATTERY, rangeStr)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showBatteryRangePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showWifiPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.WIFI) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.WIFI)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_wifi_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showWifiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.WIFI, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showWifiPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showBtPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.BLUETOOTH) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.BLUETOOTH)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_bt_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showBtPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.BLUETOOTH, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showBtPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showLocPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.LOCATION) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.LOCATION)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_loc_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showLocPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.LOCATION, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showLocPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showDataPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.MOBILE_DATA) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.MOBILE_DATA)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_data_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showDataPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.MOBILE_DATA, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showDataPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showChargingPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.CHARGING) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.CHARGING)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_charging_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_connected), offLabel = stringResource(R.string.state_none), onDismiss = { showChargingPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.CHARGING, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showChargingPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showLockPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.SCREEN_LOCK) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.SCREEN_LOCK)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_lock_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_locked), offLabel = stringResource(R.string.state_unlocked), onDismiss = { showLockPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.SCREEN_LOCK, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showLockPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showAirplanePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.AIRPLANE_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.AIRPLANE_MODE)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_airplane_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showAirplanePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.AIRPLANE_MODE, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showAirplanePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showNfcPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.NFC) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.NFC)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = "NFC", current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showNfcPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.NFC, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showNfcPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showAutoRotatePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.AUTO_ROTATE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.AUTO_ROTATE)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.control_rotation), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showAutoRotatePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.AUTO_ROTATE, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showAutoRotatePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showHeadphonesPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.HEADPHONES) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.HEADPHONES)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_headphones_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_present), offLabel = stringResource(R.string.state_none), onDismiss = { showHeadphonesPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.HEADPHONES, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showHeadphonesPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showDndPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.DND) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.DND)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.control_dnd), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showDndPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.DND, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showDndPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showMotionPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.MOTION) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.MOTION)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_motion_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_moving), offLabel = stringResource(R.string.state_still), onDismiss = { showMotionPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.MOTION, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showMotionPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showDarkModePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.DARK_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.DARK_MODE)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.app_theme), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showDarkModePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.DARK_MODE, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showDarkModePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showSilentPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.SILENT_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.SILENT_MODE)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_silent_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showSilentPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.SILENT_MODE, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showSilentPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showVibratePicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.VIBRATE_MODE) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.VIBRATE_MODE)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_vibrate_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onDismiss = { showVibratePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.VIBRATE_MODE, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showVibratePicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
        if (showHotspotPicker || (editingConditionIndex >= 0 && editingConditionIndex < currentRule.conditions.size && currentRule.conditions[editingConditionIndex].type == ConditionType.HOTSPOT) || (editingExclusionIndex >= 0 && editingExclusionIndex < currentRule.exclusions.size && currentRule.exclusions[editingExclusionIndex].type == ConditionType.HOTSPOT)) {
            val index = if (pickingForExclusion) editingExclusionIndex else editingConditionIndex
            val targetList = if (pickingForExclusion) currentRule.exclusions else currentRule.conditions
            BinaryStatePickerDialog(title = stringResource(R.string.cond_hotspot_label).substringBefore("."), current = if (index != -1 && index < targetList.size) targetList[index].value else "ON", onLabel = stringResource(R.string.state_active), offLabel = stringResource(R.string.state_none), onDismiss = { showHotspotPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false }, onConfirmed = { state ->
                val cond = EventCondition(ConditionType.HOTSPOT, state)
                if (index != -1) { val newList = targetList.toMutableList().apply { set(index, cond) }; currentRule = if (pickingForExclusion) currentRule.copy(exclusions = newList) else currentRule.copy(conditions = newList) }
                else { currentRule = if (pickingForExclusion) currentRule.copy(exclusions = currentRule.exclusions + cond) else currentRule.copy(conditions = currentRule.conditions + cond) }
                showHotspotPicker = false; editingConditionIndex = -1; editingExclusionIndex = -1; pickingForExclusion = false
            })
        }
    }
}









fun LazyListScope.eventLogicItems(
    rule: EventRule,
    onRuleChange: (EventRule) -> Unit,
    apps: List<AppInfo>,
    macros: List<Macro>,
    glyphPatterns: List<Macro> = emptyList(),
    editingConditionIndex: (Int) -> Unit,
    editingActionIndex: (Int) -> Unit,
    editingExclusionIndex: (Int) -> Unit,
    pickingForExclusion: (Boolean) -> Unit,
    draggedIndex: Int,
    onDraggedIndexChange: (Int) -> Unit,
    actionIds: List<String>,
    onActionIdsChange: (List<String>) -> Unit,
    onShowTouchPicker: () -> Unit,
    onShowSwipePicker: () -> Unit,
    onShowDelayPicker: () -> Unit,
    onShowBrightnessPicker: () -> Unit,
    onShowMacroPicker: () -> Unit,
    onShowVolumePicker: () -> Unit,
    onShowMediaPicker: () -> Unit,
    onShowUrlPicker: () -> Unit,
    onShowThemePicker: () -> Unit,
    onShowWaitConfirmationPicker: () -> Unit,
    onShowAutoBrightnessPicker: () -> Unit,
    onShowAppMultiPicker: () -> Unit,
    onShowTimePicker: () -> Unit,
    onShowRotationPicker: () -> Unit,
    onShowLightPicker: () -> Unit,
    onShowBrightnessRangePicker: () -> Unit,
    onShowBatteryRangePicker: () -> Unit,
    onShowWifiPicker: () -> Unit,
    onShowBtPicker: () -> Unit,
    onShowLocPicker: () -> Unit,
    onShowDataPicker: () -> Unit,
    onShowChargingPicker: () -> Unit,
    onShowLockPicker: () -> Unit,
    onShowAirplanePicker: () -> Unit,
    onShowNfcPicker: () -> Unit,
    onShowAutoRotatePicker: () -> Unit,
    onShowHeadphonesPicker: () -> Unit,
    onShowDndPicker: () -> Unit,
    onShowMotionPicker: () -> Unit,
    onShowDarkModePicker: () -> Unit,
    onShowSilentPicker: () -> Unit,
    onShowVibratePicker: () -> Unit,
    onShowHotspotPicker: () -> Unit,
    onShowFullAppPicker: () -> Unit,
    onShowGlyphPatternPicker: () -> Unit
) {
    // Syntax Summary
    item {
        Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            val gestureLabel = stringResource(rule.triggerGesture.labelRes).lowercase()
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hasWait = rule.actions.any { it.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX) }
                val midText = if (hasWait) stringResource(R.string.event_shortcut_mid_text) else ""
                Text(
                    text = stringResource(R.string.event_summary_template, gestureLabel, rule.conditions.size, midText, rule.actions.size),
                    color = Color.White, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp
                )
                if (rule.exclusions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.event_exclusion_template, rule.exclusions.size),
                        color = Color.Red.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Gesture Selector (Two Pagers: Key Type & Press Type)
    item {
        val scope = rememberCoroutineScope()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.when_gesture_exec), color = Color.White, style = MaterialTheme.typography.labelLarge)
            
            val initialCatIdx = keyCategories.indexOfFirst { cat -> rule.triggerGesture in cat.slots }.coerceAtLeast(0)
            val catPagerState = rememberPagerState(initialPage = initialCatIdx) { keyCategories.size }
            
            val currentCat = keyCategories[catPagerState.currentPage]
            val initialSlotIdx = currentCat.slots.indexOf(rule.triggerGesture).takeIf { it != -1 } ?: rule.triggerGesture.getPressIndex().coerceAtMost(currentCat.slots.size - 1)
            val slotPagerState = rememberPagerState(initialPage = initialSlotIdx) { currentCat.slots.size }

            LaunchedEffect(catPagerState.currentPage) {
                val cat = keyCategories[catPagerState.currentPage]
                val idx = cat.slots.indexOf(rule.triggerGesture).takeIf { it != -1 } ?: rule.triggerGesture.getPressIndex().coerceAtMost(cat.slots.size - 1)
                if (slotPagerState.currentPage != idx) {
                    slotPagerState.scrollToPage(idx)
                }
                val newSlot = cat.slots[idx]
                if (newSlot != rule.triggerGesture) {
                    onRuleChange(rule.copy(triggerGesture = newSlot))
                }
            }

            LaunchedEffect(slotPagerState.currentPage) {
                val cat = keyCategories[catPagerState.currentPage]
                val idx = slotPagerState.currentPage.coerceIn(0, cat.slots.size - 1)
                val newSlot = cat.slots[idx]
                if (newSlot != rule.triggerGesture) {
                    onRuleChange(rule.copy(triggerGesture = newSlot))
                }
            }

            // Pager 1: Key Type
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Тип клавиши:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                HorizontalPager(
                    state = catPagerState,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    Surface(
                        onClick = { scope.launch { catPagerState.animateScrollToPage(page) } },
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, if (catPagerState.currentPage == page) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(26.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            Text(
                                text = stringResource(keyCategories[page].titleRes),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (catPagerState.currentPage == page) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Pager 2: Press Type
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Вид нажатия:", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                HorizontalPager(
                    state = slotPagerState,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    val slot = currentCat.slots.getOrElse(page) { currentCat.slots.first() }
                    Surface(
                        onClick = { scope.launch { slotPagerState.animateScrollToPage(page) } },
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, if (slotPagerState.currentPage == page) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(26.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                            Text(
                                text = stringResource(slot.getPressLabelRes()),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (slotPagerState.currentPage == page) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // Conditions
    item {
        val conditionTitle = if (rule.conditions.isEmpty()) "Когда выполнено условие:"
        else if (rule.conditions.size == 1) "Когда выполнено условие:"
        else if (rule.conditionLogicAll) "Когда выполнены условия (ВСЕ):"
        else "Когда выполнено условие (ХОТЯ БЫ ОДНО):"

        Text(
            text = conditionTitle,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { onRuleChange(rule.copy(conditionLogicAll = !rule.conditionLogicAll)) }
        )
        Spacer(Modifier.height(12.dp))

        rule.conditions.forEachIndexed { index, cond ->
            ConditionRow(condition = cond, apps = apps, onEdit = { editingConditionIndex(index) }, onDelete = {
                val newList = rule.conditions.toMutableList().apply { removeAt(index) }
                onRuleChange(rule.copy(conditions = newList))
            })
            if (rule.conditionLogicAll && index < rule.conditions.size - 1) {
                ConnectingLine()
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }

        var showAddConditionPicker by remember { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        PanelAddButton(label = "Новое условие", onClick = { showAddConditionPicker = true })

        if (showAddConditionPicker) {
            EventConditionPickerDialog(onPick = { type ->
                showAddConditionPicker = false
                when (type) {
                    "condition:app" -> onShowAppMultiPicker()
                    "condition:time" -> onShowTimePicker()
                    "condition:rotation" -> onShowRotationPicker()
                    "condition:light" -> onShowLightPicker()
                    "condition:brightness" -> onShowBrightnessRangePicker()
                    "condition:battery" -> onShowBatteryRangePicker()
                    "condition:wifi" -> onShowWifiPicker()
                    "condition:bluetooth" -> onShowBtPicker()
                    "condition:location" -> onShowLocPicker()
                    "condition:mobile_data" -> onShowDataPicker()
                    "condition:charging" -> onShowChargingPicker()
                    "condition:screen_lock" -> onShowLockPicker()
                    "condition:airplane_mode" -> onShowAirplanePicker()
                    "condition:nfc" -> onShowNfcPicker()
                    "condition:auto_rotate" -> onShowAutoRotatePicker()
                    "condition:headphones" -> onShowHeadphonesPicker()
                    "condition:dnd" -> onShowDndPicker()
                    "condition:motion" -> onShowMotionPicker()
                    "condition:dark_mode" -> onShowDarkModePicker()
                    "condition:silent_mode" -> onShowSilentPicker()
                    "condition:vibrate_mode" -> onShowVibratePicker()
                    "condition:hotspot" -> onShowHotspotPicker()
                    "condition:always" -> onRuleChange(rule.copy(conditions = rule.conditions + EventCondition(ConditionType.ALWAYS)))
                }
            }, onDismiss = { showAddConditionPicker = false })
        }
        Spacer(Modifier.height(24.dp))
    }

    // Actions
    item {
        val actionTitle = if (rule.actions.size <= 1) "Будет выполнено действие:" else "Будут выполнены действия:"
        Text(actionTitle, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(12.dp))
    }

    itemsIndexed(rule.actions, key = { index, _ -> actionIds.getOrElse(index) { index.toString() } }) { index, action ->
        ActionRow(
            action = action, index = index, apps = apps,
            macros = macros, glyphPatterns = glyphPatterns,
            isDragged = draggedIndex == index,
            onDragStart = { onDraggedIndexChange(index) },
            onDragEnd = { onDraggedIndexChange(-1) },
            onEdit = {
                editingActionIndex(index)
                if (!action.startsWith("action:")) {
                    onShowFullAppPicker()
                } else if (action.startsWith(Config.ACTION_TOUCH_PREFIX)) {
                    onShowTouchPicker()
                } else if (action.startsWith(Config.ACTION_SWIPE_PREFIX)) {
                    onShowSwipePicker()
                } else if (action.startsWith(Config.ACTION_DELAY_PREFIX)) {
                    onShowDelayPicker()
                } else if (action.startsWith(Config.ACTION_BRIGHTNESS_PREFIX)) {
                    onShowBrightnessPicker()
                } else if (action.startsWith(Config.ACTION_VOLUME_PREFIX)) {
                    onShowVolumePicker()
                } else if (action.startsWith(Config.ACTION_MEDIA_PREFIX)) {
                    onShowMediaPicker()
                } else if (action.startsWith(Config.ACTION_URL_PREFIX)) {
                    onShowUrlPicker()
                } else if (action.startsWith(Config.ACTION_THEME_PREFIX)) {
                    onShowThemePicker()
                } else if (action.startsWith(Config.ACTION_AUTO_BRIGHTNESS_PREFIX)) {
                    onShowAutoBrightnessPicker()
                } else if (action.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX)) {
                    onShowWaitConfirmationPicker()
                } else if (action.startsWith(Config.ACTION_GLYPH_PATTERN_PREFIX)) {
                    onShowGlyphPatternPicker()
                }
            },
            onMove = { from, to ->
                if (to in rule.actions.indices) {
                    val newList = rule.actions.toMutableList()
                    val item = newList.removeAt(from)
                    newList.add(to, item)

                    val newIds = actionIds.toMutableList()
                    val id = newIds.removeAt(from)
                    newIds.add(to, id)

                    onRuleChange(rule.copy(actions = newList))
                    onActionIdsChange(newIds)
                    onDraggedIndexChange(to)
                }
            },
            onDelete = {
                val newActions = rule.actions.toMutableList().apply { removeAt(index) }
                val newIds = actionIds.toMutableList().apply { removeAt(index) }
                onRuleChange(rule.copy(actions = newActions))
                onActionIdsChange(newIds)
            },
            onUpdateAction = { newAction ->
                val newActions = rule.actions.toMutableList()
                newActions[index] = newAction
                onRuleChange(rule.copy(actions = newActions))
            }
        )
        val isWait = action.startsWith(Config.ACTION_WAIT_CONFIRMATION_PREFIX)
        val isLast = index == rule.actions.size - 1
        if (!isLast && !isWait) {
            ConnectingLine()
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }

    item {
        var showActionPicker by remember { mutableStateOf(false) }
        var showFullAppPicker by remember { mutableStateOf(false) }

        Spacer(Modifier.height(8.dp))
        PanelAddButton(label = "Новое действие", onClick = { showActionPicker = true })
        if (showActionPicker) {
            EventActionPickerDialog(triggerGesture = rule.triggerGesture, onPick = { pkg ->
                if (pkg == "action:pick_touch") { editingActionIndex(-1); onShowTouchPicker() }
                else if (pkg == "action:pick_swipe") { editingActionIndex(-1); onShowSwipePicker() }
                else if (pkg == "action:pick_delay") { editingActionIndex(-1); onShowDelayPicker() }
                else if (pkg == "action:pick_brightness") { editingActionIndex(-1); onShowBrightnessPicker() }
                else if (pkg == "action:pick_macro") { editingActionIndex(-1); onShowMacroPicker() }
                else if (pkg == "action:pick_volume") { editingActionIndex(-1); onShowVolumePicker() }
                else if (pkg == "action:pick_media") { editingActionIndex(-1); onShowMediaPicker() }
                else if (pkg == "action:pick_url") { editingActionIndex(-1); onShowUrlPicker() }
                else if (pkg == "action:pick_theme") { editingActionIndex(-1); onShowThemePicker() }
                else if (pkg == "action:pick_auto_brightness") { editingActionIndex(-1); onShowAutoBrightnessPicker() }
                else if (pkg == "action:pick_wait_confirmation") { editingActionIndex(-1); onShowWaitConfirmationPicker() }
                else if (pkg == "action:pick_glyph_pattern") { editingActionIndex(-1); onShowGlyphPatternPicker() }
                else if (pkg == "action:full_app_list") { editingActionIndex(-1); onShowFullAppPicker() }
                else if (pkg != null) onRuleChange(rule.copy(actions = rule.actions + pkg))
                showActionPicker = false
            }, onDismiss = { showActionPicker = false })
        }
        Spacer(Modifier.height(24.dp))
    }

    // Exclusions
    item {
        val exclusionTitle = if (rule.exclusions.isEmpty()) "Кроме исключения:"
        else if (rule.exclusions.size == 1) "Кроме исключения:"
        else if (rule.exclusionLogicAll) "Кроме исключений (ВСЕ):"
        else "Кроме исключения (ХОТЯ БЫ ОДНО):"

        Text(
            text = exclusionTitle,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable { onRuleChange(rule.copy(exclusionLogicAll = !rule.exclusionLogicAll)) }
        )
        Spacer(Modifier.height(12.dp))

        rule.exclusions.forEachIndexed { index, excl ->
            ConditionRow(condition = excl, apps = apps, isExclusion = true, onEdit = { pickingForExclusion(true); editingExclusionIndex(index) }, onDelete = {
                val newList = rule.exclusions.toMutableList().apply { removeAt(index) }
                onRuleChange(rule.copy(exclusions = newList))
            })
            if (rule.exclusionLogicAll && index < rule.exclusions.size - 1) {
                ConnectingLine()
            } else {
                Spacer(Modifier.height(12.dp))
            }
        }

        var showAddExclusionPicker by remember { mutableStateOf(false) }
        Spacer(Modifier.height(8.dp))
        PanelAddButton(label = "Новое исключение", onClick = { showAddExclusionPicker = true })
        if (showAddExclusionPicker) {
            EventConditionPickerDialog(onPick = { type ->
                showAddExclusionPicker = false; pickingForExclusion(true)
                when (type) {
                    "condition:app" -> onShowAppMultiPicker()
                    "condition:time" -> onShowTimePicker()
                    "condition:rotation" -> onShowRotationPicker()
                    "condition:light" -> onShowLightPicker()
                    "condition:brightness" -> onShowBrightnessRangePicker()
                    "condition:battery" -> onShowBatteryRangePicker()
                    "condition:wifi" -> onShowWifiPicker()
                    "condition:bluetooth" -> onShowBtPicker()
                    "condition:location" -> onShowLocPicker()
                    "condition:mobile_data" -> onShowDataPicker()
                    "condition:charging" -> onShowChargingPicker()
                    "condition:screen_lock" -> onShowLockPicker()
                    "condition:airplane_mode" -> onShowAirplanePicker()
                    "condition:nfc" -> onShowNfcPicker()
                    "condition:auto_rotate" -> onShowAutoRotatePicker()
                    "condition:headphones" -> onShowHeadphonesPicker()
                    "condition:dnd" -> onShowDndPicker()
                    "condition:motion" -> onShowMotionPicker()
                    "condition:dark_mode" -> onShowDarkModePicker()
                    "condition:silent_mode" -> onShowSilentPicker()
                    "condition:vibrate_mode" -> onShowVibratePicker()
                    "condition:hotspot" -> onShowHotspotPicker()
                    "condition:always" -> onRuleChange(rule.copy(exclusions = rule.exclusions + EventCondition(ConditionType.ALWAYS)))
                }
            }, onDismiss = { showAddExclusionPicker = false })
        }
    }
}

@Composable
fun ConditionRow(
    condition: EventCondition,
    apps: List<AppInfo>,
    isExclusion: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
            .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeIcon = when(condition.type) {
                ConditionType.APP -> Icons.Default.Apps
                ConditionType.TIME -> Icons.Default.Schedule
                ConditionType.BATTERY -> Icons.Default.BatteryChargingFull
                ConditionType.LIGHT -> Icons.Default.LightMode
                else -> Icons.Default.Tune
            }
            Icon(typeIcon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (isExclusion) "Исключение" else "Условие", color = Color.White, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.Close, null, tint = Color.White,
                modifier = Modifier.size(16.dp).clickable { onDelete() }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(18.dp))
                .clickable { onEdit() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val label = when (condition.type) {
                ConditionType.ALWAYS -> stringResource(R.string.always)
                ConditionType.APP -> {
                    val pkgs = condition.value.split(",").filter { it.isNotEmpty() }
                    if (pkgs.isEmpty()) "Любые приложения"
                    else pkgs.mapNotNull { p -> apps.find { it.pkg == p }?.label }.joinToString(", ")
                }
                ConditionType.TIME -> "${stringResource(R.string.time_label)}: ${condition.value}"
                ConditionType.ROTATION -> {
                    val rotLabel = when(condition.value) {
                        "PORTRAIT" -> stringResource(R.string.rotation_portrait)
                        "FACE_UP" -> stringResource(R.string.rotation_face_up)
                        "FACE_DOWN" -> stringResource(R.string.rotation_face_down)
                        "LANDSCAPE_LEFT" -> stringResource(R.string.rotation_landscape_left)
                        "LANDSCAPE_RIGHT" -> stringResource(R.string.rotation_landscape_right)
                        else -> condition.value
                    }
                    stringResource(R.string.orientation_label, rotLabel)
                }
                ConditionType.LIGHT -> {
                    if (condition.value.contains("-")) {
                        val parts = condition.value.split("-")
                        stringResource(R.string.light_range_label, parts[0].toInt(), parts[1].toInt())
                    } else "${stringResource(R.string.light_label)}: ${condition.value}"
                }
                ConditionType.BRIGHTNESS -> {
                    val parts = condition.value.split("-")
                    "${stringResource(R.string.brightness_label)}: ${stringResource(R.string.range_percent_label, parts.getOrNull(0)?.toInt() ?: 0, parts.getOrNull(1)?.toInt() ?: 100)}"
                }
                ConditionType.BATTERY -> {
                    val parts = condition.value.split("-")
                    "${stringResource(R.string.battery_label)}: ${stringResource(R.string.range_percent_label, parts.getOrNull(0)?.toInt() ?: 0, parts.getOrNull(1)?.toInt() ?: 100)}"
                }
                ConditionType.WIFI -> "${stringResource(R.string.wifi_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.BLUETOOTH -> "${stringResource(R.string.bt_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.LOCATION -> "${stringResource(R.string.location_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.MOBILE_DATA -> "${stringResource(R.string.mobile_data_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.CHARGING -> "${stringResource(R.string.charging_label)}: ${if (condition.value == "ON") stringResource(R.string.state_connected) else stringResource(R.string.state_none)}"
                ConditionType.SCREEN_LOCK -> "${stringResource(R.string.screen_lock_label)}: ${if (condition.value == "ON") stringResource(R.string.state_locked) else stringResource(R.string.state_unlocked)}"
                ConditionType.AIRPLANE_MODE -> "${stringResource(R.string.airplane_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.NFC -> "${stringResource(R.string.nfc_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.AUTO_ROTATE -> "${stringResource(R.string.auto_rotate_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.HEADPHONES -> "${stringResource(R.string.headphones_label)}: ${if (condition.value == "ON") stringResource(R.string.state_present) else stringResource(R.string.state_none)}"
                ConditionType.DND -> "${stringResource(R.string.dnd_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.MOTION -> "${stringResource(R.string.motion_label)}: ${if (condition.value == "ON") stringResource(R.string.state_moving) else stringResource(R.string.state_still)}"
                ConditionType.DARK_MODE -> "${stringResource(R.string.dark_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.SILENT_MODE -> "${stringResource(R.string.silent_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.VIBRATE_MODE -> "${stringResource(R.string.vibrate_mode_label)}: ${if (condition.value == "ON") stringResource(R.string.state_on_short) else stringResource(R.string.state_off_short)}"
                ConditionType.HOTSPOT -> "${stringResource(R.string.hotspot_label)}: ${if (condition.value == "ON") stringResource(R.string.state_active) else stringResource(R.string.state_none)}"
                else -> condition.value
            }
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class KeyCategory(
    val titleRes: Int,
    val slots: List<TapSlot>
)

private val keyCategories = listOf(
    KeyCategory(R.string.section_essential_key, listOf(TapSlot.SINGLE, TapSlot.LONG, TapSlot.DOUBLE, TapSlot.TRIPLE)),
    KeyCategory(R.string.section_power_button, listOf(TapSlot.POWER_LONG)),
    KeyCategory(R.string.section_volume_up, listOf(TapSlot.VOL_UP_SINGLE, TapSlot.VOL_UP_LONG, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_UP_TRIPLE)),
    KeyCategory(R.string.section_volume_down, listOf(TapSlot.VOL_DOWN_SINGLE, TapSlot.VOL_DOWN_LONG, TapSlot.VOL_DOWN_DOUBLE, TapSlot.VOL_DOWN_TRIPLE))
)

private fun TapSlot.getPressIndex(): Int = when (this) {
    TapSlot.SINGLE, TapSlot.VOL_UP_SINGLE, TapSlot.VOL_DOWN_SINGLE -> 0
    TapSlot.LONG, TapSlot.VOL_UP_LONG, TapSlot.VOL_DOWN_LONG, TapSlot.POWER_LONG -> 1
    TapSlot.DOUBLE, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_DOWN_DOUBLE -> 2
    TapSlot.TRIPLE, TapSlot.VOL_UP_TRIPLE, TapSlot.VOL_DOWN_TRIPLE -> 3
    else -> 0
}

private fun TapSlot.getPressLabelRes(): Int = when (this) {
    TapSlot.SINGLE, TapSlot.VOL_UP_SINGLE, TapSlot.VOL_DOWN_SINGLE -> R.string.press_single
    TapSlot.LONG, TapSlot.VOL_UP_LONG, TapSlot.VOL_DOWN_LONG, TapSlot.POWER_LONG -> R.string.press_hold
    TapSlot.DOUBLE, TapSlot.VOL_UP_DOUBLE, TapSlot.VOL_DOWN_DOUBLE -> R.string.press_double
    TapSlot.TRIPLE, TapSlot.VOL_UP_TRIPLE, TapSlot.VOL_DOWN_TRIPLE -> R.string.press_triple
    else -> labelRes
}
