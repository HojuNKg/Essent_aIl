package com.HojuNK.essent_all

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap,
    val isLaunchable: Boolean = true,
    val isSystem: Boolean = false
)

suspend fun loadLaunchableApps(ctx: Context): List<AppInfo> =
    withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        // Get all installed applications, including system ones
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        allApps.mapNotNull { appInfo ->
            try {
                val pkg = appInfo.packageName
                // Human-readable label
                val label = pm.getApplicationLabel(appInfo).toString()
                
                // Check if it's launchable
                val isLaunchable = pm.getLaunchIntentForPackage(pkg) != null
                
                // Check if it's a system app
                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                
                // Load icon (resized for consistency)
                val icon = appInfo.loadIcon(pm).toBitmap(144, 144).asImageBitmap()
                
                AppInfo(
                    pkg = pkg,
                    label = label,
                    icon = icon,
                    isLaunchable = isLaunchable,
                    isSystem = isSystem
                )
            } catch (e: Exception) {
                null
            }
        }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
    }

fun getControlIcon(type: ControlType): ImageVector = when(type) {
    ControlType.EDIT -> Icons.Default.Edit
    ControlType.FLASHLIGHT -> Icons.Default.FlashlightOn
    ControlType.ROTATION -> Icons.Default.ScreenRotation
    ControlType.VOLUME -> Icons.AutoMirrored.Filled.VolumeUp
    ControlType.DND -> Icons.Default.DoNotDisturbOn
    ControlType.SETTINGS -> Icons.Default.Settings
    ControlType.APP -> Icons.Default.Apps
    ControlType.URL -> Icons.Default.Language
    else -> Icons.Default.Circle
}

