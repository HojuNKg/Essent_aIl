package com.HojuNK.essent_all.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.HojuNK.essent_all.*
import com.HojuNK.essent_all.R
import com.HojuNK.essent_all.ui.components.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonActionsScreen(
    title: String,
    slots: List<TapSlot>,
    keyMap: KeyMap,
    apps: List<AppInfo>?,
    ctx: Context,
    scope: CoroutineScope,
    predictiveBackProgress: Float = 0f,
    onBack: () -> Unit
) {
    var selectedSlot by remember { mutableStateOf(slots.firstOrNull() ?: TapSlot.SINGLE) }
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

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val translateY = predictiveBackProgress * screenHeightPx
    val scaleVal = 1f - predictiveBackProgress * 0.08f

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = translateY
                scaleX = scaleVal
                scaleY = scaleVal
                alpha = (1f - predictiveBackProgress * 0.5f).coerceIn(0f, 1f)
                clip = predictiveBackProgress > 0.001f
                shape = RoundedCornerShape(with(density) { (predictiveBackProgress * 28f).dp.toPx() }.dp)
            },
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Stub (aspect ratio 3:2, where 3 is full screen width)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(24.dp))
                        .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Stub 3:2", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                }
            }

            // 2. Gesture Selector (HorizontalPager format from Event Editor)
            item {
                Column {
                    Text(stringResource(R.string.when_gesture_exec), color = Color.White, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(12.dp))
                    val initialPage = slots.indexOf(selectedSlot).coerceAtLeast(0)
                    val pagerState = rememberPagerState(initialPage = initialPage) { slots.size }

                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage < slots.size) {
                            selectedSlot = slots[pagerState.currentPage]
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        val slot = slots[page]
                        val isSelected = selectedSlot == slot
                        Surface(
                            color = if (isSelected) Color(0xFF2B2B2B) else Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.fillMaxSize().border(2.dp, if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(stringResource(slot.labelRes), color = Color.White, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // 3. Action configuration for the selectedSlot in this block
            item {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                SlotRowComposable(
                    slot = selectedSlot,
                    keyMap = keyMap,
                    apps = apps,
                    ctx = ctx,
                    scope = scope,
                    onPick = { picker = it },
                    onAppPick = { showFullAppPickerForSlot = it },
                    onPowerCheck = { showAssistantPermissionDialog = true }
                )
            }

            // 4. Inline List of Available Actions
            item {
                InlineActionPicker(
                    slot = selectedSlot,
                    currentValue = keyMap.forSlot(selectedSlot),
                    ctx = ctx,
                    scope = scope,
                    onPick = { pkg: String? ->
                        if (pkg == "action:full_app_list") {
                            showFullAppPickerForSlot = selectedSlot
                        } else {
                            picker = selectedSlot
                        }
                    },
                    onPowerCheck = { showAssistantPermissionDialog = true }
                )
            }
        }
    }

    // Pickers Dialogs
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