@Composable
fun ControlPanelDialog(
    initialItems: List<ControlItem>,
    onSave: (List<ControlItem>) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ControlPanelContent(
            initialItems = initialItems,
            isEditing = true,
            onSave = onSave,
            onControlClick = { false },
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlPanelContent(
    initialItems: List<ControlItem>,
    isEditing: Boolean,
    apps: List<AppInfo>? = null,
    onSave: (List<ControlItem>) -> Unit,
    onControlClick: (ControlItem) -> Boolean, // Returns true if should dismiss
    onDismiss: () -> Unit
) {
    var items by remember(initialItems) { mutableStateOf(initialItems) }
    var selectedItem by remember { mutableStateOf<ControlItem?>(null) }
    var showAddMenu by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isMoving by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showUrlInput by remember { mutableStateOf(false) }
    
    val ctx = LocalContext.current
    var currentApps by remember { mutableStateOf<List<AppInfo>?>(apps) }
    
    LaunchedEffect(apps) {
        if (apps != null) currentApps = apps
    }
    
    LaunchedEffect(Unit) {
        if (currentApps == null) {
            currentApps = loadLaunchableApps(ctx)
        }
    }

    val white = Color.White
    val dark = Color(0xFF1A1A1A)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(if (isEditing) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f))
        .pointerInput(isEditing) {
            if (!isEditing) {
                detectTapGestures { onDismiss() }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.55f),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = dark,
            border = BorderStroke(1.dp, white.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.edit_mode), color = Color.White, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onSave(items); onDismiss() }) {
                            Text(stringResource(R.string.done), color = white)
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val gridWidth = this.maxWidth
                    val cellSize = gridWidth / 4
                    val density = LocalDensity.current

                    if (isEditing) {
                        Canvas(modifier = Modifier.fillMaxSize().pointerInput(isEditing) {
                            detectTapGestures { offset ->
                                val cellSizePx = with(density) { cellSize.toPx() }
                                val x = (offset.x / cellSizePx).toInt().coerceIn(0, 3)
                                val y = (offset.y / cellSizePx).toInt().coerceIn(0, 3)
                                val clickedItem = items.find { x >= it.x && x < it.x + it.w && y >= it.y && y < it.y + it.h }
                                
                                if (isMoving && selectedItem != null) {
                                    val movedItem = selectedItem!!.copy(x = x, y = y)
                                    items = items.filter { it != selectedItem } + movedItem
                                    selectedItem = movedItem
                                    isMoving = false
                                } else if (clickedItem != null) {
                                    selectedItem = clickedItem
                                } else {
                                    showAddMenu = x to y
                                    selectedItem = null
                                }
                            }
                        }) {
                            val color = Color.Gray.copy(alpha = 0.2f)
                            val cellSizePx = cellSize.toPx()
                            for (i in 0..4) {
                                drawLine(color, start = Offset(i * cellSizePx, 0f), end = Offset(i * cellSizePx, 4 * cellSizePx))
                                drawLine(color, start = Offset(0f, i * cellSizePx), end = Offset(4 * cellSizePx, i * cellSizePx))
                            }
                        }
                    }

                    items.forEach { item ->
                        val isSelected = isEditing && selectedItem == item
                        Box(modifier = Modifier
                            .offset(cellSize * item.x, cellSize * item.y)
                            .size(cellSize * item.w, cellSize * item.h)
                            .padding(6.dp)
                            .background(
                                color = if (isSelected) white.copy(alpha = 0.8f) else white,
                                shape = CircleShape
                            )
                            .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                            .clickable {
                                if (isEditing) {
                                    selectedItem = item
                                } else {
                                    if (onControlClick(item)) onDismiss()
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.type == ControlType.APP && item.extra != null) {
                                val appIcon = currentApps?.find { it.pkg == item.extra }?.icon
                                if (appIcon != null) {
                                    Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(cellSize * 0.5f))
                                } else {
                                    Icon(imageVector = getControlIcon(item.type), contentDescription = null, modifier = Modifier.size(cellSize * 0.5f), tint = Color.Black)
                                }
                            } else {
                                Icon(imageVector = getControlIcon(item.type), contentDescription = null, modifier = Modifier.size(cellSize * 0.5f), tint = Color.Black)
                            }
                        }
                    }

                    if (isEditing) {
                        selectedItem?.let { item ->
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.8f),
                                tonalElevation = 8.dp
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ControlResizeButton("1x1") { items = items.filter { it != item } + item.copy(w = 1, h = 1) }
                                        ControlResizeButton("1x2") { items = items.filter { it != item } + item.copy(w = 1, h = 2) }
                                        ControlResizeButton("2x1") { items = items.filter { it != item } + item.copy(w = 2, h = 1) }
                                        ControlResizeButton("2x2") { items = items.filter { it != item } + item.copy(w = 2, h = 2) }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    if (item.type == ControlType.APP) {
                                        Button(onClick = { showAppPicker = true }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = Color.Black)) { Text(stringResource(R.string.select_app)) }
                                        Spacer(Modifier.height(8.dp))
                                    } else if (item.type == ControlType.URL) {
                                        Button(onClick = { showUrlInput = true }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = Color.Black)) { Text(stringResource(R.string.enter_url)) }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Button(onClick = { isMoving = true }, colors = ButtonDefaults.buttonColors(containerColor = white, contentColor = Color.Black)) { Text(if (isMoving) stringResource(R.string.tap_in_cell) else stringResource(R.string.move)) }
                                        IconButton(onClick = { items = items.filter { it != item }; selectedItem = null }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddMenu != null) {
            val (x, y) = showAddMenu!!
            Dialog(onDismissRequest = { showAddMenu = null }) {
                Surface(shape = RoundedCornerShape(16.dp), color = dark) {
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.7f).width(280.dp)) {
                        item { Text(stringResource(R.string.add_button), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = Color.White) }
                        items(ControlType.entries.toTypedArray()) { type ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { 
                                items = items + ControlItem(type, x, y)
                                showAddMenu = null 
                            }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(getControlIcon(type), null, tint = white); Spacer(Modifier.width(12.dp)); Text(stringResource(type.labelRes), color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        if (showAppPicker && currentApps != null) {
            UniversalAppPickerDialog(
                apps = currentApps!!.filter { it.isLaunchable },
                selected = listOfNotNull(selectedItem?.extra),
                multiSelect = false,
                onConfirmed = { picked ->
                    val pkg = picked.firstOrNull()
                    if (pkg != null) {
                        items = items.map { if (it == selectedItem) it.copy(extra = pkg) else it }
                        selectedItem = items.find { it.type == selectedItem!!.type && it.x == selectedItem!!.x }
                    }
                    showAppPicker = false
                },
                onDismiss = { showAppPicker = false }
            )
        }

        if (showUrlInput) {
            var urlText by remember { mutableStateOf(selectedItem?.extra ?: "") }
            Dialog(onDismissRequest = { showUrlInput = false }) {
                Surface(shape = RoundedCornerShape(16.dp), color = dark, modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.enter_url_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
                        TextField(value = urlText, onValueChange = { urlText = it }, modifier = Modifier.fillMaxWidth())
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showUrlInput = false }) { Text(stringResource(R.string.cancel), color = white) }
                            TextButton(onClick = { 
                                items = items.map { if (it == selectedItem) it.copy(extra = urlText) else it }
                                selectedItem = items.find { it.type == selectedItem!!.type && it.x == selectedItem!!.x }
                                showUrlInput = false 
                            }) { Text("OK", color = white) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlResizeButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp), border = BorderStroke(1.dp, Color.White)) { 
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White) 
    }
}
